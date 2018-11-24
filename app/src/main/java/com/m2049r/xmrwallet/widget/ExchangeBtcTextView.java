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
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.m2049r.xmrwallet.R;
import com.m2049r.xmrwallet.util.Helper;

import timber.log.Timber;

public class ExchangeBtcTextView extends LinearLayout
        implements NumberPadView.NumberPadListener {

    String btcAmount = null;
    String xmrAmount = null;

    private boolean validate(String amount, double max, double min) {
        boolean ok = true;
        if (amount != null) {
            try {
                double x = Double.parseDouble(amount);
                if ((x < min) || (x > max)) {
                    ok = false;
                }
            } catch (NumberFormatException ex) {
                Timber.e(ex.getLocalizedMessage());
                ok = false;
            }
        } else {
            ok = false;
        }
        return ok;
    }

    public boolean validate(double maxBtc, double minBtc) {
        Timber.d("validate(maxBtc=%f,minBtc=%f)", maxBtc, minBtc);
        boolean ok = true;
        if (!validate(btcAmount, maxBtc, minBtc)) {
            Timber.d("btcAmount invalid %s", btcAmount);
            shakeAmountField();
            return false;
        }
        return true;
    }

    void shakeAmountField() {
        tvAmountA.startAnimation(Helper.getShakeAnimation(getContext()));
    }

    void shakeExchangeField() {
        tvAmountB.startAnimation(Helper.getShakeAnimation(getContext()));
    }

    public void setRate(double xmrBtcRate) {
        this.xmrBtcRate = xmrBtcRate;
        post(new Runnable() {
            @Override
            public void run() {
                exchange();
            }
        });
    }

    public void setAmount(String btcAmount) {
        this.btcAmount = btcAmount;
        tvAmountA.setText(btcAmount);
        xmrAmount = null;
        exchange();
    }

    public String getAmount() {
        return btcAmount;
    }

    TextView tvAmountA;
    TextView tvAmountB;
    Spinner sCurrencyA;
    Spinner sCurrencyB;

    public ExchangeBtcTextView(Context context) {
        super(context);
        initializeViews(context);
    }

    public ExchangeBtcTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initializeViews(context);
    }

    public ExchangeBtcTextView(Context context,
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
        inflater.inflate(R.layout.view_exchange_btc_text, this);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        tvAmountA = findViewById(R.id.tvAmountA);
        tvAmountB = findViewById(R.id.tvAmountB);
        sCurrencyA = findViewById(R.id.sCurrencyA);
        sCurrencyB = findViewById(R.id.sCurrencyB);

        ArrayAdapter<String> btcAdapter = new ArrayAdapter<String>(getContext(),
                android.R.layout.simple_spinner_item,
                new String[]{"BTC"});
        sCurrencyA.setAdapter(btcAdapter);
        sCurrencyA.setEnabled(false);
        ArrayAdapter<String> xmrAdapter = new ArrayAdapter<String>(getContext(),
                android.R.layout.simple_spinner_item,
                new String[]{"XMR"});
        sCurrencyB.setAdapter(xmrAdapter);
        sCurrencyB.setEnabled(false);
    }

    double xmrBtcRate = 0;

    public void exchange() {
        btcAmount = tvAmountA.getText().toString();
        if (!btcAmount.isEmpty() && (xmrBtcRate > 0)) {
            double xmr = xmrBtcRate * Double.parseDouble(btcAmount);
            xmrAmount = Helper.getFormattedAmount(xmr, true);
        } else {
            xmrAmount = "";
        }
        tvAmountB.setText(getResources().getString(R.string.send_amount_btc_xmr, xmrAmount));
        Timber.d("%s BTC =%f> %s XMR", btcAmount, xmrBtcRate, xmrAmount);
    }

    // deal with attached numpad
    @Override
    public void onDigitPressed(final int digit) {
        tvAmountA.append(String.valueOf(digit));
        exchange();
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
            exchange();
        }
    }

    @Override
    public void onClearAll() {
        tvAmountA.setText(null);
        exchange();
    }
}