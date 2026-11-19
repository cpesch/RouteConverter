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

    Copyright (C) 2002 Robert Lipe, robertlipe+source@gpsbabel.org
    Translated to Java from GPSBabel geo.cc (master 9c1e336) for RouteConverter, 2026; modified.
*/

package slash.navigation.loc;

import slash.navigation.base.GpxBackedNavigationFormat;
import slash.navigation.base.ParserContext;
import slash.navigation.gpx.GpxPosition;
import slash.navigation.gpx.GpxRoute;
import slash.navigation.gpx.Gpx10Format;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.singletonList;
import static slash.navigation.base.RouteCharacteristics.Waypoints;

/**
 * Reads and writes Geocaching.com/EasyGPS (.loc) files.
 *
 * @author Christian Pesch
 */

public class GeoCachingFormat extends GpxBackedNavigationFormat {
    private static final String LOC = "loc";
    private static final String WAYPOINT = "waypoint";

    public String getExtension() {
        return ".loc";
    }

    public String getName() {
        return "Geocaching.com/EasyGPS (*" + getExtension() + ")";
    }

    public boolean isSupportsMultipleRoutes() {
        return false;
    }

    // reading

    protected List<GpxRoute> readRoutes(InputStream source) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] bytes = new byte[8192];
        int count;
        while ((count = source.read(bytes)) != -1)
            buffer.write(bytes, 0, count);

        List<GpxPosition> positions;
        try {
            positions = parse(new ByteArrayInputStream(buffer.toByteArray()));
        } catch (XMLStreamException | RuntimeException e) {
            return null;
        }
        if (positions == null || positions.isEmpty())
            return null;
        return singletonList(new GpxRoute(new Gpx10Format(), Waypoints, null, null, positions));
    }

    private static XMLInputFactory createFactory() {
        XMLInputFactory factory = XMLInputFactory.newInstance();
        factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
        factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
        return factory;
    }

    private List<GpxPosition> parse(InputStream stream) throws XMLStreamException {
        XMLStreamReader reader = createFactory().createXMLStreamReader(stream);
        try {
            List<GpxPosition> result = new ArrayList<>();
            List<String> path = new ArrayList<>();
            Double latitude = null, longitude = null;
            String id = null, text = null;
            boolean inWaypoint = false;

            while (reader.hasNext()) {
                int event = reader.next();
                if (event == XMLStreamConstants.START_ELEMENT) {
                    path.add(reader.getLocalName());
                    String current = "/" + String.join("/", path);
                    if (current.equals("/loc/waypoint")) {
                        inWaypoint = true;
                        // GPSBabel: there is no 'unknown' altitude, geo.cc sets 0
                        latitude = 0.0;
                        longitude = 0.0;
                        id = null;
                        text = null;
                    } else if (current.equals("/loc/waypoint/name")) {
                        id = reader.getAttributeValue(null, "id");
                        text = reader.getElementText();
                        path.remove(path.size() - 1);
                    } else if (current.equals("/loc/waypoint/coord")) {
                        latitude = parseDouble(reader.getAttributeValue(null, "lat"));
                        longitude = parseDouble(reader.getAttributeValue(null, "lon"));
                    }
                } else if (event == XMLStreamConstants.END_ELEMENT) {
                    if (inWaypoint && path.size() == 2 && LOC.equals(path.get(0)) && WAYPOINT.equals(path.get(1))) {
                        result.add(new GpxPosition(longitude, latitude, 0.0, null, null,
                                asDescription(emptyToNull(id), emptyToNull(text))));
                        inWaypoint = false;
                    }
                    path.remove(path.size() - 1);
                }
            }
            return result;
        } finally {
            reader.close();
        }
    }

    private static Double parseDouble(String value) {
        if (value == null)
            return 0.0;
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    private static String emptyToNull(String value) {
        return value == null || value.isEmpty() ? null : value;
    }

    // writing

    public void write(GpxRoute route, OutputStream target, int startIndex, int endIndex) throws IOException {
        StringBuilder buffer = new StringBuilder();
        buffer.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        buffer.append("<loc version=\"1.0\" src=\"EasyGPS\">\n");
        List<GpxPosition> positions = route.getPositions();
        for (int i = startIndex; i < endIndex; i++) {
            GpxPosition position = positions.get(i);
            if (position.getLatitude() == null || position.getLongitude() == null)
                continue;

            String description = position.getDescription();
            buffer.append("<waypoint>\n");
            buffer.append("<name id=\"").append(escapeAttribute(clean(asName(description)))).append("\">");
            buffer.append("<![CDATA[").append(escapeCData(clean(asDesc(description)))).append("]]></name>\n");
            buffer.append("<coord lat=\"").append(number(position.getLatitude()))
                    .append("\" lon=\"").append(number(position.getLongitude())).append("\"/>\n");
            // GPSBabel writes the icon description of the waypoint; none is known for a GPX waypoint without <sym>
            buffer.append("<type></type>\n");
            buffer.append("</waypoint>\n");
        }
        buffer.append("</loc>\n");
        target.write(buffer.toString().getBytes(UTF_8));
        target.flush();
    }

    private static String number(Double value) {
        return String.format(Locale.ROOT, "%.6f", value);
    }

    private static String clean(String value) {
        String result = stripIllegalXmlCharacters(value);
        return result == null ? "" : result;
    }

    private static String escapeAttribute(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")
                .replace("\n", "&#10;").replace("\r", "&#13;").replace("\t", "&#9;");
    }

    private static String escapeCData(String value) {
        return value.replace("]]>", "]]]]><![CDATA[>");
    }
}
