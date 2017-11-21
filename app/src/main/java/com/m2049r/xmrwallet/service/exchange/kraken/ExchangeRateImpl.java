/*
 * Copyright (c) 2017 m2049r et al.
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

package com.m2049r.xmrwallet.service.exchange.kraken;

import android.support.annotation.NonNull;

import com.m2049r.xmrwallet.service.exchange.api.ExchangeCallback;
import com.m2049r.xmrwallet.service.exchange.api.ExchangeException;
import com.m2049r.xmrwallet.service.exchange.api.ExchangeRate;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.NoSuchElementException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class ExchangeRateImpl implements ExchangeRate {

    private final String baseCurrency;
    private final String quoteCurrency;
    private final double rate;

    @Override
    public String getServiceName() {
        return "kraken.com";
    }

    @Override
    public String getBaseCurrency() {
        return baseCurrency;
    }

    @Override
    public String getQuoteCurrency() {
        return quoteCurrency;
    }

    @Override
    public double getRate() {
        return rate;
    }

    ExchangeRateImpl(@NonNull final String baseCurrency, @NonNull final String quoteCurrency, double rate) {
        super();
        this.baseCurrency = baseCurrency;
        this.quoteCurrency = quoteCurrency;
        this.rate = rate;
    }

    ExchangeRateImpl(final JSONObject jsonObject) throws JSONException, ExchangeException {
        try {
            final String key = jsonObject.keys().next(); // we expect only one
            Pattern pattern = Pattern.compile("^X(.*?)Z(.*?)$");
            Matcher matcher = pattern.matcher(key);
            if (matcher.find()) {
                this.baseCurrency = matcher.group(1);
                this.quoteCurrency = matcher.group(2);
            } else {
                throw new ExchangeException("no pair returned!");
            }

            JSONObject pair = jsonObject.getJSONObject(key);
            JSONArray close = pair.getJSONArray("c");
            String closePrice = close.getString(0);
            if (closePrice != null) {
                try {
                    this.rate = Double.parseDouble(closePrice);
                } catch (NumberFormatException ex) {
                    throw new ExchangeException(ex.getLocalizedMessage());
                }
            } else {
                throw new ExchangeException("no close price returned!");
            }
        } catch (NoSuchElementException ex) {
            throw new ExchangeException(ex.getLocalizedMessage());
        }
    }

    public static void call(@NonNull final ExchangeApiCall api,
                            @NonNull final String baseCurrency, @NonNull final String quoteCurrency,
                            @NonNull final ExchangeCallback callback) {

        if (baseCurrency.equals(quoteCurrency)) {
            callback.onSuccess(new ExchangeRateImpl(baseCurrency, quoteCurrency, 1.0));
            return;
        }

        boolean inverse = false;
        String fiat = null;

        if (baseCurrency.equals("XMR")) {
            fiat = quoteCurrency;
            inverse = false;
        }

        if (quoteCurrency.equals("XMR")) {
            fiat = baseCurrency;
            inverse = true;
        }

        if (fiat == null) {
            callback.onError(new IllegalArgumentException("no fiat specified"));
            return;
        }

        api.call(fiat, new NetworkCallback() {
            @Override
            public void onSuccess(JSONObject jsonObject) {
                try {
                    final ExchangeRate exchangeRate = new ExchangeRateImpl(jsonObject);
                    callback.onSuccess(exchangeRate);
                } catch (JSONException ex) {
                    callback.onError(new ExchangeException(ex.getLocalizedMessage()));
                } catch (ExchangeException ex) {
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
