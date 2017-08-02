package com.m2049r.xmrwallet.model;

public interface WalletListener {
    /**
     * moneySpent - called when money spent
     * @param txId   - transaction id
     * @param amount - tvAmount
     */
    void moneySpent(String txId, long amount);

    /**
     * moneyReceived - called when money received
     * @param txId   - transaction id
     * @param amount - tvAmount
     */
    void moneyReceived(String txId, long amount);

    /**
     * unconfirmedMoneyReceived - called when payment arrived in tx pool
     * @param txId   - transaction id
     * @param amount - tvAmount
     */
    void unconfirmedMoneyReceived(String txId, long amount);

    /**
     * newBlock      - called when new block received
     * @param height - block height
     */
    void newBlock(long height);

    /**
     * updated  - generic callback, called when any event (sent/received/block reveived/etc) happened with the wallet;
     */
    void updated();

    /**
     * refreshed - called when wallet refreshed by background thread or explicitly refreshed by calling "refresh" synchronously
     */
    void refreshed();

}
