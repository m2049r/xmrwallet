#!/bin/bash

set -e

source script/env.sh

cd $EXTERNAL_LIBS_BUILD_ROOT/monero

orig_path=$PATH
base_dir=`pwd`

build_root=$EXTERNAL_LIBS_BUILD_ROOT
lib_root=$EXTERNAL_LIBS_ROOT

build_type=release # or debug
android_api=21
archs=(arm arm64 x86 x86_64)

for arch in ${archs[@]}; do
    ldflags=""
    extra_cmake_flags=""
    case ${arch} in
        "arm")
            target_host=arm-linux-androideabi
            xarch=armv7-a
            sixtyfour=OFF
            extra_cmake_flags="-D NO_AES=true"
            ;;
        "arm64")
            target_host=aarch64-linux-android
            xarch="armv8-a"
            sixtyfour=ON
            ;;
        "x86")
            target_host=i686-linux-android
            xarch="i686"
            sixtyfour=OFF
            ;;
        "x86_64")
            target_host=x86_64-linux-android
            xarch="x86-64"
            sixtyfour=ON
            ;;
        *)
            exit 16
            ;;
    esac

    TARGET_LIB_DIR=$lib_root/monero/$arch/lib
    if [ -f "$TARGET_LIB_DIR/libwallet_api.a" ]; then
      continue
    fi

    OUTPUT_DIR=$base_dir/build/$build_type.$arch
    rm -rf $OUTPUT_DIR
    mkdir -p $OUTPUT_DIR
    cd $OUTPUT_DIR

    PATH=$NDK_TOOL_DIR/$arch/$target_host/bin:$NDK_TOOL_DIR/$arch/bin:$PATH \
    CC=clang CXX=clang++ \
    CMAKE_LIBRARY_PATH=$lib_root/libsodium/$arch/lib \
    cmake \
      -D BUILD_GUI_DEPS=1 \
      -D BUILD_TESTS=OFF \
      -D ARCH="$xarch" \
      -D STATIC=ON \
      -D BUILD_64=$sixtyfour \
      -D CMAKE_BUILD_TYPE=$build_type \
      -D CMAKE_CXX_FLAGS="-D__ANDROID_API__=$android_api" \
      -D ANDROID=true \
      -D BUILD_TAG="android" \
      -D BOOST_ROOT=$lib_root/boost/$arch \
      -D BOOST_LIBRARYDIR=$lib_root/boost/$arch/lib \
      -D OPENSSL_ROOT_DIR=$lib_root/openssl/$arch \
      -D OPENSSL_INCLUDE_DIR=$lib_root/openssl/$arch \
      -D OPENSSL_CRYPTO_LIBRARY=$lib_root/openssl/$arch/lib/libcrypto.so \
      -D OPENSSL_SSL_LIBRARY=$lib_root/openssl/$arch/lib/libssl.so \
      -D CMAKE_POSITION_INDEPENDENT_CODE:BOOL=true \
      -D MONERUJO_HIDAPI=ON \
      -D USE_DEVICE_TREZOR=OFF \
      -D LIBSODIUM_INCLUDE_DIR=$lib_root/libsodium/$arch/include \
       $extra_cmake_flags \
       ../..

    make -j 4 wallet_api

    find . -path ./lib -prune -o -name '*.a' -exec cp '{}' lib \;

    rm -rf $TARGET_LIB_DIR
    mkdir -p $TARGET_LIB_DIR
    cp $OUTPUT_DIR/lib/*.a $TARGET_LIB_DIR

    TARGET_INC_DIR=$lib_root/monero/include
    rm -rf $TARGET_INC_DIR
    mkdir -p $TARGET_INC_DIR
    cp -a $base_dir/src/wallet/api/wallet2_api.h $TARGET_INC_DIR

    cd $base_dir
done

exit 0
