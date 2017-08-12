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

public class TransactionInfo {
    static {
        System.loadLibrary("monerujo");
    }

    public long handle;

    TransactionInfo(long handle) {
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
