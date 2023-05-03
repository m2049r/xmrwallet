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

package com.m2049r.xmrwallet.service.exchange.ecb;

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
        exchangeApi = new ExchangeApiImpl( mockWebServer.url("/"));
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

        MockResponse jsonMockResponse = new MockResponse().setBody(createMockExchangeRateResponse());
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
        final double rate = 1.6246;

        MockResponse jsonMockResponse = new MockResponse().setBody(createMockExchangeRateResponse());
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
        final double rate = 16.3978;

        MockResponse jsonMockResponse = new MockResponse().setBody(createMockExchangeRateResponse());
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
        MockResponse jsonMockResponse = new MockResponse().setBody(createMockExchangeRateResponse());
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
                waiter.assertTrue(ex.getCode() == 404);
                waiter.assertEquals(ex.getErrorMsg(), "Currency not supported: ABC");
                waiter.resume();
            }

        });
        waiter.await();
    }

    static public String createMockExchangeRateResponse() {
        return "<gesmes:Envelope xmlns:gesmes=\"http://www.gesmes.org/xml/2002-08-01\" xmlns=\"http://www.ecb.int/vocabulary/2002-08-01/eurofxref\"><script xmlns=\"\"/>\n" +
                "\t<gesmes:subject>Reference rates</gesmes:subject>\n" +
                "\t<gesmes:Sender>\n" +
                "\t\t<gesmes:name>European Central Bank</gesmes:name>\n" +
                "\t</gesmes:Sender>\n" +
                "\t<Cube>\n" +
                "\t\t<Cube time=\"2019-10-11\">\n" +
                "\t\t\t<Cube currency=\"USD\" rate=\"1.1043\"/>\n" +
                "\t\t\t<Cube currency=\"JPY\" rate=\"119.75\"/>\n" +
                "\t\t\t<Cube currency=\"BGN\" rate=\"1.9558\"/>\n" +
                "\t\t\t<Cube currency=\"CZK\" rate=\"25.807\"/>\n" +
                "\t\t\t<Cube currency=\"DKK\" rate=\"7.4688\"/>\n" +
                "\t\t\t<Cube currency=\"GBP\" rate=\"0.87518\"/>\n" +
                "\t\t\t<Cube currency=\"HUF\" rate=\"331.71\"/>\n" +
                "\t\t\t<Cube currency=\"PLN\" rate=\"4.3057\"/>\n" +
                "\t\t\t<Cube currency=\"RON\" rate=\"4.7573\"/>\n" +
                "\t\t\t<Cube currency=\"SEK\" rate=\"10.8448\"/>\n" +
                "\t\t\t<Cube currency=\"CHF\" rate=\"1.1025\"/>\n" +
                "\t\t\t<Cube currency=\"ISK\" rate=\"137.70\"/>\n" +
                "\t\t\t<Cube currency=\"NOK\" rate=\"10.0375\"/>\n" +
                "\t\t\t<Cube currency=\"HRK\" rate=\"7.4280\"/>\n" +
                "\t\t\t<Cube currency=\"RUB\" rate=\"70.8034\"/>\n" +
                "\t\t\t<Cube currency=\"TRY\" rate=\"6.4713\"/>\n" +
                "\t\t\t<Cube currency=\"AUD\" rate=\"1.6246\"/>\n" +
                "\t\t\t<Cube currency=\"BRL\" rate=\"4.5291\"/>\n" +
                "\t\t\t<Cube currency=\"CAD\" rate=\"1.4679\"/>\n" +
                "\t\t\t<Cube currency=\"CNY\" rate=\"7.8417\"/>\n" +
                "\t\t\t<Cube currency=\"HKD\" rate=\"8.6614\"/>\n" +
                "\t\t\t<Cube currency=\"IDR\" rate=\"15601.55\"/>\n" +
                "\t\t\t<Cube currency=\"ILS\" rate=\"3.8673\"/>\n" +
                "\t\t\t<Cube currency=\"INR\" rate=\"78.4875\"/>\n" +
                "\t\t\t<Cube currency=\"KRW\" rate=\"1308.61\"/>\n" +
                "\t\t\t<Cube currency=\"MXN\" rate=\"21.3965\"/>\n" +
                "\t\t\t<Cube currency=\"MYR\" rate=\"4.6220\"/>\n" +
                "\t\t\t<Cube currency=\"NZD\" rate=\"1.7419\"/>\n" +
                "\t\t\t<Cube currency=\"PHP\" rate=\"56.927\"/>\n" +
                "\t\t\t<Cube currency=\"SGD\" rate=\"1.5177\"/>\n" +
                "\t\t\t<Cube currency=\"THB\" rate=\"33.642\"/>\n" +
                "\t\t\t<Cube currency=\"ZAR\" rate=\"16.3978\"/>\n" +
                "\t\t</Cube>\n" +
                "\t</Cube>\n" +
                "</gesmes:Envelope>";
    }
}
