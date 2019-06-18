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
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.m2049r.xmrwallet.data.NodeInfo;
import com.m2049r.xmrwallet.layout.NodeInfoAdapter;
import com.m2049r.xmrwallet.layout.WalletInfoAdapter;
import com.m2049r.xmrwallet.model.WalletManager;
import com.m2049r.xmrwallet.util.Helper;
import com.m2049r.xmrwallet.util.KeyStoreHelper;
import com.m2049r.xmrwallet.util.Notice;
import com.m2049r.xmrwallet.widget.Toolbar;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import timber.log.Timber;

public class LoginFragment extends Fragment implements WalletInfoAdapter.OnInteractionListener,
        View.OnClickListener {

    private WalletInfoAdapter adapter;

    private List<WalletManager.WalletInfo> walletList = new ArrayList<>();
    private List<WalletManager.WalletInfo> displayedList = new ArrayList<>();

    private ImageView ivGunther;
    private TextView tvNodeName;
    private TextView tvNodeAddress;
    private View pbNode;
    private View llNode;

    private Listener activityCallback;

    // Container Activity must implement this interface
    public interface Listener {
        File getStorageRoot();

        boolean onWalletSelected(String wallet, boolean streetmode);

        void onWalletDetails(String wallet);

        void onWalletReceive(String wallet);

        void onWalletRename(String name);

        void onWalletBackup(String name);

        void onWalletArchive(String walletName);

        void onAddWallet(String type);

        void onNodePrefs();

        void showNet();

        void setToolbarButton(int type);

        void setTitle(String title);

        void setNode(NodeInfo node);

        NodeInfo getNode();

        Set<NodeInfo> getFavouriteNodes();

        boolean hasLedger();
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
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        Timber.d("onResume() %s", activityCallback.getFavouriteNodes().size());
        activityCallback.setTitle(null);
        activityCallback.setToolbarButton(Toolbar.BUTTON_CREDITS);
        activityCallback.showNet();
        NodeInfo node = activityCallback.getNode();
        if (node == null)
            findBestNode();
        else
            showNode(node);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Timber.d("onCreateView");
        View view = inflater.inflate(R.layout.fragment_login, container, false);

        ivGunther = view.findViewById(R.id.ivGunther);
        fabScreen = view.findViewById(R.id.fabScreen);
        fab = view.findViewById(R.id.fab);
        fabNew = view.findViewById(R.id.fabNew);
        fabView = view.findViewById(R.id.fabView);
        fabKey = view.findViewById(R.id.fabKey);
        fabSeed = view.findViewById(R.id.fabSeed);
        fabLedger = view.findViewById(R.id.fabLedger);

        fabNewL = view.findViewById(R.id.fabNewL);
        fabViewL = view.findViewById(R.id.fabViewL);
        fabKeyL = view.findViewById(R.id.fabKeyL);
        fabSeedL = view.findViewById(R.id.fabSeedL);
        fabLedgerL = view.findViewById(R.id.fabLedgerL);

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
        fabLedger.setOnClickListener(this);
        fabScreen.setOnClickListener(this);

        RecyclerView recyclerView = view.findViewById(R.id.list);
        registerForContextMenu(recyclerView);
        this.adapter = new WalletInfoAdapter(getActivity(), this);
        recyclerView.setAdapter(adapter);

        ViewGroup llNotice = view.findViewById(R.id.llNotice);
        Notice.showAll(llNotice, ".*_login");

        pbNode = view.findViewById(R.id.pbNode);
        llNode = view.findViewById(R.id.llNode);
        llNode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (activityCallback.getFavouriteNodes().isEmpty())
                    startNodePrefs();
                else
                    findBestNode();
            }
        });
        tvNodeName = view.findViewById(R.id.tvNodeName);
        tvNodeAddress = view.findViewById(R.id.tvNodeAddress);
        view.findViewById(R.id.ibOption).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startNodePrefs();
            }
        });

        Helper.hideKeyboard(getActivity());

        loadList();

        return view;
    }

    // Callbacks from WalletInfoAdapter

    // Wallet touched
    @Override
    public void onInteraction(final View view, final WalletManager.WalletInfo infoItem) {
        String addressPrefix = WalletManager.getInstance().addressPrefix();
        if (addressPrefix.indexOf(infoItem.address.charAt(0)) < 0) {
            Toast.makeText(getActivity(), getString(R.string.prompt_wrong_net), Toast.LENGTH_LONG).show();
            return;
        }
        openWallet(infoItem.name, false);
    }

    private void openWallet(String name, boolean streetmode) {
        activityCallback.onWalletSelected(name, streetmode);
    }

    @Override
    public boolean onContextInteraction(MenuItem item, WalletManager.WalletInfo listItem) {
        switch (item.getItemId()) {
            case R.id.action_streetmode:
                openWallet(listItem.name, true);
                break;
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
        String addressPrefix = WalletManager.getInstance().addressPrefix();
        for (WalletManager.WalletInfo s : walletList) {
            if (addressPrefix.indexOf(s.address.charAt(0)) >= 0) displayedList.add(s);
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

        // remove information of non-existent wallet
        Set<String> removedWallets = getActivity()
                .getSharedPreferences(KeyStoreHelper.SecurityConstants.WALLET_PASS_PREFS_NAME, Context.MODE_PRIVATE)
                .getAll().keySet();
        for (WalletManager.WalletInfo s : walletList) {
            removedWallets.remove(s.name);
        }
        for (String name : removedWallets) {
            KeyStoreHelper.removeWalletUserPass(getActivity(), name);
        }
    }

    private void showInfo(@NonNull String name) {
        activityCallback.onWalletDetails(name);
    }

    private void showReceive(@NonNull String name) {
        activityCallback.onWalletReceive(name);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.list_menu, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    private boolean isFabOpen = false;
    private FloatingActionButton fab, fabNew, fabView, fabKey, fabSeed, fabLedger;
    private FrameLayout fabScreen;
    private RelativeLayout fabNewL, fabViewL, fabKeyL, fabSeedL, fabLedgerL;
    private Animation fab_open, fab_close, rotate_forward, rotate_backward, fab_open_screen, fab_close_screen;
    private Animation fab_pulse;

    public boolean isFabOpen() {
        return isFabOpen;
    }

    public void animateFAB() {
        if (isFabOpen) { // close the fab
            fabScreen.setClickable(false);
            fabScreen.startAnimation(fab_close_screen);
            fab.startAnimation(rotate_backward);
            if (fabLedgerL.getVisibility() == View.VISIBLE) {
                fabLedgerL.startAnimation(fab_close);
                fabLedger.setClickable(false);
            } else {
                fabNewL.startAnimation(fab_close);
                fabNew.setClickable(false);
                fabViewL.startAnimation(fab_close);
                fabView.setClickable(false);
                fabKeyL.startAnimation(fab_close);
                fabKey.setClickable(false);
                fabSeedL.startAnimation(fab_close);
                fabSeed.setClickable(false);
            }
            isFabOpen = false;
        } else { // open the fab
            fabScreen.setClickable(true);
            fabScreen.startAnimation(fab_open_screen);
            fab.startAnimation(rotate_forward);
            if (activityCallback.hasLedger()) {
                fabLedgerL.setVisibility(View.VISIBLE);
                fabNewL.setVisibility(View.GONE);
                fabViewL.setVisibility(View.GONE);
                fabKeyL.setVisibility(View.GONE);
                fabSeedL.setVisibility(View.GONE);

                fabLedgerL.startAnimation(fab_open);
                fabLedger.setClickable(true);
            } else {
                fabLedgerL.setVisibility(View.GONE);
                fabNewL.setVisibility(View.VISIBLE);
                fabViewL.setVisibility(View.VISIBLE);
                fabKeyL.setVisibility(View.VISIBLE);
                fabSeedL.setVisibility(View.VISIBLE);

                fabNewL.startAnimation(fab_open);
                fabNew.setClickable(true);
                fabViewL.startAnimation(fab_open);
                fabView.setClickable(true);
                fabKeyL.startAnimation(fab_open);
                fabKey.setClickable(true);
                fabSeedL.startAnimation(fab_open);
                fabSeed.setClickable(true);
            }
            isFabOpen = true;
        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        Timber.d("onClick %d/%d", id, R.id.fabLedger);
        switch (id) {
            case R.id.fab:
                animateFAB();
                break;
            case R.id.fabNew:
                fabScreen.setVisibility(View.INVISIBLE);
                isFabOpen = false;
                activityCallback.onAddWallet(GenerateFragment.TYPE_NEW);
                break;
            case R.id.fabView:
                animateFAB();
                activityCallback.onAddWallet(GenerateFragment.TYPE_VIEWONLY);
                break;
            case R.id.fabKey:
                animateFAB();
                activityCallback.onAddWallet(GenerateFragment.TYPE_KEY);
                break;
            case R.id.fabSeed:
                animateFAB();
                activityCallback.onAddWallet(GenerateFragment.TYPE_SEED);
                break;
            case R.id.fabLedger:
                Timber.d("FAB_LEDGER");
                animateFAB();
                activityCallback.onAddWallet(GenerateFragment.TYPE_LEDGER);
                break;
            case R.id.fabScreen:
                animateFAB();
                break;
        }
    }

    public void findBestNode() {
        new AsyncFindBestNode().execute();
    }

    private class AsyncFindBestNode extends AsyncTask<Void, Void, NodeInfo> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            pbNode.setVisibility(View.VISIBLE);
            llNode.setVisibility(View.INVISIBLE);
            activityCallback.setNode(null);
        }

        @Override
        protected NodeInfo doInBackground(Void... params) {
            List<NodeInfo> nodesToTest = new ArrayList<>(activityCallback.getFavouriteNodes());
            Timber.d("testing best node from %d", nodesToTest.size());
            if (nodesToTest.isEmpty()) return null;
            for (NodeInfo node : nodesToTest) {
                node.testRpcService(); // TODO: do this in parallel?
                // no: it's better if it looks like it's doing something
            }
            Collections.sort(nodesToTest, NodeInfo.BestNodeComparator);
            NodeInfo bestNode = nodesToTest.get(0);
            if (bestNode.isValid()) {
                activityCallback.setNode(bestNode);
                return bestNode;
            } else {
                activityCallback.setNode(null);
                return null;
            }
        }

        @Override
        protected void onPostExecute(NodeInfo result) {
            if (!isAdded()) return;
            pbNode.setVisibility(View.INVISIBLE);
            llNode.setVisibility(View.VISIBLE);
            if (result != null) {
                Timber.d("found a good node %s", result.toString());
                showNode(result);
            } else {
                if (!activityCallback.getFavouriteNodes().isEmpty()) {
                    tvNodeName.setText(getResources().getText(R.string.node_refresh_hint));
                    tvNodeName.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_refresh_black_24dp, 0, 0, 0);
                    tvNodeAddress.setText(null);
                    tvNodeAddress.setVisibility(View.GONE);
                } else {
                    tvNodeName.setText(getResources().getText(R.string.node_create_hint));
                    tvNodeName.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
                    tvNodeAddress.setText(null);
                    tvNodeAddress.setVisibility(View.GONE);
                }
            }
        }

        @Override
        protected void onCancelled(NodeInfo result) { //TODO: cancel this on exit from fragment
            Timber.d("cancelled with %s", result);
        }
    }

    private void showNode(NodeInfo nodeInfo) {
        tvNodeName.setText(nodeInfo.getName());
        tvNodeName.setCompoundDrawablesWithIntrinsicBounds(NodeInfoAdapter.getPingIcon(nodeInfo), 0, 0, 0);
        tvNodeAddress.setText(nodeInfo.getAddress());
        tvNodeAddress.setVisibility(View.VISIBLE);
    }

    private void startNodePrefs() {
        activityCallback.setNode(null);
        activityCallback.onNodePrefs();
    }
}
