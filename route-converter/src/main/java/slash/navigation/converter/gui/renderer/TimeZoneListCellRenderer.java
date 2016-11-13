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

import javax.swing.*;
import java.awt.*;
import java.util.TimeZone;

/**
 * Renders the {@link TimeZone} labels of the timezone system combo box.
 *
 * @author Christian Pesch
 */

public class TimeZoneListCellRenderer extends DefaultListCellRenderer {
    private static final String GMT_PLUS = "Etc/GMT+";
    private static final String GMT_MINUS = "Etc/GMT-";

    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        TimeZone timeZone = TimeZone.class.cast(value);
        String id = timeZone.getID();
        if(id.contains(GMT_PLUS))
            id = id.replace(GMT_PLUS, GMT_MINUS);
        else if(id.contains(GMT_MINUS))
            id = id.replace(GMT_MINUS, GMT_PLUS);
        String text = id + " (" +  timeZone.getDisplayName() + ")";
        label.setText(text);
        return label;
    }
}
