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

package slash.navigation.kml;

import slash.common.type.CompactCalendar;
import slash.navigation.base.ParserContext;
import slash.navigation.base.RouteCharacteristics;
import slash.navigation.kml.binding22beta.*;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static slash.common.io.Transfer.trim;
import static slash.common.type.ISO8601.formatDate;
import static slash.navigation.base.RouteCharacteristics.Track;
import static slash.navigation.base.RouteCharacteristics.Waypoints;
import static slash.navigation.kml.KmlUtil.marshal22Beta;
import static slash.navigation.kml.KmlUtil.unmarshal22Beta;

/**
 * Reads and writes Google Earth 4.2 (.kml) files.
 *
 * @author Christian Pesch
 */

public class Kml22BetaFormat extends KmlFormat {

    public String getName() {
        return "Google Earth 4.2 (*" + getExtension() + ")";
    }

    public void read(InputStream source, ParserContext<KmlRoute> context) throws IOException {
        KmlType kmlType = unmarshal22Beta(source);
        process(kmlType, context);
    }

    protected void process(KmlType kmlType, ParserContext<KmlRoute> context) throws IOException {
        if (kmlType == null || kmlType.getAbstractFeatureGroup() == null)
            return;
        extractTracks(kmlType, context);
    }

    @SuppressWarnings({"UnusedDeclaration", "unchecked"})
    private <T> List<JAXBElement<T>> find(List<JAXBElement<? extends AbstractFeatureType>> elements, String name, Class<T> resultClass) {
        List<JAXBElement<T>> result = new ArrayList<>();
        if(elements != null) {
            for (JAXBElement<? extends AbstractFeatureType> element : elements) {
                if (name.equals(element.getName().getLocalPart()))
                    result.add((JAXBElement<T>) element);
            }
        }
        return result;
    }

    private void extractTracks(KmlType kmlType, ParserContext<KmlRoute> context) throws IOException {
        AbstractFeatureType feature = kmlType.getAbstractFeatureGroup().getValue();
        if (feature instanceof AbstractContainerType) {
            AbstractContainerType containerType = (AbstractContainerType) feature;
            List<JAXBElement<? extends AbstractFeatureType>> features = null;
            if (containerType instanceof FolderType)
                features = ((FolderType) containerType).getAbstractFeatureGroup();
            else if (containerType instanceof DocumentType)
                features = ((DocumentType) containerType).getAbstractFeatureGroup();
            extractTracks(trim(containerType.getNameElement()), trim(containerType.getDescription()), features, context);
        }

        if (feature instanceof PlacemarkType) {
            PlacemarkType placemarkType = (PlacemarkType) feature;
            String placemarkName = asDescription(trim(placemarkType.getNameElement()),
                    trim(placemarkType.getDescription()));

            List<KmlPosition> positions = extractPositions(placemarkType.getAbstractGeometryGroup());
            for (KmlPosition position : positions) {
                enrichPosition(position, extractTime(placemarkType.getAbstractTimePrimitiveGroup()), placemarkName, placemarkType.getDescription(), context.getStartDate());
            }
            context.appendRoute(new KmlRoute(this, Waypoints, placemarkName, null, positions));
        }
    }

    private void extractTracks(String name, String description, List<JAXBElement<? extends AbstractFeatureType>> features, ParserContext<KmlRoute> context) throws IOException {
        List<JAXBElement<PlacemarkType>> placemarks = find(features, "Placemark", PlacemarkType.class);
        extractWayPointsAndTracksFromPlacemarks(name, description, placemarks, context);

        List<JAXBElement<NetworkLinkType>> networkLinks = find(features, "NetworkLink", NetworkLinkType.class);
        extractWayPointsAndTracksFromNetworkLinks(networkLinks, context);

        List<JAXBElement<FolderType>> folders = find(features, "Folder", FolderType.class);
        for (JAXBElement<FolderType> folder : folders) {
            FolderType folderTypeValue = folder.getValue();
            String folderName = concatPath(name, folderTypeValue.getNameElement());
            extractTracks(folderName, description, folderTypeValue.getAbstractFeatureGroup(), context);
        }

        List<JAXBElement<DocumentType>> documents = find(features, "Document", DocumentType.class);
        for (JAXBElement<DocumentType> document : documents) {
            DocumentType documentTypeValue = document.getValue();
            String documentName = concatPath(name, documentTypeValue.getNameElement());
            extractTracks(documentName, description, documentTypeValue.getAbstractFeatureGroup(), context);
        }
    }

    private void extractWayPointsAndTracksFromPlacemarks(String name, String description, List<JAXBElement<PlacemarkType>> placemarkTypes, ParserContext<KmlRoute> context) {
        List<KmlPosition> waypoints = new ArrayList<>();
        for (JAXBElement<PlacemarkType> placemarkType : placemarkTypes) {
            PlacemarkType placemarkTypeValue = placemarkType.getValue();
            String placemarkName = asDescription(trim(placemarkTypeValue.getNameElement()),
                    trim(placemarkTypeValue.getDescription()));

            List<KmlPosition> positions = extractPositions(placemarkTypeValue.getAbstractGeometryGroup());
            if (positions.size() == 1) {
                // all placemarks with one position form one waypoint route
                KmlPosition wayPoint = positions.get(0);
                enrichPosition(wayPoint, extractTime(placemarkTypeValue.getAbstractTimePrimitiveGroup()), placemarkName, placemarkTypeValue.getDescription(), context.getStartDate());
                waypoints.add(wayPoint);
            } else {
                // each placemark with more than one position is one track
                String routeName = concatPath(name, asName(placemarkName));
                List<String> routeDescription = asDescription(placemarkTypeValue.getDescription() != null ? placemarkTypeValue.getDescription() : description);
                RouteCharacteristics characteristics = parseCharacteristics(routeName, Track);
                context.appendRoute(new KmlRoute(this, characteristics, routeName, routeDescription, positions));
            }
        }
        if (waypoints.size() > 0) {
            RouteCharacteristics characteristics = parseCharacteristics(name, Waypoints);
            context.prependRoute(new KmlRoute(this, characteristics, name, asDescription(description), waypoints));
        }
    }

    private void extractWayPointsAndTracksFromNetworkLinks(List<JAXBElement<NetworkLinkType>> networkLinkTypes, ParserContext<KmlRoute> context) throws IOException {
        for (JAXBElement<NetworkLinkType> networkLinkType : networkLinkTypes) {
            Link link = networkLinkType.getValue().getLink();
            if (link != null) {
                String url = link.getHref();
                context.parse(url);
            }

            List<JAXBElement<?>> rest = networkLinkType.getValue().getRest();
            for (JAXBElement<?> r : rest) {
                Object rValue = r.getValue();
                if (rValue instanceof LinkType) {
                    LinkType linkType = (LinkType) rValue;
                    String url = linkType.getHref();
                    context.parse(url);
                }
            }
        }
    }

    private List<KmlPosition> extractPositions(JAXBElement<? extends AbstractGeometryType> geometryType) {
        List<KmlPosition> positions = new ArrayList<>();
        if (geometryType == null)
            return positions;
        AbstractGeometryType geometryTypeValue = geometryType.getValue();
        if (geometryTypeValue instanceof PointType) {
            PointType point = (PointType) geometryTypeValue;
            positions.addAll(asKmlPositions(point.getCoordinates()));
        }
        if (geometryTypeValue instanceof LineStringType) {
            LineStringType lineString = (LineStringType) geometryTypeValue;
            positions.addAll(asKmlPositions(lineString.getCoordinates()));
        }
        if (geometryTypeValue instanceof MultiGeometryType) {
            MultiGeometryType multiGeometryType = (MultiGeometryType) geometryTypeValue;
            List<JAXBElement<? extends AbstractGeometryType>> geometryTypes = multiGeometryType.getAbstractGeometryGroup();
            for (JAXBElement<? extends AbstractGeometryType> geometryType2 : geometryTypes) {
                positions.addAll(extractPositions(geometryType2));
            }
        }
        return positions;
    }

    private CompactCalendar extractTime(JAXBElement<? extends AbstractTimePrimitiveType> timePrimitiveType) {
        if (timePrimitiveType != null) {
            AbstractTimePrimitiveType timePrimitiveTypeValue = timePrimitiveType.getValue();
            String time = "";
            if (timePrimitiveTypeValue instanceof TimeSpanType) {
                time = ((TimeSpanType) timePrimitiveTypeValue).getBegin();
            } else if (timePrimitiveTypeValue instanceof TimeStampType) {
                time = ((TimeStampType) timePrimitiveTypeValue).getWhen();
            }
            return parseTime(time);
        }
        return null;
    }


    private FolderType createWayPoints(KmlRoute route) {
        ObjectFactory objectFactory = new ObjectFactory();
        FolderType folderType = objectFactory.createFolderType();
        folderType.setNameElement(WAYPOINTS);
        folderType.setDescription(asDescription(route.getDescription()));
        for (KmlPosition position : route.getPositions()) {
            PlacemarkType placemarkType = objectFactory.createPlacemarkType();
            folderType.getAbstractFeatureGroup().add(objectFactory.createPlacemark(placemarkType));
            placemarkType.setNameElement(asName(isWriteName() ? position.getDescription() : null));
            placemarkType.setDescription(asDesc(isWriteDesc() ? position.getDescription() : null));
            placemarkType.setVisibility(FALSE);
            if (position.hasTime()) {
                TimeStampType timeStampType = objectFactory.createTimeStampType();
                timeStampType.setWhen(formatDate(position.getTime()));
                placemarkType.setAbstractTimePrimitiveGroup(objectFactory.createTimeStamp(timeStampType));
            }
            PointType pointType = objectFactory.createPointType();
            placemarkType.setAbstractGeometryGroup(objectFactory.createPoint(pointType));
            pointType.getCoordinates().add(createCoordinates(position, false));
        }
        return folderType;
    }

    private PlacemarkType createRoute(KmlRoute route) {
        ObjectFactory objectFactory = new ObjectFactory();
        PlacemarkType placemarkType = objectFactory.createPlacemarkType();
        placemarkType.setNameElement(createPlacemarkName(ROUTE, route));
        placemarkType.setDescription(asDescription(route.getDescription()));
        placemarkType.setStyleUrl("#" + ROUTE_LINE_STYLE);
        MultiGeometryType multiGeometryType = objectFactory.createMultiGeometryType();
        placemarkType.setAbstractGeometryGroup(objectFactory.createMultiGeometry(multiGeometryType));
        LineStringType lineStringType = objectFactory.createLineStringType();
        multiGeometryType.getAbstractGeometryGroup().add(objectFactory.createLineString(lineStringType));
        List<String> coordinates = lineStringType.getCoordinates();
        for (KmlPosition position : route.getPositions()) {
            coordinates.add(createCoordinates(position, false));
        }
        return placemarkType;
    }

    private PlacemarkType createTrack(KmlRoute route) {
        ObjectFactory objectFactory = new ObjectFactory();
        PlacemarkType placemarkType = objectFactory.createPlacemarkType();
        placemarkType.setNameElement(createPlacemarkName(TRACK, route));
        placemarkType.setDescription(asDescription(route.getDescription()));
        placemarkType.setStyleUrl("#" + TRACK_LINE_STYLE);
        LineStringType lineStringType = objectFactory.createLineStringType();
        placemarkType.setAbstractGeometryGroup(objectFactory.createLineString(lineStringType));
        List<String> coordinates = lineStringType.getCoordinates();
        for (KmlPosition position : route.getPositions()) {
            coordinates.add(createCoordinates(position, false));
        }
        return placemarkType;
    }

    private StyleType createLineStyle(String styleName, float width, byte[] color) {
        ObjectFactory objectFactory = new ObjectFactory();
        StyleType style = objectFactory.createStyleType();
        style.setId(styleName);
        LineStyleType lineStyle = objectFactory.createLineStyleType();
        style.setLineStyle(lineStyle);
        lineStyle.setColor(color);
        lineStyle.setWidth((double) width);
        return style;
    }

    private KmlType createKmlType(KmlRoute route) {
        ObjectFactory objectFactory = new ObjectFactory();
        KmlType kmlType = objectFactory.createKmlType();
        DocumentType documentType = objectFactory.createDocumentType();
        kmlType.setAbstractFeatureGroup(objectFactory.createDocument(documentType));
        documentType.setNameElement(createDocumentName(route));
        documentType.setDescription(asDescription(route.getDescription()));
        documentType.setOpen(TRUE);

        documentType.getAbstractStyleSelectorGroup().add(objectFactory.createStyle(createLineStyle(ROUTE_LINE_STYLE, getLineWidth(), getRouteLineColor())));
        documentType.getAbstractStyleSelectorGroup().add(objectFactory.createStyle(createLineStyle(TRACK_LINE_STYLE, getLineWidth(), getTrackLineColor())));

        FolderType folderType = createWayPoints(route);
        documentType.getAbstractFeatureGroup().add(objectFactory.createFolder(folderType));

        PlacemarkType placemarkTrack = createTrack(route);
        documentType.getAbstractFeatureGroup().add(objectFactory.createPlacemark(placemarkTrack));
        return kmlType;
    }

    private KmlType createKmlType(List<KmlRoute> routes) {
        ObjectFactory objectFactory = new ObjectFactory();
        KmlType kmlType = objectFactory.createKmlType();
        DocumentType documentType = objectFactory.createDocumentType();
        kmlType.setAbstractFeatureGroup(objectFactory.createDocument(documentType));
        documentType.setOpen(TRUE);

        documentType.getAbstractStyleSelectorGroup().add(objectFactory.createStyle(createLineStyle(ROUTE_LINE_STYLE, getLineWidth(), getRouteLineColor())));
        documentType.getAbstractStyleSelectorGroup().add(objectFactory.createStyle(createLineStyle(TRACK_LINE_STYLE, getLineWidth(), getTrackLineColor())));

        for (KmlRoute route : routes) {
            switch (route.getCharacteristics()) {
                case Waypoints:
                    FolderType folderType = createWayPoints(route);
                    documentType.getAbstractFeatureGroup().add(objectFactory.createFolder(folderType));
                    documentType.setNameElement(createDocumentName(route));
                    documentType.setDescription(asDescription(route.getDescription()));
                    break;
                case Route:
                    PlacemarkType placemarkRoute = createRoute(route);
                    documentType.getAbstractFeatureGroup().add(objectFactory.createPlacemark(placemarkRoute));

                    break;
                case Track:
                    PlacemarkType placemarkTrack = createTrack(route);
                    documentType.getAbstractFeatureGroup().add(objectFactory.createPlacemark(placemarkTrack));
                    break;
                default:
                    throw new IllegalArgumentException("Unknown RouteCharacteristics " + route.getCharacteristics());
            }
        }
        return kmlType;
    }

    public void write(KmlRoute route, OutputStream target, int startIndex, int endIndex) throws IOException {
        try {
            marshal22Beta(createKmlType(route), target);
        } catch (JAXBException e) {
            throw new IOException("Cannot marshall " + route + ": " + e, e);
        }
    }

    public void write(List<KmlRoute> routes, OutputStream target) throws IOException {
        try {
            marshal22Beta(createKmlType(routes), target);
        } catch (JAXBException e) {
            throw new IOException("Cannot marshall " + routes + ": " + e, e);
        }
    }
}
