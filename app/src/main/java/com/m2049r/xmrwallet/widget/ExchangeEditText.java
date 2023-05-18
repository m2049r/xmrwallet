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
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.android.material.textfield.TextInputLayout;
import com.m2049r.xmrwallet.R;
import com.m2049r.xmrwallet.service.exchange.api.ExchangeApi;
import com.m2049r.xmrwallet.service.exchange.api.ExchangeCallback;
import com.m2049r.xmrwallet.service.exchange.api.ExchangeRate;
import com.m2049r.xmrwallet.util.Helper;
import com.m2049r.xmrwallet.util.ServiceHelper;
import com.m2049r.xmrwallet.util.ThemeHelper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import timber.log.Timber;

public class ExchangeEditText extends LinearLayout {

    private double getEnteredAmount() {
        String enteredAmount = etAmountA.getEditText().getText().toString();
        try {
            return Double.parseDouble(enteredAmount);
        } catch (NumberFormatException ex) {
            Timber.i(ex.getLocalizedMessage());
        }
        return 0;
    }

    public boolean validate(double max, double min) {
        Timber.d("inProgress=%b", isExchangeInProgress());
        if (isExchangeInProgress()) {
            shakeExchangeField();
            return false;
        }
        boolean ok = true;
        String nativeAmount = getNativeAmount();
        if (nativeAmount == null) {
            ok = false;
        } else {
            try {
                double amount = Double.parseDouble(nativeAmount);
                if ((amount < min) || (amount > max)) {
                    ok = false;
                }
            } catch (NumberFormatException ex) {
                // this cannot be
                Timber.e(ex.getLocalizedMessage());
                ok = false;
            }
        }
        if (!ok) {
            shakeAmountField();
        }
        return ok;
    }

    void shakeAmountField() {
        etAmountA.startAnimation(Helper.getShakeAnimation(getContext()));
    }

    void shakeExchangeField() {
        tvAmountB.startAnimation(Helper.getShakeAnimation(getContext()));
    }

    public void setAmount(String nativeAmount) {
        if (nativeAmount != null) {
            etAmountA.getEditText().setText(nativeAmount);
            tvAmountB.setText(null);
            if (sCurrencyA.getSelectedItemPosition() != 0)
                sCurrencyA.setSelection(0, true); // set native currency & trigger exchange
            else
                doExchange();
        } else {
            tvAmountB.setText(null);
        }
    }

    public void setEditable(boolean editable) {
        etAmountA.setEnabled(editable);
    }

    public String getNativeAmount() {
        if (isExchangeInProgress()) return null;
        if (getCurrencyA() == 0)
            return getCleanAmountString(etAmountA.getEditText().getText().toString());
        else
            return getCleanAmountString(tvAmountB.getText().toString());
    }

    TextInputLayout etAmountA;
    TextView tvAmountB;
    Spinner sCurrencyA;
    Spinner sCurrencyB;
    ImageView evExchange;
    ProgressBar pbExchange;

    public int getCurrencyA() {
        return sCurrencyA.getSelectedItemPosition();
    }

    public int getCurrencyB() {
        return sCurrencyB.getSelectedItemPosition();
    }

    public ExchangeEditText(Context context) {
        super(context);
        initializeViews(context);
    }

    public ExchangeEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        initializeViews(context);
    }

    public ExchangeEditText(Context context,
                            AttributeSet attrs,
                            int defStyle) {
        super(context, attrs, defStyle);
        initializeViews(context);
    }

    /**
     * Inflates the views in the layout.
     *
     * @param context the current context for the view.
     */
    void initializeViews(Context context) {
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.view_exchange_edit, this);
    }

    void setCurrencyAdapter(Spinner spinner) {
        List<String> currencies = new ArrayList<>();
        currencies.add(Helper.BASE_CRYPTO);
        setCurrencyAdapter(spinner, currencies);
    }

    protected void setCurrencyAdapter(Spinner spinner, List<String> currencies) {
        if (Helper.SHOW_EXCHANGERATES)
            currencies.addAll(Arrays.asList(getResources().getStringArray(R.array.currency)));
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, currencies);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(spinnerAdapter);
    }

    void setInitialSpinnerSelections(Spinner baseSpinner, Spinner quoteSpinner) {
        baseSpinner.setSelection(0, true);
        quoteSpinner.setSelection(0, true);
    }

    private boolean isInitialized = false;

    void postInitialize() {
        setInitialSpinnerSelections(sCurrencyA, sCurrencyB);
        isInitialized = true;
        startExchange();
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        etAmountA = findViewById(R.id.etAmountA);
        etAmountA.getEditText().addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                doExchange();
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

        });
        tvAmountB = findViewById(R.id.tvAmountB);
        sCurrencyA = findViewById(R.id.sCurrencyA);
        sCurrencyB = findViewById(R.id.sCurrencyB);
        evExchange = findViewById(R.id.evExchange);
        pbExchange = findViewById(R.id.pbExchange);

        setCurrencyAdapter(sCurrencyA);
        setCurrencyAdapter(sCurrencyB);

        post(this::postInitialize);

        // make progress circle gray
        pbExchange.getIndeterminateDrawable().
                setColorFilter(ThemeHelper.getThemedColor(getContext(), com.google.android.material.R.attr.colorPrimaryVariant),
                        android.graphics.PorterDuff.Mode.MULTIPLY);

        sCurrencyA.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                if (!isInitialized) return;
                if (position != 0) { // if not native, select native on other
                    sCurrencyB.setSelection(0, true);
                }
                doExchange();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                // nothing
            }
        });

        sCurrencyB.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(final AdapterView<?> parentView, View selectedItemView, int position, long id) {
                if (!isInitialized) return;
                if (position != 0) { // if not native, select native on other
                    sCurrencyA.setSelection(0, true);
                }
                doExchange();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                // nothing
            }
        });
    }

    private boolean exchangeRateCacheIsUsable() {
        return (exchangeRateCache != null) &&
                ((exchangeRateCache.getBaseCurrency().equals(sCurrencyA.getSelectedItem()) &&
                        exchangeRateCache.getQuoteCurrency().equals(sCurrencyB.getSelectedItem())) ||
                        (exchangeRateCache.getBaseCurrency().equals(sCurrencyB.getSelectedItem()) &&
                                exchangeRateCache.getQuoteCurrency().equals(sCurrencyA.getSelectedItem())));
    }

    private double exchangeRateFromCache() {
        if (!exchangeRateCacheIsUsable()) return 0;
        if (exchangeRateCache.getBaseCurrency().equals(sCurrencyA.getSelectedItem())) {
            return exchangeRateCache.getRate();
        } else {
            return 1.0d / exchangeRateCache.getRate();
        }
    }

    public void doExchange() {
        if (!isInitialized) return;
        tvAmountB.setText(null);
        if (getCurrencyA() == getCurrencyB()) {
            exchange(1);
            return;
        }
        // use cached exchange rate if we have it
        if (!isExchangeInProgress()) {
            double rate = exchangeRateFromCache();
            if (rate > 0) {
                if (prepareExchange()) {
                    exchange(rate);
                }
            } else {
                startExchange();
            }
        }
    }

    private final ExchangeApi exchangeApi = ServiceHelper.getExchangeApi();

    // starts exchange through exchange api
    void startExchange() {
        String currencyA = (String) sCurrencyA.getSelectedItem();
        String currencyB = (String) sCurrencyB.getSelectedItem();
        if ((currencyA == null) || (currencyB == null)) return; // nothing to do
        execExchange(currencyA, currencyB);
    }

    void execExchange(String currencyA, String currencyB) {
        showProgress();
        queryExchangeRate(currencyA, currencyB,
                new ExchangeCallback() {
                    @Override
                    public void onSuccess(final ExchangeRate exchangeRate) {
                        if (isAttachedToWindow())
                            new Handler(Looper.getMainLooper()).post(new Runnable() {
                                @Override
                                public void run() {
                                    exchange(exchangeRate);
                                }
                            });
                    }

                    @Override
                    public void onError(final Exception e) {
                        Timber.e(e.getLocalizedMessage());
                        new Handler(Looper.getMainLooper()).post(new Runnable() {
                            @Override
                            public void run() {
                                exchangeFailed();
                            }
                        });
                    }
                });
    }

    void queryExchangeRate(final String base, final String quote, ExchangeCallback callback) {
        exchangeApi.queryExchangeRate(base, quote, callback);
    }

    private void exchange(double rate) {
        double amount = getEnteredAmount();
        if (rate > 0) {
            tvAmountB.setText(Helper.getFormattedAmount(rate * amount, getCurrencyB() == 0));
        } else {
            tvAmountB.setText("--");
            Timber.d("No rate!");
        }
    }

    private static final String CLEAN_FORMAT = "%." + Helper.XMR_DECIMALS + "f";

    private String getCleanAmountString(String enteredAmount) {
        try {
            double amount = Double.parseDouble(enteredAmount);
            if (amount >= 0) {
                return String.format(Locale.US, CLEAN_FORMAT, amount);
            } else {
                return null;
            }
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    boolean prepareExchange() {
        Timber.d("prepareExchange()");
        String enteredAmount = etAmountA.getEditText().getText().toString();
        if (!enteredAmount.isEmpty()) {
            String cleanAmount = getCleanAmountString(enteredAmount);
            Timber.d("cleanAmount = %s", cleanAmount);
            if (cleanAmount == null) {
                shakeAmountField();
                return false;
            }
        } else {
            return false;
        }
        return true;
    }

    public void exchangeFailed() {
        hideProgress();
        exchange(0);
    }

    // cache for exchange rate
    ExchangeRate exchangeRateCache = null;

    public void exchange(ExchangeRate exchangeRate) {
        hideProgress();
        // make sure this is what we want
        if (!exchangeRate.getBaseCurrency().equals(sCurrencyA.getSelectedItem()) ||
                !exchangeRate.getQuoteCurrency().equals(sCurrencyB.getSelectedItem())) {
            // something's wrong
            Timber.i("Currencies don't match! A: %s==%s B: %s==%s",
                    exchangeRate.getBaseCurrency(), sCurrencyA.getSelectedItem(),
                    exchangeRate.getQuoteCurrency(), sCurrencyB.getSelectedItem());
            return;
        }

        exchangeRateCache = exchangeRate;
        if (prepareExchange()) {
            exchange(exchangeRate.getRate());
        }
    }

    void showProgress() {
        pbExchange.setVisibility(View.VISIBLE);
    }

    private boolean isExchangeInProgress() {
        return pbExchange.getVisibility() == View.VISIBLE;
    }

    private void hideProgress() {
        pbExchange.setVisibility(View.INVISIBLE);
    }
}
