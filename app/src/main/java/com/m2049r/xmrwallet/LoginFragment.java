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
import android.support.v7.widget.RecyclerView;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.m2049r.xmrwallet.layout.WalletInfoAdapter;
import com.m2049r.xmrwallet.model.WalletManager;
import com.m2049r.xmrwallet.util.Helper;
import com.m2049r.xmrwallet.util.NodeList;
import com.m2049r.xmrwallet.widget.DropDownEditText;
import com.m2049r.xmrwallet.widget.Toolbar;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;

public class LoginFragment extends Fragment implements WalletInfoAdapter.OnInteractionListener,
        View.OnClickListener {

    private WalletInfoAdapter adapter;

    List<WalletManager.WalletInfo> walletList = new ArrayList<>();
    List<WalletManager.WalletInfo> displayedList = new ArrayList<>();

    EditText etDummy;
    ImageView ivGunther;
    DropDownEditText etDaemonAddress;
    ArrayAdapter<String> nodeAdapter;

    Listener activityCallback;

    // Container Activity must implement this interface
    public interface Listener {
        SharedPreferences getPrefs();

        File getStorageRoot();

        boolean onWalletSelected(String wallet, String daemon, boolean testnet);

        void onWalletDetails(String wallet, boolean testnet);

        void onWalletReceive(String wallet, boolean testnet);

        void onWalletRename(String name);

        void onWalletBackup(String name);

        void onWalletArchive(String walletName);

        void onAddWallet(boolean testnet, String type);

        void showNet(boolean testnet);

        void setToolbarButton(int type);

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
        Timber.d("onPause()");
        savePrefs();
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        Timber.d("onResume()");
        activityCallback.setTitle(null);
        activityCallback.setToolbarButton(Toolbar.BUTTON_DONATE);
        activityCallback.showNet(isTestnet());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Timber.d("onCreateView");
        View view = inflater.inflate(R.layout.fragment_login, container, false);

        ivGunther = (ImageView) view.findViewById(R.id.ivGunther);
        fabScreen = (FrameLayout) view.findViewById(R.id.fabScreen);
        fab = (FloatingActionButton) view.findViewById(R.id.fab);
        fabNew = (FloatingActionButton) view.findViewById(R.id.fabNew);
        fabView = (FloatingActionButton) view.findViewById(R.id.fabView);
        fabKey = (FloatingActionButton) view.findViewById(R.id.fabKey);
        fabSeed = (FloatingActionButton) view.findViewById(R.id.fabSeed);

        fabNewL = (RelativeLayout) view.findViewById(R.id.fabNewL);
        fabViewL = (RelativeLayout) view.findViewById(R.id.fabViewL);
        fabKeyL = (RelativeLayout) view.findViewById(R.id.fabKeyL);
        fabSeedL = (RelativeLayout) view.findViewById(R.id.fabSeedL);

        fab_pulse = AnimationUtils.loadAnimation(getContext(), R.anim.fab_pulse);
        fab_open_screen = AnimationUtils.loadAnimation(getContext(), R.anim.fab_open_screen);
        fab_close_screen = AnimationUtils.loadAnimation(getContext(), R.anim.fab_close_screen);
        fab_open = AnimationUtils.loadAnimation(getContext(), R.anim.fab_open);
        fab_close = AnimationUtils.loadAnimation(getContext(), R.anim.fab_close);
        rotate_forward = AnimationUtils.loadAnimation(getContext(), R.anim.rotate_forward);
        rotate_backward = AnimationUtils.loadAnimation(getContext(), R.anim.rotate_backward);
        fab.setOnClickListener(this);
        fabNew.setOnClickListener(this);
        fabView.setOnClickListener(this);
        fabKey.setOnClickListener(this);
        fabSeed.setOnClickListener(this);
        fabScreen.setOnClickListener(this);

        RecyclerView recyclerView = (RecyclerView) view.findViewById(R.id.list);
        registerForContextMenu(recyclerView);
        this.adapter = new WalletInfoAdapter(getActivity(), this);
        recyclerView.setAdapter(adapter);

        etDummy = (EditText) view.findViewById(R.id.etDummy);
        etDaemonAddress = (DropDownEditText) view.findViewById(R.id.etDaemonAddress);
        nodeAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_dropdown_item_1line);
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

        etDaemonAddress.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View arg1, int pos, long id) {
                Helper.hideKeyboard(getActivity());
                etDummy.requestFocus();

            }
        });

        loadPrefs();

        return view;
    }

    // Callbacks from WalletInfoAdapter
    @Override
    public void onInteraction(final View view, final WalletManager.WalletInfo infoItem) {
        String x = isTestnet() ? "9A-" : "4-";
        if (x.indexOf(infoItem.address.charAt(0)) < 0) {
            Toast.makeText(getActivity(), getString(R.string.prompt_wrong_net), Toast.LENGTH_LONG).show();
            return;
        }

        if (activityCallback.onWalletSelected(infoItem.name, getDaemon(), isTestnet())) {
            savePrefs();
        }
    }

    @Override
    public boolean onContextInteraction(MenuItem item, WalletManager.WalletInfo listItem) {
        switch (item.getItemId()) {
            case R.id.action_info:
                showInfo(listItem.name);
                break;
            case R.id.action_receive:
                showReceive(listItem.name);
                break;
            case R.id.action_rename:
                activityCallback.onWalletRename(listItem.name);
                break;
            case R.id.action_backup:
                activityCallback.onWalletBackup(listItem.name);
                break;
            case R.id.action_archive:
                activityCallback.onWalletArchive(listItem.name);
                break;
            default:
                return super.onContextItemSelected(item);
        }
        return true;
    }

    private void filterList() {
        displayedList.clear();
        String x = isTestnet() ? "9A" : "4";
        for (WalletManager.WalletInfo s : walletList) {
            if (x.indexOf(s.address.charAt(0)) >= 0) displayedList.add(s);
        }
    }

    public void loadList() {
        Timber.d("loadList()");
        WalletManager mgr = WalletManager.getInstance();
        List<WalletManager.WalletInfo> walletInfos =
                mgr.findWallets(activityCallback.getStorageRoot());
        walletList.clear();
        walletList.addAll(walletInfos);
        filterList();
        adapter.setInfos(displayedList);
        adapter.notifyDataSetChanged();

        // deal with Gunther & FAB animation
        if (displayedList.isEmpty()) {
            fab.startAnimation(fab_pulse);
            if (ivGunther.getDrawable() == null) {
                ivGunther.setImageResource(R.drawable.gunther_desaturated);
            }
        } else {
            fab.clearAnimation();
            if (ivGunther.getDrawable() != null) {
                ivGunther.setImageDrawable(null);
            }
        }
    }

    private void showInfo(@NonNull String name) {
        activityCallback.onWalletDetails(name, isTestnet());
    }

    private boolean showReceive(@NonNull String name) {
        activityCallback.onWalletReceive(name, isTestnet());
        return true;
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
        boolean lastState = testnet;
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

    private static final String PREF_DAEMONLIST_MAINNET =
            "node.moneroworld.com:18089;node.xmrbackb.one;node.xmr.be";

    private static final String PREF_DAEMONLIST_TESTNET =
            "testnet.xmrchain.net";

    private NodeList daemonTestNet;
    private NodeList daemonMainNet;

    void loadPrefs() {
        SharedPreferences sharedPref = activityCallback.getPrefs();

        daemonMainNet = new NodeList(sharedPref.getString(PREF_DAEMON_MAINNET, PREF_DAEMONLIST_MAINNET));
        daemonTestNet = new NodeList(sharedPref.getString(PREF_DAEMON_TESTNET, PREF_DAEMONLIST_TESTNET));
        setNet(isTestnet(), false);
    }

    void savePrefs() {
        savePrefs(false);
    }

    void savePrefs(boolean usePreviousState) {
        Timber.d("SAVE / %s", usePreviousState);
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
        editor.putString(PREF_DAEMON_MAINNET, daemonMainNet.toString());
        editor.putString(PREF_DAEMON_TESTNET, daemonTestNet.toString());
        editor.apply();
    }

    String getDaemon() {
        return etDaemonAddress.getText().toString().trim();
    }

    void setDaemon(NodeList nodeList) {
        Timber.d("setDaemon() %s", nodeList.toString());
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

    private boolean isFabOpen = false;
    private FloatingActionButton fab, fabNew, fabView, fabKey, fabSeed;
    private FrameLayout fabScreen;
    private RelativeLayout fabNewL, fabViewL, fabKeyL, fabSeedL;
    private Animation fab_open, fab_close, rotate_forward, rotate_backward, fab_open_screen, fab_close_screen;
    private Animation fab_pulse;

    public boolean isFabOpen() {
        return isFabOpen;
    }

    public void animateFAB() {
        if (isFabOpen) {
            fabScreen.setVisibility(View.INVISIBLE);
            fabScreen.setClickable(false);
            fabScreen.startAnimation(fab_close_screen);
            fab.startAnimation(rotate_backward);
            fabNewL.startAnimation(fab_close);
            fabNew.setClickable(false);
            fabViewL.startAnimation(fab_close);
            fabView.setClickable(false);
            fabKeyL.startAnimation(fab_close);
            fabKey.setClickable(false);
            fabSeedL.startAnimation(fab_close);
            fabSeed.setClickable(false);
            isFabOpen = false;
        } else {
            fabScreen.setClickable(true);
            fabScreen.startAnimation(fab_open_screen);
            fab.startAnimation(rotate_forward);
            fabNewL.startAnimation(fab_open);
            fabNew.setClickable(true);
            fabViewL.startAnimation(fab_open);
            fabView.setClickable(true);
            fabKeyL.startAnimation(fab_open);
            fabKey.setClickable(true);
            fabSeedL.startAnimation(fab_open);
            fabSeed.setClickable(true);
            isFabOpen = true;
        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        switch (id) {
            case R.id.fab:
                animateFAB();
                break;
            case R.id.fabNew:
                fabScreen.setVisibility(View.INVISIBLE);
                isFabOpen = false;
                activityCallback.onAddWallet(isTestnet(), GenerateFragment.TYPE_NEW);
                break;
            case R.id.fabView:
                animateFAB();
                activityCallback.onAddWallet(isTestnet(), GenerateFragment.TYPE_VIEWONLY);
                break;
            case R.id.fabKey:
                animateFAB();
                activityCallback.onAddWallet(isTestnet(), GenerateFragment.TYPE_KEY);
                break;
            case R.id.fabSeed:
                animateFAB();
                activityCallback.onAddWallet(isTestnet(), GenerateFragment.TYPE_SEED);
                break;
            case R.id.fabScreen:
                animateFAB();
                break;
        }
    }
}
