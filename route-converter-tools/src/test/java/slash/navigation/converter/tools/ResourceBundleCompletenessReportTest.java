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
package slash.navigation.converter.tools;

import org.junit.Test;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.*;
import static slash.navigation.converter.tools.ResourceBundleCompletenessReport.DEFAULT_REGRESSION_THRESHOLD;
import static slash.navigation.converter.tools.ResourceBundleCompletenessReport.LocaleCompleteness;

/**
 * Tests for {@link ResourceBundleCompletenessReport}.
 *
 * @author Christian Pesch
 */
public class ResourceBundleCompletenessReportTest {
    private final ResourceBundleCompletenessReport report = new ResourceBundleCompletenessReport();

    private static Set<String> keys(String... keys) {
        return new HashSet<>(Set.of(keys));
    }

    @Test
    public void testCompleteLocaleIs100Percent() {
        Set<String> base = keys("a", "b", "c", "d");
        Map<String, Set<String>> locales = new LinkedHashMap<>();
        locales.put("de", keys("a", "b", "c", "d"));

        Map<String, ResourceBundleCompletenessReport.LocaleCompleteness> result = report.analyze(locales, base);

        ResourceBundleCompletenessReport.LocaleCompleteness de = result.get("de");
        assertEquals(4, de.keyCount());
        assertEquals(0, de.missingCount());
        assertEquals(100.0, de.percent(), 0.001);
    }

    @Test
    public void testHalfTranslatedLocaleIs50Percent() {
        Set<String> base = keys("a", "b", "c", "d");
        Map<String, Set<String>> locales = new LinkedHashMap<>();
        locales.put("half", keys("a", "b"));

        Map<String, ResourceBundleCompletenessReport.LocaleCompleteness> result = report.analyze(locales, base);

        ResourceBundleCompletenessReport.LocaleCompleteness half = result.get("half");
        assertEquals(2, half.keyCount());
        assertEquals(2, half.missingCount());
        assertEquals(50.0, half.percent(), 0.001);
    }

    @Test
    public void testEmptyLocaleIs0Percent() {
        Set<String> base = keys("a", "b", "c", "d");
        Map<String, Set<String>> locales = new LinkedHashMap<>();
        locales.put("empty", keys());

        Map<String, ResourceBundleCompletenessReport.LocaleCompleteness> result = report.analyze(locales, base);

        ResourceBundleCompletenessReport.LocaleCompleteness empty = result.get("empty");
        assertEquals(0, empty.keyCount());
        assertEquals(4, empty.missingCount());
        assertEquals(0.0, empty.percent(), 0.001);
    }

    @Test
    public void testEmptyBaseIs100PercentForEveryLocale() {
        Map<String, Set<String>> locales = new LinkedHashMap<>();
        locales.put("any", keys());

        Map<String, ResourceBundleCompletenessReport.LocaleCompleteness> result = report.analyze(locales, keys());

        assertEquals(100.0, result.get("any").percent(), 0.001);
    }

    @Test
    public void testRegressionFiresWhenPreviouslyAboveThresholdDropsBelow() {
        Map<String, Double> previous = Map.of("fr", 96.0);
        Map<String, ResourceBundleCompletenessReport.LocaleCompleteness> current = Map.of(
                "fr", new ResourceBundleCompletenessReport.LocaleCompleteness(90, 10, 90.0));

        assertEquals(java.util.List.of("fr"), report.findRegressions(previous, current, 95.0));
    }

    @Test
    public void testNoRegressionOnFirstSighting() {
        Map<String, Double> previous = Map.of();
        Map<String, ResourceBundleCompletenessReport.LocaleCompleteness> current = Map.of(
                "sr", new ResourceBundleCompletenessReport.LocaleCompleteness(60, 40, 60.0));

        assertTrue(report.findRegressions(previous, current, 95.0).isEmpty());
    }

    @Test
    public void testNoRegressionWhenPreviouslyAlreadyBelowThreshold() {
        Map<String, Double> previous = Map.of("sr", 79.0);
        Map<String, ResourceBundleCompletenessReport.LocaleCompleteness> current = Map.of(
                "sr", new ResourceBundleCompletenessReport.LocaleCompleteness(60, 40, 60.0));

        assertTrue(report.findRegressions(previous, current, 95.0).isEmpty());
    }

    @Test
    public void testNoRegressionWhenStillAboveThreshold() {
        Map<String, Double> previous = Map.of("de", 100.0);
        Map<String, ResourceBundleCompletenessReport.LocaleCompleteness> current = Map.of(
                "de", new ResourceBundleCompletenessReport.LocaleCompleteness(98, 2, 98.0));

        assertTrue(report.findRegressions(previous, current, 95.0).isEmpty());
    }

    @Test
    public void testAnalyzeDirectoryReadsPropertiesFiles() throws IOException {
        File directory = Files.createTempDirectory("resource-bundle-completeness-test").toFile();
        directory.deleteOnExit();
        writeProperties(directory, "Bundle_en.properties", "a=1\nb=2\nc=3\nd=4\n");
        writeProperties(directory, "Bundle_de.properties", "a=1\nb=2\nc=3\nd=4\n");
        writeProperties(directory, "Bundle_fr.properties", "a=1\nb=2\n");

        Map<String, ResourceBundleCompletenessReport.LocaleCompleteness> result =
                report.analyzeDirectory(directory, "Bundle", "en");

        assertEquals(100.0, result.get("en").percent(), 0.001);
        assertEquals(100.0, result.get("de").percent(), 0.001);
        assertEquals(50.0, result.get("fr").percent(), 0.001);
    }

    @Test
    public void testLoadBaselineReadsLocalePercentPairs() throws IOException {
        File directory = Files.createTempDirectory("resource-bundle-completeness-baseline-test").toFile();
        directory.deleteOnExit();
        File baseline = new File(directory, "baseline.properties");
        writeProperties(directory, "baseline.properties", "de=100.0\nfr=96.5\n");

        Map<String, Double> result = report.loadBaseline(baseline);

        assertEquals(100.0, result.get("de"), 0.001);
        assertEquals(96.5, result.get("fr"), 0.001);
    }

    @Test
    public void testLoadBaselineOfMissingFileIsEmpty() throws IOException {
        Map<String, Double> result = report.loadBaseline(new File("does-not-exist.properties"));

        assertTrue(result.isEmpty());
    }

    @Test
    public void testLoadBaselineOfNullFileIsEmpty() throws IOException {
        Map<String, Double> result = report.loadBaseline(null);

        assertTrue(result.isEmpty());
    }

    @Test
    public void testCheckedInBaselineHasNoRegressionAgainstCurrentTree() throws IOException {
        // Guards the committed baseline itself: re-running the reporter
        // against the current resource bundles must never regress versus
        // route-converter-tools/src/main/resources/resource-bundle-completeness-baseline.properties,
        // the same file .github/workflows/release-prepare.yml reads.
        File guiResources = new File("../route-converter-gui/src/main/resources/slash/navigation/converter/gui");
        if (!guiResources.isDirectory())
            return; // module not checked out side-by-side (e.g. isolated jar test run); covered by CI instead

        File baseline = new File("src/main/resources/resource-bundle-completeness-baseline.properties");
        Map<String, LocaleCompleteness> current = report.analyzeDirectory(guiResources, "RouteConverter", "en");
        Map<String, Double> previous = report.loadBaseline(baseline);

        assertTrue(report.findRegressions(previous, current, DEFAULT_REGRESSION_THRESHOLD).isEmpty());
    }

    private void writeProperties(File directory, String name, String content) throws IOException {
        try (FileWriter writer = new FileWriter(new File(directory, name))) {
            writer.write(content);
        }
    }
}
