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

package slash.navigation.ovl;

import slash.navigation.*;
import slash.navigation.util.Calculation;
import slash.navigation.util.Conversion;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads and writes Top50 OVL ASCII (.ovl) files.
 *
 * @author Christian Pesch
 */

public class OvlFormat extends IniFileFormat<OvlRoute> implements MultipleRoutesFormat<OvlRoute> {
    static final String SYMBOL_TITLE = "Symbol";
    static final String OVERLAY_TITLE = "Overlay";
    static final String MAPLAGE_TITLE = "MapLage";

    static final String SYMBOL_COUNT = "Symbols";
    static final String ROUTE_NAME = "RouteName";
    static final String CREATOR = "Creator";

    private static final Pattern SECTION_TITLE_PATTERN = Pattern.
            compile("\\" + SECTION_PREFIX + "(" + SYMBOL_TITLE + " \\d+|" + OVERLAY_TITLE + "|" +
                    MAPLAGE_TITLE + ")\\" + SECTION_POSTFIX);

    public String getName() {
        return "Top50 OVL ASCII (*" + getExtension() + ")";
    }

    public String getExtension() {
        return ".ovl";
    }

    public int getMaximumPositionCount() {
        return UNLIMITED_MAXIMUM_POSITION_COUNT;
    }

    public boolean isSupportsMultipleRoutes() {
        return true;
    }

    public <P extends BaseNavigationPosition> OvlRoute createRoute(RouteCharacteristics characteristics, String name, List<P> positions) {
        return new OvlRoute(characteristics, name, (List<Wgs84Position>) positions);
    }

    public List<OvlRoute> read(BufferedReader reader, Calendar startDate, String encoding) throws IOException {
        List<OvlSection> sections = new ArrayList<OvlSection>();
        OvlSection current = null;

        while (true) {
            String line = reader.readLine();
            if (line == null)
                break;
            if (line.length() == 0)
                continue;

            if (isSectionTitle(line)) {
                OvlSection section = new OvlSection(parseSectionTitle(line));
                sections.add(section);
                current = section;
            }

            if (isNameValue(line)) {
                if (current == null) {
                    // name value without section means this isn't the file format we expect
                    return null;
                } else
                    current.put(parseName(line), parseValue(line));
            }
        }

        if (hasValidSections(sections)) {
            List<OvlRoute> routes = extractRoutes(sections);
            if (routes.size() > 0)
                return routes;
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


    private OvlSection findSection(List<OvlSection> sections, String title) {
        for (OvlSection section : sections) {
            if (title.equals(section.getTitle()))
                return section;
        }
        return null;
    }

    private boolean existsSection(List<OvlSection> sections, String title) {
        return findSection(sections, title) != null;
    }

    private boolean hasValidSections(List<OvlSection> sections) {
        if (!existsSection(sections, OVERLAY_TITLE) || !existsSection(sections, MAPLAGE_TITLE))
            return false;

        OvlSection overlay = findSection(sections, OVERLAY_TITLE);
        int symbolCount = getSymbolCount(overlay);
        if (symbolCount == 0)
            return false;

        for (int i = 0; i < symbolCount; i++) {
            if (!existsSection(sections, SYMBOL_TITLE + " " + (i + 1)))
                return false;
        }
        return true;
    }

    private int getSymbolCount(OvlSection overlay) {
        String symbols = overlay.get(SYMBOL_COUNT);
        return symbols != null ? Integer.parseInt(symbols) : 0;
    }

    private List<OvlRoute> extractRoutes(List<OvlSection> sections) {
        List<OvlRoute> result = new ArrayList<OvlRoute>();

        OvlSection mapLage = findSection(sections, MAPLAGE_TITLE);
        OvlSection overlay = findSection(sections, OVERLAY_TITLE);
        int symbolCount = getSymbolCount(overlay);

        // process all sections with same group into one route
        Map<Integer, List<OvlSection>> sectionsByGroup = new LinkedHashMap<Integer, List<OvlSection>>();
        List<OvlSection> sectionsWithoutGroup = new ArrayList<OvlSection>();
        for (int i = 0; i < symbolCount; i++) {
            OvlSection section = findSection(sections, SYMBOL_TITLE + " " + (i + 1));
            if (section != null) {
                Integer group = section.getGroup();
                if (group != null) {
                    List<OvlSection> groupedSections = sectionsByGroup.get(group);
                    if (groupedSections == null) {
                        groupedSections = new ArrayList<OvlSection>();
                        sectionsByGroup.put(group, groupedSections);
                    }
                    groupedSections.add(section);
                } else
                    sectionsWithoutGroup.add(section);
            }
        }

        for (OvlSection section : sectionsWithoutGroup) {
            OvlRoute route = extractRoute(section, overlay, mapLage);
            if (route != null)
                result.add(route);
        }

        for (Integer group : sectionsByGroup.keySet()) {
            List<OvlSection> sectionList = sectionsByGroup.get(group);
            OvlRoute route = extractRoute(sectionList, overlay, mapLage);
            if (route != null)
                result.add(route);
        }
        return result;
    }

    private OvlRoute extractRoute(OvlSection symbol, OvlSection overlay, OvlSection mapLage) {
        List<Wgs84Position> positions = new ArrayList<Wgs84Position>();
        RouteCharacteristics characteristics = symbol.getCharacteristics();
        if (characteristics == null)
            characteristics = RouteCharacteristics.Route;

        int positionCount = symbol.getPositionCount();
        for (int i = 0; i < positionCount; i++) {
            positions.add(symbol.getPosition(i));
        }
        symbol.removePositions();
        return new OvlRoute(this, characteristics, symbol.getTitle(), symbol, overlay, mapLage, positions);
    }

    private OvlRoute extractRoute(List<OvlSection> symbols, OvlSection overlay, OvlSection mapLage) {
        List<Wgs84Position> positions = new ArrayList<Wgs84Position>();
        RouteCharacteristics characteristics = null;
        for (OvlSection symbol : symbols) {
            if (characteristics == null)
                characteristics = symbol.getCharacteristics();
            int positionCount = symbol.getPositionCount();
            for (int i = 0; i < positionCount; i++) {
                positions.add(symbol.getPosition(i));
            }
            symbol.removePositions();
        }
        OvlSection symbol = symbols.size() > 0 ? symbols.get(0) : null;
        if (characteristics == null)
            characteristics = RouteCharacteristics.Route;
        return new OvlRoute(this, characteristics, mapLage.getTitle(), symbol, overlay, mapLage, positions);
    }

    static RouteCharacteristics getCharacteristics(int typ) {
        switch (typ) {
            case 1:
                return RouteCharacteristics.Waypoints;
            case 2:
                return RouteCharacteristics.Track;
            case 3:
                return RouteCharacteristics.Route;
            default:
                return null;
        }
    }

    static int getTyp(RouteCharacteristics characteristics) {
        switch (characteristics) {
            case Waypoints:
                return 1;
            case Track:
                return 2;
            case Route:
                return 3;
            default:
                throw new IllegalArgumentException("Unknown RouteCharacteristics " + characteristics);
        }
    }


    private void writeSection(OvlSection section, PrintWriter writer, Collection<String> ignoreNames) {
        for (String name : section.keySet()) {
            if (!ignoreNames.contains(name)) {
                String value = section.get(name);
                if (value == null)
                    value = "";
                writer.println(name + NAME_VALUE_SEPARATOR + value);
            }
        }
    }

    private void writeMissingAttribute(OvlSection section, PrintWriter writer, String name, String defaultValue) {
        if (section.get(name) == null)
            writer.println(name + NAME_VALUE_SEPARATOR + defaultValue);
    }

    private void writeSymbol(OvlRoute route, PrintWriter writer, int startIndex, int endIndex, int symbolIndex) {
        writer.println(SECTION_PREFIX + SYMBOL_TITLE + " " + symbolIndex + SECTION_POSTFIX);
        // the Top 50 does not like other characteristics than Route :-(
        // writer.println(OvlSection.CHARACTERISTICS + NAME_VALUE_SEPARATOR + getTyp(route.getCharacteristics()));
        writer.println(OvlSection.CHARACTERISTICS + NAME_VALUE_SEPARATOR + getTyp(RouteCharacteristics.Route));
        writer.println(OvlSection.GROUP + NAME_VALUE_SEPARATOR + symbolIndex);

        writeSection(route.getSymbol(), writer, Arrays.asList(OvlSection.TEXT));
        writeMissingAttribute(route.getSymbol(), writer, "Col", "3");
        writeMissingAttribute(route.getSymbol(), writer, "Zoom", "1");
        writeMissingAttribute(route.getSymbol(), writer, "Size", "106");
        writeMissingAttribute(route.getSymbol(), writer, "Art", "1");

        writer.println(OvlSection.POSITION_COUNT + NAME_VALUE_SEPARATOR + (endIndex - startIndex));
        List<Wgs84Position> positions = route.getPositions();
        int index = 0;
        for (int i = startIndex; i < endIndex; i++) {
            Wgs84Position position = positions.get(i);
            writer.println(OvlSection.X_POSITION + index + NAME_VALUE_SEPARATOR + position.getLongitude());
            writer.println(OvlSection.Y_POSITION + index + NAME_VALUE_SEPARATOR + position.getLatitude());
            index++;
        }
        writer.println(OvlSection.TEXT + NAME_VALUE_SEPARATOR + route.getName());
    }

    private void writeOverlay(OvlRoute route, PrintWriter writer, int symbolCount) {
        writer.println(SECTION_PREFIX + OVERLAY_TITLE + SECTION_POSTFIX);
        writeSection(route.getOverlay(), writer, Arrays.asList(SYMBOL_COUNT));
        writer.println(SYMBOL_COUNT + NAME_VALUE_SEPARATOR + symbolCount);
    }

    private void writeMapLage(OvlRoute route, PrintWriter writer) {
        writer.println(SECTION_PREFIX + MAPLAGE_TITLE + SECTION_POSTFIX);
        writeSection(route.getMapLage(), writer, Arrays.asList(CREATOR));
        // Top. Karte 1:50.000 Nieders.
        writeMissingAttribute(route.getMapLage(), writer, "MapName", "Top. Karte 1:50000 Sh/HH");
        writeMissingAttribute(route.getMapLage(), writer, "DimmFc", "100");
        writeMissingAttribute(route.getMapLage(), writer, "ZoomFc", "100");
        Wgs84Position center = Calculation.center(route.getPositions());
        writeMissingAttribute(route.getMapLage(), writer, "CenterLat", Conversion.formatDoubleAsString(center.getLatitude()));
        writeMissingAttribute(route.getMapLage(), writer, "CenterLong", Conversion.formatDoubleAsString(center.getLongitude()));
        writer.println(CREATOR + NAME_VALUE_SEPARATOR + GENERATED_BY);
    }

    public void write(OvlRoute route, PrintWriter writer, int startIndex, int endIndex, boolean numberPositionNames) {
        writeSymbol(route, writer, startIndex, endIndex, 1);
        writeOverlay(route, writer, 1);
        writeMapLage(route, writer);
    }

    public void write(List<OvlRoute> routes, File target) throws IOException {
        PrintWriter writer = new PrintWriter(target, DEFAULT_ENCODING);
        try {
            int symbols = 0;
            for (OvlRoute route : routes) {
                writeSymbol(route, writer, 0, route.getPositionCount(), ++symbols);
            }
            // stupid logic: the first route written determines the properties of the Overlays and MapLage sections
            OvlRoute first = routes.size() > 0 ? routes.get(0) : null;
            if (first != null) {
                writeOverlay(first, writer, symbols);
                writeMapLage(first, writer);
            }
        }
        finally {
            writer.flush();
            writer.close();
        }
    }
}
