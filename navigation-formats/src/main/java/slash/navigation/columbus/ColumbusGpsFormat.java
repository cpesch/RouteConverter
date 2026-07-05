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
import slash.navigation.base.*;
import slash.navigation.common.NavigationPosition;

import java.io.File;
import java.io.PrintWriter;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.Math.abs;
import static slash.common.io.Transfer.*;
import static slash.common.type.CompactCalendar.createDateFormat;
import static slash.common.type.CompactCalendar.parseDate;
import static slash.navigation.base.RouteCharacteristics.Track;
import static slash.navigation.base.WaypointType.*;

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
        return new Wgs84Route(this, characteristics, name, (List<Wgs84Position>) positions);
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

    // --- shared LINE_PATTERN building blocks ---

    /**
     * A comma-terminated capturing field surrounded by optional whitespace/NUL padding.
     */
    protected static String field(String regex) {
        return SPACE_OR_ZERO + "(" + regex + ")" + SPACE_OR_ZERO + SEPARATOR;
    }

    /**
     * A capturing field without a trailing separator (the last field of a line).
     */
    protected static String lastField(String regex) {
        return SPACE_OR_ZERO + "(" + regex + ")" + SPACE_OR_ZERO;
    }

    /**
     * A coordinate field: a decimal number followed by its hemisphere letter, each captured.
     */
    protected static String coordinate(String hemisphere) {
        return SPACE_OR_ZERO + "([\\d\\.]+)(" + hemisphere + ")" + SPACE_OR_ZERO + SEPARATOR;
    }

    // --- shared read/write of the fields common to all Columbus line formats ---

    /**
     * Parses the fields common to every Columbus line format (index, tag, date/time,
     * latitude/longitude with hemisphere sign, height, speed, heading, description) and
     * builds the position; format-specific fields (dop/pressure/temperature) are set by callers.
     *
     * @param descriptionGroup the matcher group holding the description/voice column
     * @param isTypeA          whether the line has the extended (Type-A) columns, enabling a voice file
     */
    protected Wgs84Position parseCommonPosition(Matcher matcher, ParserContext context, CompactCalendar dateAndTime,
                                                int descriptionGroup, boolean isTypeA) {
        WaypointType waypointType = parseTag(trim(matcher.group(2)));
        Double latitude = parseDouble(matcher.group(5));
        if ("S".equals(matcher.group(6)) && latitude != null)
            latitude = -latitude;
        Double longitude = parseDouble(matcher.group(7));
        if ("W".equals(matcher.group(8)) && longitude != null)
            longitude = -longitude;
        String description = parseDescription(removeZeros(matcher.group(descriptionGroup)), removeZeros(matcher.group(1)), waypointType);

        Wgs84Position position = new Wgs84Position(longitude, latitude, parseDouble(matcher.group(9)), parseDouble(matcher.group(10)),
                dateAndTime, description,
                context.getFile() != null && isTypeA ? new File(context.getFile().getParentFile(), description) : null);
        position.setWaypointType(waypointType);
        position.setHeading(parseDouble(matcher.group(11)));
        return position;
    }

    protected String latitudeHemisphere(Wgs84Position position) {
        return position.getLatitude() != null && position.getLatitude() < 0.0 ? "S" : "N";
    }

    protected String longitudeHemisphere(Wgs84Position position) {
        return position.getLongitude() != null && position.getLongitude() < 0.0 ? "W" : "E";
    }

    /**
     * Formats the leading columns shared by all Columbus line formats up to and
     * including height: index, tag, date, time, latitude+hemisphere, longitude+hemisphere, height.
     */
    protected String formatCommonPrefix(Wgs84Position position, int index) {
        String date = fillWithZeros(formatDate(position.getTime()), 6);
        String time = fillWithZeros(formatTime(position.getTime()), 6);
        String latitude = formatDoubleAsString(abs(position.getLatitude()), 6);
        String longitude = formatDoubleAsString(abs(position.getLongitude()), 6);
        String height = fillWithZeros(position.getElevation() != null ? formatIntAsString(position.getElevation().intValue()) : "0", 5);
        return fillWithZeros(Integer.toString(index + 1), 6) + SEPARATOR +
                formatTag(position) + SEPARATOR +
                date + SEPARATOR + time + SEPARATOR +
                latitude + latitudeHemisphere(position) + SEPARATOR +
                longitude + longitudeHemisphere(position) + SEPARATOR +
                height;
    }
 }
