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
import com.m2049r.xmrwallet.util.UserNotes;

import timber.log.Timber;

// https://stackoverflow.com/questions/2139134/how-to-send-an-object-from-one-android-activity-to-another-using-intents
public class TxData implements Parcelable {

    public TxData() {
    }

    public TxData(TxData txData) {
        this.dstAddr = txData.dstAddr;
        this.paymentId = txData.paymentId;
        this.amount = txData.amount;
        this.mixin = txData.mixin;
        this.priority = txData.priority;
    }

    public TxData(String dstAddr,
                  String paymentId,
                  long amount,
                  int mixin,
                  PendingTransaction.Priority priority) {
        this.dstAddr = dstAddr;
        this.paymentId = paymentId;
        this.amount = amount;
        this.mixin = mixin;
        this.priority = priority;
    }

    public String getDestinationAddress() {
        return dstAddr;
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

    public void setDestinationAddress(String dstAddr) {
        this.dstAddr = dstAddr;
    }

    public void setPaymentId(String paymentId) {
        this.paymentId = paymentId;
    }

    public void setAmount(long amount) {
        this.amount = amount;
    }

    public void setMixin(int mixin) {
        this.mixin = mixin;
    }

    public void setPriority(PendingTransaction.Priority priority) {
        this.priority = priority;
    }

    public UserNotes getUserNotes() {
        return userNotes;
    }

    public void setUserNotes(UserNotes userNotes) {
        this.userNotes = userNotes;
    }

    private String dstAddr;
    private String paymentId;
    private long amount;
    private int mixin;
    private PendingTransaction.Priority priority;

    private UserNotes userNotes;

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeString(dstAddr);
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

    protected TxData(Parcel in) {
        dstAddr = in.readString();
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
        sb.append("dstAddr:");
        sb.append(dstAddr);
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
