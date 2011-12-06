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
import slash.navigation.kml.binding20.*;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.logging.Logger;

import static slash.common.io.Transfer.trim;
import static slash.navigation.base.RouteCharacteristics.Track;
import static slash.navigation.base.RouteCharacteristics.Waypoints;
import static slash.navigation.googlemaps.GoogleMapsPosition.parsePosition;
import static slash.navigation.googlemaps.GoogleMapsPosition.parsePositions;
import static slash.navigation.util.RouteComments.commentRoutePositions;

/**
 * Reads and writes Google Earth 3 (.kml) files.
 *
 * @author Christian Pesch
 */

public class Kml20Format extends KmlFormat {
    private static final Logger log = Logger.getLogger(Kml20Format.class.getName());

    public String getName() {
        return "Google Earth 3 (*" + getExtension() + ")";
    }

    public List<KmlRoute> read(InputStream source, CompactCalendar startDate) throws IOException {
        try {
            return internalRead(source, startDate);
        } catch (JAXBException e) {
            log.fine("Error reading KML 2.0 from " + source + ": " + e.getMessage());
            return null;
        }
    }

    List<KmlRoute> internalRead(InputStream source, CompactCalendar startDate) throws IOException, JAXBException {
        Object o = KmlUtil.unmarshal20(source);
        if (o instanceof Kml) {
            Kml kml = (Kml) o;
            return extractTracks(kml.getDocument(), kml.getFolder(), startDate);
        }
        if (o instanceof Document) {
            Document document = (Document) o;
            return extractTracks(document, null, startDate);
        }
        if (o instanceof Folder) {
            Folder folder = (Folder) o;
            return extractTracks(null, folder, startDate);
        }
        return null;
    }

    private List<KmlRoute> extractTracks(Document document, Folder folder, CompactCalendar startDate) {
        List<Object> elements = null;
        if (document != null && document.getDocumentOrFolderOrGroundOverlay().size() > 0)
            elements = document.getDocumentOrFolderOrGroundOverlay();

        if (folder != null && folder.getDocumentOrFolderOrGroundOverlay().size() > 0)
            elements = folder.getDocumentOrFolderOrGroundOverlay();

        if (elements == null)
            return null;

        List<KmlRoute> routes = extractTracks(extractName(elements), extractDescriptionList(elements), elements, startDate);
        commentRoutePositions(routes);
        return routes;
    }

    private JAXBElement findElement(List elements, String name) {
        for (Object element : elements) {
            if (element instanceof JAXBElement) {
                JAXBElement jaxbElement = (JAXBElement) element;
                if (name.equals(jaxbElement.getName().getLocalPart()))
                    return jaxbElement;
            }
        }
        return null;
    }

    private String extractName(List<Object> elements) {
        JAXBElement name = findElement(elements, "name");
        return name != null ? trim((String) name.getValue()) : null;
    }

    private String extractDescription(List<Object> elements) {
        JAXBElement name = findElement(elements, "description");
        if (name == null)
            return null;
        String string = (String) name.getValue();
        if (string == null)
            return null;
        string = string.replace("&#160;", " ");
        string = string.replace("&#169;", "(c)");
        return trim(string);
    }

    private String extractTime(TimeInstant timeInstant) {
        return timeInstant != null ? timeInstant.getTimePosition() : null;
    }

    private Calendar extractTime(List<Object> elements) {
        JAXBElement element = findElement(elements, "TimePeriod");
        if (element == null)
            return null;
        TimePeriod timePeriod = (TimePeriod) element.getValue();
        String time = "";
        if (timePeriod.getBegin() != null)
            time = extractTime(timePeriod.getBegin().getTimeInstant());
        if (time == null && timePeriod.getEnd() != null)
            time = extractTime(timePeriod.getEnd().getTimeInstant());
        return time != null ? ISO8601.parse(time) : null;
    }

    private List<String> extractDescriptionList(List<Object> elements) {
        JAXBElement name = findElement(elements, "description");
        return name != null ? asDescription((String) name.getValue()) : null;
    }

    private List<Folder> findFolders(List<Object> elements) {
        List<Folder> folders = new ArrayList<Folder>();
        for (Object element : elements) {
            if (element instanceof Folder) {
                folders.add((Folder) element);
            }
        }
        return folders;
    }

    private List<Placemark> findPlacemarks(List<Object> elements) {
        List<Placemark> placemarks = new ArrayList<Placemark>();
        for (Object element : elements) {
            if (element instanceof Placemark) {
                placemarks.add((Placemark) element);
            }
        }
        return placemarks;
    }

    private List<NetworkLink> findNetworkLinks(List<Object> elements) {
        List<NetworkLink> networkLinks = new ArrayList<NetworkLink>();
        for (Object element : elements) {
            if (element instanceof NetworkLink) {
                networkLinks.add((NetworkLink) element);
            }
        }
        return networkLinks;
    }

    private List<KmlRoute> extractTracks(String name, List<String> description, List<Object> elements, CompactCalendar startDate) {
        List<KmlRoute> result = new ArrayList<KmlRoute>();

        List<Placemark> placemarks = findPlacemarks(elements);
        result.addAll(extractWayPointsAndTracksFromPlacemarks(name, description, placemarks, startDate));

        List<NetworkLink> networkLinks = findNetworkLinks(elements);
        result.addAll(extractWayPointsAndTracksFromNetworkLinks(networkLinks));

        List<Folder> folders = findFolders(elements);
        for (Folder folder : folders) {
            List<Object> overlays = folder.getDocumentOrFolderOrGroundOverlay();
            String folderName = concatPath(name, extractName(overlays));
            result.addAll(extractTracks(folderName, description, overlays, startDate));
        }

        return result;
    }

    private List<KmlRoute> extractWayPointsAndTracksFromPlacemarks(String name, List<String> description, List<Placemark> placemarks, CompactCalendar startDate) {
        List<KmlRoute> result = new ArrayList<KmlRoute>();

        List<KmlPosition> wayPoints = new ArrayList<KmlPosition>();
        for (Placemark placemark : placemarks) {
            String placemarkName = asComment(extractName(placemark.getDescriptionOrNameOrSnippet()),
                    extractDescription(placemark.getDescriptionOrNameOrSnippet()));

            List<KmlPosition> positions = extractPositions(placemark.getDescriptionOrNameOrSnippet());
            if (positions.size() == 1) {
                // all placemarks with one position form one waypoint route
                KmlPosition wayPoint = positions.get(0);
                enrichPosition(wayPoint, extractTime(placemark.getDescriptionOrNameOrSnippet()), placemarkName, extractDescription(placemark.getDescriptionOrNameOrSnippet()), startDate);
                wayPoints.add(wayPoint);
            } else {
                // each placemark with more than one position is one track
                String routeName = concatPath(name, placemarkName);
                List<String> routeDescription = extractDescriptionList(placemark.getDescriptionOrNameOrSnippet());
                if (routeDescription == null)
                    routeDescription = description;
                RouteCharacteristics characteristics = parseCharacteristics(routeName, Track);
                result.add(new KmlRoute(this, characteristics, routeName, routeDescription, positions));
            }
        }
        if (wayPoints.size() != 0) {
            RouteCharacteristics characteristics = parseCharacteristics(name, Waypoints);
            result.add(0, new KmlRoute(this, characteristics, name, description, wayPoints));
        }
        return result;
    }

    private List<KmlRoute> extractWayPointsAndTracksFromNetworkLinks(List<NetworkLink> networkLinks) {
        List<KmlRoute> result = new ArrayList<KmlRoute>();
        for (NetworkLink networkLink : networkLinks) {
            String url = networkLink.getUrl().getHref();
            List<KmlRoute> routes = parseRouteFromUrl(url);
            if (routes != null)
                result.addAll(routes);
        }
        return result;
    }

    private List<KmlPosition> extractPositions(LineString lineString) {
        List<KmlPosition> result = new ArrayList<KmlPosition>();
        for (GoogleMapsPosition position : parsePositions(lineString.getCoordinates())) {
            result.add(asKmlPosition(position));
        }
        return result;
    }

    private List<KmlPosition> extractPositions(List<Object> elements) {
        List<KmlPosition> result = new ArrayList<KmlPosition>();
        for (Object element : elements) {
            if (element instanceof Point) {
                Point point = (Point) element;
                result.add(asKmlPosition(parsePosition(point.getCoordinates(), null)));
            }
            if (element instanceof LineString) {
                LineString lineString = (LineString) element;
                result.addAll(extractPositions(lineString));
            }
            if (element instanceof MultiGeometry) {
                MultiGeometry multiGeometry = (MultiGeometry) element;
                result.addAll(extractPositions(multiGeometry.getExtrudeOrTessellateOrAltitudeMode()));
            }
            if (element instanceof GeometryCollection) {
                GeometryCollection geometryCollection = (GeometryCollection) element;
                for (LineString lineString : geometryCollection.getLineString())
                    result.addAll(extractPositions(lineString));
            }
        }
        return result;
    }

    private Folder createWayPoints(KmlRoute route) {
        ObjectFactory objectFactory = new ObjectFactory();
        Folder folder = objectFactory.createFolder();
        List<Object> folderList = folder.getDocumentOrFolderOrGroundOverlay();
        folderList.add(objectFactory.createName(WAYPOINTS));
        folderList.add(objectFactory.createDescription(asDescription(route.getDescription())));
        for (KmlPosition position : route.getPositions()) {
            Placemark placemark = objectFactory.createPlacemark();
            folderList.add(placemark);
            List<Object> placemarkList = placemark.getDescriptionOrNameOrSnippet();
            placemarkList.add(objectFactory.createName(asName(position.getComment())));
            placemarkList.add(objectFactory.createDescription(asDesc(position.getComment())));
            placemarkList.add(objectFactory.createVisibility(Boolean.FALSE));
            if (position.getTime() != null)
                placemarkList.add(objectFactory.createTimePosition(ISO8601.format(position.getTime())));
            Point point = objectFactory.createPoint();
            placemarkList.add(point);
            point.setCoordinates(createCoordinates(position, false));
        }
        return folder;
    }

    private Placemark createRoute(KmlRoute route) {
        ObjectFactory objectFactory = new ObjectFactory();
        Placemark placemark = objectFactory.createPlacemark();
        List<Object> placemarkList = placemark.getDescriptionOrNameOrSnippet();
        placemarkList.add(objectFactory.createName(ROUTE + ": " + createPlacemarkName(route)));
        placemarkList.add(objectFactory.createStyleUrl("#" + ROUTE_LINE_STYLE));
        MultiGeometry multiGeometry = objectFactory.createMultiGeometry();
        placemarkList.add(multiGeometry);
        LineString lineString = objectFactory.createLineString();
        multiGeometry.getExtrudeOrTessellateOrAltitudeMode().add(lineString);
        StringBuilder coordinates = new StringBuilder();
        for (KmlPosition position : route.getPositions()) {
            coordinates.append(createCoordinates(position, false)).append(" ");
        }
        lineString.setCoordinates(coordinates.toString());
        return placemark;
    }

    private Placemark createTrack(KmlRoute route) {
        ObjectFactory objectFactory = new ObjectFactory();
        Placemark placemark = objectFactory.createPlacemark();
        List<Object> placemarkList = placemark.getDescriptionOrNameOrSnippet();
        placemarkList.add(objectFactory.createName(TRACK + ": " + createPlacemarkName(route)));
        placemarkList.add(objectFactory.createStyleUrl("#" + TRACK_LINE_STYLE));
        LineString lineString = objectFactory.createLineString();
        placemarkList.add(lineString);
        StringBuilder coordinates = new StringBuilder();
        for (KmlPosition position : route.getPositions()) {
            coordinates.append(createCoordinates(position, false)).append(" ");
        }
        lineString.setCoordinates(coordinates.toString());
        return placemark;
    }

    private Style createLineStyle(String styleName, float width, byte[] color) {
        ObjectFactory objectFactory = new ObjectFactory();
        Style style = objectFactory.createStyle();
        style.setId(styleName);
        LineStyle lineStyle = objectFactory.createLineStyle();
        style.setLineStyle(lineStyle);
        lineStyle.setColor(color);
        lineStyle.setWidth(new Float(width).intValue());
        return style;
    }

    private Kml createKml(KmlRoute route) {
        ObjectFactory objectFactory = new ObjectFactory();
        Kml kml = objectFactory.createKml();
        Folder root = objectFactory.createFolder();
        kml.setFolder(root);
        List<Object> rootList = root.getDocumentOrFolderOrGroundOverlay();
        rootList.add(objectFactory.createName(createDocumentName(route)));
        rootList.add(objectFactory.createDescription(asDescription(route.getDescription())));
        rootList.add(objectFactory.createOpen(Boolean.TRUE));

        rootList.add(createLineStyle(ROUTE_LINE_STYLE, getLineWidth(), getRouteLineColor()));
        rootList.add(createLineStyle(TRACK_LINE_STYLE, getLineWidth(), getTrackLineColor()));

        rootList.add(createWayPoints(route));
        rootList.add(createTrack(route));
        return kml;
    }

    private Kml createKml(List<KmlRoute> routes) {
        ObjectFactory objectFactory = new ObjectFactory();
        Kml kml = objectFactory.createKml();
        Folder root = objectFactory.createFolder();
        kml.setFolder(root);
        List<Object> rootList = root.getDocumentOrFolderOrGroundOverlay();
        rootList.add(objectFactory.createOpen(Boolean.TRUE));

        rootList.add(createLineStyle(ROUTE_LINE_STYLE, getLineWidth(), getRouteLineColor()));
        rootList.add(createLineStyle(TRACK_LINE_STYLE, getLineWidth(), getTrackLineColor()));

        String name = "";
        List<String> description = new ArrayList<String>();
        for (KmlRoute route : routes) {
            name += createDocumentName(route);
            if (route.getDescription() != null)
                description.addAll(route.getDescription());

            switch (route.getCharacteristics()) {
                case Waypoints:
                    rootList.add(createWayPoints(route));
                    break;
                case Route:
                    rootList.add(createRoute(route));
                    break;
                case Track:
                    rootList.add(createTrack(route));
                    break;
                default:
                    throw new IllegalArgumentException("Unknown RouteCharacteristics " + route.getCharacteristics());
            }
        }
        rootList.add(objectFactory.createName(name));
        rootList.add(objectFactory.createDescription(asDescription(description)));
        return kml;
    }

    public void write(KmlRoute route, OutputStream target, int startIndex, int endIndex) throws IOException {
        try {
            KmlUtil.marshal20(createKml(route), target);
        } catch (JAXBException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public void write(List<KmlRoute> routes, OutputStream target) throws IOException {
        try {
            KmlUtil.marshal20(createKml(routes), target);
        } catch (JAXBException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
