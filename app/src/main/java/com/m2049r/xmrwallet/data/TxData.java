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

package com.m2049r.xmrwallet.data;

import android.os.Parcel;
import android.os.Parcelable;

import com.m2049r.xmrwallet.model.PendingTransaction;

// https://stackoverflow.com/questions/2139134/how-to-send-an-object-from-one-android-activity-to-another-using-intents
public class TxData implements Parcelable {

    public TxData(TxData txData) {
        this.dst_addr = txData.dst_addr;
        this.paymentId = txData.paymentId;
        this.amount = txData.amount;
        this.mixin = txData.mixin;
        this.priority = txData.priority;
    }

    public TxData(String dst_addr,
                  String paymentId,
                  long amount,
                  int mixin,
                  PendingTransaction.Priority priority) {
        this.dst_addr = dst_addr;
        this.paymentId = paymentId;
        this.amount = amount;
        this.mixin = mixin;
        this.priority = priority;
    }

    public long getFee() {
        return 0L;
    }

    public String getDestinationAddress() {
        return dst_addr;
    }

    public String getPaymentId() {
        return paymentId;
    }

    public long getAmount() {
        return amount;
    }

    public int getMixin() {
        return mixin;
    }

    public PendingTransaction.Priority getPriority() {
        return priority;
    }

    final private String dst_addr;
    final private String paymentId;
    final private long amount;
    final private int mixin;
    final private PendingTransaction.Priority priority;

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeString(dst_addr);
        out.writeString(paymentId);
        out.writeLong(amount);
        out.writeInt(mixin);
        out.writeInt(priority.getValue());
    }

    // this is used to regenerate your object. All Parcelables must have a CREATOR that implements these two methods
    public static final Parcelable.Creator<TxData> CREATOR = new Parcelable.Creator<TxData>() {
        public TxData createFromParcel(Parcel in) {
            return new TxData(in);
        }

        public TxData[] newArray(int size) {
            return new TxData[size];
        }
    };

    private TxData(Parcel in) {
        dst_addr = in.readString();
        paymentId = in.readString();
        amount = in.readLong();
        mixin = in.readInt();
        priority = PendingTransaction.Priority.fromInteger(in.readInt());

    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("dst_addr:");
        sb.append(dst_addr);
        sb.append(",paymentId:");
        sb.append(paymentId);
        sb.append(",amount:");
        sb.append(amount);
        sb.append(",mixin:");
        sb.append(mixin);
        sb.append(",priority:");
        sb.append(String.valueOf(priority));
        return sb.toString();
    }
}
