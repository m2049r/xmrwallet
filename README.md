# Monerujo
Another Android Monero Wallet

### QUICKSTART
- Download APK (Release) and install it
- Run the App and select "Generate Wallet" to create a new wallet or recover a wallet
- Advanced users could copy over synced wallet files (all files) onto sdcard in directory Monerujo (created first time App is started)
- see the [FAQ](doc/FAQ.md)

### Disclaimer
You may loose all your Moneroj if you use this App.

### Random Notes
- Based off monero v0.10.3.1 with pull requests #2238, #2239 and #2289 applied => so can be used in mainnet!
- currently only android32
- ~~currently only use is checking incoming/outgoing transactions~~
- works in testnet & mainnet
- takes forever to sync due to 32-bit architecture
- use your own daemon - it's easy
- screen stays on until first sync is complete
- saves wallet only on first sync and when sending transactions or editing notes
- Monerujo means "Monero Wallet" according to https://www.reddit.com/r/Monero/comments/3exy7t/esperanto_corner/

### TODO
- wallet backup functions
- adjust layout so we can use bigger font sizes (maybe show only 5 decimal places instead of 12 in main view)
- review visibility of methods/classes
- more sensible error dialogs ~~(e.g. when no write permissions granted) instead of just crashing on purpose~~
- check licenses of included libraries; License Dialog
- ~~make it pretty~~ (decided to go with "form follows function")
- ~~spend monero - not so difficult with wallet api~~

### Issues
none :)

### HOW TO BUILD
No need to build. Binaries are included:

- openssl-1.0.2l
- monero-v0.10.3.1 + pull requests #2238, #2239 and #2289
- boost_1_64_0

If you want to build them yourself (recommended) check out [the instructions](doc/BUILDING-external-libs.md)

Then, fire up Android Studio and build the APK.
