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
import com.m2049r.xmrwallet.service.shift.ShiftException;
import com.m2049r.xmrwallet.service.shift.sideshift.api.QueryOrderParameters;
import com.m2049r.xmrwallet.service.shift.sideshift.api.SideShiftApi;
import com.m2049r.xmrwallet.service.shift.sideshift.network.SideShiftApiImpl;
import com.m2049r.xmrwallet.util.NetCipherHelper;

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

public class SideShiftApiOrderParameterTest {

    private MockWebServer mockWebServer;

    private SideShiftApi xmrToApi;

    private Waiter waiter;

    @Mock
    ShiftCallback<QueryOrderParameters> mockParametersXmrToCallback;

    @Before
    public void setUp() throws Exception {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        waiter = new Waiter();

        MockitoAnnotations.initMocks(this);
        NetCipherHelper.Request.mockClient = new OkHttpClient();
        xmrToApi = new SideShiftApiImpl(mockWebServer.url("/"));
    }

    @After
    public void tearDown() throws Exception {
        mockWebServer.shutdown();
    }

    @Test
    public void orderParameter_shouldBeGetMethod()
            throws InterruptedException {

        xmrToApi.queryOrderParameters(mockParametersXmrToCallback);

        RecordedRequest request = mockWebServer.takeRequest();
        assertEquals("GET", request.getMethod());
    }

    @Test
    public void orderParameter_wasSuccessfulShouldRespondWithParameters()
            throws TimeoutException, InterruptedException {
        final double rate = 0.015537;
        final double upperLimit = 20.0;
        final double lowerLimit = 0.001;

        MockResponse jsonMockResponse = new MockResponse().setBody(
                createMockOrderParameterResponse(rate, upperLimit, lowerLimit));
        mockWebServer.enqueue(jsonMockResponse);

        xmrToApi.queryOrderParameters(new ShiftCallback<QueryOrderParameters>() {
            @Override
            public void onSuccess(final QueryOrderParameters orderParameter) {
                waiter.assertEquals(orderParameter.getLowerLimit(), lowerLimit);
                waiter.assertEquals(orderParameter.getUpperLimit(), upperLimit);
                waiter.assertEquals(orderParameter.getPrice(), rate);
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
    public void orderParameter_wasNotSuccessfulShouldCallOnError()
            throws TimeoutException, InterruptedException {
        mockWebServer.enqueue(new MockResponse().setResponseCode(500));
        xmrToApi.queryOrderParameters(new ShiftCallback<QueryOrderParameters>() {
            @Override
            public void onSuccess(final QueryOrderParameters orderParameter) {
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
    public void orderParameter_SettleMethodInvalidShouldCallOnError()
            throws TimeoutException, InterruptedException {
        mockWebServer.enqueue(new MockResponse().
                setResponseCode(500).
                setBody("{\"error\":{\"message\":\"Settle method not found\"}}"));
        xmrToApi.queryOrderParameters(new ShiftCallback<QueryOrderParameters>() {
            @Override
            public void onSuccess(final QueryOrderParameters orderParameter) {
                waiter.fail();
                waiter.resume();
            }

            @Override
            public void onError(final Exception e) {
                waiter.assertTrue(e instanceof ShiftException);
                ShiftException xmrEx = (ShiftException) e;
                waiter.assertTrue(xmrEx.getCode() == 500);
                waiter.assertNotNull(xmrEx.getError());
                waiter.assertEquals(xmrEx.getError().getErrorMsg(), "Settle method not found");
                waiter.resume();
            }

        });
        waiter.await();
    }

    private String createMockOrderParameterResponse(
            final double rate,
            final double upperLimit,
            final double lowerLimit) {
        return "{\n" +
                "  \"rate\": \"" + rate + "\",\n" +
                "  \"min\": \"" + lowerLimit + "\",\n" +
                "  \"max\": \"" + upperLimit + "\"\n" +
                "}";
    }
}
