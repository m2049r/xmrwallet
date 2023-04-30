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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.m2049r.xmrwallet.data.UserNotes;

import org.junit.Test;

public class UserNoteTest {

    @Test
    public void createFromTxNote_noNote() {
        UserNotes userNotes = new UserNotes("{xmrto-iyrpxU,0.009BTC,mjn127C5wRQCULksMYMFHLp9UTdQuCfbZ9}");
        assertTrue("xmrto".equals(userNotes.xmrtoTag));
        assertTrue("iyrpxU".equals(userNotes.xmrtoKey));
        assertTrue("0.009".equals(userNotes.xmrtoAmount));
        assertTrue("mjn127C5wRQCULksMYMFHLp9UTdQuCfbZ9".equals(userNotes.xmrtoDestination));
        assertTrue(userNotes.note.isEmpty());
    }

    @Test
    public void createFromTxNote_withNote() {
        UserNotes userNotes = new UserNotes("{xmrto-iyrpxU,0.009BTC,mjn127C5wRQCULksMYMFHLp9UTdQuCfbZ9} aNote");
        assertTrue("xmrto".equals(userNotes.xmrtoTag));
        assertTrue("iyrpxU".equals(userNotes.xmrtoKey));
        assertTrue("0.009".equals(userNotes.xmrtoAmount));
        assertTrue("mjn127C5wRQCULksMYMFHLp9UTdQuCfbZ9".equals(userNotes.xmrtoDestination));
        assertTrue("aNote".equals(userNotes.note));
    }

    @Test
    public void createFromTxNote_withNoteNoSpace() {
        UserNotes userNotes = new UserNotes("{xmrto-iyrpxU,0.009BTC,mjn127C5wRQCULksMYMFHLp9UTdQuCfbZ9}aNote");
        assertTrue("xmrto".equals(userNotes.xmrtoTag));
        assertTrue("iyrpxU".equals(userNotes.xmrtoKey));
        assertTrue("0.009".equals(userNotes.xmrtoAmount));
        assertTrue("mjn127C5wRQCULksMYMFHLp9UTdQuCfbZ9".equals(userNotes.xmrtoDestination));
        assertTrue("aNote".equals(userNotes.note));
    }

    @Test
    public void createFromTxNote_brokenB() {
        String brokenNote = "{xmrto-iyrpxU,0.009BTC,mjn127C5wRQCULksMYMFHLp9UTdQuCfbZ9";
        UserNotes userNotes = new UserNotes(brokenNote);
        assertNull(userNotes.xmrtoKey);
        assertNull(userNotes.xmrtoAmount);
        assertNull(userNotes.xmrtoDestination);
        assertTrue(brokenNote.equals(userNotes.note));
    }

    @Test
    public void createFromTxNote_normal() {
        String aNote = "aNote";
        UserNotes userNotes = new UserNotes(aNote);
        assertNull(userNotes.xmrtoKey);
        assertNull(userNotes.xmrtoAmount);
        assertNull(userNotes.xmrtoDestination);
        assertTrue(aNote.equals(userNotes.note));
    }

    @Test
    public void createFromTxNote_empty() {
        String aNote = "";
        UserNotes userNotes = new UserNotes(aNote);
        assertNull(userNotes.xmrtoKey);
        assertNull(userNotes.xmrtoAmount);
        assertNull(userNotes.xmrtoDestination);
        assertTrue(aNote.equals(userNotes.note));
    }

    @Test
    public void createFromTxNote_null() {
        UserNotes userNotes = new UserNotes(null);
        assertNull(userNotes.xmrtoKey);
        assertNull(userNotes.xmrtoAmount);
        assertNull(userNotes.xmrtoDestination);
        assertNotNull(userNotes.note);
        assertTrue(userNotes.note.isEmpty());
    }
}
