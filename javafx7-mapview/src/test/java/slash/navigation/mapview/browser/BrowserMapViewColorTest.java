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

package slash.navigation.mapview.browser;

import static org.junit.Assert.assertEquals;

import static slash.common.type.HexadecimalNumber.decodeInt;

import java.awt.*;

import org.junit.Assert;
import org.junit.Test;

public class BrowserMapViewColorTest {
    private static final Color ROUTE_COLOR = new Color(decodeInt("C86CB1F3"), true);
    private static final Color TRACK_COLOR = new Color(decodeInt("FF0033FF"), true);
    private JavaFX7WebViewMapView view = new JavaFX7WebViewMapView();

    @Test
    public void testAsColor() {
        Assert.assertEquals("6CB1F3", view.asColor(ROUTE_COLOR));
        Assert.assertEquals("0033FF", view.asColor(TRACK_COLOR));
    }

    @Test
    public void testAsOpacity() {
        Assert.assertEquals(0.8f, view.asOpacity(ROUTE_COLOR), 0.05);
        Assert.assertEquals(1.0f, view.asOpacity(TRACK_COLOR), 0.05);
    }

    @Test
    public void testMinimumOpacity() {
        Assert.assertEquals(0.3f, view.asOpacity(new Color(decodeInt("00000000"), true)), 0.05);
    }
}
