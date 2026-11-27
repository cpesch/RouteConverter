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
*/

package slash.navigation.holux;

import org.junit.jupiter.api.Test;
import slash.navigation.base.BaseRoute;
import slash.navigation.base.NavigationFormat;
import slash.navigation.base.NavigationFormatParser;
import slash.navigation.base.NavigationFormatRegistry;
import slash.navigation.base.ParserContextImpl;
import slash.navigation.base.ParserResult;
import slash.navigation.base.UnreadableFileException;
import slash.navigation.gpx.GpxPosition;
import slash.navigation.gpx.GpxRoute;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import static java.nio.ByteOrder.LITTLE_ENDIAN;
import static java.nio.charset.StandardCharsets.US_ASCII;
import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static slash.navigation.base.RouteCharacteristics.Track;
import static slash.navigation.base.RouteCharacteristics.Waypoints;
import static slash.navigation.holux.MtkLoggerBinaryFormat.*;

public class MtkLoggerBinaryFormatTest {
    private static final int SECTOR = 0x10000;
    private static final int BASIC = bits(UTC, LATITUDE, LONGITUDE, HEIGHT);
    private static final int FULL = bits(UTC, VALID, LATITUDE, LONGITUDE, HEIGHT, SPEED, HEADING, PDOP, HDOP, VDOP, NSAT, RCR, MILLISECOND);
    private static final long TIME = 1200000000L;

    private final MtkLoggerBinaryFormat format = new MtkLoggerBinaryFormat();

    private static int bits(int... types) {
        int result = 0;
        for (int type : types)
            result |= 1 << type;
        return result;
    }

    /** the values of one record; fields not in the bitmask are not written */
    static class Rec {
        long utc = TIME;
        int valid = 0x0002;
        double latitude = 48.25, longitude = 11.5;
        float height = 512.5f;
        float speed = 36.0f, heading = 90.5f;
        int pdop = 250, hdop = 150, vdop = 200;
        int satellitesInView = 9, satellitesUsed = 7;
        int[] satelliteIds = {};
        int rcr = 0x0001, milliseconds = 0;
        boolean badChecksum, noStar;

        Rec utc(long utc) { this.utc = utc; return this; }
        Rec at(double latitude, double longitude) { this.latitude = latitude; this.longitude = longitude; return this; }
        Rec height(float height) { this.height = height; return this; }
        Rec valid(int valid) { this.valid = valid; return this; }
        Rec satellites(int used) { this.satellitesUsed = used; return this; }
        Rec satelliteIds(int... ids) { this.satelliteIds = ids; return this; }
        Rec rcr(int rcr) { this.rcr = rcr; return this; }
        Rec milliseconds(int milliseconds) { this.milliseconds = milliseconds; return this; }
        Rec badChecksum() { this.badChecksum = true; return this; }
        Rec noStar() { this.noStar = true; return this; }
    }

    static Rec rec() {
        return new Rec();
    }

    /** the fields of a record in the generic MTK layout: 8 byte double lat/lon, 4 byte float height */
    private static byte[] fields(int bitmask, Rec rec) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (int k = 0; k < 32; k++) {
            if ((bitmask & (1 << k)) == 0)
                continue;
            ByteBuffer field = ByteBuffer.allocate(8 + 32 * 8).order(LITTLE_ENDIAN);
            switch (k) {
                case UTC -> field.putInt((int) rec.utc);
                case VALID -> field.putShort((short) rec.valid);
                case LATITUDE -> field.putDouble(rec.latitude);
                case LONGITUDE -> field.putDouble(rec.longitude);
                case HEIGHT -> field.putFloat(rec.height);
                case SPEED -> field.putFloat(rec.speed);
                case HEADING -> field.putFloat(rec.heading);
                case PDOP -> field.putShort((short) rec.pdop);
                case HDOP -> field.putShort((short) rec.hdop);
                case VDOP -> field.putShort((short) rec.vdop);
                case NSAT -> field.put((byte) rec.satellitesInView).put((byte) rec.satellitesUsed);
                case SID -> {
                    // per satellite: id, in use flag, u16 satellites in view; then elevation, azimuth, SNR
                    int count = Math.max(1, rec.satelliteIds.length);
                    for (int i = 0; i < count; i++) {
                        int id = i < rec.satelliteIds.length ? rec.satelliteIds[i] : 0;
                        field.put((byte) id).put((byte) 1).putShort((short) rec.satelliteIds.length);
                        if (rec.satelliteIds.length > 0) {
                            if ((bitmask & (1 << ELEVATION)) != 0)
                                field.putShort((short) 45);
                            if ((bitmask & (1 << AZIMUTH)) != 0)
                                field.putShort((short) 180);
                            if ((bitmask & (1 << SNR)) != 0)
                                field.putShort((short) 40);
                        }
                    }
                }
                case ELEVATION, AZIMUTH, SNR -> {
                    continue; // written with SID
                }
                case RCR -> field.putShort((short) rec.rcr);
                case MILLISECOND -> field.putShort((short) rec.milliseconds);
                default -> throw new IllegalArgumentException("type " + k + " not supported by the test");
            }
            out.write(field.array(), 0, field.position());
        }
        return out.toByteArray();
    }

    /** a record in the generic MTK layout: fields, '*', XOR checksum of the fields */
    static byte[] record(int bitmask, Rec rec) {
        byte[] fields = fields(bitmask, rec);
        int checksum = 0;
        for (byte b : fields)
            checksum ^= b & 0xff;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(fields, 0, fields.length);
        if (!rec.noStar)
            out.write('*');
        out.write(rec.badChecksum ? checksum ^ 0x5a : checksum);
        return out.toByteArray();
    }

    /** a 16 byte separator record: AA x7, type, 4 data bytes, BB x4 */
    static byte[] info(int type, int value) {
        ByteBuffer buffer = ByteBuffer.allocate(16).order(LITTLE_ENDIAN);
        for (int i = 0; i < 7; i++)
            buffer.put((byte) 0xaa);
        buffer.put((byte) type).putInt(value);
        for (int i = 0; i < 4; i++)
            buffer.put((byte) 0xbb);
        return buffer.array();
    }

    static byte[] ascii(String string) {
        return string.getBytes(US_ASCII);
    }

    /** the 0x200 byte sector header: record count, bitmask, policy, period, distance, speed */
    static byte[] header(int bitmask, int policy, int period, int distance, int speed) {
        ByteBuffer buffer = ByteBuffer.allocate(0x200).order(LITTLE_ENDIAN);
        buffer.putShort((short) 0xffff).putInt(bitmask).putShort((short) policy)
                .putInt(period).putInt(distance).putInt(speed);
        return buffer.array();
    }

    static byte[] header(int bitmask) {
        return header(bitmask, 0x0104, 50, 0, 0);
    }

    /** a 64 KiB sector: header, data, trailing 0xFF fill */
    static byte[] sector(byte[] header, byte[]... data) {
        byte[] result = new byte[SECTOR];
        Arrays.fill(result, (byte) 0xff);
        System.arraycopy(header, 0, result, 0, header.length);
        int offset = header.length;
        for (byte[] bytes : data) {
            System.arraycopy(bytes, 0, result, offset, bytes.length);
            offset += bytes.length;
        }
        return result;
    }

    static byte[] concat(byte[]... parts) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (byte[] part : parts)
            out.write(part, 0, part.length);
        return out.toByteArray();
    }

    private List<GpxRoute> read(byte[] data) throws IOException {
        ParserContextImpl<GpxRoute> context = new ParserContextImpl<>();
        format.read(new ByteArrayInputStream(data), context);
        return context.getRoutes();
    }

    private GpxRoute readTrack(byte[] data) throws IOException {
        List<GpxRoute> routes = read(data);
        assertEquals(1, routes.size());
        GpxRoute route = routes.get(0);
        assertEquals(Track, route.getCharacteristics());
        return route;
    }

    private static List<String> descriptions(GpxRoute route) {
        return route.getPositions().stream().map(GpxPosition::getDescription).toList();
    }

    @Test
    public void nameAndCapabilities() {
        assertEquals("MTK Logger Binary (*.bin)", format.getName());
        assertEquals(".bin", format.getExtension());
        assertTrue(format.isSupportsReading());
        assertFalse(format.isSupportsWriting());
        assertFalse(format.isSupportsMultipleRoutes());
        try {
            format.write(null, new ByteArrayOutputStream(), 0, 0);
            fail("UnsupportedOperationException expected");
        } catch (UnsupportedOperationException e) {
            // expected
        }
    }

    @Test
    public void isRegisteredOnceRightAfterHoluxM241BinaryFormat() {
        List<? extends NavigationFormat<?>> formats = new NavigationFormatRegistry().getReadFormats();
        int holux = -1, mtk = -1, count = 0;
        for (int i = 0; i < formats.size(); i++) {
            if (formats.get(i).getClass() == HoluxM241BinaryFormat.class)
                holux = i;
            if (formats.get(i) instanceof MtkLoggerBinaryFormat) {
                mtk = i;
                count++;
            }
        }
        assertTrue(holux >= 0);
        assertEquals(holux + 1, mtk);
        assertEquals(1, count);
    }

    @Test
    public void readsRecordsOfASectorAsOneTrack() throws IOException {
        GpxRoute route = readTrack(sector(header(BASIC),
                record(BASIC, rec().at(48.7042084123, 9.8836384456).height(632.1f)),
                record(BASIC, rec().utc(TIME + 5).at(-33.75, -122.5).height(-12.5f))));

        assertEquals("track-1", route.getName());
        assertEquals(asList("Log every 5 sec", "0 m"), route.getDescription());
        assertEquals(asList("TP000001", "TP000002"), descriptions(route));

        GpxPosition first = route.getPosition(0);
        // 8 byte doubles, written by GPSBabel with 9 decimals to GPX
        assertEquals(48.704208412, first.getLatitude(), 0.0);
        assertEquals(9.883638446, first.getLongitude(), 0.0);
        // a 4 byte float, written by GPSBabel with 6 decimals to GPX
        assertEquals(632.099976, first.getElevation(), 0.0);
        assertEquals(TIME * 1000, first.getTime().getTimeInMillis());
        assertNull(first.getSpeed());
        assertNull(first.getHeading());
        assertNull(first.getHdop());
        assertNull(first.getSatellites());
        assertNull(first.getFixQuality());

        GpxPosition second = route.getPosition(1);
        assertEquals(-33.75, second.getLatitude(), 0.0);
        assertEquals(-122.5, second.getLongitude(), 0.0);
        assertEquals(-12.5, second.getElevation(), 0.0);
        assertEquals((TIME + 5) * 1000, second.getTime().getTimeInMillis());
    }

    @Test
    public void recordSizeAndFieldsFollowTheBitmask() throws IOException {
        GpxRoute route = readTrack(sector(header(FULL),
                record(FULL, rec().milliseconds(250)),
                record(FULL, rec().utc(TIME + 1).at(48.5, 11.75).satellites(14))));
        assertEquals(2, route.getPositionCount());

        GpxPosition first = route.getPosition(0);
        assertEquals(48.25, first.getLatitude(), 0.0);
        assertEquals(11.5, first.getLongitude(), 0.0);
        assertEquals(512.5, first.getElevation(), 0.0);
        assertEquals(36.0, first.getSpeed(), 0.000001);
        assertEquals(90.5, first.getHeading(), 0.0);
        assertEquals(2.5, first.getPdop(), 0.000001);
        assertEquals(1.5, first.getHdop(), 0.000001);
        assertEquals(2.0, first.getVdop(), 0.000001);
        assertEquals(Integer.valueOf(7), first.getSatellites());
        assertEquals(Integer.valueOf(1), first.getFixQuality());
        assertEquals(TIME * 1000 + 250, first.getTime().getTimeInMillis());

        // unlike the Holux format, more than 11 satellites do not make the track invalid
        GpxPosition second = route.getPosition(1);
        assertEquals(48.5, second.getLatitude(), 0.0);
        assertEquals(Integer.valueOf(14), second.getSatellites());
    }

    @Test
    public void satelliteDataIsSkippedPerSatellite() throws IOException {
        int bitmask = bits(UTC, LATITUDE, LONGITUDE, NSAT, SID, ELEVATION, AZIMUTH, SNR);
        GpxRoute route = readTrack(sector(header(bitmask),
                record(bitmask, rec().satelliteIds(3, 7, 12)),
                record(bitmask, rec().utc(TIME + 1).at(48.5, 11.75).satelliteIds()),
                record(bitmask, rec().utc(TIME + 2).at(48.75, 12.0).satelliteIds(5))));
        assertEquals(3, route.getPositionCount());
        assertEquals(48.5, route.getPosition(1).getLatitude(), 0.0);
        assertEquals(12.0, route.getPosition(2).getLongitude(), 0.0);
    }

    @Test
    public void recordWithBadChecksumIsSkipped() throws IOException {
        GpxRoute route = readTrack(sector(header(BASIC),
                record(BASIC, rec()),
                record(BASIC, rec().utc(TIME + 1).badChecksum()),
                record(BASIC, rec().utc(TIME + 2))));
        // GPSBabel counts the skipped record
        assertEquals(asList("TP000001", "TP000003"), descriptions(route));
    }

    @Test
    public void recordWithFillInsteadOfStarAndChecksumIsSkipped() throws IOException {
        byte[] powerOff = Arrays.copyOf(record(BASIC, rec().utc(TIME + 1)), 26);
        powerOff[24] = (byte) 0xff;
        powerOff[25] = (byte) 0xff;
        GpxRoute route = readTrack(sector(header(BASIC),
                record(BASIC, rec()), powerOff, record(BASIC, rec().utc(TIME + 2))));
        // GPSBabel does not count the record without '*'
        assertEquals(asList("TP000001", "TP000002"), descriptions(route));
        assertEquals((TIME + 2) * 1000, route.getPosition(1).getTime().getTimeInMillis());
    }

    @Test
    public void recordWithoutStarIsOneByteShorter() throws IOException {
        GpxRoute route = readTrack(sector(header(BASIC),
                record(BASIC, rec()),
                record(BASIC, rec().utc(TIME + 1).noStar()),
                record(BASIC, rec().utc(TIME + 2))));
        assertEquals(asList("TP000001", "TP000002", "TP000003"), descriptions(route));
        assertEquals((TIME + 2) * 1000, route.getPosition(2).getTime().getTimeInMillis());
    }

    @Test
    public void separatorRecordChangesTheBitmaskMidSector() throws IOException {
        int bitmask = bits(UTC, LATITUDE, LONGITUDE, HEIGHT, SPEED);
        GpxRoute route = readTrack(sector(header(BASIC),
                record(BASIC, rec()),
                info(0x02, bitmask),
                record(bitmask, rec().utc(TIME + 1).at(48.5, 11.75))));
        // a bitmask change does not start a new track
        assertEquals(2, route.getPositionCount());
        assertNull(route.getPosition(0).getSpeed());
        assertEquals(36.0, route.getPosition(1).getSpeed(), 0.000001);
        assertEquals(11.75, route.getPosition(1).getLongitude(), 0.0);
    }

    @Test
    public void validFixOnlyBitHasNoField() throws IOException {
        // bit 31 is 'log only positions with a valid fix' of the iBlue 747 and others; GPSBabel gives it size 0
        GpxRoute route = readTrack(sector(header(BASIC | 1 << 31),
                record(BASIC, rec()),
                info(0x02, BASIC | 1 << 31),
                record(BASIC, rec().utc(TIME + 1))));
        assertEquals(2, route.getPositionCount());
    }

    @Test
    public void dumpWithValidFixOnlyBitIsMtkNotHolux() throws IOException {
        int bitmask = bits(UTC, VALID, LATITUDE, LONGITUDE, HEIGHT) | 1 << 31;
        byte[][] records = new byte[100][];
        for (int i = 0; i < records.length; i++)
            records[i] = record(bitmask & ~(1 << 31), rec().utc(TIME + i * 5L).at(48.7 + i * 0.0001, 9.8 + i * 0.0001));
        byte[] data = sector(header(bitmask, 0x0104, 50, 0, 0), concat(info(0x07, 0x0106), concat(records)));

        assertEquals(100, readTrack(data).getPositionCount());
        ParserResult result = new NavigationFormatParser(new NavigationFormatRegistry()).read(new ByteArrayInputStream(data));
        assertTrue(result.isSuccessful());
        assertEquals(MtkLoggerBinaryFormat.class, result.getFormat().getClass());
        assertEquals(100, result.getTheRoute().getPositionCount());
    }

    @Test
    public void extraByteCostsNoRecord() throws IOException {
        // spec 00083 D25: after a bad checksum the reader resyncs byte by byte
        byte[][] records = new byte[41][];
        for (int i = 0; i < 40; i++)
            records[i < 20 ? i : i + 1] = record(BASIC, rec().utc(TIME + i * 5L));
        records[20] = new byte[]{0x2a};
        GpxRoute route = readTrack(sector(header(BASIC), records));
        assertEquals(40, route.getPositionCount());
        assertEquals((TIME + 39 * 5L) * 1000, route.getPosition(39).getTime().getTimeInMillis());
    }

    @Test
    public void positionOutOfRangeIsDropped() throws IOException {
        // spec 00083 D23: GPSBabel writes them, the records have a valid checksum
        GpxRoute route = readTrack(sector(header(BASIC),
                record(BASIC, rec().at(500.0, 11.5)),
                record(BASIC, rec().utc(TIME + 1).at(48.25, -400.0)),
                record(BASIC, rec().utc(TIME + 2).at(90.0, -180.0))));
        assertEquals(asList("TP000003"), descriptions(route));
    }

    @Test
    public void erasedHeaderIsNoM241Format() throws IOException {
        // the M-241 default is for Holux devices only; GPSBabel reads nothing, too
        byte[] erased = new byte[0x200];
        Arrays.fill(erased, (byte) 0xff);
        assertTrue(read(sector(erased, record(BASIC, rec()), record(BASIC, rec().utc(TIME + 1)))).isEmpty());
    }

    @Test
    public void logPeriodChangeDoesNotStartNewTrack() throws IOException {
        GpxRoute route = readTrack(sector(header(BASIC),
                record(BASIC, rec()),
                info(0x03, 10),
                info(0x04, 100),
                record(BASIC, rec().utc(TIME + 1))));
        assertEquals(2, route.getPositionCount());
        assertEquals(asList("Log every 5 sec", "0 m"), route.getDescription());
    }

    @Test
    public void loggerTurnedOnStartsNewTrack() throws IOException {
        List<GpxRoute> routes = read(sector(header(BASIC),
                record(BASIC, rec()),
                info(0x03, 10),
                info(0x07, 0x0104),
                record(BASIC, rec().utc(TIME + 1)),
                info(0x07, 0x0106),
                record(BASIC, rec().utc(TIME + 2))));
        assertEquals(2, routes.size());
        assertEquals("track-1", routes.get(0).getName());
        assertEquals(asList("TP000001", "TP000002"), descriptions(routes.get(0)));
        assertEquals("track-2", routes.get(1).getName());
        assertEquals(asList("Log every 1 sec", "0 m"), routes.get(1).getDescription());
        assertEquals(asList("TP000003"), descriptions(routes.get(1)));
    }

    @Test
    public void initialSeparatorRecordsSetBitmaskAndPeriod() throws IOException {
        int bitmask = bits(UTC, VALID, LATITUDE, LONGITUDE);
        GpxRoute route = readTrack(sector(header(BASIC, 0x0104, 50, 0, 0),
                info(0x02, bitmask), info(0x03, 150), info(0x05, 200), info(0x07, 0x0106),
                record(bitmask, rec())));
        assertEquals(asList("Log every 15 sec", "0 m when moving above 20 km/h"), route.getDescription());
        assertNull(route.getPosition(0).getElevation());
        assertEquals(Integer.valueOf(1), route.getPosition(0).getFixQuality());
    }

    @Test
    public void buttonPressRecordReasonCreatesWaypoint() throws IOException {
        int bitmask = bits(UTC, LATITUDE, LONGITUDE, RCR);
        List<GpxRoute> routes = read(sector(header(bitmask),
                record(bitmask, rec()),
                record(bitmask, rec().utc(TIME + 1).rcr(0x0008))));
        assertEquals(2, routes.size());
        assertEquals(Waypoints, routes.get(0).getCharacteristics());
        assertEquals(asList("WP000001"), descriptions(routes.get(0)));
        assertEquals(asList("TP000001", "TP000002"), descriptions(routes.get(1)));
    }

    @Test
    public void trailingFillEndsTheSectorAndReadingContinuesInTheNext() throws IOException {
        GpxRoute route = readTrack(concat(
                sector(header(BASIC), record(BASIC, rec())),
                sector(header(BASIC), record(BASIC, rec().utc(TIME + 1).at(48.5, 11.75)))));
        assertEquals(asList("TP000001", "TP000002"), descriptions(route));
        assertEquals(48.5, route.getPosition(1).getLatitude(), 0.0);
    }

    @Test
    public void readsManyRecordsAcrossBufferBoundaries() throws IOException {
        byte[][] records = new byte[500][];
        for (int i = 0; i < records.length; i++)
            records[i] = record(FULL, rec().utc(TIME + i).at(48.0 + i / 1000.0, 11.0 - i / 1000.0));
        GpxRoute route = readTrack(sector(header(FULL), records));
        assertEquals(500, route.getPositionCount());
        for (int i = 0; i < records.length; i++)
            assertEquals((TIME + i) * 1000, route.getPosition(i).getTime().getTimeInMillis());
        assertEquals(48.499, route.getPosition(499).getLatitude(), 0.0);
    }

    @Test
    public void positionWithoutFixAtLongitudeZeroIsDropped() throws IOException {
        int bitmask = bits(UTC, VALID, LATITUDE, LONGITUDE);
        GpxRoute route = readTrack(sector(header(bitmask),
                record(bitmask, rec().valid(0x0001).at(0.0, 0.0)),
                record(bitmask, rec().utc(TIME + 1).valid(0x0001))));
        assertEquals(asList("TP000002"), descriptions(route));
        assertEquals(Integer.valueOf(0), route.getPosition(0).getFixQuality());
    }

    @Test
    public void positionWithNonFiniteCoordinatesIsDropped() throws IOException {
        GpxRoute route = readTrack(sector(header(BASIC),
                record(BASIC, rec().at(Double.NaN, 11.5)),
                record(BASIC, rec().utc(TIME + 1).at(48.5, Double.POSITIVE_INFINITY)),
                record(BASIC, rec().utc(TIME + 2))));
        assertEquals(asList("TP000003"), descriptions(route));
    }

    @Test
    public void holuxM241DumpIsNotRecognized() throws IOException {
        // the marker string of an M-241 or GPSport 245 log session
        assertTrue(read(sector(header(BASIC), ascii("HOLUXGR241LOGGER"), record(BASIC, rec()))).isEmpty());
        assertTrue(read(sector(header(BASIC), info(0x07, 0x0106), record(BASIC, rec()),
                ascii("HOLUXGR245WAYPNT    "), record(BASIC, rec()))).isEmpty());
        // a record in the Holux layout: 4 byte floats, 3 byte height, no '*'; the log header is the same
        assertThrows(UnreadableFileException.class, () -> read(sector(header(BASIC), holuxRecord(), holuxRecord(), holuxRecord())));
    }

    static byte[] holuxRecord() {
        ByteBuffer buffer = ByteBuffer.allocate(16).order(LITTLE_ENDIAN);
        buffer.putInt((int) TIME).putFloat(48.25f).putFloat(11.5f);
        int height = Float.floatToIntBits(512.5f);
        buffer.put((byte) (height >> 8)).put((byte) (height >> 16)).put((byte) (height >> 24));
        int checksum = 0;
        for (int i = 0; i < 15; i++)
            checksum ^= buffer.get(i) & 0xff;
        buffer.put((byte) checksum);
        return buffer.array();
    }

    @Test
    public void holuxM241BinaryFormatDeclinesGenericDumps() throws IOException {
        // the Holux layout finds records with a matching checksum by chance in some of these dumps
        Random random = new Random(1);
        int[] bitmasks = {FULL, bits(UTC, LATITUDE, LONGITUDE), bits(UTC, VALID, LATITUDE, LONGITUDE, HEIGHT, SPEED)};
        for (int run = 0; run < 60; run++) {
            int bitmask = bitmasks[run % bitmasks.length];
            int count = 1 + random.nextInt(1500);
            byte[][] records = new byte[count][];
            double latitude = -60 + random.nextDouble() * 120, longitude = -170 + random.nextDouble() * 340;
            for (int i = 0; i < count; i++) {
                latitude += random.nextGaussian() * 0.0001;
                longitude += random.nextGaussian() * 0.0001;
                records[i] = record(bitmask, rec().utc(TIME + i * 5L).at(latitude, longitude)
                        .height((float) (random.nextDouble() * 500)).satellites(random.nextInt(13)));
            }
            int perSector = (SECTOR - 0x200 - 64) / records[0].length;
            ByteArrayOutputStream dump = new ByteArrayOutputStream();
            for (int start = 0; start < count; start += perSector) {
                byte[] sector = sector(header(bitmask, 0x0106, 50, 0, 0),
                        Arrays.copyOfRange(records, start, Math.min(count, start + perSector)));
                dump.write(sector, 0, sector.length);
            }
            byte[] data = dump.toByteArray();

            assertEquals(count, readTrack(data).getPositionCount(), "run " + run);
            ParserContextImpl<GpxRoute> holux = new ParserContextImpl<>();
            new HoluxM241BinaryFormat().read(new ByteArrayInputStream(data), holux);
            assertTrue(holux.getRoutes().isEmpty(), "run " + run);
        }
    }

    @Test
    public void zeroFilledFileWithPlausibleBitmaskIsNotRecognized() throws IOException {
        // all-zero records have no '*' and a checksum of 0 in its place; GPSBabel 1.5.4 reads them as 0/0 without time
        byte[] zeros = new byte[4096];
        ByteBuffer.wrap(zeros).order(LITTLE_ENDIAN).putInt(2, bits(UTC, LATITUDE, LONGITUDE));
        assertTrue(read(zeros).isEmpty());
        // spec 00083 D19: with a log header, the zero records make the file damaged
        assertThrows(UnreadableFileException.class, () -> read(sector(header(BASIC), new byte[SECTOR - 0x200])));

        byte[] sparse = new byte[20000];
        ByteBuffer.wrap(sparse).order(LITTLE_ENDIAN).putInt(2, bits(UTC, LATITUDE, LONGITUDE));
        for (int i = 997; i < sparse.length; i += 997)
            sparse[i] = 0x41;
        assertTrue(read(sparse).isEmpty());

        // the full registry leaves both to the formats after it, which read no route either
        for (byte[] data : new byte[][]{zeros, sparse}) {
            ParserResult result = new NavigationFormatParser(new NavigationFormatRegistry()).read(new ByteArrayInputStream(data));
            assertFalse(result.getFormat() != null && MtkLoggerBinaryFormat.class.equals(result.getFormat().getClass()));
            for (BaseRoute<?, ?> route : result.getAllRoutes())
                assertEquals(0, route.getPositionCount());
        }
    }

    @Test
    public void zeroRecordsDoNotCountForRecognition() throws IOException {
        // a few real records followed by zero fill instead of 0xFF fill: not read, but the log header matched
        assertThrows(UnreadableFileException.class, () -> read(sector(header(BASIC), record(BASIC, rec()),
                record(BASIC, rec().utc(TIME + 1)), new byte[26 * 10])));
        // GPSBabel reads the zero records as positions at 0/0 without time; they are kept once the file is recognized
        GpxRoute route = readTrack(sector(header(BASIC), record(BASIC, rec()), record(BASIC, rec().utc(TIME + 1)),
                new byte[25], record(BASIC, rec().utc(TIME + 2))));
        assertEquals(asList("TP000001", "TP000002", "TP000003", "TP000004"), descriptions(route));
        assertNull(route.getPosition(2).getTime());
    }

    @Test
    public void holuxDumpPaddedWithZerosIsNotRecognized() throws IOException {
        // else the Holux format would decline it as a generic dump
        assertThrows(UnreadableFileException.class, () -> read(sector(header(BASIC),
                holuxRecord(), holuxRecord(), holuxRecord(), new byte[SECTOR / 2])));
    }

    @Test
    public void mostlyBadRecordsAreDamaged() {
        // the resync counts every record length it skips as a record that could not be read
        assertThrows(UnreadableFileException.class, () -> read(sector(header(BASIC),
                record(BASIC, rec().badChecksum()),
                record(BASIC, rec().utc(TIME + 1).noStar().badChecksum()),
                record(BASIC, rec().utc(TIME + 2)))));
    }

    @Test
    public void randomBytesAreNotRecognized() throws IOException {
        Random random = new Random(747);
        for (int length : new int[]{0, 1, 20, 0x1ff, 0x200, 0x210, 4096, SECTOR, SECTOR + 0x300}) {
            for (int run = 0; run < 20; run++) {
                byte[] data = new byte[length];
                random.nextBytes(data);
                assertTrue(read(data).isEmpty(), "length " + length);
            }
        }
    }

    @Test
    public void textAndOtherBinariesAreNotRecognized() throws IOException {
        byte[] text = new byte[SECTOR];
        byte[] line = ascii("Name,Latitude,Longitude,Description\r\n");
        for (int i = 0; i + line.length < text.length; i += line.length)
            System.arraycopy(line, 0, text, i, line.length);
        assertTrue(read(text).isEmpty());
        assertTrue(read(new byte[SECTOR]).isEmpty());
        byte[] fill = new byte[SECTOR];
        Arrays.fill(fill, (byte) 0xff);
        assertTrue(read(fill).isEmpty());
    }

    @Test
    public void bitmaskWithUnknownBitsIsUnsupported() throws IOException {
        // spec 00083 D19: a log header whose format has fields of unknown size, like bit 20 of the Royaltek loggers
        UnreadableFileException exception = assertThrows(UnreadableFileException.class,
                () -> read(sector(header(BASIC | 1 << 20), record(BASIC, rec()))));
        assertEquals("Holux/MTK logger file has the log format 0x0010001d with fields RouteConverter does not know", exception.getMessage());
        assertThrows(UnreadableFileException.class, () -> read(sector(header(BASIC | 1 << 27), record(BASIC, rec()))));
        // without a log header, the file is not recognized
        assertTrue(read(sector(header(BASIC | 1 << 21, 0x0000, 50, 0, 0), record(BASIC, rec()))).isEmpty());
        assertTrue(read(sector(header(bits(UTC, LATITUDE)), record(bits(UTC, LATITUDE), rec()))).isEmpty());
    }

    @Test
    public void truncatedFileIsReadWithoutFailure() throws IOException {
        byte[] data = sector(header(BASIC), record(BASIC, rec()), record(BASIC, rec().utc(TIME + 1)));
        GpxRoute route = readTrack(Arrays.copyOf(data, 0x200 + 26 + 10));
        assertEquals(1, route.getPositionCount());
        assertTrue(read(Arrays.copyOf(data, 0x200)).isEmpty());
    }

    @Test
    public void parserDetectsGenericDumpsAsMtkAndM241DumpsAsHolux() throws IOException {
        byte[][] records = new byte[200][];
        for (int i = 0; i < records.length; i++)
            records[i] = record(FULL, rec().utc(TIME + i).at(48.0 + i / 1000.0, 11.0 - i / 1000.0));
        ParserResult mtk = new NavigationFormatParser(new NavigationFormatRegistry())
                .read(new ByteArrayInputStream(sector(header(FULL), records)));
        assertTrue(mtk.isSuccessful());
        assertEquals(MtkLoggerBinaryFormat.class, mtk.getFormat().getClass());
        assertEquals(200, mtk.getTheRoute().getPositionCount());

        ParserResult holux = new NavigationFormatParser(new NavigationFormatRegistry())
                .read(new ByteArrayInputStream(sector(header(BASIC | 1 << 31), ascii("HOLUXGR241LOGGER"),
                        holuxRecord(), holuxRecord())));
        assertTrue(holux.isSuccessful());
        assertEquals(HoluxM241BinaryFormat.class, holux.getFormat().getClass());
    }
}
