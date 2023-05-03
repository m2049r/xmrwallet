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
import com.m2049r.xmrwallet.service.shift.sideshift.api.QueryOrderStatus;
import com.m2049r.xmrwallet.util.DateHelper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.util.Date;

import lombok.Getter;

class QueryOrderStatusImpl implements QueryOrderStatus {

    @Getter
    private QueryOrderStatus.State state;
    @Getter
    private final String orderId;
    @Getter
    private final Date createdAt;
    @Getter
    private final Date expiresAt;
    @Getter
    private final double btcAmount;
    @Getter
    private final String btcAddress;
    @Getter
    private final double xmrAmount;
    @Getter
    private final String xmrAddress;

    public boolean isCreated() {
        return true;
    }

    public boolean isTerminal() {
        return (state.equals(State.SETTLED) || isError());
    }

    public boolean isError() {
        return state.equals(State.UNDEFINED);
    }

    public boolean isWaiting() {
        return state.equals(State.WAITING);
    }

    public boolean isPending() {
        return state.equals(State.PENDING);
    }

    public boolean isSent() {
        return state.equals(State.SETTLING);
    }

    public boolean isPaid() {
        return state.equals(State.SETTLED);
    }

    public double getPrice() {
        return btcAmount / xmrAmount;
    }

    QueryOrderStatusImpl(final JSONObject jsonObject) throws JSONException {
        try {
            String created = jsonObject.getString("createdAtISO");
            createdAt = DateHelper.parse(created);
            String expires = jsonObject.getString("expiresAtISO");
            expiresAt = DateHelper.parse(expires);
        } catch (ParseException ex) {
            throw new JSONException(ex.getLocalizedMessage());
        }
        orderId = jsonObject.getString("orderId");

        btcAmount = jsonObject.getDouble("settleAmount");
        JSONObject settleAddress = jsonObject.getJSONObject("settleAddress");
        btcAddress = settleAddress.getString("address");

        xmrAmount = jsonObject.getDouble("depositAmount");
        JSONObject depositAddress = jsonObject.getJSONObject("depositAddress");
        xmrAddress = settleAddress.getString("address");

        JSONArray deposits = jsonObject.getJSONArray("deposits");
        // we only create one deposit, so die if there are more than one:
        if (deposits.length() > 1)
            throw new IllegalStateException("more than one deposits");

        state = State.UNDEFINED;
        if (deposits.length() == 0) {
            state = State.WAITING;
        } else if (deposits.length() == 1) {
            // sanity check
            if (!orderId.equals(deposits.getJSONObject(0).getString("orderId")))
                throw new IllegalStateException("deposit has different order id!");
            String stateName = deposits.getJSONObject(0).getString("status");
            try {
                state = State.valueOf(stateName.toUpperCase());
            } catch (IllegalArgumentException ex) {
                state = State.UNDEFINED;
            }
        }
    }

    public static void call(@NonNull final ShiftApiCall api, @NonNull final String orderId,
                            @NonNull final ShiftCallback<QueryOrderStatus> callback) {
        api.call("orders/" + orderId, new NetworkCallback() {
            @Override
            public void onSuccess(JSONObject jsonObject) {
                try {
                    callback.onSuccess(new QueryOrderStatusImpl(jsonObject));
                } catch (JSONException ex) {
                    callback.onError(ex);
                }
            }

            @Override
            public void onError(Exception ex) {
                callback.onError(ex);
            }
        });
    }
}
