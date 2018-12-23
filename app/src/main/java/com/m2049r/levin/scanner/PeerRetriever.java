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

package com.m2049r.levin.scanner;

import com.m2049r.levin.data.Bucket;
import com.m2049r.levin.data.Section;
import com.m2049r.levin.util.HexHelper;
import com.m2049r.levin.util.LittleEndianDataInputStream;
import com.m2049r.levin.util.LittleEndianDataOutputStream;
import com.m2049r.xmrwallet.data.NodeInfo;
import com.m2049r.xmrwallet.util.Helper;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;

import timber.log.Timber;

public class PeerRetriever implements Callable<PeerRetriever> {
    static final public int CONNECT_TIMEOUT = 500; //ms
    static final public int SOCKET_TIMEOUT = 500; //ms
    static final public long PEER_ID = new Random().nextLong();
    static final private byte[] HANDSHAKE = handshakeRequest().asByteArray();
    static final private byte[] FLAGS_RESP = flagsResponse().asByteArray();

    final private List<LevinPeer> peers = new ArrayList<>();

    private NodeInfo nodeInfo;
    private OnGetPeers onGetPeersCallback;

    public interface OnGetPeers {
        boolean getMorePeers();
    }

    public PeerRetriever(NodeInfo nodeInfo, OnGetPeers onGetPeers) {
        this.nodeInfo = nodeInfo;
        this.onGetPeersCallback = onGetPeers;
    }

    public NodeInfo getNodeInfo() {
        return nodeInfo;
    }

    public boolean isGood() {
        return !peers.isEmpty();
    }

    public List<LevinPeer> getPeers() {
        return peers;
    }

    public PeerRetriever call() {
        if (isGood()) // we have already been called?
            throw new IllegalStateException();
        // first check for an rpc service
        nodeInfo.findRpcService();
        if (onGetPeersCallback.getMorePeers())
            try {
                Timber.d("%s CONN", nodeInfo.getLevinSocketAddress());
                if (!connect())
                    return this;
                Bucket handshakeBucket = new Bucket(Bucket.COMMAND_HANDSHAKE_ID, HANDSHAKE);
                handshakeBucket.send(getDataOutput());

                while (true) {// wait for response (which may never come)
                    Bucket recv = new Bucket(getDataInput()); // times out after SOCKET_TIMEOUT
                    if ((recv.command == Bucket.COMMAND_HANDSHAKE_ID)
                            && (!recv.haveToReturnData)) {
                        readAddressList(recv.payloadSection);
                        return this;
                    } else if ((recv.command == Bucket.COMMAND_REQUEST_SUPPORT_FLAGS_ID)
                            && (recv.haveToReturnData)) {
                        Bucket flagsBucket = new Bucket(Bucket.COMMAND_REQUEST_SUPPORT_FLAGS_ID, FLAGS_RESP, 1);
                        flagsBucket.send(getDataOutput());
                    } else {// and ignore others
                        Timber.d("Ignored LEVIN COMMAND %d", recv.command);
                    }
                }
            } catch (IOException ex) {
            } finally {
                disconnect(); // we have what we want - byebye
                Timber.d("%s DISCONN", nodeInfo.getLevinSocketAddress());
            }
        return this;
    }

    private void readAddressList(Section section) {
        Section data = (Section) section.get("payload_data");
        int topVersion = (Integer) data.get("top_version");
        long currentHeight = (Long) data.get("current_height");
        String topId = HexHelper.bytesToHex((byte[]) data.get("top_id"));
        Timber.d("PAYLOAD_DATA %d/%d/%s", topVersion, currentHeight, topId);

        @SuppressWarnings("unchecked")
        List<Section> peerList = (List<Section>) section.get("local_peerlist_new");
        if (peerList != null) {
            for (Section peer : peerList) {
                Section adr = (Section) peer.get("adr");
                Integer type = (Integer) adr.get("type");
                if ((type == null) || (type != 1))
                    continue;
                Section addr = (Section) adr.get("addr");
                if (addr == null)
                    continue;
                Integer ip = (Integer) addr.get("m_ip");
                if (ip == null)
                    continue;
                Integer sport = (Integer) addr.get("m_port");
                if (sport == null)
                    continue;
                int port = sport;
                if (port < 0) // port is unsigned
                    port = port + 0x10000;
                InetAddress inet = HexHelper.toInetAddress(ip);
                // make sure this is an address we want to talk to (i.e. a remote address)
                if (!inet.isSiteLocalAddress() && !inet.isAnyLocalAddress()
                        && !inet.isLoopbackAddress()
                        && !inet.isMulticastAddress()
                        && !inet.isLinkLocalAddress()) {
                    peers.add(new LevinPeer(inet, port, topVersion, currentHeight, topId));
                }
            }
        }
    }

    private Socket socket = null;

    private boolean connect() {
        if (socket != null) throw new IllegalStateException();
        try {
            socket = new Socket();
            socket.connect(nodeInfo.getLevinSocketAddress(), CONNECT_TIMEOUT);
            socket.setSoTimeout(SOCKET_TIMEOUT);
        } catch (IOException ex) {
            //Timber.d(ex);
            return false;
        }
        return true;
    }

    private boolean isConnected() {
        return socket.isConnected();
    }

    private void disconnect() {
        try {
            dataInput = null;
            dataOutput = null;
            if ((socket != null) && (!socket.isClosed())) {
                socket.close();
            }
        } catch (IOException ex) {
            Timber.d(ex);
        } finally {
            socket = null;
        }
    }

    private DataOutput dataOutput = null;

    private DataOutput getDataOutput() throws IOException {
        if (dataOutput == null)
            synchronized (this) {
                if (dataOutput == null)
                    dataOutput = new LittleEndianDataOutputStream(
                            socket.getOutputStream());
            }
        return dataOutput;
    }

    private DataInput dataInput = null;

    private DataInput getDataInput() throws IOException {
        if (dataInput == null)
            synchronized (this) {
                if (dataInput == null)
                    dataInput = new LittleEndianDataInputStream(
                            socket.getInputStream());
            }
        return dataInput;
    }

    static private Section handshakeRequest() {
        Section section = new Section(); // root object

        Section nodeData = new Section();
        nodeData.add("local_time", (new Date()).getTime());
        nodeData.add("my_port", 0);
        byte[] networkId = Helper.hexToBytes("1230f171610441611731008216a1a110"); // mainnet
        nodeData.add("network_id", networkId);
        nodeData.add("peer_id", PEER_ID);
        section.add("node_data", nodeData);

        Section payloadData = new Section();
        payloadData.add("cumulative_difficulty", 1L);
        payloadData.add("current_height", 1L);
        byte[] genesisHash =
                Helper.hexToBytes("418015bb9ae982a1975da7d79277c2705727a56894ba0fb246adaabb1f4632e3");
        payloadData.add("top_id", genesisHash);
        payloadData.add("top_version", (byte) 1);
        section.add("payload_data", payloadData);
        return section;
    }

    static private Section flagsResponse() {
        Section section = new Section(); // root object
        section.add("support_flags", Bucket.P2P_SUPPORT_FLAGS);
        return section;
    }
}
