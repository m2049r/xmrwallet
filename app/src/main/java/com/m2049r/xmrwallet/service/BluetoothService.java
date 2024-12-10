/*
 * Copyright (C) 2021 m2049r@monerujo.io
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

// mostly from BluetoothChatService https://github.com/android/connectivity-samples

package com.m2049r.xmrwallet.service;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

import lombok.Getter;
import lombok.Setter;
import timber.log.Timber;

/**
 * This class does all the work for setting up and managing Bluetooth
 * connections with other devices. It has a thread that listens for
 * incoming connections, a thread for connecting with a device, and a
 * thread for performing data transmissions when connected.
 */
public class BluetoothService {
    final static private byte[] MAGIC = "SIDEKICK".getBytes(StandardCharsets.US_ASCII);

    public interface MessageType {
        int STATE_CHANGE = 1;
        int READ = 2;
        int WRITE = 3;
        int DEVICE_NAME = 4;
        int TOAST = 5;
        int READ_CMD = 6;
        int CODE = 42;
    }

    //TODO refactor this using an enum with resource ids for messages and stuff
    public interface Toasts {
        String CONNECT_FAILED = "Unable to connect device";
        String CONNECTION_LOST = "Device connection was lost";
        int READ = 2;
        int WRITE = 3;
        int DEVICE_NAME = 4;
        int TOAST = 5;
    }

    // Constants that indicate the current connection state
    public interface State {
        int NONE = 0;       // we're doing nothing
        int LISTEN = 1;     // now listening for incoming connections
        int CONNECTING = 2; // now initiating an outgoing connection
        int CONNECTED = 3;  // now connected to a remote device
    }

    // Name for the SDP record when creating server socket
    private static final String SDP_NAME = "Monerujo";

    // Unique UUID for this application
    private static final UUID SDP_UUID = UUID.fromString("2150154b-58ce-4c58-99e3-ccfdd14bed3b");

    static final BluetoothService Instance = new BluetoothService();

    public static BluetoothService GetInstance() {
        return Instance;
    }

    public static boolean IsConnected() {
        return Instance.isConnected();
    }

    public static void Stop() {
        Instance.stop();
    }

    // Member fields
    private final BluetoothAdapter bluetoothAdapter;
    @Setter
    private Handler uiHandler;
    @Setter
    private Handler commHandler;
    private AcceptThread acceptThread;
    private ConnectThread connectThread;
    private ConnectedThread connectedThread;
    @Getter
    private int state = State.NONE;

    @Getter
    private String connectedName;
    @Getter
    private String connectedCode;

    public boolean isStarted() {
        return state != State.NONE;
    }

    public boolean isConnected() {
        return state == State.CONNECTED;
    }

    public BluetoothService() {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    /**
     * Notify UI of state changes
     */
    private synchronized void onStateChanged() {
        Timber.d("onStateChanged()  -> %s", state);
        if (uiHandler != null)
            uiHandler.obtainMessage(MessageType.STATE_CHANGE, state, -1).sendToTarget();
    }

    /**
     * Notify UI that we've connected
     */
    private synchronized void onConnected(String remoteName) {
        Timber.d("onConnected()  -> %s", remoteName);
        connectedName = remoteName;
        if (uiHandler != null)
            uiHandler.obtainMessage(MessageType.DEVICE_NAME, remoteName).sendToTarget();
    }

    /**
     * Start the service: Start AcceptThread to begin a session in listening (server) mode.
     */
    public synchronized void start() {
        Timber.d("start");
        halt = false;

        // Cancel any thread attempting to make a connection
        if (connectThread != null) {
            connectThread.cancel();
            connectThread = null;
        }

        // Cancel any thread currently running a connection
        if (connectedThread != null) {
            connectedThread.cancel();
            connectedThread = null;
        }

        // Start the thread to listen on a BluetoothServerSocket (or let it run if already running)
        if (acceptThread == null) {
            acceptThread = new AcceptThread();
            acceptThread.start();
        }

        onStateChanged();
    }

    /**
     * Start the ConnectThread to initiate a connection to a remote device.
     *
     * @param device The BluetoothDevice to connect
     */
    public synchronized void connect(BluetoothDevice device) {
        Timber.d("connect to: %s", device);

        // Cancel any thread attempting to make a connection
        if (state == State.CONNECTING) {
            if (connectThread != null) {
                connectThread.cancel();
                connectThread = null;
            }
        }

        // Cancel any thread currently running a connection
        if (connectedThread != null) {
            reconnect = true;
            connectedThread.cancel();
            connectedThread = null;
        }

        // Start the thread to connect with the given device
        connectThread = new ConnectThread(device);
        connectThread.start();

        onStateChanged();
    }

    boolean reconnect = false;
    boolean halt = false;

    /**
     * Start the ConnectedThread to begin managing a Bluetooth connection
     *
     * @param socket The BluetoothSocket on which the connection was made
     * @param device The BluetoothDevice that has been connected
     */
    public synchronized void startConnected(BluetoothSocket socket, BluetoothDevice device) {
        Timber.d("startConnected");

        // Cancel the thread that completed the connection
        if (connectThread != null) {
            connectThread.cancel();
            connectThread = null;
        }

        // Cancel any thread currently running a connection
        if (connectedThread != null) {
            connectedThread.cancel();
            connectedThread = null;
        }

        // Cancel the accept thread because we only want to connect to one device
        if (acceptThread != null) {
            acceptThread.cancel();
            acceptThread = null;
        }

        // Start the thread to manage the connection and perform transmissions
        connectedThread = new ConnectedThread(socket);
        connectedThread.start();

        onConnected(device.getName());
        onStateChanged();
    }

    /**
     * Stop all threads
     */
    public synchronized void stop() {
        Timber.d("stop");
        halt = true;

        if (connectThread != null) {
            connectThread.cancel();
            connectThread = null;
        }

        if (connectedThread != null) {
            connectedThread.cancel();
            connectedThread = null;
        }

        if (acceptThread != null) {
            acceptThread.cancel();
            acceptThread = null;
        }

        state = State.NONE;

        onStateChanged();

        setUiHandler(null);
    }

    /**
     * Write to the ConnectedThread in an unsynchronized manner
     *
     * @param out The bytes to write
     * @see ConnectedThread#write(byte[])
     */
    public boolean write(byte[] out) {
        // Synchronize a copy of the ConnectedThread
        ConnectedThread connectedThread;
        synchronized (this) {
            if (state != State.CONNECTED) return false;
            connectedThread = this.connectedThread;
        }
        // Perform the write itself unsynchronized
        return connectedThread.write(out);
    }

    public boolean write(int code) {
        // Synchronize a copy of the ConnectedThread
        ConnectedThread connectedThread;
        synchronized (this) {
            if (state != State.CONNECTED) return false;
            connectedThread = this.connectedThread;
        }

        final byte[] buffer = new byte[2];
        buffer[0] = (byte) (code >> 8);
        buffer[1] = (byte) (code & 0xff);

        connectedCode = String.format(Locale.US, "%04d", code);
        if (uiHandler != null)
            uiHandler.obtainMessage(MessageType.CODE, connectedCode).sendToTarget();

        return connectedThread.write(buffer);
    }

    public byte[] exchange(byte[] buffer) {
        // Synchronize a copy of the ConnectedThread
        ConnectedThread connectedThread;
        synchronized (this) {
            if (state != State.CONNECTED) return null; //TODO maybe exception?
            connectedThread = this.connectedThread;
        }
        CountDownLatch signal = new CountDownLatch(1);
        connectedThread.setReadSignal(signal);
        connectedThread.write(buffer);
        try {
            signal.await(); //TODO what happens when the reader is canceled?
            return connectedThread.getReadBuffer();
        } catch (InterruptedException ex) {
            Timber.d(ex);
            return null;
        }
    }

    /**
     * Indicate that the connection attempt failed and notify
     */
    private void onConnectFailed() {
        Timber.d("onConnectFailed()");
        if (uiHandler != null)
            uiHandler.obtainMessage(MessageType.TOAST, Toasts.CONNECT_FAILED).sendToTarget();

        state = State.NONE;

        // don't notify as start() notifies immediately afterwards
        // onStateChanged();

        // Start the service over to restart listening mode
        if (!halt) start();
    }

    /**
     * Indicate that the connection was lost
     */
    private void onConnectionLost() {
        Timber.d("onConnectionLost()");
        connectedName = null;
        connectedCode = null;
        if (reconnect) return;

        state = State.NONE;

        if (halt) return;

        // don't notify as start() notifies immediately afterwards
        // onStateChanged();

        // Start the service over to restart listening mode
        start();
    }

    /**
     * This thread runs while listening for incoming connections. It behaves
     * like a server-side client. It runs until a connection is accepted
     * (or until cancelled).
     */
    private class AcceptThread extends Thread {
        private final BluetoothServerSocket serverSocket;

        public AcceptThread() {
            // Create a new listening server socket
            try {
                serverSocket = bluetoothAdapter.listenUsingRfcommWithServiceRecord(SDP_NAME, SDP_UUID);
                state = BluetoothService.State.LISTEN;
            } catch (IOException ex) {
                Timber.d(ex, "listen() failed");
                throw new IllegalStateException();
            }
        }

        public void run() {
            Timber.d("BEGIN AcceptThread %s", this);

            BluetoothSocket socket;

            // Listen to the server socket if we're not connected
            while (state != BluetoothService.State.CONNECTED) {
                try {
                    // This is a blocking call and will only return on a
                    // successful connection or an exception
                    socket = serverSocket.accept();
                } catch (IOException ex) {
                    Timber.d(ex, "accept() failed"); // this also happens on socket.close()
                    break;
                }

                // If a connection was accepted
                if (socket != null) {
                    synchronized (BluetoothService.this) {
                        switch (state) {
                            case BluetoothService.State.LISTEN:
                            case BluetoothService.State.CONNECTING:
                                // Situation normal. Start the ConnectedThread.
                                startConnected(socket, socket.getRemoteDevice());
                                break;
                            case BluetoothService.State.NONE:
                            case BluetoothService.State.CONNECTED:
                                // Either not ready or already connected. Terminate new socket.
                                try {
                                    socket.close();
                                } catch (IOException ex) {
                                    Timber.d(ex, "Could not close unwanted socket");
                                }
                                break;
                        }
                    }
                }
            }
            Timber.d("END AcceptThread %s", this);
        }

        public void cancel() {
            Timber.d("cancel() %s", this);
            try {
                serverSocket.close();
            } catch (IOException ex) {
                Timber.d(ex, "close() of server failed");
            }
        }
    }

    /**
     * This thread runs while attempting to make an outgoing connection
     * with a device. It runs straight through; the connection either
     * succeeds or fails.
     */
    private class ConnectThread extends Thread {
        private final BluetoothSocket socket;
        private final BluetoothDevice device;

        public ConnectThread(BluetoothDevice device) {
            this.device = device;

            // Create a BluetoothSocket
            try {
                socket = device.createRfcommSocketToServiceRecord(SDP_UUID);
                state = BluetoothService.State.CONNECTING;
            } catch (IOException ex) {
                Timber.d(ex, "create() failed");
                throw new IllegalStateException(); //TODO really die here?
            }
        }

        public void run() {
            Timber.d("BEGIN ConnectThread");

            // Always cancel discovery because it will slow down a connection
            bluetoothAdapter.cancelDiscovery(); //TODO show & remember discovery state?

            // Make a connection to the BluetoothSocket
            try {
                // This is a blocking call and will only return on a
                // successful connection or an exception
                socket.connect(); // sometimes this fails - why?
            } catch (IOException ex) {
                try {
                    socket.close();
                } catch (IOException exClose) {
                    Timber.d(exClose, "unable to close() socket during connection failure");
                }
                onConnectFailed();
                return;
            } finally {
                // Reset the ConnectThread because we're done
                synchronized (BluetoothService.this) {
                    connectThread = null;
                }
            }

            // Start the ConnectedThread
            startConnected(socket, device);
        }

        public void cancel() {
            try {
                socket.close();
            } catch (IOException ex) {
                Timber.d(ex, "close() of connect socket failed");
            }
        }
    }

    /**
     * This thread runs during a connection with a remote device.
     * It handles all incoming and outgoing transmissions.
     */
    private class ConnectedThread extends Thread {
        private final BluetoothSocket socket;
        private final InputStream in;
        private final OutputStream out;

        private final ByteArrayOutputStream bytesIn = new ByteArrayOutputStream();
        private final ByteArrayOutputStream bytesOut = new ByteArrayOutputStream();

        private CountDownLatch readSignal = null; // TODO this needs to be a Map with correlationIds

        public void setReadSignal(CountDownLatch signal) { //TODO see above
            readSignal = signal;
            readBuffer = null;
        }

        @Getter
        private byte[] readBuffer = null;

        public ConnectedThread(BluetoothSocket bluetoothSocket) {
            Timber.d("ConnectedThread()");
            socket = bluetoothSocket;
            InputStream tmpIn = null;
            OutputStream tmpOut;

            // Get the BluetoothSocket input and output streams
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException ex) {
                Timber.d(ex, "temp sockets not created");
                if (tmpIn != null)
                    try {
                        tmpIn.close();
                    } catch (IOException exIn) {
                        Timber.d(exIn);
                    }
                throw new IllegalStateException();
            }

            in = tmpIn;
            out = tmpOut;
            state = BluetoothService.State.CONNECTED;
        }

        public void run() {
            Timber.d("BEGIN ConnectedThread %s", this);
            final byte[] buffer = new byte[4096];
            int bytesRead;

            // Keep listening to the InputStream while connected
            while (state == BluetoothService.State.CONNECTED) {
                // Protocol: "SIDEKICK"|1-byte:reserved|2-bytes:length|buffer
                try {
                    // protocol header
                    bytesRead = in.read(buffer, 0, MAGIC.length + 3);
                    int a = in.available() + bytesRead;
                    if (bytesRead != MAGIC.length + 3)
                        throw new IllegalStateException("message too short");
                    for (int i = 0; i < MAGIC.length; i++) {
                        if (buffer[i] != MAGIC[i]) throw new IllegalStateException("no MAGIC");
                    }
                    final int options = buffer[MAGIC.length]; // 0 regular message, else CODE instead of payload length
                    final int payloadLength = ((0xff & buffer[MAGIC.length + 1]) << 8) + (0xff & buffer[MAGIC.length + 2]);
                    Timber.d("READ options %d, payloadLength=%d, available=%d", options, payloadLength, a);
                    if ((options & 0x01) != 0) { // CODE
                        connectedCode = String.format(Locale.US, "%04d", payloadLength);
                        if (uiHandler != null)
                            uiHandler.obtainMessage(MessageType.CODE, connectedCode).sendToTarget();
                        continue;
                    }

                    int remainingBytes = payloadLength;
                    bytesIn.reset();
                    while (remainingBytes > 0) {
                        bytesRead = in.read(buffer, 0, Math.min(remainingBytes, buffer.length));
                        remainingBytes -= bytesRead;
                        bytesIn.write(buffer, 0, bytesRead);
                    }

                    readBuffer = bytesIn.toByteArray();
                    if (readSignal != null) { // someone is awaiting this
                        readSignal.countDown();
                    } else if (commHandler != null) { // we are the counterparty
                        final int command = readBuffer[0];
                        commHandler.obtainMessage(command, readBuffer).sendToTarget();
                        if (uiHandler != null) {
                            uiHandler.obtainMessage(MessageType.READ_CMD, readBuffer.length, command).sendToTarget();
                        }
                    } else {
                        throw new IllegalStateException("would drop a message");
                    }

                } catch (IOException ex) {
                    Timber.d(ex, "disconnected");
                    if (readSignal != null) readSignal.countDown(); // readBudder is still null
                    onConnectionLost();
                    reconnect = false;
                    break;
                }
            }
            Timber.d("END ConnectedThread %s", this);
        }

        /**
         * Write to the connected OutStream.
         * <p>
         * Protocol: "SIDEKICK"|1-byte:reserved|2-bytes:length|buffer
         *
         * @param buffer The bytes to write
         */
        public boolean write(byte[] buffer) {
            boolean sendCode = buffer.length == 2; // TODO undo this hack
            try {
                final int len = buffer.length;
                if (len > 65535) {
                    Timber.w("buffer too long %d", len);
                    return false;
                }
                bytesOut.reset();
                bytesOut.write(MAGIC);
                if (sendCode) {
                    bytesOut.write(0x01); // options bit 0 is CODE
                } else {
                    bytesOut.write(0);
                    bytesOut.write(len >> 8);
                    bytesOut.write(len & 0xff);
                }
                bytesOut.write(buffer);
                out.write(bytesOut.toByteArray());

                if (uiHandler != null) {
                    uiHandler.obtainMessage(MessageType.WRITE, buffer.length, -1).sendToTarget();
                }
            } catch (IOException ex) {
                Timber.d(ex, "Exception during write");
                return false;
                //TODO probably kill the connection if this happens?
                // but the read operation probably throws it as well, and that takes care of that!
            }
            return true;
        }

        public void cancel() {
            try {
                socket.close();
            } catch (IOException ex) {
                Timber.d(ex, "close() of connect socket failed");
            }
        }

    }

    // for direct communication from JNI
    // this should block
    static public byte[] Exchange(byte[] request) {
        Timber.d("EXCHANGE req  = %d bytes", request.length);
        final byte[] response = Instance.exchange(request);
        Timber.d("EXCHANGE resp = %d bytes", response.length);
        return response;
    }

    static public boolean Write(byte[] request) {
        return Instance.write(request);
    }
}
