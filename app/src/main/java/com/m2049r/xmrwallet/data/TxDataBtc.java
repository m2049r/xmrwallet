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

import androidx.annotation.NonNull;

import com.m2049r.xmrwallet.service.shift.ShiftService;
import com.m2049r.xmrwallet.service.shift.api.RequestQuote;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class TxDataBtc extends TxData {
    private ShiftService shiftService;
    private String btcSymbol; // the actual non-XMR thing we're sending
    private String xmrtoOrderId; // shown in success screen
    private String btcAddress;
    private CryptoAmount shiftAmount; // what we want to send
    private String xmrtoQueryOrderToken; // used for queryOrder API

    public TxDataBtc() {
        super();
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        super.writeToParcel(out, flags);
        out.writeString(btcSymbol);
        out.writeString(xmrtoOrderId);
        out.writeString(btcAddress);
        out.writeString(shiftAmount.getCrypto().name());
        out.writeDouble(shiftAmount.getAmount());
        out.writeString(xmrtoQueryOrderToken);
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
        btcSymbol = in.readString();
        xmrtoOrderId = in.readString();
        btcAddress = in.readString();
        shiftAmount = new CryptoAmount(Crypto.valueOf(in.readString()), in.readDouble());
        xmrtoQueryOrderToken = in.readString();
    }

    @NonNull
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("xmrtoOrderId:");
        sb.append(xmrtoOrderId);
        sb.append(",btcSymbol:");
        sb.append(btcSymbol);
        sb.append(",btcAddress:");
        sb.append(btcAddress);
        sb.append(",amount:");
        sb.append(shiftAmount);
        sb.append(",xmrtoQueryOrderToken:");
        sb.append(xmrtoQueryOrderToken);
        return sb.toString();
    }

    public boolean validateAddress(@NonNull String address) {
        final Crypto crypto = Crypto.withSymbol(btcSymbol);
        if (crypto == null) return false;
        return address.equalsIgnoreCase(btcAddress);
    }

    public double getBtcAmount() {
        return (shiftAmount.getCrypto() == Crypto.XMR) ? 0 : shiftAmount.getAmount();
    }

    public double getXmrAmount() {
        return (shiftAmount.getCrypto() == Crypto.XMR) ? shiftAmount.getAmount() : 0;
    }

    public boolean validate(RequestQuote quote) {
        if (shiftAmount.getCrypto() == Crypto.XMR) {
            return (quote.getXmrAmount() == getXmrAmount());
        } else {
            return (quote.getBtcAmount() == getBtcAmount());
        }
    }

}
