#!/usr/bin/env make

.PHONY: clean all

clean:
	rm -rf artifacts || true
	rm -rf artifacts-verification || true
	rm -rf distfiles || true
	rm .docker_image || true

all: distfiles artifacts/packages.checksum

distfiles: .docker_image
	docker run --rm -it \
	  -v $(shell pwd)/distfiles:/var/src/distfiles \
	  xmrwallet_build /bin/bash /usr/local/bin/build-artifacts.sh \
	    download

artifacts/openssl: distfiles
	mkdir -p artifacts/openssl
	docker run --rm -it \
	  -v $(shell pwd)/artifacts:/var/src/artifacts \
	  -v $(shell pwd)/distfiles:/var/src/distfiles:ro \
	  xmrwallet_build /bin/bash /usr/local/bin/build-artifacts.sh \
	    openssl

artifacts/boost: distfiles
	mkdir -p artifacts/boost
	docker run --rm -it \
	  -v $(shell pwd)/artifacts:/var/src/artifacts \
	  -v $(shell pwd)/distfiles:/var/src/distfiles:ro \
	  xmrwallet_build /bin/bash /usr/local/bin/build-artifacts.sh \
	    boost

artifacts/monero: artifacts/boost artifacts/openssl
	mkdir -p artifacts/monero
	docker run --rm -it \
	  -v $(shell pwd)/artifacts:/var/src/artifacts \
	  -v $(shell pwd)/distfiles:/var/src/distfiles:ro \
	  xmrwallet_build /bin/bash /usr/local/bin/build-artifacts.sh \
	    monero

artifacts/apk: artifacts/monero
	mkdir -p artifacts/apk
	docker run --rm -it \
	  -v $(shell pwd)/artifacts:/var/src/artifacts \
	  -v $(shell pwd)/distfiles:/var/src/distfiles:ro \
	  xmrwallet_build /bin/bash /usr/local/bin/build-artifacts.sh \
	    apk

artifacts/packages.checksum: artifacts/apk
	docker run --rm -it \
	  -v $(shell pwd)/artifacts:/var/src/artifacts \
	  -v $(shell pwd)/distfiles:/var/src/distfiles:ro \
	  xmrwallet_build /bin/bash /usr/local/bin/build-artifacts.sh \
	    checksum

artifactsd: distfiles
	@echo "debugable build - enters the container and requires you to execute the commands"
	mkdir -p artifacts
	mkdir -p distfiles
	docker run --rm -it \
	  -v $(shell pwd)/artifacts:/var/src/artifacts \
	  -v $(shell pwd)/artifacts-verification:/var/src/artifacts-verification \
	  -v $(shell pwd)/distfiles:/var/src/distfiles \
	  -v $(shell pwd)/build-artifacts.sh:/usr/local/bin/build-artifacts.sh \
	  --entrypoint /bin/bash \
	  xmrwallet_build

artifacts-verification: distfiles artifacts/packages.checksum
	@echo "running a verification build that compares with a previous artifacts build"
	mkdir -p artifacts-verification
	mkdir -p distfiles
	docker run --rm -it \
	  -v $(shell pwd)/artifacts-verification:/var/src/artifacts \
	  -v $(shell pwd)/distfiles:/var/src/distfiles:ro \
	  xmrwallet_build /bin/bash /usr/local/bin/build-artifacts.sh \
	    openssl boost monero apk checksum

	docker run --rm -it \
	  -v $(shell pwd)/artifacts-verification:/var/src/artifacts-verification:ro \
	  -v $(shell pwd)/artifacts:/var/src/artifacts:ro \
	  xmrwallet_build /bin/bash /usr/local/bin/build-artifacts.sh \
	    verify

.docker_image: build-artifacts.sh
	docker build . -t xmrwallet_build
	touch .docker_image
