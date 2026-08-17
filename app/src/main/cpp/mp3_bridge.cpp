#include <jni.h>
#include <algorithm>
#include <cstdint>
#include "lame.h"

extern "C" JNIEXPORT jlong JNICALL
Java_com_clonoymejoromivoz_audio_Mp3Native_create(
        JNIEnv *, jobject, jint sampleRate, jint bitrateKbps, jint quality) {
    lame_t lame = lame_init();
    if (lame == nullptr) return 0;
    lame_set_in_samplerate(lame, sampleRate);
    lame_set_out_samplerate(lame, sampleRate);
    lame_set_num_channels(lame, 1);
    lame_set_mode(lame, MONO);
    lame_set_brate(lame, bitrateKbps);
    lame_set_quality(lame, std::clamp(static_cast<int>(quality), 0, 9));
    if (lame_init_params(lame) < 0) {
        lame_close(lame);
        return 0;
    }
    return reinterpret_cast<jlong>(lame);
}

extern "C" JNIEXPORT jint JNICALL
Java_com_clonoymejoromivoz_audio_Mp3Native_encode(
        JNIEnv *env, jobject, jlong handle, jshortArray pcmArray, jint length,
        jbyteArray outputArray) {
    auto lame = reinterpret_cast<lame_t>(handle);
    if (lame == nullptr) return -1;
    const jsize pcmSize = env->GetArrayLength(pcmArray);
    const jsize outputSize = env->GetArrayLength(outputArray);
    length = std::min(length, pcmSize);
    jshort *pcm = env->GetShortArrayElements(pcmArray, nullptr);
    jbyte *output = env->GetByteArrayElements(outputArray, nullptr);
    const int encoded = lame_encode_buffer(
        lame,
        reinterpret_cast<short *>(pcm),
        reinterpret_cast<short *>(pcm),
        length,
        reinterpret_cast<unsigned char *>(output),
        outputSize
    );
    env->ReleaseShortArrayElements(pcmArray, pcm, JNI_ABORT);
    env->ReleaseByteArrayElements(outputArray, output, 0);
    return encoded;
}

extern "C" JNIEXPORT jint JNICALL
Java_com_clonoymejoromivoz_audio_Mp3Native_flush(
        JNIEnv *env, jobject, jlong handle, jbyteArray outputArray) {
    auto lame = reinterpret_cast<lame_t>(handle);
    if (lame == nullptr) return -1;
    const jsize outputSize = env->GetArrayLength(outputArray);
    jbyte *output = env->GetByteArrayElements(outputArray, nullptr);
    const int encoded = lame_encode_flush(
        lame,
        reinterpret_cast<unsigned char *>(output),
        outputSize
    );
    env->ReleaseByteArrayElements(outputArray, output, 0);
    return encoded;
}

extern "C" JNIEXPORT void JNICALL
Java_com_clonoymejoromivoz_audio_Mp3Native_close(
        JNIEnv *, jobject, jlong handle) {
    auto lame = reinterpret_cast<lame_t>(handle);
    if (lame != nullptr) lame_close(lame);
}
