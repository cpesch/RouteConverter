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
import slash.navigation.base.NavigationFormat;
import slash.navigation.base.NavigationFormatParser;
import slash.navigation.base.NavigationFormatRegistry;
import slash.navigation.base.ParserContextImpl;
import slash.navigation.base.ParserResult;
import slash.navigation.base.UnreadableFileException;
import slash.navigation.gpx.GpxPosition;
import slash.navigation.gpx.GpxRoute;
import slash.navigation.tomtom.TomTomPoiFormat;

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
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static slash.navigation.base.RouteCharacteristics.Track;
import static slash.navigation.base.RouteCharacteristics.Waypoints;
import static slash.navigation.holux.HoluxM241BinaryFormat.*;

public class HoluxM241BinaryFormatTest {
    private static final int SECTOR = 0x10000;
    private static final int BASIC = bits(UTC, LATITUDE, LONGITUDE, HEIGHT);
    private static final long TIME = 1200000000L;

    private final HoluxM241BinaryFormat format = new HoluxM241BinaryFormat();

    private static int bits(int... types) {
        int result = 0;
        for (int type : types)
            result |= 1 << type;
        return result;
    }

    /** the values of one record; fields not in the bitmask are not written */
    private static class Rec {
        long utc = TIME;
        int valid = 0x0002;
        float latitude = 48.25f, longitude = 11.5f;
        float height = 512.5f;
        float speed = 36.0f, heading = 90.5f;
        int pdop = 250, hdop = 150, vdop = 200;
        int satellitesInView = 9, satellitesUsed = 7;
        int rcr = 0x0001, milliseconds = 0;
        int speedCentimetersPerSecond = 1000; // GPSport 245
        boolean badChecksum;

        Rec utc(long utc) { this.utc = utc; return this; }
        Rec at(float latitude, float longitude) { this.latitude = latitude; this.longitude = longitude; return this; }
        Rec height(float height) { this.height = height; return this; }
        Rec valid(int valid) { this.valid = valid; return this; }
        Rec satellites(int used) { this.satellitesUsed = used; return this; }
        Rec vdop(int vdop) { this.vdop = vdop; return this; }
        Rec rcr(int rcr) { this.rcr = rcr; return this; }
        Rec milliseconds(int milliseconds) { this.milliseconds = milliseconds; return this; }
        Rec badChecksum() { this.badChecksum = true; return this; }
    }

    private static Rec rec() {
        return new Rec();
    }

    private static void float24(ByteArrayOutputStream out, float value) {
        int bits = Float.floatToIntBits(value);
        out.write(bits >> 8);
        out.write(bits >> 16);
        out.write(bits >> 24);
    }

    /** a record in the Holux layout: no '*', 4 byte float lat/lon, 3 byte height, XOR checksum */
    private static byte[] record(int bitmask, boolean holux245, Rec rec) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (int k = 0; k < 32; k++) {
            if ((bitmask & (1 << k)) == 0)
                continue;
            ByteBuffer field = ByteBuffer.allocate(8).order(LITTLE_ENDIAN);
            switch (k) {
                case UTC -> field.putInt((int) rec.utc);
                case VALID -> field.putShort((short) rec.valid);
                case LATITUDE -> field.putFloat(rec.latitude);
                case LONGITUDE -> field.putFloat(rec.longitude);
                case HEIGHT -> {
                    if (holux245)
                        field.putInt(rec.speedCentimetersPerSecond);
                    else {
                        float24(out, rec.height);
                        continue;
                    }
                }
                case SPEED -> {
                    if (holux245) {
                        float24(out, rec.height);
                        continue;
                    }
                    field.putFloat(rec.speed);
                }
                case HEADING -> field.putFloat(rec.heading);
                case PDOP -> field.putShort((short) rec.pdop);
                case HDOP -> field.putShort((short) rec.hdop);
                case VDOP -> field.putShort((short) rec.vdop);
                case NSAT -> field.put((byte) rec.satellitesInView).put((byte) rec.satellitesUsed);
                case RCR -> field.putShort((short) rec.rcr);
                case MILLISECOND -> field.putShort((short) rec.milliseconds);
                default -> throw new IllegalArgumentException("type " + k + " not supported by the test");
            }
            out.write(field.array(), 0, field.position());
        }
        byte[] bytes = out.toByteArray();
        int checksum = 0;
        for (byte b : bytes)
            checksum ^= b & 0xff;
        out.write(rec.badChecksum ? checksum ^ 0x5a : checksum);
        return out.toByteArray();
    }

    private static byte[] record(int bitmask, Rec rec) {
        return record(bitmask, false, rec);
    }

    /** a 16 byte separator record: AA x7, type, 4 data bytes, BB x4 */
    private static byte[] info(int type, int value) {
        ByteBuffer buffer = ByteBuffer.allocate(16).order(LITTLE_ENDIAN);
        for (int i = 0; i < 7; i++)
            buffer.put((byte) 0xaa);
        buffer.put((byte) type).putInt(value);
        for (int i = 0; i < 4; i++)
            buffer.put((byte) 0xbb);
        return buffer.array();
    }

    private static byte[] ascii(String string) {
        return string.getBytes(US_ASCII);
    }

    /** the 0x200 byte sector header: record count, bitmask, policy, period, distance, speed */
    private static byte[] header(int bitmask, int policy, int period, int distance, int speed) {
        ByteBuffer buffer = ByteBuffer.allocate(0x200).order(LITTLE_ENDIAN);
        buffer.putShort((short) 0xffff).putInt(bitmask).putShort((short) policy)
                .putInt(period).putInt(distance).putInt(speed);
        return buffer.array();
    }

    private static byte[] header(int bitmask) {
        return header(bitmask, 0x0104, 50, 0, 0);
    }

    /** a 64 KiB sector: header, data, trailing 0xFF fill */
    private static byte[] sector(byte[] header, byte[]... data) {
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

    private static byte[] concat(byte[]... parts) {
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

    // the 3 byte height drops the low byte of the float's mantissa
    private static double truncated(float value) {
        return Float.intBitsToFloat(Float.floatToIntBits(value) & 0xffffff00);
    }

    @Test
    public void nameAndCapabilities() {
        assertEquals("Holux M-241 Binary (*.bin)", format.getName());
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
    public void isRegisteredAfterTomTomPoiFormat() {
        List<? extends NavigationFormat<?>> formats = new NavigationFormatRegistry().getReadFormats();
        int tomTom = -1, holux = -1;
        for (int i = 0; i < formats.size(); i++) {
            if (formats.get(i).getClass() == TomTomPoiFormat.class)
                tomTom = i;
            if (formats.get(i).getClass() == HoluxM241BinaryFormat.class)
                holux = i;
        }
        assertTrue(tomTom >= 0);
        assertEquals(tomTom + 1, holux);
    }

    @Test
    public void readsRecordsOfASectorAsOneTrack() throws IOException {
        GpxRoute route = readTrack(sector(header(BASIC),
                record(BASIC, rec().at(48.25f, 11.5f).height(632.109375f)),
                record(BASIC, rec().utc(TIME + 5).at(-33.75f, -122.5f).height(-12.5f))));

        assertEquals("track-1", route.getName());
        assertEquals(asList("Log every 5 sec", "0 m"), route.getDescription());
        assertEquals(asList("TP000001", "TP000002"), descriptions(route));

        GpxPosition first = route.getPosition(0);
        assertEquals(48.25, first.getLatitude(), 0.0);
        assertEquals(11.5, first.getLongitude(), 0.0);
        assertEquals(632.109375, first.getElevation(), 0.0);
        assertEquals(TIME * 1000, first.getTime().getTimeInMillis());
        assertNull(first.getSpeed());
        assertNull(first.getHeading());
        assertNull(first.getHdop());
        assertNull(first.getVdop());
        assertNull(first.getPdop());
        assertNull(first.getSatellites());

        GpxPosition second = route.getPosition(1);
        assertEquals(-33.75, second.getLatitude(), 0.0);
        assertEquals(-122.5, second.getLongitude(), 0.0);
        assertEquals(-12.5, second.getElevation(), 0.0);
        assertEquals((TIME + 5) * 1000, second.getTime().getTimeInMillis());
    }

    @Test
    public void heightHasThreeBytes() throws IOException {
        float height = 123.456f;
        GpxRoute route = readTrack(sector(header(BASIC), record(BASIC, rec().height(height))));
        assertEquals(truncated(height), route.getPosition(0).getElevation(), 0.000001);
    }

    @Test
    public void coordinatesAreFourByteFloats() throws IOException {
        GpxRoute route = readTrack(sector(header(BASIC), record(BASIC, rec().at(48.7042084f, 9.8836384f))));
        // GPSBabel writes the float coordinates with 9 decimals to GPX
        assertEquals(48.704208374, route.getPosition(0).getLatitude(), 0.0);
        assertEquals(9.883638382, route.getPosition(0).getLongitude(), 0.0);
    }

    @Test
    public void recordSizeAndFieldsFollowTheBitmask() throws IOException {
        int bitmask = bits(UTC, VALID, LATITUDE, LONGITUDE, HEIGHT, SPEED, HEADING, PDOP, HDOP, VDOP, NSAT, RCR, MILLISECOND);
        GpxRoute route = readTrack(sector(header(bitmask),
                record(bitmask, rec().milliseconds(250)),
                record(bitmask, rec().utc(TIME + 1).at(48.5f, 11.75f))));
        assertEquals(2, route.getPositionCount());

        GpxPosition first = route.getPosition(0);
        assertEquals(TIME * 1000 + 250, first.getTime().getTimeInMillis());
        assertEquals(48.25, first.getLatitude(), 0.0);
        assertEquals(11.5, first.getLongitude(), 0.0);
        assertEquals(512.5, first.getElevation(), 0.0);
        // km/h in the file, m/s in GPX, km/h in RouteConverter
        assertEquals(36.0, first.getSpeed(), 0.00001);
        assertEquals(90.5, first.getHeading(), 0.0);
        assertEquals(2.5, first.getPdop(), 0.000001);
        assertEquals(1.5, first.getHdop(), 0.000001);
        assertEquals(2.0, first.getVdop(), 0.000001);
        assertEquals(Integer.valueOf(7), first.getSatellites());

        GpxPosition second = route.getPosition(1);
        assertEquals((TIME + 1) * 1000, second.getTime().getTimeInMillis());
        assertEquals(48.5, second.getLatitude(), 0.0);
        assertEquals(11.75, second.getLongitude(), 0.0);
    }

    @Test
    public void zeroDopsAndSatellitesAreNotWrittenByGpsBabel() throws IOException {
        int bitmask = bits(UTC, LATITUDE, LONGITUDE, PDOP, HDOP, VDOP, NSAT);
        Rec rec = rec().satellites(0);
        rec.pdop = rec.hdop = rec.vdop = 0;
        GpxPosition position = readTrack(sector(header(bitmask), record(bitmask, rec))).getPosition(0);
        assertNull(position.getPdop());
        assertNull(position.getHdop());
        assertNull(position.getVdop());
        assertNull(position.getSatellites());
        assertNull(position.getElevation());
    }

    @Test
    public void recordWithBadChecksumIsSkipped() throws IOException {
        GpxRoute route = readTrack(sector(header(BASIC),
                record(BASIC, rec()),
                record(BASIC, rec().utc(TIME + 5).badChecksum()),
                record(BASIC, rec().utc(TIME + 10))));
        // the skipped record keeps its number, as GPSBabel counts it
        assertEquals(asList("TP000001", "TP000003"), descriptions(route));
        assertEquals((TIME + 10) * 1000, route.getPosition(1).getTime().getTimeInMillis());
    }

    @Test
    public void separatorRecordChangesTheBitmaskMidSector() throws IOException {
        int extended = bits(UTC, LATITUDE, LONGITUDE, HEIGHT, NSAT);
        GpxRoute route = readTrack(sector(header(BASIC),
                record(BASIC, rec()),
                info(0x02, extended),
                record(extended, rec().utc(TIME + 5).satellites(8)),
                info(0x02, BASIC),
                record(BASIC, rec().utc(TIME + 10))));
        // a bitmask change does not start a new track
        assertEquals(asList("TP000001", "TP000002", "TP000003"), descriptions(route));
        assertNull(route.getPosition(0).getSatellites());
        assertEquals(Integer.valueOf(8), route.getPosition(1).getSatellites());
        assertNull(route.getPosition(2).getSatellites());
    }

    @Test
    public void logPeriodChangeStartsNewTrack() throws IOException {
        List<GpxRoute> routes = read(sector(header(BASIC),
                record(BASIC, rec()),
                info(0x03, 10),
                record(BASIC, rec().utc(TIME + 1)),
                info(0x04, 1000),
                record(BASIC, rec().utc(TIME + 2)),
                info(0x05, 100),
                record(BASIC, rec().utc(TIME + 3))));
        assertEquals(3, routes.size());
        assertEquals("track-1", routes.get(0).getName());
        assertEquals(asList("Log every 5 sec", "0 m"), routes.get(0).getDescription());
        assertEquals("track-2", routes.get(1).getName());
        assertEquals(asList("Log every 1 sec", "0 m"), routes.get(1).getDescription());
        assertEquals("track-3", routes.get(2).getName());
        assertEquals(asList("Log every 1 sec", "100 m"), routes.get(2).getDescription());
        // a speed change does not start a new track
        assertEquals(asList("TP000003", "TP000004"), descriptions(routes.get(2)));
    }

    @Test
    public void logSpeedIsPartOfTheTrackDescription() throws IOException {
        GpxRoute route = readTrack(sector(header(BASIC, 0x0106, 150, 25, 100), record(BASIC, rec())));
        // printf's %.0f rounds half to even
        assertEquals(asList("Log every 15 sec", "2 m when moving above 10 km/h"), route.getDescription());
    }

    @Test
    public void initialSeparatorRecordsSetBitmaskAndPeriod() throws IOException {
        int bitmask = bits(UTC, LATITUDE, LONGITUDE);
        GpxRoute route = readTrack(sector(header(BASIC),
                info(0x02, bitmask), info(0x03, 20),
                record(bitmask, rec()), record(bitmask, rec().utc(TIME + 2))));
        assertEquals(asList("Log every 2 sec", "0 m"), route.getDescription());
        assertEquals(2, route.getPositionCount());
        assertNull(route.getPosition(0).getElevation());
    }

    @Test
    public void holuxStringIsSkipped() throws IOException {
        GpxRoute route = readTrack(sector(header(BASIC),
                ascii("HOLUXGR241LOGGER"),
                record(BASIC, rec()),
                ascii("HOLUXGR241LOGGER    "),
                record(BASIC, rec().utc(TIME + 5))));
        assertEquals(asList("TP000001", "TP000002"), descriptions(route));
        assertTrue(route.getPositions().stream().allMatch(position -> position.getLatitude() == 48.25));
    }

    @Test
    public void holuxWaypointStringCreatesWaypoint() throws IOException {
        List<GpxRoute> routes = read(sector(header(BASIC),
                record(BASIC, rec()),
                ascii("HOLUXGR241WAYPNT"),
                record(BASIC, rec().utc(TIME + 5).at(48.5f, 11.75f)),
                record(BASIC, rec().utc(TIME + 10))));
        assertEquals(2, routes.size());
        GpxRoute waypoints = routes.get(0);
        assertEquals(Waypoints, waypoints.getCharacteristics());
        assertNull(waypoints.getName());
        assertEquals(1, waypoints.getPositionCount());
        GpxPosition waypoint = waypoints.getPosition(0);
        assertEquals("WP000001", waypoint.getDescription());
        assertEquals(48.5, waypoint.getLatitude(), 0.0);
        assertEquals((TIME + 5) * 1000, waypoint.getTime().getTimeInMillis());
        // the waypoint is a track point, too
        assertEquals(asList("TP000001", "TP000002", "TP000003"), descriptions(routes.get(1)));
    }

    @Test
    public void buttonPressRecordReasonCreatesWaypoint() throws IOException {
        int bitmask = BASIC | bits(RCR);
        List<GpxRoute> routes = read(sector(header(bitmask),
                record(bitmask, rec().rcr(0x0001)),
                record(bitmask, rec().utc(TIME + 5).rcr(0x0008)),
                record(bitmask, rec().utc(TIME + 10).rcr(0x0009))));
        assertEquals(Waypoints, routes.get(0).getCharacteristics());
        assertEquals(asList("WP000001", "WP000002"), descriptions(routes.get(0)));
        assertEquals(3, routes.get(1).getPositionCount());
    }

    @Test
    public void trailingFillEndsTheSectorAndReadingContinuesInTheNext() throws IOException {
        byte[] first = sector(header(BASIC), record(BASIC, rec()), record(BASIC, rec().utc(TIME + 5)));
        // anything after the fill of a sector is ignored
        byte[] garbage = record(BASIC, rec().utc(TIME + 99));
        System.arraycopy(garbage, 0, first, 0x8000, garbage.length);
        byte[] second = sector(header(BASIC), record(BASIC, rec().utc(TIME + 10)));
        GpxRoute route = readTrack(concat(first, second));
        assertEquals(asList("TP000001", "TP000002", "TP000003"), descriptions(route));
        assertEquals((TIME + 10) * 1000, route.getPosition(2).getTime().getTimeInMillis());
    }

    @Test
    public void readsManyRecordsAcrossBufferBoundaries() throws IOException {
        // 16 byte records are shorter than the 20 byte Holux string
        int bitmask = BASIC;
        byte[][] records = new byte[1000][];
        for (int i = 0; i < records.length; i++) {
            records[i] = record(bitmask, rec().utc(TIME + i));
            // a Holux string straddling the 512 byte buffer of GPSBabel now and then
            if (i % 37 == 0)
                records[i] = concat(ascii("HOLUXGR241LOGGER    "), records[i]);
        }
        GpxRoute route = readTrack(sector(header(bitmask), records));
        assertEquals(records.length, route.getPositionCount());
        for (int i = 0; i < records.length; i++)
            assertEquals((TIME + i) * 1000, route.getPosition(i).getTime().getTimeInMillis());
    }

    @Test
    public void usesHeaderOfSecondSectorIfFirstHasInvalidPolicy() throws IOException {
        int bitmask = bits(UTC, LATITUDE, LONGITUDE);
        byte[] first = sector(header(BASIC, 0x0000, 50, 0, 0), record(bitmask, rec()));
        byte[] second = sector(header(bitmask, 0x0104, 30, 0, 0), record(bitmask, rec().utc(TIME + 3)));
        GpxRoute route = readTrack(concat(first, second));
        assertEquals(asList("Log every 3 sec", "0 m"), route.getDescription());
        assertEquals(2, route.getPositionCount());
        assertNull(route.getPosition(0).getElevation());
    }

    @Test
    public void readsGpSport245WithSpeedAndHeightSwapped() throws IOException {
        int bitmask = bits(UTC, LATITUDE, LONGITUDE, HEIGHT, SPEED);
        Rec slow = rec().height(320.5f);
        slow.speedCentimetersPerSecond = 1000;
        Rec fast = rec().utc(TIME + 5).height(-1.5f);
        fast.speedCentimetersPerSecond = 2500;
        GpxRoute route = readTrack(sector(header(bitmask | HOLUX245_MASK),
                ascii("HOLUXGR245LOGGER    "),
                record(bitmask, true, slow),
                record(bitmask, true, fast)));
        assertEquals(2, route.getPositionCount());
        // 10 m/s as cm/s in the height position
        assertEquals(36.0, route.getPosition(0).getSpeed(), 0.00001);
        assertEquals(320.5, route.getPosition(0).getElevation(), 0.0);
        assertEquals(90.0, route.getPosition(1).getSpeed(), 0.00001);
        assertEquals(-1.5, route.getPosition(1).getElevation(), 0.0);
    }

    @Test
    public void gpSport245BitmaskChangeClearsItsMarkerBit() throws IOException {
        int bitmask = bits(UTC, LATITUDE, LONGITUDE, HEIGHT, SPEED);
        int reduced = bits(UTC, LATITUDE, LONGITUDE, SPEED);
        GpxRoute route = readTrack(sector(header(bitmask),
                ascii("HOLUXGR245LOGGER    "),
                record(bitmask, true, rec()),
                info(0x02, reduced | HOLUX245_MASK),
                record(reduced, true, rec().utc(TIME + 5).height(100.5f))));
        assertEquals(2, route.getPositionCount());
        // GPSBabel reads the height from the speed position but writes it only if the height bit is set,
        // and writes the speed of the speed bit, which is never read and zero
        assertNull(route.getPosition(1).getElevation());
        assertNull(route.getPosition(1).getSpeed());
        assertEquals(48.25, route.getPosition(1).getLatitude(), 0.0);
    }

    @Test
    public void positionWithoutFixAtLongitudeZeroIsDropped() throws IOException {
        int bitmask = BASIC | bits(VALID);
        GpxRoute route = readTrack(sector(header(bitmask),
                record(bitmask, rec().valid(0x0001).at(0.0f, 0.0f)),
                record(bitmask, rec().utc(TIME + 1).valid(0x0002).at(1.5f, -5.0f)),
                record(bitmask, rec().utc(TIME + 2).valid(0x0001).at(48.25f, 11.5f)),
                record(bitmask, rec().utc(TIME + 3).valid(0x0040).at(48.25f, -11.5f))));
        assertEquals(asList("TP000002", "TP000003"), descriptions(route));
    }

    @Test
    public void routeWithTooManySatellitesIsInvalid() throws IOException {
        int bitmask = BASIC | bits(NSAT);
        assertEquals(1, read(sector(header(bitmask), record(bitmask, rec().satellites(11)))).size());
        assertTrue(read(sector(header(bitmask),
                record(bitmask, rec().satellites(11)),
                record(bitmask, rec().utc(TIME + 1).satellites(12)))).isEmpty());
    }

    @Test
    public void routeWithLargeVdopIsInvalid() throws IOException {
        int bitmask = BASIC | bits(VDOP);
        assertEquals(1, read(sector(header(bitmask), record(bitmask, rec().vdop(1999)))).size());
        assertTrue(read(sector(header(bitmask), record(bitmask, rec().vdop(2000)))).isEmpty());
    }

    @Test
    public void nonFiniteHeightSpeedAndHeadingBecomeNull() throws IOException {
        // a partially written record: 3 byte height FF FF FF is a NaN
        GpxRoute route = readTrack(sector(header(BASIC),
                record(BASIC, rec()),
                record(BASIC, rec().utc(TIME + 1).height(Float.intBitsToFloat(0xffffff00))),
                record(BASIC, rec().utc(TIME + 2))));
        assertEquals(asList("TP000001", "TP000002", "TP000003"), descriptions(route));
        assertEquals(512.5, route.getPosition(0).getElevation(), 0.0);
        assertNull(route.getPosition(1).getElevation());
        assertEquals(48.25, route.getPosition(1).getLatitude(), 0.0);

        int bitmask = BASIC | bits(SPEED, HEADING);
        Rec infinite = rec().height(Float.NEGATIVE_INFINITY);
        infinite.speed = Float.POSITIVE_INFINITY;
        infinite.heading = Float.NaN;
        GpxPosition position = readTrack(sector(header(bitmask), record(bitmask, infinite))).getPosition(0);
        assertNull(position.getElevation());
        assertNull(position.getSpeed());
        assertNull(position.getHeading());
        assertEquals(11.5, position.getLongitude(), 0.0);
    }

    @Test
    public void positionWithNonFiniteCoordinatesIsDropped() throws IOException {
        GpxRoute route = readTrack(sector(header(BASIC),
                record(BASIC, rec()),
                record(BASIC, rec().utc(TIME + 1).at(Float.NaN, 11.5f)),
                record(BASIC, rec().utc(TIME + 2).at(48.25f, Float.POSITIVE_INFINITY)),
                record(BASIC, rec().utc(TIME + 3))));
        assertEquals(asList("TP000001", "TP000004"), descriptions(route));
    }

    @Test
    public void badChecksumKeepsPendingLogRestart() throws IOException {
        List<GpxRoute> routes = read(sector(header(BASIC),
                record(BASIC, rec()),
                info(0x03, 10),
                record(BASIC, rec().utc(TIME + 1).badChecksum()),
                record(BASIC, rec().utc(TIME + 2))));
        assertEquals(2, routes.size());
        assertEquals(asList("TP000001"), descriptions(routes.get(0)));
        assertEquals("track-2", routes.get(1).getName());
        assertEquals(asList("Log every 1 sec", "0 m"), routes.get(1).getDescription());
        assertEquals(asList("TP000003"), descriptions(routes.get(1)));
    }

    @Test
    public void badChecksumKeepsPendingWaypoint() throws IOException {
        List<GpxRoute> routes = read(sector(header(BASIC),
                record(BASIC, rec()),
                ascii("HOLUXGR241WAYPNT"),
                record(BASIC, rec().utc(TIME + 1).badChecksum()),
                record(BASIC, rec().utc(TIME + 2).at(48.5f, 11.75f)),
                record(BASIC, rec().utc(TIME + 3))));
        assertEquals(2, routes.size());
        GpxRoute waypoints = routes.get(0);
        assertEquals(Waypoints, waypoints.getCharacteristics());
        assertEquals(asList("WP000001"), descriptions(waypoints));
        assertEquals((TIME + 2) * 1000, waypoints.getPosition(0).getTime().getTimeInMillis());
        assertEquals(asList("TP000001", "TP000003", "TP000004"), descriptions(routes.get(1)));
    }

    @Test
    public void fixQualityFromValid() throws IOException {
        int bitmask = BASIC | bits(VALID);
        GpxRoute route = readTrack(sector(header(bitmask),
                record(bitmask, rec().valid(0x0002)),
                record(bitmask, rec().utc(TIME + 1).valid(0x0004)),
                record(bitmask, rec().utc(TIME + 2).valid(0x0008)),
                record(bitmask, rec().utc(TIME + 3).valid(0x0001)),
                record(bitmask, rec().utc(TIME + 4).valid(0x0040)),
                record(bitmask, rec().utc(TIME + 5).valid(0x0010))));
        assertEquals(6, route.getPositionCount());
        assertEquals(Integer.valueOf(1), route.getPosition(0).getFixQuality());
        assertEquals(Integer.valueOf(2), route.getPosition(1).getFixQuality());
        assertEquals(Integer.valueOf(3), route.getPosition(2).getFixQuality());
        assertEquals(Integer.valueOf(0), route.getPosition(3).getFixQuality());
        assertNull(route.getPosition(4).getFixQuality());
        assertNull(route.getPosition(5).getFixQuality());

        // without VALID GPSBabel knows no fix
        assertNull(readTrack(sector(header(BASIC), record(BASIC, rec()))).getPosition(0).getFixQuality());
    }

    @Test
    public void randomBytesAreNotRecognized() throws IOException {
        Random random = new Random(241);
        for (int length : new int[]{0, 1, 20, 0x1ff, 0x200, 0x210, 4096, SECTOR, SECTOR + 0x300}) {
            byte[] data = new byte[length];
            random.nextBytes(data);
            assertTrue(read(data).isEmpty(), "length " + length);
        }
    }

    @Test
    public void textAndOtherBinariesAreNotRecognized() throws IOException {
        byte[] text = new byte[SECTOR];
        Arrays.fill(text, (byte) ' ');
        byte[] line = ascii("Name,Latitude,Longitude,Description\r\n");
        for (int i = 0; i + line.length < text.length; i += line.length)
            System.arraycopy(line, 0, text, i, line.length);
        assertTrue(read(text).isEmpty());

        // a Garmin POI database like file: a "GRMREC" signature instead of a sector header
        byte[] garmin = new byte[4096];
        System.arraycopy(ascii("GRMREC00"), 0, garmin, 6, 8);
        assertTrue(read(garmin).isEmpty());

        byte[] zeros = new byte[SECTOR];
        assertTrue(read(zeros).isEmpty());
        byte[] fill = new byte[SECTOR];
        Arrays.fill(fill, (byte) 0xff);
        assertTrue(read(fill).isEmpty());
    }

    @Test
    public void bitmaskWithoutTimeOrPositionIsNotRecognized() throws IOException {
        for (int bitmask : new int[]{bits(LATITUDE, LONGITUDE, HEIGHT), bits(UTC, LONGITUDE), bits(UTC, LATITUDE)}) {
            Rec rec = rec();
            assertTrue(read(sector(header(bitmask), record(bitmask, rec), record(bitmask, rec))).isEmpty());
        }
    }

    @Test
    public void bitmaskWithUnknownBitsIsUnsupported() throws IOException {
        // spec 00083 D19: with a log header, the file is reported; without, it is not recognized
        assertThrows(UnreadableFileException.class, () -> read(sector(header(BASIC | 1 << 21), record(BASIC, rec()))));
        assertTrue(read(sector(header(BASIC | 1 << 21, 0x0000, 50, 0, 0), record(BASIC, rec()))).isEmpty());
        // the Holux 'low precision' bit is cleared before
        assertEquals(1, read(sector(header(BASIC | 1 << 31), record(BASIC, rec()))).size());
    }

    @Test
    public void fileWithLogHeaderButWithoutValidChecksumIsDamaged() {
        // spec 00083 D19: the log header matched, so the reader reports why it cannot read the file
        UnreadableFileException exception = assertThrows(UnreadableFileException.class, () -> read(sector(header(BASIC),
                record(BASIC, rec().badChecksum()),
                record(BASIC, rec().utc(TIME + 1).badChecksum()))));
        assertTrue(exception.getMessage().startsWith("Holux/MTK logger file is damaged"));
    }

    @Test
    public void fileWithoutLogHeaderAndValidChecksumIsNotRecognized() throws IOException {
        // a log status of 0 is no log header
        assertTrue(read(sector(header(BASIC, 0x0000, 50, 0, 0),
                record(BASIC, rec().badChecksum()),
                record(BASIC, rec().utc(TIME + 1).badChecksum()))).isEmpty());
    }

    @Test
    public void logWithoutRecordsIsNotDamaged() throws IOException {
        assertTrue(read(header(BASIC)).isEmpty());
        assertTrue(read(sector(header(BASIC))).isEmpty());
        assertTrue(read(sector(header(BASIC), info(0x07, 0x0106), ascii("HOLUXGR241LOGGER    "))).isEmpty());
    }

    @Test
    public void parserReportsUnreadableDumpInsteadOfEmptyRoute() throws IOException {
        byte[] data = sector(header(BASIC), ascii("HOLUXGR241LOGGER    "),
                record(BASIC, rec().badChecksum()), record(BASIC, rec().utc(TIME + 1).badChecksum()));
        ParserResult result = new NavigationFormatParser(new NavigationFormatRegistry()).read(new ByteArrayInputStream(data));
        assertFalse(result.isSuccessful());
        assertTrue(result.getFailure() instanceof UnreadableFileException);
    }

    @Test
    public void extraByteCostsNoRecord() throws IOException {
        // spec 00083 D25: after a bad checksum the reader resyncs byte by byte; GPSBabel loses the rest of the sector
        byte[][] records = new byte[101][];
        for (int i = 0; i < 100; i++)
            records[i < 50 ? i : i + 1] = record(BASIC, rec().utc(TIME + i * 5L).at(48.25f + i * 0.0001f, 11.5f));
        records[50] = new byte[]{0x00};
        GpxRoute route = readTrack(sector(header(BASIC), records));
        assertEquals(100, route.getPositionCount());
        assertEquals((TIME + 50 * 5L) * 1000, route.getPosition(50).getTime().getTimeInMillis());
        assertEquals((TIME + 99 * 5L) * 1000, route.getPosition(99).getTime().getTimeInMillis());
    }

    @Test
    public void lostBytesCostOneRecord() throws IOException {
        byte[] damaged = Arrays.copyOf(record(BASIC, rec().utc(TIME + 5)), 7);
        GpxRoute route = readTrack(sector(header(BASIC),
                record(BASIC, rec()), damaged, record(BASIC, rec().utc(TIME + 10)), record(BASIC, rec().utc(TIME + 15))));
        assertEquals(3, route.getPositionCount());
        assertEquals((TIME + 10) * 1000, route.getPosition(1).getTime().getTimeInMillis());
    }

    @Test
    public void resyncSkipsRecordsWithImplausibleValues() throws IOException {
        // the remains of a damaged record plus a record whose checksum matches but whose values do not look like
        // a position: out of range coordinates, a time before 1970 and an unknown fix mode
        int bitmask = bits(UTC, VALID, LATITUDE, LONGITUDE, HEIGHT);
        GpxRoute route = readTrack(sector(header(bitmask),
                record(bitmask, rec()),
                new byte[]{0x01, 0x02, 0x03},
                record(bitmask, rec().utc(TIME + 1).at(91.0f, 11.5f)),
                record(bitmask, rec().utc(-5)),
                record(bitmask, rec().utc(TIME + 3).valid(0x0003)),
                record(bitmask, rec().utc(TIME + 4).height(25000.0f)),
                record(bitmask, rec().utc(TIME + 5))));
        assertEquals(2, route.getPositionCount());
        assertEquals((TIME + 5) * 1000, route.getPosition(1).getTime().getTimeInMillis());
    }

    @Test
    public void resyncEndsAtTheSectorBoundary() throws IOException {
        byte[] garbage = new byte[100];
        new Random(241).nextBytes(garbage);
        byte[] first = sector(header(BASIC), record(BASIC, rec()), garbage);
        byte[] second = sector(header(BASIC), record(BASIC, rec().utc(TIME + 10)));
        GpxRoute route = readTrack(concat(first, second));
        assertEquals((TIME + 10) * 1000, route.getPosition(route.getPositionCount() - 1).getTime().getTimeInMillis());
    }

    @Test
    public void positionOutOfRangeIsDropped() throws IOException {
        // spec 00083 D23: GPSBabel writes them, the records have a valid checksum
        GpxRoute route = readTrack(sector(header(BASIC),
                record(BASIC, rec().at(500.0f, 11.5f)),
                record(BASIC, rec().utc(TIME + 1).at(48.25f, -400.0f)),
                record(BASIC, rec().utc(TIME + 2).at(-90.0f, 180.0f))));
        assertEquals(asList("TP000003"), descriptions(route));
    }

    @Test
    public void sectorHeaderSetsTheBitmaskOfItsSector() throws IOException {
        // spec 00083 D25: GPSBabel keeps the bitmask of the first sector and reads the second one misaligned
        int extended = bits(UTC, LATITUDE, LONGITUDE, HEIGHT, HEADING);
        byte[] first = sector(header(BASIC), record(BASIC, rec()), record(BASIC, rec().utc(TIME + 5)));
        byte[] second = sector(header(extended), record(extended, rec().utc(TIME + 10)), record(extended, rec().utc(TIME + 15)));
        GpxRoute route = readTrack(concat(first, second));
        assertEquals(4, route.getPositionCount());
        assertNull(route.getPosition(1).getHeading());
        assertEquals(90.5, route.getPosition(3).getHeading(), 0.0);
    }

    @Test
    public void erasedSectorHeaderKeepsTheCurrentBitmask() throws IOException {
        int bitmask = bits(UTC, LATITUDE, LONGITUDE, HEIGHT, HEADING);
        byte[] first = sector(header(BASIC), info(0x02, bitmask), record(bitmask, rec()));
        byte[] second = sector(header(0xffffffff, 0xffff, -1, -1, -1), record(bitmask, rec().utc(TIME + 10)));
        GpxRoute route = readTrack(concat(first, second));
        assertEquals(2, route.getPositionCount());
        assertEquals(90.5, route.getPosition(1).getHeading(), 0.0);
    }

    @Test
    public void erasedFirstHeaderMeansTheM241Format() throws IOException {
        // like BT747; GPSBabel reads nothing
        byte[] erased = new byte[0x200];
        Arrays.fill(erased, (byte) 0xff);
        GpxRoute route = readTrack(sector(erased, ascii("HOLUXGR241LOGGER    "),
                record(BASIC, rec()), record(BASIC, rec().utc(TIME + 5))));
        assertEquals(2, route.getPositionCount());
        assertEquals(truncated(512.5f), route.getPosition(0).getElevation(), 0.0);
    }

    private static byte[] ring(int policy) {
        // sector 0 was overwritten with the newest positions, sector 1 holds the oldest, sector 2 the ones between
        byte[] newest = sector(header(BASIC, policy, 50, 0, 0), record(BASIC, rec().utc(TIME + 200)), record(BASIC, rec().utc(TIME + 205)));
        byte[] oldest = sector(header(BASIC, policy, 50, 0, 0), record(BASIC, rec().utc(TIME)), record(BASIC, rec().utc(TIME + 5)));
        byte[] between = sector(header(BASIC, policy, 50, 0, 0), record(BASIC, rec().utc(TIME + 100)));
        return concat(newest, oldest, Arrays.copyOf(between, 0x400));
    }

    private static List<Long> times(GpxRoute route) {
        return route.getPositions().stream().map(position -> position.getTime().getTimeInMillis() / 1000 - TIME).toList();
    }

    @Test
    public void wrappedRingWithOverwritePolicyIsReadFromItsOldestSector() throws IOException {
        // spec 00083 D25, like BT747; GPSBabel reads in file order
        assertEquals(asList(0L, 5L, 100L, 200L, 205L), times(readTrack(ring(0x0102))));
        assertEquals(asList(0L, 5L, 100L, 200L, 205L), times(readTrack(ring(0x0100))));
    }

    @Test
    public void ringWithStopPolicyIsReadInFileOrder() throws IOException {
        // a logger that stops when full never wraps
        assertEquals(asList(200L, 205L, 0L, 5L, 100L), times(readTrack(ring(0x0104))));
        assertEquals(asList(200L, 205L, 0L, 5L, 100L), times(readTrack(ring(0x0106))));
    }

    @Test
    public void unwrappedRingWithOverwritePolicyIsReadInFileOrder() throws IOException {
        byte[] first = sector(header(BASIC, 0x0102, 50, 0, 0), record(BASIC, rec()));
        byte[] second = sector(header(BASIC, 0x0102, 50, 0, 0), record(BASIC, rec().utc(TIME + 10)));
        assertEquals(asList(0L, 10L), times(readTrack(concat(first, second))));
    }

    @Test
    public void truncatedFileIsReadWithoutFailure() throws IOException {
        byte[] data = sector(header(BASIC), record(BASIC, rec()), record(BASIC, rec().utc(TIME + 1)));
        GpxRoute route = readTrack(Arrays.copyOf(data, 0x200 + 16 + 8));
        assertEquals(1, route.getPositionCount());
        assertTrue(read(Arrays.copyOf(data, 0x200)).isEmpty());
    }

    @Test
    public void formatInstanceKeepsNoStateBetweenReads() throws IOException {
        byte[] data = sector(header(BASIC), ascii("HOLUXGR245LOGGER    "), record(BASIC, true, rec()));
        read(data);
        GpxRoute route = readTrack(sector(header(BASIC), record(BASIC, rec())));
        assertEquals(512.5, route.getPosition(0).getElevation(), 0.0);
        assertEquals("track-1", route.getName());
        assertEquals("TP000001", route.getPosition(0).getDescription());
    }

    @Test
    public void formatIsSingletonInRegistry() {
        int count = 0;
        for (NavigationFormat<?> aFormat : new NavigationFormatRegistry().getReadFormats())
            if (aFormat instanceof HoluxM241BinaryFormat)
                count++;
        assertEquals(1, count);
        assertSame(HoluxM241BinaryFormat.class, new HoluxM241BinaryFormat().getClass());
    }
}
