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

import slash.navigation.maps.LocalMap;

import javax.swing.*;
import java.awt.*;

/**
 * Renders the table cells of the available maps table.
 *
 * @author Christian Pesch
 */

public class MapsTableCellRenderer extends AlternatingColorTableCellRenderer {
    private final JCheckBox checkBox = new JCheckBox();

    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int rowIndex, int columnIndex) {
        Component component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, rowIndex, columnIndex);
        LocalMap map = (LocalMap) value;
        switch (columnIndex) {
            case 0:
                JLabel label = (JLabel) component;
                label.setText(map.getDescription());
                label.setToolTipText(map.getUrl());
                break;
            case 1:
                checkBox.setBackground(component.getBackground());
                checkBox.setSelected(map.isRenderer());
                component = checkBox;
                break;
            default:
                throw new IllegalArgumentException("Row " + rowIndex + ", column " + columnIndex + " does not exist");
        }
        return component;
    }
}
