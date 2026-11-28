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

package slash.navigation.topo;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

import static java.nio.charset.StandardCharsets.US_ASCII;
import static slash.navigation.topo.NationalGeographicTopo3Format.CHARSET;

/**
 * Builds synthetic TOPO! 3.x/4.x files: a 512 byte header with the version string and the end of the
 * embedded symbol image, then a chain of blocks as tpo_find_block() follows it.
 */
class TopoFileBuilder {
    private final String version;
    private final List<Integer> blockTypes = new ArrayList<>();
    private final List<byte[]> blockPayloads = new ArrayList<>();

    TopoFileBuilder() {
        this("TOPO! Ver. 3.4.3");
    }

    TopoFileBuilder(String version) {
        this.version = version;
    }

    static int coordinate(double degrees) {
        return (int) Math.round(degrees * 0x800000);
    }

    static byte[] header(String version, boolean withStartSection) {
        Bytes bytes = new Bytes();
        byte[] versionBytes = version.getBytes(US_ASCII);
        bytes.u8(versionBytes.length);
        bytes.raw(versionBytes);
        bytes.u8(0);
        bytes.zeros(64 - bytes.size());
        if (withStartSection)
            bytes.raw("Red Without Arrow".getBytes(US_ASCII));
        bytes.zeros(512 - bytes.size());
        return bytes.toByteArray();
    }

    TopoFileBuilder block(int type, Bytes payload) {
        blockTypes.add(type);
        blockPayloads.add(payload.toByteArray());
        return this;
    }

    byte[] build() {
        Bytes bytes = new Bytes();
        bytes.raw(header(version, true));
        if (blockTypes.isEmpty()) {
            // a single unknown block ends the chain
            bytes.i32(0x7f0000);
            bytes.i32(0);
        }
        for (int i = 0; i < blockTypes.size(); i++) {
            byte[] payload = blockPayloads.get(i);
            int next = bytes.size() + 8 + payload.length;
            bytes.i32(blockTypes.get(i));
            bytes.i32(i < blockTypes.size() - 1 ? next : 0);
            bytes.raw(payload);
        }
        return bytes.toByteArray();
    }

    /**
     * Little-endian bytes with tpo_read_int()'s variable-length integers.
     */
    static class Bytes {
        private final ByteArrayOutputStream out = new ByteArrayOutputStream();

        Bytes u8(int value) {
            out.write(value);
            return this;
        }

        Bytes u16(int value) {
            return u8(value & 0xff).u8((value >> 8) & 0xff);
        }

        Bytes i32(int value) {
            return u16(value & 0xffff).u16((value >> 16) & 0xffff);
        }

        Bytes raw(byte[] bytes) {
            out.writeBytes(bytes);
            return this;
        }

        Bytes zeros(int count) {
            return raw(new byte[count]);
        }

        Bytes varint(long value) {
            if (value < 0xfe)
                return u8((int) value);
            if (value <= 0xffff)
                return u8(0xfe).u16((int) value);
            return u8(0xff).i32((int) value);
        }

        Bytes string(String string) {
            byte[] bytes = string.getBytes(CHARSET);
            return varint(bytes.length).raw(bytes);
        }

        Bytes point(double longitude, double latitude) {
            return i32(coordinate(longitude)).i32(coordinate(latitude));
        }

        int size() {
            return out.size();
        }

        byte[] toByteArray() {
            return out.toByteArray();
        }
    }

    static Bytes styles(Bytes... styles) {
        Bytes bytes = new Bytes().varint(styles.length);
        for (Bytes style : styles)
            bytes.raw(style.toByteArray());
        return bytes;
    }

    static Bytes style(int red, int green, int blue, String name, int wide, int dash) {
        byte[] nameBytes = name.getBytes(CHARSET);
        return new Bytes().u8(0x0f).u8(0x00).u8(red).u8(green).u8(blue).u8(0)
                .u8(nameBytes.length).raw(nameBytes).u8(wide).u8(dash).u8(0).u8(0);
    }

    static Bytes tracks(Bytes... tracks) {
        Bytes bytes = new Bytes().varint(tracks.length);
        for (Bytes track : tracks)
            bytes.raw(track.toByteArray());
        return bytes;
    }

    static Bytes track(int style, String name, Bytes data) {
        byte[] trackBytes = data.toByteArray();
        return new Bytes().varint(0).varint(style).varint(1234).string(name).varint(trackBytes.length).raw(trackBytes);
    }

    static Bytes waypoints(Bytes... waypoints) {
        Bytes bytes = new Bytes().varint(waypoints.length);
        for (Bytes waypoint : waypoints)
            bytes.raw(waypoint.toByteArray());
        return bytes;
    }

    static Bytes waypoint(String name, double longitude, double latitude, int centimeters, String comment) {
        return new Bytes().varint(0).varint(0).string(name).varint(0).point(longitude, latitude)
                .i32(centimeters).string(comment).u8(0).u8(0).u8(0).u8(0);
    }

    static Bytes routes(Bytes... routes) {
        Bytes bytes = new Bytes().varint(routes.length);
        for (Bytes route : routes)
            bytes.raw(route.toByteArray());
        return bytes;
    }

    static Bytes route(String name, int... waypointNumbers) {
        Bytes bytes = new Bytes().varint(0).varint(0).string(name).varint(0).varint(waypointNumbers.length);
        for (int number : waypointNumbers)
            bytes.varint(number);
        return bytes;
    }

    static Bytes mapNotes(Bytes... notes) {
        Bytes bytes = new Bytes().varint(notes.length);
        for (Bytes note : notes)
            bytes.raw(note.toByteArray());
        return bytes;
    }

    static Bytes mapNote(double longitude, double latitude, String comment, String externalPath, String imagePath, int skip) {
        return new Bytes().varint(0).point(longitude, latitude).varint(0).varint(0).varint(0)
                .string(comment).string(externalPath).string(imagePath).varint(0).varint(0)
                .varint(skip).zeros(skip).varint(0).varint(0);
    }

    static Bytes symbols(double... coordinates) {
        Bytes bytes = new Bytes().varint(coordinates.length / 2);
        for (int i = 0; i < coordinates.length; i += 2)
            bytes.varint(0).varint(0).point(coordinates[i], coordinates[i + 1]);
        return bytes;
    }

    /** symbol styles as in the public tpo-sample3.tpo: 2 unknown, name, icon?, 12 unknown, 4 color bytes */
    static Bytes symbolStyles(String... names) {
        Bytes bytes = new Bytes().varint(names.length);
        for (String name : names)
            bytes.u8(0xff).u8(0xff).string(name).u8(7).zeros(12).u8(0xff).u8(0xff).u8(0).u8(0);
        return bytes;
    }

    /** symbols with their 1-based style */
    static Bytes styledSymbols(int[] styles, double... coordinates) {
        Bytes bytes = new Bytes().varint(styles.length);
        for (int i = 0; i < styles.length; i++)
            bytes.varint(0).varint(styles[i]).point(coordinates[2 * i], coordinates[2 * i + 1]);
        return bytes;
    }

    static Bytes textLabels(Bytes... labels) {
        Bytes bytes = new Bytes().varint(labels.length);
        for (Bytes label : labels)
            bytes.raw(label.toByteArray());
        return bytes;
    }

    static Bytes textLabel(double longitude, double latitude, String text) {
        return new Bytes().varint(0).varint(0).point(longitude, latitude).zeros(16).string(text);
    }
}
