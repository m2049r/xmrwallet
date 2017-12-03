/*
 * Copyright (c) 2017 m2049r
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.m2049r.xmrwallet.model;

import com.m2049r.xmrwallet.data.TxData;

import java.io.File;

public class Wallet {
    static {
        System.loadLibrary("monerujo");
    }

    static final String TAG = "Wallet";

    public String getName() {
        return new File(getPath()).getName();
    }

    private long handle = 0;
    private long listenerHandle = 0;

    Wallet(long handle) {
        this.handle = handle;
    }

    public enum Status {
        Status_Ok,
        Status_Error,
        Status_Critical
    }

    public enum ConnectionStatus {
        ConnectionStatus_Disconnected,
        ConnectionStatus_Connected,
        ConnectionStatus_WrongVersion
    }

    public native String getSeed();

    public native String getSeedLanguage();

    public native void setSeedLanguage(String language);

    public Status getStatus() {
        return Wallet.Status.values()[getStatusJ()];
    }

    private native int getStatusJ();

    public native String getErrorString();

    public native boolean setPassword(String password);

    private String address = null;

    public String getAddress() {
        if (address == null) {
            address = getAddressJ();
        }
        return address;
    }

    private native String getAddressJ();

    public native String getPath();

    public native boolean isTestNet();

//TODO virtual void hardForkInfo(uint8_t &version, uint64_t &earliest_height) const = 0;
//TODO virtual bool useForkRules(uint8_t version, int64_t early_blocks) const = 0;

    public native String getIntegratedAddress(String payment_id);

    public native String getSecretViewKey();

    public native String getSecretSpendKey();

    public boolean store() {
        return store("");
    }

    public native boolean store(String path);

    public boolean close() {
        disposePendingTransaction();
        return WalletManager.getInstance().close(this);
    }

    public native String getFilename();

    //    virtual std::string keysFilename() const = 0;
    public boolean init(long upper_transaction_size_limit) {
        return initJ(WalletManager.getInstance().getDaemonAddress(), upper_transaction_size_limit,
                WalletManager.getInstance().getDaemonUsername(),
                WalletManager.getInstance().getDaemonPassword());
    }

    private native boolean initJ(String daemon_address, long upper_transaction_size_limit,
                                 String daemon_username, String daemon_password);

//    virtual bool createWatchOnly(const std::string &path, const std::string &password, const std::string &language) const = 0;
//    virtual void setRefreshFromBlockHeight(uint64_t refresh_from_block_height) = 0;
//    virtual void setRecoveringFromSeed(bool recoveringFromSeed) = 0;
//    virtual bool connectToDaemon() = 0;

    public ConnectionStatus getConnectionStatus() {
        int s = getConnectionStatusJ();
        return Wallet.ConnectionStatus.values()[s];
    }

    private native int getConnectionStatusJ();

//TODO virtual void setTrustedDaemon(bool arg) = 0;
//TODO virtual bool trustedDaemon() const = 0;

    public native long getBalance();

    public native long getUnlockedBalance();

    public native boolean isWatchOnly();

    public native long getBlockChainHeight();

    public native long getApproximateBlockChainHeight();

    public native long getDaemonBlockChainHeight();

    public native long getDaemonBlockChainTargetHeight();

    public native boolean isSynchronized();

    public static native String getDisplayAmount(long amount);

    public static native long getAmountFromString(String amount);

    public static native long getAmountFromDouble(double amount);

    public static native String generatePaymentId();

    public static native boolean isPaymentIdValid(String payment_id);

    public static native boolean isAddressValid(String address, boolean isTestNet);

//TODO static static bool keyValid(const std::string &secret_key_string, const std::string &address_string, bool isViewKey, bool testnet, std::string &error);

    public static native String getPaymentIdFromAddress(String address, boolean isTestNet);

    public static native long getMaximumAllowedAmount();

    public native void startRefresh();

    public native void pauseRefresh();

    public native boolean refresh();

    public native void refreshAsync();

//TODO virtual void setAutoRefreshInterval(int millis) = 0;
//TODO virtual int autoRefreshInterval() const = 0;


    private PendingTransaction pendingTransaction = null;

    public PendingTransaction getPendingTransaction() {
        return pendingTransaction;
    }

    public void disposePendingTransaction() {
        if (pendingTransaction != null) {
            disposeTransaction(pendingTransaction);
            pendingTransaction = null;
        }
    }

    public PendingTransaction createTransaction(TxData txData) {
        return createTransaction(
                txData.getDestinationAddress(),
                txData.getPaymentId(),
                txData.getAmount(),
                txData.getMixin(),
                txData.getPriority());
    }

    public PendingTransaction createTransaction(String dst_addr, String payment_id,
                                                long amount, int mixin_count,
                                                PendingTransaction.Priority priority) {
        disposePendingTransaction();
        int _priority = priority.getValue();
        long txHandle = createTransactionJ(dst_addr, payment_id, amount, mixin_count, _priority);
        pendingTransaction = new PendingTransaction(txHandle);
        return pendingTransaction;
    }

    private native long createTransactionJ(String dst_addr, String payment_id,
                                           long amount, int mixin_count,
                                           int priority);


    public PendingTransaction createSweepUnmixableTransaction() {
        disposePendingTransaction();
        long txHandle = createSweepUnmixableTransactionJ();
        pendingTransaction = new PendingTransaction(txHandle);
        return pendingTransaction;
    }

    private native long createSweepUnmixableTransactionJ();

//virtual UnsignedTransaction * loadUnsignedTx(const std::string &unsigned_filename) = 0;
//virtual bool submitTransaction(const std::string &fileName) = 0;

    public native void disposeTransaction(PendingTransaction pendingTransaction);

//virtual bool exportKeyImages(const std::string &filename) = 0;
//virtual bool importKeyImages(const std::string &filename) = 0;


//virtual TransactionHistory * history() const = 0;

    private TransactionHistory history = null;

    public TransactionHistory getHistory() {
        if (history == null) {
            history = new TransactionHistory(getHistoryJ());
        }
        return history;
    }

    private native long getHistoryJ();

//virtual AddressBook * addressBook() const = 0;
//virtual void setListener(WalletListener *) = 0;

    private native long setListenerJ(WalletListener listener);

    public void setListener(WalletListener listener) {
        this.listenerHandle = setListenerJ(listener);
    }

    public native int getDefaultMixin();

    public native void setDefaultMixin(int mixin);

    public native boolean setUserNote(String txid, String note);

    public native String getUserNote(String txid);

    public native String getTxKey(String txid);

//virtual std::string signMessage(const std::string &message) = 0;
//virtual bool verifySignedMessage(const std::string &message, const std::string &addres, const std::string &signature) const = 0;

//virtual bool parse_uri(const std::string &uri, std::string &address, std::string &payment_id, uint64_t &tvAmount, std::string &tx_description, std::string &recipient_name, std::vector<std::string> &unknown_parameters, std::string &error) = 0;
//virtual bool rescanSpent() = 0;


}
