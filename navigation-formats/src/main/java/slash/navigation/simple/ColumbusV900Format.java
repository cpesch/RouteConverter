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

package slash.navigation.simple;

import slash.common.type.CompactCalendar;
import slash.navigation.common.NavigationPosition;
import slash.navigation.base.RouteCharacteristics;
import slash.navigation.base.SimpleLineBasedFormat;
import slash.navigation.base.SimpleRoute;
import slash.navigation.base.Wgs84Position;
import slash.navigation.base.Wgs84Route;

import java.io.PrintWriter;
import java.util.List;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static slash.common.io.Transfer.trim;
import static slash.common.type.CompactCalendar.createDateFormat;
import static slash.common.type.CompactCalendar.parseDate;
import static slash.navigation.base.RouteCharacteristics.Track;

/**
 * The base of all Columbus V900 formats.
 *
 * @author Christian Pesch
 */

public abstract class ColumbusV900Format extends SimpleLineBasedFormat<SimpleRoute> {
    private static final Preferences preferences = Preferences.userNodeForPackage(ColumbusV900Format.class);
    protected static final Logger log = Logger.getLogger(ColumbusV900Format.class.getName());

    protected static final char SEPARATOR = ',';
    protected static final String SPACE_OR_ZERO = "[\\s\u0000]*";
    protected static final String WAYPOINT_POSITION = "T";
    protected static final String VOICE_POSITION = "V";
    protected static final String POI_POSITION = "C";

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
        return matcher.matches() && hasValidFix(line, trim(matcher.group(2)), "G");
    }

    protected abstract Pattern getHeaderPattern();

    protected boolean isHeader(String line) {
        Matcher matcher = getHeaderPattern().matcher(line);
        return matcher.matches();
    }

    protected abstract String getHeader();

    private boolean hasValidFix(String line, String field, String valueThatIndicatesNoFix) {
        if (field != null && field.equals(valueThatIndicatesNoFix)) {
            log.severe("Fix for '" + line + "' is invalid. Contains '" + valueThatIndicatesNoFix + "'");
            return preferences.getBoolean("ignoreInvalidFix", false);
        }
        return true;
    }

    protected CompactCalendar parseDateAndTime(String date, String time) {
        date = trim(date);
        time = trim(time);
        if(date == null || time == null)
            return null;
        String dateAndTime = date + " " + time;
        return parseDate(dateAndTime, DATE_AND_TIME_FORMAT);
    }

    protected String removeZeros(String string) {
        return string != null ? string.replace('\u0000', ' ') : "";
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

    protected String formatLineType(String description) {
        if (description != null) {
            if (description.startsWith("VOX"))
                return VOICE_POSITION;
            if (description.startsWith("POI")) {
                return POI_POSITION;
            }
        }
        return WAYPOINT_POSITION;
    }
}