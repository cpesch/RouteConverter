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
package slash.navigation.converter.gui.models;

import org.junit.Test;

import java.awt.*;
import java.util.UUID;

import static org.junit.Assert.*;

/**
 * Tests for {@link ColorModel}.
 *
 * @author Christian Pesch
 */
public class ColorModelTest {

    /** Returns a fresh prefix that has never been written to Preferences. */
    private static String freshPrefix() {
        return "test-phase4-" + UUID.randomUUID();
    }

    @Test
    public void testGetDefaultColorOpaque() {
        // "ffffffff" ARGB = alpha 255, R 255, G 255, B 255
        ColorModel model = new ColorModel(freshPrefix(), "ffffffff");
        Color color = model.getColor();
        assertNotNull(color);
        assertEquals(255, color.getRed());
        assertEquals(255, color.getGreen());
        assertEquals(255, color.getBlue());
        assertEquals(255, color.getAlpha());
    }

    @Test
    public void testGetDefaultColorTransparent() {
        // "00000000" ARGB = fully transparent black
        ColorModel model = new ColorModel(freshPrefix(), "00000000");
        Color color = model.getColor();
        assertNotNull(color);
        assertEquals(0, color.getRed());
        assertEquals(0, color.getGreen());
        assertEquals(0, color.getBlue());
        assertEquals(0, color.getAlpha());
    }

    @Test
    public void testSetAndGetColorRoundTrip() {
        String prefix = freshPrefix();
        ColorModel model = new ColorModel(prefix, "00000000");
        Color red = new Color(200, 50, 30, 180);
        model.setColor(red);

        ColorModel model2 = new ColorModel(prefix, "ffffffff");
        Color result = model2.getColor();
        assertEquals(red.getRed(),   result.getRed());
        assertEquals(red.getGreen(), result.getGreen());
        assertEquals(red.getBlue(),  result.getBlue());
        assertEquals(red.getAlpha(), result.getAlpha());
    }

    @Test
    public void testChangeListenerFiredOnSetColor() {
        ColorModel model = new ColorModel(freshPrefix(), "00000000");
        int[] callCount = {0};
        model.addChangeListener(e -> callCount[0]++);
        model.setColor(Color.BLUE);
        assertEquals(1, callCount[0]);
    }

    @Test
    public void testChangeListenerRemovedIsNotFired() {
        ColorModel model = new ColorModel(freshPrefix(), "00000000");
        int[] callCount = {0};
        javax.swing.event.ChangeListener listener = e -> callCount[0]++;
        model.addChangeListener(listener);
        model.removeChangeListener(listener);
        model.setColor(Color.GREEN);
        assertEquals(0, callCount[0]);
    }
}

