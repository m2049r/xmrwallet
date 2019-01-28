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

public class TxDataBtc extends TxData {

    private String xmrtoUuid;
    private String btcAddress;
    private String bip70;
    private double btcAmount;

    public TxDataBtc() {
        super();
    }

    public TxDataBtc(TxDataBtc txDataBtc) {
        super(txDataBtc);
    }

    public String getXmrtoUuid() {
        return xmrtoUuid;
    }

    public void setXmrtoUuid(String xmrtoUuid) {
        this.xmrtoUuid = xmrtoUuid;
    }

    public String getBtcAddress() {
        return btcAddress;
    }

    public void setBtcAddress(String btcAddress) {
        this.btcAddress = btcAddress;
    }

    public String getBip70() {
        return bip70;
    }

    public void setBip70(String bip70) {
        this.bip70 = bip70;
    }

    public double getBtcAmount() {
        return btcAmount;
    }

    public void setBtcAmount(double btcAmount) {
        this.btcAmount = btcAmount;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        super.writeToParcel(out, flags);
        out.writeString(xmrtoUuid);
        out.writeString(btcAddress);
        out.writeString(bip70);
        out.writeDouble(btcAmount);
    }

    // this is used to regenerate your object. All Parcelables must have a CREATOR that implements these two methods
    public static final Creator<TxDataBtc> CREATOR = new Creator<TxDataBtc>() {
        public TxDataBtc createFromParcel(Parcel in) {
            return new TxDataBtc(in);
        }

        public TxDataBtc[] newArray(int size) {
            return new TxDataBtc[size];
        }
    };

    protected TxDataBtc(Parcel in) {
        super(in);
        xmrtoUuid = in.readString();
        btcAddress = in.readString();
        bip70 = in.readString();
        btcAmount = in.readDouble();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(",xmrtoUuid:");
        sb.append(xmrtoUuid);
        sb.append(",btcAddress:");
        sb.append(btcAddress);
        sb.append(",bip70:");
        sb.append(bip70);
        sb.append(",btcAmount:");
        sb.append(btcAmount);
        return sb.toString();
    }
}
