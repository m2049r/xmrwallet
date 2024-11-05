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

package com.m2049r.xmrwallet.fragment.send;

import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.Html;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.material.textfield.TextInputLayout;
import com.m2049r.xmrwallet.R;
import com.m2049r.xmrwallet.data.BarcodeData;
import com.m2049r.xmrwallet.data.Crypto;
import com.m2049r.xmrwallet.data.TxData;
import com.m2049r.xmrwallet.data.TxDataBtc;
import com.m2049r.xmrwallet.data.UserNotes;
import com.m2049r.xmrwallet.model.PendingTransaction;
import com.m2049r.xmrwallet.model.Wallet;
import com.m2049r.xmrwallet.service.shift.ShiftService;
import com.m2049r.xmrwallet.util.Helper;
import com.m2049r.xmrwallet.util.OpenAliasHelper;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import timber.log.Timber;

public class SendAddressWizardFragment extends SendWizardFragment {

    static final int INTEGRATED_ADDRESS_LENGTH = 106;

    public static SendAddressWizardFragment newInstance(Listener listener) {
        SendAddressWizardFragment instance = new SendAddressWizardFragment();
        instance.sendListener = listener;
        return instance;
    }

    private Listener sendListener;

    public interface Listener {
        void setBarcodeData(BarcodeData data);

        BarcodeData getBarcodeData();

        BarcodeData popBarcodeData();

        void setMode(SendFragment.Mode mode);

        TxData getTxData();
    }

    private EditText etDummy;
    private TextInputLayout etAddress;
    private TextInputLayout etNotes;
    private TextView tvXmrTo;
    private TextView tvTor;
    private Map<Crypto, ImageButton> ibCrypto;
    final private Set<Crypto> possibleCryptos = new HashSet<>();
    private Crypto selectedCrypto = null;

    private boolean resolvingOA = false;

    OnScanListener onScanListener;

    public interface OnScanListener {
        void onScan();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Timber.d("onCreateView() %s", (String.valueOf(savedInstanceState)));

        View view = inflater.inflate(R.layout.fragment_send_address, container, false);

        tvTor = view.findViewById(R.id.tvTor);
        tvXmrTo = view.findViewById(R.id.tvXmrTo);
        ibCrypto = new HashMap<>();
        for (Crypto crypto : Crypto.values()) {
            final ImageButton button = view.findViewById(crypto.getButtonId());
            if ((crypto == Crypto.XMR)
                    || (Helper.ALLOW_SHIFT && ShiftService.isAssetSupported(crypto))) {
                button.setVisibility(View.VISIBLE);
                ibCrypto.put(crypto, button);
                button.setOnClickListener(v -> {
                    if (possibleCryptos.contains(crypto)) {
                        selectedCrypto = crypto;
                        updateCryptoButtons(false);
                    } else {
                        // show help what to do:
                        if (button.getId() != R.id.ibXMR) {
                            tvXmrTo.setText(Html.fromHtml(getString(R.string.info_xmrto_help, crypto.getNetwork(), crypto.getLabel(), ShiftService.DEFAULT.getLabel())));
                            tvXmrTo.setVisibility(View.VISIBLE);
                        } else {
                            tvXmrTo.setText(Html.fromHtml(getString(R.string.info_xmrto_help_xmr)));
                            tvXmrTo.setVisibility(View.VISIBLE);
                            tvTor.setVisibility(View.INVISIBLE);
                        }
                    }
                });
            } else {
                button.setVisibility(View.INVISIBLE);
            }
        }
        if (!Helper.ALLOW_SHIFT) {
            tvTor.setVisibility(View.VISIBLE);
        }
        updateCryptoButtons(true);

        etAddress = view.findViewById(R.id.etAddress);
        etAddress.getEditText().setRawInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        etAddress.getEditText().setOnEditorActionListener(new TextView.OnEditorActionListener() {
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                // ignore ENTER
                return ((event != null) && (event.getKeyCode() == KeyEvent.KEYCODE_ENTER));
            }
        });
        etAddress.getEditText().setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                String enteredAddress = etAddress.getEditText().getText().toString().trim();
                String dnsOA = dnsFromOpenAlias(enteredAddress);
                Timber.d("OpenAlias is %s", dnsOA);
                if (dnsOA != null) {
                    processOpenAlias(dnsOA);
                }
            }
        });
        etAddress.getEditText().addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable editable) {
                Timber.d("AFTER: %s", editable.toString());
                BarcodeData bc = sendListener.getBarcodeData();
                if (bc == null) {
                    final String address = etAddress.getEditText().getText().toString();
                    bc = BarcodeData.fromString(address);
                }
                sendListener.setBarcodeData(null); // it's used up now
                possibleCryptos.clear();
                selectedCrypto = null;
                if ((bc != null) && (bc.filter(ShiftService.getPossibleAssets()))) {
                    possibleCryptos.clear();
                    possibleCryptos.addAll(bc.getPossibleAssets());
                    selectedCrypto = bc.getAsset();
                    if (checkAddress()) {
                        if (bc.getSecurity() == BarcodeData.Security.OA_NO_DNSSEC)
                            etAddress.setError(getString(R.string.send_address_no_dnssec));
                        else if (bc.getSecurity() == BarcodeData.Security.OA_DNSSEC)
                            etAddress.setError(getString(R.string.send_address_openalias));
                    }
                }
                updateCryptoButtons(false);
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
        });

        final ImageButton bPasteAddress = view.findViewById(R.id.bPasteAddress);
        bPasteAddress.setOnClickListener(v -> {
            final String clip = Helper.getClipBoardText(requireActivity());
            if (clip == null) return;
            // clean it up
            final String address = clip.replaceAll("( +)|(\\r?\\n?)", "");
            BarcodeData bc = BarcodeData.fromString(address);
            if (bc != null) {
                processScannedData(bc);
                final EditText et = etAddress.getEditText();
                et.setSelection(et.getText().length());
                etAddress.requestFocus();
            } else {
                Toast.makeText(getActivity(), getString(R.string.send_address_invalid), Toast.LENGTH_SHORT).show();
            }
        });

        etNotes = view.findViewById(R.id.etNotes);
        etNotes.getEditText().setRawInputType(InputType.TYPE_CLASS_TEXT);
        etNotes.getEditText().setOnEditorActionListener((v, actionId, event) -> {
            if ((event != null && (event.getKeyCode() == KeyEvent.KEYCODE_ENTER) && (event.getAction() == KeyEvent.ACTION_DOWN))
                    || (actionId == EditorInfo.IME_ACTION_DONE)) {
                etDummy.requestFocus();
                return true;
            }
            return false;
        });

        final View cvScan = view.findViewById(R.id.bScan);
        cvScan.setOnClickListener(v -> onScanListener.onScan());

        etDummy = view.findViewById(R.id.etDummy);
        etDummy.setRawInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        etDummy.requestFocus();

        return view;
    }

    private void selectedCrypto(Crypto crypto) {
        final ImageButton button = ibCrypto.get(crypto);
        assert button != null;
        button.setImageResource(crypto.getIconEnabledId());
        button.setImageAlpha(255);
        button.setEnabled(true);
        if (selectedCrypto == Crypto.XMR)
            sendListener.setMode(SendFragment.Mode.XMR);
        else
            sendListener.setMode(SendFragment.Mode.BTC);
    }

    private void possibleCrypto(Crypto crypto) {
        final ImageButton button = ibCrypto.get(crypto);
        assert button != null;
        button.setImageResource(crypto.getIconDisabledId());
        button.setImageAlpha(255);
        button.setEnabled(true);
    }

    private void impossibleCrypto(Crypto crypto) {
        final ImageButton button = ibCrypto.get(crypto);
        if (button == null) return; // not all buttons exist for all providers
        button.setImageResource(crypto.getIconDisabledId());
        button.setImageAlpha(128);
        button.setEnabled(true);
    }

    private void updateCryptoButtons(boolean noAddress) {
        if (!Helper.ALLOW_SHIFT) return;
        for (Crypto crypto : Crypto.values()) {
            if (crypto == selectedCrypto) {
                selectedCrypto(crypto);
            } else if (possibleCryptos.contains(crypto)) {
                possibleCrypto(crypto);
            } else {
                impossibleCrypto(crypto);
            }
        }
        if ((selectedCrypto != null) && (selectedCrypto != Crypto.XMR)) {
            tvXmrTo.setText(Html.fromHtml(getString(R.string.info_xmrto, selectedCrypto.getNetwork(), selectedCrypto.getLabel(), ShiftService.DEFAULT.getLabel())));
            tvXmrTo.setVisibility(View.VISIBLE);
        } else if ((selectedCrypto == null) && (possibleCryptos.size() > 1)) {
            tvXmrTo.setText(Html.fromHtml(getString(R.string.info_xmrto_ambiguous)));
            tvXmrTo.setVisibility(View.VISIBLE);
        } else {
            tvXmrTo.setVisibility(View.INVISIBLE);
        }
        if (noAddress) {
            selectedCrypto(Crypto.XMR);
        }
    }

    private void processOpenAlias(String dnsOA) {
        if (resolvingOA) return; // already resolving - just wait
        sendListener.popBarcodeData();
        if (dnsOA != null) {
            resolvingOA = true;
            etAddress.setError(getString(R.string.send_address_resolve_openalias));
            OpenAliasHelper.resolve(dnsOA, new OpenAliasHelper.OnResolvedListener() {
                @Override
                public void onResolved(Map<Crypto, BarcodeData> dataMap) {
                    resolvingOA = false;
                    BarcodeData barcodeData = dataMap.get(Crypto.XMR);
                    if (barcodeData == null) barcodeData = dataMap.get(Crypto.BTC);
                    if (barcodeData != null) {
                        Timber.d("Security=%s, %s", barcodeData.getSecurity().toString(), barcodeData.getAddress());
                        processScannedData(barcodeData);
                    } else {
                        etAddress.setError(getString(R.string.send_address_not_openalias));
                        Timber.d("NO XMR OPENALIAS TXT FOUND");
                    }
                }

                @Override
                public void onFailure() {
                    resolvingOA = false;
                    etAddress.setError(getString(R.string.send_address_not_openalias));
                    Timber.e("OA FAILED");
                }
            });
        } // else ignore
    }

    private boolean checkAddressNoError() {
        return selectedCrypto != null;
    }

    private boolean checkAddress() {
        boolean ok = checkAddressNoError();
        if (possibleCryptos.isEmpty()) {
            etAddress.setError(getString(R.string.send_address_invalid));
        } else {
            etAddress.setError(null);
        }
        return ok;
    }

    private boolean isStandardAddress(String address) {
        return Wallet.isAddressValid(address);
    }

    private boolean isIntegratedAddress(String address) {
        return (address.length() == INTEGRATED_ADDRESS_LENGTH)
                && Wallet.isAddressValid(address);
    }

    private void shakeAddress() {
        if (possibleCryptos.size() > 1) { // address ambiguous
            for (Crypto crypto : Crypto.values()) {
                if (possibleCryptos.contains(crypto)) {
                    ibCrypto.get(crypto).startAnimation(Helper.getShakeAnimation(getContext()));
                }
            }
        } else {
            etAddress.startAnimation(Helper.getShakeAnimation(getContext()));
        }
    }

    @Override
    public boolean onValidateFields() {
        if (!checkAddressNoError()) {
            shakeAddress();
            String enteredAddress = etAddress.getEditText().getText().toString().trim();
            String dnsOA = dnsFromOpenAlias(enteredAddress);
            Timber.d("OpenAlias is %s", dnsOA);
            if (dnsOA != null) {
                processOpenAlias(dnsOA);
            }
            return false;
        }

        if (sendListener != null) {
            TxData txData = sendListener.getTxData();
            if (txData instanceof TxDataBtc) {
                ((TxDataBtc) txData).setBtcAddress(etAddress.getEditText().getText().toString());
                ((TxDataBtc) txData).setBtcSymbol(selectedCrypto.getSymbol());
                txData.setDestination(null);
                ShiftService.ASSET = selectedCrypto;
            } else {
                txData.setDestination(etAddress.getEditText().getText().toString());
                ShiftService.ASSET = null;
            }
            txData.setUserNotes(new UserNotes(etNotes.getEditText().getText().toString()));
            txData.setPriority(PendingTransaction.Priority.Priority_Default);
            txData.setMixin(SendFragment.MIXIN);
        }
        return true;
    }

    @Override
    public void onAttach(@NonNull Context context) {
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
        processScannedData();
    }

    public void processScannedData(BarcodeData barcodeData) {
        barcodeData.filter(ShiftService.getPossibleAssets());
        sendListener.setBarcodeData(barcodeData);
        if (isResumed())
            processScannedData();
    }

    public void processScannedData() {
        final BarcodeData barcodeData = sendListener.getBarcodeData();
        if (barcodeData != null) {
            Timber.d("GOT DATA");
            if (!Helper.ALLOW_SHIFT && (barcodeData.getAsset() != Crypto.XMR)) {
                Timber.d("BUT ONLY XMR SUPPORTED");
                sendListener.setBarcodeData(null);
                return;
            }
            if (barcodeData.getAddress() != null) {
                etAddress.getEditText().setText(barcodeData.getAddress());
            } else {
                etAddress.getEditText().getText().clear();
                etAddress.setError(null);
            }

            String scannedNotes = barcodeData.getAddressName();
            if (scannedNotes == null) {
                scannedNotes = barcodeData.getDescription();
            } else if (barcodeData.getDescription() != null) {
                scannedNotes = scannedNotes + ": " + barcodeData.getDescription();
            }
            if (scannedNotes != null) {
                etNotes.getEditText().setText(scannedNotes);
            } else {
                etNotes.getEditText().getText().clear();
                etNotes.setError(null);
            }
        } else
            Timber.d("barcodeData=null");
    }

    @Override
    public void onResumeFragment() {
        super.onResumeFragment();
        Timber.d("onResumeFragment()");
        etDummy.requestFocus();
    }

    String dnsFromOpenAlias(String openalias) {
        Timber.d("checking openalias candidate %s", openalias);
        if (Patterns.DOMAIN_NAME.matcher(openalias).matches()) return openalias;
        if (Patterns.EMAIL_ADDRESS.matcher(openalias).matches()) {
            openalias = openalias.replaceFirst("@", ".");
            if (Patterns.DOMAIN_NAME.matcher(openalias).matches()) return openalias;
        }
        return null; // not an openalias
    }
}
