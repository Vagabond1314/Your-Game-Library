#include <jni.h>
#include <string>

extern "C" JNIEXPORT jstring JNICALL
Java_com_your_1game_1library_Config_getIgdbClientId(JNIEnv* env, jobject /* this */) {
    std::string client_id = "rqq19ustfra7exmdrlgh8xierya2e1";
    return env->NewStringUTF(client_id.c_str());
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_your_1game_1library_Config_getIgdbClientSecret(JNIEnv* env, jobject /* this */) {
    std::string client_secret = "j1cpbiepdc7omjxzmafmqxiyk9je5o";
    return env->NewStringUTF(client_secret.c_str());
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_your_1game_1library_Config_getSteamAPIKEY(JNIEnv* env, jobject /* this */) {
    std::string steam_key = "96CC52FC4AE0F3235832EC8FC4CBAE28";
    return env->NewStringUTF(steam_key.c_str());
}