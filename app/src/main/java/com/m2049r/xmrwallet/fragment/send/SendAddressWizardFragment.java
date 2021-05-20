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
import android.nfc.NfcManager;
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
import com.m2049r.xmrwallet.util.Helper;
import com.m2049r.xmrwallet.util.OpenAliasHelper;
import com.m2049r.xmrwallet.util.ServiceHelper;
import com.m2049r.xmrwallet.util.validator.BitcoinAddressType;
import com.m2049r.xmrwallet.util.validator.BitcoinAddressValidator;
import com.m2049r.xmrwallet.util.validator.EthAddressValidator;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import timber.log.Timber;

public class SendAddressWizardFragment extends SendWizardFragment {

    static final int INTEGRATED_ADDRESS_LENGTH = 106;

    public static SendAddressWizardFragment newInstance(Listener listener) {
        SendAddressWizardFragment instance = new SendAddressWizardFragment();
        instance.setSendListener(listener);
        return instance;
    }

    Listener sendListener;

    public void setSendListener(Listener listener) {
        this.sendListener = listener;
    }

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
    private Map<Crypto, ImageButton> ibCrypto;
    final private Set<Crypto> possibleCryptos = new HashSet<>();
    private Crypto selectedCrypto = null;

    private boolean resolvingOA = false;

    OnScanListener onScanListener;

    public interface OnScanListener {
        void onScan();
    }

    private Crypto getCryptoForButton(ImageButton button) {
        for (Map.Entry<Crypto, ImageButton> entry : ibCrypto.entrySet()) {
            if (entry.getValue() == button) return entry.getKey();
        }
        return null;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Timber.d("onCreateView() %s", (String.valueOf(savedInstanceState)));

        View view = inflater.inflate(R.layout.fragment_send_address, container, false);

        if (Helper.ALLOW_SHIFT) {
            tvXmrTo = view.findViewById(R.id.tvXmrTo);
            ibCrypto = new HashMap<>();
            for (Crypto crypto : Crypto.values()) {
                final ImageButton button = view.findViewById(crypto.getButtonId());
                ibCrypto.put(crypto, button);
                button.setOnClickListener(v -> {
                    if (possibleCryptos.contains(crypto)) {
                        selectedCrypto = crypto;
                        updateCryptoButtons(false);
                    } else {
                        // show help what to do:
                        if (button.getId() != R.id.ibXMR) {
                            final String name = getResources().getStringArray(R.array.cryptos)[crypto.ordinal()];
                            final String symbol = getCryptoForButton(button).getSymbol();
                            tvXmrTo.setText(Html.fromHtml(getString(R.string.info_xmrto_help, name, symbol)));
                            tvXmrTo.setVisibility(View.VISIBLE);
                        } else {
                            tvXmrTo.setText(Html.fromHtml(getString(R.string.info_xmrto_help_xmr)));
                            tvXmrTo.setVisibility(View.VISIBLE);
                        }
                    }
                });
            }
            updateCryptoButtons(true);
        } else {
            view.findViewById(R.id.llExchange).setVisibility(View.GONE);
        }
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
                etAddress.setError(null);
                possibleCryptos.clear();
                selectedCrypto = null;
                final String address = etAddress.getEditText().getText().toString();
                if (isIntegratedAddress(address)) {
                    Timber.d("isIntegratedAddress");
                    possibleCryptos.add(Crypto.XMR);
                    selectedCrypto = Crypto.XMR;
                    etAddress.setError(getString(R.string.info_paymentid_integrated));
                    sendListener.setMode(SendFragment.Mode.XMR);
                } else if (isStandardAddress(address)) {
                    Timber.d("isStandardAddress");
                    possibleCryptos.add(Crypto.XMR);
                    selectedCrypto = Crypto.XMR;
                    sendListener.setMode(SendFragment.Mode.XMR);
                }
                if (!Helper.ALLOW_SHIFT) return;
                if ((selectedCrypto == null) && isEthAddress(address)) {
                    Timber.d("isEthAddress");
                    possibleCryptos.add(Crypto.ETH);
                    selectedCrypto = Crypto.ETH;
                    tvXmrTo.setVisibility(View.VISIBLE);
                    sendListener.setMode(SendFragment.Mode.BTC);
                }
                if (possibleCryptos.isEmpty()) {
                    Timber.d("isBitcoinAddress");
                    for (BitcoinAddressType type : BitcoinAddressType.values()) {
                        if (BitcoinAddressValidator.validate(address, type)) {
                            possibleCryptos.add(Crypto.valueOf(type.name()));
                        }
                    }
                    if (!possibleCryptos.isEmpty()) // found something in need of shifting!
                        sendListener.setMode(SendFragment.Mode.BTC);
                    if (possibleCryptos.size() == 1) {
                        selectedCrypto = (Crypto) possibleCryptos.toArray()[0];
                    }
                }
                if (possibleCryptos.isEmpty()) {
                    Timber.d("other");
                    tvXmrTo.setVisibility(View.INVISIBLE);
                    sendListener.setMode(SendFragment.Mode.XMR);
                }
                updateCryptoButtons(address.isEmpty());
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
            final String clip = Helper.getClipBoardText(getActivity());
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
        etNotes.getEditText().

                setOnEditorActionListener((v, actionId, event) -> {
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

        View tvNfc = view.findViewById(R.id.tvNfc);
        NfcManager manager = (NfcManager) getContext().getSystemService(Context.NFC_SERVICE);
        if ((manager != null) && (manager.getDefaultAdapter() != null))
            tvNfc.setVisibility(View.VISIBLE);

        return view;
    }

    private void selectedCrypto(Crypto crypto) {
        final ImageButton button = ibCrypto.get(crypto);
        button.setImageResource(crypto.getIconEnabledId());
        button.setImageAlpha(255);
        button.setEnabled(true);
    }

    private void possibleCrypto(Crypto crypto) {
        final ImageButton button = ibCrypto.get(crypto);
        button.setImageResource(crypto.getIconDisabledId());
        button.setImageAlpha(255);
        button.setEnabled(true);
    }

    private void impossibleCrypto(Crypto crypto) {
        final ImageButton button = ibCrypto.get(crypto);
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
            tvXmrTo.setText(Html.fromHtml(getString(R.string.info_xmrto, selectedCrypto.getSymbol())));
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
                        Timber.d("Security=%s, %s", barcodeData.security.toString(), barcodeData.address);
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

    private boolean isBitcoinishAddress(String address) {
        return BitcoinAddressValidator.validate(address, BitcoinAddressType.BTC)
                ||
                BitcoinAddressValidator.validate(address, BitcoinAddressType.LTC)
                ||
                BitcoinAddressValidator.validate(address, BitcoinAddressType.DASH);
    }

    private boolean isEthAddress(String address) {
        return EthAddressValidator.validate(address);
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
                txData.setDestinationAddress(null);
                ServiceHelper.ASSET = selectedCrypto.getSymbol().toLowerCase();
            } else {
                txData.setDestinationAddress(etAddress.getEditText().getText().toString());
                ServiceHelper.ASSET = null;
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
        sendListener.setBarcodeData(barcodeData);
        if (isResumed())
            processScannedData();
    }

    public void processScannedData() {
        BarcodeData barcodeData = sendListener.getBarcodeData();
        if (barcodeData != null) {
            Timber.d("GOT DATA");
            if (!Helper.ALLOW_SHIFT && (barcodeData.asset != Crypto.XMR)) {
                Timber.d("BUT ONLY XMR SUPPORTED");
                barcodeData = null;
                sendListener.setBarcodeData(barcodeData);
            }
            if (barcodeData.address != null) {
                etAddress.getEditText().setText(barcodeData.address);
                possibleCryptos.clear();
                selectedCrypto = null;
                if (barcodeData.isAmbiguous()) {
                    possibleCryptos.addAll(barcodeData.ambiguousAssets);
                } else {
                    possibleCryptos.add(barcodeData.asset);
                    selectedCrypto = barcodeData.asset;
                }
                if (Helper.ALLOW_SHIFT)
                    updateCryptoButtons(false);
                if (checkAddress()) {
                    if (barcodeData.security == BarcodeData.Security.OA_NO_DNSSEC)
                        etAddress.setError(getString(R.string.send_address_no_dnssec));
                    else if (barcodeData.security == BarcodeData.Security.OA_DNSSEC)
                        etAddress.setError(getString(R.string.send_address_openalias));
                }
            } else {
                etAddress.getEditText().getText().clear();
                etAddress.setError(null);
            }

            String scannedNotes = barcodeData.addressName;
            if (scannedNotes == null) {
                scannedNotes = barcodeData.description;
            } else if (barcodeData.description != null) {
                scannedNotes = scannedNotes + ": " + barcodeData.description;
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
