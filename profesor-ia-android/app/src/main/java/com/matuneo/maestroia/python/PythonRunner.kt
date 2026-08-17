package com.matuneo.maestroia.python

import android.content.Context
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

data class ExecutionResult(
    val success: Boolean,
    val output: String,
    val error: String,
    val line: Int
)

class PythonRunner(private val context: Context) {
    suspend fun execute(code: String): ExecutionResult = withContext(Dispatchers.IO) {
        try {
            synchronized(Python::class.java) {
                if (!Python.isStarted()) Python.start(AndroidPlatform(context.applicationContext))
            }
            val raw = Python.getInstance()
                .getModule("safe_runner")
                .callAttr("run_code", code, 3)
                .toString()
            val json = JSONObject(raw)
            ExecutionResult(
                success = json.optBoolean("success"),
                output = json.optString("output"),
                error = json.optString("error"),
                line = json.optInt("line")
            )
        } catch (error: Throwable) {
            ExecutionResult(false, "", "No se pudo iniciar Python: ${error.message}", 0)
        }
    }
}
