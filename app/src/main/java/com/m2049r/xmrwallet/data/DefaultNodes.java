package com.m2049r.xmrwallet.data;

import lombok.AllArgsConstructor;
import lombok.Getter;

// Nodes stolen from https://moneroworld.com/#nodes

@AllArgsConstructor
public enum DefaultNodes {
    MONERUJO("nodex.monerujo.io:18081"),
    XMRTO("node.xmr.to:18081"),
    SUPPORTXMR("node.supportxmr.com:18081"),
    HASHVAULT("nodes.hashvault.pro:18081"),
    MONEROWORLD("node.moneroworld.com:18089"),
    XMRTW("opennode.xmr-tw.org:18089");

    @Getter
    private final String uri;
}
