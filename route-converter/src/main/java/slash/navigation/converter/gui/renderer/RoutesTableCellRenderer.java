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

import slash.navigation.catalog.domain.Route;
import slash.navigation.converter.gui.RouteConverter;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;

/**
 * Renders the table cells of the positions table.
 *
 * @author Christian Pesch
 */

public class RoutesTableCellRenderer extends AlternatingColorTableCellRenderer {

    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int rowIndex, int columnIndex) {
        JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, rowIndex, columnIndex);
        Route route = (Route) value;
        switch (columnIndex) {
            case 0:
                try {
                    String name = route.getName();
                    if (name == null)
                        name = RouteConverter.getBundle().getString("no-name");
                    label.setText(name);
                } catch (IOException e) {
                    label.setText(RouteConverter.getBundle().getString("loading"));
                }
                break;
            case 1:
                try {
                    String description = route.getDescription();
                    if (description == null)
                        description = RouteConverter.getBundle().getString("no-description");
                    label.setText(description);
                } catch (IOException e) {
                    label.setText(RouteConverter.getBundle().getString("loading"));
                }
                break;
            case 2:
                try {
                    String creator = route.getCreator();
                    if (creator == null)
                        creator = RouteConverter.getBundle().getString("no-creator");
                    label.setText(creator);
                } catch (IOException e) {
                    label.setText(RouteConverter.getBundle().getString("loading"));
                }
                break;
            default:
                throw new IllegalArgumentException("Row " + rowIndex + ", column " + columnIndex + " does not exist");
        }
        return label;
    }
}
