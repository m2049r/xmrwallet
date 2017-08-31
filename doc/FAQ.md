# FAQ

## What features does it have?

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

## I cannot select and copy the mnemonic seed
Copying anything to the clipboard on Android exposes it to any other App running. So this
is a security measure to keep your seed safe(r). 

## I sent a transaction but it's not in my received transactions list!
Don't worry. Received transactions which are not mined yet disappear after the wallet is saved -
I blame this on the monero code. Wait for the block to be mined.

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
