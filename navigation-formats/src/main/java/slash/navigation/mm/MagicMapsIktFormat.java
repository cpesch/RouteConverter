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

package slash.navigation.mm;

import slash.navigation.base.MultipleRoutesFormat;
import slash.navigation.base.ParserContext;
import slash.navigation.base.RouteCharacteristics;
import slash.navigation.base.Wgs84Position;
import slash.navigation.base.XmlNavigationFormat;
import slash.navigation.common.NavigationPosition;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.Characters;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.singletonList;
import static slash.common.io.Transfer.UTF8_ENCODING;
import static slash.common.io.Transfer.formatIntAsString;
import static slash.common.io.Transfer.parseDouble;
import static slash.common.io.Transfer.trim;
import static slash.navigation.base.RouteCalculations.asWgs84Position;
import static slash.navigation.common.NavigationConversion.formatPositionAsString;

/**
 * Reads and writes MagicMaps Project (.ikt) files.
 *
 * @author Christian Pesch
 */

public class MagicMapsIktFormat extends XmlNavigationFormat<MagicMapsIktRoute> implements MultipleRoutesFormat<MagicMapsIktRoute> {
    private static final String ROOT_ELEMENT = "Root";
    private static final String FILE_FORMAT_ATTRIBUTE = "FileFormat";
    private static final String FILE_FORMAT = "IK3D-Project-File";
    private static final String HEADER_ELEMENT = "Header";
    private static final String PROJECT_NAME_ELEMENT = "Projectname";
    private static final String DESCRIPTION_ELEMENT = "description";
    private static final String CONTENT_ELEMENT = "Content";
    private static final String GEO_OBJECTS_ELEMENT = "MMGeoObjects";
    private static final String COUNT_ELEMENT = "count";
    private static final String GEO_OBJECT_ELEMENT = "MMGeoObject";
    private static final String GEO_OBJECT_TYPE_ELEMENT = "GeoObjectType";
    private static final String NAME_ELEMENT = "Name";
    private static final String PATH_DRAW_TYPE_ELEMENT = "PathDrawType";
    private static final String PATH_POINTS_ELEMENT = "PathPoints";
    private static final String POINT_ELEMENT = "Point";
    private static final String GEO_POSITION_ELEMENT = "GeoPosition";
    private static final String X_ATTRIBUTE = "X";
    private static final String Y_ATTRIBUTE = "Y";

    public String getExtension() {
        return ".ikt";
    }

    public String getName() {
        return "MagicMaps Project (*" + getExtension() + ")";
    }

    public boolean isSupportsMultipleRoutes() {
        return true;
    }

    public boolean isWritingRouteCharacteristics() {
        return false;
    }

    @SuppressWarnings("unchecked")
    public <P extends NavigationPosition> MagicMapsIktRoute createRoute(RouteCharacteristics characteristics, String name, List<P> positions) {
        return new MagicMapsIktRoute(name, null, (List<Wgs84Position>) positions);
    }

    private Double extractValue(StartElement startElement, String attributeName) {
        Attribute attribute = startElement.getAttributeByName(new QName(attributeName));
        if (attribute == null)
            return null;
        return parseDouble(attribute.getValue());
    }

    private String extractValue(Characters chars, String value) {
        String append = trim(chars.getData());
        if (append == null)
            return value;
        if (value == null)
            return append;
        return value + append;
    }

    private Wgs84Position processPosition(StartElement startElement) {
        return asWgs84Position(extractValue(startElement, X_ATTRIBUTE), extractValue(startElement, Y_ATTRIBUTE), null);
    }

    private List<MagicMapsIktRoute> process(XMLEventReader eventReader) throws XMLStreamException {
        boolean hasValidRoot = false, nextIsProjectName = false, nextIsDescription = false, nextIsName = false;
        String projectName = null, description = null, name = null;
        List<MagicMapsIktRoute> routes = new ArrayList<>();
        List<Wgs84Position> positions = null;

        while (eventReader.hasNext()) {
            XMLEvent event = eventReader.nextEvent();
            if (event.getEventType() == XMLStreamConstants.CHARACTERS) {
                Characters chars = event.asCharacters();

                if (nextIsProjectName)
                    projectName = extractValue(chars, projectName);
                if (nextIsDescription)
                    description = extractValue(chars, description);
                if (nextIsName)
                    name = extractValue(chars, name);
            }
            if (event.getEventType() == XMLStreamConstants.START_ELEMENT) {
                StartElement startElement = event.asStartElement();
                String elementName = startElement.getName().getLocalPart();

                // detect by having first element to be <Root FileFormat="IK3D-Project-File">
                if (!hasValidRoot) {
                    if (!(ROOT_ELEMENT.equals(elementName)))
                        return null;
                    Attribute fileFormat = startElement.getAttributeByName(new QName(FILE_FORMAT_ATTRIBUTE));
                    if (fileFormat == null || !FILE_FORMAT.equals(fileFormat.getValue()))
                        return null;

                    hasValidRoot = true;

                } else if (elementName.startsWith(GEO_OBJECT_ELEMENT)) {
                    name = null;
                    positions = new ArrayList<>();
                } else if (elementName.startsWith(GEO_POSITION_ELEMENT)) {
                    positions.add(processPosition(startElement));
                }

                nextIsProjectName = PROJECT_NAME_ELEMENT.equals(elementName);
                nextIsDescription = DESCRIPTION_ELEMENT.equals(elementName);
                nextIsName = NAME_ELEMENT.equals(elementName);
            }
            if (event.getEventType() == XMLStreamConstants.END_ELEMENT) {
                EndElement endElement = event.asEndElement();
                String elementName = endElement.getName().getLocalPart();

                if (ROOT_ELEMENT.equals(elementName)) {
                    return routes;
                } else //noinspection StatementWithEmptyBody
                    if (elementName.startsWith(GEO_OBJECTS_ELEMENT)) {
                } else if (elementName.startsWith(GEO_OBJECT_ELEMENT)) {
                    if (name == null)
                        name = projectName;
                    routes.add(new MagicMapsIktRoute(this, name, asDescription(description), positions));
                }
            }
        }
        return null;
    }

    public void read(InputStream source, ParserContext<MagicMapsIktRoute> context) throws Exception {
        XMLInputFactory factory = XMLInputFactory.newInstance();
        XMLEventReader eventReader = factory.createXMLEventReader(source, UTF8_ENCODING);
        context.appendRoutes(process(eventReader));
    }

    private void writeHeader(String name, String description, XMLEventWriter writer, XMLEventFactory eventFactory) throws XMLStreamException {
        writer.add(eventFactory.createStartDocument(UTF8_ENCODING, "1.0", true));
        List<Attribute> rootAttributes = new ArrayList<>();
        rootAttributes.add(eventFactory.createAttribute(FILE_FORMAT_ATTRIBUTE, FILE_FORMAT));
        writer.add(eventFactory.createStartElement(new QName(ROOT_ELEMENT), rootAttributes.iterator(), null));

        writer.add(eventFactory.createStartElement(new QName(HEADER_ELEMENT), null, null));
        writer.add(eventFactory.createStartElement(new QName(PROJECT_NAME_ELEMENT), null, null));
        writer.add(eventFactory.createCharacters(name));
        writer.add(eventFactory.createEndElement(new QName(PROJECT_NAME_ELEMENT), null));
        writer.add(eventFactory.createStartElement(new QName(DESCRIPTION_ELEMENT), null, null));
        writer.add(eventFactory.createCData(description));
        writer.add(eventFactory.createEndElement(new QName(DESCRIPTION_ELEMENT), null));
        writer.add(eventFactory.createEndElement(new QName(HEADER_ELEMENT), null));

        writer.add(eventFactory.createStartElement(new QName(CONTENT_ELEMENT), null, null));
        writer.add(eventFactory.createStartElement(new QName(GEO_OBJECTS_ELEMENT), null, null));
    }

    private void writePosition(Wgs84Position position, int index, XMLEventWriter writer, XMLEventFactory eventFactory) throws XMLStreamException {
        writer.add(eventFactory.createStartElement(new QName(POINT_ELEMENT + "_" + index), null, null));

        List<Attribute> attributes = new ArrayList<>();
        attributes.add(eventFactory.createAttribute(X_ATTRIBUTE, formatPositionAsString(position.getLongitude())));
        attributes.add(eventFactory.createAttribute(Y_ATTRIBUTE, formatPositionAsString(position.getLatitude())));
        writer.add(eventFactory.createStartElement(new QName(GEO_POSITION_ELEMENT), attributes.iterator(), null));
        writer.add(eventFactory.createEndElement(new QName(GEO_POSITION_ELEMENT), null));

        writer.add(eventFactory.createEndElement(new QName(POINT_ELEMENT + "_" + index), null));
    }

    private void writeRoute(MagicMapsIktRoute route, int index, XMLEventWriter writer, XMLEventFactory eventFactory) throws XMLStreamException {
        writer.add(eventFactory.createStartElement(new QName(GEO_OBJECT_ELEMENT + "_" + index), null, null));

        writer.add(eventFactory.createStartElement(new QName(GEO_OBJECT_TYPE_ELEMENT), null, null));
        writer.add(eventFactory.createCharacters("1"));
        writer.add(eventFactory.createEndElement(new QName(GEO_OBJECT_TYPE_ELEMENT), null));

        writer.add(eventFactory.createStartElement(new QName(NAME_ELEMENT), null, null));
        writer.add(eventFactory.createCharacters(asRouteName(route.getName())));
        writer.add(eventFactory.createEndElement(new QName(NAME_ELEMENT), null));

        writer.add(eventFactory.createStartElement(new QName(PATH_DRAW_TYPE_ELEMENT), null, null));
        writer.add(eventFactory.createCharacters("5"));
        writer.add(eventFactory.createEndElement(new QName(PATH_DRAW_TYPE_ELEMENT), null));

        writer.add(eventFactory.createStartElement(new QName(PATH_POINTS_ELEMENT), null, null));

        List<Wgs84Position> positions = route.getPositions();
        writer.add(eventFactory.createStartElement(new QName(COUNT_ELEMENT), null, null));
        writer.add(eventFactory.createCharacters(formatIntAsString(positions.size())));
        writer.add(eventFactory.createEndElement(new QName(COUNT_ELEMENT), null));

        for (int i = 0; i < positions.size(); i++) {
            writePosition(positions.get(i), i, writer, eventFactory);
        }

        writer.add(eventFactory.createEndElement(new QName(PATH_POINTS_ELEMENT), null));
        writer.add(eventFactory.createEndElement(new QName(GEO_OBJECT_ELEMENT + "_" + index), null));
    }

    private void writeFooter(XMLEventWriter writer, XMLEventFactory eventFactory) throws XMLStreamException {
        writer.add(eventFactory.createEndElement(new QName(GEO_OBJECTS_ELEMENT), null));
        writer.add(eventFactory.createEndElement(new QName(CONTENT_ELEMENT), null));
        writer.add(eventFactory.createEndElement(new QName(ROOT_ELEMENT), null));
        writer.add(eventFactory.createEndDocument());
    }

    public void write(MagicMapsIktRoute route, OutputStream target, int startIndex, int endIndex) throws IOException {
        write(singletonList(route), target);
    }

    private String getProjectName(List<MagicMapsIktRoute> routes) {
        StringBuilder buffer = new StringBuilder();
        for (int i = 0; i < routes.size(); i++) {
            MagicMapsIktRoute route = routes.get(i);
            buffer.append(route.getName());
            if (i < routes.size() - 1)
                buffer.append(", ");
        }
        return buffer.toString();
    }

    private String getDescription(List<MagicMapsIktRoute> routes) {
        StringBuilder buffer = new StringBuilder();
        for (int i = 0; i < routes.size(); i++) {
            MagicMapsIktRoute route = routes.get(i);
            List<String> descriptions = route.getDescription();
            if (descriptions == null)
                continue;
            for (int j = 0; j < descriptions.size(); j++) {
                String description = descriptions.get(j);
                buffer.append(description);
                if (j < descriptions.size() - 1)
                    buffer.append(", ");
            }
            if (i < routes.size() - 1)
                buffer.append("; ");
        }
        return buffer.toString();
    }

    public void write(List<MagicMapsIktRoute> routes, OutputStream target) throws IOException {
        try {
            XMLEventFactory eventFactory = XMLEventFactory.newInstance();
            XMLOutputFactory output = XMLOutputFactory.newInstance();
            XMLEventWriter writer = output.createXMLEventWriter(target, UTF8_ENCODING);

            try {
                writeHeader(getProjectName(routes), getDescription(routes), writer, eventFactory);

                writer.add(eventFactory.createStartElement(new QName(COUNT_ELEMENT), null, null));
                writer.add(eventFactory.createCharacters(formatIntAsString(routes.size())));
                writer.add(eventFactory.createEndElement(new QName(COUNT_ELEMENT), null));

                for (int i = 0; i < routes.size(); i++) {
                    writeRoute(routes.get(i), i, writer, eventFactory);
                }

                writeFooter(writer, eventFactory);
            } finally {
                writer.flush();
                writer.close();
                target.flush();
                target.close();
            }
        } catch (XMLStreamException e) {
            throw new IOException("Error while marshalling: " + e, e);
        }
    }
}