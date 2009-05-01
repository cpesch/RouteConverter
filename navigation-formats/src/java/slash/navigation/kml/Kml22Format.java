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

import slash.navigation.RouteCharacteristics;
import slash.navigation.util.RouteComments;
import slash.navigation.hex.HexDecoder;
import slash.navigation.kml.binding22.*;
import slash.navigation.kml.bindingatom.Link;
import slash.navigation.util.Bearing;
import slash.navigation.util.Conversion;
import slash.navigation.util.ISO8601;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.logging.Logger;

/**
 * Reads and writes Google Earth 5 (.kml) files.
 *
 * @author Christian Pesch
 */

public class Kml22Format extends KmlFormat {
    private static final Logger log = Logger.getLogger(Kml22Format.class.getName());

    public String getName() {
        return "Google Earth 5 (*" + getExtension() + ")";
    }

    public List<KmlRoute> read(InputStream source, Calendar startDate) throws IOException {
        try {
            return internalRead(source);
        } catch (JAXBException e) {
            log.fine("Error reading " + source + ": " + e.getMessage());
            return null;
        }
    }

    List<KmlRoute> internalRead(InputStream source) throws IOException, JAXBException {
        KmlType kmlType = KmlUtil.unmarshal22(source);
        return process(kmlType);
    }

    protected List<KmlRoute> process(KmlType kmlType) {
        if (kmlType == null || kmlType.getAbstractFeatureGroup() == null)
            return null;
        return extractTracks(kmlType);
    }


    private <T> List<JAXBElement<T>> find(List<JAXBElement<? extends AbstractFeatureType>> elements, String name, Class<T> resultClass) {
        List<JAXBElement<T>> result = new ArrayList<JAXBElement<T>>();
        for (JAXBElement<? extends AbstractFeatureType> element : elements) {
            if (name.equals(element.getName().getLocalPart()))
                result.add((JAXBElement<T>) element);
        }
        return result;
    }

    private List<KmlRoute> extractTracks(KmlType kmlType) {
        List<KmlRoute> routes = null;

        AbstractFeatureType feature = kmlType.getAbstractFeatureGroup().getValue();
        if (feature instanceof AbstractContainerType) {
            AbstractContainerType containerType = (AbstractContainerType) feature;
            List<JAXBElement<? extends AbstractFeatureType>> features = null;
            if (containerType instanceof FolderType)
                features = ((FolderType) containerType).getAbstractFeatureGroup();
            else if (containerType instanceof DocumentType)
                features = ((DocumentType) containerType).getAbstractFeatureGroup();
            routes = extractTracks(Conversion.trim(containerType.getName()), Conversion.trim(containerType.getDescription()), features);
        }

        if (feature instanceof PlacemarkType) {
            PlacemarkType placemarkType = (PlacemarkType) feature;
            String placemarkName = asComment(Conversion.trim(placemarkType.getName()),
                    Conversion.trim(placemarkType.getDescription()));

            List<KmlPosition> positions = extractPositions(placemarkType.getAbstractGeometryGroup());
            for (KmlPosition position : positions) {
                enrichPosition(position, extractTime(placemarkType.getAbstractTimePrimitiveGroup()), placemarkName, placemarkType.getDescription());
            }
            routes = Arrays.asList(new KmlRoute(this, RouteCharacteristics.Waypoints, placemarkName, null, positions));
        }

        if (routes != null)
            RouteComments.commentRoutePositions(routes);
        return routes;
    }

    private List<KmlRoute> extractTracks(String name, String description, List<JAXBElement<? extends AbstractFeatureType>> features) {
        List<KmlRoute> result = new ArrayList<KmlRoute>();

        List<JAXBElement<PlacemarkType>> placemarks = find(features, "Placemark", PlacemarkType.class);
        result.addAll(extractWayPointsAndTracksFromPlacemarks(name, description, placemarks));

        List<JAXBElement<NetworkLinkType>> networkLinks = find(features, "NetworkLink", NetworkLinkType.class);
        result.addAll(extractWayPointsAndTracksFromNetworkLinks(networkLinks));

        List<JAXBElement<FolderType>> folders = find(features, "Folder", FolderType.class);
        for (JAXBElement<FolderType> folder : folders) {
            FolderType folderTypeValue = folder.getValue();
            String folderName = concatPath(name, folderTypeValue.getName());
            result.addAll(extractTracks(folderName, description, folderTypeValue.getAbstractFeatureGroup()));
        }

        List<JAXBElement<DocumentType>> documents = find(features, "Document", DocumentType.class);
        for (JAXBElement<DocumentType> document : documents) {
            DocumentType documentTypeValue = document.getValue();
            String documentName = concatPath(name, documentTypeValue.getName());
            result.addAll(extractTracks(documentName, description, documentTypeValue.getAbstractFeatureGroup()));
        }
        return result;
    }

    private List<KmlRoute> extractWayPointsAndTracksFromPlacemarks(String name, String description, List<JAXBElement<PlacemarkType>> placemarkTypes) {
        List<KmlRoute> result = new ArrayList<KmlRoute>();

        List<KmlPosition> wayPoints = new ArrayList<KmlPosition>();
        for (JAXBElement<PlacemarkType> placemarkType : placemarkTypes) {
            PlacemarkType placemarkTypeValue = placemarkType.getValue();
            String placemarkName = asComment(Conversion.trim(placemarkTypeValue.getName()),
                    Conversion.trim(placemarkTypeValue.getDescription()));

            List<KmlPosition> positions = extractPositions(placemarkTypeValue.getAbstractGeometryGroup());
            if (positions.size() == 1) {
                // all placemarks with one position form one waypoint route
                KmlPosition wayPoint = positions.get(0);
                enrichPosition(wayPoint, extractTime(placemarkTypeValue.getAbstractTimePrimitiveGroup()), placemarkName, placemarkTypeValue.getDescription());
                wayPoints.add(wayPoint);
            } else {
                // each placemark with more than one position is one track
                String routeName = concatPath(name, placemarkName);
                List<String> routeDescription = asDescription(placemarkTypeValue.getDescription() != null ? placemarkTypeValue.getDescription() : description);
                RouteCharacteristics characteristics = parseCharacteristics(routeName, RouteCharacteristics.Track);
                result.add(new KmlRoute(this, characteristics, routeName, routeDescription, positions));
            }
        }
        if (wayPoints.size() > 0) {
            RouteCharacteristics characteristics = parseCharacteristics(name, RouteCharacteristics.Waypoints);
            result.add(0, new KmlRoute(this, characteristics, name, asDescription(description), wayPoints));
        }
        return result;
    }

    private List<KmlRoute> extractWayPointsAndTracksFromNetworkLinks(List<JAXBElement<NetworkLinkType>> networkLinkTypes) {
        List<KmlRoute> result = new ArrayList<KmlRoute>();
        for (JAXBElement<NetworkLinkType> networkLinkType : networkLinkTypes) {
            Link link = networkLinkType.getValue().getLink();
            if (link != null) {
                String url = link.getHref();
                List<KmlRoute> routes = parseRouteFromUrl(url);
                if (routes != null)
                    result.addAll(routes);
            }

            List<JAXBElement<?>> rest = networkLinkType.getValue().getRest();
            for (JAXBElement<?> r : rest) {
                Object rValue = r.getValue();
                if (rValue instanceof LinkType) {
                    LinkType linkType = (LinkType) rValue;
                    String url = linkType.getHref();
                    List<KmlRoute> routes = parseRouteFromUrl(url);
                    if (routes != null)
                        result.addAll(routes);
                }
            }
        }
        return result;
    }

    private List<KmlPosition> extractPositions(JAXBElement<? extends AbstractGeometryType> geometryType) {
        List<KmlPosition> positions = new ArrayList<KmlPosition>();
        AbstractGeometryType geometryTypeValue = geometryType.getValue();
        if (geometryTypeValue instanceof PointType) {
            PointType point = (PointType) geometryTypeValue;
            for (String coordinates : point.getCoordinates())
                positions.add(parsePosition(coordinates, null));
        }
        if (geometryTypeValue instanceof LineStringType) {
            LineStringType lineString = (LineStringType) geometryTypeValue;
            for (String coordinates : lineString.getCoordinates())
                positions.add(parsePosition(coordinates, null));
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

    private Calendar extractTime(JAXBElement<? extends AbstractTimePrimitiveType> timePrimitiveType) {
        if (timePrimitiveType != null) {
            AbstractTimePrimitiveType timePrimitiveTypeValue = timePrimitiveType.getValue();
            String time = "";
            if (timePrimitiveTypeValue instanceof TimeSpanType) {
                time = ((TimeSpanType) timePrimitiveTypeValue).getBegin();
            } else if (timePrimitiveTypeValue instanceof TimeStampType) {
                time = ((TimeStampType) timePrimitiveTypeValue).getWhen();
            }
            return ISO8601.parse(time);
        }
        return null;
    }


    private FolderType createWayPoints(KmlRoute route) {
        ObjectFactory objectFactory = new ObjectFactory();
        FolderType folderType = objectFactory.createFolderType();
        folderType.setName(WAYPOINTS);
        for (KmlPosition position : route.getPositions()) {
            PlacemarkType placemarkType = objectFactory.createPlacemarkType();
            folderType.getAbstractFeatureGroup().add(objectFactory.createPlacemark(placemarkType));
            placemarkType.setName(position.getComment());
            placemarkType.setVisibility(Boolean.FALSE);
            if (position.getTime() != null) {
                TimeStampType timeStampType = objectFactory.createTimeStampType();
                timeStampType.setWhen(ISO8601.format(position.getTime()));
                placemarkType.setAbstractTimePrimitiveGroup(objectFactory.createTimeStamp(timeStampType));
            }
            PointType pointType = objectFactory.createPointType();
            placemarkType.setAbstractGeometryGroup(objectFactory.createPoint(pointType));
            pointType.getCoordinates().add(Conversion.formatDoubleAsString(position.getLongitude()) + "," +
                    Conversion.formatDoubleAsString(position.getLatitude()) + "," +
                    Conversion.formatDoubleAsString(position.getElevation()));
        }
        return folderType;
    }

    private PlacemarkType createRoute(KmlRoute route) {
        ObjectFactory objectFactory = new ObjectFactory();
        PlacemarkType placemarkType = objectFactory.createPlacemarkType();
        placemarkType.setName(ROUTE + ": " + createPlacemarkName(route));
        placemarkType.setStyleUrl("#" + ROUTE_LINE_STYLE);
        MultiGeometryType multiGeometryType = objectFactory.createMultiGeometryType();
        placemarkType.setAbstractGeometryGroup(objectFactory.createMultiGeometry(multiGeometryType));
        LineStringType lineStringType = objectFactory.createLineStringType();
        multiGeometryType.getAbstractGeometryGroup().add(objectFactory.createLineString(lineStringType));
        List<String> coordinates = lineStringType.getCoordinates();
        for (KmlPosition position : route.getPositions()) {
            coordinates.add(Conversion.formatDoubleAsString(position.getLongitude()) + "," +
                    Conversion.formatDoubleAsString(position.getLatitude()) + "," +
                    Conversion.formatDoubleAsString(position.getElevation()));
        }
        return placemarkType;
    }

    private PlacemarkType createTrack(KmlRoute route) {
        ObjectFactory objectFactory = new ObjectFactory();
        PlacemarkType placemarkType = objectFactory.createPlacemarkType();
        placemarkType.setName(TRACK + ": " + createPlacemarkName(route));
        placemarkType.setStyleUrl("#" + TRACK_LINE_STYLE);
        LineStringType lineStringType = objectFactory.createLineStringType();
        placemarkType.setAbstractGeometryGroup(objectFactory.createLineString(lineStringType));
        List<String> coordinates = lineStringType.getCoordinates();
        for (KmlPosition position : route.getPositions()) {
            coordinates.add(Conversion.formatDoubleAsString(position.getLongitude()) + "," +
                    Conversion.formatDoubleAsString(position.getLatitude()) + "," +
                    Conversion.formatDoubleAsString(position.getElevation()));
        }
        return placemarkType;
    }


    private float getSpeedLineWidth() {
        return preferences.getFloat("speedLineWidth", 5.0f);
    }

    private String getSpeedColorCode(double speed) {
        int arrayPos = (int) speed / SPEED_SCALE;
        if (arrayPos < SPEED_COLORS.length)
            return SPEED_COLORS[arrayPos];
        return SPEED_COLORS[SPEED_COLORS.length - 1];
    }

    private String getSpeedColorName(double speed) {
        int speedInt = (int) speed / SPEED_SCALE;
        return "speedColor_" + String.valueOf(speedInt < SPEED_COLORS.length ? speedInt : SPEED_COLORS.length - 1);
    }

    private String getSpeedDescription(double speed) {
        int speedGroup = (int) speed / SPEED_SCALE;
        if (speedGroup == 0)
            return "< " + String.valueOf(SPEED_SCALE) + " km/h";
        else if (speedGroup <= SPEED_COLORS.length)
            return String.valueOf(speedGroup * SPEED_SCALE) + " - " + String.valueOf((speedGroup + 1) * SPEED_SCALE) + " km/h";
        return "> " + String.valueOf(speedGroup * SPEED_SCALE) + " km/h";
    }

    private boolean isWriteMarks() {
        return preferences.getBoolean("writeMarks", false);
    }

    private boolean isWriteSpeed() {
        return preferences.getBoolean("writeSpeed", false);
    }

    private List<StyleType> createSpeedTrackColors(float width) {
        List<StyleType> styleTypeList = new ArrayList<StyleType>();
        for (int i = 0; i < SPEED_COLORS.length; i++) {
            String styleName = getSpeedColorName((i) * SPEED_SCALE);
            StyleType styleType = createLineStyle(styleName, width, HexDecoder.decodeBytes(SPEED_COLORS[i]));
            styleTypeList.add(styleType);
        }
        return styleTypeList;
    }

    private ScreenOverlayType createScreenOverlayImage(String name, String url, Vec2Type overlayXY,
                                                       Vec2Type screenXY, Vec2Type size) {
        ObjectFactory objectFactory = new ObjectFactory();
        ScreenOverlayType screenOverlayType = objectFactory.createScreenOverlayType();
        screenOverlayType.setName(name);
        screenOverlayType.setOverlayXY(overlayXY);
        screenOverlayType.setScreenXY(screenXY);
        screenOverlayType.setSize(size);

        LinkType icon = objectFactory.createLinkType();
        icon.setHref(url);
        screenOverlayType.setIcon(icon);
        return screenOverlayType;
    }

    private Vec2Type createVec2Type(double x, double y, UnitsEnumType unitX, UnitsEnumType unitY) {
        ObjectFactory objectFactory = new ObjectFactory();
        Vec2Type vec2Type = objectFactory.createVec2Type();
        vec2Type.setX(x);
        vec2Type.setY(y);
        vec2Type.setXunits(unitX);
        vec2Type.setYunits(unitY);
        return vec2Type;
    }

    private FolderType createSpeed(KmlRoute route) {
        // Structure for Speedtrack:
        // -Speedtrack
        //  |-Speedbar
        //  |-Speedsegments
        //  | |-Segment 1
        //  | |-Segment n
        //  |-Startpoint
        //  |-Endpoint

        // check if the first point of the route has as all neccesary data: position and time
        // without time -> no speed
        KmlPosition firstPos = route.getPositions().get(0);
        if ((firstPos.getTime() == null) || (route.getPositionCount() < 2)) {
            return null;
        }

        ObjectFactory objectFactory = new ObjectFactory();
        FolderType speedSegments = objectFactory.createFolderType();
        speedSegments.setName("Segments");
        speedSegments.setOpen(false);
        speedSegments.setVisibility(false);

        String previousSpeedColorCode = null;
        int segmentNumber = 0;
        List<String> coordinates = null;
        List<KmlPosition> positions = route.getPositions();
        for (int i = 0; i < positions.size() - 1; i++) {
            Double speed = positions.get(i).calculateSpeed(positions.get(i + 1));
            if(speed == null)
                continue;
            String speedColorCode = getSpeedColorCode(speed);

            // if speed class is different
            if (!speedColorCode.equals(previousSpeedColorCode)) {
                previousSpeedColorCode = speedColorCode;

                if (coordinates != null)
                    coordinates.add(Conversion.formatDoubleAsString(positions.get(i).getLongitude()) + "," +
                            Conversion.formatDoubleAsString(positions.get(i).getLatitude()) + "," +
                            Conversion.formatDoubleAsString(positions.get(i).getElevation()));

                PlacemarkType placemarkType = objectFactory.createPlacemarkType();
                placemarkType.setName("Segment " + (++segmentNumber));
                placemarkType.setDescription(getSpeedDescription(speed));
                placemarkType.setStyleUrl('#' + getSpeedColorName(speed));
                placemarkType.setVisibility(false);

                LineStringType lineStringType = objectFactory.createLineStringType();
                coordinates = lineStringType.getCoordinates();

                placemarkType.setAbstractGeometryGroup(objectFactory.createLineString(lineStringType));
                speedSegments.getAbstractFeatureGroup().add(objectFactory.createPlacemark(placemarkType));
            }

            if (coordinates != null)
                coordinates.add(Conversion.formatDoubleAsString(positions.get(i).getLongitude()) + "," +
                        Conversion.formatDoubleAsString(positions.get(i).getLatitude()) + "," +
                        Conversion.formatDoubleAsString(positions.get(i).getElevation()));
        }

        if (coordinates != null) {
            KmlPosition lastPosition = positions.get(positions.size() - 1);
            coordinates.add(Conversion.formatDoubleAsString(lastPosition.getLongitude()) + "," +
                    Conversion.formatDoubleAsString(lastPosition.getLatitude()) + "," +
                    Conversion.formatDoubleAsString(lastPosition.getElevation()));
        }

        FolderType speed = objectFactory.createFolderType();
        speed.setName("Speed [km/h]");
        speed.setOpen(false);
        speed.setVisibility(false);

        ScreenOverlayType speedbar = createScreenOverlayImage("Speedbar",
                SPEEDBAR_URL,
                createVec2Type(0.0, 0.01, UnitsEnumType.FRACTION, UnitsEnumType.FRACTION),
                createVec2Type(0.0, 0.01, UnitsEnumType.FRACTION, UnitsEnumType.FRACTION),
                createVec2Type(250, 0, UnitsEnumType.PIXELS, UnitsEnumType.PIXELS));
        speedbar.setVisibility(false);

        speed.getAbstractFeatureGroup().add(objectFactory.createScreenOverlay(speedbar));
        speed.getAbstractFeatureGroup().add(objectFactory.createFolder(speedSegments));
        return speed;
    }

    private FolderType createMarks(KmlRoute route) {
        ObjectFactory objectFactory = new ObjectFactory();

        FolderType marks = objectFactory.createFolderType();
        marks.setName("Marks [km]");
        marks.setVisibility(false);
        marks.setOpen(false);

        double distance = 0, previousDistance = 0;
        int distPoint = 0;
        for (int i = 1; i < route.getPositions().size(); i++) {
            KmlPosition position = route.getPositions().get(i);
            KmlPosition previousPosition = route.getPositions().get(i - 1);

            // TODO check hasCoordinates
            distance += position.calculateDistance(previousPosition);
            if (distance >= 1000) {
                // calculate the point at the Kilometermark that's between the current position and the
                // previous one. It is possible, that there's more than one point to create
                // See: http://www.movable-type.co.uk/scripts/latlong.html and http://williams.best.vwh.net/avform.htm#LL
                KmlPosition lastPosition = new KmlPosition(previousPosition.getLongitude(), previousPosition.getLatitude(), 0d, null, null);

                // remaining distance between the last point and the mark
                double remaining = 1000 - (previousDistance % (1000));
                do {
                    // TODO check hasCoordinates
                    double angle = Math.toRadians(lastPosition.calculateAngle(position));
                    double latitude1 = Math.toRadians(lastPosition.getLatitude());
                    double longitude1 = Math.toRadians(lastPosition.getLongitude());
                    double latitude2 = Math.asin(Math.sin(latitude1) * Math.cos(remaining / Bearing.EARTH_RADIUS) +
                            Math.cos(latitude1) * Math.sin(remaining / Bearing.EARTH_RADIUS) * Math.cos(angle));
                    double longitude2 = longitude1 +
                            Math.atan2(Math.sin(angle) * Math.sin(remaining / Bearing.EARTH_RADIUS) * Math.cos(latitude1),
                                    Math.cos(remaining / Bearing.EARTH_RADIUS) - Math.sin(latitude1) * Math.sin(latitude2));

                    lastPosition.setLatitude(Math.toDegrees(latitude2));
                    lastPosition.setLongitude(Math.toDegrees(longitude2));

                    PlacemarkType placeMark = objectFactory.createPlacemarkType();
                    placeMark.setName((++distPoint) + ". km");
                    placeMark.setVisibility(false);
                    PointType point = objectFactory.createPointType();
                    point.getCoordinates().add(Conversion.formatDoubleAsString(lastPosition.getLongitude()) + "," +
                            Conversion.formatDoubleAsString(lastPosition.getLatitude()) + "," +
                            "0");
                    placeMark.setAbstractGeometryGroup(objectFactory.createPoint(point));

                    marks.getAbstractFeatureGroup().add(objectFactory.createPlacemark(placeMark));

                    remaining = 1000;
                } while (lastPosition.calculateDistance(position) > 1000);

                distance = distance % 1000;
            }
            previousDistance = distance;
        }
        return marks;
    }

    private StyleType createLineStyle(String styleName, double width, byte[] color) {
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
        kmlType.setAbstractFeatureGroup(objectFactory.createDocument(documentType));
        documentType.setName(createDocumentName(route));
        documentType.setDescription(asDescription(route.getDescription()));
        documentType.setOpen(Boolean.TRUE);

        documentType.getAbstractStyleSelectorGroup().add(objectFactory.createStyle(createLineStyle(ROUTE_LINE_STYLE, getLineWidth(), getRouteLineColor())));
        documentType.getAbstractStyleSelectorGroup().add(objectFactory.createStyle(createLineStyle(TRACK_LINE_STYLE, getLineWidth(), getTrackLineColor())));
        if (isWriteSpeed())
            for (StyleType style : createSpeedTrackColors(getSpeedLineWidth()))
                documentType.getAbstractStyleSelectorGroup().add(objectFactory.createStyle(style));

        FolderType folderType = createWayPoints(route);
        documentType.getAbstractFeatureGroup().add(objectFactory.createFolder(folderType));

        // TODO no TIME for track - exchange waypoints and track?
        PlacemarkType placemarkTrack = createTrack(route);
        documentType.getAbstractFeatureGroup().add(objectFactory.createPlacemark(placemarkTrack));
        if (isWriteSpeed()) {
            FolderType speed = createSpeed(route);
            if (speed != null)
                documentType.getAbstractFeatureGroup().add(objectFactory.createFolder(speed));
        }
        if (isWriteMarks()) {
            FolderType marks = createMarks(route);
            documentType.getAbstractFeatureGroup().add(objectFactory.createFolder(marks));
        }
        return kmlType;
    }

    private KmlType createKmlType(List<KmlRoute> routes) {
        ObjectFactory objectFactory = new ObjectFactory();
        KmlType kmlType = objectFactory.createKmlType();
        DocumentType documentType = objectFactory.createDocumentType();
        kmlType.setAbstractFeatureGroup(objectFactory.createDocument(documentType));
        documentType.setOpen(Boolean.TRUE);

        documentType.getAbstractStyleSelectorGroup().add(objectFactory.createStyle(createLineStyle(ROUTE_LINE_STYLE, getLineWidth(), getRouteLineColor())));
        documentType.getAbstractStyleSelectorGroup().add(objectFactory.createStyle(createLineStyle(TRACK_LINE_STYLE, getLineWidth(), getTrackLineColor())));
        if (isWriteSpeed())
            for (StyleType style : createSpeedTrackColors(getSpeedLineWidth()))
                documentType.getAbstractStyleSelectorGroup().add(objectFactory.createStyle(style));

        for (KmlRoute route : routes) {
            switch (route.getCharacteristics()) {
                case Waypoints:
                    FolderType folderType = createWayPoints(route);
                    documentType.getAbstractFeatureGroup().add(objectFactory.createFolder(folderType));
                    documentType.setName(createDocumentName(route));
                    documentType.setDescription(asDescription(route.getDescription()));
                    break;
                case Route:
                    PlacemarkType placemarkRoute = createRoute(route);
                    documentType.getAbstractFeatureGroup().add(objectFactory.createPlacemark(placemarkRoute));
                    if (isWriteSpeed()) {
                        FolderType speed = createSpeed(route);
                        if (speed != null)
                            documentType.getAbstractFeatureGroup().add(objectFactory.createFolder(speed));
                    }
                    if (isWriteMarks())
                        documentType.getAbstractFeatureGroup().add(objectFactory.createFolder(createMarks(route)));
                    break;
                case Track:
                    PlacemarkType placemarkTrack = createTrack(route);
                    documentType.getAbstractFeatureGroup().add(objectFactory.createPlacemark(placemarkTrack));
                    if (isWriteSpeed()) {
                        FolderType speed = createSpeed(route);
                        if (speed != null)
                            documentType.getAbstractFeatureGroup().add(objectFactory.createFolder(speed));
                    }
                    if (isWriteMarks())
                        documentType.getAbstractFeatureGroup().add(objectFactory.createFolder(createMarks(route)));
                    break;
                default:
                    throw new IllegalArgumentException("Unknown RouteCharacteristics " + route.getCharacteristics());
            }
        }
        return kmlType;
    }

    public void write(KmlRoute route, File target, int startIndex, int endIndex, boolean numberPositionNames) {
        try {
            KmlUtil.marshal22(createKmlType(route), target);
        } catch (JAXBException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public void write(List<KmlRoute> routes, File target) throws IOException {
        try {
            KmlUtil.marshal22(createKmlType(routes), target);
        } catch (JAXBException e) {
            throw new IllegalArgumentException(e);
        }
    }
}