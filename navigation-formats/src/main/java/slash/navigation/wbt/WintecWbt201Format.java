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

package slash.navigation.wbt;

import slash.navigation.base.BaseNavigationPosition;
import slash.navigation.base.ParserContext;
import slash.navigation.base.RouteCharacteristics;
import slash.navigation.base.SimpleFormat;
import slash.navigation.base.Wgs84Position;
import slash.navigation.base.Wgs84Route;
import slash.navigation.common.NavigationPosition;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import static java.lang.Long.parseLong;
import static java.nio.ByteBuffer.allocate;
import static java.nio.ByteOrder.LITTLE_ENDIAN;
import static java.util.Calendar.DAY_OF_MONTH;
import static java.util.Calendar.HOUR_OF_DAY;
import static java.util.Calendar.MILLISECOND;
import static java.util.Calendar.MINUTE;
import static java.util.Calendar.MONTH;
import static java.util.Calendar.SECOND;
import static java.util.Calendar.YEAR;
import static slash.common.type.CompactCalendar.UTC;
import static slash.common.type.CompactCalendar.createDateFormat;
import static slash.common.type.CompactCalendar.fromCalendar;
import static slash.navigation.base.RouteCharacteristics.Track;
import static slash.navigation.base.RouteCharacteristics.Waypoints;

/**
 * The base of all Wintec WBT-201 formats.
 *
 * @author Malte Neumann, Christian Pesch
 */

public abstract class WintecWbt201Format extends SimpleFormat<Wgs84Route> {
    private static final String TRACK_NAME_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";

    public String getName() {
        return "Wintec WBT-201 (*" + getExtension() + ")";
    }

    public int getMaximumPositionCount() {
        return UNLIMITED_MAXIMUM_POSITION_COUNT;
    }

    public boolean isSupportsReading() {
        return true;
    }

    public boolean isSupportsWriting() {
        return false;
    }

    public boolean isSupportsMultipleRoutes() {
        return true;
    }

    protected abstract int getHeaderSize();

    @SuppressWarnings({"unchecked"})
    public <P extends NavigationPosition> Wgs84Route createRoute(RouteCharacteristics characteristics, String name, List<P> positions) {
        return new Wgs84Route(this, characteristics, (List<Wgs84Position>) positions);
    }

    public void read(BufferedReader reader, String encoding, ParserContext<Wgs84Route> context) {
        // this format parses the InputStream directly but wants to derive from SimpleFormat to use Wgs84Route
        throw new UnsupportedOperationException();
    }

    public void write(Wgs84Route route, PrintWriter writer, int startIndex, int endIndex) {
        // this format parses the InputStream directly but wants to derive from SimpleFormat to use Wgs84Route
        throw new UnsupportedOperationException();
    }

    protected abstract boolean checkFormatDescriptor(ByteBuffer buffer) throws IOException;

    protected abstract List<Wgs84Route> internalRead(ByteBuffer buffer);

    public void read(InputStream source, ParserContext<Wgs84Route> context) throws Exception {
        byte[] header = new byte[getHeaderSize()];
        if (source.read(header) == getHeaderSize()) {

            // copy headerbytes in ByteBuffer, because header contains little endian int
            ByteBuffer headerBuffer = allocate(getHeaderSize());
            headerBuffer.position(0);
            headerBuffer.put(header);

            if (checkFormatDescriptor(headerBuffer)) {
                // read whole file in ByteBuffer with a size limit of about 2 MB
                int available = source.available();
                ByteBuffer sourceBuffer = allocate(header.length + available);
                byte[] data = new byte[available];
                if (source.read(data) != available)
                    throw new IOException("Could not read " + available + " bytes");

                sourceBuffer.position(0);
                sourceBuffer.put(header);
                sourceBuffer.put(data);

                context.appendRoutes(internalRead(sourceBuffer));
            }
        }
    }

    List<Wgs84Route> readPositions(ByteBuffer source, int startDataAddress, long trackInfoAddress) {
        /* http://forum.pocketnavigation.de/attachment.php?attachmentid=1082953
           2 byte Trackflag
               00001 = 1 --> That point is the start point of a trajectory
               00010 = 2 --> That point is push to log
               00100 = 4 --> That point is over speed point
               * The flag of one point may be combination with two or three flags.
                 The Wintec WSG-1000 has a lot more flags, don't now what they mean.

           4 byte Date & Time (UTC)
               6 bits year (+ 2000)
               4 bits month
               5 bits day
               5 bits hour
               6 bits minute
               6 bits second

           4 byte Latitude
               integer / 10000000 (degree)

           4 byte Longitude
               integer / 10000000 (degree)

           2 byte Altitude
               short in meters
        */

        // seek to begin of trackpoints
        source.position(startDataAddress);
        source.order(LITTLE_ENDIAN);

        List<Wgs84Route> result = new ArrayList<>();

        List<NavigationPosition> trackPoints = null;
        List<NavigationPosition> pushPoints = null;
        int trackPointNo = 1;
        int pushPointNo = 1;

        while ((source.position() < trackInfoAddress) && (source.position() + 2+4+4+4+2 <= source.capacity())) {
            short trackFlag = source.getShort();
            int time = source.getInt();
            int latitude = source.getInt();
            int longitude = source.getInt();
            short altitude = source.getShort();

            // if internal gps memory is full, the first points will override and no trackflag is set
            if ((trackPoints == null) && (trackFlag == 0))
                trackFlag = 1;

            if ((trackFlag & 1) == 1) {
                // new track
                trackPoints = new ArrayList<>();
                Wgs84Route track = createRoute(Track, null, trackPoints);
                result.add(track);
                trackPointNo = 1;

                // trackname = time of first point
                NavigationPosition newPoint = createWaypoint(time, latitude, longitude, altitude, 0, true);
                track.setName(createDateFormat(TRACK_NAME_DATE_FORMAT).format(newPoint.getTime().getTime()));
            }

            if ((trackFlag & 2) == 2) {
                // track pushpoint
                if (pushPoints == null) {
                    pushPoints = new ArrayList<>();
                    Wgs84Route points = createRoute(Waypoints, "Pushpoints", pushPoints);
                    result.add(points);
                }

                pushPoints.add(createWaypoint(time, latitude, longitude, altitude, pushPointNo++, false));
            }

            // all points are included in the track
            if (trackPoints != null)
                trackPoints.add(createWaypoint(time, latitude, longitude, altitude, trackPointNo++, true));
        }
        return result;
    }

    private static final long YEAR_MASK = parseLong("11111100000000000000000000000000", 2);
    private static final long MONTH_MASK = parseLong("00000011110000000000000000000000", 2);
    private static final long DAY_MASK = parseLong("00000000001111100000000000000000", 2);
    private static final long HOUR_MASK = parseLong("00000000000000011111000000000000", 2);
    private static final long MINUTE_MASK = parseLong("00000000000000000000111111000000", 2);
    private static final long SECOND_MASK = parseLong("00000000000000000000000000111111", 2);
    private static final double FACTOR = 10000000.0;

    protected BaseNavigationPosition createWaypoint(long time, long latitude, long longitude,
                                                    int altitude, int pointNo, boolean isTrackpoint) {
        int year = (int) ((time & YEAR_MASK) >> 26);
        int month = (int) ((time & MONTH_MASK) >> 22);
        int day = (int) ((time & DAY_MASK) >> 17);
        int hour = (int) ((time & HOUR_MASK) >> 12);
        int minute = (int) ((time & MINUTE_MASK) >> 6);
        int second = (int) ((time & SECOND_MASK));

        Calendar calendar = Calendar.getInstance(UTC);
        calendar.set(YEAR, 2000 + year);
        calendar.set(MONTH, month - 1);
        calendar.set(DAY_OF_MONTH, day);
        calendar.set(HOUR_OF_DAY, hour);
        calendar.set(MINUTE, minute);
        calendar.set(SECOND, second);
        calendar.set(MILLISECOND, 0);

        String description;
        if (isTrackpoint)
            description = "Trackpoint " + String.valueOf(pointNo);
        else
            description = "Pushpoint " + String.valueOf(pointNo);

        return new Wgs84Position(longitude / FACTOR, latitude / FACTOR, (double) altitude, null,
                fromCalendar(calendar), description);
    }

    public void write(Wgs84Route route, OutputStream target, int startIndex, int endIndex) {
        throw new UnsupportedOperationException();
    }
}
