package com.clonoymejoromivoz.audio

import java.io.ByteArrayOutputStream
import java.io.EOFException
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import kotlin.math.max

data class PcmAudio(val samples: FloatArray, val sampleRate: Int)

object WavPcmIO {
    fun writeMono16(file: File, samples: FloatArray, sampleRate: Int) {
        file.parentFile?.mkdirs()
        FileOutputStream(file).use { output ->
            val dataSize = samples.size * 2
            output.write("RIFF".toByteArray())
            writeIntLE(output, 36 + dataSize)
            output.write("WAVE".toByteArray())
            output.write("fmt ".toByteArray())
            writeIntLE(output, 16)
            writeShortLE(output, 1)
            writeShortLE(output, 1)
            writeIntLE(output, sampleRate)
            writeIntLE(output, sampleRate * 2)
            writeShortLE(output, 2)
            writeShortLE(output, 16)
            output.write("data".toByteArray())
            writeIntLE(output, dataSize)
            samples.forEach { sample ->
                val value = (sample.coerceIn(-1f, 1f) * 32767f).toInt()
                writeShortLE(output, value)
            }
        }
    }

    fun read(input: InputStream): PcmAudio {
        val riff = readAscii(input, 4)
        require(riff == "RIFF") { "El archivo no es WAV RIFF" }
        readIntLE(input)
        require(readAscii(input, 4) == "WAVE") { "Formato WAV no válido" }

        var channels = 0
        var sampleRate = 0
        var bits = 0
        var format = 0
        var pcmBytes: ByteArray? = null

        while (pcmBytes == null) {
            val chunkId = try {
                readAscii(input, 4)
            } catch (_: EOFException) {
                break
            }
            val chunkSize = readIntLE(input)
            require(chunkSize >= 0) { "Bloque WAV inválido" }
            when (chunkId) {
                "fmt " -> {
                    format = readShortLE(input)
                    channels = readShortLE(input)
                    sampleRate = readIntLE(input)
                    readIntLE(input)
                    readShortLE(input)
                    bits = readShortLE(input)
                    skipFully(input, chunkSize - 16)
                }
                "data" -> pcmBytes = readExactly(input, chunkSize)
                else -> skipFully(input, chunkSize)
            }
            if (chunkSize % 2 == 1) skipFully(input, 1)
        }

        require(format == 1) { "Solo se admite WAV PCM sin compresión" }
        require(channels == 1 || channels == 2) { "El WAV debe ser mono o estéreo" }
        require(bits == 16) { "El WAV debe usar PCM de 16 bits" }
        require(sampleRate in 8_000..96_000) { "Frecuencia WAV no compatible" }
        val bytes = requireNotNull(pcmBytes) { "El WAV no contiene audio" }
        val frames = bytes.size / (2 * channels)
        require(frames > 0) { "El WAV está vacío" }

        val samples = FloatArray(frames)
        var cursor = 0
        for (frame in 0 until frames) {
            var total = 0
            repeat(channels) {
                val lo = bytes[cursor].toInt() and 0xff
                val hi = bytes[cursor + 1].toInt()
                total += ((hi shl 8) or lo).toShort().toInt()
                cursor += 2
            }
            samples[frame] = (total.toFloat() / channels / 32768f).coerceIn(-1f, 1f)
        }
        return PcmAudio(samples, sampleRate)
    }

    private fun readExactly(input: InputStream, size: Int): ByteArray {
        val out = ByteArrayOutputStream(max(size, 32))
        val buffer = ByteArray(8192)
        var remaining = size
        while (remaining > 0) {
            val read = input.read(buffer, 0, minOf(buffer.size, remaining))
            if (read < 0) throw EOFException("WAV incompleto")
            out.write(buffer, 0, read)
            remaining -= read
        }
        return out.toByteArray()
    }

    private fun readAscii(input: InputStream, size: Int) = String(readExactly(input, size), Charsets.US_ASCII)

    private fun readShortLE(input: InputStream): Int {
        val b0 = input.read()
        val b1 = input.read()
        if (b1 < 0) throw EOFException("WAV incompleto")
        return b0 or (b1 shl 8)
    }

    private fun readIntLE(input: InputStream): Int {
        val b0 = input.read()
        val b1 = input.read()
        val b2 = input.read()
        val b3 = input.read()
        if (b3 < 0) throw EOFException("WAV incompleto")
        return b0 or (b1 shl 8) or (b2 shl 16) or (b3 shl 24)
    }

    private fun skipFully(input: InputStream, count: Int) {
        var remaining = count
        while (remaining > 0) {
            val skipped = input.skip(remaining.toLong()).toInt()
            if (skipped <= 0) {
                if (input.read() < 0) throw EOFException("WAV incompleto")
                remaining--
            } else {
                remaining -= skipped
            }
        }
    }

    private fun writeShortLE(output: FileOutputStream, value: Int) {
        output.write(value and 0xff)
        output.write((value ushr 8) and 0xff)
    }

    private fun writeIntLE(output: FileOutputStream, value: Int) {
        output.write(value and 0xff)
        output.write((value ushr 8) and 0xff)
        output.write((value ushr 16) and 0xff)
        output.write((value ushr 24) and 0xff)
    }
}
