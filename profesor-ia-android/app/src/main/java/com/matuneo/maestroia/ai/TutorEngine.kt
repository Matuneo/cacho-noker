package com.matuneo.maestroia.ai

import android.content.Context
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.Conversation
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.SamplerConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.withContext

class TutorEngine(private val context: Context) {
    private var engine: Engine? = null
    private var conversation: Conversation? = null

    val isReady: Boolean get() = conversation != null

    suspend fun load(modelPath: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            close()
            val created = try {
                Engine(
                    EngineConfig(
                        modelPath = modelPath,
                        backend = Backend.GPU(),
                        cacheDir = context.cacheDir.absolutePath
                    )
                ).also { it.initialize() }
            } catch (_: Throwable) {
                Engine(
                    EngineConfig(
                        modelPath = modelPath,
                        backend = Backend.CPU(),
                        cacheDir = context.cacheDir.absolutePath
                    )
                ).also { it.initialize() }
            }
            engine = created
            conversation = created.createConversation(
                ConversationConfig(
                    systemInstruction = Contents.of(SYSTEM_PROMPT),
                    samplerConfig = SamplerConfig(topK = 32, topP = 0.9, temperature = 0.35)
                )
            )
        }
    }

    suspend fun answer(prompt: String, onUpdate: (String) -> Unit): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val active = requireNotNull(conversation) { "Primero instala y carga el modelo local" }
            val response = StringBuilder()
            active.sendMessageAsync(prompt).collect { message ->
                response.append(message.toString())
                onUpdate(response.toString())
            }
            response.toString().trim()
        }
    }

    fun resetConversation() {
        val activeEngine = engine ?: return
        conversation?.close()
        conversation = activeEngine.createConversation(
            ConversationConfig(
                systemInstruction = Contents.of(SYSTEM_PROMPT),
                samplerConfig = SamplerConfig(topK = 32, topP = 0.9, temperature = 0.35)
            )
        )
    }

    fun close() {
        conversation?.close()
        conversation = null
        engine?.close()
        engine = null
    }

    companion object {
        private val SYSTEM_PROMPT = """
            Eres PROFESOR IA, un mentor experto en Python, Kotlin, Android Studio, Jetpack Compose,
            arquitectura de software, algoritmos, bases de datos, pruebas, depuración e IA aplicada.
            Respondes siempre en español claro, salvo que el estudiante pida otro idioma.

            Método obligatorio:
            1. Detecta el nivel del estudiante sin humillarlo.
            2. Explica primero la idea con lenguaje sencillo y luego la precisión técnica.
            3. Cuando haya código, analiza objetivo, flujo, estado, errores, complejidad y seguridad.
            4. Ofrece una versión corregida completa y explica cada cambio importante.
            5. Usa ejemplos ejecutables y plantea un pequeño reto para comprobar comprensión.
            6. Si faltan datos, declara las suposiciones. No inventes APIs ni resultados de ejecución.
            7. Prioriza prácticas actuales: Kotlin, Compose, coroutines, arquitectura por capas y pruebas.
            8. Ayuda a comprender código propio u open source; no ayudes a vulnerar, robar credenciales,
               evadir licencias ni desactivar protecciones de aplicaciones ajenas.

            Formato recomendado: Diagnóstico, Explicación, Solución, Prueba y Siguiente reto.
        """.trimIndent()
    }
}
