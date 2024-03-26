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

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.m2049r.xmrwallet.data.DefaultNodes;
import com.m2049r.xmrwallet.data.Node;
import com.m2049r.xmrwallet.data.NodeInfo;
import com.m2049r.xmrwallet.dialog.HelpFragment;
import com.m2049r.xmrwallet.ledger.Ledger;
import com.m2049r.xmrwallet.ledger.LedgerProgressDialog;
import com.m2049r.xmrwallet.model.NetworkType;
import com.m2049r.xmrwallet.model.Wallet;
import com.m2049r.xmrwallet.model.WalletManager;
import com.m2049r.xmrwallet.service.WalletService;
import com.m2049r.xmrwallet.util.Helper;
import com.m2049r.xmrwallet.util.KeyStoreHelper;
import com.m2049r.xmrwallet.util.LegacyStorageHelper;
import com.m2049r.xmrwallet.util.MoneroThreadPoolExecutor;
import com.m2049r.xmrwallet.util.NetCipherHelper;
import com.m2049r.xmrwallet.util.ThemeHelper;
import com.m2049r.xmrwallet.util.ZipBackup;
import com.m2049r.xmrwallet.util.ZipRestore;
import com.m2049r.xmrwallet.widget.Toolbar;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import lombok.Getter;
import timber.log.Timber;

public class LoginActivity extends BaseActivity
        implements LoginFragment.Listener, GenerateFragment.Listener,
        GenerateReviewFragment.Listener, GenerateReviewFragment.AcceptListener,
        NodeFragment.Listener, SettingsFragment.Listener {
    private static final String GENERATE_STACK = "gen";

    private static final String NODES_PREFS_NAME = "nodes";
    private static final String SELECTED_NODE_PREFS_NAME = "selected_node";
    private static final String PREF_DAEMON_TESTNET = "daemon_testnet";
    private static final String PREF_DAEMON_STAGENET = "daemon_stagenet";
    private static final String PREF_DAEMON_MAINNET = "daemon_mainnet";

    private NodeInfo node = null;

    Set<NodeInfo> favouriteNodes = new HashSet<>();

    @Override
    public NodeInfo getNode() {
        return node;
    }

    @Override
    public void setNode(NodeInfo node) {
        setNode(node, true);
    }

    private void setNode(NodeInfo node, boolean save) {
        if (node != this.node) {
            if ((node != null) && (node.getNetworkType() != WalletManager.getInstance().getNetworkType()))
                throw new IllegalArgumentException("network type does not match");
            this.node = node;
            for (NodeInfo nodeInfo : favouriteNodes) {
                nodeInfo.setSelected(nodeInfo == node);
            }
            WalletManager.getInstance().setDaemon(node);
            if (save)
                saveSelectedNode();
        }
    }

    @Override
    public Set<NodeInfo> getFavouriteNodes() {
        return favouriteNodes;
    }

    @Override
    public Set<NodeInfo> getOrPopulateFavourites() {
        if (favouriteNodes.isEmpty()) {
            for (DefaultNodes node : DefaultNodes.values()) {
                NodeInfo nodeInfo = NodeInfo.fromString(node.getUri());
                if (nodeInfo != null) {
                    nodeInfo.setFavourite(true);
                    favouriteNodes.add(nodeInfo);
                }
            }
            saveFavourites();
        }
        return favouriteNodes;
    }

    @Override
    public void setFavouriteNodes(Collection<NodeInfo> nodes) {
        Timber.d("adding %d nodes", nodes.size());
        favouriteNodes.clear();
        for (NodeInfo node : nodes) {
            Timber.d("adding %s %b", node, node.isFavourite());
            if (node.isFavourite())
                favouriteNodes.add(node);
        }
        saveFavourites();
    }

    private void loadFavouritesWithNetwork() {
        Helper.runWithNetwork(() -> {
            loadFavourites();
            return true;
        });
    }

    private void loadFavourites() {
        Timber.d("loadFavourites");
        favouriteNodes.clear();
        final String selectedNodeId = getSelectedNodeId();
        Map<String, ?> storedNodes = getSharedPreferences(NODES_PREFS_NAME, Context.MODE_PRIVATE).getAll();
        for (Map.Entry<String, ?> nodeEntry : storedNodes.entrySet()) {
            if (nodeEntry != null) { // just in case, ignore possible future errors
                final String nodeId = (String) nodeEntry.getValue();
                final NodeInfo addedNode = addFavourite(nodeId);
                if (addedNode != null) {
                    if (nodeId.equals(selectedNodeId)) {
                        addedNode.setSelected(true);
                    }
                }
            }
        }
        if (storedNodes.isEmpty()) { // try to load legacy list & remove it (i.e. migrate the data once)
            SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
            switch (WalletManager.getInstance().getNetworkType()) {
                case NetworkType_Mainnet:
                    loadLegacyList(sharedPref.getString(PREF_DAEMON_MAINNET, null));
                    sharedPref.edit().remove(PREF_DAEMON_MAINNET).apply();
                    break;
                case NetworkType_Stagenet:
                    loadLegacyList(sharedPref.getString(PREF_DAEMON_STAGENET, null));
                    sharedPref.edit().remove(PREF_DAEMON_STAGENET).apply();
                    break;
                case NetworkType_Testnet:
                    loadLegacyList(sharedPref.getString(PREF_DAEMON_TESTNET, null));
                    sharedPref.edit().remove(PREF_DAEMON_TESTNET).apply();
                    break;
                default:
                    throw new IllegalStateException("unsupported net " + WalletManager.getInstance().getNetworkType());
            }
        }
    }

    private void saveFavourites() {
        Timber.d("SAVE");
        SharedPreferences.Editor editor = getSharedPreferences(NODES_PREFS_NAME, Context.MODE_PRIVATE).edit();
        editor.clear();
        int i = 1;
        for (Node info : favouriteNodes) {
            final String nodeString = info.toNodeString();
            editor.putString(Integer.toString(i), nodeString);
            Timber.d("saved %d:%s", i, nodeString);
            i++;
        }
        editor.apply();
    }

    private NodeInfo addFavourite(String nodeString) {
        final NodeInfo nodeInfo = NodeInfo.fromString(nodeString);
        if (nodeInfo != null) {
            nodeInfo.setFavourite(true);
            favouriteNodes.add(nodeInfo);
        } else
            Timber.w("nodeString invalid: %s", nodeString);
        return nodeInfo;
    }

    private void loadLegacyList(final String legacyListString) {
        if (legacyListString == null) return;
        final String[] nodeStrings = legacyListString.split(";");
        for (final String nodeString : nodeStrings) {
            addFavourite(nodeString);
        }
    }

    private void saveSelectedNode() { // save only if changed
        final NodeInfo nodeInfo = getNode();
        final String selectedNodeId = getSelectedNodeId();
        if (nodeInfo != null) {
            if (!nodeInfo.toNodeString().equals(selectedNodeId))
                saveSelectedNode(nodeInfo);
        } else {
            if (selectedNodeId != null)
                saveSelectedNode(null);
        }
    }

    private void saveSelectedNode(NodeInfo nodeInfo) {
        SharedPreferences.Editor editor = getSharedPreferences(SELECTED_NODE_PREFS_NAME, Context.MODE_PRIVATE).edit();
        if (nodeInfo == null) {
            editor.clear();
        } else {
            editor.putString("0", getNode().toNodeString());
        }
        editor.apply();
    }

    private String getSelectedNodeId() {
        return getSharedPreferences(SELECTED_NODE_PREFS_NAME, Context.MODE_PRIVATE)
                .getString("0", null);
    }


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
    public boolean hasLedger() {
        return Ledger.isConnected();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Timber.d("onCreate()");
        ThemeHelper.setPreferred(this);
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_login);
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        toolbar.setOnButtonListener(type -> {
            switch (type) {
                case Toolbar.BUTTON_BACK:
                    getOnBackPressedDispatcher().onBackPressed();
                    break;
                case Toolbar.BUTTON_CLOSE:
                    finish();
                    break;
                case Toolbar.BUTTON_SETTINGS:
                    startSettingsFragment();
                    break;
                case Toolbar.BUTTON_NONE:
                    break;
                default:
                    Timber.e("Button " + type + "pressed - how can this be?");
            }
        });

        loadFavouritesWithNetwork();

        LegacyStorageHelper.migrateWallets(this);

        if (savedInstanceState == null) startLoginFragment();

        // try intents
        Intent intent = getIntent();
        if (!processUsbIntent(intent))
            processUriIntent(intent);
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
    public boolean onWalletSelected(String walletName, boolean streetmode) {
        if (node == null) {
            Toast.makeText(this, getString(R.string.prompt_daemon_missing), Toast.LENGTH_SHORT).show();
            return false;
        }
        if (checkServiceRunning()) return false;
        try {
            new AsyncOpenWallet(walletName, node, streetmode).execute();
        } catch (IllegalArgumentException ex) {
            Timber.e(ex.getLocalizedMessage());
            Toast.makeText(this, ex.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    @Override
    public void onWalletDetails(final String walletName) {
        Timber.d("details for wallet .%s.", walletName);
        if (checkServiceRunning()) return;
        DialogInterface.OnClickListener dialogClickListener = (dialog, which) -> {
            switch (which) {
                case DialogInterface.BUTTON_POSITIVE:
                    final File walletFile = Helper.getWalletFile(LoginActivity.this, walletName);
                    if (WalletManager.getInstance().walletExists(walletFile)) {
                        Helper.promptPassword(LoginActivity.this, walletName, true, new Helper.PasswordAction() {
                            @Override
                            public void act(String walletName1, String password, boolean fingerprintUsed) {
                                if (checkDevice(walletName1, password))
                                    startDetails(walletFile, password, GenerateReviewFragment.VIEW_TYPE_DETAILS);
                            }

                            @Override
                            public void fail(String walletName) {
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
        };

        AlertDialog.Builder builder = new MaterialAlertDialogBuilder(this);
        builder.setMessage(getString(R.string.details_alert_message))
                .setPositiveButton(getString(R.string.details_alert_yes), dialogClickListener)
                .setNegativeButton(getString(R.string.details_alert_no), dialogClickListener)
                .show();
    }

    private void renameWallet(String oldName, String newName) {
        File walletFile = Helper.getWalletFile(this, oldName);
        boolean success = renameWallet(walletFile, newName);
        if (success) {
            reloadWalletList();
        } else {
            Toast.makeText(LoginActivity.this, getString(R.string.rename_failed), Toast.LENGTH_LONG).show();
        }
    }

    // copy + delete seems safer than rename because we can rollback easily
    boolean renameWallet(File walletFile, String newName) {
        if (copyWallet(walletFile, new File(walletFile.getParentFile(), newName), false, true)) {
            try {
                KeyStoreHelper.copyWalletUserPass(this, walletFile.getName(), newName);
            } catch (KeyStoreHelper.BrokenPasswordStoreException ex) {
                Timber.w(ex);
            }
            deleteWallet(walletFile); // also deletes the keystore entry
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

        AlertDialog.Builder alertDialogBuilder = new MaterialAlertDialogBuilder(this);
        alertDialogBuilder.setView(promptsView);

        final EditText etRename = promptsView.findViewById(R.id.etRename);
        final TextView tvRenameLabel = promptsView.findViewById(R.id.tvRenameLabel);

        tvRenameLabel.setText(getString(R.string.prompt_rename, walletName));

        // set dialog message
        alertDialogBuilder
                .setCancelable(false)
                .setPositiveButton(getString(R.string.label_ok),
                        (dialog, id) -> {
                            Helper.hideKeyboardAlways(LoginActivity.this);
                            String newName = etRename.getText().toString();
                            renameWallet(walletName, newName);
                        })
                .setNegativeButton(getString(R.string.label_cancel),
                        (dialog, id) -> {
                            Helper.hideKeyboardAlways(LoginActivity.this);
                            dialog.cancel();
                        });

        final AlertDialog dialog = alertDialogBuilder.create();
        Helper.showKeyboard(dialog);

        // accept keyboard "ok"
        etRename.setOnEditorActionListener((v, actionId, event) -> {
            if ((event != null && (event.getKeyCode() == KeyEvent.KEYCODE_ENTER) && (event.getAction() == KeyEvent.ACTION_DOWN))
                    || (actionId == EditorInfo.IME_ACTION_DONE)) {
                Helper.hideKeyboardAlways(LoginActivity.this);
                String newName = etRename.getText().toString();
                dialog.cancel();
                renameWallet(walletName, newName);
                return false;
            }
            return false;
        });

        dialog.show();
    }

    private static final int CREATE_BACKUP_INTENT = 4711;
    private static final int RESTORE_BACKUP_INTENT = 4712;
    private ZipBackup zipBackup;

    @Override
    public void onWalletBackup(String walletName) {
        Timber.d("backup for wallet ." + walletName + ".");
        // overwrite any pending backup request
        zipBackup = new ZipBackup(this, walletName);

        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/zip");
        intent.putExtra(Intent.EXTRA_TITLE, zipBackup.getBackupName());
        startActivityForResult(intent, CREATE_BACKUP_INTENT);
    }

    @Override
    public void onWalletRestore() {
        Timber.d("restore wallet");

        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/zip");
        startActivityForResult(intent, RESTORE_BACKUP_INTENT);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CREATE_BACKUP_INTENT) {
            if (data == null) {
                // nothing selected
                Toast.makeText(this, getString(R.string.backup_failed), Toast.LENGTH_LONG).show();
                zipBackup = null;
                return;
            }
            try {
                if (zipBackup == null) return; // ignore unsolicited request
                zipBackup.writeTo(data.getData());
                Toast.makeText(this, getString(R.string.backup_success), Toast.LENGTH_SHORT).show();
            } catch (IOException ex) {
                Timber.e(ex);
                Toast.makeText(this, getString(R.string.backup_failed), Toast.LENGTH_LONG).show();
            } finally {
                zipBackup = null;
            }
        } else if (requestCode == RESTORE_BACKUP_INTENT) {
            if (data == null) {
                // nothing selected
                Toast.makeText(this, getString(R.string.restore_failed), Toast.LENGTH_LONG).show();
                return;
            }
            try {
                ZipRestore zipRestore = new ZipRestore(this, data.getData());
                Toast.makeText(this, getString(R.string.menu_restore), Toast.LENGTH_SHORT).show();
                if (zipRestore.restore()) {
                    reloadWalletList();
                } else {
                    Toast.makeText(this, getString(R.string.restore_failed), Toast.LENGTH_LONG).show();
                }
            } catch (IOException ex) {
                Timber.e(ex);
                Toast.makeText(this, getString(R.string.restore_failed), Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public void onWalletDelete(final String walletName) {
        Timber.d("delete for wallet ." + walletName + ".");
        if (checkServiceRunning()) return;
        DialogInterface.OnClickListener dialogClickListener = (dialog, which) -> {
            switch (which) {
                case DialogInterface.BUTTON_POSITIVE:
                    if (deleteWallet(Helper.getWalletFile(LoginActivity.this, walletName))) {
                        reloadWalletList();
                    } else {
                        Toast.makeText(LoginActivity.this, getString(R.string.delete_failed), Toast.LENGTH_LONG).show();
                    }
                    break;
                case DialogInterface.BUTTON_NEGATIVE:
                    // do nothing
                    break;
            }
        };

        final AlertDialog.Builder builder = new MaterialAlertDialogBuilder(this);
        final AlertDialog confirm = builder.setMessage(getString(R.string.delete_alert_message))
                .setTitle(walletName)
                .setPositiveButton(getString(R.string.delete_alert_yes), dialogClickListener)
                .setNegativeButton(getString(R.string.delete_alert_no), dialogClickListener)
                .setView(View.inflate(builder.getContext(), R.layout.checkbox_confirm, null))
                .show();
        confirm.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(false);
        final MaterialCheckBox checkBox = confirm.findViewById(R.id.checkbox);
        checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            confirm.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(isChecked);
        });
    }

    @Override
    public void onWalletDeleteCache(final String walletName) {
        Timber.d("delete cache for wallet ." + walletName + ".");
        if (checkServiceRunning()) return;
        DialogInterface.OnClickListener dialogClickListener = (dialog, which) -> {
            switch (which) {
                case DialogInterface.BUTTON_POSITIVE:
                    if (!deleteWalletCache(Helper.getWalletFile(LoginActivity.this, walletName))) {
                        Toast.makeText(LoginActivity.this, getString(R.string.delete_failed), Toast.LENGTH_LONG).show();
                    }
                    break;
                case DialogInterface.BUTTON_NEGATIVE:
                    // do nothing
                    break;
            }
        };

        final AlertDialog.Builder builder = new MaterialAlertDialogBuilder(this);
        final AlertDialog confirm = builder.setMessage(getString(R.string.deletecache_alert_message))
                .setTitle(walletName)
                .setPositiveButton(getString(R.string.delete_alert_yes), dialogClickListener)
                .setNegativeButton(getString(R.string.delete_alert_no), dialogClickListener)
                .setView(View.inflate(builder.getContext(), R.layout.checkbox_confirm, null))
                .show();
        confirm.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(false);
        final MaterialCheckBox checkBox = confirm.findViewById(R.id.checkbox);
        checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            confirm.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(isChecked);
        });
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
            Timber.w(ex);
        }
    }

    public void onWalletChangePassword() {//final String walletName, final String walletPassword) {
        try {
            GenerateReviewFragment detailsFragment = (GenerateReviewFragment)
                    getSupportFragmentManager().findFragmentById(R.id.fragment_container);
            AlertDialog dialog = detailsFragment.createChangePasswordDialog();
            if (dialog != null) {
                Helper.showKeyboard(dialog);
                dialog.show();
            }
        } catch (ClassCastException ex) {
            Timber.w("onWalletChangePassword() called, but no GenerateReviewFragment active");
        }
    }

    @Override
    public void onAddWallet(String type) {
        if (checkServiceRunning()) return;
        startGenerateFragment(type);
    }

    @Override
    public void onNodePrefs() {
        Timber.d("node prefs");
        if (checkServiceRunning()) return;
        startNodeFragment();
    }

    ////////////////////////////////////////
    // LoginFragment.Listener
    ////////////////////////////////////////

    @Override
    public File getStorageRoot() {
        return Helper.getWalletRoot(getApplicationContext());
    }

    ////////////////////////////////////////
    ////////////////////////////////////////

    @Override
    public void showNet() {
        showNet(WalletManager.getInstance().getNetworkType());
    }

    private void showNet(NetworkType net) {
        switch (net) {
            case NetworkType_Mainnet:
                toolbar.setSubtitle(null);
                toolbar.setBackgroundResource(R.drawable.backgound_toolbar_mainnet);
                break;
            case NetworkType_Testnet:
                toolbar.setSubtitle(getString(R.string.connect_testnet));
                toolbar.setBackgroundResource(ThemeHelper.getThemedResourceId(this, androidx.appcompat.R.attr.colorPrimaryDark));
                break;
            case NetworkType_Stagenet:
                toolbar.setSubtitle(getString(R.string.connect_stagenet));
                toolbar.setBackgroundResource(ThemeHelper.getThemedResourceId(this, androidx.appcompat.R.attr.colorPrimaryDark));
                break;
            default:
                throw new IllegalStateException("NetworkType unknown: " + net);
        }
    }

    @Override
    protected void onPause() {
        Timber.d("onPause()");
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        Timber.d("onDestroy");
        dismissProgressDialog();
        unregisterDetachReceiver();
        Ledger.disconnect();
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
        if (!Ledger.isConnected()) attachLedger();
        registerTor();
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

    void startWallet(String walletName, String walletPassword,
                     boolean fingerprintUsed, boolean streetmode) {
        Timber.d("startWallet()");
        Intent intent = new Intent(getApplicationContext(), WalletActivity.class);
        intent.putExtra(WalletActivity.REQUEST_ID, walletName);
        intent.putExtra(WalletActivity.REQUEST_PW, walletPassword);
        intent.putExtra(WalletActivity.REQUEST_FINGERPRINT_USED, fingerprintUsed);
        intent.putExtra(WalletActivity.REQUEST_STREETMODE, streetmode);
        if (uri != null) {
            intent.putExtra(WalletActivity.REQUEST_URI, uri);
            uri = null; // use only once
        }
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

    void startLoginFragment() {
        // we set these here because we cannot be ceratin we have permissions for storage before
        Helper.setMoneroHome(this);
        Helper.initLogger(this);
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

    void startNodeFragment() {
        replaceFragment(new NodeFragment(), null, null);
        Timber.d("NodeFragment placed");
    }

    void startSettingsFragment() {
        replaceFragment(new SettingsFragment(), null, null);
        Timber.d("SettingsFragment placed");
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
        final String walletName;
        final String walletPassword;
        final WalletCreator walletCreator;

        File newWalletFile;

        AsyncCreateWallet(final String name, final String password,
                          final WalletCreator walletCreator) {
            super();
            this.walletName = name;
            this.walletPassword = password;
            this.walletCreator = walletCreator;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            acquireWakeLock();
            if (walletCreator.isLedger()) {
                showLedgerProgressDialog(LedgerProgressDialog.TYPE_RESTORE);
            } else {
                showProgressDialog(R.string.generate_wallet_creating);
            }
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

            newWalletFile = new File(walletFolder, walletName);
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
            releaseWakeLock(RELEASE_WAKE_LOCK_DELAY);
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

    public void createWallet(final String name, final String password,
                             final WalletCreator walletCreator) {
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

        boolean isLedger();

    }

    boolean checkAndCloseWallet(Wallet aWallet) {
        Wallet.Status walletStatus = aWallet.getStatus();
        if (!walletStatus.isOk()) {
            Timber.e(walletStatus.getErrorString());
            toast(walletStatus.getErrorString());
        }
        aWallet.close();
        return walletStatus.isOk();
    }

    @Override
    public void onGenerate(final String name, final String password) {
        createWallet(name, password,
                new WalletCreator() {
                    @Override
                    public boolean isLedger() {
                        return false;
                    }

                    @Override
                    public boolean createWallet(File aFile, String password) {
                        NodeInfo currentNode = getNode();
                        // get it from the connected node if we have one
                        final long restoreHeight =
                                (currentNode != null) ? currentNode.getHeight() : -1;
                        Wallet newWallet = WalletManager.getInstance()
                                .createWallet(aFile, password, MNEMONIC_LANGUAGE, restoreHeight);
                        return checkAndCloseWallet(newWallet);
                    }
                });
    }

    @Override
    public void onGenerate(final String name, final String password,
                           final String seed, final String offset,
                           final long restoreHeight) {
        createWallet(name, password,
                new WalletCreator() {
                    @Override
                    public boolean isLedger() {
                        return false;
                    }

                    @Override
                    public boolean createWallet(File aFile, String password) {
                        Wallet newWallet = WalletManager.getInstance()
                                .recoveryWallet(aFile, password, seed, offset, restoreHeight);
                        return checkAndCloseWallet(newWallet);
                    }
                });
    }

    @Override
    public void onGenerateLedger(final String name, final String password,
                                 final long restoreHeight) {
        createWallet(name, password,
                new WalletCreator() {
                    @Override
                    public boolean isLedger() {
                        return true;
                    }

                    @Override
                    public boolean createWallet(File aFile, String password) {
                        Wallet newWallet = WalletManager.getInstance()
                                .createWalletFromDevice(aFile, password,
                                        restoreHeight, "Ledger");
                        return checkAndCloseWallet(newWallet);
                    }
                });
    }

    @Override
    public void onGenerate(final String name, final String password,
                           final String address, final String viewKey, final String spendKey,
                           final long restoreHeight) {
        createWallet(name, password,
                new WalletCreator() {
                    @Override
                    public boolean isLedger() {
                        return false;
                    }

                    @Override
                    public boolean createWallet(File aFile, String password) {
                        Wallet newWallet = WalletManager.getInstance()
                                .createWalletWithKeys(aFile, password, MNEMONIC_LANGUAGE, restoreHeight,
                                        address, viewKey, spendKey);
                        return checkAndCloseWallet(newWallet);
                    }
                });
    }

    private void toast(final String msg) {
        runOnUiThread(() -> Toast.makeText(LoginActivity.this, msg, Toast.LENGTH_LONG).show());
    }

    private void toast(final int msgId) {
        runOnUiThread(() -> Toast.makeText(LoginActivity.this, getString(msgId), Toast.LENGTH_LONG).show());
    }

    @Override
    public void onAccept(final String name, final String password) {
        File walletFolder = getStorageRoot();
        File walletFile = new File(walletFolder, name);
        Timber.d("New Wallet %s", walletFile.getAbsolutePath());
        walletFile.delete(); // when recovering wallets, the cache seems corrupt - so remove it

        popFragmentStack(GENERATE_STACK);
        Toast.makeText(LoginActivity.this,
                getString(R.string.generate_wallet_created), Toast.LENGTH_SHORT).show();
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
            copyFile(new File(srcDir, srcName + ".keys"), new File(dstDir, dstName + ".keys"));
            try { // cache & address.txt are optional files
                copyFile(new File(srcDir, srcName), new File(dstDir, dstName));
                copyFile(new File(srcDir, srcName + ".address.txt"), new File(dstDir, dstName + ".address.txt"));
            } catch (IOException ex) {
                Timber.d("CACHE %s", ignoreCacheError);
                if (!ignoreCacheError) { // ignore cache backup error if backing up (can be resynced)
                    throw ex;
                }
            }
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
        KeyStoreHelper.removeWalletUserPass(this, walletFile.getName());
        return success;
    }

    boolean deleteWalletCache(File walletFile) {
        Timber.d("deleteWalletCache %s", walletFile.getAbsolutePath());
        File dir = walletFile.getParentFile();
        String name = walletFile.getName();
        boolean success = true;
        File cacheFile = new File(dir, name);
        if (cacheFile.exists()) {
            success = cacheFile.delete();
        }
        return success;
    }

    void copyFile(File src, File dst) throws IOException {
        try (FileChannel inChannel = new FileInputStream(src).getChannel();
             FileChannel outChannel = new FileOutputStream(dst).getChannel()) {
            inChannel.transferTo(0, inChannel.size(), outChannel);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final int id = item.getItemId();
        if (id == R.id.action_create_help_new) {
            HelpFragment.display(getSupportFragmentManager(), R.string.help_create_new);
            return true;
        } else if (id == R.id.action_create_help_keys) {
            HelpFragment.display(getSupportFragmentManager(), R.string.help_create_keys);
            return true;
        } else if (id == R.id.action_create_help_view) {
            HelpFragment.display(getSupportFragmentManager(), R.string.help_create_view);
            return true;
        } else if (id == R.id.action_create_help_seed) {
            HelpFragment.display(getSupportFragmentManager(), R.string.help_create_seed);
            return true;
        } else if (id == R.id.action_create_help_ledger) {
            HelpFragment.display(getSupportFragmentManager(), R.string.help_create_ledger);
            return true;
        } else if (id == R.id.action_details_help) {
            HelpFragment.display(getSupportFragmentManager(), R.string.help_details);
            return true;
        } else if (id == R.id.action_details_changepw) {
            onWalletChangePassword();
            return true;
        } else if (id == R.id.action_help_list) {
            HelpFragment.display(getSupportFragmentManager(), R.string.help_list);
            return true;
        } else if (id == R.id.action_help_node) {
            HelpFragment.display(getSupportFragmentManager(), R.string.help_node);
            return true;
        } else if (id == R.id.action_default_nodes) {
            Fragment f = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
            if ((WalletManager.getInstance().getNetworkType() == NetworkType.NetworkType_Mainnet) &&
                    (f instanceof NodeFragment)) {
                ((NodeFragment) f).restoreDefaultNodes();
            }
            return true;
        } else if (id == R.id.action_ledger_seed) {
            Fragment f = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
            if (f instanceof GenerateFragment) {
                ((GenerateFragment) f).convertLedgerSeed();
            }
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    // an AsyncTask which tests the node before trying to open the wallet
    private class AsyncOpenWallet extends AsyncTask<Void, Void, Boolean> {
        final static int OK = 0;
        final static int TIMEOUT = 1;
        final static int INVALID = 2;
        final static int IOEX = 3;

        private final String walletName;
        private final NodeInfo node;
        private final boolean streetmode;

        AsyncOpenWallet(String walletName, NodeInfo node, boolean streetmode) {
            this.walletName = walletName;
            this.node = node;
            this.streetmode = streetmode;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            Timber.d("checking %s", node.getAddress());
            return node.testRpcService();
        }

        @Override
        protected void onPostExecute(Boolean result) {
            super.onPostExecute(result);
            if (isDestroyed()) {
                return;
            }
            if (result) {
                Timber.d("selected wallet is .%s.", node.getName());
                // now it's getting real, onValidateFields if wallet exists
                promptAndStart(walletName, streetmode);
            } else {
                if (node.getResponseCode() == 0) { // IOException
                    Toast.makeText(LoginActivity.this, getString(R.string.status_wallet_node_invalid), Toast.LENGTH_LONG).show();
                } else { // connected but broken
                    Toast.makeText(LoginActivity.this, getString(R.string.status_wallet_connect_ioex), Toast.LENGTH_LONG).show();
                }
            }
        }

    }

    boolean checkDevice(String walletName, String password) {
        String keyPath = new File(Helper.getWalletRoot(LoginActivity.this),
                walletName + ".keys").getAbsolutePath();
        // check if we need connected hardware
        Wallet.Device device = WalletManager.getInstance().queryWalletDevice(keyPath, password);
        if (device == Wallet.Device.Device_Ledger) {
            if (!hasLedger()) {
                toast(R.string.open_wallet_ledger_missing);
            } else {
                return true;
            }
        } else {// device could be undefined meaning the password is wrong
            // this gets dealt with later
            return true;
        }
        return false;
    }

    void promptAndStart(String walletName, final boolean streetmode) {
        File walletFile = Helper.getWalletFile(this, walletName);
        if (WalletManager.getInstance().walletExists(walletFile)) {
            Helper.promptPassword(LoginActivity.this, walletName, false,
                    new Helper.PasswordAction() {
                        @Override
                        public void act(String walletName, String password, boolean fingerprintUsed) {
                            if (checkDevice(walletName, password))
                                startWallet(walletName, password, fingerprintUsed, streetmode);
                        }

                        @Override
                        public void fail(String walletName) {
                        }

                    });
        } else { // this cannot really happen as we prefilter choices
            Toast.makeText(this, getString(R.string.bad_wallet), Toast.LENGTH_SHORT).show();
        }
    }

    // USB Stuff - (Ledger)

    private static final String ACTION_USB_PERMISSION = "com.m2049r.xmrwallet.USB_PERMISSION";

    void attachLedger() {
        final UsbManager usbManager = getUsbManager();
        UsbDevice device = Ledger.findDevice(usbManager);
        if (device != null) {
            if (usbManager.hasPermission(device)) {
                connectLedger(usbManager, device);
            } else {
                registerReceiver(usbPermissionReceiver, new IntentFilter(ACTION_USB_PERMISSION));
                usbManager.requestPermission(device,
                        PendingIntent.getBroadcast(this, 0,
                                new Intent(ACTION_USB_PERMISSION),
                                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0));
            }
        } else {
            Timber.d("no ledger device found");
        }
    }

    private final BroadcastReceiver usbPermissionReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ACTION_USB_PERMISSION.equals(action)) {
                unregisterReceiver(usbPermissionReceiver);
                synchronized (this) {
                    UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        if (device != null) {
                            connectLedger(getUsbManager(), device);
                        }
                    } else {
                        Timber.w("User denied permission for device %s", device.getProductName());
                    }
                }
            }
        }
    };

    private void connectLedger(UsbManager usbManager, final UsbDevice usbDevice) {
        if (Ledger.ENABLED)
            try {
                Ledger.connect(usbManager, usbDevice);
                if (!Ledger.check()) {
                    Ledger.disconnect();
                    runOnUiThread(() -> Toast.makeText(LoginActivity.this,
                                    getString(R.string.toast_ledger_start_app, usbDevice.getProductName()),
                                    Toast.LENGTH_SHORT)
                            .show());
                } else {
                    registerDetachReceiver();
                    onLedgerAction();
                    runOnUiThread(() -> Toast.makeText(LoginActivity.this,
                                    getString(R.string.toast_ledger_attached, usbDevice.getProductName()),
                                    Toast.LENGTH_SHORT)
                            .show());
                }
            } catch (IOException ex) {
                runOnUiThread(() -> Toast.makeText(LoginActivity.this,
                                getString(R.string.open_wallet_ledger_missing),
                                Toast.LENGTH_SHORT)
                        .show());
            }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        processUsbIntent(intent);
    }

    private boolean processUsbIntent(Intent intent) {
        String action = intent.getAction();
        if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
            synchronized (this) {
                final UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                if (device != null) {
                    final UsbManager usbManager = getUsbManager();
                    if (usbManager.hasPermission(device)) {
                        Timber.d("Ledger attached by intent");
                        connectLedger(usbManager, device);
                    }
                }
            }
            return true;
        }
        return false;
    }

    private String uri = null;

    private void processUriIntent(Intent intent) {
        String action = intent.getAction();
        if (Intent.ACTION_VIEW.equals(action)) {
            synchronized (this) {
                uri = intent.getDataString();
                Timber.d("URI Intent %s", uri);
                HelpFragment.display(getSupportFragmentManager(), R.string.help_uri);
            }
        }
    }

    BroadcastReceiver detachReceiver;

    private void unregisterDetachReceiver() {
        if (detachReceiver != null) {
            unregisterReceiver(detachReceiver);
            detachReceiver = null;
        }
    }

    private void registerDetachReceiver() {
        detachReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals(UsbManager.ACTION_USB_DEVICE_DETACHED)) {
                    unregisterDetachReceiver();
                    final UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    Timber.i("Ledger detached!");
                    if (device != null)
                        runOnUiThread(() -> Toast.makeText(LoginActivity.this,
                                        getString(R.string.toast_ledger_detached, device.getProductName()),
                                        Toast.LENGTH_SHORT)
                                .show());
                    Ledger.disconnect();
                    onLedgerAction();
                }
            }
        };

        registerReceiver(detachReceiver, new IntentFilter(UsbManager.ACTION_USB_DEVICE_DETACHED));
    }

    public void onLedgerAction() {
        Fragment f = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
        if (f instanceof GenerateFragment) {
            getOnBackPressedDispatcher().onBackPressed();
        } else if (f instanceof LoginFragment) {
            if (((LoginFragment) f).isFabOpen()) {
                ((LoginFragment) f).animateFAB();
            }
        }
    }

    // get UsbManager or die trying
    @NonNull
    private UsbManager getUsbManager() {
        final UsbManager usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        if (usbManager == null) {
            throw new IllegalStateException("no USB_SERVICE");
        }
        return usbManager;
    }

    //
    // Tor (Orbot) stuff
    //

    void torNotify() {
        final Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
        if (fragment == null) return;

        if (fragment instanceof LoginFragment) {
            runOnUiThread(((LoginFragment) fragment)::showNetwork);
        }
    }

    private void deregisterTor() {
        NetCipherHelper.deregister();
    }

    private void registerTor() {
        NetCipherHelper.register(new NetCipherHelper.OnStatusChangedListener() {
            @Override
            public void connected() {
                Timber.d("CONNECTED");
                WalletManager.getInstance().setProxy(NetCipherHelper.getProxy());
                torNotify();
                if (waitingUiTask != null) {
                    Timber.d("RUN");
                    runOnUiThread(waitingUiTask);
                    waitingUiTask = null;
                }
            }

            @Override
            public void disconnected() {
                Timber.d("DISCONNECTED");
                WalletManager.getInstance().setProxy("");
                torNotify();
            }

            @Override
            public void notInstalled() {
                Timber.d("NOT INSTALLED");
                WalletManager.getInstance().setProxy("");
                torNotify();
            }

            @Override
            public void notEnabled() {
                Timber.d("NOT ENABLED");
                notInstalled();
            }
        });
    }

    private Runnable waitingUiTask;

    @Override
    public void runOnNetCipher(Runnable uiTask) {
        if (waitingUiTask != null) throw new IllegalStateException("only one tor task at a time");
        if (NetCipherHelper.hasClient()) {
            runOnUiThread(uiTask);
        } else {
            waitingUiTask = uiTask;
        }
    }
}
