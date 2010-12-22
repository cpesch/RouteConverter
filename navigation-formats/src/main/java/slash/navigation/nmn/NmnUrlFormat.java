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

import slash.common.io.CompactCalendar;
import slash.common.io.Transfer;
import slash.navigation.base.*;

import java.io.*;
import java.net.URLDecoder;
import java.util.*;
import java.util.prefs.Preferences;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads and writes Navigon Mobile Navigator for iPhone/iPad URL from/to files.
 *
 * @author Christian Pesch
 */

public class NmnUrlFormat extends SimpleFormat<Wgs84Route> {
    private static final Preferences preferences = Preferences.userNodeForPackage(NmnUrlFormat.class);
    private static final Pattern URL_PATTERN = Pattern.compile(".*navigonDEU://route/\\?([^\\s|\"]+).*");
    private static final Pattern COORDINATE_PATTERN = Pattern.compile("coordinate//(" + POSITION + ")/(" + POSITION + ")");
    private static final Pattern ADDRESS_PATTERN = Pattern.compile("address//[^/]*/([^/]*)/([^/]*)/([^/]*)/([^/]*)/" +
            "(" + POSITION + ")/(" + POSITION + ")");
    private static final int READ_BUFFER_SIZE = 1024 * 1024;

    public String getExtension() {
        return ".url";
    }

    public String getName() {
        return "Navigon Mobile Navigator URL (*" + getExtension() + ")";
    }

    public int getMaximumPositionCount() {
        return preferences.getInt("maximumNavigonUrlPositionCount", 100); // TODO or 25?
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

    String findURL(String text) {
        text = text.replaceAll("[\n|\r]", "&");
        text = text.replaceAll("&amp;", "&");
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

    private byte convertHexDigit(byte b) {
        if ((b >= '0') && (b <= '9')) return (byte) (b - '0');
        if ((b >= 'a') && (b <= 'f')) return (byte) (b - 'a' + 10);
        if ((b >= 'A') && (b <= 'F')) return (byte) (b - 'A' + 10);
        return 0;
    }

    private void putMapEntry(Map<String, List<String>> map, String name, String value) {
        List<String> values = map.get(name);
        if (values == null) {
            values = new ArrayList<String>(1);
            map.put(name, values);
        }
        values.add(value);
    }

    private Map<String, List<String>> parseParameters(byte[] data, String encoding) throws UnsupportedEncodingException {
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
                case '&':
                    value = new String(data, 0, ox, encoding);
                    if (key != null) {
                        putMapEntry(result, key, value);
                        key = null;
                    }
                    ox = 0;
                    break;
                case '=':
                    if (key == null) {
                        key = new String(data, 0, ox, encoding);
                        ox = 0;
                    } else {
                        data[ox++] = c;
                    }
                    break;
                case '+':
                    data[ox++] = (byte) ' ';
                    break;
                case '%':
                    int leftNibble = convertHexDigit(data[ix++]) << 4;
                    byte rightNibble = ix < data.length ? convertHexDigit(data[ix++]) : 0;
                    data[ox++] = (byte) (leftNibble + rightNibble);
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

    private String decodeComment(String string) {
        if (string == null)
            return "";
        try {
            return URLDecoder.decode(string, UTF8_ENCODING);
        } catch (UnsupportedEncodingException e) {
            return string;
        }
    }

    Wgs84Position parsePosition(String position) {
        Matcher addressPattern = ADDRESS_PATTERN.matcher(position);
        if (addressPattern.matches()) {
            String zip = Transfer.trim(addressPattern.group(1));
            String city = Transfer.trim(addressPattern.group(2));
            String street = Transfer.trim(addressPattern.group(3));
            String houseNumber = Transfer.trim(addressPattern.group(4));
            String comment = Transfer.toMixedCase(decodeComment((zip != null ? zip + " " : "") +
                (city != null ? city : "") +
                (street != null ? ", " + street : "") +
                (houseNumber != null ? " " + houseNumber : "")));
            Double longitude = Transfer.parseDouble(addressPattern.group(5));
            Double latitude = Transfer.parseDouble(addressPattern.group(6));
            return new Wgs84Position(longitude, latitude, null, null, null, Transfer.trim(comment));
        }
        Matcher coordinatesMatcher = COORDINATE_PATTERN.matcher(position);
        if (coordinatesMatcher.matches()) {
            Double longitude = Transfer.parseDouble(coordinatesMatcher.group(1));
            Double latitude = Transfer.parseDouble(coordinatesMatcher.group(2));
            return new Wgs84Position(longitude, latitude, null, null, null, null);
        }
        throw new IllegalArgumentException("'" + position + "' does not match");
    }

    List<Wgs84Position> parsePositions(Map<String, List<String>> parameters) {
        List<Wgs84Position> result = new ArrayList<Wgs84Position>();
        if (parameters == null)
            return result;

        List<String> targets = parameters.get("target");
        for (String target : targets) {
            result.add(parsePosition(target));
        }
        return result;
    }

    String createURL(List<Wgs84Position> positions, int startIndex, int endIndex) {
        StringBuffer buffer = new StringBuffer("<a href=\"navigonDEU://route/?");
        for (int i = startIndex; i < endIndex; i++) {
            Wgs84Position position = positions.get(i);
            String longitude = Transfer.formatDoubleAsString(position.getLongitude(), 6);
            String latitude = Transfer.formatDoubleAsString(position.getLatitude(), 6);
            if (i > startIndex)
                buffer.append("&amp;");
            buffer.append("target=coordinate//").append(longitude).append("/").append(latitude);
        }
        buffer.append("\">");
        for (int i = startIndex; i < endIndex; i++) {
            Wgs84Position position = positions.get(i);
            String comment = Transfer.trim(position.getComment());
            if (i > startIndex)
                buffer.append(" -> ");
            buffer.append(comment);
        }
        buffer.append("</a>");
        return buffer.toString();
    }

    public void write(Wgs84Route route, PrintWriter writer, int startIndex, int endIndex) {
        List<Wgs84Position> positions = route.getPositions();
        // idea from forum: add start point from previous route section since your not at the
        // last position of the previous segment heading for the first position of the next segment
        startIndex = Math.max(startIndex - 1, 0);
        writer.println(createURL(positions, startIndex, endIndex));
        writer.println();
    }
}
