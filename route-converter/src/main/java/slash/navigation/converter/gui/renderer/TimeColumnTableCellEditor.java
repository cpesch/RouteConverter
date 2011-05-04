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

import slash.common.io.CompactCalendar;
import slash.navigation.base.BaseNavigationPosition;
import slash.navigation.converter.gui.RouteConverter;

import javax.swing.*;
import java.text.DateFormat;
import java.util.TimeZone;

/**
 * Renders the time column of the positions table.
 *
 * @author Christian Pesch
 */

public class TimeColumnTableCellEditor extends PositionsTableCellEditor {
    private static final DateFormat timeFormat = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM);
    private String currentTimeZone = "";

    public TimeColumnTableCellEditor() {
        super(RIGHT);
    }

    private String formatTime(CompactCalendar time) {
        String timeZonePreference = RouteConverter.getInstance().getTimeZonePreference();
        if (!currentTimeZone.equals(timeZonePreference)) {
            timeFormat.setTimeZone(TimeZone.getTimeZone(timeZonePreference));
            currentTimeZone = timeZonePreference;
        }
        return timeFormat.format(time.getTime());
    }

    protected void formatCell(JLabel label, BaseNavigationPosition position) {
        label.setText(extractValue(position));
    }

    protected String extractValue(BaseNavigationPosition position) {
        CompactCalendar time = position.getTime();
        return time != null ? formatTime(time) : "";
    }
}