/*
 * Copyright (c) 2017 dm77, m2049r
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
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.Result;
import com.m2049r.xmrwallet.model.WalletManager;
import com.m2049r.xmrwallet.util.Helper;

import me.dm7.barcodescanner.zxing.ZXingScannerView;

public class ScannerFragment extends Fragment implements ZXingScannerView.ResultHandler {
    static final String TAG = "ScannerFragment";

    Listener activityCallback;

    public interface Listener {
        void onAddressScanned(String address, String paymentId);

        boolean isPaymentIdValid(String paymentId);
    }

    private ZXingScannerView mScannerView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView");
        mScannerView = new ZXingScannerView(getActivity());
        return mScannerView;
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
        mScannerView.setResultHandler(this);
        mScannerView.startCamera();
    }

    static final String URI_PREFIX = "monero:";
    static final String PAYMENTID_STRING = "?tx_payment_id=";

    @Override
    public void handleResult(Result rawResult) {
        Log.d(TAG, rawResult.getBarcodeFormat().toString() + "/" + rawResult.getText());
        String text = rawResult.getText();
        if ((rawResult.getBarcodeFormat() == BarcodeFormat.QR_CODE) &&
                (text.startsWith(URI_PREFIX))) {
            String address = null;
            String paymentId = null;
            String s = text.substring(URI_PREFIX.length());
            if (s.length() == 95) {
                address = s;
            } else {
                int i = s.indexOf(PAYMENTID_STRING);
                if ((i == 95) && (s.length() == (95 + PAYMENTID_STRING.length() + 16))) {
                    address = s.substring(0, 95);
                    paymentId = s.substring(95 + PAYMENTID_STRING.length());
                    if (!activityCallback.isPaymentIdValid(paymentId)) {
                        address = null;
                    }
                }
            }
            if (Helper.isAddressOk(address, WalletManager.getInstance().isTestNet())) {
                activityCallback.onAddressScanned(address, paymentId);
                return;
            } else {
                Toast.makeText(getActivity(), getString(R.string.send_qr_address_invalid), Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(getActivity(), getString(R.string.send_qr_invalid), Toast.LENGTH_SHORT).show();
        }

        // Note from dm77:
        // * Wait 2 seconds to resume the preview.
        // * On older devices continuously stopping and resuming camera preview can result in freezing the app.
        // * I don't know why this is the case but I don't have the time to figure out.
        Handler handler = new Handler();
        handler.postDelayed(new

                                    Runnable() {
                                        @Override
                                        public void run() {
                                            mScannerView.resumeCameraPreview(ScannerFragment.this);
                                        }
                                    }, 2000);
    }

    @Override
    public void onPause() {
        Log.d(TAG, "onPause");
        mScannerView.stopCamera();
        super.onPause();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        Log.d(TAG, "attaching scan");
        if (context instanceof Listener) {
            this.activityCallback = (Listener) context;
        } else {
            throw new ClassCastException(context.toString()
                    + " must implement Listener");
        }
    }
}