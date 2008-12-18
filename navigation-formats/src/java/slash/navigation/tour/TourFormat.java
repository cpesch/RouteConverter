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
    along with Foobar; if not, write to the Free Software
    Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA

    Copyright (C) 2007 Christian Pesch. All Rights Reserved.
*/

package slash.navigation.tour;

import slash.navigation.IniFileFormat;
import slash.navigation.BaseNavigationPosition;
import slash.navigation.RouteCharacteristics;
import slash.navigation.util.Conversion;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads and writes Falk Navigator (.tour) files.
 *
 * @author Christian Pesch
 */

public class TourFormat extends IniFileFormat<TourRoute> {
    static final String TOUR_TITLE = "TOUR";
    static final String NAME = "Name";
    private static final String ZIPCODE = "ZipCode";
    private static final String CITY = "City";
    private static final String STREET = "Street";
    private static final String HOUSENO = "HouseNo";
    private static final String LONGITUDE = "Longitude";
    private static final String LATITUDE = "Latitude";
    private static final String POSITION_IN_LIST = "PositionInList";
    static final String CLASS = "Class";
    static final String ASSEMBLY = "Assembly";
    static final String VISITED = "Visited";
    static final String CREATOR = "Creator";
    private static final Pattern SECTION_TITLE_PATTERN = Pattern.
            compile("\\" + SECTION_PREFIX + "(" + "\\d+" + "|" + TOUR_TITLE + ")\\" + SECTION_POSTFIX);

    public String getName() {
        return "Falk Navigator (*" + getExtension() + ")";
    }

    public String getExtension() {
        return ".tour";
    }

    public int getMaximumPositionCount() {
        return UNLIMITED_MAXIMUM_POSITION_COUNT;
    }

    public boolean isSupportsMultipleRoutes() {
        return false;
    }

    public <P extends BaseNavigationPosition> TourRoute createRoute(RouteCharacteristics characteristics, String name, List<P> positions) {
        return new TourRoute(name, (List<TourPosition>) positions);        
    }

    public List<TourRoute> read(File source) throws IOException {
        return read(source, UTF8_ENCODING);
    }

    public List<TourRoute> read(BufferedReader reader, String encoding) throws IOException {
        List<TourPosition> positions = new ArrayList<TourPosition>();
        Map<String, String> map = new HashMap<String, String>();
        String sectionTitle = null, routeName = null;

        while (true) {
            String line = reader.readLine();
            if (line == null) {
                TourPosition position = parsePosition(map);
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
                        TourPosition position = parsePosition(map);
                        if (position != null)
                            positions.add(position);
                        map = new HashMap<String, String>();
                    }
                }
                sectionTitle = parseSectionTitle(line);
            } else if (isNameValue(line)) {
                String name = Conversion.trim(parseName(line));
                String value = Conversion.trim(parseValue(line));
                map.put(name, value);
            } else {
                return null;
            }
        }

        if (positions.size() > 0) {
            return Arrays.asList(new TourRoute(this, routeName, positions));
        }

        return null;
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

    TourPosition parsePosition(Map<String, String> map) {
        String zipCode = Conversion.trim(map.get(ZIPCODE));
        String city = Conversion.trim(map.get(CITY));
        String street = Conversion.trim(map.get(STREET));
        String houseNo = Conversion.trim(map.get(HOUSENO));
        String name = Conversion.trim(map.get(NAME));

        Long x = Conversion.parseLong(Conversion.trim(map.get(LONGITUDE)));
        Long y = Conversion.parseLong(Conversion.trim(map.get(LATITUDE)));
        if (x == null || y == null)
            return null;

        map.remove(ZIPCODE);
        map.remove(CITY);
        map.remove(STREET);
        map.remove(HOUSENO);
        map.remove(NAME);
        map.remove(LONGITUDE);
        map.remove(LATITUDE);
        map.remove(POSITION_IN_LIST);

        return new TourPosition(x, y, zipCode, city, street, houseNo, name, map);
    }


    public void write(TourRoute route, File target, int startIndex, int endIndex, boolean numberPositionNames) throws IOException {
        write(route, target, UTF8_ENCODING, startIndex, endIndex, numberPositionNames);
    }

    public void write(TourRoute route, PrintWriter writer, int startIndex, int endIndex, boolean numberPositionNames) {
        writer.println(SECTION_PREFIX + TOUR_TITLE + SECTION_POSTFIX);
        writer.println(NAME + NAME_VALUE_SEPARATOR + route.getName());
        writer.println(CREATOR + NAME_VALUE_SEPARATOR + GENERATED_BY);

        List<TourPosition> positions = route.getPositions();
        for (int i = startIndex; i < endIndex; i++) {
            TourPosition position = positions.get(i);

            writer.println(SECTION_PREFIX + Integer.toString(i) + SECTION_POSTFIX);

            String name = position.getName();
            if (name == null)
                name = position.getCity();
            if (name == null)
                name = position.getComment();
            writer.println(NAME + NAME_VALUE_SEPARATOR + name);
            writer.println(POSITION_IN_LIST + NAME_VALUE_SEPARATOR + Integer.toString(i));
            if (position.getZipCode() != null)
                writer.println(ZIPCODE + NAME_VALUE_SEPARATOR + position.getZipCode());
            if (position.getCity() != null && !position.getCity().equals(name))
                writer.println(CITY + NAME_VALUE_SEPARATOR + position.getCity());
            if (position.getStreet() != null)
                writer.println(STREET + NAME_VALUE_SEPARATOR + position.getStreet());
            if (position.getHouseNo() != null)
                writer.println(HOUSENO + NAME_VALUE_SEPARATOR + position.getHouseNo());

            writer.println(LONGITUDE + NAME_VALUE_SEPARATOR + position.getX());
            writer.println(LATITUDE + NAME_VALUE_SEPARATOR + position.getY());

            for (String key : position.keySet()) {
                String value = position.get(key);
                writer.println(key + NAME_VALUE_SEPARATOR + value);
            }

            if (!position.keySet().contains(CLASS))
                writer.println(CLASS + NAME_VALUE_SEPARATOR + "FMI.FalkNavigator.PersistentAddress");
            if (!position.keySet().contains(ASSEMBLY))
                writer.println(ASSEMBLY + NAME_VALUE_SEPARATOR + "FalkNavigator");
            if (!position.keySet().contains(VISITED))
                writer.println(VISITED + NAME_VALUE_SEPARATOR + "0");
        }
    }
}

