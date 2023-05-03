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
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import androidx.transition.Transition;
import androidx.transition.TransitionInflater;

import com.google.android.material.textfield.TextInputLayout;
import com.m2049r.xmrwallet.data.Subaddress;
import com.m2049r.xmrwallet.layout.TransactionInfoAdapter;
import com.m2049r.xmrwallet.model.TransactionInfo;
import com.m2049r.xmrwallet.model.Wallet;
import com.m2049r.xmrwallet.util.Helper;
import com.m2049r.xmrwallet.widget.Toolbar;

import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;

public class SubaddressInfoFragment extends Fragment
        implements TransactionInfoAdapter.Listener, OnBlockUpdateListener {
    private TransactionInfoAdapter adapter;

    private Subaddress subaddress;

    private TextInputLayout etName;
    private TextView tvAddress;
    private TextView tvTxLabel;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_subaddressinfo, container, false);

        etName = view.findViewById(R.id.etName);
        tvAddress = view.findViewById(R.id.tvAddress);
        tvTxLabel = view.findViewById(R.id.tvTxLabel);

        final RecyclerView list = view.findViewById(R.id.list);
        adapter = new TransactionInfoAdapter(getActivity(), this);
        list.setAdapter(adapter);

        final Wallet wallet = activityCallback.getWallet();

        Bundle b = getArguments();
        final int subaddressIndex = b.getInt("subaddressIndex");
        subaddress = wallet.getSubaddressObject(subaddressIndex);

        etName.getEditText().setText(subaddress.getDisplayLabel());
        tvAddress.setText(getContext().getString(R.string.subbaddress_info_subtitle,
                subaddress.getAddressIndex(), subaddress.getAddress()));

        etName.getEditText().setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                wallet.setSubaddressLabel(subaddressIndex, etName.getEditText().getText().toString());
            }
        });
        etName.getEditText().setOnEditorActionListener((v, actionId, event) -> {
            if ((event != null && (event.getKeyCode() == KeyEvent.KEYCODE_ENTER) && (event.getAction() == KeyEvent.ACTION_DOWN))
                    || (actionId == EditorInfo.IME_ACTION_DONE)) {
                Helper.hideKeyboard(getActivity());
                wallet.setSubaddressLabel(subaddressIndex, etName.getEditText().getText().toString());
                onRefreshed(wallet);
                return true;
            }
            return false;
        });

        onRefreshed(wallet);

        return view;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Transition transform = TransitionInflater.from(requireContext())
                .inflateTransition(R.transition.details);
        setSharedElementEnterTransition(transform);
    }

    public void onRefreshed(final Wallet wallet) {
        Timber.d("onRefreshed");
        List<TransactionInfo> list = new ArrayList<>();
        for (TransactionInfo info : wallet.getHistory().getAll()) {
            if (info.addressIndex == subaddress.getAddressIndex())
                list.add(info);
        }
        adapter.setInfos(list);
        if (list.isEmpty())
            tvTxLabel.setText(R.string.subaddress_notx_label);
        else
            tvTxLabel.setText(R.string.subaddress_tx_label);
    }

    @Override
    public void onBlockUpdate(Wallet wallet) {
        onRefreshed(wallet);
    }

    // Callbacks from TransactionInfoAdapter
    @Override
    public void onInteraction(final View view, final TransactionInfo infoItem) {
        activityCallback.onTxDetailsRequest(view, infoItem);
    }

    Listener activityCallback;

    // Container Activity must implement this interface
    public interface Listener {
        void onTxDetailsRequest(View view, TransactionInfo info);

        Wallet getWallet();

        void setToolbarButton(int type);

        void setTitle(String title, String subtitle);

        void setSubtitle(String subtitle);

        long getDaemonHeight();
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
    public void onResume() {
        super.onResume();
        Timber.d("onResume()");
        activityCallback.setSubtitle(getString(R.string.subbaddress_title));
        activityCallback.setToolbarButton(Toolbar.BUTTON_BACK);
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public long getDaemonHeight() {
        return activityCallback.getDaemonHeight();
    }
}
