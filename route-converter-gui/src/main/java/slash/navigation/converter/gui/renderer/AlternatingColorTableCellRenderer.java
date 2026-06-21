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

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;

/**
 * Renders table cells with alternating background colors.
 *
 * @author Christian Pesch
 */

public class AlternatingColorTableCellRenderer extends DefaultTableCellRenderer {
    private static final Color BACKGROUND_COLOR = new Color(232, 232, 232);
    private static final Color SELECTED_BACKGROUND_COLOR = new Color(0, 9 * 16 + 9, 255);

    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int rowIndex, int columnIndex) {
        JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, rowIndex, columnIndex);
        if (rowIndex % 2 == 0)
            label.setBackground(isSelected ? SELECTED_BACKGROUND_COLOR : table.getBackground());
        else
            label.setBackground(isSelected ? SELECTED_BACKGROUND_COLOR : BACKGROUND_COLOR);
        return label;
    }
}
