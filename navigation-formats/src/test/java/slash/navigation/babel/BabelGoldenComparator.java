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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

import static java.lang.Double.parseDouble;
import static java.lang.Math.abs;

/**
 * Compares two {@link BabelGoldenDump} JSON dumps with per-field tolerances, so a later native
 * port can be checked against what GPSBabel delivered without demanding bit-for-bit equality
 * (spec 00083 H1).
 *
 * The {@code allowlist} is the list of lines of a per-format {@code allowlist.txt} that the
 * caller (e.g. {@link BabelGoldenReadIT}) has already narrowed down to the sample under test -
 * i.e. only lines whose {@code <sample>} column matches the sample currently being compared are
 * passed in. What is left for {@link #compare} to apply is the {@code <path-glob>} column,
 * matched against the dotted path of each difference (e.g. {@code route[0].position[3].heading}).
 *
 * @author Christian Pesch
 */

public class BabelGoldenComparator {
    private static final double COORDINATE_TOLERANCE = 1e-6;
    private static final double ELEVATION_TOLERANCE = 0.05;
    private static final double SPEED_TOLERANCE = 0.01;
    private static final double HEADING_TOLERANCE = 0.01;
    private static final double DOP_TOLERANCE = 0.01;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private BabelGoldenComparator() {
    }

    public static List<String> compare(String expectedJson, String actualJson, List<String> allowlist) {
        List<String> differences = new ArrayList<>();
        JsonNode expected, actual;
        try {
            expected = MAPPER.readTree(expectedJson);
            actual = MAPPER.readTree(actualJson);
        } catch (IOException e) {
            throw new IllegalArgumentException("Cannot parse golden JSON", e);
        }
        compareRoutes(expected.get("routes"), actual.get("routes"), differences);
        return filterAllowlisted(differences, allowlist);
    }

    private static void compareRoutes(JsonNode expectedRoutes, JsonNode actualRoutes, List<String> differences) {
        if (expectedRoutes.size() != actualRoutes.size()) {
            differences.add("routes.size: expected " + expectedRoutes.size() + " actual " + actualRoutes.size());
            return;
        }
        for (int i = 0; i < expectedRoutes.size(); i++)
            compareRoute(i, expectedRoutes.get(i), actualRoutes.get(i), differences);
    }

    private static void compareRoute(int index, JsonNode expected, JsonNode actual, List<String> differences) {
        String prefix = "route[" + index + "]";
        compareText(prefix + ".characteristics", expected.get("characteristics"), actual.get("characteristics"), differences);
        compareText(prefix + ".name", expected.get("name"), actual.get("name"), differences);
        compareDescriptionList(prefix + ".description", expected.get("description"), actual.get("description"), differences);

        JsonNode expectedPositions = expected.get("positions");
        JsonNode actualPositions = actual.get("positions");
        if (expectedPositions.size() != actualPositions.size()) {
            differences.add(prefix + ".positions.size: expected " + expectedPositions.size() + " actual " + actualPositions.size());
            return;
        }
        for (int j = 0; j < expectedPositions.size(); j++)
            comparePosition(prefix + ".position[" + j + "]", expectedPositions.get(j), actualPositions.get(j), differences);
    }

    private static void comparePosition(String prefix, JsonNode expected, JsonNode actual, List<String> differences) {
        compareDouble(prefix + ".longitude", expected.get("longitude"), actual.get("longitude"), COORDINATE_TOLERANCE, differences);
        compareDouble(prefix + ".latitude", expected.get("latitude"), actual.get("latitude"), COORDINATE_TOLERANCE, differences);
        compareDouble(prefix + ".elevation", expected.get("elevation"), actual.get("elevation"), ELEVATION_TOLERANCE, differences);
        compareTime(prefix + ".time", expected.get("time"), actual.get("time"), differences);
        compareDouble(prefix + ".speed", expected.get("speed"), actual.get("speed"), SPEED_TOLERANCE, differences);
        compareDouble(prefix + ".heading", expected.get("heading"), actual.get("heading"), HEADING_TOLERANCE, differences);
        compareDouble(prefix + ".hdop", expected.get("hdop"), actual.get("hdop"), DOP_TOLERANCE, differences);
        compareDouble(prefix + ".vdop", expected.get("vdop"), actual.get("vdop"), DOP_TOLERANCE, differences);
        compareDouble(prefix + ".pdop", expected.get("pdop"), actual.get("pdop"), DOP_TOLERANCE, differences);
        compareInt(prefix + ".satellites", expected.get("satellites"), actual.get("satellites"), differences);
        compareText(prefix + ".description", expected.get("description"), actual.get("description"), differences);
    }

    private static String textOrNull(JsonNode node) {
        return node == null || node.isNull() ? null : node.asText();
    }

    private static void compareText(String path, JsonNode expectedNode, JsonNode actualNode, List<String> differences) {
        String expected = textOrNull(expectedNode);
        String actual = textOrNull(actualNode);
        String expectedTrimmed = expected != null ? expected.trim() : null;
        String actualTrimmed = actual != null ? actual.trim() : null;
        if (!Objects.equals(expectedTrimmed, actualTrimmed))
            differences.add(path + ": expected " + quote(expected) + " actual " + quote(actual));
    }

    private static String quote(String value) {
        return value == null ? "null" : "'" + value + "'";
    }

    private static List<String> toStringList(JsonNode node) {
        if (node == null || node.isNull())
            return null;
        List<String> result = new ArrayList<>();
        for (JsonNode element : node)
            result.add(element.asText());
        return result;
    }

    private static List<String> trimAll(List<String> strings) {
        if (strings == null)
            return null;
        List<String> result = new ArrayList<>(strings.size());
        for (String string : strings)
            result.add(string.trim());
        return result;
    }

    private static void compareDescriptionList(String path, JsonNode expectedNode, JsonNode actualNode, List<String> differences) {
        List<String> expected = toStringList(expectedNode);
        List<String> actual = toStringList(actualNode);
        if (!Objects.equals(trimAll(expected), trimAll(actual)))
            differences.add(path + ": expected " + expected + " actual " + actual);
    }

    private static Double toDouble(JsonNode node) {
        if (node == null || node.isNull())
            return null;
        return parseDouble(node.asText());
    }

    private static void compareDouble(String path, JsonNode expectedNode, JsonNode actualNode, double tolerance, List<String> differences) {
        Double expected = toDouble(expectedNode);
        Double actual = toDouble(actualNode);
        if (expected == null || actual == null) {
            if (!Objects.equals(expected, actual))
                differences.add(path + ": expected " + expected + " actual " + actual);
            return;
        }
        if (abs(expected - actual) > tolerance)
            differences.add(path + ": expected " + expected + " actual " + actual);
    }

    private static Integer toInt(JsonNode node) {
        return node == null || node.isNull() ? null : node.asInt();
    }

    private static void compareInt(String path, JsonNode expectedNode, JsonNode actualNode, List<String> differences) {
        Integer expected = toInt(expectedNode);
        Integer actual = toInt(actualNode);
        if (!Objects.equals(expected, actual))
            differences.add(path + ": expected " + expected + " actual " + actual);
    }

    private static void compareTime(String path, JsonNode expectedNode, JsonNode actualNode, List<String> differences) {
        String expectedText = textOrNull(expectedNode);
        String actualText = textOrNull(actualNode);
        if (expectedText == null || actualText == null) {
            if (!Objects.equals(expectedText, actualText))
                differences.add(path + ": expected " + expectedText + " actual " + actualText);
            return;
        }
        Instant expected = Instant.parse(expectedText);
        Instant actual = Instant.parse(actualText);
        if (expected.getEpochSecond() != actual.getEpochSecond())
            differences.add(path + ": expected " + expectedText + " actual " + actualText);
    }

    private static List<String> filterAllowlisted(List<String> differences, List<String> allowlist) {
        List<Pattern> allowedPathPatterns = parseAllowlist(allowlist);
        if (allowedPathPatterns.isEmpty())
            return differences;

        List<String> result = new ArrayList<>();
        for (String difference : differences) {
            String path = difference.substring(0, difference.indexOf(':'));
            if (!isAllowed(path, allowedPathPatterns))
                result.add(difference);
        }
        return result;
    }

    private static boolean isAllowed(String path, List<Pattern> allowedPathPatterns) {
        for (Pattern pattern : allowedPathPatterns)
            if (pattern.matcher(path).matches())
                return true;
        return false;
    }

    private static List<Pattern> parseAllowlist(List<String> allowlist) {
        List<Pattern> result = new ArrayList<>();
        if (allowlist == null)
            return result;

        for (String line : allowlist) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#"))
                continue;
            String[] parts = trimmed.split("\\s+", 4);
            if (parts.length < 2)
                continue;
            result.add(Pattern.compile(globToRegex(parts[1])));
        }
        return result;
    }

    private static String globToRegex(String glob) {
        StringBuilder regex = new StringBuilder();
        for (int i = 0; i < glob.length(); i++) {
            char character = glob.charAt(i);
            if (character == '*')
                regex.append(".*");
            else
                regex.append(Pattern.quote(String.valueOf(character)));
        }
        return regex.toString();
    }
}
