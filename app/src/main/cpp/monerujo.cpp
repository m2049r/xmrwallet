/**
 * Copyright (c) 2017 m2049r
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include <inttypes.h>
#include "monerujo.h"
#include "wallet2_api.h"

//TODO explicit casting jlong, jint, jboolean to avoid warnings

#ifdef __cplusplus
extern "C"
{
#endif

#include <android/log.h>
#define LOG_TAG "WalletNDK"
#define LOGV(...) __android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG,__VA_ARGS__)
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG  , LOG_TAG,__VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO   , LOG_TAG,__VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN   , LOG_TAG,__VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR  , LOG_TAG,__VA_ARGS__)

static JavaVM *cachedJVM;
static jclass class_ArrayList;
static jclass class_WalletListener;
static jclass class_TransactionInfo;
static jclass class_Transfer;

std::mutex _listenerMutex;

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *jvm, void *reserved) {
    cachedJVM = jvm;
    LOGI("JNI_OnLoad");
    JNIEnv *jenv;
    if (jvm->GetEnv(reinterpret_cast<void **>(&jenv), JNI_VERSION_1_6) != JNI_OK) {
        return -1;
    }
    //LOGI("JNI_OnLoad ok");

    class_ArrayList = static_cast<jclass>(jenv->NewGlobalRef(
            jenv->FindClass("java/util/ArrayList")));
    class_TransactionInfo = static_cast<jclass>(jenv->NewGlobalRef(
            jenv->FindClass("com/m2049r/xmrwallet/model/TransactionInfo")));
    class_Transfer = static_cast<jclass>(jenv->NewGlobalRef(
            jenv->FindClass("com/m2049r/xmrwallet/model/Transfer")));
    class_WalletListener = static_cast<jclass>(jenv->NewGlobalRef(
            jenv->FindClass("com/m2049r/xmrwallet/model/WalletListener")));
    return JNI_VERSION_1_6;
}
#ifdef __cplusplus
}
#endif

int attachJVM(JNIEnv **jenv) {
    int envStat = cachedJVM->GetEnv((void **) jenv, JNI_VERSION_1_6);
    if (envStat == JNI_EDETACHED) {
        if (cachedJVM->AttachCurrentThread(jenv, nullptr) != 0) {
            LOGE("Failed to attach");
            return JNI_ERR;
        }
    } else if (envStat == JNI_EVERSION) {
        LOGE("GetEnv: version not supported");
        return JNI_ERR;
    }
    //LOGI("envStat=%i", envStat);
    return envStat;
}

void detachJVM(JNIEnv *jenv, int envStat) {
    //LOGI("envStat=%i", envStat);
    if (jenv->ExceptionCheck()) {
        jenv->ExceptionDescribe();
    }

    if (envStat == JNI_EDETACHED) {
        cachedJVM->DetachCurrentThread();
    }
}

struct MyWalletListener : Bitmonero::WalletListener {
    jobject jlistener;

    MyWalletListener(JNIEnv *env, jobject aListener) {
        LOGD("Created MyListener");
        jlistener = env->NewGlobalRef(aListener);;
    }

    ~MyWalletListener() {
        LOGD("Destroyed MyListener");
    };

    void deleteGlobalJavaRef(JNIEnv *env) {
        std::lock_guard<std::mutex> lock(_listenerMutex);
        env->DeleteGlobalRef(jlistener);
        jlistener = nullptr;
    }

    /**
 * @brief updated  - generic callback, called when any event (sent/received/block reveived/etc) happened with the wallet;
 */
    void updated() {
        std::lock_guard<std::mutex> lock(_listenerMutex);
        if (jlistener == nullptr) return;
        LOGD("updated");
        JNIEnv *jenv;
        int envStat = attachJVM(&jenv);
        if (envStat == JNI_ERR) return;

        jmethodID listenerClass_updated = jenv->GetMethodID(class_WalletListener, "updated", "()V");
        jenv->CallVoidMethod(jlistener, listenerClass_updated);

        detachJVM(jenv, envStat);
    }


    /**
     * @brief moneySpent - called when money spent
     * @param txId       - transaction id
     * @param amount     - amount
     */
    void moneySpent(const std::string &txId, uint64_t amount) {
        std::lock_guard<std::mutex> lock(_listenerMutex);
        if (jlistener == nullptr) return;
        LOGD("moneySpent %"
                     PRIu64, amount);
    }

    /**
     * @brief moneyReceived - called when money received
     * @param txId          - transaction id
     * @param amount        - amount
     */
    void moneyReceived(const std::string &txId, uint64_t amount) {
        std::lock_guard<std::mutex> lock(_listenerMutex);
        if (jlistener == nullptr) return;
        LOGD("moneyReceived %"
                     PRIu64, amount);
    }

    /**
     * @brief unconfirmedMoneyReceived - called when payment arrived in tx pool
     * @param txId          - transaction id
     * @param amount        - amount
     */
    void unconfirmedMoneyReceived(const std::string &txId, uint64_t amount) {
        std::lock_guard<std::mutex> lock(_listenerMutex);
        if (jlistener == nullptr) return;
        LOGD("unconfirmedMoneyReceived %"
                     PRIu64, amount);
    }

    /**
     * @brief newBlock      - called when new block received
     * @param height        - block height
     */
    void newBlock(uint64_t height) {
        std::lock_guard<std::mutex> lock(_listenerMutex);
        if (jlistener == nullptr) return;
        //LOGD("newBlock");
        JNIEnv *jenv;
        int envStat = attachJVM(&jenv);
        if (envStat == JNI_ERR) return;

        jlong h = static_cast<jlong>(height);
        jmethodID listenerClass_newBlock = jenv->GetMethodID(class_WalletListener, "newBlock",
                                                             "(J)V");
        jenv->CallVoidMethod(jlistener, listenerClass_newBlock, h);

        detachJVM(jenv, envStat);
    }

/**
 * @brief refreshed - called when wallet refreshed by background thread or explicitly refreshed by calling "refresh" synchronously
 */
    void refreshed() {
        std::lock_guard<std::mutex> lock(_listenerMutex);
        if (jlistener == nullptr) return;
        LOGD("refreshed");
        JNIEnv *jenv;

        int envStat = attachJVM(&jenv);
        if (envStat == JNI_ERR) return;

        jmethodID listenerClass_refreshed = jenv->GetMethodID(class_WalletListener, "refreshed",
                                                              "()V");
        jenv->CallVoidMethod(jlistener, listenerClass_refreshed);
        detachJVM(jenv, envStat);
    }
};


//// helper methods
std::vector<std::string> java2cpp(JNIEnv *env, jobject arrayList) {

    jmethodID java_util_ArrayList_size = env->GetMethodID(class_ArrayList, "size", "()I");
    jmethodID java_util_ArrayList_get = env->GetMethodID(class_ArrayList, "get",
                                                         "(I)Ljava/lang/Object;");

    jint len = env->CallIntMethod(arrayList, java_util_ArrayList_size);
    std::vector<std::string> result;
    result.reserve(len);
    for (jint i = 0; i < len; i++) {
        jstring element = static_cast<jstring>(env->CallObjectMethod(arrayList,
                                                                     java_util_ArrayList_get, i));
        const char *pchars = env->GetStringUTFChars(element, JNI_FALSE);
        result.emplace_back(pchars);
        env->ReleaseStringUTFChars(element, pchars);
        env->DeleteLocalRef(element);
    }
    return result;
}

jobject cpp2java(JNIEnv *env, std::vector<std::string> vector) {

    jmethodID java_util_ArrayList_ = env->GetMethodID(class_ArrayList, "<init>", "(I)V");
    jmethodID java_util_ArrayList_add = env->GetMethodID(class_ArrayList, "add",
                                                         "(Ljava/lang/Object;)Z");

    jobject result = env->NewObject(class_ArrayList, java_util_ArrayList_, vector.size());
    for (std::string &s: vector) {
        jstring element = env->NewStringUTF(s.c_str());
        env->CallBooleanMethod(result, java_util_ArrayList_add, element);
        env->DeleteLocalRef(element);
    }
    return result;
}

/// end helpers

#ifdef __cplusplus
extern "C"
{
#endif


/**********************************/
/********** WalletManager *********/
/**********************************/
JNIEXPORT jlong JNICALL
Java_com_m2049r_xmrwallet_model_WalletManager_createWalletJ(JNIEnv *env, jobject instance,
                                                            jstring path, jstring password,
                                                            jstring language,
                                                            jboolean isTestNet) {
    const char *_path = env->GetStringUTFChars(path, JNI_FALSE);
    const char *_password = env->GetStringUTFChars(password, JNI_FALSE);
    const char *_language = env->GetStringUTFChars(language, JNI_FALSE);

    Bitmonero::Wallet *wallet =
            Bitmonero::WalletManagerFactory::getWalletManager()->createWallet(
                    std::string(_path),
                    std::string(_password),
                    std::string(_language),
                    isTestNet);

    env->ReleaseStringUTFChars(path, _path);
    env->ReleaseStringUTFChars(password, _password);
    env->ReleaseStringUTFChars(language, _language);
    return reinterpret_cast<jlong>(wallet);
}

JNIEXPORT jlong JNICALL
Java_com_m2049r_xmrwallet_model_WalletManager_openWalletJ(JNIEnv *env, jobject instance,
                                                          jstring path, jstring password,
                                                          jboolean isTestNet) {
    const char *_path = env->GetStringUTFChars(path, JNI_FALSE);
    const char *_password = env->GetStringUTFChars(password, JNI_FALSE);

    Bitmonero::Wallet *wallet =
            Bitmonero::WalletManagerFactory::getWalletManager()->openWallet(
                    std::string(_path),
                    std::string(_password),
                    isTestNet);

    env->ReleaseStringUTFChars(path, _path);
    env->ReleaseStringUTFChars(password, _password);
    return reinterpret_cast<jlong>(wallet);
}

JNIEXPORT jlong JNICALL
Java_com_m2049r_xmrwallet_model_WalletManager_recoveryWalletJ(JNIEnv *env, jobject instance,
                                                              jstring path, jstring mnemonic,
                                                              jboolean isTestNet,
                                                              jlong restoreHeight) {
    const char *_path = env->GetStringUTFChars(path, JNI_FALSE);
    const char *_mnemonic = env->GetStringUTFChars(mnemonic, JNI_FALSE);

    Bitmonero::Wallet *wallet =
            Bitmonero::WalletManagerFactory::getWalletManager()->recoveryWallet(
                    std::string(_path),
                    std::string(_mnemonic),
                    isTestNet,
                    restoreHeight);

    env->ReleaseStringUTFChars(path, _path);
    env->ReleaseStringUTFChars(mnemonic, _mnemonic);
    return reinterpret_cast<jlong>(wallet);
}

JNIEXPORT jlong JNICALL
Java_com_m2049r_xmrwallet_model_WalletManager_createWalletFromKeysJ(JNIEnv *env, jobject instance,
                                                                    jstring path, jstring language,
                                                                    jboolean isTestNet,
                                                                    jlong restoreHeight,
                                                                    jstring addressString,
                                                                    jstring viewKeyString,
                                                                    jstring spendKeyString) {
    const char *_path = env->GetStringUTFChars(path, JNI_FALSE);
    const char *_language = env->GetStringUTFChars(language, JNI_FALSE);
    const char *_addressString = env->GetStringUTFChars(addressString, JNI_FALSE);
    const char *_viewKeyString = env->GetStringUTFChars(viewKeyString, JNI_FALSE);
    const char *_spendKeyString = env->GetStringUTFChars(spendKeyString, JNI_FALSE);

    Bitmonero::Wallet *wallet =
            Bitmonero::WalletManagerFactory::getWalletManager()->createWalletFromKeys(
                    std::string(_path),
                    std::string(_language),
                    isTestNet,
                    restoreHeight,
                    std::string(_addressString),
                    std::string(_viewKeyString),
                    std::string(_spendKeyString));

    env->ReleaseStringUTFChars(path, _path);
    env->ReleaseStringUTFChars(language, _language);
    env->ReleaseStringUTFChars(addressString, _addressString);
    env->ReleaseStringUTFChars(viewKeyString, _viewKeyString);
    env->ReleaseStringUTFChars(spendKeyString, _spendKeyString);
    return reinterpret_cast<jlong>(wallet);
}

JNIEXPORT jboolean JNICALL
Java_com_m2049r_xmrwallet_model_WalletManager_walletExists(JNIEnv *env, jobject instance,
                                                           jstring path) {
    const char *_path = env->GetStringUTFChars(path, JNI_FALSE);
    bool exists =
            Bitmonero::WalletManagerFactory::getWalletManager()->walletExists(std::string(_path));
    env->ReleaseStringUTFChars(path, _path);
    return exists;
}

JNIEXPORT jboolean JNICALL
Java_com_m2049r_xmrwallet_model_WalletManager_verifyWalletPassword(JNIEnv *env, jobject instance,
                                                                   jstring keys_file_name,
                                                                   jstring password,
                                                                   jboolean watch_only) {
    const char *_keys_file_name = env->GetStringUTFChars(keys_file_name, JNI_FALSE);
    const char *_password = env->GetStringUTFChars(password, JNI_FALSE);
    bool passwordOk =
            Bitmonero::WalletManagerFactory::getWalletManager()->verifyWalletPassword(
                    std::string(_keys_file_name), std::string(_password), watch_only);
    env->ReleaseStringUTFChars(keys_file_name, _keys_file_name);
    env->ReleaseStringUTFChars(password, _password);
    return passwordOk;
}


JNIEXPORT jobject JNICALL
Java_com_m2049r_xmrwallet_model_WalletManager_findWallets(JNIEnv *env, jobject instance,
                                                          jstring path) {
    const char *_path = env->GetStringUTFChars(path, JNI_FALSE);
    std::vector<std::string> walletPaths =
            Bitmonero::WalletManagerFactory::getWalletManager()->findWallets(std::string(_path));
    env->ReleaseStringUTFChars(path, _path);
    return cpp2java(env, walletPaths);
}

JNIEXPORT jstring JNICALL
Java_com_m2049r_xmrwallet_model_WalletManager_getErrorString(JNIEnv *env, jobject instance) {
    Bitmonero::Wallet *wallet = getHandle<Bitmonero::Wallet>(env, instance);
    return env->NewStringUTF(wallet->errorString().c_str());
}

//TODO virtual bool checkPayment(const std::string &address, const std::string &txid, const std::string &txkey, const std::string &daemon_address, uint64_t &received, uint64_t &height, std::string &error) const = 0;

JNIEXPORT void JNICALL
Java_com_m2049r_xmrwallet_model_WalletManager_setDaemonAddressJ(JNIEnv *env, jobject instance,
                                                                jstring address) {
    const char *_address = env->GetStringUTFChars(address, JNI_FALSE);
    Bitmonero::WalletManagerFactory::getWalletManager()->setDaemonAddress(std::string(_address));
    env->ReleaseStringUTFChars(address, _address);
}

// returns whether the daemon can be reached, and its version number
JNIEXPORT jint JNICALL
Java_com_m2049r_xmrwallet_model_WalletManager_getDaemonVersion(JNIEnv *env,
                                                               jobject instance) {
    uint32_t version;
    bool isConnected =
            Bitmonero::WalletManagerFactory::getWalletManager()->connected(&version);
    if (!isConnected) version = 0;
    return version;
}

JNIEXPORT jlong JNICALL
Java_com_m2049r_xmrwallet_model_WalletManager_getBlockchainHeight(JNIEnv *env, jobject instance) {
    return Bitmonero::WalletManagerFactory::getWalletManager()->blockchainHeight();
}

JNIEXPORT jlong JNICALL
Java_com_m2049r_xmrwallet_model_WalletManager_getBlockchainTargetHeight(JNIEnv *env,
                                                                        jobject instance) {
    return Bitmonero::WalletManagerFactory::getWalletManager()->blockchainTargetHeight();
}

JNIEXPORT jlong JNICALL
Java_com_m2049r_xmrwallet_model_WalletManager_getNetworkDifficulty(JNIEnv *env, jobject instance) {
    return Bitmonero::WalletManagerFactory::getWalletManager()->networkDifficulty();
}

JNIEXPORT jdouble JNICALL
Java_com_m2049r_xmrwallet_model_WalletManager_getMiningHashRate(JNIEnv *env, jobject instance) {
    return Bitmonero::WalletManagerFactory::getWalletManager()->miningHashRate();
}

JNIEXPORT jlong JNICALL
Java_com_m2049r_xmrwallet_model_WalletManager_getBlockTarget(JNIEnv *env, jobject instance) {
    return Bitmonero::WalletManagerFactory::getWalletManager()->blockTarget();
}

JNIEXPORT jboolean JNICALL
Java_com_m2049r_xmrwallet_model_WalletManager_isMining(JNIEnv *env, jobject instance) {
    return Bitmonero::WalletManagerFactory::getWalletManager()->isMining();
}

JNIEXPORT jboolean JNICALL
Java_com_m2049r_xmrwallet_model_WalletManager_startMining(JNIEnv *env, jobject instance,
                                                          jstring address,
                                                          jboolean background_mining,
                                                          jboolean ignore_battery) {
    const char *_address = env->GetStringUTFChars(address, JNI_FALSE);
    bool success =
            Bitmonero::WalletManagerFactory::getWalletManager()->startMining(std::string(_address),
                                                                             background_mining,
                                                                             ignore_battery);
    env->ReleaseStringUTFChars(address, _address);
    return success;
}

JNIEXPORT jboolean JNICALL
Java_com_m2049r_xmrwallet_model_WalletManager_stopMining(JNIEnv *env, jobject instance) {
    return Bitmonero::WalletManagerFactory::getWalletManager()->stopMining();
}

JNIEXPORT jstring JNICALL
Java_com_m2049r_xmrwallet_model_WalletManager_resolveOpenAlias(JNIEnv *env, jobject instance,
                                                               jstring address,
                                                               jboolean dnssec_valid) {
    const char *_address = env->GetStringUTFChars(address, JNI_FALSE);
    bool _dnssec_valid = (bool) dnssec_valid;
    std::string resolvedAlias =
            Bitmonero::WalletManagerFactory::getWalletManager()->resolveOpenAlias(
                    std::string(_address),
                    _dnssec_valid);
    env->ReleaseStringUTFChars(address, _address);
    return env->NewStringUTF(resolvedAlias.c_str());
}

//TODO static std::tuple<bool, std::string, std::string, std::string, std::string> checkUpdates(const std::string &software, const std::string &subdir);

JNIEXPORT jboolean JNICALL
Java_com_m2049r_xmrwallet_model_WalletManager_closeJ(JNIEnv *env, jobject instance,
                                                     jobject walletInstance) {
    Bitmonero::Wallet *wallet = getHandle<Bitmonero::Wallet>(env, walletInstance);
    bool closeSuccess = Bitmonero::WalletManagerFactory::getWalletManager()->closeWallet(wallet,
                                                                                         false);
    if (closeSuccess) {
        MyWalletListener *walletListener = getHandle<MyWalletListener>(env, walletInstance,
                                                                       "listenerHandle");
        if (walletListener != nullptr) {
            walletListener->deleteGlobalJavaRef(env);
            delete walletListener;
        }
    }
    LOGD("wallet closed");
    return closeSuccess;
}




/**********************************/
/************ Wallet **************/
/**********************************/

JNIEXPORT jstring JNICALL
Java_com_m2049r_xmrwallet_model_Wallet_getSeed(JNIEnv *env, jobject instance) {
    Bitmonero::Wallet *wallet = getHandle<Bitmonero::Wallet>(env, instance);
    return env->NewStringUTF(wallet->seed().c_str());
}

JNIEXPORT jstring JNICALL
Java_com_m2049r_xmrwallet_model_Wallet_getSeedLanguage(JNIEnv *env, jobject instance) {
    Bitmonero::Wallet *wallet = getHandle<Bitmonero::Wallet>(env, instance);
    return env->NewStringUTF(wallet->getSeedLanguage().c_str());
}

JNIEXPORT void JNICALL
Java_com_m2049r_xmrwallet_model_Wallet_setSeedLanguage(JNIEnv *env, jobject instance,
                                                       jstring language) {
    const char *_language = env->GetStringUTFChars(language, JNI_FALSE);
    Bitmonero::Wallet *wallet = getHandle<Bitmonero::Wallet>(env, instance);
    wallet->setSeedLanguage(std::string(_language));
    env->ReleaseStringUTFChars(language, _language);
}

JNIEXPORT jint JNICALL
Java_com_m2049r_xmrwallet_model_Wallet_getStatusJ(JNIEnv *env, jobject instance) {
    Bitmonero::Wallet *wallet = getHandle<Bitmonero::Wallet>(env, instance);
    return wallet->status();
}

JNIEXPORT jstring JNICALL
Java_com_m2049r_xmrwallet_model_Wallet_getErrorString(JNIEnv *env, jobject instance) {
    Bitmonero::Wallet *wallet = getHandle<Bitmonero::Wallet>(env, instance);
    return env->NewStringUTF(wallet->errorString().c_str());
}

JNIEXPORT jboolean JNICALL
Java_com_m2049r_xmrwallet_model_Wallet_setPassword(JNIEnv *env, jobject instance,
                                                   jstring password) {
    const char *_password = env->GetStringUTFChars(password, JNI_FALSE);
    Bitmonero::Wallet *wallet = getHandle<Bitmonero::Wallet>(env, instance);
    bool success = wallet->setPassword(std::string(_password));
    env->ReleaseStringUTFChars(password, _password);
    return success;
}

JNIEXPORT jstring JNICALL
Java_com_m2049r_xmrwallet_model_Wallet_getAddressJ(JNIEnv *env, jobject instance) {
    Bitmonero::Wallet *wallet = getHandle<Bitmonero::Wallet>(env, instance);
    return env->NewStringUTF(wallet->address().c_str());
}

JNIEXPORT jstring JNICALL
Java_com_m2049r_xmrwallet_model_Wallet_getPath(JNIEnv *env, jobject instance) {
    Bitmonero::Wallet *wallet = getHandle<Bitmonero::Wallet>(env, instance);
    return env->NewStringUTF(wallet->path().c_str());
}

JNIEXPORT jboolean JNICALL
Java_com_m2049r_xmrwallet_model_Wallet_isTestNet(JNIEnv *env, jobject instance) {
    Bitmonero::Wallet *wallet = getHandle<Bitmonero::Wallet>(env, instance);
    return wallet->testnet();
}

//TODO virtual void hardForkInfo(uint8_t &version, uint64_t &earliest_height) const = 0;
//TODO virtual bool useForkRules(uint8_t version, int64_t early_blocks) const = 0;

JNIEXPORT jstring JNICALL
Java_com_m2049r_xmrwallet_model_Wallet_getIntegratedAddress(JNIEnv *env, jobject instance,
                                                            jstring payment_id) {
    const char *_payment_id = env->GetStringUTFChars(payment_id, JNI_FALSE);
    Bitmonero::Wallet *wallet = getHandle<Bitmonero::Wallet>(env, instance);
    std::string address = wallet->integratedAddress(_payment_id);
    env->ReleaseStringUTFChars(payment_id, _payment_id);
    return env->NewStringUTF(address.c_str());
}

JNIEXPORT jstring JNICALL
Java_com_m2049r_xmrwallet_model_Wallet_getSecretViewKey(JNIEnv *env, jobject instance) {
    Bitmonero::Wallet *wallet = getHandle<Bitmonero::Wallet>(env, instance);
    return env->NewStringUTF(wallet->secretViewKey().c_str());
}

JNIEXPORT jstring JNICALL
Java_com_m2049r_xmrwallet_model_Wallet_getSecretSpendKey(JNIEnv *env, jobject instance) {
    Bitmonero::Wallet *wallet = getHandle<Bitmonero::Wallet>(env, instance);
    return env->NewStringUTF(wallet->secretSpendKey().c_str());
}

JNIEXPORT jboolean JNICALL
Java_com_m2049r_xmrwallet_model_Wallet_store(JNIEnv *env, jobject instance,
                                             jstring path) {
    const char *_path = env->GetStringUTFChars(path, JNI_FALSE);
    Bitmonero::Wallet *wallet = getHandle<Bitmonero::Wallet>(env, instance);
    bool success = wallet->store(std::string(_path));
    if (!success) {
        LOGE("store() %s", wallet->errorString().c_str());
    }
    env->ReleaseStringUTFChars(path, _path);
    return success;
}

JNIEXPORT jstring JNICALL
Java_com_m2049r_xmrwallet_model_Wallet_getFilename(JNIEnv *env, jobject instance) {
    Bitmonero::Wallet *wallet = getHandle<Bitmonero::Wallet>(env, instance);
    return env->NewStringUTF(wallet->filename().c_str());
}

//    virtual std::string keysFilename() const = 0;

JNIEXPORT jboolean JNICALL
Java_com_m2049r_xmrwallet_model_Wallet_initJ(JNIEnv *env, jobject instance,
                                             jstring daemon_address,
                                             jlong upper_transaction_size_limit,
                                             jstring daemon_username, jstring daemon_password) {
    const char *_daemon_address = env->GetStringUTFChars(daemon_address, JNI_FALSE);
    const char *_daemon_username = env->GetStringUTFChars(daemon_username, JNI_FALSE);
    const char *_daemon_password = env->GetStringUTFChars(daemon_password, JNI_FALSE);
    Bitmonero::Wallet *wallet = getHandle<Bitmonero::Wallet>(env, instance);
    bool status = wallet->init(_daemon_address, upper_transaction_size_limit, _daemon_username,
                               _daemon_password);
    env->ReleaseStringUTFChars(daemon_address, _daemon_address);
    env->ReleaseStringUTFChars(daemon_username, _daemon_username);
    env->ReleaseStringUTFChars(daemon_password, _daemon_password);
    return status;
}

//    virtual bool createWatchOnly(const std::string &path, const std::string &password, const std::string &language) const = 0;
//    virtual void setRefreshFromBlockHeight(uint64_t refresh_from_block_height) = 0;
//    virtual void setRecoveringFromSeed(bool recoveringFromSeed) = 0;
//    virtual bool connectToDaemon() = 0;

JNIEXPORT jint JNICALL
Java_com_m2049r_xmrwallet_model_Wallet_getConnectionStatusJ(JNIEnv *env, jobject instance) {
    Bitmonero::Wallet *wallet = getHandle<Bitmonero::Wallet>(env, instance);
    return wallet->connected();
}
//TODO virtual void setTrustedDaemon(bool arg) = 0;
//TODO virtual bool trustedDaemon() const = 0;

JNIEXPORT jlong JNICALL
Java_com_m2049r_xmrwallet_model_Wallet_getBalance(JNIEnv *env, jobject instance) {
    Bitmonero::Wallet *wallet = getHandle<Bitmonero::Wallet>(env, instance);
    return wallet->balance();
}

JNIEXPORT jlong JNICALL
Java_com_m2049r_xmrwallet_model_Wallet_getUnlockedBalance(JNIEnv *env, jobject instance) {
    Bitmonero::Wallet *wallet = getHandle<Bitmonero::Wallet>(env, instance);
    return wallet->unlockedBalance();
}

JNIEXPORT jboolean JNICALL
Java_com_m2049r_xmrwallet_model_Wallet_isWatchOnly(JNIEnv *env, jobject instance) {
    Bitmonero::Wallet *wallet = getHandle<Bitmonero::Wallet>(env, instance);
    return wallet->watchOnly();
}

JNIEXPORT jlong JNICALL
Java_com_m2049r_xmrwallet_model_Wallet_getBlockChainHeight(JNIEnv *env, jobject instance) {
    Bitmonero::Wallet *wallet = getHandle<Bitmonero::Wallet>(env, instance);
    return wallet->blockChainHeight();
}

JNIEXPORT jlong JNICALL
Java_com_m2049r_xmrwallet_model_Wallet_getApproximateBlockChainHeight(JNIEnv *env,
                                                                      jobject instance) {
    Bitmonero::Wallet *wallet = getHandle<Bitmonero::Wallet>(env, instance);
    return wallet->approximateBlockChainHeight();
}

JNIEXPORT jlong JNICALL
Java_com_m2049r_xmrwallet_model_Wallet_getDaemonBlockChainHeight(JNIEnv *env, jobject instance) {
    Bitmonero::Wallet *wallet = getHandle<Bitmonero::Wallet>(env, instance);
    return wallet->daemonBlockChainHeight();
}

JNIEXPORT jlong JNICALL
Java_com_m2049r_xmrwallet_model_Wallet_getDaemonBlockChainTargetHeight(JNIEnv *env,
                                                                       jobject instance) {
    Bitmonero::Wallet *wallet = getHandle<Bitmonero::Wallet>(env, instance);
    return wallet->daemonBlockChainTargetHeight();
}

JNIEXPORT jboolean JNICALL
Java_com_m2049r_xmrwallet_model_Wallet_isSynchronized(JNIEnv *env, jobject instance) {
    Bitmonero::Wallet *wallet = getHandle<Bitmonero::Wallet>(env, instance);
    return wallet->synchronized();
}

JNIEXPORT jstring JNICALL
Java_com_m2049r_xmrwallet_model_Wallet_getDisplayAmount(JNIEnv *env, jobject clazz,
                                                        jlong amount) {
    return env->NewStringUTF(Bitmonero::Wallet::displayAmount(amount).c_str());
}

JNIEXPORT jlong JNICALL
Java_com_m2049r_xmrwallet_model_Wallet_getAmountFromString(JNIEnv *env, jobject clazz,
                                                           jstring amount) {
    const char *_amount = env->GetStringUTFChars(amount, JNI_FALSE);
    uint64_t x = Bitmonero::Wallet::amountFromString(_amount);
    env->ReleaseStringUTFChars(amount, _amount);
    return x;
}

JNIEXPORT jlong JNICALL
Java_com_m2049r_xmrwallet_model_Wallet_getAmountFromDouble(JNIEnv *env, jobject clazz,
                                                           jdouble amount) {
    return Bitmonero::Wallet::amountFromDouble(amount);
}

JNIEXPORT jstring JNICALL
Java_com_m2049r_xmrwallet_model_Wallet_generatePaymentId(JNIEnv *env, jobject clazz) {
    return env->NewStringUTF(Bitmonero::Wallet::genPaymentId().c_str());
}

JNIEXPORT jboolean JNICALL
Java_com_m2049r_xmrwallet_model_Wallet_isPaymentIdValid(JNIEnv *env, jobject clazz,
                                                        jstring payment_id) {
    const char *_payment_id = env->GetStringUTFChars(payment_id, JNI_FALSE);
    bool isValid = Bitmonero::Wallet::paymentIdValid(_payment_id);
    env->ReleaseStringUTFChars(payment_id, _payment_id);
    return isValid;
}

JNIEXPORT jboolean JNICALL
Java_com_m2049r_xmrwallet_model_Wallet_isAddressValid(JNIEnv *env, jobject clazz,
                                                      jstring address, jboolean isTestNet) {
    const char *_address = env->GetStringUTFChars(address, JNI_FALSE);
    bool isValid = Bitmonero::Wallet::addressValid(_address, isTestNet);
    env->ReleaseStringUTFChars(address, _address);
    return isValid;
}

//TODO static static bool keyValid(const std::string &secret_key_string, const std::string &address_string, bool isViewKey, bool testnet, std::string &error);

JNIEXPORT jstring JNICALL
Java_com_m2049r_xmrwallet_model_Wallet_getPaymentIdFromAddress(JNIEnv *env, jobject clazz,
                                                               jstring address,
                                                               jboolean isTestNet) {
    const char *_address = env->GetStringUTFChars(address, JNI_FALSE);
    std::string payment_id = Bitmonero::Wallet::paymentIdFromAddress(_address, isTestNet);
    env->ReleaseStringUTFChars(address, _address);
    return env->NewStringUTF(payment_id.c_str());
}

JNIEXPORT jlong JNICALL
Java_com_m2049r_xmrwallet_model_Wallet_getMaximumAllowedAmount(JNIEnv *env, jobject clazz) {
    return Bitmonero::Wallet::maximumAllowedAmount();
}

JNIEXPORT void JNICALL
Java_com_m2049r_xmrwallet_model_Wallet_startRefresh(JNIEnv *env, jobject instance) {
    Bitmonero::Wallet *wallet = getHandle<Bitmonero::Wallet>(env, instance);
    wallet->startRefresh();
}

JNIEXPORT void JNICALL
Java_com_m2049r_xmrwallet_model_Wallet_pauseRefresh(JNIEnv *env, jobject instance) {
    Bitmonero::Wallet *wallet = getHandle<Bitmonero::Wallet>(env, instance);
    wallet->pauseRefresh();
}

JNIEXPORT jboolean JNICALL
Java_com_m2049r_xmrwallet_model_Wallet_refresh(JNIEnv *env, jobject instance) {
    Bitmonero::Wallet *wallet = getHandle<Bitmonero::Wallet>(env, instance);
    return wallet->refresh();
}

JNIEXPORT void JNICALL
Java_com_m2049r_xmrwallet_model_Wallet_refreshAsync(JNIEnv *env, jobject instance) {
    Bitmonero::Wallet *wallet = getHandle<Bitmonero::Wallet>(env, instance);
    wallet->refreshAsync();
}

//TODO virtual void setAutoRefreshInterval(int millis) = 0;
//TODO virtual int autoRefreshInterval() const = 0;

JNIEXPORT jlong JNICALL
Java_com_m2049r_xmrwallet_model_Wallet_createTransactionJ(JNIEnv *env, jobject instance,
                                                          jstring dst_addr, jstring payment_id,
                                                          jlong amount, jint mixin_count,
                                                          jint priority) {

    const char *_dst_addr = env->GetStringUTFChars(dst_addr, JNI_FALSE);
    const char *_payment_id = env->GetStringUTFChars(payment_id, JNI_FALSE);
    Bitmonero::PendingTransaction::Priority _priority =
            static_cast<Bitmonero::PendingTransaction::Priority>(priority);
    Bitmonero::Wallet *wallet = getHandle<Bitmonero::Wallet>(env, instance);

    Bitmonero::PendingTransaction *tx = wallet->createTransaction(_dst_addr, _payment_id,
                                                                  amount, mixin_count,
                                                                  _priority);

    env->ReleaseStringUTFChars(dst_addr, _dst_addr);
    env->ReleaseStringUTFChars(payment_id, _payment_id);
    return reinterpret_cast<jlong>(tx);
}

JNIEXPORT jlong JNICALL
Java_com_m2049r_xmrwallet_model_Wallet_createSweepUnmixableTransactionJ(JNIEnv *env,
                                                                        jobject instance) {
    Bitmonero::Wallet *wallet = getHandle<Bitmonero::Wallet>(env, instance);
    Bitmonero::PendingTransaction *tx = wallet->createSweepUnmixableTransaction();
    return reinterpret_cast<jlong>(tx);
}

//virtual UnsignedTransaction * loadUnsignedTx(const std::string &unsigned_filename) = 0;
//virtual bool submitTransaction(const std::string &fileName) = 0;

JNIEXPORT void JNICALL
Java_com_m2049r_xmrwallet_model_Wallet_disposeTransaction(JNIEnv *env, jobject instance,
                                                          jobject pendingTransaction) {
    Bitmonero::Wallet *wallet = getHandle<Bitmonero::Wallet>(env, instance);
    Bitmonero::PendingTransaction *_pendingTransaction =
            getHandle<Bitmonero::PendingTransaction>(env, pendingTransaction);
    wallet->disposeTransaction(_pendingTransaction);
}

//virtual bool exportKeyImages(const std::string &filename) = 0;
//virtual bool importKeyImages(const std::string &filename) = 0;


//virtual TransactionHistory * history() const = 0;
JNIEXPORT jlong JNICALL
Java_com_m2049r_xmrwallet_model_Wallet_getHistoryJ(JNIEnv *env, jobject instance) {
    Bitmonero::Wallet *wallet = getHandle<Bitmonero::Wallet>(env, instance);
    return reinterpret_cast<jlong>(wallet->history());
}

//virtual AddressBook * addressBook() const = 0;

JNIEXPORT jlong JNICALL
Java_com_m2049r_xmrwallet_model_Wallet_setListenerJ(JNIEnv *env, jobject instance,
                                                    jobject javaListener) {
    Bitmonero::Wallet *wallet = getHandle<Bitmonero::Wallet>(env, instance);
    wallet->setListener(nullptr); // clear old listener
    // delete old listener
    MyWalletListener *oldListener = getHandle<MyWalletListener>(env, instance,
                                                                "listenerHandle");
    if (oldListener != nullptr) {
        oldListener->deleteGlobalJavaRef(env);
        delete oldListener;
    }
    if (javaListener == nullptr) {
        LOGD("null listener");
        return 0;
    } else {
        MyWalletListener *listener = new MyWalletListener(env, javaListener);
        wallet->setListener(listener);
        return reinterpret_cast<jlong>(listener);
    }
}

JNIEXPORT jint JNICALL
Java_com_m2049r_xmrwallet_model_Wallet_getDefaultMixin(JNIEnv *env, jobject instance) {
    Bitmonero::Wallet *wallet = getHandle<Bitmonero::Wallet>(env, instance);
    return wallet->defaultMixin();
}

JNIEXPORT void JNICALL
Java_com_m2049r_xmrwallet_model_Wallet_setDefaultMixin(JNIEnv *env, jobject instance, jint mixin) {
    Bitmonero::Wallet *wallet = getHandle<Bitmonero::Wallet>(env, instance);
    return wallet->setDefaultMixin(mixin);
}

JNIEXPORT jboolean JNICALL
Java_com_m2049r_xmrwallet_model_Wallet_setUserNote(JNIEnv *env, jobject instance,
                                                   jstring txid, jstring note) {

    const char *_txid = env->GetStringUTFChars(txid, JNI_FALSE);
    const char *_note = env->GetStringUTFChars(note, JNI_FALSE);

    Bitmonero::Wallet *wallet = getHandle<Bitmonero::Wallet>(env, instance);

    bool success = wallet->setUserNote(_txid, _note);

    env->ReleaseStringUTFChars(txid, _txid);
    env->ReleaseStringUTFChars(note, _note);

    return success;
}

JNIEXPORT jstring JNICALL
Java_com_m2049r_xmrwallet_model_Wallet_getUserNote(JNIEnv *env, jobject instance,
                                                   jstring txid) {

    const char *_txid = env->GetStringUTFChars(txid, JNI_FALSE);

    Bitmonero::Wallet *wallet = getHandle<Bitmonero::Wallet>(env, instance);

    std::string note = wallet->getUserNote(_txid);

    env->ReleaseStringUTFChars(txid, _txid);
    return env->NewStringUTF(note.c_str());
}

JNIEXPORT jstring JNICALL
Java_com_m2049r_xmrwallet_model_Wallet_getTxKey(JNIEnv *env, jobject instance,
                                                jstring txid) {

    const char *_txid = env->GetStringUTFChars(txid, JNI_FALSE);

    Bitmonero::Wallet *wallet = getHandle<Bitmonero::Wallet>(env, instance);

    std::string txKey = wallet->getTxKey(_txid);

    env->ReleaseStringUTFChars(txid, _txid);
    return env->NewStringUTF(txKey.c_str());
}


//virtual std::string signMessage(const std::string &message) = 0;
//virtual bool verifySignedMessage(const std::string &message, const std::string &addres, const std::string &signature) const = 0;

//virtual bool parse_uri(const std::string &uri, std::string &address, std::string &payment_id, uint64_t &tvAmount, std::string &tx_description, std::string &recipient_name, std::vector<std::string> &unknown_parameters, std::string &error) = 0;
//virtual bool rescanSpent() = 0;


// TransactionHistory
JNIEXPORT jint JNICALL
Java_com_m2049r_xmrwallet_model_TransactionHistory_getCount(JNIEnv *env, jobject instance) {
    Bitmonero::TransactionHistory *history = getHandle<Bitmonero::TransactionHistory>(env,
                                                                                      instance);
    return history->count();
}

jobject newTransferInstance(JNIEnv *env, uint64_t amount, const std::string &address) {
    jmethodID c = env->GetMethodID(class_Transfer, "<init>",
                                   "(JLjava/lang/String;)V");
    jstring _address = env->NewStringUTF(address.c_str());
    jobject transfer = env->NewObject(class_Transfer, c, amount, _address);
    env->DeleteLocalRef(_address);
    return transfer;
}

jobject newTransferList(JNIEnv *env, Bitmonero::TransactionInfo *info) {
    const std::vector<Bitmonero::TransactionInfo::Transfer> &transfers = info->transfers();
    if (transfers.size() == 0) { // don't create empty Lists
        return nullptr;
    }
    // make new ArrayList
    jmethodID java_util_ArrayList_ = env->GetMethodID(class_ArrayList, "<init>", "(I)V");
    jmethodID java_util_ArrayList_add = env->GetMethodID(class_ArrayList, "add",
                                                         "(Ljava/lang/Object;)Z");
    jobject result = env->NewObject(class_ArrayList, java_util_ArrayList_, transfers.size());
    // create Transfer objects and stick them in the List
    for (const Bitmonero::TransactionInfo::Transfer &s: transfers) {
        jobject element = newTransferInstance(env, s.amount, s.address);
        env->CallBooleanMethod(result, java_util_ArrayList_add, element);
        env->DeleteLocalRef(element);
    }
    return result;
}

jobject newTransactionInfo(JNIEnv *env, Bitmonero::TransactionInfo *info) {
    jmethodID c = env->GetMethodID(class_TransactionInfo, "<init>",
                                   "(IZZJJJLjava/lang/String;JLjava/lang/String;JLjava/util/List;)V");
    jobject transfers = newTransferList(env, info);
    jstring _hash = env->NewStringUTF(info->hash().c_str());
    jstring _paymentId = env->NewStringUTF(info->paymentId().c_str());
    jobject result = env->NewObject(class_TransactionInfo, c,
                                    info->direction(),
                                    info->isPending(),
                                    info->isFailed(),
                                    info->amount(),
                                    info->fee(),
                                    info->blockHeight(),
                                    _hash,
                                    static_cast<jlong> (info->timestamp()),
                                    _paymentId,
                                    info->confirmations(),
                                    transfers);
    env->DeleteLocalRef(transfers);
    env->DeleteLocalRef(_hash);
    env->DeleteLocalRef(_paymentId);
    return result;
}

#include <stdio.h>
#include <stdlib.h>
jobject cpp2java(JNIEnv *env, std::vector<Bitmonero::TransactionInfo *> vector) {

    jmethodID java_util_ArrayList_ = env->GetMethodID(class_ArrayList, "<init>", "(I)V");
    jmethodID java_util_ArrayList_add = env->GetMethodID(class_ArrayList, "add",
                                                         "(Ljava/lang/Object;)Z");

    jobject arrayList = env->NewObject(class_ArrayList, java_util_ArrayList_, vector.size());
    for (Bitmonero::TransactionInfo *s: vector) {
        jobject info = newTransactionInfo(env, s);
        env->CallBooleanMethod(arrayList, java_util_ArrayList_add, info);
        env->DeleteLocalRef(info);
    }
    return arrayList;
}

JNIEXPORT jobject JNICALL
Java_com_m2049r_xmrwallet_model_TransactionHistory_refreshJ(JNIEnv *env, jobject instance) {
    Bitmonero::TransactionHistory *history = getHandle<Bitmonero::TransactionHistory>(env,
                                                                                      instance);
    history->refresh();
    return cpp2java(env, history->getAll());
}

// TransactionInfo is implemented in Java - no need here

JNIEXPORT jint JNICALL
Java_com_m2049r_xmrwallet_model_PendingTransaction_getStatusJ(JNIEnv *env, jobject instance) {
    Bitmonero::PendingTransaction *tx = getHandle<Bitmonero::PendingTransaction>(env, instance);
    return tx->status();
}

JNIEXPORT jstring JNICALL
Java_com_m2049r_xmrwallet_model_PendingTransaction_getErrorString(JNIEnv *env, jobject instance) {
    Bitmonero::PendingTransaction *tx = getHandle<Bitmonero::PendingTransaction>(env, instance);
    return env->NewStringUTF(tx->errorString().c_str());
}

// commit transaction or save to file if filename is provided.
JNIEXPORT jboolean JNICALL
Java_com_m2049r_xmrwallet_model_PendingTransaction_commit(JNIEnv *env, jobject instance,
                                                          jstring filename, jboolean overwrite) {

    const char *_filename = env->GetStringUTFChars(filename, JNI_FALSE);

    Bitmonero::PendingTransaction *tx = getHandle<Bitmonero::PendingTransaction>(env, instance);
    bool success = tx->commit(_filename, overwrite);

    env->ReleaseStringUTFChars(filename, _filename);
    return success;
}


JNIEXPORT jlong JNICALL
Java_com_m2049r_xmrwallet_model_PendingTransaction_getAmount(JNIEnv *env, jobject instance) {
    Bitmonero::PendingTransaction *tx = getHandle<Bitmonero::PendingTransaction>(env, instance);
    return tx->amount();
}
JNIEXPORT jlong JNICALL
Java_com_m2049r_xmrwallet_model_PendingTransaction_getDust(JNIEnv *env, jobject instance) {
    Bitmonero::PendingTransaction *tx = getHandle<Bitmonero::PendingTransaction>(env, instance);
    return tx->dust();
}
JNIEXPORT jlong JNICALL
Java_com_m2049r_xmrwallet_model_PendingTransaction_getFee(JNIEnv *env, jobject instance) {
    Bitmonero::PendingTransaction *tx = getHandle<Bitmonero::PendingTransaction>(env, instance);
    return tx->fee();
}

// TODO this returns a vector of strings - deal with this later - for now return first one
JNIEXPORT jstring JNICALL
Java_com_m2049r_xmrwallet_model_PendingTransaction_getFirstTxId(JNIEnv *env, jobject instance) {
    Bitmonero::PendingTransaction *tx = getHandle<Bitmonero::PendingTransaction>(env, instance);
    std::vector<std::string> txids = tx->txid();
    return env->NewStringUTF(txids.front().c_str());
}


JNIEXPORT jlong JNICALL
Java_com_m2049r_xmrwallet_model_PendingTransaction_getTxCount(JNIEnv *env, jobject instance) {
    Bitmonero::PendingTransaction *tx = getHandle<Bitmonero::PendingTransaction>(env, instance);
    return tx->txCount();
}


#ifdef __cplusplus
}
#endif

