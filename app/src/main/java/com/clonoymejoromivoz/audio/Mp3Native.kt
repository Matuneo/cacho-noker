package com.clonoymejoromivoz.audio

internal object Mp3Native {
    init {
        System.loadLibrary("mp3bridge")
    }

    external fun create(sampleRate: Int, bitrateKbps: Int, quality: Int): Long
    external fun encode(handle: Long, pcm: ShortArray, length: Int, output: ByteArray): Int
    external fun flush(handle: Long, output: ByteArray): Int
    external fun close(handle: Long)
}
