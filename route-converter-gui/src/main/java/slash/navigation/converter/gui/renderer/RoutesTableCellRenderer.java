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

import slash.navigation.common.DistanceAndTime;
import slash.navigation.converter.gui.models.RouteMetadataSource;
import slash.navigation.routes.impl.RouteModel;

import javax.swing.*;
import java.awt.*;

import static slash.common.io.Transfer.formatDuration;
import static slash.navigation.converter.gui.helpers.PositionHelper.formatDistance;
import static slash.navigation.converter.gui.helpers.RouteHelper.*;
import static slash.navigation.routes.impl.RoutesTableModel.*;

/**
 * Renders the table cells of the routes table.
 *
 * @author Christian Pesch
 */

public class RoutesTableCellRenderer extends AlternatingColorTableCellRenderer {
    private static final String NO_VALUE = "\u2013";

    private final RouteMetadataSource routeMetadataSource;

    public RoutesTableCellRenderer(RouteMetadataSource routeMetadataSource) {
        this.routeMetadataSource = routeMetadataSource;
    }

    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int rowIndex, int columnIndex) {
        JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, rowIndex, columnIndex);
        RouteModel route = (RouteModel) value;
        int modelColumnIndex = table.convertColumnIndexToModel(columnIndex);
        switch (modelColumnIndex) {
            case NAME_COLUMN -> {
                label.setText(formatDescription(route));
                label.setToolTipText(formatUrl(route));
            }
            case CREATOR_COLUMN -> {
                label.setText(formatCreator(route));
                label.setToolTipText(formatUrl(route));
            }
            case LENGTH_COLUMN -> {
                label.setText(formatLength(getDistanceAndTime(route)));
                label.setToolTipText(formatUrl(route));
            }
            case DURATION_COLUMN -> {
                label.setText(formatTime(getDistanceAndTime(route)));
                label.setToolTipText(formatUrl(route));
            }
            default ->
                    throw new IllegalArgumentException("Row " + rowIndex + ", column " + modelColumnIndex + " does not exist");
        }
        return label;
    }

    private DistanceAndTime getDistanceAndTime(RouteModel route) {
        return routeMetadataSource.getDistanceAndTime(route.getUrl());
    }

    private String formatLength(DistanceAndTime distanceAndTime) {
        if (RouteMetadataSource.hasNoDistance(distanceAndTime))
            return NO_VALUE;
        return formatDistance(distanceAndTime.distance());
    }

    private String formatTime(DistanceAndTime distanceAndTime) {
        if (RouteMetadataSource.hasNoTime(distanceAndTime))
            return NO_VALUE;
        return formatDuration(distanceAndTime.timeInMillis());
    }
}
