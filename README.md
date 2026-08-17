# CLONO Y MEJORO MI VOZ

Aplicación Android `arm64-v8a` para generar voz totalmente en el teléfono. No declara permiso de Internet: la grabación, la clonación, la narración y la conversión de archivos permanecen locales.

## Funciones incluidas

- Grabación de una muestra de voz o importación de un WAV PCM de 16 bits.
- Clonación local con PocketTTS mediante sherpa-onnx.
- Voz de narrador local en español con Supertonic 3.
- Texto a voz, reproducción, guardado y uso compartido.
- Salida WAV de máxima fidelidad.
- Salida MP3 mono a 192 kbps codificada con LAME dentro del teléfono.
- Perfil de voz persistente en el almacenamiento privado de la aplicación.

## Compatibilidad y alcance

- Android 8.0 o posterior.
- Solo teléfonos modernos de 64 bits (`arm64-v8a`).
- Se recomienda un equipo de gama alta y al menos 1 GB libre para instalar y procesar los modelos.
- La voz de narrador admite español. El modelo PocketTTS convertido oficialmente por sherpa-onnx incluido en esta primera versión está optimizado para inglés; la identidad vocal se clona localmente, pero el español clonado puede pronunciar peor que el modo narrador.

## Compilar con GitHub Actions

El flujo `.github/workflows/build-apk.yml` descarga fuentes y modelos oficiales durante la compilación, crea un APK de depuración firmado por Android y lo publica como artefacto `CLONO-Y-MEJORO-MI-VOZ-APK`.

Para compilar manualmente en Linux:

1. Instala Java 17, Android SDK 35, NDK `27.2.12479018`, CMake 3.22.1 y Gradle 8.9.
2. Ejecuta `bash scripts/prepare_dependencies.sh`.
3. Ejecuta `gradle :app:assembleDebug`.

## Privacidad y uso responsable

La aplicación no necesita una cuenta ni una API. Usa únicamente tu propia voz o una voz para la que tengas autorización expresa. No la utilices para suplantación, fraude o contenido engañoso.

## Componentes

- sherpa-onnx: Apache-2.0.
- PocketTTS: motor y modelo conforme a sus licencias oficiales.
- Supertonic: motor y modelo conforme a sus licencias oficiales.
- LAME/TAndroidLame: GPL-3.0-or-later. Por esta dependencia, el código de esta aplicación se distribuye bajo GPL-3.0-or-later.
