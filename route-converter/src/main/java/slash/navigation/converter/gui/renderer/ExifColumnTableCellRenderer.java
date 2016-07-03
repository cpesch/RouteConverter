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

import org.apache.commons.imaging.common.RationalNumber;
import org.apache.commons.imaging.formats.tiff.constants.ExifTagConstants;
import slash.navigation.converter.gui.RouteConverter;
import slash.navigation.photo.PhotoPosition;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.Field;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

import static slash.common.type.CompactCalendar.UTC;
import static slash.navigation.converter.gui.helpers.PositionHelper.formatDate;
import static slash.navigation.converter.gui.helpers.PositionHelper.formatTime;

/**
 * Renders the EXIF column of the photos table.
 *
 * @author Christian Pesch
 */

public class ExifColumnTableCellRenderer extends AlternatingColorTableCellRenderer {
    private static final String COMMA = ", ";
    public static final String UTC_TIMEZONE_ID = UTC.getID();
    private static Map<String, Integer> EXIF_FLASH_CONSTANTS = new HashMap<>();

    static {
        for(Field field : ExifTagConstants.class.getFields()) {
            String fieldName = field.getName();
            if (!fieldName.startsWith("FLASH"))
                continue;
            try {
                int fieldValue = field.getInt(null);
                EXIF_FLASH_CONSTANTS.put(fieldName, fieldValue);
            } catch (IllegalAccessException e) {
                // intentionally left empty
            }

        }
    }

    private String getFlash(Integer value) {
        StringBuilder buffer = new StringBuilder();
        if(value != null) {
            for (Map.Entry<String, Integer> entry : EXIF_FLASH_CONSTANTS.entrySet()) {
                int fieldValue = entry.getValue();
                int andValue = value & fieldValue;
                if (andValue == fieldValue)
                    buffer.append(entry.getKey()).append(COMMA);
            }
        } else
            buffer.append("?");

        String result = buffer.toString();
        if(result.endsWith(COMMA))
            result = result.substring(0, result.length() - COMMA.length());

        return result;
    }

    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int rowIndex, int columnIndex) {
        JLabel label = JLabel.class.cast(super.getTableCellRendererComponent(table, value, isSelected, hasFocus, rowIndex, columnIndex));
        PhotoPosition position = PhotoPosition.class.cast(value);
        String exposure = position.getExposure() != null ? position.getExposure().numerator + "/" + position.getExposure().divisor : "?";
        String text = MessageFormat.format(RouteConverter.getBundle().getString("exif-data"),
                formatDate(position.getTime(), UTC_TIMEZONE_ID),
                formatTime(position.getTime(), UTC_TIMEZONE_ID),
                formatString(position.getMake()), formatString(position.getModel()),
                formatString(position.getWidth()), formatString(position.getHeight()),
                formatString(position.getfNumber()), exposure, formatString(position.getFocal()), getFlash(position.getFlash()),
                formatString(position.getPhotographicSensitivity()));
        label.setText(text);
        label.setVerticalAlignment(TOP);
        return label;
    }

    private String formatString(String string) {
        return string != null ? string : "?";
    }

    private String formatString(Integer integer) {
        return integer != null ? integer.toString() : "?";
    }

    private String formatString(RationalNumber rationalNumber) {
        return rationalNumber != null ? rationalNumber.toString() : "?";
    }
}
