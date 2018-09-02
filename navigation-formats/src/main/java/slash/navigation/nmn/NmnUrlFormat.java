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

package slash.navigation.nmn;

import slash.navigation.base.BaseUrlParsingFormat;
import slash.navigation.base.Wgs84Position;
import slash.navigation.base.Wgs84Route;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.prefs.Preferences;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static slash.common.io.Transfer.formatDoubleAsString;
import static slash.common.io.Transfer.parseDouble;
import static slash.common.io.Transfer.toMixedCase;
import static slash.common.io.Transfer.trim;
import static slash.navigation.base.RouteCalculations.asWgs84Position;

/**
 * Reads and writes Navigon Mobile Navigator for iPhone/iPad URL from/to files.
 *
 * @author Christian Pesch
 */

public class NmnUrlFormat extends BaseUrlParsingFormat {
    private static final Preferences preferences = Preferences.userNodeForPackage(NmnUrlFormat.class);
    private static final Pattern URL_PATTERN = Pattern.compile(".*navigon.*://route/\\?([^\\s|\"]+).*");
    private static final Pattern COORDINATE_PATTERN = Pattern.compile("coordinate//(" + POSITION + ")/(" + POSITION + ")");
    private static final Pattern ADDRESS_PATTERN = Pattern.compile("address//[^/]*/([^/]*)/([^/]*)/([^/]*)/([^/]*)/" +
            "(" + POSITION + ")/(" + POSITION + ").*");

    public String getExtension() {
        return ".txt";
    }

    public String getName() {
        return "Navigon Mobile Navigator URL (*" + getExtension() + ")";
    }

    public int getMaximumPositionCount() {
        return preferences.getInt("maximumNavigonUrlPositionCount", 25);
    }

    protected String findURL(String text) {
        text = replaceLineFeeds(text, "&");
        Matcher urlMatcher = URL_PATTERN.matcher(text);
        if (!urlMatcher.matches())
            return null;
        return trim(urlMatcher.group(1));
    }

    Wgs84Position parsePosition(String position) {
        Matcher addressPattern = ADDRESS_PATTERN.matcher(position);
        if (addressPattern.matches()) {
            String zip = trim(addressPattern.group(1));
            String city = trim(addressPattern.group(2));
            String street = trim(addressPattern.group(3));
            String houseNumber = trim(addressPattern.group(4));
            String description = toMixedCase(decodeDescription((zip != null ? zip + " " : "") +
                    (city != null ? city : "") +
                    (street != null ? ", " + street : "") +
                    (houseNumber != null ? " " + houseNumber : "")));
            Double longitude = parseDouble(addressPattern.group(5));
            Double latitude = parseDouble(addressPattern.group(6));
            return asWgs84Position(longitude, latitude, trim(description));
        }
        Matcher coordinatesMatcher = COORDINATE_PATTERN.matcher(position);
        if (coordinatesMatcher.matches()) {
            Double longitude = parseDouble(coordinatesMatcher.group(1));
            Double latitude = parseDouble(coordinatesMatcher.group(2));
            return asWgs84Position(longitude, latitude);
        }
        throw new IllegalArgumentException("'" + position + "' does not match");
    }

    protected List<Wgs84Position> parsePositions(Map<String, List<String>> parameters) {
        List<Wgs84Position> result = new ArrayList<>();
        if (parameters == null)
            return result;

        List<String> targets = parameters.get("target");
        for (String target : targets) {
            result.add(parsePosition(target));
        }
        return result;
    }

    String createURL(List<Wgs84Position> positions, int startIndex, int endIndex) {
        StringBuilder buffer = new StringBuilder();
        buffer.append("navigon");
        String mapName = trim(preferences.get("navigonUrlMapName", null));
        if (mapName != null)
            buffer.append(mapName);
        buffer.append("://route/?");
        for (int i = startIndex; i < endIndex; i++) {
            Wgs84Position position = positions.get(i);
            String longitude = formatDoubleAsString(position.getLongitude(), 6);
            String latitude = formatDoubleAsString(position.getLatitude(), 6);
            if (i > startIndex)
                buffer.append("&");
            buffer.append("target=coordinate//").append(longitude).append("/").append(latitude);
        }
        return buffer.toString();
    }

    public void write(Wgs84Route route, PrintWriter writer, int startIndex, int endIndex) {
        List<Wgs84Position> positions = route.getPositions();
        // idea from forum: add start point from previous route section since your not at the
        // last position of the previous segment heading for the first position of the next segment
        // startIndex = max(startIndex - 1, 0);
        writer.println(createURL(positions, startIndex, endIndex));
        writer.println();
    }
}
