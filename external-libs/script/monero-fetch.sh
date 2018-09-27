#!/usr/bin/env bash

set -e

source script/env.sh

cd $EXTERNAL_LIBS_BUILD_ROOT

url="https://github.com/m2049r/monero"
version="release-v0.13-prep"

if [ ! -d "monero" ]; then
  git clone ${url} -b ${version}
  cd monero
  git submodule update --recursive --init
else
  cd monero
  git submodule update --recursive
  git checkout ${version}
fi
