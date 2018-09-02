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
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.Math.max;
import static slash.common.io.Transfer.decodeUri;
import static slash.common.io.Transfer.formatDoubleAsString;
import static slash.common.io.Transfer.parseDouble;
import static slash.common.io.Transfer.trim;
import static slash.navigation.base.RouteCalculations.asWgs84Position;
import static slash.navigation.base.RouteCharacteristics.Route;

/**
 * Reads and writes Google Maps URLs from/to files.
 *
 * @author Christian Pesch
 */

public class GoogleMapsUrlFormat extends BaseUrlParsingFormat {
    private static final Logger log = Logger.getLogger(GoogleMapsUrlFormat.class.getName());
    private static final Preferences preferences = Preferences.userNodeForPackage(GoogleMapsUrlFormat.class);
    private static final Pattern URL_PATTERN = Pattern.compile(".*http[s]?://.+\\.google\\..+/maps([^\\s]+).*");
    private static final Pattern BOOKMARK_PATTERN = Pattern.compile(".*InternetShortcut(.+)IconFile.*");
    private static final Pattern PLAIN_POSITION_PATTERN = Pattern.compile("(\\s*[-|\\d|\\.]+\\s*),(\\s*[-|\\d|\\.]+\\s*)");
    private static final Pattern COMMENT_POSITION_PATTERN = Pattern.
            compile("(" + WHITE_SPACE + ".*?" + WHITE_SPACE + ")" +
                    "(@?(" + WHITE_SPACE + "[-|\\d|\\.]+" + WHITE_SPACE + ")," +
                    "(" + WHITE_SPACE + "[-|\\d|\\.]+" + WHITE_SPACE + "))?");
    private static final String DESTINATION_SEPARATOR = "to:";

    public String getExtension() {
        return ".url";
    }

    public String getName() {
        return "Google Maps URL (*" + getExtension() + ")";
    }

    public int getMaximumPositionCount() {
        return preferences.getInt("maximumGoogleMapsUrlPositionCount", 15);
    }

    public static boolean isGoogleMapsLinkUrl(URL url) {
        String found = internalFindUrl(url.toExternalForm());
        return found != null && (found.startsWith("?") || found.startsWith("/dir/"));
    }

    public static boolean isGoogleMapsProfileUrl(URL url) {
        String found = internalFindUrl(url.toExternalForm());
        return found != null && found.startsWith("/ms?");
    }

    private static String internalFindUrl(String text) {
        text = replaceLineFeeds(text, "&");
        Matcher bookmarkMatcher = BOOKMARK_PATTERN.matcher(text);
        if (bookmarkMatcher.matches())
            text = bookmarkMatcher.group(1);
        Matcher urlMatcher = URL_PATTERN.matcher(text);
        if (!urlMatcher.matches())
            return null;
        return trim(urlMatcher.group(1));
    }

    protected void processURL(String url, String encoding, ParserContext<Wgs84Route> context) {
        if (url.startsWith("/dir/")) {
            List<Wgs84Position> positions = parsePositions(url.substring(5));
            if (positions.size() > 0)
                context.appendRoute(createRoute(Route, null, positions));
        } else
            super.processURL(url, encoding, context);
    }

    List<Wgs84Position> parsePositions(String url) {
        List<Wgs84Position> result = new ArrayList<>();
        String[] segments = url.split("/");
        for (String segment : segments) {
            if (segment.startsWith("@") || segment.startsWith("data"))
                break;
            result.add(asWgs84Position(null, null, decodeUri(segment)));
        }
        return result;
    }

    protected String findURL(String text) {
        return internalFindUrl(text);
    }

    Wgs84Position parsePlainPosition(String coordinates) {
        Matcher matcher = PLAIN_POSITION_PATTERN.matcher(coordinates);
        if (!matcher.matches())
            throw new IllegalArgumentException("'" + coordinates + "' does not match");
        Double latitude = parseDouble(matcher.group(1));
        Double longitude = parseDouble(matcher.group(2));
        return asWgs84Position(longitude, latitude);
    }

    Wgs84Position parseCommentPosition(String position) {
        Matcher matcher = COMMENT_POSITION_PATTERN.matcher(position);
        if (!matcher.matches())
            throw new IllegalArgumentException("'" + position + "' does not match");
        String comment = trim(matcher.group(1));
        Double latitude = parseDouble(matcher.group(3));
        Double longitude = parseDouble(matcher.group(4));
        return asWgs84Position(longitude, latitude, comment);
    }

    List<Wgs84Position> parseDestinationPositions(String destinationComments) {
        List<Wgs84Position> result = new ArrayList<>();
        int startIndex = 0;
        while (startIndex < destinationComments.length()) {
            int endIndex = destinationComments.indexOf(DESTINATION_SEPARATOR, startIndex);
            if (endIndex == -1)
                endIndex = destinationComments.length();
            String position = destinationComments.substring(startIndex, endIndex);
            result.add(parseCommentPosition(position));
            startIndex = endIndex + 3 /* DESTINATION_SEPARATOR */;
        }
        return result;
    }

    List<Wgs84Position> extractGeocodePositions(List<Wgs84Position> positions) {
        List<Wgs84Position> result = new ArrayList<>(positions);
        for (int i = result.size() - 1; i >= 0; i--) {
            Wgs84Position position = result.get(i);
            if (trim(position.getDescription()) == null)
                result.remove(i);
        }
        return result;
    }

    protected List<Wgs84Position> parsePositions(Map<String, List<String>> parameters) {
        List<Wgs84Position> result = new ArrayList<>();
        if (parameters == null)
            return result;

        // ignore ll and sll parameters as they contain positions far off the route

        List<String> startPositions = parameters.get("saddr");
        if (startPositions != null) {
            for (String startComment : startPositions) {
                result.add(parseCommentPosition(startComment));
            }
        }

        List<String> destinationPositions = parameters.get("daddr");
        if(destinationPositions != null) {
            for (String destinationPosition : destinationPositions) {
                result.addAll(parseDestinationPositions(destinationPosition));
            }

            List<String> geocode = parameters.get("geocode");
            if(geocode != null && geocode.size() > 0 && result.size() > 0) {
                List<Wgs84Position> geocodePositions = extractGeocodePositions(result);
                StringTokenizer tokenizer = new StringTokenizer(geocode.get(0), ",;");
                int positionIndex = 0;
                while(tokenizer.hasMoreTokens() && positionIndex < geocodePositions.size()) {
                    tokenizer.nextToken();
                    if (tokenizer.hasMoreTokens()) {
                        try {
                            Double latitude = parseDouble(tokenizer.nextToken());
                            if (tokenizer.hasMoreTokens()) {
                                Double longitude = parseDouble(tokenizer.nextToken());
                                Wgs84Position position = geocodePositions.get(positionIndex++);
                                position.setLongitude(longitude);
                                position.setLatitude(latitude);
                            }
                        }
                        catch (NumberFormatException e) {
                           log.warning("Cannot parse tokens from " + geocode.get(0));
                        }
                    }
                }
            }
        }
        return result;
    }

    String createURL(List<Wgs84Position> positions, int startIndex, int endIndex) {
        StringBuilder buffer = new StringBuilder("http://maps.google.com/maps?ie=UTF8&");
        for (int i = startIndex; i < endIndex; i++) {
            Wgs84Position position = positions.get(i);
            String longitude = position.getLongitude() != null ? formatDoubleAsString(position.getLongitude(), 6) : null;
            String latitude = position.getLatitude() != null ? formatDoubleAsString(position.getLatitude(), 6) : null;
            String comment = encodeDescription(trim(position.getDescription()));
            if (i == startIndex) {
                buffer.append("saddr=").append(comment);
                if(longitude != null && latitude != null)
                    buffer.append("%40").append(latitude).append(",").append(longitude);
                if (endIndex > startIndex + 1)
                    buffer.append("&daddr=");
            } else {
                if (i > startIndex + 1 && i < endIndex)
                    buffer.append("+").append(DESTINATION_SEPARATOR);
                buffer.append(comment);
                if(longitude != null && latitude != null)
                    buffer.append("%40").append(latitude).append(",").append(longitude);
            }
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
