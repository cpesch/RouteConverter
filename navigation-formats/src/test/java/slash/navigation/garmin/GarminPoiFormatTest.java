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

import org.junit.jupiter.api.Test;
import slash.navigation.base.NavigationFormat;
import slash.navigation.base.NavigationFormatRegistry;
import slash.navigation.base.ParserContextImpl;
import slash.navigation.gpsbabel.LittleEndianInput;
import slash.navigation.gpsbabel.LittleEndianOutput;
import slash.navigation.gpx.Gpx10Format;
import slash.navigation.gpx.GpxPosition;
import slash.navigation.gpx.GpxRoute;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.Random;

import static java.nio.charset.StandardCharsets.US_ASCII;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static slash.navigation.base.RouteCharacteristics.Waypoints;

/**
 * Tests {@link GarminPoiFormat} with GPI files built byte by byte in the test.
 */
public class GarminPoiFormatTest {
    private static final Charset WINDOWS_1252 = Charset.forName("windows-1252");
    private final GarminPoiFormat format = new GarminPoiFormat();

    // a builder for the records of a GPI file as Oppmann describes them

    private static byte[] concat(byte[]... parts) {
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        for (byte[] part : parts)
            result.writeBytes(part);
        return result.toByteArray();
    }

    private static byte[] record(int type, byte[] main) {
        LittleEndianOutput output = new LittleEndianOutput();
        output.writeUInt16(type);
        output.writeUInt16(0);
        output.writeInt32(main.length);
        output.writeBytes(main);
        return output.toByteArray();
    }

    private static byte[] record(int type, byte[] main, byte[] extra) {
        LittleEndianOutput output = new LittleEndianOutput();
        output.writeUInt16(type);
        output.writeUInt16(8);
        output.writeInt32(main.length + extra.length);
        output.writeInt32(main.length);
        output.writeBytes(main);
        output.writeBytes(extra);
        return output.toByteArray();
    }

    private static byte[] languageString(byte[]... entries) {
        LittleEndianOutput strings = new LittleEndianOutput();
        String[] languages = {"EN", "DE", "FR"};
        for (int i = 0; i < entries.length; i++) {
            strings.writeBytes(languages[i].getBytes(US_ASCII));
            strings.writeInt16(entries[i].length);
            strings.writeBytes(entries[i]);
        }
        LittleEndianOutput output = new LittleEndianOutput();
        output.writeInt32(strings.size());
        output.writeBytes(strings.toByteArray());
        return output.toByteArray();
    }

    private static byte[] string(String string) {
        return languageString(string.getBytes(WINDOWS_1252));
    }

    private static byte[] int16(int value) {
        LittleEndianOutput output = new LittleEndianOutput();
        output.writeInt16(value);
        return output.toByteArray();
    }

    private static byte[] int32(int value) {
        LittleEndianOutput output = new LittleEndianOutput();
        output.writeInt32(value);
        return output.toByteArray();
    }

    private static int semicircles(double degrees) {
        return (int) Math.round(degrees * 2147483648.0 / 180.0);
    }

    private static byte[] header1(String version, int obfuscation, byte[] extra) {
        return header1(version, obfuscation, extra, "my.gpi".getBytes(US_ASCII));
    }

    private static byte[] header1(String version, int obfuscation, byte[] extra, byte[] name) {
        byte[] main = concat(("GRMREC" + version).getBytes(US_ASCII), int32(0), new byte[]{0, (byte) obfuscation},
                int16(name.length), name);
        return extra != null ? record(0, main, extra) : record(0, main);
    }

    private static byte[] header2(String version, int codePage, byte[] extra) {
        byte[] main = concat("POI".getBytes(US_ASCII), new byte[3], version.getBytes(US_ASCII), int16(codePage), int16(0));
        return extra != null ? record(1, main, extra) : record(1, main);
    }

    private static byte[] end() {
        return concat(int16(0xffff), int16(0), int32(0));
    }

    private static byte[] gpi(int codePage, byte[]... records) {
        return concat(header1("00", 0, null), header2("00", codePage, null), concat(records), end());
    }

    private static byte[] waypoint(double latitude, double longitude, byte[] shortName, byte[]... records) {
        byte[] main = concat(int32(semicircles(latitude)), int32(semicircles(longitude)), int16(1), new byte[]{0}, shortName);
        return record(2, main, concat(records));
    }

    private static byte[] waypoint(double latitude, double longitude, String shortName, byte[]... records) {
        return waypoint(latitude, longitude, string(shortName), records);
    }

    private static byte[] alert(int proximity, int speed) {
        return record(3, concat(int16(proximity), int16(speed), int32(0), new byte[]{1, 1, 5, 0x10}));
    }

    private static byte[] comment(String text) {
        return record(0xa, string(text));
    }

    private static byte[] notes(int flag, String text) {
        return record(0xe, concat(new byte[]{(byte) flag}, string(text)));
    }

    private static byte[] area(byte[]... records) {
        return record(8, concat(int32(0), int32(0), int32(0), int32(0), int32(0), int16(1), new byte[]{0}), concat(records));
    }

    private static byte[] group(String name, byte[]... records) {
        return record(9, concat(string(name), concat(records)));
    }

    private List<GpxRoute> read(byte[] bytes) throws IOException {
        ParserContextImpl<GpxRoute> context = new ParserContextImpl<>();
        format.read(new ByteArrayInputStream(bytes), context);
        return context.getRoutes();
    }

    private List<String> readDescriptions(byte[] bytes) throws IOException {
        List<GpxRoute> routes = read(bytes);
        assertEquals(1, routes.size());
        assertEquals(Waypoints, routes.get(0).getCharacteristics());
        List<String> result = new ArrayList<>();
        for (GpxPosition position : routes.get(0).getPositions())
            result.add(position.getDescription());
        return result;
    }

    // read

    @Test
    public void readsWaypointsInFileOrderAcrossGroupsAndAreas() throws IOException {
        byte[] bytes = gpi(1252,
                group("First",
                        area(area(waypoint(48.1, 11.5, "Inner", comment("in"))),
                                waypoint(48.2, 11.6, "Outer"))),
                group("Second", area(waypoint(-33.9, -70.6, "South West", comment("far")))));
        List<GpxRoute> routes = read(bytes);
        assertEquals(1, routes.size());
        GpxRoute route = routes.get(0);
        assertNull(route.getName());
        assertEquals(3, route.getPositionCount());
        GpxPosition first = route.getPosition(0);
        assertEquals(48.1, first.getLatitude(), 1e-7);
        assertEquals(11.5, first.getLongitude(), 1e-7);
        assertNull(first.getElevation());
        assertNull(first.getTime());
        assertEquals("Inner; in", first.getDescription());
        assertEquals("Outer", route.getPosition(1).getDescription());
        assertEquals(-33.9, route.getPosition(2).getLatitude(), 1e-7);
        assertEquals(-70.6, route.getPosition(2).getLongitude(), 1e-7);
        assertEquals("South West; far", route.getPosition(2).getDescription());
    }

    @Test
    public void readsTheHeaderNameAsTheListName() throws IOException {
        byte[] bytes = concat(header1("00", 0, null, "BMW_Motorrad_v18".getBytes(US_ASCII)), header2("00", 1252, null),
                group("Dealers", waypoint(48.1, 11.5, "Munich")), end());
        assertEquals("BMW_Motorrad_v18", read(bytes).get(0).getName());
    }

    @Test
    public void readsTheHeaderNameInTheCodePageOfTheFile() throws IOException {
        byte[] bytes = concat(header1("00", 0, null, " H\u00e4ndler ".getBytes(WINDOWS_1252)), header2("00", 1252, null),
                group("Dealers", waypoint(48.1, 11.5, "Munich")), end());
        assertEquals("H\u00e4ndler", read(bytes).get(0).getName());
    }

    @Test
    public void ignoresAnEmptyOrPlaceholderHeaderName() throws IOException {
        for (byte[] name : List.of(new byte[0], "   ".getBytes(US_ASCII), "my.gpi".getBytes(US_ASCII))) {
            byte[] bytes = concat(header1("00", 0, null, name), header2("00", 1252, null),
                    group("Dealers", waypoint(48.1, 11.5, "Munich")), end());
            assertNull(read(bytes).get(0).getName());
        }
    }

    @Test
    public void readsTheHeaderNameOfAnObfuscatedFile() throws IOException {
        byte[] headers = concat(header1("01", GarminPoiFormat.OBFUSCATION_UNTIL_END_OF_FILE, productInfo(),
                "Collection".getBytes(US_ASCII)), header2("01", 1252, copyright()));
        byte[] plain = concat(headers, poiGroups(), end());
        byte[] obfuscated = obfuscate(plain, headers.length, plain.length);
        assertEquals("Collection", read(obfuscated).get(0).getName());
    }

    @Test
    public void writesMyGpiForARouteWithoutName() throws IOException {
        byte[] actual = write(route(null, position(11.5, 48.1, "A")), 0x12345678L);
        byte[] header = concat(int32(0), int32(0x16), "GRMREC00".getBytes(US_ASCII), int32(0x12345678), int16(0),
                int16(6), "my.gpi".getBytes(US_ASCII));
        assertArrayEquals(header, Arrays.copyOf(actual, header.length));
    }

    @Test
    public void roundTripsTheListNameThroughTheHeader() throws IOException {
        byte[] bytes = write(route("H\u00e4ndler S\u00fcd", position(11.5, 48.1, "A"), position(11.6, 48.2, "B")), 0x12345678L);
        assertEquals("H\u00e4ndler S\u00fcd", read(bytes).get(0).getName());
    }

    @Test
    public void readsWaypointRecordWithoutExtraDataAndSubordinatedRecordsBehindTheShortName() throws IOException {
        // GPSBabel tag 0x2: no extra data flag, the subordinated records follow the shortname inside the main data
        byte[] main = concat(int32(semicircles(10.0)), int32(semicircles(20.0)), int16(1), new byte[]{0}, string("Plain"),
                comment("described"));
        assertEquals(List.of("Plain; described"), readDescriptions(gpi(1252, record(2, main))));
    }

    @Test
    public void skipsRecordTypesGpsBabelDoesNotInterpretByTheirLength() throws IOException {
        byte[] skipped = concat(
                record(4, int16(0)),                                 // bitmap reference
                record(6, int16(0)),                                 // category reference
                record(0xb, concat(int16(0x1f), new byte[12])),      // address
                record(0xc, concat(int16(0x01), new byte[7])),       // contact
                record(0xd, concat(new byte[]{0}, int32(3), new byte[3])),  // image
                record(16, concat(int16(1), new byte[12])),          // alert circles
                record(19, new byte[26]),                            // safety camera
                record(27, new byte[5]),                             // alert trigger options
                record(0x1234, new byte[9]));                        // unknown
        byte[] bytes = gpi(1252,
                record(15, new byte[5]),                             // product info
                record(17, new byte[24]),                            // copyright
                record(9, concat(string("Group"), area(waypoint(1.0, 2.0, "A", concat(skipped, comment("a"))))),
                        concat(record(7, concat(int16(0), string("Category"))),
                                record(5, new byte[36]),             // bitmap
                                record(18, new byte[3], new byte[8]))),  // media
                record(21, new byte[58]),                            // additions
                group("Next", area(waypoint(3.0, 4.0, "B"))));
        assertEquals(List.of("A; a", "B"), readDescriptions(bytes));
    }

    @Test
    public void appendsTheSpeedOfAnAlertToTheShortName() throws IOException {
        byte[] bytes = gpi(1252, group("Cameras", area(
                waypoint(1.0, 1.0, "Camera", alert(0, 1389)),       // 13.89 m/s = 50.004 km/h
                waypoint(1.1, 1.0, "Camera@30", alert(0, 1389)),    // already has a speed
                waypoint(1.2, 1.0, new byte[2], alert(0, 1389)),     // no shortname
                waypoint(1.3, 1.0, "Tie", alert(0, 125)),           // 4.5 km/h, 1.5.4 rounds half to even
                waypoint(1.4, 1.0, "Slow", alert(0, -5), alert(0, 2083)),  // negative speeds are ignored
                waypoint(1.5, 1.0, "Proximity", alert(250, 0)))));
        assertEquals(List.of("Camera@50", "Camera@30", "WPT@50", "Tie@4", "Slow@75", "Proximity"), readDescriptions(bytes));
    }

    @Test
    public void mapsDescriptionAndNotesAsGpsBabelDoes() throws IOException {
        byte[] bytes = gpi(1252, group("Texts", area(
                waypoint(1.0, 1.0, "Both", comment("description"), notes(1, "note")),
                waypoint(1.1, 1.0, "Reverse", notes(5, "note"), comment("description")),
                waypoint(1.2, 1.0, "Notes", notes(0x32, "note")),
                waypoint(1.3, 1.0, "Ignored", notes(6, "plain")),
                waypoint(1.4, 1.0, "Same", comment("Same and more")))));
        assertEquals(List.of("Both; note", "Reverse; description", "Notes; note", "Ignored", "Same and more"),
                readDescriptions(bytes));
    }

    @Test
    public void fillsAMissingShortNameLikeGpsBabel() throws IOException {
        byte[] noName = new byte[2];
        byte[] bytes = gpi(1252, group("Names", area(
                waypoint(1.0, 1.0, noName, comment("from comment")),
                waypoint(1.1, 1.0, noName, notes(1, "from notes")),
                waypoint(1.2, 1.0, noName))));
        assertEquals(List.of("from comment", "from notes", "WPT003"), readDescriptions(bytes));
    }

    @Test
    public void handlesTextLikeGpsBabelsGpxExchange() throws IOException {
        byte[] bytes = gpi(1252, group("Texts", area(
                waypoint(1.0, 1.0, "Cut\0off", comment("\0gone")),
                waypoint(1.1, 1.0, "Line", comment("a\u0001b\r\nc\rd\te")))));
        // the GPX exchange replaces control characters with a space and normalizes line ends,
        // RouteConverter's descriptions turn line feeds into blanks
        assertEquals(List.of("Cut", "Line; a b c d\te"), readDescriptions(bytes));
    }

    @Test
    public void decodesTheCodePageOfTheFile() throws IOException {
        byte[] polish = {(byte) 0xb3, (byte) 0xf3, 'd', (byte) 0xbf};  // "łódż" in windows-1250
        assertEquals(List.of("łódż"),
                readDescriptions(gpi(1250, group("G", area(waypoint(1.0, 1.0, languageString(polish)))))));
        assertEquals(List.of("Grüße; Straße"),
                readDescriptions(gpi(65001, group("G", area(waypoint(1.0, 1.0, languageString("Grüße".getBytes(UTF_8)),
                        record(0xa, languageString("Straße".getBytes(UTF_8)))))))));
        assertEquals(List.of("Grüße"), readDescriptions(gpi(1252, group("G", area(waypoint(1.0, 1.0, "Grüße"))))));
    }

    @Test
    public void refusesCodePagesGpsBabelCannotDecode() throws IOException {
        assertTrue(read(gpi(932, group("G", area(waypoint(1.0, 1.0, "A"))))).isEmpty());
    }

    @Test
    public void takesTheFirstOfSeveralLanguages() throws IOException {
        byte[] bytes = gpi(1252, group("G", area(waypoint(1.0, 1.0,
                languageString("Hello".getBytes(US_ASCII), "Hallo".getBytes(US_ASCII))))));
        assertEquals(List.of("Hello"), readDescriptions(bytes));
    }

    @Test
    public void readsSingleByteStringsWithoutLanguage() throws IOException {
        byte[] plain = concat(int16(5), "Plain".getBytes(US_ASCII));
        assertEquals(List.of("Plain"), readDescriptions(gpi(1252, group("G", area(waypoint(1.0, 1.0, plain))))));
    }

    @Test
    public void readsTheSpeedOfAnAlertWithExtraData() throws IOException {
        // GPSBabel reads tag 0x80003 without its main length and appends no speed; the Oppmann layout
        // reads the speed of format version 01 speed cameras (declared deviation)
        byte[] alertWithExtraData = record(3, concat(int16(0), int16(1389), int32(0), new byte[]{1, 1, 5, 0x10}),
                record(27, new byte[5]));
        assertEquals(List.of("Camera@50"),
                readDescriptions(gpi(1252, group("G", area(waypoint(1.0, 1.0, "Camera", alertWithExtraData))))));
    }

    @Test
    public void stopsAtAnEndRecordWithFlags() throws IOException {
        byte[] bytes = concat(header1("00", 0, null), header2("00", 1252, null),
                group("G", area(waypoint(1.0, 1.0, "A"))), concat(int16(0xffff), int16(8), int32(0), int32(0)));
        assertEquals(List.of("A"), readDescriptions(bytes));
    }

    private static byte[] nestedAreas(int depth, byte[] innermost) {
        // written front to back: every Area record holds the next one in its extra data
        LittleEndianOutput output = new LittleEndianOutput();
        for (int level = depth; level >= 1; level--) {
            output.writeUInt16(8);
            output.writeUInt16(8);
            output.writeInt32(23 + (level - 1) * 35 + innermost.length);
            output.writeInt32(23);
            output.writeBytes(new byte[23]);
        }
        output.writeBytes(innermost);
        return output.toByteArray();
    }

    @Test
    public void readsWaypointsInNestedAreasUpToTheBound() throws IOException {
        byte[] bytes = gpi(1252, nestedAreas(GarminPoiFormat.MAXIMUM_NESTING - 2, waypoint(1.0, 1.0, "Deep")));
        assertEquals(List.of("Deep"), readDescriptions(bytes));
    }

    @Test
    public void refusesHostilelyDeepNestingWithoutStackOverflow() throws IOException {
        assertTrue(read(gpi(1252, nestedAreas(100_000, new byte[0]))).isEmpty());
        assertTrue(read(gpi(1252, nestedAreas(GarminPoiFormat.MAXIMUM_NESTING + 2, waypoint(1.0, 1.0, "Deep")))).isEmpty());
    }

    // obfuscated files of format version 01

    /**
     * A test-only encoder written from Oppmann's "Obfuscation" section: starting with the first obfuscated
     * byte, consecutive groups of four bytes get the constants 0x48, 0x06, 0xB3, 0x00 added nibble-wise
     * without half-carry, the inverse of the subtraction a reader does.
     */
    private static byte[] obfuscate(byte[] clear, int start, int end) {
        int[] constants = {0x48, 0x06, 0xb3, 0x00};
        byte[] result = clear.clone();
        for (int i = start; i < end; i++) {
            int constant = constants[(i - start) % 4];
            int value = clear[i] & 0xff;
            int high = ((value >> 4) + (constant >> 4)) & 0xf;
            int low = ((value & 0xf) + (constant & 0xf)) & 0xf;
            result[i] = (byte) (high << 4 | low);
        }
        return result;
    }

    private static byte[] productInfo() {
        return record(15, new byte[]{(byte) 0xff, (byte) 0xff, (byte) 0xff, 8, 1});
    }

    private static byte[] copyright() {
        return record(17, concat(int16(0x0004), int16(0), int16(0), int16(1), string("Synthetic source"), string("")));
    }

    private static byte[] poiGroups() {
        return concat(
                group("Fuel", area(waypoint(52.52, 13.405, "Station One", comment("open 24h")),
                        waypoint(48.137, 11.575, "Station Two", alert(0, 834)))),
                group("Camping", area(waypoint(47.37, 8.54, "Lake site", notes(1, "showers")))));
    }

    private static final List<String> POI_GROUP_DESCRIPTIONS = List.of(
            "Station One; open 24h", "Station Two@30", "Lake site; showers");

    @Test
    public void deobfuscatesLikeTheExampleInOppmannsDocument() {
        byte[] bytes = HexFormat.of().parseHex("61736f6e21205d06b30072 06b30070 06".replace(" ", ""));
        GarminPoiFormat.deobfuscate(bytes, 6, bytes.length);
        assertArrayEquals(HexFormat.of().parseHex("61736f6e2120150000003a0000003800"), bytes);
    }

    @Test
    public void readsAnObfuscatedFileUntilTheEndOfFile() throws IOException {
        byte[] headers = concat(header1("01", GarminPoiFormat.OBFUSCATION_UNTIL_END_OF_FILE, productInfo()),
                header2("01", 1252, copyright()));
        byte[] clear = concat(headers, poiGroups(), end());
        byte[] obfuscated = obfuscate(clear, headers.length, clear.length);
        assertTrue(!Arrays.equals(clear, obfuscated));
        assertEquals(POI_GROUP_DESCRIPTIONS, readDescriptions(obfuscated));
    }

    @Test
    public void readsAnObfuscatedFileWithAdditionalData() throws IOException {
        byte[] headers = concat(header1("01", GarminPoiFormat.OBFUSCATION_UNTIL_ADDITIONAL_DATA, productInfo()),
                header2("01", 1252, copyright()));
        byte[] additions = record(21, new byte[58]);
        byte[] recordList = concat(headers, additions, poiGroups(), end());
        byte[] additionalData = new byte[]{1, 2, 3, 4, 5, 6, 7};
        // obfuscation stops behind the record list, the additional data stays clear
        byte[] obfuscated = concat(obfuscate(recordList, headers.length, recordList.length), additionalData);
        assertEquals(POI_GROUP_DESCRIPTIONS, readDescriptions(obfuscated));
    }

    @Test
    public void readsAClearFileOfFormatVersion01() throws IOException {
        byte[] bytes = concat(header1("01", 0, productInfo()), header2("01", 1252, copyright()), poiGroups(), end());
        assertEquals(POI_GROUP_DESCRIPTIONS, readDescriptions(bytes));
    }

    @Test
    public void obfuscatedFileIsGarbageWithoutDeobfuscation() throws IOException {
        // the same obfuscated bytes with an Obfuscation field of 0 must not be read as clear text
        byte[] headers = concat(header1("01", 0, productInfo()), header2("01", 1252, copyright()));
        byte[] clear = concat(headers, poiGroups(), end());
        assertTrue(read(obfuscate(clear, headers.length, clear.length)).isEmpty());
    }

    // write

    private static GpxRoute route(String name, GpxPosition... positions) {
        return new GpxRoute(new Gpx10Format(), Waypoints, name, null, new ArrayList<>(List.of(positions)));
    }

    private static GpxPosition position(double longitude, double latitude, String description) {
        return new GpxPosition(longitude, latitude, null, null, null, description);
    }

    private byte[] write(GpxRoute route, long garminTime) throws IOException {
        return format.write(route, 0, route.getPositionCount(), garminTime);
    }

    private static byte[] writtenString(String string) {
        byte[] bytes = string.getBytes(WINDOWS_1252);
        return concat(int32(bytes.length + 4), "EN".getBytes(US_ASCII), int16(bytes.length), bytes);
    }

    @Test
    public void writesTheExactBytesGpsBabelWritesExceptTheHeaderName() throws IOException {
        GpxRoute route = route("Test", position(11.5, 48.1, "B; second"), position(11.6, 48.2, "A"));
        byte[] actual = write(route, 0x12345678L);

        // the route name instead of GPSBabel's "my.gpi" (decision D16.3)
        byte[] header = concat(int32(0), int32(0x14), "GRMREC00".getBytes(US_ASCII), int32(0x12345678), int16(0),
                int16(4), "Test".getBytes(US_ASCII), int32(1), int32(0xc), "POI".getBytes(US_ASCII), new byte[3],
                "00".getBytes(US_ASCII), int16(1252), int16(0));
        // sorted by shortname; GPSBabel copies the shortname into the empty description of "A"
        byte[] poiA = concat(int32(0x80002), int32(20 + 10 + 17), int32(20),
                int32(GarminPoiFormat.degreesToSemicircles(48.2)), int32(GarminPoiFormat.degreesToSemicircles(11.6)),
                int16(1), new byte[]{0}, writtenString("A"),
                int32(4), int32(2), int16(0),
                int32(0xa), int32(9), writtenString("A"));
        byte[] poiB = concat(int32(0x80002), int32(20 + 10 + 22), int32(20),
                int32(GarminPoiFormat.degreesToSemicircles(48.1)), int32(GarminPoiFormat.degreesToSemicircles(11.5)),
                int16(1), new byte[]{0}, writtenString("B"),
                int32(4), int32(2), int16(0),
                int32(0xa), int32(14), writtenString("second"));
        byte[] area = concat(int32(0x80008), int32(23 + poiA.length + poiB.length), int32(23),
                int32(GarminPoiFormat.degreesToSemicircles(48.2)), int32(GarminPoiFormat.degreesToSemicircles(11.6)),
                int32(GarminPoiFormat.degreesToSemicircles(48.1)), int32(GarminPoiFormat.degreesToSemicircles(11.5)),
                int32(0), int16(1), new byte[]{0}, poiA, poiB);
        byte[] category = writtenString("Test");
        int groupSize = category.length + area.length;
        byte[] group = concat(int32(0x80009), int32(groupSize + 8 + 1636), int32(groupSize), category, area,
                int32(5), int32(1636), GarminPoiFormat.DEFAULT_BITMAP);
        assertArrayEquals(concat(header, group, int32(0xffff), int32(0)), actual);
    }

    @Test
    public void writesTextLikeGpsBabelGotItFromRouteConvertersGpx() throws IOException {
        // the XML parser keeps a carriage return written as &#13;, QTextCodec writes '?' per UTF-16 unit
        byte[] bytes = write(route("Text", position(9.0, 49.0, "Crlf; desc\r\nmore"),
                position(9.1, 49.0, "\uD83D\uDE00 face; x\uD83D\uDE00y")), 0);
        assertTrue(indexOf(bytes, writtenString("desc\r\nmore")) > 0);
        assertTrue(indexOf(bytes, writtenString("?? face")) > 0);
        assertTrue(indexOf(bytes, writtenString("x??y")) > 0);
    }

    @Test
    public void embedsTheGpsBabelBitmap() {
        LittleEndianInput input = new LittleEndianInput(GarminPoiFormat.DEFAULT_BITMAP);
        try {
            assertEquals(0, input.readUInt16());            // bitmap id
            assertEquals(24, input.readUInt16());           // height
            assertEquals(24, input.readUInt16());           // width
            assertEquals(24, input.readUInt16());           // line size
            assertEquals(8, input.readUInt16());            // bits per pixel
        } catch (IOException e) {
            fail(e.getMessage());
        }
        assertEquals(1636, GarminPoiFormat.DEFAULT_BITMAP.length);
    }

    @Test
    public void writesTheRouteNameAsCategory() throws IOException {
        byte[] named = write(route("Größe & <Test> \"q\"", position(1.0, 2.0, "A")), 0);
        assertTrue(indexOf(named, writtenString("Größe & <Test> \"q\"")) > 0);
        byte[] unnamed = write(route(null, position(1.0, 2.0, "A")), 0);
        assertTrue(indexOf(unnamed, writtenString(GarminPoiFormat.DEFAULT_CATEGORY)) > 0);
    }

    private static int indexOf(byte[] bytes, byte[] part) {
        for (int i = 0; i + part.length <= bytes.length; i++)
            if (Arrays.equals(Arrays.copyOfRange(bytes, i, i + part.length), part))
                return i;
        return -1;
    }

    @Test
    public void makesShortNamesUniqueAndDropsNearlyEqualWaypoints() throws IOException {
        GpxRoute route = route("Unique",
                position(10.0, 50.0, "Dup"), position(10.1, 50.0, "Dup"), position(10.2, 50.0, "dup"),
                position(10.0, 50.0, "Dup"), position(10.3, 50.0, "Track one"), position(10.4, 50.0, "a  b   c"),
                position(10.5, 50.0, null), position(10.6, 50.0, "Euro € and Ł"));
        List<String> descriptions = readDescriptions(write(route, 0));
        // the description is the shortname before mkshort() made it unique and collapsed blanks
        assertEquals(List.of("Dup", "Dup.1", "Euro € and ?", "Track 1; Track one", "WPT007", "a b  c; a  b   c", "dup.2"),
                descriptions);
    }

    @Test
    public void comparesDuplicatesWithTheNamesAlreadyMadeUniqueLikeGpsBabel154() throws IOException {
        // enum_waypt_cb() compares the original name with the names mkshort() already made unique: the third
        // waypoint equals the second, which is stored as "Foo.1", so 1.5.4 keeps it as "Foo.2"; only the fourth,
        // which equals the first "Foo", is dropped (checked with the GPSBabel 1.5.4 binary)
        GpxRoute route = route("Dedup", position(10.0, 50.0, "Foo"), position(10.1, 50.0, "Foo"),
                position(10.1, 50.0, "Foo"), position(10.0, 50.0, "Foo"));
        assertEquals(List.of("Foo", "Foo.1", "Foo.2"), readDescriptions(write(route, 0)));
    }

    @Test
    public void splitsIntoBlocksOfAtMost128Waypoints() throws IOException {
        Random random = new Random(4711);
        List<GpxPosition> positions = new ArrayList<>();
        for (int i = 0; i < 700; i++)
            positions.add(position(5 + random.nextDouble() * 10, 45 + random.nextDouble() * 10, "P" + i));
        for (int i = 0; i < 200; i++)
            positions.add(position(8.5, 47.5, "Same" + i));
        GpxRoute route = route("Big", positions.toArray(new GpxPosition[0]));
        byte[] bytes = write(route, 0);

        int areas = 0;
        for (int i = 0; i + 4 <= bytes.length; i++)
            if (Arrays.equals(Arrays.copyOfRange(bytes, i, i + 4), int32(0x80008)))
                areas++;
        assertTrue(areas >= 900 / 128 + 1, "areas: " + areas);
        assertEquals(900, read(bytes).get(0).getPositionCount());
    }

    @Test
    public void readsWhatItWrites() throws IOException {
        GpxRoute route = route("Roundtrip", position(11.5, 48.1, "Name; Description"), position(-70.6, -33.9, "South"),
                position(179.5, 89.5, "Corner; ä ö ü"));
        List<GpxRoute> routes = read(write(route, 0));
        GpxRoute read = routes.get(0);
        assertEquals(3, read.getPositionCount());
        assertEquals(-33.9, read.getPosition(2).getLatitude(), 1e-6);
        assertEquals(-70.6, read.getPosition(2).getLongitude(), 1e-6);
        // sorted by shortname
        assertEquals(List.of("Corner; ä ö ü", "Name; Description", "South"), readDescriptions(write(route, 0)));
        assertEquals(89.5, read.getPosition(0).getLatitude(), 1e-6);
        assertEquals(179.5, read.getPosition(0).getLongitude(), 1e-6);
    }

    @Test
    public void refusesALatitudeGpsBabelRefuses() {
        try {
            write(route("Invalid", position(1.0, 300.0, "A")), 0);
            fail("IOException expected");
        } catch (IOException expected) {
            // waypt_add(): Invalid latitude
        }
    }

    @Test
    public void truncatesSemicirclesLikeGpsBabel154() {
        assertEquals(1, GarminPoiFormat.degreesToSemicircles(1.5 * 180.0 / 2147483648.0));
        assertEquals(-1, GarminPoiFormat.degreesToSemicircles(-1.5 * 180.0 / 2147483648.0));
        assertEquals(Integer.MIN_VALUE, GarminPoiFormat.degreesToSemicircles(180.0));
        assertEquals(Integer.MIN_VALUE, GarminPoiFormat.degreesToSemicircles(Double.NaN));
    }

    // detection

    @Test
    public void returnsNoRouteForForeignOrBrokenInput() throws IOException {
        byte[] valid = gpi(1252, group("G", area(waypoint(1.0, 1.0, "A"))));
        List<byte[]> inputs = new ArrayList<>();
        inputs.add(new byte[0]);
        inputs.add("<?xml version=\"1.0\"?><gpx version=\"1.0\"></gpx>".getBytes(UTF_8));
        inputs.add("11.5,48.1,Name,Description\n".getBytes(UTF_8));
        inputs.add(Arrays.copyOf(valid, 20));                       // truncated header
        inputs.add(Arrays.copyOf(valid, valid.length - 8));         // no End record
        inputs.add(concat(header1("00", 0, null), header2("00", 1252, null)));
        inputs.add(concat(header1("00", 0, null), record(1, "XYZ".getBytes(US_ASCII)), end()));
        inputs.add(gpi(1252, concat(int16(9), int16(0), int32(Integer.MAX_VALUE))));  // record exceeds the file
        inputs.add(gpi(1252, group("G", area(waypoint(1.0, 1.0, concat(int16(8), new byte[]{0, 0, 'e', 'n', 4, 0},
                "Name".getBytes(US_ASCII)))))));            // invalid language code
        inputs.add(gpi(1252, group("Empty")));                     // no waypoints
        Random random = new Random(453);
        for (int i = 0; i < 200; i++) {
            byte[] bytes = valid.clone();
            for (int j = 0; j < 4; j++)
                bytes[16 + random.nextInt(bytes.length - 16)] = (byte) random.nextInt(256);
            inputs.add(bytes);
            byte[] noise = new byte[random.nextInt(200)];
            random.nextBytes(noise);
            inputs.add(noise);
        }
        for (byte[] input : inputs) {
            try {
                List<GpxRoute> routes = read(input);
                if (input.length < 100 || indexOf(input, "GRMREC".getBytes(US_ASCII)) < 0)
                    assertTrue(routes.isEmpty());
            } catch (Exception e) {
                fail("Throws " + e + " for " + HexFormat.of().formatHex(input));
            }
        }
    }

    @Test
    public void isFirstOfTheGreedyFormatsAtTheEndOfTheRegistry() {
        List<NavigationFormat<?>> formats = new ArrayList<>();
        for (NavigationFormat<?> format : new NavigationFormatRegistry().getReadFormats())
            formats.add(format);
        int size = formats.size();
        assertEquals(GarminPoiFormat.class, formats.get(size - 3).getClass());
        assertEquals("Igo8TrackFormat", formats.get(size - 2).getClass().getSimpleName());
        assertEquals(GarminPoiDbFormat.class, formats.get(size - 1).getClass());
        assertEquals("Garmin POI (*.gpi)", formats.get(size - 3).getName());
    }
}
