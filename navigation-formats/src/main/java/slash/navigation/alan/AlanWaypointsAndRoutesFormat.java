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
import slash.navigation.base.MultipleRoutesFormat;
import slash.navigation.gpsbabel.LittleEndianInput;
import slash.navigation.gpsbabel.LittleEndianOutput;
import slash.navigation.gpx.Gpx10Format;
import slash.navigation.gpx.GpxPosition;
import slash.navigation.gpx.GpxRoute;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.prefs.Preferences;

import static java.lang.Math.floorDiv;
import static java.lang.Math.min;
import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.Locale.ROOT;
import static slash.common.io.Transfer.trim;
import static slash.common.type.CompactCalendar.fromMillis;
import static slash.navigation.alan.Map500.asGpxCoordinate;
import static slash.navigation.alan.Map500.deg2pt;
import static slash.navigation.alan.Map500.fromLabel;
import static slash.navigation.alan.Map500.isPlausibleTime;
import static slash.navigation.alan.Map500.packTime;
import static slash.navigation.alan.Map500.pt2deg;
import static slash.navigation.alan.Map500.str2lab;
import static slash.navigation.alan.Map500.unpackTime;
import static slash.navigation.alan.Map500.wrapCoordinates;
import static slash.navigation.base.RouteCharacteristics.Route;
import static slash.navigation.base.RouteCharacteristics.Waypoints;
import static slash.navigation.common.NavigationConversion.formatPosition;

/**
 * Reads and writes Alan Map 500 Waypoints and Routes (.wpr) files.
 *
 * @author Christian Pesch
 */

public class AlanWaypointsAndRoutesFormat extends GpxBackedNavigationFormat implements MultipleRoutesFormat<GpxRoute> {
    // the preference node of the GPSBabel-backed predecessor in slash.navigation.babel
    private static final Preferences preferences = Preferences.userRoot().node("slash/navigation/babel");
    static final String MAXIMUM_POSITION_COUNT_PREFERENCE = "maximumAlanWaypointsAndRoutesPositionCount";

    static final int MAXWPT = 1000;
    static final int MAXRTE = 50;
    static final int MAXWPTINRTE = 150;
    static final int WPT_NAME_LEN = 8;
    static final int WPT_COMMENT_LEN = 12;
    static final int RTE_NAME_LEN = 8;
    static final int RTE_COMMENT_LEN = 12;

    // struct sizes of alan.cc on the map500 (little-endian, natural alignment)
    static final int WPTHDR_SIZE = 4 + 2 + 2 + 2 * MAXWPT + MAXWPT;                            // 3008
    static final int WPT_SIZE = WPT_NAME_LEN + WPT_COMMENT_LEN + 4 + 4 + 4 + 4 + 2 + 1 + 1;   // 40
    static final int RTEHDR_SIZE = 4 + 2 + 2 + 2 * MAXRTE + MAXRTE + 2;                        // 160
    static final int RTE_SIZE = RTE_NAME_LEN + RTE_COMMENT_LEN + 2 + 2 * MAXWPTINRTE + 2 + 4 + 4; // 332
    static final int WPRDATA_SIZE = WPTHDR_SIZE + MAXWPT * WPT_SIZE + RTEHDR_SIZE + MAXRTE * RTE_SIZE; // 59768
    static final int WPT_OFFSET = WPTHDR_SIZE;
    static final int RTEHDR_OFFSET = WPT_OFFSET + MAXWPT * WPT_SIZE;
    static final int RTE_OFFSET = RTEHDR_OFFSET + RTEHDR_SIZE;

    static final long WPT_HDR_ID = 0x5C38A600L;
    static final long RTE_HDR_ID = 0xD87F5900L;
    private static final short WPT_IDX_NONE = -1;
    private static final byte WPT_USED = (byte) 0xff;
    private static final short RTE_IDX_NONE = -1;
    private static final byte RTE_USED = (byte) 0xff;
    private static final short RTE_RTENO_NONE = -1;

    private final Clock clock;

    public AlanWaypointsAndRoutesFormat() {
        this(Clock.systemUTC());
    }

    AlanWaypointsAndRoutesFormat(Clock clock) {
        this.clock = clock;
    }

    public String getExtension() {
        return ".wpr";
    }

    public String getName() {
        return "Alan Map 500 Waypoints and Routes (*" + getExtension() + ")";
    }

    public int getMaximumPositionCount() {
        return preferences.getInt(MAXIMUM_POSITION_COUNT_PREFERENCE, MAXWPTINRTE);
    }

    public boolean isSupportsMultipleRoutes() {
        // the format supports more than one route per file but if set to true the current multiple routes per
        // file and splitting logic ignores the maximum position count and only one unreadable file is created
        return false;
    }

    // reading

    protected List<GpxRoute> readRoutes(InputStream source) throws IOException {
        // like alan.cc wpr_read(), which reads the size of the struct and ignores what follows
        byte[] data = source.readNBytes(WPRDATA_SIZE);
        if (data.length != WPRDATA_SIZE)
            return emptyList();

        LittleEndianInput input = new LittleEndianInput(data);
        if (input.readUInt32() != WPT_HDR_ID)
            return emptyList();
        input.seek(RTEHDR_OFFSET);
        if (input.readUInt32() != RTE_HDR_ID)
            return emptyList();

        WprData wpr = WprData.read(data);
        List<GpxRoute> result = new ArrayList<>();

        List<GpxPosition> waypoints = new ArrayList<>();
        for (int i = 0; i < MAXWPT; i++) {
            GpxPosition position = readWaypoint(wpr, i);
            if (position != null)
                waypoints.add(position);
        }
        // the GPX reader drops a waypoint list without waypoints
        if (!waypoints.isEmpty())
            result.add(new GpxRoute(new Gpx10Format(), Waypoints, null, null, waypoints));

        for (int i = 0; i < MAXRTE; i++) {
            int idx = wpr.rteIdx[i];
            // alan.cc reads behind its arrays for an index out of range
            if (idx == RTE_IDX_NONE || idx < 0 || idx >= MAXRTE || wpr.rteUsed[idx] == 0)
                continue;
            Rte rte = wpr.rte[idx];

            List<GpxPosition> positions = new ArrayList<>();
            for (int j = 0; j < min(rte.wptnum, MAXWPTINRTE); j++) {
                GpxPosition position = readWaypoint(wpr, rte.wptidx[j]);
                if (position != null)
                    positions.add(position);
            }
            String name = stripIllegalXmlCharacters(trim(fromLabel(rte.name, 0, RTE_NAME_LEN, false)));
            String comment = stripIllegalXmlCharacters(trim(fromLabel(rte.comment, 0, RTE_COMMENT_LEN, false)));
            result.add(new GpxRoute(new Gpx10Format(), Route, name, asDescription(comment), positions));
        }
        return result;
    }

    // alan.cc get_wpt()
    private GpxPosition readWaypoint(WprData wpr, int n) {
        if (n < 0 || n >= MAXWPT)
            return null;
        int idx = wpr.wptIdx[n];
        if (idx == WPT_IDX_NONE || idx < 0 || idx >= MAXWPT || wpr.wptUsed[idx] == 0)
            return null;
        Wpt wpt = wpr.wpt[idx];

        // like GPSBabel's waypt_add() for the waypoints, but an out-of-range point is skipped (D23)
        double[] coordinates = wrapCoordinates(pt2deg(wpt.x), -pt2deg(wpt.y));
        if (coordinates == null)
            return null;
        double longitude = asGpxCoordinate(coordinates[0]);
        double latitude = asGpxCoordinate(coordinates[1]);
        // an implausible date is no time (D23)
        Long seconds = isPlausibleTime(wpt.date, wpt.time) ? unpackTime(wpt.date, wpt.time) : null;
        String name = stripIllegalXmlCharacters(trim(fromLabel(wpt.name, 0, WPT_NAME_LEN, false)));
        String comment = stripIllegalXmlCharacters(trim(fromLabel(wpt.comment, 0, WPT_COMMENT_LEN, false)));
        // GPSBabel 1.5.4 writes the creation time into GPX only if it is not zero
        return new GpxPosition(longitude, latitude, null, null,
                seconds != null && seconds != 0 ? fromMillis(seconds * 1000) : null,
                asDescription(name, comment));
    }

    // writing

    public void write(GpxRoute route, OutputStream target, int startIndex, int endIndex) throws IOException {
        // the positions of the route go to GPSBabel as waypoints and as a route
        List<GpxPosition> positions = route.getPositions().subList(startIndex, endIndex);
        WprData wpr = new WprData();
        for (GpxPosition position : positions)
            addWaypoint(wpr, position, false);
        addRoute(wpr, route, positions);
        target.write(wpr.toBytes());
    }

    public void write(List<GpxRoute> routes, OutputStream target) throws IOException {
        // GPSBabel handles all waypoints before the routes, tracks are not written
        WprData wpr = new WprData();
        for (GpxRoute route : routes) {
            if (route.getCharacteristics() == Waypoints)
                for (GpxPosition position : route.getPositions())
                    addWaypoint(wpr, position, false);
        }
        for (GpxRoute route : routes) {
            if (route.getCharacteristics() == Route)
                addRoute(wpr, route, route.getPositions());
        }
        target.write(wpr.toBytes());
    }

    private static boolean hasCoordinates(GpxPosition position) {
        // the GPX written for GPSBabel skips positions without valid coordinates
        return formatPosition(position.getLongitude()) != null && formatPosition(position.getLatitude()) != null;
    }

    /**
     * The waypoint as GPSBabel 1.5.4 hands it to alan.cc after reading the GPX that
     * Gpx10Format(false, true) writes: name as shortname, desc as notes and no cmt, so
     * the description is empty. waypt_add() then names a waypoint without name after its
     * notes or "WPT%03d" of the number of waypoints and fills its description from the
     * notes or the shortname; route_add_wpt() names a route point without name
     * "RPT%03d" of the number of route points and leaves its description empty.
     */
    private Wpt asPattern(WprData wpr, GpxPosition position, boolean isRoute) {
        Wpt wpt = new Wpt();
        String name = stripIllegalXmlCharacters(asName(position.getDescription()));
        String notes = stripIllegalXmlCharacters(asDesc(position.getDescription()));
        if (isRoute) {
            wpr.routePointCount++;
            wpt.shortName = name != null ? name : format(ROOT, "RPT%03d", wpr.routePointCount);
            wpt.description = null;
        } else {
            wpr.waypointCount++;
            wpt.shortName = name != null ? name : notes != null ? notes : format(ROOT, "WPT%03d", wpr.waypointCount);
            wpt.description = notes != null ? notes : wpt.shortName;
        }
        wpt.x = deg2pt(formatPosition(position.getLongitude()).doubleValue());
        wpt.y = deg2pt(-formatPosition(position.getLatitude()).doubleValue());
        wpt.seconds = position.getTime() != null ? floorDiv(position.getTime().getTimeInMillis(), 1000L) : 0;
        return wpt;
    }

    // alan.cc find_wpt()
    private static int findWaypoint(WprData wpr, Wpt pattern) {
        byte[] name = str2lab(pattern.shortName, WPT_NAME_LEN, null, 0);
        for (int i = 0; i < MAXWPT; i++) {
            int idx = wpr.wptIdx[i];
            if (idx == WPT_IDX_NONE || wpr.wptUsed[idx] == 0)
                continue;
            Wpt wpt = wpr.wpt[idx];
            if (Arrays.equals(wpt.name, name) && wpt.x == pattern.x && wpt.y == pattern.y)
                return i;
        }
        return -1;
    }

    private static int firstIndex(short[] values, short value) {
        int i = 0;
        while (i < values.length && values[i] != value)
            i++;
        return i;
    }

    private static int firstIndex(byte[] values, byte value) {
        int i = 0;
        while (i < values.length && values[i] != value)
            i++;
        return i;
    }

    // alan.cc add_wpt()
    private int addWaypoint(WprData wpr, GpxPosition position, boolean isRoute) throws IOException {
        if (!hasCoordinates(position))
            return -1;
        Wpt pattern = asPattern(wpr, position, isRoute);

        int hdrIdx = findWaypoint(wpr, pattern);
        if (hdrIdx >= 0) {
            // duplicate waypoint
            if (isRoute)
                wpr.wpt[wpr.wptIdx[hdrIdx]].usecount++;
            return hdrIdx;
        }

        hdrIdx = firstIndex(wpr.wptIdx, WPT_IDX_NONE);
        int wptIdx = firstIndex(wpr.wptUsed, (byte) 0);
        if (wpr.wptNum >= MAXWPT || hdrIdx >= MAXWPT || wptIdx >= MAXWPT)
            throw new IOException("Can't store more than " + MAXWPT + " waypoints");

        Wpt wpt = wpr.wpt[wptIdx];
        wpt.name = str2lab(pattern.shortName, WPT_NAME_LEN, "W%05d", wptIdx);
        wpt.comment = str2lab(pattern.description, WPT_COMMENT_LEN, null, 0);
        wpt.x = pattern.x;
        wpt.y = pattern.y;
        wpt.usecount = (short) (isRoute ? 1 : 0);
        wpt.checked = (byte) (isRoute ? 0 : 1);
        int[] dateAndTime = packTime(pattern.seconds);
        wpt.date = dateAndTime[0];
        wpt.time = dateAndTime[1];

        wpr.wptIdx[hdrIdx] = (short) wptIdx;
        wpr.wptUsed[wptIdx] = WPT_USED;
        wpr.wptNum++;
        wpr.wptNext++;
        if (wpr.wptNext >= MAXWPT)
            wpr.wptNext = 0;
        return hdrIdx;
    }

    // alan.cc wpr_route_hdr() and wpr_route_wpt()
    private void addRoute(WprData wpr, GpxRoute route, List<GpxPosition> positions) throws IOException {
        int hdrIdx = firstIndex(wpr.rteIdx, RTE_IDX_NONE);
        int rteIdx = firstIndex(wpr.rteUsed, (byte) 0);
        if (wpr.rteNum >= MAXRTE || hdrIdx >= MAXRTE || rteIdx >= MAXRTE)
            throw new IOException("Can't store more than " + MAXRTE + " routes");

        Rte rte = wpr.rte[rteIdx];
        rte.name = str2lab(stripIllegalXmlCharacters(asRouteName(route.getName())), RTE_NAME_LEN, "R%03d", rteIdx);
        rte.comment = str2lab(stripIllegalXmlCharacters(trim(asDescription(route.getDescription()))), RTE_COMMENT_LEN, null, 0);
        // GPSBabel stamps the moment of writing into the route
        int[] dateAndTime = packTime(floorDiv(clock.millis(), 1000L));
        rte.date = dateAndTime[0];
        rte.time = dateAndTime[1];

        wpr.rteIdx[hdrIdx] = (short) rteIdx;
        wpr.rteUsed[rteIdx] = RTE_USED;
        wpr.rteNum++;
        wpr.rteNext++;
        if (wpr.rteNext >= MAXRTE)
            wpr.rteNext = 0;

        Rte current = wpr.rte[wpr.rteNum - 1];
        for (GpxPosition position : positions) {
            if (!hasCoordinates(position))
                continue;
            if (current.wptnum >= MAXWPTINRTE)
                throw new IOException("Can't store more than " + MAXWPTINRTE + " waypoints per route");
            int wptIdx = addWaypoint(wpr, position, true);
            current.wptidx[current.wptnum] = (short) wptIdx;
            current.wptnum++;
        }
    }

    private String asDescription(List<String> strings) {
        if (strings == null)
            return null;
        return String.join(",\n", strings);
    }

    // alan.cc struct wpt; shortName, description and seconds only describe a waypoint to add
    private static class Wpt {
        byte[] name = new byte[WPT_NAME_LEN];
        byte[] comment = new byte[WPT_COMMENT_LEN];
        int x, y, date, time;
        short usecount;
        byte checked, reserved;
        String shortName, description;
        long seconds;
    }

    // alan.cc struct rte
    private static class Rte {
        byte[] name = new byte[RTE_NAME_LEN];
        byte[] comment = new byte[RTE_COMMENT_LEN];
        short wptnum;
        short[] wptidx = new short[MAXWPTINRTE];
        short reserved;
        int date, time;
    }

    // alan.cc struct wprdata, initialized like wpr_write() does
    private static class WprData {
        // the waypoints and route points GPSBabel has read so far
        int waypointCount, routePointCount;
        short wptNum, wptNext;
        final short[] wptIdx = new short[MAXWPT];
        final byte[] wptUsed = new byte[MAXWPT];
        final Wpt[] wpt = new Wpt[MAXWPT];
        short rteNum, rteNext, rteNo = RTE_RTENO_NONE;
        final short[] rteIdx = new short[MAXRTE];
        final byte[] rteUsed = new byte[MAXRTE];
        final Rte[] rte = new Rte[MAXRTE];

        WprData() {
            Arrays.fill(wptIdx, WPT_IDX_NONE);
            Arrays.fill(rteIdx, RTE_IDX_NONE);
            for (int i = 0; i < MAXWPT; i++)
                wpt[i] = new Wpt();
            for (int i = 0; i < MAXRTE; i++)
                rte[i] = new Rte();
        }

        static WprData read(byte[] data) throws IOException {
            WprData result = new WprData();
            LittleEndianInput input = new LittleEndianInput(data);
            input.skip(4);
            result.wptNum = (short) input.readInt16();
            result.wptNext = (short) input.readInt16();
            for (int i = 0; i < MAXWPT; i++)
                result.wptIdx[i] = (short) input.readInt16();
            for (int i = 0; i < MAXWPT; i++)
                result.wptUsed[i] = (byte) input.readUInt8();
            for (int i = 0; i < MAXWPT; i++) {
                Wpt wpt = result.wpt[i];
                wpt.name = input.readBytes(WPT_NAME_LEN);
                wpt.comment = input.readBytes(WPT_COMMENT_LEN);
                wpt.x = input.readInt32();
                wpt.y = input.readInt32();
                wpt.date = input.readInt32();
                wpt.time = input.readInt32();
                wpt.usecount = (short) input.readInt16();
                wpt.checked = (byte) input.readUInt8();
                wpt.reserved = (byte) input.readUInt8();
            }
            input.skip(4);
            result.rteNum = (short) input.readInt16();
            result.rteNext = (short) input.readInt16();
            for (int i = 0; i < MAXRTE; i++)
                result.rteIdx[i] = (short) input.readInt16();
            for (int i = 0; i < MAXRTE; i++)
                result.rteUsed[i] = (byte) input.readUInt8();
            result.rteNo = (short) input.readInt16();
            for (int i = 0; i < MAXRTE; i++) {
                Rte rte = result.rte[i];
                rte.name = input.readBytes(RTE_NAME_LEN);
                rte.comment = input.readBytes(RTE_COMMENT_LEN);
                rte.wptnum = (short) input.readInt16();
                for (int j = 0; j < MAXWPTINRTE; j++)
                    rte.wptidx[j] = (short) input.readInt16();
                rte.reserved = (short) input.readInt16();
                rte.date = input.readInt32();
                rte.time = input.readInt32();
            }
            return result;
        }

        byte[] toBytes() {
            LittleEndianOutput output = new LittleEndianOutput();
            output.writeUInt32(WPT_HDR_ID);
            output.writeInt16(wptNum);
            output.writeInt16(wptNext);
            for (short idx : wptIdx)
                output.writeInt16(idx);
            output.writeBytes(wptUsed);
            for (Wpt wpt : wpt) {
                output.writeBytes(wpt.name);
                output.writeBytes(wpt.comment);
                output.writeInt32(wpt.x);
                output.writeInt32(wpt.y);
                output.writeInt32(wpt.date);
                output.writeInt32(wpt.time);
                output.writeInt16(wpt.usecount);
                output.writeUInt8(wpt.checked & 0xff);
                output.writeUInt8(wpt.reserved & 0xff);
            }
            output.writeUInt32(RTE_HDR_ID);
            output.writeInt16(rteNum);
            output.writeInt16(rteNext);
            for (short idx : rteIdx)
                output.writeInt16(idx);
            output.writeBytes(rteUsed);
            output.writeInt16(rteNo);
            for (Rte rte : rte) {
                output.writeBytes(rte.name);
                output.writeBytes(rte.comment);
                output.writeInt16(rte.wptnum);
                for (short idx : rte.wptidx)
                    output.writeInt16(idx);
                output.writeInt16(rte.reserved);
                output.writeInt32(rte.date);
                output.writeInt32(rte.time);
            }
            return output.toByteArray();
        }
    }
}
