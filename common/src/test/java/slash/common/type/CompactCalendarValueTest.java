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
package slash.common.type;

import org.junit.Test;

import java.util.Calendar;
import java.util.TimeZone;

import static org.junit.Assert.*;
import static slash.common.type.CompactCalendar.*;

/**
 * Covers the value-object behavior of {@link CompactCalendar}: equality, hashing,
 * date-presence detection, string parsing and time-zone propagation of the factories.
 *
 * @author Christian Pesch
 */
public class CompactCalendarValueTest {
    private static final long ONE_DAY = 24 * 60 * 60 * 1000L;

    @Test
    public void equalWhenMillisAndZoneMatch() {
        assertEquals(fromMillisAndTimeZone(1000, "UTC"), fromMillisAndTimeZone(1000, "UTC"));
        assertEquals(fromMillisAndTimeZone(1000, "UTC").hashCode(), fromMillisAndTimeZone(1000, "UTC").hashCode());
    }

    @Test
    public void sameMillisDifferentZoneIsNotEqual() {
        assertNotEquals(fromMillisAndTimeZone(1000, "UTC"), fromMillisAndTimeZone(1000, "Europe/Berlin"));
    }

    @Test
    public void differentMillisIsNotEqual() {
        assertNotEquals(fromMillis(1000), fromMillis(2000));
    }

    @Test
    public void notEqualToNullOrOtherType() {
        assertNotEquals(fromMillis(1000), null);
        assertNotEquals(fromMillis(1000), "1000");
    }

    @Test
    public void hasDateDefinedIsFalseForTheUnixEpochDay() {
        assertFalse(fromMillis(0).hasDateDefined());
        assertFalse(fromMillis(ONE_DAY - 1).hasDateDefined());
    }

    @Test
    public void hasDateDefinedIsTrueFromTheSecondDayOn() {
        assertTrue(fromMillis(ONE_DAY).hasDateDefined());
    }

    @Test
    public void parseDateReturnsNullForNullOrUnparseableInput() {
        assertNull(parseDate(null, "yyyy-MM-dd"));
        assertNull(parseDate("not a date", "yyyy-MM-dd", false));
    }

    @Test
    public void parseDateParsesInUtc() {
        CompactCalendar parsed = parseDate("2025-06-08", "yyyy-MM-dd");

        assertNotNull(parsed);
        assertEquals("UTC", parsed.getTimeZoneId());
        Calendar calendar = parsed.getCalendar();
        assertEquals(2025, calendar.get(Calendar.YEAR));
        assertEquals(Calendar.JUNE, calendar.get(Calendar.MONTH));
        assertEquals(8, calendar.get(Calendar.DAY_OF_MONTH));
    }

    @Test
    public void factoriesPropagateTheTimeZone() {
        assertEquals("UTC", fromMillis(0).getTimeZoneId());
        assertEquals("Europe/Berlin", fromMillisAndTimeZone(0, "Europe/Berlin").getTimeZoneId());

        Calendar berlin = Calendar.getInstance(TimeZone.getTimeZone("Europe/Berlin"));
        assertEquals("Europe/Berlin", fromCalendar(berlin).getTimeZoneId());
    }

    @Test
    public void asUTCTimeInTimeZoneShiftsByTheOffset() {
        TimeZone berlin = TimeZone.getTimeZone("Europe/Berlin");
        CompactCalendar original = fromMillisAndTimeZone(1_000_000_000L, "Europe/Berlin");

        CompactCalendar shifted = original.asUTCTimeInTimeZone(berlin);

        assertEquals("UTC", shifted.getTimeZoneId());
        assertEquals(1_000_000_000L - berlin.getOffset(1_000_000_000L), shifted.getTimeInMillis());
    }

    @Test
    public void beforeAndAfterCompareAcrossDifferentZones() {
        CompactCalendar earlier = fromMillisAndTimeZone(1000, "Europe/Berlin");
        CompactCalendar later = fromMillisAndTimeZone(2000, "UTC");

        assertTrue(earlier.before(later));
        assertTrue(later.after(earlier));
        assertFalse(earlier.after(later));
    }

    @Test
    public void accessorsReturnTheConstructionValues() {
        CompactCalendar calendar = fromMillisAndTimeZone(1234, "UTC");

        assertEquals(1234, calendar.getTimeInMillis());
        assertEquals("UTC", calendar.getTimeZoneId());
        assertEquals(1234, calendar.getCalendar().getTimeInMillis());
    }
}
