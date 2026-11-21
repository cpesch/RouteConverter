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

    Copyright (C) 2002-2006 Robert Lipe, robertlipe+source@gpsbabel.org
    Copyright (C) 2002 Robert Lipe, robertlipe@usa.net
    Translated to Java from GPSBabel mapsend.cc, mapsend.h (gpsbabel_1_7_0) for RouteConverter, 2026; modified.
    Copyright (C) 2003-2006 Robert Lipe, robertlipe+source@gpsbabel.org
    Translated to Java from GPSBabel mkshort.cc (gpsbabel_1_5_4) for RouteConverter, 2026; modified.
*/

package slash.navigation.magellan;

import slash.navigation.base.GpxBackedNavigationFormat;
import slash.navigation.gpsbabel.LittleEndianInput;
import slash.navigation.gpsbabel.LittleEndianOutput;
import slash.navigation.gpx.Gpx10Format;
import slash.navigation.gpx.GpxPosition;
import slash.navigation.gpx.GpxRoute;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Collections.emptyList;
import static slash.common.io.Transfer.trim;
import static slash.navigation.base.RouteCharacteristics.Route;
import static slash.navigation.base.RouteCharacteristics.Waypoints;
import static slash.navigation.common.NavigationConversion.formatElevation;
import static slash.navigation.common.NavigationConversion.formatPosition;

/**
 * Reads and writes Magellan MapSend (.wpt) files. A native translation of GPSBabel's mapsend format as
 * RouteConverter used it: options "-r -w", i.e. waypoint files with their routes; track files are not delivered.
 *
 * @author Christian Pesch
 */

public class MagellanMapSendFormat extends GpxBackedNavigationFormat {
    // the format spec declares no character set; Windows-1252 covers what MapSend files contain
    static final Charset CHARSET = Charset.forName("windows-1252");

    private static final int HEADER_LENGTH = 13;
    private static final int HEADER_SIZE = 18;
    private static final String SIGNATURE_PREFIX = "4D53333";
    private static final String SIGNATURE_SUFFIX = " MS";
    private static final int TYPE_WAYPOINT = 1;
    // name, description, number, icon, status, altitude, longitude, latitude
    private static final int MINIMUM_WAYPOINT_SIZE = 1 + 1 + 4 + 1 + 1 + 3 * 8;
    // name, number, count
    private static final int MINIMUM_ROUTE_SIZE = 1 + 4 + 4;
    // name, number, longitude, latitude, icon
    private static final int MINIMUM_ROUTE_POINT_SIZE = 1 + 4 + 2 * 8 + 1;
    private static final int MAXIMUM_DESCRIPTION_LENGTH = 30;
    private static final int SHORT_NAME_LENGTH = 8;
    private static final String DEFAULT_ROUTE_NAME = "Route";
    // GPSBabel's unknown_alt
    private static final double UNKNOWN_ALTITUDE = -99999999.0;
    // GPSBabel writes coordinates to GPX with %0.9f and elevations with %0.6f
    private static final int GPX_COORDINATE_DIGITS = 9;
    private static final int GPX_ELEVATION_DIGITS = 6;

    public String getExtension() {
        return ".wpt";
    }

    public String getName() {
        return "Magellan MapSend (*" + getExtension() + ")";
    }

    public boolean isSupportsMultipleRoutes() {
        return false; // just guesses
    }

    // reading

    static boolean isMapSendHeader(byte[] data) {
        if (data.length < HEADER_SIZE || (data[0] & 0xFF) != HEADER_LENGTH)
            return false;
        String signature = new String(data, 1, 11, CHARSET);
        if (!signature.startsWith(SIGNATURE_PREFIX) || !signature.endsWith(SIGNATURE_SUFFIX) ||
                !isDigit(signature.charAt(SIGNATURE_PREFIX.length())))
            return false;
        return isDigit((char) data[12]) && isDigit((char) data[13]);
    }

    private static boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    protected List<GpxRoute> readRoutes(InputStream source) throws IOException {
        byte[] data = source.readAllBytes();
        if (!isMapSendHeader(data))
            return emptyList();

        LittleEndianInput input = new LittleEndianInput(data);
        input.seek(HEADER_SIZE - 4);
        int type = input.readInt16();
        // regions and logs are fatal to GPSBabel; track files are not delivered with "-r -w"
        if (type != TYPE_WAYPOINT)
            return emptyList();

        input.seek(HEADER_SIZE);
        try {
            return readWaypointFile(input);
        } catch (EOFException e) {
            return emptyList();
        }
    }

    private static boolean fits(LittleEndianInput input, long count, int minimumSize) {
        return count >= 0 && count * minimumSize <= input.remaining();
    }

    private List<GpxRoute> readWaypointFile(LittleEndianInput input) throws EOFException {
        List<GpxRoute> result = new ArrayList<>();

        int waypointCount = input.readInt32();
        if (!fits(input, waypointCount, MINIMUM_WAYPOINT_SIZE))
            return emptyList();

        List<GpxPosition> waypoints = new ArrayList<>(waypointCount);
        for (int i = 0; i < waypointCount; i++) {
            String shortName = trim(input.readPascalString(CHARSET));
            String description = trim(input.readPascalString(CHARSET));
            input.readInt32(); // number
            input.readUInt8(); // icon
            input.readUInt8(); // status
            double altitude = input.readDouble();
            double longitude = input.readDouble();
            double latitude = -input.readDouble();
            if (!Double.isFinite(longitude) || !Double.isFinite(latitude))
                return emptyList();

            // GPSBabel's waypt_add() wraps coordinates once and fails the whole file if they are still out of range
            if (latitude < -90.0)
                latitude += 180.0;
            else if (latitude > 90.0)
                latitude -= 180.0;
            if (longitude < -180.0)
                longitude += 360.0;
            else if (longitude > 180.0)
                longitude -= 360.0;
            if (latitude < -90.0 || latitude > 90.0 || longitude < -180.0 || longitude > 180.0)
                return emptyList();

            // GPSBabel's waypt_add(): a waypoint without description takes its name; gbfgetpstr() never returns
            // a null name, so nothing is numbered and both empty leave the description empty
            if (description == null)
                description = shortName;
            // the GPX that RouteConverter reads delivers <desc> only, see asWayPointDescription()
            waypoints.add(createPosition(longitude, latitude, asGpxElevation(altitude), null, description));
        }
        if (!waypoints.isEmpty())
            result.add(new GpxRoute(new Gpx10Format(), Waypoints, null, null, waypoints));

        int routeCount = input.readInt32();
        if (!fits(input, routeCount, MINIMUM_ROUTE_SIZE))
            return emptyList();

        // GPSBabel's route_add_wpt() numbers with rte_waypts, which counts the points of all routes
        int routePointCount = 0;
        for (int i = 0; i < routeCount; i++) {
            String routeName = stripIllegalXmlCharacters(trim(input.readPascalString(CHARSET)));
            input.readInt32(); // route number
            int pointCount = input.readInt32();
            if (!fits(input, pointCount, MINIMUM_ROUTE_POINT_SIZE))
                return emptyList();

            List<GpxPosition> positions = new ArrayList<>(pointCount);
            for (int j = 0; j < pointCount; j++) {
                String shortName = trim(input.readPascalString(CHARSET));
                input.readInt32(); // waypoint number
                double longitude = input.readDouble();
                double latitude = -input.readDouble();
                input.readUInt8(); // icon
                if (!Double.isFinite(longitude) || !Double.isFinite(latitude))
                    return emptyList();

                routePointCount++;
                // GPSBabel's route_add_wpt(): a route point without name is numbered across all routes
                if (shortName == null)
                    shortName = String.format("RPT%03d", routePointCount);
                // route points carry no altitude: the GPX gets no <ele>
                positions.add(createPosition(longitude, latitude, null, shortName, null));
            }
            // GPSBabel 1.5.4 delivers a route without points as an empty <rte>, which carries nothing
            if (!positions.isEmpty())
                result.add(new GpxRoute(new Gpx10Format(), Route, routeName, null, positions));
        }
        return result;
    }

    private GpxPosition createPosition(double longitude, double latitude, Double elevation, String name, String desc) {
        String description = asDescription(stripIllegalXmlCharacters(name), stripIllegalXmlCharacters(desc));
        return new GpxPosition(asGpxCoordinate(longitude), asGpxCoordinate(latitude), elevation, null, null, description);
    }

    private static double asGpxCoordinate(double value) {
        return new BigDecimal(value).setScale(GPX_COORDINATE_DIGITS, RoundingMode.HALF_EVEN).doubleValue();
    }

    private static Double asGpxElevation(double altitude) {
        if (altitude == UNKNOWN_ALTITUDE || !Double.isFinite(altitude))
            return null;
        return new BigDecimal(altitude).setScale(GPX_ELEVATION_DIGITS, RoundingMode.HALF_EVEN).doubleValue();
    }

    // writing

    private static byte[] encode(String string) {
        return string.getBytes(CHARSET);
    }

    private static void writeHeader(LittleEndianOutput output) {
        output.writeUInt8(HEADER_LENGTH);
        output.writeBytes(encode(SIGNATURE_PREFIX + "0" + SIGNATURE_SUFFIX));
        output.writeBytes(encode("30"));
        output.writeUInt8(TYPE_WAYPOINT);
        output.writeBytes(new byte[3]);
    }

    public void write(GpxRoute route, OutputStream target, int startIndex, int endIndex) throws IOException {
        List<GpxPosition> positions = new ArrayList<>();
        for (int i = startIndex; i < endIndex; i++) {
            GpxPosition position = route.getPositions().get(i);
            // the GPX written for GPSBabel skips positions without valid coordinates
            if (formatPosition(position.getLongitude()) != null && formatPosition(position.getLatitude()) != null)
                positions.add(position);
        }

        LittleEndianOutput output = new LittleEndianOutput();
        writeHeader(output);

        // "-w" comes last and makes the waypoints the objective: every position is a waypoint
        output.writeInt32(positions.size());
        ShortNames shortNames = new ShortNames(SHORT_NAME_LENGTH);
        for (int i = 0; i < positions.size(); i++)
            writeWaypoint(output, positions.get(i), i + 1, shortNames);

        // the route
        output.writeInt32(1);
        String routeName = stripIllegalXmlCharacters(asRouteName(route.getName()));
        output.writePascalString(routeName != null ? routeName : DEFAULT_ROUTE_NAME, CHARSET);
        output.writeInt32(0); // route number
        output.writeInt32(positions.size());
        for (int i = 0; i < positions.size(); i++)
            writeRoutePoint(output, positions.get(i), i + 1);

        target.write(output.toByteArray());
    }

    private static double asCoordinate(Double value) {
        return formatPosition(value).doubleValue();
    }

    private static double asAltitude(Double elevation) {
        BigDecimal altitude = formatElevation(elevation);
        // GPSBabel writes 0 for an unknown altitude
        return altitude != null ? altitude.doubleValue() : 0.0;
    }

    private void writeWaypoint(LittleEndianOutput output, GpxPosition position, int number, ShortNames shortNames) {
        String name = stripIllegalXmlCharacters(asName(position.getDescription()));
        String desc = stripIllegalXmlCharacters(asDesc(position.getDescription()));
        // GPSBabel reads <name> as shortname and <desc> as notes; waypt_add() fills the gaps
        String shortName = name != null ? name : desc != null ? desc : String.format("WPT%03d", number);
        String description = desc != null ? desc : shortName;

        output.writePascalString(shortNames.mkshort(shortName), CHARSET);
        String cut = description.substring(0, Math.min(description.length(), MAXIMUM_DESCRIPTION_LENGTH));
        output.writePascalString(cut, CHARSET);
        output.writeInt32(number);
        output.writeUInt8(0); // icon: none
        output.writeUInt8(1); // status
        output.writeDouble(asAltitude(position.getElevation()));
        output.writeDouble(asCoordinate(position.getLongitude()));
        output.writeDouble(-asCoordinate(position.getLatitude()));
    }

    private void writeRoutePoint(LittleEndianOutput output, GpxPosition position, int number) {
        String name = stripIllegalXmlCharacters(asName(position.getDescription()));
        // GPSBabel's route_add_wpt() numbers a route point without name
        output.writePascalString(name != null ? name : String.format("RPT%03d", number), CHARSET);
        output.writeInt32(number);
        output.writeDouble(asCoordinate(position.getLongitude()));
        output.writeDouble(-asCoordinate(position.getLatitude()));
        output.writeUInt8(0); // icon: none
    }

    /**
     * GPSBabel 1.5.4's mkshort() with the settings of mapsend.cc's waypoint handle: whitespace allowed, target
     * length 8, default bad characters, unique names. It works on characters where GPSBabel works on UTF-8 bytes.
     */
    static class ShortNames {
        private static final String VOWELS = "aeiouAEIOU";
        private static final String BAD_CHARACTERS = "\"$.,'!-";
        private static final String DEFAULT_NAME = "WPT";
        private static final String[][] REPLACEMENTS = {
                {"zero", "0"}, {"one", "1"}, {"two", "2"}, {"three", "3"}, {"four", "4"},
                {"five", "5"}, {"six", "6"}, {"seven", "7"}, {"eight", "8"}, {"nine", "9"}
        };

        private final int targetLength;
        private final Map<String, int[]> conflictCounters = new HashMap<>();

        ShortNames(int targetLength) {
            this.targetLength = targetLength;
        }

        private static boolean isSpace(char c) {
            return c == ' ' || (c >= '\t' && c <= '\r');
        }

        private static String upperCase(String string) {
            StringBuilder result = new StringBuilder(string.length());
            for (int i = 0; i < string.length(); i++) {
                char c = string.charAt(i);
                result.append(c >= 'a' && c <= 'z' ? (char) (c - 'a' + 'A') : c);
            }
            return result.toString();
        }

        private static String replaceConstants(String string) {
            int length = string.length();
            for (String[] replacement : REPLACEMENTS) {
                int replacementLength = replacement[0].length();
                if (length - replacementLength > 1 &&
                        string.substring(length - replacementLength).equalsIgnoreCase(replacement[0]) &&
                        string.charAt(length - replacementLength - 1) == ' ')
                    return string.substring(0, length - replacementLength) + replacement[1];
            }
            return string;
        }

        // delete_last_vowel(): removes the last vowel behind start that does not begin a word
        private static boolean deleteLastVowel(int start, StringBuilder string) {
            for (int l = string.length(); l > start; l--) {
                if (VOWELS.indexOf(string.charAt(l - 1)) != -1) {
                    if (string.charAt(l - 2) == ' ')
                        continue;
                    string.deleteCharAt(l - 1);
                    return true;
                }
            }
            return false;
        }

        String mkshort(String input) {
            String string = input;

            // the seven digit geocache hack
            if (targetLength == 6 && string.length() == 7 && string.startsWith("GC"))
                string = string.substring(1);

            // whack leading "[Tt]he"
            if (string.length() > targetLength + 4 && (string.startsWith("The ") || string.startsWith("the ")))
                string = string.substring(4);

            // eliminate leading whitespace
            int start = 0;
            while (start < string.length() && isSpace(string.charAt(start)))
                start++;
            string = string.substring(start);

            string = replaceConstants(string);

            // eliminate characters on the blacklist
            int lengthBeforeBlacklist = string.length();
            StringBuilder builder = new StringBuilder(string.length());
            for (int i = 0; i < string.length(); i++) {
                char c = string.charAt(i);
                if (BAD_CHARACTERS.indexOf(c) == -1)
                    builder.append(c);
            }

            // eliminate repeated whitespace, as GPSBabel does: one pass that skips the character behind a removal
            for (int i = 0; i < lengthBeforeBlacklist - 1; i++) {
                if (i + 1 < builder.length() && builder.charAt(i) == ' ' && builder.charAt(i + 1) == ' ')
                    builder.deleteCharAt(i);
            }

            // toss vowels from the end to approach the target length, keeping the leading two characters
            boolean replaced = targetLength < 15;
            while (replaced && builder.length() > targetLength)
                replaced = deleteLastVowel(2, builder);

            // truncate, but preserve trailing digits
            int digitsStart = builder.length();
            while (digitsStart > 0 && builder.charAt(digitsStart - 1) >= '0' && builder.charAt(digitsStart - 1) <= '9')
                digitsStart--;
            String digits = builder.substring(digitsStart);
            if (builder.length() > targetLength) {
                int keep = Math.max(0, targetLength - digits.length());
                builder.setLength(keep);
                builder.append(digits);
                // rtrim()
                int end = builder.length();
                while (end > 0 && isSpace(builder.charAt(end - 1)))
                    end--;
                builder.setLength(end);
            }

            String result = builder.length() > 0 ? builder.toString() : DEFAULT_NAME;
            return makeUnique(result);
        }

        // mkshort_add_to_list(): appends or overlays ".<n>" until the name is unique, ignoring case
        private String makeUnique(String name) {
            int[] counter;
            while ((counter = conflictCounters.get(upperCase(name))) != null) {
                counter[0]++;
                String suffix = "." + counter[0];
                int length = name.length();
                if (length + suffix.length() < targetLength)
                    name = name + suffix;
                else
                    name = name.substring(0, Math.max(0, length - suffix.length())) + suffix;
            }
            conflictCounters.put(upperCase(name), new int[]{0});
            return name;
        }
    }
}
