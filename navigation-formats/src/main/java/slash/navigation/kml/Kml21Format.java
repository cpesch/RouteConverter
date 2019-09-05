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
import slash.navigation.kml.binding21.*;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import static java.lang.Boolean.TRUE;
import static slash.common.io.Transfer.trim;
import static slash.common.type.ISO8601.formatDate;
import static slash.navigation.base.RouteCharacteristics.Track;
import static slash.navigation.base.RouteCharacteristics.Waypoints;
import static slash.navigation.kml.KmlUtil.marshal21;
import static slash.navigation.kml.KmlUtil.unmarshal21;

/**
 * Reads and writes Google Earth 4 (.kml) files.
 *
 * @author Christian Pesch
 */

public class Kml21Format extends KmlFormat {

    public String getName() {
        return "Google Earth 4 (*" + getExtension() + ")";
    }

    public void read(InputStream source, ParserContext<KmlRoute> context) throws IOException {
        KmlType kmlType = unmarshal21(source);
        process(kmlType, context);
    }

    protected void process(KmlType kmlType, ParserContext<KmlRoute> context) throws IOException {
        if (kmlType == null || kmlType.getFeature() == null)
            return;
        extractTracks(kmlType, context);
    }

    @SuppressWarnings({"UnusedDeclaration", "unchecked"})
    private <T> List<JAXBElement<T>> find(List<JAXBElement<? extends FeatureType>> elements, String name, Class<T> resultClass) {
        List<JAXBElement<T>> result = new ArrayList<>();
        if (elements != null) {
            for (JAXBElement<? extends FeatureType> element : elements) {
                if (name.equals(element.getName().getLocalPart()))
                    result.add((JAXBElement<T>) element);
            }
        }
        return result;
    }

    private void extractTracks(KmlType kmlType, ParserContext<KmlRoute> context) throws IOException {
        FeatureType feature = kmlType.getFeature().getValue();
        if (feature instanceof ContainerType) {
            ContainerType containerType = (ContainerType) feature;
            List<JAXBElement<? extends FeatureType>> features = null;
            if (containerType instanceof FolderType)
                features = ((FolderType) containerType).getFeature();
            else if (containerType instanceof DocumentType)
                features = ((DocumentType) containerType).getFeature();
            extractTracks(trim(containerType.getName()), trim(containerType.getDescription()), features, context);
        }

        if (feature instanceof PlacemarkType) {
            PlacemarkType placemarkType = (PlacemarkType) feature;
            String placemarkName = asDescription(trim(placemarkType.getName()),
                    trim(placemarkType.getDescription()));

            List<KmlPosition> positions = extractPositions(placemarkType.getGeometry());
            for (KmlPosition position : positions) {
                enrichPosition(position, extractTime(placemarkType.getTimePrimitive()), placemarkName, placemarkType.getDescription(), context.getStartDate());
            }
            context.appendRoute(new KmlRoute(this, Waypoints, placemarkName, null, positions));
        }
    }

    private void extractTracks(String name, String description, List<JAXBElement<? extends FeatureType>> features, ParserContext<KmlRoute> context) throws IOException {
        List<JAXBElement<PlacemarkType>> placemarks = find(features, "Placemark", PlacemarkType.class);
        extractWayPointsAndTracksFromPlacemarks(name, description, placemarks, context);

        List<JAXBElement<NetworkLinkType>> networkLinks = find(features, "NetworkLink", NetworkLinkType.class);
        extractWayPointsAndTracksFromNetworkLinks(networkLinks, context);

        List<JAXBElement<FolderType>> folders = find(features, "Folder", FolderType.class);
        for (JAXBElement<FolderType> folder : folders) {
            FolderType folderTypeValue = folder.getValue();
            String folderName = concatPath(name, folderTypeValue.getName());
            extractTracks(folderName, description, folderTypeValue.getFeature(), context);
        }

        List<JAXBElement<DocumentType>> documents = find(features, "Document", DocumentType.class);
        for (JAXBElement<DocumentType> document : documents) {
            DocumentType documentTypeValue = document.getValue();
            String documentName = concatPath(name, documentTypeValue.getName());
            extractTracks(documentName, description, documentTypeValue.getFeature(), context);
        }
    }

    private void extractWayPointsAndTracksFromPlacemarks(String name, String description, List<JAXBElement<PlacemarkType>> placemarkTypes, ParserContext<KmlRoute> context) {
        List<KmlPosition> waypoints = new ArrayList<>();
        for (JAXBElement<PlacemarkType> placemarkType : placemarkTypes) {
            PlacemarkType placemarkTypeValue = placemarkType.getValue();
            String placemarkName = asDescription(trim(placemarkTypeValue.getName()),
                    trim(placemarkTypeValue.getDescription()));

            List<KmlPosition> positions = extractPositions(placemarkTypeValue.getGeometry());
            if (positions.size() == 1) {
                // all placemarks with one position form one waypoint route
                KmlPosition wayPoint = positions.get(0);
                enrichPosition(wayPoint, extractTime(placemarkTypeValue.getTimePrimitive()), placemarkName, placemarkTypeValue.getDescription(), context.getStartDate());
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
            LinkType linkType = networkLinkType.getValue().getUrl();
            if (linkType != null) {
                String url = linkType.getHref();
                context.parse(url);
            }
        }
    }

    private List<KmlPosition> extractPositions(JAXBElement<? extends GeometryType> geometryType) {
        List<KmlPosition> positions = new ArrayList<>();
        if (geometryType != null) {
            GeometryType geometryTypeValue = geometryType.getValue();
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
                List<JAXBElement<? extends GeometryType>> geometryTypes = multiGeometryType.getGeometry();
                for (JAXBElement<? extends GeometryType> geometryType2 : geometryTypes) {
                    positions.addAll(extractPositions(geometryType2));
                }
            }
        }
        return positions;
    }

    private CompactCalendar extractTime(JAXBElement<? extends TimePrimitiveType> timePrimitiveType) {
        if (timePrimitiveType != null) {
            TimePrimitiveType timePrimitiveTypeValue = timePrimitiveType.getValue();
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
        folderType.setName(WAYPOINTS);
        folderType.setDescription(asDescription(route.getDescription()));
        for (KmlPosition position : route.getPositions()) {
            PlacemarkType placemarkType = objectFactory.createPlacemarkType();
            folderType.getFeature().add(objectFactory.createPlacemark(placemarkType));
            placemarkType.setName(asName(isWriteName() ? position.getDescription() : null));
            placemarkType.setDescription(asDesc(isWriteDesc() ? position.getDescription() : null));
            placemarkType.setVisibility(Boolean.FALSE);
            if (position.hasTime()) {
                TimeStampType timeStampType = objectFactory.createTimeStampType();
                timeStampType.setWhen(formatDate(position.getTime()));
                placemarkType.setTimePrimitive(objectFactory.createTimeStamp(timeStampType));
            }
            PointType pointType = objectFactory.createPointType();
            placemarkType.setGeometry(objectFactory.createPoint(pointType));
            pointType.getCoordinates().add(createCoordinates(position, false));
        }
        return folderType;
    }

    private PlacemarkType createRoute(KmlRoute route) {
        ObjectFactory objectFactory = new ObjectFactory();
        PlacemarkType placemarkType = objectFactory.createPlacemarkType();
        placemarkType.setName(createPlacemarkName(ROUTE, route));
        placemarkType.setDescription(asDescription(route.getDescription()));
        placemarkType.setStyleUrl("#" + ROUTE_LINE_STYLE);
        MultiGeometryType multiGeometryType = objectFactory.createMultiGeometryType();
        placemarkType.setGeometry(objectFactory.createMultiGeometry(multiGeometryType));
        LineStringType lineStringType = objectFactory.createLineStringType();
        multiGeometryType.getGeometry().add(objectFactory.createLineString(lineStringType));
        List<String> coordinates = lineStringType.getCoordinates();
        for (KmlPosition position : route.getPositions()) {
            coordinates.add(createCoordinates(position, false));
        }
        return placemarkType;
    }

    private PlacemarkType createTrack(KmlRoute route) {
        ObjectFactory objectFactory = new ObjectFactory();
        PlacemarkType placemarkType = objectFactory.createPlacemarkType();
        placemarkType.setName(createPlacemarkName(TRACK, route));
        placemarkType.setDescription(asDescription(route.getDescription()));
        placemarkType.setStyleUrl("#" + TRACK_LINE_STYLE);
        LineStringType lineStringType = objectFactory.createLineStringType();
        placemarkType.setGeometry(objectFactory.createLineString(lineStringType));
        List<String> coordinates = lineStringType.getCoordinates();
        for (KmlPosition position : route.getPositions()) {
            coordinates.add(createCoordinates(position, false));
        }
        return placemarkType;
    }

    private StyleType createLineStyle(String styleName, float width, byte[] color) {
        ObjectFactory objectFactory = new ObjectFactory();
        StyleType styleType = objectFactory.createStyleType();
        styleType.setId(styleName);
        LineStyleType lineStyleType = objectFactory.createLineStyleType();
        styleType.setLineStyle(lineStyleType);
        lineStyleType.setColor(color);
        lineStyleType.setWidth(width);
        return styleType;
    }

    private KmlType createKmlType(KmlRoute route) {
        ObjectFactory objectFactory = new ObjectFactory();
        KmlType kmlType = objectFactory.createKmlType();
        DocumentType documentType = objectFactory.createDocumentType();
        kmlType.setFeature(objectFactory.createDocument(documentType));
        documentType.setName(createDocumentName(route));
        documentType.setDescription(asDescription(route.getDescription()));
        documentType.setOpen(TRUE);

        documentType.getStyleSelector().add(objectFactory.createStyle(createLineStyle(ROUTE_LINE_STYLE, getLineWidth(), getRouteLineColor())));
        documentType.getStyleSelector().add(objectFactory.createStyle(createLineStyle(TRACK_LINE_STYLE, getLineWidth(), getTrackLineColor())));

        FolderType folderType = createWayPoints(route);
        documentType.getFeature().add(objectFactory.createFolder(folderType));

        PlacemarkType placemarkTrack = createTrack(route);
        documentType.getFeature().add(objectFactory.createPlacemark(placemarkTrack));
        return kmlType;
    }

    private KmlType createKmlType(List<KmlRoute> routes) {
        ObjectFactory objectFactory = new ObjectFactory();
        KmlType kmlType = objectFactory.createKmlType();
        DocumentType documentType = objectFactory.createDocumentType();
        kmlType.setFeature(objectFactory.createDocument(documentType));
        documentType.setOpen(TRUE);

        documentType.getStyleSelector().add(objectFactory.createStyle(createLineStyle(ROUTE_LINE_STYLE, getLineWidth(), getRouteLineColor())));
        documentType.getStyleSelector().add(objectFactory.createStyle(createLineStyle(TRACK_LINE_STYLE, getLineWidth(), getTrackLineColor())));

        for (KmlRoute route : routes) {
            switch (route.getCharacteristics()) {
                case Waypoints:
                    FolderType folderType = createWayPoints(route);
                    documentType.getFeature().add(objectFactory.createFolder(folderType));
                    documentType.setName(createDocumentName(route));
                    documentType.setDescription(asDescription(route.getDescription()));
                    break;
                case Route:
                    PlacemarkType placemarkRoute = createRoute(route);
                    documentType.getFeature().add(objectFactory.createPlacemark(placemarkRoute));
                    break;
                case Track:
                    PlacemarkType placemarkTrack = createTrack(route);
                    documentType.getFeature().add(objectFactory.createPlacemark(placemarkTrack));
                    break;
                default:
                    throw new IllegalArgumentException("Unknown RouteCharacteristics " + route.getCharacteristics());
            }
        }
        return kmlType;
    }

    public void write(KmlRoute route, OutputStream target, int startIndex, int endIndex) throws IOException {
        try {
            marshal21(createKmlType(route), target);
        } catch (JAXBException e) {
            throw new IOException("Cannot marshall " + route + ": " + e, e);
        }
    }

    public void write(List<KmlRoute> routes, OutputStream target) throws IOException {
        try {
            marshal21(createKmlType(routes), target);
        } catch (JAXBException e) {
            throw new IOException("Cannot marshall " + routes + ": " + e, e);
        }
    }
}
