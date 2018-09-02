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

package slash.navigation.tour;

import slash.navigation.base.IniFileFormat;
import slash.navigation.base.ParserContext;
import slash.navigation.base.RouteCharacteristics;
import slash.navigation.common.NavigationPosition;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Arrays.asList;
import static java.util.Arrays.sort;
import static slash.common.io.Transfer.UTF8_ENCODING;
import static slash.common.io.Transfer.parseLong;
import static slash.common.io.Transfer.trim;
import static slash.navigation.base.RouteCharacteristics.Waypoints;

/**
 * Reads and writes Falk Navigator (.tour) files.
 *
 * @author Christian Pesch
 */

public class TourFormat extends IniFileFormat<TourRoute> {
    private static final String TOUR_TITLE = "TOUR";
    private static final String HOME_TITLE = "HOME";
    private static final String NAME = "Name";
    private static final String ZIPCODE = "ZipCode";
    private static final String CITY = "City";
    private static final String STREET = "Street";
    private static final String HOUSENO = "HouseNo";
    private static final String LONGITUDE = "Longitude";
    private static final String LATITUDE = "Latitude";
    static final String POSITION_IN_LIST = "PositionInList";
    static final String EXTEND_ROUTE = "ExtendRoute";
    static final String CLASS = "Class";
    static final String ASSEMBLY = "Assembly";
    static final String VISITED = "Visited";
    private static final String CREATOR = "Creator";
    private static final Pattern SECTION_TITLE_PATTERN = Pattern.
            compile(".*\\" + SECTION_PREFIX + "(" + "\\d+" + "|" + TOUR_TITLE + "|" + HOME_TITLE + ")\\" + SECTION_POSTFIX + ".*");

    public String getExtension() {
        return ".tour";
    }

    public String getName() {
        return "Falk Navigator (*" + getExtension() + ")";
    }

    public int getMaximumPositionCount() {
        return UNLIMITED_MAXIMUM_POSITION_COUNT;
    }

    public boolean isSupportsMultipleRoutes() {
        return false;
    }

    public boolean isWritingRouteCharacteristics() {
        return false;
    }

    @SuppressWarnings("unchecked")
    public <P extends NavigationPosition> TourRoute createRoute(RouteCharacteristics characteristics, String name, List<P> positions) {
        return new TourRoute(name, (List<TourPosition>) positions);
    }

    public void read(InputStream source, ParserContext<TourRoute> context) throws Exception {
        read(source, UTF8_ENCODING, context);
    }

    public void read(BufferedReader reader, String encoding, ParserContext<TourRoute> context) throws IOException {
        List<TourPosition> positions = new ArrayList<>();
        Map<String, String> map = new HashMap<>();
        String sectionTitle = null, routeName = null;

        while (true) {
            String line = reader.readLine();
            if (line == null) {
                TourPosition position = parsePosition(map, sectionTitle);
                if (position != null)
                    positions.add(position);
                break;
            }
            if (line.length() == 0)
                continue;

            if (isSectionTitle(line)) {
                if (sectionTitle != null) {
                    if (TOUR_TITLE.equals(sectionTitle)) {
                        routeName = map.get(NAME);
                        // ignore everything else from the [TOUR] section
                        map.clear();
                    } else {
                        // if there is no PositionInList key, use the sectionTitle to order the positions
                        if (!map.containsKey(POSITION_IN_LIST))
                            map.put(POSITION_IN_LIST, sectionTitle);

                        TourPosition position = parsePosition(map, sectionTitle);
                        if (position != null)
                            positions.add(position);
                        map = new HashMap<>();
                    }
                }
                sectionTitle = parseSectionTitle(line);
            } else if (isNameValue(line)) {
                String name = trim(parseName(line));
                String value = trim(parseValue(line));
                map.put(name, value);
            } else {
                return;
            }
        }

        if (positions.size() > 0)
            context.appendRoute(createRoute(Waypoints, routeName, sortPositions(positions)));
    }

    private List<TourPosition> sortPositions(List<TourPosition> positions) {
        TourPosition[] positionArray = positions.toArray(new TourPosition[0]);
        sort(positionArray, new PositionInListComparator());
        return new ArrayList<>(asList(positionArray));
    }

    boolean isSectionTitle(String line) {
        Matcher matcher = SECTION_TITLE_PATTERN.matcher(line);
        return matcher.matches();
    }

    String parseSectionTitle(String line) {
        Matcher matcher = SECTION_TITLE_PATTERN.matcher(line);
        if (!matcher.matches())
            throw new IllegalArgumentException("'" + line + "' does not match");
        return matcher.group(1);
    }

    TourPosition parsePosition(Map<String, String> map, String sectionTitle) {
        String zipCode = trim(map.get(ZIPCODE));
        String city = trim(map.get(CITY));
        String street = trim(map.get(STREET));
        String houseNo = trim(map.get(HOUSENO));
        String name = trim(map.get(NAME));

        Long x = parseLong(trim(map.get(LONGITUDE)));
        Long y = parseLong(trim(map.get(LATITUDE)));
        if (x == null || y == null)
            return null;

        map.remove(ZIPCODE);
        map.remove(CITY);
        map.remove(STREET);
        map.remove(HOUSENO);
        map.remove(NAME);
        map.remove(LONGITUDE);
        map.remove(LATITUDE);

        return new TourPosition(x, y, zipCode, city, street, houseNo, name, HOME_TITLE.equals(sectionTitle), map);
    }


    public void write(TourRoute route, OutputStream target, int startIndex, int endIndex) throws IOException {
        write(route, target, UTF8_ENCODING, startIndex, endIndex);
    }

    private static final String TOUR_FORMAT_NAME_VALUE_SEPARATOR = " " + NAME_VALUE_SEPARATOR + " ";

    public void write(TourRoute route, PrintWriter writer, int startIndex, int endIndex) {
        writer.println(SECTION_PREFIX + TOUR_TITLE + SECTION_POSTFIX);
        writer.println(NAME + TOUR_FORMAT_NAME_VALUE_SEPARATOR + asRouteName(route.getName()));
        writer.println(CREATOR + TOUR_FORMAT_NAME_VALUE_SEPARATOR + GENERATED_BY);
        writer.println();

        List<TourPosition> positions = route.getPositions();
        for (int i = startIndex; i < endIndex; i++) {
            TourPosition position = positions.get(i);

            String sectionTitle = position.isHome() ? HOME_TITLE : Integer.toString(i);
            writer.println(SECTION_PREFIX + sectionTitle + SECTION_POSTFIX);

            String name = position.getName();
            if (name == null)
                name = position.getCity();
            if (name == null)
                name = position.getDescription();
            writer.println(NAME + TOUR_FORMAT_NAME_VALUE_SEPARATOR + name);
            writer.println(POSITION_IN_LIST + TOUR_FORMAT_NAME_VALUE_SEPARATOR + Integer.toString(i));
            if (position.getZipCode() != null)
                writer.println(ZIPCODE + TOUR_FORMAT_NAME_VALUE_SEPARATOR + position.getZipCode());
            if (position.getCity() != null && !position.getCity().equals(name))
                writer.println(CITY + TOUR_FORMAT_NAME_VALUE_SEPARATOR + position.getCity());
            if (position.getStreet() != null)
                writer.println(STREET + TOUR_FORMAT_NAME_VALUE_SEPARATOR + position.getStreet());
            if (position.getHouseNo() != null)
                writer.println(HOUSENO + TOUR_FORMAT_NAME_VALUE_SEPARATOR + position.getHouseNo());

            writer.println(LONGITUDE + TOUR_FORMAT_NAME_VALUE_SEPARATOR + position.getX());
            writer.println(LATITUDE + TOUR_FORMAT_NAME_VALUE_SEPARATOR + position.getY());
            if (i < endIndex - 1)
                writer.println(EXTEND_ROUTE + TOUR_FORMAT_NAME_VALUE_SEPARATOR + "1");

            for (String key : position.keySet()) {
                if (!key.equals(POSITION_IN_LIST) && !key.equals(EXTEND_ROUTE)) {
                    String value = position.get(key);
                    writer.println(key + TOUR_FORMAT_NAME_VALUE_SEPARATOR + value);
                }
            }

            if (!position.keySet().contains(CLASS))
                writer.println(CLASS + TOUR_FORMAT_NAME_VALUE_SEPARATOR + "FMI.FalkNavigator.PersistentAddress");
            if (!position.keySet().contains(ASSEMBLY))
                writer.println(ASSEMBLY + TOUR_FORMAT_NAME_VALUE_SEPARATOR + "FalkNavigator");
            if (!position.keySet().contains(VISITED))
                writer.println(VISITED + TOUR_FORMAT_NAME_VALUE_SEPARATOR + "0");

            writer.println();
        }
    }
}

