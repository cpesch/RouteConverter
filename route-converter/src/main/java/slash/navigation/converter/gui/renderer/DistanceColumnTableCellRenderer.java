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

import slash.common.io.Transfer;

import javax.swing.*;
import java.awt.*;
import java.util.prefs.Preferences;

import static java.lang.Math.abs;
import static java.lang.Math.round;

/**
 * Renders the distance column of the positions table.
 *
 * @author Christian Pesch
 */

public class DistanceColumnTableCellRenderer extends AlternatingColorTableCellRenderer {
    private static final Preferences preferences = Preferences.userNodeForPackage(DistanceColumnTableCellRenderer.class);
    private static final double maximumDistanceDisplayedInMeters = preferences.getDouble("maximumDistanceDisplayedInMeters", 10000.0);
    private static final double maximumDistanceDisplayedInHundredMeters = preferences.getDouble("maximumDistanceDisplayedInHundredMeters", 200000.0);

    private String formatDistance(double distance) {
        if (distance <= 0.0)
            return "";
        if (abs(distance) < maximumDistanceDisplayedInMeters)
            return round(distance) + " m";
        if (abs(distance) < maximumDistanceDisplayedInHundredMeters)
            return Transfer.roundFraction(distance / 1000.0, 1) + " Km";
        return round(distance / 1000.0) + " Km";
    }

    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int rowIndex, int columnIndex) {
        JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, rowIndex, columnIndex);
        label.setHorizontalAlignment(RIGHT);
        Double distance = Double.class.cast(value);
        label.setText(formatDistance(distance));
        return label;
    }
}
