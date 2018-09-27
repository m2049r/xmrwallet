#!/usr/bin/env bash

set -e

version=v1.5.10.5

curl -O -L https://github.com/fuwa0529/wownerujo/releases/download/${version}/external-libs.tgz

rm -rf external-libs

echo "unpacking external-libs"
tar zxfv external-libs.tgz
rm external-libs.tgz
