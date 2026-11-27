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

package slash.navigation.babel;

import org.junit.jupiter.api.Test;
import slash.navigation.base.AllNavigationFormatRegistry;
import slash.navigation.base.NavigationFormat;
import slash.navigation.base.NavigationFormatParser;
import slash.navigation.base.NavigationFormatRegistry;
import slash.navigation.base.ParserResult;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Compares what the GPSBabel-backed formats write today against the golden files recorded by
 * {@link BabelGoldenGenerator}, so a later native port of one of these formats can be checked
 * against the same baseline (spec 00083 H1).
 *
 * Needs the golden files the maintainer records post-merge by running
 * {@link BabelGoldenGenerator} with GPSBabel 1.5.4 present; excluded from the hermetic CI run
 * (see the {@code failsafe.hermetic.excludes} profiles in the root pom) until then.
 *
 * @author Christian Pesch
 */

public class BabelGoldenWriteIT {
    // classes whose write golden must match byte-for-byte rather than only semantically
    // (via re-reading both outputs and comparing the resulting route dumps, see compareSemantically)
    private static final Set<String> BYTE_STABLE = Set.of(
            "GeoCachingFormat", "GarminPoiDbFormat", "TomTomPoiFormat", "Igo8TrackFormat",
            "GarminPcx5Format", "CompeGPSDataRouteFormat", "CompeGPSDataTrackFormat",
            "CompeGPSDataWaypointFormat", "OziExplorerRouteFormat", "OziExplorerTrackFormat",
            "OziExplorerWaypointFormat", "MagellanMapSendFormat", "AlanTrackLogFormat",
            "AlanWaypointsAndRoutesFormat", "GarminMapSource5Format", "GarminMapSource6Format", "GarminPoiFormat"
    );

    // inputs a native format refuses on purpose where GPSBabel 1.5.4 writes nonsense, as "class/input"
    // (maintainer 2026-11-19: prefer correct behaviour over GPSBabel 1.5.4 bugs): FlightRecorderDataFormat refuses a
    // track without timestamps (issue #392, igc.cc "Bad track timestamp"), 1.5.4 writes the epoch instead
    private static final Set<String> REFUSED_BY_DESIGN = Set.of("FlightRecorderDataFormat/from11.gpx");
    // byte-stable classes ("class": every input, "class/input": that input only) that differ from GPSBabel 1.5.4 on
    // purpose (maintainer 2026-11-19: prefer correct behaviour over GPSBabel 1.5.4 bugs); they are compared
    // semantically against the class's allowlist:
    // Garmin PCX5 writes the time as UTC on a 24 hour clock and a placeholder for a position without time (#389),
    // 1.5.4 writes the local time on a 12 hour clock and the current time;
    // the CompeGPS Data formats write all text as Windows-1252 (#390), 1.5.4 writes names as UTF-8 bytes next to a
    // single byte degree sign;
    // the OziExplorer formats write all text as Windows-1252 (#391), 1.5.4 writes route point text as UTF-8
    // bytes and replaces the euro sign in waypoint files;
    // Magellan MapSend writes all text as Windows-1252 with a length byte that counts the bytes written (#431),
    // 1.5.4 writes UTF-8 bytes behind a length byte that counts characters, which corrupts the file;
    // Garmin MapSource 5.x writes all text as Windows-1252 with record lengths that count the bytes written (#452),
    // 1.5.4 writes UTF-8 bytes, record lengths that count characters and waypoints with non-ASCII names twice;
    // Garmin POI writes the category in the code page the header declares, Windows-1252 (#453), 1.5.4 writes the UTF-8
    // bytes of the command line option, from which its option parser also drops the double quotes
    private static final Set<String> CORRECTED_BY_DESIGN = Set.of(
            "GarminPcx5Format",
            "CompeGPSDataRouteFormat/synthetic", "CompeGPSDataTrackFormat/synthetic", "CompeGPSDataWaypointFormat/synthetic",
            "OziExplorerRouteFormat/from11.gpx", "OziExplorerRouteFormat/synthetic", "OziExplorerWaypointFormat/synthetic",
            "MagellanMapSendFormat/from11.gpx", "MagellanMapSendFormat/synthetic",
            "GarminMapSource5Format/from11.gpx", "GarminMapSource5Format/synthetic",
            "GarminPoiFormat/synthetic",
            // Garmin POI rounds a coordinate to the nearest semicircle (D18, audit 2026-11-27), 1.5.4 truncates it
            "GarminPoiFormat/from10.gpx", "GarminPoiFormat/from10trk.gpx", "GarminPoiFormat/from11.gpx"
    );
    // native writers of formats GPSBabel 1.5.4 never wrote: there is no write golden and none can exist
    // (spec 00083.3 decision D3.1, issue #386: "there is no oracle for the writer"); see TourExchangeFormatTest
    private static final Set<String> WITHOUT_GPSBABEL_WRITER = Set.of("TourExchangeFormat");

    private final NavigationFormatRegistry registry = new AllNavigationFormatRegistry();

    private static List<String> readAllowlist(String className, String sample) throws IOException {
        File allowlistFile = new File(BabelGoldenGenerator.goldenClassDirectory(className), "allowlist.txt");
        if (!allowlistFile.exists())
            return List.of();

        List<String> result = new ArrayList<>();
        for (String line : Files.readAllLines(allowlistFile.toPath(), StandardCharsets.UTF_8)) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#"))
                continue;
            String[] parts = trimmed.split("\\s+", 2);
            if (parts.length > 0 && parts[0].equals(sample))
                result.add(line);
        }
        return result;
    }

    private static String normalizeXmlWhitespace(String xml) {
        return xml.replaceAll(">\\s+<", "><");
    }

    // GPSBabel stamps the moment of writing into these two formats, so their goldens can only
    // be compared with the stamp masked out:
    // PCX5 writes the current local time for every waypoint without a time
    private static final Pattern PCX5_TIME = Pattern.compile("\\d{2}-[A-Z]{3}-\\d{2} \\d{2}:\\d{2}:\\d{2}");
    // Alan .wpr is a fixed-size file; the date and time of the first route are two little-endian
    // 32-bit values behind its waypoint indices
    private static final int ALAN_WPR_ROUTE_DATE_OFFSET = 43492;
    private static final int ALAN_WPR_ROUTE_DATE_LENGTH = 8;

    // Garmin POI stamps the creation time into the Header1 record behind 'GRMREC00'
    private static final int GPI_CREATION_TIME_OFFSET = 16;
    private static final int GPI_CREATION_TIME_LENGTH = 4;

    // Header1 carries the POI collection name: GPSBabel 1.5.4 always writes "my.gpi", the native writer the route
    // name (decision D16.3, maintainer 2026-11-23); the comparison sets the name and Header1's record length aside
    private static final int GPI_HEADER1_LENGTH_OFFSET = 4;
    private static final int GPI_HEADER_NAME_LENGTH_OFFSET = 22;

    private static byte[] withoutGpiHeaderName(byte[] bytes) {
        int nameOffset = GPI_HEADER_NAME_LENGTH_OFFSET + 2;
        if (bytes.length < nameOffset)
            return bytes;
        int nameLength = (bytes[GPI_HEADER_NAME_LENGTH_OFFSET] & 0xff) | (bytes[GPI_HEADER_NAME_LENGTH_OFFSET + 1] & 0xff) << 8;
        if (bytes.length < nameOffset + nameLength)
            return bytes;
        byte[] result = new byte[bytes.length - nameLength];
        System.arraycopy(bytes, 0, result, 0, nameOffset);
        System.arraycopy(bytes, nameOffset + nameLength, result, nameOffset, bytes.length - nameOffset - nameLength);
        Arrays.fill(result, GPI_HEADER1_LENGTH_OFFSET, GPI_HEADER1_LENGTH_OFFSET + 4, (byte) 0);
        Arrays.fill(result, GPI_HEADER_NAME_LENGTH_OFFSET, GPI_HEADER_NAME_LENGTH_OFFSET + 2, (byte) 0);
        return result;
    }

    private static boolean gpiEqual(byte[] expected, byte[] actual) {
        expected = withoutGpiHeaderName(expected);
        actual = withoutGpiHeaderName(actual);
        int end = GPI_CREATION_TIME_OFFSET + GPI_CREATION_TIME_LENGTH;
        if (expected.length != actual.length || expected.length < end)
            return Arrays.equals(expected, actual);
        byte[] expectedMasked = expected.clone();
        byte[] actualMasked = actual.clone();
        Arrays.fill(expectedMasked, GPI_CREATION_TIME_OFFSET, end, (byte) 0);
        Arrays.fill(actualMasked, GPI_CREATION_TIME_OFFSET, end, (byte) 0);
        return Arrays.equals(expectedMasked, actualMasked);
    }

    private static boolean pcx5Equal(byte[] expected, byte[] actual) {
        String[] expectedLines = new String(expected, StandardCharsets.ISO_8859_1).split("\n", -1);
        String[] actualLines = new String(actual, StandardCharsets.ISO_8859_1).split("\n", -1);
        if (expectedLines.length != actualLines.length)
            return false;
        for (int i = 0; i < expectedLines.length; i++) {
            if (expectedLines[i].equals(actualLines[i]))
                continue;
            String expectedMasked = PCX5_TIME.matcher(expectedLines[i]).replaceAll("<time>");
            String actualMasked = PCX5_TIME.matcher(actualLines[i]).replaceAll("<time>");
            if (!expectedMasked.equals(actualMasked))
                return false;
        }
        return true;
    }

    private static boolean alanWprEqual(byte[] expected, byte[] actual) {
        int end = ALAN_WPR_ROUTE_DATE_OFFSET + ALAN_WPR_ROUTE_DATE_LENGTH;
        if (expected.length != actual.length || expected.length < end)
            return Arrays.equals(expected, actual);
        byte[] expectedMasked = expected.clone();
        byte[] actualMasked = actual.clone();
        Arrays.fill(expectedMasked, ALAN_WPR_ROUTE_DATE_OFFSET, end, (byte) 0);
        Arrays.fill(actualMasked, ALAN_WPR_ROUTE_DATE_OFFSET, end, (byte) 0);
        return Arrays.equals(expectedMasked, actualMasked);
    }

    // Garmin MapSource 6.x: the header's creator record names the writer and its release date (spec 00083.15
    // D15.1, issue #451): signature, version and every record after the header must match byte for byte, the
    // creator's watermark and release date only where the class's allowlist names them
    private static final int GDB_CREATOR_RECORD_OFFSET = 13;
    static final String GDB_WATERMARK = "header.creator.watermark";
    static final String GDB_RELEASE_DATE = "header.creator.release-date";

    // the creator record's fields: [program version, watermark, date, time, everything behind the record]
    private static List<byte[]> splitGdbHeader(byte[] bytes) {
        if (bytes.length < GDB_CREATOR_RECORD_OFFSET + 5)
            return null;
        int length = (bytes[GDB_CREATOR_RECORD_OFFSET] & 0xFF) | (bytes[GDB_CREATOR_RECORD_OFFSET + 1] & 0xFF) << 8 |
                (bytes[GDB_CREATOR_RECORD_OFFSET + 2] & 0xFF) << 16 | (bytes[GDB_CREATOR_RECORD_OFFSET + 3] & 0xFF) << 24;
        int start = GDB_CREATOR_RECORD_OFFSET + 5;
        int end = start + length;
        if (bytes[GDB_CREATOR_RECORD_OFFSET + 4] != 'A' || length < 2 || end > bytes.length)
            return null;
        List<byte[]> result = new ArrayList<>();
        result.add(Arrays.copyOfRange(bytes, 0, start + 2));
        int position = start + 2;
        for (int i = 0; i < 3; i++) {
            int nul = position;
            while (nul < end && bytes[nul] != 0)
                nul++;
            if (nul >= end)
                return null;
            result.add(Arrays.copyOfRange(bytes, position, nul));
            position = nul + 1;
        }
        if (position != end)
            return null;
        result.add(Arrays.copyOfRange(bytes, end, bytes.length));
        return result;
    }

    private static boolean isAllowlisted(List<String> allowlist, String field) {
        for (String line : allowlist) {
            String[] parts = line.trim().split("\\s+", 3);
            if (parts.length > 1 && parts[1].equals(field))
                return true;
        }
        return false;
    }

    private static boolean gdbEqual(byte[] expected, byte[] actual, List<String> allowlist) {
        List<byte[]> expectedParts = splitGdbHeader(expected);
        List<byte[]> actualParts = splitGdbHeader(actual);
        if (expectedParts == null || actualParts == null)
            return Arrays.equals(expected, actual);
        // the record length in the header prefix follows the watermark and date
        boolean variableHeader = isAllowlisted(allowlist, GDB_WATERMARK) || isAllowlisted(allowlist, GDB_RELEASE_DATE);
        byte[] expectedPrefix = expectedParts.get(0), actualPrefix = actualParts.get(0);
        if (variableHeader) {
            expectedPrefix = expectedPrefix.clone();
            actualPrefix = actualPrefix.clone();
            Arrays.fill(expectedPrefix, GDB_CREATOR_RECORD_OFFSET, GDB_CREATOR_RECORD_OFFSET + 4, (byte) 0);
            Arrays.fill(actualPrefix, GDB_CREATOR_RECORD_OFFSET, GDB_CREATOR_RECORD_OFFSET + 4, (byte) 0);
        }
        return Arrays.equals(expectedPrefix, actualPrefix) &&
                (isAllowlisted(allowlist, GDB_WATERMARK) || Arrays.equals(expectedParts.get(1), actualParts.get(1))) &&
                (isAllowlisted(allowlist, GDB_RELEASE_DATE) || Arrays.equals(expectedParts.get(2), actualParts.get(2))) &&
                (isAllowlisted(allowlist, GDB_RELEASE_DATE) || Arrays.equals(expectedParts.get(3), actualParts.get(3))) &&
                Arrays.equals(expectedParts.get(4), actualParts.get(4));
    }

    private static boolean bytesEqual(String className, byte[] expected, byte[] actual, List<String> allowlist) {
        if ("GeoCachingFormat".equals(className)) {
            String expectedText = normalizeXmlWhitespace(new String(expected, StandardCharsets.UTF_8));
            String actualText = normalizeXmlWhitespace(new String(actual, StandardCharsets.UTF_8));
            return expectedText.equals(actualText);
        }
        if ("GarminPcx5Format".equals(className))
            return pcx5Equal(expected, actual);
        if ("AlanWaypointsAndRoutesFormat".equals(className))
            return alanWprEqual(expected, actual);
        if ("GarminMapSource6Format".equals(className))
            return gdbEqual(expected, actual, allowlist);
        if ("GarminPoiFormat".equals(className))
            return gpiEqual(expected, actual);
        return Arrays.equals(expected, actual);
    }

    private static String dumpJson(NavigationFormat<?> format, NavigationFormatRegistry registry, byte[] bytes) throws IOException {
        NavigationFormatParser parser = new NavigationFormatParser(registry);
        ParserResult result = parser.read(new ByteArrayInputStream(bytes), singletonList(format));
        return result.isSuccessful() ? BabelGoldenDump.toJson(result.getAllRoutes()) : "{\"routes\":[]}";
    }

    private void compareInput(String className, NavigationFormat<?> format, String inputName, List<String> differences) throws IOException {
        File writeDirectory = BabelGoldenGenerator.goldenDirectory(className, "write");
        File bytesGolden = new File(writeDirectory, inputName + format.getExtension());
        File errorGolden = new File(writeDirectory, inputName + ".error");

        if (errorGolden.exists()) {
            try {
                BabelGoldenGenerator.writeBytes(format, BabelGoldenGenerator.createWriteInputRoute(inputName));
                differences.add(className + " " + inputName + ": expected write to throw, but it succeeded");
            } catch (Exception expected) {
                // the golden expects a write failure and got one - nothing to compare
            }
            return;
        }

        if (!bytesGolden.exists()) {
            fail("Missing write golden " + bytesGolden);
            return;
        }

        byte[] expectedBytes = Files.readAllBytes(bytesGolden.toPath());
        byte[] actualBytes;
        try {
            actualBytes = BabelGoldenGenerator.writeBytes(format, BabelGoldenGenerator.createWriteInputRoute(inputName));
        } catch (IOException refused) {
            if (!REFUSED_BY_DESIGN.contains(className + "/" + inputName))
                throw refused;
            return;
        }

        boolean corrected = CORRECTED_BY_DESIGN.contains(className) || CORRECTED_BY_DESIGN.contains(className + "/" + inputName);
        if (BYTE_STABLE.contains(className) && !corrected) {
            if (!bytesEqual(className, expectedBytes, actualBytes, readAllowlist(className, "write/" + inputName)))
                differences.add(className + " " + inputName + ": byte-stable write golden differs");
            return;
        }

        String expectedJson = dumpJson(format, registry, expectedBytes);
        String actualJson = dumpJson(format, registry, actualBytes);
        List<String> allowlist = readAllowlist(className, "write/" + inputName);
        for (String difference : BabelGoldenComparator.compare(expectedJson, actualJson, allowlist))
            differences.add(className + " " + inputName + " " + difference);
    }

    @Test
    public void compareWriteGoldensToGpsBabel() throws IOException {
        Map<String, NavigationFormat<?>> formatsByName = BabelGoldenGenerator.indexFormatsBySimpleName(registry.getWriteFormats());
        Set<String> writableClassNames = new LinkedHashSet<>();
        for (BabelGoldenGenerator.Sample sample : BabelGoldenGenerator.readSamples())
            if (formatsByName.get(sample.className()) != null && !WITHOUT_GPSBABEL_WRITER.contains(sample.className()))
                writableClassNames.add(sample.className());

        List<String> allDifferences = new ArrayList<>();
        for (String className : writableClassNames) {
            NavigationFormat<?> format = formatsByName.get(className);
            for (String inputName : BabelGoldenGenerator.WRITE_INPUT_NAMES)
                compareInput(className, format, inputName, allDifferences);
        }

        assertTrue(allDifferences.isEmpty(), "Differences from golden write files:\n" + String.join("\n", allDifferences));
    }
}
