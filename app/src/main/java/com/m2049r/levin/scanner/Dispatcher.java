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

import com.m2049r.xmrwallet.data.NodeInfo;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import timber.log.Timber;

public class Dispatcher implements PeerRetriever.OnGetPeers {
    static final public int NUM_THREADS = 50;
    static final public int MAX_PEERS = 1000;
    static final public long MAX_TIME = 30000000000L; //30 seconds

    private int peerCount = 0;
    final private Set<NodeInfo> knownNodes = new HashSet<>(); // set of nodes to test
    final private Set<NodeInfo> rpcNodes = new HashSet<>(); // set of RPC nodes we like
    final private ExecutorService exeService = Executors.newFixedThreadPool(NUM_THREADS);

    public interface Listener {
        void onGet(NodeInfo nodeInfo);
    }

    private Listener listener;

    public Dispatcher(Listener listener) {
        this.listener = listener;
    }

    public Set<NodeInfo> getRpcNodes() {
        return rpcNodes;
    }

    public int getPeerCount() {
        return peerCount;
    }

    public boolean getMorePeers() {
        return peerCount < MAX_PEERS;
    }

    public void awaitTermination(int nodesToFind) {
        try {
            final long t = System.nanoTime();
            while (!jobs.isEmpty()) {
                try {
                    Timber.d("Remaining jobs %d", jobs.size());
                    final PeerRetriever retrievedPeer = jobs.poll().get();
                    if (retrievedPeer.isGood() && getMorePeers())
                        retrievePeers(retrievedPeer);
                    final NodeInfo nodeInfo = retrievedPeer.getNodeInfo();
                    Timber.d("Retrieved %s", nodeInfo);
                    if ((nodeInfo.isValid() || nodeInfo.isFavourite())) {
                        nodeInfo.setDefaultName();
                        rpcNodes.add(nodeInfo);
                        Timber.d("RPC: %s", nodeInfo);
                        // the following is not totally correct but it works (otherwise we need to
                        // load much more before filtering - but we don't have time
                        if (listener != null) listener.onGet(nodeInfo);
                        if (rpcNodes.size() >= nodesToFind) {
                            Timber.d("are we done here?");
                            filterRpcNodes();
                            if (rpcNodes.size() >= nodesToFind) {
                                Timber.d("we're done here");
                                break;
                            }
                        }
                    }
                    if (System.nanoTime() - t > MAX_TIME) break; // watchdog
                } catch (ExecutionException ex) {
                    Timber.d(ex); // tell us about it and continue
                }
            }
        } catch (InterruptedException ex) {
            Timber.d(ex);
        } finally {
            Timber.d("Shutting down!");
            exeService.shutdownNow();
            try {
                exeService.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
            } catch (InterruptedException ex) {
                Timber.d(ex);
            }
        }
        filterRpcNodes();
    }

    static final public int HEIGHT_WINDOW = 1;

    private boolean testHeight(long height, long consensus) {
        return (height >= (consensus - HEIGHT_WINDOW))
                && (height <= (consensus + HEIGHT_WINDOW));
    }

    private long calcConsensusHeight() {
        Timber.d("Calc Consensus height from %d nodes", rpcNodes.size());
        final Map<Long, Integer> nodeHeights = new TreeMap<Long, Integer>();
        for (NodeInfo info : rpcNodes) {
            if (!info.isValid()) continue;
            Integer h = nodeHeights.get(info.getHeight());
            if (h == null)
                h = 0;
            nodeHeights.put(info.getHeight(), h + 1);
        }
        long consensusHeight = 0;
        long consensusCount = 0;
        for (Map.Entry<Long, Integer> entry : nodeHeights.entrySet()) {
            final long entryHeight = entry.getKey();
            int count = 0;
            for (long i = entryHeight - HEIGHT_WINDOW; i <= entryHeight + HEIGHT_WINDOW; i++) {
                Integer v = nodeHeights.get(i);
                if (v == null)
                    v = 0;
                count += v;
            }
            if (count >= consensusCount) {
                consensusCount = count;
                consensusHeight = entryHeight;
            }
            Timber.d("%d - %d/%d", entryHeight, count, entry.getValue());
        }
        return consensusHeight;
    }

    private void filterRpcNodes() {
        long consensus = calcConsensusHeight();
        Timber.d("Consensus Height = %d for %d nodes", consensus, rpcNodes.size());
        for (Iterator<NodeInfo> iter = rpcNodes.iterator(); iter.hasNext(); ) {
            NodeInfo info = iter.next();
            // don't remove favourites
            if (!info.isFavourite()) {
                if (!testHeight(info.getHeight(), consensus)) {
                    iter.remove();
                    Timber.d("Removed %s", info);
                }
            }
        }
    }

    // TODO: does this NEED to be a ConcurrentLinkedDeque?
    private ConcurrentLinkedDeque<Future<PeerRetriever>> jobs = new ConcurrentLinkedDeque<>();

    private void retrievePeer(NodeInfo nodeInfo) {
        if (knownNodes.add(nodeInfo)) {
            Timber.d("\t%d:%s", knownNodes.size(), nodeInfo);
            jobs.add(exeService.submit(new PeerRetriever(nodeInfo, this)));
            peerCount++; // jobs.size() does not perform well
        }
    }

    private void retrievePeers(PeerRetriever peer) {
        for (LevinPeer levinPeer : peer.getPeers()) {
            if (getMorePeers())
                retrievePeer(new NodeInfo(levinPeer));
            else
                break;
        }
    }

    public void seedPeers(Collection<NodeInfo> seedNodes) {
        for (NodeInfo node : seedNodes) {
            if (node.isFavourite()) {
                rpcNodes.add(node);
                if (listener != null) listener.onGet(node);
            }
            retrievePeer(node);
        }
    }
}
