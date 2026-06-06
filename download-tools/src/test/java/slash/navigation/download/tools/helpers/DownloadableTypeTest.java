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
package slash.navigation.download.tools.helpers;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link DownloadableType}.
 *
 * @author Christian Pesch
 */
public class DownloadableTypeTest {

    @Test
    public void valueReturnsLowercaseString() {
        assertEquals("file", DownloadableType.File.value());
        assertEquals("map", DownloadableType.Map.value());
        assertEquals("theme", DownloadableType.Theme.value());
    }

    @Test
    public void fromValueExactMatch() {
        assertEquals(DownloadableType.File, DownloadableType.fromValue("file"));
        assertEquals(DownloadableType.Map, DownloadableType.fromValue("map"));
        assertEquals(DownloadableType.Theme, DownloadableType.fromValue("theme"));
    }

    @Test
    public void fromValueCaseInsensitive() {
        assertEquals(DownloadableType.File, DownloadableType.fromValue("FILE"));
        assertEquals(DownloadableType.File, DownloadableType.fromValue("File"));
        assertEquals(DownloadableType.Map, DownloadableType.fromValue("MAP"));
        assertEquals(DownloadableType.Theme, DownloadableType.fromValue("THEME"));
    }

    @Test
    public void fromValueTrimsWhitespace() {
        assertEquals(DownloadableType.File, DownloadableType.fromValue("  file  "));
        assertEquals(DownloadableType.Map, DownloadableType.fromValue(" map "));
    }

    @Test
    public void fromValueReturnsNullForUnknown() {
        assertNull(DownloadableType.fromValue("unknown"));
        assertNull(DownloadableType.fromValue(""));
    }

    @Test
    public void fromValueReturnsNullForNull() {
        assertNull(DownloadableType.fromValue(null));
    }
}

