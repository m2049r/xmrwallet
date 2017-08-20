# FAQ

## What features does it have?

- Testnet and Mainnet
- Generate new wallets
- Recover wallets form nmemonic seed or from keys
- Create Watch Only wallets from address + viewkey
- Multiple wallets
- View wallet details (address, keys, etc.)
- View transactions inlcuding details and copy to clipboard
- Spend Moneroj (only on testnet - someone will loose money and want to blame me. No thanks!)
- Manually import existing wallet (by copying them to the Monerujo folder)
- Background updating (make sure you exit the wallet to stop updating to save some battery)

## I sent a transaction but it's not in my transactions list!
Don't worry. Received transactions which are not mined yet disappear after the wallet is saved -
I blame this on the monero code. Wait for the block to be mined.

## Do you have any screenshots of what it looks like and how it works?
Here are some old screenshots with a bit of description.
I will be removing them soon. Just check out the App.

#### [Select Wallet](images/A-wallet_selection.png)

Here you see a list of installed wallets and an entry field at the top to enter the daemon address. To the right there is a pushbutton to change between testnet and mainnet. The entered daemon is saved and displayed according to the state of this button.

#### [Wallet Password](images/B-enter_password.png)

After selecting the wallet, the password is entered.

#### [Wallet Syncing](images/C-wallet_syncing.png)

After some seconds the wallet is displyed with it's last known state and synced to the network. If it says "disconnected" or takes forever to show this screen then the entered daemon is wrong or unreachable. (Yes, I need to check the daemon availability on the login screen ...) Go back, and check that.

During syncing, the number of remaining blocks is displayed - when this reaches 0 the blockchain is fully synced.

The balance is updated during sync.

### [Wallet Synced](images/D-wallet_synced.png)

When the blockchain is synced, the screen shows "Synced" and the current blockchain height. When new blocks become available they are also synced and new transactions are displayed.

## What files do I need to copy?

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
