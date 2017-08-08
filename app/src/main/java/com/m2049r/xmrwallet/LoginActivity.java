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
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.m2049r.xmrwallet.model.WalletManager;
import com.m2049r.xmrwallet.util.Helper;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class LoginActivity extends Activity {
    static final String TAG = "LoginActivity";

    static final int MIN_DAEMON_VERSION = 65544;
    static final int DAEMON_TIMEOUT = 500; // deamon must respond in 500ms

    ListView listView;
    List<String> walletList = new ArrayList<>();
    List<String> displayedList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        final EditText etDaemonAddress = (EditText) findViewById(R.id.etDaemonAddress);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);

        etDaemonAddress.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.showSoftInput(etDaemonAddress, InputMethodManager.SHOW_IMPLICIT);
            }
        });
        etDaemonAddress.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if ((event != null && (event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) || (actionId == EditorInfo.IME_ACTION_DONE)) {
                    getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
                    return false;
                }
                return false;
            }
        });

        ToggleButton tbMainNet = (ToggleButton) findViewById(R.id.tbMainNet);
        tbMainNet.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean mainnet = ((ToggleButton) v).isChecked();  // current state
                savePrefs(true); // use previous state as we just clicked it
                if (mainnet) {
                    setDaemon(daemonMainNet);
                } else {
                    setDaemon(daemonTestNet);
                }
                filterList();
                ((BaseAdapter) listView.getAdapter()).notifyDataSetChanged();
            }
        });

        loadPrefs();

        filterList();

        listView = (ListView) findViewById(R.id.list);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, android.R.id.text1, this.displayedList);

        listView.setAdapter(adapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                EditText tvDaemonAddress = (EditText) findViewById(R.id.etDaemonAddress);
                if (tvDaemonAddress.getText().toString().length() == 0) {
                    Toast.makeText(LoginActivity.this, getString(R.string.prompt_daemon_missing), Toast.LENGTH_LONG).show();
                    return;
                }

                String itemValue = (String) listView.getItemAtPosition(position);
                if ((isMainNet() && itemValue.charAt(1) != '4')
                        || (!isMainNet() && itemValue.charAt(1) != '9')) {
                    Toast.makeText(LoginActivity.this, getString(R.string.prompt_wrong_net), Toast.LENGTH_LONG).show();
                    return;
                }

                final int preambleLength = "[123456] ".length();
                if (itemValue.length() <= (preambleLength)) {
                    Toast.makeText(LoginActivity.this, getString(R.string.panic), Toast.LENGTH_LONG).show();
                    return;
                }
                setWalletDaemon();
                if (!checkWalletDaemon()) {
                    Toast.makeText(LoginActivity.this, getString(R.string.warn_daemon_unavailable), Toast.LENGTH_LONG).show();
                    return;
                }

                // looking good
                savePrefs(false);

                String wallet = itemValue.substring(preambleLength);
                promptPassword(wallet);
            }
        });
        if (Helper.getWritePermission(this)) {
            new LoadListTask().execute();
        } else {
            Log.i(TAG, "Waiting for permissions");
        }
    }

    // adapted from http://www.mkyong.com/android/android-prompt-user-input-dialog-example/
    void promptPassword(final String wallet) {
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
                                getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
                                String pass = etPassword.getText().toString();
                                processPasswordEntry(wallet, pass);
                            }
                        })
                .setNegativeButton("Cancel",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
                                dialog.cancel();
                            }
                        });

        final AlertDialog alertDialog = alertDialogBuilder.create();
        // request keyboard
        alertDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        // accept keyboard "ok"
        etPassword.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if ((event != null && (event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) || (actionId == EditorInfo.IME_ACTION_DONE)) {
                    getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
                    String pass = etPassword.getText().toString();
                    alertDialog.cancel();
                    processPasswordEntry(wallet, pass);
                    return false;
                }
                return false;
            }
        });

        alertDialog.show();
    }

    private void processPasswordEntry(String walletName, String pass) {
        if (checkWalletPassword(walletName, pass)) {
            startWallet(walletName, pass);
        } else {
            Toast.makeText(this, getString(R.string.bad_password), Toast.LENGTH_SHORT).show();
        }
    }

    private boolean checkWalletDaemon() {
//        if (android.os.Build.VERSION.SDK_INT > 9) {
        StrictMode.ThreadPolicy prevPolicy = StrictMode.getThreadPolicy();
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder(prevPolicy).permitNetwork().build();
        StrictMode.setThreadPolicy(policy);
        String d[] = WalletManager.getInstance().getDaemonAddress().split(":");
        String host = d[0];
        int port = Integer.parseInt(d[1]);
        Socket socket = new Socket();
        long a = new Date().getTime();
        try {
            socket.connect(new InetSocketAddress(host, port), DAEMON_TIMEOUT);
            socket.close();
        } catch (IOException ex) {
            Log.d(TAG, "Cannot reach daemon " + host + ":" + port + " because " + ex.getLocalizedMessage());
            return false;
        } finally {
            StrictMode.setThreadPolicy(prevPolicy);
        }
        long b = new Date().getTime();
        Log.d(TAG, "Daemon is " + (b - a) + "ms away.");
        int version = WalletManager.getInstance().getDaemonVersion();
        Log.d(TAG, "Daemon is v" + version);
        return (version >= MIN_DAEMON_VERSION);
    }

    private boolean checkWalletPassword(String walletName, String password) {
        String walletPath = new File(Helper.getStorageRoot(getApplicationContext()),
                walletName + ".keys").getAbsolutePath();
        // only test view key
        return WalletManager.getInstance().verifyWalletPassword(walletPath, password, true);
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "onPause()");
        savePrefs(false);
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume()");
    }

    boolean isMainNet() {
        ToggleButton tbMainNet = (ToggleButton) findViewById(R.id.tbMainNet);
        return tbMainNet.isChecked();
    }

    void setMainNet(boolean mainnet) {
        ToggleButton tbMainNet = (ToggleButton) findViewById(R.id.tbMainNet);
        tbMainNet.setChecked(mainnet);
    }

    String getDaemon() {
        EditText tvDaemonAddress = (EditText) findViewById(R.id.etDaemonAddress);
        return tvDaemonAddress.getText().toString();
    }

    void setDaemon(String address) {
        EditText tvDaemonAddress = (EditText) findViewById(R.id.etDaemonAddress);
        tvDaemonAddress.setText(address);
    }

    private static final String PREF_DAEMON_TESTNET = "daemon_testnet";
    private static final String PREF_DAEMON_MAINNET = "daemon_mainnet";
    private static final String PREF_MAINNET = "mainnet";

    private String daemonTestNet;
    private String daemonMainNet;

    void loadPrefs() {
        SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);

        boolean mainnet = sharedPref.getBoolean(PREF_MAINNET, false);
        daemonMainNet = sharedPref.getString(PREF_DAEMON_MAINNET, "localhost:18081");
        daemonTestNet = sharedPref.getString(PREF_DAEMON_TESTNET, "localhost:28081");

        setMainNet(mainnet);
        if (mainnet) {
            setDaemon(daemonMainNet);
        } else {
            setDaemon(daemonTestNet);
        }
    }

    void savePrefs(boolean usePreviousState) {
        // save the daemon address for the net
        boolean mainnet = isMainNet() ^ usePreviousState;
        String daemon = getDaemon();
        if (mainnet) {
            daemonMainNet = daemon;
        } else {
            daemonTestNet = daemon;
        }

        SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putBoolean(PREF_MAINNET, mainnet);
        editor.putString(PREF_DAEMON_MAINNET, daemonMainNet);
        editor.putString(PREF_DAEMON_TESTNET, daemonTestNet);
        editor.apply();
    }

    private void setWalletDaemon() {
        boolean testnet = !isMainNet();
        String daemon = getDaemon();

        if (!daemon.contains(":")) {
            daemon = daemon + (testnet ? ":28081" : ":18081");
        }

        WalletManager.getInstance().setDaemon(daemon, testnet);
    }

    void startWallet(String walletName, String walletPassword) {
        Log.d(TAG, "startWallet()");
        Intent intent = new Intent(getApplicationContext(), WalletActivity.class);
        intent.putExtra(WalletActivity.REQUEST_ID, walletName);
        intent.putExtra(WalletActivity.REQUEST_PW, walletPassword);
        startActivity(intent);
    }

    private void filterList() {
        displayedList.clear();
        char x = isMainNet() ? '4' : '9';
        for (String s : walletList) {
            if (s.charAt(1) == x) displayedList.add(s);
        }
    }

    private class LoadListTask extends AsyncTask<String, Void, Integer> {
        protected void onPreExecute() {
            //Toast.makeText(LoginActivity.this, getString(R.string.status_walletlist_loading), Toast.LENGTH_LONG).show();
        }

        protected Integer doInBackground(String... params) {
            WalletManager mgr = WalletManager.getInstance();
            List<WalletManager.WalletInfo> walletInfos =
                    mgr.findWallets(Helper.getStorageRoot(getApplicationContext()));

            walletList.clear();
            for (WalletManager.WalletInfo walletInfo : walletInfos) {
                Log.d(TAG, walletInfo.address);
                String displayAddress = walletInfo.address;
                if (displayAddress.length() == 95) {
                    displayAddress = walletInfo.address.substring(0, 6);
                }
                walletList.add("[" + displayAddress + "] " + walletInfo.name);
            }
            return 0;
        }

        protected void onPostExecute(Integer result) {
            if (result == 0) {
                filterList();
                ((BaseAdapter) listView.getAdapter()).notifyDataSetChanged();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        Log.d(TAG, "onRequestPermissionsResult()");
        switch (requestCode) {
            case Helper.PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE:
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    new LoadListTask().execute();
                } else {
                    String msg = getString(R.string.message_strorage_not_permitted);
                    Log.e(TAG, msg);
                    throw new IllegalStateException(msg);
                }
                break;
            default:
        }
    }
}

