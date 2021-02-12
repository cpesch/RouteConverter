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

package slash.navigation.url;

import slash.navigation.base.BaseUrlParsingFormat;
import slash.navigation.base.ParserContext;
import slash.navigation.base.Wgs84Position;
import slash.navigation.base.Wgs84Route;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.prefs.Preferences;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.Math.max;
import static slash.common.io.Transfer.*;
import static slash.navigation.base.RouteCalculations.asWgs84Position;
import static slash.navigation.base.RouteCharacteristics.Route;

/**
 * Reads and writes Kurviger URLs from/to files.
 *
 * @author Christian Pesch
 */

public class KurvigerUrlFormat extends BaseUrlParsingFormat {
    private static final Preferences preferences = Preferences.userNodeForPackage(KurvigerUrlFormat.class);
    private static final Pattern URL_PATTERN = Pattern.compile(".*http[s]?://kurviger.de/\\?([^\\s]+).*");

    public String getExtension() {
        return ".url";
    }

    public String getName() {
        return "Kurviger URL (*" + getExtension() + ")";
    }

    public int getMaximumPositionCount() {
        return preferences.getInt("maximumKurvigerUrlPositionCount", 100);
    }

    protected List<Wgs84Position> parsePositions(Map<String, List<String>> parameters) {
        throw new UnsupportedOperationException();
    }

    private Wgs84Position parsePosition(String data) {
        StringTokenizer tokenizer = new StringTokenizer(data, ",");
        if (tokenizer.countTokens() != 2)
            return null;

        String latitude = tokenizer.nextToken();
        String longitude = tokenizer.nextToken();
        return asWgs84Position(parseDouble(longitude), parseDouble(latitude));
    }

    List<Wgs84Position> parsePositions(String data) {
        List<Wgs84Position> result = new ArrayList<>();
        StringTokenizer tokenizer = new StringTokenizer(data, "&");
        while (tokenizer.hasMoreTokens()) {
            String token = tokenizer.nextToken();
            if (token.startsWith("point="))
                token = token.substring(6);
            Wgs84Position position = parsePosition(token);
            if (position != null)
                result.add(position);
        }
        return result;
    }

    private List<Wgs84Position> parseMatrixParameters(String data) {
        if (data == null || data.length() == 0)
            return null;
        return parsePositions(data);
    }

    protected void processURL(String url, String encoding, ParserContext<Wgs84Route> context) {
        List<Wgs84Position> positions = parseMatrixParameters(url);
        if (positions.size() > 0)
            context.appendRoute(createRoute(Route, null, positions));
    }

    protected String findURL(String text) {
        text = replaceLineFeeds(text, "");
        Matcher urlMatcher = URL_PATTERN.matcher(text);
        if (!urlMatcher.matches())
            return null;
        return trim(urlMatcher.group(1));
    }

    String createURL(List<Wgs84Position> positions, int startIndex, int endIndex) {
        StringBuilder buffer = new StringBuilder("https://kurviger.de/?");
        for (int i = startIndex; i < endIndex; i++) {
            Wgs84Position position = positions.get(i);
            String longitude = position.getLongitude() != null ? formatDoubleAsString(position.getLongitude(), 6) : null;
            String latitude = position.getLatitude() != null ? formatDoubleAsString(position.getLatitude(), 6) : null;
            if (longitude != null && latitude != null)
                buffer.append("point=").append(latitude).append(",").append(longitude);
            if (i < endIndex - 1)
                buffer.append("&");
        }
        return buffer.toString();
    }

    public void write(Wgs84Route route, PrintWriter writer, int startIndex, int endIndex) {
        List<Wgs84Position> positions = route.getPositions();
        writer.println("[InternetShortcut]");
        // idea from forum: add start point from previous route section since your not at the
        // last position of the previous segment heading for the first position of the next segment
        startIndex = max(startIndex - 1, 0);
        writer.println("URL=" + createURL(positions, startIndex, endIndex));
        writer.println();
    }
}
