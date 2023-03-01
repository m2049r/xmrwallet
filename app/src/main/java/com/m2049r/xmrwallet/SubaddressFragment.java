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
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.m2049r.xmrwallet.data.Subaddress;
import com.m2049r.xmrwallet.layout.SubaddressInfoAdapter;
import com.m2049r.xmrwallet.ledger.LedgerProgressDialog;
import com.m2049r.xmrwallet.model.TransactionInfo;
import com.m2049r.xmrwallet.model.Wallet;
import com.m2049r.xmrwallet.model.WalletManager;
import com.m2049r.xmrwallet.util.Helper;
import com.m2049r.xmrwallet.util.MoneroThreadPoolExecutor;
import com.m2049r.xmrwallet.widget.Toolbar;

import java.util.ArrayList;
import java.util.List;

import lombok.RequiredArgsConstructor;
import timber.log.Timber;

public class SubaddressFragment extends Fragment implements SubaddressInfoAdapter.OnInteractionListener,
        View.OnClickListener, OnBlockUpdateListener {
    static public final String KEY_MODE = "mode";
    static public final String MODE_MANAGER = "manager";

    private SubaddressInfoAdapter adapter;

    private Listener activityCallback;

    private Wallet wallet;

    // Container Activity must implement this interface
    public interface Listener {
        void onSubaddressSelected(Subaddress subaddress);

        void setSubtitle(String title);

        void setToolbarButton(int type);

        void showSubaddress(View view, final int subaddressIndex);

        void saveWallet();
    }

    public interface ProgressListener {
        void showProgressDialog(int msgId);

        void showLedgerProgressDialog(int mode);

        void dismissProgressDialog();
    }

    private ProgressListener progressCallback = null;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof ProgressListener) {
            progressCallback = (ProgressListener) context;
        }
        if (context instanceof Listener) {
            activityCallback = (Listener) context;
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
        activityCallback.setSubtitle(getString(R.string.subbaddress_title));
        activityCallback.setToolbarButton(Toolbar.BUTTON_BACK);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Timber.d("onCreateView");

        final Bundle b = getArguments();
        managerMode = ((b != null) && (MODE_MANAGER.equals(b.getString(KEY_MODE))));

        View view = inflater.inflate(R.layout.fragment_subaddress, container, false);
        view.findViewById(R.id.fab).setOnClickListener(this);

        if (managerMode) {
            view.findViewById(R.id.tvInstruction).setVisibility(View.GONE);
            view.findViewById(R.id.tvHint).setVisibility(View.GONE);
        }

        final RecyclerView list = view.findViewById(R.id.list);
        adapter = new SubaddressInfoAdapter(getActivity(), this);
        list.setAdapter(adapter);
        adapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                list.scrollToPosition(positionStart);
            }
        });

        Helper.hideKeyboard(getActivity());

        wallet = WalletManager.getInstance().getWallet();

        loadList();

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    public void loadList() {
        Timber.d("loadList()");
        final int numSubaddresses = wallet.getNumSubaddresses();
        final List<Subaddress> list = new ArrayList<>();
        for (int i = 0; i < numSubaddresses; i++) {
            list.add(wallet.getSubaddressObject(i));
        }
        adapter.setInfos(list);
    }

    @Override
    public void onBlockUpdate(Wallet wallet) {
        loadList();
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.fab) {
            getNewSubaddress();
        }
    }

    private int lastUsedSubaddress() {
        int lastUsedSubaddress = 0;
        for (TransactionInfo info : wallet.getHistory().getAll()) {
            if (info.addressIndex > lastUsedSubaddress)
                lastUsedSubaddress = info.addressIndex;
        }
        return lastUsedSubaddress;
    }

    private void getNewSubaddress() {
        final int maxSubaddresses = lastUsedSubaddress() + wallet.getDeviceType().getSubaddressLookahead();
        if (wallet.getNumSubaddresses() < maxSubaddresses)
            new AsyncSubaddress().executeOnExecutor(MoneroThreadPoolExecutor.MONERO_THREAD_POOL_EXECUTOR);
        else
            Toast.makeText(getActivity(), getString(R.string.max_subaddress_warning), Toast.LENGTH_LONG).show();
    }

    @SuppressLint("StaticFieldLeak")
    @RequiredArgsConstructor
    private class AsyncSubaddress extends AsyncTask<Void, Void, Boolean> {
        boolean dialogOpened = false;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            if ((wallet.getDeviceType() == Wallet.Device.Device_Ledger) && (progressCallback != null)) {
                progressCallback.showLedgerProgressDialog(LedgerProgressDialog.TYPE_SUBADDRESS);
                dialogOpened = true;
            }
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            if (params.length != 0) return false;
            wallet.getNewSubaddress();
            if (activityCallback != null) {
                activityCallback.saveWallet();
            }
            return true;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            super.onPostExecute(result);
            if (dialogOpened)
                progressCallback.dismissProgressDialog();
            if (!isAdded()) // never mind then
                return;
            loadList();
        }
    }

    boolean managerMode = false;

    // Callbacks from SubaddressInfoAdapter
    @Override
    public void onInteraction(final View view, final Subaddress subaddress) {
        if (managerMode)
            activityCallback.showSubaddress(view, subaddress.getAddressIndex());
        else
            activityCallback.onSubaddressSelected(subaddress); // also closes the fragment with onBackpressed()
    }

    @Override
    public boolean onLongInteraction(View view, Subaddress subaddress) {
        activityCallback.showSubaddress(view, subaddress.getAddressIndex());
        return false;
    }

}
