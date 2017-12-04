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
import android.os.Bundle;
import android.support.design.widget.TextInputLayout;
import android.support.v7.widget.CardView;
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
import android.widget.TextView;

import com.m2049r.xmrwallet.data.BarcodeData;
import com.m2049r.xmrwallet.model.Wallet;
import com.m2049r.xmrwallet.model.WalletManager;
import com.m2049r.xmrwallet.util.Helper;

import timber.log.Timber;

public class SendAddressWizardFragment extends SendWizardFragment {

    static final int INTEGRATED_ADDRESS_LENGTH = 106;

    public static SendAddressWizardFragment newInstance(Listener listener) {
        SendAddressWizardFragment instance = new SendAddressWizardFragment();
        instance.setSendListener(listener);
        return instance;
    }

    Listener sendListener;

    public SendAddressWizardFragment setSendListener(Listener listener) {
        this.sendListener = listener;
        return this;
    }

    interface Listener {
        void setAddress(final String address);

        void setPaymentId(final String paymentId);

        void setBarcodeData(BarcodeData data);
    }

    private EditText etDummy;
    private TextInputLayout etAddress;
    private TextInputLayout etPaymentId;
    private Button bPaymentId;
    private CardView cvScan;
    private View tvPaymentIdIntegrated;
    private View llPaymentId;

    private String scannedAmount = null;

    OnScanListener onScanListener;

    public interface OnScanListener {
        void onScan();

        BarcodeData popScannedData();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Timber.d("onCreateView() %s", (String.valueOf(savedInstanceState)));

        View view = inflater.inflate(R.layout.fragment_send_address, container, false);

        tvPaymentIdIntegrated = view.findViewById(R.id.tvPaymentIdIntegrated);
        llPaymentId = view.findViewById(R.id.llPaymentId);

        etAddress = (TextInputLayout) view.findViewById(R.id.etAddress);
        etAddress.getEditText().setRawInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        etAddress.getEditText().setOnEditorActionListener(new TextView.OnEditorActionListener() {
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if ((event != null && (event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) || (actionId == EditorInfo.IME_ACTION_NEXT)) {
                    if (checkAddress()) {
                        if (llPaymentId.getVisibility() == View.VISIBLE) {
                            etPaymentId.requestFocus();
                        } else {
                            etDummy.requestFocus();
                            Helper.hideKeyboard(getActivity());
                        }
                    }
                    return true;
                }
                return false;
            }
        });
        etAddress.getEditText().addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable editable) {
                if ((etAddress.getEditText().getText().toString().length() == INTEGRATED_ADDRESS_LENGTH) &&
                        checkAddressNoError()) { // we have an integrated address
                    etPaymentId.getEditText().getText().clear();
                    llPaymentId.setVisibility(View.GONE);
                    tvPaymentIdIntegrated.setVisibility(View.VISIBLE);
                } else { // we don't
                    llPaymentId.setVisibility(View.VISIBLE);
                    tvPaymentIdIntegrated.setVisibility(View.GONE);
                }
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
        });


        etPaymentId = (TextInputLayout) view.findViewById(R.id.etPaymentId);
        etPaymentId.getEditText().setRawInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        etPaymentId.getEditText().setOnEditorActionListener(new TextView.OnEditorActionListener() {
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if ((event != null && (event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) || (actionId == EditorInfo.IME_ACTION_DONE)) {
                    if (checkPaymentId()) {
                        etDummy.requestFocus();
                        Helper.hideKeyboard(getActivity());
                    }
                    return true;
                }
                return false;
            }
        });
        etPaymentId.getEditText().addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable editable) {
                etPaymentId.setError(null);
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
        });

        bPaymentId = (Button) view.findViewById(R.id.bPaymentId);
        bPaymentId.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                etPaymentId.getEditText().setText((Wallet.generatePaymentId()));
            }
        });

        cvScan = (CardView) view.findViewById(R.id.bScan);
        cvScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onScanListener.onScan();
            }
        });


        etDummy = (EditText) view.findViewById(R.id.etDummy);
        etDummy.setRawInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        etDummy.requestFocus();
        Helper.hideKeyboard(getActivity());

        return view;
    }

    private boolean checkAddressNoError() {
        String address = etAddress.getEditText().getText().toString();
        return Wallet.isAddressValid(address, WalletManager.getInstance().isTestNet());
    }

    private boolean checkAddress() {
        boolean ok = checkAddressNoError();
        if (!ok) {
            etAddress.setError(getString(R.string.send_qr_address_invalid));
        } else {
            etAddress.setError(null);
        }
        return ok;
    }

    private boolean isIntegratedAddress() {
        String address = etAddress.getEditText().getText().toString();
        return Wallet.isAddressValid(address, WalletManager.getInstance().isTestNet())
                && address.length() == 106;
    }

    private boolean checkPaymentId() {
        String paymentId = etPaymentId.getEditText().getText().toString();
        boolean ok = paymentId.isEmpty() || Wallet.isPaymentIdValid(paymentId);
        if (!ok) {
            etPaymentId.setError(getString(R.string.receive_paymentid_invalid));
        } else {
            if (!paymentId.isEmpty() && isIntegratedAddress()) {
                ok = false;
                etPaymentId.setError(getString(R.string.receive_integrated_paymentid_invalid));
            } else {
                etPaymentId.setError(null);
            }
        }
        return ok;
    }

    @Override
    public boolean onValidateFields() {
        boolean ok = true;
        if (!checkAddressNoError()) {
            etAddress.startAnimation(Helper.getShakeAnimation(getContext()));
            ok = false;
        }
        if (!checkPaymentId()) {
            etPaymentId.startAnimation(Helper.getShakeAnimation(getContext()));
            ok = false;
        }
        if (!ok) return false;
        if (sendListener != null) {
            sendListener.setAddress(etAddress.getEditText().getText().toString());
            sendListener.setPaymentId(etPaymentId.getEditText().getText().toString());
        }
        return true;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnScanListener) {
            onScanListener = (OnScanListener) context;
        } else {
            throw new ClassCastException(context.toString()
                    + " must implement ScanListener");
        }
    }

    // QR Scan Stuff

    @Override
    public void onResume() {
        super.onResume();
        Timber.d("onResume");
        BarcodeData data = onScanListener.popScannedData();
        sendListener.setBarcodeData(data);
        if (data != null) {
            Timber.d("GOT DATA");
            String scannedAddress = data.address;
            if (scannedAddress != null) {
                etAddress.getEditText().setText(scannedAddress);
                checkAddress();
            } else {
                etAddress.getEditText().getText().clear();
                etAddress.setError(null);
            }
            String scannedPaymenId = data.paymentId;
            if (scannedPaymenId != null) {
                etPaymentId.getEditText().setText(scannedPaymenId);
                checkPaymentId();
            } else {
                etPaymentId.getEditText().getText().clear();
                etPaymentId.setError(null);
            }
        }
    }

    @Override
    public void onResumeFragment() {
        super.onResumeFragment();
        Timber.d("onResumeFragment()");
        Helper.hideKeyboard(getActivity());
        etDummy.requestFocus();
    }
}
