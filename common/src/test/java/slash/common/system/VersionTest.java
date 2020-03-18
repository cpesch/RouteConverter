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

package slash.common.system;

import org.junit.Test;

import java.text.DateFormat;
import java.util.Date;

import static org.junit.Assert.*;
import static slash.common.TestCase.calendar;
import static slash.common.system.Version.compareVersion;

public class VersionTest {

    @Test
    public void testGetVersion() {
        assertEquals("1.2.3", new Version("1.2.3").getVersion());
        assertEquals("1.2", new Version("1.2-3").getVersion());
        assertEquals("1.2-SNAPSHOT-3", new Version("1.2-SNAPSHOT-3").getVersion());
        assertEquals("1.2_3", new Version("1.2_3").getVersion());
    }

    @Test
    public void testGetMajor() {
        assertEquals(14, new Version("14").getMajor());
        assertEquals(11, new Version("11.0.1").getMajor());
        assertEquals(8, new Version("1.8.0_241").getMajor());
        assertEquals(8, new Version("").getMajor());
    }

    @Test
    public void testGetDate() {
        Date date = calendar(2009, 11, 23, 13, 53, 49, 0, "UTC").getCalendar().getTime();
        DateFormat dateFormat = DateFormat.getDateInstance(DateFormat.LONG);
        String expected = dateFormat.format(date);
        String actual = new Version(null, "2009-11-23 13:53:49", null).getDate();
        assertEquals(expected, actual);
    }

    @Test
    public void testGetPlatform() {
        assertEquals("Windows", new Version(null, null, "Windows").getOperationSystem());
        assertEquals("?", new Version(null, null, null).getOperationSystem());
    }

    @Test
    public void testCompareVersion() {
        assertEquals(-1, compareVersion("1", "2"));
        assertEquals(0, compareVersion("2", "2"));
        assertEquals(1, compareVersion("2", "1"));

        assertEquals(-1, compareVersion("9", "10"));
        assertEquals(0, compareVersion("10", "10"));
        assertEquals(1, compareVersion("10", "9"));

        assertEquals(-1, compareVersion("1.9", "2.0"));
        assertEquals(-1, compareVersion("1", "2.0"));
        assertEquals(-1, compareVersion("1.9", "2"));

        assertEquals(1, compareVersion("2.0.1", "2.0.0"));
        assertEquals(1, compareVersion("2.1.0", "2.0.0.1"));
        assertEquals(1, compareVersion("2.2", "2.0.99"));

        assertEquals(1, compareVersion("1.8.0_92", "1.8.0_6"));
        assertEquals(1, compareVersion("1.8.0_92", "1.8.0_91"));
        assertEquals(1, compareVersion("1.8.0_101", "1.8.0_91"));
    }

    @Test
    public void testIsLaterVersionThan() {
        assertFalse(new Version("1").isLaterVersionThan(new Version("1")));
        assertTrue(new Version("2").isLaterVersionThan(new Version("1.3")));
        assertTrue(new Version("10").isLaterVersionThan(new Version("9")));
        assertTrue(new Version("11").isLaterVersionThan(new Version("10")));

        assertTrue(new Version("1.3").isLaterVersionThan(new Version("1")));
        assertFalse(new Version("1.3").isLaterVersionThan(new Version("1.3")));
        assertTrue(new Version("1.10").isLaterVersionThan(new Version("1.9")));
        assertTrue(new Version("1.100").isLaterVersionThan(new Version("1.99")));

        assertTrue(new Version("1.3.1").isLaterVersionThan(new Version("1.3")));
        assertTrue(new Version("1.3.10").isLaterVersionThan(new Version("1.3.9")));

        assertTrue(new Version("1.3-SNAPSHOT-1").isLaterVersionThan(new Version("1.3")));
        assertTrue(new Version("1.3-SNAPSHOT-2").isLaterVersionThan(new Version("1.3-SNAPSHOT-1")));
        assertTrue(new Version("?.?.?").isLaterVersionThan(new Version("2.18.4")));

        assertTrue(new Version("0.3").isLaterVersionThan(new Version("0.2")));
        assertTrue(new Version("1.3").isLaterVersionThan(new Version("1.2")));
        assertTrue(new Version("2.3").isLaterVersionThan(new Version("2.2")));

        assertTrue(new Version("1.1").isLaterVersionThan(new Version("0.9")));
        assertTrue(new Version("1.30.1").isLaterVersionThan(new Version("1.29.2")));
    }

    @Test
    public void testIsLaterJavaVersion() {
        assertTrue(new Version("1.8.0_92").isLaterVersionThan(new Version("1.8.0_91")));
        assertTrue(new Version("1.8.0_101").isLaterVersionThan(new Version("1.8.0_91")));
        assertTrue(new Version("1.8.0_102").isLaterVersionThan(new Version("1.8.0_101")));
        assertFalse(new Version("1.8.0_101").isLaterVersionThan(new Version("1.8.0_102")));
    }

    @Test
    public void testIsNotLatestVersion() {
        assertFalse(new Version("1").isLaterVersionThan(new Version("1.3")));
        assertFalse(new Version("1.2").isLaterVersionThan(new Version("1.3")));
        assertFalse(new Version("1.2.1").isLaterVersionThan(new Version("1.3")));
        assertFalse(new Version("1.2-SNAPSHOT-1").isLaterVersionThan(new Version("1.3")));
    }
}
