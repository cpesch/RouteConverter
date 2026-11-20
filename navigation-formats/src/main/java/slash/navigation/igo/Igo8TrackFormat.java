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
import slash.navigation.gpsbabel.LittleEndianInput;
import slash.navigation.gpsbabel.LittleEndianOutput;
import slash.navigation.gpx.Gpx10Format;
import slash.navigation.gpx.GpxPosition;
import slash.navigation.gpx.GpxRoute;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static java.lang.Math.abs;
import static java.lang.Math.floorDiv;
import static java.math.RoundingMode.HALF_EVEN;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static slash.common.type.CompactCalendar.fromMillis;
import static slash.navigation.base.RouteCharacteristics.Track;
import static slash.navigation.common.NavigationConversion.formatPosition;

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
    private static final String TITLE = "Title";
    private static final String DESCRIPTION = "Description";

    public String getExtension() {
        return ".trk";
    }

    public String getName() {
        return "iGO8 Track (*" + getExtension() + ")";
    }

    public boolean isSupportsMultipleRoutes() {
        return false;
    }

    protected boolean isValidRoute(GpxRoute route) {
        // is really greedy in parsing the data of NetS files
        double length = route.getDistance();
        double distancePerPosition = length / route.getPositionCount();
        return distancePerPosition < 1000000.0;
    }

    // reading

    protected List<GpxRoute> readRoutes(InputStream source) throws IOException {
        byte[] data = source.readAllBytes();
        if (data.length < POINTS_OFFSET)
            return emptyList();

        LittleEndianInput input = new LittleEndianInput(data);
        input.seek(TOTAL_FILE_SIZE_OFFSET);
        long totalFileSize = input.readUInt32();
        if (totalFileSize < POINTS_OFFSET || totalFileSize > data.length ||
                (totalFileSize - POINTS_OFFSET) % POINT_SIZE != 0)
            return emptyList();

        int count = (int) ((totalFileSize - POINTS_OFFSET) / POINT_SIZE);
        List<GpxPosition> positions = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            long unixTime = input.readUInt32();
            double longitude = input.readInt32() / COORDINATE_FACTOR;
            double latitude = input.readInt32() / COORDINATE_FACTOR;
            if (abs(longitude) > 180.0 || abs(latitude) > 90.0)
                return emptyList();
            // GPSBabel 1.5.4 writes the creation time into GPX only if it is not zero
            positions.add(new GpxPosition(asGpxCoordinate(longitude), asGpxCoordinate(latitude), null, null,
                    unixTime != 0 ? fromMillis(unixTime * 1000) : null, null));
        }

        // an empty track is dropped by the GPX reader
        if (positions.isEmpty())
            return emptyList();
        return singletonList(new GpxRoute(new Gpx10Format(), Track, null, null, positions));
    }

    // GPSBabel writes coordinates to GPX with %0.9f
    private static double asGpxCoordinate(double value) {
        return new BigDecimal(value).setScale(GPX_COORDINATE_DIGITS, HALF_EVEN).doubleValue();
    }

    // writing

    public void write(GpxRoute route, OutputStream target, int startIndex, int endIndex) throws IOException {
        LittleEndianOutput output = new LittleEndianOutput();
        writeHeader(output);

        long inventedTime = 1;
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
            // GPSBabel 1.5.4 treats a zero creation time as missing
            if (time == 0)
                time = inventedTime++;

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

    private void writeHeader(LittleEndianOutput output) {
        output.writeUInt32(UNKNOWN_1);
        output.writeUInt32(UNKNOWN_2);
        output.writeUInt32(UNKNOWN_3);
        output.writeUInt32(DEFAULT_TRACK_NUMBER);
        output.writeUInt32(UNKNOWN_4);

        byte[] description = new byte[DESCRIPTION_BLOCK_SIZE];
        // the title leaves room for the terminator of the description that follows it
        int position = printUnicode(description, 0, DESCRIPTION_BLOCK_SIZE - 2, TITLE);
        printUnicode(description, position, DESCRIPTION_BLOCK_SIZE - position, DESCRIPTION);
        output.writeBytes(description);
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
        for (int i = 0; i < count; i++) {
            char c = string.charAt(i);
            destination[offset + i * 2] = (byte) c;
            destination[offset + i * 2 + 1] = (byte) (c >> 8);
        }
        // terminator is already zero
        return (count + 1) * 2;
    }
}
