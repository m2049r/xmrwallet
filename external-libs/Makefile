# ln -s /opt/monero monero

MONERO_BRANCH=`git -C monero branch | grep "^\*" | sed 's/^..//'`
MONERUJO_BRANCH=`git -C . branch | grep "^\*" | sed 's/^..//'`

all: arm64-v8a armeabi-v7a x86_64 x86 include VERSION

arm64-v8a: monero
	-rm -rf arm64-v8a
	-docker container rm monero-android-arm64 -f
	docker build -f android64.Dockerfile -t monero-android-arm64 monero
	docker create -it --name monero-android-arm64 monero-android-arm64 bash
	docker cp monero-android-arm64:/opt/android/prefix/lib/. arm64-v8a/
	docker cp monero-android-arm64:/src/build/release/lib/. arm64-v8a//monero

armeabi-v7a: monero
	-rm -rf armeabi-v7a
	-docker container rm monero-android-arm32 -f
	docker build -f android32.Dockerfile -t monero-android-arm32 monero
	docker create -it --name monero-android-arm32 monero-android-arm32 bash
	docker cp monero-android-arm32:/opt/android/prefix/lib/. armeabi-v7a
	docker cp monero-android-arm32:/src/build/release/lib/. armeabi-v7a/monero

x86_64: monero
	-rm -rf x86_64
	-docker container rm monero-android-x86_64 -f
	docker build -f android64_x86.Dockerfile -t monero-android-x86_64 monero
	docker create -it --name monero-android-x86_64 monero-android-x86_64 bash
	docker cp monero-android-x86_64:/opt/android/prefix/lib/. x86_64
	docker cp monero-android-x86_64:/src/build/release/lib/. x86_64/monero

x86: monero
	-rm -rf x86
	-docker container rm monero-android-x86 -f
	docker build -f android32_x86.Dockerfile -t monero-android-x86 monero
	docker create -it --name monero-android-x86 monero-android-x86 bash
	docker cp monero-android-x86:/opt/android/prefix/lib/. x86
	docker cp monero-android-x86:/src/build/release/lib/. x86/monero

include: monero
	-mkdir include
	cp monero/src/wallet/api/wallet2_api.h include/

VERSION:
	echo MONERUJO `git -C . branch | grep "^\*" | sed 's/^..//'` with MONERO `git -C monero branch | grep "^\*" | sed 's/^..//'` > VERSION

clean:
	-rm -rf arm64-v8a
	-rm -rf armeabi-v7a
	-rm -rf x86_64
	-rm -rf x86
	-rm -rf include
	-rm -f VERSION

distclean: clean
	-docker container rm monero-android-arm64 -f
	-docker container rm monero-android-arm32 -f
	-docker container rm monero-android-x86_64 -f
	-docker container rm monero-android-x86 -f
	-docker image rm monero-android-arm64 -f
	-docker image rm monero-android-arm32 -f
	-docker image rm monero-android-x86_64 -f
	-docker image rm monero-android-x86 -f
	-rm monero

monero:
	$(error Please ln -s monero)

.PHONY: all clean distclean include
