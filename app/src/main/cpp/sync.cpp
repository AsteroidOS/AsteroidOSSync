#include <jni.h>
#include <stdlib.h>
//#include <arpa/inet.h>

#include "libslirp/src/libvdeslirp.h"

extern "C" {

JNIEXPORT void JNICALL Java_org_asteroidos_sync_connectivity_SlirpService_initNative
        (JNIEnv* env, jobject thisObject, jint mtu) {

    SlirpConfig slirpcfg;
    struct vdeslirp *myslirp;
    vdeslirp_init(&slirpcfg, VDE_INIT_DEFAULT);
    slirpcfg.if_mtu = mtu;

    myslirp = vdeslirp_open(&slirpcfg);

    if (!myslirp) {
        abort();
    }

    auto fid = env->GetFieldID(env->GetObjectClass(thisObject), "mySlirp", "J");
    env->SetLongField(thisObject, fid, reinterpret_cast<jlong>(myslirp));
}

#define GET_MYSLIRP(env, thisObject)                \
    reinterpret_cast<struct vdeslirp *>(            \
        env->GetLongField(                          \
            thisObject,                             \
            env->GetFieldID(                        \
                env->GetObjectClass(thisObject),    \
                "mySlirp",                          \
                "J")))

JNIEXPORT void JNICALL Java_org_asteroidos_sync_connectivity_SlirpService_finalizeNative
        (JNIEnv* env, jobject thisObject) {

    auto mySlirp = GET_MYSLIRP(env, thisObject);

    if (mySlirp == nullptr) {
        return;
    }

    vdeslirp_close(mySlirp);
    auto fid = env->GetFieldID(env->GetObjectClass(thisObject), "mySlirp", "J");
    env->SetLongField(thisObject, fid, 0L);
}

JNIEXPORT jlong JNICALL Java_org_asteroidos_sync_connectivity_SlirpService_vdeRecv
        (JNIEnv* env, jobject thisObject, jobject dbb, jlong offset, jlong count) {

    void *buf = reinterpret_cast<char *>(env->GetDirectBufferAddress(dbb)) + offset;
    return vdeslirp_recv(GET_MYSLIRP(env, thisObject), buf, count);
}

JNIEXPORT jlong JNICALL Java_org_asteroidos_sync_connectivity_SlirpService_vdeSend
        (JNIEnv* env, jobject thisObject, jobject dbb, jlong offset, jlong count) {

    void *buf = reinterpret_cast<char *>(env->GetDirectBufferAddress(dbb)) + offset;
    return vdeslirp_send(GET_MYSLIRP(env, thisObject), buf, count);
}

JNIEXPORT jobject JNICALL Java_org_asteroidos_sync_connectivity_SlirpService_getVdeFd
        (JNIEnv* env, jobject thisObject) {

    auto fd = vdeslirp_fd(GET_MYSLIRP(env, thisObject));

    const auto class_fdesc = env->FindClass("java/io/FileDescriptor");
    const auto const_fdesc = env->GetMethodID(class_fdesc, "<init>", "()V");
    auto ret = env->NewObject(class_fdesc, const_fdesc);

    const auto field_fd = env->GetFieldID(class_fdesc, "descriptor", "I");
    env->SetIntField(ret, field_fd, fd);

    return ret;
}

JNIEXPORT jint JNICALL Java_org_asteroidos_sync_connectivity_SlirpService_vdeAddUnixFwd
        (JNIEnv* env, jobject thisObject, jstring path, jstring ip, jint port) {
    const char *c_path = env->GetStringUTFChars(path, nullptr);
    const char *c_ip = env->GetStringUTFChars(ip, nullptr);

    struct in_addr addr{ inet_addr(c_ip) };
    int rv = vdeslirp_add_unixfwd(GET_MYSLIRP(env, thisObject), const_cast<char *>(c_path), &addr, port);

    env->ReleaseStringUTFChars(path, c_path);
    env->ReleaseStringUTFChars(ip, c_ip);

    return rv;
}

JNIEXPORT jint JNICALL Java_org_asteroidos_sync_connectivity_SlirpService_vdeAddFwd
        (JNIEnv* env, jobject thisObject, jboolean udp, jstring hostip, jint hostport, jstring ip, jint port) {
    const char *c_hostip = env->GetStringUTFChars(hostip, nullptr);
    const char *c_ip = env->GetStringUTFChars(ip, nullptr);

    int rv = vdeslirp_add_fwd(GET_MYSLIRP(env, thisObject), udp, (struct in_addr){inet_addr(c_hostip) }, hostport, (struct in_addr){inet_addr(c_ip) }, port);

    env->ReleaseStringUTFChars(hostip, c_hostip);
    env->ReleaseStringUTFChars(ip, c_ip);

    return rv;
}

}
