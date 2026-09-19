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
import slash.common.prefs.InMemoryPreferences;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.prefs.Preferences;

import static org.junit.Assert.*;

/**
 * Tests for {@link BooleanModel}.
 *
 * @author Christian Pesch
 */

public class BooleanModelTest {

    // isolated in-memory store per test instance, so concurrent forks cannot
    // clobber each other's writes on the shared OS preferences node
    private final Preferences preferences = new InMemoryPreferences();

    private static String key(String suffix) {
        return "test.bool." + suffix + "." + UUID.randomUUID();
    }

    private BooleanModel model(String suffix, boolean defaultValue) {
        return new BooleanModel(key(suffix), defaultValue, preferences);
    }

    @Test
    public void testDefaultValueTrue() {
        BooleanModel model = model("default.true", true);
        assertTrue(model.getBoolean());
    }

    @Test
    public void testDefaultValueFalse() {
        BooleanModel model = model("default.false", false);
        assertFalse(model.getBoolean());
    }

    @Test
    public void testSetBooleanTrue() {
        BooleanModel model = model("set.true", true);
        model.setBoolean(false);
        model.setBoolean(true);
        assertTrue(model.getBoolean());
    }

    @Test
    public void testSetBooleanFalse() {
        BooleanModel model = model("set.false", true);
        model.setBoolean(false);
        assertFalse(model.getBoolean());
    }

    @Test
    public void testSetBooleanRoundTripsThroughTheInjectedStore() {
        String preferencesName = key("round.trip");
        BooleanModel model = new BooleanModel(preferencesName, false, preferences);
        model.setBoolean(true);
        assertEquals("true", preferences.get(preferencesName, null));
        assertTrue(model.getBoolean());
        model.setBoolean(false);
        assertEquals("false", preferences.get(preferencesName, null));
        assertFalse(model.getBoolean());
    }

    @Test
    public void testSetBooleanFiresChangeListener() {
        BooleanModel model = model("change", false);
        AtomicInteger count = new AtomicInteger(0);
        model.addChangeListener(e -> count.incrementAndGet());
        model.setBoolean(true);
        assertEquals(1, count.get());
    }

    @Test
    public void testSetBooleanFiresMultipleChangeListeners() {
        BooleanModel model = model("multi", false);
        AtomicInteger count = new AtomicInteger(0);
        model.addChangeListener(e -> count.incrementAndGet());
        model.addChangeListener(e -> count.incrementAndGet());
        model.setBoolean(true);
        assertEquals(2, count.get());
    }

    @Test
    public void testRemoveChangeListenerNoFire() {
        BooleanModel model = model("remove", false);
        AtomicInteger count = new AtomicInteger(0);
        javax.swing.event.ChangeListener listener = e -> count.incrementAndGet();
        model.addChangeListener(listener);
        model.removeChangeListener(listener);
        model.setBoolean(true);
        assertEquals(0, count.get());
    }
}
