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
package slash.navigation.converter.gui.helpers;

import org.junit.Test;

import java.awt.Point;
import java.awt.Rectangle;

import static org.junit.Assert.assertEquals;
import static slash.navigation.converter.gui.helpers.AbstractTablePopupMenu.keyboardPopupLocation;

/**
 * Testable seam for issue #324: {@link AbstractTablePopupMenu#keyboardPopupLocation} picks where
 * to show the popup when the context-menu key is pressed before any mouse event over the table
 * has set {@code lastMouseEvent}.
 */
public class AbstractTablePopupMenuTest {
    private static final Rectangle VISIBLE_RECT = new Rectangle(0, 0, 500, 300);

    @Test
    public void selectedCellInsideVisibleRectReturnsItsBottomLeftCorner() {
        Rectangle selectedCellRect = new Rectangle(0, 40, 100, 20);
        assertEquals(new Point(0, 60), keyboardPopupLocation(selectedCellRect, VISIBLE_RECT));
    }

    @Test
    public void noSelectionReturnsVisibleRectTopLeftPlusOffset() {
        assertEquals(new Point(10, 10), keyboardPopupLocation(null, VISIBLE_RECT));
    }

    @Test
    public void selectedCellOutsideVisibleRectReturnsVisibleRectTopLeftPlusOffset() {
        Rectangle selectedCellRect = new Rectangle(0, 900, 100, 20);
        assertEquals(new Point(10, 10), keyboardPopupLocation(selectedCellRect, VISIBLE_RECT));
    }
}
