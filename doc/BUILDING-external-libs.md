# BUILDING external libs

Based on https://forum.getmonero.org/5/support/87643/building-monero-v0-10-3-1-for-android and the internet.

Do not follow this blindly.

These instructions build all supported architectures: ```'armeabi-v7a', 'arm64-v8a', 'x86', 'x86_64'```.

Yes, lots of copy&paste here. TODO: Script this.

## Prepare Ubuntu environment

```
sudo apt-get install build-essential cmake tofrodos libtool-bin
sudo mkdir /opt/android
sudo chown $LOGNAME /opt/android
```

## Install Android NDK
```
cd /opt/android
wget https://dl.google.com/android/repository/android-ndk-r15c-linux-x86_64.zip
unzip android-ndk-r15c-linux-x86_64.zip
android-ndk-r15c/build/tools/make_standalone_toolchain.py --api 21 --stl=libc++ --arch arm --install-dir /opt/android/tool32
android-ndk-r15c/build/tools/make_standalone_toolchain.py --api 21 --stl=libc++ --arch arm64 --install-dir /opt/android/tool64
android-ndk-r15c/build/tools/make_standalone_toolchain.py --api 21 --stl=libc++ --arch x86 --install-dir /opt/android/toolx86
android-ndk-r15c/build/tools/make_standalone_toolchain.py --api 21 --stl=libc++ --arch x86_64 --install-dir /opt/android/toolx86_64
```

## Build OpenSSL
Best is to compile openssl from sources. Copying from your phone or elsewhere (don't!) ends up in misery.

If you don't want to build for all architectures, edit ```build-all-arch.sh``` before running it (Line 12).

```
cd /opt/android
git clone https://github.com/m2049r/android-openssl.git
wget https://github.com/openssl/openssl/archive/OpenSSL_1_0_2l.tar.gz
cd android-openssl
tar xfz ../OpenSSL_1_0_2l.tar.gz
export ANDROID_NDK_ROOT=/opt/android/android-ndk-r15c
./build-all-arch.sh
```

### Make symlinks
```
cd /opt/android/tool32/sysroot/usr/include
ln -s ../../../../android-openssl/openssl-OpenSSL_1_0_2l/include/openssl/
cd /opt/android/tool32/sysroot/usr/lib
ln -s ../../../../android-openssl/prebuilt/armeabi/libssl.so
ln -s ../../../../android-openssl/prebuilt/armeabi/libcrypto.so

cd /opt/android/tool64/sysroot/usr/include
ln -s ../../../../android-openssl/openssl-OpenSSL_1_0_2l/include/openssl/
cd /opt/android/tool64/sysroot/usr/lib
ln -s ../../../../android-openssl/prebuilt/arm64-v8a/libssl.so
ln -s ../../../../android-openssl/prebuilt/arm64-v8a/libcrypto.so

cd /opt/android/toolx86/sysroot/usr/include
ln -s ../../../../android-openssl/openssl-OpenSSL_1_0_2l/include/openssl/
cd /opt/android/toolx86/sysroot/usr/lib
ln -s ../../../../android-openssl/prebuilt/x86/libssl.so
ln -s ../../../../android-openssl/prebuilt/x86/libcrypto.so

cd /opt/android/toolx86_64/sysroot/usr/include
ln -s ../../../../android-openssl/openssl-OpenSSL_1_0_2l/include/openssl/
cd /opt/android/toolx86_64/sysroot/usr/lib
ln -s ../../../../android-openssl/prebuilt/x86_64/libssl.so
ln -s ../../../../android-openssl/prebuilt/x86_64/libcrypto.so
```

## Build Boost
```
cd /opt/android
wget https://sourceforge.net/projects/boost/files/boost/1.58.0/boost_1_58_0.tar.gz/download -O boost_1_58_0.tar.gz
tar xfz boost_1_58_0.tar.gz
cd boost_1_58_0
./bootstrap.sh
```
The NDK r15c above gives errors about fsetpos and fgetpos not found(!?!), so we "just" comment them out in the include file:
* `vi /opt/android/tool32/include/c++/4.9.x/cstdio` (`//using ::fgetpos`, `//using ::fsetpos`)
* `vi /opt/android/tool64/include/c++/4.9.x/cstdio` (`//using ::fgetpos`, `//using ::fsetpos`)
* `vi /opt/android/toolx86/include/c++/4.9.x/cstdio` (`//using ::fgetpos`, `//using ::fsetpos`)
* `vi /opt/android/toolx86_64/include/c++/4.9.x/cstdio` (`//using ::fgetpos`, `//using ::fsetpos`)

Then:
```
PATH=/opt/android/tool32/arm-linux-androideabi/bin:/opt/android/tool32/bin:$PATH ./b2 --build-type=minimal link=static runtime-link=static --with-chrono --with-date_time --with-filesystem --with-program_options --with-regex --with-serialization --with-system --with-thread --build-dir=android32 --stagedir=android32 toolset=clang threading=multi threadapi=pthread target-os=android stage
PATH=/opt/android/tool64/aarch64-linux-android/bin:/opt/android/tool64/bin:$PATH ./b2 --build-type=minimal link=static runtime-link=static --with-chrono --with-date_time --with-filesystem --with-program_options --with-regex --with-serialization --with-system --with-thread --build-dir=android64 --stagedir=android64 toolset=clang threading=multi threadapi=pthread target-os=android stage
PATH=/opt/android/toolx86/i686-linux-android/bin:/opt/android/toolx86/bin:$PATH ./b2 --build-type=minimal link=static runtime-link=static --with-chrono --with-date_time --with-filesystem --with-program_options --with-regex --with-serialization --with-system --with-thread --build-dir=androidx86 --stagedir=androidx86 toolset=clang threading=multi threadapi=pthread target-os=android stage
PATH=/opt/android/toolx86_64/x86_64-linux-android/bin:/opt/android/toolx86_64/bin:$PATH ./b2 --build-type=minimal link=static runtime-link=static --with-chrono --with-date_time --with-filesystem --with-program_options --with-regex --with-serialization --with-system --with-thread --build-dir=androidx86_64 --stagedir=androidx86_64 toolset=clang threading=multi threadapi=pthread target-os=android stage
```

## And finally: Build Monero
```
cd /opt/android
git clone https://github.com/m2049r/monero.git

cd /opt/android/monero
mkdir -p build/release.android32
cd build/release.android32
PATH=/opt/android/tool32/arm-linux-androideabi/bin:/opt/android/tool32/bin:$PATH CC=clang CXX=clang++ cmake -D BUILD_TESTS=OFF -D ARCH="armv7-a" -D STATIC=ON -D BUILD_64=OFF -D CMAKE_BUILD_TYPE=release -D ANDROID=true -D BUILD_TAG="android" -D BOOST_ROOT=/opt/android/boost_1_58_0 -D BOOST_LIBRARYDIR=/opt/android/boost_1_58_0/android32/lib  -D OPENSSL_ROOT_DIR=/opt/android/android-openssl/openssl-OpenSSL_1_0_2l -D OPENSSL_CRYPTO_LIBRARY=/opt/android/android-openssl/prebuilt/armeabi/libcrypto.so -D OPENSSL_SSL_LIBRARY=/opt/android/android-openssl/prebuilt/armeabi/libssl.so -D CMAKE_POSITION_INDEPENDENT_CODE:BOOL=true ../..
make
find . -name '*.a' -exec cp '{}' lib \;

cd /opt/android/monero
mkdir -p build/release.android64
cd build/release.android64
PATH=/opt/android/tool64/aarch64-linux-android/bin:/opt/android/tool64/bin:$PATH CC=clang CXX=clang++ cmake -D BUILD_TESTS=OFF -D ARCH="armv8-a" -D STATIC=ON -D BUILD_64=ON -D CMAKE_BUILD_TYPE=release -D ANDROID=true -D BUILD_TAG="android" -D BOOST_ROOT=/opt/android/boost_1_58_0 -D BOOST_LIBRARYDIR=/opt/android/boost_1_58_0/android64/lib  -D OPENSSL_ROOT_DIR=/opt/android/android-openssl/openssl-OpenSSL_1_0_2l -D OPENSSL_CRYPTO_LIBRARY=/opt/android/android-openssl/prebuilt/arm64-v8a/libcrypto.so -D OPENSSL_SSL_LIBRARY=/opt/android/android-openssl/prebuilt/arm64-v8a/libssl.so -D CMAKE_POSITION_INDEPENDENT_CODE:BOOL=true ../..
make
find . -name '*.a' -exec cp '{}' lib \;

cd /opt/android/monero
mkdir -p build/release.androidx86
cd build/release.androidx86
PATH=/opt/android/toolx86/i686-linux-android/bin:/opt/android/toolx86/bin:$PATH CC=clang CXX=clang++ cmake -D BUILD_TESTS=OFF -D ARCH="i686" -D STATIC=ON -D BUILD_64=OFF -D CMAKE_BUILD_TYPE=release -D ANDROID=true -D BUILD_TAG="android" -D BOOST_ROOT=/opt/android/boost_1_58_0 -D BOOST_LIBRARYDIR=/opt/android/boost_1_58_0/androidx86/lib  -D OPENSSL_ROOT_DIR=/opt/android/android-openssl/openssl-OpenSSL_1_0_2l -D OPENSSL_CRYPTO_LIBRARY=/opt/android/android-openssl/prebuilt/x86/libcrypto.so -D OPENSSL_SSL_LIBRARY=/opt/android/android-openssl/prebuilt/x86/libssl.so -D CMAKE_POSITION_INDEPENDENT_CODE:BOOL=true ../..
make
find . -name '*.a' -exec cp '{}' lib \;

cd /opt/android/monero
mkdir -p build/release.androidx86_64
cd build/release.androidx86_64
PATH=/opt/android/toolx86_64/x86_64-linux-android/bin:/opt/android/toolx86_64/bin:$PATH CC=clang CXX=clang++ cmake -D BUILD_TESTS=OFF -D ARCH="x86-64" -D STATIC=ON -D BUILD_64=ON -D CMAKE_BUILD_TYPE=release -D ANDROID=true -D BUILD_TAG="android" -D BOOST_ROOT=/opt/android/boost_1_58_0 -D BOOST_LIBRARYDIR=/opt/android/boost_1_58_0/androidx86_64/lib  -D OPENSSL_ROOT_DIR=/opt/android/android-openssl/openssl-OpenSSL_1_0_2l -D OPENSSL_CRYPTO_LIBRARY=/opt/android/android-openssl/prebuilt/x86_64/libcrypto.so -D OPENSSL_SSL_LIBRARY=/opt/android/android-openssl/prebuilt/x86_64/libssl.so -D CMAKE_POSITION_INDEPENDENT_CODE:BOOL=true ../..
make
find . -name '*.a' -exec cp '{}' lib \;
```
Ignore the warning from ```find``` - all monero static libraries are now in `lib`.

# Bringing it all together
- Copy all .a libraries into the appropriate `external-libs` folders.
- Copy `/opt/android/monero/src/wallet/wallet2_api.h` into `external-libs/monero/include`

If using default locations, this would mean:
```
mkdir -p ~/StudioProjects/xmrwallet/external-libs/boost/lib/armeabi-v7a
cp -a /opt/android/boost_1_58_0/android32/lib/*.a ~/StudioProjects/xmrwallet/external-libs/boost/lib/armeabi-v7a
mkdir -p ~/StudioProjects/xmrwallet/external-libs/openssl/lib/armeabi-v7a
cp -a /opt/android/android-openssl/prebuilt/armeabi/*.a ~/StudioProjects/xmrwallet/external-libs/openssl/lib/armeabi-v7a
mkdir -p ~/StudioProjects/xmrwallet/external-libs/monero/lib/armeabi-v7a
cp -a /opt/android/monero/build/release.android32/lib/*.a ~/StudioProjects/xmrwallet/external-libs/monero/lib/armeabi-v7a

mkdir -p ~/StudioProjects/xmrwallet/external-libs/boost/lib/arm64-v8a
cp -a /opt/android/boost_1_58_0/android64/lib/*.a ~/StudioProjects/xmrwallet/external-libs/boost/lib/arm64-v8a
mkdir -p ~/StudioProjects/xmrwallet/external-libs/openssl/lib/arm64-v8a
cp -a /opt/android/android-openssl/prebuilt/arm64-v8a/*.a ~/StudioProjects/xmrwallet/external-libs/openssl/lib/arm64-v8a
mkdir -p ~/StudioProjects/xmrwallet/external-libs/monero/lib/arm64-v8a
cp -a /opt/android/monero/build/release.android64/lib/*.a ~/StudioProjects/xmrwallet/external-libs/monero/lib/arm64-v8a

mkdir -p ~/StudioProjects/xmrwallet/external-libs/boost/lib/x86
cp -a /opt/android/boost_1_58_0/androidx86/lib/*.a ~/StudioProjects/xmrwallet/external-libs/boost/lib/x86
mkdir -p ~/StudioProjects/xmrwallet/external-libs/openssl/lib/x86
cp -a /opt/android/android-openssl/prebuilt/x86/*.a ~/StudioProjects/xmrwallet/external-libs/openssl/lib/x86
mkdir -p ~/StudioProjects/xmrwallet/external-libs/monero/lib/x86
cp -a /opt/android/monero/build/release.androidx86/lib/*.a ~/StudioProjects/xmrwallet/external-libs/monero/lib/x86

mkdir -p ~/StudioProjects/xmrwallet/external-libs/boost/lib/x86_64
cp -a /opt/android/boost_1_58_0/androidx86_64/lib/*.a ~/StudioProjects/xmrwallet/external-libs/boost/lib/x86_64
mkdir -p ~/StudioProjects/xmrwallet/external-libs/openssl/lib/x86_64
cp -a /opt/android/android-openssl/prebuilt/x86_64/*.a ~/StudioProjects/xmrwallet/external-libs/openssl/lib/x86_64
mkdir -p ~/StudioProjects/xmrwallet/external-libs/monero/lib/x86_64
cp -a /opt/android/monero/build/release.androidx86_64/lib/*.a ~/StudioProjects/xmrwallet/external-libs/monero/lib/x86_64
```
