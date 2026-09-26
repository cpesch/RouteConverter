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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;

/**
 * Computes, per locale, how many of a base locale's resource bundle keys
 * a translated bundle carries, and flags regressions against a previous
 * measurement.
 *
 * This class is pure: it never reads or writes files itself, so its logic
 * can be unit-tested against in-memory key sets. {@link #analyzeDirectory}
 * is the only method that touches the file system.
 *
 * @author Christian Pesch
 */

public class ResourceBundleCompletenessReport {
    public static final double DEFAULT_REGRESSION_THRESHOLD = 95.0;

    public record LocaleCompleteness(int keyCount, int missingCount, double percent) {
    }

    /**
     * Computes completeness of every locale in {@code localeKeys} against
     * {@code baseKeys}. A locale not present in {@code baseKeys} at all
     * still gets a result: 0 keys, all missing, 0 percent.
     */
    public Map<String, LocaleCompleteness> analyze(Map<String, Set<String>> localeKeys, Set<String> baseKeys) {
        Map<String, LocaleCompleteness> result = new TreeMap<>();
        int total = baseKeys.size();
        for (Map.Entry<String, Set<String>> entry : localeKeys.entrySet()) {
            Set<String> keys = entry.getValue();
            int present = 0;
            for (String key : baseKeys) {
                if (keys.contains(key))
                    present++;
            }
            int missing = total - present;
            double percent = total == 0 ? 100.0 : (present * 100.0) / total;
            result.put(entry.getKey(), new LocaleCompleteness(present, missing, percent));
        }
        return result;
    }

    /**
     * Returns the locales that regressed: they were previously at or above
     * {@code threshold} and are now below it. A locale with no previous
     * measurement (a first sighting, or a brand-new locale) never counts
     * as a regression, however low its current percentage is.
     */
    public List<String> findRegressions(Map<String, Double> previousPercentByLocale,
                                         Map<String, LocaleCompleteness> current,
                                         double threshold) {
        List<String> regressions = new ArrayList<>();
        for (Map.Entry<String, LocaleCompleteness> entry : current.entrySet()) {
            Double previous = previousPercentByLocale.get(entry.getKey());
            if (previous == null)
                continue;
            if (previous >= threshold && entry.getValue().percent() < threshold)
                regressions.add(entry.getKey());
        }
        return regressions;
    }

    /**
     * Loads every {@code <prefix>_<locale>.properties} file in {@code directory}
     * and reports its completeness against {@code <prefix>_<baseLocale>.properties}.
     */
    public Map<String, LocaleCompleteness> analyzeDirectory(File directory, String prefix, String baseLocale) throws IOException {
        String baseFileName = prefix + "_" + baseLocale + ".properties";
        Set<String> baseKeys = loadKeys(new File(directory, baseFileName));

        File[] files = directory.listFiles((dir, name) ->
                name.startsWith(prefix + "_") && name.endsWith(".properties"));
        if (files == null)
            throw new IOException("Not a directory: " + directory);

        Map<String, Set<String>> localeKeys = new LinkedHashMap<>();
        for (File file : files) {
            String locale = file.getName().substring(prefix.length() + 1,
                    file.getName().length() - ".properties".length());
            localeKeys.put(locale, loadKeys(file));
        }
        return analyze(localeKeys, baseKeys);
    }

    private Set<String> loadKeys(File file) throws IOException {
        Properties properties = new Properties();
        try (Reader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.ISO_8859_1)) {
            properties.load(reader);
        }
        return properties.stringPropertyNames();
    }

    /**
     * Loads a {@code locale=percent} properties file, as written by a
     * previous run of this report. Used as the "previous measurement" input
     * to {@link #findRegressions}. Returns an empty map if {@code file} is
     * null or does not exist, so a first run never has anything to regress
     * against.
     */
    public Map<String, Double> loadBaseline(File file) throws IOException {
        Map<String, Double> baseline = new TreeMap<>();
        if (file == null || !file.exists())
            return baseline;
        Properties properties = new Properties();
        try (Reader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.ISO_8859_1)) {
            properties.load(reader);
        }
        for (String locale : properties.stringPropertyNames())
            baseline.put(locale, Double.parseDouble(properties.getProperty(locale)));
        return baseline;
    }

    /**
     * Command line entry point for the release-preparation workflow: prints
     * a per-locale completeness report and exits non-zero only when a
     * locale that was previously at or above {@link #DEFAULT_REGRESSION_THRESHOLD}
     * has dropped below it since the baseline file was last written. A
     * locale absent from the baseline (a first sighting, or a brand-new
     * locale, e.g. sr today) never fails the run, however low its
     * percentage is.
     */
    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            System.err.println("Usage: ResourceBundleCompletenessReport <directory> [prefix] [baseLocale] [baselineFile]");
            System.exit(2);
            return;
        }
        File directory = new File(args[0]);
        String prefix = args.length > 1 ? args[1] : "RouteConverter";
        String baseLocale = args.length > 2 ? args[2] : "en";
        File baselineFile = args.length > 3 ? new File(args[3]) : null;

        ResourceBundleCompletenessReport report = new ResourceBundleCompletenessReport();
        Map<String, LocaleCompleteness> result = report.analyzeDirectory(directory, prefix, baseLocale);

        for (Map.Entry<String, LocaleCompleteness> entry : result.entrySet()) {
            LocaleCompleteness completeness = entry.getValue();
            System.out.printf("%-8s %3d keys, %3d missing, %6.1f%%%n",
                    entry.getKey(), completeness.keyCount(), completeness.missingCount(), completeness.percent());
        }

        Map<String, Double> baseline = report.loadBaseline(baselineFile);
        List<String> regressions = report.findRegressions(baseline, result, DEFAULT_REGRESSION_THRESHOLD);
        if (!regressions.isEmpty()) {
            System.err.println("Translation completeness regression in: " + regressions
                    + " (was >= " + DEFAULT_REGRESSION_THRESHOLD + "%, now below)");
            System.exit(1);
        }
    }
}
