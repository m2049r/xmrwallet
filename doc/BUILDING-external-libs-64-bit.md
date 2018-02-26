# BUILDING external libs (64-bit)

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
android-ndk-r15c/build/tools/make_standalone_toolchain.py --api 21 --stl=libc++ --arch arm64 --install-dir /opt/android/tool64
```

## Build OpenSSL
Best is to compile openssl from sources. Copying from your phone or elsewhere (don't!) ends up in misery.

### Setup environment
```
cd /opt/android
wget https://wiki.openssl.org/images/7/70/Setenv-android.sh
fromdos Setenv-android.sh
```

Apply the following patch:
```
--- a/Setenv-android.sh
+++ b/Setenv-android.sh
@@ -22,12 +22,12 @@
 # list in $ANDROID_NDK_ROOT/toolchains. This value is always used.
 # _ANDROID_EABI="x86-4.6"
 # _ANDROID_EABI="arm-linux-androideabi-4.6"
-_ANDROID_EABI="arm-linux-androideabi-4.8"
+_ANDROID_EABI="aarch64-linux-android-4.9"
 
 # Set _ANDROID_ARCH to the architecture you are building for.
 # This value is always used.
 # _ANDROID_ARCH=arch-x86
-_ANDROID_ARCH=arch-arm
+_ANDROID_ARCH=arch-arm64
 
 # Set _ANDROID_API to the API you want to use. You should set it
 # to one of: android-14, android-9, android-8, android-14, android-5
@@ -36,7 +36,7 @@
 # Android 5.0, there will likely be another platform added (android-22?).
 # This value is always used.
 # _ANDROID_API="android-14"
-_ANDROID_API="android-18"
+_ANDROID_API="android-21"
 # _ANDROID_API="android-19"
 
 #####################################################################
@@ -121,6 +121,9 @@
 	arch-arm)	  
       ANDROID_TOOLS="arm-linux-androideabi-gcc arm-linux-androideabi-ranlib arm-linux-androideabi-ld"
 	  ;;
+	arch-arm64)	  
+      ANDROID_TOOLS="aarch64-linux-android-gcc aarch64-linux-android-ranlib aarch64-linux-android-ld"
+	  ;;
 	arch-x86)	  
       ANDROID_TOOLS="i686-linux-android-gcc i686-linux-android-ranlib i686-linux-android-ld"
 	  ;;	  
@@ -198,6 +201,14 @@
 export ARCH=arm
 export CROSS_COMPILE="arm-linux-androideabi-"
 
+if [ "$_ANDROID_ARCH" == "arch-arm64" ]; then
+	export MACHINE=aarch64
+	export RELEASE=2.6.37
+	export SYSTEM=android
+	export ARCH=aarch64
+	export CROSS_COMPILE="aarch64-linux-android-"
+fi
+
 if [ "$_ANDROID_ARCH" == "arch-x86" ]; then
 	export MACHINE=i686
 	export RELEASE=2.6.37
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
```
Apply patch required to build for aarch64:
```
--- a/openssl-OpenSSL_1_0_2l/config
+++ b/openssl-OpenSSL_1_0_2l/config
@@ -871,6 +871,7 @@
   *-*-qnx6) OUT="QNX6" ;;
   x86-*-android|i?86-*-android) OUT="android-x86" ;;
   armv[7-9]*-*-android) OUT="android-armv7" ;;
+  aarch64-*-android) OUT="android-aarch64" ;;
   *) OUT=`echo $GUESSOS | awk -F- '{print $3}'`;;
 esac
 
--- a/openssl-OpenSSL_1_0_2l/Configure
+++ b/openssl-OpenSSL_1_0_2l/Configure
@@ -474,6 +474,7 @@
 "android","gcc:-mandroid -I\$(ANDROID_DEV)/include -B\$(ANDROID_DEV)/lib -O3 -fomit-frame-pointer -Wall::-D_REENTRANT::-ldl:BN_LLONG RC4_CHAR RC4_CHUNK DES_INT DES_UNROLL BF_PTR:${no_asm}:dlfcn:linux-shared:-fPIC::.so.\$(SHLIB_MAJOR).\$(SHLIB_MINOR)",
 "android-x86","gcc:-mandroid -I\$(ANDROID_DEV)/include -B\$(ANDROID_DEV)/lib -O3 -fomit-frame-pointer -Wall::-D_REENTRANT::-ldl:BN_LLONG ${x86_gcc_des} ${x86_gcc_opts}:".eval{my $asm=${x86_elf_asm};$asm=~s/:elf/:android/;$asm}.":dlfcn:linux-shared:-fPIC::.so.\$(SHLIB_MAJOR).\$(SHLIB_MINOR)",
 "android-armv7","gcc:-march=armv7-a -mandroid -I\$(ANDROID_DEV)/include -B\$(ANDROID_DEV)/lib -O3 -fomit-frame-pointer -Wall::-D_REENTRANT::-ldl:BN_LLONG RC4_CHAR RC4_CHUNK DES_INT DES_UNROLL BF_PTR:${armv4_asm}:dlfcn:linux-shared:-fPIC::.so.\$(SHLIB_MAJOR).\$(SHLIB_MINOR)",
+"android-aarch64","gcc:-march=armv8-a -mandroid -I\$(ANDROID_DEV)/include -B\$(ANDROID_DEV)/lib -O3 -fomit-frame-pointer -Wall::-D_REENTRANT::-ldl:SIXTY_FOUR_BIT_LONG RC4_CHAR RC4_CHUNK DES_INT DES_UNROLL BF_PTR:${aarch64_asm}:linux64:dlfcn:linux-shared:-fPIC::.so.\$(SHLIB_MAJOR).\$(SHLIB_MINOR)",
 "android-mips","gcc:-mandroid -I\$(ANDROID_DEV)/include -B\$(ANDROID_DEV)/lib -O3 -Wall::-D_REENTRANT::-ldl:BN_LLONG RC4_CHAR RC4_CHUNK DES_INT DES_UNROLL BF_PTR:${mips32_asm}:o32:dlfcn:linux-shared:-fPIC::.so.\$(SHLIB_MAJOR).\$(SHLIB_MINOR)",
 
 #### *BSD [do see comment about ${BSDthreads} above!]

```
```
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
cd /opt/android/tool64/sysroot/usr/include
ln -s ../../../../openssl/android-21/include/openssl
cd /opt/android/tool64/sysroot/usr/lib
ln -s ../../../../openssl/android-21/lib/libssl.so
ln -s ../../../../openssl/android-21/lib/libcrypto.so
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
`nano /opt/android/tool64/include/c++/4.9.x/cstdio` (`//using ::fgetpos`, `//using ::fsetpos`)

Then:
```
export PATH=/opt/android/tool64/aarch64-linux-android/bin:/opt/android/tool64/bin:$PATH
./b2 --build-type=minimal link=static runtime-link=static --with-chrono --with-date_time --with-filesystem --with-program_options --with-regex --with-serialization --with-system --with-thread --build-dir=android64 --stagedir=android64 toolset=clang threading=multi threadapi=pthread target-os=android stage
```

## Build & prepare zeromq
Only needed for zeromq versions (>v0.11.1.0).
```
cd /opt/android
git clone https://github.com/zeromq/zeromq3-x.git
export PATH=/opt/android/tool64/aarch64-linux-android/bin:/opt/android/tool64/bin:$PATH
export OUTPUT_DIR=/opt/android/zeromq
./configure --enable-static --disable-shared --host=aarch64-linux-android --prefix=$OUTPUT_DIR LDFLAGS="-L$OUTPUT_DIR/lib" CPPFLAGS="-isystem /opt/android/tool64/include/c++/4.9.x -fPIC -I$OUTPUT_DIR/include -Wno-error -D__ANDROID_API__=21" LIBS="-lgcc"
make
make install

git clone https://github.com/zeromq/cppzmq.git
cp cppzmq/*.hpp zeromq/include/
```

## And finally: Build Monero
```
cd /opt/android
git clone https://github.com/m2049r/monero.git
cd monero
git checkout monerujo-v0.11.1.0

mkdir -p build/release.android64
cd build/release.android64

# only if not set already set
export PATH=/opt/android/tool64/aarch64-linux-android/bin:/opt/android/tool64/bin:$PATH

# for zeromq versions (>v0.11.1.0) - not really tested
CC=clang CXX=clang++ cmake -D BUILD_TESTS=OFF -D ARCH="armv8-a" -D STATIC=ON -D BUILD_64=ON -D CMAKE_BUILD_TYPE=release -D ANDROID=true -D BUILD_TAG="android" -D BOOST_ROOT=/opt/android/boost_1_64_0 -D BOOST_LIBRARYDIR=/opt/android/boost_1_64_0/android64/lib  -D OPENSSL_ROOT_DIR=/opt/android/openssl/android-21 -D CMAKE_POSITION_INDEPENDENT_CODE:BOOL=true -D ZMQ_INCLUDE_PATH=/opt/android/zeromq/include -D ZMQ_LIB=/opt/android/zeromq/lib/libzmq.a ../..

# for pre-zeromq versions (<=v0.11.1.0).
CC=clang CXX=clang++ cmake -D BUILD_TESTS=OFF -D ARCH="armv8-a" -D STATIC=ON -D BUILD_64=ON -D CMAKE_BUILD_TYPE=release -D ANDROID=true -D BUILD_TAG="android" -D BOOST_ROOT=/opt/android/boost_1_58_0 -D BOOST_LIBRARYDIR=/opt/android/boost_1_58_0/android64/lib  -D OPENSSL_ROOT_DIR=/opt/android/openssl/android-21 -D CMAKE_POSITION_INDEPENDENT_CODE:BOOL=true ../..

make

find . -name '*.a' -exec cp '{}' lib \;
```
Ignore the warning from the last command - all static libraries are now in `lib`.

# Bringing it all together
- Copy all .a libraries into the appropriate `external-libs` folders.
- Copy `/opt/android/monero/src/wallet/wallet2_api.h` into `external-libs/monero/include`
