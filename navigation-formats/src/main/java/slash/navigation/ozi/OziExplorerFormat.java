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

    Copyright (C) 2002-2005 Robert Lipe, robertlipe+source@gpsbabel.org

    Translated to Java from GPSBabel ozi.cc (master 9c1e336) for RouteConverter, 2026; modified.
*/

package slash.navigation.ozi;

import slash.common.type.CompactCalendar;
import slash.navigation.base.GpxBackedNavigationFormat;
import slash.navigation.base.MultipleRoutesFormat;
import slash.navigation.base.RouteCharacteristics;
import slash.navigation.common.Datums;
import slash.navigation.gpx.Gpx10Format;
import slash.navigation.gpx.GpxPosition;
import slash.navigation.gpx.GpxRoute;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import static java.lang.Math.min;
import static java.nio.charset.CodingErrorAction.REPORT;
import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.math.RoundingMode.HALF_EVEN;
import static slash.common.io.InputOutput.readBytes;
import static slash.common.io.Transfer.isEmpty;
import static slash.common.io.Transfer.trim;
import static slash.navigation.base.RouteCharacteristics.Route;
import static slash.navigation.base.RouteCharacteristics.Track;
import static slash.navigation.base.RouteCharacteristics.Waypoints;

/**
 * The base of the OziExplorer formats: reads and writes OziExplorer Track (.plt), Route (.rte) and
 * Waypoint (.wpt) files natively, a translation of GPSBabel's ozi.cc with the options "pack" and
 * codec "windows-1252".
 *
 * @author Christian Pesch
 */

public abstract class OziExplorerFormat extends GpxBackedNavigationFormat implements MultipleRoutesFormat<GpxRoute> {
    private static final Charset WINDOWS_1252 = Charset.forName("windows-1252");
    private static final String CRLF = "\r\n";
    private static final double DAYS_SINCE_1990 = 25569; // sic: days between 1899-12-30 and 1970-01-01
    private static final int SECONDS_PER_DAY = 86400;
    private static final double FEET_TO_METERS = .3048;
    private static final double UNKNOWN_ALTITUDE = -777;
    private static final int MAXIMUM_DESCRIPTION_LENGTH = 40;
    private static final String DEFAULT_TRACK_NAME = "ComplimentsOfGPSBabel";
    private static final String ROUTE2_HEADER = "H1,OziExplorer CE Route2 File";
    private static final String ROUTE2_DATUM = "H2,";
    private static final String ROUTE2_NAME = "H3,";

    protected abstract RouteCharacteristics getRouteCharacteristics();

    /*
     * All text is Windows-1252, as the format defines it. GPSBabel 1.5.4 passes the text of route and track points
     * through as bytes to and from GPX (UTF-8), which destroys umlauts in both directions, and writes names as
     * ISO-8859-1 - maintainer 2026-11-19: prefer correct behaviour over GPSBabel 1.5.4 bugs. The file is handled
     * as ISO-8859-1 internally, so that every byte survives, and strings are converted per field.
     */

    /** Decodes a text from a string holding one character per file byte. */
    private static String decodeText(String field) {
        return new String(field.getBytes(ISO_8859_1), WINDOWS_1252);
    }

    /** Encodes a text to a string holding one character per file byte. */
    private static String encodeText(String text) {
        return new String(text.getBytes(WINDOWS_1252), ISO_8859_1);
    }

    /**
     * Decides from the first line of a file whether it is of this format. Like GPSBabel's ozi reader a line
     * naming a track or a route file is one, everything else starting with the OziExplorer prefix is read as waypoints.
     */
    protected abstract boolean isFileType(String firstLine);

    // reading

    protected GpxRoute sanitizeRouteAfterReading(GpxRoute route) {
        // all routes except for the first start with an 0.0/0.0/RPTxxx waypoint
        if (route != null &&
                route.getPositionCount() > 0 &&
                route.getPosition(0).getLongitude() == 0.0 &&
                route.getPosition(0).getLatitude() == 0.0 &&
                route.getPosition(0).getDescription() != null &&
                route.getPosition(0).getDescription().startsWith("RPT"))
            route.getPositions().remove(0);
        return route;
    }

    protected boolean isValidRoute(GpxRoute route) {
        List<GpxPosition> positions = route.getPositions();
        int count = 0;
        // has lots of zero element routes and routes with only one 0.0/0.0 waypoint
        for (GpxPosition position : positions) {
            if (isEmpty(position.getLongitude()) &&
                    isEmpty(position.getLatitude()) &&
                    isEmpty(position.getElevation()))
                count++;
        }
        return count != positions.size();
    }

    private static List<String> splitLines(String content) {
        List<String> lines = new ArrayList<>();
        int start = 0;
        while (start < content.length()) {
            int end = content.indexOf('\n', start);
            String line = content.substring(start, end == -1 ? content.length() : end);
            if (line.endsWith("\r"))
                line = line.substring(0, line.length() - 1);
            lines.add(line);
            if (end == -1)
                break;
            start = end + 1;
        }
        return lines;
    }

    private static double toDouble(String string) {
        try {
            double result = Double.parseDouble(string.trim());
            return Double.isNaN(result) || Double.isInfinite(result) ? 0.0 : result;
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    private static int toInt(String string) {
        try {
            return Integer.parseInt(string.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static String clean(String string) {
        return stripIllegalXmlCharacters(string);
    }

    private static String nullIfEmpty(String string) {
        return string == null || string.isEmpty() ? null : string;
    }

    private static CompactCalendar toTime(String string) {
        double oziTime = toDouble(string);
        if (oziTime > DAYS_SINCE_1990) {
            double seconds = (oziTime - DAYS_SINCE_1990) * SECONDS_PER_DAY;
            long millis = Math.round(seconds * 1000.0);
            return CompactCalendar.fromMillis(millis);
        }
        return null;
    }

    private static Double toElevation(String string, double altitudeScale) {
        double altitude = toDouble(string);
        if (altitude == UNKNOWN_ALTITUDE)
            return null;
        // GPSBabel hands the elevation over as text with six fractional digits
        return new BigDecimal(altitude * altitudeScale).setScale(6, HALF_EVEN).doubleValue();
    }

    /** A position as GPSBabel sees it while parsing a line. */
    private static class Point {
        double latitude, longitude;
        Double elevation;
        CompactCalendar time;
        String name, description;
    }

    private String decode(String field) {
        return field == null ? null : decodeText(field);
    }

    private GpxPosition toPosition(Point point, int datum) {
        double[] converted = Datums.toWgs84(point.latitude, point.longitude, 0.0, datum);
        String description = asDescription(nullIfEmpty(clean(decode(point.name))), nullIfEmpty(clean(decode(point.description))));
        return new GpxPosition(converted[1], converted[0], point.elevation, null, point.time, description);
    }

    /**
     * Looks up the datum of a header line. Files exported from spreadsheets pad every line with commas
     * up to the widest one, which GPSBabel's ozi reader tolerates.
     */
    private static int lookupDatum(String line) {
        int end = line.length();
        while (end > 0 && (line.charAt(end - 1) == ',' || Character.isWhitespace(line.charAt(end - 1))))
            end--;
        return Datums.lookupIndex(line.substring(0, end).trim());
    }

    /** Decodes the text of a Route2 file, which other programs write as UTF-8 instead of Windows-1252. */
    private static String decodeRoute2Text(String field) {
        try {
            return UTF_8.newDecoder().onMalformedInput(REPORT).onUnmappableCharacter(REPORT)
                    .decode(ByteBuffer.wrap(field.getBytes(ISO_8859_1))).toString();
        } catch (CharacterCodingException e) {
            return decodeText(field);
        }
    }

    /**
     * Reads an OziExplorer CE Route2 (.rt2) file, which GPSBabel does not know: three header lines
     * H1 (file type), H2 (datum) and H3 (route name) followed by one "W,name,latitude,longitude,flag"
     * line per position.
     */
    private List<GpxRoute> readRoute2(List<String> lines) {
        if (!lines.get(1).startsWith(ROUTE2_DATUM))
            return null;
        int datum = lookupDatum(lines.get(1).substring(ROUTE2_DATUM.length()));
        if (datum < 0)
            return null;

        String routeName = null;
        List<GpxPosition> positions = new ArrayList<>();
        for (int i = 2; i < lines.size(); i++) {
            String line = lines.get(i);
            String[] parts = line.split(",", -1);
            if (line.startsWith(ROUTE2_NAME))
                routeName = nullIfEmpty(clean(decodeRoute2Text(parts[1]).trim()));
            else if (line.startsWith("W,") && parts.length >= 4) {
                double[] converted = Datums.toWgs84(toDouble(parts[2]), toDouble(parts[3]), 0.0, datum);
                String name = nullIfEmpty(clean(decodeRoute2Text(parts[1]).trim()));
                positions.add(new GpxPosition(converted[1], converted[0], null, null, null, name));
            }
        }

        List<GpxRoute> result = new ArrayList<>();
        result.add(createRoute(Route, routeName, null, positions));
        return result;
    }

    protected List<GpxRoute> readRoutes(InputStream source) throws IOException {
        List<String> lines = splitLines(new String(readBytes(source), ISO_8859_1));
        if (lines.size() < 2)
            return null;

        String first = lines.get(0);
        if (first.startsWith(ROUTE2_HEADER))
            return getRouteCharacteristics() == Route ? readRoute2(lines) : null;
        if (!first.startsWith("OziExplorer ") || !isFileType(first))
            return null;

        int datum = lookupDatum(lines.get(1));
        if (datum < 0)
            return null;

        // default is feet, a track file may state otherwise in line 3
        double altitudeScale = FEET_TO_METERS;
        if (lines.size() > 2 && lines.get(2).regionMatches(true, 0, "Altitude is in ", 0, 15)) {
            String unit = lines.get(2).substring(15);
            if (unit.regionMatches(true, 0, "Feet", 0, 4))
                altitudeScale = FEET_TO_METERS;
            else if (unit.regionMatches(true, 0, "Meter", 0, 5))
                altitudeScale = 1.0;
            else
                return null;
        }

        RouteCharacteristics characteristics = getRouteCharacteristics();
        List<GpxRoute> result = new ArrayList<>();
        List<GpxPosition> current = characteristics == Track ? new ArrayList<>() : null;
        String trackName = null;
        String routeName = null, routeDescription = null;

        for (int i = 0; i < lines.size(); i++) {
            int lineCount = i + 1;
            String line = lines.get(i);

            if (characteristics == Track && lineCount == 5) {
                String[] trackParts = line.split(",", -1);
                if (trackParts.length >= 4)
                    trackName = nullIfEmpty(clean(decodeText(trackParts[3]).trim()));
            }

            if (line.indexOf(',') == -1)
                continue;
            String[] parts = line.split(",", -1);

            if (characteristics == Track) {
                if (lineCount <= 6)
                    continue;
                Point point = new Point();
                boolean newTrack = false;
                for (int field = 0; field < parts.length; field++) {
                    String part = parts[field];
                    if (part.isEmpty())
                        continue;
                    switch (field) {
                        case 0:
                            point.latitude = toDouble(part);
                            break;
                        case 1:
                            point.longitude = toDouble(part);
                            break;
                        case 2:
                            newTrack = toInt(part) == 1;
                            break;
                        case 3:
                            point.elevation = toElevation(part, altitudeScale);
                            break;
                        case 4:
                            point.time = toTime(part);
                            break;
                        default:
                            break;
                    }
                }
                if (newTrack && !current.isEmpty()) {
                    result.add(createRoute(Track, trackName, null, current));
                    current = new ArrayList<>();
                    // the names of the following tracks are not set by GPSBabel
                    trackName = null;
                }
                current.add(toPosition(point, datum));
            } else if (characteristics == Route) {
                if (line.charAt(0) == 'R') {
                    if (current != null)
                        result.add(createRoute(Route, routeName, routeDescription, current));
                    current = new ArrayList<>();
                    routeName = null;
                    routeDescription = null;
                    for (int field = 0; field < parts.length; field++) {
                        String part = parts[field].replace(",", "");
                        if (field == 2)
                            routeName = nullIfEmpty(clean(decodeText(part)));
                        else if (field == 3)
                            routeDescription = nullIfEmpty(clean(decodeText(part)));
                    }
                } else if (lineCount > 5 && current != null) {
                    Point point = new Point();
                    for (int field = 0; field < parts.length; field++) {
                        String part = parts[field];
                        if (part.isEmpty())
                            continue;
                        switch (field) {
                            case 4:
                                point.name = part;
                                break;
                            case 5:
                                point.latitude = toDouble(part);
                                break;
                            case 6:
                                point.longitude = toDouble(part);
                                break;
                            case 7:
                                point.time = toTime(part);
                                break;
                            case 13:
                                point.description = part;
                                break;
                            default:
                                break;
                        }
                    }
                    current.add(toPosition(point, datum));
                }
            } else if (characteristics == Waypoints) {
                if (lineCount <= 4)
                    continue;
                if (current == null)
                    current = new ArrayList<>();
                Point point = new Point();
                for (int field = 0; field < parts.length; field++) {
                    String part = parts[field];
                    if (part.isEmpty())
                        continue;
                    switch (field) {
                        case 1:
                            point.name = part.trim();
                            break;
                        case 2:
                            point.latitude = toDouble(part);
                            break;
                        case 3:
                            point.longitude = toDouble(part);
                            break;
                        case 4:
                            point.time = toTime(part);
                            break;
                        case 10:
                            point.description = part.trim();
                            break;
                        case 14:
                            point.elevation = toElevation(part, altitudeScale);
                            break;
                        default:
                            break;
                    }
                }
                current.add(toPosition(point, datum));
            }
        }

        if (characteristics == Track) {
            if (current != null)
                result.add(createRoute(Track, trackName, null, current));
        } else if (characteristics == Route) {
            if (current != null)
                result.add(createRoute(Route, routeName, routeDescription, current));
        } else if (current != null)
            result.add(createRoute(Waypoints, null, null, current));
        return result;
    }

    private GpxRoute createRoute(RouteCharacteristics characteristics, String name, String description, List<GpxPosition> positions) {
        return new GpxRoute(new Gpx10Format(), characteristics, name, asDescription(description), positions);
    }

    // writing

    /** A position as GPSBabel sees it after reading the GPX written by RouteConverter. */
    private static class WritePoint {
        double latitude, longitude;
        Double elevation;
        CompactCalendar time;
        String shortName, description;
    }

    private List<WritePoint> prepareForWriting(GpxRoute route, int startIndex, int endIndex) {
        // like GPSBabel's old OziExplorerFormat.prepareForWriting() this modifies the route
        route.setCharacteristics(getRouteCharacteristics());

        List<WritePoint> result = new ArrayList<>();
        List<GpxPosition> positions = route.getPositions();
        // all descriptions are reduced, not only the ones in the range to be written
        for (GpxPosition position : positions) {
            String description = position.getDescription();
            if (description != null) {
                description = description.substring(0, min(description.length(), MAXIMUM_DESCRIPTION_LENGTH));
                position.setDescription(description.replace(",", " "));
            }
        }
        for (int i = startIndex; i < endIndex; i++) {
            GpxPosition position = positions.get(i);
            if (position.getLatitude() == null || position.getLongitude() == null)
                continue;

            // reduce description to 40 chars and replace comma by space (character 209 aka Ñ didn't work)
            String description = position.getDescription();
            if (description != null) {
                description = description.substring(0, min(description.length(), MAXIMUM_DESCRIPTION_LENGTH));
                description = description.replace(",", " ");
            }

            WritePoint point = new WritePoint();
            point.latitude = position.getLatitude();
            point.longitude = position.getLongitude();
            point.elevation = position.getElevation();
            point.time = position.getTime();
            point.shortName = asName(description);
            point.description = asDesc(description);
            result.add(point);
        }
        return result;
    }

    private static String fixed(double value, int digits) {
        String result = new BigDecimal(value).setScale(digits, HALF_EVEN).toPlainString();
        // C and Qt keep the sign of values that round to zero
        if (value < 0.0 && !result.startsWith("-"))
            result = "-" + result;
        return result;
    }

    private static String clean(String string, String badCharacters) {
        if (string == null)
            return "";
        StringBuilder buffer = new StringBuilder(string.length());
        for (int i = 0; i < string.length(); i++) {
            if (badCharacters.indexOf(string.charAt(i)) == -1)
                buffer.append(string.charAt(i));
        }
        return buffer.toString();
    }

    private static String joinDescription(List<String> strings) {
        return strings == null ? null : String.join(",\n", strings);
    }

    private static String nullToEmpty(String string) {
        return string == null ? "" : string;
    }

    private static String toTimeString(CompactCalendar time) {
        if (time == null)
            return "";
        double days = (time.getTimeInMillis() / 1000.0) / SECONDS_PER_DAY + DAYS_SINCE_1990;
        return fixed(days, 7);
    }

    private static double toFeet(Double elevation) {
        if (elevation == null || elevation.isNaN() || elevation.isInfinite())
            return UNKNOWN_ALTITUDE;
        return elevation * (1.0 / FEET_TO_METERS);
    }

    private void writeTracks(StringBuilder buffer, List<GpxRoute> routes, List<List<WritePoint>> points) {
        // all tracks go into one file, the name is the one of the first track
        String name = nullIfEmpty(asRouteName(routes.get(0).getName()));
        buffer.append("OziExplorer Track Point File Version 2.1").append(CRLF)
                .append("WGS 84").append(CRLF)
                .append("Altitude is in Feet").append(CRLF)
                .append("Reserved 3").append(CRLF)
                .append("0,2,255,").append(name == null ? DEFAULT_TRACK_NAME : encodeText(name)).append(",0,0,2,8421376").append(CRLF)
                .append("0").append(CRLF);
        for (List<WritePoint> track : points) {
            int newTrack = 1;
            for (WritePoint point : track) {
                buffer.append(fixed(point.latitude, 6)).append(',')
                        .append(fixed(point.longitude, 6)).append(',')
                        .append(newTrack).append(',')
                        .append(fixed(toFeet(point.elevation), 0)).append(',')
                        .append(toTimeString(point.time)).append(",,").append(CRLF);
                newTrack = 0;
            }
        }
    }

    private void writeRoutes(StringBuilder buffer, List<GpxRoute> routes, List<List<WritePoint>> points) {
        buffer.append("OziExplorer Route File Version 1.0").append(CRLF)
                .append("WGS 84").append(CRLF)
                .append("Reserved 1").append(CRLF)
                .append("Reserved 2").append(CRLF);
        for (int i = 0; i < routes.size(); i++) {
            GpxRoute route = routes.get(i);
            int routeNumber = i + 1;
            buffer.append("R,").append(routeNumber).append(',')
                    .append(encodeText(nullToEmpty(asRouteName(route.getName())))).append(',')
                    .append(encodeText(nullToEmpty(joinDescription(route.getDescription())))).append(',').append(CRLF);
            int count = 0;
            for (WritePoint point : points.get(i)) {
                count++;
                buffer.append("W,").append(routeNumber).append(",,").append(count).append(',')
                        .append(encodeText(nullToEmpty(point.shortName))).append(',')
                        .append(fixed(point.latitude, 6)).append(',')
                        .append(fixed(point.longitude, 6)).append(',')
                        .append(toTimeString(point.time)).append(",0,1,3,0,65535,")
                        .append(",0,0").append(CRLF);
            }
        }
    }

    private void writeWaypoints(StringBuilder buffer, List<List<WritePoint>> points) {
        buffer.append("OziExplorer Waypoint File Version 1.1").append(CRLF)
                .append("WGS 84").append(CRLF)
                .append("Reserved 2").append(CRLF)
                .append("Reserved 3").append(CRLF);
        int index = 0;
        for (List<WritePoint> list : points) {
            for (WritePoint point : list) {
                String shortName, description;
                if (point.shortName == null || point.shortName.isEmpty())
                    shortName = point.description != null && !point.description.isEmpty() ?
                            clean(point.description, ",\r\n") : "";
                else
                    shortName = clean(point.shortName, ",\r\n");
                if (point.description == null || point.description.isEmpty())
                    description = clean(shortName, ",\r\n");
                else
                    description = clean(point.description, ",\r\n");

                buffer.append(++index).append(',')
                        .append(encodeText(shortName)).append(',')
                        .append(fixed(point.latitude, 6)).append(',')
                        .append(fixed(point.longitude, 6)).append(',')
                        .append(toTimeString(point.time)).append(',')
                        .append("0,1,3,0,65535,")
                        .append(encodeText(description)).append(",0,0,0,")
                        .append(fixed(toFeet(point.elevation), 0))
                        .append(",6,0,17").append(CRLF);
            }
        }
    }

    public void write(GpxRoute route, OutputStream target, int startIndex, int endIndex) throws IOException {
        write(java.util.Collections.singletonList(route), java.util.Collections.singletonList(prepareForWriting(route, startIndex, endIndex)), target);
    }

    public void write(List<GpxRoute> routes, OutputStream target) throws IOException {
        List<List<WritePoint>> points = new ArrayList<>();
        for (GpxRoute route : routes)
            points.add(prepareForWriting(route, 0, route.getPositionCount()));
        write(routes, points, target);
    }

    private void write(List<GpxRoute> routes, List<List<WritePoint>> points, OutputStream target) throws IOException {
        if (routes.isEmpty())
            return;

        // the native writer uses the characteristics of the class: .rte for Routes, .plt for Tracks, .wpt for Waypoints
        RouteCharacteristics characteristics = getRouteCharacteristics();
        StringBuilder buffer = new StringBuilder();
        if (characteristics == Track)
            writeTracks(buffer, routes, points);
        else if (characteristics == Route)
            writeRoutes(buffer, routes, points);
        else
            writeWaypoints(buffer, points);
        target.write(buffer.toString().getBytes(ISO_8859_1));
        target.flush();
    }
}
