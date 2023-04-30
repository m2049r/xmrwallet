/*
 * Copyright (c) 2017 m2049r et al.
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

package com.m2049r.xmrwallet.service.exchange.kraken;

import static org.junit.Assert.assertEquals;

import com.m2049r.xmrwallet.service.exchange.api.ExchangeApi;
import com.m2049r.xmrwallet.service.exchange.api.ExchangeCallback;
import com.m2049r.xmrwallet.service.exchange.api.ExchangeException;
import com.m2049r.xmrwallet.service.exchange.api.ExchangeRate;
import com.m2049r.xmrwallet.util.NetCipherHelper;

import net.jodah.concurrentunit.Waiter;

import org.json.JSONException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.concurrent.TimeoutException;

import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;


public class ExchangeRateTest {

    private MockWebServer mockWebServer;

    private ExchangeApi exchangeApi;

    private Waiter waiter;

    @Mock
    ExchangeCallback mockExchangeCallback;

    @Before
    public void setUp() throws Exception {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        waiter = new Waiter();

        MockitoAnnotations.initMocks(this);
        NetCipherHelper.Request.mockClient = new OkHttpClient();
        exchangeApi = new ExchangeApiImpl(mockWebServer.url("/"));
    }

    @After
    public void tearDown() throws Exception {
        mockWebServer.shutdown();
    }

    @Test
    public void queryExchangeRate_shouldBeGetMethod()
            throws InterruptedException, TimeoutException {

        exchangeApi.queryExchangeRate("XMR", "USD", mockExchangeCallback);

        RecordedRequest request = mockWebServer.takeRequest();
        assertEquals("GET", request.getMethod());
    }

    @Test
    public void queryExchangeRate_shouldHavePairInUrl()
            throws InterruptedException, TimeoutException {

        exchangeApi.queryExchangeRate("XMR", "USD", mockExchangeCallback);

        RecordedRequest request = mockWebServer.takeRequest();
        assertEquals("/?pair=XMRUSD", request.getPath());
    }

    @Test
    public void queryExchangeRate_wasSuccessfulShouldRespondWithRate()
            throws InterruptedException, JSONException, TimeoutException {
        final String base = "XMR";
        final String quote = "USD";
        final double rate = 100;
        MockResponse jsonMockResponse = new MockResponse().setBody(
                createMockExchangeRateResponse(base, quote, rate));
        mockWebServer.enqueue(jsonMockResponse);

        exchangeApi.queryExchangeRate(base, quote, new ExchangeCallback() {
            @Override
            public void onSuccess(final ExchangeRate exchangeRate) {
                waiter.assertEquals(exchangeRate.getBaseCurrency(), base);
                waiter.assertEquals(exchangeRate.getQuoteCurrency(), quote);
                waiter.assertEquals(exchangeRate.getRate(), rate);
                waiter.resume();
            }

            @Override
            public void onError(final Exception e) {
                waiter.fail(e);
                waiter.resume();
            }
        });
        waiter.await();
    }

    @Test
    public void queryExchangeRate_wasNotSuccessfulShouldCallOnError()
            throws InterruptedException, JSONException, TimeoutException {
        mockWebServer.enqueue(new MockResponse().setResponseCode(500));
        exchangeApi.queryExchangeRate("XMR", "USD", new ExchangeCallback() {
            @Override
            public void onSuccess(final ExchangeRate exchangeRate) {
                waiter.fail();
                waiter.resume();
            }

            @Override
            public void onError(final Exception e) {
                waiter.assertTrue(e instanceof ExchangeException);
                waiter.assertTrue(((ExchangeException) e).getCode() == 500);
                waiter.resume();
            }

        });
        waiter.await();
    }

    @Test
    public void queryExchangeRate_unknownAssetShouldCallOnError()
            throws InterruptedException, JSONException, TimeoutException {
        mockWebServer.enqueue(new MockResponse().
                setResponseCode(200).
                setBody("{\"error\":[\"EQuery:Unknown asset pair\"]}"));
        exchangeApi.queryExchangeRate("XMR", "ABC", new ExchangeCallback() {
            @Override
            public void onSuccess(final ExchangeRate exchangeRate) {
                waiter.fail();
                waiter.resume();
            }

            @Override
            public void onError(final Exception e) {
                waiter.assertTrue(e instanceof ExchangeException);
                ExchangeException ex = (ExchangeException) e;
                waiter.assertTrue(ex.getCode() == 200);
                waiter.assertEquals(ex.getErrorMsg(), "EQuery:Unknown asset pair");
                waiter.resume();
            }

        });
        waiter.await();
    }

    static public String createMockExchangeRateResponse(final String base, final String quote, final double rate) {
        return "{\n" +
                "   \"error\":[],\n" +
                "   \"result\":{\n" +
                "       \"X" + base + "Z" + quote + "\":{\n" +
                "           \"a\":[\"" + rate + "\",\"322\",\"322.000\"],\n" +
                "           \"b\":[\"" + rate + "\",\"76\",\"76.000\"],\n" +
                "           \"c\":[\"" + rate + "\",\"2.90000000\"],\n" +
                "           \"v\":[\"4559.03962053\",\"5231.33235586\"],\n" +
                "           \"p\":[\"" + rate + "\",\"" + rate + "\"],\n" +
                "           \"t\":[801,1014],\n" +
                "           \"l\":[\"" + (rate * 0.8) + "\",\"" + rate + "\"],\n" +
                "           \"h\":[\"" + (rate * 1.2) + "\",\"" + rate + "\"],\n" +
                "           \"o\":\"" + rate + "\"\n" +
                "       }\n" +
                "   }\n" +
                "}";
    }
}
