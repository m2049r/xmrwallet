# FAQ

## Do you have any screenshots of what it looks like and how it works?

### [Select Wallet](images/A-wallet_selection.png)

Here you see a list of installed wallets and an entry field at the top to enter the daemon address. To the right there is a pushbutton to change between testnet and mainnet. The entered daemon is saved and displayed according to the state of this button.

### [Wallet Password](images/B-enter_password.png)

After selecting the wallet, the password is entered.

### [Wallet Syncing](images/C-wallet_syncing.png)

After some seconds the wallet is displyed with it's last known state and synced to the network. If it says "disconnected" or takes forever to show this screen then the entered daemon is wrong or unreachable. (Yes, I need to check the daemon availability on the login screen ...) Go back, and check that.

During syncing, the number of remaining blocks is displayed - when this reaches 0 the blockchain is fully synced.

The balance is updated during sync.

### [Wallet Synced](images/D-wallet_synced.png)

When the blockchain is synced, the screen shows "Synced" and the current blockchain height. When new blocks become available they are also synced and new transactions are displayed.

## What features does it have?

That's about it. Select a wallet and show the balance. Behind the scenes it keeps in sync with the blockchain while the app is open. So currently it is a view only wallet. You can use it to monitor your wallets on the go.

In future it will have the possibility of executing transactions. And generating wallets. Technically, it can generate wallets now, but they are pointless since you need another client to make transactions anyway - so you can make the wallets on the other client.

## What files do I need to copy?

You need to copy the wallet files from you current Monero client. These are:
```
WalletName
WalletName.address.txt
WalletName.keys
```

### From where?

This depends on your installation - you could search for them in your home directory or check the settings of your current client. Places to try are `C:\Users\<YOURUSERNAME>\Documents\Monero\wallets` for Windows or `~/.bitmonero/wallet` in Linux. Or just search for `WalletName.keys`.

### What if don't have these files?

As this is a view-only App right now, you need another client for generating wallets and sending transactions. This will change soon<sup>TM</sup>.
