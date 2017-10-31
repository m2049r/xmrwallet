# Monerujo
Another Android Monero Wallet

### QUICKSTART
- Download the APK for the most current release [here](https://github.com/m2049r/xmrwallet/releases) and install it
- Run the App and select "Generate Wallet" to create a new wallet or recover a wallet
- Advanced users can copy over synced wallet files (all files) onto sdcard in directory Monerujo (created first time App is started)
- See the [FAQ](doc/FAQ.md)

### Disclaimer
You may lose all your Moneroj if you use this App. Be cautious when spending on the mainnet.

### Random Notes
- Based off monero v0.11.0.0 with PR #2289 applied
- currently only android32 (runs on 64-bit as well)
- works on the testnet & mainnet
- sync is slow due to 32-bit architecture
- use your own daemon - it's easy
- screen stays on until first sync is complete
- saves wallet only on first sync and when sending transactions or editing notes
- Monerujo means "Monero Wallet" according to https://www.reddit.com/r/Monero/comments/3exy7t/esperanto_corner/

### TODO
- more sensible error dialogs

### Issues / Pitfalls
- The backups folder is now called "backups" and not ".backups" - which in most file explorers was a hidden folder
- Wallets are now created directly in the "monerujo" folder, and not in the ".new" folder as before
- You may want to check the old folders with a file browsing app and delete the ".new" and ".backups" folders AFTER moving neccessary wallet files to the new locations. Or simply make new backups from within Monerujo.
- Also note, that on some devices the backups will only be visible on a PC over USB after a reboot of the device (it's an Android bug/feature)
- Created wallets on a private testnet are unusable because the restore height is set to that
of the "real" testnet.  After creating a new wallet, make a **new** one by recovering from the seed.
The official monero client shows the same behaviour.

### HOW TO BUILD
No need to build. Binaries are included:

- openssl-1.0.2l
- monero-v0.11.0.0 + pull requests #2289
- boost_1_64_0

If you want to build them yourself (recommended) check out [the instructions](doc/BUILDING-external-libs.md)

Then, fire up Android Studio and build the APK.

### Donations
- Address: 4AdkPJoxn7JCvAby9szgnt93MSEwdnxdhaASxbTBm6x5dCwmsDep2UYN4FhStDn5i11nsJbpU7oj59ahg8gXb1Mg3viqCuk
- Viewkey: b1aff2a12191723da0afbe75516f94dd8b068215f6e847d8da57aca5f1f98e0c
