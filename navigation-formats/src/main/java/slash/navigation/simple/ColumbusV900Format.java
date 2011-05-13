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

import slash.common.io.CompactCalendar;
import slash.common.io.Transfer;
import slash.navigation.base.*;

import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The base of all Columbus V900 formats.
 *
 * @author Christian Pesch
 */

public abstract class ColumbusV900Format extends SimpleLineBasedFormat<SimpleRoute> {
    protected static final Logger log = Logger.getLogger(ColumbusV900Format.class.getName());

    protected static final char SEPARATOR = ',';
    protected static final String SPACE_OR_ZERO = "[\\s\u0000]*";
    protected static final String WAYPOINT_POSITION = "T";
    protected static final String VOICE_POSITION = "V";
    protected static final String POI_POSITION = "C";

    private static final DateFormat DATE_AND_TIME_FORMAT = new SimpleDateFormat("yyMMdd HHmmss");
    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyMMdd");
    private static final DateFormat TIME_FORMAT = new SimpleDateFormat("HHmmss");
    static {
        DATE_AND_TIME_FORMAT.setTimeZone(CompactCalendar.UTC);
        DATE_FORMAT.setTimeZone(CompactCalendar.UTC);
        TIME_FORMAT.setTimeZone(CompactCalendar.UTC);
    }

    public String getExtension() {
        return ".csv";
    }

    @SuppressWarnings("unchecked")
    public <P extends BaseNavigationPosition> SimpleRoute createRoute(RouteCharacteristics characteristics, String name, List<P> positions) {
        return new Wgs84Route(this, characteristics, (List<Wgs84Position>) positions);
    }

    protected RouteCharacteristics getRouteCharacteristics() {
        return RouteCharacteristics.Track;
    }

    protected abstract String getHeader();

    protected boolean isValidLine(String line) {
        return isPosition(line) || line != null && line.startsWith(getHeader());
    }

    protected abstract Pattern getPattern();

    protected boolean isPosition(String line) {
        Matcher matcher = getPattern().matcher(line);
        return matcher.matches();
    }

    protected CompactCalendar parseDateAndTime(String date, String time) {
        date = Transfer.trim(date);
        time = Transfer.trim(time);
        if(date == null || time == null)
            return null;
        String dateAndTime = date + " " + time;
        try {
            Date parsed = DATE_AND_TIME_FORMAT.parse(dateAndTime);
            return CompactCalendar.fromDate(parsed);
        } catch (ParseException e) {
            log.severe("Could not parse date and time '" + dateAndTime + "'");
        }
        return null;
    }

    protected String removeZeros(String string) {
        return string != null ? string.replace('\u0000', ' ') : "";
    }

    protected void writeHeader(PrintWriter writer) {
        writer.println(getHeader());
    }

    protected String fillWithZeros(String string, int length) {
        StringBuffer buffer = new StringBuffer(string != null ? string : "");
        while (buffer.length() < length) {
            buffer.append('\u0000');
        }
        return buffer.toString();
    }

    protected String formatDate(CompactCalendar date) {
        if (date == null)
            return "";
        return DATE_FORMAT.format(date.getTime());
    }

    protected String formatTime(CompactCalendar time) {
        if (time == null)
            return "";
        return TIME_FORMAT.format(time.getTime());
    }

    protected String formatLineType(String comment) {
        if (comment != null) {
            if (comment.startsWith("VOX"))
                return VOICE_POSITION;
            if (comment.startsWith("POI")) {
                return POI_POSITION;
            }
        }
        return WAYPOINT_POSITION;
    }
}