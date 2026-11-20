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

    Translated to Java from GPSBabel pcx.cc (gpsbabel_1_7_0) for RouteConverter, 2026; modified.
    Copyright (C) 2002-2017 Robert Lipe, robertlipe+source@gpsbabel.org
*/

package slash.navigation.garmin;

import slash.common.type.CompactCalendar;
import slash.navigation.base.GpxBackedNavigationFormat;
import slash.navigation.gpx.GpxPosition;
import slash.navigation.gpx.GpxRoute;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.DateTimeException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.nio.charset.CodingErrorAction.REPORT;
import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static java.nio.charset.StandardCharsets.UTF_16BE;
import static java.nio.charset.StandardCharsets.UTF_16LE;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.time.ZoneOffset.UTC;
import static slash.common.io.Transfer.isEmpty;
import static slash.common.io.Transfer.trim;
import static slash.navigation.base.RouteCharacteristics.Route;
import static slash.navigation.base.RouteCharacteristics.Track;
import static slash.navigation.base.RouteCharacteristics.Waypoints;
import static slash.navigation.gpsbabel.GpsBabelMath.ddmm2degrees;
import static slash.navigation.gpsbabel.GpsBabelMath.degrees2ddmm;
import static slash.navigation.gpsbabel.GpsBabelMath.monthLookup;
import static slash.navigation.gpsbabel.GpsBabelText.humanToDec;

/**
 * Reads and writes Garmin PCX5 (.wpt) files. A native translation of GPSBabel's pcx format as RouteConverter
 * used it: options "-r -w", i.e. trackpoints are not delivered.
 *
 * @author Christian Pesch
 */

public class GarminPcx5Format extends GpxBackedNavigationFormat {
    private static final Logger log = Logger.getLogger(GarminPcx5Format.class.getName());
    private static final String RECORD_TYPES = "HIMUFRWT";
    private static final int DEFAULT_COMMENT_COLUMN = 60;
    private static final int UNKNOWN_ELEVATION = -9999;
    // the symbol number GPSBabel 1.5.4 writes for a waypoint without an icon
    static final int WAYPOINT_ICON_NUMBER = 0;
    // Qt's QDate::fromString() reads the two digit year "yy" in the 1900s
    // years below are 20yy, the others 19yy
    private static final int TWO_DIGIT_YEAR_PIVOT = 70;
    // a position without time is written as 01-JAN-70 00:00:00
    private static final long NO_TIME_SECONDS = 0;
    private static final int DESCRIPTION_WIDTH = 40;
    private static final Pattern NUMBER = Pattern.compile("[+-]?(\\d+\\.?\\d*|\\.\\d+)([eE][+-]?\\d+)?");
    private static final Pattern DATE = Pattern.compile("(\\d{2})-([A-Za-z]{3})-(\\d{2})");
    private static final Pattern TIME = Pattern.compile("(\\d{2}):(\\d{2}):(\\d{2})");
    private static final String[] MONTHS = {"JAN", "FEB", "MAR", "APR", "MAY", "JUN", "JUL", "AUG", "SEP", "OCT", "NOV", "DEC"};
    private static final String WRITE_HEADER = "H  SOFTWARE NAME & VERSION\n" +
            "I  PCX5 2.09\n" +
            "\n" +
            "H  R DATUM                IDX DA            DF            DX      " +
            "      DY            DZ\n" +
            "M  G WGS 84               121 +0.000000e+00 +0.000000e+00 " +
            "+0.000000e+00 +0.000000e+00 +0.000000e+00\n" +
            "\n" +
            "H  COORDINATE SYSTEM\n" +
            "U  LAT LON DM\n";
    private static final String WAYPOINTS_HEADER = "\n" +
            "H  IDNT   LATITUDE    LONGITUDE    DATE      TIME     ALT   " +
            "DESCRIPTION                              PROXIMITY     SYMBOL " +
            ";waypts\n";

    public String getExtension() {
        return ".wpt";
    }

    public String getName() {
        return "Garmin PCX5 (*" + getExtension() + ")";
    }

    public boolean isSupportsMultipleRoutes() {
        return false;
    }

    protected boolean isValidRoute(GpxRoute route) {
        // clashes with some TomTom POI .ov2 files
        List<GpxPosition> positions = route.getPositions();
        if (positions.isEmpty())
            return false;
        int count = 0;
        for (GpxPosition position : positions) {
            if ((isEmpty(position.getLongitude()) && (position.getElevation() != null) && (position.getElevation() > 100000.0)) ||
                    (isEmpty(position.getLongitude()) && isEmpty(position.getLatitude())))
                count++;
        }
        return count != positions.size();
    }

    // reading

    private static String decode(byte[] bytes) {
        if (bytes.length >= 3 && (bytes[0] & 0xFF) == 0xEF && (bytes[1] & 0xFF) == 0xBB && (bytes[2] & 0xFF) == 0xBF)
            return new String(bytes, 3, bytes.length - 3, UTF_8);
        if (bytes.length >= 2 && (bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xFE)
            return new String(bytes, 2, bytes.length - 2, UTF_16LE);
        if (bytes.length >= 2 && (bytes[0] & 0xFF) == 0xFE && (bytes[1] & 0xFF) == 0xFF)
            return new String(bytes, 2, bytes.length - 2, UTF_16BE);
        // GPSBabel 1.5.4 passes bytes through and its GPX is read as UTF-8, so the letters of a Latin-1 file become
        // U+FFFD - maintainer 2026-11-19: prefer correct behaviour over GPSBabel 1.5.4 bugs
        try {
            return UTF_8.newDecoder().onMalformedInput(REPORT).onUnmappableCharacter(REPORT).decode(ByteBuffer.wrap(bytes)).toString();
        } catch (CharacterCodingException e) {
            return new String(bytes, ISO_8859_1);
        }
    }

    protected List<GpxRoute> readRoutes(InputStream source) throws IOException {
        return parse(decode(source.readAllBytes()));
    }

    private static double parseNumber(String string) {
        return NUMBER.matcher(string).matches() ? Double.parseDouble(string) : Double.NaN;
    }

    private static String firstTokenAt(String line, int index) {
        if (index >= line.length())
            return "";
        String rest = line.substring(index).strip();
        int end = 0;
        while (end < rest.length() && !Character.isWhitespace(rest.charAt(end)))
            end++;
        return rest.substring(0, end);
    }

    /**
     * The UTC time of a "dd-MMM-yy" date and a "hh:mm:ss" time or null if one of them is invalid or if it is
     * the placeholder that is written for a position without time.
     *
     * GPSBabel 1.5.4 (Qt) matches the month name case sensitively, so it never reads the upper-case names it
     * writes, and takes every year for 19yy - maintainer 2026-11-19: prefer correct behaviour over GPSBabel 1.5.4 bugs
     */
    static CompactCalendar parseTime(String date, String time) {
        Matcher dateMatcher = DATE.matcher(date);
        Matcher timeMatcher = TIME.matcher(time);
        if (!dateMatcher.matches() || !timeMatcher.matches())
            return null;
        int month = monthLookup(dateMatcher.group(2));
        if (month == -1)
            return null;
        try {
            int year = Integer.parseInt(dateMatcher.group(3));
            LocalDateTime dateTime = LocalDateTime.of((year < TWO_DIGIT_YEAR_PIVOT ? 2000 : 1900) + year,
                    month + 1, Integer.parseInt(dateMatcher.group(1)),
                    Integer.parseInt(timeMatcher.group(1)), Integer.parseInt(timeMatcher.group(2)), Integer.parseInt(timeMatcher.group(3)));
            long seconds = dateTime.toEpochSecond(UTC);
            return seconds == NO_TIME_SECONDS ? null : CompactCalendar.fromMillis(seconds * 1000);
        } catch (DateTimeException e) {
            return null;
        }
    }

    /**
     * Longitude and latitude of the two coordinate tokens or null if they cannot be parsed.
     */
    private static double[] parseCoordinates(String latitude, String longitude, boolean asDegrees) {
        double lat, lon;
        if (asDegrees) {
            lat = 0.0;
            lon = 0.0;
            for (int which = 1; which <= 2; which++) {
                double[] result = humanToDec(which == 1 ? latitude : longitude, which);
                if (!Double.isNaN(result[0]))
                    lat = result[0];
                if (!Double.isNaN(result[1]))
                    lon = result[1];
            }
            if (Double.isNaN(lat) || Double.isNaN(lon))
                return null;
        } else {
            if (latitude.length() < 2 || longitude.length() < 2)
                return null;
            char latitudeHemisphere = latitude.charAt(0), longitudeHemisphere = longitude.charAt(0);
            if ((latitudeHemisphere != 'N' && latitudeHemisphere != 'S') || (longitudeHemisphere != 'E' && longitudeHemisphere != 'W'))
                return null;
            lat = parseNumber(latitude.substring(1));
            lon = parseNumber(longitude.substring(1));
            if (Double.isNaN(lat) || Double.isNaN(lon))
                return null;
            if (latitudeHemisphere == 'S')
                lat = -lat;
            if (longitudeHemisphere == 'W')
                lon = -lon;
            lat = ddmm2degrees(lat);
            lon = ddmm2degrees(lon);
        }
        return new double[]{lon, lat};
    }

    private GpxPosition createPosition(double[] coordinates, Double elevation, CompactCalendar time, String name, String desc) {
        String description = asDescription(stripIllegalXmlCharacters(trim(name)), stripIllegalXmlCharacters(trim(desc)));
        return new GpxPosition(coordinates[0], coordinates[1], elevation, null, time, description);
    }

    // pcx.cc data_read(); where GPSBabel would stop with fatal() there is no route
    List<GpxRoute> parse(String text) {
        List<GpxPosition> waypoints = new ArrayList<>();
        List<GpxRoute> routes = new ArrayList<>();
        List<GpxRoute> tracks = new ArrayList<>();
        String trackName = null;
        List<GpxPosition> trackPositions = null;
        int points = 0;
        String routeName = null;
        List<GpxPosition> routePositions = null;
        boolean asDegrees = false, gpsu = false;
        int commentColumn = DEFAULT_COMMENT_COLUMN, latitudeColumn = 0, longitudeColumn = 0;

        for (String raw : text.split("\r\n|\n|\r")) {
            String line = raw.strip();
            if (line.isEmpty())
                continue;
            char type = line.charAt(0);
            if (RECORD_TYPES.indexOf(type) == -1)
                return null;

            switch (type) {
                case 'W': {
                    String[] tokens = line.split("\\s+");
                    if (tokens.length < 6)
                        return null;
                    String desc = commentColumn > 0 && commentColumn < line.length() ?
                            line.substring(commentColumn, Math.min(line.length(), commentColumn + DESCRIPTION_WIDTH)) : "";
                    String latitude = latitudeColumn > 0 ? firstTokenAt(line, latitudeColumn) : tokens[2];
                    String longitude = longitudeColumn > 0 ? firstTokenAt(line, longitudeColumn) : tokens[3];
                    double[] coordinates = parseCoordinates(latitude, longitude, asDegrees || gpsu);
                    if (coordinates == null && latitudeColumn <= 0 && longitudeColumn <= 0 && line.length() > 10 && line.charAt(9) == ' ') {
                        // the name column of the written layout is six characters wide and may contain whitespace
                        String[] rest = line.substring(10).strip().split("\\s+");
                        if (rest.length >= 4) {
                            tokens = new String[rest.length + 2];
                            tokens[0] = "W";
                            tokens[1] = line.substring(3, 9).strip();
                            System.arraycopy(rest, 0, tokens, 2, rest.length);
                            coordinates = parseCoordinates(tokens[2], tokens[3], asDegrees || gpsu);
                        }
                    }
                    if (coordinates == null)
                        return null;
                    Double elevation = null;
                    if (tokens.length >= 7) {
                        double number = parseNumber(tokens[6]);
                        // Qt's toDouble() yields 0 for a token that is no number; the source stores it in a long
                        elevation = (double) (long) (Double.isNaN(number) ? 0.0 : number);
                        // -9999 is what is written for an unknown elevation
                        if (elevation == UNKNOWN_ELEVATION)
                            elevation = null;
                    }
                    CompactCalendar time = parseTime(tokens[4], tokens[5]);
                    waypoints.add(createPosition(coordinates, elevation, time, tokens[1], desc));
                    if (routePositions != null)
                        routePositions.add(createPosition(coordinates, elevation, time, tokens[1], desc));
                    points++;
                    break;
                }
                case 'R':
                    if (routePositions != null && !routePositions.isEmpty())
                        routes.add(createRoute(Route, routeName, routePositions));
                    routeName = stripIllegalXmlCharacters(trim(line.substring(1)));
                    routePositions = new ArrayList<>();
                    break;
                case 'T':
                    // GPSBabel delivers the tracks to RouteConverter as well, whatever its -r -w options say
                    String[] tokens = line.split("\\s+");
                    if (tokens.length < 6)
                        return null;
                    double[] coordinates = parseCoordinates(tokens[1], tokens[2], asDegrees);
                    if (coordinates == null)
                        return null;
                    double altitude = parseNumber(tokens[5]);
                    // a trackpoint before a track header starts the track "Default"
                    if (trackPositions == null) {
                        trackName = "Default";
                        trackPositions = new ArrayList<>();
                    }
                    trackPositions.add(new GpxPosition(coordinates[0], coordinates[1], Double.isNaN(altitude) ? 0.0 : altitude,
                            null, parseTime(tokens[3], tokens[4]), null));
                    points++;
                    break;
                case 'U':
                    asDegrees = line.startsWith("LAT LON DEG", 3);
                    if (line.contains("UTM"))
                        return null;
                    break;
                case 'I':
                    gpsu = line.contains("GPSU");
                    break;
                case 'F': {
                    String lower = line.toLowerCase();
                    commentColumn = lower.indexOf("comment");
                    latitudeColumn = lower.indexOf("latitude");
                    longitudeColumn = lower.indexOf("longitude");
                    break;
                }
                case 'M':
                    if (!line.toUpperCase().contains("WGS 84"))
                        log.info("Ignoring datum line '" + line + "', assuming WGS 84");
                    break;
                default:
                    // 'H': "H  LATITUDE ..." or "H  TN name" start a track
                    if (points > 0) {
                        if (trackPositions != null && !trackPositions.isEmpty())
                            tracks.add(createRoute(Track, trackName, trackPositions));
                        trackPositions = null;
                        points = 0;
                    }
                    if (trackPositions == null) {
                        if (line.startsWith("LA", 3)) {
                            trackName = "track";
                            trackPositions = new ArrayList<>();
                        } else if (line.startsWith("TN", 3)) {
                            trackName = stripIllegalXmlCharacters(trim(line.length() > 6 ? line.substring(6) : ""));
                            trackPositions = new ArrayList<>();
                        }
                    }
                    break;
            }
        }

        if (trackPositions != null && !trackPositions.isEmpty())
            tracks.add(createRoute(Track, trackName, trackPositions));
        if (waypoints.isEmpty() && tracks.isEmpty())
            return null;
        if (routePositions != null && !routePositions.isEmpty())
            routes.add(createRoute(Route, routeName, routePositions));
        if (!waypoints.isEmpty())
            routes.add(0, createRoute(Waypoints, null, waypoints));
        routes.addAll(tracks);
        return routes;
    }

    // writing

    /**
     * C's printf("%[0][width].[decimals]f"): the exact binary value is rounded half to even, which is
     * what Java's Formatter does not do.
     */
    static String formatFixed(double value, int width, int decimals, boolean zeroPad) {
        BigDecimal rounded = new BigDecimal(value).setScale(decimals, RoundingMode.HALF_EVEN);
        boolean negative = value < 0.0 || (value == 0.0 && 1.0 / value < 0.0);
        String digits = rounded.abs().toPlainString();
        String sign = negative ? "-" : "";
        StringBuilder result = new StringBuilder();
        int padding = width - sign.length() - digits.length();
        if (zeroPad) {
            result.append(sign);
            for (int i = 0; i < padding; i++)
                result.append('0');
        } else {
            for (int i = 0; i < padding; i++)
                result.append(' ');
            result.append(sign);
        }
        return result.append(digits).toString();
    }

    /**
     * C's printf("%e") for 0.0, the proximity that is always written.
     */
    static String formatProximity() {
        return "0.000000e+00";
    }

    private static String cutAndPad(String string, int length) {
        String cut = string == null ? "" : string.substring(0, Math.min(string.length(), length));
        StringBuilder result = new StringBuilder(cut);
        while (result.length() < length)
            result.append(' ');
        return result.toString();
    }

    static String formatTime(CompactCalendar time) {
        // UTC on a 24 hour clock, 01-JAN-70 00:00:00 for a position without time. GPSBabel 1.5.4 writes the local
        // time on a 12 hour clock without am/pm and the current time for a position without time -
        // maintainer 2026-11-19: prefer correct behaviour over GPSBabel 1.5.4 bugs
        long millis = time == null ? NO_TIME_SECONDS * 1000 : time.getTimeInMillis();
        LocalDateTime dateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), UTC);
        return String.format("%02d-%s-%02d %02d:%02d:%02d", dateTime.getDayOfMonth(), MONTHS[dateTime.getMonthValue() - 1],
                Math.floorMod(dateTime.getYear(), 100), dateTime.getHour(), dateTime.getMinute(), dateTime.getSecond());
    }

    /**
     * pcx.cc gpsutil_disp(): "W  %-6.6s %c%08.5f %c%011.5f %s %5.f %-40.40s %5e  %d\n"
     */
    static String formatWaypointLine(String shortName, double longitude, double latitude, CompactCalendar time,
                                     Double elevation, String description, int symbol) {
        double lon = degrees2ddmm(longitude);
        double lat = degrees2ddmm(latitude);
        return "W  " + cutAndPad(shortName, 6) + " " +
                (lat < 0.0 ? 'S' : 'N') + formatFixed(Math.abs(lat), 8, 5, true) + " " +
                (lon < 0.0 ? 'W' : 'E') + formatFixed(Math.abs(lon), 11, 5, true) + " " +
                formatTime(time) + " " +
                formatFixed(elevation == null ? UNKNOWN_ELEVATION : elevation, 5, 0, false) + " " +
                cutAndPad(description, 40) + " " + formatProximity() + "  " + symbol + "\n";
    }

    public void write(GpxRoute route, OutputStream target, int startIndex, int endIndex) throws IOException {
        StringBuilder buffer = new StringBuilder(WRITE_HEADER);
        // GPSBabel 1.5.4 writes the positions of every kind of route as waypoints
        buffer.append(WAYPOINTS_HEADER);

        List<GpxPosition> positions = route.getPositions();
        for (int i = startIndex; i < endIndex; i++) {
            GpxPosition position = positions.get(i);
            if (position.getLongitude() == null || position.getLatitude() == null)
                continue;
            String name = stripIllegalXmlCharacters(asName(position.getDescription()));
            String desc = stripIllegalXmlCharacters(asDesc(position.getDescription()));
            // GPSBabel's GPX reader uses the name as the description if there is none
            if (desc == null)
                desc = name;
            buffer.append(formatWaypointLine(name, position.getLongitude(), position.getLatitude(), position.getTime(),
                    position.getElevation(), desc, WAYPOINT_ICON_NUMBER));
        }
        target.write(buffer.toString().getBytes(ISO_8859_1));
    }
}
