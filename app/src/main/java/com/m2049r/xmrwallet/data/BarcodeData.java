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
import com.m2049r.xmrwallet.util.BitcoinAddressValidator;
import com.m2049r.xmrwallet.util.OpenAliasHelper;
import com.m2049r.xmrwallet.util.PaymentProtocolHelper;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import timber.log.Timber;

public class BarcodeData {

    public static final String XMR_SCHEME = "monero:";
    public static final String XMR_PAYMENTID = "tx_payment_id";
    public static final String XMR_AMOUNT = "tx_amount";
    public static final String XMR_DESCRIPTION = "tx_description";

    public static final String OA_XMR_ASSET = "xmr";
    public static final String OA_BTC_ASSET = "btc";

    static final String BTC_SCHEME = "bitcoin";
    static final String BTC_DESCRIPTION = "message";
    static final String BTC_AMOUNT = "amount";
    static final String BTC_BIP70_PARM = "r";

    public enum Asset {
        XMR, BTC
    }

    public enum Security {
        NORMAL,
        OA_NO_DNSSEC,
        OA_DNSSEC,
        BIP70
    }

    final public Asset asset;
    final public String address;
    final public String addressName;
    final public String paymentId;
    final public String amount;
    final public String description;
    final public Security security;
    final public String bip70;

    public BarcodeData(Asset asset, String address) {
        this(asset, address, null, null, null, null, Security.NORMAL);
    }

    public BarcodeData(Asset asset, String address, String amount) {
        this(asset, address, null, null, null, amount, Security.NORMAL);
    }

    public BarcodeData(Asset asset, String address, String amount, String description, Security security) {
        this(asset, address, null, null, description, amount, security);
    }

    public BarcodeData(Asset asset, String address, String paymentId, String amount) {
        this(asset, address, null, paymentId, null, amount, Security.NORMAL);
    }

    public BarcodeData(Asset asset, String address, String paymentId, String description, String amount) {
        this(asset, address, null, paymentId, description, amount, Security.NORMAL);
    }

    public BarcodeData(Asset asset, String address, String addressName, String paymentId, String description, String amount, Security security) {
        this(asset, address, addressName, null, paymentId, description, amount, security);
    }

    public BarcodeData(Asset asset, String address, String addressName, String bip70, String paymentId, String description, String amount, Security security) {
        this.asset = asset;
        this.address = address;
        this.bip70 = bip70;
        this.addressName = addressName;
        this.paymentId = paymentId;
        this.description = description;
        this.amount = amount;
        this.security = security;
    }


    public Uri getUri() {
        return Uri.parse(getUriString());
    }

    public String getUriString() {
        if (asset != Asset.XMR) throw new IllegalStateException("We can only do XMR stuff!");
        StringBuilder sb = new StringBuilder();
        sb.append(BarcodeData.XMR_SCHEME).append(address);
        boolean first = true;
        if ((paymentId != null) && !paymentId.isEmpty()) {
            sb.append("?");
            first = false;
            sb.append(BarcodeData.XMR_PAYMENTID).append('=').append(paymentId);
        }
        if ((description != null) && !description.isEmpty()) {
            sb.append(first ? "?" : "&");
            first = false;
            sb.append(BarcodeData.XMR_DESCRIPTION).append('=').append(Uri.encode(description));
        }
        if ((amount != null) && !amount.isEmpty()) {
            sb.append(first ? "?" : "&");
            sb.append(BarcodeData.XMR_AMOUNT).append('=').append(amount);
        }
        return sb.toString();
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
        // check for btc payment uri (like bitpay)
        if (bcData == null) {
            bcData = parseBitcoinPaymentUrl(qrCode);
        }
        // check for naked btc address
        if (bcData == null) {
            bcData = parseBitcoinNaked(qrCode);
        }
        // check for OpenAlias
        if (bcData == null) {
            bcData = parseOpenAlias(qrCode, false);
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
        String query = monero.getEncodedQuery();
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
        // deal with empty payment_id created by non-spec-conforming apps
        if ((paymentId != null) && paymentId.isEmpty()) paymentId = null;

        String description = parms.get(XMR_DESCRIPTION);
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
        return new BarcodeData(Asset.XMR, address, paymentId, description, amount);
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
    // bitcoin:?r=https://bitpay.com/i/xxx
    static public BarcodeData parseBitcoinUri(String uriString) {
        Timber.d("parseBitcoinUri=%s", uriString);

        if (uriString == null) return null;
        URI uri;
        try {
            uri = new URI(uriString);
        } catch (URISyntaxException ex) {
            return null;
        }
        if (!uri.isOpaque() ||
                !uri.getScheme().equals(BTC_SCHEME)) return null;

        String[] parts = uri.getRawSchemeSpecificPart().split("[?]");
        if ((parts.length <= 0) || (parts.length > 2)) {
            Timber.d("invalid number of parts %d", parts.length);
            return null;
        }
        Map<String, String> parms = new HashMap<>();
        if (parts.length == 2) {
            String[] args = parts[1].split("&");
            for (String arg : args) {
                String[] namevalue = arg.split("=");
                if (namevalue.length == 0) {
                    continue;
                }
                parms.put(Uri.decode(namevalue[0]).toLowerCase(),
                        namevalue.length > 1 ? Uri.decode(namevalue[1]) : "");
            }
        }
        String description = parms.get(BTC_DESCRIPTION);
        String address = parts[0]; // no need to decode as there can bo no special characters
        if (address.isEmpty()) { // possibly a BIP72 uri
            String bip70 = parms.get(BTC_BIP70_PARM);
            if (bip70 == null) {
                Timber.d("no address and can't find pp url");
                return null;
            }
            if (!PaymentProtocolHelper.isHttp(bip70)) {
                Timber.d("[%s] is not http url", bip70);
                return null;
            }
            return new BarcodeData(BarcodeData.Asset.BTC, null, null, bip70, null, description, null, Security.NORMAL);
        }
        if (!BitcoinAddressValidator.validate(address)) {
            Timber.d("BTC address (%s) invalid", address);
            return null;
        }
        String amount = parms.get(BTC_AMOUNT);
        if ((amount != null) && (!amount.isEmpty())) {
            try {
                Double.parseDouble(amount);
            } catch (NumberFormatException ex) {
                Timber.d(ex.getLocalizedMessage());
                return null; // we have an amount but its not a number!
            }
        }
        return new BarcodeData(BarcodeData.Asset.BTC, address, null, description, amount);
    }

    // https://bitpay.com/invoice?id=xxx
    // https://bitpay.com/i/KbMdd4EhnLXSbpWGKsaeo6
    static public BarcodeData parseBitcoinPaymentUrl(String url) {
        Timber.d("parseBitcoinUri=%s", url);

        if (url == null) return null;

        if (!PaymentProtocolHelper.isHttp(url)) {
            Timber.d("[%s] is not http url", url);
            return null;
        }

        return new BarcodeData(Asset.BTC, url);
    }

    static public BarcodeData parseBitcoinNaked(String address) {
        Timber.d("parseBitcoinNaked=%s", address);

        if (address == null) return null;

        if (!BitcoinAddressValidator.validate(address)) {
            Timber.d("address invalid");
            return null;
        }

        return new BarcodeData(BarcodeData.Asset.BTC, address);
    }

    static public BarcodeData parseOpenAlias(String oaString, boolean dnssec) {
        Timber.d("parseOpenAlias=%s", oaString);
        if (oaString == null) return null;

        Map<String, String> oaAttrs = OpenAliasHelper.parse(oaString);
        if (oaAttrs == null) return null;

        String oaAsset = oaAttrs.get(OpenAliasHelper.OA1_ASSET);
        if (oaAsset == null) return null;

        String address = oaAttrs.get(OpenAliasHelper.OA1_ADDRESS);
        if (address == null) return null;

        Asset asset;
        if (OA_XMR_ASSET.equals(oaAsset)) {
            if (!Wallet.isAddressValid(address)) {
                Timber.d("XMR address invalid");
                return null;
            }
            asset = Asset.XMR;
        } else if (OA_BTC_ASSET.equals(oaAsset)) {
            if (!BitcoinAddressValidator.validate(address)) {
                Timber.d("BTC address invalid");
                return null;
            }
            asset = Asset.BTC;
        } else {
            Timber.i("Unsupported OpenAlias asset %s", oaAsset);
            return null;
        }

        String paymentId = oaAttrs.get(OpenAliasHelper.OA1_PAYMENTID);
        String description = oaAttrs.get(OpenAliasHelper.OA1_DESCRIPTION);
        if (description == null) {
            description = oaAttrs.get(OpenAliasHelper.OA1_NAME);
        }
        String amount = oaAttrs.get(OpenAliasHelper.OA1_AMOUNT);
        String addressName = oaAttrs.get(OpenAliasHelper.OA1_NAME);

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

        Security sec = dnssec ? BarcodeData.Security.OA_DNSSEC : BarcodeData.Security.OA_NO_DNSSEC;

        return new BarcodeData(asset, address, addressName, paymentId, description, amount, sec);
    }
}