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

import slash.navigation.gui.Application;
import slash.navigation.maps.LocalTheme;
import slash.navigation.maps.impl.VectorTheme;

import javax.swing.*;
import java.awt.*;

/**
 * Renders the {@link LocalTheme} labels of the map and theme selector combo box.
 *
 * @author Christian Pesch
 */

public class ThemeListCellRenderer extends DefaultListCellRenderer {
    public static final LocalTheme SEPARATOR_TO_DOWNLOAD_THEME = new VectorTheme(null, null, null);
    public static final LocalTheme DOWNLOAD_THEME = new VectorTheme(null, null, null);
    private static final JSeparator SEPARATOR = new JSeparator();

    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        if (SEPARATOR_TO_DOWNLOAD_THEME.equals(value))
            return SEPARATOR;

        LocalTheme theme = (LocalTheme) value;
        String text = "?";
        String tooltip = "";
        if (DOWNLOAD_THEME.equals(value)) {
            text = Application.getInstance().getContext().getBundle().getString("download-theme-text");
            tooltip = Application.getInstance().getContext().getBundle().getString("download-theme-tooltip");
        } else if (theme != null) {
            text = theme.getDescription();
            tooltip = theme.getUrl();
        }

        JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        label.setText(text);
        label.setToolTipText(tooltip);
        return label;
    }
}
