/*
 * Copyright (c) 2017-2019 m2049r
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

// based on https://code.tutsplus.com/tutorials/creating-compound-views-on-android--cms-22889

package com.m2049r.xmrwallet.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.widget.Spinner;

import androidx.annotation.NonNull;

import com.m2049r.xmrwallet.R;
import com.m2049r.xmrwallet.service.exchange.api.ExchangeCallback;
import com.m2049r.xmrwallet.service.exchange.api.ExchangeRate;
import com.m2049r.xmrwallet.util.Helper;

import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;

public class ExchangeOtherEditText extends ExchangeEditText {
    /*
        all exchanges are done through XMR
        baseCurrency is the native currency
     */

    String baseCurrency = null; // not XMR
    private double exchangeRate = 0; // baseCurrency to XMR

    public void setExchangeRate(double rate) {
        exchangeRate = rate;
        post(this::startExchange);
    }

    public void setBaseCurrency(@NonNull String symbol) {
        if (symbol.equals(baseCurrency)) return;
        baseCurrency = symbol;
        setCurrencyAdapter(sCurrencyA);
        setCurrencyAdapter(sCurrencyB);
        post(this::postInitialize);
    }

    private void setBaseCurrency(Context context, AttributeSet attrs) {
        TypedArray ta = context.getTheme().obtainStyledAttributes(attrs, R.styleable.ExchangeEditText, 0, 0);
        try {
            baseCurrency = ta.getString(R.styleable.ExchangeEditText_baseSymbol);
            if (baseCurrency == null)
                throw new IllegalArgumentException("base currency must be set");
        } finally {
            ta.recycle();
        }
    }

    public ExchangeOtherEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        setBaseCurrency(context, attrs);
    }

    public ExchangeOtherEditText(Context context,
                                 AttributeSet attrs,
                                 int defStyle) {
        super(context, attrs, defStyle);
        setBaseCurrency(context, attrs);
    }

    @Override
    void setCurrencyAdapter(Spinner spinner) {
        List<String> currencies = new ArrayList<>();
        if (!baseCurrency.equals(Helper.BASE_CRYPTO)) currencies.add(baseCurrency);
        currencies.add(Helper.BASE_CRYPTO);
        setCurrencyAdapter(spinner, currencies);
    }

    @Override
    void setInitialSpinnerSelections(Spinner baseSpinner, Spinner quoteSpinner) {
        baseSpinner.setSelection(0, true);
        quoteSpinner.setSelection(1, true);
    }

    private void localExchange(final String base, final String quote, final double rate) {
        exchange(new ExchangeRate() {
            @Override
            public String getServiceName() {
                return "Local";
            }

            @Override
            public String getBaseCurrency() {
                return base;
            }

            @Override
            public String getQuoteCurrency() {
                return quote;
            }

            @Override
            public double getRate() {
                return rate;
            }
        });
    }

    @Override
    void execExchange(String currencyA, String currencyB) {
        if (!currencyA.equals(baseCurrency) && !currencyB.equals(baseCurrency)) {
            throw new IllegalStateException("I can only exchange " + baseCurrency);
        }

        showProgress();

        Timber.d("execExchange(%s, %s)", currencyA, currencyB);

        // first deal with XMR/baseCurrency & baseCurrency/XMR

        if (currencyA.equals(Helper.BASE_CRYPTO) && (currencyB.equals(baseCurrency))) {
            localExchange(currencyA, currencyB, 1.0d / exchangeRate);
            return;
        }
        if (currencyA.equals(baseCurrency) && (currencyB.equals(Helper.BASE_CRYPTO))) {
            localExchange(currencyA, currencyB, exchangeRate);
            return;
        }

        // next, deal with XMR/baseCurrency

        if (currencyA.equals(baseCurrency)) {
            queryExchangeRate(Helper.BASE_CRYPTO, currencyB, exchangeRate, true);
        } else {
            queryExchangeRate(currencyA, Helper.BASE_CRYPTO, 1.0d / exchangeRate, false);
        }
    }

    private void queryExchangeRate(final String base, final String quote, final double factor,
                                   final boolean baseIsBaseCrypto) {
        queryExchangeRate(base, quote,
                new ExchangeCallback() {
                    @Override
                    public void onSuccess(final ExchangeRate exchangeRate) {
                        if (isAttachedToWindow())
                            new Handler(Looper.getMainLooper()).post(new Runnable() {
                                @Override
                                public void run() {
                                    ExchangeRate xchange = new ExchangeRate() {
                                        @Override
                                        public String getServiceName() {
                                            return exchangeRate.getServiceName() + "+" + baseCurrency;
                                        }

                                        @Override
                                        public String getBaseCurrency() {
                                            return baseIsBaseCrypto ? baseCurrency : base;
                                        }

                                        @Override
                                        public String getQuoteCurrency() {
                                            return baseIsBaseCrypto ? quote : baseCurrency;
                                        }

                                        @Override
                                        public double getRate() {
                                            return exchangeRate.getRate() * factor;
                                        }
                                    };
                                    exchange(xchange);
                                }
                            });
                    }

                    @Override
                    public void onError(final Exception e) {
                        Timber.e(e.getLocalizedMessage());
                        new Handler(Looper.getMainLooper()).post(() -> exchangeFailed());
                    }
                });
    }
}
