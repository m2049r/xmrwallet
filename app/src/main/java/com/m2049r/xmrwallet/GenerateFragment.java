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
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.m2049r.xmrwallet.model.Wallet;
import com.m2049r.xmrwallet.model.WalletManager;
import com.m2049r.xmrwallet.util.Helper;

import java.io.File;

public class GenerateFragment extends Fragment {
    static final String TAG = "GenerateFragment";

    EditText etWalletName;
    EditText etWalletPassword;
    EditText etWalletAddress;
    EditText etWalletMnemonic;
    LinearLayout llRestoreKeys;
    EditText etWalletViewKey;
    EditText etWalletSpendKey;
    EditText etWalletRestoreHeight;
    Button bGenerate;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.gen_fragment, container, false);

        etWalletName = (EditText) view.findViewById(R.id.etWalletName);
        etWalletPassword = (EditText) view.findViewById(R.id.etWalletPassword);
        etWalletMnemonic = (EditText) view.findViewById(R.id.etWalletMnemonic);
        etWalletAddress = (EditText) view.findViewById(R.id.etWalletAddress);
        llRestoreKeys = (LinearLayout) view.findViewById(R.id.llRestoreKeys);
        etWalletViewKey = (EditText) view.findViewById(R.id.etWalletViewKey);
        etWalletSpendKey = (EditText) view.findViewById(R.id.etWalletSpendKey);
        etWalletRestoreHeight = (EditText) view.findViewById(R.id.etWalletRestoreHeight);
        bGenerate = (Button) view.findViewById(R.id.bGenerate);

        etWalletMnemonic.setRawInputType(InputType.TYPE_CLASS_TEXT);
        etWalletAddress.setRawInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        etWalletViewKey.setRawInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        etWalletSpendKey.setRawInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);

        Helper.showKeyboard(getActivity());
        etWalletName.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable editable) {
                if (etWalletName.length() > 0) {
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
        etWalletName.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if ((event != null && (event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) || (actionId == EditorInfo.IME_ACTION_NEXT)) {
                    if (etWalletName.length() > 0) {
                        etWalletPassword.requestFocus();
                    } // otherwise ignore
                    return true;
                }
                return false;
            }
        });

        etWalletPassword.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if ((event != null && (event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) || (actionId == EditorInfo.IME_ACTION_NEXT)) {
                    if (etWalletAddress.length() > 0) {
                        etWalletAddress.requestFocus();
                    } else {
                        etWalletMnemonic.requestFocus();
                    }
                    return true;
                }
                return false;
            }
        });

        etWalletMnemonic.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if ((event != null && (event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) || (actionId == EditorInfo.IME_ACTION_NEXT)) {
                    if (etWalletMnemonic.length() == 0) {
                        etWalletAddress.requestFocus();
                    } else if (mnemonicOk()) {
                        etWalletRestoreHeight.requestFocus();
                    } else {
                        Toast.makeText(getActivity(), getString(R.string.generate_check_mnemonic), Toast.LENGTH_LONG).show();
                    }
                    return true;
                }
                return false;
            }
        });
        etWalletMnemonic.addTextChangedListener(new

                                                        TextWatcher() {
                                                            @Override
                                                            public void afterTextChanged(Editable editable) {
                                                                if (etWalletMnemonic.length() > 0) {
                                                                    etWalletRestoreHeight.setVisibility(View.VISIBLE);
                                                                    etWalletAddress.setVisibility(View.GONE);
                                                                } else {
                                                                    etWalletAddress.setVisibility(View.VISIBLE);
                                                                    if (etWalletAddress.length() == 0) {
                                                                        etWalletRestoreHeight.setVisibility(View.GONE);
                                                                    } else {
                                                                        etWalletRestoreHeight.setVisibility(View.VISIBLE);
                                                                    }

                                                                }
                                                            }

                                                            @Override
                                                            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                                                            }

                                                            @Override
                                                            public void onTextChanged(CharSequence s, int start, int before, int count) {
                                                            }
                                                        });

        etWalletAddress.setOnEditorActionListener(new TextView.OnEditorActionListener()

        {
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if ((event != null && (event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) || (actionId == EditorInfo.IME_ACTION_NEXT)) {
                    if (etWalletAddress.length() == 0) {
                        if (bGenerate.getVisibility() == View.VISIBLE) {
                            Helper.hideKeyboard(getActivity());
                            generateWallet();
                        }
                    } else if (addressOk()) {
                        etWalletViewKey.requestFocus();
                    } else {
                        Toast.makeText(getActivity(), getString(R.string.generate_check_address), Toast.LENGTH_LONG).show();
                    }
                    return true;
                }
                return false;
            }
        });
        etWalletAddress.addTextChangedListener(new

                                                       TextWatcher() {
                                                           @Override
                                                           public void afterTextChanged(Editable editable) {
                                                               if (etWalletAddress.length() > 0) {
                                                                   llRestoreKeys.setVisibility(View.VISIBLE);
                                                                   etWalletMnemonic.setVisibility(View.INVISIBLE);
                                                                   etWalletRestoreHeight.setVisibility(View.VISIBLE);
                                                               } else {
                                                                   llRestoreKeys.setVisibility(View.GONE);
                                                                   etWalletMnemonic.setVisibility(View.VISIBLE);
                                                                   if (etWalletMnemonic.length() == 0) {
                                                                       etWalletRestoreHeight.setVisibility(View.GONE);
                                                                   } else {
                                                                       etWalletRestoreHeight.setVisibility(View.VISIBLE);
                                                                   }
                                                               }
                                                           }

                                                           @Override
                                                           public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                                                           }

                                                           @Override
                                                           public void onTextChanged(CharSequence s, int start, int before, int count) {
                                                           }
                                                       });

        etWalletViewKey.setOnEditorActionListener(new TextView.OnEditorActionListener()

        {
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if ((event != null && (event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) || (actionId == EditorInfo.IME_ACTION_NEXT)) {
                    if (viewKeyOk()) {
                        etWalletSpendKey.requestFocus();
                    } else {
                        Toast.makeText(getActivity(), getString(R.string.generate_check_key), Toast.LENGTH_LONG).show();
                    }
                    return true;
                }
                return false;
            }
        });
        etWalletSpendKey.setOnEditorActionListener(new TextView.OnEditorActionListener()

        {
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if ((event != null && (event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) || (actionId == EditorInfo.IME_ACTION_NEXT)) {
                    if (spendKeyOk()) {
                        etWalletRestoreHeight.requestFocus();
                    } else {
                        Toast.makeText(getActivity(), getString(R.string.generate_check_key), Toast.LENGTH_LONG).show();
                    }
                    return true;
                }
                return false;
            }
        });
        etWalletRestoreHeight.setOnEditorActionListener(new TextView.OnEditorActionListener()

        {
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if ((event != null && (event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) || (actionId == EditorInfo.IME_ACTION_DONE)) {
                    if (bGenerate.getVisibility() == View.VISIBLE) {
                        Helper.hideKeyboard(getActivity());
                        generateWallet();
                    } else {
                        Toast.makeText(getActivity(), getString(R.string.generate_check_something), Toast.LENGTH_LONG).show();
                    }
                    return true;
                }
                return false;
            }
        });

        bGenerate.setOnClickListener(new View.OnClickListener()

        {
            @Override
            public void onClick(View v) {
                Helper.hideKeyboard(getActivity());
                generateWallet();
            }
        });

        etWalletName.requestFocus();
        return view;
    }

    private boolean mnemonicOk() {
        String seed = etWalletMnemonic.getText().toString();
        return (seed.split("\\s").length == 25); // 25 words
    }

    private boolean addressOk() {
        String address = etWalletAddress.getText().toString();
        return Wallet.isAddressValid(address, WalletManager.getInstance().isTestNet());
    }

    private boolean viewKeyOk() {
        String viewKey = etWalletViewKey.getText().toString();
        return (viewKey.length() == 64) && (viewKey.matches("^[0-9a-fA-F]+$"));
    }

    private boolean spendKeyOk() {
        String spendKey = etWalletSpendKey.getText().toString();
        return ((spendKey.length() == 0) || ((spendKey.length() == 64) && (spendKey.matches("^[0-9a-fA-F]+$"))));
    }

    private void generateWallet() {
        String name = etWalletName.getText().toString();
        if (name.length() == 0) return;
        if (name.charAt(0) == '.') {
            Toast.makeText(getActivity(), getString(R.string.generate_wallet_dot), Toast.LENGTH_LONG).show();
            etWalletName.requestFocus();
        }
        File walletFile = Helper.getWalletFile(getActivity(), name);
        if (WalletManager.getInstance().walletExists(walletFile)) {
            Toast.makeText(getActivity(), getString(R.string.generate_wallet_exists), Toast.LENGTH_LONG).show();
            etWalletName.requestFocus();
            return;
        }
        String password = etWalletPassword.getText().toString();

        String seed = etWalletMnemonic.getText().toString();
        String address = etWalletAddress.getText().toString();

        long height;
        try {
            height = Long.parseLong(etWalletRestoreHeight.getText().toString());
        } catch (NumberFormatException ex) {
            height = 0; // Keep calm and carry on!
        }

        // figure out how we want to create this wallet
        // A. from scratch
        if ((seed.length() == 0) && (address.length() == 0)) {
            bGenerate.setVisibility(View.GONE);
            activityCallback.onGenerate(name, password);
        } else
            // B. from seed
            if (mnemonicOk()) {
                bGenerate.setVisibility(View.GONE);
                activityCallback.onGenerate(name, password, seed, height);
            } else
                // C. from keys
                if (addressOk() && viewKeyOk() && (spendKeyOk())) {
                    String viewKey = etWalletViewKey.getText().toString();
                    String spendKey = etWalletSpendKey.getText().toString();
                    bGenerate.setVisibility(View.GONE);
                    activityCallback.onGenerate(name, password, address, viewKey, spendKey, height);
                } else
                // D. none of the above :)
                {
                    Toast.makeText(getActivity(), getString(R.string.generate_check_something), Toast.LENGTH_LONG).show();
                }
    }

    public void walletGenerateError() {
        bGenerate.setEnabled(etWalletName.length() > 0);
        bGenerate.setVisibility(View.VISIBLE);
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume()");
        activityCallback.setTitle(getString(R.string.generate_title));
    }

    GenerateFragment.Listener activityCallback;

    public interface Listener {
        void onGenerate(String name, String password);

        void onGenerate(String name, String password, String seed, long height);

        void onGenerate(String name, String password, String address, String viewKey, String spendKey, long height);

        File getStorageRoot();

        void setTitle(String title);

    }

    @Override
    public void onAttach(Context context) {
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
        inflater.inflate(R.menu.create_wallet_menu, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }
}
