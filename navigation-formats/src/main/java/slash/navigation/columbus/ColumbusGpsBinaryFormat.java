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

package slash.navigation.columbus;

import slash.common.type.CompactCalendar;
import slash.navigation.base.*;
import slash.navigation.common.NavigationPosition;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

import static java.lang.Long.parseLong;
import static java.nio.ByteBuffer.allocate;
import static java.nio.ByteBuffer.wrap;
import static java.nio.ByteOrder.LITTLE_ENDIAN;
import static java.util.Calendar.*;
import static slash.common.type.CompactCalendar.UTC;
import static slash.common.type.CompactCalendar.fromCalendar;
import static slash.navigation.base.RouteCharacteristics.Track;
import static slash.navigation.base.WaypointType.PointOfInterestC;
import static slash.navigation.base.WaypointType.Waypoint;
import static slash.navigation.columbus.ColumbusV1000Device.getTimeZone;
import static slash.navigation.columbus.ColumbusV1000Device.getUseLocalTimeZone;

/**
 * Reads Columbus GPS Binary (.gps) files.
 *
 * @author Christian Pesch
 */

public class ColumbusGpsBinaryFormat extends SimpleFormat<Wgs84Route> {
    private static final int HEADER_SIZE = 2;
    private static final short HEADER = 0x0707;
    private static final double COORDINATE_FACTOR = 1000000.0;
    private static final double ALTITUDE_FACTOR = 10.0; // 0.1m
    private static final double SPEED_FACTOR = 10.0; // 0.1km/h
    private static final double PRESSURE_FACTOR = 10.0; // 0.1hPa
    private static final double TEMPERATURE_FACTOR = 10.0; // 0.1Degress Celsius

    public String getName() {
        return "Columbus GPS Binary (*" + getExtension() + ")";
    }

    public String getExtension() {
        return ".gps";
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
    public <P extends NavigationPosition> Wgs84Route createRoute(RouteCharacteristics characteristics, String name, List<P> positions) {
        return new Wgs84Route(this, characteristics, name, (List<Wgs84Position>) positions);
    }

    public void read(BufferedReader reader, String encoding, ParserContext<Wgs84Route> context) {
        // this format parses the InputStream directly but wants to derive from SimpleFormat to use Wgs84Route
        throw new UnsupportedOperationException();
    }

    public void read(InputStream source, ParserContext<Wgs84Route> context) throws IOException {
        if (isValidHeader(source)) {
            int available = source.available();
            ByteBuffer body = allocate(available);
            byte[] data = new byte[available];
            if (source.read(data) != available)
                throw new IOException("Could not read " + available + " bytes");

            body.put(data);
            body.position(0);

            List<Wgs84Position> positions = internalRead(body);
            if (positions.size() > 0)
                context.appendRoute(new Wgs84Route(this, Track, null, positions));
        }
    }

    private boolean isValidHeader(InputStream inputStream) throws IOException {
        byte[] header = new byte[HEADER_SIZE];
        if (inputStream.read(header) == HEADER_SIZE) {
            ByteBuffer buffer = wrap(header);
            buffer.order(LITTLE_ENDIAN);
            return buffer.getShort() == HEADER;
        }
        return false;
    }

    private List<Wgs84Position> internalRead(ByteBuffer buffer) {
        List<Wgs84Position> result = new ArrayList<>();

        while (buffer.position() + 28 <= buffer.capacity()) {
            byte index0 = buffer.get();
            byte index1 = buffer.get();
            byte index2 = buffer.get();
            int index = (index2 & 0xFF) | ((index1 & 0xFF) << 8) | ((index0 & 0x0F) << 16);

            byte byte3 = buffer.get();
            WaypointType tag = parseTag(byte3);
            CompactCalendar time = parseTime(buffer.getInt());
            boolean isSouth = hasBitSet(byte3, 2);
            boolean isWest = hasBitSet(byte3, 3);
            double latitude = parseCoordinate(buffer.getInt(), isSouth);
            double longitude = parseCoordinate(buffer.getInt(), isWest);
            double altitude = buffer.getInt() / ALTITUDE_FACTOR;
            double speed = buffer.getShort() / SPEED_FACTOR;
            double heading = buffer.getShort();
            double pressure = buffer.getShort() / PRESSURE_FACTOR;
            double temperature = buffer.getShort() / TEMPERATURE_FACTOR;

            Wgs84Position position = new Wgs84Position(longitude, latitude, altitude, speed, time, "Trackpoint " + String.valueOf(index));
            position.setHeading(heading);
            position.setPressure(pressure);
            position.setTemperature(temperature);
            position.setWaypointType(tag);

            result.add(position);
        }
        return result;
    }

    boolean hasBitSet(byte aByte, int position) {
        return ((aByte >> position) & 1) == 1;
    }

    private WaypointType parseTag(byte aByte) {
        if (!hasBitSet(aByte, 0) && !hasBitSet(aByte, 1))
            return Waypoint;
        else if (hasBitSet(aByte, 0) && !hasBitSet(aByte, 1))
            return PointOfInterestC;
        return Waypoint;
    }

    private static final long YEAR_MASK = parseLong("11111100000000000000000000000000", 2);
    private static final long MONTH_MASK = parseLong("00000011110000000000000000000000", 2);
    private static final long DAY_MASK = parseLong("00000000001111100000000000000000", 2);
    private static final long HOUR_MASK = parseLong("00000000000000011111000000000000", 2);
    private static final long MINUTE_MASK = parseLong("00000000000000000000111111000000", 2);
    private static final long SECOND_MASK = parseLong("00000000000000000000000000111111", 2);

    private CompactCalendar parseTime(int time) {
        int year = (int) ((time & YEAR_MASK) >> 26);
        int month = (int) ((time & MONTH_MASK) >> 22);
        int day = (int) ((time & DAY_MASK) >> 17);
        int hour = (int) ((time & HOUR_MASK) >> 12);
        int minute = (int) ((time & MINUTE_MASK) >> 6);
        int second = (int) ((time & SECOND_MASK));

        Calendar calendar = Calendar.getInstance(UTC);
        calendar.set(YEAR, 2016 + year);
        calendar.set(MONTH, month - 1);
        calendar.set(DAY_OF_MONTH, day);
        calendar.set(HOUR_OF_DAY, hour);
        calendar.set(MINUTE, minute);
        calendar.set(SECOND, second);
        calendar.set(MILLISECOND, 0);

        CompactCalendar dateAndTime = fromCalendar(calendar);
        if(getUseLocalTimeZone())
            dateAndTime = dateAndTime.asUTCTimeInTimeZone(TimeZone.getTimeZone(getTimeZone()));
        return dateAndTime;
    }

    double parseCoordinate(int integer, boolean isSouthOrWest) {
        return integer / COORDINATE_FACTOR * (isSouthOrWest ? -1 : 1);
    }

    public void write(Wgs84Route route, PrintWriter writer, int startIndex, int endIndex) {
        // this format parses the InputStream directly but wants to derive from SimpleFormat to use Wgs84Route
        throw new UnsupportedOperationException();
    }

    public void write(Wgs84Route route, OutputStream target, int startIndex, int endIndex) {
        throw new UnsupportedOperationException();
    }
}
