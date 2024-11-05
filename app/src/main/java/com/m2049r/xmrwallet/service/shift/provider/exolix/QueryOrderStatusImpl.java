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

import com.m2049r.xmrwallet.service.shift.NetworkCallback;
import com.m2049r.xmrwallet.service.shift.ShiftApiCall;
import com.m2049r.xmrwallet.service.shift.ShiftCallback;
import com.m2049r.xmrwallet.service.shift.ShiftService;
import com.m2049r.xmrwallet.service.shift.api.QueryOrderStatus;
import com.m2049r.xmrwallet.util.DateHelper;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.util.Date;

class QueryOrderStatusImpl extends QueryOrderStatus {

    private QueryOrderStatusImpl(String orderId, Status status, String btcCurrency, double btcAmount, String btcAddress, double xmrAmount, String xmrAddress, Date createdAt, Date expiresAt) {
        super(orderId, status, btcCurrency, btcAmount, btcAddress, xmrAmount, xmrAddress, createdAt, expiresAt);
    }

    static private Status getStatus(String status) {
        switch (status) {
            case "wait":
                return Status.WAITING;
            case "confirmation":
            case "confirmed":
                return Status.PENDING;
            case "exchanging":
            case "sending":
                return Status.SETTLING;
            case "success":
                return Status.SETTLED;
            case "overdue":
                return Status.EXPIRED;
            case "refunded":
            default:
                return Status.UNDEFINED;
        }
    }

    static private QueryOrderStatusImpl of(final JSONObject jsonObject) throws JSONException {
        final JSONObject coinFrom = jsonObject.getJSONObject("coinFrom");
        final JSONObject coinTo = jsonObject.getJSONObject("coinTo");
        // sanity checks
        final String depositMethod = coinFrom.getString("coinCode");
        final String settleMethod = coinTo.getString("coinCode");
        if (!"xmr".equalsIgnoreCase(depositMethod)
                || !ShiftService.ASSET.getSymbol().equalsIgnoreCase(settleMethod))
            throw new IllegalStateException();

        final double btcAmount = jsonObject.getDouble("amountTo");
        final String btcAddress = jsonObject.getString("withdrawalAddress");

        final double xmrAmount = jsonObject.getDouble("amount");
        final String xmrAddress = jsonObject.getString("depositAddress");

        final String orderId = jsonObject.getString("id");

        Date createdAt;
        Date expiresAt;
        try {
            final String created = jsonObject.getString("createdAt");
            createdAt = DateHelper.parse(created);
            expiresAt = new Date(createdAt.getTime() + 300000);
        } catch (ParseException ex) {
            throw new JSONException(ex.getLocalizedMessage());
        }

        final String status = jsonObject.getString("status");

        return new QueryOrderStatusImpl(
                orderId,
                getStatus(status),
                settleMethod,
                btcAmount,
                btcAddress,
                xmrAmount,
                xmrAddress,
                createdAt,
                expiresAt
        );
    }

    public static void call(@NonNull final ShiftApiCall api, @NonNull final String orderId,
                            @NonNull final ShiftCallback<QueryOrderStatus> callback) {
        api.get("transactions/" + orderId, null, new NetworkCallback() {
            @Override
            public void onSuccess(JSONObject jsonObject) {
                try {
                    callback.onSuccess(QueryOrderStatusImpl.of(jsonObject));
                } catch (JSONException ex) {
                    callback.onError(ex);
                }
            }

            @Override
            public void onError(Exception ex, JSONObject json) {
                callback.onError(ex);
            }
        });
    }
}
