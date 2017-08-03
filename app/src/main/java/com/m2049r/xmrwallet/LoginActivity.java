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
import java.util.ArrayList;
import java.util.List;

public class LoginActivity extends Activity {
    static final String TAG = "LoginActivity";
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
                Log.d(TAG, "CLICK NET! mainnet=" + mainnet);
                savePrefs(true); // use previous state as we just clicked it
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
                if ((getMainNetSetting() && itemValue.charAt(1) != '4')
                        || (!getMainNetSetting() && itemValue.charAt(1) != '9')) {
                    Toast.makeText(LoginActivity.this, getString(R.string.prompt_wrong_net), Toast.LENGTH_LONG).show();
                    return;
                }

                final int preambleLength = "[123456] ".length();
                if (itemValue.length() <= (preambleLength)) {
                    Toast.makeText(LoginActivity.this, "something's wrong", Toast.LENGTH_LONG).show();
                    return;
                }

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

    boolean getMainNetSetting() {
        ToggleButton tbMainNet = (ToggleButton) findViewById(R.id.tbMainNet);
        return tbMainNet.isChecked();
    }

    private static final String PREF_DAEMON_TESTNET = "daemon_testnet";
    private static final String PREF_DAEMON_MAINNET = "daemon_mainnet";
    private static final String PREF_MAINNET = "mainnet";

    private String daemonTestNet;
    private String daemonMainNet;

    void loadPrefs() {
        SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);

        boolean mainnet = sharedPref.getBoolean(PREF_MAINNET, false);
        ToggleButton tbMainNet = (ToggleButton) findViewById(R.id.tbMainNet);
        tbMainNet.setChecked(mainnet);

        daemonMainNet = sharedPref.getString(PREF_DAEMON_MAINNET, "localhost:18081");
        daemonTestNet = sharedPref.getString(PREF_DAEMON_TESTNET, "localhost:28081");

        EditText tvDaemonAddress = (EditText) findViewById(R.id.etDaemonAddress);
        if (mainnet) {
            tvDaemonAddress.setText(daemonMainNet);
        } else {
            tvDaemonAddress.setText(daemonTestNet);
        }
    }

    void savePrefs(boolean usePreviousState) {
        // save the daemon address for the net
        boolean mainnet = getMainNetSetting() ^ usePreviousState;
        EditText etDaemonAddress = (EditText) findViewById(R.id.etDaemonAddress);
        if (mainnet) {
            daemonMainNet = etDaemonAddress.getText().toString();
            etDaemonAddress.setText(daemonTestNet);
        } else {
            daemonTestNet = etDaemonAddress.getText().toString();
            etDaemonAddress.setText(daemonMainNet);
        }

        SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putBoolean(PREF_MAINNET, mainnet);
        editor.putString(PREF_DAEMON_MAINNET, daemonMainNet);
        editor.putString(PREF_DAEMON_TESTNET, daemonTestNet);
        editor.apply();
    }

    void startWallet(String walletName, String walletPassword) {
        Log.d(TAG, "startWallet()");
        savePrefs(false);
        EditText tvDaemonAddress = (EditText) findViewById(R.id.etDaemonAddress);
        boolean testnet = !getMainNetSetting();
        Intent intent = new Intent(getApplicationContext(), WalletActivity.class);
        String daemon = tvDaemonAddress.getText().toString();
        if (!daemon.contains(":")) {
            daemon = daemon + (testnet ? ":28081" : ":18081");
        }
        intent.putExtra("daemon", daemon);
        intent.putExtra("testnet", testnet);
        intent.putExtra("wallet", walletName);
        intent.putExtra("password", walletPassword);
        startActivity(intent);
    }

    private void filterList() {
        displayedList.clear();
        char x = getMainNetSetting() ? '4' : '9';
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
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[],@NonNull int[] grantResults) {
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

