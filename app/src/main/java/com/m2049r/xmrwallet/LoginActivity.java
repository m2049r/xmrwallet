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
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.m2049r.xmrwallet.model.Wallet;
import com.m2049r.xmrwallet.model.WalletManager;
import com.m2049r.xmrwallet.service.MoneroHandlerThread;
import com.m2049r.xmrwallet.util.Helper;

import java.io.File;

public class LoginActivity extends Activity
        implements LoginFragment.Listener, GenerateFragment.Listener {
    static final String TAG = "LoginActivity";

    static final int DAEMON_TIMEOUT = 500; // deamon must respond in 500ms

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login_activity);
        if (savedInstanceState != null) {
            return;
        }
        if (Helper.getWritePermission(this)) {
            startLoginFragment();
        } else {
            Log.i(TAG, "Waiting for permissions");
        }
    }

    @Override
    public void onWalletSelected(final String walletName) {
        Log.d(TAG, "selected wallet is ." + walletName + ".");
        if (walletName.equals(':' + getString(R.string.generate_title))) {
            startGenerateFragment();
        } else {
            // now it's getting real, check if wallet exists
            String walletPath = Helper.getWalletPath(this, walletName);
            if (WalletManager.getInstance().walletExists(walletPath)) {
                promptPassword(walletName);
            } else { // this cannot really happen as we prefilter choices
                Toast.makeText(this, getString(R.string.bad_wallet), Toast.LENGTH_SHORT).show();
            }
        }
    }

    AlertDialog passwordDialog = null; // for preventing multiple clicks in wallet list

    void promptPassword(final String wallet) {
        if (passwordDialog != null) return; // we are already asking for password
        Context context = LoginActivity.this;
        LayoutInflater li = LayoutInflater.from(context);
        View promptsView = li.inflate(R.layout.prompt_password, null);

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context);
        alertDialogBuilder.setView(promptsView);

        final EditText etPassword = (EditText) promptsView.findViewById(R.id.etPassword);
        final TextView tvPasswordLabel = (TextView) promptsView.findViewById(R.id.tvPasswordLabel);

        tvPasswordLabel.setText(LoginActivity.this.getString(R.string.prompt_password) + " " + wallet);

        // set dialog message
        alertDialogBuilder
                .setCancelable(false)
                .setPositiveButton("OK",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                Helper.hideKeyboardAlways(LoginActivity.this);
                                String pass = etPassword.getText().toString();
                                processPasswordEntry(wallet, pass);
                                passwordDialog = null;
                            }
                        })
                .setNegativeButton("Cancel",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                Helper.hideKeyboardAlways(LoginActivity.this);
                                dialog.cancel();
                                passwordDialog = null;
                            }
                        });

        passwordDialog = alertDialogBuilder.create();
        Helper.showKeyboard(passwordDialog);

        // accept keyboard "ok"
        etPassword.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if ((event != null && (event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) || (actionId == EditorInfo.IME_ACTION_DONE)) {
                    Helper.hideKeyboardAlways(LoginActivity.this);
                    String pass = etPassword.getText().toString();
                    passwordDialog.cancel();
                    processPasswordEntry(wallet, pass);
                    passwordDialog = null;
                    return false;
                }
                return false;
            }
        });

        passwordDialog.show();
    }

    private boolean checkWalletPassword(String walletName, String password) {
        String walletPath = new File(Helper.getStorageRoot(getApplicationContext()),
                walletName + ".keys").getAbsolutePath();
        // only test view key
        return WalletManager.getInstance().verifyWalletPassword(walletPath, password, true);
    }

    private void processPasswordEntry(String walletName, String pass) {
        if (checkWalletPassword(walletName, pass)) {
            startWallet(walletName, pass);
        } else {
            Toast.makeText(this, getString(R.string.bad_password), Toast.LENGTH_SHORT).show();
        }
    }

    ////////////////////////////////////////
    // LoginFragment.Listener
    ////////////////////////////////////////
    @Override
    public SharedPreferences getPrefs() {
        return getPreferences(Context.MODE_PRIVATE);
    }

    @Override
    public File getStorageRoot() {
        return Helper.getStorageRoot(getApplicationContext());
    }

    ////////////////////////////////////////
    ////////////////////////////////////////

    @Override
    protected void onPause() {
        Log.d(TAG, "onPause()");
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume()");
    }


    void startWallet(String walletName, String walletPassword) {
        Log.d(TAG, "startWallet()");
        Intent intent = new Intent(getApplicationContext(), WalletActivity.class);
        intent.putExtra(WalletActivity.REQUEST_ID, walletName);
        intent.putExtra(WalletActivity.REQUEST_PW, walletPassword);
        startActivity(intent);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        Log.d(TAG, "onRequestPermissionsResult()");
        switch (requestCode) {
            case Helper.PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE:
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startLoginFragment();
                } else {
                    String msg = getString(R.string.message_strorage_not_permitted);
                    Log.e(TAG, msg);
                    Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                    //throw new IllegalStateException(msg);
                }
                break;
            default:
        }
    }

    void startLoginFragment() {
        Fragment fragment = new LoginFragment();
        getFragmentManager().beginTransaction()
                .add(R.id.fragment_container, fragment).commit();
        Log.d(TAG, "LoginFragment added");
    }

    void startGenerateFragment() {
        replaceFragment(new GenerateFragment());
        Log.d(TAG, "GenerateFragment placed");
    }

    void replaceFragment(Fragment newFragment) {
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, newFragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }

    //////////////////////////////////////////
    // GenerateFragment.Listener
    //////////////////////////////////////////
    static final String MNEMONIC_LANGUAGE = "English"; // see mnemonics/electrum-words.cpp for more

    @Override
    public void onGenerate(final String name, final String password) {
        final GenerateFragment genFragment = (GenerateFragment)
                getFragmentManager().findFragmentById(R.id.fragment_container);
        File newWalletFolder = new File(getStorageRoot(), ".new");
        if (!newWalletFolder.exists()) {
            if (!newWalletFolder.mkdir()) {
                Log.e(TAG, "Cannot create new wallet dir " + newWalletFolder.getAbsolutePath());
                genFragment.showMnemonic("");
                return;
            }
        }
        if (!newWalletFolder.isDirectory()) {
            Log.e(TAG, "New wallet dir " + newWalletFolder.getAbsolutePath() + "is not a directory");
            genFragment.showMnemonic("");
            return;
        }
        File cache = new File(newWalletFolder, name);
        cache.delete();
        File keys = new File(newWalletFolder, name + ".keys");
        keys.delete();
        File address = new File(newWalletFolder, name + ".address.txt");
        address.delete();

        if (cache.exists() || keys.exists() || address.exists()) {
            Log.e(TAG, "Cannot remove all old wallet files: " + cache.getAbsolutePath());
            genFragment.showMnemonic("");
            return;
        }

        final String newWalletPath = new File(newWalletFolder, name).getAbsolutePath();
        new Thread(null,
                new Runnable() {
                    @Override
                    public void run() {
                        Log.d(TAG, "creating wallet " + newWalletPath);
                        Wallet newWallet = WalletManager.getInstance()
                                .createWallet(newWalletPath, password, MNEMONIC_LANGUAGE);
                        Log.d(TAG, "wallet created");
                        Log.d(TAG, "Created " + newWallet.getAddress());
                        Log.d(TAG, "Seed " + newWallet.getSeed() + ".");
                        final String mnemonic = newWallet.getSeed();
                        newWallet.close();
                        runOnUiThread(new Runnable() {
                            public void run() {
                                if (genFragment.isAdded())
                                    genFragment.showMnemonic(mnemonic);
                            }
                        });
                    }
                }
                , "CreateWallet", MoneroHandlerThread.THREAD_STACK_SIZE).start();
    }

    @Override
    public void onAccept(final String name, final String password) {
        final GenerateFragment genFragment = (GenerateFragment)
                getFragmentManager().findFragmentById(R.id.fragment_container);
        File newWalletFolder = new File(getStorageRoot(), ".new");
        if (!newWalletFolder.isDirectory()) {
            Log.e(TAG, "New wallet dir " + newWalletFolder.getAbsolutePath() + "is not a directory");
            return;
        }
        final String newWalletPath = new File(newWalletFolder, name).getAbsolutePath();
        new Thread(null,
                new Runnable() {
                    @Override
                    public void run() {
                        Log.d(TAG, "opening wallet " + newWalletPath);
                        Wallet newWallet = WalletManager.getInstance()
                                .openWallet(newWalletPath, password);
                        Wallet.Status status = newWallet.getStatus();
                        Log.d(TAG, "wallet opened " + newWallet.getStatus());
                        if (status != Wallet.Status.Status_Ok) {
                            Log.e(TAG, "New wallet is " + status.toString());
                            runOnUiThread(new Runnable() {
                                public void run() {
                                    Toast.makeText(LoginActivity.this,
                                            getString(R.string.generate_wallet_create_failed_1), Toast.LENGTH_LONG).show();
                                }
                            });
                            newWallet.close();
                            return;
                        }
                        final String walletPath = new File(getStorageRoot(), name).getAbsolutePath();
                        final boolean rc = newWallet.store(walletPath);
                        Log.d(TAG, "wallet stored with rc=" + rc);
                        newWallet.close();
                        Log.d(TAG, "wallet closed");
                        runOnUiThread(new Runnable() {
                            public void run() {
                                if (rc) {
                                    if (genFragment.isAdded())
                                        getFragmentManager().popBackStack();
                                    Toast.makeText(LoginActivity.this,
                                            getString(R.string.generate_wallet_created), Toast.LENGTH_SHORT).show();
                                } else {
                                    Log.e(TAG, "Wallet store failed to " + walletPath);
                                    Toast.makeText(LoginActivity.this,
                                            getString(R.string.generate_wallet_create_failed_2), Toast.LENGTH_LONG).show();
                                }
                            }
                        });
                    }
                }
                , "AcceptWallet", MoneroHandlerThread.THREAD_STACK_SIZE).start();
    }

}
