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
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.m2049r.xmrwallet.R;
import com.m2049r.xmrwallet.util.Helper;

import timber.log.Timber;

public class ExchangeBtcEditText extends LinearLayout {

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
        etAmountA.startAnimation(Helper.getShakeAnimation(getContext()));
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
        etAmountA.setText(btcAmount);
        xmrAmount = null;
        exchange();
    }

    public void setEditable(boolean editable) {
        etAmountA.setEnabled(editable);
    }

    public String getAmount() {
        return btcAmount;
    }

    EditText etAmountA;
    TextView tvAmountB;
    Spinner sCurrencyA;
    Spinner sCurrencyB;

    public ExchangeBtcEditText(Context context) {
        super(context);
        initializeViews(context);
    }

    public ExchangeBtcEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        initializeViews(context);
    }

    public ExchangeBtcEditText(Context context,
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
        inflater.inflate(R.layout.view_exchange_btc_edit, this);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        etAmountA = findViewById(R.id.etAmountA);
        etAmountA.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                exchange();
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
        etAmountA.setFocusable(true);
        etAmountA.setFocusableInTouchMode(true);
    }

    double xmrBtcRate = 0;

    public void exchange() {
        btcAmount = etAmountA.getText().toString();
        if (!btcAmount.isEmpty() && (xmrBtcRate > 0)) {
            double xmr = xmrBtcRate * Double.parseDouble(btcAmount);
            xmrAmount = Helper.getFormattedAmount(xmr, true);
        } else {
            xmrAmount = "";
        }
        tvAmountB.setText(getResources().getString(R.string.send_amount_btc_xmr, xmrAmount));
        Timber.d("%s BTC =%f> %s XMR", btcAmount, xmrBtcRate, xmrAmount);
    }
}