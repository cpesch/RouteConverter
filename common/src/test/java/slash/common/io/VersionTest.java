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

package slash.common.io;

import org.junit.Test;

import java.text.DateFormat;
import java.util.Date;
import java.util.Map;

import static org.junit.Assert.*;
import static slash.common.TestCase.calendar;

public class VersionTest {

    @Test
    public void testParseParameters() {
        Map<String, String> params = Version.parseParameters("b=c,routeconverter.version=1.3,a=b");
        assertEquals("c", params.get("b"));
        assertEquals("1.3", params.get("routeconverter.version"));
        assertEquals("b", params.get("a"));
        assertNull( params.get("c"));
    }

    @Test
    public void testParseVersion() {
        assertEquals("1.3", Version.getLatestRouteConverterVersion("b=c,routeconverter.version=1.3,a=b"));
        assertEquals("2", Version.getLatestRouteConverterVersion("x=y,routeconverter.version=2,y=z"));
    }

    @Test
    public void testGetVersion() {
        assertEquals("1.2.3", new Version("1.2.3").getVersion());
        assertEquals("1.2", new Version("1.2-3").getVersion());
        assertEquals("1.2-SNAPSHOT-3", new Version("1.2-SNAPSHOT-3").getVersion());
    }

    @Test
    public void testGetMajor() {
        assertEquals("1", new Version("1").getMajor());
        assertEquals("1", new Version("1.2").getMajor());
        assertEquals("1", new Version("1.2.3").getMajor());
        assertEquals("1", new Version("1.2-SNAPSHOT-3").getMajor());
    }

    @Test
    public void testGetMinor() {
        assertEquals("1", new Version("1").getMinor());
        assertEquals("2", new Version("1.2").getMinor());
        assertEquals("2-3", new Version("1.2-3").getMinor());
        assertEquals("2.3-4", new Version("1.2.3-4").getMinor());
        assertEquals("2-SNAPSHOT-3", new Version("1.2-SNAPSHOT-3").getMinor());
        assertEquals("2.3-SNAPSHOT-4", new Version("1.2.3-SNAPSHOT-4").getMinor());
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
        assertEquals("Windows", new Version(null, null, "Windows64").getOperationSystem());
        assertEquals("Linux", new Version(null, null, "Linux32").getOperationSystem());
    }

    @Test
    public void testGetBits() {
        assertEquals("64", new Version(null, null, "Windows64").getBits());
        assertEquals("32", new Version(null, null, "Linux32").getBits());
    }

    @Test
    public void testIsLatestVersion() {
        assertTrue(new Version("1").isLaterVersionThan(new Version("1")));
        assertTrue(new Version("2").isLaterVersionThan(new Version("1.3")));
        assertTrue(new Version("10").isLaterVersionThan(new Version("9")));
        assertTrue(new Version("11").isLaterVersionThan(new Version("10")));

        assertTrue(new Version("1.3").isLaterVersionThan(new Version("1")));
        assertTrue(new Version("1.3").isLaterVersionThan(new Version("1.3")));
        assertTrue(new Version("1.10").isLaterVersionThan(new Version("1.9")));
        assertTrue(new Version("1.100").isLaterVersionThan(new Version("1.99")));

        assertTrue(new Version("1.3.1").isLaterVersionThan(new Version("1.3")));
        assertTrue(new Version("1.3.10").isLaterVersionThan(new Version("1.3.9")));

        assertTrue(new Version("1.3-SNAPSHOT-1").isLaterVersionThan(new Version("1.3")));
        assertTrue(new Version("1.3-SNAPSHOT-2").isLaterVersionThan(new Version("1.3-SNAPSHOT-1")));

        assertTrue(new Version("0.3").isLaterVersionThan(new Version("0.2")));
        assertTrue(new Version("1.3").isLaterVersionThan(new Version("1.2")));
        assertTrue(new Version("2.3").isLaterVersionThan(new Version("2.2")));

        assertTrue(new Version("1.1").isLaterVersionThan(new Version("0.9")));
        assertTrue(new Version("1.30.1").isLaterVersionThan(new Version("1.29.2")));

        assertTrue(new Version("1.30.1").isLaterVersionThan(new Version("1.29.2")));
    }

    @Test
    public void testIsNotLatestVersion() {
        assertFalse(new Version("1").isLaterVersionThan(new Version("1.3")));
        assertFalse(new Version("1.2").isLaterVersionThan(new Version("1.3")));
        assertFalse(new Version("1.2.1").isLaterVersionThan(new Version("1.3")));
        assertFalse(new Version("1.2-SNAPSHOT-1").isLaterVersionThan(new Version("1.3")));
    }
}
