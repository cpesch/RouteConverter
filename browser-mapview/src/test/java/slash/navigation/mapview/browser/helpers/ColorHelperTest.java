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
package slash.navigation.mapview.browser.helpers;

import org.junit.Test;

import java.awt.Color;

import static org.junit.Assert.*;
import static slash.navigation.mapview.browser.helpers.ColorHelper.*;

/**
 * Tests for {@link ColorHelper} ? opacity and hex-color encoding.
 *
 * @author Christian Pesch
 */
public class ColorHelperTest {

    private static final float DELTA = 0.0001f;

    // ---- asOpacity ----

    @Test
    public void testAsOpacityFullyOpaque() {
        // alpha = 255 -> opacity = 0.3 + 255/256 * 0.7 ? 0.9961
        float opacity = asOpacity(new Color(0, 0, 0, 255));
        assertEquals(0.3f + 255 / 256f * (1 - 0.3f), opacity, DELTA);
        assertTrue("opacity should be close to 1.0", opacity > 0.99f);
    }

    @Test
    public void testAsOpacityFullyTransparent() {
        // alpha = 0 -> opacity = 0.3 + 0/256 * 0.7 = 0.3
        float opacity = asOpacity(new Color(0, 0, 0, 0));
        assertEquals(0.3f, opacity, DELTA);
    }

    @Test
    public void testAsOpacityHalfTransparent() {
        // alpha = 128 -> opacity = 0.3 + 128/256 * 0.7 = 0.3 + 0.35 = 0.65
        float opacity = asOpacity(new Color(0, 0, 0, 128));
        assertEquals(0.3f + 128 / 256f * 0.7f, opacity, DELTA);
    }

    @Test
    public void testAsOpacityMinimumIsThirtyPercent() {
        float opacity = asOpacity(new Color(255, 255, 255, 0));
        assertTrue("minimum opacity is 0.3", opacity >= 0.3f);
    }

    // ---- asColor ----

    @Test
    public void testAsColorBlack() {
        assertEquals("000000", asColor(new Color(0, 0, 0)));
    }

    @Test
    public void testAsColorWhite() {
        assertEquals("FFFFFF", asColor(new Color(255, 255, 255)).toUpperCase());
    }

    @Test
    public void testAsColorRed() {
        String hex = asColor(new Color(255, 0, 0));
        assertTrue("red component should be ff", hex.toLowerCase().startsWith("ff"));
        assertTrue("green component should be 00", hex.substring(2, 4).equalsIgnoreCase("00"));
        assertTrue("blue component should be 00", hex.substring(4, 6).equalsIgnoreCase("00"));
    }

    @Test
    public void testAsColorLength() {
        assertEquals(6, asColor(new Color(100, 150, 200)).length());
    }

    @Test
    public void testAsColorIgnoresAlpha() {
        // asColor only uses R, G, B ? alpha does not appear
        String withoutAlpha = asColor(new Color(10, 20, 30));
        String withAlpha = asColor(new Color(10, 20, 30, 128));
        assertEquals(withoutAlpha, withAlpha);
    }
}

