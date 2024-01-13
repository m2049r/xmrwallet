/*
 * Copyright (c) 2019-2023 m2049r@monerujo.io
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

package com.m2049r.xmrwallet.service.exchange.krakenFiat;

import androidx.annotation.NonNull;

import com.m2049r.xmrwallet.service.exchange.api.ExchangeApi;
import com.m2049r.xmrwallet.service.exchange.api.ExchangeCallback;
import com.m2049r.xmrwallet.service.exchange.api.ExchangeRate;
import com.m2049r.xmrwallet.util.Helper;
import com.m2049r.xmrwallet.util.ServiceHelper;

import timber.log.Timber;

/*
    Gets the XMR/EUR rate from kraken and then gets the EUR/fiat rate from the yadio
 */

public class ExchangeApiImpl implements ExchangeApi {
    static public final String BASE_FIAT = "EUR";

    final ExchangeApi krakenApi = new com.m2049r.xmrwallet.service.exchange.kraken.ExchangeApiImpl();

    private ExchangeApi getFiatApi(String symbol) {
        return ServiceHelper.getFiatApi(symbol);
    }

    @Override
    public String getName() {
        return krakenApi.getName() + "+";
    }

    public String getRealName(String fiatService) {
        return getName() + fiatService;
    }

    @Override
    public void queryExchangeRate(@NonNull final String baseCurrency, @NonNull final String quoteCurrency,
                                  @NonNull final ExchangeCallback callback) {
        Timber.d("B=%s Q=%s", baseCurrency, quoteCurrency);
        if (baseCurrency.equals(quoteCurrency)) {
            Timber.d("BASE=QUOTE=1");
            callback.onSuccess(new ExchangeRateImpl(getName(), baseCurrency, quoteCurrency, 1.0));
            return;
        }

        if (!Helper.BASE_CRYPTO.equals(baseCurrency)
                && !Helper.BASE_CRYPTO.equals(quoteCurrency)) {
            callback.onError(new IllegalArgumentException("no " + Helper.BASE_CRYPTO + " specified"));
            return;
        }

        final String quote = Helper.BASE_CRYPTO.equals(baseCurrency) ? quoteCurrency : baseCurrency;

        krakenApi.queryExchangeRate(Helper.BASE_CRYPTO, BASE_FIAT, new ExchangeCallback() {
            @Override
            public void onSuccess(final ExchangeRate krakenRate) {
                Timber.d("kraken = %f", krakenRate.getRate());
                final ExchangeApi fiatApi = getFiatApi(quote);
                fiatApi.queryExchangeRate(BASE_FIAT, quote, new ExchangeCallback() {
                    @Override
                    public void onSuccess(final ExchangeRate fiatRate) {
                        Timber.d("FIAT = %f", fiatRate.getRate());
                        double rate = fiatRate.getRate() * krakenRate.getRate();
                        Timber.d("Q=%s QC=%s", quote, quoteCurrency);
                        if (!quote.equals(quoteCurrency)) rate = 1.0d / rate;
                        Timber.d("rate = %f", rate);
                        final ExchangeRate exchangeRate =
                                new ExchangeRateImpl(getRealName(fiatApi.getName()), baseCurrency, quoteCurrency, rate);
                        callback.onSuccess(exchangeRate);
                    }

                    @Override
                    public void onError(Exception ex) {
                        Timber.d(ex);
                        callback.onError(ex);
                    }
                });
            }

            @Override
            public void onError(Exception ex) {
                Timber.d(ex);
                callback.onError(ex);
            }
        });
    }
}