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
import slash.navigation.common.NavigationPosition;
import slash.navigation.kml.binding22.ObjectFactory;
import slash.navigation.kml.binding22.*;
import slash.navigation.kml.binding22gx.*;
import slash.navigation.kml.bindingatom.Link;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.lang.Math.*;
import static java.lang.String.valueOf;
import static java.util.Collections.singletonList;
import static slash.common.io.Transfer.*;
import static slash.common.type.HexadecimalNumber.decodeBytes;
import static slash.common.type.ISO8601.formatDate;
import static slash.navigation.base.RouteCharacteristics.*;
import static slash.navigation.common.Bearing.EARTH_RADIUS;
import static slash.navigation.common.NavigationConversion.formatPositionAsString;
import static slash.navigation.common.PositionParser.parseExtensionPositions;
import static slash.navigation.kml.KmlUtil.marshal22;
import static slash.navigation.kml.KmlUtil.unmarshal22;
import static slash.navigation.kml.binding22.UnitsEnumType.FRACTION;
import static slash.navigation.kml.binding22.UnitsEnumType.PIXELS;

/**
 * Reads and writes Google Earth 5 (.kml) files.
 *
 * @author Christian Pesch
 */

public class Kml22Format extends KmlFormat {
    private static final int METERS_BETWEEN_MARKS = 1000;

    public String getName() {
        return "Google Earth 5 (*" + getExtension() + ")";
    }

    public void read(InputStream source, ParserContext<KmlRoute> context) throws IOException {
        KmlType kmlType = unmarshal22(source);
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

    protected void extractTracks(KmlType kmlType, ParserContext<KmlRoute> context) throws IOException {
        AbstractFeatureType feature = kmlType.getAbstractFeatureGroup().getValue();
        if (feature instanceof AbstractContainerType) {
            AbstractContainerType containerType = (AbstractContainerType) feature;
            List<JAXBElement<? extends AbstractFeatureType>> features = null;
            if (containerType instanceof FolderType)
                features = ((FolderType) containerType).getAbstractFeatureGroup();
            else if (containerType instanceof DocumentType)
                features = ((DocumentType) containerType).getAbstractFeatureGroup();
            extractTracks(trim(containerType.getName()), trim(containerType.getDescription()), features, context);
        }

        if (feature instanceof PlacemarkType) {
            PlacemarkType placemarkType = (PlacemarkType) feature;
            String placemarkName = asDescription(trim(placemarkType.getName()), trim(placemarkType.getDescription()));

            List<KmlPosition> positions = extractPositionsFromGeometry(placemarkType.getAbstractGeometryGroup());
            for (KmlPosition position : positions) {
                enrichPosition(position, extractTime(placemarkType.getAbstractTimePrimitiveGroup()), placemarkName, placemarkType.getDescription(), context.getStartDate());
            }
            context.appendRoute(new KmlRoute(this, Waypoints, placemarkName, null, positions));
        }

        if (feature instanceof TourType) {
            TourType tourType = (TourType) feature;
            String tourName = asDescription(trim(tourType.getName()), trim(tourType.getDescription()));

            List<KmlPosition> positions = extractPositionsFromTour(tourType.getPlaylist().getAbstractTourPrimitiveGroup());
            for (KmlPosition position : positions) {
                enrichPosition(position, extractTime(tourType.getAbstractTimePrimitiveGroup()), tourName, tourType.getDescription(), context.getStartDate());
            }
            context.appendRoute(new KmlRoute(this, Track, tourName, null, positions));
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
            String folderName = trim(folderTypeValue.getName());
            // ignore speed and marks folders
            if (folderName == null || (!folderName.equals(SPEED) && !folderName.equals(MARKS)))
                extractTracks(concatPath(name, folderName), description, folderTypeValue.getAbstractFeatureGroup(), context);
        }

        List<JAXBElement<DocumentType>> documents = find(features, "Document", DocumentType.class);
        for (JAXBElement<DocumentType> document : documents) {
            DocumentType documentTypeValue = document.getValue();
            String documentName = concatPath(name, documentTypeValue.getName());
            extractTracks(documentName, description, documentTypeValue.getAbstractFeatureGroup(), context);
        }
    }

    private CompactCalendar extractTime(JAXBElement<? extends AbstractTimePrimitiveType> timePrimitiveType) {
        if (timePrimitiveType != null) {
            AbstractTimePrimitiveType timePrimitiveTypeValue = timePrimitiveType.getValue();
            String time = null;
            if (timePrimitiveTypeValue instanceof TimeSpanType) {
                time = ((TimeSpanType) timePrimitiveTypeValue).getBegin();
            } else if (timePrimitiveTypeValue instanceof TimeStampType) {
                time = ((TimeStampType) timePrimitiveTypeValue).getWhen();
            }
            return parseTime(time);
        }
        return null;
    }

    private void extractWayPointsAndTracksFromPlacemarks(String name, String description, List<JAXBElement<PlacemarkType>> placemarkTypes, ParserContext<KmlRoute> context) {
        List<KmlPosition> waypoints = new ArrayList<>();
        for (JAXBElement<PlacemarkType> placemarkType : placemarkTypes) {
            PlacemarkType placemarkTypeValue = placemarkType.getValue();
            String placemarkName = asDescription(trim(placemarkTypeValue.getName()), trim(placemarkTypeValue.getDescription()));

            JAXBElement<? extends AbstractGeometryType> abstractGeometryGroup = placemarkTypeValue.getAbstractGeometryGroup();
            if (abstractGeometryGroup == null)
                continue;

            List<KmlPosition> positions = extractPositionsFromGeometry(abstractGeometryGroup);
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

    private List<KmlPosition> asExtendedKmlPositions(List<String> strings) {
        List<KmlPosition> result = new ArrayList<>();
        for (String string : strings) {
            for (NavigationPosition position : parseExtensionPositions(string)) {
                result.add(asKmlPosition(position));
            }
        }
        return result;
    }

    private List<KmlPosition> extractPositions(TrackType trackType) {
        List<String> coords = trackType.getCoord();
        List<String> whens = trackType.getWhen();
        List<KmlPosition> result = asExtendedKmlPositions(coords);
        for (int i = 0; i < whens.size(); i++) {
            String when = whens.get(i);
            if (when != null) {
                CompactCalendar calendar = parseTime(when);
                if (calendar != null && i < result.size())
                    result.get(i).setTime(calendar);
            }
        }
        return result;
    }

    private List<KmlPosition> extractPositionsFromGeometry(JAXBElement<? extends AbstractGeometryType> geometryType) {
        List<KmlPosition> positions = new ArrayList<>();
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
                positions.addAll(extractPositionsFromGeometry(geometryType2));
            }
        }
        if (geometryTypeValue instanceof MultiTrackType) {
            MultiTrackType multiTrackType = (MultiTrackType) geometryTypeValue;
            List<TrackType> tracks = multiTrackType.getTrack();
            for (TrackType track : tracks) {
                positions.addAll(extractPositions(track));
            }
        }
        if (geometryTypeValue instanceof TrackType) {
            TrackType trackType = (TrackType) geometryTypeValue;
            positions.addAll(extractPositions(trackType));
        }
        return positions;
    }

    private List<KmlPosition> extractPositionsFromTour(List<JAXBElement<? extends AbstractTourPrimitiveType>> tourPrimitives) {
        List<KmlPosition> positions = new ArrayList<>();
        for (JAXBElement<? extends AbstractTourPrimitiveType> tourPrimitive : tourPrimitives) {
            AbstractTourPrimitiveType tourPrimitiveValue = tourPrimitive.getValue();
            if (tourPrimitiveValue instanceof FlyToType) {
                FlyToType flyToType = (FlyToType) tourPrimitiveValue;
                AbstractViewType abstractViewGroupValue = flyToType.getAbstractViewGroup().getValue();
                if (abstractViewGroupValue instanceof LookAtType) {
                    LookAtType lookAtType = (LookAtType) abstractViewGroupValue;
                    Double elevation = isEmpty(lookAtType.getAltitude()) ? null : lookAtType.getAltitude();
                    KmlPosition position = new KmlPosition(lookAtType.getLongitude(), lookAtType.getLatitude(),
                            elevation, null, null, null);
                    position.setHeading(lookAtType.getHeading());
                    positions.add(position);
                }
            }
        }
        return positions;
    }

    private List<PlacemarkType> createWayPoints(KmlRoute route, int startIndex, int endIndex) {
        ObjectFactory objectFactory = new ObjectFactory();
        List<PlacemarkType> result = new ArrayList<>();
        List<KmlPosition> positions = route.getPositions();
        for (int i = startIndex; i < endIndex; i++) {
            KmlPosition position = positions.get(i);
            PlacemarkType placemarkType = objectFactory.createPlacemarkType();
            result.add(placemarkType);
            placemarkType.setName(asName(isWriteName() ? position.getDescription() : null));
            placemarkType.setDescription(asDesc(isWriteDesc() ? position.getDescription() : null));
            if (position.hasTime()) {
                TimeStampType timeStampType = objectFactory.createTimeStampType();
                timeStampType.setWhen(formatDate(position.getTime()));
                placemarkType.setAbstractTimePrimitiveGroup(objectFactory.createTimeStamp(timeStampType));
            }
            PointType pointType = objectFactory.createPointType();
            placemarkType.setAbstractGeometryGroup(objectFactory.createPoint(pointType));
            pointType.getCoordinates().add(createCoordinates(position, false));
        }
        return result;
    }

    private PlacemarkType createRoute(KmlRoute route) {
        ObjectFactory objectFactory = new ObjectFactory();
        PlacemarkType placemarkType = objectFactory.createPlacemarkType();
        // deactivated to preserve route name in folder name
        // placemarkType.setName(ROUTE);
        // placemarkType.setDescription(asDescription(route.getDescription()));
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

    private PlacemarkType createTrack(KmlRoute route, int startIndex, int endIndex) {
        ObjectFactory objectFactory = new ObjectFactory();
        PlacemarkType placemarkType = objectFactory.createPlacemarkType();
        // deactivated to preserve route name in folder name
        // placemarkType.setName(TRACK);
        // placemarkType.setDescription(asDescription(route.getDescription()));
        placemarkType.setStyleUrl("#" + TRACK_LINE_STYLE);
        // create gx:Track if there are at least two positions with a time stamp
        if (containTime(route)) {
            slash.navigation.kml.binding22gx.ObjectFactory gxObjectFactory = new slash.navigation.kml.binding22gx.ObjectFactory();
            TrackType trackType = gxObjectFactory.createTrackType();
            List<KmlPosition> positions = route.getPositions();
            for (int i = startIndex; i < endIndex; i++) {
                KmlPosition position = positions.get(i);
                String time = position.hasTime() ? formatDate(position.getTime()) : "";
                trackType.getWhen().add(time);
            }
            for (int i = startIndex; i < endIndex; i++) {
                KmlPosition position = positions.get(i);
                trackType.getCoord().add(createCoordinates(position, true));
            }
            placemarkType.setAbstractGeometryGroup(gxObjectFactory.createTrack(trackType));
        } else {
            LineStringType lineStringType = objectFactory.createLineStringType();
            placemarkType.setAbstractGeometryGroup(objectFactory.createLineString(lineStringType));
            List<String> coordinates = lineStringType.getCoordinates();
            List<KmlPosition> positions = route.getPositions();
            for (int i = startIndex; i < endIndex; i++) {
                KmlPosition position = positions.get(i);
                coordinates.add(createCoordinates(position, false));
            }
        }
        return placemarkType;
    }

    private boolean containTime(KmlRoute route) {
        int foundTime = 0;
        for (NavigationPosition position : route.getPositions()) {
            if (position.hasTime())
                foundTime++;
        }
        return foundTime > 1;
    }

    private boolean isWriteMarks() {
        return preferences.getBoolean("writeMarks", true);
    }

    private boolean isWriteSpeed() {
        return preferences.getBoolean("writeSpeed", true);
    }

    private boolean hasCharacteristics(List<KmlRoute> routes, RouteCharacteristics characteristics) {
        for(KmlRoute route : routes)
            if(route.getCharacteristics().equals(characteristics))
                return true;
        return false;
    }

    private static final String[] SPEED_COLORS = new String[]{
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
            return "&lt; " + valueOf(getSpeedScale()) + " Km/h";
        else if (speedClass <= SPEED_COLORS.length)
            return valueOf(speedClass * getSpeedScale()) + " - " + valueOf((speedClass + 1) * getSpeedScale()) + " Km/h";
        return "&gt; " + valueOf(speedClass * getSpeedScale()) + " Km/h";
    }

    private List<StyleType> createSpeedTrackColors(float width) {
        List<StyleType> styleTypeList = new ArrayList<>();
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

    private FolderType createSpeed(KmlRoute route, int startIndex, int endIndex) {
        ObjectFactory objectFactory = new ObjectFactory();
        FolderType folderType = objectFactory.createFolderType();
        folderType.setName(SPEED);
        folderType.setVisibility(FALSE);
        folderType.setOpen(FALSE);
        folderType.getAbstractFeatureGroup().add(objectFactory.createScreenOverlay(createSpeedbar()));

        int segmentIndex = 0;
        List<String> coordinates = new ArrayList<>();
        Integer previousSpeedClass = null;
        Double previousSpeed = null;
        KmlPosition previous = null;
        List<KmlPosition> positions = route.getPositions();

        // since the speed of a position is the average speed of the previous segment
        for (int i = startIndex; i < endIndex; i++) {
            KmlPosition position = positions.get(i);

            Double speed = null;
            if(position.hasSpeed())
                speed = position.getSpeed();
            else if (previous != null)
                speed = previous.calculateSpeed(position);
            if (speed == null)
                speed = previousSpeed;
            if (speed == null)
                continue;

            coordinates.add(createCoordinates(position, false));

            int speedClass = getSpeedClass(speed);
            if (previousSpeedClass != null && previousSpeedClass != speedClass) {
                PlacemarkType placemarkType = createSpeedSegment(++segmentIndex, previousSpeedClass, coordinates);
                folderType.getAbstractFeatureGroup().add(objectFactory.createPlacemark(placemarkType));

                coordinates.clear();
                coordinates.add(createCoordinates(position, false));
            }

            previousSpeedClass = speedClass;
            previousSpeed = speed;
            previous = position;
        }

        return segmentIndex > 0 ? folderType : null;
    }

    private PlacemarkType createSpeedSegment(int currentSegment, int speedClass, List<String> coordinates) {
        ObjectFactory objectFactory = new ObjectFactory();
        PlacemarkType placemarkType = objectFactory.createPlacemarkType();
        placemarkType.setName("Segment " + currentSegment);
        placemarkType.setDescription(getSpeedDescription(speedClass));
        placemarkType.setStyleUrl('#' + getSpeedColor(speedClass));
        placemarkType.setVisibility(FALSE);
        LineStringType lineStringType = objectFactory.createLineStringType();
        lineStringType.getCoordinates().addAll(coordinates);
        placemarkType.setAbstractGeometryGroup(objectFactory.createLineString(lineStringType));
        return placemarkType;
    }

    private ScreenOverlayType createSpeedbar() {
        ScreenOverlayType speedbar = createScreenOverlayImage("Speedbar",
                SPEEDBAR_URL,
                createVec2Type(0.0, 0.01, FRACTION, FRACTION),
                createVec2Type(0.0, 0.01, FRACTION, FRACTION),
                createVec2Type(250, 0, PIXELS, PIXELS));
        speedbar.setVisibility(FALSE);
        return speedbar;
    }

    private FolderType createMarks(KmlRoute route, int startIndex, int endIndex) {
        ObjectFactory objectFactory = new ObjectFactory();

        FolderType marks = objectFactory.createFolderType();
        marks.setName(MARKS);
        marks.setVisibility(FALSE);
        marks.setOpen(FALSE);

        double currentDistance = 0, previousDistance = 0;
        int currentKiloMeter = 1;
        List<KmlPosition> positions = route.getPositions();
        for (int i = startIndex + 1; i < endIndex; i++) {
            KmlPosition previousPosition = positions.get(i - 1);
            KmlPosition currentPosition = positions.get(i);

            Double distance = currentPosition.calculateDistance(previousPosition);
            if (isEmpty(distance))
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

                    PlacemarkType placeMark = createMark(currentKiloMeter++, intermediate.getLongitude(), intermediate.getLatitude());
                    marks.getAbstractFeatureGroup().add(objectFactory.createPlacemark(placeMark));

                    remainingDistance = METERS_BETWEEN_MARKS;
                } while (toDouble(intermediate.calculateDistance(currentPosition)) > METERS_BETWEEN_MARKS);

                currentDistance = currentDistance % METERS_BETWEEN_MARKS;
            }
            previousDistance = currentDistance;
        }
        return marks;
    }

    private PlacemarkType createMark(int kiloMeter, double longitude, double latitude) {
        ObjectFactory objectFactory = new ObjectFactory();
        PlacemarkType placeMark = objectFactory.createPlacemarkType();
        placeMark.setName(kiloMeter + ". Km");
        placeMark.setVisibility(FALSE);
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

    protected KmlType createKmlType(KmlRoute route, int startIndex, int endIndex) {
        ObjectFactory objectFactory = new ObjectFactory();
        KmlType kmlType = objectFactory.createKmlType();
        DocumentType documentType = objectFactory.createDocumentType();
        kmlType.setAbstractFeatureGroup(objectFactory.createDocument(documentType));
        documentType.setName(asRouteName(route.getName()));
        documentType.setDescription(asDescription(route.getDescription()));
        documentType.setOpen(TRUE);

        if (hasCharacteristics(singletonList(route), Route))
            documentType.getAbstractStyleSelectorGroup().add(objectFactory.createStyle(createLineStyle(ROUTE_LINE_STYLE, getLineWidth(), getRouteLineColor())));
        if (hasCharacteristics(singletonList(route), Track)) {
            documentType.getAbstractStyleSelectorGroup().add(objectFactory.createStyle(createLineStyle(TRACK_LINE_STYLE, getLineWidth(), getTrackLineColor())));
            if (isWriteSpeed())
                for (StyleType style : createSpeedTrackColors(getSpeedLineWidth()))
                    documentType.getAbstractStyleSelectorGroup().add(objectFactory.createStyle(style));
        }

        List<PlacemarkType> wayPoints = createWayPoints(route, startIndex, endIndex);
        for (PlacemarkType wayPoint : wayPoints)
            documentType.getAbstractFeatureGroup().add(objectFactory.createPlacemark(wayPoint));

        PlacemarkType track = createTrack(route, startIndex, endIndex);
        documentType.getAbstractFeatureGroup().add(objectFactory.createPlacemark(track));

        if (hasCharacteristics(singletonList(route), Track)) {
            FolderType speed = createSpeed(route, startIndex, endIndex);
            if (speed != null)
                documentType.getAbstractFeatureGroup().add(objectFactory.createFolder(speed));
        }
        if (!route.getCharacteristics().equals(Waypoints) && isWriteMarks()) {
            FolderType marks = createMarks(route, startIndex, endIndex);
            documentType.getAbstractFeatureGroup().add(objectFactory.createFolder(marks));
        }
        return kmlType;
    }

    private KmlType createKmlType(List<KmlRoute> routes) {
        ObjectFactory objectFactory = new ObjectFactory();
        KmlType kmlType = objectFactory.createKmlType();
        DocumentType documentType = objectFactory.createDocumentType();
        kmlType.setAbstractFeatureGroup(objectFactory.createDocument(documentType));
        documentType.setOpen(TRUE);

        if (hasCharacteristics(routes, Route))
            documentType.getAbstractStyleSelectorGroup().add(objectFactory.createStyle(createLineStyle(ROUTE_LINE_STYLE, getLineWidth(), getRouteLineColor())));
        if (hasCharacteristics(routes, Track)) {
            documentType.getAbstractStyleSelectorGroup().add(objectFactory.createStyle(createLineStyle(TRACK_LINE_STYLE, getLineWidth(), getTrackLineColor())));
            if (isWriteSpeed())
                for (StyleType style : createSpeedTrackColors(getSpeedLineWidth()))
                    documentType.getAbstractStyleSelectorGroup().add(objectFactory.createStyle(style));
        }

        for (KmlRoute route : routes) {
            switch (route.getCharacteristics()) {
                case Waypoints:
                    FolderType wayPointsFolder = objectFactory.createFolderType();
                    wayPointsFolder.setName(createPlacemarkName(WAYPOINTS, route));
                    wayPointsFolder.setDescription(asDescription(route.getDescription()));
                    documentType.getAbstractFeatureGroup().add(objectFactory.createFolder(wayPointsFolder));

                    List<PlacemarkType> wayPoints = createWayPoints(route, 0, route.getPositionCount());
                    for (PlacemarkType wayPoint : wayPoints)
                        wayPointsFolder.getAbstractFeatureGroup().add(objectFactory.createPlacemark(wayPoint));
                    break;
                case Route:
                    FolderType routeFolder = objectFactory.createFolderType();
                    routeFolder.setName(createPlacemarkName(ROUTE, route));
                    documentType.getAbstractFeatureGroup().add(objectFactory.createFolder(routeFolder));

                    PlacemarkType routePlacemarks = createRoute(route);
                    routeFolder.getAbstractFeatureGroup().add(objectFactory.createPlacemark(routePlacemarks));
                    if (isWriteMarks())
                        routeFolder.getAbstractFeatureGroup().add(objectFactory.createFolder(createMarks(route, 0, route.getPositionCount())));
                    break;
                case Track:
                    FolderType trackFolder = objectFactory.createFolderType();
                    trackFolder.setName(createPlacemarkName(TRACK, route));
                    documentType.getAbstractFeatureGroup().add(objectFactory.createFolder(trackFolder));

                    PlacemarkType track = createTrack(route, 0, route.getPositionCount());
                    trackFolder.getAbstractFeatureGroup().add(objectFactory.createPlacemark(track));
                    if (isWriteSpeed()) {
                        FolderType speed = createSpeed(route, 0, route.getPositionCount());
                        if (speed != null)
                            trackFolder.getAbstractFeatureGroup().add(objectFactory.createFolder(speed));
                    }
                    if (isWriteMarks())
                        trackFolder.getAbstractFeatureGroup().add(objectFactory.createFolder(createMarks(route, 0, route.getPositionCount())));
                    break;
                default:
                    throw new IllegalArgumentException("Unknown RouteCharacteristics " + route.getCharacteristics());
            }
        }
        return kmlType;
    }

    public void write(KmlRoute route, OutputStream target, int startIndex, int endIndex) throws IOException {
        try {
            marshal22(createKmlType(route, startIndex, endIndex), target);
        } catch (JAXBException e) {
            throw new IOException("Cannot marshall " + route + ": " + e, e);
        }
    }

    public void write(List<KmlRoute> routes, OutputStream target) throws IOException {
        try {
            marshal22(createKmlType(routes), target);
        } catch (JAXBException e) {
            throw new IOException("Cannot marshall " + routes + ": " + e, e);
        }
    }
}