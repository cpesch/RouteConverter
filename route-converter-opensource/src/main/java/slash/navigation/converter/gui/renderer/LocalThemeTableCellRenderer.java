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

import slash.navigation.maps.mapsforge.LocalTheme;

import javax.swing.*;
import java.awt.*;

/**
 * Renders the table cells of the available {@link LocalTheme} table.
 *
 * @author Christian Pesch
 */

public class LocalThemeTableCellRenderer extends AlternatingColorTableCellRenderer {
    public static final int DESCRIPTION_COLUMN = 0;

    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int rowIndex, int columnIndex) {
        JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, rowIndex, columnIndex);
        LocalTheme theme = (LocalTheme) value;
        switch (columnIndex) {
            case DESCRIPTION_COLUMN -> {
                label.setText(theme.getDescription());
                label.setToolTipText(theme.getUrl());
            }
            default ->
                    throw new IllegalArgumentException("Row " + rowIndex + ", column " + columnIndex + " does not exist");
        }
        return label;
    }
}
