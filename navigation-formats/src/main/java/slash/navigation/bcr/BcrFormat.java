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

package slash.navigation.bcr;

import slash.navigation.base.IniFileFormat;
import slash.navigation.base.ParserContext;
import slash.navigation.base.RouteCharacteristics;
import slash.navigation.common.NavigationPosition;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static slash.common.io.Transfer.*;
import static slash.navigation.bcr.BcrPosition.NO_ALTITUDE_DEFINED;

/**
 * The base of all Map &amp; Guide Tourenplaner Route formats.
 *
 * @author Christian Pesch
 */

public abstract class BcrFormat extends IniFileFormat<BcrRoute> {
    private static final Logger log = Logger.getLogger(BcrFormat.class.getName());
    private static final Preferences preferences = Preferences.userNodeForPackage(BcrFormat.class);

    static final String CLIENT_TITLE = "CLIENT";
    static final String COORDINATES_TITLE = "COORDINATES";
    static final String DESCRIPTION_TITLE = "DESCRIPTION";
    static final String ROUTE_TITLE = "ROUTE";

    private static final Pattern SECTION_TITLE_PATTERN = Pattern.
            compile("\\" + SECTION_PREFIX + "([\\p{Upper}|\\s]+)" + SECTION_POSTFIX);

    static final char VALUE_SEPARATOR = ',';
    private static final Pattern COORDINATES_VALUE_PATTERN = Pattern.compile("(-?\\d+)" + VALUE_SEPARATOR + "(-?\\d+)");
    private static final Pattern ALTITUDE_VALUE_PATTERN = Pattern.compile("([\\w|\\s]+)" + VALUE_SEPARATOR + "([^,]+),?.*");

    static final String ROUTE_NAME = "ROUTENAME";
    static final String EXPECTED_DISTANCE = "EXP_DISTANCE";
    static final String DESCRIPTION_LINE_COUNT = "DESCRIPTIONLINES";
    static final String DESCRIPTION = "DESCRIPTION";
    static final String CREATOR = "CREATOR";

    public String getExtension() {
        return ".bcr";
    }

    public int getMaximumPositionCount() {
        return preferences.getInt("maximumPositionCount", 1 + 99 + 1);
    }

    public boolean isSupportsMultipleRoutes() {
        return false;
    }

    public boolean isWritingRouteCharacteristics() {
        return false;
    }

    @SuppressWarnings("unchecked")
    public <P extends NavigationPosition> BcrRoute createRoute(RouteCharacteristics characteristics, String name, List<P> positions) {
        return new BcrRoute(this, name, null, (List<BcrPosition>) positions);
    }

    public void read(BufferedReader reader, String encoding, ParserContext<BcrRoute> context) throws IOException {
        List<BcrSection> sections = new ArrayList<>();
        List<BcrPosition> positions = new ArrayList<>();
        BcrSection current = null;

        while (true) {
            String line = reader.readLine();
            if (line == null)
                break;
            if (line.length() == 0)
                continue;

            if (isSectionTitle(line)) {
                BcrSection section = new BcrSection(parseSectionTitle(line));
                sections.add(section);
                current = section;
            }

            if (isNameValue(line)) {
                if (current == null) {
                    // name value without section means this isn't the file format we expect
                    return;
                } else
                    current.put(parseName(line), parseValue(line));
            }
        }

        if (hasValidSections(sections)) {
            extractPositions(sections, positions);
            if (positions.size() >= 2)
                context.appendRoute(new BcrRoute(this, sections, positions));
        }
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


    private BcrSection findSection(List<BcrSection> sections, String title) {
        for (BcrSection section : sections) {
            if (title.equals(section.getTitle()))
                return section;
        }
        return null;
    }

    private boolean existsSection(List<BcrSection> sections, String title) {
        return findSection(sections, title) != null;
    }

    protected abstract boolean isValidDescription(BcrSection description);

    private boolean hasValidSections(List<BcrSection> sections) {
        if (existsSection(sections, CLIENT_TITLE) && existsSection(sections, DESCRIPTION_TITLE) &&
                existsSection(sections, COORDINATES_TITLE)) {
            BcrSection client = findSection(sections, CLIENT_TITLE);
            BcrSection coordinates = findSection(sections, COORDINATES_TITLE);
            BcrSection description = findSection(sections, DESCRIPTION_TITLE);
            return isValidDescription(description) &&
                    client != null && coordinates != null && description != null &&
                    client.getStationCount() == coordinates.getStationCount() &&
                    coordinates.getStationCount() == description.getStationCount();
        }
        return false;
    }

    private void extractPositions(List<BcrSection> sections, List<BcrPosition> positions) {
        BcrSection client = findSection(sections, CLIENT_TITLE);
        BcrSection coordinates = findSection(sections, COORDINATES_TITLE);
        BcrSection description = findSection(sections, DESCRIPTION_TITLE);
        if(client == null || coordinates == null || description == null)
            return;

        for (int i = 1; i < client.getStationCount(); i++) {
            String clientStr = client.getStation(i);
            String coordinatesStr = coordinates.getStation(i);
            String descriptionStr = description.getStation(i);
            positions.add(parsePosition(clientStr, coordinatesStr, descriptionStr));
        }
        client.removeStations();
        coordinates.removeStations();
        description.removeStations();
    }

    BcrPosition parsePosition(String client, String coordinate, String description) {
        Matcher coordinateMatcher = COORDINATES_VALUE_PATTERN.matcher(coordinate);
        if (!coordinateMatcher.matches())
            throw new IllegalArgumentException("'" + coordinate + "' does not match coordinates pattern");
        String x = coordinateMatcher.group(1);
        String y = coordinateMatcher.group(2);

        long altitude = NO_ALTITUDE_DEFINED;
        Matcher clientMatcher = ALTITUDE_VALUE_PATTERN.matcher(client);
        if (!clientMatcher.matches())
            log.info("'" + client + "' does not match client station pattern; ignoring it");
        else {
            String string = clientMatcher.group(2);
            try {
                Long aLong = parseLong(string);
                if (aLong != null)
                    altitude = aLong;
            }
            catch (NumberFormatException e) {
                log.info("'" + string + "' is not a valid altitude; ignoring it");
            }
        }
        return new BcrPosition(parseInt(x), parseInt(y), altitude, trim(description));
    }


    protected abstract void writePosition(BcrPosition position, PrintWriter writer, int index);

    public void write(BcrRoute route, PrintWriter writer, int startIndex, int endIndex) {
        List<BcrPosition> positions = route.getPositions();
        for (BcrSection section : route.getSections()) {
            writer.println(SECTION_PREFIX + section.getTitle() + SECTION_POSTFIX);

            for (String name : section.keySet()) {
                if (!name.equals(CREATOR) && !name.equals(ROUTE_NAME) && !name.equals(EXPECTED_DISTANCE)) {
                    String value = section.get(name);
                    if (value == null)
                        value = "";
                    writer.println(name + NAME_VALUE_SEPARATOR + value);
                }
            }

            if (CLIENT_TITLE.equals(section.getTitle())) {
                writer.println(CREATOR + NAME_VALUE_SEPARATOR + GENERATED_BY);
                writer.println(ROUTE_NAME + NAME_VALUE_SEPARATOR + asRouteName(route.getName()));
                double length = route.getDistance();
                if(length > 0)
                    length = length / 1000.0;
                writer.println(EXPECTED_DISTANCE + NAME_VALUE_SEPARATOR + (int)length);

                int index = 1;
                int maxIndex = positions.size();
                for (int i = startIndex; i < endIndex; i++) {
                    BcrPosition position = positions.get(i);
                    long altitude = position.getAltitude();
                    boolean center = position.getStreet() != null && position.getStreet().equals(BcrPosition.STREET_DEFINES_CENTER_NAME);
                    boolean first = index == 1;
                    boolean last = index == maxIndex;
                    String altitutdeDescription = center || first || last ? "TOWN" : "Standort";
                    writer.println(BcrSection.STATION_PREFIX + (index++) + NAME_VALUE_SEPARATOR +
                            altitutdeDescription + VALUE_SEPARATOR + altitude);

                }
            }

            if (COORDINATES_TITLE.equals(section.getTitle())) {
                int index = 1;
                for (int i = startIndex; i < endIndex; i++) {
                    BcrPosition position = positions.get(i);
                    writer.println(BcrSection.STATION_PREFIX + (index++) + NAME_VALUE_SEPARATOR +
                            position.getX() + VALUE_SEPARATOR + position.getY());

                }
            }

            if (DESCRIPTION_TITLE.equals(section.getTitle())) {
                int index = 1;
                for (int i = startIndex; i < endIndex; i++) {
                    BcrPosition position = positions.get(i);
                    writePosition(position, writer, index++);
                }
            }
        }
    }
}
