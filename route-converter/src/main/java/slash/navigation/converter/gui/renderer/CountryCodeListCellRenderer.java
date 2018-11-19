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

import slash.navigation.converter.gui.RouteConverter;
import slash.navigation.fpl.CountryCode;

import javax.swing.*;
import java.awt.*;

import static slash.navigation.fpl.CountryCode.None;

/**
 * Renders the {@link CountryCode} labels of the complete flight plan country code combo box.
 *
 * @author Christian Pesch
 */

public class CountryCodeListCellRenderer extends DefaultListCellRenderer {
    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        CountryCode countryCode = (CountryCode) value;
        String text;
        if (countryCode != null) {
            if (None.equals(countryCode))
                text = RouteConverter.getBundle().getString("country-code-none");
            else
                text = countryCode.name().replaceAll("_", " ") +
                        " (" + countryCode.value() + ")";
        } else
            text = null;
        label.setText(text);
        return label;
    }
}
