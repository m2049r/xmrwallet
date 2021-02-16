package com.m2049r.xmrwallet.util.validator;

import lombok.Getter;

public enum BitcoinAddressType {
    BTC(Type.BTC, Type.BTC_BECH32_PREFIX),
    LTC(Type.LTC, Type.LTC_BECH32_PREFIX),
    DASH(Type.DASH, null),
    DOGE(Type.DOGE, null);

    @Getter
    private final byte[] production;
    @Getter
    private final byte[] testnet;

    @Getter
    private final String productionBech32Prefix;
    @Getter
    private final String testnetBech32Prefix;

    public boolean hasBech32() {
        return productionBech32Prefix != null;
    }

    public String getBech32Prefix(boolean testnet) {
        return testnet ? testnetBech32Prefix : productionBech32Prefix;
    }

    BitcoinAddressType(byte[][] types, String[] bech32Prefix) {
        production = types[0];
        testnet = types[1];
        if (bech32Prefix != null) {
            productionBech32Prefix = bech32Prefix[0];
            testnetBech32Prefix = bech32Prefix[1];
        } else {
            productionBech32Prefix = null;
            testnetBech32Prefix = null;
        }
    }

    // Java is silly and doesn't allow array initializers in the construction
    private static class Type {
        private static final byte[][] BTC = {{0x00, 0x05}, {0x6f, (byte) 0xc4}};
        private static final String[] BTC_BECH32_PREFIX = {"bc", "tb"};
        private static final byte[][] LTC = {{0x30, 0x05, 0x32}, {0x6f, (byte) 0xc4, 0x3a}};
        private static final String[] LTC_BECH32_PREFIX = {"ltc", "tltc"};
        private static final byte[][] DASH = {{0x4c, 0x10}, {(byte) 0x8c, 0x13}};
        private static final byte[][] DOGE = {{0x1e, 0x16}, {0x71, (byte) 0xc4}};
    }

}
