#!/usr/bin/env bash

set -e

source script/env.sh

rm -rf $EXTERNAL_LIBS_BUILD
mkdir -p $EXTERNAL_LIBS_BUILD/src
