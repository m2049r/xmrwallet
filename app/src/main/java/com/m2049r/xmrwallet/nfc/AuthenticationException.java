package com.m2049r.xmrwallet.nfc;

public class AuthenticationException extends Exception{

    /**
     *
     */
    private static final long serialVersionUID = 101001L;

    public AuthenticationException(String info)
    {
        super(info);
    }

    public AuthenticationException(String info,Exception e)
    {
        super(info,e);
    }
}
