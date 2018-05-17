package com.m2049r.xmrwallet.nfc;

import java.math.BigInteger;

/**
 * ISO/IEC FCD 14443-3
 * Name   : "CRC-A"    Width  : 16    Poly   : 1021    Init   : C6C6    RefIn  : True    RefOut : True    XorOut : 0000    Check  : BF05
 * for more reference: http://wg8.de/wg8n1496_17n3613_Ballot_FCD14443-3.pdf
 */

public class CRC16 {

    private int width = 16;
    private long poly = (new BigInteger("1021", 16)).longValue();
    private long initialValue = (new BigInteger("c6c6", 16)).longValue();
    private boolean refIn = true;
    private boolean refOut = true;
    private long xorOut = (new BigInteger("0", 16)).longValue();
    ;
    private long[] table;
    private long value = 0L;
    private long length = 0L;
    private long topBit;
    private long maskAllBits;
    private long maskHelp;

    public static void main(String[] args) {
        try {
            CRC16 test = new CRC16();
            String hexString = Long.toHexString(test.getCRC16(ThreeDES.defaultKey));
            byte[] reversedBytes = TagUtil.reverse(TagUtil.hexStringToBytes(hexString));
            System.out.println(TagUtil.bytesToHexString(reversedBytes));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public long getCRC16(byte[] bytes) {
        init();
        exec(bytes, 0, bytes.length);
        long result = this.value;
        if (this.refIn != this.refOut) {
            result = reverse(result, this.width);
        }
        return (result ^ this.xorOut) & this.maskAllBits;
    }


    private void init() {
        this.topBit = 1L << this.width - 1;
        this.maskAllBits = -1L >>> 64 - this.width;
        this.maskHelp = this.maskAllBits >>> 8;
        initTable();
        length = 0L;
        this.value = this.initialValue;
        if (this.refIn) {
            this.value = reverse(this.value, this.width);
        }

    }

    private void exec(byte[] bytes, int start, int length) {
        for (int i = start; i < length + start; ++i) {
            this.exec(bytes[i]);
        }
    }

    private void exec(byte var1) {
        int var2;
        if (this.refIn) {
            var2 = (int) (this.value ^ (long) var1) & 255;
            this.value >>>= 8;
            this.value &= this.maskHelp;
        } else {
            var2 = (int) (this.value >>> this.width - 8 ^ (long) var1) & 255;
            this.value <<= 8;
        }
        this.value ^= this.table[var2];
        ++length;
    }


    private void initTable() {
        this.table = new long[256];
        for (int i = 0; i < 256; ++i) {
            long var1 = (long) i;
            if (refIn) {
                var1 = reverse(var1, 8);
            }
            var1 <<= this.width - 8;
            for (int j = 0; j < 8; ++j) {
                boolean var3 = (var1 & this.topBit) != 0L;
                var1 <<= 1;
                if (var3) {
                    var1 ^= this.poly;
                }
            }
            if (refIn) {
                var1 = reverse(var1, this.width);
            }
            this.table[i] = var1 & this.maskAllBits;
        }
    }

    private static long reverse(long var, int width) {
        long temp = 0L;
        for (int i = 0; i < width; ++i) {
            temp <<= 1;
            temp |= var & 1L;
            var >>>= 1;
        }
        return var << width | temp;
    }

}
