#!/usr/bin/env bash

# build the dependencies with the android ndk cross toolchain
# download the files, checksum them
# configure and make to BUILD_ROOT_ARTIFACT
# this file may be used with a tagged container:
# have 3gb data and some cpu performance for this task
# docker build . -t xmr-wallet-build
# docker run -it \
#   -v $(pwd)/artifacts:/var/src/artifacts \
#   -v $(pwd)/distfiles:/var/src/distfiles \
#   -v $(pwd)/build-artifacts.sh:/usr/local/bin/build-artifacts.sh \
#   xmr-wallet-build /bin/bash
# in-docker: time bash /usr/local/bin/build-artifacts.sh
# until it is working, then rebuild the container (ADD's file)
# and use the entrypoint / Makefile
# shellcheck disable=SC2164
set -x
if [ ! -f /.dockerenv ]; then
  echo "This script is designed to run in a docker-enviroment."
  echo "It will leave leftover files or may clean up too much"
  echo "when run on a host. Please use the Makefile."
  exit 1
fi

BUILD_ROOT=${BUILD_ROOT:-/var/src}
ARTIFACT_ROOT=${ARTIFACT_ROOT:-${BUILD_ROOT}/artifacts}
PACKAGE_DIR=${PACKAGE_DIR:-${ARTIFACT_ROOT}/package}

NDK=${NDK:-android-ndk-r16b}
export ANDROID_NDK_ROOT=/opt/android/${NDK}
export ANDROID_SDK_ROOT=/opt/android/sdk

BOOST_VERSION=${BOOST_VERSION:-1.58.0}
OPENSSL_VERSION=${OPENSSL_VERSION:-1.0.2o}
MONERO_GIT_URL=https://github.com/m2049r/monero.git
MONERO_GIT_BRANCH=${MONERO_GIT_BRANCH:-latest}

ANDROID_OPENSSL_GIT_URL=${ANDROID_OPENSSL_GIT_URL:-https://github.com/m2049r/android-openssl.git}
OPENSSL_VERSION_=${OPENSSL_VERSION//\./_}
OPENSSL_TAR=OpenSSL_${OPENSSL_VERSION_}.tar.gz
OPENSSL_TAR_SHA256SUM=${OPENSSL_TAR_SHA256SUM:-fd8bff81636c262ff82cb22286957d73213f899c1a80a3ec712c7ae80761ea9b}
OPENSSL_URL_BASE=${OPENSSL_URL_BASE:-https://github.com/openssl/openssl/archive}
OPENSSL_URL=${OPENSSL_URL_BASE}/${OPENSSL_TAR}

BOOST_VERSION_=${BOOST_VERSION//\./_}
BOOST_DIR=boost_${BOOST_VERSION_}
BOOST_TAR=boost_${BOOST_VERSION_}.tar.gz
BOOST_TAR_SHA256SUM=${BOOST_TAR_SHA256SUM:-a004d9b3fa95e956383693b86fce1b68805a6f71c2e68944fa813de0fb8c8102}
BOOST_URL_BASE=${BOOST_URL_BASE:-https://sourceforge.net/projects/boost/files/boost}
BOOST_URL=${BOOST_URL_BASE}/${BOOST_VERSION}/${BOOST_TAR}/download

APK_ROOT=${APK_ROOT:-${BUILD_ROOT}/xmrwallet}
XMRWALLET_GIT_URL=${XMRWALLET_GIT_URL:-https://github.com/m2049r/xmrwallet.git}

declare -a BUILD_ARCHS=("arm" "arm64" "x86" "x86_64")
declare -a BUILD_PACKAGES=("boost" "openssl" "monero")
declare -a BUILD_ACTIONS=("download" "boost" "openssl" "monero" "apk" "checksum" "verify")
# release or debug
BUILD_TYPE=${BUILD_TYPE:-release}
BUILD_CLEAN=${BUILD_CLEAN:-1}
BUILD_CLEAN_ARTIFACT=${BUILD_CLEAN_ARTIFACT:-1}


die() {
  echo "$@"
  exit 1
}


download() {
  # Download files
  # Fetch all remote files that are required for the build to avoid breaking
  # the build when the remotes are temporary down.
  # Check the sha256sum

  if [ "${BUILD_CLEAN}" == "1" ]; then
    rm -rf "${BUILD_ROOT}/distfiles"
  fi
  mkdir -p "${BUILD_ROOT}/distfiles"
  pushd "${BUILD_ROOT}/distfiles"

  test -f "${OPENSSL_TAR}" \
    || wget "${OPENSSL_URL}" -O "${OPENSSL_TAR}" \
      || die "cannot get ${OPENSSL_URL}"
  checksum "${OPENSSL_TAR}" "${OPENSSL_TAR_SHA256SUM}"
  test -f "${BOOST_TAR}" \
    || wget "${BOOST_URL}" -O "${BOOST_TAR}" \
      || die "cannot get ${BOOST_URL}"
  checksum "${BOOST_TAR}" "${BOOST_TAR_SHA256SUM}"
  test -d android-openssl \
    && (cd android-openssl && git pull \
      || die "android-openssl could not be updated") \
    || git clone "${ANDROID_OPENSSL_GIT_URL}" \
    || die "android-openssl could not be cloned"

  # monero with the (master) submodules in the MONERO_GIT_BRANCH
  test -d monero \
    && (cd monero && git checkout master && git pull \
      || die "monero could not be updated") \
    || git clone --recursive "${MONERO_GIT_URL}" \
    || die "monero could not be cloned"
  pushd monero
    if [ "${MONERO_GIT_BRANCH}" == "latest" ]; then
      MONERO_GIT_BRANCH=$( \
        git branch -a --sort=-committerdate \
        | head -1 | tr -d '[:space:]')
    fi
    git checkout "${MONERO_GIT_BRANCH}"
    git pull
    git submodule init \
      || die "monero submodules could not be initialized"
    git submodule update \
      || die "monero submodules could not be updated"
  popd # monero

  # xmrwallet for apk
  test -d xmrwallet \
    && (cd xmrwallet && git pull || die "xmrwallet could not be updated") \
    || git clone "${XMRWALLET_GIT_URL}" \
    || die "xmrwallet could not be cloned"
  popd
}


checksum() {
  # Checksum
  # check the given filename has the given sha256sum, die when not the same
  FILENAME="$1"
  SHA256SUM="$2"
  echo -n "${SHA256SUM}  ${FILENAME}" \
    | sha256sum -c - \
    || die "checksum for ${FILENAME} not expected:" \
      " is $(sha256sum "${FILENAME}") instead of ${SHA256SUM}"
}


build_openssl () {
  # Build OpenSSL
  # Best is to compile openssl from sources.
  # Copying from your phone or elsewhere (don't!) ends up in misery.
  if [ "${BUILD_CLEAN_ARTIFACT}" == "1" ]; then
    rm -rf "${ARTIFACT_ROOT}/openssl"
  fi
  if [ "${BUILD_CLEAN}" == "1" ]; then
    rm -rf "${BUILD_ROOT}/android-openssl"
  fi
  pushd "${BUILD_ROOT}"
  git clone "${BUILD_ROOT}/distfiles/android-openssl"
  pushd "${BUILD_ROOT}/android-openssl"

  for arch in "${BUILD_ARCHS[@]}"; do

    # inspired by ./build-all-arch.sh in android-openssl
    arch_exports "${arch}"

    # shellcheck disable=SC1091
    source ./setenv-android-mod.sh
    echo "CROSS COMPILE ENV : ${CROSS_COMPILE}"

    # ensure that a clean tar was used for all archs. do not trust 'make clean'
    rm -rf "openssl-OpenSSL_${OPENSSL_VERSION_}"
    tar xzf "${BUILD_ROOT}/distfiles/${OPENSSL_TAR}" \
      || die "cannot unpack ${OPENSSL_TAR}"
    pushd "openssl-OpenSSL_${OPENSSL_VERSION_}"

    xCFLAGS="-DSHARED_EXTENSION=.so -fPIC"
    xCFLAGS+=" -DOPENSSL_PIC -DDSO_DLFCN -DHAVE_DLFCN_H"
    xCFLAGS+=" -mandroid -I${ANDROID_NDK_ROOT}/sysroot/usr/include"
    xCFLAGS+=" -I${ANDROID_NDK_ROOT}/sysroot/usr/include/${_ANDROID_EABI_INC}"
    xCFLAGS+=" -I${ANDROID_DEV}/include -B${ANDROID_DEV}/${xLIB}"
    xCFLAGS+=" -O3 -fomit-frame-pointer"
    xCFLAGS+=" -Wall"

    # shellcheck disable=SC2086
    ./Configure \
      --prefix="${ARTIFACT_ROOT}/openssl/${arch}" \
      shared no-threads no-asm no-zlib no-ssl2 no-ssl3 no-comp no-hw no-engine \
      -D__ANDROID_API__=21 \
      ${configure_platform} ${xCFLAGS} \
      || die "could not configure openssl for ${arch}"

    # patch SONAME
    perl -pi -e 's/SHLIB_EXT=\.so\.\$\(SHLIB_MAJOR\)\.\$\(SHLIB_MINOR\)/SHLIB_EXT=\.so/g' \
      Makefile
    perl -pi -e 's/SHARED_LIBS_LINK_EXTS=\.so\.\$\(SHLIB_MAJOR\) \.so//g' \
      Makefile
    # quote injection for proper SONAME
    perl -pi -e 's/SHLIB_MAJOR=1/SHLIB_MAJOR=`/g' \
      Makefile
    perl -pi -e 's/SHLIB_MINOR=0.0/SHLIB_MINOR=`/g' \
      Makefile

    make clean
    PATH="/opt/android/tool/${arch}/bin/:${PATH}" \
    make depend \
      || die "could not make depend for openssl ${arch}."
    PATH="/opt/android/tool/${arch}/bin/:${PATH}" \
    make all \
      || die "could not make all for openssl ${arch}."

    if [ "${OPENSSL_VERSION#*1.1.0}" != "${OPENSSL_VERSION}" ]; then
      # OpenSSL 1.1.0 uses DESTDIR
      DESTDIR="${ARTIFACT_ROOT}/openssl/${arch}" \
      INSTALLTOP=/ \
      MANDIR=/tmp \
        make install_sw \
        || die "could not install openssl ${arch}"
    else
      # OpenSSL 1.0.1 uses INSTALL_PREFX
      INSTALL_PREFIX="${ARTIFACT_ROOT}/openssl/${arch}" \
      INSTALL_TOP=/ \
      MANDIR=/tmp \
        make install_sw \
        || die "could not install openssl ${arch}"
    fi
    popd # openssl-${VERSION}

    clean_archives "${ARTIFACT_ROOT}/openssl/${arch}/lib"
  done
  popd # /var/src/android-openssl
  popd # /var/src
  if [ "${BUILD_CLEAN}" == "1" ]; then
    rm -rf "${BUILD_ROOT}/android-openssl"
  fi
}


build_boost() {
  # Build Boost
  if [ "${BUILD_CLEAN_ARTIFACT}" == "1" ]; then
    rm -rf "${ARTIFACT_ROOT}/boost"
  fi
  if [ "${BUILD_CLEAN}" == "1" ]; then
    rm -rf "${BUILD_ROOT}/boost"
  fi
  mkdir -p "${BUILD_ROOT}/boost"
  pushd "${BUILD_ROOT}/boost"
  tar xzf "${BUILD_ROOT}/distfiles/${BOOST_TAR}" \
    || die "failed to unpack boost"
  pushd "${BOOST_DIR}"
  ./bootstrap.sh

  # Comment out using ::fgetpos; & using ::fsetpos; in cstdio.

  # XXXXX this is weird... maybe use a more recent boost version?
  sed -i backup "s|using ::fgetpos;|//using ::fgetpos;|" \
    boost/compatibility/cpp_c_headers/cstdio
  sed -i backup "s|using ::fsetpos;|//using ::fsetpos;|" \
    boost/compatibility/cpp_c_headers/cstdio

  # Then build & install to ${ARTIFACT_ROOT}/boost/${arch}
  for arch in "${BUILD_ARCHS[@]}"; do
    arch_exports "${arch}"

    PATH="/opt/android/tool/${arch}/${target_host}/bin:/opt/android/tool/${arch}/bin:${PATH}" \
    ./b2 --build-type=minimal link=static runtime-link=static \
      --with-chrono \
      --with-date_time \
      --with-filesystem \
      --with-program_options \
      --with-regex \
      --with-serialization \
      --with-system \
      --with-thread \
      --with-locale \
      --build-dir="android-${arch}" \
      --prefix="${ARTIFACT_ROOT}/boost/${arch}"  \
      --includedir="${ARTIFACT_ROOT}/boost/${arch}/include" \
      toolset=clang threading=multi threadapi=pthread target-os=android \
      install

    clean_archives "${ARTIFACT_ROOT}/boost/${arch}/lib"
  done

  popd # BOOST_DIR
  popd # /var/src/boost
  if [ "${BUILD_CLEAN}" == "1" ]; then
    rm -rf "${BUILD_ROOT}/boost"
  fi
}


build_monero() {
  # Build Monero
  if [ "${BUILD_CLEAN_ARTIFACT}" == "1" ]; then
    rm -rf "${ARTIFACT_ROOT}/monero"
  fi
  if [ "${BUILD_CLEAN}" == "1" ]; then
    rm -rf "${BUILD_ROOT}/monero"
  fi

  pushd "${BUILD_ROOT}"
  cp -r "${BUILD_ROOT}/distfiles/monero" monero
  pushd monero
  if [ "${MONERO_GIT_BRANCH}" == "latest" ]; then
    MONERO_GIT_BRANCH=$( \
      git branch -a --sort=-committerdate \
      | head -1 | tr -d '[:space:]')
  fi
  git checkout "${MONERO_GIT_BRANCH}"
  git status

  for arch in "${BUILD_ARCHS[@]}"; do
    arch_exports "${arch}"

    # patch CMakefile to downgrade error:
    # error: the specified comparator type does not provide a const call operator
    # in easylogging++
    sed -ino_user_defined_warnings \
      's|set(WARNINGS "-Wall|set(WARNINGS "-Wno-error=user-defined-warnings -Wall|;
       s|set(WARNINGS_AS_ERRORS_FLAG "-Werror")||' \
      CMakeLists.txt

    OUTPUT_DIR="${BUILD_ROOT}/monero/build/${BUILD_TYPE}.${arch}"
    mkdir -p "${OUTPUT_DIR}"
    pushd "${OUTPUT_DIR}"


    PATH="/opt/android/tool/${arch}/${target_host}/bin:/opt/android/tool/${arch}/bin:${PATH}" \
    CC=clang CXX=clang++ cmake \
      -D BUILD_GUI_DEPS=1 -D BUILD_TESTS=OFF \
      -D ARCH="${xarch}" -D STATIC=ON -D BUILD_64="${sixtyfour}" \
      -D CMAKE_BUILD_TYPE="${BUILD_TYPE}" -D ANDROID=true \
      -D BUILD_TAG="android" \
      -D BOOST_ROOT="${ARTIFACT_ROOT}/boost/${arch}" \
      -D OPENSSL_ROOT_DIR="${ARTIFACT_ROOT}/openssl/${arch}" \
      -D CMAKE_POSITION_INDEPENDENT_CODE:BOOL=true \
      ../.. \
      || die "could not configure monero"
    make wallet_api -j4 \
      || die "could not build wallet api"

    # "install" all libs from the source-tree to the build-lib directory
    find . -path ./lib -prune -o -name '*.a' -exec cp '{}' lib \;

    # "install" all libs to the artifacts
    TARGET_LIB_DIR="${ARTIFACT_ROOT}/monero/${arch}/lib"
    rm -rf "${TARGET_LIB_DIR}"
    mkdir -p "${TARGET_LIB_DIR}"
    cp "${OUTPUT_DIR}/lib/"*.a "${TARGET_LIB_DIR}" \
      || die "could not copy *.a files to artifacts ${arch}"

    TARGET_INC_DIR="${ARTIFACT_ROOT}/monero/include"
    rm -rf "${TARGET_INC_DIR}"
    mkdir -p "${TARGET_INC_DIR}"
    cp -a ../../src/wallet/api/wallet2_api.h "${TARGET_INC_DIR}" \
      || die "could not copy wallet2_api.h files to artifacts ${arch}"
    popd # OUTPUT_DIR

    clean_archives "${ARTIFACT_ROOT}/monero/${arch}/lib"
  done
  popd # monero
  popd # /var/src/monero
  if [ "${BUILD_CLEAN}" == "1" ]; then
    rm -rf "${BUILD_ROOT}/boost"
  fi
}


record_checksum() {
  # Store a checksum file in the artifact root.
  # Remove the META-INF from the files so subsequent builds (with different
  # keystores) will not differ
  pushd "${ARTIFACT_ROOT}"
  find . -type f \( \
    -iname '*.*' \
    ! -iname '*.checksums' \
    ! -iname 'output.json' \
    ! -iname '*.apk' \
    ! -path '*/include/*' \) \
    -print0 | sort -z | xargs -r0 sha256sum \
    > "${ARTIFACT_ROOT}/package.checksums"
  for apk in $(find . -type f \( -iname '*.apk' \)); do
    unzip -d "${apk}_unzip" "${apk}" \
      || die "cannot unpack apk ${apk}"
    rm -rf "${apk}_unzip/META-INF"
    find "${apk}_unzip" -type f -print0 | sort -z | xargs -r0 sha256sum \
      | sed 's|_unzip/|/|g' \
      >> "${ARTIFACT_ROOT}/package.checksums"
    rm -rf "${apk}_unzip"
  done
  popd # PACKAGE_DIR

  pushd "${BUILD_ROOT}/distfiles"
  find . -type f \( -iname '*' ! -path '*/.git/*' \) \
    -print0 | sort -z | xargs -r0 sha256sum \
    > "${ARTIFACT_ROOT}/sources.checksums"
  popd # distfiles
}


verify() {
  # Compare checksums from artifacts and artifacts-verification
  diff "${ARTIFACT_ROOT}/package.checksums" \
    "${ARTIFACT_ROOT}-verification/package.checksums" \
    || die "package checksums do not match with verification build"
  diff "${ARTIFACT_ROOT}/sources.checksums" \
    "${ARTIFACT_ROOT}-verification/sources.checksums" \
    || die "source checksums do not match with verification build"
  echo "checksums verified ok"
}


apk() {
  # run java foo to compile apk

  if [ "${BUILD_CLEAN}" == "1" ]; then
    rm -rf "${APK_ROOT}"
  fi
  if [ "${BUILD_CLEAN_ARTIFACT}" == "1" ]; then
    rm -rf "${ARTIFACT_ROOT}/apk"
  fi
  cp -r "${BUILD_ROOT}/distfiles/xmrwallet" "${APK_ROOT}"
  pushd "${APK_ROOT}"

  # collect from artifacts
  for arch in "${BUILD_ARCHS[@]}"; do
    arch_exports "${arch}"

    for package in "${BUILD_PACKAGES[@]}"; do
      OUTPUT_DIR="${APK_ROOT}/external-libs/${package}/lib/${apk_xarch}"
      mkdir -p "${OUTPUT_DIR}"
      cp -a "${ARTIFACT_ROOT}/${package}/${arch}/lib/"*.a "${OUTPUT_DIR}"
      local package_include_dir="${ARTIFACT_ROOT}/${package}/${arch}/include"
      if [ "${package}" = "monero" ] && [ -d "${package_include_dir}" ]; then
        cp -a "${package_include_dir}" "${APK_ROOT}/external-libs/${package}"
      fi
    done
  done

  # setup sdk and ndk directory
  echo "ndk.dir=${ANDROID_NDK_ROOT}" > local.properties
  echo "sdk.dir=${ANDROID_SDK_ROOT}" >> local.properties

  declare -i NUM_RETRY=0
  declare -i MAX_RETRY=10
  while [ ${NUM_RETRY} -lt ${MAX_RETRY} ]; do
  PATH="/opt/gradle/bin:$PATH" \
  gradle build \
    && break \
    || NUM_RETRY+=1
  done
  if [ ${NUM_RETRY} -eq ${MAX_RETRY} ]; then
    die "failed to build apk"
  fi

  # copy it to ${ARTIFACT_ROOT}/apk/
  cp -r "${APK_ROOT}/app/build/outputs/apk/" "${ARTIFACT_ROOT}/apk"

  popd # APK_ROOT
  if [ "${BUILD_CLEAN}" == "1" ]; then
    rm -rf "${APK_ROOT}"
  fi
}


clean_archives() {
  # Static libraries (.a) on Unix-like systems are ar archives. Like other
  # archive formats, they contain metadata, namely timestamps, UIDs, GIDs,
  # and permissions. None are actually required for using them as libraries.
  pushd "$1"
  for archive in *.a; do
    PATH="/opt/android/tool/${arch}/${target_host}/bin" \
      objcopy --enable-deterministic-archives "${archive}"
  done
  popd # "path"
}


arch_exports() {
  # common method that exports variables based on the given architecture.
  # used to avoid this block in more than one method.
  export ldflags=""
  export xLIB=""
  case "$1" in
    "arm")
      export target_host="arm-linux-androideabi"
      export ldflags="-march=armv7-a -Wl,--fix-cortex-a8"
      export xarch="armv7-a"
      export sixtyfour=OFF
      export apk_xarch="armeabi-v7a"
      export _ANDROID_TARGET_SELECT="arch-arm"
      export _ANDROID_ARCH="arch-arm"
      export _ANDROID_EABI="arm-linux-androideabi-4.9"
      export _ANDROID_EABI_INC="arm-linux-androideabi"
      export configure_platform="android-armv7"
      ;;
    "arm64")
      export target_host="aarch64-linux-android"
      export xarch="armv8-a"
      export sixtyfour=ON
      export apk_xarch="arm64-v8a"
      export _ANDROID_TARGET_SELECT="arch-arm64-v8a"
      export _ANDROID_ARCH="arch-arm64"
      export _ANDROID_EABI="aarch64-linux-android-4.9"
      export _ANDROID_EABI_INC="aarch64-linux-android"
      export configure_platform="linux-generic64 -DB_ENDIAN"
      ;;
    "x86")
      export target_host="i686-linux-android"
      export xarch="i686"
      export sixtyfour=OFF
      export apk_xarch="x86"
      export _ANDROID_TARGET_SELECT="arch-x86"
      export _ANDROID_ARCH="arch-x86"
      export _ANDROID_EABI="x86-4.9"
      export _ANDROID_EABI_INC="i686-linux-android"
      export configure_platform="android-x86"
      ;;
    "x86_64")
      export target_host="x86_64-linux-android"
      export xarch="x86-64"
      export sixtyfour=ON
      export apk_xarch="x86_64"
      export _ANDROID_TARGET_SELECT="arch-x86_64"
      export _ANDROID_ARCH="arch-x86_64"
      export _ANDROID_EABI="x86_64-4.9"
      export _ANDROID_EABI_INC="x86_64-linux-android"
      export xLIB="/lib64"
      export configure_platform="linux-generic64"
      ;;
    *)
      die "unknown arch ${arch}"
      ;;
  esac
}


usage() {
  echo "$0 [actions] [options]"
  echo "build artifacts for xmr-wallet"
  echo "actions are:"
  echo "${BUILD_ACTIONS[*]}. default is all actions"
  echo "options are:"
  echo " --arch '${BUILD_ARCHS[*]}'"
  echo " --no-clean does not remove the build directory"
  echo " --no-clean-artifact does not remove the artifact directory"
  exit 0
}


# Parameter evaluation
if [ -n "$1" ]; then
  BUILD_ACTIONS=()
  while [ ! -z "$1" ]; do
    case "$1" in
      "--arch")
        BUILD_ARCHS=("$2")
        shift
        ;;
      "--help")
        usage
        ;;
      "--no-clean")
        BUILD_CLEAN=0
        ;;
      "--no-clean-artifact")
        BUILD_CLEAN_ARTIFACT=0
        ;;
      *)
        BUILD_ACTIONS+=("$1")
    esac
    shift
  done
fi


# Main
for action in "${BUILD_ACTIONS[@]}"; do
  case "${action}" in
    "download")
      download
    ;;
    "boost")
      build_boost
    ;;
    "openssl")
      build_openssl
    ;;
    "monero")
      build_monero
    ;;
    "apk")
      apk
    ;;
    "checksum")
      record_checksum
    ;;
    "verify")
      verify
    ;;
  esac
done
