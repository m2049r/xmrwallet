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

package com.m2049r.xmrwallet.service.shift.provider;

import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.m2049r.xmrwallet.data.CryptoAmount;
import com.m2049r.xmrwallet.service.shift.NetworkCallback;
import com.m2049r.xmrwallet.service.shift.ShiftApiCall;
import com.m2049r.xmrwallet.service.shift.ShiftCallback;
import com.m2049r.xmrwallet.service.shift.ShiftError;
import com.m2049r.xmrwallet.service.shift.ShiftException;
import com.m2049r.xmrwallet.service.shift.api.CreateOrder;
import com.m2049r.xmrwallet.service.shift.api.QueryOrderParameters;
import com.m2049r.xmrwallet.service.shift.api.QueryOrderStatus;
import com.m2049r.xmrwallet.service.shift.api.RequestQuote;
import com.m2049r.xmrwallet.service.shift.api.ShiftApi;
import com.m2049r.xmrwallet.util.NetCipherHelper;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.HttpUrl;
import okhttp3.Response;
import timber.log.Timber;

abstract public class ShiftApiImpl implements ShiftApi, ShiftApiCall {

    protected abstract String getBaseUrl();

    protected abstract String getApiUrl();

    private final HttpUrl api;

    public ShiftApiImpl() {
        api = HttpUrl.parse(getApiUrl());
    }

    @Override
    public void queryOrderParameters(CryptoAmount btcAmount, @NonNull final ShiftCallback<QueryOrderParameters> callback) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void requestQuote(@Nullable final String btcAddress, @NonNull final CryptoAmount btcAmount, @NonNull final ShiftCallback<RequestQuote> callback) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void createOrder(@NonNull final RequestQuote quote, @NonNull final String btcAddress,
                            @NonNull final ShiftCallback<CreateOrder> callback) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void queryOrderStatus(@NonNull final String uuid,
                                 @NonNull final ShiftCallback<QueryOrderStatus> callback) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Uri getQueryOrderUri(String orderId) {
        throw new UnsupportedOperationException();
    }


    //   void post(@NonNull final String path, final JSONObject data, @NonNull final NetworkCallback callback);

    @Override
    public void get(@NonNull final String path, final String parameters, @NonNull final NetworkCallback callback) {
        Timber.d("GET parameters=%s", parameters);
        final HttpUrl.Builder builder = api.newBuilder().addPathSegments(path);
        if (parameters != null)
            for (String parm : parameters.split("&")) {
                String[] p = parm.split("=");
                builder.addQueryParameter(p[0], p[1]);
            }
        NetCipherHelper.Request request = new NetCipherHelper.Request(builder.build());
        augment(request, null);
        enqueue(request, callback);
    }

    @Override
    public void post(@NonNull final String path, final JSONObject data, @NonNull final NetworkCallback callback) {
        Timber.d("data=%s", data);
        final HttpUrl url = api.newBuilder().addPathSegments(path).build();
        final NetCipherHelper.Request request = new NetCipherHelper.Request(url, data);
        augment(request, data);
        enqueue(request, callback);
    }

    protected void augment(@NonNull final NetCipherHelper.Request request, @Nullable final JSONObject data) {
    }

    private void enqueue(NetCipherHelper.Request request, @NonNull final NetworkCallback callback) {
        Timber.d("REQ: %s", request);
        request.enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(@NonNull final Call call, @NonNull final IOException ex) {
                callback.onError(ex, null);
            }

            @Override
            public void onResponse(@NonNull final Call call, @NonNull final Response response) throws IOException {
                Timber.d("onResponse code=%d", response.code());
                try {
                    if (response.body() == null) {
                        callback.onError(new IllegalStateException("Empty response from service"), null);
                        return;
                    }
                    final String body = response.body().string();
                    if ((response.code() >= 200) && (response.code() <= 499)) {
                        try {
                            Timber.d(" SUCCESS %s", body);
                            final JSONObject json = new JSONObject(body);
                            final ShiftError error = createShiftError(ShiftError.Type.SERVICE, json);
                            if (error != null) {
                                callback.onError(new ShiftException(response.code(), error), json);
                            } else {
                                callback.onSuccess(json);
                            }
                        } catch (JSONException ex) {
                            callback.onError(ex, null);
                        }
                    } else {
                        try {
                            Timber.d("!SUCCESS %s", body);
                            final JSONObject json = new JSONObject(body);
                            Timber.d(json.toString(2));
                            final ShiftError error = createShiftError(ShiftError.Type.INFRASTRUCTURE, json);
                            Timber.d("%s says %d/%s", getBaseUrl(), response.code(), error);
                            callback.onError(new ShiftException(response.code(), error), json);
                        } catch (JSONException ex) {
                            callback.onError(new ShiftException(response.code()), null);
                        }
                    }
                } finally {
                    response.close();
                }
            }
        });
    }

    abstract protected ShiftError createShiftError(ShiftError.Type type, final JSONObject jsonObject);
}