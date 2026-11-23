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
package slash.navigation.googletakeout;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import slash.navigation.base.*;
import slash.navigation.common.NavigationPosition;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Locale.ROOT;
import static java.util.regex.Pattern.CASE_INSENSITIVE;
import static slash.common.io.Transfer.*;
import static slash.navigation.base.RouteCharacteristics.Waypoints;

/**
 * Reads the saved lists (Favorites, Want to go, own lists) that Google Takeout exports as
 * {@code Saved/<list>.csv}: title, note, URL, tags and comment per place, but no coordinates.
 * Places become positions without coordinates unless the URL or the note carries them; the
 * user adds coordinates with Position &gt; Complete&hellip; &gt; Coordinates.
 * <p>
 * Takeout localizes the folder ({@code Saved}, {@code Gespeichert}, ...) and the column names
 * ({@code Title,Note,URL} in English, {@code Titel,Notiz,URL} in German). Column names known
 * from real exports are matched by name; for other languages the columns are taken by their
 * position, title first, note second and {@code URL} third, as long as a row links to Google Maps.
 *
 * @see <a href="https://developers.google.com/data-portability/schema-reference/save">Google's schema</a>
 * @author Christian Pesch
 */

public class GoogleSavedListFormat extends SimpleFormat<Wgs84Route> {
    // "ort" and "adresse" head the German followed places file (Orte in der Nähe, denen Sie folgen.csv);
    // "place" and "address" are the assumed English names, not seen in an export yet
    private static final Set<String> TITLE_COLUMNS = new HashSet<>(Arrays.asList("title", "titel", "place", "ort"));
    private static final Set<String> NOTE_COLUMNS = new HashSet<>(Arrays.asList("note", "notiz"));
    private static final Set<String> ADDRESS_COLUMNS = new HashSet<>(Arrays.asList("address", "adresse"));
    private static final String URL = "url";
    private static final int URL_COLUMN_INDEX = 2;
    private static final int MAXIMUM_COLUMN_COUNT = 5;
    private static final Set<String> COORDINATE_COLUMNS = new HashSet<>(Arrays.asList(
            "latitude", "longitude", "lat", "lon", "lng", "breite", "länge", "breitengrad", "längengrad",
            "coordinates", "koordinaten"));
    private static final int HEADER_SEARCH_ROW_LIMIT = 5;
    private static final int HEADER_SEARCH_MARK_LIMIT = 1 << 16;

    private static final String NUMBER = "([-+]?\\d{1,3}(?:\\.\\d+)?)";
    private static final String SEPARATOR = "\\s*(?:,|%2C|%2c)\\s*\\+?";
    private static final Pattern URL_COORDINATES_PATTERN = Pattern.compile(
            "(?:/search/|/place/|/dir/|[?&](?:q|query|destination)=)" + NUMBER + SEPARATOR + NUMBER);
    private static final Pattern PLACE_PIN_PATTERN = Pattern.compile("!3d" + NUMBER + "!4d" + NUMBER);
    private static final Pattern GOOGLE_MAPS_URL_PATTERN = Pattern.compile(
            "^https?://(?:(?:[a-z]+\\.)?google\\.[a-z.]+/maps|maps\\.google\\.|maps\\.app\\.goo\\.gl/)", CASE_INSENSITIVE);
    private static final Pattern NOTE_COORDINATES_PATTERN = Pattern.compile(
            "\\s*" + NUMBER + "\\s*,\\s*" + NUMBER + "\\s*");

    public String getExtension() {
        return ".csv";
    }

    public String getName() {
        return "Google Maps Saved List (" + getExtension() + ")";
    }

    public int getMaximumPositionCount() {
        return UNLIMITED_MAXIMUM_POSITION_COUNT;
    }

    public boolean isSupportsReading() {
        return true;
    }

    public boolean isSupportsWriting() {
        return false;
    }

    @SuppressWarnings("unchecked")
    public <P extends NavigationPosition> Wgs84Route createRoute(RouteCharacteristics characteristics, String name, List<P> positions) {
        return new Wgs84Route(this, characteristics, name, (List<Wgs84Position>) positions);
    }

    public void read(InputStream source, ParserContext<Wgs84Route> context) throws IOException {
        read(source, UTF8_ENCODING, context);
    }

    public void read(BufferedReader reader, String encoding, ParserContext<Wgs84Route> context) throws IOException {
        reader.mark(HEADER_SEARCH_MARK_LIMIT);
        boolean byteOrderMark = false;
        List<String> leadingLines = new ArrayList<>();
        int headerLineIndex = -1;
        Columns columns = null;
        String line;
        for (int i = 0; i < HEADER_SEARCH_ROW_LIMIT && (line = reader.readLine()) != null; i++) {
            if (i == 0 && line.startsWith("\uFEFF")) {
                byteOrderMark = true;
                line = line.substring(1);
            }
            columns = parseHeader(line);
            if (columns != null) {
                headerLineIndex = i;
                break;
            }
            leadingLines.add(line);
        }
        if (headerLineIndex == -1)
            return;

        reader.reset();
        if (byteOrderMark)
            //noinspection ResultOfMethodCallIgnored
            reader.read();
        for (int i = 0; i < headerLineIndex; i++)
            reader.readLine();

        List<Wgs84Position> positions = new ArrayList<>();
        boolean linksToGoogleMaps = false;
        CsvSchema schema = CsvSchema.emptySchema().withHeader();
        ObjectReader objectReader = new CsvMapper().readerFor(LinkedHashMap.class).with(schema);
        try (MappingIterator<LinkedHashMap<String, String>> iterator = objectReader.readValues(reader)) {
            while (iterator.hasNext()) {
                Map<String, String> row = lowerCaseKeys(iterator.next());
                String url = trim(row.get(URL));
                if (url != null && GOOGLE_MAPS_URL_PATTERN.matcher(url).find())
                    linksToGoogleMaps = true;
                Wgs84Position position = createPosition(trim(row.get(columns.title)), trim(row.get(columns.note)),
                        columns.address != null ? trim(row.get(columns.address)) : null, url);
                if (position != null)
                    positions.add(position);
            }
        }

        // columns taken by position could belong to any CSV with a URL column: insist on Google Maps
        if (!columns.byName && !linksToGoogleMaps)
            return;

        if (!positions.isEmpty())
            context.appendRoute(new Wgs84Route(this, Waypoints, createRouteName(context.getFile(), leadingLines), positions));
    }

    private static class Columns {
        final String title;
        final String note;
        final String address;
        final boolean byName;

        Columns(String title, String note, String address, boolean byName) {
            this.title = title;
            this.note = note;
            this.address = address;
            this.byName = byName;
        }
    }

    static boolean isHeader(String line) {
        return parseHeader(line) != null;
    }

    private static Columns parseHeader(String line) {
        List<String> cells = new ArrayList<>();
        for (String cell : line.split(",", -1))
            cells.add(trimQuotes(cell).toLowerCase(ROOT));
        if (!cells.contains(URL))
            return null;
        for (String cell : cells)
            if (COORDINATE_COLUMNS.contains(cell))
                return null;

        String title = firstOf(cells, TITLE_COLUMNS);
        if (title != null) {
            return new Columns(title, firstOf(cells, NOTE_COLUMNS), firstOf(cells, ADDRESS_COLUMNS), true);
        }

        // a language whose column names are not known: Takeout writes title, note, URL, tags, comment
        if (cells.indexOf(URL) == URL_COLUMN_INDEX && cells.size() <= MAXIMUM_COLUMN_COUNT &&
                !cells.get(0).isEmpty() && !cells.get(1).isEmpty())
            return new Columns(cells.get(0), cells.get(1), null, false);
        return null;
    }

    private static String firstOf(List<String> cells, Set<String> names) {
        for (String cell : cells)
            if (names.contains(cell))
                return cell;
        return null;
    }

    private static String trimQuotes(String cell) {
        String result = cell.trim();
        if (result.length() >= 2 && result.startsWith("\"") && result.endsWith("\""))
            result = result.substring(1, result.length() - 1).trim();
        return result;
    }

    private static Map<String, String> lowerCaseKeys(Map<String, String> row) {
        Map<String, String> result = new HashMap<>();
        for (Map.Entry<String, String> entry : row.entrySet())
            result.put(entry.getKey().trim().toLowerCase(ROOT), entry.getValue());
        return result;
    }

    private Wgs84Position createPosition(String title, String note, String address, String url) {
        double[] coordinates = parseUrlCoordinates(url);
        if (coordinates == null)
            coordinates = parseNoteCoordinates(note);

        if (title == null && coordinates == null)
            return null;

        // an address helps the geocoder of Complete > Coordinates, which tries the whole
        // description first and then the name before the first ';'
        String description = address != null ? asDescription(title, address) :
                coordinates != null ? asDescription(title, note) : title;
        return new Wgs84Position(coordinates != null ? coordinates[0] : null,
                coordinates != null ? coordinates[1] : null, null, null, null, description);
    }

    /**
     * @return {longitude, latitude} or null
     */
    static double[] parseUrlCoordinates(String url) {
        if (url == null)
            return null;
        double[] coordinates = firstValidCoordinates(PLACE_PIN_PATTERN, url);
        if (coordinates == null)
            coordinates = firstValidCoordinates(URL_COORDINATES_PATTERN, url);
        return coordinates;
    }

    private static double[] firstValidCoordinates(Pattern pattern, String url) {
        Matcher matcher = pattern.matcher(url);
        while (matcher.find()) {
            double[] coordinates = asCoordinates(matcher.group(1), matcher.group(2));
            if (coordinates != null)
                return coordinates;
        }
        return null;
    }

    /**
     * @return {longitude, latitude} or null
     */
    static double[] parseNoteCoordinates(String note) {
        if (note == null)
            return null;
        Matcher matcher = NOTE_COORDINATES_PATTERN.matcher(note);
        return matcher.matches() ? asCoordinates(matcher.group(1), matcher.group(2)) : null;
    }

    private static double[] asCoordinates(String latitudeString, String longitudeString) {
        double latitude = Double.parseDouble(latitudeString);
        double longitude = Double.parseDouble(longitudeString);
        if (latitude < -90.0 || latitude > 90.0 || longitude < -180.0 || longitude > 180.0)
            return null;
        if (latitude == 0.0 && longitude == 0.0)
            return null;
        return new double[]{longitude, latitude};
    }

    private static String createRouteName(File file, List<String> leadingLines) {
        if (file != null) {
            String name = file.getName();
            int dot = name.lastIndexOf('.');
            name = trim(dot > 0 ? name.substring(0, dot) : name);
            if (name != null)
                return name;
        }
        for (String line : leadingLines) {
            String description = trim(trimQuotes(line));
            if (description != null)
                return description;
        }
        return null;
    }

    public void write(Wgs84Route route, PrintWriter writer, int startIndex, int endIndex) {
        throw new UnsupportedOperationException("GoogleSavedListFormat is read-only");
    }
}
