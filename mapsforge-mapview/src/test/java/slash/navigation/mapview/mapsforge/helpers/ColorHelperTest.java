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
package slash.navigation.mapview.mapsforge.helpers;

import static org.junit.Assert.assertEquals;

import static slash.common.type.HexadecimalNumber.decodeInt;
import static slash.navigation.mapview.mapsforge.helpers.ColorHelper.asRGBA;

import java.awt.*;

import org.junit.Test;

public class ColorHelperTest {
    private static final Color ROUTE_COLOR = new Color(decodeInt("C86CB1F3"), true);
    private static final Color TRACK_COLOR = new Color(decodeInt("FF0033FF"), true);

    @Test
    public void testAsRGBA() {
        assertEquals(-76762637, asRGBA(ROUTE_COLOR));
        assertEquals(-16763905, asRGBA(TRACK_COLOR));
    }

    @Test
    public void testMinimumAlpha() {
        assertEquals((int)(256 * 0.3) << 24, asRGBA(new Color(decodeInt("00000000"), true)), 0.05);
        assertEquals(((int)(256 * 0.3) << 24) | 2 << 16 | 3 << 8 | 4, asRGBA(new Color(decodeInt("00020304"), true)), 0.05);
    }
}
