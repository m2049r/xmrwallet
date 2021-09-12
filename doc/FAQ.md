# FAQ

## What is it?
- A **Monero** lightwallet for Android. You get to choose what remote node you want to connect to. No need to download the blockchain.

## What features does it have?

- Seamless XMR.TO integration - instantly pay BTC with XMR using third-party XMR.TO service
- Speaks spanish and whole lot more
- Uses Monero v0.12.2
- Support for Android >= 5.0
- Testnet and Mainnet
- Generate new wallets
- Recover wallets from mnemonic seed or from keys
- Create *Watch Only* wallets from address + viewkey
- Multiple wallets
- Support for accounts
- View wallet details (address, keys, etc.)
- View transactions including details and copy to clipboard
- Spend Moneroj (handle with care on mainnet!)
- Manually import existing wallet (by copying them to the Monerujo folder)
- Background updating (make sure you exit the wallet to stop updating to save some battery)
- Access to daemon with username/password and nonstandard port
- Only 5 decimal places shown in transactions (full amount in details - click on transaction)
- All significant figures shown in balance
- QR Code scanning - make sure to *ALWAYS* verify the scanned code is what it is advertised to be!
- QR Code for receiving with conversion of XMR to USD/EUR and back through Kraken API
- Backup wallets to `backups` folder in main wallet folder (old backups are overwritten)
- Rename wallets
- Archive (=Backup and delete)
- 3 Default nodes + History of last 5 used nodes

## How does the wallet get an exchange rate?
/app/src/main/java/com/m2049r/xmrwallet/util/ServiceHelper.java currently specifies to use Kraken's exchange API.

## How do I use a node with username & password?
```username:password@node.address:portnumber```

## The app always crashes as soon as I open a wallet!
Users of Zenfone MAX & Zenfone 2 Laser (possibly others) **MUST** use the armeabi-v7a APK as the arm64-v8a build uses hardware AES
functionality these models don't have. If this does not help, please report the problem.

## I am having problems with my BTC transaction
Please contact the friendly people over at http://xmr.to for support - you will need your XMR.TO Secret Key.

## My new testnet wallet does not update
Since testnet block times are highly variable, the algorithm calculating block height does not
produce good results - so in general your testnet wallet will not work out of the box.

The remedy is simple: restore from seed and the correct / current block height. Take the block number
of your first transaction or the current block height from the
[Testnet Blockchain Explorer](https://testnet.xmrchain.com/).

## I cannot select and copy the mnemonic seed
Copying anything to the clipboard on Android exposes it to any other App running. So this
is a security measure to keep your seed safe(r). 

## My storage is getting full
Newly generated wallets are stored in `.new` in the main wallet folder.
They are never erased (for now). You can delete this whole folder from time to time.

Also, the backup folder named "`backups`" (formerly `.backups`) is never automatically cleaned up.
You may want to do housekeeping manually with a file browser.

All wallet files (`testnet` and `mainnet`) are stored in the main `Monerujo` folder.
So be careful erasing stuff. One of the future releases will split the wallets and move `testnet`
 wallets out of there.

## Do you have any screenshots of what it looks like and how it works?
No, but it looks fantastic. Just check it out.

## Can I use existing wallet files?

If you want to use existing wallet files, you need to copy the wallet files from you current Monero client. These are:
```
WalletName
WalletName.address.txt
WalletName.keys
```

### From where?

This depends on your installation - you could search for them in your home directory or check the settings of your current client. Places to try are:

- Windows: `%USERPROFILE%\Documents\Monero\wallets`
- Mac: `~/Monero/wallets` (for the GUI) or `~/.bitmonero/wallets` for the daemon.
- Linux: `~/.bitmonero/wallet`

...or just search for `WalletName.keys`.

### What if don't have these files?

Keep calm and make a new wallet.

## Why does it make a 'monero' folder?
This is a new feature of monero core to share certain key images with other monero forks.

## CrAzYpass is awesome - but I don't want it!
Creating a file named `.nocrazypass` in the wallets folder will disable generation of crazypass for NEW passwords (new wallet or change password).
The content of the file is not read and is irrelevant.
Wallets with CrAzYpass will continue working normally. The currently set real wallet password can be checked in the "Show Secrets".

**NB: This feature is for test purposed only - all your XMR will be stolen if you use it!**
