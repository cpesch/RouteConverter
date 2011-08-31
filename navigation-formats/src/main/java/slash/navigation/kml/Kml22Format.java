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

import slash.common.io.CompactCalendar;
import slash.common.io.ISO8601;
import slash.navigation.base.RouteCharacteristics;
import slash.navigation.googlemaps.GoogleMapsPosition;
import slash.navigation.kml.binding22.*;
import slash.navigation.kml.binding22.ObjectFactory;
import slash.navigation.kml.binding22gx.*;
import slash.navigation.kml.bindingatom.Link;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.logging.Logger;

import static java.lang.Math.*;
import static java.lang.String.valueOf;
import static slash.common.hex.HexDecoder.decodeBytes;
import static slash.common.io.Transfer.*;
import static slash.navigation.base.RouteCharacteristics.Track;
import static slash.navigation.base.RouteCharacteristics.Waypoints;
import static slash.navigation.googlemaps.GoogleMapsPosition.parseExtensionPositions;
import static slash.navigation.util.Bearing.EARTH_RADIUS;
import static slash.navigation.util.RouteComments.commentRoutePositions;

/**
 * Reads and writes Google Earth 5 (.kml) files.
 *
 * @author Christian Pesch
 */

public class Kml22Format extends KmlFormat {
    private static final Logger log = Logger.getLogger(Kml22Format.class.getName());
    private static final int METERS_BETWEEN_MARKS = 1000;

    public String getName() {
        return "Google Earth 5 (*" + getExtension() + ")";
    }

    public List<KmlRoute> read(InputStream source, CompactCalendar startDate) throws IOException {
        try {
            return internalRead(source, startDate);
        } catch (JAXBException e) {
            log.fine("Error reading KML 2.2 from " + source + ": " + e.getMessage());
            return null;
        }
    }

    List<KmlRoute> internalRead(InputStream source, CompactCalendar startDate) throws IOException, JAXBException {
        KmlType kmlType = KmlUtil.unmarshal22(source);
        return process(kmlType, startDate);
    }

    protected List<KmlRoute> process(KmlType kmlType, CompactCalendar startDate) {
        if (kmlType == null || kmlType.getAbstractFeatureGroup() == null)
            return null;
        return extractTracks(kmlType, startDate);
    }

    @SuppressWarnings({"UnusedDeclaration", "unchecked"})
    private <T> List<JAXBElement<T>> find(List<JAXBElement<? extends AbstractFeatureType>> elements, String name, Class<T> resultClass) {
        List<JAXBElement<T>> result = new ArrayList<JAXBElement<T>>();
        for (JAXBElement<? extends AbstractFeatureType> element : elements) {
            if (name.equals(element.getName().getLocalPart()))
                result.add((JAXBElement<T>) element);
        }
        return result;
    }

    private List<KmlRoute> extractTracks(KmlType kmlType, CompactCalendar startDate) {
        List<KmlRoute> routes = null;

        AbstractFeatureType feature = kmlType.getAbstractFeatureGroup().getValue();
        if (feature instanceof AbstractContainerType) {
            AbstractContainerType containerType = (AbstractContainerType) feature;
            List<JAXBElement<? extends AbstractFeatureType>> features = null;
            if (containerType instanceof FolderType)
                features = ((FolderType) containerType).getAbstractFeatureGroup();
            else if (containerType instanceof DocumentType)
                features = ((DocumentType) containerType).getAbstractFeatureGroup();
            routes = extractTracks(trim(containerType.getName()), trim(containerType.getDescription()), features, startDate);
        }

        if (feature instanceof PlacemarkType) {
            PlacemarkType placemarkType = (PlacemarkType) feature;
            String placemarkName = asComment(trim(placemarkType.getName()), trim(placemarkType.getDescription()));

            List<KmlPosition> positions = extractPositions(placemarkType.getAbstractGeometryGroup());
            for (KmlPosition position : positions) {
                enrichPosition(position, extractTime(placemarkType.getAbstractTimePrimitiveGroup()), placemarkName, placemarkType.getDescription(), startDate);
            }
            routes = Arrays.asList(new KmlRoute(this, Waypoints, placemarkName, null, positions));
        }

        if (routes != null)
            commentRoutePositions(routes);
        return routes;
    }

    private List<KmlRoute> extractTracks(String name, String description, List<JAXBElement<? extends AbstractFeatureType>> features, CompactCalendar startDate) {
        List<KmlRoute> result = new ArrayList<KmlRoute>();

        List<JAXBElement<PlacemarkType>> placemarks = find(features, "Placemark", PlacemarkType.class);
        result.addAll(extractWayPointsAndTracksFromPlacemarks(name, description, placemarks, startDate));

        List<JAXBElement<NetworkLinkType>> networkLinks = find(features, "NetworkLink", NetworkLinkType.class);
        result.addAll(extractWayPointsAndTracksFromNetworkLinks(networkLinks));

        List<JAXBElement<FolderType>> folders = find(features, "Folder", FolderType.class);
        for (JAXBElement<FolderType> folder : folders) {
            FolderType folderTypeValue = folder.getValue();
            String folderName = concatPath(name, folderTypeValue.getName());
            result.addAll(extractTracks(folderName, description, folderTypeValue.getAbstractFeatureGroup(), startDate));
        }

        List<JAXBElement<DocumentType>> documents = find(features, "Document", DocumentType.class);
        for (JAXBElement<DocumentType> document : documents) {
            DocumentType documentTypeValue = document.getValue();
            String documentName = concatPath(name, documentTypeValue.getName());
            result.addAll(extractTracks(documentName, description, documentTypeValue.getAbstractFeatureGroup(), startDate));
        }
        return result;
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

    private List<KmlRoute> extractWayPointsAndTracksFromPlacemarks(String name, String description, List<JAXBElement<PlacemarkType>> placemarkTypes, CompactCalendar startDate) {
        List<KmlRoute> result = new ArrayList<KmlRoute>();

        List<KmlPosition> wayPoints = new ArrayList<KmlPosition>();
        for (JAXBElement<PlacemarkType> placemarkType : placemarkTypes) {
            PlacemarkType placemarkTypeValue = placemarkType.getValue();
            String placemarkName = asComment(trim(placemarkTypeValue.getName()), trim(placemarkTypeValue.getDescription()));

            JAXBElement<? extends AbstractGeometryType> abstractGeometryGroup = placemarkTypeValue.getAbstractGeometryGroup();
            if (abstractGeometryGroup == null)
                continue;

            List<KmlPosition> positions = extractPositions(abstractGeometryGroup);
            if (positions.size() == 1) {
                // all placemarks with one position form one waypoint route
                KmlPosition wayPoint = positions.get(0);
                enrichPosition(wayPoint, extractTime(placemarkTypeValue.getAbstractTimePrimitiveGroup()), placemarkName, placemarkTypeValue.getDescription(), startDate);
                wayPoints.add(wayPoint);
            } else {
                // each placemark with more than one position is one track
                String routeName = concatPath(name, placemarkName);
                List<String> routeDescription = asDescription(placemarkTypeValue.getDescription() != null ? placemarkTypeValue.getDescription() : description);
                RouteCharacteristics characteristics = parseCharacteristics(routeName, Track);
                result.add(new KmlRoute(this, characteristics, routeName, routeDescription, positions));
            }
        }
        if (wayPoints.size() > 0) {
            RouteCharacteristics characteristics = parseCharacteristics(name, Waypoints);
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

    protected List<KmlPosition> asExtendedKmlPositions(List<String> strings) {
        List<KmlPosition> result = new ArrayList<KmlPosition>();
        for (String string : strings) {
            for (GoogleMapsPosition position : parseExtensionPositions(string)) {
                result.add(asKmlPosition(position));
            }
        }
        return result;
    }

    private List<KmlPosition> extractPositions(List<String> coords, List<String> whens) {
        List<KmlPosition> result = asExtendedKmlPositions(coords);
        for (int i = 0; i < whens.size(); i++) {
            String when = whens.get(i);
            if (when != null) {
                Calendar calendar = ISO8601.parse(when);
                if (calendar != null && i < result.size())
                    result.get(i).setTime(CompactCalendar.fromCalendar(calendar));
            }
        }
        return result;
    }

    private List<KmlPosition> extractPositions(JAXBElement<? extends AbstractGeometryType> geometryType) {
        List<KmlPosition> positions = new ArrayList<KmlPosition>();
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
        if (geometryTypeValue instanceof TrackType) {
            TrackType trackType = (TrackType) geometryTypeValue;
            List<String> coord = trackType.getCoord();
            List<String> when = trackType.getWhen();
            positions.addAll(extractPositions(coord, when));
        }
        return positions;
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
            pointType.getCoordinates().add(createCoordinates(position, false));
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
            coordinates.add(createCoordinates(position, false));
        }
        return placemarkType;
    }

    private PlacemarkType createTrack(KmlRoute route) {
        ObjectFactory objectFactory = new ObjectFactory();
        PlacemarkType placemarkType = objectFactory.createPlacemarkType();
        placemarkType.setName(TRACK + ": " + createPlacemarkName(route));
        placemarkType.setStyleUrl("#" + TRACK_LINE_STYLE);
        slash.navigation.kml.binding22gx.ObjectFactory gxObjectFactory = new slash.navigation.kml.binding22gx.ObjectFactory();
        TrackType trackType = gxObjectFactory.createTrackType();
        for (KmlPosition position : route.getPositions()) {
            String time = position.getTime() != null ? ISO8601.format(position.getTime()) : "";
            trackType.getWhen().add(time);
        }
        for (KmlPosition position : route.getPositions()) {
            trackType.getCoord().add(createCoordinates(position, true));
        }
        placemarkType.setAbstractGeometryGroup(gxObjectFactory.createTrack(trackType));
        return placemarkType;
    }

    private boolean isWriteMarks() {
        return preferences.getBoolean("writeMarks", false);
    }

    private boolean isWriteSpeed() {
        return preferences.getBoolean("writeSpeed", false);
    }

    private static final String[] SPEED_COLORS = {
            "FF00ffff",
            "FF008080",
            "FF00ff00",
            "FF008000",
            "FFffff00",
            "FF808000",
            "FFff0000",
            "FF800000",
            "FFff00ff",
            "FF800080",
            "FF0000ff",
            "FF000080",
            "FF194c80",
            "FF000000"};
    private static final String SPEEDBAR_URL = "http://www.routeconverter.com/images/speedbar.png";

    private float getSpeedLineWidth() {
        return preferences.getFloat("speedLineWidth", 5.0f);
    }

    private int getSpeedScale() {
        return preferences.getInt("speedScale", 10);
    }

    private int getSpeedClass(double speed) {
        int speedClass = (int) speed / getSpeedScale();
        return speedClass < SPEED_COLORS.length ? speedClass >= 0 ? speedClass : 0 : SPEED_COLORS.length - 1;
    }

    private String getSpeedColor(int speedClass) {
        return "speedColor_" + valueOf(speedClass);
    }

    private String getSpeedDescription(int speedClass) {
        if (speedClass == 0)
            return "< " + valueOf(getSpeedScale()) + " Km/h";
        else if (speedClass <= SPEED_COLORS.length)
            return valueOf(speedClass * getSpeedScale()) + " - " + valueOf((speedClass + 1) * getSpeedScale()) + " Km/h";
        return "> " + valueOf(speedClass * getSpeedScale()) + " Km/h";
    }

    private List<StyleType> createSpeedTrackColors(float width) {
        List<StyleType> styleTypeList = new ArrayList<StyleType>();
        for (int i = 0; i < SPEED_COLORS.length; i++) {
            String styleName = getSpeedColor(i);
            StyleType styleType = createLineStyle(styleName, width, decodeBytes(SPEED_COLORS[i]));
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
        ObjectFactory objectFactory = new ObjectFactory();
        FolderType speedSegments = objectFactory.createFolderType();
        speedSegments.setName("Segments");
        speedSegments.setVisibility(false);
        speedSegments.setOpen(false);

        boolean foundSpeed = false;
        int currentSegment = 1;
        int previousSpeedClass = -1;
        List<String> coordinates = new ArrayList<String>();
        List<KmlPosition> positions = route.getPositions();
        for (int i = 0; i < positions.size() - 1; i++) {
            KmlPosition nextPosition = positions.get(i + 1);
            Double speed = positions.get(i).calculateSpeed(nextPosition);
            if (speed == null)
                continue;
            foundSpeed = true;

            int speedClass = getSpeedClass(speed);
            if (previousSpeedClass != speedClass && previousSpeedClass != -1) {
                PlacemarkType placemarkType = createSpeedSegment(currentSegment, speedClass, coordinates);
                speedSegments.getAbstractFeatureGroup().add(objectFactory.createPlacemark(placemarkType));

                coordinates.clear();
                currentSegment++;
            }

            previousSpeedClass = speedClass;
            coordinates.add(createCoordinates(positions.get(i), false));
        }

        if (!foundSpeed)
            return null;

        KmlPosition lastPosition = positions.get(positions.size() - 1);
        coordinates.add(createCoordinates(lastPosition, false));
        PlacemarkType placemarkType = createSpeedSegment(currentSegment, previousSpeedClass, coordinates);
        speedSegments.getAbstractFeatureGroup().add(objectFactory.createPlacemark(placemarkType));

        FolderType speed = objectFactory.createFolderType();
        speed.setName("Speed [Km/h]");
        speed.setVisibility(false);
        speed.setOpen(false);

        speed.getAbstractFeatureGroup().add(objectFactory.createScreenOverlay(createSpeedbar()));
        speed.getAbstractFeatureGroup().add(objectFactory.createFolder(speedSegments));
        return speed;
    }

    private PlacemarkType createSpeedSegment(int currentSegment, int speedClass, List<String> coordinates) {
        ObjectFactory objectFactory = new ObjectFactory();
        PlacemarkType placemarkType = objectFactory.createPlacemarkType();
        placemarkType.setName("Segment " + currentSegment);
        placemarkType.setDescription(getSpeedDescription(speedClass));
        placemarkType.setStyleUrl('#' + getSpeedColor(speedClass));
        placemarkType.setVisibility(false);
        LineStringType lineStringType = objectFactory.createLineStringType();
        lineStringType.getCoordinates().addAll(coordinates);
        placemarkType.setAbstractGeometryGroup(objectFactory.createLineString(lineStringType));
        return placemarkType;
    }

    private ScreenOverlayType createSpeedbar() {
        ScreenOverlayType speedbar = createScreenOverlayImage("Speedbar",
                SPEEDBAR_URL,
                createVec2Type(0.0, 0.01, UnitsEnumType.FRACTION, UnitsEnumType.FRACTION),
                createVec2Type(0.0, 0.01, UnitsEnumType.FRACTION, UnitsEnumType.FRACTION),
                createVec2Type(250, 0, UnitsEnumType.PIXELS, UnitsEnumType.PIXELS));
        speedbar.setVisibility(false);
        return speedbar;
    }

    private FolderType createMarks(KmlRoute route) {
        ObjectFactory objectFactory = new ObjectFactory();

        FolderType marks = objectFactory.createFolderType();
        marks.setName("Marks [Km]");
        marks.setVisibility(false);
        marks.setOpen(false);

        double currentDistance = 0, previousDistance = 0;
        int currentKilometer = 1;
        for (int i = 1; i < route.getPositions().size(); i++) {
            KmlPosition previousPosition = route.getPositions().get(i - 1);
            KmlPosition currentPosition = route.getPositions().get(i);

            Double distance = currentPosition.calculateDistance(previousPosition);
            if (distance == null)
                continue;

            currentDistance += distance;
            if (currentDistance >= METERS_BETWEEN_MARKS) {
                // calculate the point at the kilometer mark that's between the current position and the previous one.
                // it is possible, that there's more than one point to create
                // see: http://www.movable-type.co.uk/scripts/latlong.html and http://williams.best.vwh.net/avform.htm#LL
                KmlPosition intermediate = new KmlPosition(previousPosition.getLongitude(), previousPosition.getLatitude(), null, null, null, null);

                // remaining distance between the last point and the mark
                double remainingDistance = METERS_BETWEEN_MARKS - (previousDistance % METERS_BETWEEN_MARKS);
                do {
                    double angle = toRadians(intermediate.calculateAngle(currentPosition));
                    double latitude1 = toRadians(intermediate.getLatitude());
                    double longitude1 = toRadians(intermediate.getLongitude());
                    double latitude2 = asin(sin(latitude1) * cos(remainingDistance / EARTH_RADIUS) +
                            cos(latitude1) * sin(remainingDistance / EARTH_RADIUS) * cos(angle));
                    double longitude2 = longitude1 +
                            atan2(sin(angle) * sin(remainingDistance / EARTH_RADIUS) * cos(latitude1),
                                    cos(remainingDistance / EARTH_RADIUS) - sin(latitude1) * sin(latitude2));
                    intermediate.setLatitude(toDegrees(latitude2));
                    intermediate.setLongitude(toDegrees(longitude2));

                    PlacemarkType placeMark = createMark(currentKilometer++, intermediate.getLongitude(), intermediate.getLatitude());
                    marks.getAbstractFeatureGroup().add(objectFactory.createPlacemark(placeMark));

                    remainingDistance = METERS_BETWEEN_MARKS;
                } while (intermediate.calculateDistance(currentPosition) > METERS_BETWEEN_MARKS);

                currentDistance = currentDistance % METERS_BETWEEN_MARKS;
            }
            previousDistance = currentDistance;
        }
        return marks;
    }

    private PlacemarkType createMark(int kilometer, double longitude, double latitude) {
        ObjectFactory objectFactory = new ObjectFactory();
        PlacemarkType placeMark = objectFactory.createPlacemarkType();
        placeMark.setName(kilometer + ". Km");
        placeMark.setVisibility(kilometer % 10 == 0);
        PointType point = objectFactory.createPointType();
        point.getCoordinates().add(formatPositionAsString(longitude) + "," + formatPositionAsString(latitude) + ",0");
        placeMark.setAbstractGeometryGroup(objectFactory.createPoint(point));
        return placeMark;
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

        PlacemarkType placemarkTrack = createTrack(route);
        documentType.getAbstractFeatureGroup().add(objectFactory.createPlacemark(placemarkTrack));

        if (route.getCharacteristics().equals(Track) && isWriteSpeed()) {
            FolderType speed = createSpeed(route);
            if (speed != null)
                documentType.getAbstractFeatureGroup().add(objectFactory.createFolder(speed));
        }
        if (!route.getCharacteristics().equals(Waypoints) && isWriteMarks()) {
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

    public void write(KmlRoute route, OutputStream target, int startIndex, int endIndex) {
        try {
            KmlUtil.marshal22(createKmlType(route), target);
        } catch (JAXBException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public void write(List<KmlRoute> routes, OutputStream target) throws IOException {
        try {
            KmlUtil.marshal22(createKmlType(routes), target);
        } catch (JAXBException e) {
            throw new IllegalArgumentException(e);
        }
    }
}