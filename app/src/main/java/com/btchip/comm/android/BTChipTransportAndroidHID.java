/*
 *******************************************************************************
 *   BTChip Bitcoin Hardware Wallet Java API
 *   (c) 2014 BTChip - 1BTChip7VfTnrPra5jqci7ejnMguuHogTn
 *   (c) 2018 m2049r
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

package com.btchip.comm.android;

import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.hardware.usb.UsbRequest;

import com.btchip.comm.BTChipTransport;
import com.btchip.comm.LedgerHelper;
import com.btchip.utils.Dump;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;

import timber.log.Timber;

public class BTChipTransportAndroidHID implements BTChipTransport {

    public static UsbDevice getDevice(UsbManager manager) {
        HashMap<String, UsbDevice> deviceList = manager.getDeviceList();
        for (UsbDevice device : deviceList.values()) {
            Timber.d("%04X:%04X %s, %s", device.getVendorId(), device.getProductId(), device.getManufacturerName(), device.getProductName());
            if (device.getVendorId() == VID) {
                final int deviceProductId = device.getProductId();
                for (int pid : PID_HIDS) {
                    if (deviceProductId == pid)
                        return device;
                }
            }
        }
        return null;
    }

    public static BTChipTransport open(UsbManager manager, UsbDevice device) throws IOException {
        UsbDeviceConnection connection = manager.openDevice(device);
        if (connection == null) throw new IOException("Device not connected");
        // Must only be called once permission is granted (see http://developer.android.com/reference/android/hardware/usb/UsbManager.html)
        // Important if enumerating, rather than being awaken by the intent notification
        UsbInterface dongleInterface = device.getInterface(0);
        UsbEndpoint in = null;
        UsbEndpoint out = null;
        for (int i = 0; i < dongleInterface.getEndpointCount(); i++) {
            UsbEndpoint tmpEndpoint = dongleInterface.getEndpoint(i);
            if (tmpEndpoint.getDirection() == UsbConstants.USB_DIR_IN) {
                in = tmpEndpoint;
            } else {
                out = tmpEndpoint;
            }
        }
        connection.claimInterface(dongleInterface, true);
        return new BTChipTransportAndroidHID(connection, dongleInterface, in, out);
    }

    private static final int VID = 0x2C97;
    private static final int[] PID_HIDS = {0x0001, 0x0004, 0x0005};

    private UsbDeviceConnection connection;
    private UsbInterface dongleInterface;
    private UsbEndpoint in;
    private UsbEndpoint out;
    private byte transferBuffer[];
    private boolean debug;

    public BTChipTransportAndroidHID(UsbDeviceConnection connection, UsbInterface dongleInterface, UsbEndpoint in, UsbEndpoint out) {
        this.connection = connection;
        this.dongleInterface = dongleInterface;
        this.in = in;
        this.out = out;
        transferBuffer = new byte[HID_BUFFER_SIZE];
    }

    @Override
    public byte[] exchange(byte[] command) {
        ByteArrayOutputStream response = new ByteArrayOutputStream();
        byte[] responseData = null;
        int offset = 0;
        if (debug) {
            Timber.d("=> %s", Dump.dump(command));
        }
        command = LedgerHelper.wrapCommandAPDU(LEDGER_DEFAULT_CHANNEL, command, HID_BUFFER_SIZE);
        UsbRequest requestOut = new UsbRequest();
        requestOut.initialize(connection, out);
        while (offset != command.length) {
            int blockSize = (command.length - offset > HID_BUFFER_SIZE ? HID_BUFFER_SIZE : command.length - offset);
            System.arraycopy(command, offset, transferBuffer, 0, blockSize);
            requestOut.queue(ByteBuffer.wrap(transferBuffer), HID_BUFFER_SIZE);
            connection.requestWait();
            offset += blockSize;
        }
        requestOut.close();
        ByteBuffer responseBuffer = ByteBuffer.allocate(HID_BUFFER_SIZE);
        UsbRequest requestIn = new UsbRequest();
        requestIn.initialize(connection, in);
        while ((responseData = LedgerHelper.unwrapResponseAPDU(LEDGER_DEFAULT_CHANNEL, response.toByteArray(), HID_BUFFER_SIZE)) == null) {
            responseBuffer.clear();
            requestIn.queue(responseBuffer, HID_BUFFER_SIZE);
            connection.requestWait();
            responseBuffer.rewind();
            responseBuffer.get(transferBuffer, 0, HID_BUFFER_SIZE);
            response.write(transferBuffer, 0, HID_BUFFER_SIZE);
        }
        requestIn.close();
        if (debug) {
            Timber.d("<= %s", Dump.dump(responseData));
        }
        return responseData;
    }

    @Override
    public void close() {
        connection.releaseInterface(dongleInterface);
        connection.close();
    }

    @Override
    public void setDebug(boolean debugFlag) {
        this.debug = debugFlag;
    }

    private static final int HID_BUFFER_SIZE = 64;
    private static final int LEDGER_DEFAULT_CHANNEL = 1;
    private static final int SW1_DATA_AVAILABLE = 0x61;
}
