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

import junit.framework.TestCase;

import java.util.Map;

public class VersionTest extends TestCase {

    public void testParseParameters() {
        Map<String, String> params = Version.parseParameters("b=c,routeconverter.version=1.3,a=b");
        assertEquals("c", params.get("b"));
        assertEquals("1.3", params.get("routeconverter.version"));
        assertEquals("b", params.get("a"));
        assertNull( params.get("c"));
    }

    public void testParseVersion() {
        assertEquals("1.3", Version.parseVersionFromParameters("b=c,routeconverter.version=1.3,a=b"));
        assertEquals("2", Version.parseVersionFromParameters("x=y,routeconverter.version=2,y=z"));
    }

    public void testGetMajor() {
        assertEquals("1", Version.getMajor("1"));
        assertEquals("1", Version.getMajor("1.1"));
        assertEquals("1", Version.getMajor("1.1.1"));
    }

    public void testGetMinor() {
        assertEquals("1", Version.getMinor("1"));
        assertEquals("1", Version.getMinor("1.1"));
        assertEquals("1.1", Version.getMinor("1.1.1"));
    }

    public void testIsCurrent() {
        assertTrue(Version.isLatestVersion("1.3", "1.3"));
        assertFalse(Version.isLatestVersion("2.1", "1.9"));
        assertFalse(Version.isLatestVersion("1.3", "1.2"));
        assertFalse(Version.isLatestVersion("1.3", "1"));
        assertTrue(Version.isLatestVersion("1.2", "1.3"));
        assertFalse(Version.isLatestVersion("1.9.1", "1.9"));
        assertFalse(Version.isLatestVersion("1.9.2", "1.9.1"));
        assertFalse(Version.isLatestVersion("1.9.10", "1.9.9"));
        assertFalse(Version.isLatestVersion("1.9a", "1.9"));
        assertFalse(Version.isLatestVersion("1.10", "1.09"));
        assertFalse(Version.isLatestVersion("1.10", "1.9"));
        assertFalse(Version.isLatestVersion("1.100", "1.99"));
    }
}
