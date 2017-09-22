# FAQ

## What is it?
- A Monero lightwallet for Android. You get to choose what remote node you want to connect to. No need to download the blockchain.

## What features does it have?

- Uses monero v0.11.0.0
- Support for Android >= 5.0 with ARM processor
- Testnet and Mainnet
- Generate new wallets
- Recover wallets form mnemonic seed or from keys
- Create Watch Only wallets from address + viewkey
- Multiple wallets
- View wallet details (address, keys, etc.)
- View transactions including details and copy to clipboard
- Spend Moneroj (handle with care on mainnet!)
- Manually import existing wallet (by copying them to the Monerujo folder)
- Background updating (make sure you exit the wallet to stop updating to save some battery)
- Access to daemon with username/password and nonstandard port
- Only 5 decimal places shown in transactions (full amount in details - click on transaction)
- All significant figures shown in balance
- QR Code scanning - make sure to *ALWAYS* verify the scanned code is what it is advertised to be!
- QR Code for receiving with conversion of XMR to USD/EUR and back through kraken API
- Backup wallets to ```.backups``` folder in main wallet folder (old backups are overwritten)
- Rename wallets
- Archive (=Backup and delete)
- 3 Default nodes + History of last 5 used nodes

## Backup / Archive says "Backup/Archive failed"
You need to have synced at least once for these functions to work as the cache file must have been created.

## I cannot select and copy the mnemonic seed
Copying anything to the clipboard on Android exposes it to any other App running. So this
is a security measure to keep your seed safe(r). 

## My storage is getting full
Newly generated wallets are stored in ```.new``` in the main wallet folder.
They are never erased (for now). You can delete this whole folder from time to time.

Also, the backup folder ```.backups``` is never automatically cleaned up.
You may want to do housekeeping manually with a file browser.

All wallet files (```testnet``` and ```mainnet```) are stored in the main ```Monerujo``` folder.
So be careful erasing stuff. One of the future releases will split the wallets and move ```testnet```
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

This depends on your installation - you could search for them in your home directory or check the settings of your current client. Places to try are `C:\Users\<YOURUSERNAME>\Documents\Monero\wallets` for Windows or `~/.bitmonero/wallet` in Linux. Or just search for `WalletName.keys`.

### What if don't have these files?

Keep calm and make a new wallet.
