# Monerujo
Another Android Monero Wallet

### QUICKSTART
- Download APK (Release) and install it [here](https://github.com/m2049r/xmrwallet/releases)
- Run the App and select "Generate Wallet" to create a new wallet or recover a wallet
- Advanced users could copy over synced wallet files (all files) onto sdcard in directory Monerujo (created first time App is started)
- see the [FAQ](doc/FAQ.md)

### Disclaimer
You may loose all your Moneroj if you use this App. Be cautious when spending on mainnet.

### Random Notes
- Based off monero v0.10.3.1 with pull requests #2238, #2239 and #2289 applied => so can be used in mainnet!
- currently only android32 (runs on 64-bit as well)
- works in testnet & mainnet
- takes forever to sync due to 32-bit architecture
- use your own daemon - it's easy
- screen stays on until first sync is complete
- saves wallet only on first sync and when sending transactions or editing notes
- Monerujo means "Monero Wallet" according to https://www.reddit.com/r/Monero/comments/3exy7t/esperanto_corner/

### TODO
- review visibility of methods/classes
- more sensible error dialogs
- check licenses of included libraries; License Dialog

### Issues
- Pending incoming transactions disappear after reload (and appear after being mined)

### HOW TO BUILD
No need to build. Binaries are included:

- openssl-1.0.2l
- monero-v0.10.3.1 + pull requests #2238, #2239 and #2289
- boost_1_64_0

If you want to build them yourself (recommended) check out [the instructions](doc/BUILDING-external-libs.md)

Then, fire up Android Studio and build the APK.

### Donations
- Address: 4AdkPJoxn7JCvAby9szgnt93MSEwdnxdhaASxbTBm6x5dCwmsDep2UYN4FhStDn5i11nsJbpU7oj59ahg8gXb1Mg3viqCuk
- Viewkey: b1aff2a12191723da0afbe75516f94dd8b068215f6e847d8da57aca5f1f98e0c
