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
import android.util.Log;

import java.util.List;
import java.util.Random;

// this is not the TransactionInfo from the API as that is owned by the TransactionHistory
// this is a POJO for the TransactionInfoAdapter
public class TransactionInfo implements Parcelable {
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
    public long confirmations;
    public List<Transfer> transfers;

    public String txKey = null;
    public String notes = null;

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
            long confirmations,
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
        this.confirmations = confirmations;
        this.transfers = transfers;
    }
    Random rnd = new Random();

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
        out.writeLong(confirmations);
        out.writeList(transfers);
        out.writeString(txKey);
        out.writeString(notes);
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
        confirmations = in.readLong();
        transfers = in.readArrayList(Transfer.class.getClassLoader());
        txKey = in.readString();
        notes = in.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

}
