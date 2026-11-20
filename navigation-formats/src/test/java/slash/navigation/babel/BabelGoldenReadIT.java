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

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static java.util.Collections.singletonList;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static slash.navigation.base.NavigationTestCase.ROUTE_PATH;

/**
 * Compares what the GPSBabel-backed formats read today against the golden files recorded by
 * {@link BabelGoldenGenerator}, so a later native port of one of these formats can be checked
 * against the same baseline (spec 00083 H1).
 *
 * Needs the golden files the maintainer records post-merge by running
 * {@link BabelGoldenGenerator} with GPSBabel 1.5.4 present; excluded from the hermetic CI run
 * (see the {@code failsafe.hermetic.excludes} profiles in the root pom) until then.
 *
 * @author Christian Pesch
 */

public class BabelGoldenReadIT {
    private final NavigationFormatRegistry registry = new AllNavigationFormatRegistry();
    private final NavigationFormatParser parser = new NavigationFormatParser(registry);

    private static List<String> readAllowlist(String className, String samplePath) throws IOException {
        File allowlistFile = new File(BabelGoldenGenerator.goldenClassDirectory(className), "allowlist.txt");
        if (!allowlistFile.exists())
            return List.of();

        List<String> result = new ArrayList<>();
        for (String line : Files.readAllLines(allowlistFile.toPath(), StandardCharsets.UTF_8)) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#"))
                continue;
            String[] parts = trimmed.split("\\s+", 2);
            if (parts.length > 0 && parts[0].equals(samplePath))
                result.add(line);
        }
        return result;
    }

    @Test
    public void compareReadGoldensToGpsBabel() throws IOException {
        Map<String, NavigationFormat<?>> formatsByName = BabelGoldenGenerator.indexFormatsBySimpleName(registry.getReadFormats());
        List<String> allDifferences = new ArrayList<>();

        for (BabelGoldenGenerator.Sample sample : BabelGoldenGenerator.readSamples()) {
            NavigationFormat<?> format = formatsByName.get(sample.className());
            if (format == null)
                throw new IllegalStateException("Cannot find read format " + sample.className());

            File golden = new File(BabelGoldenGenerator.goldenClassDirectory(sample.className()),
                    "read" + File.separator + sample.path().replace("/", "__") + ".json");
            if (!golden.exists()) {
                fail("Missing read golden " + golden);
                return;
            }
            String expectedJson = Files.readString(golden.toPath(), StandardCharsets.UTF_8);

            File source = new File(ROUTE_PATH + sample.path());
            ParserResult result = parser.read(source, singletonList(format));
            String actualJson = result.isSuccessful() ? BabelGoldenDump.toJson(result.getAllRoutes()) : "{\"routes\":[]}";

            List<String> allowlist = readAllowlist(sample.className(), sample.path());
            for (String difference : BabelGoldenComparator.compare(expectedJson, actualJson, allowlist))
                allDifferences.add(sample.className() + " " + sample.path() + " " + difference);
        }

        assertTrue("Differences from golden read files:\n" + String.join("\n", allDifferences), allDifferences.isEmpty());
    }
}
