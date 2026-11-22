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

    Translated to Java from GPSBabel gdb.cc/.h and garmin_fs.cc/.h (master 9c1e336) for RouteConverter, 2026; modified.
    Copyright (C) 2005-2008 Olaf Klein, o.b.klein@gpsbabel.org
    Copyright (C) 2005 Robert Lipe, robertlipe+source@gpsbabel.org
*/

package slash.navigation.garmin;

import slash.common.type.CompactCalendar;
import slash.navigation.base.GpxBackedNavigationFormat;
import slash.navigation.base.MultipleRoutesFormat;
import slash.navigation.base.RouteCharacteristics;
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
import java.util.List;
import java.util.Locale;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.singletonList;
import static slash.common.type.CompactCalendar.fromMillis;
import static slash.navigation.base.RouteCharacteristics.Route;
import static slash.navigation.base.RouteCharacteristics.Track;
import static slash.navigation.base.RouteCharacteristics.Waypoints;
import static slash.navigation.common.NavigationConversion.formatElevation;
import static slash.navigation.common.NavigationConversion.formatPosition;
import static slash.navigation.garmin.GarminDefaults.UNKNOWN_COLOR;
import static slash.navigation.garmin.GarminDefaults.gdbRouteColorIndex;
import static slash.navigation.garmin.GarminDefaults.gdbTrackColorIndex;
import static slash.navigation.garmin.GarminDefaults.gdbWaypointClass;
import static slash.navigation.garmin.GarminDefaults.gdbWaypointDisplayMode;
import static slash.navigation.garmin.GarminDefaults.gdbWaypointIconNumber;
import static slash.navigation.gpsbabel.GpsBabelMath.semicirclesToDegrees;

/**
 * Reads and writes Garmin MapSource 6.x (.gdb) files, the Garmin GPS Database versions 1 to 3.
 * <p>
 * Reading delivers what GPSBabel 1.5.4 delivered through its GPX 1.0 exchange file: every waypoint
 * record as a waypoint, the routes with their via points and the tracks. Writing produces version 3
 * files the way GPSBabel's {@code ver=3} did, with RouteConverter's watermark in the header (D15.1).
 *
 * @author Christian Pesch
 */

public class GarminMapSource6Format extends GpxBackedNavigationFormat implements MultipleRoutesFormat<GpxRoute> {
    static final byte[] SIGNATURE = {'M', 's', 'R', 'c', 'f', 0};
    static final String WATERMARK = "RouteConverter";
    // the date of the native port, written like GPSBabel's gdb_release_dt
    static final String WATERMARK_DATE = "Nov 22 2026";
    static final String WATERMARK_TIME = "00:00:00";
    static final int WRITE_VERSION = 3;
    static final int UTF8_VERSION = 3;
    static final int PROGRAM_VERSION = 605;
    private static final int FMT_USE_MASK = 3;
    static final double UNKNOWN_ALTITUDE = -99999999.0;
    private static final double ALTITUDE_LIMIT = 1.0e24;
    private static final int NAME_LENGTH = 1024;
    private static final int MAXIMUM_HEADER_RECORD_LENGTH = 128;
    private static final int GPX_COORDINATE_DIGITS = 9;
    private static final int GPX_ELEVATION_DIGITS = 6;
    private static final double SEMICIRCLES_PER_DEGREE = (double) (1L << 31) / 180.0;
    private static final double EARTH_RADIUS = 6378137.0;
    private static final double MAXIMUM_ROUTE_POINT_DISTANCE = 100.0;

    public String getExtension() {
        return ".gdb";
    }

    public String getName() {
        return "Garmin MapSource 6.x (*" + getExtension() + ")";
    }

    public boolean isSupportsMultipleRoutes() {
        return true;
    }

    public boolean isWritingRouteCharacteristics() {
        return true;
    }

    /** a file GPSBabel refuses: the native reader returns no route */
    static class GdbException extends Exception {
        GdbException(String message) {
            super(message);
        }
    }

    /** GPSBabel's Waypoint, reduced to the fields gdb.cc and the GPX exchange use */
    static class Wpt {
        String shortName;
        String notes;
        String description;
        double latitude;
        double longitude;
        double altitude = UNKNOWN_ALTITUDE;
        // seconds since 1970 as GPSBabel's SetCreationTime(int32) keeps them, 0 = none
        long time;
        int waypointClass;

        Wpt copy() {
            Wpt result = new Wpt();
            result.shortName = shortName;
            result.notes = notes;
            result.description = description;
            result.latitude = latitude;
            result.longitude = longitude;
            result.altitude = altitude;
            result.time = time;
            result.waypointClass = waypointClass;
            return result;
        }
    }

    static class Rte {
        String name;
        String description;
        final List<Wpt> points = new ArrayList<>();
    }

    // reading

    protected List<GpxRoute> readRoutes(InputStream source) throws IOException {
        byte[] bytes = source.readAllBytes();
        try {
            return new Reader(bytes).read();
        } catch (GdbException | EOFException e) {
            // GPSBabel stops with a fatal error: no route
            return null;
        }
    }

    static boolean isGdb(byte[] bytes) {
        if (bytes.length < SIGNATURE.length)
            return false;
        for (int i = 0; i < SIGNATURE.length; i++)
            if (bytes[i] != SIGNATURE[i])
                return false;
        return true;
    }

    /** {@code GdbFormat::read_file_header()}, {@code read_waypoint()}, {@code read_route()}, {@code read_track()} */
    class Reader {
        private final byte[] bytes;
        private int version;
        private final List<Wpt> waypoints = new ArrayList<>();
        // the copies in wayptq_in/wayptq_in_hidden; GPSBabel 1.5.4 searches them first match first
        private final List<Wpt> waypointQueue = new ArrayList<>();
        private final List<Rte> routes = new ArrayList<>();
        private final List<Rte> tracks = new ArrayList<>();
        private int routePointCount;

        Reader(byte[] bytes) {
            this.bytes = bytes;
        }

        private Charset charset() {
            return version >= UTF8_VERSION ? UTF_8 : ISO_8859_1;
        }

        // fread_cstr(): an empty string becomes a null string
        private String readString(LittleEndianInput input) throws EOFException {
            String result = input.readCString(charset());
            return result.isEmpty() ? null : result;
        }

        // FREAD_CSTR_AS_QSTR: gbfgetcstr() decodes UTF-8 in every version and never returns a null string
        private String readQString(LittleEndianInput input) throws EOFException {
            return input.readCString(UTF_8);
        }

        private void skipString(LittleEndianInput input) throws EOFException {
            input.readCString(ISO_8859_1);
        }

        private double readLatLon(LittleEndianInput input) throws EOFException {
            return semicirclesToDegrees(input.readInt32());
        }

        private void readHeader(LittleEndianInput input) throws IOException, GdbException {
            if (!isGdb(bytes))
                throw new GdbException("Invalid file");
            input.skip(SIGNATURE.length);
            input.readInt32();
            String record = input.readCString(ISO_8859_1);
            if (record.isEmpty() || record.charAt(0) != 'D')
                throw new GdbException("Invalid file");
            version = record.length() > 1 ? record.charAt(1) - 'k' + 1 : -'k' + 1;
            if (version < 1 || version > 3)
                throw new GdbException("Unknown or/and unsupported GDB version " + version);
            int length = input.readInt32();
            if (length < 0 || length + 1 > MAXIMUM_HEADER_RECORD_LENGTH)
                throw new GdbException("Invalid record length");
            input.skip(length + 1);
            String application = input.readCString(ISO_8859_1);
            if (!("MapSource".equals(application) || "BaseCamp".equals(application)))
                throw new GdbException("Not a recognized signature in header");
        }

        List<GpxRoute> read() throws IOException, GdbException {
            LittleEndianInput input = new LittleEndianInput(bytes);
            readHeader(input);

            while (true) {
                int length = input.readInt32();
                char type = (char) input.readUInt8();
                if (type == 'V')
                    break;
                // gbfcopyfrom() caches the record; the reader resyncs on the declared length
                if (length < 0 || length > input.remaining())
                    throw new GdbException("Attempt to read past EOF");
                LittleEndianInput record = new LittleEndianInput(input.readBytes(length));
                switch (type) {
                    case 'W' -> addWaypoint(readWaypoint(record));
                    case 'R' -> routes.add(readRoute(record));
                    case 'T' -> tracks.add(readTrack(record));
                    default -> {
                    }
                }
            }
            return toRoutes();
        }

        // waypt_add(): normalizes the position and fills missing names
        private void addWaypoint(Wpt wpt) {
            waypoints.add(wpt);
            normalize(wpt);

            if (wpt.shortName == null) {
                if (wpt.description != null)
                    wpt.shortName = wpt.description;
                else if (wpt.notes != null)
                    wpt.shortName = wpt.notes;
                else
                    wpt.shortName = String.format("WPT%03d", waypoints.size());
            }
            if (wpt.description == null || wpt.description.isEmpty()) {
                if (wpt.notes != null)
                    wpt.description = wpt.notes;
                else
                    wpt.description = wpt.shortName;
            }
            // GPSBabel 1.5.4 never hides a waypoint without the via/roadbook options
            waypointQueue.add(wpt.copy());
        }

        private Wpt readWaypoint(LittleEndianInput input) throws EOFException {
            Wpt wpt = new Wpt();
            wpt.shortName = readString(input);
            wpt.waypointClass = input.readInt32();
            skipString(input);                     // country code
            input.skip(22);                         // subclass
            wpt.latitude = readLatLon(input);
            wpt.longitude = readLatLon(input);
            if (input.readUInt8() == 1) {
                double altitude = input.readDouble();
                if (altitude < ALTITUDE_LIMIT)
                    wpt.altitude = altitude;
            }
            wpt.notes = readString(input);
            if (input.readUInt8() == 1)
                input.readDouble();                 // proximity
            input.readInt32();                      // display
            input.readInt32();                      // color
            input.readInt32();                      // icon
            skipString(input);                      // city
            skipString(input);                      // state
            skipString(input);                      // facility
            input.skip(1);
            if (input.readUInt8() == 1)
                input.readDouble();                 // depth

            int waypointFlag = 0;
            if (version <= 2) {
                input.skip(2);
                waypointFlag = input.readUInt8();
                input.skip(waypointFlag == 0 ? 3 : 2);
                readQString(input);                 // undocumented & unused string
                String link = readQString(input);
                if (wpt.waypointClass != 0)
                    wpt.description = link;
            } else {
                skipString(input);                  // street address
                input.skip(1);
                input.readUInt32();                 // duration
                wpt.description = readQString(input);  // instruction
                int urlCount = input.readInt32();
                for (int i = urlCount; i != 0; i--)
                    readQString(input);
            }

            input.readInt16();                      // category
            if (input.readUInt8() == 1)
                input.readDouble();                 // temperature
            if (version <= 2 && waypointFlag != 0)
                input.skip(1);
            if (input.readUInt8() == 1)
                wpt.time = input.readUInt32();

            if (version >= 3) {
                if (input.readInt32() == 1) {
                    skipString(input);              // phone number
                    skipString(input);              // fax / mobile
                }
                skipString(input);                  // country
                skipString(input);                  // postal code
            }
            return wpt;
        }

        private Wpt findWaypoint(Wpt wpt, boolean exact) {
            for (Wpt candidate : waypointQueue) {
                if (!qEqualsIgnoreCase(candidate.shortName, wpt.shortName))
                    continue;
                if (!exact || isSamePosition(candidate, wpt))
                    return candidate;
            }
            return null;
        }

        // find_waypt_by_name(): case-sensitive over the waypoint list
        private Wpt findWaypointByName(String name) {
            for (Wpt candidate : waypoints) {
                if (qEquals(candidate.shortName, name))
                    return candidate;
            }
            return null;
        }

        // route_add_wpt(): numbers a route point without name across all routes
        private void addRoutePoint(Rte rte, Wpt wpt) {
            rte.points.add(wpt);
            routePointCount++;
            if (wpt.shortName == null || wpt.shortName.isEmpty())
                wpt.shortName = String.format("RPT%03d", routePointCount);
        }

        // gdb_add_route_waypt()
        private void addRouteWaypoint(Rte rte, Wpt ref) throws GdbException {
            Wpt found = findWaypoint(ref, true);
            if (found == null) {
                found = findWaypointByName(ref.shortName);
                if (found == null) {
                    addRoutePoint(rte, ref);
                    return;
                }
                if (Math.abs(distance(ref, found)) > MAXIMUM_ROUTE_POINT_DISTANCE)
                    throw new GdbException("Route point mismatch");
            }
            addRoutePoint(rte, found.copy());
        }

        private Rte readRoute(LittleEndianInput input) throws EOFException, GdbException {
            Rte rte = new Rte();
            rte.name = readString(input);
            input.skip(1);                          // display/autoname
            if (input.readUInt8() == 0)
                skipBounds(input);
            int points = input.readInt32();
            int warnings = 0;

            for (int i = 0; i < points; i++) {
                Wpt wpt = new Wpt();
                wpt.shortName = readString(input);
                wpt.waypointClass = input.readInt32();
                skipString(input);                  // country code
                input.skip(18 + 4);                 // subclass part 1-3 / unknown
                if (input.readUInt8() != 0) {
                    input.skip(8);                  // aviation data
                    if (version >= 3)
                        input.skip(8);
                }
                int unknown = input.readUInt8();
                input.skip(17);
                if (unknown != 0x00 && unknown != 0x01 && unknown != 0x03) {
                    warnings++;
                    if (warnings > 3)
                        throw new GdbException("Too many warnings in route");
                }

                int links = input.readInt32();
                for (int j = 0; j < links; j++) {
                    double latitude = readLatLon(input);
                    double longitude = readLatLon(input);
                    double altitude = input.readUInt8() == 1 ? input.readDouble() : UNKNOWN_ALTITUDE;
                    if (j == 0) {
                        wpt.latitude = latitude;
                        wpt.longitude = longitude;
                        wpt.altitude = altitude;
                    }
                }
                if (input.readUInt8() == 0)
                    skipBounds(input);              // interlink bounds

                if (links == 0) {
                    // without links all information comes from the waypoint
                    Wpt found = findWaypoint(wpt, false);
                    if (found == null)
                        throw new GdbException("Sleeping route point without coordinates");
                    wpt = found.copy();
                }

                if (version >= 2) {
                    input.skip(8);
                    if (version >= 3)
                        input.skip(2);
                }
                addRouteWaypoint(rte, wpt);
            }

            if (version <= 2) {
                readString(input);                  // url
            } else {
                readStringList(input);              // url
                input.readInt32();                  // color
                if (input.readUInt8() == 1)
                    input.skip(6 + 1 + 4 + 1 + 4 + 5 * 8 + 8);  // autoroute settings
                rte.description = readString(input);
            }
            return rte;
        }

        private void skipBounds(LittleEndianInput input) throws EOFException {
            input.skip(8);
            if (input.readUInt8() == 1)
                input.skip(8);
            input.skip(8);
            if (input.readUInt8() == 1)
                input.skip(8);
        }

        private void readStringList(LittleEndianInput input) throws EOFException {
            int count = input.readInt32();
            while (count > 0) {
                readString(input);
                count--;
            }
        }

        private Rte readTrack(LittleEndianInput input) throws EOFException {
            Rte trk = new Rte();
            trk.name = readString(input);
            input.skip(1);                          // display
            input.readInt32();                      // color
            int points = input.readInt32();
            for (int i = 0; i < points; i++) {
                Wpt wpt = new Wpt();
                wpt.latitude = readLatLon(input);
                wpt.longitude = readLatLon(input);
                if (input.readUInt8() == 1) {
                    double altitude = input.readDouble();
                    if (altitude < ALTITUDE_LIMIT)
                        wpt.altitude = altitude;
                }
                if (input.readUInt8() == 1)
                    wpt.time = input.readUInt32();
                if (input.readUInt8() == 1)
                    input.readDouble();             // depth
                if (input.readUInt8() == 1)
                    input.readDouble();             // temperature
                trk.points.add(wpt);
            }
            if (version >= 3)
                readStringList(input);              // url
            else
                readQString(input);                 // url
            return trk;
        }

        private List<GpxRoute> toRoutes() {
            List<GpxRoute> result = new ArrayList<>();
            if (!waypoints.isEmpty())
                result.add(createRoute(Waypoints, null, null, waypoints));
            for (Rte rte : routes)
                result.add(createRoute(Route, rte.name, rte.description, rte.points));
            for (Rte trk : tracks)
                if (!trk.points.isEmpty())
                    result.add(createRoute(Track, trk.name, null, trk.points));
            return result.isEmpty() ? null : result;
        }

        private GpxRoute createRoute(RouteCharacteristics characteristics, String name, String description, List<Wpt> points) {
            List<GpxPosition> positions = new ArrayList<>();
            for (Wpt wpt : points)
                positions.add(createPosition(wpt, characteristics != Track));
            return new GpxRoute(new Gpx10Format(), characteristics, asGpxText(name),
                    asDescription(asGpxText(description)), positions);
        }

        // what GPSBabel's GPX writer stores and Gpx10Format(false, true) reads back
        private GpxPosition createPosition(Wpt wpt, boolean withName) {
            String name = withName ? asGpxText(wpt.shortName) : null;
            String desc = asGpxText(wpt.notes != null && !wpt.notes.isEmpty() ? wpt.notes : wpt.description);
            Double elevation = wpt.altitude != UNKNOWN_ALTITUDE && Double.isFinite(wpt.altitude) ?
                    round(wpt.altitude, GPX_ELEVATION_DIGITS) : null;
            CompactCalendar time = wpt.time != 0 ? fromMillis(wpt.time * 1000) : null;
            return new GpxPosition(round(wpt.longitude, GPX_COORDINATE_DIGITS), round(wpt.latitude, GPX_COORDINATE_DIGITS),
                    elevation, null, time, asDescription(name, desc));
        }
    }

    private static double round(double value, int digits) {
        return new BigDecimal(value).setScale(digits, RoundingMode.HALF_EVEN).doubleValue();
    }

    /**
     * A text element of the GPX exchange file: omitted when empty, line breaks normalized by
     * the XML parser, characters XML forbids removed.
     */
    static String asGpxText(String string) {
        if (string == null || string.isEmpty())
            return null;
        String result = stripIllegalXmlCharacters(string.replace("\r\n", "\n").replace('\r', '\n'));
        return result.isEmpty() ? null : result;
    }

    private static boolean qEquals(String a, String b) {
        // QString compares a null and an empty string as equal
        return (a == null ? "" : a).equals(b == null ? "" : b);
    }

    // the exact comparison of the doubles in gdb_find_wayptq(), as C's == does it
    private static boolean isSamePosition(Wpt a, Wpt b) {
        return a.latitude <= b.latitude && a.latitude >= b.latitude &&
                a.longitude <= b.longitude && a.longitude >= b.longitude;
    }

    // QString::compare(..., Qt::CaseInsensitive)
    private static boolean qEqualsIgnoreCase(String a, String b) {
        return (a == null ? "" : a).equalsIgnoreCase(b == null ? "" : b);
    }

    // grtcirc.cc gcdist() and radtometers()
    static double distance(Wpt a, Wpt b) {
        double lat1 = Math.toRadians(a.latitude), lon1 = Math.toRadians(a.longitude);
        double lat2 = Math.toRadians(b.latitude), lon2 = Math.toRadians(b.longitude);
        double sdlat = Math.sin((lat1 - lat2) / 2.0);
        double sdlon = Math.sin((lon1 - lon2) / 2.0);
        double result = Math.sqrt(sdlat * sdlat + Math.cos(lat1) * Math.cos(lat2) * sdlon * sdlon);
        result = Math.min(1.0, Math.max(-1.0, result));
        return 2.0 * Math.asin(result) * EARTH_RADIUS;
    }

    // writing

    public void write(GpxRoute route, OutputStream target, int startIndex, int endIndex) throws IOException {
        // the GPX exchange file carries the positions as waypoints, as a route and as a track
        List<GpxPosition> positions = route.getPositions().subList(startIndex, endIndex);
        Writer writer = new Writer();
        writer.addWaypoints(positions);
        writer.addRoute(route, positions, false);
        writer.addRoute(route, positions, true);
        target.write(writer.write());
    }

    public void write(List<GpxRoute> routes, OutputStream target) throws IOException {
        Writer writer = new Writer();
        for (GpxRoute route : routes) {
            switch (route.getCharacteristics()) {
                case Waypoints -> writer.addWaypoints(route.getPositions());
                case Route -> writer.addRoute(route, route.getPositions(), false);
                case Track -> writer.addRoute(route, route.getPositions(), true);
            }
        }
        target.write(writer.write());
    }

    private static boolean isQtSpace(char c) {
        if ((c >= '\t' && c <= '\r') || c == ' ' || c == 0x85 || c == 0xA0)
            return true;
        int type = Character.getType(c);
        return type == Character.SPACE_SEPARATOR || type == Character.LINE_SEPARATOR || type == Character.PARAGRAPH_SEPARATOR;
    }

    // QString::trimmed()
    static String qtTrimmed(String string) {
        int start = 0, end = string.length();
        while (start < end && isQtSpace(string.charAt(start)))
            start++;
        while (end > start && isQtSpace(string.charAt(end - 1)))
            end--;
        return string.substring(start, end);
    }

    /**
     * A text element of the GPX exchange file as GPSBabel's GPX reader keeps it: absent elements
     * are null strings, the character data is trimmed.
     */
    static String asGpsBabelText(String string) {
        if (string == null)
            return null;
        return qtTrimmed(stripIllegalXmlCharacters(string).replace("\r\n", "\n").replace('\r', '\n'));
    }

    private static String joinDescriptions(List<String> strings) {
        if (strings == null)
            return null;
        return String.join(",\n", strings);
    }

    /** {@code GdbFormat::write()} with the waypoints, routes and tracks GPSBabel's GPX reader delivers */
    class Writer {
        private final List<Wpt> waypoints = new ArrayList<>();
        private final List<Rte> routes = new ArrayList<>();
        private final List<Rte> tracks = new ArrayList<>();
        private final List<Wpt> writtenWaypoints = new ArrayList<>();
        private int routePointCount;
        private ShortNames shortNames;

        private Wpt createWpt(GpxPosition position) {
            Wpt wpt = new Wpt();
            wpt.longitude = formatPosition(position.getLongitude()).doubleValue();
            wpt.latitude = formatPosition(position.getLatitude()).doubleValue();
            BigDecimal elevation = formatElevation(position.getElevation());
            wpt.altitude = elevation != null ? elevation.doubleValue() : UNKNOWN_ALTITUDE;
            CompactCalendar time = position.getTime();
            if (time != null) {
                long seconds = Math.floorDiv(time.getTimeInMillis(), 1000L);
                // DateTime::toTime_t() > 0 as an int
                wpt.time = seconds > 0 && seconds <= Integer.MAX_VALUE ? seconds : 0;
            }
            wpt.shortName = asGpsBabelText(asName(position.getDescription()));
            wpt.notes = asGpsBabelText(asDesc(position.getDescription()));
            return wpt;
        }

        private boolean isValid(GpxPosition position) {
            // the GPX written for GPSBabel skips positions without valid coordinates
            return formatPosition(position.getLongitude()) != null && formatPosition(position.getLatitude()) != null;
        }

        // gpx.cc <wpt> and waypt_add()
        void addWaypoints(List<GpxPosition> positions) {
            for (GpxPosition position : positions) {
                if (!isValid(position))
                    continue;
                Wpt wpt = createWpt(position);
                waypoints.add(wpt);
                if (wpt.shortName == null)
                    wpt.shortName = wpt.notes != null ? wpt.notes : String.format("WPT%03d", waypoints.size());
                if (wpt.description == null || wpt.description.isEmpty())
                    wpt.description = wpt.notes != null ? wpt.notes : wpt.shortName;
                normalize(wpt);
            }
        }

        // gpx.cc <rte>/<trk> and route_add_wpt()/track_add_wpt()
        void addRoute(GpxRoute route, List<GpxPosition> positions, boolean track) {
            Rte rte = new Rte();
            rte.name = asGpsBabelText(asRouteName(route.getName()));
            rte.description = asGpsBabelText(joinDescriptions(route.getDescription()));
            for (GpxPosition position : positions) {
                if (!isValid(position))
                    continue;
                Wpt wpt = createWpt(position);
                if (!track) {
                    routePointCount++;
                    if (wpt.shortName == null || wpt.shortName.isEmpty())
                        wpt.shortName = String.format("RPT%03d", routePointCount);
                }
                rte.points.add(wpt);
            }
            (track ? tracks : routes).add(rte);
        }

        byte[] write() throws IOException {
            LittleEndianOutput output = new LittleEndianOutput();
            writeHeader(output);

            shortNames = new ShortNames("WPT");
            for (Wpt wpt : waypoints)
                writeWaypoint(output, wpt, false);
            for (Rte rte : routes)
                for (Wpt wpt : rte.points)
                    writeWaypoint(output, wpt, true);

            shortNames = new ShortNames("Route");
            for (Rte rte : routes) {
                if (rte.points.isEmpty())
                    continue;
                String name = shortNames.mkshort(rte.name != null ? rte.name : "Route0000");
                writeRecord(output, 'R', writeRoute(rte, name, writtenWaypoints));
            }

            shortNames = new ShortNames("Track");
            for (Rte trk : tracks) {
                if (trk.points.isEmpty())
                    continue;
                String name = shortNames.mkshort(trk.name != null ? trk.name : "Track0000");
                writeRecord(output, 'T', writeTrack(trk, name));
            }

            output.writeInt32(2);   // finalize gdb with empty map segment
            writeString(output, "V");
            output.writeUInt8(1);
            return output.toByteArray();
        }

        // GdbFormat::write_waypoint_cb()
        private void writeWaypoint(LittleEndianOutput output, Wpt reference, boolean routePoint) throws IOException {
            if (reference.shortName != null)
                reference.shortName = qtTrimmed(reference.shortName);
            Wpt test = findWritten(writtenWaypoints, reference);
            if (test != null && !routePoint && !qEquals(test.notes, reference.notes))
                test = null;
            if (test != null)
                return;

            Wpt wpt = reference.copy();
            checkPosition(wpt);
            writtenWaypoints.add(wpt);

            int waypointClass = gdbWaypointClass(routePoint);
            int icon = gdbWaypointIconNumber(WRITE_VERSION);
            int display = gdbWaypointDisplayMode(waypointClass);
            // GPSBabel keeps the class in the two bit field wpt_flags.fmt_use ("trick"): the map point
            // class 8 of a waypoint that only exists in a route is written as 0
            wpt.waypointClass = waypointClass & FMT_USE_MASK;

            String name = wpt.shortName;
            if (name == null || name.isEmpty()) {
                name = wpt.notes;
                if (name == null || name.isEmpty())
                    name = wpt.description;
                if (name == null || name.isEmpty())
                    name = wpt.shortName;
            }
            name = shortNames.mkshort(name != null ? name : "");
            writeRecord(output, 'W', writeWaypointRecord(wpt, name, icon, display));
        }
    }

    // waypt_add(): wraps the position into range
    static void normalize(Wpt wpt) {
        if (wpt.latitude < -90)
            wpt.latitude += 180;
        else if (wpt.latitude > 90)
            wpt.latitude -= 180;
        if (wpt.longitude < -180)
            wpt.longitude += 360;
        else if (wpt.longitude > 180)
            wpt.longitude -= 360;
    }

    // gdb_check_waypt()
    static void checkPosition(Wpt wpt) throws IOException {
        normalize(wpt);
        if (wpt.latitude < -90 || wpt.latitude > 90.0)
            throw new IOException("Invalid latitude " + wpt.latitude + " in waypoint " + wpt.shortName);
        if (wpt.longitude < -180 || wpt.longitude > 180.0)
            throw new IOException("Invalid longitude " + wpt.longitude + " in waypoint " + wpt.shortName);
    }

    // gdb_find_wayptq(): the first written waypoint with the same name and position
    static Wpt findWritten(List<Wpt> writtenWaypoints, Wpt wpt) {
        for (Wpt candidate : writtenWaypoints) {
            if (qEqualsIgnoreCase(candidate.shortName, wpt.shortName) && isSamePosition(candidate, wpt))
                return candidate;
        }
        return null;
    }

    static void writeRecord(LittleEndianOutput output, char type, byte[] payload) {
        output.writeInt32(payload.length);
        output.writeUInt8(type);
        output.writeBytes(payload);
    }

    // gdb_write_cstr(): an empty string is a single NUL, version 3 writes UTF-8
    static void writeString(LittleEndianOutput output, String string) {
        if (string != null) {
            int nul = string.indexOf('\0');
            if (nul != -1)
                string = string.substring(0, nul);
            output.writeBytes(string.getBytes(UTF_8));
        }
        output.writeUInt8(0);
    }

    // gdb_write_dbl()
    private static void writeDouble(LittleEndianOutput output, double value, double defaultValue) {
        if (value == defaultValue) {
            output.writeUInt8(0);
        } else {
            output.writeUInt8(1);
            output.writeDouble(value);
        }
    }

    // gdb_write_time()
    private static void writeTime(LittleEndianOutput output, long time) {
        if (time > 0) {
            output.writeUInt8(1);
            output.writeInt32((int) time);
        } else {
            output.writeUInt8(0);
        }
    }

    /**
     * GPS_Math_Deg_To_Semi() of GPSBabel 1.5.4: the product is truncated towards zero
     * (master rounds); out of range values saturate.
     */
    static int degreesToSemicircles(double degrees) {
        return (int) (SEMICIRCLES_PER_DEGREE * degrees);
    }

    private static void writeLatLon(LittleEndianOutput output, double degrees) {
        output.writeInt32(degreesToSemicircles(degrees));
    }

    private static void writeSubclass(LittleEndianOutput output) {
        output.writeBytes(new byte[4]);                     // subclass part 1
        output.writeBytes(filled(12));                      // subclass part 2
        output.writeBytes(new byte[2]);                     // subclass part 3
        output.writeBytes(filled(4));                       // unknown
    }

    private static byte[] filled(int count) {
        byte[] bytes = new byte[count];
        java.util.Arrays.fill(bytes, (byte) 0xFF);
        return bytes;
    }

    // GdbFormat::write_header() with RouteConverter's watermark (D15.1)
    static void writeHeader(LittleEndianOutput output) {
        output.writeBytes(new byte[]{'M', 's', 'R', 'c'});  // signature
        output.writeInt16(0x66);                            // primary file format
        output.writeInt32(2);                               // record length
        output.writeUInt8('D');                             // record type
        output.writeInt16(WRITE_VERSION + 0x6a);            // file format

        LittleEndianOutput record = new LittleEndianOutput();
        record.writeInt16(PROGRAM_VERSION);
        writeString(record, WATERMARK);
        writeString(record, WATERMARK_DATE);
        writeString(record, WATERMARK_TIME);
        writeRecord(output, 'A', record.toByteArray());

        writeString(output, "MapSource");                   // MapSource magic
    }

    // GdbFormat::write_waypoint() for version 3 and a waypoint without Garmin data
    static byte[] writeWaypointRecord(Wpt wpt, String shortName, int icon, int display) {
        LittleEndianOutput output = new LittleEndianOutput();
        writeString(output, shortName);
        output.writeInt32(wpt.waypointClass);
        writeString(output, "");                            // country code
        writeSubclass(output);
        writeLatLon(output, wpt.latitude);
        writeLatLon(output, wpt.longitude);
        writeDouble(output, wpt.altitude, UNKNOWN_ALTITUDE);
        writeString(output, wpt.notes != null && !wpt.notes.isEmpty() ? wpt.notes : wpt.description);
        writeDouble(output, UNKNOWN_ALTITUDE, UNKNOWN_ALTITUDE);  // proximity
        output.writeInt32(display);
        output.writeInt32(GarminDefaults.GDB_WAYPOINT_COLOR);
        output.writeInt32(icon);
        writeString(output, "");                            // city
        writeString(output, "");                            // state
        writeString(output, "");                            // facility
        output.writeUInt8(0);                               // unknown
        writeDouble(output, UNKNOWN_ALTITUDE, UNKNOWN_ALTITUDE);  // depth

        writeString(output, "");                            // street address
        output.writeBytes(new byte[5]);                     // instruction dependent
        // GDB has no description field: GPSBabel misuses the instruction
        String instruction = wpt.description;
        if (qEquals(wpt.description, wpt.shortName))
            instruction = null;
        if (qEquals("", wpt.notes))
            instruction = null;
        writeString(output, instruction);
        output.writeInt32(0);                               // urls

        output.writeInt16(0);                               // category
        writeDouble(output, 0, 0);                          // temperature
        writeTime(output, wpt.time);
        output.writeInt32(0);                               // phone number
        writeString(output, "");                            // country
        writeString(output, "");                            // postal code
        return output.toByteArray();
    }

    /** {@code bounds} with waypt_init_bounds()/waypt_add_to_bounds() */
    static class Bounds {
        double maxLatitude = -9999, maxLongitude = -9999, minLatitude = 9999, minLongitude = 9999;
        double maxAltitude = UNKNOWN_ALTITUDE, minAltitude = -UNKNOWN_ALTITUDE;

        void add(Wpt wpt) {
            if (wpt.latitude > maxLatitude)
                maxLatitude = wpt.latitude;
            if (wpt.longitude > maxLongitude)
                maxLongitude = wpt.longitude;
            if (wpt.latitude < minLatitude)
                minLatitude = wpt.latitude;
            if (wpt.longitude < minLongitude)
                minLongitude = wpt.longitude;
            if (wpt.altitude != UNKNOWN_ALTITUDE) {
                if (wpt.altitude < minAltitude)
                    minAltitude = wpt.altitude;
                if (wpt.altitude > maxAltitude)
                    maxAltitude = wpt.altitude;
            }
        }

        // route_write_bounds()
        void write(LittleEndianOutput output) {
            if (maxLatitude > -9999) {
                output.writeUInt8(0);
                writeLatLon(output, maxLatitude);
                writeLatLon(output, maxLongitude);
                writeDouble(output, maxAltitude, UNKNOWN_ALTITUDE);
                writeLatLon(output, minLatitude);
                writeLatLon(output, minLongitude);
                writeDouble(output, minAltitude, -UNKNOWN_ALTITUDE);
            } else {
                output.writeUInt8(1);
            }
        }
    }

    // GdbFormat::write_route() for version 3
    static byte[] writeRoute(Rte rte, String name, List<Wpt> writtenWaypoints) throws IOException {
        LittleEndianOutput output = new LittleEndianOutput();
        writeString(output, name);
        output.writeUInt8(0);                               // display/autoname

        Bounds bounds = new Bounds();
        for (Wpt wpt : rte.points) {
            checkPosition(wpt);
            bounds.add(wpt);
        }
        bounds.write(output);

        int points = rte.points.size();
        output.writeInt32(points);
        for (int index = 0; index < points; index++) {
            boolean last = index == points - 1;
            Wpt next = last ? null : rte.points.get(index + 1);
            if (!last)
                checkPosition(next);

            Wpt wpt = findWritten(writtenWaypoints, rte.points.get(index));
            if (wpt == null)
                throw new IOException("Sorry, that should never happen");

            // GPSBabel 1.5.4 writes the waypoint's own name, not the one made unique by mkshort()
            writeString(output, wpt.shortName);
            output.writeInt32(wpt.waypointClass);
            writeString(output, "");                        // country
            writeSubclass(output);
            output.writeUInt8(0);                           // unknown value or string
            output.writeUInt8(3);                           // unknown 18 bytes starting with 0x03
            output.writeBytes(new byte[3]);
            output.writeBytes(filled(4));
            output.writeBytes(new byte[10]);

            if (last) {
                output.writeInt32(0);                       // no more steps
                output.writeUInt8(1);                       // skip bounds
            } else {
                output.writeInt32(2);                       // two interstep links
                writeLatLon(output, wpt.latitude);
                writeLatLon(output, wpt.longitude);
                writeDouble(output, wpt.altitude, UNKNOWN_ALTITUDE);
                writeLatLon(output, next.latitude);
                writeLatLon(output, next.longitude);
                writeDouble(output, next.altitude, UNKNOWN_ALTITUDE);

                Bounds linkBounds = new Bounds();
                linkBounds.add(wpt);
                linkBounds.add(next);
                linkBounds.write(output);
            }
            output.writeBytes(filled(8));
            output.writeBytes(new byte[2]);
        }

        output.writeInt32(0);                               // url list
        output.writeInt32(gdbRouteColorIndex(UNKNOWN_COLOR));
        output.writeUInt8(0);
        writeString(output, rte.description);
        return output.toByteArray();
    }

    // GdbFormat::write_track() for version 3
    static byte[] writeTrack(Rte trk, String name) {
        LittleEndianOutput output = new LittleEndianOutput();
        writeString(output, name);
        output.writeUInt8(0);
        output.writeInt32(gdbTrackColorIndex(UNKNOWN_COLOR));
        output.writeInt32(trk.points.size());
        for (Wpt wpt : trk.points) {
            writeLatLon(output, wpt.latitude);
            writeLatLon(output, wpt.longitude);
            writeDouble(output, wpt.altitude, UNKNOWN_ALTITUDE);
            writeTime(output, wpt.time);
            writeDouble(output, UNKNOWN_ALTITUDE, UNKNOWN_ALTITUDE);  // depth
            writeDouble(output, -99999, -99999);                       // temperature
        }
        output.writeInt32(0);                               // url list
        return output.toByteArray();
    }

    /**
     * GPSBabel 1.5.4's mkshort() with the settings of {@code GdbFormat::reset_short_handle()}: target
     * length 1024, bad characters CR, LF and TAB, mixed case, whitespace allowed, unique names, UTF-8.
     */
    static class ShortNames {
        private static final String[][] REPLACEMENTS = {
                {"zero", "0"}, {"one", "1"}, {"two", "2"}, {"three", "3"}, {"four", "4"},
                {"five", "5"}, {"six", "6"}, {"seven", "7"}, {"eight", "8"}, {"nine", "9"}
        };
        private static final String BAD_CHARACTERS = "\r\n\t";

        private final String defaultName;
        private final List<String> names = new ArrayList<>();
        private final List<int[]> conflictCounters = new ArrayList<>();

        ShortNames(String defaultName) {
            this.defaultName = defaultName;
        }

        // isspace() in the C locale
        private static boolean isSpace(char c) {
            return c == ' ' || (c >= '\t' && c <= '\r');
        }

        private static int utf8Length(String string) {
            return string.getBytes(UTF_8).length;
        }

        // case_ignore_strcmp(): ASCII case folding only
        private static boolean equalsIgnoreAsciiCase(String a, String b) {
            if (a.length() != b.length())
                return false;
            for (int i = 0; i < a.length(); i++) {
                char ca = a.charAt(i), cb = b.charAt(i);
                if (ca == cb)
                    continue;
                if (ca < 128 && cb < 128 && Character.toUpperCase(ca) == Character.toUpperCase(cb))
                    continue;
                return false;
            }
            return true;
        }

        static String replaceConstants(String string) {
            int length = string.length();
            for (String[] replacement : REPLACEMENTS) {
                int replacementLength = replacement[0].length();
                if (length - replacementLength > 1 &&
                        string.substring(length - replacementLength).toLowerCase(Locale.ROOT).equals(replacement[0]) &&
                        string.charAt(length - replacementLength - 1) == ' ')
                    return string.substring(0, length - replacementLength) + replacement[1];
            }
            return string;
        }

        String mkshort(String input) {
            String string = input;

            // whack leading "[Tt]he"
            if (utf8Length(string) > NAME_LENGTH + 4 && (string.startsWith("The ") || string.startsWith("the ")))
                string = string.substring(4);

            // eliminate leading whitespace
            int start = 0;
            while (start < string.length() && isSpace(string.charAt(start)))
                start++;
            string = string.substring(start);

            string = replaceConstants(string);

            // eliminate characters on the blacklist
            StringBuilder builder = new StringBuilder(string.length());
            for (int i = 0; i < string.length(); i++) {
                char c = string.charAt(i);
                if (BAD_CHARACTERS.indexOf(c) == -1)
                    builder.append(c);
            }
            string = builder.toString();

            // truncate to the target length in UTF-8 characters
            if (string.codePointCount(0, string.length()) > NAME_LENGTH)
                string = string.substring(0, string.offsetByCodePoints(0, NAME_LENGTH));

            if (string.isEmpty())
                string = defaultName;
            return addToList(string);
        }

        // mkshort_add_to_list()
        private String addToList(String name) {
            int index;
            while ((index = indexOf(name)) != -1) {
                int counter = ++conflictCounters.get(index)[0];
                String suffix = "." + counter;
                byte[] bytes = name.getBytes(UTF_8);
                if (bytes.length + suffix.length() < NAME_LENGTH) {
                    name = name + suffix;
                } else {
                    byte[] replaced = java.util.Arrays.copyOf(bytes, bytes.length);
                    byte[] suffixBytes = suffix.getBytes(UTF_8);
                    System.arraycopy(suffixBytes, 0, replaced, bytes.length - suffixBytes.length, suffixBytes.length);
                    name = new String(replaced, UTF_8);
                }
            }
            names.add(name);
            conflictCounters.add(new int[1]);
            return name;
        }

        private int indexOf(String name) {
            for (int i = 0; i < names.size(); i++)
                if (equalsIgnoreAsciiCase(names.get(i), name))
                    return i;
            return -1;
        }
    }
}
