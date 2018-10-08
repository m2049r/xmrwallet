#!/bin/bash

set -e

source script/env.sh

cd $EXTERNAL_LIBS_BUILD_ROOT/libsodium

if [ ! -f "configure" ]; then
  ./autogen.sh
fi

archs=(arm arm64 x86 x86_64)
for arch in ${archs[@]}; do
    case ${arch} in
        "arm")
            target_host=arm-linux-androideabi
            ;;
        "arm64")
            target_host=aarch64-linux-android
            ;;
        "x86")
            target_host=i686-linux-android
            ;;
        "x86_64")
            target_host=x86_64-linux-android
            ;;
        *)
            exit 16
            ;;
    esac

    TARGET_DIR=$EXTERNAL_LIBS_ROOT/libsodium/$arch

    if [ -f "$TARGET_DIR/lib/libsodium.la" ]; then
      continue
    fi

    mkdir -p $TARGET_DIR
    echo "building for ${arch}"

    PATH=$NDK_TOOL_DIR/$arch/$target_host/bin:$NDK_TOOL_DIR/$arch/bin:$PATH \
        CC=clang CXX=clang++; \
        ./configure \
        --prefix=${TARGET_DIR} \
        --host=${target_host} \
        --enable-static \
        --disable-shared \
        && make -j 4 \
        && make install \
        && make clean

done

exit 0
