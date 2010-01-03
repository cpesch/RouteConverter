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

import slash.navigation.gui.Constants;
import slash.navigation.converter.gui.RouteConverter;

import javax.swing.*;
import java.awt.*;
import java.util.Locale;

/**
 * Renders the locale labels of the language combo box.
 *
 * @author Christian Pesch
 */

public class LocaleListCellRenderer extends DefaultListCellRenderer {

    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        Locale locale = (Locale) value;

        String text;
        if (Constants.ROOT_LOCALE.equals(locale))
            text = RouteConverter.getBundle().getString("locale-default");
        else {
            String language = locale.getLanguage();
            String localizedText = RouteConverter.getBundle().getString("locale-" + language);
            if (localizedText != null)
                text = localizedText;
            else
                text = locale.toString();
        }
        label.setText(text);
        return label;
    }
}
