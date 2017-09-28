# BUILDING external libs

Based on https://forum.getmonero.org/5/support/87643/building-monero-v0-10-3-1-for-android and the internet.

Do not follow this blindly. These instructions are for 32-bit only. 64-bit building mostly involves
replacing "32" with "64".

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
```

## Build OpenSSL
Best is to compile openssl from sources. Copying from your phone or elsewhere (don't!) ends up in misery.

### Setup environment
```
cd /opt/android
wget https://wiki.openssl.org/images/7/70/Setenv-android.sh
fromdos Setenv-android.sh
```

Edit Setenv-android.sh:
```
_ANDROID_EABI="arm-linux-androideabi-4.9"
_ANDROID_API="android-21"
```
Then:
```
export ANDROID_NDK_ROOT=/opt/android/android-ndk-r15c
. ./Setenv-android.sh
```
and ignore error about FIPS_SIG.

### Download and build OpenSSL
```
wget https://github.com/openssl/openssl/archive/OpenSSL_1_0_2l.tar.gz
tar xfz OpenSSL_1_0_2l.tar.gz
cd openssl-OpenSSL_1_0_2l/
perl -pi -e 's/install: all install_docs install_sw/install: install_docs install_sw/g' Makefile.org
./config shared no-ssl2 no-ssl3 no-comp no-hw no-engine --openssldir=/opt/android/openssl/android-21/
make depend
# Make sure we don't get versioned .so which Android can't deal with
make CALC_VERSIONS="SHLIB_COMPAT=; SHLIB_SOVER=" MAKE="make -e" all
mkdir -p /opt/android/openssl/android-21/lib
echo "place-holder make target for avoiding symlinks" >> /opt/android/openssl/android-21/lib/link-shared
make SHLIB_EXT=.so install_sw
```

### Make symlinks
```
cd /opt/android/tool32/sysroot/usr/include
ln -s ../../../../openssl/android-21/include/openssl
cd /opt/android/tool32/sysroot/usr/lib
ln -s ../../../../openssl/android-21/lib/libssl.so
ln -s ../../../../openssl/android-21/lib/libcrypto.so
```

## Build Boost
```
cd /opt/android
wget https://sourceforge.net/projects/boost/files/boost/1.58.0/boost_1_58_0.tar.gz/download -O boost_1_58_0.tar.gz
tar xfz boost_1_58_0.tar.gz
(cd boost_1_58_0; ./bootstrap.sh)
```
The NDK r15c above gives errors about fsetpos and fgetpos not found(!?!), so we "just" comment them out in the include file:
`nano /opt/android/tool32/include/c++/4.9.x/cstdio` (`//using ::fgetpos`, `//using ::fsetpos`)

Then:
```
export PATH=/opt/android/tool32/arm-linux-androideabi/bin:/opt/android/tool32/bin:$PATH
./b2 --build-type=minimal link=static runtime-link=static --with-chrono --with-date_time --with-filesystem --with-program_options --with-regex --with-serialization --with-system --with-thread --build-dir=android32 --stagedir=android32 toolset=clang threading=multi threadapi=pthread target-os=android stage
```

## Build & prepare zeromq
Only needed for zeromq versions (>v0.11.0.0).
```
cd /opt/android
wget git clone https://github.com/zeromq/zeromq3-x.git
export PATH=/opt/android/tool32/arm-linux-androideabi/bin:/opt/android/tool32/bin:$PATH
export OUTPUT_DIR=/opt/android/zeromq
./configure --enable-static --disable-shared --host=arm-linux-androideabi --prefix=$OUTPUT_DIR LDFLAGS="-L$OUTPUT_DIR/lib" CPPFLAGS="-isystem /opt/android/tool32/include/c++/4.9.x -fPIC -I$OUTPUT_DIR/include -Wno-error -D__ANDROID_API__=21" LIBS="-lgcc"
make
make install

git clone https://github.com/zeromq/cppzmq.git
cp cppzmq/*.hpp zeromq/include/
```

## And finally: Build Monero
```
cd /opt/android
git clone https://github.com/monero-project/monero
cd monero
```
```
# <patch monero code as needed>
# also, don't abort on warnings (this is only an issue >v0.11.0.0):
diff --git a/CMakeLists.txt b/CMakeLists.txt
index 1f74f59..2c791c0 100644
--- a/CMakeLists.txt
+++ b/CMakeLists.txt
@@ -400,7 +400,7 @@ else()
     set(ARCH_FLAG "-march=${ARCH}")
   endif()
   set(WARNINGS "-Wall -Wextra -Wpointer-arith -Wundef -Wvla -Wwrite-strings -Wno-error=extra -Wno-error=deprecated-declarations -Wno-unused-parameter -Wno-unused-variable -Wno-error=unused-variable -Wno-error=undef -Wno-error=uninitialized")
-  if(NOT MINGW)
+  if(NOT MINGW AND NOT ANDROID)
     set(WARNINGS_AS_ERRORS_FLAG "-Werror")
   endif()
   if(CMAKE_C_COMPILER_ID STREQUAL "Clang")
```
```
mkdir -p build/release.android32
cd build/release.android32

# only if not set already set
export PATH=/opt/android/tool32/arm-linux-androideabi/bin:/opt/android/tool32/bin:$PATH

# for zeromq versions (>v0.11.0.0).
CC=clang CXX=clang++ cmake -D BUILD_TESTS=OFF -D ARCH="armv7-a" -D STATIC=ON -D BUILD_64=OFF -D CMAKE_BUILD_TYPE=release -D ANDROID=true -D BUILD_TAG="android" -D BOOST_ROOT=/opt/android/boost_1_58_0 -D BOOST_LIBRARYDIR=/opt/android/boost_1_58_0/android32/lib  -D OPENSSL_ROOT_DIR=/opt/android/openssl/android-21 -D CMAKE_POSITION_INDEPENDENT_CODE:BOOL=true -D ZMQ_INCLUDE_PATH=/opt/android/zeromq/include -D ZMQ_LIB=/opt/android/zeromq/lib/libzmq.a ../..

# for pre-zeromq versions (<=v0.11.0.0).
CC=clang CXX=clang++ cmake -D BUILD_TESTS=OFF -D ARCH="armv7-a" -D STATIC=ON -D BUILD_64=OFF -D CMAKE_BUILD_TYPE=release -D ANDROID=true -D BUILD_TAG="android" -D BOOST_ROOT=/opt/android/boost_1_58_0 -D BOOST_LIBRARYDIR=/opt/android/boost_1_58_0/android32/lib  -D OPENSSL_ROOT_DIR=/opt/android/openssl/android-21 -D CMAKE_POSITION_INDEPENDENT_CODE:BOOL=true ../..

make

find . -name '*.a' -exec cp '{}' lib \;
```
Ignore the warning from the last command - all static libraries are now in `lib`.

# Bringing it all together
- Copy all .a libraries into the appropriate `external-libs` folders.
- Copy `/opt/android/monero/src/wallet/wallet2_api.h` into `external-libs/monero/include`
