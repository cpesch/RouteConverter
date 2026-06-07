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

import static org.junit.Assert.*;

/**
 * Tests for {@link TimeZoneAndIds}.
 *
 * @author Christian Pesch
 */

public class TimeZoneAndIdsTest {

    @Test
    public void testGetInstanceReturnsSameObject() {
        TimeZoneAndIds a = TimeZoneAndIds.getInstance();
        TimeZoneAndIds b = TimeZoneAndIds.getInstance();
        assertSame(a, b);
    }

    @Test
    public void testGetTimeZonesNotEmpty() {
        TimeZoneAndId[] zones = TimeZoneAndIds.getInstance().getTimeZones();
        assertNotNull(zones);
        assertTrue(zones.length > 0);
    }

    @Test
    public void testGetTimeZonesContainUtc() {
        TimeZoneAndId[] zones = TimeZoneAndIds.getInstance().getTimeZones();
        boolean found = false;
        for (TimeZoneAndId tz : zones) {
            if ("UTC".equals(tz.id())) {
                found = true;
                break;
            }
        }
        assertTrue("UTC should be present", found);
    }

    @Test
    public void testGetTimeZoneAndIdForKnownZone() {
        TimeZoneAndIds instance = TimeZoneAndIds.getInstance();
        TimeZoneAndId[] zones = instance.getTimeZones();
        // Use the exact TimeZone object reference from the list to guarantee equals() works
        TimeZoneAndId first = zones[0];
        TimeZoneAndId result = instance.getTimeZoneAndIdFor(first.timeZone());
        assertNotNull(result);
        assertSame(first.timeZone(), result.timeZone());
    }

    @Test
    public void testGetTimeZonesAreSorted() {
        TimeZoneAndId[] zones = TimeZoneAndIds.getInstance().getTimeZones();
        for (int i = 1; i < zones.length; i++) {
            assertTrue(zones[i - 1].id().compareTo(zones[i].id()) <= 0);
        }
    }

    @Test
    public void testNoDuplicateIds() {
        TimeZoneAndId[] zones = TimeZoneAndIds.getInstance().getTimeZones();
        java.util.Set<String> ids = new java.util.HashSet<>();
        for (TimeZoneAndId tz : zones) {
            assertTrue("Duplicate id: " + tz.id(), ids.add(tz.id()));
        }
    }
}



