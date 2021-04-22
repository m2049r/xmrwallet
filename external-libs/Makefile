FLAVOUR = aeon

all: x86_64 arm64-v8a include/wallet2_api.h VERSION

arm64-v8a: $(FLAVOUR) android64.Dockerfile
	-rm -rf arm64-v8a
	-docker container rm $(FLAVOUR)-android-arm64 -f
	docker build -f android64.Dockerfile -t $(FLAVOUR)-android-arm64 $(FLAVOUR)
	docker create -it --name $(FLAVOUR)-android-arm64 $(FLAVOUR)-android-arm64 bash
	docker cp $(FLAVOUR)-android-arm64:/opt/android/prefix/lib/. arm64-v8a/
	docker cp $(FLAVOUR)-android-arm64:/src/build/release/lib/. arm64-v8a/$(FLAVOUR)

x86_64: $(FLAVOUR) android64_x86.Dockerfile
	-rm -rf x86_64
	-docker container rm $(FLAVOUR)-android-x86_64 -f
	docker build -f android64_x86.Dockerfile -t $(FLAVOUR)-android-x86_64 $(FLAVOUR)
	docker create -it --name $(FLAVOUR)-android-x86_64 $(FLAVOUR)-android-x86_64 bash
	docker cp $(FLAVOUR)-android-x86_64:/opt/android/prefix/lib/. x86_64
	docker cp $(FLAVOUR)-android-x86_64:/src/build/release/lib/. x86_64/$(FLAVOUR)

include/wallet2_api.h: $(FLAVOUR) include $(FLAVOUR)/src/wallet/api/wallet2_api.h
	cp $(FLAVOUR)/src/wallet/api/wallet2_api.h include/wallet2_api.h

include:
	mkdir include

VERSION:
	echo MONERUJO_$(FLAVOUR) `git -C . branch | grep "^\*" | sed 's/^..//'` with $(FLAVOUR) `git -C $(FLAVOUR) branch | grep "^\*" | sed 's/^..//'` > VERSION

clean:
	-rm -rf arm64-v8a
	-rm -rf x86_64
	-rm -rf include
	-rm -f VERSION

distclean: clean
	-docker container rm $(FLAVOUR)-android-arm64 -f
	-docker container rm $(FLAVOUR)-android-x86_64 -f
	-docker image rm $(FLAVOUR)-android-arm64 -f
	-docker image rm $(FLAVOUR)-android-x86_64 -f
	-rm $(FLAVOUR)

$(FLAVOUR):
	$(error Please ln -s $(FLAVOUR))

.PHONY: all clean distclean
