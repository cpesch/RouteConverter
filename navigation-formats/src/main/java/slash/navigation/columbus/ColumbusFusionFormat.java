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

import slash.common.type.CompactCalendar;
import slash.navigation.base.ParserContext;
import slash.navigation.base.SimpleRoute;
import slash.navigation.base.WaypointType;
import slash.navigation.base.Wgs84Position;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Arrays.asList;
import static slash.common.io.Transfer.formatDoubleAsString;
import static slash.common.io.Transfer.formatIntAsString;
import static slash.common.io.Transfer.parseDouble;
import static slash.common.io.Transfer.trim;
import static slash.common.type.CompactCalendar.fromMillisAndTimeZone;
import static slash.navigation.base.WaypointType.Waypoint;
import static slash.navigation.columbus.ColumbusV1000Device.getTimeZone;
import static slash.navigation.columbus.ColumbusV1000Device.getUseLocalTimeZone;

/**
 * Reads and writes Columbus Fusion track (.csv) files, a GNSS track format with
 * five possible column layouts (GNSS/GNSS+SAT/GNSS+SAT+FIX/GNSS+IMU/IMU),
 * distinguished by the Type directive and the second header line. Empty tag/date
 * columns carry forward from the previous row.
 *
 * The two acceleration layouts sample at 10-20 Hz: only the row carrying a time
 * value becomes a position - with the first acceleration triple of that second -
 * while the continuation rows are consumed silently. IMU positions have no
 * coordinates at all, just time and acceleration.
 *
 * Writing picks the layout from the data: a route with coordinates throughout emits
 * the widest GNSS+SAT+FIX layout, so that satellites, hdop and fix quality survive a
 * roundtrip, while a route whose positions carry no coordinates emits IMU (or GNSS+IMU
 * when only some of them do), whose lat/lon columns may be empty. Columns without a
 * value stay empty.
 *
 * @author Christian Pesch
 */
public class ColumbusFusionFormat extends ColumbusGpsFormat {
    private static final Pattern FORMAT_DIRECTIVE_PATTERN =
            Pattern.compile("#\\s*Format=ColumbusFusion;\\s*Version=[\\d.]+;\\s*Type=(GNSS\\+IMU|GNSS|IMU)\\s*");

    private static final Pattern COLUMN_HEADER_GNSS_PATTERN =
            Pattern.compile("\\s*tag,date,time,lat,lon,alt,speed,heading\\s*");
    private static final Pattern COLUMN_HEADER_GNSS_SAT_PATTERN =
            Pattern.compile("\\s*tag,date,time,lat,lon,alt,speed,heading,sat,hdop\\s*");
    private static final Pattern COLUMN_HEADER_GNSS_SAT_FIX_PATTERN =
            Pattern.compile("\\s*tag,date,time,lat,lon,alt,speed,heading,sat,hdop,fix\\s*");
    private static final Pattern COLUMN_HEADER_GNSS_IMU_PATTERN =
            Pattern.compile("\\s*tag,date,time,lat,lon,alt,speed,heading,ax,ay,az\\s*");
    private static final Pattern COLUMN_HEADER_IMU_PATTERN =
            Pattern.compile("\\s*tag,date,time,ax,ay,az\\s*");

    private static final String FORMAT_DIRECTIVE = "# Format=ColumbusFusion; Version=1.0; Type=GNSS";
    private static final String FORMAT_DIRECTIVE_GNSS_IMU = "# Format=ColumbusFusion; Version=1.0; Type=GNSS+IMU";
    private static final String FORMAT_DIRECTIVE_IMU = "# Format=ColumbusFusion; Version=1.0; Type=IMU";
    private static final String COLUMN_HEADER_GNSS_SAT_FIX = "tag,date,time,lat,lon,alt,speed,heading,sat,hdop,fix";
    private static final String COLUMN_HEADER_GNSS_IMU = "tag,date,time,lat,lon,alt,speed,heading,ax,ay,az";
    private static final String COLUMN_HEADER_IMU = "tag,date,time,ax,ay,az";

    private static final Set<String> WRITABLE_TAG_VALUES = new HashSet<>(asList("C", "D", "G", "T"));

    // layout ids, historically the column count of the layout
    static final int LAYOUT_IMU = 6;
    static final int LAYOUT_GNSS = 8;
    static final int LAYOUT_GNSS_SAT = 10;
    static final int LAYOUT_GNSS_SAT_FIX = 11;
    static final int LAYOUT_GNSS_IMU = 12;

    /**
     * Layout of the file currently being written, chosen from the route by
     * writeHeader() and read by the writePosition() calls that follow it. Safe as
     * instance state because one write() drives header-then-positions in order;
     * the read path stays stateless, taking its layout as a parameter.
     */
    private int writeLayout = LAYOUT_GNSS_SAT_FIX;

    private static final String S = ",";
    private static final String TAG = "([CDGT]?)", DATE = "(\\d*)", TIME = "(\\d+)",
            LATLON = "(-?[\\d.]+)", ALT = "([-\\d.]*)", SPEED = "([\\d.]*)", HEAD = "([\\d.]*)",
            SAT = "(\\d*)", HDOP = "([\\d.]*)", FIX = "(\\d*)";
    // the acceleration layouts leave every column but ax/ay/az empty on their continuation rows
    private static final String OPTIONAL_TIME = "(\\d*)", OPTIONAL_LATLON = "(-?[\\d.]*)",
            ACCELERATION = "(-?[\\d.]*)";

    private static final Pattern LINE_GNSS = Pattern.compile(
            TAG + S + DATE + S + TIME + S + LATLON + S + LATLON + S + ALT + S + SPEED + S + HEAD);
    private static final Pattern LINE_GNSS_SAT = Pattern.compile(
            TAG + S + DATE + S + TIME + S + LATLON + S + LATLON + S + ALT + S + SPEED + S + HEAD + S + SAT + S + HDOP);
    private static final Pattern LINE_GNSS_SAT_FIX = Pattern.compile(
            TAG + S + DATE + S + TIME + S + LATLON + S + LATLON + S + ALT + S + SPEED + S + HEAD + S + SAT + S + HDOP + S + FIX);
    private static final Pattern LINE_GNSS_IMU = Pattern.compile(
            TAG + S + DATE + S + OPTIONAL_TIME + S + OPTIONAL_LATLON + S + OPTIONAL_LATLON + S + ALT + S + SPEED + S + HEAD +
                    S + ACCELERATION + S + ACCELERATION + S + ACCELERATION);
    private static final Pattern LINE_IMU = Pattern.compile(
            TAG + S + DATE + S + OPTIONAL_TIME + S + ACCELERATION + S + ACCELERATION + S + ACCELERATION);

    public String getName() {
        return "Columbus Fusion (*" + getExtension() + ")";
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
        return headerFor(writeLayout);
    }

    private String headerFor(int layout) {
        switch (layout) {
            case LAYOUT_IMU:
                return FORMAT_DIRECTIVE_IMU + "\n" + COLUMN_HEADER_IMU;
            case LAYOUT_GNSS_IMU:
                return FORMAT_DIRECTIVE_GNSS_IMU + "\n" + COLUMN_HEADER_GNSS_IMU;
            default:
                return FORMAT_DIRECTIVE + "\n" + COLUMN_HEADER_GNSS_SAT_FIX;
        }
    }

    /**
     * Picks the layout to write from the data, because not every position fits every
     * layout: IMU positions carry no coordinates, and the GNSS layouts have no column
     * that can hold their absence - lat/lon there must match {@code (-?[\d.]+)}, so
     * writing them as empty produces a line the reader rejects, and writing them via
     * {@code formatDoubleAsString} produced a literal 0.0, silently turning
     * accelerometer samples into positions off the coast of Africa.
     *
     * A route whose positions all lack coordinates round-trips as IMU; a mixed route
     * as GNSS+IMU, whose lat/lon and time columns are optional. Anything with
     * coordinates throughout keeps the widest GNSS layout, unchanged.
     */
    private int detectWriteLayout(SimpleRoute route) {
        boolean anyWithoutCoordinates = false, allWithoutCoordinates = true;
        for (Object position : route.getPositions()) {
            Wgs84Position wgs84Position = (Wgs84Position) position;
            if (wgs84Position.getLatitude() == null || wgs84Position.getLongitude() == null)
                anyWithoutCoordinates = true;
            else
                allWithoutCoordinates = false;
        }

        if (anyWithoutCoordinates)
            return allWithoutCoordinates ? LAYOUT_IMU : LAYOUT_GNSS_IMU;
        return LAYOUT_GNSS_SAT_FIX;
    }

    protected void writeHeader(PrintWriter writer, SimpleRoute route) {
        writeLayout = detectWriteLayout(route);
        writer.println(headerFor(writeLayout));
    }

    protected boolean isPosition(String line) {
        return matchesAnyLine(line);
    }

    public boolean isValidLine(String line) {
        return super.isValidLine(line) || isColumnHeader(line);
    }

    private boolean matchesAnyLine(String line) {
        return LINE_GNSS.matcher(line).matches() || LINE_GNSS_SAT.matcher(line).matches() ||
                LINE_GNSS_SAT_FIX.matcher(line).matches() || LINE_GNSS_IMU.matcher(line).matches() ||
                LINE_IMU.matcher(line).matches();
    }

    private boolean isColumnHeader(String line) {
        return COLUMN_HEADER_GNSS_PATTERN.matcher(line).matches() ||
                COLUMN_HEADER_GNSS_SAT_PATTERN.matcher(line).matches() ||
                COLUMN_HEADER_GNSS_SAT_FIX_PATTERN.matcher(line).matches() ||
                COLUMN_HEADER_GNSS_IMU_PATTERN.matcher(line).matches() ||
                COLUMN_HEADER_IMU_PATTERN.matcher(line).matches();
    }

    int detectLayout(String columnHeaderLine) {
        if (COLUMN_HEADER_GNSS_SAT_FIX_PATTERN.matcher(columnHeaderLine).matches())
            return LAYOUT_GNSS_SAT_FIX;
        if (COLUMN_HEADER_GNSS_SAT_PATTERN.matcher(columnHeaderLine).matches())
            return LAYOUT_GNSS_SAT;
        if (COLUMN_HEADER_GNSS_IMU_PATTERN.matcher(columnHeaderLine).matches())
            return LAYOUT_GNSS_IMU;
        if (COLUMN_HEADER_IMU_PATTERN.matcher(columnHeaderLine).matches())
            return LAYOUT_IMU;
        if (COLUMN_HEADER_GNSS_PATTERN.matcher(columnHeaderLine).matches())
            return LAYOUT_GNSS;
        return -1;
    }

    private Pattern linePatternFor(int layout) {
        return switch (layout) {
            case LAYOUT_GNSS_SAT_FIX -> LINE_GNSS_SAT_FIX;
            case LAYOUT_GNSS_SAT -> LINE_GNSS_SAT;
            case LAYOUT_GNSS_IMU -> LINE_GNSS_IMU;
            case LAYOUT_IMU -> LINE_IMU;
            default -> LINE_GNSS;
        };
    }

    private boolean isAccelerationLayout(int layout) {
        return layout == LAYOUT_GNSS_IMU || layout == LAYOUT_IMU;
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

            // a continuation row of an acceleration layout carries a further sample of the
            // current second only - a valid line, but not a position of its own
            if (isAccelerationLayout(layout) && trim(lineMatcher.group(3)) == null)
                continue;

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
        for (int candidate : new int[]{LAYOUT_GNSS_SAT_FIX, LAYOUT_GNSS_SAT, LAYOUT_GNSS_IMU, LAYOUT_GNSS, LAYOUT_IMU}) {
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
        if (layout == LAYOUT_IMU)
            return parseImuRow(lineMatcher, tag, date, index);

        String time = lineMatcher.group(3);
        Double latitude = parseDouble(lineMatcher.group(4));
        Double longitude = parseDouble(lineMatcher.group(5));
        Double elevation = parseDouble(lineMatcher.group(6));
        Double speed = parseDouble(lineMatcher.group(7));
        Double heading = parseDouble(lineMatcher.group(8));

        WaypointType waypointType = parseTag(tag);

        CompactCalendar dateAndTime = parseDateAndTime(date, time);
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
        if (layout == LAYOUT_GNSS_IMU)
            setAcceleration(position, lineMatcher, 9);

        return position;
    }

    /**
     * IMU rows have no coordinates at all - just time, tag and the first acceleration
     * triple of that second.
     */
    private Wgs84Position parseImuRow(Matcher lineMatcher, String tag, String date, int index) {
        WaypointType waypointType = parseTag(tag);

        CompactCalendar dateAndTime = parseDateAndTime(date, lineMatcher.group(3));
        if (dateAndTime != null && getUseLocalTimeZone())
            dateAndTime = dateAndTime.asUTCTimeInTimeZone(TimeZone.getTimeZone(getTimeZone()));

        String description = parseDescription("", String.valueOf(index + 1), waypointType);

        Wgs84Position position = new Wgs84Position(null, null, null, null, dateAndTime, description);
        position.setWaypointType(waypointType);
        setAcceleration(position, lineMatcher, 4);
        return position;
    }

    private void setAcceleration(Wgs84Position position, Matcher lineMatcher, int firstGroup) {
        position.setAccelerationX(parseDouble(lineMatcher.group(firstGroup)));
        position.setAccelerationY(parseDouble(lineMatcher.group(firstGroup + 1)));
        position.setAccelerationZ(parseDouble(lineMatcher.group(firstGroup + 2)));
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

    /**
     * parseRow()/parseImuRow() convert the file's device-local time to UTC, so writing
     * must convert back. Uses the shared, DST-correct inverse in ColumbusGpsFormat —
     * the private single-step version here shifted an hour around a transition
     * (013000 came back as 023000 on the 2026-10-25 Berlin fall-back). Shares the
     * inversion with Type2 via {@link ColumbusGpsFormat#getDeviceLocalTimeToWrite}.
     */
    protected CompactCalendar getTimeToWrite(Wgs84Position position) {
        return getDeviceLocalTimeToWrite(position);
    }

    /**
     * Fusion lines only allow the tags C, D, G and T, so any other waypoint type
     * falls back to a plain track point.
     */
    private String formatFusionTag(Wgs84Position position) {
        String tag = formatTag(position);
        return tag != null && WRITABLE_TAG_VALUES.contains(tag) ? tag : Waypoint.value();
    }

    /**
     * Formats with the full precision of the value, since Fusion files carry up to
     * seven fraction digits for coordinates and a fixed fraction count would truncate them.
     */
    private String formatDoubleOrEmpty(Double aDouble) {
        return aDouble != null ? formatDoubleAsString(aDouble) : "";
    }

    private String formatIntegerOrEmpty(Integer anInteger) {
        return anInteger != null ? formatIntAsString(anInteger) : "";
    }

    protected void writePosition(Wgs84Position position, PrintWriter writer, int index, boolean firstPosition) {
        CompactCalendar time = getTimeToWrite(position);

        String date = formatDate(time);
        String timeOfDay = formatTime(time);
        if (timeOfDay.isEmpty())
            timeOfDay = "000000";

        String prefix = formatFusionTag(position) + S + date + S + timeOfDay + S;

        switch (writeLayout) {
            case LAYOUT_IMU:
                writer.println(prefix +
                        formatDoubleOrEmpty(position.getAccelerationX()) + S +
                        formatDoubleOrEmpty(position.getAccelerationY()) + S +
                        formatDoubleOrEmpty(position.getAccelerationZ()));
                break;

            case LAYOUT_GNSS_IMU:
                writer.println(prefix +
                        formatDoubleOrEmpty(position.getLatitude()) + S +
                        formatDoubleOrEmpty(position.getLongitude()) + S +
                        formatDoubleOrEmpty(position.getElevation()) + S +
                        formatDoubleOrEmpty(position.getSpeed()) + S +
                        formatDoubleOrEmpty(position.getHeading()) + S +
                        formatDoubleOrEmpty(position.getAccelerationX()) + S +
                        formatDoubleOrEmpty(position.getAccelerationY()) + S +
                        formatDoubleOrEmpty(position.getAccelerationZ()));
                break;

            default:
                writer.println(prefix +
                        formatDoubleAsString(position.getLatitude()) + S +
                        formatDoubleAsString(position.getLongitude()) + S +
                        formatDoubleOrEmpty(position.getElevation()) + S +
                        formatDoubleOrEmpty(position.getSpeed()) + S +
                        formatDoubleOrEmpty(position.getHeading()) + S +
                        formatIntegerOrEmpty(position.getSatellites()) + S +
                        formatDoubleOrEmpty(position.getHdop()) + S +
                        formatIntegerOrEmpty(position.getFixQuality()));
        }
    }
}
