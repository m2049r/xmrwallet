/*
 * Copyright (c) 2017-2023 m2049r et al.
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

package com.m2049r.xmrwallet.service.exchange.yadio;

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

import java.util.Date;
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

        exchangeApi.queryExchangeRate("EUR", "USD", mockExchangeCallback);

        RecordedRequest request = mockWebServer.takeRequest();
        assertEquals("GET", request.getMethod());
    }

    @Test
    public void queryExchangeRate_shouldBeEUR()
            throws InterruptedException, TimeoutException {

        exchangeApi.queryExchangeRate("CHF", "USD", new ExchangeCallback() {
            @Override
            public void onSuccess(final ExchangeRate exchangeRate) {
                waiter.fail();
                waiter.resume();
            }

            @Override
            public void onError(final Exception e) {
                waiter.assertTrue(e instanceof IllegalArgumentException);
                waiter.resume();
            }

        });
        waiter.await();
    }


    @Test
    public void queryExchangeRate_shouldBeOneForEur()
            throws InterruptedException, TimeoutException {

        exchangeApi.queryExchangeRate("EUR", "EUR", new ExchangeCallback() {
            @Override
            public void onSuccess(final ExchangeRate exchangeRate) {
                waiter.assertEquals(1.0, exchangeRate.getRate());
                waiter.resume();
            }

            @Override
            public void onError(final Exception e) {
                waiter.fail();
                waiter.resume();
            }

        });
        waiter.await();
    }

    @Test
    public void queryExchangeRate_wasSuccessfulShouldRespondWithUsdRate()
            throws InterruptedException, JSONException, TimeoutException {
        final String base = "EUR";
        final String quote = "USD";
        final double rate = 1.1043;

        MockResponse jsonMockResponse = new MockResponse().setBody(createMockExchangeRateResponse(quote, rate));
        mockWebServer.enqueue(jsonMockResponse);

        exchangeApi.queryExchangeRate(base, quote, new ExchangeCallback() {
            @Override
            public void onSuccess(final ExchangeRate exchangeRate) {
                waiter.assertEquals(base, exchangeRate.getBaseCurrency());
                waiter.assertEquals(quote, exchangeRate.getQuoteCurrency());
                waiter.assertEquals(rate, exchangeRate.getRate());
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
    public void queryExchangeRate_wasSuccessfulShouldRespondWithAudRate()
            throws InterruptedException, JSONException, TimeoutException {
        final String base = "EUR";
        final String quote = "AUD";
        final double rate = 99.114651;

        MockResponse jsonMockResponse = new MockResponse().setBody(createMockExchangeRateResponse(quote, rate));
        mockWebServer.enqueue(jsonMockResponse);

        exchangeApi.queryExchangeRate(base, quote, new ExchangeCallback() {
            @Override
            public void onSuccess(final ExchangeRate exchangeRate) {
                waiter.assertEquals(base, exchangeRate.getBaseCurrency());
                waiter.assertEquals(quote, exchangeRate.getQuoteCurrency());
                waiter.assertEquals(rate, exchangeRate.getRate());
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
    public void queryExchangeRate_wasSuccessfulShouldRespondWithZarRate()
            throws InterruptedException, JSONException, TimeoutException {
        final String base = "EUR";
        final String quote = "ZAR";
        final double rate = 99.114651;

        MockResponse jsonMockResponse = new MockResponse().setBody(createMockExchangeRateResponse(quote, rate));
        mockWebServer.enqueue(jsonMockResponse);

        exchangeApi.queryExchangeRate(base, quote, new ExchangeCallback() {
            @Override
            public void onSuccess(final ExchangeRate exchangeRate) {
                waiter.assertEquals(base, exchangeRate.getBaseCurrency());
                waiter.assertEquals(quote, exchangeRate.getQuoteCurrency());
                waiter.assertEquals(rate, exchangeRate.getRate());
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
        exchangeApi.queryExchangeRate("EUR", "USD", new ExchangeCallback() {
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
        MockResponse jsonMockResponse = new MockResponse().setBody(createMockExchangeRateResponse("X", 0));
        mockWebServer.enqueue(jsonMockResponse);
        exchangeApi.queryExchangeRate("EUR", "ABC", new ExchangeCallback() {
            @Override
            public void onSuccess(final ExchangeRate exchangeRate) {
                waiter.fail();
                waiter.resume();
            }

            @Override
            public void onError(final Exception e) {
                waiter.assertTrue(e instanceof ExchangeException);
                ExchangeException ex = (ExchangeException) e;
                waiter.assertEquals(ex.getCode(), 200);
                waiter.assertEquals(ex.getErrorMsg(), "currency not found");
                waiter.resume();
            }
        });
        waiter.await();
    }

    static public String createMockExchangeRateResponse(String quote, double rate) {
        if (!quote.equals("X")) {
            return "{\"request\":{\"amount\":1,\"from\":\"EUR\",\"to\":\"" + quote + "\"},\"result\":" + rate + ",\"rate\":" + rate + ",\"timestamp\":" + new Date().getTime() + "}";
        }
        return "{\"error\":\"currency not found\"}";
    }
}
