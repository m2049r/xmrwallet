# External lib build

## Requirements:

1. Docker

2. `make` (sudo apt install make, little tricky to get it on Windows, https://stackoverflow.com/questions/32127524/how-to-install-and-use-make-in-windows)

3. Huge amount of RAM and free disk space

## Building:

1. Clone https://github.com/m2049r/monero repo.

2. Change repo to correct branch (example: `git checkout release-v0.17.1.9-monerujo`).

3. Update submodules: `git submodule update --init --force`.

4. Then go to folder with xmrwallet repo, then external-libs. Here you need to create symbol link to `monero` folder:

Linux: `ln -s ~/monero ~/xmrwallet/external-libs/monero`

Windows: `mklink /D "C:\Users\<USERNAME>\xmrwallet\external-libs\monero" "C:\Users\<USERNAME>\monero"`

5. Start Docker and then run `make` in `external-libs` folder. It will fail at end on Windows, but if `wallet2_api.h` exists in `include` folder, the build was successful. 
