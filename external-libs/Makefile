# ln -s /opt/aeon aeon

AEON_BRANCH=`git -C aeon branch | grep "^\*" | sed 's/^..//'`
AEONRUJO_BRANCH=`git -C . branch | grep "^\*" | sed 's/^..//'`

all: arm64-v8a x86_64 include VERSION

arm64-v8a: aeon
	-rm -rf arm64-v8a
	-docker container rm aeon-android-arm64 -f
	docker build -f android64.Dockerfile -t aeon-android-arm64 aeon
	docker create -it --name aeon-android-arm64 aeon-android-arm64 bash
	docker cp aeon-android-arm64:/opt/android/prefix/lib/. arm64-v8a/
	docker cp aeon-android-arm64:/src/build/release/lib/. arm64-v8a/aeon

armeabi-v7a: aeon
	-rm -rf armeabi-v7a
	-docker container rm aeon-android-arm32 -f
	docker build -f android32.Dockerfile -t aeon-android-arm32 aeon
	docker create -it --name aeon-android-arm32 aeon-android-arm32 bash
	docker cp aeon-android-arm32:/opt/android/prefix/lib/. armeabi-v7a
	docker cp aeon-android-arm32:/src/build/release/lib/. armeabi-v7a/aeon

x86_64: aeon
	-rm -rf x86_64
	-docker container rm aeon-android-x86_64 -f
	docker build -f android64_x86.Dockerfile -t aeon-android-x86_64 aeon
	docker create -it --name aeon-android-x86_64 aeon-android-x86_64 bash
	docker cp aeon-android-x86_64:/opt/android/prefix/lib/. x86_64
	docker cp aeon-android-x86_64:/src/build/release/lib/. x86_64/aeon

x86: aeon
	-rm -rf x86
	-docker container rm aeon-android-x86 -f
	docker build -f android32_x86.Dockerfile -t aeon-android-x86 aeon
	docker create -it --name aeon-android-x86 aeon-android-x86 bash
	docker cp aeon-android-x86:/opt/android/prefix/lib/. x86
	docker cp aeon-android-x86:/src/build/release/lib/. x86/aeon

include: aeon
	-mkdir include
	cp aeon/src/wallet/api/wallet2_api.h include/

VERSION:
	echo MONERUJO_AE `git -C . branch | grep "^\*" | sed 's/^..//'` with AEON `git -C aeon branch | grep "^\*" | sed 's/^..//'` > VERSION

clean:
	-rm -rf arm64-v8a
	-rm -rf armeabi-v7a
	-rm -rf x86_64
	-rm -rf x86
	-rm -rf include
	-rm -f VERSION

distclean: clean
	-docker container rm aeon-android-arm64 -f
	-docker container rm aeon-android-arm32 -f
	-docker container rm aeon-android-x86_64 -f
	-docker container rm aeon-android-x86 -f
	-docker image rm aeon-android-arm64 -f
	-docker image rm aeon-android-arm32 -f
	-docker image rm aeon-android-x86_64 -f
	-docker image rm aeon-android-x86 -f
	-rm aeon

aeon:
	$(error Please ln -s aeon)

.PHONY: all clean distclean include
