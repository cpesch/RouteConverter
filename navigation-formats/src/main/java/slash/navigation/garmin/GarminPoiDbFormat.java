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

    Translated to Java from GPSBabel style/garmin_poi.style (master 9c1e336) for RouteConverter, 2026; modified.
    Copyright (C) 2005 Robert Lipe (style/garmin_poi.style, "Author: Robert Lipe, Date: 10/07/2005")
*/

package slash.navigation.garmin;

import slash.navigation.base.GpxBackedNavigationFormat;
import slash.navigation.base.RouteCharacteristics;
import slash.navigation.gpx.Gpx10Format;
import slash.navigation.gpx.GpxPosition;
import slash.navigation.gpx.GpxRoute;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.singletonList;
import static slash.common.io.Transfer.isEmpty;
import static slash.navigation.base.RouteCharacteristics.Waypoints;
import static slash.navigation.common.NavigationConversion.formatPosition;
import static slash.navigation.gpsbabel.GpsBabelText.cleanField;
import static slash.navigation.gpsbabel.GpsBabelText.csvLineParse;
import static slash.navigation.gpsbabel.GpsBabelText.decodeUtf8OrCp1252;
import static slash.navigation.gpsbabel.GpsBabelText.humanToDec;

/**
 * Reads and writes Garmin POI Database (.xcsv) files, the CSV input of Garmin POI Loader:
 * <code>longitude,latitude,shortname,description</code>, one waypoint per line.
 * <p>
 * Reading is more lenient than GPSBabel: fields may be enclosed in double quotes (decision D25), lines may end with
 * CR, LF or CR LF, a byte order mark, a header line and comment lines starting with {@code #} or {@code ;} are
 * skipped, and text that is not UTF-8 is read as Windows-1252 (decision D22). Every other line has to be a point of
 * interest, else the file is some other text.
 *
 * @author Christian Pesch
 */

public class GarminPoiDbFormat extends GpxBackedNavigationFormat {
    // style/garmin_poi.style: FIELD_DELIMITER COMMA, BADCHARS COMMA, SHORTLEN 24, OFIELD DESCRIPTION "%-.50s"
    private static final String DELIMITER = ",";
    private static final char BAD_CHARACTER = ',';
    private static final int SHORT_LENGTH = 24;
    private static final int DESCRIPTION_LENGTH = 50;
    private static final int FIELD_COUNT = 3;
    private static final String RECORD_DELIMITER = "\n";
    private static final Pattern LINE_BREAK = Pattern.compile("\r\n|\r|\n");
    private static final String ENCLOSURE = "\"";
    private static final Set<String> HEADER_FIELDS = Set.of("longitude", "latitude", "lon", "long", "lng", "lat",
            "x", "y", "breite", "länge", "laenge");
    // GPSBabel 1.5.4 writes the characters as UTF-8
    private static final Charset CHARSET = UTF_8;

    public String getExtension() {
        return ".xcsv";
    }

    public String getName() {
        return "Garmin POI Database (*" + getExtension() + ")";
    }

    public boolean isSupportsMultipleRoutes() {
        return false;
    }

    protected List<RouteCharacteristics> getWriteCharacteristics() {
        return singletonList(Waypoints);
    }

    protected boolean isValidRoute(GpxRoute route) {
        // is really greedy in parsing the data of various text files
        List<GpxPosition> positions = route.getPositions();
        int count = 0;
        for (GpxPosition position : positions) {
            if ((isEmpty(position.getLongitude()) && isEmpty(position.getLatitude())) ||
                    (isEmpty(position.getLongitude()) && isEmpty(position.getElevation())) ||
                    (isEmpty(position.getLatitude()) && isEmpty(position.getElevation())))
                count++;
        }
        return count != positions.size();
    }

    // read

    private static byte[] readFully(InputStream source) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] bytes = new byte[8192];
        int count;
        while ((count = source.read(bytes)) != -1)
            buffer.write(bytes, 0, count);
        return buffer.toByteArray();
    }

    private static boolean isHumanReadableCoordinate(String field) {
        boolean digit = false;
        for (int i = 0; i < field.length(); i++) {
            char c = field.charAt(i);
            if (c >= '0' && c <= '9')
                digit = true;
            else if (!(c == '.' || c == ' ' || c == '\t' || c == '+' || c == '-' || c == '\'' || c == '"' ||
                    c == '°' || c == '′' || c == '″' ||
                    "NSEWnsew".indexOf(c) != -1))
                return false;
        }
        return digit;
    }

    private static String trim(String field) {
        int start = 0, end = field.length();
        while (start < end && field.charAt(start) <= ' ')
            start++;
        while (end > start && field.charAt(end - 1) <= ' ')
            end--;
        return field.substring(start, end);
    }

    private GpxPosition parseLine(String line) {
        // a double quote that is no enclosure, like the seconds sign of 41 9 5.6", leaves the enclosures unbalanced
        // or the fields unusable: then the line is read without enclosures, as GPSBabel does
        boolean balanced = line.chars().filter(c -> c == '"').count() % 2 == 0;
        GpxPosition position = balanced ? parseFields(csvLineParse(line, DELIMITER, ENCLOSURE), true) : null;
        return position != null ? position : parseFields(csvLineParse(line, DELIMITER, null), false);
    }

    private static String unescape(String field, boolean enclosed) {
        return enclosed ? field.replace(ENCLOSURE + ENCLOSURE, ENCLOSURE) : field;
    }

    private GpxPosition parseFields(List<String> fields, boolean enclosed) {
        if (fields.size() < FIELD_COUNT)
            return null;

        String longitudeField = trim(fields.get(0));
        String latitudeField = trim(fields.get(1));
        if (!isHumanReadableCoordinate(longitudeField) || !isHumanReadableCoordinate(latitudeField))
            return null;

        // LON_HUMAN_READABLE, then LAT_HUMAN_READABLE: later values win
        double latitude = Double.NaN, longitude = Double.NaN;
        double[] first = humanToDec(longitudeField, 2);
        if (!Double.isNaN(first[0]))
            latitude = first[0];
        if (!Double.isNaN(first[1]))
            longitude = first[1];
        double[] second = humanToDec(latitudeField, 1);
        if (!Double.isNaN(second[0]))
            latitude = second[0];
        if (!Double.isNaN(second[1]))
            longitude = second[1];

        if (Double.isNaN(latitude) || Double.isNaN(longitude) ||
                latitude < -90.0 || latitude > 90.0 || longitude < -180.0 || longitude > 180.0)
            return null;

        String name = stripIllegalXmlCharacters(trim(unescape(fields.get(2), enclosed)));
        String description = stripIllegalXmlCharacters(trim(unescape(fields.size() > 3 ? fields.get(3) : "", enclosed)));
        String text = asWayPointDescription(name.isEmpty() ? null : name, description.isEmpty() ? null : description);
        return new GpxPosition(longitude, latitude, null, null, null, text);
    }

    // GPSBabel delivered these points as GPX <wpt>, which Gpx10Format reads with asWayPointDescription()
    private String asWayPointDescription(String name, String description) {
        return asDescription(name, description);
    }

    private static boolean isComment(String line) {
        return line.startsWith("#") || line.startsWith(";");
    }

    private static boolean isHeader(String line) {
        List<String> fields = csvLineParse(line, DELIMITER, ENCLOSURE);
        return fields.size() >= FIELD_COUNT &&
                HEADER_FIELDS.contains(trim(fields.get(0)).toLowerCase(Locale.ROOT)) &&
                HEADER_FIELDS.contains(trim(fields.get(1)).toLowerCase(Locale.ROOT));
    }

    protected List<GpxRoute> readRoutes(InputStream source) throws IOException {
        String content = decodeUtf8OrCp1252(readFully(source));
        List<GpxPosition> positions = new ArrayList<>();
        boolean first = true;
        for (String line : LINE_BREAK.split(content, -1)) {
            String trimmed = trim(line);
            if (trimmed.isEmpty() || isComment(trimmed))
                continue;
            boolean header = first && isHeader(line);
            first = false;
            if (header)
                continue;
            GpxPosition position = parseLine(line);
            // every line has to be a point of interest, else this is some other text
            if (position == null)
                return null;
            positions.add(position);
        }
        if (positions.isEmpty())
            return null;
        return singletonList(new GpxRoute(new Gpx10Format(), Waypoints, null, null, positions));
    }

    // write

    /**
     * Formats like the C format "%08.5f" does for a double value.
     * C printf rounds the exact binary value of the double, which is what new BigDecimal(double)
     * represents; BigDecimal.valueOf(double) would round the shortest decimal string instead and
     * differ from GPSBabel's output for values whose binary expansion lies just below a half.
     */
    static String formatCoordinate(Double value) {
        BigDecimal rounded = new BigDecimal(formatPosition(value).doubleValue()).setScale(5, RoundingMode.HALF_EVEN);
        boolean negative = value < 0.0 || rounded.signum() < 0;
        String digits = rounded.abs().toPlainString();
        StringBuilder result = new StringBuilder();
        if (negative)
            result.append('-');
        for (int i = result.length() + digits.length(); i < 8; i++)
            result.append('0');
        return result.append(digits).toString();
    }

    private static String clean(String string, int maximumLength) {
        if (string == null)
            return "";
        // a comma is dropped, a line break would split the record in two lines
        String result = cleanField(string.replace(String.valueOf(BAD_CHARACTER), ""), "");
        // "%-.Ns" cuts after N characters
        return result.length() > maximumLength ? result.substring(0, maximumLength) : result;
    }

    // GPSBabel hands the shortname on as the description when a waypoint has no description
    private static String asDescriptionColumn(String description) {
        String desc = asDesc(description);
        return desc == null || desc.isEmpty() ? asName(description) : desc;
    }

    public void write(GpxRoute route, OutputStream target, int startIndex, int endIndex) throws IOException {
        StringBuilder buffer = new StringBuilder();
        List<GpxPosition> positions = route.getPositions();
        for (int i = startIndex; i < endIndex; i++) {
            GpxPosition position = positions.get(i);
            if (position.getLongitude() == null || position.getLatitude() == null)
                continue;
            buffer.append(formatCoordinate(position.getLongitude())).append(DELIMITER)
                    .append(formatCoordinate(position.getLatitude())).append(DELIMITER)
                    .append(clean(asName(position.getDescription()), SHORT_LENGTH)).append(DELIMITER)
                    .append(clean(asDescriptionColumn(position.getDescription()), DESCRIPTION_LENGTH))
                    .append(RECORD_DELIMITER);
        }
        target.write(buffer.toString().getBytes(CHARSET));
        target.flush();
    }
}
