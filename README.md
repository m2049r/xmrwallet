# Monerujo
Another Android Monero Wallet

### QUICKSTART
- Download APK (Release) and install it
- Copy over synced wallet (all three files) onto sdcard in directory Monerujo (created first time app is started)
- Start app (again)

### Disclaimer
This is my first serious Android App.

### Notes
- Monerujo means "Monero Wallet" according to https://www.reddit.com/r/Monero/comments/3exy7t/esperanto_corner/
- Special thanks to /u/gkruja for inspiration to pick this project up again
- Based off monero v0.10.3.1 with pull requests #2238 & #2239 applied => so can be used in mainnet!
- currently only android32
- sorry for my messups in github
- this is more of a proof of concept
- currently only  use is checking incoming/outgoing transactions
- works in testnet & mainnet (please use your own daemons)
- takes forever to sync on mainnet (even with own daemon)
- use your own daemon - it's easy
- screen stays on until first sync is complete

### TODO
- don't have the screen on for first sync - use IntentService with WakeLock instead?
- make it pretty
- show current block height - is that relevant?
- License Dialog
- support for right-to-left layouts
- visibility of methods/classes
- adjust layout so we can use bigger font sizes
- sensible error dialogs (e.g. when no write permissions granted) instead of just crashing on purpose
- sensible loading/saving progress bars instead of just freezing up
- spend monero - not so difficult with wallet api
- figure out how to make it all flow better (loading/saving takes forever and does not run in background)
- currently loading in background thread produces segfaults in JNI
- check licenses of included libraries

### Issues
- occasional crashes ...

### HOW TO BUILD
No need to build. Binaries are included:

- openssl-1.0.2l
- monero-v0.10.3.1 + pull requests #2238 & #2239
- boost_1_64_0

If you want to build - fire up Android Studio and build. Also you can rebuild all of the above.
