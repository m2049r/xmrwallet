/*
 * Copyright (c) 2019 m2049r@monerujo.io
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

// https://developer.android.com/training/basics/network-ops/xml

package com.m2049r.xmrwallet.service.exchange.yadio;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import com.m2049r.xmrwallet.service.exchange.api.ExchangeApi;
import com.m2049r.xmrwallet.service.exchange.api.ExchangeCallback;
import com.m2049r.xmrwallet.service.exchange.api.ExchangeException;
import com.m2049r.xmrwallet.util.NetCipherHelper;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.HttpUrl;
import okhttp3.Response;
import timber.log.Timber;

public class ExchangeApiImpl implements ExchangeApi {
    @NonNull
    private final HttpUrl baseUrl;

    //so we can inject the mockserver url
    @VisibleForTesting
    public ExchangeApiImpl(@NonNull final HttpUrl baseUrl) {
        this.baseUrl = baseUrl;
    }

    public ExchangeApiImpl() {
        this(HttpUrl.parse("https://api.yadio.io/convert/1/eur"));
    }

    @Override
    public String getName() {
        return "yadio";
    }

    @Override
    public void queryExchangeRate(@NonNull final String baseCurrency, @NonNull final String quoteCurrency,
                                  @NonNull final ExchangeCallback callback) {
        if (!baseCurrency.equals("EUR")) {
            callback.onError(new IllegalArgumentException("Only EUR supported as base"));
            return;
        }

        if (baseCurrency.equals(quoteCurrency)) {
            callback.onSuccess(new ExchangeRateImpl(quoteCurrency, 1.0, 0));
            return;
        }

        final HttpUrl url = baseUrl.newBuilder()
                .addPathSegments(quoteCurrency.substring(0, 3))
                .build();
        final NetCipherHelper.Request httpRequest = new NetCipherHelper.Request(url);

        httpRequest.enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(@NonNull final Call call, @NonNull final IOException ex) {
                callback.onError(ex);
            }

            @Override
            public void onResponse(@NonNull final Call call, @NonNull final Response response) throws IOException {
                if (response.isSuccessful()) {
                    try {
                        assert response.body() != null;
                        final JSONObject json = new JSONObject(response.body().string());
                        if (json.has("error")) {
                            Timber.d("%d: %s", response.code(), json.getString("error"));
                            callback.onError(new ExchangeException(response.code(), json.getString("error")));
                            return;
                        }
                        double rate = json.getDouble("rate");
                        long timestamp = json.getLong("timestamp");
                        callback.onSuccess(new ExchangeRateImpl(quoteCurrency, rate, timestamp));
                    } catch (JSONException ex) {
                        callback.onError(ex);
                    }
                } else {
                    callback.onError(new ExchangeException(response.code(), response.message()));
                }
            }
        });
    }
}