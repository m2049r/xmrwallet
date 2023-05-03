/*
 * Copyright (c) 2018 m2049r
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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.Map;

public class OpenAliasHelperTest {

    private final static String MONERUJO = "oa1:xmr recipient_address=4AdkPJoxn7JCvAby9szgnt93MSEwdnxdhaASxbTBm6x5dCwmsDep2UYN4FhStDn5i11nsJbpU7oj59ahg8gXb1Mg3viqCuk; recipient_name=Monerujo Development; tx_description=Donation to Monerujo Core Team;";

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void asset() {
        Map<String, String> attrs = OpenAliasHelper.parse(MONERUJO);
        assertNotNull(attrs);
        assertTrue("xmr".equals(attrs.get(OpenAliasHelper.OA1_ASSET)));
    }

    @Test
    public void quotedSemicolon() {
        Map<String, String> attrs = OpenAliasHelper.parse("oa1:xmr abc=\";\";def=99;");
        assertNotNull(attrs);
        assertTrue(attrs.get("abc").equals(";"));
        assertTrue(attrs.get("def").equals("99"));
    }

    @Test
    public void space() {
        Map<String, String> attrs = OpenAliasHelper.parse("oa1:xmr abc=\\ ;def=99;");
        assertNotNull(attrs);
        assertTrue(attrs.get("abc").equals(" "));
        assertTrue(attrs.get("def").equals("99"));
    }

    @Test
    public void quotaedSpace() {
        Map<String, String> attrs = OpenAliasHelper.parse("oa1:xmr abc=\" \";def=99;");
        assertNotNull(attrs);
        assertTrue(attrs.get("abc").equals(" "));
        assertTrue(attrs.get("def").equals("99"));
    }

    @Test
    public void quotes() {
        Map<String, String> attrs;
        attrs = OpenAliasHelper.parse("oa1:xmr abc=\"def\";");
        assertNotNull(attrs);
        assertTrue(attrs.get("abc").equals("def"));
    }

    @Test
    public void simple() {
        Map<String, String> attrs;
        attrs = OpenAliasHelper.parse("oa1:xmr abc=def;");
        assertNotNull(attrs);
        assertTrue(attrs.get("abc").equals("def"));
    }

    @Test
    public void duplex() {
        Map<String, String> attrs;
        attrs = OpenAliasHelper.parse("oa1:xmr abc=def;ghi=jkl;");
        assertNotNull(attrs);
        assertTrue(attrs.get("abc").equals("def"));
        assertTrue(attrs.get("ghi").equals("jkl"));
    }

    @Test
    public void duplexQ() {
        Map<String, String> attrs;
        attrs = OpenAliasHelper.parse("oa1:xmr abc=def;ghi=jkl;");
        assertNotNull(attrs);
        assertTrue(attrs.get("abc").equals("def"));
        assertTrue(attrs.get("ghi").equals("jkl"));
    }

    @Test
    public void simple_unterminated() {
        Map<String, String> attrs;
        attrs = OpenAliasHelper.parse("oa1:xmr abc=def;ghi=jkl");
        assertNull(attrs);
    }

    @Test
    public void unterminatedQuotes() {
        Map<String, String> attrs;
        attrs = OpenAliasHelper.parse("oa1:xmr abc=\"def;ghi=jkl;");
        assertNull(attrs);
    }

    @Test
    public void quoteEnd() {
        Map<String, String> attrs;
        attrs = OpenAliasHelper.parse("oa1:xmr abc=def\";ghi=jkl;");
        assertNotNull(attrs);
        assertTrue(attrs.get("abc").equals("def\""));
        assertTrue(attrs.get("ghi").equals("jkl"));
    }

    @Test
    public void quoteMiddle() {
        Map<String, String> attrs;
        attrs = OpenAliasHelper.parse("oa1:xmr abc=d\"ef;ghi=jkl;");
        assertNotNull(attrs);
        assertTrue(attrs.get("abc").equals("d\"ef"));
        assertTrue(attrs.get("ghi").equals("jkl"));
    }

    @Test
    public void quoteMultiple() {
        Map<String, String> attrs;
        attrs = OpenAliasHelper.parse("oa1:xmr abc=d\"ef\";ghi=jkl;");
        assertNotNull(attrs);
        assertTrue(attrs.get("abc").equals("d\"ef\""));
        assertTrue(attrs.get("ghi").equals("jkl"));
    }

    @Test
    public void quoteMalformedValue() {
        Map<String, String> attrs;
        attrs = OpenAliasHelper.parse("oa1:xmr abc=d\"e;f\";ghi=jkl;");
        assertNull(attrs);
    }

    @Test
    public void quotedSemicolon2() {
        Map<String, String> attrs;
        attrs = OpenAliasHelper.parse("oa1:xmr abc=\"d;ef\";ghi=jkl;");
        assertNotNull(attrs);
        assertTrue(attrs.get("abc").equals("d;ef"));
        assertTrue(attrs.get("ghi").equals("jkl"));
    }

    @Test
    public void quotedQuote() {
        Map<String, String> attrs;
        attrs = OpenAliasHelper.parse("oa1:xmr abc=\"d\"ef\";ghi=jkl;");
        assertNotNull(attrs);
        assertTrue(attrs.get("abc").equals("d\"ef"));
        assertTrue(attrs.get("ghi").equals("jkl"));
    }
}
