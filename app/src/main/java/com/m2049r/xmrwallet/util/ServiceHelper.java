package com.m2049r.xmrwallet.util;

import com.m2049r.xmrwallet.model.NetworkType;
import com.m2049r.xmrwallet.model.WalletManager;
import com.m2049r.xmrwallet.service.exchange.api.ExchangeApi;

import okhttp3.HttpUrl;

public class ServiceHelper {
    public static String ASSET = null;

    static public HttpUrl getXmrToBaseUrl() {
        if ((WalletManager.getInstance() == null)
                || (WalletManager.getInstance().getNetworkType() != NetworkType.NetworkType_Mainnet)) {
            throw new IllegalStateException("Only mainnet not supported");
        } else {
            return HttpUrl.parse("https://sideshift.ai/api/v1/");
        }
    }

    static public ExchangeApi getExchangeApi() {
        return new com.m2049r.xmrwallet.service.exchange.krakenFiat.ExchangeApiImpl();
    }

    static private final ExchangeApi[] fiatApis = new ExchangeApi[]{
            new com.m2049r.xmrwallet.service.exchange.ecb.ExchangeApiImpl(),
            new com.m2049r.xmrwallet.service.exchange.yadio.ExchangeApiImpl()};

    static public ExchangeApi getFiatApi(String symbol) {
        return (symbol.length() == 3) ? fiatApis[0] : fiatApis[1];
    }
}
