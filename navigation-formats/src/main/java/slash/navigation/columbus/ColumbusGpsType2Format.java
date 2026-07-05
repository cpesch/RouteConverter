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

package slash.navigation.columbus;

import slash.common.type.CompactCalendar;
import slash.navigation.base.ParserContext;
import slash.navigation.base.Wgs84Position;

import java.io.PrintWriter;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static slash.common.io.Transfer.*;
import static slash.navigation.base.RouteComments.isDefaultDescription;
import static slash.navigation.columbus.ColumbusV1000Device.getTimeZone;
import static slash.navigation.columbus.ColumbusV1000Device.getUseLocalTimeZone;

/**
 * Reads and writes Columbus GPS Type 2 (.csv) files.
 *
 * Type A:
 *
 * Header: INDEX,TAG,DATE,TIME,LATITUDE N/S,LONGITUDE E/W,HEIGHT,SPEED,HEADING,PRES,TEMP
 * Format: 7,T,160325,151927,26.097885N,119.265160E,-25,39.3,83,1020.2,17
 *
 * Type B:
 *
 * Header: INDEX,TAG,DATE,TIME,LATITUDE N/S,LONGITUDE E/W,HEIGHT,SPEED,HEADING
 * Format: 7,T,160325,151927,26.097885N,119.265160E,-25,39.3,83
 *
 * @author Christian Pesch
 */

public class ColumbusGpsType2Format extends ColumbusGpsFormat {
    private static final String COMMON_HEADER = "INDEX,TAG,DATE,TIME,LATITUDE N/S,LONGITUDE E/W,HEIGHT,SPEED,HEADING";
    private static final String TYPE_A_HEADER = ",PRES,TEMP";
    private static final String HEADER_LINE = COMMON_HEADER + TYPE_A_HEADER;
    private static final Pattern HEADER_PATTERN = Pattern.compile(COMMON_HEADER + "(" + TYPE_A_HEADER + ")?");
    private static final Pattern LINE_PATTERN = Pattern.
            compile(BEGIN_OF_LINE +
                    field("\\d+") +                         // 1 index
                    field("[" + VALID_TAG_VALUES + "]") +   // 2 tag
                    field("\\d*") +                         // 3 date
                    field("\\d*") +                         // 4 time
                    coordinate("[NS]") +                    // 5 latitude, 6 hemisphere
                    coordinate("[WE]") +                    // 7 longitude, 8 hemisphere
                    field("[-\\d]+") +                      // 9 height
                    field("[\\d\\.]+") +                    // 10 speed
                    lastField("\\d*") +                     // 11 heading
                    "(" + SEPARATOR +                       // 12 optional Type-A block
                    field("[\\d\\.]+") +                    // 13 pressure
                    field("[-\\d]+") + "?" +                // 14 temperature (trailing separator optional)
                    lastField("[^" + SEPARATOR + "]*") +    // 15 description
                    ")?" +
                    END_OF_LINE);

    public String getName() {
        return "Columbus GPS Type 2 (*" + getExtension() + ")";
    }

    protected Pattern getLinePattern() {
        return LINE_PATTERN;
    }


    protected boolean hasValidFix(String line, Matcher matcher) {
        return true;
    }

    protected String getHeader() {
        return HEADER_LINE;
    }

    protected Pattern getHeaderPattern() {
        return HEADER_PATTERN;
    }

    protected Wgs84Position parsePosition(String line, ParserContext context) {
        Matcher lineMatcher = LINE_PATTERN.matcher(line);
        if (!lineMatcher.matches())
            throw new IllegalArgumentException("'" + line + "' does not match");
        boolean isTypeA = trim(lineMatcher.group(12)) != null;
        CompactCalendar dateAndTime = parseDateAndTime(lineMatcher.group(3), lineMatcher.group(4));
        if (getUseLocalTimeZone())
            dateAndTime = dateAndTime.asUTCTimeInTimeZone(TimeZone.getTimeZone(getTimeZone()));
        Wgs84Position position = parseCommonPosition(lineMatcher, context, dateAndTime, 15, isTypeA);
        position.setPressure(parseDouble(lineMatcher.group(13)));
        position.setTemperature(parseDouble(lineMatcher.group(14)));
        return position;
    }

    protected void writePosition(Wgs84Position position, PrintWriter writer, int index, boolean firstPosition) {
        String speed = position.getSpeed() != null ? formatDoubleAsString(position.getSpeed()) : "0";
        String heading = fillWithZeros(position.getHeading() != null ? formatIntAsString(position.getHeading().intValue()) : "0", 3);
        String pressure = position.getPressure() != null ? formatDoubleAsString(position.getPressure()) : "0";
        String temperature = fillWithZeros(position.getTemperature() != null ? formatIntAsString(position.getTemperature().intValue()) : "0", 2);
        String description = !isDefaultDescription(position.getDescription()) ? position.getDescription() : "";

        writer.println(formatCommonPrefix(position, index) + SEPARATOR +
                speed + SEPARATOR +
                heading + SEPARATOR +
                pressure + SEPARATOR +
                temperature + SEPARATOR +
                fillWithZeros(escape(description, SEPARATOR, ';'), 8));
    }
}
