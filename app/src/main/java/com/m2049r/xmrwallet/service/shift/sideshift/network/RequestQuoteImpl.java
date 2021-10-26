/*
 * Copyright (c) 2017-2021 m2049r et al.
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

package com.m2049r.xmrwallet.service.shift.sideshift.network;

import androidx.annotation.NonNull;

import com.m2049r.xmrwallet.service.shift.NetworkCallback;
import com.m2049r.xmrwallet.service.shift.ShiftApiCall;
import com.m2049r.xmrwallet.service.shift.ShiftCallback;
import com.m2049r.xmrwallet.service.shift.sideshift.api.RequestQuote;
import com.m2049r.xmrwallet.util.DateHelper;
import com.m2049r.xmrwallet.util.ServiceHelper;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.ParseException;
import java.util.Date;
import java.util.Locale;

import lombok.Getter;

class RequestQuoteImpl implements RequestQuote {
    @Getter
    private final double btcAmount;
    @Getter
    private final String id;
    @Getter
    private final Date createdAt;
    @Getter
    private final Date expiresAt;
    @Getter
    private final double xmrAmount;
    @Getter
    private final double price;

    // TODO do something with errors - they always seem to send us 500

    RequestQuoteImpl(final JSONObject jsonObject) throws JSONException {
        // sanity checks
        final String depositMethod = jsonObject.getString("depositMethod");
        final String settleMethod = jsonObject.getString("settleMethod");
        if (!"xmr".equals(depositMethod) || !ServiceHelper.ASSET.equals(settleMethod))
            throw new IllegalStateException();

        btcAmount = jsonObject.getDouble("settleAmount");
        id = jsonObject.getString("id");

        try {
            final String created = jsonObject.getString("createdAt");
            createdAt = DateHelper.parse(created);
            final String expires = jsonObject.getString("expiresAt");
            expiresAt = DateHelper.parse(expires);
        } catch (ParseException ex) {
            throw new JSONException(ex.getLocalizedMessage());
        }
        xmrAmount = jsonObject.getDouble("depositAmount");
        price = jsonObject.getDouble("rate");
    }

    public static void call(@NonNull final ShiftApiCall api, final double btcAmount,
                            @NonNull final ShiftCallback<RequestQuote> callback) {
        try {
            final JSONObject request = createRequest(btcAmount);
            api.call("quotes", request, new NetworkCallback() {
                @Override
                public void onSuccess(JSONObject jsonObject) {
                    try {
                        callback.onSuccess(new RequestQuoteImpl(jsonObject));
                    } catch (JSONException ex) {
                        callback.onError(ex);
                    }
                }

                @Override
                public void onError(Exception ex) {
                    callback.onError(ex);
                }
            });
        } catch (JSONException ex) {
            callback.onError(ex);
        }
    }

    /**
     * Create JSON request object
     *
     * @param btcAmount how much XMR to shift to BTC
     */

    static JSONObject createRequest(final double btcAmount) throws JSONException {
        final JSONObject jsonObject = new JSONObject();
        jsonObject.put("depositMethod", "xmr");
        jsonObject.put("settleMethod", ServiceHelper.ASSET);
        // #sideshift is silly and likes numbers as strings
        String amount = AmountFormatter.format(btcAmount);
        jsonObject.put("settleAmount", amount);
        return jsonObject;
    }

    static final DecimalFormat AmountFormatter;

    static {
        AmountFormatter = new DecimalFormat();
        AmountFormatter.setDecimalFormatSymbols(new DecimalFormatSymbols(Locale.US));
        AmountFormatter.setMinimumIntegerDigits(1);
        AmountFormatter.setMaximumFractionDigits(12);
        AmountFormatter.setGroupingUsed(false);
    }
}
