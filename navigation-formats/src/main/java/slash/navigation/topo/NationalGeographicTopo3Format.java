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

    Copyright (C) 2005 Steve Chamberlin, slc at alum.mit.edu
    Portions Copyright (C) 2006 Curtis E. Mills, archer at eskimo dot com
    Translated to Java from GPSBabel tpo.cc, tpo.h (master 9c1e336) for RouteConverter, 2026; modified.
*/

package slash.navigation.topo;

import slash.navigation.base.GpxBackedNavigationFormat;
import slash.navigation.base.UnreadableFileException;
import slash.navigation.gpsbabel.LittleEndianInput;
import slash.navigation.gpx.Gpx10Format;
import slash.navigation.gpx.GpxPosition;
import slash.navigation.gpx.GpxRoute;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import static java.math.RoundingMode.HALF_EVEN;
import static java.nio.charset.StandardCharsets.US_ASCII;
import static java.util.Collections.emptyList;
import static slash.navigation.base.RouteCharacteristics.Route;
import static slash.navigation.base.RouteCharacteristics.Track;
import static slash.navigation.base.RouteCharacteristics.Waypoints;

/**
 * Reads National Geographic TOPO! 3.x and 4.x (.tpo) files. A native translation of the read path of
 * GPSBabel's tpo3 format as RouteConverter used it: options "-r -w -t", i.e. tracks (freehand routes),
 * waypoints, routes, map notes, symbols and text labels; map notes, symbols and text labels become waypoints.
 * Tracks are decoded with GPSBabel's 2020 algorithm (spec 00083 decision D13.1).
 *
 * @author Christian Pesch
 */

public class NationalGeographicTopo3Format extends GpxBackedNavigationFormat {
    // the format spec declares Windows-1252 for the strings of these Windows-only files
    static final Charset CHARSET = Charset.forName("windows-1252");

    private static final byte[] VERSION_PREFIX = "TOPO! Ver".getBytes(US_ASCII);
    private static final int FIXED_HEADER_SIZE = 512;

    static final int SYMBOLS_BLOCK = 0x040000;
    static final int TRACK_STYLES_BLOCK = 0x050000;
    static final int TRACKS_BLOCK = 0x060000;
    static final int TEXT_LABELS_BLOCK = 0x080000;
    static final int MAP_NOTES_BLOCK = 0x090000;
    static final int WAYPOINTS_BLOCK = 0x0e0000;
    static final int ROUTES_BLOCK = 0x0f0000;

    private static final int TRACK_NAME_LENGTH = 255;
    private static final int UNKNOWN_ALTITUDE = -3276800;
    private static final double COORDINATE_SCALE = 0x800000;
    private static final int END_SCALE_TAG = 0x00;
    private static final int FULL_POINT_TAG = 0x88;
    // MAGIC! (no idea where this comes from)
    private static final int[] SCALE_MULTIPLIERS = {0, 1, 2, 3, 4, 5, 6, 7, -8, -7, -6, -5, -4, -3, -2, -1};
    // GPSBabel writes coordinates to GPX with %0.9f and elevations with %0.6f
    private static final int GPX_COORDINATE_DIGITS = 9;
    private static final int GPX_ELEVATION_DIGITS = 6;

    public String getExtension() {
        return ".tpo";
    }

    public String getName() {
        return "National Geographic Topo 3/4 (*" + getExtension() + ")";
    }

    public boolean isSupportsWriting() {
        return false;
    }

    public boolean isSupportsMultipleRoutes() {
        return false;
    }

    public void write(GpxRoute route, OutputStream target, int startIndex, int endIndex) {
        throw new UnsupportedOperationException();
    }

    // reading

    /**
     * tpo_check_version_string(): a byte count, then a NUL-terminated string starting with "TOPO! Ver"
     * that marks 3.x and 4.x files; earlier files are tpo2, which GPSBabel refuses with "-r -w".
     */
    static boolean isTopo3Header(byte[] data) {
        if (data.length < 1 + VERSION_PREFIX.length)
            return false;
        for (int i = 0; i < VERSION_PREFIX.length; i++) {
            if (data[1 + i] != VERSION_PREFIX[i])
                return false;
        }
        return findNul(data) != -1;
    }

    private static int findNul(byte[] data) {
        for (int i = 1; i < data.length; i++) {
            if (data[i] == 0)
                return i;
        }
        return -1;
    }

    /**
     * Checks the chain of blocks that follows the fixed-length header: every block points to the next one
     * inside the file until a pointer of 0 ends the chain. GPSBabel scans for
     * the "Red Without Arrow" compass style instead, a user-editable name that the readers do not need,
     * since every reader finds its block by following this chain (tpo_find_block()).
     *
     * @return null if the chain is sound, otherwise what is wrong with it
     */
    static String checkBlockChain(byte[] data) {
        long offset = FIXED_HEADER_SIZE;
        // a chain of blocks of at least 8 bytes each cannot be longer than this
        int maximumHops = data.length / 8 + 1;
        for (int hops = 0; hops < maximumHops; hops++) {
            if (offset + 8 > data.length)
                return "block " + (hops + 1) + " at byte " + offset + " is beyond the end of the file at " + data.length;
            // the type of the block at position does not matter here
            int position = (int) offset;
            offset = ((data[position + 4] & 0xFFL) | (data[position + 5] & 0xFFL) << 8 |
                    (data[position + 6] & 0xFFL) << 16 | (data[position + 7] & 0xFFL) << 24);
            if (offset == 0)
                return null;
        }
        return "block chain loops";
    }

    protected List<GpxRoute> readRoutes(InputStream source) throws IOException {
        byte[] data = source.readAllBytes();
        if (!isTopo3Header(data))
            return emptyList();

        // the version string at the start makes this a TOPO! file: from here on it is damaged, not foreign
        String chainProblem = checkBlockChain(data);
        if (chainProblem != null)
            throw new UnreadableFileException("TOPO! file is damaged: " + chainProblem);
        try {
            return new Parser(data).read();
        } catch (EOFException e) {
            throw new UnreadableFileException("TOPO! file ends unexpectedly: " + e.getMessage(), e);
        } catch (InvalidTopoException e) {
            throw new UnreadableFileException("TOPO! file is damaged: " + e.getMessage(), e);
        }
    }

    private static class InvalidTopoException extends Exception {
        InvalidTopoException(String message) {
            super(message);
        }
    }

    private static class Style {
        String name;
        final int[] color = new int[3];
        int wide;
        int dash;
    }

    /**
     * A waypoint as GPSBabel holds it; RouteConverter sees its GPX form.
     */
    private static class Point {
        double longitude, latitude;
        Double elevation;
        String shortName, description;

        Point(int lat, int lon) {
            // tpo_convert_ll(): the 3.x files are already in WGS84
            this.latitude = (double) lat / COORDINATE_SCALE;
            this.longitude = (double) lon / COORDINATE_SCALE;
        }

        Point(Point other) {
            this.longitude = other.longitude;
            this.latitude = other.latitude;
            this.elevation = other.elevation;
            this.shortName = other.shortName;
            this.description = other.description;
        }
    }

    private class Parser {
        private final LittleEndianInput input;
        private final List<Point> waypoints = new ArrayList<>();
        private final List<GpxRoute> routes = new ArrayList<>();
        private final List<GpxRoute> tracks = new ArrayList<>();

        Parser(byte[] data) {
            this.input = new LittleEndianInput(data);
        }

        /**
         * tpo_read_3_x(): GPSBabel's GPX lists waypoints, routes and tracks in this order
         */
        List<GpxRoute> read() throws EOFException, InvalidTopoException {
            readTracks();
            List<Point> index = readWaypoints();
            readRoutes(index);
            readMapNotes();
            readSymbols();
            readTextLabels();

            List<GpxRoute> result = new ArrayList<>();
            if (!waypoints.isEmpty()) {
                List<GpxPosition> positions = new ArrayList<>();
                for (Point waypoint : waypoints) {
                    // GPSBabel's waypt_add(): a waypoint without description takes its name
                    if (waypoint.description == null || waypoint.description.isEmpty())
                        waypoint.description = waypoint.shortName;
                    positions.add(createPosition(waypoint));
                }
                result.add(new GpxRoute(new Gpx10Format(), Waypoints, null, null, positions));
            }
            result.addAll(routes);
            result.addAll(tracks);
            return result;
        }

        /**
         * tpo_read_int(): 8, 16 or 32 bits depending on the first byte
         */
        private long readInt() throws EOFException {
            int value = input.readUInt8();
            return switch (value) {
                case 0xff -> input.readInt32() & 0xFFFFFFFFL;
                case 0xfe -> input.readUInt16();
                default -> value;
            };
        }

        /**
         * A count or length that cannot fit in the rest of the file comes from a corrupt or hostile file.
         */
        private int readLength(int minimumSize) throws EOFException, InvalidTopoException {
            long length = readInt();
            if (length * minimumSize > input.remaining())
                throw new InvalidTopoException("Count " + length + " exceeds the " + input.remaining() + " remaining bytes");
            return (int) length;
        }

        /**
         * GPSBabel 1.5.4 held the strings as C strings: they end at the first NUL
         */
        private String readString(int length) throws EOFException {
            byte[] bytes = input.readBytes(length);
            int end = 0;
            while (end < bytes.length && bytes[end] != 0)
                end++;
            return asGpxText(new String(bytes, 0, end, CHARSET));
        }

        /**
         * gbfgetc() returns EOF at the end of the file and GPSBabel ignores these bytes
         */
        private void skipIgnoredBytes(int count) throws EOFException {
            input.skip(Math.min(count, input.remaining()));
        }

        /**
         * tpo_find_block(): follows the chain of blocks after the fixed-length header and positions the
         * input at the first data byte of the desired block
         */
        private boolean findBlock(int desired) throws EOFException, InvalidTopoException {
            long offset = FIXED_HEADER_SIZE;
            int type;
            int hops = 0;
            do {
                if (offset > Integer.MAX_VALUE)
                    throw new EOFException("Cannot seek to " + offset);
                input.seek((int) offset);
                type = input.readInt32();
                offset = input.readInt32() & 0xFFFFFFFFL;
                // a chain of blocks of at least 8 bytes each cannot be longer than this
                if (++hops > (input.position() + input.remaining()) / 8 + 1)
                    throw new InvalidTopoException("Block chain loops");
            } while (type != desired && offset != 0);
            return type == desired;
        }

        /**
         * tpo_process_tracks(): track styles, then the freehand routes, delta-encoded with the 2020 algorithm
         */
        private void readTracks() throws EOFException, InvalidTopoException {
            if (!findBlock(TRACK_STYLES_BLOCK))
                return;

            // per style: 2 unknown, 3 color, 1 unknown, 1 name length, 2 width/dashed bytes; the 2 unknown
            // bytes at the end may be missing at the end of the file
            int styleCount = readLength(9);
            Style[] styles = new Style[styleCount];
            for (int i = 0; i < styleCount; i++) {
                Style style = new Style();
                input.skip(2);
                for (int j = 0; j < style.color.length; j++)
                    style.color[j] = input.readUInt8();
                input.skip(1);

                int nameLength = input.readUInt8();
                // GPSBabel warns and skips all tracks
                if (nameLength >= TRACK_NAME_LENGTH)
                    return;
                String name = nameLength > 0 ? readString(nameLength) : "STYLE " + i;
                style.name = name.replace(',', '_').replace('=', '_');

                style.wide = input.readUInt8();
                style.dash = input.readUInt8();
                skipIgnoredBytes(2);
                styles[i] = style;
            }

            if (!findBlock(TRACKS_BLOCK))
                return;

            // per track: line type, style, length, name length, byte count
            int trackCount = readLength(5);
            for (int i = 0; i < trackCount; i++) {
                readInt(); // line type
                long styleIndex = readInt() - 1; // starts at 1
                if (styleIndex < 0 || styleIndex >= styles.length)
                    throw new InvalidTopoException("Track " + (i + 1) + " refers to undefined style " + (styleIndex + 1));
                Style style = styles[(int) styleIndex];
                readInt(); // length in meters?

                int nameLength = readLength(1);
                String name = nameLength > 0 ? readString(nameLength) : "TRK " + (i + 1);
                String description = String.format("Style=%s, Width=%d, Dashed=%d, Color=#%02x%02x%02x",
                        style.name, style.wide, style.dash, style.color[0], style.color[1], style.color[2]);

                int byteCount = readLength(1);
                byte[] bytes = input.readBytes(byteCount);
                List<GpxPosition> positions = new ArrayList<>();
                for (Point point : decodeTrack(bytes))
                    positions.add(createPosition(point));
                // the GPX reader of RouteConverter skips tracks without points
                if (!positions.isEmpty())
                    tracks.add(new GpxRoute(new Gpx10Format(), Track, emptyToNull(name), asDescription(description), positions));
            }
        }

        /**
         * tpo_process_waypoints(): GPS waypoints; their copies, made before waypt_add(), make up the routes
         */
        private List<Point> readWaypoints() throws EOFException, InvalidTopoException {
            List<Point> index = new ArrayList<>();
            if (!findBlock(WAYPOINTS_BLOCK))
                return index;

            // per waypoint: 2 unknown, name length, 1 unknown, longitude, latitude, altitude, comment length;
            // the 4 unknown bytes at the end may be missing at the end of the file
            int waypointCount = readLength(17);
            for (int i = 0; i < waypointCount; i++) {
                readInt();
                readInt();
                int nameLength = readLength(1);
                String name = nameLength > 0 ? readString(nameLength) : "WPT " + (i + 1);
                readInt();

                int lon = input.readInt32();
                int lat = input.readInt32();
                Point waypoint = new Point(lat, lon);
                waypoint.shortName = name;

                // the altitude in centimeters
                int altitude = input.readInt32();
                if (altitude != UNKNOWN_ALTITUDE)
                    waypoint.elevation = (double) altitude / 100.0;

                int commentLength = readLength(1);
                if (commentLength > 0)
                    waypoint.description = readString(commentLength);

                index.add(new Point(waypoint));
                waypoints.add(waypoint);
                skipIgnoredBytes(4);
            }
            return index;
        }

        /**
         * tpo_process_routes(): GPS routes, made of waypoints by their 1-based index
         */
        private void readRoutes(List<Point> index) throws EOFException, InvalidTopoException {
            if (!findBlock(ROUTES_BLOCK))
                return;

            // per route: 2 unknown, name length, comment, point count
            int routeCount = readLength(5);
            int routePointCount = 0;
            for (int i = 0; i < routeCount; i++) {
                readInt();
                readInt();
                int nameLength = readLength(1);
                String name = nameLength > 0 ? readString(nameLength) : "RTE " + (i + 1);
                readInt(); // comment?

                int pointCount = readLength(1);
                List<GpxPosition> positions = new ArrayList<>();
                for (int j = 0; j < pointCount; j++) {
                    long number = readInt();
                    if (number < 1 || number > index.size())
                        throw new InvalidTopoException("Route " + (i + 1) + " refers to undefined waypoint " + number);
                    Point point = new Point(index.get((int) number - 1));
                    // route_add_wpt() numbers a point without name across all routes
                    routePointCount++;
                    if (point.shortName == null || point.shortName.isEmpty())
                        point.shortName = String.format("RPT%03d", routePointCount);
                    positions.add(createPosition(point));
                }
                // GPSBabel delivers a route without points as an empty <rte>, which the GPX reader keeps
                routes.add(new GpxRoute(new Gpx10Format(), Route, emptyToNull(name), null, positions));
            }
        }

        /**
         * tpo_process_map_notes(): map notes become waypoints "NOTE n" with their comment; the external
         * and image paths become links, which RouteConverter does not read
         */
        private void readMapNotes() throws EOFException, InvalidTopoException {
            if (!findBlock(MAP_NOTES_BLOCK))
                return;

            // per note: 1 unknown, longitude, latitude, 3 unknown, 3 lengths, 2 unknown, skip count, 2 unknown
            int noteCount = readLength(19);
            for (int i = 0; i < noteCount; i++) {
                readInt();
                int lon = input.readInt32();
                int lat = input.readInt32();
                Point note = new Point(lat, lon);
                note.shortName = "NOTE " + (i + 1);
                readInt();
                readInt();
                readInt();

                int commentLength = readLength(1);
                if (commentLength > 0)
                    note.description = readString(commentLength);
                // external path, image path
                input.skip(readLength(1));
                input.skip(readLength(1));
                readInt();
                readInt();
                // bytes until the next element or the end of the block
                input.skip(readLength(1));
                readInt();
                readInt();
                waypoints.add(note);
            }
        }

        /**
         * tpo_process_symbols(): symbols become waypoints "SYM n"
         */
        private void readSymbols() throws EOFException, InvalidTopoException {
            if (!findBlock(SYMBOLS_BLOCK))
                return;

            // per symbol: 2 unknown, longitude, latitude
            int symbolCount = readLength(10);
            for (int i = 0; i < symbolCount; i++) {
                readInt();
                readInt();
                int lon = input.readInt32();
                int lat = input.readInt32();
                Point symbol = new Point(lat, lon);
                symbol.shortName = "SYM " + (i + 1);
                waypoints.add(symbol);
            }
        }

        /**
         * tpo_process_text_labels(): text labels become waypoints "TXT n" with their text
         */
        private void readTextLabels() throws EOFException, InvalidTopoException {
            if (!findBlock(TEXT_LABELS_BLOCK))
                return;

            // per label: 2 unknown, longitude, latitude, 16 unknown bytes, text length
            int labelCount = readLength(27);
            for (int i = 0; i < labelCount; i++) {
                readInt();
                readInt();
                int lon = input.readInt32();
                int lat = input.readInt32();
                Point label = new Point(lat, lon);
                label.shortName = "TXT " + (i + 1);
                input.skip(16);

                int textLength = readLength(1);
                if (textLength > 0)
                    label.description = readString(textLength);
                waypoints.add(label);
            }
        }
    }

    private static boolean isScale(byte[] buffer, int index) {
        return index + 3 < buffer.length && buffer[index + 3] == 0 && buffer[index + 2] == 0;
    }

    private static int readInt32(byte[] buffer, int index) throws InvalidTopoException {
        if (index + 4 > buffer.length)
            throw new InvalidTopoException("Track ends within a coordinate at byte " + index);
        return (buffer[index] & 0xFF) | (buffer[index + 1] & 0xFF) << 8 |
                (buffer[index + 2] & 0xFF) << 16 | (buffer[index + 3] & 0xFF) << 24;
    }

    private enum Mode {CheckLonScale, CheckLatScale, Check0x88Tag, ScaleOneByte, GetFullPoint}

    /**
     * The 2020 track algorithm of tpo_process_tracks(): full points of 4 bytes longitude and 4 bytes latitude,
     * optional longitude and latitude scales whose upper two bytes are zero, and single-byte deltas against
     * the scales; 0x88 announces a full point, 0x00 ends a run of deltas.
     */
    static List<Point> decodeTrack(byte[] buffer) throws InvalidTopoException {
        List<Point> result = new ArrayList<>();
        // set repeatedly, they retain their value between settings
        int latScale = 0, lonScale = 0;
        int lat = 0, lon = 0;
        Mode mode = Mode.GetFullPoint;

        for (int index = 0; index < buffer.length; ) {
            switch (mode) {
                case GetFullPoint -> {
                    lon = readInt32(buffer, index);
                    index += 4;
                    lat = readInt32(buffer, index);
                    index += 4;
                    result.add(new Point(lat, lon));
                    // after a full point: scaling, 0x88 for another full point or a single byte to be scaled
                    mode = isScale(buffer, index) ? Mode.CheckLonScale : Mode.Check0x88Tag;
                }
                case CheckLonScale -> {
                    if (isScale(buffer, index)) {
                        lonScale = readInt32(buffer, index);
                        index += 4;
                        mode = Mode.CheckLatScale;
                    } else
                        mode = Mode.Check0x88Tag;
                }
                case CheckLatScale -> {
                    if (isScale(buffer, index)) {
                        latScale = readInt32(buffer, index);
                        index += 4;
                    }
                    mode = Mode.Check0x88Tag;
                }
                case Check0x88Tag -> {
                    if ((buffer[index] & 0xFF) == FULL_POINT_TAG) {
                        index++;
                        mode = Mode.GetFullPoint;
                    } else
                        mode = Mode.ScaleOneByte;
                }
                case ScaleOneByte -> {
                    int value = buffer[index] & 0xFF;
                    if (value == END_SCALE_TAG) {
                        index++;
                        mode = isScale(buffer, index) ? Mode.CheckLonScale : Mode.GetFullPoint;
                    } else if (value == FULL_POINT_TAG) {
                        index++;
                        mode = Mode.GetFullPoint;
                    } else {
                        if (latScale == 0 || lonScale == 0)
                            throw new InvalidTopoException("Found bad scales lonscale=" + lonScale + " latscale=" + latScale);
                        lon += lonScale * SCALE_MULTIPLIERS[value >> 4];
                        lat += latScale * SCALE_MULTIPLIERS[value & 0xf];
                        index++;
                        result.add(new Point(lat, lon));
                    }
                }
            }
        }
        return result;
    }

    /**
     * Waypoint::NormalizePosition() as GPSBabel's waypt_add(), route_add_wpt() and track_add_wpt() apply it.
     */
    static void normalizePosition(Point point) {
        if (point.latitude < -90.0 || point.latitude > 90.0) {
            boolean flipLongitude = false;
            point.latitude = Math.IEEEremainder(point.latitude, 360.0);
            if (point.latitude < -90.0) {
                point.latitude = -180.0 - point.latitude;
                flipLongitude = true;
            } else if (point.latitude > 90.0) {
                point.latitude = 180.0 - point.latitude;
                flipLongitude = true;
            }

            if (flipLongitude) {
                if (point.longitude < 0.0)
                    point.longitude += 180.0;
                else
                    point.longitude -= 180.0;
            }
        }

        if (point.longitude < -180.0 || point.longitude >= 180.0) {
            point.longitude = Math.IEEEremainder(point.longitude, 360.0);
            if (Double.compare(point.longitude, 180.0) == 0)
                point.longitude = -180.0;
        }
    }

    /**
     * The text as RouteConverter read it from GPSBabel's GPX: the GPX writer's XmlTextCodec replaces the control
     * characters that XML forbids with a space, the XML parser normalizes line ends to a line feed.
     */
    static String asGpxText(String string) {
        StringBuilder result = new StringBuilder(string.length());
        for (int i = 0; i < string.length(); i++) {
            char c = string.charAt(i);
            if (c == '\r') {
                result.append('\n');
                if (i + 1 < string.length() && string.charAt(i + 1) == '\n')
                    i++;
            } else if (c < 0x20 && c != '\t' && c != '\n')
                result.append(' ');
            else
                result.append(c);
        }
        return result.toString();
    }

    private GpxPosition createPosition(Point point) {
        normalizePosition(point);
        // the GPX that RouteConverter reads delivers the shortname as <name> and the description as <desc>;
        // GPSBabel writes no element for an empty string
        String description = asDescription(emptyToNull(point.shortName), emptyToNull(point.description));
        return new GpxPosition(asGpxCoordinate(point.longitude), asGpxCoordinate(point.latitude),
                asGpxElevation(point.elevation), null, null, description);
    }

    private static String emptyToNull(String string) {
        return string == null || string.isEmpty() ? null : string;
    }

    private static double asGpxCoordinate(double value) {
        return new BigDecimal(value).setScale(GPX_COORDINATE_DIGITS, HALF_EVEN).doubleValue();
    }

    private static Double asGpxElevation(Double elevation) {
        if (elevation == null)
            return null;
        return new BigDecimal(elevation).setScale(GPX_ELEVATION_DIGITS, HALF_EVEN).doubleValue();
    }
}
