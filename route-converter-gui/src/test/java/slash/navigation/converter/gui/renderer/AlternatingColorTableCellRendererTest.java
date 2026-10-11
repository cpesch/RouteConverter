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

package slash.navigation.converter.gui.renderer;

import org.junit.Test;

import javax.swing.*;
import java.awt.*;

import static org.junit.Assert.assertEquals;
import static slash.navigation.converter.gui.renderer.AlternatingColorTableCellRenderer.getAlternateBackground;

/**
 * Tests for {@link AlternatingColorTableCellRenderer}.
 *
 * @author Christian Pesch
 */
public class AlternatingColorTableCellRendererTest {

    @Test
    public void testGetAlternateBackgroundOnLightTheme() {
        JTable table = new JTable();
        table.setBackground(Color.WHITE);
        table.setForeground(Color.BLACK);
        assertEquals(new Color(232, 232, 232), getAlternateBackground(table));
    }

    @Test
    public void testGetAlternateBackgroundOnDarkTheme() {
        JTable table = new JTable();
        table.setBackground(Color.BLACK);
        table.setForeground(Color.WHITE);
        assertEquals(new Color(23, 23, 23), getAlternateBackground(table));
    }
}
