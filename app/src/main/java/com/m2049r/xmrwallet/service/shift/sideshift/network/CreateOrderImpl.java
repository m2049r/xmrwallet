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

import com.m2049r.xmrwallet.BuildConfig;
import com.m2049r.xmrwallet.service.shift.NetworkCallback;
import com.m2049r.xmrwallet.service.shift.ShiftApiCall;
import com.m2049r.xmrwallet.service.shift.ShiftCallback;
import com.m2049r.xmrwallet.service.shift.sideshift.api.CreateOrder;
import com.m2049r.xmrwallet.util.DateHelper;
import com.m2049r.xmrwallet.util.ServiceHelper;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.util.Date;

import lombok.Getter;

class CreateOrderImpl implements CreateOrder {
    @Getter
    private final String btcCurrency;
    @Getter
    private final double btcAmount;
    @Getter
    private final String btcAddress;
    @Getter
    private final String quoteId;
    @Getter
    private final String orderId;
    @Getter
    private final double xmrAmount;
    @Getter
    private final String xmrAddress;
    @Getter
    private final Date createdAt;
    @Getter
    private final Date expiresAt;

    CreateOrderImpl(final JSONObject jsonObject) throws JSONException {
        // sanity checks
        final String depositMethod = jsonObject.getString("depositMethodId");
        final String settleMethod = jsonObject.getString("settleMethodId");
        if (!"xmr".equals(depositMethod) || !ServiceHelper.ASSET.equals(settleMethod))
            throw new IllegalStateException();

        btcCurrency = settleMethod.toUpperCase();
        btcAmount = jsonObject.getDouble("settleAmount");
        JSONObject settleAddress = jsonObject.getJSONObject("settleAddress");
        btcAddress = settleAddress.getString("address");

        xmrAmount = jsonObject.getDouble("depositAmount");
        JSONObject depositAddress = jsonObject.getJSONObject("depositAddress");
        xmrAddress = depositAddress.getString("address");

        quoteId = jsonObject.getString("quoteId");

        orderId = jsonObject.getString("orderId");

        try {
            final String created = jsonObject.getString("createdAtISO");
            createdAt = DateHelper.parse(created);
            final String expires = jsonObject.getString("expiresAtISO");
            expiresAt = DateHelper.parse(expires);
        } catch (ParseException ex) {
            throw new JSONException(ex.getLocalizedMessage());
        }
    }

    public static void call(@NonNull final ShiftApiCall api, final String quoteId, @NonNull final String btcAddress,
                            @NonNull final ShiftCallback<CreateOrder> callback) {
        try {
            final JSONObject request = createRequest(quoteId, btcAddress);
            api.call("orders", request, new NetworkCallback() {
                @Override
                public void onSuccess(JSONObject jsonObject) {
                    try {
                        callback.onSuccess(new CreateOrderImpl(jsonObject));
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

    static JSONObject createRequest(final String quoteId, final String address) throws JSONException {
        final JSONObject jsonObject = new JSONObject();
        jsonObject.put("type", "fixed");
        jsonObject.put("quoteId", quoteId);
        jsonObject.put("settleAddress", address);
        if (!BuildConfig.ID_A.isEmpty() && !"null".equals(BuildConfig.ID_A)) {
            jsonObject.put("affiliateId", BuildConfig.ID_A);
        }
        return jsonObject;
    }
}
