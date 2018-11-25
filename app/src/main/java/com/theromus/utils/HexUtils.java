package com.theromus.utils;


import java.io.ByteArrayOutputStream;
import java.math.BigInteger;

/**
 * Hex-utils.
 *
 * @author romus
 */
public class HexUtils {

    private static final byte[] ENCODE_BYTE_TABLE = {
            (byte) '0', (byte) '1', (byte) '2', (byte) '3', (byte) '4', (byte) '5', (byte) '6', (byte) '7',
            (byte) '8', (byte) '9', (byte) 'a', (byte) 'b', (byte) 'c', (byte) 'd', (byte) 'e', (byte) 'f'
    };

    /**
     * Convert byte array to unsigned array.
     *
     * @param data byte array
     * @return unsigned array
     */
    public static int[] convertToUint(final byte[] data) {
        int[] converted = new int[data.length];
        for (int i = 0; i < data.length; i++) {
            converted[i] = data[i] & 0xFF;
        }

        return converted;
    }

    /**
     * Convert LE to 64-bit value (unsigned long).
     *
     * @param data data
     * @return 64-bit value (unsigned long)
     */
    public static BigInteger convertFromLittleEndianTo64(final int[] data) {
        BigInteger uLong = new BigInteger("0");
        for (int i = 0; i < 8; i++) {
            uLong = uLong.add(new BigInteger(Integer.toString(data[i])).shiftLeft(8 * i));
        }

        return uLong;
    }

    /**
     * Convert 64-bit (unsigned long) value to LE.
     *
     * @param uLong 64-bit value (unsigned long)
     * @return LE
     */
    public static int[] convertFrom64ToLittleEndian(final BigInteger uLong) {
        int[] data = new int[8];
        BigInteger mod256 = new BigInteger("256");
        for (int i = 0; i < 8; i++) {
            data[i] = uLong.shiftRight((8 * i)).mod(mod256).intValue();
        }

        return data;
    }

    /**
     * Bitwise rotate left.
     *
     * @param value  unsigned long value
     * @param rotate rotate left
     * @return result
     */
    public static BigInteger leftRotate64(final BigInteger value, final int rotate) {
        BigInteger lp = value.shiftRight(64 - (rotate % 64));
        BigInteger rp = value.shiftLeft(rotate % 64);

        return lp.add(rp).mod(new BigInteger("18446744073709551616"));
    }

    /**
     * Convert bytes to string.
     *
     * @param data bytes array
     * @return string
     */
    public static String convertBytesToString(final byte[] data) {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        for (int i = 0; i < data.length; i++) {
            int uVal = data[i] & 0xFF;

            buffer.write(ENCODE_BYTE_TABLE[(uVal >>> 4)]);
            buffer.write(ENCODE_BYTE_TABLE[uVal & 0xF]);
        }

        return new String(buffer.toByteArray());
    }

}
