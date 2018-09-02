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
import slash.navigation.base.RouteCharacteristics;
import slash.navigation.base.SimpleLineBasedFormat;
import slash.navigation.base.SimpleRoute;
import slash.navigation.base.WaypointType;
import slash.navigation.base.Wgs84Position;
import slash.navigation.base.Wgs84Route;
import slash.navigation.common.NavigationPosition;

import java.io.PrintWriter;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static slash.common.io.Transfer.trim;
import static slash.common.type.CompactCalendar.createDateFormat;
import static slash.common.type.CompactCalendar.parseDate;
import static slash.navigation.base.RouteCharacteristics.Track;
import static slash.navigation.base.WaypointType.PointOfInterestC;
import static slash.navigation.base.WaypointType.Voice;
import static slash.navigation.base.WaypointType.Waypoint;

/**
 * The base of all Columbus GPS formats.
 *
 * @author Christian Pesch
 */

public abstract class ColumbusGpsFormat extends SimpleLineBasedFormat<SimpleRoute> {
    protected static final Logger log = Logger.getLogger(ColumbusGpsFormat.class.getName());

    protected static final char SEPARATOR = ',';
    protected static final String SPACE_OR_ZERO = "[\\s\u0000]*";
    protected static final String VALID_TAG_VALUES = "CDGTV";
    private static final String DATE_AND_TIME_FORMAT = "yyMMdd HHmmss";
    private static final String DATE_FORMAT = "yyMMdd";
    private static final String TIME_FORMAT = "HHmmss";

    public String getExtension() {
        return ".csv";
    }

    @SuppressWarnings("unchecked")
    public <P extends NavigationPosition> SimpleRoute createRoute(RouteCharacteristics characteristics, String name, List<P> positions) {
        return new Wgs84Route(this, characteristics, (List<Wgs84Position>) positions);
    }

    protected RouteCharacteristics getRouteCharacteristics() {
        return Track;
    }

    protected boolean isValidLine(String line) {
        return isPosition(line) || isHeader(line);
    }

    protected abstract Pattern getLinePattern();

    protected boolean isPosition(String line) {
        Matcher matcher = getLinePattern().matcher(line);
        return matcher.matches() && hasValidFix(line, matcher);
    }

    protected abstract boolean hasValidFix(String line, Matcher matcher);

    protected abstract Pattern getHeaderPattern();

    protected boolean isHeader(String line) {
        Matcher matcher = getHeaderPattern().matcher(line);
        return matcher.matches();
    }

    protected abstract String getHeader();

    protected CompactCalendar parseDateAndTime(String date, String time) {
        date = trim(date);
        time = trim(time);
        if (date == null || time == null)
            return null;
        String dateAndTime = date + " " + time;
        return parseDate(dateAndTime, DATE_AND_TIME_FORMAT);
    }

    protected WaypointType parseTag(String string) {
        WaypointType type = WaypointType.fromValue(string);
        return type != null ? type : Waypoint;
    }

    protected String parseDescription(String description, String index, WaypointType waypointType) {
        int descriptionSeparatorIndex = description.lastIndexOf(SEPARATOR);
        if (descriptionSeparatorIndex != -1)
            description = description.substring(descriptionSeparatorIndex + 1);
        description = trim(description);
        if (description == null)
            description = waypointType + " " + trim(removeZeros(index));
        if (waypointType.equals(Voice) && !description.endsWith(".wav"))
            description += ".wav";
        return description;
    }

    protected String removeZeros(String string) {
        return string != null ? string.replace("\u0000", "") : "";
    }

    protected void writeHeader(PrintWriter writer, SimpleRoute route) {
        writer.println(getHeader());
    }

    protected String fillWithZeros(String string, int length) {
        StringBuilder buffer = new StringBuilder(string != null ? string : "");
        while (buffer.length() < length) {
            buffer.append('\u0000');
        }
        return buffer.toString();
    }

    protected String formatDate(CompactCalendar date) {
        if (date == null)
            return "";
        return createDateFormat(DATE_FORMAT).format(date.getTime());
    }

    protected String formatTime(CompactCalendar time) {
        if (time == null)
            return "";
        return createDateFormat(TIME_FORMAT).format(time.getTime());
    }

    private WaypointType extractWaypointType(Wgs84Position position) {
        WaypointType waypointType = position.getWaypointType();
        if(waypointType != null)
            return waypointType;

        String description = position.getDescription();
        if (description != null) {
            if (description.startsWith("VOX"))
                return Voice;
            if (description.startsWith("POI")) {
                return PointOfInterestC;
            }
        }
        return Waypoint;
    }

    protected String formatTag(Wgs84Position position) {
        return extractWaypointType(position).value();
    }
 }