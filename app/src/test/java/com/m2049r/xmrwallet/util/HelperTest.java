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

import org.junit.Test;

public class HelperTest {

    @Test
    public void testMinus() {
        long l = -1000000000000L;
        String s = Helper.getDisplayAmount(l, 5);
        System.out.println(s);
        assertTrue(s.equals("-1.00"));
    }

    @Test
    public void testTen() {
        long l = 10L;
        String s = Helper.getDisplayAmount(l);
        System.out.println(s);
        assertTrue(s.equals("0.00000000001"));
    }

    @Test
    public void testZero() {
        long l = 0L;
        String s = Helper.getDisplayAmount(l);
        System.out.println(s);
        assertTrue(s.equals("0.00"));
    }

    @Test
    public void testG() {
        long l = 1234567891234L;
        String s = Helper.getDisplayAmount(l);
        System.out.println(s);
        assertTrue(s.equals("1.234567891234"));
    }

    @Test
    public void testG2() {
        long l = 1000000000000L;
        String s = Helper.getDisplayAmount(l);
        System.out.println(s);
        assertTrue(s.equals("1.00"));
    }


    @Test
    public void testE() {
        long l = 1234567891234L;
        String s = Helper.getDisplayAmount(l, 4);
        System.out.println(s);
        assertTrue(s.equals("1.2346"));
    }

    @Test
    public void testF() {
        long l = 1234567891234L;
        String s = Helper.getDisplayAmount(l, 12);
        System.out.println(s);
        assertTrue(s.equals("1.234567891234"));
    }

    @Test
    public void testH() {
        long l = 1004567891234L;
        String s = Helper.getDisplayAmount(l, 2);
        System.out.println(s);
        assertTrue(s.equals("1.00"));
    }

    @Test
    public void testGetDisplayAmount() {
        assertTrue("0.000000051".equals(Helper.getDisplayAmount(0.000000051)));
        assertTrue("1.000000051".equals(Helper.getDisplayAmount(1.000000051)));
        assertTrue("1.0".equals(Helper.getDisplayAmount(1d)));
        assertTrue("0.0".equals(Helper.getDisplayAmount(0d)));
    }
}
