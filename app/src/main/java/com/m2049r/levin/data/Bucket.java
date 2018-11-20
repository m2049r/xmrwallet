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

package com.m2049r.levin.data;

import com.m2049r.levin.util.HexHelper;
import com.m2049r.levin.util.LevinReader;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class Bucket {

    // constants copied from monero p2p & epee

    public final static int P2P_COMMANDS_POOL_BASE = 1000;
    public final static int COMMAND_HANDSHAKE_ID = P2P_COMMANDS_POOL_BASE + 1;
    public final static int COMMAND_TIMED_SYNC_ID = P2P_COMMANDS_POOL_BASE + 2;
    public final static int COMMAND_PING_ID = P2P_COMMANDS_POOL_BASE + 3;
    public final static int COMMAND_REQUEST_STAT_INFO_ID = P2P_COMMANDS_POOL_BASE + 4;
    public final static int COMMAND_REQUEST_NETWORK_STATE_ID = P2P_COMMANDS_POOL_BASE + 5;
    public final static int COMMAND_REQUEST_PEER_ID_ID = P2P_COMMANDS_POOL_BASE + 6;
    public final static int COMMAND_REQUEST_SUPPORT_FLAGS_ID = P2P_COMMANDS_POOL_BASE + 7;

    public final static long LEVIN_SIGNATURE = 0x0101010101012101L; // Bender's nightmare

    public final static long LEVIN_DEFAULT_MAX_PACKET_SIZE = 100000000; // 100MB by default

    public final static int LEVIN_PACKET_REQUEST = 0x00000001;
    public final static int LEVIN_PACKET_RESPONSE = 0x00000002;

    public final static int LEVIN_PROTOCOL_VER_0 = 0;
    public final static int LEVIN_PROTOCOL_VER_1 = 1;

    public final static int LEVIN_OK = 0;
    public final static int LEVIN_ERROR_CONNECTION = -1;
    public final static int LEVIN_ERROR_CONNECTION_NOT_FOUND = -2;
    public final static int LEVIN_ERROR_CONNECTION_DESTROYED = -3;
    public final static int LEVIN_ERROR_CONNECTION_TIMEDOUT = -4;
    public final static int LEVIN_ERROR_CONNECTION_NO_DUPLEX_PROTOCOL = -5;
    public final static int LEVIN_ERROR_CONNECTION_HANDLER_NOT_DEFINED = -6;
    public final static int LEVIN_ERROR_FORMAT = -7;

    public final static int P2P_SUPPORT_FLAG_FLUFFY_BLOCKS = 0x01;
    public final static int P2P_SUPPORT_FLAGS = P2P_SUPPORT_FLAG_FLUFFY_BLOCKS;

    final private long signature;
    final private long cb;
    final public boolean haveToReturnData;
    final public int command;
    final public int returnCode;
    final private int flags;
    final private int protcolVersion;
    final byte[] payload;

    final public Section payloadSection;

    // create a request
    public Bucket(int command, byte[] payload) throws IOException {
        this.signature = LEVIN_SIGNATURE;
        this.cb = payload.length;
        this.haveToReturnData = true;
        this.command = command;
        this.returnCode = 0;
        this.flags = LEVIN_PACKET_REQUEST;
        this.protcolVersion = LEVIN_PROTOCOL_VER_1;
        this.payload = payload;
        payloadSection = LevinReader.readPayload(payload);
    }

    // create a response
    public Bucket(int command, byte[] payload, int rc) throws IOException {
        this.signature = LEVIN_SIGNATURE;
        this.cb = payload.length;
        this.haveToReturnData = false;
        this.command = command;
        this.returnCode = rc;
        this.flags = LEVIN_PACKET_RESPONSE;
        this.protcolVersion = LEVIN_PROTOCOL_VER_1;
        this.payload = payload;
        payloadSection = LevinReader.readPayload(payload);
    }

    public Bucket(DataInput in) throws IOException {
        signature = in.readLong();
        cb = in.readLong();
        haveToReturnData = in.readBoolean();
        command = in.readInt();
        returnCode = in.readInt();
        flags = in.readInt();
        protcolVersion = in.readInt();

        if (signature == Bucket.LEVIN_SIGNATURE) {
            if (cb > Integer.MAX_VALUE)
                throw new IllegalArgumentException();
            payload = new byte[(int) cb];
            in.readFully(payload);
        } else
            throw new IllegalStateException();
        payloadSection = LevinReader.readPayload(payload);
    }

    public Section getPayloadSection() {
        return payloadSection;
    }

    public void send(DataOutput out) throws IOException {
        out.writeLong(signature);
        out.writeLong(cb);
        out.writeBoolean(haveToReturnData);
        out.writeInt(command);
        out.writeInt(returnCode);
        out.writeInt(flags);
        out.writeInt(protcolVersion);
        out.write(payload);
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("sig:  ").append(signature).append("\n");
        sb.append("cb:   ").append(cb).append("\n");
        sb.append("call: ").append(haveToReturnData).append("\n");
        sb.append("cmd:  ").append(command).append("\n");
        sb.append("rc:   ").append(returnCode).append("\n");
        sb.append("flags:").append(flags).append("\n");
        sb.append("proto:").append(protcolVersion).append("\n");
        sb.append(HexHelper.bytesToHex(payload)).append("\n");
        sb.append(payloadSection.toString());
        return sb.toString();
    }
}
