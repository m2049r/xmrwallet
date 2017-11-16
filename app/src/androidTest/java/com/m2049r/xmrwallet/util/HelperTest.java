package com.m2049r.xmrwallet.util;

import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.SmallTest;

import com.m2049r.xmrwallet.model.Wallet;

import junit.framework.Assert;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Created by bruno on 14/11/2017.
 */

@RunWith(AndroidJUnit4.class)
@SmallTest
public class HelperTest {

    /**
     *
     */
    @Test
    public void checkValidAddressExample_1() {
        // random generated address by https://xmr.llcoins.net/addresstests.html
        String publicAddr = "4GKacjHuTGeReA7PvxPhehDswu3RwpnAqeJV2kdU3MLMNUr4Lt9pxcRiWU2E6BoDReADhCPtHfvNFh1YVNLFoZE5YtaSMc8MSbGGudRftK";
        Assert.assertTrue(Wallet.isAddressValid(publicAddr, false));
    }
}
