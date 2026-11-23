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
import slash.navigation.base.BaseRoute;
import slash.navigation.base.NavigationFormat;
import slash.navigation.base.NavigationFormatConverter;
import slash.navigation.base.NavigationFormatParser;
import slash.navigation.base.NavigationFormatRegistry;
import slash.navigation.base.ParserResult;
import slash.navigation.gpx.Gpx10Format;
import slash.navigation.gpx.GpxPosition;
import slash.navigation.gpx.GpxRoute;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static slash.common.type.CompactCalendar.fromMillis;
import static slash.navigation.base.NavigationTestCase.ROUTE_PATH;
import static slash.navigation.base.NavigationTestCase.TEST_PATH;
import static slash.navigation.base.RouteCharacteristics.Route;

/**
 * Generates the golden files {@link BabelGoldenReadIT} and {@link BabelGoldenWriteIT} compare
 * against, recording what GPSBabel-backed formats deliver today (spec 00083 H1). Not matched by
 * surefire's default patterns; run explicitly:
 * {@code mvn -pl navigation-formats test -Dtest=BabelGoldenGenerator -Dbabel.golden.generate=true}
 *
 * @author Christian Pesch
 */

public class BabelGoldenGenerator {
    private static final String GOLDEN_VERSION = "babel-1.5.4";
    private static final String SAMPLES_RESOURCE = "babel-golden-samples.txt";
    static final List<String> WRITE_INPUTS = List.of("from10.gpx", "from10trk.gpx", "from11.gpx");
    // 2026-01-01T10:00:00Z
    private static final long SYNTHETIC_ROUTE_BASE_TIME = 1767261600000L;

    public record Sample(String className, String path) {
    }

    static List<Sample> readSamples() throws IOException {
        List<Sample> result = new ArrayList<>();
        try (InputStream stream = BabelGoldenGenerator.class.getResourceAsStream(SAMPLES_RESOURCE)) {
            if (stream == null)
                throw new IOException("Cannot find resource " + SAMPLES_RESOURCE);
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String trimmed = line.trim();
                    if (trimmed.isEmpty() || trimmed.startsWith("#"))
                        continue;
                    String[] parts = trimmed.split("\\s+", 2);
                    if (parts.length != 2)
                        continue;
                    result.add(new Sample(parts[0], parts[1]));
                }
            }
        }
        return result;
    }

    static File goldenClassDirectory(String className) {
        return new File(ROUTE_PATH + "golden" + File.separator + GOLDEN_VERSION + File.separator + className);
    }

    static File goldenDirectory(String className, String kind) {
        return new File(goldenClassDirectory(className), kind);
    }

    private static void writeFile(File file, byte[] bytes) throws IOException {
        File parent = file.getParentFile();
        if (!parent.exists() && !parent.mkdirs())
            throw new IOException("Cannot create directory " + parent);
        try (OutputStream out = new FileOutputStream(file)) {
            out.write(bytes);
        }
    }

    private static void writeText(File file, String text) throws IOException {
        writeFile(file, text.getBytes(StandardCharsets.UTF_8));
    }

    static Map<String, NavigationFormat<?>> indexFormatsBySimpleName(List<NavigationFormat<BaseRoute<?, ?>>> formats) {
        Map<String, NavigationFormat<?>> result = new HashMap<>();
        for (NavigationFormat<?> format : formats)
            result.put(format.getClass().getSimpleName(), format);
        return result;
    }

    /**
     * Only GPSBabel-backed formats are recorded: a format that has been ported natively keeps the
     * golden GPSBabel once produced for it, which is the oracle the port is checked against.
     */
    static boolean isGpsBabelBacked(NavigationFormat<?> format) {
        return format instanceof BabelFormat;
    }

    private void generateReadGoldens(NavigationFormatRegistry registry) throws IOException {
        Map<String, NavigationFormat<?>> formatsByName = indexFormatsBySimpleName(registry.getReadFormats());
        NavigationFormatParser parser = new NavigationFormatParser(registry);

        for (Sample sample : readSamples()) {
            NavigationFormat<?> format = formatsByName.get(sample.className());
            if (format == null)
                throw new IllegalStateException("Cannot find read format " + sample.className());
            if (!isGpsBabelBacked(format))
                continue;

            File source = new File(ROUTE_PATH + sample.path());
            ParserResult result = parser.read(source, singletonList(format));
            String json = result.isSuccessful() ? BabelGoldenDump.toJson(result.getAllRoutes()) : "{\"routes\":[]}";

            String fileName = sample.path().replace("/", "__") + ".json";
            writeText(new File(goldenDirectory(sample.className(), "read"), fileName), json);
        }
    }

    static final List<String> WRITE_INPUT_NAMES;
    static {
        List<String> names = new ArrayList<>(WRITE_INPUTS);
        names.add("synthetic");
        WRITE_INPUT_NAMES = List.copyOf(names);
    }

    private static slash.common.type.CompactCalendar syntheticTime(int offsetSeconds) {
        return fromMillis(SYNTHETIC_ROUTE_BASE_TIME + offsetSeconds * 1000L);
    }

    static GpxRoute createSyntheticRoute() {
        List<GpxPosition> positions = new ArrayList<>();
        positions.add(new GpxPosition(11.500, 48.100, 1.5, null, syntheticTime(0), "Ä 1"));
        positions.add(new GpxPosition(11.501, 48.101, 2.5, null, syntheticTime(60), "ß,2"));
        positions.add(new GpxPosition(11.502, 48.102, null, null, syntheticTime(120), "€ 3"));
        return new GpxRoute(new Gpx10Format(), Route, "Größe & <Test> \"q\"", null, positions);
    }

    private static List<GpxRoute> createWriteInputRoutes() throws IOException {
        NavigationFormatParser parser = new NavigationFormatParser(new AllNavigationFormatRegistry());
        List<GpxRoute> result = new ArrayList<>();
        for (String inputName : WRITE_INPUTS) {
            File file = new File(TEST_PATH + inputName);
            ParserResult parserResult = parser.read(file);
            if (!parserResult.isSuccessful())
                throw new IOException("Cannot read " + file);
            result.add((GpxRoute) NavigationFormatConverter.asFormat(parserResult.getTheRoute(), new Gpx10Format()));
        }
        return result;
    }

    /**
     * Builds the route for one of {@link #WRITE_INPUT_NAMES} - the fixed set of write-golden
     * inputs, independent of the per-class samples in {@code babel-golden-samples.txt} - keeping
     * {@link BabelGoldenGenerator} and {@link BabelGoldenWriteIT} looking at the exact same input.
     */
    static GpxRoute createWriteInputRoute(String inputName) throws IOException {
        if ("synthetic".equals(inputName))
            return createSyntheticRoute();
        int index = WRITE_INPUTS.indexOf(inputName);
        if (index < 0)
            throw new IllegalArgumentException("Unknown write input " + inputName);
        return createWriteInputRoutes().get(index);
    }

    @SuppressWarnings("unchecked")
    static byte[] writeBytes(NavigationFormat<?> format, GpxRoute route) throws IOException {
        NavigationFormat<BaseRoute<?, ?>> anyFormat = (NavigationFormat<BaseRoute<?, ?>>) format;
        BaseRoute<?, ?> converted = NavigationFormatConverter.asFormat(route, anyFormat);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        anyFormat.write(converted, out, 0, converted.getPositionCount());
        return out.toByteArray();
    }

    private void writeWriteGolden(String className, NavigationFormat<?> format, String inputName, GpxRoute route) throws IOException {
        File writeDirectory = goldenDirectory(className, "write");
        try {
            byte[] bytes = writeBytes(format, route);
            writeFile(new File(writeDirectory, inputName + format.getExtension()), bytes);
        } catch (Exception e) {
            writeText(new File(writeDirectory, inputName + ".error"), String.valueOf(e.getMessage()));
        }
    }

    private void generateWriteGoldens(NavigationFormatRegistry registry) throws IOException {
        Map<String, NavigationFormat<?>> formatsByName = indexFormatsBySimpleName(registry.getWriteFormats());
        Set<String> classNames = new LinkedHashSet<>();
        for (Sample sample : readSamples())
            classNames.add(sample.className());

        for (String className : classNames) {
            NavigationFormat<?> format = formatsByName.get(className);
            if (format == null || !format.isSupportsWriting() || !isGpsBabelBacked(format))
                continue;

            // a fresh route per write: formats like OziExplorerFormat#prepareForWriting modify the route they
            // are given, and BabelGoldenWriteIT hands every write a fresh route, too
            for (String inputName : WRITE_INPUT_NAMES)
                writeWriteGolden(className, format, inputName, createWriteInputRoute(inputName));
        }
    }

    @Test
    public void generate() throws IOException {
        assumeTrue(Boolean.getBoolean("babel.golden.generate"));

        NavigationFormatRegistry registry = new AllNavigationFormatRegistry();
        generateReadGoldens(registry);
        generateWriteGoldens(registry);
    }
}
