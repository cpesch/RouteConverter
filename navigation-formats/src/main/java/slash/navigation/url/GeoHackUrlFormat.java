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
import slash.navigation.base.Wgs84Position;
import slash.navigation.base.Wgs84Route;
import slash.navigation.common.Orientation;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.prefs.Preferences;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.Math.abs;
import static java.lang.Math.max;
import static slash.common.io.Transfer.*;
import static slash.navigation.base.RouteCalculations.asWgs84Position;
import static slash.navigation.common.Orientation.South;
import static slash.navigation.common.Orientation.West;

/**
 * Reads and writes GeoHack URLs from/to files.
 *
 * @author Christian Pesch
 */

public class GeoHackUrlFormat extends BaseUrlParsingFormat {
    private static final Preferences preferences = Preferences.userNodeForPackage(GeoHackUrlFormat.class);
    private static final Pattern URL_PATTERN = Pattern.compile(".*http[s]?://.+\\.org/geohack.*\\?([^\\s]+).*");
    private static final Pattern PARAM_PATTERN = Pattern.compile("^(" + POSITION + ")_([N|S])_(" + POSITION + ")_([W|E]).*");

    public String getExtension() {
        return ".url";
    }

    public String getName() {
        return "GeoHack URL (*" + getExtension() + ")";
    }

    public int getMaximumPositionCount() {
        return preferences.getInt("maximumGeoHackUrlPositionCount", 1);
    }

    protected List<Wgs84Position> parsePositions(Map<String, List<String>> parameters) {
        List<Wgs84Position> result = new ArrayList<>();
        if (parameters == null)
            return result;

        List<String> params = parameters.get("params");
        List<String> pagenames = parameters.get("pagename");
        for (int i = 0; i < params.size(); i++) {
            String param = params.get(i);
            String pagename = pagenames.size() >= i ? pagenames.get(i) : null;
            result.add(parsePosition(param, pagename));
        }
        return result;
    }

    private Double parseCoordinate(String coordinate, String orientation) {
        Double degrees = parseDouble(coordinate);
        Orientation o = Orientation.fromValue(trim(orientation));
        if(degrees == null || o == null)
            return null;
        boolean southOrWest = o.equals(South) || o.equals(West);
        return southOrWest ? -degrees : degrees;
    }

    private String parseDescription(String description) {
        description = decodeUri(description);
        if(description != null)
            description = description.replaceAll("_+", " ");
        return description;
    }

    Wgs84Position parsePosition(String params, String pagename) {
        Matcher matcher = PARAM_PATTERN.matcher(params);
        if (matcher.matches()) {
            Double latitude = parseCoordinate(matcher.group(1), matcher.group(2));
            Double longitude = parseCoordinate(matcher.group(3), matcher.group(4));
            String description = parseDescription(pagename);
            return asWgs84Position(longitude, latitude, description);
        }
        return null;
    }

   protected String findURL(String text) {
        text = replaceLineFeeds(text, "");
        Matcher urlMatcher = URL_PATTERN.matcher(text);
        if (!urlMatcher.matches())
            return null;
        return trim(urlMatcher.group(1));
    }

    private String formatDescription(String description) {
        if(description != null)
            description = description.replaceAll(" ", "_");
        return encodeUri(description);
    }

    String createURL(List<Wgs84Position> positions, int startIndex, int endIndex) {
        StringBuilder buffer = new StringBuilder("http://geohack.toolforge.org/geohack.php?");
        for (int i = startIndex; i < endIndex; i++) {
            Wgs84Position position = positions.get(i);
            buffer.append("params=");
            buffer.append(abs(position.getLatitude())).append("_").append(position.getLatitude() < 0 ? "S" : "N").append("_");
            buffer.append(abs(position.getLongitude())).append("_").append(position.getLongitude() < 0 ? "W" : "E").append("_");
            buffer.append("&pagename=").append(formatDescription(position.getDescription()));
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
