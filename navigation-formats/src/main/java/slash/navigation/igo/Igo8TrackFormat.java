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

    Copyright (C) 2008 Dustin Johnson, Dustin@Dustinj.us
    Translated to Java from GPSBabel igo8.cc (gpsbabel_1_8_0) for RouteConverter, 2026; modified.
*/

package slash.navigation.igo;

import slash.navigation.base.GpxBackedNavigationFormat;
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
import java.util.ArrayList;
import java.util.List;

import static java.lang.Math.abs;
import static java.lang.Math.floorDiv;
import static java.math.RoundingMode.HALF_EVEN;
import static java.nio.charset.StandardCharsets.UTF_16LE;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static slash.common.type.CompactCalendar.fromMillis;
import static slash.navigation.base.RouteCharacteristics.Track;
import static slash.navigation.common.NavigationConversion.formatPosition;
import static slash.navigation.gpsbabel.GpsBabelText.cleanField;

/**
 * Reads and writes iGO8 Track (.trk) files.
 *
 * @author Christian Pesch
 */

public class Igo8TrackFormat extends GpxBackedNavigationFormat {
    private static final int ID_BLOCK_SIZE = 20;
    private static final int DESCRIPTION_BLOCK_SIZE = 256;
    private static final int HEADER_SIZE = ID_BLOCK_SIZE + DESCRIPTION_BLOCK_SIZE;
    private static final int INFORMATION_BLOCK_SIZE = 12;
    private static final int POINTS_OFFSET = HEADER_SIZE + INFORMATION_BLOCK_SIZE;
    private static final int POINT_SIZE = 12;
    private static final int TOTAL_FILE_SIZE_OFFSET = HEADER_SIZE + 8;
    private static final double COORDINATE_FACTOR = 0x800000;
    private static final int GPX_COORDINATE_DIGITS = 9;

    private static final int UNKNOWN_1 = 0x0000029B;
    private static final int UNKNOWN_2 = 0x000003E7;
    private static final int UNKNOWN_3 = 0x00000003;
    private static final int DEFAULT_TRACK_NUMBER = 0x00000010;
    private static final int UNKNOWN_4 = 0x00000001;
    // what GPSBabel writes without its title and description options
    private static final String TITLE = "Title";
    private static final String DESCRIPTION = "Description";
    private static final long MAXIMUM_TIME = 0xFFFFFFFFL;

    public String getExtension() {
        return ".trk";
    }

    public String getName() {
        return "iGO8 Track (*" + getExtension() + ")";
    }

    public boolean isSupportsMultipleRoutes() {
        return false;
    }

    // a file without the ID block of iGO8 is only taken if its points are not too far apart: without
    // the magic the reader is really greedy in parsing the data of NetS files
    private static boolean isPlausibleWithoutMagic(GpxRoute route) {
        double length = route.getDistance();
        double distancePerPosition = length / route.getPositionCount();
        return distancePerPosition < 1000000.0;
    }

    // reading

    /**
     * The ID block of every known iGO8 track starts with these two words; files without them are still
     * read if their points pass {@link #isPlausibleWithoutMagic(GpxRoute)}, in case other iGO versions
     * write other constants.
     */
    static boolean hasMagic(byte[] data) {
        if (data.length < 8)
            return false;
        LittleEndianInput input = new LittleEndianInput(data);
        try {
            return input.readInt32() == UNKNOWN_1 && input.readInt32() == UNKNOWN_2;
        } catch (EOFException e) {
            return false;
        }
    }

    protected List<GpxRoute> readRoutes(InputStream source) throws IOException {
        byte[] data = source.readAllBytes();
        boolean magic = hasMagic(data);
        if (data.length < POINTS_OFFSET) {
            if (magic)
                throw new UnreadableFileException("iGO8 track ends in its header after " + data.length + " bytes");
            return emptyList();
        }

        LittleEndianInput input = new LittleEndianInput(data);
        input.seek(TOTAL_FILE_SIZE_OFFSET);
        long totalFileSize = input.readUInt32();
        if (totalFileSize < POINTS_OFFSET) {
            if (magic)
                throw new UnreadableFileException("iGO8 track is damaged: its size field says " + totalFileSize +
                        " bytes, the header alone has " + POINTS_OFFSET);
            return emptyList();
        }
        if (totalFileSize > data.length || (totalFileSize - POINTS_OFFSET) % POINT_SIZE != 0) {
            // without the ID block only a coherent size field is evidence enough
            if (!magic)
                return emptyList();
            // igo8.cc reads (size - 288) / 12 points until the end of the file: a cut file or an odd size field
            // keeps its complete points (D19, audit 2026-11-27)
            totalFileSize = Math.min(totalFileSize, data.length);
        }

        int count = (int) ((totalFileSize - POINTS_OFFSET) / POINT_SIZE);
        List<GpxPosition> positions = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            long unixTime = input.readUInt32();
            double longitude = input.readInt32() / COORDINATE_FACTOR;
            double latitude = input.readInt32() / COORDINATE_FACTOR;
            if (abs(longitude) > 180.0 || abs(latitude) > 90.0) {
                if (magic)
                    throw new UnreadableFileException("iGO8 track is damaged: point " + (i + 1) + " is out of range");
                return emptyList();
            }
            // GPSBabel 1.5.4 writes the creation time into GPX only if it is not zero
            positions.add(new GpxPosition(asGpxCoordinate(longitude), asGpxCoordinate(latitude), null, null,
                    unixTime != 0 ? fromMillis(unixTime * 1000) : null, null));
        }

        // an empty track is dropped by the GPX reader
        if (positions.isEmpty())
            return emptyList();
        GpxRoute route = new GpxRoute(new Gpx10Format(), Track, readTitle(data), null, positions);
        if (!magic && !isPlausibleWithoutMagic(route))
            return emptyList();
        return singletonList(route);
    }

    /**
     * The title of the header or null if it is empty or what GPSBabel writes without its title option
     * (D25, audit 2026-11-27). The description is not read: iGO leaves the rest of a longer, earlier title behind
     * the terminator of the title.
     */
    static String readTitle(byte[] data) {
        int titleEnd = findTerminator(data, ID_BLOCK_SIZE, HEADER_SIZE);
        String title = stripIllegalXmlCharacters(new String(data, ID_BLOCK_SIZE, titleEnd - ID_BLOCK_SIZE, UTF_16LE)).trim();
        return title.isEmpty() || title.equals(TITLE) ? null : title;
    }

    // the index of the UTF-16LE NUL terminator of the string at offset or the limit
    private static int findTerminator(byte[] data, int offset, int limit) {
        for (int i = offset; i + 1 < limit; i += 2) {
            if (data[i] == 0 && data[i + 1] == 0)
                return i;
        }
        return limit;
    }

    // GPSBabel writes coordinates to GPX with %0.9f
    private static double asGpxCoordinate(double value) {
        return new BigDecimal(value).setScale(GPX_COORDINATE_DIGITS, HALF_EVEN).doubleValue();
    }

    // writing

    public void write(GpxRoute route, OutputStream target, int startIndex, int endIndex) throws IOException {
        LittleEndianOutput output = new LittleEndianOutput();
        writeHeader(output, title(route), description(route));

        long previousTime = 0;
        int pointCount = 0;
        List<GpxPosition> positions = route.getPositions();
        for (int i = startIndex; i < endIndex; i++) {
            GpxPosition position = positions.get(i);
            // the GPX written for GPSBabel skips positions without valid coordinates
            BigDecimal longitude = formatPosition(position.getLongitude());
            BigDecimal latitude = formatPosition(position.getLatitude());
            if (longitude == null || latitude == null)
                continue;

            long time = position.getTime() != null ? floorDiv(position.getTime().getTimeInMillis(), 1000L) : 0;
            // GPSBabel 1.5.4 treats a zero creation time as missing and invents 1, 2, 3, ... for missing times;
            // continuing from the time before keeps the track in order (D18, audit 2026-11-27)
            if (time == 0)
                time = previousTime + 1;
            // the time field is an unsigned 32 bit number of seconds since 1970, which GPSBabel lets wrap around
            time = Math.max(1, Math.min(MAXIMUM_TIME, time));
            previousTime = time;

            // the Information Block starts with the time of the first point
            if (pointCount == 0) {
                output.writeUInt32(time);
                output.writeUInt32(0);
                output.writeUInt32(0);
            }

            output.writeUInt32(time);
            output.writeInt32(floatToInt(longitude.doubleValue() * COORDINATE_FACTOR));
            output.writeInt32(floatToInt(latitude.doubleValue() * COORDINATE_FACTOR));
            pointCount++;
        }

        if (pointCount == 0) {
            // GPSBabel seeks past the end and patches the size, leaving a zero-filled Information Block
            output.writeUInt32(0);
            output.writeUInt32(0);
            output.writeUInt32(0);
        }
        output.patchInt32(TOTAL_FILE_SIZE_OFFSET, output.size());
        target.write(output.toByteArray());
    }

    private static int floatToInt(double value) {
        return (int) (value + (value < 0 ? -0.5 : 0.5));
    }

    // the title is the name of the route (D25, audit 2026-11-27)
    private static String title(GpxRoute route) {
        String name = route.getName() != null ? cleanField(route.getName(), "").trim() : "";
        return name.isEmpty() ? TITLE : name;
    }

    // the description is the first line of the description of the route (D25, audit 2026-11-27)
    private static String description(GpxRoute route) {
        List<String> description = route.getDescription();
        if (description != null) {
            for (String line : description) {
                String text = line != null ? cleanField(line, "").trim() : "";
                if (!text.isEmpty())
                    return text;
            }
        }
        return DESCRIPTION;
    }

    private void writeHeader(LittleEndianOutput output, String title, String description) {
        output.writeUInt32(UNKNOWN_1);
        output.writeUInt32(UNKNOWN_2);
        output.writeUInt32(UNKNOWN_3);
        output.writeUInt32(DEFAULT_TRACK_NUMBER);
        output.writeUInt32(UNKNOWN_4);

        byte[] block = new byte[DESCRIPTION_BLOCK_SIZE];
        // the title leaves room for the terminator of the description that follows it
        int position = printUnicode(block, 0, DESCRIPTION_BLOCK_SIZE - 2, title);
        printUnicode(block, position, DESCRIPTION_BLOCK_SIZE - position, description);
        output.writeBytes(block);
    }

    /**
     * Writes as many UTF-16LE code units of the string as fit into maxLength bytes while leaving room for the
     * NUL terminator, which is written as well.
     *
     * @return the number of bytes written
     */
    private static int printUnicode(byte[] destination, int offset, int maxLength, String string) {
        int maxChars = maxLength / 2;
        if (maxChars < 1)
            throw new IllegalArgumentException("iGO8 header overflow");
        int count = Math.min(string.length(), maxChars - 1);
        // a surrogate pair is not cut in two
        if (count > 0 && count < string.length() && Character.isHighSurrogate(string.charAt(count - 1)))
            count--;
        for (int i = 0; i < count; i++) {
            char c = string.charAt(i);
            destination[offset + i * 2] = (byte) c;
            destination[offset + i * 2 + 1] = (byte) (c >> 8);
        }
        // terminator is already zero
        return (count + 1) * 2;
    }
}
