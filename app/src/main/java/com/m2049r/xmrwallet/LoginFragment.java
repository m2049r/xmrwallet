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
import android.view.inputmethod.EditorInfo;
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
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class LoginFragment extends Fragment {
    private static final String TAG = "LoginFragment";
    public static final String WALLETNAME_PREAMBLE = "[------] ";
    public static final int WALLETNAME_PREAMBLE_LENGTH = WALLETNAME_PREAMBLE.length();


    ListView listView;
    Set<String> walletList = new TreeSet<>(new Comparator<String>() {
        @Override
        public int compare(String o1, String o2) {
            return o1.substring(WALLETNAME_PREAMBLE_LENGTH).toLowerCase()
                    .compareTo(o2.substring(WALLETNAME_PREAMBLE_LENGTH).toLowerCase());
        }
    });
    List<String> displayedList = new ArrayList<>();

    ToggleButton tbMainNet;
    EditText etDaemonAddress;

    Listener activityCallback;

    // Container Activity must implement this interface
    public interface Listener {
        SharedPreferences getPrefs();

        File getStorageRoot();

        void onWalletSelected(final String wallet);

        void onWalletDetails(final String wallet);

        void setTitle(String title);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof Listener) {
            this.activityCallback = (Listener) context;
        } else {
            throw new ClassCastException(context.toString()
                    + " must implement Listener");
        }
    }

    @Override
    public void onPause() {
        Log.d(TAG, "onPause()");
        savePrefs();
        super.onPause();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.login_fragment, container, false);

        tbMainNet = (ToggleButton) view.findViewById(R.id.tbMainNet);
        etDaemonAddress = (EditText) view.findViewById(R.id.etDaemonAddress);

        Helper.hideKeyboard(getActivity());

        etDaemonAddress.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Helper.showKeyboard(getActivity());
            }
        });
        etDaemonAddress.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if ((event != null && (event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) || (actionId == EditorInfo.IME_ACTION_DONE)) {
                    Helper.hideKeyboard(getActivity());
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
                activityCallback.setTitle(getString(R.string.app_name) + " " +
                        getString(mainnet ? R.string.connect_mainnet : R.string.connect_testnet));
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
                    Toast.makeText(getActivity(), getString(R.string.prompt_daemon_missing), Toast.LENGTH_SHORT).show();
                    tvDaemonAddress.requestFocus();
                    Helper.showKeyboard(getActivity());
                    return;
                }

                String itemValue = (String) listView.getItemAtPosition(position);

                if (itemValue.length() <= (WALLETNAME_PREAMBLE_LENGTH)) {
                    Toast.makeText(getActivity(), getString(R.string.panic), Toast.LENGTH_LONG).show();
                    return;
                }

                String x = isMainNet() ? "4-" : "9A-";
                if (x.indexOf(itemValue.charAt(1)) < 0) {
                    Toast.makeText(getActivity(), getString(R.string.prompt_wrong_net), Toast.LENGTH_LONG).show();
                    return;
                }

                if (!checkAndSetWalletDaemon(getDaemon(), !isMainNet())) {
                    Toast.makeText(getActivity(), getString(R.string.warn_daemon_unavailable), Toast.LENGTH_SHORT).show();
                    return;
                }

                // looking good
                savePrefs();

                String wallet = itemValue.substring(WALLETNAME_PREAMBLE_LENGTH);
                if (itemValue.charAt(1) == '-') wallet = ':' + wallet;
                activityCallback.onWalletSelected(wallet);
            }
        });

        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                // Difference to opening wallet is that we don't need a daemon set
                String itemValue = (String) listView.getItemAtPosition(position);

                if (itemValue.length() <= (WALLETNAME_PREAMBLE_LENGTH)) {
                    Toast.makeText(getActivity(), getString(R.string.panic), Toast.LENGTH_LONG).show();
                    return true;
                }

                String wallet = itemValue.substring(WALLETNAME_PREAMBLE_LENGTH);
                if (itemValue.charAt(1) == '-') {
                    Toast.makeText(getActivity(), getString(R.string.bad_wallet), Toast.LENGTH_LONG).show();
                    return true;
                }

                String x = isMainNet() ? "4" : "9A";
                if (x.indexOf(itemValue.charAt(1)) < 0) {
                    Toast.makeText(getActivity(), getString(R.string.prompt_wrong_net), Toast.LENGTH_LONG).show();
                    return true;
                }

                if (!checkAndSetWalletDaemon("", !isMainNet())) {
                    Toast.makeText(getActivity(), getString(R.string.warn_daemon_unavailable), Toast.LENGTH_SHORT).show();
                    return true;
                }

                activityCallback.onWalletDetails(wallet);
                return true;
            }
        });

        activityCallback.setTitle(getString(R.string.app_name) + " " +
                getString(isMainNet() ? R.string.connect_mainnet : R.string.connect_testnet));

        loadList();
        return view;
    }

    private void filterList() {
        displayedList.clear();
        String x = isMainNet() ? "4" : "9A";
        for (String s : walletList) {
            // Log.d(TAG, "filtering " + s);
            if (x.indexOf(s.charAt(1)) >= 0) displayedList.add(s);
        }
        displayedList.add(WALLETNAME_PREAMBLE + getString(R.string.generate_title));
    }

    private void loadList() {
        WalletManager mgr = WalletManager.getInstance();
        List<WalletManager.WalletInfo> walletInfos =
                mgr.findWallets(activityCallback.getStorageRoot());

        walletList.clear();
        for (WalletManager.WalletInfo walletInfo : walletInfos) {
            // Log.d(TAG, walletInfo.address);
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
        daemonMainNet = sharedPref.getString(PREF_DAEMON_MAINNET, "");
        daemonTestNet = sharedPref.getString(PREF_DAEMON_TESTNET, "");

        setMainNet(mainnet);
        if (mainnet) {
            setDaemon(daemonMainNet);
        } else {
            setDaemon(daemonTestNet);
        }
    }

    void savePrefs() {
        savePrefs(false);
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
        if (!daemonAddress.isEmpty()) {
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
        }
        WalletManager mgr = WalletManager.getInstance();
        mgr.setDaemon(daemonAddress, testnet);
        if (!daemonAddress.isEmpty()) {
            int version = mgr.getDaemonVersion();
            Log.d(TAG, "Daemon is v" + version);
            return (version >= WalletActivity.MIN_DAEMON_VERSION);
        } else {
            return true;
        }
    }
}
