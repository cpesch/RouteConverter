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

package slash.navigation.garmin;

import slash.navigation.gpsbabel.LittleEndianOutput;

import java.util.Arrays;

import static java.nio.charset.StandardCharsets.US_ASCII;

/**
 * Builds synthetic Garmin MapSource (.mps) files of the versions 3, 4 and 5 for tests,
 * after the record layouts {@code mapsource.cc} reads.
 */
class MpsBuilder {
    static final double SEMICIRCLES_PER_DEGREE = (double) (1L << 31) / 180.0;

    private final int version;
    private final LittleEndianOutput output = new LittleEndianOutput();

    MpsBuilder(int version) {
        this(version, version == 3 ? 'd' : version == 4 ? 'g' : 'i');
    }

    MpsBuilder(int version, char flag) {
        this.version = version;
        output.writeBytes(new byte[]{'M', 's', 'R', 'c', 'd', 0});
        output.writeInt32(2);
        output.writeBytes(new byte[]{'D', (byte) flag, 0});
        byte[] signature = new byte[27];
        signature[0] = (byte) 0xF4;
        signature[1] = 0x01;
        record('A', signature);
    }

    static int semicircles(double degrees) {
        return (int) Math.round(degrees * SEMICIRCLES_PER_DEGREE);
    }

    static byte[] ascii(String string) {
        return string.getBytes(US_ASCII);
    }

    private static void string(LittleEndianOutput output, byte[] bytes) {
        if (bytes != null)
            output.writeBytes(bytes);
        output.writeUInt8(0);
    }

    private static void altitude(LittleEndianOutput output, Double altitude) {
        if (altitude == null) {
            output.writeBytes(new byte[9]);
        } else {
            output.writeUInt8(1);
            output.writeDouble(altitude);
        }
    }

    private static byte[] filled(int count) {
        byte[] bytes = new byte[count];
        Arrays.fill(bytes, (byte) 0xFF);
        return bytes;
    }

    MpsBuilder record(char type, byte[] payload) {
        return record(payload.length, type, payload);
    }

    MpsBuilder record(int length, char type, byte[] payload) {
        output.writeInt32(length);
        output.writeUInt8(type);
        output.writeBytes(payload);
        return this;
    }

    MpsBuilder raw(byte[] bytes) {
        output.writeBytes(bytes);
        return this;
    }

    byte[] waypointPayload(byte[] name, int waypointClass, double latitude, double longitude, Double altitude,
                           byte[] description, byte[] notes) {
        LittleEndianOutput record = new LittleEndianOutput();
        string(record, name);
        record.writeInt32(waypointClass);
        string(record, null);                               // country
        record.writeBytes(new byte[17]);                    // subclass
        if (version >= 4)
            record.writeBytes(new byte[5]);
        record.writeInt32(semicircles(latitude));
        record.writeInt32(semicircles(longitude));
        altitude(record, altitude);
        string(record, description);
        altitude(record, null);                             // proximity
        record.writeInt32(1);                               // display
        record.writeInt32(0);                               // colour
        record.writeInt32(18);                              // symbol
        record.writeBytes(new byte[3]);                     // city, state, facility
        record.writeUInt8(0);
        altitude(record, null);                             // depth
        if (version >= 4) {
            record.writeBytes(new byte[6]);
            string(record, notes);
        } else
            record.writeBytes(new byte[2]);
        return record.toByteArray();
    }

    MpsBuilder waypoint(String name, int waypointClass, double latitude, double longitude, Double altitude,
                        String description, String notes) {
        return waypoint(ascii(name), waypointClass, latitude, longitude, altitude,
                description != null ? ascii(description) : null, notes != null ? ascii(notes) : null);
    }

    MpsBuilder waypoint(byte[] name, int waypointClass, double latitude, double longitude, Double altitude,
                        byte[] description, byte[] notes) {
        return record('W', waypointPayload(name, waypointClass, latitude, longitude, altitude, description, notes));
    }

    record Point(String name, int waypointClass, double latitude, double longitude, Double altitude) {
    }

    private void routePointData(LittleEndianOutput record) {
        if (version >= 4) {
            record.writeBytes(new byte[4]);
            record.writeBytes(filled(12));
            record.writeBytes(new byte[2]);
            record.writeBytes(filled(4));
            record.writeUInt8(0);                           // terminates the variable length data
            record.writeUInt8(3);
            record.writeBytes(new byte[17]);
        } else {
            record.writeBytes(new byte[8]);
            record.writeBytes(filled(8));
            record.writeUInt8(0);
            record.writeUInt8(0);
            record.writeUInt8(3);
            record.writeBytes(new byte[16]);
        }
    }

    private static void position(LittleEndianOutput record, Point point) {
        record.writeInt32(semicircles(point.latitude()));
        record.writeInt32(semicircles(point.longitude()));
        altitude(record, point.altitude());
    }

    byte[] routePayload(String name, boolean bounds, Point... points) {
        LittleEndianOutput record = new LittleEndianOutput();
        string(record, ascii(name));
        record.writeUInt8(0);                               // autoname
        if (bounds) {
            record.writeUInt8(0);
            position(record, points[0]);
            position(record, points[0]);
        } else
            record.writeUInt8(1);
        record.writeInt32(points.length);
        for (int i = 0; i < points.length; i++) {
            Point point = points[i];
            string(record, ascii(point.name()));
            record.writeInt32(point.waypointClass());
            string(record, null);                           // country
            routePointData(record);
            if (i < points.length - 1) {
                record.writeInt32(2);
                position(record, point);
                position(record, points[i + 1]);
                record.writeUInt8(0);
                position(record, point);
                position(record, points[i + 1]);
            }
        }
        record.writeInt32(0);
        record.writeUInt8(1);
        return record.toByteArray();
    }

    MpsBuilder route(String name, Point... points) {
        return record('R', routePayload(name, true, points));
    }

    record TrackPoint(double latitude, double longitude, Double altitude, Integer time) {
    }

    byte[] trackPayload(String name, TrackPoint... points) {
        LittleEndianOutput record = new LittleEndianOutput();
        string(record, ascii(name));
        record.writeUInt8(1);                               // display
        record.writeInt32(0);                               // colour
        record.writeInt32(points.length);
        for (TrackPoint point : points) {
            record.writeInt32(semicircles(point.latitude()));
            record.writeInt32(semicircles(point.longitude()));
            altitude(record, point.altitude());
            if (point.time() != null) {
                record.writeUInt8(1);
                record.writeInt32(point.time());
            } else
                record.writeBytes(new byte[5]);
            altitude(record, null);                         // depth
        }
        return record.toByteArray();
    }

    MpsBuilder track(String name, TrackPoint... points) {
        return record('T', trackPayload(name, points));
    }

    MpsBuilder mapSet() {
        return record('V', new byte[]{0, 1});
    }

    byte[] toBytes() {
        return output.toByteArray();
    }
}
