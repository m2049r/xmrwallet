package com.m2049r.levin.scanner;

import java.net.InetAddress;
import java.net.InetSocketAddress;

public class LevinPeer {
    final public InetSocketAddress socketAddress;
    final public int version;
    final public long height;
    final public String top;


    public InetSocketAddress getSocketAddress() {
        return socketAddress;
    }

    LevinPeer(InetAddress address, int port, int version, long height, String top) {
        this.socketAddress = new InetSocketAddress(address, port);
        this.version = version;
        this.height = height;
        this.top = top;
    }
}
