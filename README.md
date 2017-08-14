# Monerujo
Another Android Monero Wallet

### QUICKSTART
- Download APK (Release) and install it
- Copy over synced wallet (all three files) onto sdcard in directory Monerujo (created first time app is started)
- Start app (again)
- see the [FAQ](doc/FAQ.md)

### Disclaimer
This is my first serious Android App.

### Random Notes
- Based off monero v0.10.3.1 with pull requests #2238, #2239 and #2289 applied => so can be used in mainnet!
- currently only android32
- currently only use is checking incoming/outgoing transactions
- works in testnet & mainnet (please use your own daemons)
- takes forever to sync on mainnet (even with own daemon) - due to 32-bit architecture
- use your own daemon - it's easy
- screen stays on until first sync is complete
- saves wallet only on first sync
- Monerujo means "Monero Wallet" according to https://www.reddit.com/r/Monero/comments/3exy7t/esperanto_corner/

### TODO
- make it pretty
- adjust layout so we can use bigger font sizes (maybe show only 5 decimal places instead of 12 in main view)
- review visibility of methods/classes
- sensible error dialogs (e.g. when no write permissions granted) instead of just crashing on purpose
- spend monero - not so difficult with wallet api
- check licenses of included libraries; License Dialog
- ~~provide detailed build instructions for third party binaries~~
- ~~sensible loading/saving progress bars instead of just freezing up~~
- ~~figure out how to make it all flow better (loading/saving takes forever and does not run in background)~~
- ~~currently loading in background thread produces segfaults in JNI~~

### Issues
- ~~screen rotation crashes the app~~
- ~~turning the display off/on during sync stops sync~~

### HOW TO BUILD
No need to build. Binaries are included:

- openssl-1.0.2l
- monero-v0.10.3.1 + pull requests #2238, #2239 and #2289
- boost_1_64_0

If you want to build them yourself (recommended) check out [the instructions](doc/BUILDING-external-libs.md)

Then, fire up Android Studio and build the APK.
