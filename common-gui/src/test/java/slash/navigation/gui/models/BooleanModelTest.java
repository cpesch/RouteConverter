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

package slash.navigation.gui.models;

import org.junit.Test;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

/**
 * Tests for {@link BooleanModel}.
 *
 * @author Christian Pesch
 */

public class BooleanModelTest {

    private static String key(String suffix) {
        return "test.bool." + suffix + "." + UUID.randomUUID();
    }

    @Test
    public void testDefaultValueTrue() {
        BooleanModel model = new BooleanModel(key("default.true"), true);
        assertTrue(model.getBoolean());
    }

    @Test
    public void testDefaultValueFalse() {
        BooleanModel model = new BooleanModel(key("default.false"), false);
        assertFalse(model.getBoolean());
    }

    @Test
    public void testSetBooleanTrue() {
        // Write false first so the key exists, then overwrite with true
        BooleanModel model = new BooleanModel(key("set.true"), true);
        model.setBoolean(false);
        model.setBoolean(true);
        assertTrue(model.getBoolean());
    }

    @Test
    public void testSetBooleanFalse() {
        BooleanModel model = new BooleanModel(key("set.false"), true);
        model.setBoolean(false);
        assertFalse(model.getBoolean());
    }

    @Test
    public void testSetBooleanFiresChangeListener() {
        BooleanModel model = new BooleanModel(key("change"), false);
        AtomicInteger count = new AtomicInteger(0);
        model.addChangeListener(e -> count.incrementAndGet());
        model.setBoolean(true);
        assertEquals(1, count.get());
    }

    @Test
    public void testSetBooleanFiresMultipleChangeListeners() {
        BooleanModel model = new BooleanModel(key("multi"), false);
        AtomicInteger count = new AtomicInteger(0);
        model.addChangeListener(e -> count.incrementAndGet());
        model.addChangeListener(e -> count.incrementAndGet());
        model.setBoolean(true);
        assertEquals(2, count.get());
    }

    @Test
    public void testRemoveChangeListenerNoFire() {
        BooleanModel model = new BooleanModel(key("remove"), false);
        AtomicInteger count = new AtomicInteger(0);
        javax.swing.event.ChangeListener listener = e -> count.incrementAndGet();
        model.addChangeListener(listener);
        model.removeChangeListener(listener);
        model.setBoolean(true);
        assertEquals(0, count.get());
    }
}

