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

import com.bulenkov.iconloader.IconLoader;
import slash.navigation.base.WaypointType;
import slash.navigation.base.Wgs84Position;
import slash.navigation.common.NavigationPosition;
import slash.navigation.converter.gui.RouteConverter;

import javax.swing.*;
import java.awt.*;
import java.util.MissingResourceException;

import static slash.navigation.base.WaypointType.Waypoint;

/**
 * Renders the {@link WaypointType} column of the positions table.
 *
 * @author Christian Pesch
 */

public class WaypointTypeColumnTableCellEditor extends PositionsTableCellEditor {
    public WaypointTypeColumnTableCellEditor() {
        super(LEFT);
    }

    protected void formatLabel(JLabel label, Object value, boolean firstRow, boolean lastRow) {
        if(value instanceof NavigationPosition position) {
            String key = null;
            if (lastRow)
                key = "end";
            else if (firstRow)
                key = "start";
            else {
                WaypointType waypointType = getWaypointType(position);
                if (waypointType != null && waypointType != Waypoint)
                    key = waypointType.name().toLowerCase();
            }

            Icon icon = null;
            if (key != null)
                icon = IconLoader.getIcon("/slash/navigation/converter/gui/waypoint-type/" + key + ".png");

            label.setIcon(icon);
        } else
            label.setText("");
    }

    protected void formatCell(JLabel label, NavigationPosition position) {
        label.setText(extractValue(position));
    }

    protected String extractValue(NavigationPosition position) {
        String text = "?";
        WaypointType waypointType = getWaypointType(position);
        if (waypointType != null) {
            try {
                text = RouteConverter.getBundle().getString("waypoint-type-" + waypointType.name().toLowerCase());
            } catch (MissingResourceException e) {
                text = waypointType.name();
            }
        }
        return text;
    }

    private WaypointType getWaypointType(NavigationPosition position) {
        if (position instanceof Wgs84Position wgs84Position) {
            return wgs84Position.getWaypointType();
        }
        return null;
    }
}
