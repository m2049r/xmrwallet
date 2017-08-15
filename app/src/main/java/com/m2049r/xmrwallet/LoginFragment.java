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

import android.app.Fragment;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.StrictMode;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class LoginFragment extends Fragment {
    private static final String TAG = "LoginFragment";

    ListView listView;
    List<String> walletList = new ArrayList<>();
    List<String> displayedList = new ArrayList<>();

    ToggleButton tbMainNet;
    EditText etDaemonAddress;

    LoginFragment.LoginFragmentListener activityCallback;

    // Container Activity must implement this interface
    public interface LoginFragmentListener {
        SharedPreferences getPrefs();

        File getStorageRoot();

        void promptPassword(final String wallet);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof LoginFragment.LoginFragmentListener) {
            this.activityCallback = (LoginFragment.LoginFragmentListener) context;
        } else {
            throw new ClassCastException(context.toString()
                    + " must implement WalletFragmentListener");
        }
    }

    @Override
    public void onPause() {
        Log.d(TAG, "onPause()");
        savePrefs(false);
        super.onPause();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.login_fragment, container, false);

        tbMainNet = (ToggleButton) view.findViewById(R.id.tbMainNet);
        etDaemonAddress = (EditText) view.findViewById(R.id.etDaemonAddress);

        getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);

        etDaemonAddress.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.showSoftInput(etDaemonAddress, InputMethodManager.SHOW_IMPLICIT);
            }
        });
        etDaemonAddress.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if ((event != null && (event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) || (actionId == EditorInfo.IME_ACTION_DONE)) {
                    getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
                    return false;
                }
                return false;
            }
        });

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

        listView = (ListView) view.findViewById(R.id.list);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getActivity(),
                android.R.layout.simple_list_item_1, android.R.id.text1, this.displayedList);

        listView.setAdapter(adapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                EditText tvDaemonAddress = (EditText) getView().findViewById(R.id.etDaemonAddress);
                if (tvDaemonAddress.getText().toString().length() == 0) {
                    Toast.makeText(getActivity(), getString(R.string.prompt_daemon_missing), Toast.LENGTH_LONG).show();
                    return;
                }

                String itemValue = (String) listView.getItemAtPosition(position);
                if ((isMainNet() && itemValue.charAt(1) != '4')
                        || (!isMainNet() && itemValue.charAt(1) != '9')) {
                    Toast.makeText(getActivity(), getString(R.string.prompt_wrong_net), Toast.LENGTH_LONG).show();
                    return;
                }

                final int preambleLength = "[123456] ".length();
                if (itemValue.length() <= (preambleLength)) {
                    Toast.makeText(getActivity(), getString(R.string.panic), Toast.LENGTH_LONG).show();
                    return;
                }
                if (!checkAndSetWalletDaemon(getDaemon(), !isMainNet())) {
                    Toast.makeText(getActivity(), getString(R.string.warn_daemon_unavailable), Toast.LENGTH_LONG).show();
                    return;
                }

                // looking good
                savePrefs(false);

                String wallet = itemValue.substring(preambleLength);
                activityCallback.promptPassword(wallet);
            }
        });
        loadList();
        return view;
    }

    private void filterList() {
        displayedList.clear();
        char x = isMainNet() ? '4' : '9';
        for (String s : walletList) {
            if (s.charAt(1) == x) displayedList.add(s);
        }
    }

    private void loadList() {
        WalletManager mgr = WalletManager.getInstance();
        List<WalletManager.WalletInfo> walletInfos =
                mgr.findWallets(activityCallback.getStorageRoot());

        walletList.clear();
        for (WalletManager.WalletInfo walletInfo : walletInfos) {
            Log.d(TAG, walletInfo.address);
            String displayAddress = walletInfo.address;
            if (displayAddress.length() == 95) {
                displayAddress = walletInfo.address.substring(0, 6);
            }
            walletList.add("[" + displayAddress + "] " + walletInfo.name);
        }
        filterList();
        ((BaseAdapter) listView.getAdapter()).notifyDataSetChanged();

    }


    boolean isMainNet() {
        return tbMainNet.isChecked();
    }

    void setMainNet(boolean mainnet) {
        tbMainNet.setChecked(mainnet);
    }

    String getDaemon() {
        return etDaemonAddress.getText().toString();
    }

    void setDaemon(String address) {
        etDaemonAddress.setText(address);
    }

    private static final String PREF_DAEMON_TESTNET = "daemon_testnet";
    private static final String PREF_DAEMON_MAINNET = "daemon_mainnet";
    private static final String PREF_MAINNET = "mainnet";

    private String daemonTestNet;
    private String daemonMainNet;

    void loadPrefs() {
        SharedPreferences sharedPref = activityCallback.getPrefs();

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

        SharedPreferences sharedPref = activityCallback.getPrefs();
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putBoolean(PREF_MAINNET, mainnet);
        editor.putString(PREF_DAEMON_MAINNET, daemonMainNet);
        editor.putString(PREF_DAEMON_TESTNET, daemonTestNet);
        editor.apply();
    }

    private boolean checkAndSetWalletDaemon(String daemonAddress, boolean testnet) {
        String d[] = daemonAddress.split(":");
        if (d.length > 2) return false;
        if (d.length < 1) return false;
        String host = d[0];
        int port;
        if (d.length == 2) {
            try {
                port = Integer.parseInt(d[1]);
            } catch (NumberFormatException ex) {
                return false;
            }
        } else {
            port = (testnet ? 28081 : 18081);
        }
//        if (android.os.Build.VERSION.SDK_INT > 9) {
        StrictMode.ThreadPolicy prevPolicy = StrictMode.getThreadPolicy();
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder(prevPolicy).permitNetwork().build();
        StrictMode.setThreadPolicy(policy);
        Socket socket = new Socket();
        long a = new Date().getTime();
        try {
            socket.connect(new InetSocketAddress(host, port), LoginActivity.DAEMON_TIMEOUT);
            socket.close();
        } catch (IOException ex) {
            Log.d(TAG, "Cannot reach daemon " + host + ":" + port + " because " + ex.getLocalizedMessage());
            return false;
        } finally {
            StrictMode.setThreadPolicy(prevPolicy);
        }
        long b = new Date().getTime();
        Log.d(TAG, "Daemon is " + (b - a) + "ms away.");

        WalletManager mgr = WalletManager.getInstance();
        mgr.setDaemon(daemonAddress, testnet);
        int version = mgr.getDaemonVersion();
        Log.d(TAG, "Daemon is v" + version);
        return (version >= WalletActivity.MIN_DAEMON_VERSION);
    }
}
