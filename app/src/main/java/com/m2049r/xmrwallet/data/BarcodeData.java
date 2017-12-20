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

import android.net.Uri;

import com.m2049r.xmrwallet.model.Wallet;
import com.m2049r.xmrwallet.model.WalletManager;
import com.m2049r.xmrwallet.util.BitcoinAddressValidator;

import java.util.HashMap;
import java.util.Map;

import timber.log.Timber;

public class BarcodeData {
    public static final String XMR_SCHEME = "monero:";
    public static final String XMR_PAYMENTID = "tx_payment_id";
    public static final String XMR_AMOUNT = "tx_amount";

    static final String BTC_SCHEME = "bitcoin:";
    static final String BTC_AMOUNT = "amount";

    public enum Asset {
        XMR, BTC
    }

    public Asset asset = null;
    public String address = null;
    public String paymentId = null;
    public String amount = null;

    public BarcodeData(Asset asset, String address) {
        this.asset = asset;
        this.address = address;
    }

    public BarcodeData(Asset asset, String address, String amount) {
        this.asset = asset;
        this.address = address;
        this.amount = amount;
    }

    public BarcodeData(Asset asset, String address, String paymentId, String amount) {
        this.asset = asset;
        this.address = address;
        this.paymentId = paymentId;
        this.amount = amount;
    }

    static public BarcodeData fromQrCode(String qrCode) {
        // check for monero uri
        BarcodeData bcData = parseMoneroUri(qrCode);
        // check for naked monero address / integrated address
        if (bcData == null) {
            bcData = parseMoneroNaked(qrCode);
        }
        // check for btc uri
        if (bcData == null) {
            bcData = parseBitcoinUri(qrCode);
        }
        // check for naked btc addres
        if (bcData == null) {
            bcData = parseBitcoinNaked(qrCode);
        }
        return bcData;
    }

    /**
     * Parse and decode a monero scheme string. It is here because it needs to validate the data.
     *
     * @param uri String containing a monero URL
     * @return BarcodeData object or null if uri not valid
     */

    static public BarcodeData parseMoneroUri(String uri) {
        Timber.d("parseMoneroUri=%s", uri);

        if (uri == null) return null;

        if (!uri.startsWith(XMR_SCHEME)) return null;

        String noScheme = uri.substring(XMR_SCHEME.length());
        Uri monero = Uri.parse(noScheme);
        Map<String, String> parms = new HashMap<>();
        String query = monero.getQuery();
        if (query != null) {
            String[] args = query.split("&");
            for (String arg : args) {
                String[] namevalue = arg.split("=");
                if (namevalue.length == 0) {
                    continue;
                }
                parms.put(Uri.decode(namevalue[0]).toLowerCase(),
                        namevalue.length > 1 ? Uri.decode(namevalue[1]) : "");
            }
        }
        String address = monero.getPath();
        String paymentId = parms.get(XMR_PAYMENTID);
        String amount = parms.get(XMR_AMOUNT);
        if (amount != null) {
            try {
                Double.parseDouble(amount);
            } catch (NumberFormatException ex) {
                Timber.d(ex.getLocalizedMessage());
                return null; // we have an amount but its not a number!
            }
        }
        if ((paymentId != null) && !Wallet.isPaymentIdValid(paymentId)) {
            Timber.d("paymentId invalid");
            return null;
        }

        if (!Wallet.isAddressValid(address)) {
            Timber.d("address invalid");
            return null;
        }
        return new BarcodeData(Asset.XMR, address, paymentId, amount);
    }

    static public BarcodeData parseMoneroNaked(String address) {
        Timber.d("parseMoneroNaked=%s", address);

        if (address == null) return null;

        if (!Wallet.isAddressValid(address)) {
            Timber.d("address invalid");
            return null;
        }

        return new BarcodeData(Asset.XMR, address);
    }

    // bitcoin:mpQ84J43EURZHkCnXbyQ4PpNDLLBqdsMW2?amount=0.01
    static public BarcodeData parseBitcoinUri(String uri) {
        Timber.d("parseBitcoinUri=%s", uri);

        if (uri == null) return null;

        if (!uri.startsWith(BTC_SCHEME)) return null;

        String noScheme = uri.substring(BTC_SCHEME.length());
        Uri bitcoin = Uri.parse(noScheme);
        Map<String, String> parms = new HashMap<>();
        String query = bitcoin.getQuery();
        if (query != null) {
            String[] args = query.split("&");
            for (String arg : args) {
                String[] namevalue = arg.split("=");
                if (namevalue.length == 0) {
                    continue;
                }
                parms.put(Uri.decode(namevalue[0]).toLowerCase(),
                        namevalue.length > 1 ? Uri.decode(namevalue[1]) : "");
            }
        }
        String address = bitcoin.getPath();
        String amount = parms.get(BTC_AMOUNT);
        if (amount != null) {
            try {
                Double.parseDouble(amount);
            } catch (NumberFormatException ex) {
                Timber.d(ex.getLocalizedMessage());
                return null; // we have an amount but its not a number!
            }
        }
        if (!BitcoinAddressValidator.validate(address, WalletManager.getInstance().isTestNet())) {
            Timber.d("address invalid");
            return null;
        }
        return new BarcodeData(BarcodeData.Asset.BTC, address, amount);
    }

    static public BarcodeData parseBitcoinNaked(String address) {
        Timber.d("parseBitcoinNaked=%s", address);

        if (address == null) return null;

        if (!BitcoinAddressValidator.validate(address, WalletManager.getInstance().isTestNet())) {
            Timber.d("address invalid");
            return null;
        }

        return new BarcodeData(BarcodeData.Asset.BTC, address);
    }
}