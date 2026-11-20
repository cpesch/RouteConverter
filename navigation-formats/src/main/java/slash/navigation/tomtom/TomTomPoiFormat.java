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

    Copyright (C) 2005  Ronald Parker (babeltomtom@parkrrrr.com) and
                        Robert Lipe (robertlipe+source@gpsbabel.org)

    Translated to Java from GPSBabel tomtom.cc (gpsbabel_1_8_0) for RouteConverter, 2026; modified.
*/

package slash.navigation.tomtom;

import slash.navigation.base.GpxBackedNavigationFormat;
import slash.navigation.gpsbabel.LittleEndianInput;
import slash.navigation.gpsbabel.LittleEndianOutput;
import slash.navigation.gpx.Gpx10Format;
import slash.navigation.gpx.GpxPosition;
import slash.navigation.gpx.GpxRoute;
import slash.navigation.itn.TomTomRouteFormat;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.prefs.Preferences;

import static java.util.Collections.singletonList;
import static slash.common.io.Transfer.trim;
import static slash.common.io.Transfer.trimLineFeeds;
import static slash.navigation.base.RouteCharacteristics.Waypoints;
import static slash.navigation.common.NavigationConversion.formatPosition;

/**
 * Reads and writes Tom Tom POI (.ov2) files.
 * <p>
 * The whole description of a position is the text of the POI, it is not split into name and description.
 * A {@code >} in the text is TomTom's separator of name and phone number; like GPSBabel the text is kept as a whole.
 * For the icon on the device a 22x22, 16-colour {@code .bmp} with the same base name is needed.
 * <p>
 * Translated from GPSBabel tomtom.cc, which is based on a reverse-engineering of the format.
 *
 * @author Christian Pesch
 */

public class TomTomPoiFormat extends GpxBackedNavigationFormat {
    private static final Preferences preferences = Preferences.userNodeForPackage(TomTomRouteFormat.class);
    private static final Charset WINDOWS_1252 = Charset.forName("windows-1252");

    private static final int DELETED_RECORD = 0;
    private static final int BLOCK_HEADER_RECORD = 1;
    private static final int POI_RECORD = 2;
    private static final int EXTENDED_POI_RECORD = 3;
    private static final int COMPRESSED_RECORD_8 = 8;
    private static final int SKIPPED_RECORD_9 = 9;
    private static final int COMPRESSED_RECORD_24 = 24;
    private static final int SKIPPED_RECORD_25 = 25;
    private static final int DELETED_RECORD_100 = 100;

    private static final int MAXIMUM_RECORD_SIZE = 100000;
    private static final int MINIMUM_POI_RECORD_SIZE = 13;
    private static final int DELETED_HEADER_SIZE = 5;
    private static final int POI_LEAF_LIMIT = 20;
    private static final int MAXIMUM_TEXT_BYTES = 255;
    private static final long MAXIMUM_LONGITUDE = 18000000L;
    private static final long MAXIMUM_LATITUDE = 9000000L;

    public String getName() {
        return "Tom Tom POI (*" + getExtension() + ")";
    }

    public String getExtension() {
        return ".ov2";
    }

    public int getMaximumFileNameLength() {
        return preferences.getInt("maximumFileNameLength", 18);
    }

    public boolean isSupportsMultipleRoutes() {
        return false;
    }

    // reading

    private static class Poi {
        final double longitude, latitude;
        final String description;
        final byte[] text;

        Poi(double longitude, double latitude, String description) {
            this.longitude = longitude;
            this.latitude = latitude;
            this.description = description;
            this.text = encodeText(description);
        }
    }

    protected List<GpxRoute> readRoutes(InputStream source) throws IOException {
        List<String> texts = new ArrayList<>();
        List<double[]> coordinates = new ArrayList<>();
        if (!parse(source.readAllBytes(), texts, coordinates) || texts.isEmpty())
            return null;

        List<GpxPosition> positions = new ArrayList<>(texts.size());
        for (int i = 0; i < texts.size(); i++) {
            String text = stripIllegalXmlCharacters(texts.get(i));
            // GPSBabel passes the text on as <name> and <desc> of the GPX waypoint; for an empty text
            // GPSBabel 1.5.4 (waypt_add) synthesizes the name WPT%03d from the ordinal of the waypoint
            String description = trimLineFeeds(text.isEmpty() ? String.format("WPT%03d", i + 1) : text);
            positions.add(new GpxPosition(coordinates.get(i)[0], coordinates.get(i)[1], null, null, null, description));
        }
        return singletonList(new GpxRoute(new Gpx10Format(), Waypoints, null, null, positions));
    }

    /**
     * Parses the records of an .ov2 file like GPSBabel's data_read(), but returns false instead of a fatal error
     * for everything that is not an .ov2 file: other first record type than 0, 1, 2, 3 or 100, bad record sizes,
     * truncated records and coordinates outside of the world.
     */
    private boolean parse(byte[] data, List<String> texts, List<double[]> coordinates) {
        if (data.length == 0 || !isFirstRecordType(data[0] & 0xFF))
            return false;

        LittleEndianInput input = new LittleEndianInput(data);
        try {
            while (input.remaining() > 0) {
                int type = input.readUInt8();
                switch (type) {
                    case DELETED_RECORD:
                    case DELETED_RECORD_100:
                        // tomtom.cc: recsize = read_long(file_in) - 5; check_recsize(recsize) - the check applies
                        // to the payload size, not to the size field
                        int deletedSize = input.readInt32() - DELETED_HEADER_SIZE;
                        if (!isValidRecordSize(deletedSize))
                            return false;
                        input.skip(deletedSize);
                        break;
                    case BLOCK_HEADER_RECORD:
                        // a block header: size, maximum longitude and latitude, minimum longitude and latitude
                        for (int i = 0; i < 5; i++)
                            input.readInt32();
                        break;
                    case POI_RECORD:
                    case EXTENDED_POI_RECORD:
                        // a type 3 record is treated as type 2, further strings after the NUL are ignored
                        int size = input.readInt32();
                        if (!isValidRecordSize(size) || size < MINIMUM_POI_RECORD_SIZE)
                            return false;
                        int x = input.readInt32();
                        int y = input.readInt32();
                        if (Math.abs((long) x) > MAXIMUM_LONGITUDE || Math.abs((long) y) > MAXIMUM_LATITUDE)
                            return false;
                        byte[] text = input.readBytes(size - MINIMUM_POI_RECORD_SIZE);
                        coordinates.add(new double[]{x / 100000.0, y / 100000.0});
                        texts.add(decodeText(text));
                        break;
                    case COMPRESSED_RECORD_8:
                    case SKIPPED_RECORD_9:
                    case COMPRESSED_RECORD_24:
                    case SKIPPED_RECORD_25:
                        input.skip(input.readUInt8() + 6);
                        break;
                    default:
                        // GPSBabel only warns and continues without consuming anything: stop, keep what has been read
                        return true;
                }
            }
        } catch (EOFException e) {
            return false;
        }
        return true;
    }

    private static boolean isFirstRecordType(int type) {
        return type == DELETED_RECORD || type == BLOCK_HEADER_RECORD || type == POI_RECORD ||
                type == EXTENDED_POI_RECORD || type == DELETED_RECORD_100;
    }

    private static boolean isValidRecordSize(int size) {
        return size >= 0 && size <= MAXIMUM_RECORD_SIZE;
    }

    private static String decodeText(byte[] bytes) {
        int length = 0;
        while (length < bytes.length && bytes[length] != 0)
            length++;
        return new String(bytes, 0, length, WINDOWS_1252);
    }

    // writing

    private static byte[] encodeText(String description) {
        byte[] bytes = description.getBytes(WINDOWS_1252);
        // strlen() of the C string
        int length = 0;
        while (length < bytes.length && bytes[length] != 0)
            length++;
        // snprintf() into a buffer of 256 bytes
        return Arrays.copyOf(bytes, Math.min(length, MAXIMUM_TEXT_BYTES));
    }

    /**
     * GPSBabel's write_float_as_long(): {@code (long)(value + 0.500000000001)}. The cast truncates toward zero,
     * thus negative values are not rounded half up: -1.0000001 becomes 0, not -1. Kept for byte-stable files.
     */
    static long writeFloatAsLong(double value) {
        return (long) (value + 0.500000000001);
    }

    private static int compareDescriptions(Poi a, Poi b) {
        return Integer.signum(a.description.compareTo(b.description));
    }

    private static int compareLatitude(Poi a, Poi b) {
        double difference = a.latitude - b.latitude;
        if (difference < 0)
            return -1;
        if (difference != 0)
            return 1;
        if (a.longitude - b.longitude == 0)
            return compareDescriptions(a, b);
        return compareLongitude(a, b);
    }

    private static int compareLongitude(Poi a, Poi b) {
        double difference = a.longitude - b.longitude;
        if (difference < 0)
            return -1;
        if (difference != 0)
            return 1;
        if (a.latitude - b.latitude == 0)
            return compareDescriptions(a, b);
        return compareLatitude(a, b);
    }

    private static class Block {
        final int start, count;
        long size = 4 * 5 + 1; // header is 5 longs, 1 char
        double minLongitude, maxLongitude, minLatitude, maxLatitude;
        Block first, second;

        Block(int start, int count, double minLongitude, double maxLongitude, double minLatitude, double maxLatitude) {
            this.start = start;
            this.count = count;
            this.minLongitude = minLongitude;
            this.maxLongitude = maxLongitude;
            this.minLatitude = minLatitude;
            this.maxLatitude = maxLatitude;
        }
    }

    private static Block computeBlocks(Poi[] pois, int start, int count,
                                       double minLongitude, double maxLongitude, double minLatitude, double maxLatitude) {
        Block block = new Block(start, count, minLongitude, maxLongitude, minLatitude, maxLatitude);
        if (count < POI_LEAF_LIMIT) {
            for (int i = 0; i < count; i++) {
                // constant part of a POI is 3 longs and 1 char, plus the text bytes actually written and the NUL.
                // tomtom.cc counts description.length() (UTF-16 units of the uncut text) here while it writes at
                // most 255 bytes, so its block sizes are wrong for longer or non-BMP texts; this port counts what
                // it writes (maintainer preference 2026-11-19: correct behaviour over GPSBabel bugs; no golden
                // input has such a text, the byte-stable goldens are unchanged)
                block.size += 4 * 3 + 1;
                block.size += pois[start + i].text.length + 1;
            }
        } else {
            int half = count / 2;
            if ((maxLatitude - minLatitude) > (maxLongitude - minLongitude)) {
                Arrays.sort(pois, start, start + count, TomTomPoiFormat::compareLatitude);
                double split = pois[start + half].latitude;
                block.first = computeBlocks(pois, start, half, minLongitude, maxLongitude, minLatitude, split);
                block.second = computeBlocks(pois, start + half, count - half, minLongitude, maxLongitude, split, maxLatitude);
            } else {
                Arrays.sort(pois, start, start + count, TomTomPoiFormat::compareLongitude);
                double split = pois[start + half].longitude;
                block.first = computeBlocks(pois, start, half, minLongitude, split, minLatitude, maxLatitude);
                block.second = computeBlocks(pois, start + half, count - half, split, maxLongitude, minLatitude, maxLatitude);
            }
            block.size += block.first.size + block.second.size;
        }
        return block;
    }

    private static void writeBlocks(LittleEndianOutput output, Poi[] pois, Block block) {
        output.writeUInt8(BLOCK_HEADER_RECORD);
        output.writeInt32((int) block.size);
        output.writeInt32((int) writeFloatAsLong(block.maxLongitude * 100000));
        output.writeInt32((int) writeFloatAsLong(block.maxLatitude * 100000));
        output.writeInt32((int) writeFloatAsLong(block.minLongitude * 100000));
        output.writeInt32((int) writeFloatAsLong(block.minLatitude * 100000));
        if (block.first != null)
            writeBlocks(output, pois, block.first);
        if (block.second != null)
            writeBlocks(output, pois, block.second);
        if (block.first == null && block.second == null) {
            for (int i = 0; i < block.count; i++) {
                Poi poi = pois[block.start + i];
                output.writeUInt8(POI_RECORD);
                output.writeInt32(poi.text.length + 14);
                output.writeInt32((int) writeFloatAsLong(poi.longitude * 100000));
                output.writeInt32((int) writeFloatAsLong(poi.latitude * 100000));
                output.writeBytes(poi.text);
                output.writeUInt8(0);
            }
        }
    }

    public void write(GpxRoute route, OutputStream target, int startIndex, int endIndex) throws IOException {
        List<GpxPosition> positions = route.getPositions();
        List<Poi> list = new ArrayList<>();
        double minLongitude = 200, maxLongitude = -200, minLatitude = 200, maxLatitude = -200;
        for (int i = startIndex; i < endIndex; i++) {
            GpxPosition position = positions.get(i);
            if (position.getLongitude() == null || position.getLatitude() == null)
                continue;
            // the coordinates went through the GPX file with the precision RouteConverter writes it with
            double longitude = formatPosition(position.getLongitude()).doubleValue();
            double latitude = formatPosition(position.getLatitude()).doubleValue();
            String description = stripIllegalXmlCharacters(trim(position.getDescription()));
            list.add(new Poi(longitude, latitude, description != null ? description : ""));
            if (longitude > maxLongitude)
                maxLongitude = longitude;
            if (longitude < minLongitude)
                minLongitude = longitude;
            if (latitude > maxLatitude)
                maxLatitude = latitude;
            if (latitude < minLatitude)
                minLatitude = latitude;
        }

        Poi[] pois = list.toArray(new Poi[0]);
        Block blocks = computeBlocks(pois, 0, pois.length, minLongitude, maxLongitude, minLatitude, maxLatitude);
        LittleEndianOutput output = new LittleEndianOutput();
        writeBlocks(output, pois, blocks);
        target.write(output.toByteArray());
        target.flush();
    }
}
