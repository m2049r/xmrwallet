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

package com.m2049r.xmrwallet.service.shift.provider.exolix;

import androidx.annotation.NonNull;

import com.m2049r.xmrwallet.data.Crypto;
import com.m2049r.xmrwallet.service.shift.NetworkCallback;
import com.m2049r.xmrwallet.service.shift.ShiftApiCall;
import com.m2049r.xmrwallet.service.shift.ShiftCallback;
import com.m2049r.xmrwallet.service.shift.ShiftService;
import com.m2049r.xmrwallet.service.shift.ShiftType;
import com.m2049r.xmrwallet.service.shift.api.CreateOrder;
import com.m2049r.xmrwallet.service.shift.api.RequestQuote;
import com.m2049r.xmrwallet.util.DateHelper;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.util.Date;

import lombok.Getter;

@Getter
class CreateOrderImpl implements CreateOrder {
    private final static long EXPIRE = 10 * 60 * 1000; // 10 minutes
    private final String btcCurrency;
    private final double btcAmount;
    private final String btcAddress;
    private final String orderId;
    private final double xmrAmount;
    private final String xmrAddress;
    private final Date createdAt;
    private final Date expiresAt;
    private final ShiftType type;

    @Override
    public String getQueryOrderId() {
        return orderId;
    }

    @Override
    public String getQuoteId() {
        return null;
    }

    CreateOrderImpl(final JSONObject jsonObject) throws JSONException {
        final JSONObject coinFrom = jsonObject.getJSONObject("coinFrom");
        final JSONObject coinTo = jsonObject.getJSONObject("coinTo");
        // sanity checks
        final String depositMethod = coinFrom.getString("coinCode");
        final String settleMethod = coinTo.getString("coinCode");
        if (!"xmr".equalsIgnoreCase(depositMethod)
                || !ShiftService.ASSET.getSymbol().equalsIgnoreCase(settleMethod))
            throw new IllegalStateException();

        btcCurrency = settleMethod.toUpperCase();
        btcAmount = jsonObject.getDouble("amountTo");
        btcAddress = jsonObject.getString("withdrawalAddress");

        xmrAmount = jsonObject.getDouble("amount");
        xmrAddress = jsonObject.getString("depositAddress");

        orderId = jsonObject.getString("id");

        try {
            final String created = jsonObject.getString("createdAt");
            createdAt = DateHelper.parse(created);
            expiresAt = new Date(createdAt.getTime() + EXPIRE);
        } catch (ParseException ex) {
            throw new JSONException(ex.getLocalizedMessage());
        }

        type = jsonObject.getString("rateType").equals("float") ? ShiftType.FLOAT : ShiftType.FIXED;
    }

    public static void call(@NonNull final ShiftApiCall api,
                            @NonNull final String btcAddress,
                            @NonNull final RequestQuote quote,
                            @NonNull final ShiftCallback<CreateOrder> callback) {
        try {
            final JSONObject request = createRequest(btcAddress, quote);
            api.post("transactions", request, new NetworkCallback() {
                @Override
                public void onSuccess(JSONObject jsonObject) {
                    try {
                        callback.onSuccess(new CreateOrderImpl(jsonObject));
                    } catch (JSONException ex) {
                        callback.onError(ex);
                    }
                }

                @Override
                public void onError(Exception ex, JSONObject json) {
                    callback.onError(ex);
                }
            });
        } catch (JSONException ex) {
            callback.onError(ex);
        }
    }

    static JSONObject createRequest(@NonNull final String btcAddress, @NonNull final RequestQuote quote) throws JSONException {
        final JSONObject jsonObject = new JSONObject();
        if (quote.getType() == ShiftType.FLOAT) {
            jsonObject.put("rateType", "float");
            jsonObject.put("amount", quote.getXmrAmount());
        } else { // default is FIXED
            jsonObject.put("rateType", "fixed");
            jsonObject.put("withdrawalAmount", quote.getBtcAmount());
        }
        jsonObject.put("coinFrom", Crypto.XMR.getSymbol());
        jsonObject.put("networkFrom", Crypto.XMR.getNetwork());
        jsonObject.put("coinTo", ShiftService.ASSET.getSymbol());
        jsonObject.put("networkTo", ShiftService.ASSET.getNetwork());
        jsonObject.put("withdrawalAddress", btcAddress);
        return jsonObject;
    }

    @Override
    public String getTag() {
        return ShiftService.EXOLIX.getTag();
    }
}
