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
import slash.navigation.gpsbabel.LittleEndianOutput;
import slash.navigation.gpx.Gpx10Format;
import slash.navigation.gpx.GpxPosition;
import slash.navigation.gpx.GpxRoute;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.nio.charset.CodingErrorAction.REPORT;
import static java.nio.charset.StandardCharsets.US_ASCII;
import static java.nio.charset.StandardCharsets.UTF_8;
import static slash.common.type.CompactCalendar.fromMillis;
import static slash.navigation.base.RouteCharacteristics.Route;
import static slash.navigation.base.RouteCharacteristics.Track;
import static slash.navigation.base.RouteCharacteristics.Waypoints;
import static slash.navigation.common.NavigationConversion.formatElevation;
import static slash.navigation.common.NavigationConversion.formatPosition;
import static slash.navigation.garmin.GarminDefaults.MAPSOURCE_DEFAULT_CLASS;
import static slash.navigation.garmin.GarminDefaults.MAPSOURCE_TRACK_COLOR;
import static slash.navigation.garmin.GarminDefaults.MAPSOURCE_TRACK_DISPLAY;
import static slash.navigation.garmin.GarminDefaults.MAPSOURCE_WAYPOINT_COLOR;
import static slash.navigation.garmin.GarminDefaults.MAPSOURCE_WAYPOINT_DISPLAY;
import static slash.navigation.garmin.GarminDefaults.mapSourceWaypointClass;
import static slash.navigation.garmin.GarminDefaults.mapSourceWaypointIconNumber;
import static slash.navigation.garmin.GarminMapSource6Format.asGpxText;
import static slash.navigation.garmin.GarminMapSource6Format.degreesToSemicircles;
import static slash.navigation.garmin.GarminMapSource6Format.qtTrimmed;
import static slash.navigation.gpsbabel.GpsBabelMath.semicirclesToDegrees;

/**
 * Reads and writes Garmin MapSource 5.x (.mps) files, the MapSource file format GPSBabel calls
 * versions 3, 4 and 5.
 * <p>
 * GPSBabel numbers the versions after the MapSource release that wrote them; the file carries
 * the format version in the second byte of its {@code D} record (Oppmann, <i>Garmin MapSource (MPS),
 * GPS Database (GDB) and GFI File Format</i>: format 1.0 to 1.5): {@code 'd'} (MapSource 3.x) is
 * version 3, {@code 'e'} to {@code 'h'} (MapSource 4.x) version 4 and {@code 'i'} (MapSource 5.x
 * and later) version 5. Other versions are not read.
 * <p>
 * Reading delivers what GPSBabel 1.5.4 delivered through its GPX 1.0 exchange file: the user
 * waypoints, the routes with the waypoints they refer to and the tracks. Writing produces version 5
 * files the way GPSBabel's {@code mapsource} writer did without options. Text is Windows-1252 like
 * MapSource writes it; on reading, text that is valid UTF-8 (as GPSBabel wrote it) is kept as UTF-8.
 *
 * @author Christian Pesch
 */

public class GarminMapSource5Format extends GpxBackedNavigationFormat implements MultipleRoutesFormat<GpxRoute> {
    static final byte[] SIGNATURE = {'M', 's', 'R', 'c', 'd', 0};
    static final int WRITE_VERSION = 5;
    static final double UNKNOWN_ALTITUDE = -99999999.0;
    static final Charset WINDOWS_1252 = Charset.forName("windows-1252");
    // MPSNAMEBUFFERLEN and the size of tbuf in mapsource.cc
    private static final int NAME_BUFFER_LENGTH = 1024;
    private static final int BUFFER_LENGTH = 100;
    private static final int GPX_COORDINATE_DIGITS = 9;
    private static final int GPX_ELEVATION_DIGITS = 6;
    // the value DateTime::toTime_t() returns for a time it cannot represent
    private static final long INVALID_TIME_T = 0xFFFFFFFFL;

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

    /** a file GPSBabel refuses with a fatal error: the native reader returns no route */
    static class MpsException extends Exception {
        MpsException(String message) {
            super(message);
        }
    }

    /** GPSBabel's Waypoint, reduced to the fields mapsource.cc and the GPX exchange use */
    static class Wpt {
        String shortName;
        String description;
        String notes;
        double latitude;
        double longitude;
        double altitude = UNKNOWN_ALTITUDE;
        // seconds since 1970 as DateTime::toTime_t() returns them, 0 = none
        long time;
        int waypointClass;

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
            return result;
        }
    }

    static class Rte {
        String name;
        final List<Wpt> points = new ArrayList<>();
    }

    /**
     * gbfile.cc for a file GPSBabel reads through zlib ({@code gzapi_*}): seeking beyond the end is
     * allowed, {@code gbfeof()} is true at the end of the file, {@code gbfgetc()} returns EOF there,
     * a block read that ends early and a number read beyond the end are fatal.
     */
    static class GbFile {
        private final byte[] bytes;
        private long position;

        GbFile(byte[] bytes) {
            this.bytes = bytes;
        }

        long tell() {
            return position;
        }

        private long available() {
            return Math.max(0, bytes.length - position);
        }

        boolean eof() {
            return position >= bytes.length;
        }

        // gbfseek(file, int32_t offset, SEEK_SET)
        void seek(long offset) throws MpsException {
            long target = (int) offset;
            if (target < 0)
                throw new MpsException("Seek before the start of the file");
            position = target;
        }

        // gbfseek(file, int32_t offset, SEEK_CUR)
        void skip(int offset) throws MpsException {
            long target = position + offset;
            if (target < 0)
                throw new MpsException("Seek before the start of the file");
            position = target;
        }

        int getc() {
            if (position >= bytes.length)
                return -1;
            return bytes[(int) position++] & 0xFF;
        }

        // gbfread(buffer, count, 1): returns false at the end of the file and leaves the buffer as it is
        boolean read(byte[] buffer, int count) throws MpsException {
            long available = available();
            if (available == 0)
                return false;
            if (available < count)
                throw new MpsException("Unexpected end of file");
            System.arraycopy(bytes, (int) position, buffer, 0, count);
            position += count;
            return true;
        }

        void read(int count) throws MpsException {
            read(new byte[count], count);
        }

        private ByteBuffer number(int size) throws MpsException {
            if (available() < size)
                throw new MpsException("Unexpected end of file");
            ByteBuffer buffer = ByteBuffer.wrap(bytes, (int) position, size).order(ByteOrder.LITTLE_ENDIAN);
            position += size;
            return buffer;
        }

        int getInt32() throws MpsException {
            return number(4).getInt();
        }

        double getDouble() throws MpsException {
            return number(8).getDouble();
        }

        /** mps_readstr(): at most size bytes up to and including a NUL, the bytes before the NUL are returned */
        byte[] readString(byte[] buffer, int size) {
            ByteArrayOutputStream result = new ByteArrayOutputStream();
            int index = 0, c;
            while (size-- > 0 && (c = getc()) != -1) {
                if (buffer != null)
                    buffer[index++] = (byte) c;
                if (c == 0)
                    break;
                result.write(c);
            }
            return result.toByteArray();
        }

        /** gbfgetcstr(): the bytes up to a NUL or the end of the file */
        byte[] readCString() {
            ByteArrayOutputStream result = new ByteArrayOutputStream();
            int c;
            while ((c = getc()) != -1 && c != 0)
                result.write(c);
            return result.toByteArray();
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
        try {
            return new Reader(bytes).read();
        } catch (MpsException e) {
            // GPSBabel stops with a fatal error: no route
            return null;
        }
    }

    static boolean isMps(byte[] bytes) {
        if (bytes.length < SIGNATURE.length)
            return false;
        for (int i = 0; i < SIGNATURE.length; i++)
            if (bytes[i] != SIGNATURE[i])
                return false;
        return true;
    }

    /** {@code mps_read()} with {@code mps_fileHeader_r()}, {@code mps_waypoint_r()}, {@code mps_route_r()}, {@code mps_track_r()} */
    class Reader {
        private final GbFile file;
        private int version;
        // waypt_add()ed user waypoints and read_route_wpt_head, the waypoints only used by routes
        private final List<Wpt> waypoints = new ArrayList<>();
        private final Names waypointNames = new Names();
        private final Names routeWaypointNames = new Names();
        private final List<Rte> routes = new ArrayList<>();
        private final List<Rte> tracks = new ArrayList<>();
        // rte_waypts in route.cc: numbers the route points without name across all routes
        private int routePointCount;

        Reader(byte[] bytes) {
            this.file = new GbFile(bytes);
        }

        int readVersion() throws MpsException {
            readHeader();
            return version;
        }

        private void readHeader() throws MpsException {
            byte[] signature = file.readString(null, BUFFER_LENGTH);
            if (!Arrays.equals(signature, "MsRcd".getBytes(US_ASCII)))
                throw new MpsException("This doesn't look like a mapsource file");
            int length = file.getInt32();
            byte[] data = new byte[3];
            if (!file.read(data, 3) || data[0] != 'D')
                throw new MpsException("This doesn't look like a mapsource file");
            // a signed char as in mapsource.cc
            byte flag = data[1];
            if (flag == 'd')
                version = 3;
            else if (flag > 'd' && flag <= 'h')
                version = 4;
            else if (flag > 'h' && flag <= 'i')
                version = 5;
            else
                throw new MpsException("Unsupported version of mapsource file");
            // skip the format details and the program signature
            file.skip(length + 1 - 3);
            length = file.getInt32();
            if (length >= 0)
                file.skip(length + 1);
        }

        List<GpxRoute> read() throws MpsException {
            readHeader();

            boolean moreWork = true;
            // the type of the previous record stays when the file ends behind a record length
            char type = 0;
            byte[] buffer = new byte[1];
            while (moreWork && !file.eof()) {
                int length = file.getInt32();
                if (length < 0)
                    throw new MpsException("A record length read from the input file is invalid");
                if (file.read(buffer, 1))
                    type = (char) (buffer[0] & 0xFF);
                long position = file.tell();
                switch (type) {
                    case 'W' -> {
                        Wpt wpt = readWaypoint();
                        if (file.tell() != position + length)
                            // lost sync with the file: junk the waypoint
                            file.seek(position + length);
                        else if (wpt.waypointClass == MAPSOURCE_DEFAULT_CLASS)
                            addWaypoint(wpt);
                        else
                            routeWaypointNames.add(wpt.copy());
                    }
                    case 'R' -> {
                        readRoute();
                        resync(position + length);
                    }
                    case 'T' -> {
                        readTrack();
                        resync(position + length);
                    }
                    case 'L' -> {
                        // map segment: skipped
                        file.skip(-5);
                        int segmentLength = file.getInt32();
                        if (segmentLength >= 0)
                            file.skip(segmentLength + 1);
                        resync(position + length);
                    }
                    case 'V' -> {
                        // map set: the last record in the file
                        file.skip(-5);
                        int setLength = file.getInt32();
                        file.skip(setLength + 1);
                        moreWork = false;
                    }
                    default -> file.skip(length);
                }
            }
            return toRoutes();
        }

        // a route or track that lost sync with the file is kept, only the file position is corrected
        private void resync(long position) throws MpsException {
            if (file.tell() != position)
                file.seek(position);
        }

        // waypt_add(): wraps the position into range and fills a missing description
        private void addWaypoint(Wpt wpt) {
            normalize(wpt);
            waypoints.add(wpt);
            waypointNames.add(wpt);
            if (wpt.description == null || wpt.description.isEmpty())
                wpt.description = wpt.notes != null ? wpt.notes : wpt.shortName;
        }

        private double readAltitude() throws MpsException {
            if (file.getc() == 1)
                return file.getDouble();
            file.skip(8);
            return UNKNOWN_ALTITUDE;
        }

        private Wpt readWaypoint() throws MpsException {
            Wpt wpt = new Wpt();
            wpt.shortName = decode(file.readString(null, NAME_BUFFER_LENGTH));
            wpt.waypointClass = file.getInt32();
            file.readString(null, BUFFER_LENGTH);           // country
            file.read(17);                                  // subclass
            if (version >= 4)
                file.read(5);                               // additional subclass and terminator
            int latitude = file.getInt32();
            int longitude = file.getInt32();
            wpt.altitude = readAltitude();
            wpt.description = decode(file.readCString());
            readAltitude();                                 // proximity
            file.getInt32();                                // display
            file.getInt32();                                // colour
            file.getInt32();                                // symbol
            file.readString(null, BUFFER_LENGTH);           // city
            file.readString(null, BUFFER_LENGTH);           // state
            file.readString(null, BUFFER_LENGTH);           // facility
            file.read(1);                                   // unknown
            readAltitude();                                 // depth
            if (version >= 4) {
                file.read(6);                               // unknown
                wpt.notes = decode(file.readCString());
            } else
                file.read(2);                               // unknown
            wpt.latitude = semicirclesToDegrees(latitude);
            wpt.longitude = semicirclesToDegrees(longitude);
            return wpt;
        }

        // the subclass data and the unknown bytes of a route point; tbuf is mps_route_r()'s buffer
        private void skipRoutePointData(byte[] tbuf) throws MpsException {
            if (version >= 4) {
                file.read(tbuf, 22);                        // subclass data
                // a variable length of data terminated by a zero
                do {
                    // at the end of the file GPSBabel keeps reading the same non-zero byte forever
                    if (!file.read(tbuf, 1) && tbuf[0] != 0)
                        throw new MpsException("Unexpected end of file in route");
                } while (tbuf[0] != 0);
                file.read(tbuf, 18);                        // unknown 0x03 0x00 .. 0x00
            } else {
                file.read(tbuf, 17);                        // subclass data
                file.read(tbuf, 18);                        // unknown 0x00 0x03 0x00 .. 0x00
            }
        }

        // find_waypt_by_name(), then the waypoints only used by routes, then a fallback from the link
        private Wpt findRoutePoint(byte[] name, int latitude, int longitude, double altitude) {
            String shortName = decode(name);
            Wpt found = waypointNames.find(shortName);
            if (found == null)
                found = routeWaypointNames.find(shortName);
            if (found != null)
                return found.copy();

            Wpt wpt = new Wpt();
            wpt.shortName = shortName;
            wpt.latitude = semicirclesToDegrees(latitude);
            wpt.longitude = semicirclesToDegrees(longitude);
            wpt.altitude = altitude;
            return wpt;
        }

        // route_add_wpt()
        private void addRoutePoint(Rte rte, Wpt wpt) {
            rte.points.add(wpt);
            routePointCount++;
            if (wpt.shortName == null || wpt.shortName.isEmpty())
                wpt.shortName = String.format("RPT%03d", routePointCount);
        }

        private void readRoute() throws MpsException {
            byte[] tbuf = new byte[BUFFER_LENGTH];
            int latitude = 0, longitude = 0;
            double altitude = UNKNOWN_ALTITUDE;

            String name = decode(file.readCString());
            file.read(tbuf, 1);                             // autoname flag
            file.read(tbuf, 1);
            if (tbuf[0] == 0) {
                latitude = file.getInt32();                 // maximum of the route
                longitude = file.getInt32();
                altitude = readAltitude();
                latitude = file.getInt32();                 // minimum of the route
                longitude = file.getInt32();
                altitude = readAltitude();
            }

            int count = file.getInt32();
            if (count < 0)
                return;

            Rte rte = new Rte();
            rte.name = name;
            routes.add(rte);

            // GPSBabel loops round for one less than the number of route points, for none until the end of the file
            count--;
            while (count-- != 0) {
                byte[] pointName = file.readString(null, NAME_BUFFER_LENGTH);
                file.getInt32();                            // class
                file.readString(tbuf, BUFFER_LENGTH);       // country
                skipRoutePointData(tbuf);

                int steps = file.getInt32();
                if (steps < 1)
                    return;

                // first end of the link
                latitude = file.getInt32();
                longitude = file.getInt32();
                altitude = readAltitude();
                addRoutePoint(rte, findRoutePoint(pointName, latitude, longitude, altitude));

                for (int step = steps - 1; step > 0; step--) {
                    latitude = file.getInt32();
                    longitude = file.getInt32();
                    altitude = readAltitude();
                }

                file.read(tbuf, 1);                         // NUL
                file.read(tbuf, 4);                         // link maximum latitude
                file.read(tbuf, 4);                         // link maximum longitude
                file.read(tbuf, 9);                         // link maximum altitude
                file.read(tbuf, 4);                         // link minimum latitude
                file.read(tbuf, 4);                         // link minimum longitude
                file.read(tbuf, 9);                         // link minimum altitude
            }

            // the last route point with a small trailer, its position comes from the last link
            byte[] pointName = file.readString(null, NAME_BUFFER_LENGTH);
            file.getInt32();                                // class
            file.readString(tbuf, BUFFER_LENGTH);           // country
            skipRoutePointData(tbuf);
            file.read(tbuf, 5);                             // trailer
            addRoutePoint(rte, findRoutePoint(pointName, latitude, longitude, altitude));
        }

        private void readTrack() throws MpsException {
            String name = decode(file.readCString());
            file.getc();                                    // display flag
            file.getInt32();                                // colour
            int count = file.getInt32();
            if (count < 0)
                return;

            Rte trk = new Rte();
            trk.name = name;
            tracks.add(trk);

            // a point without time keeps the time of the previous point
            int time = 0;
            while (count-- != 0) {
                int latitude = file.getInt32();
                int longitude = file.getInt32();
                double altitude = readAltitude();
                if (file.getc() == 1)
                    time = file.getInt32();
                else
                    file.getInt32();
                readAltitude();                             // depth

                Wpt wpt = new Wpt();
                wpt.latitude = semicirclesToDegrees(latitude);
                wpt.longitude = semicirclesToDegrees(longitude);
                wpt.altitude = altitude;
                // SetCreationTime(time_t) takes the seconds as an unsigned int
                wpt.time = time & 0xFFFFFFFFL;
                trk.points.add(wpt);
            }
        }

        private List<GpxRoute> toRoutes() {
            List<GpxRoute> result = new ArrayList<>();
            if (!waypoints.isEmpty())
                result.add(createRoute(Waypoints, null, waypoints));
            for (Rte rte : routes)
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

    /**
     * The waypoints of find_waypt_by_name() and mps_find_wpt_q_by_name() by name: the first waypoint
     * with a name wins, a null and an empty name are equal like QString's operator==.
     */
    static class Names {
        private final Map<String, Wpt> waypoints = new HashMap<>();

        private static String key(String name) {
            return name == null ? "" : name;
        }

        void add(Wpt wpt) {
            waypoints.putIfAbsent(key(wpt.shortName), wpt);
        }

        Wpt find(String name) {
            return waypoints.get(key(name));
        }
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
     * {@code DateTime::toTime_t()} of a time read from the GPX exchange file: the seconds since
     * 1970 as an unsigned int, 0 for no time, {@code 0xFFFFFFFF} for a time it cannot represent.
     */
    static long toTimeT(CompactCalendar time) {
        if (time == null)
            return 0;
        long millis = time.getTimeInMillis();
        if (millis < 0)
            return INVALID_TIME_T;
        long seconds = millis / 1000;
        return seconds < INVALID_TIME_T ? seconds : INVALID_TIME_T;
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
        // written_wpt_head and written_route_wpt_head
        private final Names waypointNames = new Names();
        private final Names writtenWaypoints = new Names();
        private final Names writtenRouteWaypoints = new Names();
        private int routePointCount;

        private Wpt createWpt(GpxPosition position) {
            Wpt wpt = new Wpt();
            wpt.longitude = formatPosition(position.getLongitude()).doubleValue();
            wpt.latitude = formatPosition(position.getLatitude()).doubleValue();
            BigDecimal elevation = formatElevation(position.getElevation());
            wpt.altitude = elevation != null ? elevation.doubleValue() : UNKNOWN_ALTITUDE;
            wpt.time = toTimeT(position.getTime());
            wpt.shortName = asGpsBabelText(asName(position.getDescription()));
            wpt.notes = asGpsBabelText(asDesc(position.getDescription()));
            return wpt;
        }

        private boolean isValid(GpxPosition position) {
            // the GPX written for GPSBabel skips positions without valid coordinates
            return formatPosition(position.getLongitude()) != null && formatPosition(position.getLatitude()) != null;
        }

        // gpx.cc <wpt> and waypt_add()
        void addWaypoints(List<GpxPosition> positions) throws IOException {
            for (GpxPosition position : positions) {
                if (!isValid(position))
                    continue;
                Wpt wpt = createWpt(position);
                normalize(wpt);
                if (wpt.latitude < -90 || wpt.latitude > 90.0)
                    throw new IOException("Invalid latitude " + position.getLatitude() + " in waypoint " + wpt.shortName);
                if (wpt.longitude < -180 || wpt.longitude > 180.0)
                    throw new IOException("Invalid longitude " + position.getLongitude() + " in waypoint " + wpt.shortName);
                waypoints.add(wpt);
                if (wpt.shortName == null)
                    wpt.shortName = wpt.notes != null ? wpt.notes : String.format("WPT%03d", waypoints.size());
                if (wpt.description == null || wpt.description.isEmpty())
                    wpt.description = wpt.notes != null ? wpt.notes : wpt.shortName;
                waypointNames.add(wpt);
            }
        }

        // gpx.cc <rte>/<trk> and route_add_wpt()/track_add_wpt()
        void addRoute(GpxRoute route, List<GpxPosition> positions, boolean track) {
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

        private boolean isWritten(Wpt wpt) {
            return writtenWaypoints.find(wpt.shortName) != null || writtenRouteWaypoints.find(wpt.shortName) != null;
        }

        byte[] write() {
            LittleEndianOutput output = new LittleEndianOutput();
            writeHeader(output);

            // mps_waypoint_w_unique_wrapper(): a waypoint is written once per name
            for (Wpt wpt : waypoints) {
                if (isWritten(wpt))
                    continue;
                writeRecord(output, 'W', writeWaypoint(wpt, false));
                writtenWaypoints.add(wpt);
            }

            // mps_route_wpt_w_unique_wrapper(): every route point needs a waypoint, the ones that are
            // no user waypoint are written hidden
            for (Rte rte : routes) {
                for (Wpt wpt : rte.points) {
                    if (isWritten(wpt))
                        continue;
                    if (waypointNames.find(wpt.shortName) == null) {
                        writeRecord(output, 'W', writeWaypoint(wpt, true));
                        writtenRouteWaypoints.add(wpt);
                    } else {
                        writeRecord(output, 'W', writeWaypoint(wpt, false));
                        writtenWaypoints.add(wpt);
                    }
                }
            }

            for (Rte rte : routes) {
                // GPSBabel writes a route without points with a broken record length
                if (rte.points.isEmpty())
                    continue;
                writeRecord(output, 'R', writeRoute(rte, writtenRouteWaypoints));
            }

            for (Rte trk : tracks)
                writeRecord(output, 'T', writeTrack(trk));

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
        writeString(output, wpt.shortName);
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

    // the time that makes the name of a route or track without name unique
    private static long uniqueValue(Rte rte) {
        long result = rte.points.isEmpty() ? 0 : rte.points.get(0).time;
        return result != 0 ? result : System.currentTimeMillis() / 1000;
    }

    private static String nameOf(Rte rte, String prefix) {
        if (rte.name != null && !rte.name.isEmpty())
            return rte.name;
        return String.format("%s%04x", prefix, uniqueValue(rte) & 0xFFFFFFFFL);
    }

    // mps_routehdr_w(), mps_routedatapoint_w() and mps_routetrlr_w() for version 5
    static byte[] writeRoute(Rte rte, Names writtenRouteWaypoints) {
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
        writeString(output, nameOf(rte, "Route"));
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

            writeString(output, wpt.shortName);
            boolean routePoint = writtenRouteWaypoints.find(wpt.shortName) != null;
            output.writeInt32(mapSourceWaypointClass(routePoint));
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
    static byte[] writeTrack(Rte trk) {
        LittleEndianOutput output = new LittleEndianOutput();
        writeString(output, nameOf(trk, "Track"));
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
