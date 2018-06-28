# MonerujoAE
The Android AEON Wallet

### QUICKSTART
- Download the APK for the most current release [here](https://github.com/monerujo-io/aeonwallet/releases) and install it
- Alternatively add our F-Droid repo https://aeon.monerujo.io/fdroid/repo with fingerpint ```F8 20 2E E7 A8 D0 2D 07 AF B1 3D 5A 14 F2 9F 39 04 8C 4D 2D B3 87 76 11 03 5E F9 EA 20 6A 55 5D``` to your F-Droid client
- Run the App and select "Generate Wallet" to create a new wallet or recover a wallet
- Advanced users can copy over synced wallet files (all files) onto sdcard in directory ```aeonwallet``` (created first time App is started)
- See the [FAQ](doc/FAQ.md)

## Translations
Help us translate Monerujo! You can find instructions [On Taiga](https://taiga.getmonero.org/project/erciccione-monero-localization/wiki/monerujo), and if you need help/support, open an issue or contact the Localization Workgroup. You can find us on the freenode channel `#monero-translations`, also relayed on [MatterMost](https://mattermost.getmonero.org/monero/channels/monero-translations), and matrix/riot.

### Disclaimer
You may lose all your AEON if you use this App.

### Random Notes
- Based off aeon v0.12.0
- works on the stagenet & mainnet
- use your own daemon - it's easy
- screen stays on until first sync is complete

### TODO
- see taiga.getmonero.org & issues on github

### Issues / Pitfalls
- Users of Zenfone MAX & Zenfone 2 Laser (possibly others) **MUST** use the armeabi-v7a APK as the arm64-v8a build uses hardware AES
functionality these models don't have.
- You should backup your wallet files in the "aeonwallet" folder periodically.
- Also note, that on some devices the backups will only be visible on a PC over USB after a reboot of the device (it's an Android bug/feature)
- Created wallets on a private testnet are unusable because the restore height is set to that
of the "real" stagenet.  After creating a new wallet, make a **new** one by recovering from the seed.
The official client shows the same behaviour.

### HOW TO BUILD
Check out [the instructions](doc/BUILDING-external-libs.md)

### Donations
- Address: WmsfCJfmd6QQ84Rfb2mD1y7ryBzvTiQ8MMngmgRjxDMCCkeuChA9B9ZRNZyQyjgH1zdQMBXeQB9vZhUwPHmLGhyo2nMoE2ARv
