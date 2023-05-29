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

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Editable;
import android.text.Html;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputLayout;
import com.m2049r.xmrwallet.model.NetworkType;
import com.m2049r.xmrwallet.model.Wallet;
import com.m2049r.xmrwallet.model.WalletManager;
import com.m2049r.xmrwallet.util.FingerprintHelper;
import com.m2049r.xmrwallet.util.Helper;
import com.m2049r.xmrwallet.util.KeyStoreHelper;
import com.m2049r.xmrwallet.util.RestoreHeight;
import com.m2049r.xmrwallet.util.ledger.Monero;
import com.m2049r.xmrwallet.widget.PasswordEntryView;
import com.m2049r.xmrwallet.widget.Toolbar;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import timber.log.Timber;

public class GenerateFragment extends Fragment {

    static final String TYPE = "type";
    static final String TYPE_NEW = "new";
    static final String TYPE_KEY = "key";
    static final String TYPE_SEED = "seed";
    static final String TYPE_LEDGER = "ledger";
    static final String TYPE_VIEWONLY = "view";

    private TextInputLayout etWalletName;
    private PasswordEntryView etWalletPassword;
    private LinearLayout llFingerprintAuth;
    private TextInputLayout etWalletAddress;
    private TextInputLayout etWalletMnemonic;
    private TextInputLayout etWalletViewKey;
    private TextInputLayout etWalletSpendKey;
    private TextInputLayout etWalletRestoreHeight;
    private Button bGenerate;

    private Button bSeedOffset;
    private TextInputLayout etSeedOffset;

    private String type = null;

    private void clearErrorOnTextEntry(final TextInputLayout textInputLayout) {
        textInputLayout.getEditText().addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable editable) {
                textInputLayout.setError(null);
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        Bundle args = getArguments();
        this.type = args.getString(TYPE);

        View view = inflater.inflate(R.layout.fragment_generate, container, false);

        etWalletName = view.findViewById(R.id.etWalletName);
        etWalletPassword = view.findViewById(R.id.etWalletPassword);
        llFingerprintAuth = view.findViewById(R.id.llFingerprintAuth);
        etWalletMnemonic = view.findViewById(R.id.etWalletMnemonic);
        etWalletAddress = view.findViewById(R.id.etWalletAddress);
        etWalletViewKey = view.findViewById(R.id.etWalletViewKey);
        etWalletSpendKey = view.findViewById(R.id.etWalletSpendKey);
        etWalletRestoreHeight = view.findViewById(R.id.etWalletRestoreHeight);
        bGenerate = view.findViewById(R.id.bGenerate);
        bSeedOffset = view.findViewById(R.id.bSeedOffset);
        etSeedOffset = view.findViewById(R.id.etSeedOffset);

        etWalletAddress.getEditText().setRawInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        etWalletViewKey.getEditText().setRawInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        etWalletSpendKey.getEditText().setRawInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);

        etWalletName.getEditText().setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                checkName();
            }
        });
        clearErrorOnTextEntry(etWalletName);

        etWalletMnemonic.getEditText().setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                checkMnemonic();
            }
        });
        clearErrorOnTextEntry(etWalletMnemonic);

        etWalletAddress.getEditText().setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                checkAddress();
            }
        });
        clearErrorOnTextEntry(etWalletAddress);

        etWalletViewKey.getEditText().setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                checkViewKey();
            }
        });
        clearErrorOnTextEntry(etWalletViewKey);

        etWalletSpendKey.getEditText().setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                checkSpendKey();
            }
        });
        clearErrorOnTextEntry(etWalletSpendKey);

        Helper.showKeyboard(requireActivity());

        etWalletName.getEditText().setOnEditorActionListener((v, actionId, event) -> {
            if ((event != null && (event.getKeyCode() == KeyEvent.KEYCODE_ENTER) && (event.getAction() == KeyEvent.ACTION_DOWN))
                    || (actionId == EditorInfo.IME_ACTION_NEXT)) {
                if (checkName()) {
                    etWalletPassword.requestFocus();
                } // otherwise ignore
                return true;
            }
            return false;
        });

        if (FingerprintHelper.isDeviceSupported(getContext())) {
            llFingerprintAuth.setVisibility(View.VISIBLE);

            final SwitchMaterial swFingerprintAllowed = (SwitchMaterial) llFingerprintAuth.getChildAt(0);
            swFingerprintAllowed.setOnClickListener(view1 -> {
                if (!swFingerprintAllowed.isChecked()) return;

                MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireActivity());
                builder.setMessage(Html.fromHtml(getString(R.string.generate_fingerprint_warn)))
                        .setCancelable(false)
                        .setPositiveButton(getString(R.string.label_ok), null)
                        .setNegativeButton(getString(R.string.label_cancel), (dialogInterface, i) -> swFingerprintAllowed.setChecked(false))
                        .show();
            });
        }

        switch (type) {
            case TYPE_NEW:
                etWalletPassword.getEditText().setImeOptions(EditorInfo.IME_ACTION_UNSPECIFIED);
                break;
            case TYPE_LEDGER:
                etWalletPassword.getEditText().setImeOptions(EditorInfo.IME_ACTION_DONE);
                etWalletPassword.getEditText().setOnEditorActionListener((v, actionId, event) -> {
                    if ((event != null && (event.getKeyCode() == KeyEvent.KEYCODE_ENTER) && (event.getAction() == KeyEvent.ACTION_DOWN))
                            || (actionId == EditorInfo.IME_ACTION_DONE)) {
                        etWalletRestoreHeight.requestFocus();
                        return true;
                    }
                    return false;
                });
                break;
            case TYPE_SEED:
                etWalletPassword.getEditText().setOnEditorActionListener((v, actionId, event) -> {
                    if ((event != null && (event.getKeyCode() == KeyEvent.KEYCODE_ENTER) && (event.getAction() == KeyEvent.ACTION_DOWN))
                            || (actionId == EditorInfo.IME_ACTION_NEXT)) {
                        etWalletMnemonic.requestFocus();
                        return true;
                    }
                    return false;
                });
                etWalletMnemonic.setVisibility(View.VISIBLE);
                etWalletMnemonic.getEditText().setOnEditorActionListener((v, actionId, event) -> {
                    if ((event != null && (event.getKeyCode() == KeyEvent.KEYCODE_ENTER) && (event.getAction() == KeyEvent.ACTION_DOWN))
                            || (actionId == EditorInfo.IME_ACTION_NEXT)) {
                        if (checkMnemonic()) {
                            etWalletRestoreHeight.requestFocus();
                        }
                        return true;
                    }
                    return false;
                });
                bSeedOffset.setVisibility(View.VISIBLE);
                bSeedOffset.setOnClickListener(v -> toggleSeedOffset());
                break;
            case TYPE_KEY:
            case TYPE_VIEWONLY:
                etWalletPassword.getEditText().setOnEditorActionListener((v, actionId, event) -> {
                    if ((event != null && (event.getKeyCode() == KeyEvent.KEYCODE_ENTER) && (event.getAction() == KeyEvent.ACTION_DOWN))
                            || (actionId == EditorInfo.IME_ACTION_NEXT)) {
                        etWalletAddress.requestFocus();
                        return true;
                    }
                    return false;
                });
                etWalletAddress.setVisibility(View.VISIBLE);
                etWalletAddress.getEditText().setOnEditorActionListener((v, actionId, event) -> {
                    if ((event != null && (event.getKeyCode() == KeyEvent.KEYCODE_ENTER) && (event.getAction() == KeyEvent.ACTION_DOWN))
                            || (actionId == EditorInfo.IME_ACTION_NEXT)) {
                        if (checkAddress()) {
                            etWalletViewKey.requestFocus();
                        }
                        return true;
                    }
                    return false;
                });
                etWalletViewKey.setVisibility(View.VISIBLE);
                etWalletViewKey.getEditText().setOnEditorActionListener((v, actionId, event) -> {
                    if ((event != null && (event.getKeyCode() == KeyEvent.KEYCODE_ENTER) && (event.getAction() == KeyEvent.ACTION_DOWN))
                            || (actionId == EditorInfo.IME_ACTION_NEXT)) {
                        if (checkViewKey()) {
                            if (type.equals(TYPE_KEY)) {
                                etWalletSpendKey.requestFocus();
                            } else {
                                etWalletRestoreHeight.requestFocus();
                            }
                        }
                        return true;
                    }
                    return false;
                });
                break;
        }
        if (type.equals(TYPE_KEY)) {
            etWalletSpendKey.setVisibility(View.VISIBLE);
            etWalletSpendKey.getEditText().setOnEditorActionListener((v, actionId, event) -> {
                if ((event != null && (event.getKeyCode() == KeyEvent.KEYCODE_ENTER) && (event.getAction() == KeyEvent.ACTION_DOWN))
                        || (actionId == EditorInfo.IME_ACTION_NEXT)) {
                    if (checkSpendKey()) {
                        etWalletRestoreHeight.requestFocus();
                    }
                    return true;
                }
                return false;
            });
        }
        if (!type.equals(TYPE_NEW)) {
            etWalletRestoreHeight.setVisibility(View.VISIBLE);
            etWalletRestoreHeight.getEditText().setImeOptions(EditorInfo.IME_ACTION_UNSPECIFIED);
        }
        bGenerate.setOnClickListener(v -> {
            Helper.hideKeyboard(getActivity());
            generateWallet();
        });

        etWalletName.requestFocus();

        return view;
    }

    void toggleSeedOffset() {
        if (etSeedOffset.getVisibility() == View.VISIBLE) {
            etSeedOffset.getEditText().getText().clear();
            etSeedOffset.setVisibility(View.GONE);
            bSeedOffset.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_keyboard_arrow_down, 0, 0, 0);
        } else {
            etSeedOffset.setVisibility(View.VISIBLE);
            bSeedOffset.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_keyboard_arrow_up, 0, 0, 0);
            etSeedOffset.requestFocusFromTouch();
        }
    }

    private boolean checkName() {
        String name = etWalletName.getEditText().getText().toString();
        boolean ok = true;
        if (name.length() == 0) {
            etWalletName.setError(getString(R.string.generate_wallet_name));
            ok = false;
        } else if (name.charAt(0) == '.') {
            etWalletName.setError(getString(R.string.generate_wallet_dot));
            ok = false;
        } else {
            File walletFile = Helper.getWalletFile(getActivity(), name);
            if (WalletManager.getInstance().walletExists(walletFile)) {
                etWalletName.setError(getString(R.string.generate_wallet_exists));
                ok = false;
            }
        }
        if (ok) {
            etWalletName.setError(null);
        }
        return ok;
    }

    private boolean checkHeight() {
        long height = !type.equals(TYPE_NEW) ? getHeight() : 0;
        boolean ok = true;
        if (height < 0) {
            etWalletRestoreHeight.setError(getString(R.string.generate_restoreheight_error));
            ok = false;
        }
        if (ok) {
            etWalletRestoreHeight.setError(null);
        }
        return ok;
    }

    private long getHeight() {
        long height = -1;

        String restoreHeight = etWalletRestoreHeight.getEditText().getText().toString().trim();
        if (restoreHeight.isEmpty()) return -1;
        if (WalletManager.getInstance().getNetworkType() == NetworkType.NetworkType_Mainnet) {
            try {
                // is it a date?
                SimpleDateFormat parser = new SimpleDateFormat("yyyy-MM-dd");
                parser.setLenient(false);
                height = RestoreHeight.getInstance().getHeight(parser.parse(restoreHeight));
            } catch (ParseException ignored) {
            }
            if ((height < 0) && (restoreHeight.length() == 8))
                try {
                    // is it a date without dashes?
                    SimpleDateFormat parser = new SimpleDateFormat("yyyyMMdd");
                    parser.setLenient(false);
                    height = RestoreHeight.getInstance().getHeight(parser.parse(restoreHeight));
                } catch (ParseException ignored) {
                }
        }
        if (height < 0)
            try {
                // or is it a height?
                height = Long.parseLong(restoreHeight);
            } catch (NumberFormatException ex) {
                return -1;
            }
        Timber.d("Using Restore Height = %d", height);
        return height;
    }

    private boolean checkMnemonic() {
        String seed = etWalletMnemonic.getEditText().getText().toString();
        boolean ok = (seed.split("\\s").length == 25); // 25 words
        if (!ok) {
            etWalletMnemonic.setError(getString(R.string.generate_check_mnemonic));
        } else {
            etWalletMnemonic.setError(null);
        }
        return ok;
    }

    private boolean checkAddress() {
        String address = etWalletAddress.getEditText().getText().toString();
        boolean ok = Wallet.isAddressValid(address);
        if (!ok) {
            etWalletAddress.setError(getString(R.string.generate_check_address));
        } else {
            etWalletAddress.setError(null);
        }
        return ok;
    }

    private boolean checkViewKey() {
        String viewKey = etWalletViewKey.getEditText().getText().toString();
        boolean ok = (viewKey.length() == 64) && (viewKey.matches("^[0-9a-fA-F]+$"));
        if (!ok) {
            etWalletViewKey.setError(getString(R.string.generate_check_key));
        } else {
            etWalletViewKey.setError(null);
        }
        return ok;
    }

    private boolean checkSpendKey() {
        String spendKey = etWalletSpendKey.getEditText().getText().toString();
        boolean ok = ((spendKey.length() == 0) || ((spendKey.length() == 64) && (spendKey.matches("^[0-9a-fA-F]+$"))));
        if (!ok) {
            etWalletSpendKey.setError(getString(R.string.generate_check_key));
        } else {
            etWalletSpendKey.setError(null);
        }
        return ok;
    }

    private void generateWallet() {
        if (!checkName()) return;
        if (!checkHeight()) return;

        String name = etWalletName.getEditText().getText().toString();
        String password = etWalletPassword.getEditText().getText().toString();
        boolean fingerprintAuthAllowed = ((SwitchMaterial) llFingerprintAuth.getChildAt(0)).isChecked();

        // create the real wallet password
        String crazyPass = KeyStoreHelper.getCrazyPass(getActivity(), password);

        long height = getHeight();
        if (height < 0) height = 0;

        switch (type) {
            case TYPE_NEW:
                bGenerate.setEnabled(false);
                if (fingerprintAuthAllowed) {
                    KeyStoreHelper.saveWalletUserPass(requireActivity(), name, password);
                }
                activityCallback.onGenerate(name, crazyPass);
                break;
            case TYPE_SEED:
                if (!checkMnemonic()) return;
                final String seed = etWalletMnemonic.getEditText().getText().toString();
                bGenerate.setEnabled(false);
                if (fingerprintAuthAllowed) {
                    KeyStoreHelper.saveWalletUserPass(requireActivity(), name, password);
                }
                final String offset = etSeedOffset.getEditText().getText().toString();
                activityCallback.onGenerate(name, crazyPass, seed, offset, height);
                break;
            case TYPE_LEDGER:
                bGenerate.setEnabled(false);
                if (fingerprintAuthAllowed) {
                    KeyStoreHelper.saveWalletUserPass(requireActivity(), name, password);
                }
                activityCallback.onGenerateLedger(name, crazyPass, height);
                break;
            case TYPE_KEY:
            case TYPE_VIEWONLY:
                if (checkAddress() && checkViewKey() && checkSpendKey()) {
                    bGenerate.setEnabled(false);
                    String address = etWalletAddress.getEditText().getText().toString();
                    String viewKey = etWalletViewKey.getEditText().getText().toString();
                    String spendKey = "";
                    if (type.equals(TYPE_KEY)) {
                        spendKey = etWalletSpendKey.getEditText().getText().toString();
                    }
                    if (fingerprintAuthAllowed) {
                        KeyStoreHelper.saveWalletUserPass(requireActivity(), name, password);
                    }
                    activityCallback.onGenerate(name, crazyPass, address, viewKey, spendKey, height);
                }
                break;
        }
    }

    public void walletGenerateError() {
        bGenerate.setEnabled(true);
    }

    @Override
    public void onResume() {
        super.onResume();
        Timber.d("onResume()");
        activityCallback.setTitle(getString(R.string.generate_title) + " - " + getType());
        activityCallback.setToolbarButton(Toolbar.BUTTON_BACK);

    }

    String getType() {
        switch (type) {
            case TYPE_KEY:
                return getString(R.string.generate_wallet_type_key);
            case TYPE_NEW:
                return getString(R.string.generate_wallet_type_new);
            case TYPE_SEED:
                return getString(R.string.generate_wallet_type_seed);
            case TYPE_LEDGER:
                return getString(R.string.generate_wallet_type_ledger);
            case TYPE_VIEWONLY:
                return getString(R.string.generate_wallet_type_view);
            default:
                Timber.e("unknown type %s", type);
                return "?";
        }
    }

    GenerateFragment.Listener activityCallback;

    public interface Listener {
        void onGenerate(String name, String password);

        void onGenerate(String name, String password, String seed, String offset, long height);

        void onGenerate(String name, String password, String address, String viewKey, String spendKey, long height);

        void onGenerateLedger(String name, String password, long height);

        void setTitle(String title);

        void setToolbarButton(int type);

    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof GenerateFragment.Listener) {
            this.activityCallback = (GenerateFragment.Listener) context;
        } else {
            throw new ClassCastException(context.toString()
                    + " must implement Listener");
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        switch (type) {
            case TYPE_KEY:
                inflater.inflate(R.menu.create_wallet_keys, menu);
                break;
            case TYPE_NEW:
                inflater.inflate(R.menu.create_wallet_new, menu);
                break;
            case TYPE_SEED:
                inflater.inflate(R.menu.create_wallet_seed, menu);
                break;
            case TYPE_LEDGER:
                inflater.inflate(R.menu.create_wallet_ledger, menu);
                break;
            case TYPE_VIEWONLY:
                inflater.inflate(R.menu.create_wallet_view, menu);
                break;
            default:
        }
        super.onCreateOptionsMenu(menu, inflater);
    }

    AlertDialog ledgerDialog = null;

    public void convertLedgerSeed() {
        if (ledgerDialog != null) return;
        final Activity activity = requireActivity();
        View promptsView = getLayoutInflater().inflate(R.layout.prompt_ledger_seed, null);
        MaterialAlertDialogBuilder alertDialogBuilder = new MaterialAlertDialogBuilder(activity);
        alertDialogBuilder.setView(promptsView);

        final TextInputLayout etSeed = promptsView.findViewById(R.id.etSeed);
        final TextInputLayout etPassphrase = promptsView.findViewById(R.id.etPassphrase);

        clearErrorOnTextEntry(etSeed);

        alertDialogBuilder
                .setCancelable(false)
                .setPositiveButton(getString(R.string.label_ok), null)
                .setNegativeButton(getString(R.string.label_cancel),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                Helper.hideKeyboardAlways(activity);
                                etWalletMnemonic.getEditText().getText().clear();
                                dialog.cancel();
                                ledgerDialog = null;
                            }
                        });

        ledgerDialog = alertDialogBuilder.create();

        ledgerDialog.setOnShowListener(dialog -> {
            Button button = ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_POSITIVE);
            button.setOnClickListener(view -> {
                String ledgerSeed = etSeed.getEditText().getText().toString();
                String ledgerPassphrase = etPassphrase.getEditText().getText().toString();
                String moneroSeed = Monero.convert(ledgerSeed, ledgerPassphrase);
                if (moneroSeed != null) {
                    etWalletMnemonic.getEditText().setText(moneroSeed);
                    ledgerDialog.dismiss();
                    ledgerDialog = null;
                } else {
                    etSeed.setError(getString(R.string.bad_ledger_seed));
                }
            });
        });

        if (Helper.preventScreenshot()) {
            ledgerDialog.getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
        }

        ledgerDialog.show();
    }
}
