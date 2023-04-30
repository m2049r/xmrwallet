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

package com.m2049r.xmrwallet.service.shift.sideshift;

import static org.junit.Assert.assertEquals;

import com.m2049r.xmrwallet.service.shift.ShiftCallback;
import com.m2049r.xmrwallet.service.shift.ShiftError;
import com.m2049r.xmrwallet.service.shift.ShiftException;
import com.m2049r.xmrwallet.service.shift.sideshift.api.RequestQuote;
import com.m2049r.xmrwallet.service.shift.sideshift.api.SideShiftApi;
import com.m2049r.xmrwallet.service.shift.sideshift.network.SideShiftApiImpl;
import com.m2049r.xmrwallet.util.NetCipherHelper;
import com.m2049r.xmrwallet.util.ServiceHelper;

import net.jodah.concurrentunit.Waiter;

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

public class SideShiftApiRequestQuoteTest {

    private MockWebServer mockWebServer;

    private SideShiftApi xmrToApi;

    private Waiter waiter;

    @Mock
    ShiftCallback<RequestQuote> mockXmrToCallback;

    @Before
    public void setUp() throws Exception {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        waiter = new Waiter();

        MockitoAnnotations.initMocks(this);
        NetCipherHelper.Request.mockClient = new OkHttpClient();
        xmrToApi = new SideShiftApiImpl(mockWebServer.url("/"));
        ServiceHelper.ASSET="btc"; // all tests run with BTC
    }

    @After
    public void tearDown() throws Exception {
        mockWebServer.shutdown();
        ServiceHelper.ASSET = null;
    }

    @Test
    public void requestQuote_shouldBePostMethod()
            throws InterruptedException {

        xmrToApi.requestQuote(1.01, mockXmrToCallback);

        RecordedRequest request = mockWebServer.takeRequest();
        assertEquals("POST", request.getMethod());
    }

    @Test
    public void requestQuote_shouldBeContentTypeJson()
            throws InterruptedException {

        xmrToApi.requestQuote(1.01, mockXmrToCallback);

        RecordedRequest request = mockWebServer.takeRequest();
        assertEquals("application/json; charset=utf-8", request.getHeader("Content-Type"));
    }

    @Test
    public void requestQuote_shouldContainValidBody() throws InterruptedException {

        final String validBody = "{\"settleAmount\":\"1.01\",\"settleMethod\":\"btc\",\"depositMethod\":\"xmr\"}";
        xmrToApi.requestQuote(1.01, mockXmrToCallback);

        RecordedRequest request = mockWebServer.takeRequest();
        String body = request.getBody().readUtf8();
        assertEquals(validBody, body);
    }

    @Test
    public void requestQuote_wasSuccessfulShouldRespondWithQuote()
            throws TimeoutException, InterruptedException {
        final double btcAmount = 1.01;
        final double rate = 0.00397838;
        final String uuid = "66fc0749-f320-4361-b0fb-7873576cba67";
        MockResponse jsonMockResponse = new MockResponse().setBody(
                createMockRequestQuoteResponse(btcAmount, rate, uuid));
        mockWebServer.enqueue(jsonMockResponse);

        xmrToApi.requestQuote(btcAmount, new ShiftCallback<RequestQuote>() {
            @Override
            public void onSuccess(final RequestQuote quote) {
                waiter.assertEquals(quote.getXmrAmount(), btcAmount / rate);
                waiter.assertEquals(quote.getBtcAmount(), btcAmount);
                waiter.assertEquals(quote.getId(), uuid);
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
    public void requestQuote_wasNotSuccessfulShouldCallOnError()
            throws TimeoutException, InterruptedException {
        mockWebServer.enqueue(new MockResponse().setResponseCode(500));
        xmrToApi.requestQuote(1.01, new ShiftCallback<RequestQuote>() {
            @Override
            public void onSuccess(final RequestQuote quote) {
                waiter.fail();
                waiter.resume();
            }

            @Override
            public void onError(final Exception e) {
                waiter.assertTrue(e instanceof ShiftException);
                waiter.assertTrue(((ShiftException) e).getCode() == 500);
                waiter.resume();
            }

        });
        waiter.await();
    }

    @Test
    public void requestQuote_AmountTooHighShouldCallOnError()
            throws TimeoutException, InterruptedException {
        mockWebServer.enqueue(new MockResponse().
                setResponseCode(500).
                setBody("{\"error\":{\"message\":\"Amount too high\"}}"));
        xmrToApi.requestQuote(1000, new ShiftCallback<RequestQuote>() {
            @Override
            public void onSuccess(final RequestQuote quote) {
                waiter.fail();
                waiter.resume();
            }

            @Override
            public void onError(final Exception e) {
                waiter.assertTrue(e instanceof ShiftException);
                ShiftException xmrEx = (ShiftException) e;
                waiter.assertTrue(xmrEx.getCode() == 500);
                waiter.assertNotNull(xmrEx.getError());
                waiter.assertEquals(xmrEx.getError().getErrorType(), ShiftError.Error.SERVICE);
                waiter.assertEquals(xmrEx.getError().getErrorMsg(), "Amount too high");
                waiter.resume();
            }

        });
        waiter.await();
    }

    private String createMockRequestQuoteResponse(final double btcAmount, final double rate,
                                                  final String uuid) {

        return "{\n" +
                "\"createdAt\":\"2021-02-04T13:09:14.484Z\",\n" +
                "\"settleAmount\":\"" + btcAmount + "\",\n" +
                "\"depositMethod\":\"xmr\",\n" +
                "\"expiresAt\":\"2021-02-04T13:24:14.484Z\",\n" +
                "\"id\":\"" + uuid + "\",\n" +
                "\"rate\":\"" + rate + "\",\n" +
                "\"depositAmount\":\"" + (btcAmount / rate) + "\",\n" +
                "\"settleMethod\":\"btc\"\n" +
                "}";
    }
}