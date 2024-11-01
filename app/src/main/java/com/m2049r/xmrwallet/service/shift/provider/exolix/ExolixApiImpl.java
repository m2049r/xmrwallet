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

import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.m2049r.xmrwallet.BuildConfig;
import com.m2049r.xmrwallet.data.CryptoAmount;
import com.m2049r.xmrwallet.service.shift.ShiftCallback;
import com.m2049r.xmrwallet.service.shift.ShiftError;
import com.m2049r.xmrwallet.service.shift.api.CreateOrder;
import com.m2049r.xmrwallet.service.shift.api.QueryOrderParameters;
import com.m2049r.xmrwallet.service.shift.api.QueryOrderStatus;
import com.m2049r.xmrwallet.service.shift.api.RequestQuote;
import com.m2049r.xmrwallet.service.shift.provider.ShiftApiImpl;
import com.m2049r.xmrwallet.util.IdHelper;
import com.m2049r.xmrwallet.util.NetCipherHelper;

import org.json.JSONException;
import org.json.JSONObject;

import lombok.Getter;

public class ExolixApiImpl extends ShiftApiImpl {
    @Getter
    final private String baseUrl = "https://exolix.com";
    @Getter
    final private String apiUrl = baseUrl + "/api/v2";

    @Override
    public void queryOrderParameters(CryptoAmount btcAmount, @NonNull final ShiftCallback<QueryOrderParameters> callback) {
        QueryOrderParametersImpl.call(this, btcAmount, callback);
    }

    @Override
    public void createOrder(@NonNull final RequestQuote quote, @NonNull final String btcAddress,
                            @NonNull final ShiftCallback<CreateOrder> callback) {
        CreateOrderImpl.call(this, btcAddress, quote, callback);
    }

    @Override
    public void queryOrderStatus(@NonNull final String uuid,
                                 @NonNull final ShiftCallback<QueryOrderStatus> callback) {
        QueryOrderStatusImpl.call(this, uuid, callback);
    }

    @Override
    public Uri getQueryOrderUri(String orderId) {
        return Uri.parse(getBaseUrl() + "/transaction/" + orderId);
    }

    @Override
    protected ShiftError createShiftError(ShiftError.Type type, final JSONObject jsonObject) {
        try {
            if (jsonObject.has("message")) {
                final String message = jsonObject.getString("message");
                if (!"null".equals(message))
                    return new ShiftError(type, message);
            }
        } catch (JSONException ex) {
            return new ShiftError(ShiftError.Type.INFRASTRUCTURE, "unknown");
        }
        return null;
    }

    @Override
    protected void augment(@NonNull final NetCipherHelper.Request request, @Nullable final JSONObject data) {
        request.setAugmenter((b) -> IdHelper.addHeader(b, "Authorization", BuildConfig.ID_F));
    }
}