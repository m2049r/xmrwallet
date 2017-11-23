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

package com.m2049r.xmrwallet.layout;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.support.design.widget.TextInputLayout;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
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
import com.m2049r.xmrwallet.service.exchange.kraken.ExchangeApiImpl;
import com.m2049r.xmrwallet.util.Helper;
import com.m2049r.xmrwallet.util.OkHttpClientSingleton;

import java.util.Locale;

import okhttp3.OkHttpClient;

public class ExchangeView extends LinearLayout {
    static final String TAG = "ExchangeView";

    public boolean focus() {
        return etAmount.requestFocus();
    }

    public void enable(boolean enable) {
        etAmount.setEnabled(enable);
        sCurrencyA.setEnabled(enable);
        sCurrencyB.setEnabled(enable);
    }

    String xmrAmount = null;
    String notXmrAmount = null;

    void setXmr(String xmr) {
        xmrAmount = xmr;
        if (onNewAmountListener != null) {
            onNewAmountListener.onNewAmount(xmr);
        }
    }

    public void setAmount(String xmrAmount) {
        if (xmrAmount != null) {
            setCurrencyA(0);
            etAmount.getEditText().setText(xmrAmount);
            setXmr(xmrAmount);
            this.notXmrAmount = null;
            doExchange();
        } else {
            setXmr(null);
            this.notXmrAmount = null;
            tvAmountB.setText("--");
        }
    }

    public String getAmount() {
        return xmrAmount;
    }

    public void setError(String msg) {
        etAmount.setError(msg);
    }

    TextInputLayout etAmount;
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

    public ExchangeView(Context context) {
        super(context);
        initializeViews(context);
    }

    public ExchangeView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initializeViews(context);
    }

    public ExchangeView(Context context,
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
        inflater.inflate(R.layout.view_exchange, this);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        etAmount = (TextInputLayout) findViewById(R.id.etAmount);
        tvAmountB = (TextView) findViewById(R.id.tvAmountB);
        sCurrencyA = (Spinner) findViewById(R.id.sCurrencyA);
        sCurrencyB = (Spinner) findViewById(R.id.sCurrencyB);
        evExchange = (ImageView) findViewById(R.id.evExchange);
        pbExchange = (ProgressBar) findViewById(R.id.pbExchange);

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
                // nothing (yet?)
            }
        });

        sCurrencyB.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(final AdapterView<?> parentView, View selectedItemView, int position, long id) {
                if (position != 0) { // if not XMR, select XMR on other
                    sCurrencyA.setSelection(0, true);
                }
                parentView.post(new Runnable() {
                    @Override
                    public void run() {
                        ((TextView) parentView.getChildAt(0)).setTextColor(getResources().getColor(R.color.moneroGray));
                    }
                });
                doExchange();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                // nothing (yet?)
            }
        });

        etAmount.getEditText().setOnFocusChangeListener(new OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    doExchange();
                }
            }
        });

        etAmount.getEditText().setOnEditorActionListener(new TextView.OnEditorActionListener() {
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if ((event != null && (event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) || (actionId == EditorInfo.IME_ACTION_DONE)) {
                    doExchange();
                    return true;
                }
                return false;
            }
        });


        etAmount.getEditText().addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable editable) {
                etAmount.setError(null);
                if ((xmrAmount != null) || (notXmrAmount != null)) {
                    tvAmountB.setText("--");
                    setXmr(null);
                    notXmrAmount = null;
                }
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
        });

    }

    final static double MAX_AMOUNT_XMR = 1000;
    final static double MAX_AMOUNT_NOTXMR = 100000;

    public boolean checkEnteredAmount() {
        boolean ok = true;
        Log.d(TAG, "checkEnteredAmount");
        String amountEntry = etAmount.getEditText().getText().toString();
        if (!amountEntry.isEmpty()) {
            try {
                double a = Double.parseDouble(amountEntry);
                double maxAmount = (getCurrencyA() == 0) ? MAX_AMOUNT_XMR : MAX_AMOUNT_NOTXMR;
                if (a > (maxAmount)) {
                    etAmount.setError(getResources().
                            getString(R.string.receive_amount_too_big,
                                    String.format(Locale.US, "%,.0f", maxAmount)));
                    ok = false;
                } else if (a < 0) {
                    etAmount.setError(getResources().getString(R.string.receive_amount_negative));
                    ok = false;
                }
            } catch (NumberFormatException ex) {
                etAmount.setError(getResources().getString(R.string.receive_amount_nan));
                ok = false;
            }
        }
        if (ok) {
            etAmount.setError(null);
        }
        return ok;
    }

    public void doExchange() {
        tvAmountB.setText("--");
        // TODO cache & use cached exchange rate here
        startExchange();
    }

    private final ExchangeApi exchangeApi = new ExchangeApiImpl(OkHttpClientSingleton.getOkHttpClient());

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
                        Log.e(TAG, e.getLocalizedMessage());
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
        if (getCurrencyA() == 0) {
            if (xmrAmount == null) return;
            if (!xmrAmount.isEmpty() && (rate > 0)) {
                double amountB = rate * Double.parseDouble(xmrAmount);
                notXmrAmount = Helper.getFormattedAmount(amountB, getCurrencyB() == 0);
            } else {
                notXmrAmount = "";
            }
            tvAmountB.setText(notXmrAmount);
        } else if (getCurrencyB() == 0) {
            if (notXmrAmount == null) return;
            if (!notXmrAmount.isEmpty() && (rate > 0)) {
                double amountB = rate * Double.parseDouble(notXmrAmount);
                setXmr(Helper.getFormattedAmount(amountB, true));
            } else {
                setXmr("");
            }
            tvAmountB.setText(xmrAmount);
        } else { // no XMR currency - cannot happen!
            Log.e(TAG, "No XMR currency!");
            setXmr(null);
            notXmrAmount = null;
            return;
        }
    }

    boolean prepareExchange() {
        Log.d(TAG, "prepareExchange()");
        if (checkEnteredAmount()) {
            String enteredAmount = etAmount.getEditText().getText().toString();
            if (!enteredAmount.isEmpty()) {
                String cleanAmount = "";
                if (getCurrencyA() == 0) {
                    // sanitize the input
                    cleanAmount = Helper.getDisplayAmount(Wallet.getAmountFromString(enteredAmount));
                    setXmr(cleanAmount);
                    notXmrAmount = null;
                    Log.d(TAG, "cleanAmount = " + cleanAmount);
                } else if (getCurrencyB() == 0) { // we use B & 0 here for the else below ...
                    // sanitize the input
                    double amountA = Double.parseDouble(enteredAmount);
                    cleanAmount = String.format(Locale.US, "%.2f", amountA);
                    setXmr(null);
                    notXmrAmount = cleanAmount;
                } else { // no XMR currency - cannot happen!
                    Log.e(TAG, "No XMR currency!");
                    setXmr(null);
                    notXmrAmount = null;
                    return false;
                }
                Log.d(TAG, "prepareExchange() " + cleanAmount);
                //etAmount.getEditText().setText(cleanAmount); // display what we use
            } else {
                setXmr("");
                notXmrAmount = "";
            }
            return true;
        } else {
            setXmr(null);
            notXmrAmount = null;
            return false;
        }
    }

    public void exchangeFailed() {
        hideProgress();
        exchange(0);
        if (onFailedExchangeListener != null) {
            onFailedExchangeListener.onFailedExchange();
        }
    }

    public void exchange(ExchangeRate exchangeRate) {
        hideProgress();
        // first, make sure this is what we want
        String enteredCurrencyA = (String) sCurrencyA.getSelectedItem();
        String enteredCurrencyB = (String) sCurrencyB.getSelectedItem();
        if (!exchangeRate.getBaseCurrency().equals(enteredCurrencyA)
                || !exchangeRate.getQuoteCurrency().equals(enteredCurrencyB)) {
            // something's wrong
            Log.e(TAG, "Currencies don't match!");
            return;
        }
        if (prepareExchange()) {
            exchange(exchangeRate.getRate());
        }
    }

    private void showProgress() {
        pbExchange.setVisibility(View.VISIBLE);
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
}