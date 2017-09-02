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
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
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
    FloatingActionButton fabAdd;

    Listener activityCallback;

    // Container Activity must implement this interface
    public interface Listener {
        SharedPreferences getPrefs();

        File getStorageRoot();

        void onWalletSelected(final String wallet);

        void onWalletDetails(final String wallet);

        void onWalletReceive(final String wallet);

        void onAddWallet();

        void setTitle(String title);

        void setSubtitle(String subtitle);

        void setTestNet(boolean testnet);

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
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume()");
        activityCallback.setTitle(getString(R.string.login_activity_name));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.login_fragment, container, false);

        tbMainNet = (ToggleButton) view.findViewById(R.id.tbMainNet);
        etDaemonAddress = (EditText) view.findViewById(R.id.etDaemonAddress);


        fabAdd = (FloatingActionButton) view.findViewById(R.id.fabAdd);
        fabAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                checkAndSetWalletDaemon("", !isMainNet());
                activityCallback.onAddWallet();
            }
        });

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
                activityCallback.setTestNet(!mainnet);
                savePrefs(true); // use previous state as we just clicked it
                if (mainnet) {
                    setDaemon(daemonMainNet);
                } else {
                    setDaemon(daemonTestNet);
                }
                activityCallback.setSubtitle(getString(mainnet ? R.string.connect_mainnet : R.string.connect_testnet));
                filterList();
                ((BaseAdapter) listView.getAdapter()).notifyDataSetChanged();
            }
        });

        loadPrefs();
        activityCallback.setSubtitle(getString(isMainNet() ? R.string.connect_mainnet : R.string.connect_testnet));

        listView = (ListView) view.findViewById(R.id.list);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getActivity(),
                android.R.layout.simple_list_item_1, android.R.id.text1, this.displayedList);

        listView.setAdapter(adapter);
        registerForContextMenu(listView);

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
                activityCallback.onWalletSelected(wallet);
            }
        });

/*        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                // Difference to opening wallet is that we don't need a daemon set
                String itemValue = (String) listView.getItemAtPosition(position);

                if (itemValue.length() <= (WALLETNAME_PREAMBLE_LENGTH)) {
                    Toast.makeText(getActivity(), getString(R.string.panic), Toast.LENGTH_LONG).show();
                    return true;
                }

                String wallet = itemValue.substring(WALLETNAME_PREAMBLE_LENGTH);
                String x = isMainNet() ? "4" : "9A";
                if (x.indexOf(itemValue.charAt(1)) < 0) {
                    Toast.makeText(getActivity(), getString(R.string.prompt_wrong_net), Toast.LENGTH_LONG).show();
                    return true;
                }

                checkAndSetWalletDaemon("", !isMainNet()); // just set selected net

                activityCallback.onWalletDetails(wallet);
                return true;
            }
        });
*/
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
        activityCallback.setTestNet(!mainnet);

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

    private boolean checkAndSetWalletDaemon(String daemon, boolean testnet) {
        String daemonAddress = "";
        String username = "";
        String password = "";
        if (!daemon.isEmpty()) { // no actual daemon is also fine
            String a[] = daemon.split("@");
            if (a.length == 1) { // no credentials
                daemonAddress = a[0];
            } else if (a.length == 2) { // credentials
                String up[] = a[0].split(":");
                if (up.length != 2) return false;
                username = up[0];
                if (!username.isEmpty()) password = up[1];
                daemonAddress = a[1];
            } else {
                return false;
            }

            String da[] = daemonAddress.split(":");
            if ((da.length > 2) || (da.length < 1)) return false;
            String host = da[0];
            int port;
            if (da.length == 2) {
                try {
                    port = Integer.parseInt(da[1]);
                } catch (NumberFormatException ex) {
                    return false;
                }
            } else {
                port = (testnet ? 28081 : 18081);
                daemonAddress = daemonAddress + ":" + port;
            }
            //Log.d(TAG, "DAEMON " + username + "/" + password + "/" + host + "/" + port);
//        if (android.os.Build.VERSION.SDK_INT > 9) {
            StrictMode.ThreadPolicy prevPolicy = StrictMode.getThreadPolicy();
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder(prevPolicy).permitNetwork().build();
            StrictMode.setThreadPolicy(policy);
            Socket socket = new Socket();
            long timeA = new Date().getTime();
            try {
                socket.connect(new InetSocketAddress(host, port), LoginActivity.DAEMON_TIMEOUT);
                socket.close();
            } catch (IOException ex) {
                Log.d(TAG, "Cannot reach daemon " + host + "/" + port + " because " + ex.getLocalizedMessage());
                return false;
            } finally {
                StrictMode.setThreadPolicy(prevPolicy);
            }
            long timeB = new Date().getTime();
            Log.d(TAG, "Daemon is " + (timeB - timeA) + "ms away.");
        }
        WalletManager mgr = WalletManager.getInstance();
        mgr.setDaemon(daemonAddress, testnet, username, password);
        return true;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getActivity().getMenuInflater();
        inflater.inflate(R.menu.list_menu, menu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        String listItem = (String) listView.getItemAtPosition(info.position);
        switch (item.getItemId()) {
            case R.id.action_info:
                return showInfo(listItem);
            case R.id.action_receive:
                return showReceive(listItem);
            default:
                return super.onContextItemSelected(item);
        }
    }

    private boolean showInfo(String listItem) {
        if (listItem.length() <= (WALLETNAME_PREAMBLE_LENGTH)) {
            Toast.makeText(getActivity(), getString(R.string.panic), Toast.LENGTH_LONG).show();
            return true;
        }

        String wallet = listItem.substring(WALLETNAME_PREAMBLE_LENGTH);
        String x = isMainNet() ? "4" : "9A";
        if (x.indexOf(listItem.charAt(1)) < 0) {
            Toast.makeText(getActivity(), getString(R.string.prompt_wrong_net), Toast.LENGTH_LONG).show();
            return true;
        }

        checkAndSetWalletDaemon("", !isMainNet()); // just set selected net

        activityCallback.onWalletDetails(wallet);
        return true;
    }

    private boolean showReceive(String listItem) {
        if (listItem.length() <= (WALLETNAME_PREAMBLE_LENGTH)) {
            Toast.makeText(getActivity(), getString(R.string.panic), Toast.LENGTH_LONG).show();
            return true;
        }

        String wallet = listItem.substring(WALLETNAME_PREAMBLE_LENGTH);
        String x = isMainNet() ? "4" : "9A";
        if (x.indexOf(listItem.charAt(1)) < 0) {
            Toast.makeText(getActivity(), getString(R.string.prompt_wrong_net), Toast.LENGTH_LONG).show();
            return true;
        }

        checkAndSetWalletDaemon("", !isMainNet()); // just set selected net

        activityCallback.onWalletReceive(wallet);
        return true;
    }
}
