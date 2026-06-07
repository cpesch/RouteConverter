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
 * Tests for {@link IntegerModel}.
 *
 * @author Christian Pesch
 */

public class IntegerModelTest {

    private static String key(String suffix) {
        return "test.int." + suffix + "." + UUID.randomUUID();
    }

    @Test
    public void testDefaultValue() {
        IntegerModel model = new IntegerModel(key("default"), 42);
        assertEquals(Integer.valueOf(42), model.getInteger());
    }

    @Test
    public void testDefaultValueZero() {
        IntegerModel model = new IntegerModel(key("default.zero"), 0);
        assertEquals(Integer.valueOf(0), model.getInteger());
    }

    @Test
    public void testSetInteger() {
        IntegerModel model = new IntegerModel(key("set"), 0);
        model.setInteger(99);
        assertEquals(Integer.valueOf(99), model.getInteger());
    }

    @Test
    public void testSetIntegerNegative() {
        IntegerModel model = new IntegerModel(key("negative"), 0);
        model.setInteger(-5);
        assertEquals(Integer.valueOf(-5), model.getInteger());
    }

    @Test
    public void testSetIntegerFiresChangeListener() {
        IntegerModel model = new IntegerModel(key("change"), 0);
        AtomicInteger count = new AtomicInteger(0);
        model.addChangeListener(e -> count.incrementAndGet());
        model.setInteger(1);
        assertEquals(1, count.get());
    }

    @Test
    public void testSetIntegerFiresMultipleChangeListeners() {
        IntegerModel model = new IntegerModel(key("multi"), 0);
        AtomicInteger count = new AtomicInteger(0);
        model.addChangeListener(e -> count.incrementAndGet());
        model.addChangeListener(e -> count.incrementAndGet());
        model.setInteger(1);
        assertEquals(2, count.get());
    }

    @Test
    public void testRemoveChangeListenerNoFire() {
        IntegerModel model = new IntegerModel(key("remove"), 0);
        AtomicInteger count = new AtomicInteger(0);
        javax.swing.event.ChangeListener listener = e -> count.incrementAndGet();
        model.addChangeListener(listener);
        model.removeChangeListener(listener);
        model.setInteger(1);
        assertEquals(0, count.get());
    }
}




