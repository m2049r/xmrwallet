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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.junit.Assert.assertTrue;


public class RestoreHeightTest {
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void pre2014() {
        assertTrue(RestoreHeight.getInstance().getHeight("2013-12-01") == 0);
        assertTrue(RestoreHeight.getInstance().getHeight("1958-12-01") == 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void notDateA() {
        RestoreHeight.getInstance().getHeight("2013-13-04");
    }

    @Test(expected = IllegalArgumentException.class)
    public void notDateB() {
        RestoreHeight.getInstance().getHeight("2013-13-01-");
    }

    @Test(expected = IllegalArgumentException.class)
    public void notDateC() {
        RestoreHeight.getInstance().getHeight("x013-13-01");
    }

    @Test(expected = IllegalArgumentException.class)
    public void notDateD() {
        RestoreHeight.getInstance().getHeight("2013-12-41");
    }

    @Test
    public void test20170901() {
        assertTrue(RestoreHeight.getInstance().getHeight("2017-09-01") == 1366680);
        assertTrue(RestoreHeight.getInstance().getHeight("2017-09-05") == 1389120);
        assertTrue(RestoreHeight.getInstance().getHeight("2017-09-21") == 1389120);
    }
}
