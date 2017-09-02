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

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Bundle;
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
import android.widget.ImageView;
import android.widget.TextView;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.m2049r.xmrwallet.model.Wallet;
import com.m2049r.xmrwallet.util.Helper;

import java.util.HashMap;
import java.util.Map;

public class ReceiveFragment extends Fragment {
    static final String TAG = "ReceiveFragment";

    TextView tvAddress;
    EditText etPaymentId;
    EditText etAmount;
    Button bPaymentId;
    Button bGenerate;
    ImageView qrCode;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.receive_fragment, container, false);

        tvAddress = (TextView) view.findViewById(R.id.tvAddress);
        etPaymentId = (EditText) view.findViewById(R.id.etPaymentId);
        etAmount = (EditText) view.findViewById(R.id.etAmount);
        bPaymentId = (Button) view.findViewById(R.id.bPaymentId);
        qrCode = (ImageView) view.findViewById(R.id.qrCode);
        bGenerate = (Button) view.findViewById(R.id.bGenerate);

        etPaymentId.setRawInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);

        Helper.showKeyboard(getActivity());
        etPaymentId.requestFocus();
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
                qrCode.setImageBitmap(null);
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
                if ((event != null && (event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) || (actionId == EditorInfo.IME_ACTION_NEXT)) {
                    if (paymentIdOk() && amountOk()) {
                        Helper.hideKeyboard(getActivity());
                        generateQr();
                    }
                    return true;
                }
                return false;
            }
        });
        etAmount.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable editable) {
                qrCode.setImageBitmap(null);
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
                    generateQr();
                }
            }
        });

        bGenerate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (paymentIdOk() && amountOk()) {
                    Helper.hideKeyboard(getActivity());
                    generateQr();
                }
            }
        });

        Bundle b = getArguments();
        String address = b.getString("address", "");
        tvAddress.setText(address);
        return view;
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

    private void generateQr() {
        String address = tvAddress.getText().toString();
        String paymentId = etPaymentId.getText().toString();
        String enteredAmount = etAmount.getText().toString();
        // that's a lot of converting ...
        String amount = (enteredAmount.isEmpty()?enteredAmount:Helper.getDisplayAmount(Wallet.getAmountFromString(enteredAmount)));
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
        if (!amount.isEmpty()) {
            if (first) {
                sb.append("?");
            } else {
                sb.append("&");
            }
            sb.append(ScannerFragment.QR_AMOUNT).append('=').append(amount);
        }
        String text = sb.toString();
        Bitmap qr = generate(text, 500, 500);
        if (qr != null) {
            qrCode.setImageBitmap(qr);
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

}
