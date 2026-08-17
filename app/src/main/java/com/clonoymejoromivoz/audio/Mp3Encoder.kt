package com.clonoymejoromivoz.audio

import java.io.File
import java.io.FileOutputStream
import kotlin.math.roundToInt

object Mp3Encoder {
    fun encode(samples: FloatArray, sampleRate: Int, outputFile: File, bitrateKbps: Int = 192) {
        val handle = Mp3Native.create(sampleRate, bitrateKbps, 2)
        require(handle != 0L) { "No se pudo abrir el codificador MP3" }

        val chunkSize = 8192
        val pcm = ShortArray(chunkSize)
        val mp3 = ByteArray((chunkSize * 5 / 4) + 7200)

        try {
            FileOutputStream(outputFile).use { stream ->
                var offset = 0
                while (offset < samples.size) {
                    val count = minOf(chunkSize, samples.size - offset)
                    for (i in 0 until count) {
                        pcm[i] = (samples[offset + i].coerceIn(-1f, 1f) * 32767f)
                            .roundToInt()
                            .toShort()
                    }
                    val encoded = Mp3Native.encode(handle, pcm, count, mp3)
                    check(encoded >= 0) { "LAME devolvió el error $encoded" }
                    if (encoded > 0) stream.write(mp3, 0, encoded)
                    offset += count
                }
                val remaining = Mp3Native.flush(handle, mp3)
                check(remaining >= 0) { "No se pudo finalizar el archivo MP3" }
                if (remaining > 0) stream.write(mp3, 0, remaining)
            }
        } finally {
            Mp3Native.close(handle)
        }
    }
}
