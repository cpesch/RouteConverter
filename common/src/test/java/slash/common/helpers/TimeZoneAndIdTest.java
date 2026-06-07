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

package slash.common.helpers;

import org.junit.Test;

import java.util.TimeZone;

import static org.junit.Assert.*;

/**
 * Tests for {@link TimeZoneAndId}.
 *
 * @author Christian Pesch
 */

public class TimeZoneAndIdTest {

    @Test
    public void testIdAccessor() {
        TimeZone tz = TimeZone.getTimeZone("UTC");
        TimeZoneAndId tzAndId = new TimeZoneAndId("UTC", tz);
        assertEquals("UTC", tzAndId.id());
    }

    @Test
    public void testTimeZoneAccessor() {
        TimeZone tz = TimeZone.getTimeZone("Europe/Berlin");
        TimeZoneAndId tzAndId = new TimeZoneAndId("Europe/Berlin", tz);
        assertEquals(tz, tzAndId.timeZone());
    }

    @Test
    public void testEquality() {
        TimeZone tz = TimeZone.getTimeZone("UTC");
        TimeZoneAndId a = new TimeZoneAndId("UTC", tz);
        TimeZoneAndId b = new TimeZoneAndId("UTC", tz);
        assertEquals(a, b);
    }

    @Test
    public void testInequalityDifferentId() {
        TimeZone tz = TimeZone.getTimeZone("UTC");
        TimeZoneAndId a = new TimeZoneAndId("UTC", tz);
        TimeZoneAndId b = new TimeZoneAndId("GMT", tz);
        assertNotEquals(a, b);
    }

    @Test
    public void testHashCodeConsistency() {
        TimeZone tz = TimeZone.getTimeZone("UTC");
        TimeZoneAndId a = new TimeZoneAndId("UTC", tz);
        TimeZoneAndId b = new TimeZoneAndId("UTC", tz);
        assertEquals(a.hashCode(), b.hashCode());
    }
}

