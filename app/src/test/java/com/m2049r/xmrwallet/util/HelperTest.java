package com.m2049r.xmrwallet.util;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

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

}
