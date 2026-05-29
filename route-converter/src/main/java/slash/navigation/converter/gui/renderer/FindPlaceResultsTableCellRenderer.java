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

import slash.navigation.common.NavigationPosition;
import slash.navigation.geocoding.GeocodingResult;

import javax.swing.*;
import java.awt.*;

import static java.util.Objects.requireNonNullElse;
import static slash.navigation.converter.gui.helpers.PositionHelper.formatLatitude;
import static slash.navigation.converter.gui.helpers.PositionHelper.formatLongitude;
import static slash.navigation.converter.gui.models.FindPlaceResultsModel.*;

/**
 * Renders the table cells of the Find Place results table.
 *
 * @author Christian Pesch
 */
public class FindPlaceResultsTableCellRenderer extends AlternatingColorTableCellRenderer {
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
                                                   int rowIndex, int columnIndex) {
        JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, rowIndex, columnIndex);
        GeocodingResult result = (GeocodingResult) value;
        NavigationPosition position = result.position();
        String description = requireNonNullElse(position.getDescription(), "");
        String longitude = formatLongitude(position.getLongitude());
        String latitude = formatLatitude(position.getLatitude());
        String toolTipText = result.geocodingServiceName() + ": " + description + " @ " + longitude + "," + latitude;
        switch (columnIndex) {
            case NAME_COLUMN -> {
                label.setText(description);
                label.setToolTipText(toolTipText);
                label.setHorizontalAlignment(LEFT);
            }
            case LONGITUDE_COLUMN -> {
                label.setText(longitude);
                label.setToolTipText(toolTipText);
                label.setHorizontalAlignment(RIGHT);
            }
            case LATITUDE_COLUMN -> {
                label.setText(latitude);
                label.setToolTipText(toolTipText);
                label.setHorizontalAlignment(RIGHT);
            }
            case GEOCODING_SERVICE_COLUMN -> {
                label.setText(result.geocodingServiceName());
                label.setToolTipText(result.geocodingServiceName());
                label.setHorizontalAlignment(LEFT);
            }
            default -> throw new IllegalArgumentException("Row " + rowIndex + ", column " + columnIndex + " does not exist");
        }
        return label;
    }
}

