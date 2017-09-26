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
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
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

import com.m2049r.xmrwallet.layout.DropDownEditText;
import com.m2049r.xmrwallet.model.WalletManager;
import com.m2049r.xmrwallet.util.Helper;
import com.m2049r.xmrwallet.util.NodeList;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
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

    EditText etDummy;
    DropDownEditText etDaemonAddress;
    ArrayAdapter<String> nodeAdapter;
    FloatingActionButton fabAdd;

    Listener activityCallback;

    // Container Activity must implement this interface
    public interface Listener {
        SharedPreferences getPrefs();

        File getStorageRoot();

        boolean onWalletSelected(String daemon, String wallet, boolean testnet);

        void onWalletDetails(String wallet, boolean testnet);

        void onWalletReceive(String wallet, boolean testnet);

        void onWalletRename(String name);

        void onWalletBackup(String name);

        void onWalletArchive(String walletName);

        void onAddWallet(boolean testnet);

        void showNet(boolean testnet);
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
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView");
        View view = inflater.inflate(R.layout.login_fragment, container, false);

        fabAdd = (FloatingActionButton) view.findViewById(R.id.fabAdd);
        fabAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                activityCallback.onAddWallet(isTestnet());
            }
        });

        listView = (ListView) view.findViewById(R.id.list);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getActivity(),
                android.R.layout.simple_list_item_1, android.R.id.text1, this.displayedList);
        listView.setAdapter(adapter);
        registerForContextMenu(listView);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String itemValue = (String) listView.getItemAtPosition(position);

                if (itemValue.length() <= (WALLETNAME_PREAMBLE_LENGTH)) {
                    Toast.makeText(getActivity(), getString(R.string.panic), Toast.LENGTH_LONG).show();
                    return;
                }

                String x = isTestnet() ? "9A-" : "4-";
                if (x.indexOf(itemValue.charAt(1)) < 0) {
                    Toast.makeText(getActivity(), getString(R.string.prompt_wrong_net), Toast.LENGTH_LONG).show();
                    return;
                }

                String wallet = itemValue.substring(WALLETNAME_PREAMBLE_LENGTH);

                if (activityCallback.onWalletSelected(getDaemon(), wallet, isTestnet())) {
                    savePrefs();
                }
            }
        });

        etDummy = (EditText) view.findViewById(R.id.etDummy);
        etDaemonAddress = (DropDownEditText) view.findViewById(R.id.etDaemonAddress);
        nodeAdapter = new ArrayAdapter(getContext(), android.R.layout.simple_dropdown_item_1line);
        etDaemonAddress.setAdapter(nodeAdapter);

        Helper.hideKeyboard(getActivity());

        etDaemonAddress.setThreshold(0);
        etDaemonAddress.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                etDaemonAddress.showDropDown();
                Helper.showKeyboard(getActivity());
            }
        });

        etDaemonAddress.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus && !getActivity().isFinishing() && etDaemonAddress.isLaidOut()) {
                    etDaemonAddress.showDropDown();
                    Helper.showKeyboard(getActivity());
                }
            }
        });

        etDaemonAddress.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if ((event != null && (event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) || (actionId == EditorInfo.IME_ACTION_DONE)) {
                    Helper.hideKeyboard(getActivity());
                    etDummy.requestFocus();
                    return true;
                }
                return false;
            }
        });

        loadPrefs();
        return view;
    }

    private void filterList() {
        displayedList.clear();
        String x = isTestnet() ? "9A" : "4";
        for (String s : walletList) {
            if (x.indexOf(s.charAt(1)) >= 0) displayedList.add(s);
        }
    }

    public void loadList() {
        Log.d(TAG, "loadList()");
        // TODO this should probably be in LoginActivity
        WalletManager mgr = WalletManager.getInstance();
        List<WalletManager.WalletInfo> walletInfos =
                mgr.findWallets(activityCallback.getStorageRoot());

        walletList.clear();
        for (WalletManager.WalletInfo walletInfo : walletInfos) {
            // ONCE the walletInfo.address was null - because the address.txt was empty
            // this was before the wallet generation was in its own therad with huge stack
            // TODO: keep an eye on Wallet.getAddress() returning empty
            String displayAddress = walletInfo.address;
            if ((displayAddress != null) && displayAddress.length() == 95) {
                displayAddress = walletInfo.address.substring(0, 6);
                walletList.add("[" + displayAddress + "] " + walletInfo.name);
            }
        }
        filterList();
        ((BaseAdapter) listView.getAdapter()).notifyDataSetChanged();

    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getActivity().getMenuInflater();
        inflater.inflate(R.menu.list_context_menu, menu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        String listItem = (String) listView.getItemAtPosition(info.position);
        String name = nameFromListItem(listItem, isTestnet());
        if (name == null) {
            Toast.makeText(getActivity(), getString(R.string.panic), Toast.LENGTH_LONG).show();
        }
        switch (item.getItemId()) {
            case R.id.action_info:
                showInfo(name);
                break;
            case R.id.action_receive:
                showReceive(name);
                break;
            case R.id.action_rename:
                activityCallback.onWalletRename(name);
                break;
            case R.id.action_backup:
                activityCallback.onWalletBackup(name);
                break;
            case R.id.action_archive:
                activityCallback.onWalletArchive(name);
                break;
            default:
                return super.onContextItemSelected(item);
        }
        return true;
    }

    private void showInfo(@NonNull String name) {
        activityCallback.onWalletDetails(name, isTestnet());
    }

    private boolean showReceive(@NonNull String name) {
        activityCallback.onWalletReceive(name, isTestnet());
        return true;
    }

    private String nameFromListItem(String listItem, boolean testnet) {
        String wallet = listItem.substring(WALLETNAME_PREAMBLE_LENGTH);
        String x = testnet ? "9A" : "4";
        if (x.indexOf(listItem.charAt(1)) < 0) {
            return null;
        }
        return wallet;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.list_menu, menu);
        menu.findItem(R.id.action_testnet).setChecked(isTestnet());
        super.onCreateOptionsMenu(menu, inflater);
    }

    boolean testnet = false;

    boolean isTestnet() {
        return testnet;
    }

    public boolean onTestnetMenuItem() {
        boolean lastState = testnet;//item.isChecked();
        setNet(!lastState, true); // set and save
        return !lastState;
    }

    public void setNet(boolean testnet, boolean save) {
        this.testnet = testnet;
        activityCallback.showNet(testnet);
        if (save) {
            savePrefs(true); // use previous state as we just clicked it
        }
        if (testnet) {
            setDaemon(daemonTestNet);
        } else {
            setDaemon(daemonMainNet);
        }
        loadList();
    }

    private static final String PREF_DAEMON_TESTNET = "daemon_testnet";
    private static final String PREF_DAEMON_MAINNET = "daemon_mainnet";
    //private static final String PREF_TESTNET = "testnet";

    private static final String PREF_DAEMONLIST_MAINNET =
            "node.moneroworld.com:18089;node.xmrbackb.one:18081;node.xmr.be:18081";

    private NodeList daemonTestNet;
    private NodeList daemonMainNet;

    void loadPrefs() {
        SharedPreferences sharedPref = activityCallback.getPrefs();

        daemonMainNet = new NodeList(sharedPref.getString(PREF_DAEMON_MAINNET, PREF_DAEMONLIST_MAINNET));
        daemonTestNet = new NodeList(sharedPref.getString(PREF_DAEMON_TESTNET, ""));
        setNet(isTestnet(), false);
    }

    void savePrefs() {
        savePrefs(false);
    }

    void savePrefs(boolean usePreviousState) {
        Log.d(TAG, "SAVE / " + usePreviousState);
        // save the daemon address for the net
        boolean testnet = isTestnet() ^ usePreviousState;
        String daemon = getDaemon();
        if (testnet) {
            daemonTestNet.setRecent(daemon);
        } else {
            daemonMainNet.setRecent(daemon);
        }

        SharedPreferences sharedPref = activityCallback.getPrefs();
        SharedPreferences.Editor editor = sharedPref.edit();
        //editor.putBoolean(PREF_TESTNET, testnet);
        editor.putString(PREF_DAEMON_MAINNET, daemonMainNet.toString());
        editor.putString(PREF_DAEMON_TESTNET, daemonTestNet.toString());
        editor.apply();
    }

    String getDaemon() {
        return etDaemonAddress.getText().toString().trim();
    }

    void setDaemon(NodeList nodeList) {
        Log.d(TAG, "setDaemon() " + nodeList.toString());
        String[] nodes = nodeList.getNodes().toArray(new String[0]);
        nodeAdapter.clear();
        nodeAdapter.addAll(nodes);
        etDaemonAddress.getText().clear();
        if (nodes.length > 0) {
            etDaemonAddress.setText(nodes[0]);
        }
        etDaemonAddress.dismissDropDown();
        etDummy.requestFocus();
        Helper.hideKeyboard(getActivity());
    }
}
