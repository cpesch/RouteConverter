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

package slash.navigation.gpx;

import org.w3c.dom.Element;
import slash.navigation.BaseNavigationFormat;
import slash.navigation.RouteCharacteristics;
import slash.navigation.gpx.binding11.*;
import slash.navigation.gpx.garmin3.AutoroutePointT;
import slash.navigation.gpx.garmin3.RoutePointExtensionT;
import slash.navigation.util.Conversion;
import slash.navigation.util.CompactCalendar;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;
import java.util.logging.Logger;

/**
 * Reads and writes GPS Exchange Format 1.1 (.gpx) files.
 *
 * @author Christian Pesch
 */

public class Gpx11Format extends GpxFormat {
    private static final Logger log = Logger.getLogger(Gpx11Format.class.getName());
    protected static final String VERSION = "1.1";

    public String getName() {
        return "GPS Exchange Format " + VERSION + " (*" + getExtension() + ")";
    }

    protected List<GpxRoute> process(GpxType gpxType) {
        if (gpxType == null || !VERSION.equals(gpxType.getVersion()))
            return null;

        List<GpxRoute> result = new ArrayList<GpxRoute>();
        GpxRoute wayPointsAsRoute = extractWayPoints(gpxType);
        if (wayPointsAsRoute != null)
            result.add(wayPointsAsRoute);
        result.addAll(extractRoutes(gpxType));
        result.addAll(extractTracks(gpxType));
        return result;
    }

    public List<GpxRoute> read(InputStream source, CompactCalendar startDate) throws IOException {
        try {
            GpxType gpxType = GpxUtil.unmarshal11(source);
            return process(gpxType);
        } catch (JAXBException e) {
            log.fine("Error reading " + source + ": " + e.getMessage());
            return null;
        }
    }

    private List<GpxRoute> extractRoutes(GpxType gpxType) {
        List<GpxRoute> result = new ArrayList<GpxRoute>();

        for (RteType rteType : gpxType.getRte()) {
            String name = rteType.getName();
            String desc = rteType.getDesc();
            List<String> descriptions = asDescription(desc);
            List<GpxPosition> positions = extractRoute(rteType);
            result.add(new GpxRoute(this, RouteCharacteristics.Route, name, descriptions, positions, gpxType, rteType));

            // Garmin Extensions v3
            if (rteType.getExtensions() != null && rteType.getExtensions().getAny().size() > 0) {
                List<GpxPosition> extendedPositions = extractRouteWithGarminExtensions(rteType);
                result.add(new GpxRoute(this, RouteCharacteristics.Track, name, descriptions, extendedPositions, gpxType, rteType));
            }
        }

        return result;
    }

    private GpxRoute extractWayPoints(GpxType gpxType) {
        String name = gpxType.getMetadata() != null ? gpxType.getMetadata().getName() : null;
        String desc = gpxType.getMetadata() != null ? gpxType.getMetadata().getDesc() : null;
        List<String> descriptions = asDescription(desc);
        List<GpxPosition> positions = extractWayPoints(gpxType.getWpt());
        return positions.size() == 0 ? null : new GpxRoute(this, RouteCharacteristics.Waypoints, name, descriptions, positions, gpxType);
    }

    private List<GpxRoute> extractTracks(GpxType gpxType) {
        List<GpxRoute> result = new ArrayList<GpxRoute>();

        for (TrkType trkType : gpxType.getTrk()) {
            String name = trkType.getName();
            String desc = trkType.getDesc();
            List<String> descriptions = asDescription(desc);
            List<GpxPosition> positions = extractTrack(trkType);
            result.add(new GpxRoute(this, RouteCharacteristics.Track, name, descriptions, positions, gpxType, trkType));
        }

        return result;
    }

    private List<GpxPosition> extractRoute(RteType rteType) {
        List<GpxPosition> positions = new ArrayList<GpxPosition>();
        if (rteType != null) {
            for (WptType wptType : rteType.getRtept()) {
                positions.add(new GpxPosition(wptType.getLon(), wptType.getLat(), wptType.getEle(), parseSpeed(wptType.getCmt()), parseTime(wptType.getTime()), asComment(wptType.getName(), wptType.getDesc()), wptType));
            }
        }
        return positions;
    }

    private List<GpxPosition> extractRouteWithGarminExtensions(RteType rteType) {
        List<GpxPosition> positions = new ArrayList<GpxPosition>();
        if (rteType != null) {
            for (WptType wptType : rteType.getRtept()) {
                positions.add(new GpxPosition(wptType.getLon(), wptType.getLat(), wptType.getEle(), parseSpeed(wptType.getCmt()), parseTime(wptType.getTime()), asComment(wptType.getName(), wptType.getDesc()), wptType));

                for (Object any : wptType.getExtensions().getAny()) {
                    if(any instanceof JAXBElement) {
                        Object anyValue = ((JAXBElement) any).getValue();
                        if (anyValue instanceof RoutePointExtensionT) {
                            RoutePointExtensionT routePoint = (RoutePointExtensionT) anyValue;
                            for (AutoroutePointT autoroutePoint : routePoint.getRpt()) {
                                positions.add(new GpxPosition(autoroutePoint.getLon(), autoroutePoint.getLat(), null, null, null, null, null));
                            }
                        }
                    }
                }
            }
        }
        return positions;
    }

    private List<GpxPosition> extractWayPoints(List<WptType> wptTypes) {
        List<GpxPosition> positions = new ArrayList<GpxPosition>();
        for (WptType wptType : wptTypes) {
            positions.add(new GpxPosition(wptType.getLon(), wptType.getLat(), wptType.getEle(), parseSpeed(wptType.getCmt()), parseTime(wptType.getTime()), asComment(wptType.getName(), wptType.getDesc()), wptType));
        }
        return positions;
    }

    private List<GpxPosition> extractTrack(TrkType trkType) {
        List<GpxPosition> positions = new ArrayList<GpxPosition>();
        if (trkType != null) {
            for (TrksegType trkSegType : trkType.getTrkseg()) {
                for (WptType wptType : trkSegType.getTrkpt()) {
                    Double speed = getSpeed(wptType);
                    if (speed == null)
                        speed = parseSpeed(wptType.getCmt());
                    positions.add(new GpxPosition(wptType.getLon(), wptType.getLat(), wptType.getEle(), speed, parseTime(wptType.getTime()), asComment(wptType.getName(), wptType.getDesc()), wptType));
                }
            }
        }
        return positions;
    }

    private Double getSpeed(WptType wptType) {
        Double speed = null;
        if (wptType.getExtensions() != null) {
            for (Object any : wptType.getExtensions().getAny()) {
                if (any instanceof Element) {
                    Element element = (Element) any;
                    if ("speed".equals(element.getLocalName()))
                        speed = Conversion.parseDouble(element.getTextContent());
                }
            }
        }
        return speed;
    }

    private void setSpeed(WptType wptType, Double speed) {
        if (wptType.getExtensions() == null)
            wptType.setExtensions(new ObjectFactory().createExtensionsType());
        List<Object> anys = wptType.getExtensions().getAny();

        boolean foundSpeed = false;
        Iterator<Object> iterator = anys.iterator();
        while (iterator.hasNext()) {
            Object any = iterator.next();

            // this is parsed
            if (any instanceof Element) {
                Element element = (Element) any;
                if ("speed".equals(element.getLocalName())) {
                    if(foundSpeed || speed == null)
                        iterator.remove();
                    else {
                        element.setTextContent(Conversion.formatDoubleAsString(speed));
                        foundSpeed = true;
                    }
                }
            }

            // this is if I create the extensions with JAXB
            if (any instanceof JAXBElement) {
                JAXBElement element = (JAXBElement) any;
                if ("speed".equals(element.getName().getLocalPart())) {
                    if(foundSpeed || speed == null)
                        iterator.remove();
                    else {
                        element.setValue(Conversion.formatDoubleAsString(speed));
                        foundSpeed = true;
                    }
                }
            }
        }
        if (!foundSpeed && speed != null) {
            slash.navigation.gpx.trekbuddy.ObjectFactory tbFactory = new slash.navigation.gpx.trekbuddy.ObjectFactory();
            anys.add(tbFactory.createSpeed(Conversion.formatDouble(speed)));
        }

        if(anys.size() == 0)
            wptType.setExtensions(null);
    }

    private void clearWptType(WptType wptType) {
        if (!isWriteElevation())
            wptType.setEle(null);
        if (!isWriteSpeed())
            setSpeed(wptType, null);
        if (!isWriteTime())
            wptType.setTime(null);
        if (!isWriteName()) {
            wptType.setName(null);
            wptType.setDesc(null);
        }
    }

    private List<WptType> createWayPoints(GpxRoute route) {
        ObjectFactory objectFactory = new ObjectFactory();
        List<WptType> wptTypes = new ArrayList<WptType>();
        for (GpxPosition position : route.getPositions()) {
            WptType wptType = position.getOrigin(WptType.class);
            if (wptType == null)
                wptType = objectFactory.createWptType();
            wptType.setLat(Conversion.formatDouble(position.getLatitude()));
            wptType.setLon(Conversion.formatDouble(position.getLongitude()));
            wptType.setEle(Conversion.formatDouble(position.getElevation()));
            setSpeed(wptType, position.getSpeed());
            wptType.setTime(formatTime(position.getTime()));
            wptType.setName(asName(position.getComment()));
            wptType.setDesc(asDesc(position.getComment(), wptType.getDesc()));
            clearWptType(wptType);
            wptTypes.add(wptType);
        }
        return wptTypes;
    }

    private List<RteType> createRoute(GpxRoute route) {
        ObjectFactory objectFactory = new ObjectFactory();
        List<RteType> rteTypes = new ArrayList<RteType>();
        RteType rteType = route.getOrigin(RteType.class);
        if (rteType != null)
            rteType.getRtept().clear();
        else
            rteType = objectFactory.createRteType();
        if (isWriteName()) {
            rteType.setName(route.getName());
            rteType.setDesc(asDescription(route.getDescription()));
        }
        rteTypes.add(rteType);
        for (GpxPosition position : route.getPositions()) {
            WptType wptType = position.getOrigin(WptType.class);
            if (wptType == null)
                wptType = objectFactory.createWptType();
            wptType.setLat(Conversion.formatDouble(position.getLatitude()));
            wptType.setLon(Conversion.formatDouble(position.getLongitude()));
            wptType.setEle(Conversion.formatDouble(position.getElevation()));
            setSpeed(wptType, position.getSpeed());
            wptType.setName(asName(position.getComment()));
            wptType.setDesc(asDesc(position.getComment(), wptType.getDesc()));
            wptType.setTime(formatTime(position.getTime()));
            clearWptType(wptType);
            rteType.getRtept().add(wptType);
        }
        return rteTypes;
    }

    private List<TrkType> createTrack(GpxRoute route) {
        ObjectFactory objectFactory = new ObjectFactory();
        List<TrkType> trkTypes = new ArrayList<TrkType>();
        TrkType trkType = route.getOrigin(TrkType.class);
        if (trkType != null)
            trkType.getTrkseg().clear();
        else
            trkType = objectFactory.createTrkType();
        if (isWriteName()) {
            trkType.setName(route.getName());
            trkType.setDesc(asDescription(route.getDescription()));
        }
        trkTypes.add(trkType);
        TrksegType trksegType = objectFactory.createTrksegType();
        for (GpxPosition position : route.getPositions()) {
            WptType wptType = position.getOrigin(WptType.class);
            if (wptType == null)
                wptType = objectFactory.createWptType();
            wptType.setLat(Conversion.formatDouble(position.getLatitude()));
            wptType.setLon(Conversion.formatDouble(position.getLongitude()));
            wptType.setEle(Conversion.formatDouble(position.getElevation()));
            setSpeed(wptType, position.getSpeed());
            wptType.setName(asName(position.getComment()));
            wptType.setDesc(asDesc(position.getComment(), wptType.getDesc()));
            wptType.setTime(formatTime(position.getTime()));
            clearWptType(wptType);
            trksegType.getTrkpt().add(wptType);
        }
        trkType.getTrkseg().add(trksegType);
        return trkTypes;
    }

    private GpxType recycleGpxType(GpxRoute route) {
        GpxType gpxType = route.getOrigin(GpxType.class);
        if (gpxType != null) {
            gpxType.getRte().clear();
            gpxType.getTrk().clear();
            gpxType.getWpt().clear();
        }
        return gpxType;
    }

    private GpxType createGpxType(GpxRoute route) {
        ObjectFactory objectFactory = new ObjectFactory();

        GpxType gpxType = recycleGpxType(route);
        if (gpxType == null)
            gpxType = objectFactory.createGpxType();
        gpxType.setCreator(BaseNavigationFormat.GENERATED_BY);
        gpxType.setVersion(VERSION);

        MetadataType metadataType = gpxType.getMetadata();
        if(metadataType == null) {
            metadataType = objectFactory.createMetadataType();
            gpxType.setMetadata(metadataType);
        }
        if (isWriteName()) {
            metadataType.setName(route.getName());
            metadataType.setDesc(asDescription(route.getDescription()));
        }

        gpxType.getWpt().addAll(createWayPoints(route));
        gpxType.getRte().addAll(createRoute(route));
        gpxType.getTrk().addAll(createTrack(route));
        return gpxType;
    }

    private GpxType createGpxType(List<GpxRoute> routes) {
        ObjectFactory objectFactory = new ObjectFactory();
        GpxType gpxType = null;
        for(GpxRoute route : routes) {
            gpxType = recycleGpxType(route);
            if(gpxType != null)
                break;
        }
        if (gpxType == null)
            gpxType = objectFactory.createGpxType();
        gpxType.setCreator(BaseNavigationFormat.GENERATED_BY);
        gpxType.setVersion(VERSION);

        for (GpxRoute route : routes) {
            switch (route.getCharacteristics()) {
                case Waypoints:
                    gpxType.getWpt().addAll(createWayPoints(route));
                    break;
                case Route:
                    gpxType.getRte().addAll(createRoute(route));
                    break;
                case Track:
                    gpxType.getTrk().addAll(createTrack(route));
                    break;
                default:
                    throw new IllegalArgumentException("Unknown RouteCharacteristics " + route.getCharacteristics());
            }
        }
        return gpxType;
    }

    public void write(GpxRoute route, File target, int startIndex, int endIndex) {
        try {
            GpxUtil.marshal11(createGpxType(route), target);
        } catch (JAXBException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public void write(List<GpxRoute> routes, File target) {
        try {
            GpxUtil.marshal11(createGpxType(routes), target);
        } catch (JAXBException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
