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
import static slash.common.io.Transfer.isEmpty;
import static slash.common.type.CompactCalendar.fromMillis;
import static slash.navigation.base.RouteCharacteristics.Route;
import static slash.navigation.base.RouteCharacteristics.Track;
import static slash.navigation.base.RouteCharacteristics.Waypoints;
import static slash.navigation.common.NavigationConversion.formatElevation;
import static slash.navigation.common.NavigationConversion.formatPosition;
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
    private static final String DUMMY_WAYPOINT_DATE = "27-MAR-62";
    private static final String[] MONTHS = {"JAN", "FEB", "MAR", "APR", "MAY", "JUN", "JUL", "AUG", "SEP", "OCT", "NOV", "DEC"};
    private static final Pattern ATOF = Pattern.compile("^[+-]?(?:\\d+\\.?\\d*|\\.\\d+)(?:[eE][+-]?\\d+)?");
    private static final Pattern ATOI = Pattern.compile("^\\s*[+-]?\\d+");
    private static final double UNKNOWN_ALTITUDE_WRITTEN = -99999999.0;
    private static final double UNKNOWN_ALTITUDE_TWONAV = -9999.0;
    private static final char DEGREE = 0xBA;

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

    private static int atoi(String string) {
        Matcher matcher = ATOI.matcher(string);
        if (!matcher.find())
            return 0;
        try {
            return Integer.parseInt(matcher.group().trim().replace("+", ""));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static String from(String string, int index) {
        return index <= string.length() ? string.substring(index) : "";
    }

    private static int monthLookup(String month) {
        for (int i = 0; i < MONTHS.length; i++) {
            if (MONTHS[i].equalsIgnoreCase(month))
                return i;
        }
        return -1;
    }

    private static class DateTime {
        private int day, month, year; // year as years since 1900, like struct tm
        private int hour, minute, second, millisecond;

        private void parseDate(String string) {
            day = atoi(string);
            month = monthLookup(string.length() >= 6 ? string.substring(3, 6) : from(string, 3));
            int y = atoi(from(string, 7));
            if (y < 70)
                y += 100;
            if (y > 1900)
                y -= 1900;
            year = y;
        }

        private void parseTime(String string) {
            hour = atoi(string);
            minute = atoi(from(string, 3));
            String seconds = from(string, 6);
            second = atoi(seconds);
            int dot = seconds.indexOf('.');
            if (dot != -1) {
                StringBuilder fraction = new StringBuilder();
                for (int i = dot + 1; i < seconds.length() && fraction.length() < 3 && Character.isDigit(seconds.charAt(i)); i++)
                    fraction.append(seconds.charAt(i));
                while (fraction.length() > 0 && fraction.length() < 3)
                    fraction.append('0');
                if (fraction.length() > 0)
                    millisecond = Integer.parseInt(fraction.toString());
            }
        }

        private CompactCalendar toCalendar() {
            LocalDateTime dateTime = LocalDateTime.of(1900 + year, 1, 1, 0, 0)
                    .plusMonths(month).plusDays(day - 1L)
                    .plusHours(hour).plusMinutes(minute).plusSeconds(second);
            return fromMillis(dateTime.toInstant(UTC).toEpochMilli() + millisecond);
        }
    }

    private GpxPosition createPosition(double latitude, double longitude, Double elevation, CompactCalendar time,
                                       String name, String description, int datum) {
        double[] wgs84 = Datums.toWgs84(latitude, longitude, 0.0, datum);
        if (elevation != null && elevation == UNKNOWN_ALTITUDE_TWONAV)
            elevation = null;
        return new GpxPosition(wgs84[1], wgs84[0], elevation, null, time,
                stripIllegalXmlCharacters(asDescription(stripIllegalXmlCharacters(name), stripIllegalXmlCharacters(description))));
    }

    /**
     * "W" lines: name, "A", latitude, longitude, date, time, altitude, description...
     */
    private GpxPosition parseWaypoint(String tail, int datum) {
        String name = null, description = null;
        double latitude = 0.0, longitude = 0.0;
        Double elevation = null;
        CompactCalendar time = null;
        DateTime dateTime = new DateTime();
        boolean hasTime = false;

        int column = tail.startsWith("A ") ? 1 : 0;
        for (String field : csvLineParse(tail, " ", "")) {
            String c = field.trim();
            if (!c.isEmpty()) {
                switch (column) {
                    case 0:
                        int end = c.length();
                        while (end > 0 && c.charAt(end - 1) == '_')
                            end--;
                        c = c.substring(0, end);
                        if (!c.isEmpty())
                            name = c;
                        break;
                    case 2:
                        double[] lat = humanToDec(c, 1);
                        if (!Double.isNaN(lat[0]))
                            latitude = lat[0];
                        break;
                    case 3:
                        double[] lon = humanToDec(c, 2);
                        if (!Double.isNaN(lon[1]))
                            longitude = lon[1];
                        break;
                    case 4:
                        if (!DUMMY_WAYPOINT_DATE.equals(c)) {
                            hasTime = true;
                            dateTime.parseDate(c);
                        }
                        break;
                    case 5:
                        if (hasTime) {
                            dateTime.parseTime(c);
                            time = dateTime.toCalendar();
                        }
                        break;
                    case 6:
                        elevation = atof(c);
                        break;
                    case 7:
                        description = c;
                        break;
                    default:
                        if (column > 7)
                            description = (description != null ? description : "") + " " + c;
                }
            }
            column++;
        }
        return createPosition(latitude, longitude, elevation, time, name, description, datum);
    }

    /**
     * "T" lines: "A", latitude, longitude, date, time, "s", altitude, ...
     */
    private GpxPosition parseTrackPoint(String tail, int datum) {
        double latitude = 0.0, longitude = 0.0;
        Double elevation = null;
        CompactCalendar time = null;
        DateTime dateTime = new DateTime();
        boolean hasDate = false;

        int column = tail.startsWith("A ") ? 1 : 0;
        for (String field : csvLineParse(tail, " ", "")) {
            String c = field.trim();
            if (!c.isEmpty()) {
                switch (column) {
                    case 2:
                        double[] lat = humanToDec(c, 1);
                        if (!Double.isNaN(lat[0]))
                            latitude = lat[0];
                        break;
                    case 3:
                        double[] lon = humanToDec(c, 2);
                        if (!Double.isNaN(lon[1]))
                            longitude = lon[1];
                        break;
                    case 4:
                        dateTime.parseDate(c);
                        hasDate = true;
                        break;
                    case 5:
                        dateTime.parseTime(c);
                        if (hasDate)
                            time = dateTime.toCalendar();
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
        return createPosition(latitude, longitude, elevation, time, null, null, datum);
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
                    if (datum < 0) {
                        log.warning("CompeGPS Data: Unsupported datum \"" + tail + "\"");
                        return null;
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
                    lastWaypoint = parseWaypoint(tail, datum);
                    if (route != null)
                        route.positions.add(lastWaypoint);
                    else
                        waypoints.add(lastWaypoint);
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
                    lastWaypoint = parseTrackPoint(tail, datum);
                    track.positions.add(lastWaypoint);
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

    private static String clean(String string, char nuke) {
        return string.replace(String.valueOf(nuke), "");
    }

    private static String formatDateTime(CompactCalendar time) {
        if (time == null)
            return "01-JAN-70 00:00:00";
        LocalDateTime dateTime = LocalDateTime.ofEpochSecond(Math.floorDiv(time.getTimeInMillis(), 1000L), 0, UTC);
        return format(Locale.ROOT, "%02d-%s-%02d %02d:%02d:%02d", dateTime.getDayOfMonth(), MONTHS[dateTime.getMonthValue() - 1],
                dateTime.getYear() % 100, dateTime.getHour(), dateTime.getMinute(), dateTime.getSecond());
    }

    private void writeWaypoints(GpxRoute route, int startIndex, int endIndex, StringBuilder builder) {
        List<GpxPosition> positions = route.getPositions();
        for (int i = startIndex; i < endIndex; i++) {
            GpxPosition position = positions.get(i);
            BigDecimal latitude = formatPosition(position.getLatitude());
            BigDecimal longitude = formatPosition(position.getLongitude());
            if (latitude == null || longitude == null)
                continue;
            BigDecimal elevation = formatElevation(position.getElevation());

            String name = stripIllegalXmlCharacters(asName(position.getDescription()));
            String description = stripIllegalXmlCharacters(asDesc(position.getDescription()));
            builder.append("W  ").append(name != null ? name.replace(' ', '_') : "").append(" A ")
                    .append(latitude(latitude.doubleValue())).append(' ')
                    .append(longitude(longitude.doubleValue())).append(' ')
                    .append(DUMMY_WAYPOINT_DATE).append(" 00:00:00 ")
                    .append(format(Locale.ROOT, "%.6f", elevation != null ? elevation.doubleValue() : 0.0));
            if (description != null && !description.isEmpty())
                builder.append(' ').append(description);
            builder.append('\n');
            builder.append("w  Waypoint,0,0.0,16777215,255,1,7,,0.0\n");
        }
    }

    private void writeTrackPoints(GpxRoute route, int startIndex, int endIndex, StringBuilder builder) {
        List<GpxPosition> positions = route.getPositions();
        String routeName = stripIllegalXmlCharacters(asRouteName(route.getName()));
        boolean writeTrackInfo = true;
        for (int i = startIndex; i < endIndex; i++) {
            GpxPosition position = positions.get(i);
            BigDecimal latitude = formatPosition(position.getLatitude());
            BigDecimal longitude = formatPosition(position.getLongitude());
            if (latitude == null || longitude == null)
                continue;
            BigDecimal elevation = formatElevation(position.getElevation());

            builder.append("T  A ").append(latitude(latitude.doubleValue())).append(' ')
                    .append(longitude(longitude.doubleValue())).append(' ')
                    .append(formatDateTime(position.getTime()))
                    .append(format(Locale.ROOT, " s %.1f 0.0 0.0 0.0 0 -1000.0 -1.0 -1.0 -1.0 -1.0",
                            elevation != null ? elevation.doubleValue() : UNKNOWN_ALTITUDE_WRITTEN))
                    .append('\n');
            if (writeTrackInfo) {
                writeTrackInfo = false;
                if (routeName != null)
                    builder.append("t 4294967295|").append(clean(routeName, '|')).append("|-1|-1\n");
            }
        }
        builder.append("F  1234\n");
    }

    private void writeRoute(GpxRoute route, int startIndex, int endIndex, StringBuilder builder) {
        String name = stripIllegalXmlCharacters(asRouteName(route.getName()));
        builder.append("R  16711680,").append(name != null ? clean(name, ',') : " ").append(",1,-1\n");
        writeWaypoints(route, startIndex, endIndex, builder);
    }

    public void write(GpxRoute route, OutputStream target, int startIndex, int endIndex) throws IOException {
        StringBuilder builder = new StringBuilder();
        builder.append("G  WGS 84\n");
        builder.append("U  1\n");
        switch (route.getCharacteristics()) {
            case Waypoints -> writeWaypoints(route, startIndex, endIndex, builder);
            case Route -> writeRoute(route, startIndex, endIndex, builder);
            case Track -> writeTrackPoints(route, startIndex, endIndex, builder);
        }
        target.write(builder.toString().getBytes(WINDOWS_1252));
    }
}
