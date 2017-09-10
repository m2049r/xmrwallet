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

package com.m2049r.xmrwallet;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.m2049r.xmrwallet.model.Wallet;
import com.m2049r.xmrwallet.model.WalletManager;
import com.m2049r.xmrwallet.util.AsyncExchangeRate;
import com.m2049r.xmrwallet.util.Helper;
import com.m2049r.xmrwallet.util.MoneroThreadPoolExecutor;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class ReceiveFragment extends Fragment implements AsyncExchangeRate.Listener {
    static final String TAG = "ReceiveFragment";

    ProgressBar pbProgress;
    TextView tvAddress;
    EditText etPaymentId;
    EditText etAmount;
    TextView tvAmountB;
    Button bPaymentId;
    Button bGenerate;
    ImageView qrCode;
    EditText etDummy;

    Spinner sCurrencyA;
    Spinner sCurrencyB;

    public interface Listener {
        void onExchange(AsyncExchangeRate.Listener listener, String currencyA, String currencyB);
    }

    @Override
    public void exchange(String currencyA, String currencyB, double rate) {
        // first, make sure this is what we want
        String enteredCurrencyA = (String) sCurrencyA.getSelectedItem();
        String enteredCurrencyB = (String) sCurrencyB.getSelectedItem();
        if (!currencyA.equals(enteredCurrencyA) || !currencyB.equals(enteredCurrencyB)) {
            // something's wrong
            Log.e(TAG, "Currencies don't match!");
            tvAmountB.setText("");
            return;
        }
        String enteredAmount = etAmount.getText().toString();
        String xmrAmount = "";
        if (!enteredAmount.isEmpty()) {
            // losing precision using double here doesn't matter
            double amountA = Double.parseDouble(enteredAmount);
            double amountB = amountA * rate;
            if (enteredCurrencyA.equals("XMR")) {
                String validatedAmountA = Helper.getDisplayAmount(Wallet.getAmountFromString(enteredAmount));
                xmrAmount = validatedAmountA; // take what was entered in XMR
                etAmount.setText(xmrAmount); // display what we stick into the QR code
                String displayB = String.format(Locale.US, "%.2f", amountB);
                tvAmountB.setText(displayB);
            } else if (enteredCurrencyB.equals("XMR")) {
                xmrAmount = Wallet.getDisplayAmount(Wallet.getAmountFromDouble(amountB));
                // cut off at 5 decimals
                xmrAmount = xmrAmount.substring(0, xmrAmount.length() - (12 - 5));
                tvAmountB.setText(xmrAmount);
            } else { // no XMR currency
                tvAmountB.setText("");
                return; // and no qr code
            }
        } else {
            tvAmountB.setText("");
        }
        generateQr(xmrAmount);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.receive_fragment, container, false);

        pbProgress = (ProgressBar) view.findViewById(R.id.pbProgress);
        tvAddress = (TextView) view.findViewById(R.id.tvAddress);
        etPaymentId = (EditText) view.findViewById(R.id.etPaymentId);
        etAmount = (EditText) view.findViewById(R.id.etAmountA);
        tvAmountB = (TextView) view.findViewById(R.id.tvAmountB);
        bPaymentId = (Button) view.findViewById(R.id.bPaymentId);
        qrCode = (ImageView) view.findViewById(R.id.qrCode);
        bGenerate = (Button) view.findViewById(R.id.bGenerate);
        etDummy = (EditText) view.findViewById(R.id.etDummy);

        sCurrencyA = (Spinner) view.findViewById(R.id.sCurrencyA);
        sCurrencyB = (Spinner) view.findViewById(R.id.sCurrencyB);

        etPaymentId.setRawInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        etDummy.setRawInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);

        loadPrefs();

        etPaymentId.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if ((event != null && (event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) || (actionId == EditorInfo.IME_ACTION_NEXT)) {
                    if (paymentIdOk()) {
                        etAmount.requestFocus();
                    } // otherwise ignore
                    return true;
                }
                return false;
            }
        });
        etPaymentId.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable editable) {
                qrCode.setImageBitmap(getMoneroLogo());
                if (paymentIdOk() && amountOk()) {
                    bGenerate.setEnabled(true);
                } else {
                    bGenerate.setEnabled(false);
                }
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
        });

        etAmount.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if ((event != null && (event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) || (actionId == EditorInfo.IME_ACTION_DONE)) {
                    if (paymentIdOk() && amountOk()) {
                        Helper.hideKeyboard(getActivity());
                        startExchange();
                    }
                    return true;
                }
                return false;
            }
        });
        etAmount.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable editable) {
                tvAmountB.setText("");
                qrCode.setImageBitmap(getMoneroLogo());
                if (paymentIdOk() && amountOk()) {
                    bGenerate.setEnabled(true);
                } else {
                    bGenerate.setEnabled(false);
                }
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
        });

        bPaymentId.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                etPaymentId.setText((Wallet.generatePaymentId()));
                etPaymentId.setSelection(etPaymentId.getText().length());
                if (paymentIdOk() && amountOk()) {
                    startExchange();
                }
            }
        });

        bGenerate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (paymentIdOk() && amountOk()) {
                    Helper.hideKeyboard(getActivity());
                    startExchange();
                }
            }
        });

        sCurrencyA.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                if (position != 0) {
                    sCurrencyB.setSelection(0, true);
                }
                startExchange();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                // nothing (yet?)
            }
        });

        sCurrencyB.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                if (position != 0) {
                    sCurrencyA.setSelection(0, true);
                }
                tvAmountB.setText("");
                startExchange();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                // nothing (yet?)
            }
        });

        showProgress();
        qrCode.setImageBitmap(getMoneroLogo());

        Bundle b = getArguments();
        String address = b.getString("address");
        if (address == null) {
            String path = b.getString("path");
            String password = b.getString("password");
            show(path, password);
        } else {
            show(address);
        }
        return view;
    }

    void startExchange() {
        if (paymentIdOk() && amountOk() && tvAddress.getText().length() > 0) {
            String enteredCurrencyA = (String) sCurrencyA.getSelectedItem();
            String enteredCurrencyB = (String) sCurrencyB.getSelectedItem();
            String enteredAmount = etAmount.getText().toString();
            tvAmountB.setText("");
            if (!enteredAmount.isEmpty()) { // start conversion
                listenerCallback.onExchange(ReceiveFragment.this, enteredCurrencyA, enteredCurrencyB);
            } else {
                generateQr("");
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume()");
        if (paymentIdOk() && amountOk() && tvAddress.getText().length() > 0) {
            startExchange();
        }
    }

    private void show(String address) {
        tvAddress.setText(address);
        etPaymentId.setEnabled(true);
        etAmount.setEnabled(true);
        bPaymentId.setEnabled(true);
        bGenerate.setEnabled(true);
        hideProgress();
        startExchange();
    }

    private void show(String walletPath, String password) {
        new AsyncShow().executeOnExecutor(MoneroThreadPoolExecutor.MONERO_THREAD_POOL_EXECUTOR,
                walletPath, password);
    }

    private class AsyncShow extends AsyncTask<String, Void, Boolean> {
        String password;

        String address;

        @Override
        protected Boolean doInBackground(String... params) {
            if (params.length != 2) return false;
            String walletPath = params[0];
            password = params[1];
            Wallet wallet = WalletManager.getInstance().openWallet(walletPath, password);
            address = wallet.getAddress();
            wallet.close();
            return true;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            super.onPostExecute(result);
            if (result) {
                show(address);
            } else {
                Toast.makeText(getActivity(), getString(R.string.receive_cannot_open), Toast.LENGTH_LONG).show();
                hideProgress();
            }
        }
    }


    private boolean amountOk() {
        String amountEntry = etAmount.getText().toString();
        if (amountEntry.isEmpty()) return true;
        long amount = Wallet.getAmountFromString(amountEntry);
        return (amount > 0);
    }

    private boolean paymentIdOk() {
        String paymentId = etPaymentId.getText().toString();
        return paymentId.isEmpty() || Wallet.isPaymentIdValid(paymentId);
    }

    private void generateQr(String xmrAmount) {
        String address = tvAddress.getText().toString();
        String paymentId = etPaymentId.getText().toString();
        StringBuffer sb = new StringBuffer();
        sb.append(ScannerFragment.QR_SCHEME).append(address);
        boolean first = true;
        if (!paymentId.isEmpty()) {
            if (first) {
                sb.append("?");
                first = false;
            }
            sb.append(ScannerFragment.QR_PAYMENTID).append('=').append(paymentId);
        }
        if (!xmrAmount.isEmpty()) {
            if (first) {
                sb.append("?");
            } else {
                sb.append("&");
            }
            sb.append(ScannerFragment.QR_AMOUNT).append('=').append(xmrAmount);
        }
        String text = sb.toString();
        Bitmap qr = generate(text, 500, 500);
        if (qr != null) {
            qrCode.setImageBitmap(qr);
            etDummy.requestFocus();
            bGenerate.setEnabled(false);
        }
    }

    public Bitmap generate(String text, int width, int height) {
        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.CHARACTER_SET, "utf-8");
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
        try {
            BitMatrix bitMatrix = new QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, width, height, hints);
            int[] pixels = new int[width * height];
            for (int i = 0; i < height; i++) {
                for (int j = 0; j < width; j++) {
                    if (bitMatrix.get(j, i)) {
                        pixels[i * width + j] = 0x00000000;
                    } else {
                        pixels[i * height + j] = 0xffffffff;
                    }
                }
            }
            Bitmap bitmap = Bitmap.createBitmap(pixels, 0, width, width, height, Bitmap.Config.RGB_565);
            bitmap = addLogo(bitmap);
            return bitmap;
        } catch (WriterException e) {
            e.printStackTrace();
        }
        return null;
    }

    // TODO check if we can sensibly cache some of this
    private Bitmap addLogo(Bitmap qrBitmap) {
        Bitmap logo = getMoneroLogo();
        int qrWidth = qrBitmap.getWidth();
        int qrHeight = qrBitmap.getHeight();
        int logoWidth = logo.getWidth();
        int logoHeight = logo.getHeight();

        Bitmap logoBitmap = Bitmap.createBitmap(qrWidth, qrHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(logoBitmap);
        canvas.drawBitmap(qrBitmap, 0, 0, null);
        canvas.save(Canvas.ALL_SAVE_FLAG);
        // figure out how to scale the logo
        float scaleSize = 1.0f;
        while ((logoWidth / scaleSize) > (qrWidth / 5) || (logoHeight / scaleSize) > (qrHeight / 5)) {
            scaleSize *= 2;
        }
        float sx = 1.0f / scaleSize;
        canvas.scale(sx, sx, qrWidth / 2, qrHeight / 2);
        canvas.drawBitmap(logo, (qrWidth - logoWidth) / 2, (qrHeight - logoHeight) / 2, null);
        canvas.restore();
        return logoBitmap;
    }

    private Bitmap logo = null;

    private Bitmap getMoneroLogo() {
        if (logo == null) {
            logo = Helper.getBitmap(getContext(), R.drawable.ic_monero_qr);
        }
        return logo;
    }

    public void showProgress() {
        pbProgress.setIndeterminate(true);
        pbProgress.setVisibility(View.VISIBLE);
    }

    public void hideProgress() {
        pbProgress.setVisibility(View.GONE);
    }

    Listener listenerCallback = null;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof Listener) {
            this.listenerCallback = (Listener) context;
        } else {
            throw new ClassCastException(context.toString()
                    + " must implement Listener");
        }
    }

    static final String PREF_CURRENCY_A = "PREF_CURRENCY_A";
    static final String PREF_CURRENCY_B = "PREF_CURRENCY_B";

    void loadPrefs() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext());
        int currencyA = sharedPreferences.getInt(PREF_CURRENCY_A, 0);
        int currencyB = sharedPreferences.getInt(PREF_CURRENCY_B, 0);

        if (currencyA * currencyB != 0) { // make sure one of them is 0 (=XMR)
            currencyA = 0;
        }
        // in case we change the currency lists in the future
        if (currencyA >= sCurrencyA.getCount()) currencyA = 0;
        if (currencyB >= sCurrencyB.getCount()) currencyB = 0;
        sCurrencyA.setSelection(currencyA);
        sCurrencyB.setSelection(currencyB);
    }

    void savePrefs() {
        int currencyA = sCurrencyA.getSelectedItemPosition();
        int currencyB = sCurrencyB.getSelectedItemPosition();

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext());
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(PREF_CURRENCY_A, currencyA);
        editor.putInt(PREF_CURRENCY_B, currencyB);
        editor.apply();
    }

    @Override
    public void onPause() {
        Log.d(TAG, "onPause()");
        savePrefs();
        super.onPause();
    }
}
