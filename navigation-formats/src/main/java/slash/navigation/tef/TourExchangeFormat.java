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

    Copyright (C) 2005 Olaf Klein, o.b.klein@gpsbabel.org
    Copyright (C) 2002-2014 Robert Lipe, robertlipe+source@gpsbabel.org

    Translated to Java from GPSBabel tef_xml.cc (gpsbabel_1_8_0, 1.5.4 for fix_notes) for RouteConverter, 2026; modified.
*/

package slash.navigation.tef;

import slash.common.type.CompactCalendar;
import slash.navigation.base.GpxBackedNavigationFormat;
import slash.navigation.common.NavigationPosition;
import slash.navigation.gpx.Gpx10Format;
import slash.navigation.gpx.GpxPosition;
import slash.navigation.gpx.GpxRoute;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import static java.lang.Math.max;
import static java.lang.Math.round;
import static java.math.RoundingMode.HALF_UP;
import static java.nio.charset.StandardCharsets.UTF_8;
import static slash.navigation.base.RouteCharacteristics.Route;
import static slash.navigation.base.RouteCharacteristics.Waypoints;

/**
 * Reads and writes Map &amp; Guide Tour Exchange Format (.tef) files.
 *
 * The reader is a port of GPSBabel's tef_xml.cc; the writer has no GPSBabel counterpart.
 *
 * @author Christian Pesch
 */

public class TourExchangeFormat extends GpxBackedNavigationFormat {
    private static final String ROOT = "TEF";
    private static final String COMMENT = "TourExchangeFormat";
    private static final DateTimeFormatter TIME_STAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
    private static final DateTimeFormatter CREATION_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssxxx");

    public String getExtension() {
        return ".tef";
    }

    public String getName() {
        return "Tour Exchange Format (*" + getExtension() + ")";
    }

    public boolean isSupportsMultipleRoutes() {
        return false; // just guesses
    }

    // reading

    private static final class Item {
        private String shortname, description;
        private double longitude, latitude;
        private Double altitude;
        private boolean via;
    }

    private static boolean is(String name, String expected) {
        return name.equalsIgnoreCase(expected);
    }

    private static double toDouble(String value) {
        try {
            double result = Double.parseDouble(value.trim());
            return Double.isNaN(result) || Double.isInfinite(result) ? 0.0 : result;
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    // tef_read_comma_float: only the first comma is replaced
    private static double toDoubleWithComma(String value) {
        return toDouble(value.replaceFirst(",", "."));
    }

    private static int toUnsignedInt(String value) {
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static String emptyToNull(String string) {
        return string == null || string.isEmpty() ? null : string;
    }

    private GpxPosition asPosition(Item item, double version, String prefix, int index) {
        // waypoint_final(): before version 2 the description is kept in the notes
        String description = item.description, notes = null;
        if (version < 2) {
            notes = description;
            description = null;
        }
        // the GPX writer of GPSBabel writes the notes into <desc> and uses the description if there are no notes
        String desc = emptyToNull(notes) != null ? notes : description;
        // fix_notes() is disabled in tef_xml.cc (#if OMG) of 1.8.0 and 1.5.4: notes stay as they are
        // GPSBabel 1.5.4 (observed in its output, not in the source) names unnamed items: via items without any
        // description WPT<nnn> (waypoint number, shared with their route copy), other route points RPT<nnn>
        String shortname = emptyToNull(item.shortname);
        if (shortname == null && (prefix.equals("WPT") ? emptyToNull(desc) == null : !item.via))
            shortname = String.format("%s%03d", prefix, index);
        String text = asDescription(shortname, emptyToNull(desc));
        return new GpxPosition(item.longitude, item.latitude, item.altitude, null, null, stripIllegalXmlCharacters(text));
    }

    protected List<GpxRoute> readRoutes(InputStream source) throws IOException {
        byte[] bytes = source.readAllBytes();
        try {
            return parse(bytes);
        } catch (XMLStreamException | RuntimeException e) {
            return null;
        }
    }

    private List<GpxRoute> parse(byte[] bytes) throws XMLStreamException {
        XMLInputFactory factory = XMLInputFactory.newInstance();
        factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
        factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
        XMLStreamReader reader = factory.createXMLStreamReader(new ByteArrayInputStream(bytes));
        try {
            List<String> path = new ArrayList<>();
            double version = 1.5;
            int itemCount = -1, waypoints = 0;
            Item item = null;
            boolean hasRoute = false;
            String routeName = null, routeDescription = null;
            List<GpxPosition> wayPositions = new ArrayList<>(), routePositions = new ArrayList<>();

            while (reader.hasNext()) {
                int event = reader.next();
                if (event == XMLStreamConstants.START_ELEMENT) {
                    String name = reader.getLocalName();
                    if (path.isEmpty() && !ROOT.equals(name))
                        return null;
                    path.add(name);
                    String where = String.join("/", path);

                    if ("TEF".equals(where)) {
                        boolean valid = false;
                        for (int i = 0; i < reader.getAttributeCount(); i++) {
                            String attribute = reader.getAttributeLocalName(i);
                            if (is(attribute, "Comment")) {
                                if (is(reader.getAttributeValue(i), COMMENT))
                                    valid = true;
                            } else if (is(attribute, "Version"))
                                version = toDouble(reader.getAttributeValue(i));
                        }
                        if (!valid)
                            return null;
                    } else if ("TEF/Header".equals(where)) {
                        hasRoute = true;
                        routeName = null;
                        routeDescription = null;
                        for (int i = 0; i < reader.getAttributeCount(); i++) {
                            String attribute = reader.getAttributeLocalName(i);
                            if (is(attribute, "Name"))
                                routeName = reader.getAttributeValue(i).trim();
                            else if (is(attribute, "Software"))
                                routeDescription = reader.getAttributeValue(i).trim();
                        }
                    } else if ("TEF/WaypointList".equals(where)) {
                        for (int i = 0; i < reader.getAttributeCount(); i++) {
                            if (is(reader.getAttributeLocalName(i), "ItemCount"))
                                itemCount = toUnsignedInt(reader.getAttributeValue(i));
                        }
                    } else if ("TEF/WaypointList/Item".equals(where)) {
                        waypoints++;
                        item = new Item();
                        if (waypoints == 1 || waypoints == itemCount)
                            item.via = true;
                        for (int i = 0; i < reader.getAttributeCount(); i++) {
                            String attribute = reader.getAttributeLocalName(i);
                            String value = reader.getAttributeValue(i);
                            if (is(attribute, "SegDescription"))
                                item.shortname = value.trim();
                            else if (is(attribute, "PointDescription") || is(attribute, "Instruction"))
                                item.description = value.trim();
                            else if (is(attribute, "ViaStation") && is(value, "true"))
                                item.via = true;
                            else if (is(attribute, "Altitude"))
                                item.altitude = toDouble(value);
                        }
                    } else if ("TEF/WaypointList/Item/Point".equals(where) && item != null) {
                        for (int i = 0; i < reader.getAttributeCount(); i++) {
                            String attribute = reader.getAttributeLocalName(i);
                            if (attribute.equals("y"))
                                item.latitude = toDoubleWithComma(reader.getAttributeValue(i));
                            else if (attribute.equals("x"))
                                item.longitude = toDoubleWithComma(reader.getAttributeValue(i));
                        }
                    }

                } else if (event == XMLStreamConstants.END_ELEMENT) {
                    String where = String.join("/", path);
                    path.remove(path.size() - 1);

                    boolean listEnd = "TEF/WaypointList".equals(where);
                    if ("TEF/WaypointList/Item".equals(where) || listEnd) {
                        // waypoint_final()
                        if (item != null) {
                            int wayIndex = wayPositions.size() + 1;
                            if (item.via)
                                wayPositions.add(asPosition(item, version, "WPT", wayIndex));
                            if (hasRoute)
                                routePositions.add(item.via ? asPosition(item, version, "WPT", wayIndex) :
                                        asPosition(item, version, "RPT", routePositions.size() + 1));
                            item = null;
                        }
                        if (listEnd && waypoints != itemCount)
                            return null;
                    }
                }
            }

            List<GpxRoute> result = new ArrayList<>();
            if (!wayPositions.isEmpty())
                result.add(new GpxRoute(new Gpx10Format(), Waypoints, null, null, wayPositions));
            if (hasRoute) {
                String name = routeName == null || routeName.isEmpty() ? null : stripIllegalXmlCharacters(routeName);
                result.add(new GpxRoute(new Gpx10Format(), Route, name,
                        asDescription(stripIllegalXmlCharacters(routeDescription)), routePositions));
            }
            return result;
        } finally {
            reader.close();
        }
    }

    // writing

    private static String escape(String value) {
        StringBuilder buffer = new StringBuilder(value.length() + 16);
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '&':
                    buffer.append("&amp;");
                    break;
                case '<':
                    buffer.append("&lt;");
                    break;
                case '>':
                    buffer.append("&gt;");
                    break;
                case '"':
                    buffer.append("&quot;");
                    break;
                case '\n':
                    buffer.append("&#10;");
                    break;
                case '\r':
                    buffer.append("&#13;");
                    break;
                case '\t':
                    buffer.append("&#9;");
                    break;
                default:
                    buffer.append(c);
            }
        }
        return buffer.toString();
    }

    private static void attribute(StringBuilder buffer, String name, String value) {
        buffer.append(' ').append(name).append("=\"").append(escape(stripIllegalXmlCharacters(value))).append('"');
    }

    private static String formatCoordinate(double value) {
        return new BigDecimal(value).setScale(5, HALF_UP).stripTrailingZeros().toPlainString();
    }

    private static boolean hasTime(NavigationPosition position) {
        return position.getTime() != null && position.hasTime();
    }

    private static String formatTimeStamp(CompactCalendar time) {
        ZoneId zone = time.getTimeZoneId() != null ? ZoneId.of(time.getTimeZoneId()) : ZoneId.of("UTC");
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(time.getTimeInMillis()), zone).format(TIME_STAMP);
    }

    private static long seconds(NavigationPosition from, NavigationPosition to) {
        if (!hasTime(from) || !hasTime(to))
            return 0;
        return max(0, round((to.getTime().getTimeInMillis() - from.getTime().getTimeInMillis()) / 1000.0));
    }

    private static double distance(NavigationPosition from, NavigationPosition to) {
        Double distance = from.calculateDistance(to);
        return distance != null ? distance : 0.0;
    }

    private String routeName(GpxRoute route, List<GpxPosition> positions) {
        String name = route.getName() != null ? route.getName().trim() : "";
        if (name.isEmpty() && !positions.isEmpty()) {
            String first = positions.get(0).getDescription(), last = positions.get(positions.size() - 1).getDescription();
            if (first != null && last != null && !first.trim().isEmpty() && !last.trim().isEmpty())
                name = "von " + first.trim() + " nach " + last.trim();
        }
        return name;
    }

    public void write(GpxRoute route, OutputStream target, int startIndex, int endIndex) throws IOException {
        List<GpxPosition> positions = new ArrayList<>();
        List<GpxPosition> all = route.getPositions();
        for (int i = startIndex; i < endIndex; i++) {
            GpxPosition position = all.get(i);
            if (position.getLongitude() != null && position.getLatitude() != null)
                positions.add(position);
        }

        double total = 0.0;
        for (int i = 0; i < positions.size() - 1; i++)
            total += distance(positions.get(i), positions.get(i + 1));
        long duration = positions.isEmpty() ? 0 : seconds(positions.get(0), positions.get(positions.size() - 1));

        StringBuilder buffer = new StringBuilder();
        buffer.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        buffer.append("<TEF Comment=\"TourExchangeFormat\" Version=\"2.0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n");
        buffer.append("<Header");
        attribute(buffer, "Datasource", "RoutePlanningProgramm");
        attribute(buffer, "Name", routeName(route, positions));
        attribute(buffer, "Software", getCreator());
        attribute(buffer, "CreationTime", OffsetDateTime.now().format(CREATION_TIME));
        buffer.append(">\n");
        buffer.append("  <Setup CoordinateSystem=\"GeoDecimal\" Measure=\"metric\"/>\n");
        buffer.append("  <Route Distance=\"").append(round(total)).append("\" Duration=\"").append(duration).append("\"/>\n");
        buffer.append("</Header>\n");
        buffer.append("<WaypointList ItemCount=\"").append(positions.size()).append("\">\n");

        double cumulated = 0.0;
        for (int i = 0; i < positions.size(); i++) {
            GpxPosition position = positions.get(i);
            GpxPosition next = i + 1 < positions.size() ? positions.get(i + 1) : null;
            double segment = next != null ? distance(position, next) : 0.0;
            boolean first = i == 0, last = i == positions.size() - 1;

            buffer.append("<Item");
            Double elevation = position.getElevation();
            attribute(buffer, "Altitude", Long.toString(elevation != null ? round(elevation) : 0));
            attribute(buffer, "PointDist", Long.toString(round(cumulated)));
            if (position.getDescription() != null)
                attribute(buffer, "PointDescription", position.getDescription());
            attribute(buffer, "Prio", first ? "-2" : last ? "-4" : "4");
            attribute(buffer, "SegDist", Long.toString(round(segment)));
            attribute(buffer, "SegTime", Long.toString(next != null ? seconds(position, next) : 0));
            if (hasTime(position))
                attribute(buffer, "TimeStamp", formatTimeStamp(position.getTime()));
            if (!first && !last)
                attribute(buffer, "ViaStation", "true");
            buffer.append("><Point x=\"").append(formatCoordinate(position.getLongitude()))
                    .append("\" y=\"").append(formatCoordinate(position.getLatitude())).append("\"/></Item>\n");
            cumulated += segment;
        }

        buffer.append("</WaypointList>\n");
        buffer.append("</TEF>\n");

        Writer writer = new OutputStreamWriter(target, UTF_8);
        writer.write(buffer.toString());
        writer.flush();
    }
}
