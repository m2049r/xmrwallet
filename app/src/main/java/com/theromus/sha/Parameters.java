package com.theromus.sha;

/**
 * The parameters defining the standard FIPS 202.
 *
 * @author romus
 */
public enum Parameters {
    KECCAK_224 (1152, 0x01, 224),
    KECCAK_256 (1088, 0x01, 256),
    KECCAK_384 (832, 0x01, 384),
    KECCAK_512 (576, 0x01, 512),

    SHA3_224 (1152, 0x06, 224),
    SHA3_256 (1088, 0x06, 256),
    SHA3_384 (832, 0x06, 384),
    SHA3_512 (576, 0x06, 512),

    SHAKE128 (1344, 0x1F, 256),
    SHAKE256 (1088, 0x1F, 512);

    private final int rate;

    /**
     * Delimited suffix.
     */
    public final int d;

    /**
     * Output length (bits).
     */
    public final int outputLen;

    Parameters(int rate, int d, int outputLen) {
        this.rate = rate;
        this.d = d;
        this.outputLen = outputLen;
    }

    public int getRate() {
        return rate;
    }

    public int getD() {
        return d;
    }

    public int getOutputLen() {
        return outputLen;
    }
}
