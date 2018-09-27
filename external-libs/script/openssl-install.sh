#!/usr/bin/env bash

set -e

source script/env.sh

SRC_DIR=$EXTERNAL_LIBS_ROOT/android-openssl
TARGET_DIR=$EXTERNAL_LIBS_ROOT/openssl

ln -sf $TARGET_DIR/include $NDK_TOOL_DIR/arm/sysroot/usr/include/openssl
ln -sf $TARGET_DIR/arm/lib/*.so $NDK_TOOL_DIR/arm/sysroot/usr/lib

ln -sf $TARGET_DIR/include $NDK_TOOL_DIR/arm64/sysroot/usr/include/openssl
ln -sf $TARGET_DIR/arm64/lib/*.so $NDK_TOOL_DIR/arm64/sysroot/usr/lib

ln -sf $TARGET_DIR/include $NDK_TOOL_DIR/x86/sysroot/usr/include/openssl
ln -sf $TARGET_DIR/x86/lib/*.so $NDK_TOOL_DIR/x86/sysroot/usr/lib

ln -sf $TARGET_DIR/include $NDK_TOOL_DIR/x86_64/sysroot/usr/include/openssl
ln -sf $TARGET_DIR/x86_64/lib/*.so $NDK_TOOL_DIR/x86_64/sysroot/usr/lib64
