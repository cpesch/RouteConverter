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
package slash.navigation.csv;

import slash.navigation.base.ParserContext;

import java.io.File;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static slash.common.io.Transfer.formatDoubleAsString;
import static slash.common.io.Transfer.trim;
import static slash.navigation.csv.ColumnType.Description;
import static slash.navigation.csv.ColumnType.Latitude;
import static slash.navigation.csv.ColumnType.Longitude;

/**
 * Reads the Saved-list CSVs (<code>Starred places.csv</code>, <code>Want to go.csv</code>,
 * <code>Favorites.csv</code> and custom lists) from a <a href="https://takeout.google.com/">Google Takeout</a>
 * export of Google Maps saved places.
 *
 * @author Christian Pesch
 */

public class GoogleTakeoutSavedListFormat extends CsvFormat {
    private static final String BOM = "﻿";
    private static final String TITLE_COLUMN = "Title";
    private static final String NOTE_COLUMN = "Note";
    private static final String URL_COLUMN = "URL";

    // the exact place pin, e.g. .../data=!4m2!3m1!1s0x...!3d53.5493424!4d9.973844
    private static final Pattern PLACE_PIN_PATTERN =
            Pattern.compile("!3d(-?\\d+(?:\\.\\d+)?)!4d(-?\\d+(?:\\.\\d+)?)");
    // a dropped pin, e.g. https://www.google.com/maps/search/52.516275,13.377704
    private static final Pattern DROPPED_PIN_PATTERN =
            Pattern.compile("/maps/search/\\s*(-?\\d+(?:\\.\\d+)?)\\s*,\\s*(-?\\d+(?:\\.\\d+)?)");
    // a q= or query= URL parameter, e.g. ?q=48.8566,2.3522 or ?api=1&query=48.8566,2.3522
    private static final Pattern QUERY_PARAMETER_PATTERN =
            Pattern.compile("[?&](?:q|query)=\\s*(-?\\d+(?:\\.\\d+)?)\\s*,\\s*(-?\\d+(?:\\.\\d+)?)");

    public String getName() {
        return "Google Takeout Saved List (" + getExtension() + ")";
    }

    protected char getColumnSeparator() {
        return ',';
    }

    public boolean isSupportsWriting() {
        return false;
    }

    protected String getRouteName(ParserContext<CsvRoute> context) {
        File file = context.getFile();
        if (file == null)
            return null;

        String name = file.getName();
        if (name.toLowerCase(Locale.ROOT).endsWith(".csv"))
            name = name.substring(0, name.length() - 4);
        return name;
    }

    protected LinkedHashMap<String, String> transformRead(LinkedHashMap<String, String> rowAsMap) {
        LinkedHashMap<String, String> result = new LinkedHashMap<>();

        String titleKey = findKey(rowAsMap, TITLE_COLUMN);
        String urlKey = findKey(rowAsMap, URL_COLUMN);
        if (titleKey == null || urlKey == null)
            return result;

        String title = trim(rowAsMap.get(titleKey));
        String note = trim(rowAsMap.get(NOTE_COLUMN));
        String url = trim(rowAsMap.get(urlKey));

        Double[] coordinates = parseCoordinates(url);
        if (title == null && coordinates == null)
            return result;

        if (coordinates != null) {
            result.put(Latitude.name(), formatDoubleAsString(coordinates[0]));
            result.put(Longitude.name(), formatDoubleAsString(coordinates[1]));
        }

        String description = coordinates != null ? asDescription(title, note) : title;
        if (description != null)
            result.put(Description.name(), description);

        return result;
    }

    private static String findKey(Map<String, String> rowAsMap, String name) {
        if (rowAsMap.containsKey(name))
            return name;
        String bomName = BOM + name;
        if (rowAsMap.containsKey(bomName))
            return bomName;
        return null;
    }

    static Double[] parseCoordinates(String url) {
        if (url == null || url.isEmpty())
            return null;

        String decoded;
        try {
            decoded = URLDecoder.decode(url, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            decoded = url;
        }

        Double[] coordinates = tryPattern(PLACE_PIN_PATTERN, decoded);
        if (coordinates == null)
            coordinates = tryPattern(DROPPED_PIN_PATTERN, decoded);
        if (coordinates == null)
            coordinates = tryPattern(QUERY_PARAMETER_PATTERN, decoded);
        return coordinates;
    }

    private static Double[] tryPattern(Pattern pattern, String url) {
        Matcher matcher = pattern.matcher(url);
        if (!matcher.find())
            return null;

        try {
            double latitude = Double.parseDouble(matcher.group(1));
            double longitude = Double.parseDouble(matcher.group(2));
            if (latitude < -90.0 || latitude > 90.0 || longitude < -180.0 || longitude > 180.0)
                return null;
            return new Double[]{latitude, longitude};
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
