# PROFESOR IA: PYTHON Y ANDROID

Aplicación Android nativa para aprender programación desde cero hasta nivel avanzado con práctica real y un tutor de IA ejecutándose dentro del teléfono.

## Funciones de la versión 1.0

- 16 módulos y 112 lecciones de Python, Kotlin, Android Studio, Compose, arquitectura, pruebas e IA.
- Tutor conversacional local con LiteRT-LM y aceleración GPU.
- Descarga desde la app del modelo recomendado Qwen3 0.6B INT4 (~347 MB, Apache-2.0).
- Importación de otros modelos `.litertlm` compatibles.
- Laboratorio Python real mediante Chaquopy, con salida, errores y línea exacta.
- Ejecución limitada a 3 segundos y conjunto seguro de funciones; sin importaciones ni acceso a archivos.
- Analizador de Python, Kotlin, Java, Gradle y XML.
- Seis proyectos guiados de portafolio.
- Progreso guardado localmente.

## Requisitos

- Android 9 o posterior.
- Teléfono arm64-v8a reciente; se recomienda gama alta y 6 GB de RAM o más.
- Internet solamente para descargar el modelo la primera vez. Después, el tutor puede funcionar sin conexión.

## Tecnología

- Kotlin + Jetpack Compose + Material 3.
- LiteRT-LM 0.16.0 para inferencia local.
- Chaquopy 17.0 para el laboratorio de Python.
- Arquitectura offline-first sin claves API.

El modelo no viene dentro del APK para mantener una instalación manejable y permitir reemplazarlo por modelos mejores sin reinstalar la aplicación.
