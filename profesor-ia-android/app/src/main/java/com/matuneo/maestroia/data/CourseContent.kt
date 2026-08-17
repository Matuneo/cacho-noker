package com.matuneo.maestroia.data

data class StudyModule(
    val id: String,
    val track: String,
    val title: String,
    val level: String,
    val description: String,
    val lessons: List<String>
)

data class GuidedProject(
    val title: String,
    val level: String,
    val technologies: String,
    val outcome: String,
    val milestones: List<String>
)

object CourseContent {
    val python = listOf(
        StudyModule("py01", "Python", "Fundamentos sin lagunas", "Inicial", "Aprende a pensar como programador antes de memorizar sintaxis.", listOf("Variables y tipos", "Entrada y salida", "Operadores", "Condiciones", "Bucles", "Funciones", "Errores frecuentes")),
        StudyModule("py02", "Python", "Estructuras de datos", "Inicial", "Modela información real con colecciones y comprensiones.", listOf("Listas", "Tuplas", "Diccionarios", "Conjuntos", "Comprensiones", "Ordenamiento", "Complejidad básica")),
        StudyModule("py03", "Python", "Programación profesional", "Intermedio", "Escribe código legible, modular, probado y mantenible.", listOf("Módulos y paquetes", "Entornos virtuales", "Type hints", "Dataclasses", "Excepciones", "Logging", "PEP 8")),
        StudyModule("py04", "Python", "POO y patrones", "Intermedio", "Domina objetos, composición y patrones útiles sin sobreingeniería.", listOf("Clases", "Encapsulación", "Herencia", "Composición", "Protocolos", "SOLID", "Factory y Strategy")),
        StudyModule("py05", "Python", "Algoritmos y datos", "Avanzado", "Resuelve problemas difíciles con razonamiento paso a paso.", listOf("Big O", "Recursión", "Búsqueda", "Grafos", "Árboles", "Programación dinámica", "Optimización")),
        StudyModule("py06", "Python", "APIs, automatización y datos", "Avanzado", "Construye herramientas conectadas a servicios y bases de datos.", listOf("HTTP y JSON", "REST", "SQLite", "Asyncio", "Web scraping ético", "Pandas", "Automatización")),
        StudyModule("py07", "Python", "Pruebas y arquitectura", "Avanzado", "Convierte scripts en productos confiables.", listOf("Pytest", "Mocks", "Clean Architecture", "Repositorios", "Inyección de dependencias", "Profiling", "CI/CD")),
        StudyModule("py08", "Python", "IA aplicada", "Experto", "Comprende cómo se preparan, evalúan e integran modelos.", listOf("Numpy", "Datos y features", "ML clásico", "Redes neuronales", "Embeddings", "RAG", "Evaluación y seguridad"))
    )

    val android = listOf(
        StudyModule("an01", "Android", "Kotlin esencial", "Inicial", "La base moderna para programar Android con seguridad de tipos.", listOf("Sintaxis Kotlin", "Null safety", "Funciones", "Colecciones", "Clases", "Lambdas", "Coroutines")),
        StudyModule("an02", "Android", "Android Studio desde cero", "Inicial", "Entiende proyectos, Gradle, emuladores, Logcat y depuración.", listOf("Estructura del proyecto", "SDK y emulador", "Gradle", "Manifest", "Recursos", "Logcat", "Depurador")),
        StudyModule("an03", "Android", "Interfaces con Jetpack Compose", "Intermedio", "Crea interfaces adaptables, rápidas y accesibles.", listOf("Composables", "Estado", "Layouts", "Material 3", "Listas", "Navegación", "Accesibilidad")),
        StudyModule("an04", "Android", "Datos y persistencia", "Intermedio", "Conecta la interfaz con datos locales y remotos.", listOf("ViewModel", "StateFlow", "Room", "DataStore", "Retrofit", "Serialización", "Cache offline")),
        StudyModule("an05", "Android", "Arquitectura de aplicaciones", "Avanzado", "Diseña apps escalables, testeables y fáciles de mantener.", listOf("MVVM", "Clean Architecture", "Use cases", "Repositorios", "DI con Hilt", "Modularización", "Manejo de estado")),
        StudyModule("an06", "Android", "Multimedia, sensores e IA", "Avanzado", "Usa cámara, audio, ubicación y modelos en el dispositivo.", listOf("CameraX", "Audio", "Permisos", "Sensores", "WorkManager", "LiteRT", "Inferencia local")),
        StudyModule("an07", "Android", "Calidad y rendimiento", "Avanzado", "Diagnostica ANR, fugas, consumo y fallos complejos.", listOf("Unit tests", "UI tests", "Profiler", "Memoria", "Rendimiento Compose", "Seguridad", "Observabilidad")),
        StudyModule("an08", "Android", "Publicación profesional", "Experto", "Prepara una aplicación real para usuarios y tiendas.", listOf("Firmado", "AAB y APK", "Versionado", "Play Console", "Privacidad", "CI/CD", "Monitoreo de producción"))
    )

    val allModules = python + android

    val projects = listOf(
        GuidedProject("Gestor inteligente de gastos", "Inicial", "Python · SQLite", "Aplicación de consola con informes y persistencia.", listOf("Modelo de datos", "CRUD", "Validación", "Informes", "Pruebas")),
        GuidedProject("API para una tienda", "Intermedio", "Python · FastAPI · SQL", "Backend documentado con autenticación y pruebas.", listOf("Diseño REST", "Base de datos", "Endpoints", "Seguridad", "Docker")),
        GuidedProject("App de hábitos offline", "Intermedio", "Kotlin · Compose · Room", "Aplicación Android completa que funciona sin conexión.", listOf("UX", "Arquitectura", "Persistencia", "Notificaciones", "Pruebas")),
        GuidedProject("Cliente Android de API", "Avanzado", "Compose · Retrofit · Clean", "App resiliente con caché, estados y paginación.", listOf("Contrato API", "Capas", "Cache", "Errores", "Rendimiento")),
        GuidedProject("Tutor con IA local", "Avanzado", "LiteRT-LM · RAG · Android", "Chat privado con conocimiento especializado en el teléfono.", listOf("Modelo", "Prompts", "Recuperación", "Streaming", "Evaluación")),
        GuidedProject("Aplicación Android de producción", "Experto", "Multi-módulo · CI/CD", "Producto firmado, probado, observable y listo para publicar.", listOf("Especificación", "Arquitectura", "Implementación", "Seguridad", "Release"))
    )

    fun knowledgeFor(question: String): String {
        val words = question.lowercase().split(Regex("\\W+")).filter { it.length > 3 }.toSet()
        val ranked = allModules.map { module ->
            val text = (module.title + " " + module.description + " " + module.lessons.joinToString(" ")).lowercase()
            module to words.count { text.contains(it) }
        }.sortedByDescending { it.second }.take(3).map { it.first }

        return ranked.joinToString("\n") { module ->
            "${module.track} — ${module.title}: ${module.description} Temas: ${module.lessons.joinToString(", ")}."
        }
    }
}

