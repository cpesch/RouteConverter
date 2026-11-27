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

    Translated to Java from GPSBabel garmin_gpi.cc and garmin_gpi.h (master 9c1e336) for RouteConverter, 2026; modified.
    Copyright (C) 2007 Olaf Klein, o.b.klein@gpsbabel.org
    Copyright (C) 2007-2012 Robert Lipe, robertlipe+source@gpsbabel.org

    The record layout and the de-obfuscation of format version 01 files are implemented from
    Herbert Oppmann, "Garmin Points of Interest (POI) File Format" (2026-07-24).
*/

package slash.navigation.garmin;

import slash.navigation.base.GpxBackedNavigationFormat;
import slash.navigation.gpsbabel.LittleEndianInput;
import slash.navigation.gpsbabel.LittleEndianOutput;
import slash.navigation.gpx.Gpx10Format;
import slash.navigation.gpx.GpxPosition;
import slash.navigation.gpx.GpxRoute;

import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;

import static java.nio.charset.StandardCharsets.US_ASCII;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.singletonList;
import static slash.navigation.base.RouteCharacteristics.Waypoints;
import static slash.navigation.common.NavigationConversion.formatPosition;
import static slash.navigation.gpsbabel.GpsBabelMath.semicirclesToDegrees;

/**
 * Reads and writes Garmin Points of Interest (.gpi) files: a list of records with nested POI
 * groups, areas and waypoints. Obfuscated files of format version 01 are de-obfuscated on read.
 * The address and contact records of a waypoint are read into its description; the writer keeps them
 * as part of the comment text, not as address and contact records.
 *
 * @author Christian Pesch
 */

public class GarminPoiFormat extends GpxBackedNavigationFormat {
    // Oppmann: record flags bit 3 = extra data present, a total length precedes the main length
    static final int EXTRA_DATA = 0x8;
    static final int HEADER1 = 0;
    static final int HEADER2 = 1;
    static final int WAYPOINT = 2;
    static final int ALERT = 3;
    static final int BITMAP_REFERENCE = 4;
    static final int BITMAP = 5;
    static final int AREA = 8;
    static final int POI_GROUP = 9;
    static final int COMMENT = 0xa;
    static final int ADDRESS = 0xb;
    static final int CONTACT = 0xc;
    static final int DESCRIPTION = 0xe;
    static final int END = 0xffff;

    // Oppmann: Header1 field Obfuscation
    static final int OBFUSCATION_UNTIL_ADDITIONAL_DATA = 3;
    static final int OBFUSCATION_UNTIL_END_OF_FILE = 5;
    private static final int[] OBFUSCATION_KEY = {0x48, 0x06, 0xb3, 0x00};

    static final int WAYPOINTS_PER_BLOCK = 128;
    static final String DEFAULT_CATEGORY = "My points";
    // GPSBabel's fixed Header1 name, read as "no name" and written for a route without a name
    static final String GPSBABEL_HEADER_NAME = "my.gpi";
    private static final int MAXIMUM_HEADER_NAME_LENGTH = 0xFFFF;
    private static final String DEFAULT_NAME = "POI";
    private static final int WRITE_CODEPAGE = 1252;
    private static final Charset WRITE_CHARSET = Charset.forName("windows-1252");
    // GPS_Math_Utime_To_Gtime(): seconds between 1970-01-01 and the Garmin epoch 1989-12-31
    static final long GARMIN_EPOCH_OFFSET = 631065600L;
    private static final double SEMICIRCLES_PER_DEGREE = 2147483648.0 / 180.0;
    private static final int UNKNOWN_SIZE = -1;
    // Oppmann's record structure nests four to five levels deep; the bound keeps hostile files from
    // exhausting the stack
    static final int MAXIMUM_NESTING = 32;
    // Oppmann: the flags of the Address and Contact records
    static final int ADDRESS_CITY = 0x1, ADDRESS_COUNTRY = 0x2, ADDRESS_STATE = 0x4, ADDRESS_POSTAL_CODE = 0x8,
            ADDRESS_STREET = 0x10, ADDRESS_HOUSE_NUMBER = 0x20;
    static final int CONTACT_PHONE = 0x1, CONTACT_PHONE2 = 0x2, CONTACT_FAX = 0x4, CONTACT_EMAIL = 0x8,
            CONTACT_LINK = 0x10;

    public String getExtension() {
        return ".gpi";
    }

    public String getName() {
        return "Garmin POI (*" + getExtension() + ")";
    }

    public boolean isSupportsMultipleRoutes() {
        return false;
    }

    static class GpiException extends Exception {
        GpiException(String message) {
            super(message);
        }
    }

    // read

    private static byte[] readFully(InputStream source) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] bytes = new byte[8192];
        int count;
        while ((count = source.read(bytes)) != -1)
            buffer.write(bytes, 0, count);
        return buffer.toByteArray();
    }

    /**
     * The detection guard: a Header1 record with 'GRMREC' at the start of its main data.
     */
    static boolean isGpiHeader(byte[] bytes, int length) {
        if (length < 12 || (bytes[0] | bytes[1]) != 0)
            return false;
        int offset = (bytes[2] & EXTRA_DATA) != 0 ? 12 : 8;
        return length >= offset + 6 && "GRMREC".equals(new String(bytes, offset, 6, US_ASCII));
    }

    protected List<GpxRoute> readRoutes(InputStream source) throws IOException {
        byte[] head = source.readNBytes(18);
        if (!isGpiHeader(head, head.length))
            return null;
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        buffer.write(head);
        buffer.write(readFully(source));

        List<GpxPosition> positions;
        String name;
        try {
            Reader reader = new Reader(buffer.toByteArray());
            positions = reader.read();
            name = reader.headerName();
        } catch (GpiException | EOFException | RuntimeException e) {
            // GPSBabel stops with a fatal error and delivers no waypoints
            return null;
        }
        if (positions.isEmpty())
            return null;
        return singletonList(new GpxRoute(new Gpx10Format(), Waypoints, name, null, positions));
    }

    /**
     * Oppmann, "Obfuscation": starting with the first obfuscated byte, consecutive groups of four bytes are
     * processed with the constants 0x48, 0x06, 0xB3, 0x00 by a nibble-wise subtraction without half-carry.
     */
    static void deobfuscate(byte[] bytes, int start, int end) {
        for (int i = start; i < end; i++) {
            int key = OBFUSCATION_KEY[(i - start) % OBFUSCATION_KEY.length];
            int value = bytes[i] & 0xff;
            int high = ((value >> 4) - (key >> 4)) & 0xf;
            int low = ((value & 0xf) - (key & 0xf)) & 0xf;
            bytes[i] = (byte) (high << 4 | low);
        }
    }

    // GarminGPIFormat::rd_init(): the code pages GPSBabel can decode
    static Charset charsetForCodePage(int codePage) {
        if (codePage >= 1250 && codePage <= 1257)
            return Charset.forName("windows-" + codePage);
        if (codePage == 65001)
            return UTF_8;
        return null;
    }

    /** A record header as Oppmann describes it: type, flags, [total length,] main length. */
    static class Record {
        final int type, flags, mainEnd, end;

        Record(int type, int flags, int mainEnd, int end) {
            this.type = type;
            this.flags = flags;
            this.mainEnd = mainEnd;
            this.end = end;
        }

        boolean hasExtraData() {
            return (flags & EXTRA_DATA) != 0;
        }
    }

    /**
     * The fields of a GPSBabel Waypoint that take part in the GPX exchange of RouteConverter; null stands
     * for a null QString, which GPSBabel distinguishes from an empty one.
     */
    static class Wpt {
        double latitude, longitude;
        String shortName, description, notes;
        // read only: the Address and Contact records as text
        String address, contact;

        Wpt(double latitude, double longitude, String shortName, String description, String notes) {
            this.latitude = latitude;
            this.longitude = longitude;
            this.shortName = shortName;
            this.description = description;
            this.notes = notes;
        }

        // the description wins over the notes on GPI output
        String text() {
            return !isEmpty(description) ? description : notes;
        }

        // the GPX writer writes the notes as <desc>, else the description
        String gpxDesc() {
            return !isEmpty(notes) ? notes : description;
        }
    }

    private static boolean isEmpty(String string) {
        return string == null || string.isEmpty();
    }

    /**
     * waypt_add(): normalizes the position once and fills in a missing shortname and description from
     * the other fields; count is the number of waypoints including this one.
     */
    static void addWaypoint(Wpt wpt, int count) throws GpiException {
        double latitude = wpt.latitude, longitude = wpt.longitude;
        if (wpt.latitude < -90)
            wpt.latitude += 180;
        else if (wpt.latitude > 90)
            wpt.latitude -= 180;
        if (wpt.longitude < -180)
            wpt.longitude += 360;
        else if (wpt.longitude > 180)
            wpt.longitude -= 360;
        if (wpt.latitude < -90 || wpt.latitude > 90.0)
            throw new GpiException("Invalid latitude " + latitude + " in waypoint " + wpt.shortName);
        if (wpt.longitude < -180 || wpt.longitude > 180.0)
            throw new GpiException("Invalid longitude " + longitude + " in waypoint " + wpt.shortName);

        if (wpt.shortName == null) {
            if (wpt.description != null)
                wpt.shortName = wpt.description;
            else if (wpt.notes != null)
                wpt.shortName = wpt.notes;
            else
                wpt.shortName = String.format(Locale.ROOT, "WPT%03d", count);
        }
        if (isEmpty(wpt.description)) {
            if (wpt.notes != null)
                wpt.description = wpt.notes;
            else if (wpt.shortName != null)
                wpt.description = wpt.shortName;
        }
    }

    class Reader {
        private final byte[] data;
        private final LittleEndianInput input;
        private final List<GpxPosition> positions = new ArrayList<>();
        private Charset charset;
        private byte[] headerNameBytes;

        Reader(byte[] data) {
            this.data = data;
            this.input = new LittleEndianInput(data);
        }

        private Record readRecord(int limit) throws EOFException, GpiException {
            int type = input.readUInt16();
            int flags = input.readUInt16();
            long total = (flags & EXTRA_DATA) != 0 ? input.readUInt32() : UNKNOWN_SIZE;
            long main = input.readUInt32();
            if (total == UNKNOWN_SIZE)
                total = main;
            int start = input.position();
            if (main > total || start + total > limit)
                throw new GpiException("Record " + type + " at " + start + " exceeds its container");
            return new Record(type, flags, start + (int) main, start + (int) total);
        }

        // GarminGPIFormat::read_header() with Oppmann's record layout for Header1 and Header2
        private int readHeaders() throws EOFException, GpiException {
            Record header1 = readRecord(data.length);
            if (header1.type != HEADER1 || header1.mainEnd - input.position() < 16)
                throw new GpiException("No GPI file");
            if (!"GRMREC".equals(input.readFixedString(6, US_ASCII)))
                throw new GpiException("No GPI file");
            input.skip(2 + 4);                              // format version, creation date
            input.readUInt8();                              // flags
            int obfuscation = input.readUInt8();
            // the name of the POI collection (GPSBabel skips it and writes "my.gpi"); decoded once
            // Header2 has named the code page
            if (input.position() + 2 <= header1.mainEnd) {
                int nameLength = input.readUInt16();
                if (nameLength > 0 && input.position() + nameLength <= header1.mainEnd)
                    headerNameBytes = input.readBytes(nameLength);
            }
            input.seek(header1.end);

            Record header2 = readRecord(data.length);
            if (header2.type != HEADER2 || header2.mainEnd - input.position() < 12)
                throw new GpiException("Wrong or unsupported GPI file");
            if (!"POI".equals(input.readFixedString(3, US_ASCII)))
                throw new GpiException("Wrong or unsupported GPI file");
            input.skip(3 + 2);                              // padding, format version
            int codePage = input.readUInt16();
            charset = charsetForCodePage(codePage);
            if (charset == null)
                throw new GpiException("Unsupported code page " + codePage);

            int start = header2.end;
            if (obfuscation == OBFUSCATION_UNTIL_ADDITIONAL_DATA || obfuscation == OBFUSCATION_UNTIL_END_OF_FILE)
                // the record list up to the End record is obfuscated in both modes; additional data
                // behind the End record is not read
                deobfuscate(data, start, data.length);
            return start;
        }

        List<GpxPosition> read() throws EOFException, GpiException {
            input.seek(readHeaders());
            // GarminGPIFormat::read()
            while (true) {
                int position = input.position();
                if (input.readUInt16() == END)
                    break;
                input.seek(position);
                Record record = readRecord(data.length);
                readRecordContent(record, null, 0);
                input.seek(record.end);
            }
            return positions;
        }

        /**
         * The name of the POI collection from Header1, or null when it is empty or GPSBabel's
         * placeholder "my.gpi" (decision D16.3).
         */
        String headerName() {
            if (headerNameBytes == null || charset == null)
                return null;
            int end = 0;
            while (end < headerNameBytes.length && headerNameBytes[end] != 0)
                end++;
            String name = new String(headerNameBytes, 0, end, charset).trim();
            return name.isEmpty() || GPSBABEL_HEADER_NAME.equals(name) ? null : name;
        }

        // the loops of read_poi(), read_poi_list() and read_poi_group() over the subordinated records
        private void readRecords(int start, int end, Wpt poi, int depth) throws EOFException, GpiException {
            if (depth > MAXIMUM_NESTING)
                throw new GpiException("Records nested deeper than " + MAXIMUM_NESTING + " levels at " + start);
            input.seek(start);
            while (end - input.position() >= 8) {
                int position = input.position();
                if (input.readUInt16() == END)
                    break;
                input.seek(position);
                Record record = readRecord(end);
                readRecordContent(record, poi, depth);
                input.seek(record.end);
            }
        }

        // GarminGPIFormat::read_tag(); record types GPSBabel does not interpret are skipped by their length
        private void readRecordContent(Record record, Wpt poi, int depth) throws EOFException, GpiException {
            switch (record.type) {
                case WAYPOINT -> readWaypoint(record, depth);
                case ALERT -> {
                    if (poi != null)
                        readAlert(poi);
                }
                case AREA -> readRecords(record.mainEnd, record.end, null, depth + 1);
                case POI_GROUP -> {
                    readString(record.end);                 // group name
                    readRecords(input.position(), record.end, null, depth + 1);
                }
                case COMMENT -> {
                    if (poi != null)
                        poi.description = readString(record.end);
                }
                case DESCRIPTION -> {
                    if (poi != null)
                        readNotes(record, poi);
                }
                case ADDRESS -> {
                    if (poi != null)
                        poi.address = readAddress(record.end);
                }
                case CONTACT -> {
                    if (poi != null)
                        poi.contact = readContact(record.end);
                }
                default -> {
                    // bitmaps, categories, images, media and the records of format version 01
                }
            }
        }

        // GarminGPIFormat::read_poi()
        private void readWaypoint(Record record, int depth) throws EOFException, GpiException {
            double latitude = semicirclesToDegrees(input.readInt32());
            double longitude = semicirclesToDegrees(input.readInt32());
            input.readUInt16();                             // always 1
            input.readUInt8();                              // 1 when alerts are present
            Wpt poi = new Wpt(latitude, longitude, readString(record.end), null, null);

            // GPSBabel reads the subordinated records right behind the shortname; with extra data
            // they start behind the main data
            readRecords(record.hasExtraData() ? record.mainEnd : input.position(), record.end, poi, depth + 1);

            if (isEmpty(poi.description) && !isEmpty(poi.notes))
                poi.description = poi.notes;
            if (isEmpty(poi.notes) && !isEmpty(poi.description))
                poi.notes = poi.description;
            addWaypoint(poi, positions.size() + 1);
            positions.add(createPosition(poi));
        }

        // read_tag() case 0x3: the speed of a speed camera becomes part of the shortname. For an alert with
        // extra data (tag 0x80003) GPSBabel reads the two halves of the main length as proximity and speed
        // and never appends a speed; the Oppmann layout reads the real speed
        private void readAlert(Wpt poi) throws EOFException {
            input.readInt16();                              // proximity distance in meters
            double speed = (double) input.readInt16() / 100; // speed in meters per second
            if (speed > 0 && (isEmpty(poi.shortName) || poi.shortName.indexOf('@') == -1)) {
                String base = isEmpty(poi.shortName) ? "WPT" : poi.shortName;
                poi.shortName = base + "@" + formatSpeed(speed * 3600 / 1000);  // MPS_TO_KPH()
            }
        }

        // read_tag() case 0xe
        private void readNotes(Record record, Wpt poi) throws EOFException, GpiException {
            int mask = input.readUInt8();
            String string = null;
            if (mask == 0x01 || mask == 0x05 || mask == 0x32)
                string = readString(record.end);
            if (!isEmpty(poi.description))
                poi.notes = string;
            else
                poi.description = string;
        }

        /**
         * Oppmann, "Record type 11: Address": flags and the strings they announce, which are in the extra data
         * for format version 00 and in the main data for 01. GPSBabel 1.5.4 drops the address on its way to
         * RouteConverter's GPX; a broken or truncated record loses the address only.
         */
        private String readAddress(int limit) {
            try {
                int flags = input.readUInt16();
                String city = (flags & ADDRESS_CITY) != 0 ? readLString(limit) : null;
                String country = (flags & ADDRESS_COUNTRY) != 0 ? readLString(limit) : null;
                String state = (flags & ADDRESS_STATE) != 0 ? readLString(limit) : null;
                String postalCode = (flags & ADDRESS_POSTAL_CODE) != 0 ? readPString(limit) : null;
                String street = (flags & ADDRESS_STREET) != 0 ? readLString(limit) : null;
                String houseNumber = (flags & ADDRESS_HOUSE_NUMBER) != 0 ? readPString(limit) : null;
                return join(", ", join(" ", street, houseNumber), join(" ", postalCode, city), state, country);
            } catch (GpiException | EOFException e) {
                return null;
            }
        }

        /**
         * Oppmann, "Record type 12: Contact": like the address. A fax number would be indistinguishable
         * from the phone numbers in the description and is left out.
         */
        private String readContact(int limit) {
            try {
                int flags = input.readUInt16();
                String phone = (flags & CONTACT_PHONE) != 0 ? readPString(limit) : null;
                String phone2 = (flags & CONTACT_PHONE2) != 0 ? readPString(limit) : null;
                if ((flags & CONTACT_FAX) != 0)
                    readPString(limit);
                String email = (flags & CONTACT_EMAIL) != 0 ? readPString(limit) : null;
                String link = (flags & CONTACT_LINK) != 0 ? readPString(limit) : null;
                return join(", ", phone, phone2, email, link);
            } catch (GpiException | EOFException e) {
                return null;
            }
        }

        // Oppmann, "PString": a string with a 16 bit length
        private String readPString(int limit) throws EOFException, GpiException {
            int length = input.readUInt16();
            if (input.position() + length > limit)
                throw new GpiException("String at " + input.position() + " exceeds its record");
            return decode(input.readBytes(length));
        }

        // Oppmann, "LString": a 32 bit length and one or more localized strings
        private String readLString(int limit) throws EOFException, GpiException {
            long length = input.readUInt32();
            if (input.position() + length > limit)
                throw new GpiException("String at " + input.position() + " exceeds its record");
            if (length == 0)
                return null;
            return decode(readLanguageStrings((int) length, limit));
        }

        // GPSBabel 1.5.4 handles strings as C strings
        private String decode(byte[] bytes) {
            int end = 0;
            while (end < bytes.length && bytes[end] != 0)
                end++;
            return new String(bytes, 0, end, charset);
        }

        /**
         * GarminGPIFormat::gpi_read_string(): a string with a 16 bit length, either a single byte string
         * or - if the two bytes behind the length are zero - a language string.
         */
        private String readString(int limit) throws EOFException, GpiException {
            int length = input.readInt16();
            if (length <= 0)
                return null;
            byte[] bytes;
            if (input.readUInt8() == 0) {
                if (input.readUInt8() != 0)
                    throw new GpiException("Error reading string at " + input.position());
                if (input.position() + length > limit)
                    throw new GpiException("String at " + input.position() + " exceeds its record");
                bytes = readLanguageStrings(length, limit);
            } else {
                input.seek(input.position() - 1);
                if (input.position() + length > limit)
                    throw new GpiException("String at " + input.position() + " exceeds its record");
                bytes = input.readBytes(length);
            }
            return decode(bytes);
        }

        /**
         * GarminGPIFormat::gpi_read_lc_string(): one or more entries of a two letter language code and a
         * string with a 16 bit length. GPSBabel stops without the languagecode option for more than
         * one entry; RouteConverter has no such option and takes the first entry.
         */
        private byte[] readLanguageStrings(int length, int limit) throws EOFException, GpiException {
            byte[] result = null;
            int consumed = 0;
            while (consumed < length) {
                int language1 = input.readUInt8(), language2 = input.readUInt8();
                if (language1 < 'A' || language1 > 'Z' || language2 < 'A' || language2 > 'Z')
                    throw new GpiException("Invalid language code at " + input.position());
                int stringLength = input.readInt16();
                if (input.position() + stringLength > limit)
                    throw new GpiException("String at " + input.position() + " exceeds its record");
                byte[] bytes = stringLength > 0 ? input.readBytes(stringLength) : new byte[0];
                consumed += 4 + Math.max(stringLength, 0);
                if (result == null)
                    result = bytes;
            }
            if (consumed != length)
                throw new GpiException("Error out of sync (wrong size " + length + "/" + consumed + ")");
            return result;
        }
    }

    /**
     * QString("%1").arg(speed, 0, 'f', 0): the binary value of the double rounded half to even to an
     * integer, as GPSBabel 1.5.4 does (4.5 km/h becomes 4).
     */
    static String formatSpeed(double speed) {
        return new BigDecimal(speed).setScale(0, RoundingMode.HALF_EVEN).toPlainString();
    }

    /**
     * The text as RouteConverter read it from GPSBabel's GPX: the GPX writer replaces the control
     * characters that XML forbids with a space, the XML parser normalizes line ends to a line feed.
     */
    static String asGpxText(String string) {
        if (string == null)
            return null;
        StringBuilder result = new StringBuilder(string.length());
        for (int i = 0; i < string.length(); i++) {
            char c = string.charAt(i);
            if (c == '\r') {
                result.append('\n');
                if (i + 1 < string.length() && string.charAt(i + 1) == '\n')
                    i++;
            } else if (c < 0x20 && c != '\t' && c != '\n')
                result.append(' ');
            else
                result.append(c);
        }
        return result.toString();
    }

    private static String emptyToNull(String string) {
        return string == null || string.isEmpty() ? null : string;
    }

    // the non-empty strings joined by the separator, or null
    static String join(String separator, String... strings) {
        StringBuilder result = new StringBuilder();
        for (String string : strings) {
            String trimmed = string != null ? string.trim() : null;
            if (isEmpty(trimmed))
                continue;
            if (!result.isEmpty())
                result.append(separator);
            result.append(trimmed);
        }
        return result.isEmpty() ? null : result.toString();
    }

    private GpxPosition createPosition(Wpt poi) {
        // GPSBabel writes the shortname as <name> and the notes as <desc>, and no element for an empty string
        String name = emptyToNull(asGpxText(poi.shortName));
        String desc = emptyToNull(asGpxText(poi.gpxDesc()));
        // address and contact follow the description; the shortname that waypt_add() filled into an empty
        // description is dropped
        String details = asGpxText(join("; ", poi.address, poi.contact));
        if (details != null)
            desc = desc != null && !desc.equals(name) ? desc + "; " + details : details;
        String description = asDescription(name, desc);
        return new GpxPosition(poi.longitude, poi.latitude, null, null, null, description);
    }

    // write

    /**
     * GPSBabel 1.5.4's mkshort() with the settings of GarminGPIFormat::wr_init(): target length 1024,
     * bad characters CR and LF, mixed case, whitespace allowed but no repeated blanks, unique names,
     * default name "POI".
     */
    static class ShortNames {
        private static final int TARGET_LENGTH = 1024;
        private static final String BAD_CHARACTERS = "\r\n";
        private static final String[][] REPLACEMENTS = {
                {"zero", "0"}, {"one", "1"}, {"two", "2"}, {"three", "3"}, {"four", "4"},
                {"five", "5"}, {"six", "6"}, {"seven", "7"}, {"eight", "8"}, {"nine", "9"}
        };

        private final List<String> names = new ArrayList<>();
        private final List<int[]> conflictCounters = new ArrayList<>();

        // isspace() in the C locale
        private static boolean isSpace(char c) {
            return c == ' ' || (c >= '\t' && c <= '\r');
        }

        private static int utf8Length(String string) {
            return string.getBytes(UTF_8).length;
        }

        // case_ignore_strcmp(): ASCII case folding only
        private static boolean equalsIgnoreAsciiCase(String a, String b) {
            if (a.length() != b.length())
                return false;
            for (int i = 0; i < a.length(); i++) {
                char ca = a.charAt(i), cb = b.charAt(i);
                if (ca == cb)
                    continue;
                if (ca < 128 && cb < 128 && Character.toUpperCase(ca) == Character.toUpperCase(cb))
                    continue;
                return false;
            }
            return true;
        }

        // replace_constants(): a trailing number word preceded by a blank becomes a digit
        static String replaceConstants(String string) {
            int length = string.length();
            for (String[] replacement : REPLACEMENTS) {
                int replacementLength = replacement[0].length();
                if (length - replacementLength > 1 &&
                        string.substring(length - replacementLength).toLowerCase(Locale.ROOT).equals(replacement[0]) &&
                        string.charAt(length - replacementLength - 1) == ' ')
                    return string.substring(0, length - replacementLength) + replacement[1];
            }
            return string;
        }

        // the in-place loop of mkshort() that removes the first of two blanks and moves on
        static String removeRepeatedBlanks(String string) {
            StringBuilder builder = new StringBuilder(string);
            for (int i = 0; i < builder.length() - 1; i++) {
                if (builder.charAt(i) == ' ' && builder.charAt(i + 1) == ' ')
                    builder.deleteCharAt(i);
            }
            return builder.toString();
        }

        String mkshort(String input) {
            String string = input;

            // whack leading "[Tt]he"
            if (utf8Length(string) > TARGET_LENGTH + 4 && (string.startsWith("The ") || string.startsWith("the ")))
                string = string.substring(4);

            // eliminate leading whitespace
            int start = 0;
            while (start < string.length() && isSpace(string.charAt(start)))
                start++;
            string = string.substring(start);

            string = replaceConstants(string);

            // eliminate characters on the blacklist
            StringBuilder builder = new StringBuilder(string.length());
            for (int i = 0; i < string.length(); i++) {
                char c = string.charAt(i);
                if (BAD_CHARACTERS.indexOf(c) == -1)
                    builder.append(c);
            }
            string = removeRepeatedBlanks(builder.toString());

            // truncate to the target length in characters
            if (string.codePointCount(0, string.length()) > TARGET_LENGTH)
                string = string.substring(0, string.offsetByCodePoints(0, TARGET_LENGTH));

            if (string.isEmpty())
                string = DEFAULT_NAME;
            return addToList(string);
        }

        // mkshort_add_to_list()
        private String addToList(String name) {
            int index;
            while ((index = indexOf(name)) != -1) {
                int counter = ++conflictCounters.get(index)[0];
                String suffix = "." + counter;
                if (utf8Length(name) + suffix.length() < TARGET_LENGTH)
                    name = name + suffix;
                else
                    name = name.substring(0, name.length() - suffix.length()) + suffix;
            }
            names.add(name);
            conflictCounters.add(new int[1]);
            return name;
        }

        private int indexOf(String name) {
            for (int i = 0; i < names.size(); i++)
                if (equalsIgnoreAsciiCase(names.get(i), name))
                    return i;
            return -1;
        }
    }

    /**
     * GPS_Math_Deg_To_Semi() of GPSBabel 1.5.4: the product is truncated towards zero (master rounds).
     * A product outside the int range or NaN is undefined behaviour in C; the shipped x86 builds of 1.5.4
     * return INT_MIN for it (cvttsd2si).
     */
    static int degreesToSemicircles(double degrees) {
        double product = SEMICIRCLES_PER_DEGREE * degrees;
        if (Double.isNaN(product) || product >= 2147483648.0 || product <= -2147483649.0)
            return Integer.MIN_VALUE;
        return (int) product;
    }

    /** writer_data_t: a block of at most WAYPOINTS_PER_BLOCK waypoints or four sub-blocks. */
    static class Block {
        final List<Wpt> wpts = new ArrayList<>();
        int size;
        // waypt_init_bounds()
        double maxLatitude = -9999, maxLongitude = -9999, minLatitude = 9999, minLongitude = 9999;
        Block topLeft, topRight, bottomLeft, bottomRight;

        // wdata_add_wpt() with waypt_add_to_bounds()
        void add(Wpt wpt) {
            wpts.add(wpt);
            if (wpt.latitude > maxLatitude)
                maxLatitude = wpt.latitude;
            if (wpt.longitude > maxLongitude)
                maxLongitude = wpt.longitude;
            if (wpt.latitude < minLatitude)
                minLatitude = wpt.latitude;
            if (wpt.longitude < minLongitude)
                minLongitude = wpt.longitude;
        }

        Block[] children() {
            return new Block[]{topLeft, topRight, bottomLeft, bottomRight};
        }

        // wdata_check(): split into quadrants around the mean center until a block holds few enough waypoints
        void check() {
            if (wpts.size() <= WAYPOINTS_PER_BLOCK ||
                    // avoid an endless loop for more than WAYPOINTS_PER_BLOCK waypoints at the same coordinates
                    (minLatitude >= maxLatitude && minLongitude >= maxLongitude)) {
                // the names are unique, so the order of equal names does not matter
                wpts.sort((a, b) -> a.shortName.compareTo(b.shortName));
                return;
            }

            double centerLatitude = 0, centerLongitude = 0;
            for (Wpt wpt : wpts) {
                centerLatitude += wpt.latitude;
                centerLongitude += wpt.longitude;
            }
            centerLatitude /= wpts.size();
            centerLongitude /= wpts.size();

            for (Wpt wpt : wpts) {
                if (wpt.latitude < centerLatitude) {
                    if (wpt.longitude < centerLongitude)
                        bottomLeft = addTo(bottomLeft, wpt);
                    else
                        bottomRight = addTo(bottomRight, wpt);
                } else {
                    if (wpt.longitude < centerLongitude)
                        topLeft = addTo(topLeft, wpt);
                    else
                        topRight = addTo(topRight, wpt);
                }
            }
            wpts.clear();

            for (Block child : children())
                if (child != null)
                    child.check();
        }

        private static Block addTo(Block block, Wpt wpt) {
            Block result = block != null ? block : new Block();
            result.add(wpt);
            return result;
        }
    }

    static class Writer {
        private final LittleEndianOutput output = new LittleEndianOutput();

        // QTextCodec::fromUnicode() replaces every UTF-16 unit it cannot encode with '?', so a character
        // outside the Basic Multilingual Plane becomes "??"
        private static byte[] encode(String string) {
            if (string == null)
                return new byte[0];
            CharsetEncoder encoder = WRITE_CHARSET.newEncoder();
            StringBuilder builder = new StringBuilder(string.length());
            for (int i = 0; i < string.length(); i++) {
                char c = string.charAt(i);
                builder.append(encoder.canEncode(c) ? c : '?');
            }
            return builder.toString().getBytes(WRITE_CHARSET);
        }

        // GarminGPIFormat::write_string()
        private void writeString(byte[] bytes, boolean longFormat) {
            if (longFormat) {
                output.writeInt32(bytes.length + 4);
                output.writeBytes("EN".getBytes(US_ASCII));
            }
            output.writeInt16(bytes.length);
            output.writeBytes(bytes);
        }

        // wdata_compute_size() without alerts and address fields
        private int computeSize(Block block) {
            int result = 0;
            if (!block.wpts.isEmpty()) {
                result = 23;                                // bounds, ... of tag 0x80008
                for (Wpt wpt : block.wpts) {
                    result += 12;                           // tag/sz/sub-sz
                    result += 19;                           // poi fixed size
                    result += encode(wpt.shortName).length;
                    result += 10;                           // bitmap reference
                    byte[] text = encode(wpt.text());
                    if (text.length > 0)
                        result += 12 + 4 + text.length;  // 0xa description record
                }
            }
            for (Block child : block.children())
                if (child != null)
                    result += computeSize(child);
            block.size = result;
            return block.wpts.isEmpty() ? result : result + 12;  // + 12 = the caller needs the tag header size
        }

        // wdata_write() without alerts and address fields
        private void writeBlock(Block block) {
            if (!block.wpts.isEmpty()) {
                output.writeInt32(AREA | EXTRA_DATA << 16);
                output.writeInt32(block.size);
                output.writeInt32(23);                      // bounds + three bytes
                output.writeInt32(degreesToSemicircles(block.maxLatitude));
                output.writeInt32(degreesToSemicircles(block.maxLongitude));
                output.writeInt32(degreesToSemicircles(block.minLatitude));
                output.writeInt32(degreesToSemicircles(block.minLongitude));
                output.writeInt32(0);
                output.writeInt16(1);
                output.writeUInt8(0);                       // no alert

                for (Wpt wpt : block.wpts) {
                    byte[] shortName = encode(wpt.shortName);
                    byte[] text = encode(wpt.text());

                    output.writeInt32(WAYPOINT | EXTRA_DATA << 16);
                    int mainSize = 19 + shortName.length;
                    int size = mainSize + 10;               // bitmap reference
                    if (text.length > 0)
                        size += 12 + 4 + text.length;       // description
                    output.writeInt32(size);                // size of following data
                    output.writeInt32(mainSize);            // basic size without options

                    output.writeInt32(degreesToSemicircles(wpt.latitude));
                    output.writeInt32(degreesToSemicircles(wpt.longitude));
                    output.writeInt16(1);                   // always 1
                    output.writeUInt8(0);                   // no alerts
                    writeString(shortName, true);

                    output.writeInt32(BITMAP_REFERENCE);
                    output.writeInt32(2);
                    output.writeInt16(0);

                    if (text.length > 0) {
                        output.writeInt32(COMMENT);
                        output.writeInt32(text.length + 8); // string + string header
                        writeString(text, true);
                    }
                }
            }
            for (Block child : block.children())
                if (child != null)
                    writeBlock(child);
        }

        // GarminGPIFormat::write_header() with codepage 1252 and the POI collection name in place of
        // GPSBabel's "my.gpi" (decision D16.3)
        private void writeHeader(String headerName, long garminTime) {
            byte[] name = encode(headerName);
            if (name.length > MAXIMUM_HEADER_NAME_LENGTH)
                name = Arrays.copyOf(name, MAXIMUM_HEADER_NAME_LENGTH);
            output.writeInt32(HEADER1);
            output.writeInt32(16 + name.length);
            output.writeBytes("GRMREC00".getBytes(US_ASCII));
            output.writeInt32((int) garminTime);
            output.writeInt16(0);
            output.writeInt16(name.length);
            output.writeBytes(name);
            output.writeInt32(HEADER2);
            output.writeInt32(0xc);
            output.writeBytes("POI".getBytes(US_ASCII));
            output.writeBytes(new byte[3]);
            output.writeBytes("00".getBytes(US_ASCII));
            output.writeInt16(WRITE_CODEPAGE);
            output.writeInt16(0);
        }

        // GarminGPIFormat::write_category() with the embedded bitmap
        private void writeCategory(String category, Block root) {
            byte[] name = encode(category);
            int size = computeSize(root) + 8 + name.length;
            output.writeInt32(POI_GROUP | EXTRA_DATA << 16);
            output.writeInt32(size + DEFAULT_BITMAP.length + 8);
            output.writeInt32(size);
            writeString(name, true);

            writeBlock(root);

            output.writeInt32(BITMAP);
            output.writeInt32(DEFAULT_BITMAP.length);
            output.writeBytes(DEFAULT_BITMAP);
        }

        // GarminGPIFormat::write() with the options unique, no alerts, no address fields, default bitmap
        byte[] write(String headerName, String category, List<Wpt> wpts, long garminTime) {
            Block root = new Block();
            ShortNames shortNames = new ShortNames();
            for (Wpt wpt : wpts) {
                // enum_waypt_cb(): sort out nearly equal waypoints
                if (isNearlyEqualToAdded(root.wpts, wpt))
                    continue;
                wpt.shortName = shortNames.mkshort(wpt.shortName);
                root.add(wpt);
            }
            root.check();

            writeHeader(headerName, garminTime);
            writeCategory(category, root);
            output.writeInt32(END);                         // final tag
            output.writeInt32(0);                           // dummy size
            return output.toByteArray();
        }

        // QString::compare() treats a null and an empty string as equal
        private static boolean isSame(String a, String b) {
            return (a != null ? a : "").equals(b != null ? b : "");
        }

        // the coordinates come from BigDecimal.doubleValue(), so there is no NaN or -0.0 and Double.compare()
        // is the == of the C++ original
        private static boolean isNearlyEqualToAdded(List<Wpt> added, Wpt wpt) {
            for (Wpt compare : added) {
                if (isSame(compare.shortName, wpt.shortName) &&
                        Double.compare(compare.latitude, wpt.latitude) == 0 &&
                        Double.compare(compare.longitude, wpt.longitude) == 0 &&
                        isSame(compare.description, wpt.description) && isSame(compare.notes, wpt.notes))
                    return true;
            }
            return false;
        }
    }

    // QChar::isSpace()
    private static boolean isQtSpace(char c) {
        if (c == ' ' || (c >= '\t' && c <= '\r') || c == 0x85 || c == 0xa0)
            return true;
        int type = Character.getType(c);
        return type == Character.SPACE_SEPARATOR || type == Character.LINE_SEPARATOR || type == Character.PARAGRAPH_SEPARATOR;
    }

    /**
     * The text as GPSBabel's GPX reader got it from RouteConverter's GPX: the reader trims the character
     * data with QString::trimmed(). RouteConverter's GPX writes a carriage return as {@code &#13;}, which
     * the XML parser keeps.
     */
    static String asGpxReaderText(String string) {
        if (string == null)
            return null;
        String text = string;
        int start = 0, end = text.length();
        while (start < end && isQtSpace(text.charAt(start)))
            start++;
        while (end > start && isQtSpace(text.charAt(end - 1)))
            end--;
        return text.substring(start, end);
    }

    static long currentGarminTime() {
        return System.currentTimeMillis() / 1000 - GARMIN_EPOCH_OFFSET;
    }

    // the POI collection name in Header1: the route name, GPSBabel's "my.gpi" for a route without a name
    String asHeaderName(String routeName) {
        String name = asRouteName(routeName);
        return name == null || name.trim().isEmpty() ? GPSBABEL_HEADER_NAME : name;
    }

    // the category option of the GPSBabel call; GPSBabel's default category for a route without a name
    String asCategory(String routeName) {
        String category = asRouteName(routeName);
        return category == null || category.isEmpty() ? DEFAULT_CATEGORY : category;
    }

    List<Wpt> asWpts(GpxRoute route, int startIndex, int endIndex) throws IOException {
        List<Wpt> result = new ArrayList<>();
        List<GpxPosition> positions = route.getPositions();
        for (int i = startIndex; i < endIndex; i++) {
            GpxPosition position = positions.get(i);
            BigDecimal latitude = formatPosition(position.getLatitude());
            BigDecimal longitude = formatPosition(position.getLongitude());
            if (latitude == null || longitude == null)
                continue;
            // RouteConverter's GPX delivers the name as <name> and the description as <desc>, which
            // GPSBabel reads as the notes
            String name = asGpxReaderText(asName(position.getDescription()));
            String notes = asGpxReaderText(asDesc(position.getDescription()));
            Wpt wpt = new Wpt(latitude.doubleValue(), longitude.doubleValue(), name, null, notes);
            try {
                addWaypoint(wpt, result.size() + 1);
            } catch (GpiException e) {
                throw new IOException(e.getMessage(), e);
            }
            result.add(wpt);
        }
        return result;
    }

    byte[] write(GpxRoute route, int startIndex, int endIndex, long garminTime) throws IOException {
        return new Writer().write(asHeaderName(route.getName()), asCategory(route.getName()),
                asWpts(route, startIndex, endIndex), garminTime);
    }

    public void write(GpxRoute route, OutputStream target, int startIndex, int endIndex) throws IOException {
        target.write(write(route, startIndex, endIndex, currentGarminTime()));
        target.flush();
    }

    // garmin_gpi.h gpi_bitmap: the embedded GPSBabel icon in GPI bitmap format
    static final byte[] DEFAULT_BITMAP = HexFormat.of().parseHex(
            "000018001800180008000000400200002c00000000010000ff00ff00010000006c020000000000000000000000000000" +
            "00000000000000000000007f007e7e7e7e7e7e7e7e7e7e7e7e7e7e7e7e7e7e7e7e7e007e007e7f7f7f7f7f7f7f7f7f7f" +
            "7f7f5967657f7f7f7f7f007e007e7f7f7f7f7f7f7f7f7f7f7f743d42567e7e7f7f7f007e007e7f7f7f7f7f7f7f7f7f7f" +
            "7f723849477e7e7f7f7f007e007e7f7f7f7f7f7f7f7f7f7f7f7d37477d7e7e7f7f7f007e007e7f7f7f7f7f7f7f7f7f7f" +
            "7f7d37477d7e7e7f7f7f007e007e7f7f7f7f7f7f7f7c6c50445e4f767e7f7f7f7f7f007e007e7f777e7f7f7e620d0005" +
            "100809597e7e7e7f7f7f007e007e291c4c7f7f60020c2a3751635715587e7e7f7f7f007e007e685a415f5f070e3d4141" +
            "4d556b612657572b2f30007e007e777d4e3d3d1635417d49184852545b3131637f7f007e007e407d754747413540721e" +
            "7c5d1d20493d3d5b7f7f007e007e605c7a1a1a3b385d0e597d3c7237786060284f71007e007e7e4333696917227d2c27" +
            "2a2b7d32614f4f363f4c007e007e7f7e3a2b2b451a40477d374112255e46464d6253007e007e7f7e7371716a13391b45" +
            "62503a7e7e7b7b5c5b49007e007e7f7e5901010664354e3e2621667f450404117f7f007e007e7f7e5901010664354e3e" +
            "2621667f450404117f7f007e007e7f7e7c0a0a0f657d74717c7e7e7e5803032b7f7f007e007e7f7f7f6d6d6f2d1d637a" +
            "7e755d193270706f7f7f007e007e7f7f7f7f7f7e7d53350b1f0e345a7f7f7f7f7f7f007e000000000000000000000000" +
            "00000000000000000000007e7f7e7e7e7e7e7e7e7e7e7e7e7e7e7e7e7e7e7e7e7e7e7e7e0a0a0a000b0b0b0016161600" +
            "1f1f1f00282828002d2d2d00353535003d3d3d0040404000414141004343430046464600474747004b4b4b004e4e4e00" +
            "535353005454540056565600595959005a5a5a005f5f5f006060600062626200636363006a6a6a007474740075757500" +
            "7676760078787800797979007a7a7a007c7c7c007d7d7d007e7e7e008080800081818100828282008383830084848400" +
            "858585008787870088888800898989008a8a8a008b8b8b008e8e8e009090900091919100929292009494940095959500" +
            "9696960097979700989898009b9b9b009c9c9c009d9d9d00a0a0a000a1a1a100a2a2a200a4a4a400a6a6a600a7a7a700" +
            "ababab00acacac00adadad00aeaeae00afafaf00b0b0b000b1b1b100b2b2b200b3b3b300b4b4b400b5b5b500b6b6b600" +
            "b7b7b700b9b9b900bbbbbb00bcbcbc00bebebe00bfbfbf00c0c0c000c1c1c100c2c2c200c3c3c300c4c4c400c5c5c500" +
            "c9c9c900cbcbcb00cccccc00cecece00cfcfcf00d0d0d000d1d1d100d2d2d200d4d4d400d5d5d500d6d6d600d9d9d900" +
            "dadada00dbdbdb00dcdcdc00dddddd00e0e0e000e1e1e100e2e2e200e3e3e300e4e4e400e6e6e600e8e8e800e9e9e900" +
            "eaeaea00ebebeb00ececec00ededed00eeeeee00f0f0f000f3f3f300f4f4f400f5f5f500f7f7f700f8f8f800f9f9f900" +
            "fafafa00fbfbfb00fcfcfc00ffffff00ff00ff0000000000000000000000000000000000000000000000000000000000" +
            "000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000" +
            "000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000" +
            "000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000" +
            "000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000" +
            "000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000" +
            "000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000" +
            "000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000" +
            "000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000" +
            "000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000" +
            "000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000" +
            "00000000");
}
