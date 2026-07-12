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

import slash.navigation.gui.Application;
import slash.navigation.routing.RoutingService;

import javax.swing.*;
import java.awt.*;
import java.util.MissingResourceException;

/**
 * Renders the {@link RoutingService} labels of the routing service combo box.
 *
 * @author Christian Pesch
 */

public class RoutingServiceListCellRenderer extends DefaultListCellRenderer {
    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        RoutingService service = (RoutingService) value;
        String text = getLabelFor(service);
        if(!service.isDownload())
            text = text + " (" + Application.getInstance().getContext().getBundle().getString("online") + ")";
        label.setText(text);
        return label;
    }

    // Decouples the displayed label from RoutingService#getName(), which is a frozen
    // persistence id (stored as the routingService preference and as a key suffix for the
    // per-service travel-mode/avoid preferences - see RoutingPreferencesModel). A service with
    // a "routing-service-<name>" bundle key gets a localized label (e.g. Beeline -> "Straight
    // line" / "Luftlinie"); every other service falls back to its raw name unchanged.
    private String getLabelFor(RoutingService service) {
        String name = service.getName();
        try {
            return Application.getInstance().getContext().getBundle().getString("routing-service-" + name.toLowerCase());
        } catch (MissingResourceException e) {
            return name;
        }
    }
}

