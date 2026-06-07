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

package slash.navigation.gui.events;

import org.junit.Test;

import javax.swing.event.ListDataEvent;

import static java.lang.Integer.MIN_VALUE;
import static javax.swing.event.ListDataEvent.CONTENTS_CHANGED;
import static javax.swing.event.ListDataEvent.INTERVAL_ADDED;
import static javax.swing.event.ListDataEvent.INTERVAL_REMOVED;
import static org.junit.Assert.*;

/**
 * Tests for {@link IgnoreEvent}.
 *
 * @author Christian Pesch
 */

public class IgnoreEventTest {

    private ListDataEvent event(int type, int index0, int index1) {
        return new ListDataEvent(new Object(), type, index0, index1);
    }

    @Test
    public void testIgnoreEventContentsChangedWithIgnoreIndices() {
        ListDataEvent e = event(CONTENTS_CHANGED, MIN_VALUE, MIN_VALUE);
        assertTrue(IgnoreEvent.isIgnoreEvent(e));
    }

    @Test
    public void testNotIgnoreEventContentsChangedWithNormalIndices() {
        ListDataEvent e = event(CONTENTS_CHANGED, 0, 1);
        assertFalse(IgnoreEvent.isIgnoreEvent(e));
    }

    @Test
    public void testNotIgnoreEventIntervalAdded() {
        ListDataEvent e = event(INTERVAL_ADDED, MIN_VALUE, MIN_VALUE);
        assertFalse(IgnoreEvent.isIgnoreEvent(e));
    }

    @Test
    public void testNotIgnoreEventIntervalRemoved() {
        ListDataEvent e = event(INTERVAL_REMOVED, MIN_VALUE, MIN_VALUE);
        assertFalse(IgnoreEvent.isIgnoreEvent(e));
    }

    @Test
    public void testNotIgnoreEventContentsChangedIndex0Wrong() {
        ListDataEvent e = event(CONTENTS_CHANGED, 0, MIN_VALUE);
        assertFalse(IgnoreEvent.isIgnoreEvent(e));
    }

    @Test
    public void testNotIgnoreEventContentsChangedIndex1Wrong() {
        ListDataEvent e = event(CONTENTS_CHANGED, MIN_VALUE, 0);
        assertFalse(IgnoreEvent.isIgnoreEvent(e));
    }

    @Test
    public void testIgnoreConstantIsMinValue() {
        assertEquals(MIN_VALUE, IgnoreEvent.IGNORE);
    }
}

