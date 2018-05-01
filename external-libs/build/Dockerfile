###############################
# Dockerfile to build xmrwallet
###############################
FROM ubuntu:18.04
MAINTAINER miner2049er@women-at-work.org

ENV NDK=android-ndk-r16b \
    NDK_SUM=bcdea4f5353773b2ffa85b5a9a2ae35544ce88ec5b507301d8cf6a76b765d901 \
    SDK_TOOL_FILE=sdk-tools-linux-3859397.zip \
    SDK_TOOL_SUM=444e22ce8ca0f67353bda4b85175ed3731cae3ffa695ca18119cbacef1c1bea0 \
    GRADLE_VERSION=4.7 \
    GRADLE_SUM=fca5087dc8b50c64655c000989635664a73b11b9bd3703c7d6cabd31b7dcdb04 \
    GRADLE_HOME=/opt/gradle
ENV NDK_FILE=${NDK}-linux-x86_64.zip \
    GRADLE_FILE=gradle-${GRADLE_VERSION}-bin.zip

# prepare ubuntu environment
RUN apt-get update \
 && apt-get --no-install-recommends --yes install \
    ca-certificates \
    cmake \
    g++ \
    make \
    pkg-config \
    graphviz \
    doxygen \
    git \
    curl \
    libtool-bin \
    autoconf \
    build-essential cmake tofrodos \
    wget unzip python \
    openjdk-8-jdk

# install android ndk
RUN mkdir /opt/android \
 && cd /opt/android \
 && wget https://dl.google.com/android/repository/${NDK_FILE} \
 && echo "${NDK_SUM}  ${NDK_FILE}" > /tmp/ndk_sum_signed \
 && sha256sum -c /tmp/ndk_sum_signed \
 && unzip "${NDK_FILE}" \
 && rm -rf "${NDK_FILE}"

# prepare standalone toolchain
RUN cd /opt/android \
 && /opt/android/${NDK}/build/tools/make_standalone_toolchain.py \
    --api 21 --stl=libc++ --arch arm --install-dir /opt/android/tool/arm \
 && /opt/android/${NDK}/build/tools/make_standalone_toolchain.py \
    --api 21 --stl=libc++ --arch arm64 --install-dir /opt/android/tool/arm64 \
 && /opt/android/${NDK}/build/tools/make_standalone_toolchain.py \
    --api 21 --stl=libc++ --arch x86 --install-dir /opt/android/tool/x86 \
 && /opt/android/${NDK}/build/tools/make_standalone_toolchain.py \
    --api 21 --stl=libc++ --arch x86_64 --install-dir /opt/android/tool/x86_64

# install android sdk CLI tool
RUN mkdir /opt/android/sdk \
 && cd /opt/android/sdk \
 && wget https://dl.google.com/android/repository/${SDK_TOOL_FILE} \
 && echo "${SDK_TOOL_SUM}  ${SDK_TOOL_FILE}" > /tmp/sdk_tool_sum_signed \
 && sha256sum -c /tmp/sdk_tool_sum_signed \
 && unzip "${SDK_TOOL_FILE}" \
 && rm -rf "${SDK_TOOL_FILE}"

# accept sdk license
RUN yes | /opt/android/sdk/tools/bin/sdkmanager --licenses

# install recent gradle
RUN cd /opt/ \
 && wget https://services.gradle.org/distributions/${GRADLE_FILE} \
 && echo "${GRADLE_SUM}  ${GRADLE_FILE}" > /tmp/gradle_sum_signed \
 && sha256sum -c /tmp/gradle_sum_signed \
 && unzip "${GRADLE_FILE}" \
 && rm -rf "${GRADLE_FILE}" \
 && mv "gradle-${GRADLE_VERSION}" "gradle"

# prepare reproduceable build container
ADD build-artifacts.sh /usr/local/bin/build-artifacts.sh

RUN chmod +x /usr/local/bin/build-artifacts.sh

ENTRYPOINT ["/usr/local/bin/build-artifacts.sh"]
