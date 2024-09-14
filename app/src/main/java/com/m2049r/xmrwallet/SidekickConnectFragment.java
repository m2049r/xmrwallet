/*
 * Copyright (c) 2018 m2049r
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

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.m2049r.xmrwallet.data.BluetoothInfo;
import com.m2049r.xmrwallet.layout.BluetoothInfoAdapter;
import com.m2049r.xmrwallet.util.Helper;
import com.m2049r.xmrwallet.widget.Toolbar;

import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;

public class SidekickConnectFragment extends Fragment
        implements BluetoothInfoAdapter.OnInteractionListener {

    private BluetoothAdapter bluetoothAdapter;

    private SwipeRefreshLayout pullToRefresh;

    private BluetoothInfoAdapter infoAdapter;

    private Listener activityCallback;

    public interface Listener {
        void setToolbarButton(int type);

        void setSubtitle(String title);
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof Listener) {
            this.activityCallback = (Listener) context;
        } else {
            throw new ClassCastException(context + " must implement Listener");
        }
    }

    @Override
    public void onPause() {
        Timber.d("onPause()");
        if (bluetoothAdapter.isDiscovering()) {
            bluetoothAdapter.cancelDiscovery();
        }
        // the the activity we are connected? why? it can ask the bluetoothservice...
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        Timber.d("onResume()");
        activityCallback.setSubtitle(getString(R.string.label_bluetooth));
        activityCallback.setToolbarButton(Toolbar.BUTTON_BACK);
        final BluetoothFragment btFragment = (BluetoothFragment) getChildFragmentManager().findFragmentById(R.id.bt_fragment);
        assert btFragment != null;
        btFragment.start();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Timber.d("onCreateView");
        View view = inflater.inflate(R.layout.fragment_sidekick_connect, container, false);

        RecyclerView recyclerView = view.findViewById(R.id.list);
        infoAdapter = new BluetoothInfoAdapter(this);
        recyclerView.setAdapter(infoAdapter);

        pullToRefresh = view.findViewById(R.id.pullToRefresh);
        pullToRefresh.setOnRefreshListener(() -> {
            populateList();
            pullToRefresh.setRefreshing(false);
        });

        return view;
    }

    private void populateList() {
        List<BluetoothInfo> items = new ArrayList<>();
        for (BluetoothDevice device : bluetoothAdapter.getBondedDevices()) {
            final int deviceCLass = device.getBluetoothClass().getDeviceClass();
            switch (deviceCLass) {
                case BluetoothClass.Device.PHONE_SMART:
                    //TODO verify these are correct
                case BluetoothClass.Device.COMPUTER_HANDHELD_PC_PDA:
                case BluetoothClass.Device.COMPUTER_PALM_SIZE_PC_PDA:
                    items.add(new BluetoothInfo(device));
            }
        }
        infoAdapter.setItems(items);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        Helper.hideKeyboard(getActivity());

        // Get the local Bluetooth adapter
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        populateList();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.sidekick_connect_menu, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        // Make sure we're not doing discovery anymore
        if (bluetoothAdapter != null) {
            bluetoothAdapter.cancelDiscovery();
        }
    }

    @Override
    public void onInteraction(final View view, final BluetoothInfo item) {
        Timber.d("onInteraction %s", item);
        bluetoothAdapter.cancelDiscovery();

        final BluetoothFragment btFragment = (BluetoothFragment) getChildFragmentManager().findFragmentById(R.id.bt_fragment);
        assert btFragment != null;
        btFragment.connectDevice(item.getAddress());
    }

    public void allowClick() {
        infoAdapter.allowClick(true);
    }
}