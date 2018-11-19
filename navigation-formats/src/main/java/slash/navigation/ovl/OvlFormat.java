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

import slash.navigation.base.*;
import slash.navigation.common.BoundingBox;
import slash.navigation.common.NavigationPosition;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Collections.singletonList;
import static slash.common.io.Transfer.ISO_LATIN1_ENCODING;
import static slash.navigation.base.RouteCharacteristics.Route;
import static slash.navigation.base.RouteCharacteristics.Track;
import static slash.navigation.common.NavigationConversion.formatPositionAsString;
import static slash.navigation.ovl.OvlSection.GROUP;
import static slash.navigation.ovl.OvlSection.TEXT;

/**
 * Reads and writes Top50 OVL ASCII (.ovl) files.
 *
 * @author Christian Pesch
 */

public class OvlFormat extends IniFileFormat<OvlRoute> implements MultipleRoutesFormat<OvlRoute> {
    static final String SYMBOL_TITLE = "Symbol";
    static final String OVERLAY_TITLE = "Overlay";
    static final String MAPLAGE_TITLE = "MapLage";

    private static final String SYMBOL_COUNT = "Symbols";
    // static final String ROUTE_NAME = "RouteName";
    private static final String CREATOR = "Creator";

    private static final Pattern SECTION_TITLE_PATTERN = Pattern.
            compile("\\" + SECTION_PREFIX + "(" + SYMBOL_TITLE + " \\d+|" + OVERLAY_TITLE + "|" +
                    MAPLAGE_TITLE + ")\\" + SECTION_POSTFIX);

    public String getExtension() {
        return ".ovl";
    }

    public String getName() {
        return "Top50 OVL ASCII (*" + getExtension() + ")";
    }

    public int getMaximumPositionCount() {
        return UNLIMITED_MAXIMUM_POSITION_COUNT;
    }

    public boolean isSupportsMultipleRoutes() {
        return true;
    }

    public boolean isWritingRouteCharacteristics() {
        return false;
    }

    @SuppressWarnings({"unchecked"})
    public <P extends NavigationPosition> OvlRoute createRoute(RouteCharacteristics characteristics, String name, List<P> positions) {
        return new OvlRoute(characteristics, name, (List<Wgs84Position>) positions);
    }

    public void read(BufferedReader reader, String encoding, ParserContext<OvlRoute> context) throws IOException {
        List<OvlSection> sections = new ArrayList<>();
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
                    return;
                } else
                    current.put(parseName(line), parseValue(line));
            }
        }

        if (hasValidSections(sections))
            context.appendRoutes(extractRoutes(sections));
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
        if (!existsSection(sections, OVERLAY_TITLE))
            return false;

        OvlSection overlay = findSection(sections, OVERLAY_TITLE);
        if(overlay == null)
            return false;

        int symbolCount = getSymbolCount(overlay);
        if (symbolCount == 0)
            return false;

        int sectionCount = 0;
        for (int i = 0; i < symbolCount; i++) {
            if (existsSection(sections, SYMBOL_TITLE + " " + (i + 1)))
                sectionCount++;
        }
        return sectionCount > 0;
    }

    private int getSymbolCount(OvlSection overlay) {
        String symbols = overlay.get(SYMBOL_COUNT);
        return symbols != null ? Integer.parseInt(symbols) : 0;
    }

    private List<OvlRoute> extractRoutes(List<OvlSection> sections) {
        List<OvlRoute> result = new ArrayList<>();

        OvlSection mapLage = findSection(sections, MAPLAGE_TITLE);
        if(mapLage == null)
            mapLage = new OvlSection(MAPLAGE_TITLE);

        OvlSection overlay = findSection(sections, OVERLAY_TITLE);
        int symbolCount = overlay != null ? getSymbolCount(overlay) : 0;

        // process all sections with same group into one route
        Map<Integer, List<OvlSection>> sectionsByGroup = new LinkedHashMap<>();
        List<OvlSection> sectionsWithoutGroup = new ArrayList<>();
        for (int i = 0; i < symbolCount; i++) {
            OvlSection section = findSection(sections, SYMBOL_TITLE + " " + (i + 1));
            if (section != null) {
                Integer group = section.getGroup();
                if (group != null) {
                    List<OvlSection> groupedSections = sectionsByGroup.get(group);
                    if (groupedSections == null) {
                        groupedSections = new ArrayList<>();
                        sectionsByGroup.put(group, groupedSections);
                    }
                    groupedSections.add(section);
                } else
                    sectionsWithoutGroup.add(section);
            }
        }

        for (OvlSection section : sectionsWithoutGroup) {
            OvlRoute route = extractRoute(section, overlay, mapLage);
            result.add(route);
        }

        for (List<OvlSection> sectionList : sectionsByGroup.values()) {
            OvlRoute route = extractRoute(sectionList, overlay, mapLage);
            result.add(route);
        }
        return result;
    }

    private RouteCharacteristics estimateCharacteristics(int positionCount) {
        return positionCount > 100 ? Track : Route;
    }

    private OvlRoute extractRoute(OvlSection symbol, OvlSection overlay, OvlSection mapLage) {
        List<Wgs84Position> positions = new ArrayList<>();
        int positionCount = symbol.getPositionCount();
        for (int i = 0; i < positionCount; i++) {
            positions.add(symbol.getPosition(i));
        }
        symbol.removePositions();
        return new OvlRoute(this, estimateCharacteristics(positionCount), symbol.getTitle(), symbol, overlay, mapLage, positions);
    }

    private OvlRoute extractRoute(List<OvlSection> symbols, OvlSection overlay, OvlSection mapLage) {
        List<Wgs84Position> positions = new ArrayList<>();
        for (OvlSection symbol : symbols) {
            int positionCount = symbol.getPositionCount();
            for (int i = 0; i < positionCount; i++) {
                positions.add(symbol.getPosition(i));
            }
            symbol.removePositions();
        }
        OvlSection symbol = symbols.size() > 0 ? symbols.get(0) : null;
        return new OvlRoute(this, estimateCharacteristics(positions.size()), mapLage.getTitle(), symbol, overlay, mapLage, positions);
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
        writer.println(GROUP + NAME_VALUE_SEPARATOR + symbolIndex);

        writeSection(route.getSymbol(), writer, singletonList(TEXT));
        writeMissingAttribute(route.getSymbol(), writer, "Typ", "3");
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
        writer.println(TEXT + NAME_VALUE_SEPARATOR + asRouteName(route.getName()));
    }

    private void writeOverlay(OvlRoute route, PrintWriter writer, int symbolCount) {
        writer.println(SECTION_PREFIX + OVERLAY_TITLE + SECTION_POSTFIX);
        writeSection(route.getOverlay(), writer, singletonList(SYMBOL_COUNT));
        writer.println(SYMBOL_COUNT + NAME_VALUE_SEPARATOR + symbolCount);
    }

    private void writeMapLage(OvlRoute route, PrintWriter writer) {
        writer.println(SECTION_PREFIX + MAPLAGE_TITLE + SECTION_POSTFIX);
        writeSection(route.getMapLage(), writer, singletonList(CREATOR));
        // Top. Karte 1:50.000 Hessen
        // Top. Karte 1:50.000 Nieders.
        // Top. Karte 1:50000 Sh/HH
        writeMissingAttribute(route.getMapLage(), writer, "MapName", "Bundesrepublik 1:1 Mio");
        writeMissingAttribute(route.getMapLage(), writer, "DimmFc", "100");
        writeMissingAttribute(route.getMapLage(), writer, "ZoomFc", "100");
        NavigationPosition center = new BoundingBox(route.getPositions()).getCenter();
        writeMissingAttribute(route.getMapLage(), writer, "CenterLat", formatPositionAsString(center.getLatitude()));
        writeMissingAttribute(route.getMapLage(), writer, "CenterLong", formatPositionAsString(center.getLongitude()));
        writer.println(CREATOR + NAME_VALUE_SEPARATOR + GENERATED_BY);
    }

    public void write(OvlRoute route, PrintWriter writer, int startIndex, int endIndex) {
        writeSymbol(route, writer, startIndex, endIndex, 1);
        writeOverlay(route, writer, 1);
        writeMapLage(route, writer);
    }

    public void write(List<OvlRoute> routes, OutputStream target) throws IOException {
        try(PrintWriter writer = new PrintWriter(new OutputStreamWriter(target, ISO_LATIN1_ENCODING))) {
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
    }
}
