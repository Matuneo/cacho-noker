package com.matuneo.maestroia.ai

import android.app.DownloadManager
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Environment
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

data class ModelDownloadState(
    val active: Boolean = false,
    val progress: Int = 0,
    val message: String = "Modelo no instalado",
    val modelPath: String? = null
)

class ModelManager(private val context: Context) {
    companion object {
        const val MODEL_FILE = "qwen3_0.6b_nothink_q4_block32_ekv1280.litertlm"
        const val MODEL_URL = "https://huggingface.co/litert-community/Qwen3-0.6B-int4/resolve/main/qwen3_0.6b_nothink_q4_block32_ekv1280.litertlm?download=true"
    }

    private val downloadsDir: File = requireNotNull(
        context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
    ) { "Almacenamiento externo no disponible" }
    private val modelFile = File(downloadsDir, MODEL_FILE)
    private val importedFile = File(context.filesDir, "models/modelo_importado.litertlm")

    fun existingModel(): File? = when {
        importedFile.exists() && importedFile.length() > 10_000_000 -> importedFile
        modelFile.exists() && modelFile.length() > 10_000_000 -> modelFile
        else -> null
    }

    fun deleteModels() {
        if (modelFile.exists()) modelFile.delete()
        if (importedFile.exists()) importedFile.delete()
    }

    fun beginRecommendedDownload(): Long {
        if (modelFile.exists()) modelFile.delete()
        val request = DownloadManager.Request(Uri.parse(MODEL_URL))
            .setTitle("Profesor IA — modelo local")
            .setDescription("Descargando el cerebro de programación (347 MB)")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(false)
            .setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, MODEL_FILE)
        return context.getSystemService(DownloadManager::class.java).enqueue(request)
    }

    suspend fun observeDownload(
        downloadId: Long,
        onUpdate: (ModelDownloadState) -> Unit
    ): File? = withContext(Dispatchers.IO) {
        val manager = context.getSystemService(DownloadManager::class.java)
        while (true) {
            val cursor = manager.query(DownloadManager.Query().setFilterById(downloadId))
            cursor.use {
                if (!it.moveToFirst()) {
                    onUpdate(ModelDownloadState(message = "La descarga ya no está disponible"))
                    return@withContext null
                }
                val status = it.int(DownloadManager.COLUMN_STATUS)
                val downloaded = it.long(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                val total = it.long(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
                val progress = if (total > 0) ((downloaded * 100) / total).toInt() else 0
                when (status) {
                    DownloadManager.STATUS_SUCCESSFUL -> {
                        onUpdate(ModelDownloadState(false, 100, "Modelo instalado", modelFile.absolutePath))
                        return@withContext modelFile.takeIf { file -> file.exists() }
                    }
                    DownloadManager.STATUS_FAILED -> {
                        val reason = it.int(DownloadManager.COLUMN_REASON)
                        onUpdate(ModelDownloadState(message = "Descarga fallida (código $reason)"))
                        return@withContext null
                    }
                    DownloadManager.STATUS_PAUSED -> onUpdate(ModelDownloadState(true, progress, "Descarga pausada"))
                    DownloadManager.STATUS_PENDING -> onUpdate(ModelDownloadState(true, progress, "Esperando conexión…"))
                    else -> onUpdate(ModelDownloadState(true, progress, "Descargando modelo… $progress%"))
                }
            }
            delay(800)
        }
    }

    suspend fun importModel(uri: Uri): File? = withContext(Dispatchers.IO) {
        importedFile.parentFile?.mkdirs()
        val temp = File(importedFile.parentFile, "modelo_importado.tmp")
        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                temp.outputStream().buffered().use { output -> input.copyTo(output, 1024 * 1024) }
            } ?: return@withContext null
            if (temp.length() < 10_000_000) {
                temp.delete()
                return@withContext null
            }
            if (importedFile.exists()) importedFile.delete()
            if (!temp.renameTo(importedFile)) {
                temp.copyTo(importedFile, overwrite = true)
                temp.delete()
            }
            importedFile
        } catch (_: Throwable) {
            temp.delete()
            null
        }
    }

    private fun Cursor.int(column: String): Int = getInt(getColumnIndexOrThrow(column))
    private fun Cursor.long(column: String): Long = getLong(getColumnIndexOrThrow(column))
}

