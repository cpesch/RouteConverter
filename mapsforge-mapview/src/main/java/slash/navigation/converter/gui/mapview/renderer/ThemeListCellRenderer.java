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
package slash.navigation.converter.gui.mapview.renderer;

import slash.navigation.maps.Theme;

import javax.swing.*;
import java.awt.*;

/**
 * Renders the {@link Theme} labels of the map and theme selector combo box.
 *
 * @author Christian Pesch
 */

public class ThemeListCellRenderer extends DefaultListCellRenderer {
    private static final int MAXIMUM_NAME_LENGTH = 40;

    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        Theme theme = (Theme) value;

        String text = "?";
        String tooltip = "";
        if (theme != null) {
            text = shortenName(theme.getDescription());
            tooltip = theme.getUrl();
        }
        label.setText(text);
        label.setToolTipText(tooltip);
        return label;
    }

    static String shortenName(String name) {
        return name.length() > MAXIMUM_NAME_LENGTH ? name.substring(0, MAXIMUM_NAME_LENGTH) + "..." : name;
    }
}
