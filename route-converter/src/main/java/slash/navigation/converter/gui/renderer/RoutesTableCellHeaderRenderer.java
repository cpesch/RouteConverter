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

import slash.navigation.converter.gui.RouteConverter;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;

/**
 * Renders the column header of the routes table.
 *
 * @author Christian Pesch
 */

public class RoutesTableCellHeaderRenderer extends DefaultTableCellRenderer {

    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int rowIndex, int columnIndex) {
        JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, rowIndex, columnIndex);
        label.setHorizontalAlignment(JLabel.LEFT);
        label.setOpaque(false);
        switch (columnIndex) {
            case 0:
                label.setText(RouteConverter.getBundle().getString("number"));
                break;
            case 1:
                label.setText(RouteConverter.getBundle().getString("description"));
                break;
            case 2:
                label.setText(RouteConverter.getBundle().getString("creator"));
                break;
            default:
                throw new IllegalArgumentException("Row " + rowIndex + ", columnIndex " + columnIndex + " does not exist");
        }
        return label;
    }
}
