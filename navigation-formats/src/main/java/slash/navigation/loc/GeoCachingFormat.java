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
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.util.logging.Logger;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static java.nio.charset.StandardCharsets.UTF_16BE;
import static java.nio.charset.StandardCharsets.UTF_16LE;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.singletonList;
import static slash.navigation.base.RouteCharacteristics.Waypoints;
import static slash.navigation.gpsbabel.GpsBabelText.decodeUtf8OrCp1252;
import static slash.navigation.gpsbabel.GpsBabelText.stripBom;

/**
 * Reads and writes Geocaching.com/EasyGPS (.loc) files.
 *
 * @author Christian Pesch
 */

public class GeoCachingFormat extends GpxBackedNavigationFormat {
    private static final Logger log = Logger.getLogger(GeoCachingFormat.class.getName());
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
            positions = parse(buffer.toByteArray());
        } catch (XMLStreamException | RuntimeException e) {
            // GPSBabel 1.5.4 aborts with a fatal error: the parser sees a format that declines the file
            throw new IOException("Cannot read Geocaching.com/EasyGPS file: " + e.getMessage(), e);
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

    private static final Pattern ENCODING = Pattern.compile("^\\s*<\\?xml[^>]*encoding\\s*=\\s*[\"']([A-Za-z0-9._:-]+)[\"']");

    /**
     * Decodes the file leniently (decision D22): a UTF-16 byte order mark selects UTF-16, an encoding declared
     * in the XML declaration is used when Java knows it, undecodable bytes become U+FFFD; without a declaration,
     * with a declared UTF-8 or an unknown encoding the text is read as UTF-8 and, if that fails, as Windows-1252,
     * which ITN Converter and other tools write without declaring it. Characters that are illegal in XML are
     * replaced by a space before parsing, as the GPSBabel path did with its IllegalCharacterFilterInputStream.
     */
    static String decode(byte[] bytes) {
        String text;
        if (bytes.length >= 2 && bytes[0] == (byte) 0xFF && bytes[1] == (byte) 0xFE)
            text = new String(bytes, 2, bytes.length - 2, UTF_16LE);
        else if (bytes.length >= 2 && bytes[0] == (byte) 0xFE && bytes[1] == (byte) 0xFF)
            text = new String(bytes, 2, bytes.length - 2, UTF_16BE);
        else {
            byte[] content = stripBom(bytes);
            Charset charset = null;
            Matcher matcher = ENCODING.matcher(new String(content, 0, Math.min(content.length, 200), ISO_8859_1));
            if (matcher.find()) {
                try {
                    charset = Charset.forName(matcher.group(1));
                } catch (IllegalArgumentException e) {
                    // an unknown encoding: guess as if none was declared
                }
            }
            text = charset == null || UTF_8.equals(charset) ? decodeUtf8OrCp1252(content) : new String(content, charset);
        }
        if (!text.isEmpty() && text.charAt(0) == '\uFEFF')
            text = text.substring(1);
        return stripIllegalXmlCharacters(text);
    }

    private List<GpxPosition> parse(byte[] bytes) throws XMLStreamException {
        XMLStreamReader reader = createFactory().createXMLStreamReader(new StringReader(decode(bytes)));
        List<GpxPosition> result = new ArrayList<>();
        try {
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
                        latitude = null;
                        longitude = null;
                        id = null;
                        text = null;
                    } else if (current.equals("/loc/waypoint/name")) {
                        id = reader.getAttributeValue(null, "id");
                        text = reader.getElementText();
                        path.remove(path.size() - 1);
                    } else if (current.equals("/loc/waypoint/coord")) {
                        latitude = parseCoordinate(reader.getAttributeValue(null, "lat"), 90.0);
                        longitude = parseCoordinate(reader.getAttributeValue(null, "lon"), 180.0);
                    }
                } else if (event == XMLStreamConstants.END_ELEMENT) {
                    if (inWaypoint && path.size() == 2 && LOC.equals(path.get(0)) && WAYPOINT.equals(path.get(1))) {
                        // decision D23: a waypoint without valid coordinates is skipped, GPSBabel puts it at 0/0
                        if (latitude != null && longitude != null)
                            // GPSBabel: there is no 'unknown' altitude, geo.cc sets 0
                            result.add(new GpxPosition(longitude, latitude, 0.0, null, null,
                                    asWayPointDescription(emptyToNull(id), emptyToNull(text))));
                        else
                            log.warning("Skipped waypoint " + id + " without valid coordinates");
                        inWaypoint = false;
                    }
                    path.remove(path.size() - 1);
                }
            }
            return result;
        } catch (XMLStreamException | RuntimeException e) {
            // decision D19: keep the waypoints read before the XML broke
            if (!result.isEmpty()) {
                log.warning("Read " + result.size() + " waypoints before a broken part of the file: " + e.getMessage());
                return result;
            }
            throw e;
        } finally {
            reader.close();
        }
    }

    private String asWayPointDescription(String name, String description) {
        return asDescription(name, description);
    }

    private static final Pattern NUMBER = Pattern.compile("[+-]?(\\d+(\\.\\d*)?|\\.\\d+)([eE][+-]?\\d+)?");

    /**
     * A coordinate in decimal degrees; a decimal comma is accepted. Null for a missing, non-numeric,
     * non-finite or out of range value.
     */
    static Double parseCoordinate(String value, double maximum) {
        if (value == null)
            return null;
        String number = value.trim();
        if (number.indexOf('.') == -1)
            number = number.replace(',', '.');
        if (!NUMBER.matcher(number).matches())
            return null;
        double result = Double.parseDouble(number);
        return Double.isFinite(result) && Math.abs(result) <= maximum ? result : null;
    }

    private static String emptyToNull(String value) {
        return value == null || value.isEmpty() ? null : value;
    }

    // writing

    public void write(GpxRoute route, OutputStream target, int startIndex, int endIndex) throws IOException {
        StringBuilder buffer = new StringBuilder();
        buffer.append("<?xml version=\"1.0\"?>");
        buffer.append("<loc version=\"1.0\" src=\"EasyGPS\">\n");
        List<GpxPosition> positions = route.getPositions();
        for (int i = startIndex; i < endIndex; i++) {
            GpxPosition position = positions.get(i);
            if (position.getLatitude() == null || position.getLongitude() == null)
                continue;

            String description = position.getDescription();
            buffer.append("<waypoint>\n");
            buffer.append("<name id=\"").append(escapeAttribute(clean(asName(description)))).append("\">");
            // GPSBabel 1.5.4 uses the name as description if the description is empty
            String name = clean(asName(description)), desc = clean(asDesc(description));
            buffer.append("<![CDATA[").append(escapeCData(desc.isEmpty() ? name : desc)).append("]]></name>\n");
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
