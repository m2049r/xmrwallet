#!/usr/bin/env bash

set -e

source script/env.sh

cd $EXTERNAL_LIBS_BUILD_ROOT

version=1.0.16

if [ ! -d "libsodium" ]; then
  git clone https://github.com/jedisct1/libsodium.git -b ${version}
else
  cd libsodium
  git checkout ${version}
fi
