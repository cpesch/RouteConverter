/*
 *
 *     This file is part of RouteConverter.
 *
 *     RouteConverter is free software; you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation; either version 2 of the License, or
 *     (at your option) any later version.
 *
 *     RouteConverter is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with RouteConverter; if not, write to the Free Software
 *     Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 *
 *     Copyright (C) 2007 Christian Pesch. All Rights Reserved.
 * /
 */

package slash.navigation.converter.gui.renderer;

import slash.navigation.converter.gui.RouteConverter;
import slash.navigation.converter.gui.helpers.MapViewImplementation;
import slash.navigation.gui.Application;
import slash.navigation.mapview.MapView;

import javax.swing.*;
import java.awt.*;
import java.util.MissingResourceException;

/**
 * Renders the {@link MapView} labels of the map view combo box.
 *
 * @author Christian Pesch
 */

public class MapViewListCellRenderer extends DefaultListCellRenderer {
    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        MapViewImplementation mapView = (MapViewImplementation) value;
        String text;
        try {
            text = RouteConverter.getBundle().getString("map-view-" + mapView.name().toLowerCase());
        } catch (MissingResourceException e) {
            text = mapView.name();
        }
        if (!mapView.isDownload())
            text = text + " (" + Application.getInstance().getContext().getBundle().getString("online") + ")";
        label.setText(text);
        return label;
    }
}
