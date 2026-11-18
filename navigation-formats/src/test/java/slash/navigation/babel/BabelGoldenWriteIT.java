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

import org.junit.Test;
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

import static java.util.Collections.singletonList;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

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
            "AlanWaypointsAndRoutesFormat", "GarminMapSource5Format"
    );

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

    private static boolean bytesEqual(String className, byte[] expected, byte[] actual) {
        if ("GeoCachingFormat".equals(className)) {
            String expectedText = normalizeXmlWhitespace(new String(expected, StandardCharsets.UTF_8));
            String actualText = normalizeXmlWhitespace(new String(actual, StandardCharsets.UTF_8));
            return expectedText.equals(actualText);
        }
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
        byte[] actualBytes = BabelGoldenGenerator.writeBytes(format, BabelGoldenGenerator.createWriteInputRoute(inputName));

        if (BYTE_STABLE.contains(className)) {
            if (!bytesEqual(className, expectedBytes, actualBytes))
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
            if (formatsByName.get(sample.className()) != null)
                writableClassNames.add(sample.className());

        List<String> allDifferences = new ArrayList<>();
        for (String className : writableClassNames) {
            NavigationFormat<?> format = formatsByName.get(className);
            for (String inputName : BabelGoldenGenerator.WRITE_INPUT_NAMES)
                compareInput(className, format, inputName, allDifferences);
        }

        assertTrue("Differences from golden write files:\n" + String.join("\n", allDifferences), allDifferences.isEmpty());
    }
}
