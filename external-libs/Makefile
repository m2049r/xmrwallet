#.PHONY: f-droid-sign f-droid-clean \
#build-external-libs use-prebuilt-external-libs \
#toolchain openssl boost wownero collect \
#clean-external-libs \
#f-droid-sign f-droid-clean \
#gradle-release gradle-build gradle-sign gradle-clean \
#apk-install remove-exif

#all: build-external-libs

#build-external-libs: clean-external-libs collect

all: monero

monero: toolchain libsodium openssl boost monero_dl openssl_sysroot
	script/monero-build.sh

monero_dl:
	script/monero-fetch.sh
#	script/monero-patch.sh

toolchain:
	script/toolchain-build.sh

openssl: toolchain openssl_dl
	script/openssl-build.sh

openssl_sysroot:
	script/openssl-install.sh

openssl_dl:
	script/openssl-fetch.sh
	script/openssl-patch.sh

boost: toolchain boost_dl
	script/boost-build.sh

boost_dl:
	script/boost-fetch.sh

libsodium: toolchain libsodium_dl
	script/libsodium-build.sh

libsodium_dl:
	script/libsodium-fetch.sh

install: all
	script/install.sh

clean:
	script/clean.sh

distclean: clean
	find monero    -type f -a ! -name ".gitignore" -a ! -name "wallet2_api.h" -exec rm {} \;
	find boost     -type f -a ! -name ".gitignore" -exec rm {} \;
	find libsodium -type f -a ! -name ".gitignore" -exec rm {} \;
	find openssl   -type f -a ! -name ".gitignore" -exec rm {} \;

archive: libsodium openssl boost monero
	echo "Packing external-libs"
	tar czfv libsodium openssl boost monero external-libs.tgz


# dev shortcut

# faster build for testing f-droid release
use-prebuilt-external-libs:
	script/build-external-libs/use-archive.sh

wownero-unsafe:
	script/build-external-libs/fetch-wownero.sh
	script/build-external-libs/patch-wownero.sh
	script/build-external-libs/build-wownero.sh




fdroid_apk_path := vendor/fdroiddata/unsigned
app_id := com.wownero.wownerujo

gradle_apk_path := app/build/outputs/apk/release
gradle_app_name := wownerujo-${gradle_app_version}_universal



f-droid-sign:
	zipalign -v -p 4 \
$(fdroid_apk_path)/$(app_id)_${app_version}.apk \
$(fdroid_apk_path)/$(app_id)_${app_version}-aligned.apk

	apksigner sign --ks ${release_key} \
--out $(fdroid_apk_path)/$(app_id)_${app_version}-release.apk \
$(fdroid_apk_path)/$(app_id)_${app_version}-aligned.apk

f-droid-clean:
	@rm -f $(fdroid_apk_path)/$(app_id)_${app_version}-aligned.apk
	@rm -f $(fdroid_apk_path)/$(app_id)_${app_version}-release.apk

gradle-release: gradle-build gradle-sign

gradle-build:
	./gradlew assembleRelease

gradle-sign: gradle-clean
	zipalign -v -p 4 \
$(gradle_apk_path)/$(gradle_app_name).apk \
$(gradle_apk_path)/$(gradle_app_name)-aligned.apk

	apksigner sign --ks ${release_key} \
--out $(gradle_apk_path)/$(gradle_app_name)-release.apk \
$(gradle_apk_path)/$(gradle_app_name)-aligned.apk

gradle-clean:
	@rm -f $(gradle_apk_path)/$(gradle_app_name)-aligned.apk
	@rm -f $(gradle_apk_path)/$(gradle_app_name)-release.apk

apk-install:
	adb install -r ./app/build/outputs/apk/release/wownerujo-$(gradle_app_version)_universal-release.apk


remove-exif:
	exiftool -all= `find app/ -name '*.jp*g' -o -name '*.png'`
