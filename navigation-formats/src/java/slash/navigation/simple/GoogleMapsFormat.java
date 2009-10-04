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

package slash.navigation.simple;

import slash.navigation.*;
import slash.navigation.util.CompactCalendar;
import slash.navigation.util.Transfer;

import java.io.*;
import java.net.URL;
import java.net.URLEncoder;
import java.util.*;
import java.util.prefs.Preferences;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads and writes Google Maps URLs from/to files.
 *
 * @author Christian Pesch
 */

public class GoogleMapsFormat extends SimpleFormat<Wgs84Route> {
    private static final Logger log = Logger.getLogger(GoogleMapsFormat.class.getName());
    private static final Preferences preferences = Preferences.userNodeForPackage(GoogleMapsFormat.class);
    private static final Pattern URL_PATTERN = Pattern.compile(".*http://maps\\.google\\..+/maps\\?([^\\s]+).*");
    private static final Pattern BOOKMARK_PATTERN = Pattern.compile(".*InternetShortcut(.+)IconFile.*");
    private static final Pattern START_PATTERN = Pattern.compile("(\\s*[-|\\d|\\.]+\\s*),(\\s*[-|\\d|\\.]+\\s*)");
    private static final Pattern COMMENT_POSITION_PATTERN = Pattern.
            compile("(" + WHITE_SPACE + ".*?" + WHITE_SPACE + ")" +
                    "(@?(" + WHITE_SPACE + "[-|\\d|\\.]+" + WHITE_SPACE + ")," +
                    "(" + WHITE_SPACE + "[-|\\d|\\.]+" + WHITE_SPACE + "))?");
    private static final String DESTINATION_SEPARATOR = "to:";
    private static final int READ_BUFFER_SIZE = 1024 * 1024;

    public String getExtension() {
        return ".url";
    }

    public String getName() {
        return "Google Maps URL (*" + getExtension() + ")";
    }

    public int getMaximumPositionCount() {
        return preferences.getInt("maximumGoogleMapsPositionCount", 15);
    }

    @SuppressWarnings({"unchecked"})
    public <P extends BaseNavigationPosition> Wgs84Route createRoute(RouteCharacteristics characteristics, String name, List<P> positions) {
        return new Wgs84Route(this, characteristics, (List<Wgs84Position>) positions);
    }

    public List<Wgs84Route> read(InputStream source, CompactCalendar startDate) throws IOException {
        // used to be a UTF-8 then ISO-8859-1 fallback style
        return read(source, startDate, UTF8_ENCODING);
    }

    public List<Wgs84Route> read(BufferedReader reader, CompactCalendar startDate, String encoding) throws IOException {
        StringBuffer buffer = new StringBuffer();

        while (buffer.length() < READ_BUFFER_SIZE) {
            String line = reader.readLine();
            if (line == null)
                break;
            buffer.append(line).append("\n");
        }

        String url = findURL(buffer.toString());
        if (url == null)
            return null;

        Map<String, List<String>> parameters = parseURLParameters(url, encoding);
        if (parameters == null)
            return null;

        List<Wgs84Position> positions = parsePositions(parameters);
        if (positions.size() > 0)
            return Arrays.asList(new Wgs84Route(this, RouteCharacteristics.Route, positions));
        else
            return null;
    }

    public static boolean isGoogleMapsUrl(URL url) {
        return findURL(url.toExternalForm()) != null;
    }

    static String findURL(String text) {
        text = text.replaceAll("[\n|\r]", "");
        Matcher bookmarkMatcher = BOOKMARK_PATTERN.matcher(text);
        if (bookmarkMatcher.matches())
            text = bookmarkMatcher.group(1);
        Matcher urlMatcher = URL_PATTERN.matcher(text);
        if (!urlMatcher.matches())
            return null;
        return Transfer.trim(urlMatcher.group(1));
    }

    Map<String, List<String>> parseURLParameters(String data, String encoding) {
        if (data == null || data.length() == 0)
            return null;

        try {
            byte[] bytes = data.getBytes(encoding);
            return parseParameters(bytes, encoding);
        } catch (UnsupportedEncodingException e) {
            return null;
        }
    }

    private static byte convertHexDigit(byte b) {
        if ((b >= '0') && (b <= '9')) return (byte) (b - '0');
        if ((b >= 'a') && (b <= 'f')) return (byte) (b - 'a' + 10);
        if ((b >= 'A') && (b <= 'F')) return (byte) (b - 'A' + 10);
        return 0;
    }

    private static void putMapEntry(Map<String, List<String>> map, String name, String value) {
        List<String> values = map.get(name);
        if (values == null) {
            values = new ArrayList<String>(1);
            map.put(name, values);
        }
        values.add(value);
    }

    private static Map<String, List<String>> parseParameters(byte[] data, String encoding) throws UnsupportedEncodingException {
        if (data == null || data.length == 0)
            return null;

        Map<String, List<String>> result = new HashMap<String, List<String>>();
        int ix = 0;
        int ox = 0;
        String key = null;
        String value;
        while (ix < data.length) {
            byte c = data[ix++];
            switch ((char) c) {
                case'&':
                    value = new String(data, 0, ox, encoding);
                    if (key != null) {
                        putMapEntry(result, key, value);
                        key = null;
                    }
                    ox = 0;
                    break;
                case'=':
                    if (key == null) {
                        key = new String(data, 0, ox, encoding);
                        ox = 0;
                    } else {
                        data[ox++] = c;
                    }
                    break;
                case'+':
                    data[ox++] = (byte) ' ';
                    break;
                case'%':
                    data[ox++] = (byte) ((convertHexDigit(data[ix++]) << 4) + convertHexDigit(data[ix++]));
                    break;
                default:
                    data[ox++] = c;
            }
        }
        // The last value does not end in '&'.  So save it now.
        if (key != null) {
            value = new String(data, 0, ox, encoding);
            putMapEntry(result, key, value);
        }
        return result;
    }

    Wgs84Position parseStartPosition(String coordinates, String comment) {
        Matcher matcher = START_PATTERN.matcher(coordinates);
        if (!matcher.matches())
            throw new IllegalArgumentException("'" + coordinates + "' does not match");
        Double latitude = Transfer.parseDouble(matcher.group(1));
        Double longitude = Transfer.parseDouble(matcher.group(2));

        Wgs84Position position = comment != null ? parsePosition(comment) : null;
        if (position != null && position.hasCoordinates()) {
            return position;
        } else {
            return new Wgs84Position(longitude, latitude, null, null, null, Transfer.trim(comment));
        }
    }

    Wgs84Position parsePosition(String position) {
        Matcher matcher = COMMENT_POSITION_PATTERN.matcher(position);
        if (!matcher.matches())
            throw new IllegalArgumentException("'" + position + "' does not match");
        String comment = matcher.group(1);
        String latitude = matcher.group(3);
        String longitude = matcher.group(4);
        return new Wgs84Position(Transfer.parseDouble(longitude), Transfer.parseDouble(latitude),
                null, null, null, Transfer.trim(comment));
    }

    List<Wgs84Position> parseDestinationPositions(String destinationComments) {
        List<Wgs84Position> result = new ArrayList<Wgs84Position>();
        int startIndex = 0;
        while (startIndex < destinationComments.length()) {
            int endIndex = destinationComments.indexOf(DESTINATION_SEPARATOR, startIndex);
            if (endIndex == -1)
                endIndex = destinationComments.length();
            String position = destinationComments.substring(startIndex, endIndex);
            result.add(parsePosition(position));
            startIndex = endIndex + 3 /* DESTINATION_SEPARATOR */;
        }
        return result;
    }

    List<Wgs84Position> extractGeocodePositions(List<Wgs84Position> positions) {
        List<Wgs84Position> result = new ArrayList<Wgs84Position>(positions);
        for (int i = result.size() - 1; i >= 0; i--) {
            Wgs84Position position = result.get(i);
            if (Transfer.trim(position.getComment()) == null)
                result.remove(i);
        }
        return result;
    }

    List<Wgs84Position> parsePositions(Map<String, List<String>> parameters) {
        List<Wgs84Position> result = new ArrayList<Wgs84Position>();
        if (parameters == null)
            return result;

        List<String> startPositions = parameters.get("ll");
        if (startPositions == null)
            startPositions = parameters.get("sll");
        List<String> startComments = parameters.get("saddr");

        if(startPositions != null) {
            for (int i = 0; i < startPositions.size(); i++) {
                String startPosition = startPositions.get(i);
                String startComment = startComments != null ? startComments.get(i) : null;
                result.add(parseStartPosition(startPosition, startComment));
            }
        } else if (startComments != null) {
            for (String startComment : startComments) {
                result.add(parsePosition(startComment));
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
                            Double latitude = Transfer.parseDouble(tokenizer.nextToken());
                            if (tokenizer.hasMoreTokens()) {
                                Double longitude = Transfer.parseDouble(tokenizer.nextToken());
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
        StringBuffer buffer = new StringBuffer("http://maps.google.com/maps?ie=UTF8&");
        for (int i = startIndex; i < endIndex; i++) {
            Wgs84Position position = positions.get(i);
            String longitude = Transfer.formatDoubleAsString(position.getLongitude(), 6);
            String latitude = Transfer.formatDoubleAsString(position.getLatitude(), 6);
            String comment = encodeComment(Transfer.trim(position.getComment()));
            if (i == startIndex) {
                buffer.append("saddr=").append(comment).append("%40").append(latitude).append(",").append(longitude);
                if (endIndex > startIndex + 1)
                    buffer.append("&daddr=");
            } else {
                if (i > startIndex + 1 && i < endIndex)
                    buffer.append("+").append(DESTINATION_SEPARATOR);
                buffer.append(comment).append("%40").append(latitude).append(",").append(longitude);
            }
        }
        return buffer.toString();
    }

    public void write(Wgs84Route route, PrintWriter writer, int startIndex, int endIndex) {
        List<Wgs84Position> positions = route.getPositions();
        writer.println("[InternetShortcut]");
        // idea from forum: add start point from previous route section since your not at the
        // last position of the previous segment heading for the first position of the next segment
        startIndex = Math.max(startIndex - 1, 0);
        writer.println("URL=" + createURL(positions, startIndex, endIndex));
        writer.println();
    }

    private static String encodeComment(String string) {
        if (string == null)
            return "";
        try {
            string = URLEncoder.encode(string, UTF8_ENCODING);
            string = string.replace("%2C", ",");
            return string;
        } catch (UnsupportedEncodingException e) {
            return string;
        }
    }
}
