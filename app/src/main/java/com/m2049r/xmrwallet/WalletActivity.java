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

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.preference.PreferenceManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.navigation.NavigationView;
import com.m2049r.xmrwallet.data.BarcodeData;
import com.m2049r.xmrwallet.data.Subaddress;
import com.m2049r.xmrwallet.data.TxData;
import com.m2049r.xmrwallet.data.UserNotes;
import com.m2049r.xmrwallet.dialog.HelpFragment;
import com.m2049r.xmrwallet.dialog.PocketChangeFragment;
import com.m2049r.xmrwallet.fragment.send.SendAddressWizardFragment;
import com.m2049r.xmrwallet.fragment.send.SendFragment;
import com.m2049r.xmrwallet.ledger.LedgerProgressDialog;
import com.m2049r.xmrwallet.model.PendingTransaction;
import com.m2049r.xmrwallet.model.TransactionInfo;
import com.m2049r.xmrwallet.model.Wallet;
import com.m2049r.xmrwallet.model.WalletManager;
import com.m2049r.xmrwallet.service.WalletService;
import com.m2049r.xmrwallet.util.Helper;
import com.m2049r.xmrwallet.util.MoneroThreadPoolExecutor;
import com.m2049r.xmrwallet.util.ThemeHelper;
import com.m2049r.xmrwallet.widget.Toolbar;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import timber.log.Timber;

public class WalletActivity extends BaseActivity implements WalletFragment.Listener,
        WalletService.Observer, SendFragment.Listener, TxFragment.Listener,
        GenerateReviewFragment.ListenerWithWallet,
        GenerateReviewFragment.Listener,
        GenerateReviewFragment.PasswordChangedListener,
        ScannerFragment.OnScannedListener, ReceiveFragment.Listener,
        SendAddressWizardFragment.OnScanListener,
        WalletFragment.DrawerLocker,
        NavigationView.OnNavigationItemSelectedListener,
        SubaddressFragment.Listener,
        SubaddressInfoFragment.Listener,
        PocketChangeFragment.Listener {

    public static final String REQUEST_ID = "id";
    public static final String REQUEST_PW = "pw";
    public static final String REQUEST_FINGERPRINT_USED = "fingerprint";
    public static final String REQUEST_STREETMODE = "streetmode";
    public static final String REQUEST_URI = "uri";

    private NavigationView accountsView;
    private DrawerLayout drawer;
    private ActionBarDrawerToggle drawerToggle;

    private Toolbar toolbar;
    private boolean requestStreetMode = false;

    private String password;

    private String uri = null;

    private long streetMode = 0;

    @Override
    public void onPasswordChanged(String newPassword) {
        password = newPassword;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public void setToolbarButton(int type) {
        toolbar.setButton(type);
    }

    @Override
    public void setTitle(String title, String subtitle) {
        toolbar.setTitle(title, subtitle);
    }

    @Override
    public void setTitle(String title) {
        Timber.d("setTitle:%s.", title);
        toolbar.setTitle(title);
    }

    @Override
    public void setSubtitle(String subtitle) {
        toolbar.setSubtitle(subtitle);
    }

    private boolean synced = false;

    @Override
    public boolean isSynced() {
        return synced;
    }

    private WalletFragment getWalletFragment() {
        return (WalletFragment) getSupportFragmentManager().findFragmentByTag(WalletFragment.class.getName());
    }

    private Fragment getCurrentFragment() {
        return getSupportFragmentManager().findFragmentById(R.id.fragment_container);
    }

    @Override
    public boolean isStreetMode() {
        return streetMode > 0;
    }

    private void enableStreetMode(boolean enable) {
        if (enable) {
            streetMode = getWallet().getDaemonBlockChainHeight();
        } else {
            streetMode = 0;
        }
        final WalletFragment walletFragment = getWalletFragment();
        if (walletFragment != null) walletFragment.resetDismissedTransactions();
        forceUpdate();
        runOnUiThread(() -> {
            if (getWallet() != null)
                updateAccountsBalance();
        });
    }

    @Override
    public long getStreetModeHeight() {
        return streetMode;
    }

    @Override
    public boolean isWatchOnly() {
        return getWallet().isWatchOnly();
    }

    @Override
    public String getTxKey(String txId) {
        return getWallet().getTxKey(txId);
    }

    @Override
    public String getTxNotes(String txId) {
        return getWallet().getUserNote(txId);
    }

    @Override
    public boolean setTxNotes(String txId, String txNotes) {
        return getWallet().setUserNote(txId, txNotes);
    }

    @Override
    public String getTxAddress(int major, int minor) {
        return getWallet().getSubaddress(major, minor);
    }

    private void startWalletService() {
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            acquireWakeLock();
            String walletId = extras.getString(REQUEST_ID);
            // we can set the streetmode height AFTER opening the wallet
            requestStreetMode = extras.getBoolean(REQUEST_STREETMODE);
            password = extras.getString(REQUEST_PW);
            uri = extras.getString(REQUEST_URI);
            connectWalletService(walletId, password);
        } else {
            finish();
        }
    }

    private void stopWalletService() {
        disconnectWalletService();
        releaseWakeLock();
    }

    private void onWalletRescan() {
        try {
            final WalletFragment walletFragment = getWalletFragment();
            getWallet().rescanBlockchainAsync();
            synced = false;
            walletFragment.unsync();
            invalidateOptionsMenu();
        } catch (ClassCastException ex) {
            Timber.d(ex.getLocalizedMessage());
            // keep calm and carry on
        }
    }

    @Override
    protected void onDestroy() {
        Timber.d("onDestroy()");
        if ((mBoundService != null) && (getWallet() != null)) {
            saveWallet();
        }
        stopWalletService();
        if (drawer != null) drawer.removeDrawerListener(drawerToggle);
        super.onDestroy();
    }

    @Override
    public boolean hasWallet() {
        return haveWallet;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem renameItem = menu.findItem(R.id.action_rename);
        if (renameItem != null)
            renameItem.setEnabled(hasWallet() && getWallet().isSynchronized());
        MenuItem streetmodeItem = menu.findItem(R.id.action_streetmode);
        if (streetmodeItem != null)
            if (isStreetMode()) {
                streetmodeItem.setIcon(R.drawable.gunther_csi_24dp);
            } else {
                streetmodeItem.setIcon(R.drawable.gunther_24dp);
            }
        final MenuItem rescanItem = menu.findItem(R.id.action_rescan);
        if (rescanItem != null)
            rescanItem.setEnabled(isSynced());
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final int itemId = item.getItemId();
        if (itemId == R.id.action_rescan) {
            onWalletRescan();
        } else if (itemId == R.id.action_info) {
            onWalletDetails();
        } else if (itemId == R.id.action_share) {
            onShareTxInfo();
        } else if (itemId == R.id.action_help_tx_info) {
            HelpFragment.display(getSupportFragmentManager(), R.string.help_tx_details);
        } else if (itemId == R.id.action_help_wallet) {
            HelpFragment.display(getSupportFragmentManager(), R.string.help_wallet);
        } else if (itemId == R.id.action_details_help) {
            HelpFragment.display(getSupportFragmentManager(), R.string.help_details);
        } else if (itemId == R.id.action_details_changepw) {
            onWalletChangePassword();
        } else if (itemId == R.id.action_help_send) {
            HelpFragment.display(getSupportFragmentManager(), R.string.help_send);
        } else if (itemId == R.id.action_rename) {
            onAccountRename();
        } else if (itemId == R.id.action_pocketchange) {
            PocketChangeFragment.display(getSupportFragmentManager(), getWallet().getPocketChangeSetting());
        } else if (itemId == R.id.action_subaddresses) {
            showSubaddresses(true);
        } else if (itemId == R.id.action_streetmode) {
            if (isStreetMode()) { // disable streetmode
                onDisableStreetMode();
            } else {
                onEnableStreetMode();
            }
        } else
            return super.onOptionsItemSelected(item);
        return true;
    }

    private void updateStreetMode() {
        invalidateOptionsMenu();
    }

    private void onEnableStreetMode() {
        enableStreetMode(true);
        updateStreetMode();
    }

    private void onDisableStreetMode() {
        Helper.promptPassword(WalletActivity.this, getWallet().getName(), false, new Helper.PasswordAction() {
            @Override
            public void act(String walletName, String password, boolean fingerprintUsed) {
                runOnUiThread(() -> {
                    enableStreetMode(false);
                    updateStreetMode();
                });
            }

            @Override
            public void fail(String walletName) {
            }
        });
    }


    public void onWalletChangePassword() {
        try {
            GenerateReviewFragment detailsFragment = (GenerateReviewFragment) getCurrentFragment();
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
    protected void onCreate(Bundle savedInstanceState) {
        Timber.d("onCreate()");
        ThemeHelper.setPreferred(this);
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            // activity restarted
            // we don't want that - finish it and fall back to previous activity
            finish();
            return;
        }

        setContentView(R.layout.activity_wallet);
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        toolbar.setOnButtonListener(new Toolbar.OnButtonListener() {
            @Override
            public void onButton(int type) {
                switch (type) {
                    case Toolbar.BUTTON_BACK:
                        onDisposeRequest();
                        getOnBackPressedDispatcher().onBackPressed();
                        break;
                    case Toolbar.BUTTON_CANCEL:
                        onDisposeRequest();
                        Helper.hideKeyboard(WalletActivity.this);
                        getOnBackPressedDispatcher().onBackPressed();
                        break;
                    case Toolbar.BUTTON_CLOSE:
                        finish();
                        break;
                    case Toolbar.BUTTON_SETTINGS:
                        Toast.makeText(WalletActivity.this, getString(R.string.label_credits), Toast.LENGTH_SHORT).show();
                    case Toolbar.BUTTON_NONE:
                    default:
                        Timber.e("Button " + type + "pressed - how can this be?");
                }
            }
        });

        drawer = findViewById(R.id.drawer_layout);
        drawerToggle = new ActionBarDrawerToggle(this, drawer, toolbar, 0, 0);
        drawer.addDrawerListener(drawerToggle);
        drawerToggle.syncState();
        setDrawerEnabled(false); // disable until synced

        accountsView = findViewById(R.id.accounts_nav);
        accountsView.setNavigationItemSelectedListener(this);

        showNet();

        Fragment walletFragment = new WalletFragment();
        getSupportFragmentManager().beginTransaction()
                .add(R.id.fragment_container, walletFragment, WalletFragment.class.getName()).commit();
        Timber.d("fragment added");

        startWalletService();
        Timber.d("onCreate() done.");
    }

    public void showNet() {
        switch (WalletManager.getInstance().getNetworkType()) {
            case NetworkType_Mainnet:
                toolbar.setBackgroundResource(R.drawable.backgound_toolbar_mainnet);
                break;
            case NetworkType_Stagenet:
            case NetworkType_Testnet:
                toolbar.setBackgroundResource(ThemeHelper.getThemedResourceId(this, androidx.appcompat.R.attr.colorPrimaryDark));
                break;
            default:
                throw new IllegalStateException("Unsupported Network: " + WalletManager.getInstance().getNetworkType());
        }
    }

    @Override
    public Wallet getWallet() {
        if (mBoundService == null) throw new IllegalStateException("WalletService not bound.");
        return mBoundService.getWallet();
    }

    private WalletService mBoundService = null;
    private boolean mIsBound = false;

    private final ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            // This is called when the connection with the service has been
            // established, giving us the service object we can use to
            // interact with the service.  Because we have bound to a explicit
            // service that we know is running in our own process, we can
            // cast its IBinder to a concrete class and directly access it.
            mBoundService = ((WalletService.WalletServiceBinder) service).getService();
            mBoundService.setObserver(WalletActivity.this);
            Bundle extras = getIntent().getExtras();
            if (extras != null) {
                String walletId = extras.getString(REQUEST_ID);
                if (walletId != null) {
                    setTitle(walletId);
                }
            }
            updateProgress();
            Timber.d("CONNECTED");
        }

        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            // Because it is running in our same process, we should never
            // see this happen.
            mBoundService = null;
            setTitle(getString(R.string.wallet_activity_name), getString(R.string.status_wallet_disconnected));
            Timber.d("DISCONNECTED");
        }
    };

    void connectWalletService(String walletName, String walletPassword) {
        // Establish a connection with the service.  We use an explicit
        // class name because we want a specific service implementation that
        // we know will be running in our own process (and thus won't be
        // supporting component replacement by other applications).
        Intent intent = new Intent(getApplicationContext(), WalletService.class);
        intent.putExtra(WalletService.REQUEST_WALLET, walletName);
        intent.putExtra(WalletService.REQUEST, WalletService.REQUEST_CMD_LOAD);
        intent.putExtra(WalletService.REQUEST_CMD_LOAD_PW, walletPassword);
        startService(intent);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        mIsBound = true;
        Timber.d("BOUND");
    }

    void disconnectWalletService() {
        if (mIsBound) {
            // Detach our existing connection.
            mBoundService.setObserver(null);
            unbindService(mConnection);
            mIsBound = false;
            Timber.d("UNBOUND");
        }
    }

    @Override
    protected void onPause() {
        Timber.d("onPause()");
        lock();
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (locked) {
            unlock();
        }
        Timber.d("onResume()");
    }

    @Override
    public void saveWallet() {
        if (mIsBound) { // no point in talking to unbound service
            Intent intent = new Intent(getApplicationContext(), WalletService.class);
            intent.putExtra(WalletService.REQUEST, WalletService.REQUEST_CMD_STORE);
            startService(intent);
            Timber.d("STORE request sent");
        } else {
            Timber.e("Service not bound");
        }
    }

//////////////////////////////////////////
// WalletFragment.Listener
//////////////////////////////////////////

    @Override
    public boolean hasBoundService() {
        return mBoundService != null;
    }

    @Override
    public Wallet.ConnectionStatus getConnectionStatus() {
        return mBoundService.getConnectionStatus();
    }

    @Override
    public long getDaemonHeight() {
        return mBoundService.getDaemonHeight();
    }

    @Override
    public void onSendRequest(View view) {
        replaceFragment(SendFragment.newInstance(uri), null, null);
        uri = null; // only use uri once
    }

    @Override
    public void onTxDetailsRequest(View view, TransactionInfo info) {
        Bundle args = new Bundle();
        args.putParcelable(TxFragment.ARG_INFO, info);
        replaceFragmentWithTransition(view, new TxFragment(), null, args);
    }

    @Override
    public void forceUpdate() {
        try {
            onRefreshed(getWallet(), true);
        } catch (IllegalStateException ex) {
            Timber.e(ex.getLocalizedMessage());
        }
    }

///////////////////////////
// WalletService.Observer
///////////////////////////

    private int numAccounts = -1;

    // refresh and return true if successful
    @Override
    public boolean onRefreshed(final Wallet wallet, final boolean full) {
        Timber.d("onRefreshed()");
        runOnUiThread(() -> {
            if (getWallet() != null)
                updateAccountsBalance();
        });
        if (numAccounts != wallet.getNumAccounts()) {
            numAccounts = wallet.getNumAccounts();
            runOnUiThread(this::updateAccountsList);
        }
        try {
            final WalletFragment walletFragment = getWalletFragment();
            if (wallet.isSynchronized()) {
                releaseWakeLock(RELEASE_WAKE_LOCK_DELAY); // the idea is to stay awake until synced
                if (!synced) { // first sync
                    onProgress(-1);
                    saveWallet(); // save on first sync
                    synced = true;
                    runOnUiThread(walletFragment::onSynced);
                }
            }
            runOnUiThread(() -> {
                walletFragment.onRefreshed(wallet, full);
                updateCurrentFragment(wallet);
            });
            return true;
        } catch (ClassCastException ex) {
            // not in wallet fragment (probably send monero)
            Timber.d(ex.getLocalizedMessage());
            // keep calm and carry on
        }
        return false;
    }

    private void updateCurrentFragment(final Wallet wallet) {
        final Fragment fragment = getCurrentFragment();
        if (fragment instanceof OnBlockUpdateListener) {
            ((OnBlockUpdateListener) fragment).onBlockUpdate(wallet);
        }
    }

    @Override
    public void onWalletStored(final boolean success) {
        runOnUiThread(() -> {
            if (!success) {
                Toast.makeText(WalletActivity.this, getString(R.string.status_wallet_unload_failed), Toast.LENGTH_LONG).show();
            }
        });
    }

    boolean haveWallet = false;

    @Override
    public void onWalletOpen(final Wallet.Device device) {
        if (device == Wallet.Device.Device_Ledger) {
            runOnUiThread(() -> showLedgerProgressDialog(LedgerProgressDialog.TYPE_RESTORE));
        }
    }

    @Override
    public void onWalletStarted(final Wallet.Status walletStatus) {
        runOnUiThread(() -> {
            dismissProgressDialog();
            if (walletStatus == null) {
                // guess what went wrong
                Toast.makeText(WalletActivity.this, getString(R.string.status_wallet_connect_failed), Toast.LENGTH_LONG).show();
            } else {
                if (Wallet.ConnectionStatus.ConnectionStatus_WrongVersion == walletStatus.getConnectionStatus())
                    Toast.makeText(WalletActivity.this, getString(R.string.status_wallet_connect_wrongversion), Toast.LENGTH_LONG).show();
                else if (!walletStatus.isOk())
                    Toast.makeText(WalletActivity.this, walletStatus.getErrorString(), Toast.LENGTH_LONG).show();
            }
        });
        if ((walletStatus == null) || (Wallet.ConnectionStatus.ConnectionStatus_Connected != walletStatus.getConnectionStatus())) {
            finish();
        } else {
            haveWallet = true;
            invalidateOptionsMenu();

            loadPocketChangeSettings();

            if (requestStreetMode) onEnableStreetMode();

            final WalletFragment walletFragment = getWalletFragment();
            runOnUiThread(() -> {
                updateAccountsHeader();
                if (walletFragment != null) {
                    walletFragment.onLoaded();
                }
            });
        }
    }

    @Override
    public void onTransactionCreated(final String txTag, final PendingTransaction pendingTransaction) {
        try {
            final SendFragment sendFragment = (SendFragment) getCurrentFragment();
            runOnUiThread(() -> {
                dismissProgressDialog();
                PendingTransaction.Status status = pendingTransaction.getStatus();
                if (status != PendingTransaction.Status.Status_Ok) {
                    String errorText = pendingTransaction.getErrorString();
                    getWallet().disposePendingTransaction();
                    sendFragment.onCreateTransactionFailed(errorText);
                } else {
                    sendFragment.onTransactionCreated(txTag, pendingTransaction);
                }
            });
        } catch (ClassCastException ex) {
            // not in spend fragment
            Timber.d(ex.getLocalizedMessage());
            // don't need the transaction any more
            getWallet().disposePendingTransaction();
        }
    }

    @Override
    public void onSendTransactionFailed(final String error) {
        try {
            final SendFragment sendFragment = (SendFragment) getCurrentFragment();
            runOnUiThread(() -> sendFragment.onSendTransactionFailed(error));
        } catch (ClassCastException ex) {
            // not in spend fragment
            Timber.d(ex.getLocalizedMessage());
        }
    }

    @Override
    public void onTransactionSent(final String txId) {
        try {
            final SendFragment sendFragment = (SendFragment) getCurrentFragment();
            runOnUiThread(() -> sendFragment.onTransactionSent(txId));
        } catch (ClassCastException ex) {
            // not in spend fragment
            Timber.d(ex.getLocalizedMessage());
        }
    }

    @Override
    public void onProgress(final String text) {
        try {
            final WalletFragment walletFragment = getWalletFragment();
            runOnUiThread(new Runnable() {
                public void run() {
                    walletFragment.setProgress(text);
                }
            });
        } catch (ClassCastException ex) {
            // not in wallet fragment (probably send monero)
            Timber.d(ex.getLocalizedMessage());
            // keep calm and carry on
        }
    }

    @Override
    public void onProgress(final int n) {
        runOnUiThread(() -> {
            try {
                WalletFragment walletFragment = getWalletFragment();
                if (walletFragment != null)
                    walletFragment.setProgress(n);
            } catch (ClassCastException ex) {
                // not in wallet fragment (probably send monero)
                Timber.d(ex.getLocalizedMessage());
                // keep calm and carry on
            }
        });
    }

    private void updateProgress() {
        // TODO maybe show real state of WalletService (like "still closing previous wallet")
        if (hasBoundService()) {
            onProgress(mBoundService.getProgressText());
            onProgress(mBoundService.getProgressValue());
        }
    }

///////////////////////////
// SendFragment.Listener
///////////////////////////

    @Override
    public void onSend(UserNotes notes) {
        if (mIsBound) { // no point in talking to unbound service
            Intent intent = new Intent(getApplicationContext(), WalletService.class);
            intent.putExtra(WalletService.REQUEST, WalletService.REQUEST_CMD_SEND);
            intent.putExtra(WalletService.REQUEST_CMD_SEND_NOTES, notes.txNotes);
            startService(intent);
            Timber.d("SEND TX request sent");
        } else {
            Timber.e("Service not bound");
        }

    }

    @Override
    public void onPrepareSend(final String tag, final TxData txData) {
        if (mIsBound) { // no point in talking to unbound service
            Intent intent = new Intent(getApplicationContext(), WalletService.class);
            intent.putExtra(WalletService.REQUEST, WalletService.REQUEST_CMD_TX);
            intent.putExtra(WalletService.REQUEST_CMD_TX_DATA, txData);
            intent.putExtra(WalletService.REQUEST_CMD_TX_TAG, tag);
            startService(intent);
            Timber.d("CREATE TX request sent");
            if (getWallet().getDeviceType() == Wallet.Device.Device_Ledger)
                showLedgerProgressDialog(LedgerProgressDialog.TYPE_SEND);
        } else {
            Timber.e("Service not bound");
        }
    }

    @Override
    public Subaddress getWalletSubaddress(int accountIndex, int subaddressIndex) {
        return getWallet().getSubaddressObject(accountIndex, subaddressIndex);
    }

    public String getWalletName() {
        return getWallet().getName();
    }

    void popFragmentStack(String name) {
        if (name == null) {
            getSupportFragmentManager().popBackStack();
        } else {
            getSupportFragmentManager().popBackStack(name, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        }
    }

    void replaceFragmentWithTransition(View view, Fragment newFragment, String stackName, Bundle extras) {
        if (extras != null) {
            newFragment.setArguments(extras);
        }
        int transition;
        if (newFragment instanceof TxFragment)
            transition = R.string.tx_details_transition_name;
        else if (newFragment instanceof SubaddressInfoFragment)
            transition = R.string.subaddress_info_transition_name;
        else
            throw new IllegalStateException("expecting known transition");

        getSupportFragmentManager().beginTransaction()
                .addSharedElement(view, getString(transition))
                .replace(R.id.fragment_container, newFragment)
                .addToBackStack(stackName)
                .commit();
    }

    void replaceFragment(Fragment newFragment, String stackName, Bundle extras) {
        if (extras != null) {
            newFragment.setArguments(extras);
        }
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, newFragment)
                .addToBackStack(stackName)
                .commit();
    }

    private void onWalletDetails() {
        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case DialogInterface.BUTTON_POSITIVE:
                        final Bundle extras = new Bundle();
                        extras.putString(GenerateReviewFragment.REQUEST_TYPE, GenerateReviewFragment.VIEW_TYPE_WALLET);

                        Helper.promptPassword(WalletActivity.this, getWallet().getName(), true, new Helper.PasswordAction() {
                            @Override
                            public void act(String walletName, String password, boolean fingerprintUsed) {
                                replaceFragment(new GenerateReviewFragment(), null, extras);
                            }

                            @Override
                            public void fail(String walletName) {
                            }
                        });

                        break;
                    case DialogInterface.BUTTON_NEGATIVE:
                        // do nothing
                        break;
                }
            }
        };

        AlertDialog.Builder builder = new MaterialAlertDialogBuilder(this);
        builder.setMessage(getString(R.string.details_alert_message))
                .setPositiveButton(getString(R.string.details_alert_yes), dialogClickListener)
                .setNegativeButton(getString(R.string.details_alert_no), dialogClickListener)
                .show();
    }

    void onShareTxInfo() {
        try {
            TxFragment fragment = (TxFragment) getCurrentFragment();
            fragment.shareTxInfo();
        } catch (ClassCastException ex) {
            // not in wallet fragment
            Timber.e(ex.getLocalizedMessage());
            // keep calm and carry on
        }
    }

    @Override
    public void onDisposeRequest() {
        //TODO consider doing this through the WalletService to avoid concurrency issues
        getWallet().disposePendingTransaction();
    }

    private boolean startScanFragment = false;

    @Override
    protected void onResumeFragments() {
        super.onResumeFragments();
        if (startScanFragment) {
            startScanFragment();
            startScanFragment = false;
        }
    }

    private void startScanFragment() {
        Bundle extras = new Bundle();
        replaceFragment(new ScannerFragment(), null, extras);
    }

    /// QR scanner callbacks
    @Override
    public void onScan() {
        if (Helper.getCameraPermission(this)) {
            startScanFragment();
        } else {
            Timber.i("Waiting for permissions");
        }
    }

    @Override
    public boolean onScanned(String qrCode) {
        // #gurke
        BarcodeData bcData = BarcodeData.fromString(qrCode);
        if (bcData != null) {
            popFragmentStack(null);
            Timber.d("AAA");
            onUriScanned(bcData);
            return true;
        } else {
            return false;
        }
    }

    OnUriScannedListener onUriScannedListener = null;

    @Override
    public void setOnUriScannedListener(OnUriScannedListener onUriScannedListener) {
        this.onUriScannedListener = onUriScannedListener;
    }

    @Override
    void onUriScanned(BarcodeData barcodeData) {
        super.onUriScanned(barcodeData);
        if (onUriScannedListener != null) {
            onUriScannedListener.onUriScanned(barcodeData);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Timber.d("onRequestPermissionsResult()");
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == Helper.PERMISSIONS_REQUEST_CAMERA) { // If request is cancelled, the result arrays are empty.
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startScanFragment = true;
            } else {
                String msg = getString(R.string.message_camera_not_permitted);
                Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public void onWalletReceive(View view) {
        final String address = getWallet().getAddress();
        Timber.d("startReceive()");
        Bundle b = new Bundle();
        b.putString("address", address);
        b.putString("name", getWalletName());
        replaceFragment(new ReceiveFragment(), null, b);
        Timber.d("ReceiveFragment placed");
    }

    @Override
    public long getTotalFunds() {
        return getWallet().getUnlockedBalance();
    }

    @Override
    public void onFragmentDone() {
        popFragmentStack(null);
    }

    @Override
    public SharedPreferences getPrefs() {
        return getPreferences(Context.MODE_PRIVATE);
    }

    private final List<Integer> accountIds = new ArrayList<>();

    // generate and cache unique ids for use in accounts list
    private int getAccountId(int accountIndex) {
        final int n = accountIds.size();
        for (int i = n; i <= accountIndex; i++) {
            accountIds.add(View.generateViewId());
        }
        return accountIds.get(accountIndex);
    }

    // drawer stuff

    void updateAccountsBalance() {
        final TextView tvBalance = accountsView.getHeaderView(0).findViewById(R.id.tvBalance);
        if (!isStreetMode()) {
            tvBalance.setText(getString(R.string.accounts_balance,
                    Helper.getDisplayAmount(getWallet().getBalanceAll(), 5)));
        } else {
            tvBalance.setText(null);
        }
        updateAccountsList();
    }

    void updateAccountsHeader() {
        final Wallet wallet = getWallet();
        final TextView tvName = accountsView.getHeaderView(0).findViewById(R.id.tvName);
        tvName.setText(wallet.getName());
    }

    void updateAccountsList() {
        Menu menu = accountsView.getMenu();
        menu.removeGroup(R.id.accounts_list);
        final Wallet wallet = getWallet();
        if (wallet != null) {
            final int n = wallet.getNumAccounts();
            final int currentAccount = getWallet().getAccountIndex();
            final boolean showBalances = (n > 1) && !isStreetMode();
            for (int i = 0; i < n; i++) {
                final String label = (showBalances ?
                        getString(R.string.label_account, wallet.getAccountLabel(i), Helper.getDisplayAmount(wallet.getBalance(i), 2))
                        : wallet.getAccountLabel(i));
                final MenuItem item = menu.add(R.id.accounts_list, getAccountId(i), 2 * i, label);
                item.setIcon(i == currentAccount ? R.drawable.ic_outline_folder_open_24 : R.drawable.ic_outline_folder_24);
                if (i == wallet.getAccountIndex())
                    item.setChecked(true);
            }
            menu.setGroupCheckable(R.id.accounts_list, true, true);
        }
    }

    @Override
    public void setDrawerEnabled(boolean enabled) {
        Timber.d("setDrawerEnabled %b", enabled);
        final int lockMode = enabled ? DrawerLayout.LOCK_MODE_UNLOCKED :
                DrawerLayout.LOCK_MODE_LOCKED_CLOSED;
        drawer.setDrawerLockMode(lockMode);
        drawerToggle.setDrawerIndicatorEnabled(enabled);
        invalidateOptionsMenu(); // menu may need to be changed
    }

    void updateAccountName() {
        setSubtitle(getWallet().getAccountLabel());
        updateAccountsList();
    }

    public void onAccountRename() {
        final LayoutInflater li = LayoutInflater.from(this);
        final View promptsView = li.inflate(R.layout.prompt_rename, null);

        final AlertDialog.Builder alertDialogBuilder = new MaterialAlertDialogBuilder(this);
        alertDialogBuilder.setView(promptsView);

        final EditText etRename = promptsView.findViewById(R.id.etRename);
        final TextView tvRenameLabel = promptsView.findViewById(R.id.tvRenameLabel);
        final Wallet wallet = getWallet();
        tvRenameLabel.setText(getString(R.string.prompt_rename, wallet.getAccountLabel()));

        // set dialog message
        alertDialogBuilder
                .setCancelable(false)
                .setPositiveButton(getString(R.string.label_ok),
                        (dialog, id) -> {
                            Helper.hideKeyboardAlways(WalletActivity.this);
                            String newName = etRename.getText().toString();
                            wallet.setAccountLabel(newName);
                            updateAccountName();
                        })
                .setNegativeButton(getString(R.string.label_cancel),
                        (dialog, id) -> {
                            Helper.hideKeyboardAlways(WalletActivity.this);
                            dialog.cancel();
                        });

        final AlertDialog dialog = alertDialogBuilder.create();
        Helper.showKeyboard(dialog);

        // accept keyboard "ok"
        etRename.setOnEditorActionListener((v, actionId, event) -> {
            if ((event != null && (event.getKeyCode() == KeyEvent.KEYCODE_ENTER) && (event.getAction() == KeyEvent.ACTION_DOWN))
                    || (actionId == EditorInfo.IME_ACTION_DONE)) {
                Helper.hideKeyboardAlways(WalletActivity.this);
                String newName = etRename.getText().toString();
                dialog.cancel();
                wallet.setAccountLabel(newName);
                updateAccountName();
                return false;
            }
            return false;
        });

        dialog.show();
    }

    public void setAccountIndex(int accountIndex) {
        getWallet().setAccountIndex(accountIndex);
        loadPocketChangeSettings();
        selectedSubaddressIndex = 0;
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        final int id = item.getItemId();
        if (id == R.id.account_new) {
            addAccount();
        } else {
            Timber.d("NavigationDrawer ID=%d", id);
            int accountIdx = accountIds.indexOf(id);
            if (accountIdx >= 0) {
                Timber.d("found @%d", accountIdx);
                setAccountIndex(accountIdx);
            }
            forceUpdate();
            drawer.closeDrawer(GravityCompat.START);
        }
        return true;
    }

    private int lastUsedAccount() {
        int lastUsedAccount = 0;
        for (TransactionInfo info : getWallet().getHistory().getAll()) {
            if (info.accountIndex > lastUsedAccount)
                lastUsedAccount = info.accountIndex;
        }
        return lastUsedAccount;
    }

    private void addAccount() {
        final Wallet wallet = getWallet();
        final int maxAccounts = lastUsedAccount() + wallet.getDeviceType().getAccountLookahead();
        if (wallet.getNumAccounts() < maxAccounts)
            new AsyncAddAccount().executeOnExecutor(MoneroThreadPoolExecutor.MONERO_THREAD_POOL_EXECUTOR);
        else
            Toast.makeText(this, getString(R.string.max_account_warning), Toast.LENGTH_LONG).show();
    }

    @SuppressLint("StaticFieldLeak")
    private class AsyncAddAccount extends AsyncTask<Void, Void, Boolean> {
        boolean dialogOpened = false;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            switch (getWallet().getDeviceType()) {
                case Device_Ledger:
                    showLedgerProgressDialog(LedgerProgressDialog.TYPE_ACCOUNT);
                    dialogOpened = true;
                    break;
                case Device_Software:
                    showProgressDialog(R.string.accounts_progress_new);
                    dialogOpened = true;
                    break;
                default:
                    throw new IllegalStateException("Hardware backing not supported. At all!");
            }
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            if (params.length != 0) return false;
            getWallet().addAccount();
            setAccountIndex(getWallet().getNumAccounts() - 1);
            return true;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            super.onPostExecute(result);
            forceUpdate();
            drawer.closeDrawer(GravityCompat.START);
            if (dialogOpened)
                dismissProgressDialog();
            Toast.makeText(WalletActivity.this,
                    getString(R.string.accounts_new, getWallet().getNumAccounts() - 1),
                    Toast.LENGTH_SHORT).show();
        }
    }

    // we store the index only and always retrieve a new Subaddress object
    // to ensure we get the current label
    private int selectedSubaddressIndex = 0;

    @Override
    public Subaddress getSelectedSubaddress() {
        return getWallet().getSubaddressObject(selectedSubaddressIndex);
    }

    @Override
    public void onSubaddressSelected(@Nullable final Subaddress subaddress) {
        selectedSubaddressIndex = subaddress.getAddressIndex();
        getOnBackPressedDispatcher().onBackPressed();
    }

    @Override
    public void showSubaddresses(boolean managerMode) {
        final Bundle b = new Bundle();
        if (managerMode)
            b.putString(SubaddressFragment.KEY_MODE, SubaddressFragment.MODE_MANAGER);
        replaceFragment(new SubaddressFragment(), null, b);
    }

    @Override
    public void showSubaddress(View view, final int subaddressIndex) {
        final Bundle b = new Bundle();
        b.putInt("subaddressIndex", subaddressIndex);
        replaceFragmentWithTransition(view, new SubaddressInfoFragment(), null, b);
    }

    @Override
    public void setPocketChange(Wallet.PocketChangeSetting setting) {
        SharedPreferences.Editor editor = getPrefs().edit();
        editor.putString(getWallet().getAddress() + "_PC", setting.toPrefString());
        editor.apply();
        getWallet().setPocketChangeSetting(setting);
    }


    public void loadPocketChangeSettings() {
        final String settings = getPrefs().getString(getWallet().getAddress() + "_PC", "0");
        getWallet().setPocketChangeSetting(Wallet.PocketChangeSetting.from(settings));
    }

    @Getter
    private boolean locked = false;

    private void unlock() {
        Helper.promptPassword(WalletActivity.this, getWalletName(), false, new Helper.PasswordAction() {
            @Override
            public void act(String walletName, String password, boolean fingerprintUsed) {
                popFragmentStack(null);
                locked = false;
                Timber.d("locked: %b", isLocked());
            }

            @Override
            public void fail(String walletName) {
                finish();
            }
        });
    }

    private void lock() {
        if (PreferenceManager.getDefaultSharedPreferences(this)
                .getBoolean(getString(R.string.preferred_lock), false)) {
            replaceFragment(new LockFragment(), null, null);
            locked = true;
        }
        Timber.d("locked %b", isLocked());
    }
}
