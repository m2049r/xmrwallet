package com.m2049r.xmrwallet.model;

public class TransactionInfo {
    static {
        System.loadLibrary("monerujo");
    }

    public long handle;

    public TransactionInfo(long handle) {
        this.handle = handle;
    }

    public enum Direction {
        Direction_In,
        Direction_Out
    }

    public class Transfer {
        long amount;
        String address;

        public Transfer(long amount, String address) {
            this.amount = amount;
            this.address = address;
        }

        public long getAmount() {
            return amount;
        }

        public String getAddress() {
            return address;
        }
    }

    public String toString() {
        return getDirection() + "@" + getBlockHeight() + " " + getAmount();
    }

    public Direction getDirection() {
        return TransactionInfo.Direction.values()[getDirectionJ()];
    }

    public native int getDirectionJ();

    public native boolean isPending();

    public native boolean isFailed();

    public native long getAmount();

    public native long getFee();

    public native long getBlockHeight();

    public native long getConfirmations();

    public native String getHash();

    public native long getTimestamp();

    public native String getPaymentId();

/*
    private List<Transfer> transfers;

    public List<Transfer> getTransfers() { // not threadsafe
        if (this.transfers == null) {
            this.transfers = getTransfersJ();
        }
        return this.transfers;
    }

    private native List<Transfer> getTransfersJ();
*/

}
