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

    Copyright (C) 2007 Per Borgentun, e4borgen(at)yahoo.com
    With lot of inspiration from wbt-200.c
       Copyright (C) 2006 Andy Armstrong
    Translated to Java from GPSBabel mtk_logger.cc (master 9c1e336) for RouteConverter, 2026; modified.
*/

package slash.navigation.holux;

import slash.navigation.base.GpxBackedNavigationFormat;
import slash.navigation.gpx.Gpx10Format;
import slash.navigation.gpx.GpxPosition;
import slash.navigation.gpx.GpxRoute;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static java.math.RoundingMode.HALF_EVEN;
import static java.util.Collections.emptyList;
import static slash.common.type.CompactCalendar.fromMillis;
import static slash.navigation.base.RouteCharacteristics.Track;
import static slash.navigation.base.RouteCharacteristics.Waypoints;
import static slash.navigation.common.UnitConversion.msToKmh;

/**
 * The base of the MTK logger binary (.bin) formats.
 * <p>
 * A port of the file-reading path of GPSBabel's {@code mtk_logger.cc} as used by its {@code mtk-bin}
 * and {@code m241-bin} formats: a flash dump of a MediaTek based logger, made of 64 KiB sectors that
 * start with a 0x200 byte header and continue with fixed-size records whose fields are defined by a
 * log format bitmask. The device type decides the record layout like {@code mtk_device} does in GPSBabel:
 * the generic MTK logger writes 8 byte double coordinates, a 4 byte float height and a '*' before the
 * checksum, the Holux devices write 4 byte float coordinates, a 3 byte float height and no '*'.
 *
 * @author Christian Pesch
 */

abstract class MtkBinaryFormat extends GpxBackedNavigationFormat {
    private static final int SECTOR_SIZE = 0x10000;
    private static final int HEADER_SIZE = 0x200;
    private static final int SECTOR_HEADER_PREFIX_SIZE = 20;
    private static final int BUFFER_SIZE = 512;
    private static final int INFO_SIZE = 16;

    // data types used by the MTK chipset, the bit numbers of the log format bitmask
    static final int UTC = 0;
    static final int VALID = 1;
    static final int LATITUDE = 2;
    static final int LONGITUDE = 3;
    static final int HEIGHT = 4;
    static final int SPEED = 5;
    static final int HEADING = 6;
    static final int DSTA = 7;
    static final int DAGE = 8;
    static final int PDOP = 9;
    static final int HDOP = 10;
    static final int VDOP = 11;
    static final int NSAT = 12;
    static final int SID = 13;
    static final int ELEVATION = 14;
    static final int AZIMUTH = 15;
    static final int SNR = 16;
    static final int RCR = 17;
    static final int MILLISECOND = 18;
    static final int DISTANCE = 19;

    // log_type[32] of mtk_logger.h: the size of each field, unknown bits have size 0
    private static final int[] MTK_LOG_TYPE_SIZES = {
            4, 2, 8, 8, 4, 4, 4, 2, 4, 2, 2, 2, 2, 4, 2, 2, 2, 2, 2, 8,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
    };

    private static final int MTK_EVT_BITMASK = 1 << 0x02;
    private static final int MTK_EVT_PERIOD = 1 << 0x03;
    private static final int MTK_EVT_DISTANCE = 1 << 0x04;
    private static final int MTK_EVT_SPEED = 1 << 0x05;
    private static final int MTK_EVT_START = 1 << 0x07;
    private static final int MTK_EVT_WAYPT = 1 << 0x10;

    // Holux-specific 'low precision' bit
    private static final int HOLUX_LOW_PRECISION_MASK = 0x7fffffff;
    static final int HOLUX245_MASK = 1 << 27;

    // the guard: bits mtk_logger.cc knows a size for, plus the GPSport 245 marker bit on the Holux devices
    private static final int KNOWN_MTK_BITS = (1 << (DISTANCE + 1)) - 1;
    private static final int KNOWN_BITS = KNOWN_MTK_BITS | HOLUX245_MASK;
    private static final int REQUIRED_BITS = 1 << UTC | 1 << LATITUDE | 1 << LONGITUDE;

    private static final int GPX_COORDINATE_DIGITS = 9;
    private static final int GPX_DECIMAL_DIGITS = 6;

    public String getExtension() {
        return ".bin";
    }

    /**
     * @return true for the Holux devices (GPSBabel's {@code HOLUX_M241}, which turns into
     * {@code HOLUX_GR245} on a GPSport 245 marker), false for the generic {@code MTK_LOGGER}
     */
    abstract boolean isHolux();

    public boolean isSupportsMultipleRoutes() {
        return false;
    }

    public boolean isSupportsWriting() {
        return false;
    }

    protected List<GpxRoute> readRoutes(InputStream source) throws IOException {
        byte[] data = source.readAllBytes();
        // in a generic MTK dump, the Holux layout finds a few records with a matching checksum by chance
        if (isHolux() && !new Parser(data, false).read().isEmpty())
            return emptyList();
        return new Parser(data, isHolux()).read();
    }

    public void write(GpxRoute route, OutputStream target, int startIndex, int endIndex) {
        throw new UnsupportedOperationException();
    }

    // GPSBabel writes coordinates to GPX with %0.9f
    private static double asGpxCoordinate(double value) {
        return new BigDecimal(value).setScale(GPX_COORDINATE_DIGITS, HALF_EVEN).doubleValue();
    }

    // GPSBabel writes elevation, speed, course and dops to GPX with %f; the GPX reader turns its nan and inf into null
    private static Double asGpxDecimal(double value) {
        if (!Double.isFinite(value))
            return null;
        return new BigDecimal(value).setScale(GPX_DECIMAL_DIGITS, HALF_EVEN).doubleValue();
    }

    // printf's %.0f
    private static String formatNoDecimals(double value) {
        return new BigDecimal(value).setScale(0, HALF_EVEN).toPlainString();
    }

    /** data_item of mtk_logger.h, with the types GPSBabel stores the values in */
    private static class DataItem {
        int timestamp;
        int valid;
        double latitude;
        double longitude;
        float height;
        float speed;
        float heading;
        float pdop, hdop, vdop;
        int satellitesUsed;
        int rcr;
        int milliseconds;
    }

    /** The state GPSBabel keeps in MtkLoggerBase while reading one file of the given device type. */
    private class Parser {
        private final byte[] data;
        private final boolean holux;
        private final int[] logTypeSizes = MTK_LOG_TYPE_SIZES.clone();
        private boolean holux245;
        private int bitmask, logLength, period, distance, speed, trackEvent;
        private int count, validChecksumCount;
        private final List<GpxPosition> wayPoints = new ArrayList<>();
        private final List<GpxRoute> tracks = new ArrayList<>();
        private List<GpxPosition> trackPositions;

        Parser(byte[] data, boolean holux) {
            this.data = data;
            this.holux = holux;
            if (holux) {
                // file_init() for HOLUX_M241
                logTypeSizes[LATITUDE] = logTypeSizes[LONGITUDE] = 4;
                logTypeSizes[HEIGHT] = 3;
            }
        }

        private int u8(long offset) {
            // C reads stale buffer contents or past the end of the buffer here; take the file's bytes or 0
            return offset >= 0 && offset < data.length ? data[(int) offset] & 0xff : 0;
        }

        private int u16(long offset) {
            return u8(offset) | u8(offset + 1) << 8;
        }

        private int s32(long offset) {
            return u8(offset) | u8(offset + 1) << 8 | u8(offset + 2) << 16 | u8(offset + 3) << 24;
        }

        private float float32(long offset) {
            return Float.intBitsToFloat(s32(offset));
        }

        // the 3 byte float of the Holux devices: the missing low byte of the mantissa is zero
        private float float24(long offset) {
            return Float.intBitsToFloat(u8(offset) << 8 | u8(offset + 1) << 16 | u8(offset + 2) << 24);
        }

        private double float64(long offset) {
            long low = s32(offset) & 0xffffffffL;
            long high = s32(offset + 4) & 0xffffffffL;
            return Double.longBitsToDouble(high << 32 | low);
        }

        private boolean matches(long offset, String string) {
            for (int i = 0; i < string.length(); i++) {
                if (u8(offset + i) != string.charAt(i))
                    return false;
            }
            return true;
        }

        private boolean isLogReset(long offset) {
            for (int i = 0; i < 6; i++)
                if (u8(offset + i) != 0xaa)
                    return false;
            for (int i = 12; i < 16; i++)
                if (u8(offset + i) != 0xbb)
                    return false;
            return true;
        }

        private boolean isHoluxString(long offset, long length) {
            return length >= 5 && matches(offset, "HOLUX");
        }

        // GPSBabel clears the Holux-specific 'low precision' bit only for the Holux devices
        private int asBitmask(int bitmask) {
            return holux ? bitmask & HOLUX_LOW_PRECISION_MASK : bitmask;
        }

        private boolean isPlausibleBitmask(int bitmask) {
            int knownBits = holux ? KNOWN_BITS : KNOWN_MTK_BITS;
            return (bitmask & ~knownBits) == 0 && (bitmask & REQUIRED_BITS) == REQUIRED_BITS;
        }

        private void holux245Init() {
            // height and speed have changed position in bitmask and data on the Holux 245
            holux245 = true;
            logTypeSizes[HEIGHT] = 4; // speed size - unit: cm/sec
            logTypeSizes[SPEED] = 3; // height size
        }

        private int logLength(int bitmask) {
            int length = holux ? 1 : 2; // Holux devices have no '*' before the checksum, the others add '*' + checksum
            for (int i = 0; i < 32; i++) {
                if (((1 << i) & bitmask) != 0) {
                    if ((i == SID || i == ELEVATION || i == AZIMUTH || i == SNR) && ((1 << SID) & bitmask) != 0)
                        length += logTypeSizes[i] * 32; // worst case, max sat. count
                    else
                        length += logTypeSizes[i];
                }
            }
            return length;
        }

        List<GpxRoute> read() {
            long fileSize = data.length;
            if (fileSize <= HEADER_SIZE)
                return emptyList();

            /* header: u16 log count of this 64 KiB block, u32 default bitmask, u16 overwrite/stop policy,
               u32 log period (sec*10), u32 log distance (m*10), u32 log speed (km/h*10) */
            long header = 0;
            int logPolicy = u16(header + 6);
            if (!(logPolicy == 0x0104 || logPolicy == 0x0106) && fileSize > SECTOR_SIZE)
                header = SECTOR_SIZE; // invalid initial log policy - check next block
            bitmask = asBitmask(s32(header + 2));
            period = s32(header + 8);
            distance = s32(header + 12);
            speed = s32(header + 16);
            if (header + SECTOR_HEADER_PREFIX_SIZE > fileSize)
                return emptyList();
            trackEvent = 0;

            // read initial info blocks -- if any
            long position = HEADER_SIZE;
            long filePosition = HEADER_SIZE;
            int bufferLength;
            int consumed;
            do {
                bufferLength = (int) Math.max(0, Math.min(INFO_SIZE, fileSize - filePosition));
                long block = filePosition;
                filePosition += bufferLength;
                consumed = 0;
                if (bufferLength > 0 && u8(block) == 0xaa) {
                    consumed = parseInfo(block, bufferLength);
                    position += consumed;
                }
                // a Holux string ends the loop as well and is handled below
            } while (consumed == INFO_SIZE);
            position += bufferLength;

            // the guard: GPSBabel would try to parse anything
            if (!isPlausibleBitmask(bitmask))
                return emptyList();
            logLength = logLength(bitmask);

            // the 512 byte buffer of file_read() is a window into the file
            long windowStart = filePosition - bufferLength;
            int windowLength = bufferLength;
            while (position < fileSize) {
                int read = (int) Math.max(0, Math.min(BUFFER_SIZE - windowLength, fileSize - filePosition));
                if (read <= 0)
                    break;
                filePosition += read;
                int length = windowLength + read;
                int i = 0;
                boolean jumped = false;
                while ((length - i) >= logLength) {
                    long offset = windowStart + i;
                    int k;
                    if ((length - i) >= INFO_SIZE && isLogReset(offset)) {
                        parseInfo(offset, length - i);
                        k = INFO_SIZE;
                    } else if (isHoluxString(offset, length - i)) {
                        if (!holux)
                            // an M-241 dump, which GPSBabel's mtk-bin would parse as records: leave it to the Holux format
                            return emptyList();
                        // m241  - HOLUXGR241LOGGER or HOLUXGR241WAYPNT or HOLUXGR241LOGGER<SP><SP><SP><SP>
                        // gr245 - HOLUXGR245LOGGER<SP><SP><SP><SP> or HOLUXGR245WAYPNT<SP><SP><SP><SP>
                        if (matches(offset + 10, "WAYPNT"))
                            trackEvent |= MTK_EVT_WAYPT;
                        k = INFO_SIZE;
                        if (matches(offset, "HOLUXGR245"))
                            holux245Init();
                        // an M-1200 is like a 245
                        if (matches(offset, "HOLUXM1200"))
                            holux245Init();
                        // skip the 4 spaces that may occur on every device
                        if (matches(offset + INFO_SIZE, "    "))
                            k += 4;
                    } else if (u8(offset) == 0xff && u8(offset + 1) == 0xff && u8(offset + 2) == 0xff && u8(offset + 3) == 0xff) {
                        // end of 64 KiB block segment -- realign to next data area
                        long next = ((position + logLength + 1024) / SECTOR_SIZE) * SECTOR_SIZE + HEADER_SIZE;
                        if (next <= position)
                            next += SECTOR_SIZE;
                        position = next;
                        windowStart = filePosition = next;
                        windowLength = 0;
                        jumped = true;
                        break;
                    } else {
                        k = parse(offset, bitmask);
                    }
                    i += k;
                    position += k;
                }
                if (!jumped) {
                    windowStart += i;
                    windowLength = Math.max(0, length - i);
                    // a marker that reached beyond the buffer consumed bytes not read yet
                    filePosition = Math.max(filePosition, windowStart);
                }
            }

            if (validChecksumCount == 0)
                return emptyList();
            // the generic records end with '*' and checksum: decline files where that is the exception
            if (!holux && validChecksumCount * 2 < count)
                return emptyList();

            List<GpxRoute> result = new ArrayList<>();
            // GPSBabel writes the waypoints before the tracks to GPX
            if (!wayPoints.isEmpty())
                result.add(new GpxRoute(new Gpx10Format(), Waypoints, null, null, wayPoints));
            for (GpxRoute track : tracks) {
                // an empty track is dropped by the GPX reader
                if (track.getPositionCount() > 0)
                    result.add(track);
            }
            return result;
        }

        /** mtk_parse_info(): parse an info block, may update bitmask, period, distance, speed */
        private int parseInfo(long offset, long length) {
            if (!(length >= INFO_SIZE && isLogReset(offset)))
                return 0;

            int command = u16(offset + 8);
            switch (u8(offset + 7)) {
                case 0x02 -> {
                    int mask = asBitmask(s32(offset + 8));
                    if (holux245)
                        mask &= ~HOLUX245_MASK;
                    if (bitmask != mask)
                        trackEvent |= MTK_EVT_BITMASK;
                    bitmask = mask;
                    logLength = logLength(bitmask);
                }
                case 0x03 -> {
                    trackEvent |= MTK_EVT_PERIOD;
                    if (holux)
                        trackEvent |= MTK_EVT_START;
                    period = command;
                }
                case 0x04 -> {
                    trackEvent |= MTK_EVT_DISTANCE;
                    if (holux)
                        trackEvent |= MTK_EVT_START;
                    distance = command;
                }
                case 0x05 -> {
                    trackEvent |= MTK_EVT_SPEED;
                    speed = command;
                }
                case 0x07 -> {
                    // logger turned on starts a new track on the generic devices; log disabled has no effect
                    if (!holux && command == 0x0106)
                        trackEvent |= MTK_EVT_START;
                }
                default -> {
                    // 0x06 log policy change has no effect
                }
            }
            return INFO_SIZE;
        }

        /** mtk_parse(): parse one record, returns its length */
        private int parse(long offset, int bitmask) {
            DataItem item = new DataItem();
            int i = 0;
            int checksum = 0;
            for (int k = 0; k < 32; k++) {
                if (((1 << k) & bitmask) == 0)
                    continue;

                long field = offset + i;
                switch (k) {
                    case UTC -> item.timestamp = s32(field);
                    case VALID -> item.valid = u16(field);
                    case LATITUDE -> item.latitude = logTypeSizes[LATITUDE] == 4 ? float32(field) : float64(field);
                    case LONGITUDE -> item.longitude = logTypeSizes[LONGITUDE] == 4 ? float32(field) : float64(field);
                    case HEIGHT -> {
                        if (holux245) {
                            // the GPSport 245 logs speed as centimeters/sec in the height position
                            int centimetersPerSecond = s32(field);
                            item.speed = (float) (centimetersPerSecond * 3600.0 / 1000.0 / 100.0);
                        } else
                            item.height = logTypeSizes[HEIGHT] == 3 ? float24(field) : float32(field);
                    }
                    case SPEED -> {
                        if (holux245)
                            // the GPSport 245 logs height in the speed position
                            item.height = float24(field);
                        else
                            item.speed = float32(field);
                    }
                    case HEADING -> item.heading = float32(field);
                    case PDOP -> item.pdop = (float) (u16(field) / 100.0);
                    case HDOP -> item.hdop = (float) (u16(field) / 100.0);
                    case VDOP -> item.vdop = (float) (u16(field) / 100.0);
                    case NSAT -> item.satellitesUsed = (byte) u8(field + 1);
                    case SID -> {
                        int satelliteCount = Math.min(u16(field + 2), 32);
                        int sidSize = logTypeSizes[SID];
                        if (satelliteCount > 0) { // handle 'Zero satellites in view issue'
                            if ((bitmask & (1 << ELEVATION)) != 0)
                                sidSize += logTypeSizes[ELEVATION];
                            if ((bitmask & (1 << AZIMUTH)) != 0)
                                sidSize += logTypeSizes[AZIMUTH];
                            if ((bitmask & (1 << SNR)) != 0)
                                sidSize += logTypeSizes[SNR];
                        }
                        // satellite id, elevation, azimuth and SNR are out of the scope of RouteConverter
                        do {
                            for (int l = 0; l < sidSize; l++)
                                checksum ^= u8(offset + i + l);
                            i += sidSize;
                            satelliteCount--;
                        } while (satelliteCount > 0);
                        continue; // checksum and length are done
                    }
                    case ELEVATION, AZIMUTH, SNR -> {
                        continue; // handled in SID
                    }
                    case RCR -> item.rcr = u16(field);
                    case MILLISECOND -> item.milliseconds = u16(field);
                    default -> {
                        // DSTA, DAGE, DISTANCE and unknown bits are not used
                    }
                }

                for (int j = 0; j < logTypeSizes[k]; j++)
                    checksum ^= u8(offset + i + j);
                i += logTypeSizes[k];
            }

            if (!holux) {
                if (u8(offset + i) == '*')
                    i++; // skip '*' separator
                else if (u8(offset + i) == 0xff)
                    // in some case star-crc hasn't been written on power off: skip the record including '*' and crc
                    return i + 2;
                // else: GPSBabel reports the missing '*' and compares the checksum with the byte in its place
            }

            if (isLogReset(offset)) {
                // missed log restart
                parseInfo(offset, logLength);
                return INFO_SIZE;
            }

            boolean validChecksum = u8(offset + i) == checksum;
            i++; // checksum
            count++;

            // GPSBabel only reports a bad checksum and keeps the record; spec 00083.12 skips it and
            // keeps a pending log restart or waypoint event for the next record
            if (validChecksum) {
                validChecksumCount++;
                addTrackPoint(count, bitmask, item);
                trackEvent = 0;
            }
            return i;
        }

        private static boolean has(int bitmask, int type) {
            return (bitmask & (1 << type)) != 0;
        }

        /** add_trackpoint() plus what GPSBabel 1.5.4 writes of it to GPX 1.0 */
        private void addTrackPoint(int index, int bitmask, DataItem item) {
            if (trackPositions == null || (trackEvent & MTK_EVT_START) != 0) {
                String name = "track-" + (tracks.size() + 1);
                String description = "Log every " + formatNoDecimals(period / 10.0) + " sec, " +
                        formatNoDecimals(distance / 10.0) + " m";
                if (speed > 0)
                    description += " when moving above " + formatNoDecimals(speed / 10.0) + " km/h";
                trackPositions = new ArrayList<>();
                tracks.add(new GpxRoute(new Gpx10Format(), Track, name, asDescription(description), trackPositions));
            }

            if (!(has(bitmask, LATITUDE) && has(bitmask, LONGITUDE)))
                return; // GPX requires lat/lon

            // GPSBabel writes nan or inf coordinates, which the GPX reader cannot read at all: drop the position
            if (!Double.isFinite(item.latitude) || !Double.isFinite(item.longitude))
                return;

            if (has(bitmask, VALID)) {
                // estimated mode (0x0040), "No Fix" (0x0001) and the unknown modes; SPS, DGPS and PPS are a fix
                boolean noFix = !(item.valid == 0x0002 || item.valid == 0x0004 || item.valid == 0x0008);
                // This is a flagrantly bogus position; don't queue it. The 747 does log some "real" positions
                // with fix_none, though, so keep those. (sic: GPSBabel compares latitude - 90.0, not its absolute value)
                if (noFix && item.latitude - 90.0 < .000001 && item.longitude < 0.000001)
                    return;
            }

            // RCR is a bitmask of possibly several log reasons, Holux devices use an event prefix for each waypoint
            if ((has(bitmask, RCR) && (item.rcr & 0x0008) != 0) || (trackEvent & MTK_EVT_WAYPT) != 0)
                // button press -- create waypoint, start count at 1
                wayPoints.add(createPosition(bitmask, item, String.format("WP%06d", wayPoints.size() + 1)));

            // the logger restarts the log session from the button press, so the waypoint is a track point, too
            trackPositions.add(createPosition(bitmask, item, String.format("TP%06d", index)));
        }

        private static GpxPosition createPosition(int bitmask, DataItem item, String description) {
            Double elevation = has(bitmask, HEIGHT) ? asGpxDecimal(item.height) : null;
            long millis = item.timestamp * 1000L;
            if (has(bitmask, MILLISECOND))
                millis += item.milliseconds;
            // GPSBabel 1.5.4 keeps the speed in m/s as a float
            Double speed = has(bitmask, SPEED) ? msToKmh(asGpxDecimal((float) (item.speed * 1000.0 / 3600.0))) : null;
            // GPSBabel 1.5.4 writes the creation time into GPX only if it is not zero
            GpxPosition position = new GpxPosition(asGpxCoordinate(item.longitude), asGpxCoordinate(item.latitude),
                    elevation, speed, item.timestamp != 0 ? fromMillis(millis) : null, description);
            if (has(bitmask, HEADING))
                position.setHeading(asGpxDecimal(item.heading));
            // GPSBabel 1.5.4 writes dops only if they are not zero and satellites only if they are positive
            if (has(bitmask, PDOP) && item.pdop != 0)
                position.setPdop(asGpxDecimal(item.pdop));
            if (has(bitmask, HDOP) && item.hdop != 0)
                position.setHdop(asGpxDecimal(item.hdop));
            if (has(bitmask, VDOP) && item.vdop != 0)
                position.setVdop(asGpxDecimal(item.vdop));
            if (has(bitmask, NSAT) && item.satellitesUsed > 0)
                position.setSatellites(item.satellitesUsed);
            if (has(bitmask, VALID))
                position.setFixQuality(asFixQuality(item.valid));
            return position;
        }

        // the fix GPSBabel derives from VALID as the GPX reader parses GPSBabel 1.5.4's <fix>
        private static Integer asFixQuality(int valid) {
            return switch (valid) {
                case 0x0001 -> 0; // "No Fix": none
                case 0x0002 -> 1; // "SPS": 3d
                case 0x0004 -> 2; // DGPS: dgps
                case 0x0008 -> 3; // Military GPS: pps
                default -> null; // estimated, RTK, FRTK, manual input, simulator: unknown, no <fix>
            };
        }
    }
}
