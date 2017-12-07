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
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.MediaScannerConnection;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.m2049r.xmrwallet.dialog.AboutFragment;
import com.m2049r.xmrwallet.dialog.DonationFragment;
import com.m2049r.xmrwallet.dialog.HelpFragment;
import com.m2049r.xmrwallet.dialog.PrivacyFragment;
import com.m2049r.xmrwallet.model.Wallet;
import com.m2049r.xmrwallet.model.WalletManager;
import com.m2049r.xmrwallet.service.WalletService;
import com.m2049r.xmrwallet.util.Helper;
import com.m2049r.xmrwallet.util.MoneroThreadPoolExecutor;
import com.m2049r.xmrwallet.widget.Toolbar;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.channels.FileChannel;
import java.util.Date;

import timber.log.Timber;

public class LoginActivity extends SecureActivity
        implements LoginFragment.Listener, GenerateFragment.Listener,
        GenerateReviewFragment.Listener, GenerateReviewFragment.AcceptListener, ReceiveFragment.Listener {
    private static final String GENERATE_STACK = "gen";

    static final int DAEMON_TIMEOUT = 500; // deamon must respond in 500ms

    private Toolbar toolbar;

    @Override
    public void setToolbarButton(int type) {
        toolbar.setButton(type);
    }

    @Override
    public void setTitle(String title) {
        toolbar.setTitle(title);
    }

    @Override
    public void setSubtitle(String subtitle) {
        toolbar.setSubtitle(subtitle);
    }

    @Override
    public void setTitle(String title, String subtitle) {
        toolbar.setTitle(title, subtitle);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Timber.d("onCreate()");
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            // we don't store anything ourselves
        }

        setContentView(R.layout.activity_login);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        toolbar.setOnButtonListener(new Toolbar.OnButtonListener() {
            @Override
            public void onButton(int type) {
                switch (type) {
                    case Toolbar.BUTTON_BACK:
                        onBackPressed();
                        break;
                    case Toolbar.BUTTON_CLOSE:
                        finish();
                        break;
                    case Toolbar.BUTTON_DONATE:
                        DonationFragment.display(getSupportFragmentManager());
                        break;
                    case Toolbar.BUTTON_NONE:
                    default:
                        Timber.e("Button " + type + "pressed - how can this be?");
                }
            }
        });

        if (Helper.getWritePermission(this)) {
            if (savedInstanceState == null) startLoginFragment();
        } else {
            Timber.i("Waiting for permissions");
        }
    }

    boolean checkServiceRunning() {
        if (WalletService.Running) {
            Toast.makeText(this, getString(R.string.service_busy), Toast.LENGTH_SHORT).show();
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean onWalletSelected(String walletName, String daemon, boolean testnet) {
        if (daemon.length() == 0) {
            Toast.makeText(this, getString(R.string.prompt_daemon_missing), Toast.LENGTH_SHORT).show();
            return false;
        }
        if (checkServiceRunning()) return false;
        try {
            WalletNode aWalletNode = new WalletNode(walletName, daemon, testnet);
            new AsyncOpenWallet().execute(aWalletNode);
        } catch (IllegalArgumentException ex) {
            Timber.e(ex.getLocalizedMessage());
            Toast.makeText(this, ex.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    @Override
    public void onWalletDetails(final String walletName, boolean testnet) {
        setNet(testnet);
        Timber.d("details for wallet ." + walletName + ".");
        if (checkServiceRunning()) return;
        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case DialogInterface.BUTTON_POSITIVE:
                        final File walletFile = Helper.getWalletFile(LoginActivity.this, walletName);
                        if (WalletManager.getInstance().walletExists(walletFile)) {
                            promptPassword(walletName, new PasswordAction() {
                                @Override
                                public void action(String walletName, String password) {
                                    startDetails(walletFile, password, GenerateReviewFragment.VIEW_TYPE_DETAILS);
                                }
                            });
                        } else { // this cannot really happen as we prefilter choices
                            Timber.e("Wallet missing: %s", walletName);
                            Toast.makeText(LoginActivity.this, getString(R.string.bad_wallet), Toast.LENGTH_SHORT).show();
                        }
                        break;

                    case DialogInterface.BUTTON_NEGATIVE:
                        // do nothing
                        break;
                }
            }
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(getString(R.string.details_alert_message))
                .setPositiveButton(getString(R.string.details_alert_yes), dialogClickListener)
                .setNegativeButton(getString(R.string.details_alert_no), dialogClickListener)
                .show();
    }

    @Override
    public void onWalletReceive(String walletName, boolean testnet) {
        setNet(testnet);
        Timber.d("receive for wallet ." + walletName + ".");
        if (checkServiceRunning()) return;
        final File walletFile = Helper.getWalletFile(this, walletName);
        if (WalletManager.getInstance().walletExists(walletFile)) {
            promptPassword(walletName, new PasswordAction() {
                @Override
                public void action(String walletName, String password) {
                    startReceive(walletFile, password);
                }
            });
        } else { // this cannot really happen as we prefilter choices
            Toast.makeText(this, getString(R.string.bad_wallet), Toast.LENGTH_SHORT).show();
        }
    }

    private class AsyncRename extends AsyncTask<String, Void, Boolean> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            showProgressDialog(R.string.rename_progress);
        }

        @Override
        protected Boolean doInBackground(String... params) {
            if (params.length != 2) return false;
            File walletFile = Helper.getWalletFile(LoginActivity.this, params[0]);
            String newName = params[1];
            return renameWallet(walletFile, newName);
        }

        @Override
        protected void onPostExecute(Boolean result) {
            super.onPostExecute(result);
            if (isDestroyed()) {
                return;
            }
            dismissProgressDialog();
            if (result) {
                reloadWalletList();
            } else {
                Toast.makeText(LoginActivity.this, getString(R.string.rename_failed), Toast.LENGTH_LONG).show();
            }
        }
    }

    // copy + delete seems safer than rename because we call rollback easily
    boolean renameWallet(File walletFile, String newName) {
        if (copyWallet(walletFile, new File(walletFile.getParentFile(), newName), false, true)) {
            deleteWallet(walletFile);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void onWalletRename(final String walletName) {
        Timber.d("rename for wallet ." + walletName + ".");
        if (checkServiceRunning()) return;
        LayoutInflater li = LayoutInflater.from(this);
        View promptsView = li.inflate(R.layout.prompt_rename, null);

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setView(promptsView);

        final EditText etRename = (EditText) promptsView.findViewById(R.id.etRename);
        final TextView tvRenameLabel = (TextView) promptsView.findViewById(R.id.tvRenameLabel);

        tvRenameLabel.setText(getString(R.string.prompt_rename, walletName));

        // set dialog message
        alertDialogBuilder
                .setCancelable(false)
                .setPositiveButton("OK",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                Helper.hideKeyboardAlways(LoginActivity.this);
                                String newName = etRename.getText().toString();
                                new AsyncRename().execute(walletName, newName);
                            }
                        })
                .setNegativeButton("Cancel",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                Helper.hideKeyboardAlways(LoginActivity.this);
                                dialog.cancel();
                            }
                        });

        final AlertDialog dialog = alertDialogBuilder.create();
        Helper.showKeyboard(dialog);

        // accept keyboard "ok"
        etRename.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if ((event != null && (event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) || (actionId == EditorInfo.IME_ACTION_DONE)) {
                    Helper.hideKeyboardAlways(LoginActivity.this);
                    String newName = etRename.getText().toString();
                    dialog.cancel();
                    new AsyncRename().execute(walletName, newName);
                    return false;
                }
                return false;
            }
        });

        dialog.show();
    }

    private class AsyncBackup extends AsyncTask<String, Void, Boolean> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            showProgressDialog(R.string.backup_progress);
        }

        @Override
        protected Boolean doInBackground(String... params) {
            if (params.length != 1) return false;
            return backupWallet(params[0]);
        }

        @Override
        protected void onPostExecute(Boolean result) {
            super.onPostExecute(result);
            if (isDestroyed()) {
                return;
            }
            dismissProgressDialog();
            if (!result) {
                Toast.makeText(LoginActivity.this, getString(R.string.backup_failed), Toast.LENGTH_LONG).show();
            }
        }
    }

    private boolean backupWallet(String walletName) {
        File backupFolder = new File(getStorageRoot(), "backups");
        if (!backupFolder.exists()) {
            if (!backupFolder.mkdir()) {
                Timber.e("Cannot create backup dir %s", backupFolder.getAbsolutePath());
                return false;
            }
            // make folder visible over USB/MTP
            MediaScannerConnection.scanFile(this, new String[]{backupFolder.toString()}, null, null);
        }
        File walletFile = Helper.getWalletFile(LoginActivity.this, walletName);
        File backupFile = new File(backupFolder, walletName);
        Timber.d("backup " + walletFile.getAbsolutePath() + " to " + backupFile.getAbsolutePath());
        // TODO probably better to copy to a new file and then rename
        // then if something fails we have the old backup at least
        // or just create a new backup every time and keep n old backups
        boolean success = copyWallet(walletFile, backupFile, true, true);
        Timber.d("copyWallet is %s", success);
        return success;
    }

    @Override
    public void onWalletBackup(String walletName) {
        Timber.d("backup for wallet ." + walletName + ".");
        new AsyncBackup().execute(walletName);
    }

    private class AsyncArchive extends AsyncTask<String, Void, Boolean> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            showProgressDialog(R.string.archive_progress);
        }

        @Override
        protected Boolean doInBackground(String... params) {
            if (params.length != 1) return false;
            String walletName = params[0];
            if (backupWallet(walletName) && deleteWallet(Helper.getWalletFile(LoginActivity.this, walletName))) {
                return true;
            } else {
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean result) {
            super.onPostExecute(result);
            if (isDestroyed()) {
                return;
            }
            dismissProgressDialog();
            if (result) {
                reloadWalletList();
            } else {
                Toast.makeText(LoginActivity.this, getString(R.string.archive_failed), Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public void onWalletArchive(final String walletName) {
        Timber.d("archive for wallet ." + walletName + ".");
        if (checkServiceRunning()) return;
        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case DialogInterface.BUTTON_POSITIVE:
                        new AsyncArchive().execute(walletName);
                        break;
                    case DialogInterface.BUTTON_NEGATIVE:
                        // do nothing
                        break;
                }
            }
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(getString(R.string.archive_alert_message))
                .setTitle(walletName)
                .setPositiveButton(getString(R.string.archive_alert_yes), dialogClickListener)
                .setNegativeButton(getString(R.string.archive_alert_no), dialogClickListener)
                .show();
    }

    void reloadWalletList() {
        Timber.d("reloadWalletList()");
        try {
            LoginFragment loginFragment = (LoginFragment)
                    getSupportFragmentManager().findFragmentById(R.id.fragment_container);
            if (loginFragment != null) {
                loginFragment.loadList();
            }
        } catch (ClassCastException ex) {
        }
    }

    @Override
    public void onAddWallet(boolean testnet, String type) {
        setNet(testnet);
        if (checkServiceRunning()) return;
        startGenerateFragment(type);
    }

    AlertDialog passwordDialog = null; // for preventing multiple clicks in wallet list

    void promptPassword(final String wallet, final PasswordAction action) {
        if (passwordDialog != null) return; // we are already asking for password
        Context context = LoginActivity.this;
        LayoutInflater li = LayoutInflater.from(context);
        View promptsView = li.inflate(R.layout.prompt_password, null);

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context);
        alertDialogBuilder.setView(promptsView);

        final TextInputLayout etPassword = (TextInputLayout) promptsView.findViewById(R.id.etPassword);
        etPassword.setHint(LoginActivity.this.getString(R.string.prompt_password, wallet));

        etPassword.getEditText().addTextChangedListener(new TextWatcher() {

            @Override
            public void afterTextChanged(Editable s) {
                if (etPassword.getError() != null) {
                    etPassword.setError(null);
                }
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start,
                                          int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start,
                                      int before, int count) {
            }
        });

        // set dialog message
        alertDialogBuilder
                .setCancelable(false)
                .setPositiveButton("OK", null)
                .setNegativeButton("Cancel",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                Helper.hideKeyboardAlways(LoginActivity.this);
                                dialog.cancel();
                                passwordDialog = null;
                            }
                        });
        passwordDialog = alertDialogBuilder.create();

        passwordDialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                Button button = ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_POSITIVE);
                button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        String pass = etPassword.getEditText().getText().toString();
                        if (processPasswordEntry(wallet, pass, action)) {
                            Helper.hideKeyboardAlways(LoginActivity.this);
                            passwordDialog.dismiss();
                            passwordDialog = null;
                        } else {
                            etPassword.setError(getString(R.string.bad_password));
                        }
                    }
                });
            }
        });

        Helper.showKeyboard(passwordDialog);

        // accept keyboard "ok"
        etPassword.getEditText().setOnEditorActionListener(new TextView.OnEditorActionListener() {
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if ((event != null && (event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) || (actionId == EditorInfo.IME_ACTION_DONE)) {
                    String pass = etPassword.getEditText().getText().toString();
                    if (processPasswordEntry(wallet, pass, action)) {
                        Helper.hideKeyboardAlways(LoginActivity.this);
                        passwordDialog.dismiss();
                        passwordDialog = null;
                    } else {
                        etPassword.setError(getString(R.string.bad_password));
                    }
                    return true;
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

    private boolean processPasswordEntry(String walletName, String pass, PasswordAction action) {
        if (checkWalletPassword(walletName, pass)) {
            action.action(walletName, pass);
            return true;
        } else {
            return false;
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
    public void showNet(boolean testnet) {
        if (testnet) {
            toolbar.setBackgroundResource(R.color.colorPrimaryDark);
        } else {
            toolbar.setBackgroundResource(R.drawable.backgound_toolbar_mainnet);
        }
        toolbar.setSubtitle(getString(testnet ? R.string.connect_testnet : R.string.connect_mainnet));
    }

    @Override
    protected void onPause() {
        Timber.d("onPause()");
        super.onPause();
    }

    ProgressDialog progressDialog = null;

    private void showProgressDialog(int msgId) {
        showProgressDialog(msgId, 0);
    }

    private void showProgressDialog(int msgId, long delay) {
        dismissProgressDialog(); // just in case
        progressDialog = new MyProgressDialog(LoginActivity.this, msgId);
        if (delay > 0) {
            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                public void run() {
                    if (progressDialog != null) progressDialog.show();
                }
            }, delay);
        } else {
            progressDialog.show();
        }
    }

    private void dismissProgressDialog() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
        progressDialog = null;
    }

    @Override
    protected void onDestroy() {
        dismissProgressDialog();
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Timber.d("onResume()");
        // wait for WalletService to finish
        if (WalletService.Running && (progressDialog == null)) {
            // and show a progress dialog, but only if there isn't one already
            new AsyncWaitForService().execute();
        }
    }

    private class MyProgressDialog extends ProgressDialog {
        Activity activity;

        public MyProgressDialog(Activity activity, int msgId) {
            super(activity);
            this.activity = activity;
            setCancelable(false);
            setMessage(activity.getString(msgId));
        }

        @Override
        public void onBackPressed() {
            // prevent back button
        }
    }


    private class AsyncWaitForService extends AsyncTask<Void, Void, Void> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            showProgressDialog(R.string.service_progress);
        }

        @Override
        protected Void doInBackground(Void... params) {
            try {
                while (WalletService.Running & !isCancelled()) {
                    Thread.sleep(250);
                }
            } catch (InterruptedException ex) {
                // oh well ...
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            if (isDestroyed()) {
                return;
            }
            dismissProgressDialog();
        }
    }

    void startWallet(String walletName, String walletPassword) {
        Timber.d("startWallet()");
        Intent intent = new Intent(getApplicationContext(), WalletActivity.class);
        intent.putExtra(WalletActivity.REQUEST_ID, walletName);
        intent.putExtra(WalletActivity.REQUEST_PW, walletPassword);
        startActivity(intent);
    }

    void startDetails(File walletFile, String password, String type) {
        Timber.d("startDetails()");
        Bundle b = new Bundle();
        b.putString("path", walletFile.getAbsolutePath());
        b.putString("password", password);
        b.putString("type", type);
        startReviewFragment(b);
    }

    void startReceive(File walletFile, String password) {
        Timber.d("startReceive()");
        Bundle b = new Bundle();
        b.putString("path", walletFile.getAbsolutePath());
        b.putString("password", password);
        startReceiveFragment(b);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        Timber.d("onRequestPermissionsResult()");
        switch (requestCode) {
            case Helper.PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE:
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startLoginFragment = true;
                } else {
                    String msg = getString(R.string.message_strorage_not_permitted);
                    Timber.e(msg);
                    Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                }
                break;
            default:
        }
    }

    private boolean startLoginFragment = false;

    @Override
    protected void onResumeFragments() {
        super.onResumeFragments();
        if (startLoginFragment) {
            startLoginFragment();
            startLoginFragment = false;
        }
    }

    void startLoginFragment() {
        Fragment fragment = new LoginFragment();
        getSupportFragmentManager().beginTransaction()
                .add(R.id.fragment_container, fragment).commit();
        Timber.d("LoginFragment added");
    }

    void startGenerateFragment(String type) {
        Bundle extras = new Bundle();
        extras.putString(GenerateFragment.TYPE, type);
        replaceFragment(new GenerateFragment(), GENERATE_STACK, extras);
        Timber.d("GenerateFragment placed");
    }

    void startReviewFragment(Bundle extras) {
        replaceFragment(new GenerateReviewFragment(), null, extras);
        Timber.d("GenerateReviewFragment placed");
    }

    void startReceiveFragment(Bundle extras) {
        replaceFragment(new ReceiveFragment(), null, extras);
        Timber.d("ReceiveFragment placed");
    }

    void replaceFragment(Fragment newFragment, String stackName, Bundle extras) {
        if (extras != null) {
            newFragment.setArguments(extras);
        }
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, newFragment);
        transaction.addToBackStack(stackName);
        transaction.commit();
    }

    void popFragmentStack(String name) {
        getSupportFragmentManager().popBackStack(name, FragmentManager.POP_BACK_STACK_INCLUSIVE);
    }

    //////////////////////////////////////////
    // GenerateFragment.Listener
    //////////////////////////////////////////
    static final String MNEMONIC_LANGUAGE = "English"; // see mnemonics/electrum-words.cpp for more

    private class AsyncCreateWallet extends AsyncTask<Void, Void, Boolean> {
        String walletName;
        String walletPassword;
        WalletCreator walletCreator;

        File newWalletFile;

        public AsyncCreateWallet(String name, String password, WalletCreator walletCreator) {
            super();
            this.walletName = name;
            this.walletPassword = password;
            this.walletCreator = walletCreator;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            showProgressDialog(R.string.generate_wallet_creating);
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            // check if the wallet we want to create already exists
            File walletFolder = getStorageRoot();
            if (!walletFolder.isDirectory()) {
                Timber.e("Wallet dir " + walletFolder.getAbsolutePath() + "is not a directory");
                return false;
            }
            File cacheFile = new File(walletFolder, walletName);
            File keysFile = new File(walletFolder, walletName + ".keys");
            File addressFile = new File(walletFolder, walletName + ".address.txt");

            if (cacheFile.exists() || keysFile.exists() || addressFile.exists()) {
                Timber.e("Some wallet files already exist for %s", cacheFile.getAbsolutePath());
                return false;
            }

            File newWalletFolder = Helper.getNewWalletDir(getApplicationContext());
            if (!newWalletFolder.isDirectory()) {
                Timber.e("New Wallet dir " + newWalletFolder.getAbsolutePath() + "is not a directory");
                return false;
            }
            newWalletFile = new File(newWalletFolder, walletName);
            boolean success = walletCreator.createWallet(newWalletFile, walletPassword);
            if (success) {
                return true;
            } else {
                Timber.e("Could not create new wallet in %s", newWalletFile.getAbsolutePath());
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean result) {
            super.onPostExecute(result);
            if (isDestroyed()) {
                return;
            }
            dismissProgressDialog();
            if (result) {
                startDetails(newWalletFile, walletPassword, GenerateReviewFragment.VIEW_TYPE_ACCEPT);
            } else {
                walletGenerateError();
            }
        }
    }

    public void createWallet(String name, String password, WalletCreator walletCreator) {
        new AsyncCreateWallet(name, password, walletCreator)
                .executeOnExecutor(MoneroThreadPoolExecutor.MONERO_THREAD_POOL_EXECUTOR);
    }

    void walletGenerateError() {
        try {
            GenerateFragment genFragment = (GenerateFragment)
                    getSupportFragmentManager().findFragmentById(R.id.fragment_container);
            genFragment.walletGenerateError();
        } catch (ClassCastException ex) {
            Timber.e("walletGenerateError() but not in GenerateFragment");
        }
    }

    interface WalletCreator {
        boolean createWallet(File aFile, String password);

    }

    @Override
    public void onGenerate(String name, String password) {
        createWallet(name, password,
                new WalletCreator() {
                    public boolean createWallet(File aFile, String password) {
                        Wallet newWallet = WalletManager.getInstance()
                                .createWallet(aFile, password, MNEMONIC_LANGUAGE);
                        boolean success = (newWallet.getStatus() == Wallet.Status.Status_Ok);
                        if (!success) {
                            Timber.e(newWallet.getErrorString());
                            toast(newWallet.getErrorString());
                        }
                        newWallet.close();
                        return success;
                    }
                });
    }

    @Override
    public void onGenerate(String name, String password, final String seed, final long restoreHeight) {
        createWallet(name, password,
                new WalletCreator() {
                    public boolean createWallet(File aFile, String password) {
                        Wallet newWallet = WalletManager.getInstance().recoveryWallet(aFile, seed, restoreHeight);
                        boolean success = (newWallet.getStatus() == Wallet.Status.Status_Ok);
                        if (success) {
                            newWallet.setPassword(password);
                            success = success && newWallet.store();
                        } else {
                            Timber.e(newWallet.getErrorString());
                            toast(newWallet.getErrorString());
                        }
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
                    public boolean createWallet(File aFile, String password) {
                        Wallet newWallet = WalletManager.getInstance()
                                .createWalletFromKeys(aFile, MNEMONIC_LANGUAGE, restoreHeight,
                                        address, viewKey, spendKey);
                        boolean success = (newWallet.getStatus() == Wallet.Status.Status_Ok);
                        if (success) {
                            newWallet.setPassword(password);
                            success = success && newWallet.store();
                        } else {
                            Timber.e(newWallet.getErrorString());
                            toast(newWallet.getErrorString());
                        }
                        newWallet.close();
                        return success;
                    }
                });
    }

    void toast(final String msg) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(LoginActivity.this, msg, Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void onAccept(final String name, final String password) {
        File newWalletFile = new File(Helper.getNewWalletDir(getApplicationContext()), name);
        Timber.d("New Wallet %s", newWalletFile.getAbsolutePath());
        newWalletFile.delete(); // when recovering wallets, the cache seems corrupt
        // TODO: figure out why this is so? Only for a private testnet?

        // now copy the new wallet to the wallet folder
        File walletFile = new File(getStorageRoot(), name);
        Timber.d("Wallet %s", walletFile.getAbsolutePath());
        copyWallet(newWalletFile, walletFile, false, true);
        deleteWallet(newWalletFile); // delete it no matter what (can't recover from this anyway)

        boolean rc = testWallet(walletFile.getAbsolutePath(), password) == Wallet.Status.Status_Ok;

        if (rc) {
            popFragmentStack(GENERATE_STACK);
            Toast.makeText(LoginActivity.this,
                    getString(R.string.generate_wallet_created), Toast.LENGTH_SHORT).show();
        } else {
            Timber.e("Wallet store failed to %s", walletFile.getAbsolutePath());
            Toast.makeText(LoginActivity.this, getString(R.string.generate_wallet_create_failed), Toast.LENGTH_LONG).show();
        }
    }

    Wallet.Status testWallet(String path, String password) {
        Timber.d("testing wallet %s", path);
        Wallet aWallet = WalletManager.getInstance().openWallet(path, password);
        if (aWallet == null) return Wallet.Status.Status_Error; // does this ever happen?
        Wallet.Status status = aWallet.getStatus();
        Timber.d("wallet tested %s", aWallet.getStatus());
        aWallet.close();
        return status;
    }

    boolean walletExists(File walletFile, boolean any) {
        File dir = walletFile.getParentFile();
        String name = walletFile.getName();
        if (any) {
            return new File(dir, name).exists()
                    || new File(dir, name + ".keys").exists()
                    || new File(dir, name + ".address.txt").exists();
        } else {
            return new File(dir, name).exists()
                    && new File(dir, name + ".keys").exists()
                    && new File(dir, name + ".address.txt").exists();
        }
    }

    boolean copyWallet(File srcWallet, File dstWallet, boolean overwrite, boolean ignoreCacheError) {
        if (walletExists(dstWallet, true) && !overwrite) return false;
        boolean success = false;
        File srcDir = srcWallet.getParentFile();
        String srcName = srcWallet.getName();
        File dstDir = dstWallet.getParentFile();
        String dstName = dstWallet.getName();
        try {
            try {
                copyFile(new File(srcDir, srcName), new File(dstDir, dstName));
            } catch (IOException ex) {
                Timber.d("CACHE %s", ignoreCacheError);
                if (!ignoreCacheError) { // ignore cache backup error if backing up (can be resynced)
                    throw ex;
                }
            }
            copyFile(new File(srcDir, srcName + ".keys"), new File(dstDir, dstName + ".keys"));
            copyFile(new File(srcDir, srcName + ".address.txt"), new File(dstDir, dstName + ".address.txt"));
            success = true;
        } catch (IOException ex) {
            Timber.e("wallet copy failed: %s", ex.getMessage());
            // try to rollback
            deleteWallet(dstWallet);
        }
        return success;
    }

    // do our best to delete as much as possible of the wallet files
    boolean deleteWallet(File walletFile) {
        Timber.d("deleteWallet %s", walletFile.getAbsolutePath());
        File dir = walletFile.getParentFile();
        String name = walletFile.getName();
        boolean success = true;
        File cacheFile = new File(dir, name);
        if (cacheFile.exists()) {
            success = cacheFile.delete();
        }
        success = new File(dir, name + ".keys").delete() && success;
        File addressFile = new File(dir, name + ".address.txt");
        if (addressFile.exists()) {
            success = addressFile.delete() && success;
        }
        Timber.d("deleteWallet is %s", success);
        return success;
    }

    void copyFile(File src, File dst) throws IOException {
        FileChannel inChannel = new FileInputStream(src).getChannel();
        FileChannel outChannel = new FileOutputStream(dst).getChannel();
        try {
            inChannel.transferTo(0, inChannel.size(), outChannel);
        } finally {
            if (inChannel != null)
                inChannel.close();
            if (outChannel != null)
                outChannel.close();
        }
    }

    @Override
    public void onBackPressed() {
        Fragment f = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
        if (f instanceof GenerateReviewFragment) {
            if (((GenerateReviewFragment) f).backOk()) {
                super.onBackPressed();
            }
        } else if (f instanceof LoginFragment) {
            if (((LoginFragment) f).isFabOpen()) {
                ((LoginFragment) f).animateFAB();
            } else {
                super.onBackPressed();
            }
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_create_help_new:
                HelpFragment.display(getSupportFragmentManager(), R.string.help_create_new);
                return true;
            case R.id.action_create_help_keys:
                HelpFragment.display(getSupportFragmentManager(), R.string.help_create_keys);
                return true;
            case R.id.action_create_help_view:
                HelpFragment.display(getSupportFragmentManager(), R.string.help_create_view);
                return true;
            case R.id.action_create_help_seed:
                HelpFragment.display(getSupportFragmentManager(), R.string.help_create_seed);
                return true;
            case R.id.action_details_help:
                HelpFragment.display(getSupportFragmentManager(), R.string.help_details);
                return true;
            case R.id.action_license_info:
                AboutFragment.display(getSupportFragmentManager());
                return true;
            case R.id.action_help_list:
                HelpFragment.display(getSupportFragmentManager(), R.string.help_list);
                return true;
            case R.id.action_privacy_policy:
                PrivacyFragment.display(getSupportFragmentManager());
                return true;
            case R.id.action_testnet:
                try {
                    LoginFragment loginFragment = (LoginFragment)
                            getSupportFragmentManager().findFragmentById(R.id.fragment_container);
                    item.setChecked(loginFragment.onTestnetMenuItem());
                } catch (ClassCastException ex) {
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void setNet(boolean testnet) {
        WalletManager.getInstance().setDaemon("", testnet, "", "");
    }

    static class WalletNode {
        String name = null;
        String host = "";
        int port = 28081;
        String user = "";
        String password = "";
        boolean isTestnet;

        WalletNode(String walletName, String daemon, boolean isTestnet) {
            if ((daemon == null) || daemon.isEmpty()) return;
            this.name = walletName;
            String daemonAddress;
            String a[] = daemon.split("@");
            if (a.length == 1) { // no credentials
                daemonAddress = a[0];
            } else if (a.length == 2) { // credentials
                String userPassword[] = a[0].split(":");
                if (userPassword.length != 2)
                    throw new IllegalArgumentException("User:Password invalid");
                user = userPassword[0];
                if (!user.isEmpty()) password = userPassword[1];
                daemonAddress = a[1];
            } else {
                throw new IllegalArgumentException("Too many @");
            }

            String da[] = daemonAddress.split(":");
            if ((da.length > 2) || (da.length < 1))
                throw new IllegalArgumentException("Too many ':' or too few");
            host = da[0];
            if (da.length == 2) {
                try {
                    port = Integer.parseInt(da[1]);
                } catch (NumberFormatException ex) {
                    throw new IllegalArgumentException("Port not numeric");
                }
            } else {
                port = (isTestnet ? 28081 : 18081);
            }
            this.isTestnet = isTestnet;
        }

        String getAddress() {
            return host + ":" + port;
        }

        boolean isValid() {
            return !host.isEmpty();
        }
    }

    private class AsyncOpenWallet extends AsyncTask<WalletNode, Void, Integer> {
        final static int OK = 0;
        final static int TIMEOUT = 1;
        final static int INVALID = 2;
        final static int IOEX = 3;

        WalletNode walletNode;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            showProgressDialog(R.string.open_progress, DAEMON_TIMEOUT / 4);
        }

        @Override
        protected Integer doInBackground(WalletNode... params) {
            if (params.length != 1) return INVALID;
            this.walletNode = params[0];
            if (!walletNode.isValid()) return INVALID;

            Timber.d("checking %s", walletNode.getAddress());

            try {
                long timeDA = new Date().getTime();
                SocketAddress address = new InetSocketAddress(walletNode.host, walletNode.port);
                long timeDB = new Date().getTime();
                Timber.d("Resolving " + walletNode.host + " took " + (timeDB - timeDA) + "ms.");
                Socket socket = new Socket();
                long timeA = new Date().getTime();
                socket.connect(address, LoginActivity.DAEMON_TIMEOUT);
                socket.close();
                long timeB = new Date().getTime();
                long time = timeB - timeA;
                Timber.d("Daemon " + walletNode.host + " is " + time + "ms away.");
                return (time < LoginActivity.DAEMON_TIMEOUT ? OK : TIMEOUT);
            } catch (IOException ex) {
                Timber.d("Cannot reach daemon " + walletNode.host + "/" + walletNode.port + " because " + ex.getMessage());
                return IOEX;
            } catch (IllegalArgumentException ex) {
                Timber.d("Cannot reach daemon " + walletNode.host + "/" + walletNode.port + " because " + ex.getMessage());
                return INVALID;
            }
        }

        @Override
        protected void onPostExecute(Integer result) {
            super.onPostExecute(result);
            if (isDestroyed()) {
                return;
            }
            dismissProgressDialog();
            switch (result) {
                case OK:
                    Timber.d("selected wallet is ." + walletNode.name + ".");
                    // now it's getting real, onValidateFields if wallet exists
                    promptAndStart(walletNode);
                    break;
                case TIMEOUT:
                    Toast.makeText(LoginActivity.this, getString(R.string.status_wallet_connect_timeout), Toast.LENGTH_LONG).show();
                    break;
                case INVALID:
                    Toast.makeText(LoginActivity.this, getString(R.string.status_wallet_node_invalid), Toast.LENGTH_LONG).show();
                    break;
                case IOEX:
                    Toast.makeText(LoginActivity.this, getString(R.string.status_wallet_connect_ioex), Toast.LENGTH_LONG).show();
                    break;
            }
        }
    }

    void promptAndStart(WalletNode walletNode) {
        File walletFile = Helper.getWalletFile(this, walletNode.name);
        if (WalletManager.getInstance().walletExists(walletFile)) {
            WalletManager.getInstance().
                    setDaemon(walletNode.getAddress(), walletNode.isTestnet, walletNode.user, walletNode.password);
            promptPassword(walletNode.name, new PasswordAction() {
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
