/*
 *******************************************************************************
 *   BTChip Bitcoin Hardware Wallet Java API
 *   (c) 2014 BTChip - 1BTChip7VfTnrPra5jqci7ejnMguuHogTn
 *   (c) m2049r
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *   limitations under the License.
 ********************************************************************************
 */

package com.m2049r.xmrwallet.ledger;

import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;

import com.btchip.BTChipException;
import com.btchip.comm.BTChipTransport;
import com.btchip.comm.android.BTChipTransportAndroidHID;
import com.m2049r.xmrwallet.BuildConfig;
import com.m2049r.xmrwallet.util.Helper;

import java.io.IOException;

import timber.log.Timber;

public class Ledger {
    static final public boolean ENABLED = true;
    // 5:20 is same as wallet2.cpp::restore()
    static public final int LOOKAHEAD_ACCOUNTS = 5;
    static public final int LOOKAHEAD_SUBADDRESSES = 20;
    static public final String SUBADDRESS_LOOKAHEAD = LOOKAHEAD_ACCOUNTS + ":" + LOOKAHEAD_SUBADDRESSES;

    public static final int SW_OK = 0x9000;
    public static final int SW_INS_NOT_SUPPORTED = 0x6D00;
    public static final int OK[] = {SW_OK};

    public static UsbDevice findDevice(UsbManager usbManager) {
        if (!ENABLED) return null;
        return BTChipTransportAndroidHID.getDevice(usbManager);
    }

    static private Ledger Instance = null;

    static public String connect(UsbManager usbManager, UsbDevice usbDevice) throws IOException {
        if (Instance != null) {
            disconnect();
        }
        Instance = new Ledger(usbManager, usbDevice);
        return Name();
    }

    static public void disconnect() {
        // this is not synchronized so as to close immediately
        if (Instance != null) {
            Instance.close();
            Instance = null;
        }
    }

    static public boolean isConnected() {
        //TODO synchronize with connect/disconnect?
        return Instance != null;
    }

    static public String Name() {
        if (Instance != null) {
            return Instance.name;
        } else {
            return null;
        }
    }

    static public byte[] Exchange(byte[] apdu) {
        if (Instance != null) {
            Timber.d("INS: %s", Instruction.fromByte(apdu[1]));
            return Instance.exchangeRaw(apdu);
        } else {
            return null;
        }
    }

    final private BTChipTransport transport;
    final private String name;
    private int lastSW = 0;

    private Ledger(UsbManager usbManager, UsbDevice usbDevice) throws IOException {
        final BTChipTransport transport = BTChipTransportAndroidHID.open(usbManager, usbDevice);
        Timber.d("transport opened = %s", transport.toString());
        transport.setDebug(BuildConfig.DEBUG);
        this.transport = transport;
        this.name = usbDevice.getManufacturerName() + " " + usbDevice.getProductName();
        initKey();
    }

    synchronized private void close() {
        initKey(); // don't leak key after we disconnect
        transport.close();
        Timber.d("transport closed");
        lastSW = 0;
    }

    synchronized private byte[] exchangeRaw(byte[] apdu) {
        if (transport == null)
            throw new IllegalStateException("No transport (probably closed previously)");
        Timber.i("exchangeRaw %02x", apdu[1]);
        Instruction ins = Instruction.fromByte(apdu[1]);
        if (listener != null) listener.onInstructionSend(ins, apdu);
        sniffOut(ins, apdu);
        byte[] data = transport.exchange(apdu);
        if (listener != null) listener.onInstructionReceive(ins, data);
        sniffIn(data);
        return data;

    }

    private byte[] exchange(byte[] apdu) throws BTChipException {
        byte[] response = exchangeRaw(apdu);
        if (response.length < 2) {
            throw new BTChipException("Truncated response");
        }
        lastSW = ((response[response.length - 2] & 0xff) << 8) |
                response[response.length - 1] & 0xff;
        byte[] result = new byte[response.length - 2];
        System.arraycopy(response, 0, result, 0, response.length - 2);
        return result;
    }

    private byte[] exchangeCheck(byte[] apdu, int acceptedSW[]) throws BTChipException {
        byte[] response = exchange(apdu);
        if (acceptedSW == null) {
            return response;
        }
        for (int SW : acceptedSW) {
            if (lastSW == SW) {
                return response;
            }
        }
        throw new BTChipException("Invalid status", lastSW);
    }

    private byte[] exchangeApdu(byte cla, byte ins, byte p1, byte p2, byte[] data, int acceptedSW[]) throws BTChipException {
        byte[] apdu = new byte[data.length + 5];
        apdu[0] = cla;
        apdu[1] = ins;
        apdu[2] = p1;
        apdu[3] = p2;
        apdu[4] = (byte) (data.length);
        System.arraycopy(data, 0, apdu, 5, data.length);
        return exchangeCheck(apdu, acceptedSW);
    }

    private byte[] exchangeApdu(byte cla, byte ins, byte p1, byte p2, int length, int acceptedSW[]) throws BTChipException {
        byte[] apdu = new byte[5];
        apdu[0] = cla;
        apdu[1] = ins;
        apdu[2] = p1;
        apdu[3] = p2;
        apdu[4] = (byte) (length);
        return exchangeCheck(apdu, acceptedSW);
    }

    private byte[] exchangeApduSplit(byte cla, byte ins, byte p1, byte p2, byte[] data, int acceptedSW[]) throws BTChipException {
        int offset = 0;
        byte[] result = null;
        while (offset < data.length) {
            int blockLength = ((data.length - offset) > 255 ? 255 : data.length - offset);
            byte[] apdu = new byte[blockLength + 5];
            apdu[0] = cla;
            apdu[1] = ins;
            apdu[2] = p1;
            apdu[3] = p2;
            apdu[4] = (byte) (blockLength);
            System.arraycopy(data, offset, apdu, 5, blockLength);
            result = exchangeCheck(apdu, acceptedSW);
            offset += blockLength;
        }
        return result;
    }

    private byte[] exchangeApduSplit2(byte cla, byte ins, byte p1, byte p2, byte[] data, byte[] data2, int acceptedSW[]) throws BTChipException {
        int offset = 0;
        byte[] result = null;
        int maxBlockSize = 255 - data2.length;
        while (offset < data.length) {
            int blockLength = ((data.length - offset) > maxBlockSize ? maxBlockSize : data.length - offset);
            boolean lastBlock = ((offset + blockLength) == data.length);
            byte[] apdu = new byte[blockLength + 5 + (lastBlock ? data2.length : 0)];
            apdu[0] = cla;
            apdu[1] = ins;
            apdu[2] = p1;
            apdu[3] = p2;
            apdu[4] = (byte) (blockLength + (lastBlock ? data2.length : 0));
            System.arraycopy(data, offset, apdu, 5, blockLength);
            if (lastBlock) {
                System.arraycopy(data2, 0, apdu, 5 + blockLength, data2.length);
            }
            result = exchangeCheck(apdu, acceptedSW);
            offset += blockLength;
        }
        return result;
    }

    public interface Listener {
        void onInstructionSend(Instruction ins, byte[] apdu);

        void onInstructionReceive(Instruction ins, byte[] data);
    }

    Listener listener;

    static public void setListener(Listener listener) {
        if (Instance != null) {
            Instance.listener = listener;
        }
    }

    static public void unsetListener(Listener listener) {
        if ((Instance != null) && (Instance.listener == listener))
            Instance.listener = null;
    }

    // very stupid hack to extract the view key
    // without messing around with monero core code
    // NB: as all the ledger comm can be sniffed off the USB cable - there is no security issue here
    private boolean snoopKey = false;
    private byte[] key;

    private void initKey() {
        key = Helper.hexToBytes("0000000000000000000000000000000000000000000000000000000000000000");
    }

    static public String Key() {
        if (Instance != null) {
            return Helper.bytesToHex(Instance.key).toLowerCase();
        } else {
            return null;
        }
    }

    private void sniffOut(Instruction ins, byte[] apdu) {
        if (ins == Instruction.INS_GET_KEY) {
            snoopKey = (apdu[2] == 2);
        }

    }

    private void sniffIn(byte[] data) {
        // stupid hack to extract the view key
        // without messing around with monero core code
        if (snoopKey) {
            if (data.length == 34) { // 32 key + result code 9000
                long sw = ((data[data.length - 2] & 0xff) << 8) |
                        (data[data.length - 1] & 0xff);
                Timber.e("WS %d", sw);
                if (sw == SW_OK) {
                    System.arraycopy(data, 0, key, 0, 32);
                }
            }
            snoopKey = false;
        }
    }
}