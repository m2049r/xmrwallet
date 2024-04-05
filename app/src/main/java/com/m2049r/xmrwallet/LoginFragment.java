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
import android.text.Html;
import android.text.Spanned;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.m2049r.xmrwallet.data.NodeInfo;
import com.m2049r.xmrwallet.dialog.HelpFragment;
import com.m2049r.xmrwallet.layout.WalletInfoAdapter;
import com.m2049r.xmrwallet.model.WalletManager;
import com.m2049r.xmrwallet.util.Helper;
import com.m2049r.xmrwallet.util.KeyStoreHelper;
import com.m2049r.xmrwallet.util.NetCipherHelper;
import com.m2049r.xmrwallet.util.NodePinger;
import com.m2049r.xmrwallet.util.Notice;
import com.m2049r.xmrwallet.util.ThemeHelper;
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

    private final List<WalletManager.WalletInfo> walletList = new ArrayList<>();

    private View tvGuntherSays;
    private ImageView ivGunther;
    private TextView tvNodeName;
    private TextView tvNodeInfo;
    private ImageButton ibNetwork;
    private CircularProgressIndicator pbNetwork;

    private Listener activityCallback;

    // Container Activity must implement this interface
    public interface Listener {
        File getStorageRoot();

        boolean onWalletSelected(String wallet, boolean streetmode);

        void onWalletDetails(String wallet);

        void onWalletRename(String name);

        void onWalletBackup(String name);

        void onWalletRestore();

        void onWalletDelete(String walletName);

        void onWalletDeleteCache(String walletName);

        void onAddWallet(String type);

        void onNodePrefs();

        void showNet();

        void setToolbarButton(int type);

        void setTitle(String title);

        void setNode(NodeInfo node);

        NodeInfo getNode();

        Set<NodeInfo> getFavouriteNodes();

        Set<NodeInfo> getOrPopulateFavourites();

        boolean hasLedger();

        void runOnNetCipher(Runnable runnable);
    }

    @Override
    public void onAttach(@NonNull Context context) {
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
        torStatus = null;
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        Timber.d("onResume() %s", activityCallback.getFavouriteNodes().size());
        activityCallback.setTitle(null);
        activityCallback.setToolbarButton(Toolbar.BUTTON_SETTINGS);
        activityCallback.showNet();
        showNetwork();
        //activityCallback.runOnNetCipher(this::pingSelectedNode);
    }

    private OnBackPressedCallback onBackPressedCallback = new OnBackPressedCallback(false) {
        @Override
        public void handleOnBackPressed() {
            animateFAB();
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Timber.d("onCreateView");
        View view = inflater.inflate(R.layout.fragment_login, container, false);

        tvGuntherSays = view.findViewById(R.id.tvGuntherSays);
        ivGunther = view.findViewById(R.id.ivGunther);
        fabScreen = view.findViewById(R.id.fabScreen);
        fab = view.findViewById(R.id.fab);
        fabNew = view.findViewById(R.id.fabNew);
        fabView = view.findViewById(R.id.fabView);
        fabKey = view.findViewById(R.id.fabKey);
        fabSeed = view.findViewById(R.id.fabSeed);
        fabImport = view.findViewById(R.id.fabImport);
        fabLedger = view.findViewById(R.id.fabLedger);

        fabNewL = view.findViewById(R.id.fabNewL);
        fabViewL = view.findViewById(R.id.fabViewL);
        fabKeyL = view.findViewById(R.id.fabKeyL);
        fabSeedL = view.findViewById(R.id.fabSeedL);
        fabImportL = view.findViewById(R.id.fabImportL);
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
        fabImport.setOnClickListener(this);
        fabLedger.setOnClickListener(this);
        fabScreen.setOnClickListener(this);

        RecyclerView recyclerView = view.findViewById(R.id.list);
        registerForContextMenu(recyclerView);
        this.adapter = new WalletInfoAdapter(getActivity(), this);
        recyclerView.setAdapter(adapter);

        ViewGroup llNotice = view.findViewById(R.id.llNotice);
        Notice.showAll(llNotice, ".*_login");

        view.findViewById(R.id.llNode).setOnClickListener(v -> startNodePrefs());
        tvNodeName = view.findViewById(R.id.tvNodeName);
        tvNodeInfo = view.findViewById(R.id.tvInfo);
        view.findViewById(R.id.ibRenew).setOnClickListener(v -> findBestNode());
        ibNetwork = view.findViewById(R.id.ibNetwork);
        ibNetwork.setOnClickListener(v -> changeNetwork());
        ibNetwork.setEnabled(false);
        pbNetwork = view.findViewById(R.id.pbNetwork);

        Helper.hideKeyboard(getActivity());

        loadList();

        return view;
    }

    // Callbacks from WalletInfoAdapter

    // Wallet touched
    @Override
    public void onInteraction(final View view, final WalletManager.WalletInfo infoItem) {
        openWallet(infoItem.getName(), false);
    }

    private void openWallet(String name, boolean streetmode) {
        activityCallback.onWalletSelected(name, streetmode);
    }

    @Override
    public boolean onContextInteraction(MenuItem item, WalletManager.WalletInfo listItem) {
        final int id = item.getItemId();
        if (id == R.id.action_streetmode) {
            openWallet(listItem.getName(), true);
        } else if (id == R.id.action_info) {
            showInfo(listItem.getName());
        } else if (id == R.id.action_rename) {
            activityCallback.onWalletRename(listItem.getName());
        } else if (id == R.id.action_backup) {
            activityCallback.onWalletBackup(listItem.getName());
        } else if (id == R.id.action_archive) {
            activityCallback.onWalletDelete(listItem.getName());
        } else if (id == R.id.action_deletecache) {
            activityCallback.onWalletDeleteCache(listItem.getName());
        } else {
            return super.onContextItemSelected(item);
        }
        return true;
    }

    public void loadList() {
        Timber.d("loadList()");
        WalletManager mgr = WalletManager.getInstance();
        walletList.clear();
        walletList.addAll(mgr.findWallets(activityCallback.getStorageRoot()));
        adapter.setInfos(walletList);

        // deal with Gunther & FAB animation
        if (walletList.isEmpty()) {
            fab.startAnimation(fab_pulse);
            if (ivGunther.getDrawable() == null) {
                ivGunther.setImageResource(R.drawable.ic_emptygunther);
                tvGuntherSays.setVisibility(View.VISIBLE);
            }
        } else {
            fab.clearAnimation();
            if (ivGunther.getDrawable() != null) {
                ivGunther.setImageDrawable(null);
            }
            tvGuntherSays.setVisibility(View.GONE);
        }

        // remove information of non-existent wallet
        Set<String> removedWallets = getActivity()
                .getSharedPreferences(KeyStoreHelper.SecurityConstants.WALLET_PASS_PREFS_NAME, Context.MODE_PRIVATE)
                .getAll().keySet();
        for (WalletManager.WalletInfo s : walletList) {
            removedWallets.remove(s.getName());
        }
        for (String name : removedWallets) {
            KeyStoreHelper.removeWalletUserPass(getActivity(), name);
        }
    }

    private void showInfo(@NonNull String name) {
        activityCallback.onWalletDetails(name);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        requireActivity().getOnBackPressedDispatcher().addCallback(this, onBackPressedCallback);
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.list_menu, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    private boolean fabOpen = false;
    private FloatingActionButton fab, fabNew, fabView, fabKey, fabSeed, fabImport, fabLedger;
    private RelativeLayout fabScreen;
    private RelativeLayout fabNewL, fabViewL, fabKeyL, fabSeedL, fabImportL, fabLedgerL;
    private Animation fab_open, fab_close, rotate_forward, rotate_backward, fab_open_screen, fab_close_screen;
    private Animation fab_pulse;

    public boolean isFabOpen() {
        return fabOpen;
    }

    private void setFabOpen(boolean value) {
        fabOpen = value;
        onBackPressedCallback.setEnabled(value);
    }

    public void animateFAB() {
        if (isFabOpen()) { // close the fab
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
                fabImportL.startAnimation(fab_close);
                fabImport.setClickable(false);
            }
            setFabOpen(false);
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
                fabImportL.setVisibility(View.GONE);

                fabLedgerL.startAnimation(fab_open);
                fabLedger.setClickable(true);
            } else {
                fabLedgerL.setVisibility(View.GONE);
                fabNewL.setVisibility(View.VISIBLE);
                fabViewL.setVisibility(View.VISIBLE);
                fabKeyL.setVisibility(View.VISIBLE);
                fabSeedL.setVisibility(View.VISIBLE);
                fabImportL.setVisibility(View.VISIBLE);

                fabNewL.startAnimation(fab_open);
                fabNew.setClickable(true);
                fabViewL.startAnimation(fab_open);
                fabView.setClickable(true);
                fabKeyL.startAnimation(fab_open);
                fabKey.setClickable(true);
                fabSeedL.startAnimation(fab_open);
                fabSeed.setClickable(true);
                fabImportL.startAnimation(fab_open);
                fabImport.setClickable(true);
            }
            setFabOpen(true);
        }
    }

    @Override
    public void onClick(View v) {
        final int id = v.getId();
        Timber.d("onClick %d/%d", id, R.id.fabLedger);
        if (id == R.id.fab) {
            animateFAB();
        } else if (id == R.id.fabNew) {
            fabScreen.setVisibility(View.INVISIBLE);
            setFabOpen(false);
            activityCallback.onAddWallet(GenerateFragment.TYPE_NEW);
        } else if (id == R.id.fabView) {
            animateFAB();
            activityCallback.onAddWallet(GenerateFragment.TYPE_VIEWONLY);
        } else if (id == R.id.fabKey) {
            animateFAB();
            activityCallback.onAddWallet(GenerateFragment.TYPE_KEY);
        } else if (id == R.id.fabSeed) {
            animateFAB();
            activityCallback.onAddWallet(GenerateFragment.TYPE_SEED);
        } else if (id == R.id.fabImport) {
            animateFAB();
            activityCallback.onWalletRestore();
        } else if (id == R.id.fabLedger) {
            Timber.d("FAB_LEDGER");
            animateFAB();
            activityCallback.onAddWallet(GenerateFragment.TYPE_LEDGER);
        } else if (id == R.id.fabScreen) {
            animateFAB();
        }
    }

    public void findBestNode() {
        new AsyncFindBestNode().execute(AsyncFindBestNode.FIND_BEST);
    }

    public void pingSelectedNode() {
        new AsyncFindBestNode().execute(AsyncFindBestNode.PING_SELECTED);
    }

    private NodeInfo autoselect(Set<NodeInfo> nodes) {
        if (nodes.isEmpty()) return null;
        NodePinger.execute(nodes, null);
        List<NodeInfo> nodeList = new ArrayList<>(nodes);
        Collections.sort(nodeList, NodeInfo.BestNodeComparator);
        return nodeList.get(0);
    }

    private void setSubtext(String status) {
        final Context ctx = getContext();
        final Spanned text = Html.fromHtml(ctx.getString(R.string.status,
                Integer.toHexString(ThemeHelper.getThemedColor(ctx, R.attr.positiveColor) & 0xFFFFFF),
                Integer.toHexString(ThemeHelper.getThemedColor(ctx, android.R.attr.colorBackground) & 0xFFFFFF),
                status, ""));
        tvNodeInfo.setText(text);
    }

    private class AsyncFindBestNode extends AsyncTask<Integer, Void, NodeInfo> {
        final static int PING_SELECTED = 0;
        final static int FIND_BEST = 1;

        private boolean netState;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            tvNodeName.setVisibility(View.GONE);
            pbNetwork.setVisibility(View.VISIBLE);
            netState = ibNetwork.isClickable();
            ibNetwork.setClickable(false);
            setSubtext(getString(R.string.node_waiting));
        }

        @Override
        protected NodeInfo doInBackground(Integer... params) {
            Set<NodeInfo> favourites = activityCallback.getOrPopulateFavourites();
            NodeInfo selectedNode;
            if (params[0] == FIND_BEST) {
                selectedNode = autoselect(favourites);
            } else if (params[0] == PING_SELECTED) {
                selectedNode = activityCallback.getNode();
                if (!activityCallback.getFavouriteNodes().contains(selectedNode))
                    selectedNode = null; // it's not in the favourites (any longer)
                if (selectedNode == null)
                    for (NodeInfo node : favourites) {
                        if (node.isSelected()) {
                            selectedNode = node;
                            break;
                        }
                    }
                if (selectedNode == null) { // autoselect
                    selectedNode = autoselect(favourites);
                } else {
                    selectedNode.testRpcService();
                }
            } else throw new IllegalStateException();
            if ((selectedNode != null) && selectedNode.isValid()) {
                activityCallback.setNode(selectedNode);
                return selectedNode;
            } else {
                activityCallback.setNode(null);
                return null;
            }
        }

        @Override
        protected void onPostExecute(NodeInfo result) {
            if (!isAdded()) return;
            tvNodeName.setVisibility(View.VISIBLE);
            pbNetwork.setVisibility(View.INVISIBLE);
            ibNetwork.setClickable(netState);
            if (result != null) {
                Timber.d("found a good node %s", result.toString());
                showNode(result);
            } else {
                tvNodeName.setText(getResources().getText(R.string.node_create_hint));
                tvNodeName.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
                tvNodeInfo.setText(null);
                tvNodeInfo.setVisibility(View.GONE);
            }
        }

        @Override
        protected void onCancelled(NodeInfo result) { //TODO: cancel this on exit from fragment
            Timber.d("cancelled with %s", result);
        }
    }

    private void showNode(NodeInfo nodeInfo) {
        tvNodeName.setText(nodeInfo.getName());
        nodeInfo.showInfo(tvNodeInfo);
        tvNodeInfo.setVisibility(View.VISIBLE);
    }

    private void startNodePrefs() {
        activityCallback.onNodePrefs();
    }

    // Network (Tor) stuff

    private void changeNetwork() {
        Timber.d("S: %s", NetCipherHelper.getStatus());
        final NetCipherHelper.Status status = NetCipherHelper.getStatus();
        if (status == NetCipherHelper.Status.NOT_INSTALLED) {
            HelpFragment.display(requireActivity().getSupportFragmentManager(), R.string.help_tor);
        } else if (status == NetCipherHelper.Status.NOT_ENABLED) {
            Toast.makeText(getActivity(), getString(R.string.tor_enable_background), Toast.LENGTH_LONG).show();
        } else {
            pbNetwork.setVisibility(View.VISIBLE);
            ibNetwork.setEnabled(false);
            NetCipherHelper.getInstance().toggle();
        }
    }

    private NetCipherHelper.Status torStatus = null;

    void showNetwork() {
        final NetCipherHelper.Status status = NetCipherHelper.getStatus();
        Timber.d("SHOW %s", status);
        if (status == torStatus) return;
        torStatus = status;
        switch (status) {
            case ENABLED:
                ibNetwork.setImageResource(R.drawable.ic_network_tor_on);
                ibNetwork.setEnabled(true);
                ibNetwork.setClickable(true);
                pbNetwork.setVisibility(View.INVISIBLE);
                break;
            case NOT_ENABLED:
            case DISABLED:
                ibNetwork.setImageResource(R.drawable.ic_network_clearnet);
                ibNetwork.setEnabled(true);
                ibNetwork.setClickable(true);
                pbNetwork.setVisibility(View.INVISIBLE);
                break;
            case STARTING:
                ibNetwork.setImageResource(R.drawable.ic_network_clearnet);
                ibNetwork.setEnabled(false);
                pbNetwork.setVisibility(View.VISIBLE);
                break;
            case STOPPING:
                ibNetwork.setImageResource(R.drawable.ic_network_clearnet);
                ibNetwork.setEnabled(false);
                pbNetwork.setVisibility(View.VISIBLE);
                break;
            case NOT_INSTALLED:
                ibNetwork.setEnabled(true);
                ibNetwork.setClickable(true);
                pbNetwork.setVisibility(View.INVISIBLE);
                ibNetwork.setImageResource(R.drawable.ic_network_clearnet);
                break;
            default:
                return;
        }
        activityCallback.runOnNetCipher(this::pingSelectedNode);
    }
}
