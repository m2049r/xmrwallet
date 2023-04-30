package com.theromus.sha;

import static com.theromus.utils.HexUtils.convertFrom64ToLittleEndian;
import static com.theromus.utils.HexUtils.convertFromLittleEndianTo64;
import static com.theromus.utils.HexUtils.convertToUint;
import static com.theromus.utils.HexUtils.leftRotate64;
import static java.lang.Math.min;
import static java.lang.System.arraycopy;
import static java.util.Arrays.fill;

import java.io.ByteArrayOutputStream;
import java.math.BigInteger;


/**
 * Keccak implementation.
 *
 * @author romus
 */
public class Keccak {

    private static BigInteger BIT_64 = new BigInteger("18446744073709551615");

    /**
     * Do hash.
     *
     * @param message input data
     * @param parameter keccak param
     * @return byte-array result
     */
    public byte[] getHash(final byte[] message, final Parameters parameter) {
        int[] uState = new int[200];
        int[] uMessage = convertToUint(message);


        int rateInBytes = parameter.getRate() / 8;
        int blockSize = 0;
        int inputOffset = 0;

        // Absorbing phase
        while (inputOffset < uMessage.length) {
            blockSize = min(uMessage.length - inputOffset, rateInBytes);
            for (int i = 0; i < blockSize; i++) {
                uState[i] = uState[i] ^ uMessage[i + inputOffset];
            }

            inputOffset = inputOffset + blockSize;
            if (blockSize == rateInBytes) {
                doKeccakf(uState);
                blockSize = 0;
            }
        }

        // Padding phase
        uState[blockSize] = uState[blockSize] ^ parameter.getD();
        if ((parameter.getD() & 0x80) != 0 && blockSize == (rateInBytes - 1)) {
            doKeccakf(uState);
        }

        uState[rateInBytes - 1] = uState[rateInBytes - 1] ^ 0x80;
        doKeccakf(uState);

        // Squeezing phase
        ByteArrayOutputStream byteResults = new ByteArrayOutputStream();
        int tOutputLen = parameter.getOutputLen() / 8;
        while (tOutputLen > 0) {
            blockSize = min(tOutputLen, rateInBytes);
            for (int i = 0; i < blockSize; i++) {
                byteResults.write((byte) uState[i]);
            }

            tOutputLen -= blockSize;
            if (tOutputLen > 0) {
                doKeccakf(uState);
            }
        }

        return byteResults.toByteArray();
    }

    private void doKeccakf(final int[] uState) {
        BigInteger[][] lState = new BigInteger[5][5];

        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 5; j++) {
                int[] data = new int[8];
                arraycopy(uState, 8 * (i + 5 * j), data, 0, data.length);
                lState[i][j] = convertFromLittleEndianTo64(data);
            }
        }
        roundB(lState);

        fill(uState, 0);
        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 5; j++) {
                int[] data = convertFrom64ToLittleEndian(lState[i][j]);
                arraycopy(data, 0, uState, 8 * (i + 5 * j), data.length);
            }
        }

    }

    /**
     * Permutation on the given state.
     *
     * @param state state
     */
    private void roundB(final BigInteger[][] state) {
        int LFSRstate = 1;
        for (int round = 0; round < 24; round++) {
            BigInteger[] C = new BigInteger[5];
            BigInteger[] D = new BigInteger[5];

            // θ step
            for (int i = 0; i < 5; i++) {
                C[i] = state[i][0].xor(state[i][1]).xor(state[i][2]).xor(state[i][3]).xor(state[i][4]);
            }

            for (int i = 0; i < 5; i++) {
                D[i] = C[(i + 4) % 5].xor(leftRotate64(C[(i + 1) % 5], 1));
            }

            for (int i = 0; i < 5; i++) {
                for (int j = 0; j < 5; j++) {
                    state[i][j] = state[i][j].xor(D[i]);
                }
            }

            //ρ and π steps
            int x = 1, y = 0;
            BigInteger current = state[x][y];
            for (int i = 0; i < 24; i++) {
                int tX = x;
                x = y;
                y = (2 * tX + 3 * y) % 5;

                BigInteger shiftValue = current;
                current = state[x][y];

                state[x][y] = leftRotate64(shiftValue, (i + 1) * (i + 2) / 2);
            }

            //χ step
            for (int j = 0; j < 5; j++) {
                BigInteger[] t = new BigInteger[5];
                for (int i = 0; i < 5; i++) {
                    t[i] = state[i][j];
                }

                for (int i = 0; i < 5; i++) {
                    // ~t[(i + 1) % 5]
                    BigInteger invertVal = t[(i + 1) % 5].xor(BIT_64);
                    // t[i] ^ ((~t[(i + 1) % 5]) & t[(i + 2) % 5])
                    state[i][j] = t[i].xor(invertVal.and(t[(i + 2) % 5]));
                }
            }

            //ι step
            for (int i = 0; i < 7; i++) {
                LFSRstate = ((LFSRstate << 1) ^ ((LFSRstate >> 7) * 0x71)) % 256;
                // pow(2, i) - 1
                int bitPosition = (1 << i) - 1;
                if ((LFSRstate & 2) != 0) {
                    state[0][0] = state[0][0].xor(new BigInteger("1").shiftLeft(bitPosition));
                }
            }
        }
    }

}
