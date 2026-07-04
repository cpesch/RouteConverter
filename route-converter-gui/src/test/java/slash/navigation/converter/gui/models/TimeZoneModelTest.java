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

import javax.swing.event.ChangeListener;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

/**
 * Tests for {@link TimeZoneModel}.
 *
 * @author Christian Pesch
 */
public class TimeZoneModelTest {
    private static final TimeZone UTC = TimeZone.getTimeZone("UTC");
    private static final TimeZone BERLIN = TimeZone.getTimeZone("Europe/Berlin");

    // a unique preference key per instance keeps the persistent Preferences store test-isolated
    private static String key() {
        return "test.timezone." + UUID.randomUUID();
    }

    @Test
    public void returnsTheDefaultUntilChanged() {
        TimeZoneModel model = new TimeZoneModel(key(), UTC);

        assertEquals("UTC", model.getTimeZoneId());
        assertEquals(UTC, model.getTimeZone());
    }

    @Test
    public void setTimeZonePersistsTheNewValue() {
        TimeZoneModel model = new TimeZoneModel(key(), UTC);

        model.setTimeZone(BERLIN);

        assertEquals("Europe/Berlin", model.getTimeZoneId());
        assertEquals(BERLIN, model.getTimeZone());
    }

    @Test
    public void setTimeZoneNotifiesChangeListeners() {
        TimeZoneModel model = new TimeZoneModel(key(), UTC);
        AtomicInteger changes = new AtomicInteger();
        model.addChangeListener(e -> changes.incrementAndGet());

        model.setTimeZone(BERLIN);

        assertEquals(1, changes.get());
    }

    @Test
    public void removedListenerStopsReceivingEvents() {
        TimeZoneModel model = new TimeZoneModel(key(), UTC);
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
