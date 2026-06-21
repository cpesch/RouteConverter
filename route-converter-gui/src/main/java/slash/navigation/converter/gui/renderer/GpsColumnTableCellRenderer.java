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
import slash.navigation.converter.gui.RouteConverter;
import slash.navigation.photo.PhotoPosition;
import slash.navigation.photo.TagState;

import javax.swing.*;
import java.awt.*;
import java.text.MessageFormat;

import static slash.navigation.converter.gui.helpers.PositionHelper.*;
import static slash.navigation.photo.TagState.*;

/**
 * Renders the GPS column of the photos table.
 *
 * @author Christian Pesch
 */

public class GpsColumnTableCellRenderer extends AlternatingColorTableCellRenderer {
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int rowIndex, int columnIndex) {
        JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, rowIndex, columnIndex);
        PhotoPosition position = (PhotoPosition) value;
        TagState tagState = position.getTagState();

        String text = "?";
        if(tagState.equals(Tagged))
            text = MessageFormat.format(RouteConverter.getBundle().getString("gps-data-tagged"),
                    formatDate(position.getTime()), formatTime(position.getTime()),
                    formatLongitude(position.getLongitude()), formatLatitude(position.getLatitude()),
                    formatElevation(position.getElevation()), formatSpeed(position.getSpeed()));
        else if (tagState.equals(Taggable)) {
            NavigationPosition closestPositionForTagging = position.getClosestPositionForTagging();
            text = MessageFormat.format(RouteConverter.getBundle().getString("gps-data-taggable"),
                    formatDate(closestPositionForTagging.getTime()), formatTime(closestPositionForTagging.getTime()),
                    closestPositionForTagging.getDescription(),
                    formatLongitude(closestPositionForTagging.getLongitude()),
                    formatLatitude(closestPositionForTagging.getLatitude()),
                    formatElevation(closestPositionForTagging.getElevation()),
                    formatSpeed(closestPositionForTagging.getSpeed()));
        } else if (tagState.equals(NotTaggable))
            text = MessageFormat.format(RouteConverter.getBundle().getString("gps-data-nottaggable"),
                    formatDate(position.getTime()), formatTime(position.getTime()));
        label.setText(text);
        label.setVerticalAlignment(TOP);
        return label;
    }
}
