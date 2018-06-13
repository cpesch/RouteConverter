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
import slash.navigation.maps.mapsforge.LocalMap;
import slash.navigation.maps.mapsforge.impl.VectorMap;

import javax.swing.*;
import java.awt.*;

/**
 * Renders the {@link LocalMap} labels of the map and theme selector combo box.
 *
 * @author Christian Pesch
 */

public class MapListCellRenderer extends DefaultListCellRenderer {
    public static final LocalMap SEPARATOR_TO_DOWNLOAD_MAP = new VectorMap(null, null, null, null);
    public static final LocalMap DOWNLOAD_MAP = new VectorMap(null, null, null, null);
    private static final JSeparator SEPARATOR = new JSeparator();

    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        if (SEPARATOR_TO_DOWNLOAD_MAP.equals(value))
            return SEPARATOR;

        LocalMap map = (LocalMap) value;
        String text = "?";
        String tooltip = "";
        if (DOWNLOAD_MAP.equals(value)) {
            text = Application.getInstance().getContext().getBundle().getString("download-map-text");
            tooltip = Application.getInstance().getContext().getBundle().getString("download-map-tooltip");
        } else if (map != null) {
            text = map.getDescription();
            if(!map.isVector())
                text = text + " (" + Application.getInstance().getContext().getBundle().getString("online") + ")";
            tooltip = map.getUrl();
        }

        JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        label.setText(text);
        label.setToolTipText(tooltip);
        return label;
    }
}
