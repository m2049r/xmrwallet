/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// mostly from BluetoothChatFragment https://github.com/android/connectivity-samples

package com.m2049r.xmrwallet;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.m2049r.xmrwallet.service.BluetoothService;
import com.m2049r.xmrwallet.util.Flasher;

import java.security.SecureRandom;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import timber.log.Timber;

public class BluetoothFragment extends Fragment {

    public BluetoothFragment() {
        super();
    }

    interface Listener {
        void onDeviceConnected(String connectedDeviceName);

        void abort(String message);

        void onReceive(int commandId);
    }

    //TODO enable discover only after wallet is loaded
    Listener activityCallback;

    // Intent request codes
    private static final int REQUEST_ENABLE_BT = 3;

    private ImageView btIcon;
    private ProgressBar pbConnecting;
    private TextView btCode;
    private TextView btName;

    private String connectedDeviceName = null;

    private BluetoothAdapter bluetoothAdapter = null;

    private BluetoothService bluetoothService = null;

    public enum Mode {
        CLIENT, SERVER
    }

    private Mode mode = Mode.CLIENT;

    public BluetoothFragment(Mode mode) {
        super();
        this.mode = mode;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Get local Bluetooth adapter
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        // If the adapter is null, then Bluetooth is not supported
        if (bluetoothAdapter == null) {
            if (activityCallback != null)
                activityCallback.abort("Bluetooth is not available"); //TODO strings.xml
        }
    }

    public void start() {
        if (bluetoothAdapter == null) return;

        // If BT is not on, request that it be enabled.
        // setupComm() will then be called during onActivityResult
        if (!bluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        } else if (bluetoothService == null) {
            setupCommunication();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (bluetoothService != null) {
            bluetoothService.setUiHandler(null);
            bluetoothService = null;
        }
        // The BluetoothService is stopped in LoginActivity::onDestroy
    }

    @Override
    public void onPause() {
        Timber.d("onPause %s", mode);
        super.onPause();
    }

    @Override
    public void onResume() {
        Timber.d("onResume %s", mode);
        super.onResume();

        // Performing this check in onResume() covers the case in which BT was
        // not enabled during onStart(), so we were paused to enable it...
        // onResume() will be called when ACTION_REQUEST_ENABLE activity returns.
        if (bluetoothService != null) {
            // Only if the state is STATE_NONE, do we know that we haven't started already
            if (!bluetoothService.isStarted()) {
                // Start the Bluetooth services
                bluetoothService.start();
            }
        }
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof BluetoothFragment.Listener) {
            activityCallback = (BluetoothFragment.Listener) context;
        } else {
            throw new ClassCastException(context.toString()
                    + " must implement Listener");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        Timber.d("onCreateView");
        return inflater.inflate(R.layout.fragment_bluetooth, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        Timber.d("onViewCreated");
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) return;
        btName = view.findViewById(R.id.btName);
        btCode = view.findViewById(R.id.btCode);
        btIcon = view.findViewById(R.id.btIcon);
        pbConnecting = view.findViewById(R.id.pbConnecting);
        setConnecting(false);
        setInfo(null, null);
    }

    /**
     * Set up the UI and background operations for comms.
     */
    private void setupCommunication() {
        Timber.d("startCommunication()");
        if (!isAdded()) {
            return;
        }

        if (bluetoothService != null) throw new IllegalStateException("bluetoothService != null");

        // Initialize the BluetoothService to perform bluetooth connections
        bluetoothService = BluetoothService.GetInstance();
        bluetoothService.setUiHandler(handler);
        if (mode == Mode.SERVER)
            bluetoothService.start();
        setInfo(bluetoothService.getConnectedName(), bluetoothService.getConnectedCode());
        showState(bluetoothService.getState());
    }

    private void showState(int state) {
        if (!isAdded()) return;
        Light light;
        switch (state) {
            case BluetoothService.State.LISTEN:
                light = Light.LISTEN;
                break;
            case BluetoothService.State.CONNECTING:
                light = Light.CONNECTING;
                break;
            case BluetoothService.State.CONNECTED:
                light = Light.CONNECTED;
                break;
            case BluetoothService.State.NONE:
            default:
                light = Light.NONE;
        }
        final Flasher flash = new Flasher(requireContext(), light);
        btIcon.setImageDrawable(flash.getDrawable());
        btFlash = flash;
    }

    @Getter
    @RequiredArgsConstructor
    public enum Light implements Flasher.Light {
        LISTEN(R.drawable.ic_bluetooth_24),

        CONNECTING(R.drawable.ic_bluetooth_searching_24),

        CONNECTED(R.drawable.ic_bluetooth_connected_24),

        NONE(R.drawable.ic_bluetooth_disabled_24);

        final private int drawableId;
    }

    Flasher btFlash;

    private void flashState() {
        if (btFlash != null)
            btFlash.flash(getView());
    }

    /**
     * The Handler that gets information back from the BluetoothService
     */
    private final Handler handler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case BluetoothService.MessageType.STATE_CHANGE:
                    showState(msg.arg1);
                    if (msg.arg1 <= BluetoothService.State.LISTEN) {
                        setInfo(null, null);
                        connectedDeviceName = null;
                        activityCallback.onDeviceConnected(null); // i.e. disconnected - ugly :(
                        setConnecting(false);
                    }
                    break;
                case BluetoothService.MessageType.WRITE:
                    Timber.d("WRITE_MESSAGE: %d bytes", msg.arg1);
                    break;
                case BluetoothService.MessageType.READ_CMD:
                    Timber.d("READ_COMMAND 0x%x (%d bytes)", msg.arg2, msg.arg1);
                    if (activityCallback != null) {
                        activityCallback.onReceive(msg.arg2);
                    }
                    break;
                case BluetoothService.MessageType.CODE:
                    Timber.d("CODE: %s", msg.obj);
                    btCode.setText((String) msg.obj);
                    break;
                case BluetoothService.MessageType.DEVICE_NAME:
                    connectedDeviceName = (String) msg.obj;
                    setInfo(connectedDeviceName, null);
                    activityCallback.onDeviceConnected(connectedDeviceName);
                    setConnecting(false);
                    if (mode == Mode.CLIENT) {
                        final int code = new SecureRandom().nextInt(10000);
                        bluetoothService.write(code);
                    }
                    break;
                case BluetoothService.MessageType.TOAST:
                    if (isAdded())
                        Toast.makeText(getActivity(), (String) msg.obj, Toast.LENGTH_SHORT).show();
                    break;
            }
            flashState();
        }
    };

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_ENABLE_BT) {// When the request to enable Bluetooth returns
            if (resultCode == Activity.RESULT_OK) {
                // Bluetooth is now enabled, so set up a chat session
                setupCommunication();
            } else {
                // User did not enable Bluetooth or an error occurred
                Timber.d("BT not enabled");
                if (activityCallback != null)
                    activityCallback.abort("Bluetooth is not enabled"); //TODO strings.xml
            }
        } else {
            Timber.w("Unhandled request code %d", requestCode);
        }
        flashState();
    }

    private void setConnecting(boolean enable) {
        pbConnecting.setVisibility(enable ? View.VISIBLE : View.INVISIBLE);
    }

    public void connectDevice(String address) {
        setConnecting(true);
        bluetoothService.connect(bluetoothAdapter.getRemoteDevice(address));
    }

    private void setInfo(String name, String code) {
        try {
            btName.setText(name == null ? getResources().getString(R.string.sidekick_not_connected) : name);
            btCode.setText(getResources().getString(R.string.sidekick_pin, code != null ? code : "----"));
        } catch (IllegalStateException ex) { // no context, so no strings
            // never mind
        }
    }
}
