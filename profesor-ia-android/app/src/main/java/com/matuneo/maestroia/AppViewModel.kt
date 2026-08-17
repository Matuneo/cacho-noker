package com.matuneo.maestroia

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.matuneo.maestroia.ai.ModelDownloadState
import com.matuneo.maestroia.ai.ModelManager
import com.matuneo.maestroia.ai.TutorEngine
import com.matuneo.maestroia.data.CourseContent
import com.matuneo.maestroia.python.ExecutionResult
import com.matuneo.maestroia.python.PythonRunner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

enum class AppTab { HOME, ROUTES, TUTOR, LAB, PROJECTS }
enum class ModelPhase { MISSING, DOWNLOADING, LOADING, READY, ERROR }

data class ModelUiState(
    val phase: ModelPhase = ModelPhase.MISSING,
    val progress: Int = 0,
    val message: String = "Instala el modelo para activar el tutor local"
)

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val user: Boolean,
    val text: String
)

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val modelManager = ModelManager(application)
    private val tutor = TutorEngine(application)
    private val pythonRunner = PythonRunner(application)
    private val prefs = application.getSharedPreferences("learning", 0)

    var currentTab by mutableStateOf(AppTab.HOME)
    var selectedTrack by mutableStateOf("Python")
    var selectedLanguage by mutableStateOf("Python")
    var chatInput by mutableStateOf("")
    var code by mutableStateOf(DEFAULT_CODE)
    var execution by mutableStateOf<ExecutionResult?>(null)
    var codeAnalysis by mutableStateOf("")
    var busyLab by mutableStateOf(false)
    var modelUi by mutableStateOf(ModelUiState())
    val messages = mutableStateListOf(
        ChatMessage(
            user = false,
            text = "Hola. Soy tu Profesor IA. Puedo enseñarte desde cero, revisar código línea por línea y ayudarte a construir aplicaciones Android profesionales."
        )
    )
    val completedModules = mutableStateListOf<String>()

    val totalModules: Int get() = CourseContent.allModules.size
    val progress: Float get() = if (totalModules == 0) 0f else completedModules.size.toFloat() / totalModules
    val modelReady: Boolean get() = modelUi.phase == ModelPhase.READY

    init {
        completedModules.addAll(prefs.getStringSet("completed", emptySet()).orEmpty())
        modelManager.existingModel()?.let { loadModel(it.absolutePath) }
    }

    fun toggleModule(id: String) {
        if (completedModules.contains(id)) completedModules.remove(id) else completedModules.add(id)
        prefs.edit().putStringSet("completed", completedModules.toSet()).apply()
    }

    fun downloadRecommendedModel() {
        if (modelUi.phase == ModelPhase.DOWNLOADING || modelUi.phase == ModelPhase.LOADING) return
        val id = try {
            modelManager.beginRecommendedDownload()
        } catch (error: Throwable) {
            modelUi = ModelUiState(ModelPhase.ERROR, message = "No se pudo iniciar: ${error.message}")
            return
        }
        modelUi = ModelUiState(ModelPhase.DOWNLOADING, message = "Iniciando descarga…")
        viewModelScope.launch {
            val file = modelManager.observeDownload(id) { state -> updateDownloadState(state) }
            if (file != null) loadModel(file.absolutePath)
        }
    }

    fun importModel(uri: Uri) {
        modelUi = ModelUiState(ModelPhase.LOADING, message = "Copiando modelo seleccionado…")
        viewModelScope.launch {
            val file = modelManager.importModel(uri)
            if (file == null) {
                modelUi = ModelUiState(ModelPhase.ERROR, message = "El archivo no es un modelo válido")
            } else {
                loadModel(file.absolutePath)
            }
        }
    }

    private fun updateDownloadState(state: ModelDownloadState) {
        viewModelScope.launch(Dispatchers.Main) {
            modelUi = ModelUiState(
                phase = if (state.active) ModelPhase.DOWNLOADING else if (state.modelPath != null) ModelPhase.LOADING else ModelPhase.ERROR,
                progress = state.progress,
                message = state.message
            )
        }
    }

    private fun loadModel(path: String) {
        modelUi = ModelUiState(ModelPhase.LOADING, 100, "Cargando IA en GPU…")
        viewModelScope.launch {
            val result = tutor.load(path)
            modelUi = if (result.isSuccess) {
                ModelUiState(ModelPhase.READY, 100, "IA local lista · sin internet")
            } else {
                ModelUiState(ModelPhase.ERROR, message = "No se pudo cargar: ${result.exceptionOrNull()?.message}")
            }
        }
    }

    fun clearConversation() {
        tutor.resetConversation()
        messages.clear()
        messages += ChatMessage(user = false, text = "Conversación reiniciada. ¿Qué tema quieres dominar ahora?")
    }

    fun sendQuestion(forced: String? = null) {
        val question = (forced ?: chatInput).trim()
        if (question.isEmpty() || messages.any { it.id == STREAMING_ID }) return
        chatInput = ""
        messages += ChatMessage(user = true, text = question)

        if (!modelReady) {
            messages += ChatMessage(user = false, text = localCoach(question))
            return
        }

        messages += ChatMessage(id = STREAMING_ID, user = false, text = "Pensando…")
        val context = CourseContent.knowledgeFor(question)
        val prompt = """
            PREGUNTA DEL ESTUDIANTE:
            $question

            CONTEXTO CURRICULAR LOCAL RELEVANTE:
            $context

            Enseña el tema, revisa cualquier código incluido y termina con un reto breve.
        """.trimIndent()

        viewModelScope.launch {
            val result = tutor.answer(prompt) { partial ->
                viewModelScope.launch(Dispatchers.Main) { replaceStreaming(partial) }
            }
            if (result.isFailure) replaceStreaming("Ocurrió un error en el motor local: ${result.exceptionOrNull()?.message}")
            else if (result.getOrNull().isNullOrBlank()) replaceStreaming("El modelo no devolvió texto. Reinicia la conversación e inténtalo de nuevo.")
        }
    }

    private fun replaceStreaming(text: String) {
        val index = messages.indexOfFirst { it.id == STREAMING_ID }
        if (index >= 0) messages[index] = ChatMessage(id = STREAMING_ID, user = false, text = text)
    }

    fun runPython() {
        if (busyLab) return
        busyLab = true
        execution = null
        codeAnalysis = ""
        viewModelScope.launch {
            execution = pythonRunner.execute(code)
            busyLab = false
        }
    }

    fun analyzeCode() {
        if (busyLab || code.isBlank()) return
        busyLab = true
        codeAnalysis = "Analizando estructura, errores y mejoras…"
        viewModelScope.launch {
            if (selectedLanguage == "Python" && execution == null) execution = pythonRunner.execute(code)
            if (!modelReady) {
                codeAnalysis = localCodeReview(code, selectedLanguage, execution)
                busyLab = false
                return@launch
            }

            val resultText = execution?.let {
                "Ejecución: éxito=${it.success}; salida=${it.output}; error=${it.error}; línea=${it.line}"
            } ?: "No se ejecutó; realiza análisis estático."
            val prompt = """
                Actúa como revisor senior y profesor. Analiza este código $selectedLanguage.
                Explica: objetivo, flujo, errores, riesgos, complejidad, buenas prácticas y una versión mejorada completa.
                Resultado disponible: $resultText

                CÓDIGO:
                ```$selectedLanguage
                $code
                ```
            """.trimIndent()
            val answer = tutor.answer(prompt) { partial ->
                viewModelScope.launch(Dispatchers.Main) { codeAnalysis = partial }
            }
            if (answer.isFailure) codeAnalysis = localCodeReview(code, selectedLanguage, execution)
            busyLab = false
        }
    }

    private fun localCoach(question: String): String {
        val context = CourseContent.knowledgeFor(question)
        return """
            El curso está disponible, pero para una explicación generada especialmente para tu pregunta debes instalar el modelo local de 347 MB.

            Ruta recomendada:
            $context

            Método de estudio: comprende el concepto, escribe un ejemplo pequeño, predice el resultado, ejecútalo en Laboratorio y luego cambia una condición para comprobar que realmente lo entendiste.
        """.trimIndent()
    }

    private fun localCodeReview(source: String, language: String, run: ExecutionResult?): String {
        val findings = mutableListOf<String>()
        if (source.lines().size > 80) findings += "Divide el código en funciones o clases más pequeñas."
        if (source.contains("TODO", ignoreCase = true)) findings += "Hay tareas TODO pendientes."
        if (source.contains("password", ignoreCase = true) || source.contains("api_key", ignoreCase = true)) findings += "Revisa que no existan secretos escritos directamente en el código."
        if (language == "Python" && source.contains("except:")) findings += "Evita `except:` general; captura excepciones concretas."
        if (language == "Kotlin" && source.contains("!!")) findings += "El operador `!!` puede provocar NullPointerException; usa manejo seguro de nulos."
        if (findings.isEmpty()) findings += "No se detectaron problemas básicos mediante las reglas locales."

        return buildString {
            appendLine("REVISIÓN LOCAL DE $language")
            run?.let {
                appendLine(if (it.success) "✓ El código Python terminó correctamente." else "✗ Error en línea ${it.line}: ${it.error}")
                if (it.output.isNotBlank()) appendLine("Salida: ${it.output.trim()}")
            }
            appendLine()
            findings.forEach { appendLine("• $it") }
            appendLine()
            append("Instala el modelo local para recibir explicación línea por línea y una refactorización completa.")
        }
    }

    override fun onCleared() {
        tutor.close()
        super.onCleared()
    }

    companion object {
        private const val STREAMING_ID = "streaming"
        private const val DEFAULT_CODE = """def fibonacci(n):
    a, b = 0, 1
    resultado = []
    for _ in range(n):
        resultado.append(a)
        a, b = b, a + b
    return resultado

print(fibonacci(10))"""
    }
}
