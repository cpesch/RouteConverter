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
package slash.navigation.converter.gui.models;

import org.junit.Test;
import slash.common.prefs.InMemoryPreferences;

import javax.swing.event.ChangeListener;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.prefs.Preferences;

import static org.junit.Assert.*;

/**
 * Tests for {@link TimeZoneModel}.
 *
 * @author Christian Pesch
 */
public class TimeZoneModelTest {
    private static final TimeZone UTC = TimeZone.getTimeZone("UTC");
    private static final TimeZone BERLIN = TimeZone.getTimeZone("Europe/Berlin");

    private static final String KEY = "timeZone";

    // a fresh in-memory node per test: the OS-level user preferences are shared by every JVM
    // of the user and lose writes under concurrent test runs, even for unique keys
    private final Preferences preferences = new InMemoryPreferences();

    private TimeZoneModel createModel() {
        return new TimeZoneModel(preferences, KEY, UTC);
    }

    @Test
    public void returnsTheDefaultUntilChanged() {
        TimeZoneModel model = createModel();

        assertEquals("UTC", model.getTimeZoneId());
        assertEquals(UTC, model.getTimeZone());
    }

    @Test
    public void setTimeZonePersistsTheNewValue() {
        TimeZoneModel model = createModel();

        model.setTimeZone(BERLIN);

        assertEquals("Europe/Berlin", model.getTimeZoneId());
        assertEquals(BERLIN, model.getTimeZone());
        assertEquals("Europe/Berlin", preferences.get(KEY, null));
    }

    @Test
    public void anotherModelReadsThePersistedValue() {
        createModel().setTimeZone(BERLIN);

        assertEquals(BERLIN, createModel().getTimeZone());
    }

    @Test
    public void setTimeZoneNotifiesChangeListeners() {
        TimeZoneModel model = createModel();
        AtomicInteger changes = new AtomicInteger();
        model.addChangeListener(e -> changes.incrementAndGet());

        model.setTimeZone(BERLIN);

        assertEquals(1, changes.get());
    }

    @Test
    public void removedListenerStopsReceivingEvents() {
        TimeZoneModel model = createModel();
        AtomicInteger changes = new AtomicInteger();
        ChangeListener listener = e -> changes.incrementAndGet();
        model.addChangeListener(listener);
        model.setTimeZone(BERLIN);
        assertEquals(1, changes.get());

        model.removeChangeListener(listener);
        model.setTimeZone(UTC);

        assertEquals(1, changes.get());
    }
}
