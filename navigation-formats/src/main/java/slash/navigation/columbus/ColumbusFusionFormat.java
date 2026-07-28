/*
    This file is part of RouteConverter.

    RouteConverter is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    RouteConverter is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with RouteConverter; if not, write to the Free Software
    Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA

    Copyright (C) 2007 Christian Pesch. All Rights Reserved.
*/
package slash.navigation.columbus;

import slash.navigation.base.ParserContext;
import slash.navigation.base.WaypointType;
import slash.navigation.base.Wgs84Position;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static slash.common.io.Transfer.parseDouble;
import static slash.common.io.Transfer.trim;
import static slash.navigation.columbus.ColumbusV1000Device.getTimeZone;
import static slash.navigation.columbus.ColumbusV1000Device.getUseLocalTimeZone;

/**
 * Reads Columbus Fusion track (.csv) files, a read-only GNSS track format with
 * three possible column layouts (GNSS/GNSS+SAT/GNSS+SAT+FIX), distinguished by
 * the second header line. Empty tag/date columns carry forward from the
 * previous row.
 *
 * @author Christian Pesch
 */
public class ColumbusFusionFormat extends ColumbusGpsFormat {
    private static final Pattern FORMAT_DIRECTIVE_PATTERN =
            Pattern.compile("#\\s*Format=ColumbusFusion;\\s*Version=[\\d.]+;\\s*Type=GNSS\\s*");

    private static final Pattern COLUMN_HEADER_GNSS_PATTERN =
            Pattern.compile("\\s*tag,date,time,lat,lon,alt,speed,heading\\s*");
    private static final Pattern COLUMN_HEADER_GNSS_SAT_PATTERN =
            Pattern.compile("\\s*tag,date,time,lat,lon,alt,speed,heading,sat,hdop\\s*");
    private static final Pattern COLUMN_HEADER_GNSS_SAT_FIX_PATTERN =
            Pattern.compile("\\s*tag,date,time,lat,lon,alt,speed,heading,sat,hdop,fix\\s*");

    static final int LAYOUT_GNSS = 8;
    static final int LAYOUT_GNSS_SAT = 10;
    static final int LAYOUT_GNSS_SAT_FIX = 11;

    private static final String S = ",";
    private static final String TAG = "([CDGT]?)", DATE = "(\\d*)", TIME = "(\\d+)",
            LATLON = "(-?[\\d.]+)", ALT = "([-\\d.]*)", SPEED = "([\\d.]*)", HEAD = "([\\d.]*)",
            SAT = "(\\d*)", HDOP = "([\\d.]*)", FIX = "(\\d*)";

    private static final Pattern LINE_GNSS = Pattern.compile(
            TAG + S + DATE + S + TIME + S + LATLON + S + LATLON + S + ALT + S + SPEED + S + HEAD);
    private static final Pattern LINE_GNSS_SAT = Pattern.compile(
            TAG + S + DATE + S + TIME + S + LATLON + S + LATLON + S + ALT + S + SPEED + S + HEAD + S + SAT + S + HDOP);
    private static final Pattern LINE_GNSS_SAT_FIX = Pattern.compile(
            TAG + S + DATE + S + TIME + S + LATLON + S + LATLON + S + ALT + S + SPEED + S + HEAD + S + SAT + S + HDOP + S + FIX);

    public String getName() {
        return "Columbus Fusion (*" + getExtension() + ")";
    }

    public boolean isSupportsWriting() {
        return false;
    }

    public boolean isSupportsReading() {
        return true;
    }

    protected Pattern getLinePattern() {
        return LINE_GNSS;
    }

    protected boolean hasValidFix(String line, Matcher lineMatcher) {
        return true;
    }

    protected Pattern getHeaderPattern() {
        return FORMAT_DIRECTIVE_PATTERN;
    }

    protected String getHeader() {
        return "# Format=ColumbusFusion; Version=1.0; Type=GNSS\ntag,date,time,lat,lon,alt,speed,heading";
    }

    protected boolean isPosition(String line) {
        return matchesAnyLine(line);
    }

    public boolean isValidLine(String line) {
        return super.isValidLine(line) || isColumnHeader(line);
    }

    private boolean matchesAnyLine(String line) {
        return LINE_GNSS.matcher(line).matches() || LINE_GNSS_SAT.matcher(line).matches() ||
                LINE_GNSS_SAT_FIX.matcher(line).matches();
    }

    private boolean isColumnHeader(String line) {
        return COLUMN_HEADER_GNSS_PATTERN.matcher(line).matches() ||
                COLUMN_HEADER_GNSS_SAT_PATTERN.matcher(line).matches() ||
                COLUMN_HEADER_GNSS_SAT_FIX_PATTERN.matcher(line).matches();
    }

    int detectLayout(String columnHeaderLine) {
        if (COLUMN_HEADER_GNSS_SAT_FIX_PATTERN.matcher(columnHeaderLine).matches())
            return LAYOUT_GNSS_SAT_FIX;
        if (COLUMN_HEADER_GNSS_SAT_PATTERN.matcher(columnHeaderLine).matches())
            return LAYOUT_GNSS_SAT;
        if (COLUMN_HEADER_GNSS_PATTERN.matcher(columnHeaderLine).matches())
            return LAYOUT_GNSS;
        return -1;
    }

    private Pattern linePatternFor(int layout) {
        switch (layout) {
            case LAYOUT_GNSS_SAT_FIX:
                return LINE_GNSS_SAT_FIX;
            case LAYOUT_GNSS_SAT:
                return LINE_GNSS_SAT;
            default:
                return LINE_GNSS;
        }
    }

    public void read(BufferedReader reader, String encoding, ParserContext context) throws IOException {
        String directiveLine = reader.readLine();
        if (directiveLine == null || !FORMAT_DIRECTIVE_PATTERN.matcher(directiveLine).matches())
            return;

        String columnHeaderLine = reader.readLine();
        if (columnHeaderLine == null)
            return;
        int layout = detectLayout(columnHeaderLine);
        if (layout == -1)
            return;

        List<Wgs84Position> positions = parseBody(reader, layout);
        if (!positions.isEmpty())
            context.appendRoute(createRoute(getRouteCharacteristics(), positions));
    }

    List<Wgs84Position> parseBody(BufferedReader reader, int layout) throws IOException {
        Pattern linePattern = linePatternFor(layout);
        List<Wgs84Position> positions = new ArrayList<Wgs84Position>();

        String previousTag = "T";
        String previousDate = null;
        int garbleCount = getGarbleCount();

        String line;
        while ((line = reader.readLine()) != null) {
            if (trim(line) == null)
                continue;

            Matcher lineMatcher = linePattern.matcher(line);
            if (!lineMatcher.matches()) {
                if (garbleCount-- <= 0)
                    break;
                continue;
            }
            garbleCount = getGarbleCount();

            String tag = trim(lineMatcher.group(1));
            if (tag == null)
                tag = previousTag;
            else
                previousTag = tag;

            String date = trim(lineMatcher.group(2));
            if (date == null)
                date = previousDate;
            else
                previousDate = date;

            positions.add(parseRow(lineMatcher, tag, date, layout, positions.size()));
        }
        return positions;
    }

    public Wgs84Position parsePosition(String line, ParserContext context) {
        for (int candidate : new int[]{LAYOUT_GNSS_SAT_FIX, LAYOUT_GNSS_SAT, LAYOUT_GNSS}) {
            Matcher lineMatcher = linePatternFor(candidate).matcher(line);
            if (lineMatcher.matches()) {
                String tag = trim(lineMatcher.group(1));
                if (tag == null)
                    tag = "T";
                String date = trim(lineMatcher.group(2));
                return parseRow(lineMatcher, tag, date, candidate, 0);
            }
        }
        return null;
    }

    private Wgs84Position parseRow(Matcher lineMatcher, String tag, String date, int layout, int index) {
        String time = lineMatcher.group(3);
        Double latitude = parseDouble(lineMatcher.group(4));
        Double longitude = parseDouble(lineMatcher.group(5));
        Double elevation = parseDouble(lineMatcher.group(6));
        Double speed = parseDouble(lineMatcher.group(7));
        Double heading = parseDouble(lineMatcher.group(8));

        WaypointType waypointType = parseTag(tag);

        slash.common.io.CompactCalendar dateAndTime = parseDateAndTime(date, time);
        if (dateAndTime != null && getUseLocalTimeZone())
            dateAndTime = dateAndTime.asUTCTimeInTimeZone(TimeZone.getTimeZone(getTimeZone()));

        String description = parseDescription("", String.valueOf(index + 1), waypointType);

        Wgs84Position position = new Wgs84Position(longitude, latitude, elevation, speed, dateAndTime, description);
        position.setHeading(heading);
        position.setWaypointType(waypointType);

        if (layout == LAYOUT_GNSS_SAT || layout == LAYOUT_GNSS_SAT_FIX) {
            position.setSatellites(parseInteger(lineMatcher.group(9)));
            position.setHdop(parseDouble(lineMatcher.group(10)));
        }
        if (layout == LAYOUT_GNSS_SAT_FIX)
            position.setFixQuality(parseInteger(lineMatcher.group(11)));

        return position;
    }

    private Integer parseInteger(String string) {
        String trimmed = trim(string);
        if (trimmed == null)
            return null;
        try {
            return Integer.valueOf(trimmed);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    protected void writePosition(Wgs84Position position, PrintWriter writer, int index, boolean firstPosition) {
        throw new UnsupportedOperationException("Columbus Fusion format is read-only");
    }
}
