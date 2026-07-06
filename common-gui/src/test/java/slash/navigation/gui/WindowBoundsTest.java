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

package slash.navigation.gui;

import org.junit.Test;

import java.awt.*;

import static org.junit.Assert.*;
import static slash.navigation.gui.WindowBounds.*;

/**
 * Tests for the pure bounds math of {@link WindowBounds}.
 *
 * @author Christian Pesch
 */
public class WindowBoundsTest {
    private static final Rectangle SCREEN = new Rectangle(0, 0, 1920, 1080);
    private static final Insets NO_INSETS = new Insets(0, 0, 0, 0);

    @Test
    public void cropClampsIntoRange() {
        assertEquals(50, crop("v", 10, 50, 100));
        assertEquals(100, crop("v", 200, 50, 100));
        assertEquals(75, crop("v", 75, 50, 100));
    }

    @Test
    public void cropPreservesUnsetSentinel() {
        assertEquals(-1, crop("v", -1, 50, 100));
    }

    @Test
    public void computeSizeReturnsNullWhenUnset() {
        assertNull(computeSize(-1, -1, SCREEN, NO_INSETS));
    }

    @Test
    public void computeSizeReturnsStoredSizeWithinScreen() {
        assertEquals(new Dimension(800, 600), computeSize(800, 600, SCREEN, NO_INSETS));
    }

    @Test
    public void computeSizeClampsToUsableScreen() {
        Insets insets = new Insets(0, 0, 40, 0);
        Dimension size = computeSize(5000, 5000, SCREEN, insets);
        assertNotNull(size);
        assertEquals(1920, size.width);
        assertEquals(1040, size.height);
    }

    @Test
    public void computeSizeReturnsNullBelowMinimumGuard() {
        // width <= 120 or height <= 60 must fall back to the packed size
        assertNull(computeSize(100, 600, SCREEN, NO_INSETS));
        assertNull(computeSize(800, 50, SCREEN, NO_INSETS));
    }

    @Test
    public void computeLocationReturnsNullWhenUnset() {
        assertNull(computeLocation(-1, -1, 800, 600, SCREEN, NO_INSETS));
    }

    @Test
    public void computeLocationReturnsStoredLocationOnScreen() {
        assertEquals(new Point(100, 200), computeLocation(100, 200, 800, 600, SCREEN, NO_INSETS));
    }

    @Test
    public void computeLocationClampsOffScreenBackOn() {
        // a window pushed off the right/bottom edge is pulled back so it stays reachable
        Point location = computeLocation(5000, 5000, 800, 600, SCREEN, NO_INSETS);
        assertNotNull(location);
        assertEquals(1920 - 800, location.x);
        assertEquals(1080 - 600, location.y);
    }
}
