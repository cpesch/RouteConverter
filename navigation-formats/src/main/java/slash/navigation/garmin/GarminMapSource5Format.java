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

    Translated to Java from GPSBabel mapsource.cc (gpsbabel_1_6_0) for RouteConverter, 2026; modified.
    Copyright (C) 2002 Robert Lipe, robertlipe+source@gpsbabel.org
*/

package slash.navigation.garmin;

import slash.common.type.CompactCalendar;
import slash.navigation.base.GpxBackedNavigationFormat;
import slash.navigation.base.MultipleRoutesFormat;
import slash.navigation.base.RouteCharacteristics;
import slash.navigation.base.UnreadableFileException;
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
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import static java.nio.charset.CodingErrorAction.REPORT;
import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static java.nio.charset.StandardCharsets.US_ASCII;
import static java.nio.charset.StandardCharsets.UTF_8;
import static slash.common.type.CompactCalendar.fromMillis;
import static slash.navigation.base.RouteCharacteristics.Route;
import static slash.navigation.base.RouteCharacteristics.Track;
import static slash.navigation.base.RouteCharacteristics.Waypoints;
import static slash.navigation.garmin.GarminDefaults.MAPSOURCE_DEFAULT_CLASS;
import static slash.navigation.garmin.GarminDefaults.MAPSOURCE_TRACK_COLOR;
import static slash.navigation.garmin.GarminDefaults.MAPSOURCE_TRACK_DISPLAY;
import static slash.navigation.garmin.GarminDefaults.MAPSOURCE_WAYPOINT_COLOR;
import static slash.navigation.garmin.GarminDefaults.MAPSOURCE_WAYPOINT_DISPLAY;
import static slash.navigation.garmin.GarminDefaults.mapSourceWaypointClass;
import static slash.navigation.garmin.GarminDefaults.mapSourceWaypointIconNumber;
import static slash.navigation.garmin.GarminMapSource6Format.asGpxText;
import static slash.navigation.garmin.GarminMapSource6Format.qtTrimmed;
import static slash.navigation.gpsbabel.GpsBabelMath.degreesToSemicircles;
import static slash.navigation.gpsbabel.GpsBabelMath.semicirclesToDegrees;

/**
 * Reads and writes Garmin MapSource 5.x (.mps) files, the MapSource file format GPSBabel calls
 * versions 3, 4 and 5.
 * <p>
 * GPSBabel numbers the versions after the MapSource release that wrote them; the file carries
 * the format version in the second byte of its {@code D} record (Oppmann, <i>Garmin MapSource (MPS),
 * GPS Database (GDB) and GFI File Format</i>: format 1.0 to 1.5): {@code 'd'} (MapSource 3.x) is
 * version 3, {@code 'e'} to {@code 'h'} (MapSource 4.x) version 4 and {@code 'i'} (MapSource 5.x
 * and later) version 5. Other versions are not read. The records are read with Oppmann's layouts, which
 * also cover the formats 1.1 and 1.2 and the waypoint fields GPSBabel's fixed offsets miss.
 * <p>
 * Reading delivers what GPSBabel 1.5.4 delivered through its GPX 1.0 exchange file: the user
 * waypoints, the routes with the waypoints they refer to and the tracks. Writing produces version 5
 * files the way GPSBabel's {@code mapsource} writer did without options, except that every waypoint
 * record gets a unique name (D21). Text is Windows-1252 like MapSource writes it; on reading, text that
 * is valid UTF-8 (as GPSBabel wrote it) is kept as UTF-8.
 *
 * @author Christian Pesch
 */

public class GarminMapSource5Format extends GpxBackedNavigationFormat implements MultipleRoutesFormat<GpxRoute> {
    static final byte[] SIGNATURE = {'M', 's', 'R', 'c', 'd', 0};
    static final int WRITE_VERSION = 5;
    static final double UNKNOWN_ALTITUDE = -99999999.0;
    static final Charset WINDOWS_1252 = Charset.forName("windows-1252");
    private static final Logger log = Logger.getLogger(GarminMapSource5Format.class.getName());
    private static final int RECORD_HEADER_LENGTH = 5;
    private static final int GPX_COORDINATE_DIGITS = 9;
    private static final int GPX_ELEVATION_DIGITS = 6;

    public String getExtension() {
        return ".mps";
    }

    public String getName() {
        return "Garmin MapSource 5.x (*" + getExtension() + ")";
    }

    public boolean isSupportsMultipleRoutes() {
        return true;
    }

    public boolean isWritingRouteCharacteristics() {
        return true;
    }

    /** GPSBabel's Waypoint, reduced to the fields mapsource.cc and the GPX exchange use */
    static class Wpt {
        String shortName;
        String description;
        String notes;
        double latitude;
        double longitude;
        double altitude = UNKNOWN_ALTITUDE;
        // seconds since 1970, 0 = none
        long time;
        int waypointClass;
        // the semicircles of the file: the key of a route point's waypoint
        int latitudeSemicircles;
        int longitudeSemicircles;
        // the unique name the waypoint record carries (D21)
        String writtenName;
        boolean hidden;

        Wpt copy() {
            Wpt result = new Wpt();
            result.shortName = shortName;
            result.description = description;
            result.notes = notes;
            result.latitude = latitude;
            result.longitude = longitude;
            result.altitude = altitude;
            result.time = time;
            result.waypointClass = waypointClass;
            result.latitudeSemicircles = latitudeSemicircles;
            result.longitudeSemicircles = longitudeSemicircles;
            return result;
        }
    }

    static class Rte {
        String name;
        final List<Wpt> points = new ArrayList<>();
        int declaredPoints = -1;
        boolean pointsComplete;
    }

    /** the key of a waypoint by its exact name and the semicircles of its position */
    record NameAndPosition(String name, int latitude, int longitude) {
        static NameAndPosition of(Wpt wpt) {
            return new NameAndPosition(wpt.shortName == null ? "" : wpt.shortName, wpt.latitudeSemicircles, wpt.longitudeSemicircles);
        }
    }

    /**
     * The text of a record: MapSource writes Windows-1252, GPSBabel wrote UTF-8. Bytes that are valid
     * UTF-8 are read as UTF-8 like GPSBabel 1.5.4 did, everything else as Windows-1252, where GPSBabel
     * 1.5.4 delivered replacement characters.
     */
    static String decode(byte[] bytes) {
        try {
            return UTF_8.newDecoder().onMalformedInput(REPORT).onUnmappableCharacter(REPORT)
                    .decode(ByteBuffer.wrap(bytes)).toString();
        } catch (CharacterCodingException e) {
            return new String(bytes, WINDOWS_1252);
        }
    }

    // reading

    protected List<GpxRoute> readRoutes(InputStream source) throws IOException {
        byte[] bytes = source.readAllBytes();
        // detection guard: a file that is not a MapSource file is never claimed
        if (!isMps(bytes))
            return null;
        Reader reader = new Reader(bytes);
        List<GpxRoute> routes = reader.read();
        // a recognized file that lost everything to damage is reported, an intact empty file is declined (D19)
        if (routes == null && reader.problem != null)
            throw new UnreadableFileException("MapSource file is damaged: " + reader.problem);
        return routes;
    }

    static boolean isMps(byte[] bytes) {
        if (bytes.length < SIGNATURE.length)
            return false;
        for (int i = 0; i < SIGNATURE.length; i++)
            if (bytes[i] != SIGNATURE[i])
                return false;
        return true;
    }

    /**
     * {@code mps_read()} with the record layouts of Herbert Oppmann's description for the MPS file
     * formats 1.0 to 1.5, which MapSource 3.x to 6.x wrote ({@code 'd'} to {@code 'i'} in the {@code D} record).
     * <p>
     * Every record is read on its own from its declared length: a record that cannot be read is skipped,
     * and a file that ends early keeps the records read before (D19).
     */
    class Reader {
        private final byte[] bytes;
        // the format version: 100 for 1.0 ('d') to 105 for 1.5 ('i')
        private int version;
        // the first problem that cost a record, null for an intact file
        String problem;
        private final List<Wpt> waypoints = new ArrayList<>();
        // the user waypoints and the waypoints only used by routes, by name and by name and position
        private final Map<String, Wpt> waypointsByName = new HashMap<>();
        private final Map<String, Wpt> hiddenByName = new HashMap<>();
        private final Map<NameAndPosition, Wpt> byNameAndPosition = new HashMap<>();
        private final List<Rte> routes = new ArrayList<>();
        private final List<Rte> tracks = new ArrayList<>();
        // rte_waypts in route.cc: numbers the route points without name across all routes
        private int routePointCount;

        Reader(byte[] bytes) {
            this.bytes = bytes;
        }

        int getVersion() {
            return version;
        }

        private void damaged(String message) {
            log.warning("MapSource file is damaged: " + message);
            if (problem == null)
                problem = message;
        }

        void readHeader(LittleEndianInput input) throws UnreadableFileException {
            try {
                input.skip(SIGNATURE.length);
                int length = input.readInt32();
                if (length < 1 || input.readUInt8() != 'D')
                    throw new UnreadableFileException("MapSource file without format version");
                byte[] format = input.readBytes(length);
                int flag = format[0] & 0xFF;
                if (flag < 'd' || flag > 'i')
                    throw new UnreadableFileException(String.format("Unsupported MapSource file format version 0x%02x", flag));
                version = 100 + flag - 'd';
                // the program signature
                length = input.readInt32();
                input.readUInt8();
                if (length < 0 || length > input.remaining())
                    throw new UnreadableFileException("MapSource file with an invalid program record");
                input.skip(length);
            } catch (EOFException e) {
                throw new UnreadableFileException("MapSource file ends in its header", e);
            }
        }

        List<GpxRoute> read() throws UnreadableFileException {
            LittleEndianInput input = new LittleEndianInput(bytes);
            readHeader(input);

            // a file without the map set record ends after its last record
            while (input.remaining() > 0) {
                if (input.remaining() < RECORD_HEADER_LENGTH) {
                    damaged("it ends in a record header");
                    break;
                }
                int length, type;
                try {
                    length = input.readInt32();
                    type = input.readUInt8();
                } catch (EOFException e) {
                    throw new IllegalStateException(e);
                }
                // the map set: the last record of the file
                if (type == 'V')
                    break;
                if (length < 0) {
                    damaged("a record has the negative length " + length);
                    break;
                }
                boolean truncated = length > input.remaining();
                if (truncated)
                    damaged("the file ends in a " + (char) type + " record");
                byte[] record;
                try {
                    record = input.readBytes(truncated ? input.remaining() : length);
                } catch (EOFException e) {
                    throw new IllegalStateException(e);
                }
                readRecord((char) type, new LittleEndianInput(record), truncated);
                if (truncated)
                    break;
            }
            return toRoutes();
        }

        private void readRecord(char type, LittleEndianInput record, boolean truncated) {
            switch (type) {
                case 'W' -> {
                    Wpt wpt = readWaypoint(record);
                    if (wpt == null)
                        return;
                    if (record.remaining() > 0)
                        log.warning("Ignored " + record.remaining() + " bytes behind waypoint " + wpt.shortName);
                    addWaypoint(wpt);
                }
                case 'R', 'T' -> {
                    Rte rte = new Rte();
                    try {
                        if (type == 'R')
                            readRoute(record, rte);
                        else
                            readTrack(record, rte);
                    } catch (EOFException e) {
                        // the points before a cut keep their values; a record that is longer than its
                        // points is not understood: its points may be garbage
                        if (!rte.pointsComplete && !truncated) {
                            damaged("unreadable " + type + " record: " + e.getMessage());
                            return;
                        }
                    }
                    (type == 'R' ? routes : tracks).add(rte);
                }
                // map segments and unknown records
                default -> {
                }
            }
        }

        // waypt_add() for a user waypoint: wraps the position into range and fills a missing description;
        // a waypoint of another class is only used by routes
        private void addWaypoint(Wpt wpt) {
            if (wpt.waypointClass == MAPSOURCE_DEFAULT_CLASS) {
                normalize(wpt);
                waypoints.add(wpt);
                if (wpt.description == null || wpt.description.isEmpty())
                    wpt.description = wpt.notes != null ? wpt.notes : wpt.shortName;
                waypointsByName.putIfAbsent(key(wpt.shortName), wpt);
            } else
                hiddenByName.putIfAbsent(key(wpt.shortName), wpt);
            // the semicircles of the file are not wrapped
            byNameAndPosition.putIfAbsent(NameAndPosition.of(wpt), wpt);
        }

        private String readText(LittleEndianInput input) throws EOFException {
            return decode(input.readCString(ISO_8859_1).getBytes(ISO_8859_1));
        }

        private void skipString(LittleEndianInput input) throws EOFException {
            input.readCString(ISO_8859_1);
        }

        // fstring: a flag and the string if the flag is set
        private void skipFString(LittleEndianInput input) throws EOFException {
            if (input.readUInt8() != 0)
                skipString(input);
        }

        // fdouble: a flag and the value, which takes its 8 bytes in every MPS format
        private double readFDouble(LittleEndianInput input) throws EOFException {
            int flag = input.readUInt8();
            double value = input.readDouble();
            return flag == 1 ? value : UNKNOWN_ALTITUDE;
        }

        // fint: a flag and the value, which takes its 4 bytes in every MPS format
        private int readFInt(LittleEndianInput input) throws EOFException {
            int flag = input.readUInt8();
            int value = input.readInt32();
            return flag == 1 ? value : 0;
        }

        // Oppmann: 4 + 4 + 8 bytes, the second subclass is a byte in 1.0 and a short later, the third one is new in 1.3
        private void skipSubclass(LittleEndianInput input) throws EOFException {
            input.skip(4 + 4 + 8 + (version == 100 ? 1 : 2) + (version > 102 ? 4 : 0));
        }

        private void readPosition(LittleEndianInput input, Wpt wpt) throws EOFException {
            wpt.latitudeSemicircles = input.readInt32();
            wpt.longitudeSemicircles = input.readInt32();
            wpt.latitude = semicirclesToDegrees(wpt.latitudeSemicircles);
            wpt.longitude = semicirclesToDegrees(wpt.longitudeSemicircles);
            wpt.altitude = readFDouble(input);
        }

        private Wpt readWaypoint(LittleEndianInput input) {
            Wpt wpt = new Wpt();
            try {
                wpt.shortName = readText(input);
                wpt.waypointClass = input.readInt32();
                skipString(input);                          // country
                skipSubclass(input);
                readPosition(input, wpt);
                wpt.description = readText(input);          // comment
            } catch (EOFException e) {
                damaged("unreadable W record: " + e.getMessage());
                return null;
            }

            try {
                readFDouble(input);                         // proximity
                input.readInt32();                          // display
                input.readInt32();                          // colour
                input.readInt32();                          // symbol
                skipString(input);                          // city
                skipString(input);                          // state
                skipString(input);                          // facility
                input.skip(1);                              // unknown
                readFDouble(input);                         // depth
                skipString(input);                          // street address
                skipFString(input);                         // unknown 1
                if (version >= 102) {
                    input.readInt32();                      // predicted leg time
                    wpt.notes = readText(input);            // directions: GPSBabel's notes
                }
            } catch (EOFException e) {
                // keep name, position, altitude and comment of a record the reader does not fully understand
                damaged("read only name and position of waypoint " + wpt.shortName + ": " + e.getMessage());
            }
            return wpt;
        }

        // a route point refers to the waypoint with its name and position, else to the first one with its name
        private Wpt findRoutePoint(Wpt point) {
            Wpt found = byNameAndPosition.get(NameAndPosition.of(point));
            if (found == null)
                found = waypointsByName.get(key(point.shortName));
            if (found == null)
                found = hiddenByName.get(key(point.shortName));
            if (found != null)
                return found.copy();
            // a route point without waypoint takes its position from the route link
            return point;
        }

        // route_add_wpt()
        private void addRoutePoint(Rte rte, Wpt wpt) {
            rte.points.add(wpt);
            routePointCount++;
            if (wpt.shortName == null || wpt.shortName.isEmpty())
                wpt.shortName = String.format("RPT%03d", routePointCount);
        }

        // the position read last: a route point without links of its own lies at the end of the previous link
        private final Wpt last = new Wpt();

        private void readRoute(LittleEndianInput input, Rte rte) throws EOFException {
            rte.name = readText(input);
            input.skip(1);                                  // autoname
            if (input.readUInt8() == 0) {
                readPosition(input, last);                  // maximum of the route
                readPosition(input, last);                  // minimum of the route
            } else
                copyPosition(new Wpt(), last);

            int points = input.readInt32();
            rte.declaredPoints = points;
            for (int i = 0; i < points; i++) {
                Wpt point = new Wpt();
                point.shortName = readText(input);
                input.readInt32();                          // class
                skipString(input);                          // country
                skipSubclass(input);
                skipFString(input);                         // unknown 1
                input.skip(4 + 4 + 4 + (version == 100 ? 1 : 2) + 4);  // unknown 2 to 6

                int links = input.readInt32();
                if (links < 0 || links > input.remaining() / 17)
                    throw new EOFException("Cannot read " + links + " links");
                for (int link = 0; link < links; link++) {
                    Wpt step = link == 0 ? point : new Wpt();
                    readPosition(input, step);
                    if (link > 0 || links == 1)
                        copyPosition(step, last);
                }
                if (links == 0)
                    copyPosition(last, point);
                skipBounds(input);                          // link bounds
                addRoutePoint(rte, findRoutePoint(point));
            }
            rte.pointsComplete = true;
        }

        private void copyPosition(Wpt from, Wpt to) {
            to.latitudeSemicircles = from.latitudeSemicircles;
            to.longitudeSemicircles = from.longitudeSemicircles;
            to.latitude = from.latitude;
            to.longitude = from.longitude;
            to.altitude = from.altitude;
        }

        // a flag and, if it is clear, the maximum and minimum position and altitude
        private void skipBounds(LittleEndianInput input) throws EOFException {
            if (input.readUInt8() == 0)
                input.skip(2 * (4 + 4 + 9));
        }

        private void readTrack(LittleEndianInput input, Rte trk) throws EOFException {
            trk.name = readText(input);
            input.skip(1);                                  // display
            input.readInt32();                              // colour
            int points = input.readInt32();
            trk.declaredPoints = points;
            for (int i = 0; i < points; i++) {
                Wpt wpt = new Wpt();
                readPosition(input, wpt);
                // a signed time; a point without time has none, a time before 1970 is implausible (D23)
                int time = readFInt(input);
                wpt.time = Math.max(time, 0);
                readFDouble(input);                         // depth
                trk.points.add(wpt);
            }
            trk.pointsComplete = true;
        }

        private List<GpxRoute> toRoutes() {
            List<GpxRoute> result = new ArrayList<>();
            if (!waypoints.isEmpty())
                result.add(createRoute(Waypoints, null, waypoints));
            for (Rte rte : routes)
                // a route that lost all its points to the salvage is gone
                if (!rte.points.isEmpty() || (rte.pointsComplete && rte.declaredPoints == 0))
                    result.add(createRoute(Route, rte.name, rte.points));
            for (Rte trk : tracks)
                if (!trk.points.isEmpty())
                    result.add(createRoute(Track, trk.name, trk.points));
            return result.isEmpty() ? null : result;
        }

        private GpxRoute createRoute(RouteCharacteristics characteristics, String name, List<Wpt> points) {
            List<GpxPosition> positions = new ArrayList<>();
            for (Wpt wpt : points)
                positions.add(createPosition(wpt));
            return new GpxRoute(new Gpx10Format(), characteristics, asGpxText(name), null, positions);
        }

        // what GPSBabel's GPX writer stores and Gpx10Format(false, true) reads back
        private GpxPosition createPosition(Wpt wpt) {
            String name = asGpxText(wpt.shortName);
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

    // find_waypt_by_name(): a null and an empty name are equal like QString's operator==
    private static String key(String name) {
        return name == null ? "" : name;
    }

    // waypt_add(): wraps the position into range once
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

    /**
     * A text element of the GPX exchange file as GPSBabel's GPX reader keeps it: absent elements
     * are null strings, the character data is trimmed. Line breaks stay as they are, since the
     * GPX writer escapes carriage returns.
     */
    static String asGpsBabelText(String string) {
        if (string == null)
            return null;
        return qtTrimmed(stripIllegalXmlCharacters(string));
    }

    /**
     * The seconds since 1970 of a time to write: an MPS time is a signed int, a time before 1970 or
     * after 2038-01-19 is written without time (0), as the GDB writer does.
     */
    static long toTimeT(CompactCalendar time) {
        if (time == null)
            return 0;
        long seconds = Math.floorDiv(time.getTimeInMillis(), 1000L);
        return seconds > 0 && seconds <= Integer.MAX_VALUE ? seconds : 0;
    }

    /**
     * Names made unique the way GPSBabel's {@code mkshort()} does it: a name that is taken, ignoring
     * the case of ASCII letters, gets the suffix {@code .1}, {@code .2} and so on (D21).
     */
    static class UniqueNames {
        private final Map<String, int[]> conflictCounters = new HashMap<>();

        private static String fold(String name) {
            return GarminMapSource6Format.ShortNames.foldAsciiCase(name);
        }

        String unique(String name) {
            String result = key(name);
            int[] conflicts;
            String base = result;
            while ((conflicts = conflictCounters.get(fold(result))) != null)
                result = base + "." + ++conflicts[0];
            conflictCounters.put(fold(result), new int[1]);
            return result;
        }
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

    /** {@code mps_write()} with the waypoints, routes and tracks GPSBabel's GPX reader delivers */
    class Writer {
        private final List<Wpt> waypoints = new ArrayList<>();
        private final List<Rte> routes = new ArrayList<>();
        private final List<Rte> tracks = new ArrayList<>();
        private int routePointCount;

        private Wpt createWpt(GpxPosition position) throws IOException {
            Wpt wpt = new Wpt();
            // the exact values: neither GPSBabel's GPX exchange nor the user's display precision rounds them (D18)
            wpt.longitude = position.getLongitude();
            wpt.latitude = position.getLatitude();
            Double elevation = position.getElevation();
            wpt.altitude = elevation != null && Double.isFinite(elevation) ? elevation : UNKNOWN_ALTITUDE;
            wpt.time = toTimeT(position.getTime());
            wpt.shortName = asGpsBabelText(asName(position.getDescription()));
            wpt.notes = asGpsBabelText(asDesc(position.getDescription()));
            normalize(wpt);
            if (wpt.latitude < -90 || wpt.latitude > 90.0)
                throw new IOException("Invalid latitude " + position.getLatitude() + " in waypoint " + wpt.shortName);
            if (wpt.longitude < -180 || wpt.longitude > 180.0)
                throw new IOException("Invalid longitude " + position.getLongitude() + " in waypoint " + wpt.shortName);
            wpt.latitudeSemicircles = degreesToSemicircles(wpt.latitude);
            wpt.longitudeSemicircles = degreesToSemicircles(wpt.longitude);
            return wpt;
        }

        private boolean isValid(GpxPosition position) {
            // the GPX written for GPSBabel skips positions without valid coordinates
            Double longitude = position.getLongitude(), latitude = position.getLatitude();
            return longitude != null && latitude != null && Double.isFinite(longitude) && Double.isFinite(latitude);
        }

        // gpx.cc <wpt> and waypt_add()
        void addWaypoints(List<GpxPosition> positions) throws IOException {
            for (GpxPosition position : positions) {
                if (!isValid(position))
                    continue;
                Wpt wpt = createWpt(position);
                waypoints.add(wpt);
                if (wpt.shortName == null)
                    wpt.shortName = wpt.notes != null ? wpt.notes : String.format("WPT%03d", waypoints.size());
                if (wpt.description == null || wpt.description.isEmpty())
                    wpt.description = wpt.notes != null ? wpt.notes : wpt.shortName;
            }
        }

        // gpx.cc <rte>/<trk> and route_add_wpt()/track_add_wpt()
        void addRoute(GpxRoute route, List<GpxPosition> positions, boolean track) throws IOException {
            Rte rte = new Rte();
            rte.name = asGpsBabelText(asRouteName(route.getName()));
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

        byte[] write() {
            LittleEndianOutput output = new LittleEndianOutput();
            writeHeader(output);

            // one waypoint record per name and position, each with a unique name (D21): GPSBabel wrote one
            // per name and moved route points onto another waypoint of the same name
            UniqueNames names = new UniqueNames();
            Map<NameAndPosition, Wpt> written = new HashMap<>();
            for (Wpt wpt : waypoints) {
                if (written.containsKey(NameAndPosition.of(wpt)))
                    continue;
                wpt.writtenName = names.unique(wpt.shortName);
                writeRecord(output, 'W', writeWaypoint(wpt, false));
                written.put(NameAndPosition.of(wpt), wpt);
            }

            // every route point needs a waypoint, the ones that are no user waypoint are written hidden
            for (Rte rte : routes) {
                for (Wpt wpt : rte.points) {
                    Wpt found = written.get(NameAndPosition.of(wpt));
                    if (found == null) {
                        wpt.writtenName = names.unique(wpt.shortName);
                        wpt.hidden = true;
                        writeRecord(output, 'W', writeWaypoint(wpt, true));
                        written.put(NameAndPosition.of(wpt), wpt);
                    } else {
                        wpt.writtenName = found.writtenName;
                        wpt.hidden = found.hidden;
                    }
                }
            }

            UniqueNames routeNames = new UniqueNames();
            for (Rte rte : routes) {
                // GPSBabel writes a route without points with a broken record length
                if (rte.points.isEmpty())
                    continue;
                writeRecord(output, 'R', writeRoute(rte, routeNames.unique(nameOf(rte, "Route"))));
            }

            UniqueNames trackNames = new UniqueNames();
            for (Rte trk : tracks)
                writeRecord(output, 'T', writeTrack(trk, trackNames.unique(nameOf(trk, "Track"))));

            // mps_mapsetname_w(): an empty map set name with autoname
            output.writeInt32(2);
            output.writeUInt8('V');
            output.writeUInt8(0);
            output.writeUInt8(1);
            return output.toByteArray();
        }
    }

    // the record length counts the bytes behind the record type
    static void writeRecord(LittleEndianOutput output, char type, byte[] payload) {
        output.writeInt32(payload.length);
        output.writeUInt8(type);
        output.writeBytes(payload);
    }

    // gbfputs() and the NUL behind it, as Windows-1252
    static void writeString(LittleEndianOutput output, String string) {
        if (string != null) {
            int nul = string.indexOf('\0');
            if (nul != -1)
                string = string.substring(0, nul);
            output.writeBytes(string.getBytes(WINDOWS_1252));
        }
        output.writeUInt8(0);
    }

    private static void writeAltitude(LittleEndianOutput output, double altitude, double unknown) {
        if (altitude == unknown) {
            output.writeBytes(new byte[9]);
        } else {
            output.writeUInt8(1);
            output.writeDouble(altitude);
        }
    }

    private static void writeLatLon(LittleEndianOutput output, double degrees) {
        output.writeInt32(degreesToSemicircles(degrees));
    }

    private static byte[] filled(int count) {
        byte[] bytes = new byte[count];
        Arrays.fill(bytes, (byte) 0xFF);
        return bytes;
    }

    // the subclass of a waypoint or route point in versions 4 and 5
    private static void writeSubclass(LittleEndianOutput output) {
        output.writeBytes(new byte[4]);                     // subclass part 1
        output.writeBytes(filled(12));                      // subclass part 2
        output.writeBytes(new byte[2]);                     // subclass part 3
        output.writeBytes(filled(4));                       // unknown
    }

    // mps_fileHeader_w() for version 5
    static void writeHeader(LittleEndianOutput output) {
        output.writeBytes(new byte[]{'M', 's', 'R', 'c', 'd', 0});
        output.writeInt32(2);
        output.writeBytes(new byte[]{'D', 'i', 0});         // equates to V5.0

        LittleEndianOutput record = new LittleEndianOutput();
        record.writeInt16(500);                             // equates to V5.0
        record.writeBytes(new byte[]{'S', 'Q', 'A', 0});
        record.writeBytes("Jul  3 2003".getBytes(US_ASCII));
        record.writeUInt8(0);
        record.writeBytes("08:35:33".getBytes(US_ASCII));
        record.writeUInt8(0);
        writeRecord(output, 'A', record.toByteArray());
    }

    // mps_waypoint_w() for version 5 and a waypoint without Garmin data
    static byte[] writeWaypoint(Wpt wpt, boolean routePoint) {
        LittleEndianOutput output = new LittleEndianOutput();
        writeString(output, wpt.writtenName != null ? wpt.writtenName : wpt.shortName);
        output.writeInt32(mapSourceWaypointClass(routePoint));
        writeString(output, "");                            // country
        writeSubclass(output);
        writeLatLon(output, wpt.latitude);
        writeLatLon(output, wpt.longitude);
        writeAltitude(output, wpt.altitude, UNKNOWN_ALTITUDE);
        writeString(output, wpt.description);
        writeAltitude(output, UNKNOWN_ALTITUDE, UNKNOWN_ALTITUDE);  // proximity
        output.writeInt32(MAPSOURCE_WAYPOINT_DISPLAY);
        output.writeInt32(MAPSOURCE_WAYPOINT_COLOR);
        output.writeInt32(mapSourceWaypointIconNumber(WRITE_VERSION));
        output.writeBytes(new byte[3]);                     // city, state, facility
        output.writeUInt8(0);                               // unknown
        writeAltitude(output, UNKNOWN_ALTITUDE, UNKNOWN_ALTITUDE);  // depth
        output.writeBytes(new byte[2]);                     // unknown
        output.writeBytes(new byte[4]);                     // unknown
        writeString(output, wpt.notes);
        return output.toByteArray();
    }

    // GPSBabel names a route or track without name after the time of its first point and else after the
    // current time; without time the name is the same on every run, made unique by the caller
    static String nameOf(Rte rte, String prefix) {
        if (rte.name != null && !rte.name.isEmpty())
            return rte.name;
        long time = rte.points.isEmpty() ? 0 : rte.points.get(0).time;
        return String.format("%s%04x", prefix, time & 0xFFFFFFFFL);
    }

    // mps_routehdr_w(), mps_routedatapoint_w() and mps_routetrlr_w() for version 5
    static byte[] writeRoute(Rte rte, String name) {
        double maxLatitude = -90.0, maxLongitude = -180.0, minLatitude = 90.0, minLongitude = 180.0;
        double maxAltitude = UNKNOWN_ALTITUDE, minAltitude = -UNKNOWN_ALTITUDE;
        for (Wpt wpt : rte.points) {
            if (wpt.latitude > maxLatitude)
                maxLatitude = wpt.latitude;
            if (wpt.latitude < minLatitude)
                minLatitude = wpt.latitude;
            if (wpt.longitude > maxLongitude)
                maxLongitude = wpt.longitude;
            if (wpt.longitude < minLongitude)
                minLongitude = wpt.longitude;
            if (wpt.altitude != UNKNOWN_ALTITUDE) {
                if (wpt.altitude > maxAltitude || maxAltitude == UNKNOWN_ALTITUDE)
                    maxAltitude = wpt.altitude;
                if (wpt.altitude < minAltitude || minAltitude == -UNKNOWN_ALTITUDE)
                    minAltitude = wpt.altitude;
            }
        }

        LittleEndianOutput output = new LittleEndianOutput();
        writeString(output, name);
        output.writeInt16(0);                               // don't autoname
        writeLatLon(output, maxLatitude);
        writeLatLon(output, maxLongitude);
        writeAltitude(output, maxAltitude, UNKNOWN_ALTITUDE);
        writeLatLon(output, minLatitude);
        writeLatLon(output, minLongitude);
        writeAltitude(output, minAltitude, -UNKNOWN_ALTITUDE);
        output.writeInt32(rte.points.size());

        Wpt previous = null;
        for (Wpt wpt : rte.points) {
            if (previous != null)
                writeLink(output, previous, wpt);

            // the name and class of the waypoint record the route point refers to (D21)
            writeString(output, wpt.writtenName != null ? wpt.writtenName : wpt.shortName);
            output.writeInt32(mapSourceWaypointClass(wpt.hidden));
            writeString(output, "");                        // country
            writeSubclass(output);
            output.writeUInt8(0);
            output.writeUInt8(3);
            output.writeBytes(new byte[17]);
            previous = wpt;
        }

        // mps_routetrlr_w()
        output.writeInt32(0);
        output.writeUInt8(1);
        return output.toByteArray();
    }

    // the route link details from the previous to this route point
    private static void writeLink(LittleEndianOutput output, Wpt previous, Wpt wpt) {
        output.writeInt32(2);
        writeLatLon(output, previous.latitude);
        writeLatLon(output, previous.longitude);
        writeAltitude(output, previous.altitude, UNKNOWN_ALTITUDE);
        writeLatLon(output, wpt.latitude);
        writeLatLon(output, wpt.longitude);
        writeAltitude(output, wpt.altitude, UNKNOWN_ALTITUDE);

        double maxLatitude, minLatitude, maxLongitude, minLongitude;
        if (wpt.latitude > previous.latitude) {
            maxLatitude = wpt.latitude;
            minLatitude = previous.latitude;
        } else {
            minLatitude = wpt.latitude;
            maxLatitude = previous.latitude;
        }
        if (wpt.longitude > previous.longitude) {
            maxLongitude = wpt.longitude;
            minLongitude = previous.longitude;
        } else {
            minLongitude = wpt.longitude;
            maxLongitude = previous.longitude;
        }

        double maxAltitude = UNKNOWN_ALTITUDE, minAltitude = -UNKNOWN_ALTITUDE;
        if (wpt.altitude != UNKNOWN_ALTITUDE) {
            maxAltitude = wpt.altitude;
            minAltitude = wpt.altitude;
        }
        if (previous.altitude != UNKNOWN_ALTITUDE) {
            if (previous.altitude > maxAltitude || maxAltitude == UNKNOWN_ALTITUDE)
                maxAltitude = previous.altitude;
            if (previous.altitude < minAltitude || minAltitude == -UNKNOWN_ALTITUDE)
                minAltitude = previous.altitude;
        }

        output.writeUInt8(0);
        writeLatLon(output, maxLatitude);
        writeLatLon(output, maxLongitude);
        writeAltitude(output, maxAltitude, UNKNOWN_ALTITUDE);
        writeLatLon(output, minLatitude);
        writeLatLon(output, minLongitude);
        writeAltitude(output, minAltitude, -UNKNOWN_ALTITUDE);
    }

    // mps_trackhdr_w() and mps_trackdatapoint_w()
    static byte[] writeTrack(Rte trk, String name) {
        LittleEndianOutput output = new LittleEndianOutput();
        writeString(output, name);
        output.writeUInt8(MAPSOURCE_TRACK_DISPLAY);
        output.writeInt32(MAPSOURCE_TRACK_COLOR);
        output.writeInt32(trk.points.size());
        for (Wpt wpt : trk.points) {
            writeLatLon(output, wpt.latitude);
            writeLatLon(output, wpt.longitude);
            writeAltitude(output, wpt.altitude, UNKNOWN_ALTITUDE);
            if (wpt.time > 0) {
                output.writeUInt8(1);
                output.writeInt32((int) wpt.time);
            } else
                output.writeBytes(new byte[5]);
            writeAltitude(output, UNKNOWN_ALTITUDE, UNKNOWN_ALTITUDE);  // depth
        }
        return output.toByteArray();
    }
}
