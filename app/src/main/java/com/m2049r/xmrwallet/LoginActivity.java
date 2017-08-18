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
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.m2049r.xmrwallet.model.Wallet;
import com.m2049r.xmrwallet.model.WalletManager;
import com.m2049r.xmrwallet.service.MoneroHandlerThread;
import com.m2049r.xmrwallet.util.Helper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

public class LoginActivity extends AppCompatActivity
        implements LoginFragment.Listener, GenerateFragment.Listener, GenerateReviewFragment.Listener {
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
                promptPassword(walletName, new PasswordAction() {
                    @Override
                    public void action(String walletName, String password) {
                        startWallet(walletName, password);
                    }
                });
            } else { // this cannot really happen as we prefilter choices
                Toast.makeText(this, getString(R.string.bad_wallet), Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onWalletDetails(final String walletName) {
        Log.d(TAG, "details for wallet ." + walletName + ".");
        final String walletPath = Helper.getWalletPath(this, walletName);
        if (WalletManager.getInstance().walletExists(walletPath)) {
            promptPassword(walletName, new PasswordAction() {
                @Override
                public void action(String walletName, String password) {
                    startDetails(walletPath, password, GenerateReviewFragment.VIEW_DETAILS);
                }
            });
        } else { // this cannot really happen as we prefilter choices
            Toast.makeText(this, getString(R.string.bad_wallet), Toast.LENGTH_SHORT).show();
        }
    }

    AlertDialog passwordDialog = null; // for preventing multiple clicks in wallet list

    void promptPassword(final String wallet, final PasswordAction action) {
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
                                processPasswordEntry(wallet, pass, action);
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
                    processPasswordEntry(wallet, pass, action);
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

    interface PasswordAction {
        void action(String walletName, String password);
    }

    private void processPasswordEntry(String walletName, String pass, PasswordAction action) {
        if (checkWalletPassword(walletName, pass)) {
            action.action(walletName, pass);
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

    @Override
    public void setTitle(String title) {
        super.setTitle(title);
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

    void startDetails(final String walletPath, final String password, String type) {
        Log.d(TAG, "startDetails()");
        Bundle b = new Bundle();
        b.putString("name", walletPath);
        b.putString("password", password);
        b.putString("type", type);
        startReviewFragment(b);
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
        replaceFragment(new GenerateFragment(), "gen", null);
        Log.d(TAG, "GenerateFragment placed");
    }

    void startReviewFragment(Bundle extras) {
        replaceFragment(new GenerateReviewFragment(), null, extras);
        Log.d(TAG, "GenerateReviewFragment placed");
    }

    void replaceFragment(Fragment newFragment, String name, Bundle extras) {
        if (extras != null) {
            newFragment.setArguments(extras);
        }
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, newFragment);
        transaction.addToBackStack(name);
        transaction.commit();
    }

    void popFragmentStack(String name) {
        getFragmentManager().popBackStack(name, FragmentManager.POP_BACK_STACK_INCLUSIVE);
    }

    //////////////////////////////////////////
    // GenerateFragment.Listener
    //////////////////////////////////////////
    static final String MNEMONIC_LANGUAGE = "English"; // see mnemonics/electrum-words.cpp for more

    public void createWallet(final String name, final String password, final WalletCreator walletCreator) {
        final GenerateFragment genFragment = (GenerateFragment)
                getFragmentManager().findFragmentById(R.id.fragment_container);
        File newWalletFolder = new File(getStorageRoot(), ".new");
        if (!newWalletFolder.exists()) {
            if (!newWalletFolder.mkdir()) {
                Log.e(TAG, "Cannot create new wallet dir " + newWalletFolder.getAbsolutePath());
                genFragment.walletGenerateError();
                return;
            }
        }
        if (!newWalletFolder.isDirectory()) {
            Log.e(TAG, "New wallet dir " + newWalletFolder.getAbsolutePath() + "is not a directory");
            genFragment.walletGenerateError();
            return;
        }
        File cacheFile = new File(newWalletFolder, name);
        cacheFile.delete();
        File keysFile = new File(newWalletFolder, name + ".keys");
        keysFile.delete();
        final File addressFile = new File(newWalletFolder, name + ".address.txt");
        addressFile.delete();

        if (cacheFile.exists() || keysFile.exists() || addressFile.exists()) {
            Log.e(TAG, "Cannot remove all old wallet files: " + cacheFile.getAbsolutePath());
            genFragment.walletGenerateError();
            return;
        }

        String newWalletPath = new File(newWalletFolder, name).getAbsolutePath();
        boolean success = walletCreator.createWallet(newWalletPath, password);
        if (success) {
            startDetails(newWalletPath, password, GenerateReviewFragment.VIEW_ACCEPT);
        } else {
            Toast.makeText(LoginActivity.this,
                    getString(R.string.generate_wallet_create_failed), Toast.LENGTH_LONG).show();
            Log.e(TAG, "Could not create new wallet in " + newWalletPath);

        }
    }

    interface WalletCreator {
        boolean createWallet(String path, String password);
    }

    @Override
    public void onGenerate(String name, String password) {
        createWallet(name, password,
                new WalletCreator() {
                    public boolean createWallet(String path, String password) {
                        Wallet newWallet = WalletManager.getInstance()
                                .createWallet(path, password, MNEMONIC_LANGUAGE);
                        boolean success = (newWallet.getStatus() == Wallet.Status.Status_Ok);
                        if (!success) Log.e(TAG, newWallet.getErrorString());
                        newWallet.close();
                        return success;
                    }
                });
    }

    @Override
    public void onGenerate(String name, String password, final String seed, final long restoreHeight) {
        createWallet(name, password,
                new WalletCreator() {
                    public boolean createWallet(String path, String password) {
                        Wallet newWallet = WalletManager.getInstance().recoveryWallet(path, seed, restoreHeight);
                        boolean success = (newWallet.getStatus() == Wallet.Status.Status_Ok);
                        if (!success) Log.e(TAG, newWallet.getErrorString());
                        newWallet.setPassword(password);
                        success = success && newWallet.store();
                        newWallet.close();
                        return success;
                    }
                });
    }

    @Override
    public void onGenerate(String name, String password,
                           final String address, final String viewKey, final String spendKey, final long restoreHeight) {
        createWallet(name, password,
                new WalletCreator() {
                    public boolean createWallet(String path, String password) {
                        Wallet newWallet = WalletManager.getInstance()
                                .createWalletFromKeys(path, MNEMONIC_LANGUAGE, restoreHeight,
                                        address, viewKey, spendKey);
                        boolean success = (newWallet.getStatus() == Wallet.Status.Status_Ok);
                        if (!success) Log.e(TAG, newWallet.getErrorString());
                        newWallet.setPassword(password);
                        success = success && newWallet.store();
                        newWallet.close();
                        return success;
                    }
                });
    }


    @Override
    public void onAccept(final String name, final String password) {
        final File newWalletFolder = new File(getStorageRoot(), ".new");
        final File walletFolder = getStorageRoot();
        final String walletPath = new File(walletFolder, name).getAbsolutePath();
        final boolean rc = copyWallet(walletFolder, newWalletFolder, name)
                &&
                (testWallet(walletPath, password) == Wallet.Status.Status_Ok);
        if (rc) {
            popFragmentStack("gen");
            Toast.makeText(LoginActivity.this,
                    getString(R.string.generate_wallet_created), Toast.LENGTH_SHORT).show();
        } else {
            Log.e(TAG, "Wallet store failed to " + walletPath);
            Toast.makeText(LoginActivity.this,
                    getString(R.string.generate_wallet_create_failed_2), Toast.LENGTH_LONG).show();
        }
    }

    Wallet.Status testWallet(String path, String password) {
        Log.d(TAG, "testing wallet " + path);
        Wallet aWallet = WalletManager.getInstance().openWallet(path, password);
        if (aWallet == null) return Wallet.Status.Status_Error; // does this ever happen?
        Wallet.Status status = aWallet.getStatus();
        Log.d(TAG, "wallet tested " + aWallet.getStatus());
        aWallet.close();
        return status;
    }

    boolean copyWallet(File dstDir, File srcDir, String name) {
        boolean success = false;
        try {
            // TODO: the cache is corrupt if we recover (!!)
            // TODO: the cache is ok if we immediately to a full refresh()
            // TODO recoveryheight is ignored but not on watchonly wallet ?! - find out why
            //copyFile(dstDir, srcDir, name);
            copyFile(dstDir, srcDir, name + ".keys");
            copyFile(dstDir, srcDir, name + ".address.txt");
            success = true;
        } catch (IOException ex) {
            Log.e(TAG, "wallet copy failed: " + ex.getMessage());
            // try to rollback
            new File(dstDir, name).delete();
            new File(dstDir, name + ".keys").delete();
            new File(dstDir, name + ".address.txt").delete();
        }
        return success;
    }

    void copyFile(File dstDir, File srcDir, String name) throws IOException {
        FileChannel inChannel = new FileInputStream(new File(srcDir, name)).getChannel();
        FileChannel outChannel = new FileOutputStream(new File(dstDir, name)).getChannel();
        try {
            inChannel.transferTo(0, inChannel.size(), outChannel);
        } finally {
            if (inChannel != null)
                inChannel.close();
            if (outChannel != null)
                outChannel.close();
        }
    }
}
