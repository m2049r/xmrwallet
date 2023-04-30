/*
 * Copyright (c) 2017 m2049r
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

package com.m2049r.xmrwallet.util;

import static org.junit.Assert.assertTrue;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

// all ranges go back 5 days

public class RestoreHeightTest {
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void pre2014() {
        assertTrue(getHeight("2013-12-01") == 0);
        assertTrue(getHeight("1958-12-01") == 0);
    }

    @Test
    public void zero() {
        assertTrue(getHeight("2014-04-27") == 0);
    }

    @Test
    public void notZero() {
        assertTrue(getHeight("2014-05-07") > 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void notDateA() {
        getHeight("2013-13-04");
    }

    @Test(expected = IllegalArgumentException.class)
    public void notDateB() {
        getHeight("2013-13-01-");
    }

    @Test(expected = IllegalArgumentException.class)
    public void notDateC() {
        getHeight("x013-13-01");
    }

    @Test(expected = IllegalArgumentException.class)
    public void notDateD() {
        getHeight("2013-12-41");
    }

    @Test
    public void test201709() {
        // getHeight() returns blockheight of < two days ago
        assertTrue(isInRange(getHeight("2017-09-01"), 1383957, 1387716));
        assertTrue(isInRange(getHeight("2017-09-05"), 1386967, 1390583));
        assertTrue(isInRange(getHeight("2017-09-21"), 1398492, 1402068));
    }

    @Test
    public void test20160324() { // blocktime changed from 1 minute to 2 minutes on this day
        assertTrue(isInRange(getHeight("2016-03-23"), 998955, 1006105));
        assertTrue(isInRange(getHeight("2016-03-24"), 1000414, 1007486));
        assertTrue(isInRange(getHeight("2016-03-25"), 1001800, 1008900));
        assertTrue(isInRange(getHeight("2016-03-26"), 1003243, 1009985));
        assertTrue(isInRange(getHeight("2016-03-27"), 1004694, 1010746));
    }

    @Test
    public void test2014() {
        assertTrue(isInRange(getHeight("2014-04-26"), 0, 8501));
        assertTrue(isInRange(getHeight("2014-05-09"), 20289, 28311));
        assertTrue(isInRange(getHeight("2014-05-17"), 32608, 40075));
        assertTrue(isInRange(getHeight("2014-05-30"), 52139, 59548));
    }

    @Test
    public void test2015() {
        assertTrue(isInRange(getHeight("2015-01-26"), 397914, 405055));
        assertTrue(isInRange(getHeight("2015-08-13"), 682595, 689748));
    }

    @Test
    public void test2016() {
        assertTrue(isInRange(getHeight("2016-01-26"), 918313, 925424));
        assertTrue(isInRange(getHeight("2016-08-13"), 1107244, 1110793));
    }

    @Test
    public void test2017() {
        assertTrue(isInRange(getHeight("2017-01-26"), 1226806, 1230402));
        assertTrue(isInRange(getHeight("2017-08-13"), 1370264, 1373854));
        assertTrue(isInRange(getHeight("2017-08-31"), 1383254, 1386967));
        assertTrue(isInRange(getHeight("2017-06-09"), 1323288, 1326884));
    }

    @Test
    public void post201802() {
        assertTrue(isInRange(getHeight("2018-02-19"), 1507579, 1511127));
    }

    @Test
    public void postFuture() {
        long b_20180701 = 1606715L;
        long b_20190108 = b_20180701 + 720 * (31 + 31 + 30 + 31 + 30 + 31 + 7);
        assertTrue(isInRange(getHeight("2019-01-08"), b_20190108 - 720 * 5, b_20190108));
    }


    private boolean isInRange(long n, long min, long max) {
        if (n > max) return false;
        return n >= min;
    }

    private long getHeight(String date) {
        return RestoreHeight.getInstance().getHeight(date);
    }
}
