/*
    This file is part of RouteConverter.

    RouteConverter is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    RouteConverter is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with RouteConverter; if not, write to the Free Software
    Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA

    Copyright (C) 2007 Christian Pesch. All Rights Reserved.
*/

package slash.navigation.download;

import org.junit.Test;

import static org.junit.Assert.*;

public class StateTest {

    @Test
    public void testExactly13States() {
        assertEquals(13, State.values().length);
    }

    @Test
    public void testQueuedIsFirst() {
        assertEquals(0, State.Queued.ordinal());
    }

    @Test
    public void testSucceededOrdinal() {
        assertEquals(State.Succeeded, State.valueOf("Succeeded"));
    }

    @Test
    public void testFailedOrdinal() {
        assertEquals(State.Failed, State.valueOf("Failed"));
    }

    @Test
    public void testAllStatesHaveNonNullNames() {
        for (State state : State.values()) {
            assertNotNull(state.name());
            assertFalse(state.name().isEmpty());
        }
    }

    @Test
    public void testValueOfRoundTrip() {
        for (State state : State.values()) {
            assertEquals(state, State.valueOf(state.name()));
        }
    }

    @Test
    public void testKeyStateNames() {
        assertEquals("Queued", State.Queued.name());
        assertEquals("Running", State.Running.name());
        assertEquals("Succeeded", State.Succeeded.name());
        assertEquals("Failed", State.Failed.name());
        assertEquals("ChecksumError", State.ChecksumError.name());
        assertEquals("NoFileError", State.NoFileError.name());
    }
}

