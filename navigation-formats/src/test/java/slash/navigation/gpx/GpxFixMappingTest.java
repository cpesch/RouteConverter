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

package slash.navigation.gpx;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static slash.navigation.gpx.GpxFormat.formatFix;
import static slash.navigation.gpx.GpxFormat.parseFix;

/**
 * Unit tests for {@link GpxFormat#parseFix(String)} and {@link GpxFormat#formatFix(Integer)},
 * the GPX {@code <fix>} <-> GGA fix-quality mapping introduced for Columbus GNSS support.
 */
public class GpxFixMappingTest {

    @Test
    public void parseFixNone() {
        assertEquals(Integer.valueOf(0), parseFix("none"));
    }

    @Test
    public void parseFix2d() {
        assertEquals(Integer.valueOf(1), parseFix("2d"));
    }

    @Test
    public void parseFix3d() {
        assertEquals(Integer.valueOf(1), parseFix("3d"));
    }

    @Test
    public void parseFixDgps() {
        assertEquals(Integer.valueOf(2), parseFix("dgps"));
    }

    @Test
    public void parseFixPps() {
        assertEquals(Integer.valueOf(3), parseFix("pps"));
    }

    @Test
    public void parseFixUnknown() {
        assertNull(parseFix("unknown"));
    }

    @Test
    public void parseFixNull() {
        assertNull(parseFix(null));
    }

    @Test
    public void parseFixIsCaseInsensitive() {
        assertEquals(Integer.valueOf(2), parseFix("DGPS"));
    }

    @Test
    public void formatFixNone() {
        assertEquals("none", formatFix(0));
    }

    @Test
    public void formatFixDgps() {
        assertEquals("dgps", formatFix(2));
    }

    @Test
    public void formatFixPps() {
        assertEquals("pps", formatFix(3));
    }

    @Test
    public void formatFixSpsIsOmitted() {
        assertNull(formatFix(1));
    }

    @Test
    public void formatFixRtkFloatIsOmitted() {
        assertNull(formatFix(4));
    }

    @Test
    public void formatFixRtkFixedIsOmitted() {
        assertNull(formatFix(5));
    }

    @Test
    public void formatFixEstimatedIsOmitted() {
        assertNull(formatFix(6));
    }

    @Test
    public void formatFixManualIsOmitted() {
        assertNull(formatFix(7));
    }

    @Test
    public void formatFixNull() {
        assertNull(formatFix(null));
    }
}
