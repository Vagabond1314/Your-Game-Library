#include <jni.h>
#include <string>

extern "C" JNIEXPORT jstring JNICALL
Java_com_your_1game_1library_gamestracker_Config_getIgdbClientId(JNIEnv* env, jobject /* this */) {
    // Твій Client ID
    return env->NewStringUTF("rqq19ustfra7exmdrlgh8xierya2e1");
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_your_1game_1library_gamestracker_Config_getIgdbClientSecret(JNIEnv* env, jobject /* this */) {
    // Твій Client Secret
    return env->NewStringUTF("j1cpbiepdc7omjxzmafmqxiyk9je5o");
}
extern "C" JNIEXPORT jstring JNICALL
Java_com_your_1game_1library_gamestracker_Config_getSteamAPIKEY(JNIEnv* env, jobject /* this */) {
    // Твій Client Secret
    return env->NewStringUTF("96CC52FC4AE0F3235832EC8FC4CBAE28");
}