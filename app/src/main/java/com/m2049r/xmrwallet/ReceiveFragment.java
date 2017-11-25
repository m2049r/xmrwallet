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
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
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
import com.m2049r.xmrwallet.util.Helper;
import com.m2049r.xmrwallet.util.MoneroThreadPoolExecutor;
import com.m2049r.xmrwallet.widget.ExchangeView;
import com.m2049r.xmrwallet.widget.Toolbar;

import java.util.HashMap;
import java.util.Map;

import timber.log.Timber;

public class ReceiveFragment extends Fragment {

    private ProgressBar pbProgress;
    private TextView tvAddress;
    private TextInputLayout etPaymentId;
    private ExchangeView evAmount;
    private Button bPaymentId;
    private TextView tvQrCode;
    private ImageView qrCode;
    private ImageView qrCodeFull;
    private EditText etDummy;
    private ImageButton bCopyAddress;

    public interface Listener {
        void setToolbarButton(int type);

        void setTitle(String title);

        void setSubtitle(String subtitle);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_receive, container, false);

        pbProgress = (ProgressBar) view.findViewById(R.id.pbProgress);
        tvAddress = (TextView) view.findViewById(R.id.tvAddress);
        etPaymentId = (TextInputLayout) view.findViewById(R.id.etPaymentId);
        evAmount = (ExchangeView) view.findViewById(R.id.evAmount);
        bPaymentId = (Button) view.findViewById(R.id.bPaymentId);
        qrCode = (ImageView) view.findViewById(R.id.qrCode);
        tvQrCode = (TextView) view.findViewById(R.id.tvQrCode);
        qrCodeFull = (ImageView) view.findViewById(R.id.qrCodeFull);
        etDummy = (EditText) view.findViewById(R.id.etDummy);
        bCopyAddress = (ImageButton) view.findViewById(R.id.bCopyAddress);

        etPaymentId.getEditText().setRawInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        etDummy.setRawInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);

        bCopyAddress.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                copyAddress();
            }
        });

        evAmount.setOnNewAmountListener(new ExchangeView.OnNewAmountListener() {
            @Override
            public void onNewAmount(String xmr) {
                Timber.d("new amount = %s", xmr);
                generateQr();
            }
        });

        evAmount.setOnFailedExchangeListener(new ExchangeView.OnFailedExchangeListener() {
            @Override
            public void onFailedExchange() {
                if (isAdded()) {
                    clearQR();
                    Toast.makeText(getActivity(), getString(R.string.message_exchange_failed), Toast.LENGTH_LONG).show();
                }
            }
        });

        etPaymentId.getEditText().setOnEditorActionListener(new TextView.OnEditorActionListener() {
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if ((event != null && (event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) || (actionId == EditorInfo.IME_ACTION_DONE)) {
                    if (checkPaymentId()) { // && evAmount.checkXmrAmount(true)) {
                        generateQr();
                    }
                    return true;
                }
                return false;
            }
        });
        etPaymentId.getEditText().addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable editable) {
                clearQR();
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
                etPaymentId.getEditText().setText((Wallet.generatePaymentId()));
                etPaymentId.getEditText().setSelection(etPaymentId.getEditText().getText().length());
                if (checkPaymentId()) { //&& evAmount.checkXmrAmount(true)) {
                    generateQr();
                }
            }
        });

        qrCode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (qrValid) {
                    qrCodeFull.setImageBitmap(((BitmapDrawable) qrCode.getDrawable()).getBitmap());
                    qrCodeFull.setVisibility(View.VISIBLE);
                } else if (checkPaymentId()) {
                    evAmount.doExchange();
                }
            }
        });

        qrCodeFull.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                qrCodeFull.setImageBitmap(null);
                qrCodeFull.setVisibility(View.GONE);
            }
        });

        showProgress();
        clearQR();

        Bundle b = getArguments();
        String address = b.getString("address");
        String walletName = b.getString("name");
        Timber.d("%s/%s", address, walletName);
        if (address == null) {
            String path = b.getString("path");
            String password = b.getString("password");
            loadAndShow(path, password);
        } else {
            show(walletName, address);
        }
        return view;
    }

    void copyAddress() {
        Helper.clipBoardCopy(getActivity(), getString(R.string.label_copy_address), tvAddress.getText().toString());
        Toast.makeText(getActivity(), getString(R.string.message_copy_address), Toast.LENGTH_SHORT).show();
    }

    boolean qrValid = true;

    void clearQR() {
        if (qrValid) {
            qrCode.setImageBitmap(null);
            qrValid = false;
            if (isLoaded)
                tvQrCode.setVisibility(View.VISIBLE);
        }
    }

    void setQR(Bitmap qr) {
        qrCode.setImageBitmap(qr);
        qrValid = true;
        tvQrCode.setVisibility(View.INVISIBLE);
        Helper.hideKeyboard(getActivity());
        etDummy.requestFocus();
    }

    @Override
    public void onResume() {
        super.onResume();
        Timber.d("onResume()");
        listenerCallback.setToolbarButton(Toolbar.BUTTON_BACK);
        listenerCallback.setSubtitle(getString(R.string.receive_title));
        generateQr();
    }

    private boolean isLoaded = false;

    private void show(String name, String address) {
        Timber.d("name=%s", name);
        isLoaded = true;
        listenerCallback.setTitle(name);
        tvAddress.setText(address);
        etPaymentId.setEnabled(true);
        bPaymentId.setEnabled(true);
        bCopyAddress.setEnabled(true);
        bCopyAddress.setImageResource(R.drawable.ic_content_copy_black_24dp);
        hideProgress();
        generateQr();
    }

    private void loadAndShow(String walletPath, String password) {
        new AsyncShow().executeOnExecutor(MoneroThreadPoolExecutor.MONERO_THREAD_POOL_EXECUTOR,
                walletPath, password);
    }

    private class AsyncShow extends AsyncTask<String, Void, Boolean> {
        String password;
        String address;
        String name;

        @Override
        protected Boolean doInBackground(String... params) {
            if (params.length != 2) return false;
            String walletPath = params[0];
            password = params[1];
            Wallet wallet = WalletManager.getInstance().openWallet(walletPath, password);
            address = wallet.getAddress();
            name = wallet.getName();
            wallet.close();
            return true;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            super.onPostExecute(result);
            if (!isAdded()) return; // never mind
            if (result) {
                show(name, address);
            } else {
                Toast.makeText(getActivity(), getString(R.string.receive_cannot_open), Toast.LENGTH_LONG).show();
                hideProgress();
            }
        }
    }

    private boolean checkPaymentId() {
        String paymentId = etPaymentId.getEditText().getText().toString();
        boolean ok = paymentId.isEmpty() || Wallet.isPaymentIdValid(paymentId);

        if (!ok) {
            etPaymentId.setError(getString(R.string.receive_paymentid_invalid));
        } else {
            etPaymentId.setError(null);
        }
        return ok;
    }

    private void generateQr() {
        Timber.d("GENQR");
        String address = tvAddress.getText().toString();
        String paymentId = etPaymentId.getEditText().getText().toString();
        String xmrAmount = evAmount.getAmount();
        Timber.d("%s/%s/%s", xmrAmount, paymentId, address);
        if ((xmrAmount == null) || !Wallet.isAddressValid(address, WalletManager.getInstance().isTestNet())) {
            clearQR();
            Timber.d("CLEARQR");
            return;
        }
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
        int size = Math.min(qrCode.getHeight(), qrCode.getWidth());
        Bitmap qr = generate(text, size, size);
        if (qr != null) {
            setQR(qr);
            Timber.d("SETQR");
            etDummy.requestFocus();
            Helper.hideKeyboard(getActivity());
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
            logo = Helper.getBitmap(getContext(), R.drawable.ic_monero_logo_b);
        }
        return logo;
    }

    public void showProgress() {
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

    @Override
    public void onPause() {
        Timber.d("onPause()");
        super.onPause();
    }
}
