/*
 * Copyright (c) 2017 m2049r
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
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import com.m2049r.xmrwallet.R;
import com.m2049r.xmrwallet.model.Wallet;
import com.m2049r.xmrwallet.service.exchange.api.ExchangeApi;
import com.m2049r.xmrwallet.service.exchange.api.ExchangeCallback;
import com.m2049r.xmrwallet.service.exchange.api.ExchangeRate;
import com.m2049r.xmrwallet.util.Helper;

import java.util.Locale;

import timber.log.Timber;

public class ExchangeTextView extends LinearLayout
        implements NumberPadView.NumberPadListener {

    private static String MAX = "\u221E";

    String xmrAmount = null;
    String notXmrAmount = null;

    void setXmr(String xmr) {
        xmrAmount = xmr;
        if (onNewAmountListener != null) {
            onNewAmountListener.onNewAmount(xmr);
        }
    }

    public boolean validate(double max) {
        Timber.d("inProgress=%b", isExchangeInProgress());
        if (isExchangeInProgress()) {
            shakeExchangeField();
            return false;
        }
        boolean ok = true;
        if (xmrAmount != null) {
            try {
                double amount = Double.parseDouble(xmrAmount);
                if (amount > max) {
                    ok = false;
                }
                if (amount <= 0) { /////////////////////////////
                    ok = false;
                }
            } catch (NumberFormatException ex) {
                // this cannot be
                Timber.e(ex.getLocalizedMessage());
                ok = false;
            }
        } else {
            ok = false;
        }
        if (!ok) {
            shakeAmountField();
        }
        return ok;
    }

    void shakeAmountField() {
        tvAmountA.startAnimation(Helper.getShakeAnimation(getContext()));
    }

    void shakeExchangeField() {
        tvAmountB.startAnimation(Helper.getShakeAnimation(getContext()));
    }

    public void setAmount(String xmrAmount) {
        if (xmrAmount != null) {
            setCurrencyA(0);
            tvAmountA.setText(xmrAmount);
            setXmr(xmrAmount);
            this.notXmrAmount = null;
            doExchange();
        } else {
            setXmr(null);
            this.notXmrAmount = null;
            tvAmountB.setText(null);
        }
    }

    public String getAmount() {
        return xmrAmount;
    }

    TextView tvAmountA;
    TextView tvAmountB;
    Spinner sCurrencyA;
    Spinner sCurrencyB;
    ImageView evExchange;
    ProgressBar pbExchange;


    public void setCurrencyA(int currency) {
        if ((currency != 0) && (getCurrencyB() != 0)) {
            setCurrencyB(0);
        }
        sCurrencyA.setSelection(currency, true);
        doExchange();
    }

    public void setCurrencyB(int currency) {
        if ((currency != 0) && (getCurrencyA() != 0)) {
            setCurrencyA(0);
        }
        sCurrencyB.setSelection(currency, true);
        doExchange();
    }

    public int getCurrencyA() {
        return sCurrencyA.getSelectedItemPosition();
    }

    public int getCurrencyB() {
        return sCurrencyB.getSelectedItemPosition();
    }

    public ExchangeTextView(Context context) {
        super(context);
        initializeViews(context);
    }

    public ExchangeTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initializeViews(context);
    }

    public ExchangeTextView(Context context,
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
    private void initializeViews(Context context) {
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.view_exchange_text, this);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        tvAmountA = findViewById(R.id.tvAmountA);
        tvAmountB = findViewById(R.id.tvAmountB);
        sCurrencyA = findViewById(R.id.sCurrencyA);
        sCurrencyB = findViewById(R.id.sCurrencyB);
        evExchange = findViewById(R.id.evExchange);
        pbExchange = findViewById(R.id.pbExchange);

        // make progress circle gray
        pbExchange.getIndeterminateDrawable().
                setColorFilter(getResources().getColor(R.color.trafficGray),
                        android.graphics.PorterDuff.Mode.MULTIPLY);


        sCurrencyA.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                if (position != 0) { // if not XMR, select XMR on other
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
                if (position != 0) { // if not XMR, select XMR on other
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

    public void doExchange() {
        tvAmountB.setText(null);
        // use cached exchange rate if we have it
        if (!isExchangeInProgress()) {
            String enteredCurrencyA = (String) sCurrencyA.getSelectedItem();
            String enteredCurrencyB = (String) sCurrencyB.getSelectedItem();
            if ((enteredCurrencyA + enteredCurrencyB).equals(assetPair)) {
                if (prepareExchange()) {
                    exchange(assetRate);
                } else {
                    clearAmounts();
                }
            } else {
                clearAmounts();
                startExchange();
            }
        } else {
            clearAmounts();
        }
    }

    private void clearAmounts() {
        Timber.d("clearAmounts");
        if ((xmrAmount != null) || (notXmrAmount != null)) {
            tvAmountB.setText(null);
            setXmr(null);
            notXmrAmount = null;
        }
    }

    private final ExchangeApi exchangeApi = Helper.getExchangeApi();

    void startExchange() {
        showProgress();
        String currencyA = (String) sCurrencyA.getSelectedItem();
        String currencyB = (String) sCurrencyB.getSelectedItem();
        exchangeApi.queryExchangeRate(currencyA, currencyB,
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

    public void exchange(double rate) {
        Timber.d("%s / %s", xmrAmount, notXmrAmount);
        if (getCurrencyA() == 0) {
            if (xmrAmount == null) return;
            if (!xmrAmount.isEmpty() && (rate > 0)) {
                double amountB = rate * Double.parseDouble(xmrAmount);
                notXmrAmount = Helper.getFormattedAmount(amountB, getCurrencyB() == 0);
            } else {
                notXmrAmount = "";
            }
            tvAmountB.setText(notXmrAmount);
            Timber.d("%s / %s", xmrAmount, notXmrAmount);
        } else if (getCurrencyB() == 0) {
            if (notXmrAmount == null) return;
            if (!notXmrAmount.isEmpty() && (rate > 0)) {
                double amountB = rate * Double.parseDouble(notXmrAmount);
                setXmr(Helper.getFormattedAmount(amountB, true));
            } else {
                setXmr("");
            }
            tvAmountB.setText(xmrAmount);
            if (xmrAmount == null) {
                shakeAmountField();
            }
        } else { // no XMR currency - cannot happen!
            Timber.e("No XMR currency!");
            setXmr(null);
            notXmrAmount = null;
            return;
        }
    }

    boolean prepareExchange() {
        Timber.d("prepareExchange()");
        String enteredAmount = tvAmountA.getText().toString();
        if (!enteredAmount.isEmpty()) {
            String cleanAmount = "";
            if (getCurrencyA() == 0) {
                // sanitize the input
                long xmr = Wallet.getAmountFromString(enteredAmount);
                if (xmr >= 0) {
                    cleanAmount = Helper.getDisplayAmount(xmr);
                } else {
                    cleanAmount = null;
                }
                setXmr(cleanAmount);
                notXmrAmount = null;
                Timber.d("cleanAmount = %s", cleanAmount);
                if (cleanAmount == null) {
                    shakeAmountField();
                    return false;
                }
            } else if (getCurrencyB() == 0) { // we use B & 0 here for the else below ...
                // sanitize the input
                double amountA = Double.parseDouble(enteredAmount);
                cleanAmount = String.format(Locale.US, "%.2f", amountA);
                setXmr(null);
                notXmrAmount = cleanAmount;
            } else { // no XMR currency - cannot happen!
                Timber.e("No XMR currency!");
                setXmr(null);
                notXmrAmount = null;
                return false;
            }
            Timber.d("prepareExchange() %s", cleanAmount);
        } else {
            setXmr("");
            notXmrAmount = "";
        }
        return true;
    }

    public void exchangeFailed() {
        hideProgress();
        exchange(0);
        if (onFailedExchangeListener != null) {
            onFailedExchangeListener.onFailedExchange();
        }
    }

    String assetPair = null;
    double assetRate = 0;

    public void exchange(ExchangeRate exchangeRate) {
        hideProgress();
        // first, make sure this is what we want
        String enteredCurrencyA = (String) sCurrencyA.getSelectedItem();
        String enteredCurrencyB = (String) sCurrencyB.getSelectedItem();
        if (!exchangeRate.getBaseCurrency().equals(enteredCurrencyA)
                || !exchangeRate.getQuoteCurrency().equals(enteredCurrencyB)) {
            // something's wrong
            Timber.e("Currencies don't match!");
            return;
        }
        assetPair = enteredCurrencyA + enteredCurrencyB;
        assetRate = exchangeRate.getRate();
        if (prepareExchange()) {
            exchange(exchangeRate.getRate());
        }
    }

    private void showProgress() {
        pbExchange.setVisibility(View.VISIBLE);
    }

    private boolean isExchangeInProgress() {
        return pbExchange.getVisibility() == View.VISIBLE;
    }

    private void hideProgress() {
        pbExchange.setVisibility(View.INVISIBLE);
    }

    // Hooks
    public interface OnNewAmountListener {
        void onNewAmount(String xmr);
    }

    OnNewAmountListener onNewAmountListener;

    public void setOnNewAmountListener(OnNewAmountListener listener) {
        onNewAmountListener = listener;
    }

    public interface OnAmountInvalidatedListener {
        void onAmountInvalidated();
    }

    OnAmountInvalidatedListener onAmountInvalidatedListener;

    public void setOnAmountInvalidatedListener(OnAmountInvalidatedListener listener) {
        onAmountInvalidatedListener = listener;
    }

    public interface OnFailedExchangeListener {
        void onFailedExchange();
    }

    OnFailedExchangeListener onFailedExchangeListener;

    public void setOnFailedExchangeListener(OnFailedExchangeListener listener) {
        onFailedExchangeListener = listener;
    }

    @Override
    public void onDigitPressed(final int digit) {
        tvAmountA.append(String.valueOf(digit));
        doExchange();
    }

    @Override
    public void onPointPressed() {
        //TODO locale?
        if (tvAmountA.getText().toString().indexOf('.') == -1) {
            if (tvAmountA.getText().toString().isEmpty()) {
                tvAmountA.append("0");
            }
            tvAmountA.append(".");
        }
    }

    @Override
    public void onBackSpacePressed() {
        String entry = tvAmountA.getText().toString();
        int length = entry.length();
        if (length > 0) {
            tvAmountA.setText(entry.substring(0, entry.length() - 1));
            doExchange();
        }
    }

    @Override
    public void onClearAll() {
        tvAmountA.setText(null);
        doExchange();
    }
}
