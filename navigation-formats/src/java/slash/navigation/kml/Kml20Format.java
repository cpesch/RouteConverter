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
import slash.navigation.RouteComments;
import slash.navigation.kml.binding20.*;
import slash.navigation.util.Conversion;
import slash.navigation.util.ISO8601;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.StringTokenizer;
import java.util.logging.Logger;

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

    public List<KmlRoute> read(File source, Calendar startDate) throws IOException {
        try {
            return read(new FileInputStream(source));
        } catch (JAXBException e) {
            log.fine("Error reading " + source + ": " + e.getMessage());
            return null;
        }
    }

    List<KmlRoute> read(InputStream in) throws JAXBException {
        Object o = KmlUtil.unmarshal20(in);
        if (o instanceof Kml) {
            Kml kml = (Kml) o;
            return extractTracks(kml.getDocument(), kml.getFolder());
        }
        if (o instanceof Document) {
            Document document = (Document) o;
            return extractTracks(document, null);
        }
        if (o instanceof Folder) {
            Folder folder = (Folder) o;
            return extractTracks(null, folder);
        }
        return null;
    }

    private List<KmlRoute> extractTracks(Document document, Folder folder) {
        List<Object> elements = null;
        if (document != null && document.getDocumentOrFolderOrGroundOverlay().size() > 0)
            elements = document.getDocumentOrFolderOrGroundOverlay();

        if (folder != null && folder.getDocumentOrFolderOrGroundOverlay().size() > 0)
            elements = folder.getDocumentOrFolderOrGroundOverlay();

        if (elements == null)
            return null;

        List<KmlRoute> routes = extractTracks(extractName(elements), extractDescriptionList(elements), elements);
        RouteComments.commentRoutePositions(routes);
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
        return name != null ? Conversion.trim((String) name.getValue()) : null;
    }

    private String extractDescription(List<Object> elements) {
        JAXBElement name = findElement(elements, "description");
        return name != null ? (String) name.getValue() : null;
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

    private List<KmlRoute> extractTracks(String name, List<String> description, List<Object> elements) {
        List<KmlRoute> result = new ArrayList<KmlRoute>();

        List<Placemark> placemarks = findPlacemarks(elements);
        result.addAll(extractWayPointsAndTracksFromPlacemarks(name, description, placemarks));

        List<NetworkLink> networkLinks = findNetworkLinks(elements);
        result.addAll(extractWayPointsAndTracksFromNetworkLinks(networkLinks));

        List<Folder> folders = findFolders(elements);
        for (Folder folder : folders) {
            List<Object> overlays = folder.getDocumentOrFolderOrGroundOverlay();
            String folderName = (name != null ? name + "/" : "") + extractName(overlays);
            result.addAll(extractTracks(folderName, description, overlays));
        }

        return result;
    }

    private List<KmlRoute> extractWayPointsAndTracksFromPlacemarks(String name, List<String> description, List<Placemark> placemarks) {
        List<KmlRoute> result = new ArrayList<KmlRoute>();

        List<KmlPosition> wayPoints = new ArrayList<KmlPosition>();
        for (Placemark placemark : placemarks) {
            String placemarkName = asComment(extractName(placemark.getDescriptionOrNameOrSnippet()),
                    extractDescription(placemark.getDescriptionOrNameOrSnippet()));
            // cannot extract time in KML 2.0
            List<KmlPosition> positions = extractPositions(placemark.getDescriptionOrNameOrSnippet());

            if (positions.size() == 1) {
                // all placemarks with one position form one waypoint route
                KmlPosition wayPoint = positions.get(0);
                if (wayPoint.getComment() == null)
                    wayPoint.setComment(placemarkName);
                wayPoints.add(positions.get(0));
            } else {
                // each placemark with more than one position is one track
                String routeName = (name != null ? name + "/" : "") + placemarkName;
                List<String> routeDescription = extractDescriptionList(placemark.getDescriptionOrNameOrSnippet());
                if (routeDescription == null)
                    routeDescription = description;
                RouteCharacteristics characteristics = parseCharacteristics(routeName);
                result.add(new KmlRoute(this, characteristics, routeName, routeDescription, positions));
            }
        }
        if (wayPoints.size() != 0) {
            RouteCharacteristics characteristics = parseCharacteristics(name);
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

    private List<KmlPosition> extractPositions(List<Object> elements) {
        List<KmlPosition> positions = new ArrayList<KmlPosition>();
        for (Object element : elements) {
            if (element instanceof Point) {
                Point point = (Point) element;
                positions.add(parsePosition(point.getCoordinates(), null));
            }
            if (element instanceof LineString) {
                LineString lineString = (LineString) element;
                String coordinates = lineString.getCoordinates().trim();
                // for DataLogger which has spaces between commata and numbers
                String delimiter = "\n\r\t";
                if (!(coordinates.contains("\n") || coordinates.contains("\r") || coordinates.contains("\t")))
                    delimiter += " ";
                StringTokenizer tokenizer = new StringTokenizer(coordinates, delimiter);
                while (tokenizer.hasMoreTokens()) {
                    String token = tokenizer.nextToken().trim();
                    if (token.length() > 0)
                        positions.add(parsePosition(token, null));
                }
            }
            if (element instanceof MultiGeometry) {
                MultiGeometry multiGeometry = (MultiGeometry) element;
                positions.addAll(extractPositions(multiGeometry.getExtrudeOrTessellateOrAltitudeMode()));
            }
        }
        return positions;
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
            placemarkList.add(objectFactory.createName(position.getComment()));
            placemarkList.add(objectFactory.createVisibility(Boolean.FALSE));
            if (position.getTime() != null)
                placemarkList.add(objectFactory.createTimePosition(ISO8601.format(position.getTime())));
            Point point = objectFactory.createPoint();
            placemarkList.add(point);
            point.setCoordinates(Conversion.formatDoubleAsString(position.getLongitude()) + "," +
                    Conversion.formatDoubleAsString(position.getLatitude()) + "," +
                    formatElevation(position.getElevation()));
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
        StringBuffer coordinates = new StringBuffer();
        for (KmlPosition position : route.getPositions()) {
            coordinates.append(Conversion.formatDoubleAsString(position.getLongitude())).append(",").
                    append(Conversion.formatDoubleAsString(position.getLatitude())).append(",").
                    append(formatElevation(position.getElevation())).append(" ");
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
        StringBuffer coordinates = new StringBuffer();
        for (KmlPosition position : route.getPositions()) {
            coordinates.append(Conversion.formatDoubleAsString(position.getLongitude())).append(",").
                    append(Conversion.formatDoubleAsString(position.getLatitude())).append(",").
                    append(formatElevation(position.getElevation())).append(" ");
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

        // TODO no TIME for track - exchange waypoints and track?
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

    public void write(KmlRoute route, File target, int startIndex, int endIndex, boolean numberPositionNames) throws IOException {
        try {
            KmlUtil.marshal20(createKml(route), target);
        } catch (JAXBException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public void write(List<KmlRoute> routes, File target) throws IOException {
        try {
            KmlUtil.marshal20(createKml(routes), target);
        } catch (JAXBException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
