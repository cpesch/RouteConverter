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
package slash.navigation.gui.helpers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static slash.common.type.CompactCalendar.fromMillis;
import static slash.navigation.gui.helpers.UIHelper.formatSize;
import static slash.navigation.gui.helpers.UIHelper.formatTime;

import org.junit.Test;

public class UIHelperTest {

    // ---- formatTime ----

    @Test
    public void testFormatTimeNull() {
        assertEquals("?", formatTime(null));
    }

    @Test
    public void testFormatTimeZero() {
        // 0 millis = 00:00:00
        assertEquals("00:00:00", formatTime(fromMillis(0L)));
    }

    @Test
    public void testFormatTimeOneHour() {
        // 3600 seconds = 1 hour
        assertEquals("01:00:00", formatTime(fromMillis(3_600_000L)));
    }

    @Test
    public void testFormatTimeOneMinuteFiveSeconds() {
        // 65 seconds
        assertEquals("00:01:05", formatTime(fromMillis(65_000L)));
    }

    @Test
    public void testFormatTimeFormat() {
        // format should always be HH:MM:SS
        String result = formatTime(fromMillis(3_661_000L));
        assertTrue("should match HH:MM:SS", result.matches("\\d{2}:\\d{2}:\\d{2}"));
    }

    // ---- formatSize ----

    @Test
    public void testFormatSizeNull() {
        assertEquals("?", formatSize(null));
    }

    @Test
    public void testFormatSizeBytes() {
        String result = formatSize(500L);
        assertTrue("should contain Bytes", result.contains("Bytes"));
    }

    @Test
    public void testFormatSizeKiloBytes() {
        String result = formatSize(3_000L);
        assertTrue("should contain kByte", result.contains("kByte"));
    }

    @Test
    public void testFormatSizeMegaBytes() {
        String result = formatSize(3_000_000L);
        assertTrue("should contain MByte", result.contains("MByte"));
    }

    @Test
    public void testFormatSizeExactlyTwoKiloByteBoundary() {
        // 2 * 1024 = 2048 -> should be kByte
        String result = formatSize(2049L);
        assertTrue("2049 bytes should display as kByte", result.contains("kByte"));
    }
}
