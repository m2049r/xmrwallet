/*
 * Based on
 * https://stackoverflow.com/a/19943894
 *
 * Curve parameters from
 * https://en.bitcoin.it/wiki/Secp256k1
 *
 * Copyright (c) 2019 m2049r
 * Copyright (c) 2013 ChiaraHsieh
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

package com.m2049r.xmrwallet.util.ledger;

import java.math.BigInteger;
import java.security.spec.ECPoint;

public class ECsecp256k1 {
    static private final BigInteger TWO = new BigInteger("2");
    static private final BigInteger THREE = new BigInteger("3");
    static public final BigInteger p = new BigInteger("FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFEFFFFFC2F", 16);
    static public final BigInteger a = new BigInteger("0000000000000000000000000000000000000000000000000000000000000000", 16);
    static public final BigInteger b = new BigInteger("0000000000000000000000000000000000000000000000000000000000000007", 16);
    static public final BigInteger n = new BigInteger("FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFEBAAEDCE6AF48A03BBFD25E8CD0364141", 16);
    static public final ECPoint G = new ECPoint(
            new BigInteger("79BE667EF9DCBBAC55A06295CE870B07029BFCDB2DCE28D959F2815B16F81798", 16),
            new BigInteger("483ADA7726A3C4655DA4FBFC0E1108A8FD17B448A68554199C47D08FFB10D4B8", 16));

    public static ECPoint scalmult(BigInteger kin, ECPoint P) {
        ECPoint R = ECPoint.POINT_INFINITY, S = P;
        BigInteger k = kin.mod(n); // not necessary b/c that's how curves work
        int length = k.bitLength();
        byte[] binarray = new byte[length];
        for (int i = 0; i <= length - 1; i++) {
            binarray[i] = k.mod(TWO).byteValue();
            k = k.divide(TWO);
        }
        for (int i = length - 1; i >= 0; i--) {
            // i should start at length-1 not -2 because the MSB of binary may not be 1
            R = doublePoint(R);
            if (binarray[i] == 1)
                R = addPoint(R, S);
        }
        return R;
    }

    public static ECPoint addPoint(ECPoint r, ECPoint s) {
        if (r.equals(s))
            return doublePoint(r);
        else if (r.equals(ECPoint.POINT_INFINITY))
            return s;
        else if (s.equals(ECPoint.POINT_INFINITY))
            return r;
        BigInteger slope = (r.getAffineY().subtract(s.getAffineY()))
                .multiply(r.getAffineX().subtract(s.getAffineX()).modInverse(p));
        BigInteger Xout = (slope.modPow(TWO, p).subtract(r.getAffineX())).subtract(s.getAffineX()).mod(p);
        BigInteger Yout = s.getAffineY().negate().add(slope.multiply(s.getAffineX().subtract(Xout))).mod(p);
        return new ECPoint(Xout, Yout);
    }

    public static ECPoint doublePoint(ECPoint r) {
        if (r.equals(ECPoint.POINT_INFINITY))
            return r;
        BigInteger slope = (r.getAffineX().pow(2)).multiply(THREE).add(a)
                .multiply((r.getAffineY().multiply(TWO)).modInverse(p));
        BigInteger Xout = slope.pow(2).subtract(r.getAffineX().multiply(TWO)).mod(p);
        BigInteger Yout = (r.getAffineY().negate()).add(slope.multiply(r.getAffineX().subtract(Xout))).mod(p);
        return new ECPoint(Xout, Yout);
    }
}