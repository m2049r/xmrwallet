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

public class Transfer implements Parcelable {
    public long amount;
    public String address;

    public Transfer(long amount, String address) {
        this.amount = amount;
        this.address = address;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeLong(amount);
        out.writeString(address);
    }

    public static final Parcelable.Creator<Transfer> CREATOR = new Parcelable.Creator<Transfer>() {
        public Transfer createFromParcel(Parcel in) {
            return new Transfer(in);
        }

        public Transfer[] newArray(int size) {
            return new Transfer[size];
        }
    };

    private Transfer(Parcel in) {
        amount = in.readLong();
        address = in.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

}