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

import android.os.Parcel;
import android.os.Parcelable;

import java.util.List;

// this is not the TransactionInfo from the API as that is owned by the TransactionHistory
// this is a POJO for the TransactionInfoAdapter
public class TransactionInfo implements Parcelable, Comparable<TransactionInfo> {
    static final String TAG = "TransactionInfo";

    public enum Direction {
        Direction_In(0),
        Direction_Out(1);

        public static Direction fromInteger(int n) {
            switch (n) {
                case 0:
                    return Direction_In;
                case 1:
                    return Direction_Out;
            }
            return null;
        }

        public int getValue() {
            return value;
        }

        private int value;

        Direction(int value) {
            this.value = value;
        }
    }

    public Direction direction;
    public boolean isPending;
    public boolean isFailed;
    public long amount;
    public long fee;
    public long blockheight;
    public String hash;
    public long timestamp;
    public String paymentId;
    public int account;
    public int subaddress;
    public long confirmations;
    public String subaddressLabel;
    public List<Transfer> transfers;

    public String txKey = null;
    public String notes = null;
    public String address = null;

    public TransactionInfo(
            int direction,
            boolean isPending,
            boolean isFailed,
            long amount,
            long fee,
            long blockheight,
            String hash,
            long timestamp,
            String paymentId,
            int account,
            int subaddress,
            long confirmations,
            String subaddressLabel,
            List<Transfer> transfers) {
        this.direction = Direction.values()[direction];
        this.isPending = isPending;
        this.isFailed = isFailed;
        this.amount = amount;
        this.fee = fee;
        this.blockheight = blockheight;
        this.hash = hash;
        this.timestamp = timestamp;
        this.paymentId = paymentId;
        this.account = account;
        this.subaddress = subaddress;
        this.confirmations = confirmations;
        this.subaddressLabel = subaddressLabel;
        this.transfers = transfers;
    }

    public String toString() {
        return direction + "@" + blockheight + " " + amount;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeInt(direction.getValue());
        out.writeByte((byte) (isPending ? 1 : 0));
        out.writeByte((byte) (isFailed ? 1 : 0));
        out.writeLong(amount);
        out.writeLong(fee);
        out.writeLong(blockheight);
        out.writeString(hash);
        out.writeLong(timestamp);
        out.writeString(paymentId);
        out.writeInt(account);
        out.writeInt(subaddress);
        out.writeLong(confirmations);
        out.writeString(subaddressLabel);
        out.writeList(transfers);
        out.writeString(txKey);
        out.writeString(notes);
        out.writeString(address);
    }

    public static final Parcelable.Creator<TransactionInfo> CREATOR = new Parcelable.Creator<TransactionInfo>() {
        public TransactionInfo createFromParcel(Parcel in) {
            return new TransactionInfo(in);
        }

        public TransactionInfo[] newArray(int size) {
            return new TransactionInfo[size];
        }
    };

    private TransactionInfo(Parcel in) {
        direction = Direction.fromInteger(in.readInt());
        isPending = in.readByte() != 0;
        isFailed = in.readByte() != 0;
        amount = in.readLong();
        fee = in.readLong();
        blockheight = in.readLong();
        hash = in.readString();
        timestamp = in.readLong();
        paymentId = in.readString();
        account = in.readInt();
        subaddress = in.readInt();
        confirmations = in.readLong();
        subaddressLabel = in.readString();
        transfers = in.readArrayList(Transfer.class.getClassLoader());
        txKey = in.readString();
        notes = in.readString();
        address = in.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public int compareTo(TransactionInfo another) {
        long b1 = this.timestamp;
        long b2 = another.timestamp;
        if (b1 > b2) {
            return -1;
        } else if (b1 < b2) {
            return 1;
        } else {
            return this.hash.compareTo(another.hash);
        }
    }
}