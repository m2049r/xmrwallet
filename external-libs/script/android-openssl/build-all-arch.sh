#!/bin/bash
#
# http://wiki.openssl.org/index.php/Android
#
# needs ANDROID_NDK_ROOT set correctly (e.g. to /opt/android/android-ndk-r15c)

set -e

SRC_DIR=$EXTERNAL_LIBS_BUILD_ROOT/android-openssl
TARGET_DIR=$EXTERNAL_LIBS_ROOT/openssl

archs=(arm arm64 x86 x86_64)

for arch in ${archs[@]}; do
    xLIB="/lib"
    case ${arch} in
        "arm")
            _ANDROID_TARGET_SELECT=arch-arm
            _ANDROID_ARCH=arch-arm
            _ANDROID_EABI=arm-linux-androideabi-4.9
            _ANDROID_EABI_INC=arm-linux-androideabi
            configure_platform="android-armv7" ;;
        "arm64")
            _ANDROID_TARGET_SELECT=arch-arm64-v8a
            _ANDROID_ARCH=arch-arm64
            _ANDROID_EABI=aarch64-linux-android-4.9
            _ANDROID_EABI_INC=aarch64-linux-android
            configure_platform="linux-generic64 -DB_ENDIAN" ;;
        "x86")
            _ANDROID_TARGET_SELECT=arch-x86
            _ANDROID_ARCH=arch-x86
            _ANDROID_EABI=x86-4.9
            _ANDROID_EABI_INC=i686-linux-android
            configure_platform="android-x86" ;;
        "x86_64")
            _ANDROID_TARGET_SELECT=arch-x86_64
            _ANDROID_ARCH=arch-x86_64
            _ANDROID_EABI=x86_64-4.9
            _ANDROID_EABI_INC=x86_64-linux-android
            xLIB="/lib64"
            configure_platform="linux-generic64" ;;
        *)
            configure_platform="linux-elf" ;;
    esac

    TARGET_LIB_DIR=$TARGET_DIR/$arch/lib

    if [ -f "$TARGET_LIB_DIR/libcrypto.a" ] && [ -f "$TARGET_LIB_DIR/libssl.a" ]; then
      continue
    fi

    mkdir -p $TARGET_LIB_DIR

    . ./setenv-android-mod.sh

    echo "CROSS COMPILE ENV : $CROSS_COMPILE"
    cd openssl-OpenSSL_1_0_2l

    xCFLAGS="-DSHARED_EXTENSION=.so -fPIC -DOPENSSL_PIC -DDSO_DLFCN -DHAVE_DLFCN_H -I$ANDROID_NDK_ROOT/sysroot/usr/include -I$ANDROID_NDK_ROOT/sysroot/usr/include/$_ANDROID_EABI_INC -I$ANDROID_DEV/include -B$ANDROID_DEV/$xLIB -O -fomit-frame-pointer -W"

    perl -pi -e 's/install: all install_docs install_sw/install: install_docs install_sw/g' Makefile.org
    ./Configure shared no-threads no-asm no-zlib no-ssl2 no-ssl3 no-comp no-hw no-engine -D__ANDROID_API__=21 $configure_platform $xCFLAGS

    # patch SONAME

    perl -pi -e 's/SHLIB_EXT=\.so\.\$\(SHLIB_MAJOR\)\.\$\(SHLIB_MINOR\)/SHLIB_EXT=\.so/g' Makefile
    perl -pi -e 's/SHARED_LIBS_LINK_EXTS=\.so\.\$\(SHLIB_MAJOR\) \.so//g' Makefile
    # quote injection for proper SONAME, fuck...
    perl -pi -e 's/SHLIB_MAJOR=1/SHLIB_MAJOR=`/g' Makefile
    perl -pi -e 's/SHLIB_MINOR=0.0/SHLIB_MINOR=`/g' Makefile

    make clean
    make depend
    make -j 4 all

    file libcrypto.so
    file libssl.so
    cp libcrypto.a $TARGET_LIB_DIR
    cp libssl.a $TARGET_LIB_DIR

    cp -aL $SRC_DIR/openssl-OpenSSL_1_0_2l/include/openssl/ $TARGET_DIR/include
#    ln -sf $TARGET_DIR/include $TARGET_DIR/arm/include

    cd ..
done
exit 0

