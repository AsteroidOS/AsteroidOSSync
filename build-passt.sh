#!/usr/bin/env bash

if [ -z "$NDK" ]; then
    echo "You need to define the NDK env var. Maybe to $HOME/Android/Sdk/ndk/version/ ?"
    exit 1
fi

clang_targets=("aarch64-linux-android" "armv7a-linux-androideabi" "i686-linux-android" "x86_64-linux-android")
android_abi=("arm64-v8a" "armeabi" "x86" "x86_64")

export TOOLCHAIN=$NDK/toolchains/llvm/prebuilt/linux-x86_64
export API=23 # minSdkVersion

cd passt/
for i in "${!clang_targets[@]}"; do
    echo Building for ${android_abi[i]}...
    make clean
    make CC="$TOOLCHAIN/bin/${clang_targets[i]}$API-clang" passt
    cp passt ../app/src/main/jniLibs/${android_abi[i]}/libpasst.so
done
