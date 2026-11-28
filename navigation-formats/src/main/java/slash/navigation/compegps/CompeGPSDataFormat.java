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

    Translated to Java from GPSBabel compegps.cc (gpsbabel_1_7_0) for RouteConverter, 2026; modified.
    Copyright (C) 2005 Olaf Klein, o.b.klein@gpsbabel.org
*/

package slash.navigation.compegps;

import slash.common.type.CompactCalendar;
import slash.navigation.base.GpxBackedNavigationFormat;
import slash.navigation.base.RouteCharacteristics;
import slash.navigation.common.Datums;
import slash.navigation.gpx.Gpx10Format;
import slash.navigation.gpx.GpxPosition;
import slash.navigation.gpx.GpxRoute;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.time.DateTimeException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.Math.abs;
import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.time.ZoneOffset.UTC;
import static java.util.Arrays.asList;
import static slash.common.io.Transfer.isEmpty;
import static slash.common.type.CompactCalendar.fromMillis;
import static slash.navigation.base.RouteCharacteristics.Route;
import static slash.navigation.base.RouteCharacteristics.Track;
import static slash.navigation.base.RouteCharacteristics.Waypoints;
import static slash.navigation.common.NavigationConversion.formatElevation;
import static slash.navigation.common.NavigationConversion.formatPosition;
import static slash.navigation.gpsbabel.GpsBabelText.cleanField;
import static slash.navigation.gpsbabel.GpsBabelText.csvLineParse;
import static slash.navigation.gpsbabel.GpsBabelText.humanToDec;

/**
 * The base of the native CompeGPS Data formats, translated from GPSBabel compegps.cc.
 *
 * @author Christian Pesch
 */

public abstract class CompeGPSDataFormat extends GpxBackedNavigationFormat {
    private static final Logger log = Logger.getLogger(CompeGPSDataFormat.class.getName());
    private static final Charset WINDOWS_1252 = Charset.forName("windows-1252");
    private static final String DUMMY_WAYPOINT_DATE_TIME = "27-MAR-62 00:00:00";
    private static final String[] MONTHS = {"JAN", "FEB", "MAR", "APR", "MAY", "JUN", "JUL", "AUG", "SEP", "OCT", "NOV", "DEC"};
    private static final Pattern ATOF = Pattern.compile("^[+-]?(?:\\d+\\.?\\d*|\\.\\d+)(?:[eE][+-]?\\d+)?");
    // a latitude as CompeGPS writes it: 48.1234567890ºN, the Garmin variant has another degree sign
    private static final Pattern LATITUDE = Pattern.compile("^\\d+(?:[.,]\\d*)?\\D?[NS]$", Pattern.CASE_INSENSITIVE);
    private static final Pattern WHITESPACE = Pattern.compile("[ \\t]+");
    // what GPSBabel writes for an unknown altitude, and what TwoNav writes
    private static final double UNKNOWN_ALTITUDE_GPSBABEL = -99999999.0;
    private static final double UNKNOWN_ALTITUDE_TWONAV = -9999.0;
    // the dates that mean "no time": the dummy of CompeGPS waypoints, the one of Garmin exports and the
    // epoch GPSBabel writes for a missing time
    private static final LocalDateTime[] UNKNOWN_TIMES = {
            LocalDateTime.of(1962, 3, 27, 0, 0),
            LocalDateTime.of(2062, 3, 27, 0, 0),
            LocalDateTime.of(1989, 12, 30, 23, 59, 59),
            LocalDateTime.of(1989, 12, 31, 0, 0),
            LocalDateTime.of(1970, 1, 1, 0, 0)
    };
    // the masculine ordinal indicator that CompeGPS uses as degree sign, byte 0xBA in Windows-1252
    private static final char DEGREE = '\u00BA';

    public boolean isSupportsMultipleRoutes() {
        return false;
    }

    protected boolean isValidRoute(GpxRoute route) {
        // clashes with some iGo8 .trk files
        List<GpxPosition> positions = route.getPositions();
        int count = 0;
        for (GpxPosition position : positions) {
            if (isEmpty(position.getLongitude()) && isEmpty(position.getLatitude()))
                count++;
        }
        return count != positions.size();
    }

    protected abstract RouteCharacteristics getRouteCharacteristics();

    protected GpxRoute sanitizeRouteAfterReading(GpxRoute route) {
        route.setCharacteristics(getRouteCharacteristics());
        return route;
    }

    // reading

    private static boolean isDataLine(char type) {
        return type == 'R' || type == 'W' || type == 'w' || type == 'T' || type == 't' || type == 'a';
    }

    private static List<String> splitLines(String text) {
        List<String> lines = new ArrayList<>();
        int start = 0;
        int length = text.length();
        for (int i = 0; i < length; i++) {
            char c = text.charAt(i);
            if (c == '\n' || c == '\r') {
                lines.add(text.substring(start, i));
                if (c == '\r' && i + 1 < length && text.charAt(i + 1) == '\n')
                    i++;
                start = i + 1;
            }
        }
        if (start < length)
            lines.add(text.substring(start));
        return lines;
    }

    private static boolean hasUtf8Bom(byte[] bytes) {
        return bytes.length >= 3 && (bytes[0] & 0xFF) == 0xEF && (bytes[1] & 0xFF) == 0xBB && (bytes[2] & 0xFF) == 0xBF;
    }

    /**
     * Charset precedence: UTF-8 BOM, a "B  UTF-8" line before the first data line, Windows-1252.
     */
    private static Charset detectCharset(byte[] bytes) {
        if (hasUtf8Bom(bytes))
            return UTF_8;
        for (String line : splitLines(new String(bytes, ISO_8859_1))) {
            String trimmed = line.trim();
            if (trimmed.isEmpty())
                continue;
            if (isDataLine(trimmed.charAt(0)))
                break;
            if (trimmed.charAt(0) == 'B') {
                int space = trimmed.indexOf(' ');
                if (space != -1 && "UTF-8".equalsIgnoreCase(trimmed.substring(space).trim()))
                    return UTF_8;
            }
        }
        return WINDOWS_1252;
    }

    private static class RouteData {
        private String name;
        private final List<GpxPosition> positions = new ArrayList<>();
    }

    private static double atof(String string) {
        Matcher matcher = ATOF.matcher(string);
        return matcher.find() ? Double.parseDouble(matcher.group()) : 0.0;
    }

    private static int monthLookup(String month) {
        for (int i = 0; i < MONTHS.length; i++) {
            if (MONTHS[i].equalsIgnoreCase(month))
                return i;
        }
        return -1;
    }

    /** Parses a number of 1 to the given count of digits, -1 if it is none. */
    private static int parseDigits(String string, int maximumDigits) {
        if (string.isEmpty() || string.length() > maximumDigits)
            return -1;
        for (int i = 0; i < string.length(); i++) {
            if (!Character.isDigit(string.charAt(i)))
                return -1;
        }
        return Integer.parseInt(string);
    }

    /**
     * Parses dd-MMM-yy or dd-MMM-yyyy and H:m:s with an optional fraction of a second. One digit days, hours,
     * minutes and seconds are fine. Anything out of range or unparsable, and the dates that mean "no time",
     * give no time; this never throws.
     */
    static CompactCalendar parseDateTime(String date, String time) {
        if (date == null || time == null)
            return null;
        String[] dateParts = date.split("-", -1);
        if (dateParts.length != 3)
            return null;
        int day = parseDigits(dateParts[0], 2);
        int month = monthLookup(dateParts[1]);
        int year = parseDigits(dateParts[2], 4);
        if (day < 1 || month < 0 || year < 0 || dateParts[2].length() == 3)
            return null;
        if (dateParts[2].length() <= 2)
            year += year < 70 ? 2000 : 1900;

        String[] timeParts = time.split(":", -1);
        if (timeParts.length != 3)
            return null;
        int hour = parseDigits(timeParts[0], 2);
        int minute = parseDigits(timeParts[1], 2);
        String secondsAndFraction = timeParts[2];
        int separator = Math.max(secondsAndFraction.indexOf('.'), secondsAndFraction.indexOf(','));
        int second = parseDigits(separator == -1 ? secondsAndFraction : secondsAndFraction.substring(0, separator), 2);
        int millisecond = 0;
        if (separator != -1) {
            String fraction = secondsAndFraction.substring(separator + 1);
            if (!fraction.isEmpty() && parseDigits(fraction, fraction.length()) < 0)
                return null;
            fraction = (fraction + "000").substring(0, 3);
            millisecond = Integer.parseInt(fraction);
        }
        if (hour < 0 || hour > 23 || minute < 0 || minute > 59 || second < 0 || second > 59)
            return null;

        LocalDateTime dateTime;
        try {
            dateTime = LocalDateTime.of(year, month + 1, day, hour, minute, second);
        } catch (DateTimeException e) {
            return null;
        }
        for (LocalDateTime unknown : UNKNOWN_TIMES) {
            if (unknown.equals(dateTime))
                return null;
        }
        return fromMillis(dateTime.toInstant(UTC).toEpochMilli() + millisecond);
    }

    private GpxPosition createPosition(double latitude, double longitude, Double elevation, CompactCalendar time,
                                       String name, String description, int datum) {
        double[] wgs84 = Datums.toWgs84(latitude, longitude, 0.0, datum);
        // GPSBabel 1.5.4 reads back the unknown altitude and the missing time it writes as -99999999 m and as
        // 1970-01-01 - maintainer 2026-11-19: prefer correct behaviour over GPSBabel 1.5.4 bugs
        if (elevation != null && (elevation == UNKNOWN_ALTITUDE_TWONAV || elevation == UNKNOWN_ALTITUDE_GPSBABEL))
            elevation = null;
        return new GpxPosition(wgs84[1], wgs84[0], elevation, null, time,
                stripIllegalXmlCharacters(asDescription(stripIllegalXmlCharacters(name), stripIllegalXmlCharacters(description))));
    }

    private static boolean isLatitude(String token) {
        return LATITUDE.matcher(token).matches();
    }

    /**
     * "W" lines: name, "A", latitude, longitude, date, time, altitude, description... The name ends before the
     * "A" that is followed by a latitude: GPSBabel takes a waypoint named "A" for one without name, and a name
     * with blanks, which TwoNav and GPSBabel write as "_", shifts every field. TwoNav writes blanks in names as
     * "_", so they become blanks again for files in its dialect. Returns null for a line without coordinates.
     */
    private GpxPosition parseWaypoint(String tail, int datum, boolean twoNavDialect) {
        List<String> tokens = new ArrayList<>(asList(WHITESPACE.split(tail.trim())));
        int a = -1;
        for (int i = 0; i + 1 < tokens.size(); i++) {
            if ("A".equals(tokens.get(i)) && isLatitude(tokens.get(i + 1))) {
                a = i;
                break;
            }
        }
        if (a == -1)
            return null;

        String name = String.join(" ", tokens.subList(0, a));
        int end = name.length();
        while (end > 0 && name.charAt(end - 1) == '_')
            end--;
        name = name.substring(0, end);
        if (twoNavDialect)
            name = name.replace('_', ' ').trim();

        double latitude = humanToDec(token(tokens, a + 1), 1)[0];
        double longitude = humanToDec(token(tokens, a + 2), 2)[1];
        if (Double.isNaN(latitude) || Double.isNaN(longitude))
            return null;
        CompactCalendar time = parseDateTime(token(tokens, a + 3), token(tokens, a + 4));
        String altitude = token(tokens, a + 5);
        Double elevation = altitude != null ? atof(altitude) : null;
        String description = tokens.size() > a + 6 ? String.join(" ", tokens.subList(a + 6, tokens.size())) : null;
        return createPosition(latitude, longitude, elevation, time, name.isEmpty() ? null : name, description, datum);
    }

    private static String token(List<String> tokens, int index) {
        return index < tokens.size() ? tokens.get(index) : null;
    }

    /**
     * "T" lines: "A", latitude, longitude, date, time, "s", altitude, ... Returns null for a line without
     * coordinates.
     */
    private GpxPosition parseTrackPoint(String tail, int datum) {
        double latitude = Double.NaN, longitude = Double.NaN;
        Double elevation = null;
        String date = null, time = null;

        int column = tail.startsWith("A ") ? 1 : 0;
        for (String field : csvLineParse(tail, " ", "")) {
            String c = field.trim();
            if (!c.isEmpty()) {
                switch (column) {
                    case 2:
                        latitude = humanToDec(c, 1)[0];
                        break;
                    case 3:
                        longitude = humanToDec(c, 2)[1];
                        break;
                    case 4:
                        date = c;
                        break;
                    case 5:
                        time = c;
                        break;
                    case 7:
                        elevation = atof(c);
                        break;
                    default:
                        break;
                }
            }
            column++;
        }
        if (Double.isNaN(latitude) || Double.isNaN(longitude))
            return null;
        return createPosition(latitude, longitude, elevation, parseDateTime(date, time), null, null, datum);
    }

    /**
     * Returns the name of the n-th field of a delimited line, null for an empty field.
     */
    private static String field(String tail, String delimiter, int index) {
        List<String> fields = csvLineParse(tail, delimiter, "");
        if (index >= fields.size())
            return null;
        String result = fields.get(index).trim();
        return result.isEmpty() ? null : result;
    }

    protected List<GpxRoute> readRoutes(InputStream source) throws IOException {
        byte[] bytes = source.readAllBytes();
        Charset charset = detectCharset(bytes);
        String text = new String(bytes, charset);
        if (text.startsWith("﻿"))
            text = text.substring(1);

        boolean twoNavDialect = UTF_8.equals(charset);
        int datum = Datums.lookupIndex("WGS 84");
        boolean hasDatum = false, hasCoordinateSystem = false, hasData = false;
        boolean hasPoint = false;
        List<GpxPosition> waypoints = new ArrayList<>();
        List<RouteData> routes = new ArrayList<>();
        RouteData route = null, track = null;
        GpxPosition lastWaypoint = null;

        for (String line : splitLines(text)) {
            String cin = line.trim();
            if (cin.isEmpty())
                continue;
            int space = cin.indexOf(' ');
            if (space == -1)
                continue;
            String tail = cin.substring(space).trim();
            char type = cin.charAt(0);

            if (isDataLine(type) && !hasData) {
                hasData = true;
                if (!hasDatum || !hasCoordinateSystem) {
                    log.warning("CompeGPS Data file without \"G\" and \"U\" lines before the first data line");
                    return null;
                }
            }

            switch (type) {
                case 'G':
                    datum = Datums.lookupIndex(tail);
                    // GPSBabel declines a file with an unknown datum
                    if (datum < 0) {
                        log.warning("CompeGPS Data: unknown datum \"" + tail + "\", reading the positions as WGS 84");
                        datum = Datums.lookupIndex("WGS 84");
                    }
                    hasDatum = true;
                    hasCoordinateSystem = false;
                    break;
                case 'U':
                    char system = tail.isEmpty() ? ' ' : tail.charAt(0);
                    if (system == '0') {
                        log.warning("CompeGPS Data: UTM is not supported yet");
                        return null;
                    } else if (system != '1') {
                        log.warning("CompeGPS Data: Invalid system of coordinates \"" + cin + "\"");
                        return null;
                    }
                    hasCoordinateSystem = hasDatum;
                    break;
                case 'R':
                    route = new RouteData();
                    routes.add(route);
                    route.name = field(tail, ",", 1);
                    break;
                case 'W':
                    GpxPosition waypoint = parseWaypoint(tail, datum, twoNavDialect);
                    if (waypoint == null) {
                        log.warning("CompeGPS Data: skipping waypoint without coordinates \"" + cin + "\"");
                        break;
                    }
                    lastWaypoint = waypoint;
                    if (route != null)
                        route.positions.add(waypoint);
                    else
                        waypoints.add(waypoint);
                    hasPoint = true;
                    break;
                case 'w':
                    if (lastWaypoint == null) {
                        log.warning("CompeGPS Data: No waypoint data before \"" + cin + "\"");
                        return null;
                    }
                    break;
                case 'T':
                    if (track == null)
                        track = new RouteData();
                    GpxPosition trackPoint = parseTrackPoint(tail, datum);
                    if (trackPoint == null) {
                        log.warning("CompeGPS Data: skipping track point without coordinates \"" + cin + "\"");
                        break;
                    }
                    lastWaypoint = trackPoint;
                    track.positions.add(trackPoint);
                    hasPoint = true;
                    break;
                case 't':
                    if (track != null) {
                        String name = field(tail, "|", 1);
                        if (name != null)
                            track.name = name;
                    }
                    break;
                default:
                    break;
            }
        }

        if (!hasPoint)
            return null;

        List<GpxRoute> result = new ArrayList<>();
        if (!waypoints.isEmpty())
            result.add(new GpxRoute(new Gpx10Format(), Waypoints, null, null, waypoints));
        for (RouteData data : routes)
            result.add(new GpxRoute(new Gpx10Format(), Route, stripIllegalXmlCharacters(data.name), null, data.positions));
        if (track != null)
            result.add(new GpxRoute(new Gpx10Format(), Track, stripIllegalXmlCharacters(track.name), null, track.positions));
        return result;
    }

    // writing

    private static String latitude(double latitude) {
        return format(Locale.ROOT, "%.10f%c%c", abs(latitude), DEGREE, latitude >= 0 ? 'N' : 'S');
    }

    private static String longitude(double longitude) {
        return format(Locale.ROOT, "%.10f%c%c", abs(longitude), DEGREE, longitude >= 0 ? 'E' : 'W');
    }

    private static String formatDateTime(CompactCalendar time) {
        if (time == null)
            return "01-JAN-70 00:00:00";
        LocalDateTime dateTime = LocalDateTime.ofEpochSecond(Math.floorDiv(time.getTimeInMillis(), 1000L), 0, UTC);
        return format(Locale.ROOT, "%02d-%s-%02d %02d:%02d:%02d", dateTime.getDayOfMonth(), MONTHS[dateTime.getMonthValue() - 1],
                dateTime.getYear() % 100, dateTime.getHour(), dateTime.getMinute(), dateTime.getSecond());
    }

    private static boolean hasCoordinates(GpxPosition position) {
        return formatPosition(position.getLatitude()) != null && formatPosition(position.getLongitude()) != null;
    }

    private static String coordinates(GpxPosition position) {
        return latitude(formatPosition(position.getLatitude()).doubleValue()) + ' ' +
                longitude(formatPosition(position.getLongitude()).doubleValue());
    }

    private void writeTrackPoints(GpxRoute route, int startIndex, int endIndex, StringBuilder builder) {
        List<GpxPosition> positions = route.getPositions();
        String routeName = stripIllegalXmlCharacters(asRouteName(route.getName()));
        boolean writeTrackInfo = true;
        for (int i = startIndex; i < endIndex; i++) {
            GpxPosition position = positions.get(i);
            if (!hasCoordinates(position))
                continue;
            BigDecimal elevation = formatElevation(position.getElevation());

            builder.append("T  A ").append(coordinates(position)).append(' ')
                    .append(formatDateTime(position.getTime()))
                    // TwoNav writes -9999 for an unknown altitude, GPSBabel -99999999
                    .append(format(Locale.ROOT, " s %.1f 0.0 0.0 0.0 0 -1000.0 -1.0 -1.0 -1.0 -1.0",
                            elevation != null ? elevation.doubleValue() : UNKNOWN_ALTITUDE_TWONAV))
                    .append('\n');
            if (writeTrackInfo) {
                writeTrackInfo = false;
                if (routeName != null)
                    builder.append("t 4294967295|").append(cleanField(routeName.replace("|", ""), "")).append("|-1|-1\n");
            }
        }
        builder.append("F  1234\n");
    }

    /**
     * "W" lines as GPSBabel's compegps writer emits them: blanks in the name become "_", a waypoint without
     * name gets "_", which reads back as none. Unlike GPSBabel the time of the waypoint is written, if it has
     * one, and an unknown altitude as TwoNav writes it.
     */
    private void writeWaypoints(GpxRoute route, int startIndex, int endIndex, StringBuilder builder) {
        List<GpxPosition> positions = route.getPositions();
        for (int i = startIndex; i < endIndex; i++) {
            GpxPosition position = positions.get(i);
            if (!hasCoordinates(position))
                continue;
            String name = cleanField(asName(position.getDescription()), "").trim().replace(' ', '_');
            String description = cleanField(asDesc(position.getDescription()), "").trim();
            BigDecimal elevation = formatElevation(position.getElevation());

            builder.append("W  ").append(name.isEmpty() ? "_" : name).append(" A ")
                    .append(coordinates(position)).append(' ')
                    .append(position.getTime() != null ? formatDateTime(position.getTime()) : DUMMY_WAYPOINT_DATE_TIME)
                    .append(format(Locale.ROOT, " %.6f", elevation != null ? elevation.doubleValue() : UNKNOWN_ALTITUDE_TWONAV));
            if (!description.isEmpty())
                builder.append(' ').append(description);
            builder.append('\n');
        }
    }

    private void writeRoute(GpxRoute route, int startIndex, int endIndex, StringBuilder builder) {
        String name = cleanField(stripIllegalXmlCharacters(asRouteName(route.getName())), ",").trim();
        builder.append("R  16711680,").append(name.isEmpty() ? " " : name).append(",1,-1\n");
        writeWaypoints(route, startIndex, endIndex, builder);
    }

    /**
     * Each class writes what the programs of CompeGPS and TwoNav expect in a file of its extension: waypoints
     * as "W" lines in a .wpt, a route as an "R" line followed by "W" lines in a .rte and a track as "T" lines
     * in a .trk. GPSBabel 1.5.4 wrote the track form for all three, since RouteConverter handed it the same
     * positions as route, track and waypoints. The route characteristics are forced to the class's on reading.
     */
    public void write(GpxRoute route, OutputStream target, int startIndex, int endIndex) throws IOException {
        StringBuilder builder = new StringBuilder();
        builder.append("G  WGS 84\n");
        builder.append("U  1\n");
        RouteCharacteristics characteristics = getRouteCharacteristics();
        if (characteristics == Waypoints)
            writeWaypoints(route, startIndex, endIndex, builder);
        else if (characteristics == Route)
            writeRoute(route, startIndex, endIndex, builder);
        else
            writeTrackPoints(route, startIndex, endIndex, builder);
        // all text is Windows-1252, the charset the reader assumes without BOM or "B  UTF-8" line. GPSBabel 1.5.4
        // passes names through as UTF-8 next to a single byte degree sign, which reads back as mojibake -
        // maintainer 2026-11-19: prefer correct behaviour over GPSBabel 1.5.4 bugs
        target.write(builder.toString().getBytes(WINDOWS_1252));
    }
}
