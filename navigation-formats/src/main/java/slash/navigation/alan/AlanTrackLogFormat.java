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

    Copyright (C) 2007  Gunar Megger, 0xff@quantentunnel.de
    Copyright (C) 2002-2014 Robert Lipe, robertlipe+source@gpsbabel.org
    Translated to Java from GPSBabel alan.cc (gpsbabel_1_7_0) for RouteConverter, 2026; modified.
*/

package slash.navigation.alan;

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
import java.util.Arrays;
import java.util.List;

import static java.lang.Math.min;
import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static java.util.Collections.emptyList;
import static slash.common.io.Transfer.trim;
import static slash.navigation.alan.Map500.asGpxCoordinate;
import static slash.navigation.alan.Map500.asGpxSpeed;
import static slash.navigation.alan.Map500.deg2pt;
import static slash.navigation.alan.Map500.fromLabel;
import static slash.navigation.alan.Map500.hgt2m;
import static slash.navigation.alan.Map500.m2hgt;
import static slash.navigation.alan.Map500.mps2sp;
import static slash.navigation.alan.Map500.pt2deg;
import static slash.navigation.alan.Map500.sp2mps;
import static slash.navigation.base.RouteCharacteristics.Track;
import static slash.navigation.common.NavigationConversion.formatElevation;
import static slash.navigation.common.NavigationConversion.formatPosition;
import static slash.navigation.common.NavigationConversion.formatSpeed;
import static slash.navigation.common.UnitConversion.kmhToMs;
import static slash.navigation.common.UnitConversion.msToKmh;

/**
 * Reads and writes Alan Map 500 Tracklogs (.trl) files.
 *
 * @author Christian Pesch
 */

public class AlanTrackLogFormat extends GpxBackedNavigationFormat {
    static final int MAXTRK = 8;
    static final int MAXPTINTRK = 2500;
    static final int TRK_NAME_LEN = 12;
    static final int TRK_COMMENT_LEN = 13;

    // struct sizes of alan.cc on the map500 (little-endian, natural alignment)
    static final int TRKHDR_SIZE = 2 + 2 + TRK_NAME_LEN + TRK_COMMENT_LEN + 3 + 4 + 4 + 4;  // 44
    static final int LOGHDR_SIZE = 4 + 2 + 2 + 4 + 4 + MAXTRK * TRKHDR_SIZE;                 // 368
    static final int TRKLOG_SIZE = MAXPTINTRK * (4 + 4) + MAXPTINTRK * (2 + 2);              // 30000
    // trl_write(): two tracklogs per 64 KiB block, then the log header in a 4 KiB block
    static final int BLOCK_SIZE = 0x10000;
    static final int LOGHDR_OFFSET = BLOCK_SIZE * MAXTRK / 2;                                // 0x40000
    static final int LOGHDR_BLOCK_SIZE = 0x1000;
    static final int TRLDATA_FILE_SIZE = LOGHDR_OFFSET + LOGHDR_BLOCK_SIZE;                  // 266240

    static final long TRL_HDR_ID = 0x38CB1200L;
    private static final int TRK_USED = 1;
    private static final int TRK_UNUSED = 0;
    private static final int TRK_HIDE = 0;
    private static final int TRK_FILL = 1;
    // trl_write() leaves no track selected and no date in the log header
    private static final short LOGHDR_NUM = -1;
    private static final short LOGHDR_NEXT = -1;
    private static final int LOGHDR_DATE = 0;
    private static final int LOGHDR_TIME = 0;

    public String getExtension() {
        return ".trl";
    }

    public String getName() {
        return "Alan Map 500 Tracklog (*" + getExtension() + ")";
    }

    public boolean isSupportsMultipleRoutes() {
        return false;
    }

    // reading

    private static int trackLogOffset(int track) {
        return BLOCK_SIZE * (track / 2) + TRKLOG_SIZE * (track % 2);
    }

    protected List<GpxRoute> readRoutes(InputStream source) throws IOException {
        // like alan.cc trl_read(), which reads the tracklogs and the log header at their offsets only
        byte[] data = source.readNBytes(LOGHDR_OFFSET + LOGHDR_SIZE);
        if (data.length != LOGHDR_OFFSET + LOGHDR_SIZE)
            return emptyList();

        LittleEndianInput input = new LittleEndianInput(data);
        input.seek(LOGHDR_OFFSET);
        if (input.readUInt32() != TRL_HDR_ID)
            return emptyList();

        List<GpxRoute> result = new ArrayList<>();
        for (int i = 0; i < MAXTRK; i++) {
            int offset = LOGHDR_OFFSET + 16 + i * TRKHDR_SIZE;
            input.seek(offset);
            int totalpt = input.readInt16();
            input.seek(offset + 4 + TRK_NAME_LEN + TRK_COMMENT_LEN + 3);
            if (input.readUInt32() == TRK_UNUSED)
                continue;

            String name = stripIllegalXmlCharacters(trim(fromLabel(data, offset + 4, TRK_NAME_LEN, true)));
            String comment = stripIllegalXmlCharacters(trim(fromLabel(data, offset + 4 + TRK_NAME_LEN, TRK_COMMENT_LEN, true)));

            List<GpxPosition> positions = new ArrayList<>();
            int trackLog = trackLogOffset(i);
            // alan.cc reads behind the tracklog for more than MAXPTINTRK points
            for (int j = 0; j < min(totalpt, MAXPTINTRK); j++) {
                input.seek(trackLog + j * 8);
                int x = input.readInt32();
                int y = input.readInt32();
                input.seek(trackLog + MAXPTINTRK * 8 + j * 4);
                int speed = input.readInt16();
                int height = input.readInt16();
                // a negative speed is a bad speed
                Double kmh = speed >= 0 ? msToKmh(asGpxSpeed(sp2mps(speed))) : null;
                positions.add(new GpxPosition(asGpxCoordinate(pt2deg(x)), asGpxCoordinate(-pt2deg(y)), hgt2m(height),
                        kmh, null, null));
            }
            // the GPX reader drops a track without points
            if (!positions.isEmpty())
                result.add(new GpxRoute(new Gpx10Format(), Track, name, asDescription(comment), positions));
        }
        return result;
    }

    // writing

    public void write(GpxRoute route, OutputStream target, int startIndex, int endIndex) throws IOException {
        TrlData trl = new TrlData();
        addTrack(trl, route, route.getPositions().subList(startIndex, endIndex));
        target.write(trl.toBytes());
    }

    // alan.cc trl_track_hdr(), trl_track_wpt() and trl_track_tlr()
    private void addTrack(TrlData trl, GpxRoute route, List<GpxPosition> positions) throws IOException {
        int idx = 0;
        while (idx < MAXTRK && trl.trkhdr[idx].occupied != TRK_UNUSED)
            idx++;
        if (idx >= MAXTRK)
            throw new IOException("Can't store more than " + MAXTRK + " tracklogs");
        TrkHdr trkhdr = trl.trkhdr[idx];

        // alan.cc copies the ISO-8859-1 bytes of the name and the description
        String name = stripIllegalXmlCharacters(asRouteName(route.getName()));
        if (name != null)
            strncpy(trkhdr.name, name.getBytes(ISO_8859_1), TRK_NAME_LEN - 1);
        if (trkhdr.name[0] == 0)
            copy(trkhdr.name, String.format(java.util.Locale.ROOT, "T%03d", idx).getBytes(ISO_8859_1));
        trkhdr.name[TRK_NAME_LEN - 1] = 0;

        String description = stripIllegalXmlCharacters(trim(asDescription(route.getDescription())));
        if (description != null) {
            byte[] bytes = description.getBytes(ISO_8859_1);
            strncpy(trkhdr.comment, bytes, TRK_COMMENT_LEN - 1);
            int length = strlen(bytes);
            if (length < TRK_COMMENT_LEN - 1)
                Arrays.fill(trkhdr.comment, length, TRK_COMMENT_LEN - 1, (byte) ' ');
        }
        trkhdr.comment[TRK_COMMENT_LEN - 1] = 0;

        trkhdr.occupied = TRK_USED;
        trkhdr.totalpt = 0;
        trkhdr.next = 0;

        TrkLog trklog = trl.trklog[idx];
        for (GpxPosition position : positions) {
            // the GPX written for GPSBabel skips positions without valid coordinates
            BigDecimal longitude = formatPosition(position.getLongitude());
            BigDecimal latitude = formatPosition(position.getLatitude());
            if (longitude == null || latitude == null)
                continue;
            if (trkhdr.totalpt >= MAXPTINTRK)
                throw new IOException("Can't store more than " + MAXPTINTRK + " points per track");

            int logIdx = trkhdr.next;
            trklog.x[logIdx] = deg2pt(longitude.doubleValue());
            trklog.y[logIdx] = deg2pt(-latitude.doubleValue());
            // the GPX written for GPSBabel has the speed in m/s and the elevation with the fraction digits of RouteConverter
            BigDecimal speed = position.getSpeed() != null ? formatSpeed(kmhToMs(position.getSpeed())) : null;
            if (speed != null)
                trklog.speed[logIdx] = mps2sp(speed.doubleValue());
            BigDecimal elevation = formatElevation(position.getElevation());
            if (elevation != null)
                trklog.height[logIdx] = m2hgt(elevation.doubleValue());

            trkhdr.totalpt++;
            trkhdr.next = trkhdr.totalpt;
        }

        if (trkhdr.totalpt == 0)
            trkhdr.occupied = TRK_UNUSED;
    }

    private String asDescription(List<String> strings) {
        if (strings == null)
            return null;
        return String.join(",\n", strings);
    }

    private static int strlen(byte[] bytes) {
        int length = 0;
        while (length < bytes.length && bytes[length] != 0)
            length++;
        return length;
    }

    // C's strncpy(): copies up to count bytes of the string and pads the rest of count with NUL bytes
    private static void strncpy(byte[] destination, byte[] source, int count) {
        int length = min(strlen(source), count);
        System.arraycopy(source, 0, destination, 0, length);
        Arrays.fill(destination, length, count, (byte) 0);
    }

    // C's sprintf(): copies the string and its terminator
    private static void copy(byte[] destination, byte[] source) {
        System.arraycopy(source, 0, destination, 0, source.length);
        destination[source.length] = 0;
    }

    // alan.cc struct trkhdr, initialized like trl_write() does
    private static class TrkHdr {
        short totalpt, next;
        final byte[] name = new byte[TRK_NAME_LEN];
        final byte[] comment = new byte[TRK_COMMENT_LEN];
        final byte[] reserved = new byte[3];
        int occupied = TRK_UNUSED, show = TRK_HIDE, fill = TRK_FILL;

        TrkHdr() {
            Arrays.fill(comment, (byte) ' ');
            comment[TRK_COMMENT_LEN - 1] = 0;
        }
    }

    // alan.cc struct trklog, filled with 0xff by trl_write()
    private static class TrkLog {
        final int[] x = new int[MAXPTINTRK];
        final int[] y = new int[MAXPTINTRK];
        final short[] speed = new short[MAXPTINTRK];
        final short[] height = new short[MAXPTINTRK];

        TrkLog() {
            Arrays.fill(x, -1);
            Arrays.fill(y, -1);
            Arrays.fill(speed, (short) -1);
            Arrays.fill(height, (short) -1);
        }
    }

    // alan.cc struct trldata
    private static class TrlData {
        final TrkHdr[] trkhdr = new TrkHdr[MAXTRK];
        final TrkLog[] trklog = new TrkLog[MAXTRK];

        TrlData() {
            for (int i = 0; i < MAXTRK; i++) {
                trkhdr[i] = new TrkHdr();
                trklog[i] = new TrkLog();
            }
        }

        private static void fill(LittleEndianOutput output, int count) {
            byte[] bytes = new byte[count];
            Arrays.fill(bytes, (byte) 0xff);
            output.writeBytes(bytes);
        }

        // trl_write(): two tracklogs and 0xff up to 64 KiB, then the log header and 0xff up to 4 KiB
        byte[] toBytes() {
            LittleEndianOutput output = new LittleEndianOutput();
            for (int i = 0; i < MAXTRK; i += 2) {
                writeTrackLog(output, trklog[i]);
                writeTrackLog(output, trklog[i + 1]);
                fill(output, BLOCK_SIZE - 2 * TRKLOG_SIZE);
            }
            output.writeUInt32(TRL_HDR_ID);
            output.writeInt16(LOGHDR_NUM);
            output.writeInt16(LOGHDR_NEXT);
            output.writeInt32(LOGHDR_DATE);
            output.writeInt32(LOGHDR_TIME);
            for (TrkHdr header : trkhdr) {
                output.writeInt16(header.totalpt);
                output.writeInt16(header.next);
                output.writeBytes(header.name);
                output.writeBytes(header.comment);
                output.writeBytes(header.reserved);
                output.writeUInt32(header.occupied);
                output.writeUInt32(header.show);
                output.writeUInt32(header.fill);
            }
            fill(output, LOGHDR_BLOCK_SIZE - LOGHDR_SIZE);
            return output.toByteArray();
        }

        private static void writeTrackLog(LittleEndianOutput output, TrkLog trklog) {
            for (int j = 0; j < MAXPTINTRK; j++) {
                output.writeInt32(trklog.x[j]);
                output.writeInt32(trklog.y[j]);
            }
            for (int j = 0; j < MAXPTINTRK; j++) {
                output.writeInt16(trklog.speed[j]);
                output.writeInt16(trklog.height[j]);
            }
        }
    }
}
