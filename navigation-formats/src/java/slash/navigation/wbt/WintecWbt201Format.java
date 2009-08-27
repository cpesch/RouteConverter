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

import slash.navigation.*;
import slash.navigation.util.CompactCalendar;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * The base of all Wintec WBT-201 formats.
 *
 * @author Malte Neumann
 */

public abstract class WintecWbt201Format extends SimpleFormat<Wgs84Route> {
    private static final int HEADER_SIZE = 1024;
    private static final SimpleDateFormat TRACK_NAME_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

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

    @SuppressWarnings({"unchecked"})
    public <P extends BaseNavigationPosition> Wgs84Route createRoute(RouteCharacteristics characteristics, String name, List<P> positions) {
        return new Wgs84Route(this, characteristics, (List<Wgs84Position>) positions);
    }

    public List<Wgs84Route> read(BufferedReader reader, CompactCalendar startDate, String encoding) throws IOException {
        // this format parses the InputStream directly but wants to derive from SimpleFormat to use Wgs84Route
        throw new UnsupportedOperationException();
    }

    public void write(Wgs84Route route, PrintWriter writer, int startIndex, int endIndex) {
        // this format parses the InputStream directly but wants to derive from SimpleFormat to use Wgs84Route
        throw new UnsupportedOperationException();
    }


    protected abstract boolean checkFormatDescriptor(ByteBuffer sourceHeader) throws IOException;

    protected abstract List<Wgs84Route> read(ByteBuffer source) throws IOException;

    public List<Wgs84Route> read(InputStream source, CompactCalendar startDate) throws IOException {
        List<Wgs84Route> result = null;

        byte[] header = new byte[HEADER_SIZE];
        if (source.read(header) == HEADER_SIZE) {

            // copy headerbytes in ByteBuffer, because header contains little endian int
            ByteBuffer headerBuffer = ByteBuffer.allocate(HEADER_SIZE);
            headerBuffer.position(0);
            headerBuffer.put(header);

            if (checkFormatDescriptor(headerBuffer)) {
                // read whole file in ByteBuffer. Max. size ca. 2 MB
                ByteBuffer sourceData = ByteBuffer.allocate(header.length + source.available());
                int available = source.available();
                byte[] data = new byte[available];
                if (source.read(data) != available)
                    throw new IOException("Could not read " + available + " bytes");

                sourceData.position(0);
                sourceData.put(header);
                sourceData.put(data);

                result = read(sourceData);
            }
        }
        return result;
    }

    List<Wgs84Route> readPositions(ByteBuffer source, long trackInfoAddress) {
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
        source.position(1024);
        source.order(ByteOrder.LITTLE_ENDIAN);

        List<Wgs84Route> result = new ArrayList<Wgs84Route>();

        List<BaseNavigationPosition> trackPoints = null;
        List<BaseNavigationPosition> pushPoints = null;
        int trackPointNo = 1;
        int pushPointNo = 1;

        while ((source.position() < trackInfoAddress) && (source.position() < source.capacity())) {
            int trackFlag = source.getShort();
            long time = source.getInt();
            long latitude = source.getInt();
            long longitude = source.getInt();
            int altitude = source.getShort();

            // if internal gps memory is full, the first points will override and no trackflag is set
            if ((trackPoints == null) && (trackFlag == 0))
                trackFlag = 1;

            if ((trackFlag & 1) == 1) {
                // new track
                trackPoints = new ArrayList<BaseNavigationPosition>();
                Wgs84Route track = createRoute(RouteCharacteristics.Track, null, trackPoints);
                result.add(track);
                trackPointNo = 1;

                // trackname = time of first point
                BaseNavigationPosition newPoint = createWaypoint(time, latitude, longitude, altitude, 0, true);
                track.setName(TRACK_NAME_DATE_FORMAT.format(newPoint.getTime().getTime()));
            }

            if ((trackFlag & 2) == 2) {
                // track pushpoint
                if (pushPoints == null) {
                    pushPoints = new ArrayList<BaseNavigationPosition>();
                    Wgs84Route points = createRoute(RouteCharacteristics.Waypoints, "Pushpoints", pushPoints);
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

    private static final long YEAR_MASK = Long.parseLong("11111100000000000000000000000000", 2);
    private static final long MONTH_MASK = Long.parseLong("00000011110000000000000000000000", 2);
    private static final long DAY_MASK = Long.parseLong("00000000001111100000000000000000", 2);
    private static final long HOUR_MASK = Long.parseLong("00000000000000011111000000000000", 2);
    private static final long MINUTE_MASK = Long.parseLong("00000000000000000000111111000000", 2);
    private static final long SECOND_MASK = Long.parseLong("00000000000000000000000000111111", 2);
    private static final double FACTOR = 10000000.0;

    private BaseNavigationPosition createWaypoint(long time, long latitude, long longitude,
                                                  int altitude, int pointNo, boolean isTrackpoint) {
        int year = (int) ((time & YEAR_MASK) >> 26);
        int month = (int) ((time & MONTH_MASK) >> 22);
        int day = (int) ((time & DAY_MASK) >> 17);
        int hour = (int) ((time & HOUR_MASK) >> 12);
        int minute = (int) ((time & MINUTE_MASK) >> 6);
        int second = (int) ((time & SECOND_MASK));

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.YEAR, 2000 + year);
        calendar.set(Calendar.MONTH, month - 1); // Java month starts with 0
        calendar.set(Calendar.DAY_OF_MONTH, day);
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, second);

        String comment;
        if (isTrackpoint)
            comment = "Trackpoint " + String.valueOf(pointNo);
        else
            comment = "Pushpoint " + String.valueOf(pointNo);

        return new Wgs84Position(longitude / FACTOR, latitude / FACTOR, (double) altitude, null,
                CompactCalendar.fromCalendar(calendar), comment);
    }

    public void write(Wgs84Route route, File target, int startIndex, int endIndex) throws IOException {
        throw new UnsupportedOperationException();
    }
}