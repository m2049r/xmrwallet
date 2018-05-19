# BUILDING external libs

Based on https://forum.getmonero.org/5/support/87643/building-monero-v0-10-3-1-for-android and the internet.

Do not follow this blindly.

These instructions are tailored to building ```wallep_api```.

These instructions build all supported architectures: ```'armeabi-v7a', 'arm64-v8a', 'x86', 'x86_64'```.

Yes, lots of copy&paste here. TODO: Script this.

## Prepare Ubuntu environment

```Shell
sudo apt-get install build-essential cmake tofrodos libtool-bin

cd opt/android
sudo ln -s "$(pwd)" /opt/android
```

## Install Android NDK

```Shell
cd /opt/android
wget https://dl.google.com/android/repository/android-ndk-r16b-linux-x86_64.zip
unzip android-ndk-r16b-linux-x86_64.zip
ln -s android-ndk-r16b ndk
ndk/build/tools/make_standalone_toolchain.py --api 21 --stl=libc++ --arch arm --install-dir /opt/android/tool/arm
ndk/build/tools/make_standalone_toolchain.py --api 21 --stl=libc++ --arch arm64 --install-dir /opt/android/tool/arm64
ndk/build/tools/make_standalone_toolchain.py --api 21 --stl=libc++ --arch x86 --install-dir /opt/android/tool/x86
ndk/build/tools/make_standalone_toolchain.py --api 21 --stl=libc++ --arch x86_64 --install-dir /opt/android/tool/x86_64
```

## Prepare output

```Shell
mkdir -p /opt/android/build
```

## Build OpenSSL

Best is to compile openssl from sources. Copying from your phone or elsewhere (don't!) ends up in misery.

If you don't want to build for all architectures, edit ```build-all-arch.sh``` before running it (Line 12).

```Shell
cd /opt/android
wget https://github.com/openssl/openssl/archive/OpenSSL_1_0_2l.tar.gz
cd android-openssl
tar xfz ../OpenSSL_1_0_2l.tar.gz
ANDROID_NDK_ROOT=/opt/android/ndk ./build-all-arch.sh
```

### Install & make symlinks

```Shell
mkdir -p /opt/android/build/openssl/{arm,arm64,x86,x86_64}
cp -a /opt/android/android-openssl/prebuilt/armeabi   /opt/android/build/openssl/arm/lib
cp -a /opt/android/android-openssl/prebuilt/arm64-v8a /opt/android/build/openssl/arm64/lib
cp -a /opt/android/android-openssl/prebuilt/x86       /opt/android/build/openssl/x86/lib
cp -a /opt/android/android-openssl/prebuilt/x86_64    /opt/android/build/openssl/x86_64/lib
cp -aL /opt/android/android-openssl/openssl-OpenSSL_1_0_2l/include/openssl/ /opt/android/build/openssl/include
ln -s /opt/android/build/openssl/include /opt/android/build/openssl/arm/include
ln -s /opt/android/build/openssl/include /opt/android/build/openssl/arm64/include
ln -s /opt/android/build/openssl/include /opt/android/build/openssl/x86/include
ln -s /opt/android/build/openssl/include /opt/android/build/openssl/x86_64/include
```

```Shell
ln -sf /opt/android/build/openssl/include /opt/android/tool/arm/sysroot/usr/include/openssl
ln -sf /opt/android/build/openssl/arm/lib/*.so /opt/android/tool/arm/sysroot/usr/lib

ln -sf /opt/android/build/openssl/include /opt/android/tool/arm64/sysroot/usr/include/openssl
ln -sf /opt/android/build/openssl/arm64/lib/*.so /opt/android/tool/arm64/sysroot/usr/lib

ln -sf /opt/android/build/openssl/include /opt/android/tool/x86/sysroot/usr/include/openssl
ln -sf /opt/android/build/openssl/x86/lib/*.so /opt/android/tool/x86/sysroot/usr/lib

ln -sf /opt/android/build/openssl/include /opt/android/tool/x86_64/sysroot/usr/include/openssl
ln -sf /opt/android/build/openssl/x86_64/lib/*.so /opt/android/tool/x86_64/sysroot/usr/lib64
```

## Build Boost

```Shell
cd /opt/android
wget https://sourceforge.net/projects/boost/files/boost/1.58.0/boost_1_58_0.tar.gz/download -O boost_1_58_0.tar.gz
tar xfz boost_1_58_0.tar.gz
cd boost_1_58_0
./bootstrap.sh
```

Comment out `using ::fgetpos;` & `using ::fsetpos;` in `opt/android/boost_1_58_0/boost/compatibility/cpp_c_headers/cstdio`.

Then build & install to ```/opt/android/build/boost``` with

```Shell
PATH=/opt/android/tool/arm/arm-linux-androideabi/bin:/opt/android/tool/arm/bin:$PATH      ./b2 --build-type=minimal link=static runtime-link=static --with-chrono --with-date_time --with-filesystem --with-program_options --with-regex --with-serialization --with-system --with-thread --build-dir=android-arm    --prefix=/opt/android/build/boost/arm    --includedir=/opt/android/build/boost/include toolset=clang threading=multi threadapi=pthread target-os=android install
ln -sf ../include /opt/android/build/boost/arm
PATH=/opt/android/tool/arm64/aarch64-linux-android/bin:/opt/android/tool/arm64/bin:$PATH  ./b2 --build-type=minimal link=static runtime-link=static --with-chrono --with-date_time --with-filesystem --with-program_options --with-regex --with-serialization --with-system --with-thread --build-dir=android-arm64  --prefix=/opt/android/build/boost/arm64  --includedir=/opt/android/build/boost/include toolset=clang threading=multi threadapi=pthread target-os=android install
ln -sf ../include /opt/android/build/boost/arm64
PATH=/opt/android/tool/x86/i686-linux-android/bin:/opt/android/tool/x86/bin:$PATH         ./b2 --build-type=minimal link=static runtime-link=static --with-chrono --with-date_time --with-filesystem --with-program_options --with-regex --with-serialization --with-system --with-thread --build-dir=android-x86    --prefix=/opt/android/build/boost/x86    --includedir=/opt/android/build/boost/include toolset=clang threading=multi threadapi=pthread target-os=android install
ln -sf ../include /opt/android/build/boost/x86
PATH=/opt/android/tool/x86_64/x86_64-linux-android/bin:/opt/android/tool/x86_64/bin:$PATH ./b2 --build-type=minimal link=static runtime-link=static --with-chrono --with-date_time --with-filesystem --with-program_options --with-regex --with-serialization --with-system --with-thread --build-dir=android-x86_64 --prefix=/opt/android/build/boost/x86_64 --includedir=/opt/android/build/boost/include toolset=clang threading=multi threadapi=pthread target-os=android install
ln -sf ../include /opt/android/build/boost/x86_64
```

## And finally: Build Monero

```Shell
cd /opt/android/monero
./build-all-arch.sh
```

## Bringing it all together

- Copy all .a libraries into the appropriate `external-libs` folders.
- Copy `/opt/android/monero/src/wallet/api/wallet2_api.h` into `external-libs/monero/include`

If using default locations, this would mean:

```Shell
cd <path-to-xmrwallet>/external-libs
# remove old stuff
find . -name "*.a" -or -name "*.h" -type f -delete
./collect.sh
```
