#!/usr/bin/env bash

set -e

source script/env.sh

build_root=$EXTERNAL_LIBS_BUILD_ROOT
PATH=$ANDROID_NDK_ROOT/build/tools/:$PATH

args="--api 21 --stl=libc++"
archs=(arm arm64 x86 x86_64)

for arch in ${archs[@]}; do
    if [ ! -d "$NDK_TOOL_DIR/$arch" ]; then
        echo "installing $ANDROID_NDK_ROOT $arch $args"
        make_standalone_toolchain.py $args --arch $arch --install-dir $NDK_TOOL_DIR/$arch
        sed -i.orig "s|using ::fgetpos;|//using ::fgetpos;|" $NDK_TOOL_DIR/$arch/include/c++/4.9.x/cstdio
        sed -i.orig "s|using ::fsetpos;|//using ::fsetpos;|" $NDK_TOOL_DIR/$arch/include/c++/4.9.x/cstdio
    fi
done
