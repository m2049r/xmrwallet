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

import com.m2049r.xmrwallet.util.OpenAliasHelper;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.ToString;
import timber.log.Timber;

@ToString
public class BarcodeData {
    public enum Security {
        NORMAL,
        OA_NO_DNSSEC,
        OA_DNSSEC
    }

    final public Crypto asset;
    final public List<Crypto> ambiguousAssets;
    final public String address;
    final public String addressName;
    final public String amount;
    final public String description;
    final public Security security;

    public BarcodeData(List<Crypto> assets, String address) {
        if (assets.isEmpty())
            throw new IllegalArgumentException("no assets specified");
        this.addressName = null;
        this.description = null;
        this.amount = null;
        this.security = Security.NORMAL;
        this.address = address;
        if (assets.size() == 1) {
            this.asset = assets.get(0);
            this.ambiguousAssets = null;
        } else {
            this.asset = null;
            this.ambiguousAssets = assets;
        }
    }

    public BarcodeData(Crypto asset, String address, String description, String amount) {
        this(asset, address, null, description, amount, Security.NORMAL);
    }

    public BarcodeData(Crypto asset, String address, String addressName, String description, String amount, Security security) {
        this.ambiguousAssets = null;
        this.asset = asset;
        this.address = address;
        this.addressName = addressName;
        this.description = description;
        this.amount = amount;
        this.security = security;
    }

    public Uri getUri() {
        return Uri.parse(getUriString());
    }

    public String getUriString() {
        if (asset != Crypto.XMR) throw new IllegalStateException("We can only do XMR stuff!");
        StringBuilder sb = new StringBuilder();
        sb.append(Crypto.XMR.getUriScheme())
                .append(':')
                .append(address);
        boolean first = true;
        if ((description != null) && !description.isEmpty()) {
            sb.append(first ? "?" : "&");
            first = false;
            sb.append(Crypto.XMR.getUriMessage()).append('=').append(Uri.encode(description));
        }
        if ((amount != null) && !amount.isEmpty()) {
            sb.append(first ? "?" : "&");
            sb.append(Crypto.XMR.getUriAmount()).append('=').append(amount);
        }
        return sb.toString();
    }

    static private BarcodeData parseNaked(String address) {
        List<Crypto> possibleAssets = new ArrayList<>();
        for (Crypto crypto : Crypto.values()) {
            if (crypto.validate(address)) {
                possibleAssets.add(crypto);
            }
        }
        if (possibleAssets.isEmpty())
            return null;
        return new BarcodeData(possibleAssets, address);
    }

    static public BarcodeData parseUri(String uriString) {
        Timber.d("parseBitUri=%s", uriString);

        URI uri;
        try {
            uri = new URI(uriString);
        } catch (URISyntaxException ex) {
            return null;
        }
        if (!uri.isOpaque()) return null;
        final String scheme = uri.getScheme();
        Crypto crypto = Crypto.withScheme(scheme);
        if (crypto == null) return null;

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

        String addressName = parms.get(crypto.getUriLabel());
        String description = parms.get(crypto.getUriMessage());
        String address = parts[0]; // no need to decode as there can be no special characters
        if (address.isEmpty()) {
            Timber.d("no address");
            return null;
        }
        if (!crypto.validate(address)) {
            Timber.d("%s address (%s) invalid", crypto, address);
            return null;
        }
        String amount = parms.get(crypto.getUriAmount());
        if ((amount != null) && (!amount.isEmpty())) {
            try {
                Double.parseDouble(amount);
            } catch (NumberFormatException ex) {
                Timber.d(ex.getLocalizedMessage());
                return null; // we have an amount but its not a number!
            }
        }
        return new BarcodeData(crypto, address, addressName, description, amount, Security.NORMAL);
    }


    static public BarcodeData fromString(String qrCode) {
        BarcodeData bcData = parseUri(qrCode);
        if (bcData == null) {
            // maybe it's naked?
            bcData = parseNaked(qrCode);
        }
        if (bcData == null) {
            // check for OpenAlias
            bcData = parseOpenAlias(qrCode, false);
        }
        return bcData;
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

        Crypto crypto = Crypto.withSymbol(oaAsset);
        if (crypto == null) {
            Timber.i("Unsupported OpenAlias asset %s", oaAsset);
            return null;
        }
        if (!crypto.validate(address)) {
            Timber.d("%s address invalid", crypto);
            return null;
        }

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

        Security sec = dnssec ? BarcodeData.Security.OA_DNSSEC : BarcodeData.Security.OA_NO_DNSSEC;

        return new BarcodeData(crypto, address, addressName, description, amount, sec);
    }

    public boolean isAmbiguous() {
        return ambiguousAssets != null;
    }
}