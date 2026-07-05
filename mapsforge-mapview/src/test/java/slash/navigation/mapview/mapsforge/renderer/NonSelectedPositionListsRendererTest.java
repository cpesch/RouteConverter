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
package slash.navigation.mapview.mapsforge.renderer;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static slash.navigation.mapview.mapsforge.renderer.NonSelectedPositionListsRenderer.lighten;

public class NonSelectedPositionListsRendererTest {

    @Test
    public void factorZeroKeepsTheColor() {
        assertEquals(0xFF3379FF, lighten(0xFF3379FF, 0.0f));
    }

    @Test
    public void factorOneTurnsRgbWhiteButKeepsAlpha() {
        assertEquals(0xFFFFFFFF, lighten(0xFF3379FF, 1.0f));
        assertEquals(0x80FFFFFF, lighten(0x80000000, 1.0f));
    }

    @Test
    public void factorHalfBlendsHalfwayToWhite() {
        // each channel: c + (255 - c) * 0.5 -> 0 becomes 127
        assertEquals(0xFF7F7F7F, lighten(0xFF000000, 0.5f));
    }

    @Test
    public void alphaIsPreserved() {
        assertEquals(0x80, (lighten(0x803379FF, 0.55f) >>> 24) & 0xFF);
    }

    @Test
    public void lightenedIsBrighterPerChannel() {
        int original = 0xFF2050A0;
        int lightened = lighten(original, 0.55f);
        assertEquals(true, ((lightened >> 16) & 0xFF) > ((original >> 16) & 0xFF));
        assertEquals(true, ((lightened >> 8) & 0xFF) > ((original >> 8) & 0xFF));
        assertEquals(true, (lightened & 0xFF) > (original & 0xFF));
    }
}
