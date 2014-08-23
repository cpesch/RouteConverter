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
import org.w3c.dom.Node;
import slash.common.type.CompactCalendar;
import slash.navigation.base.ParserContext;
import slash.navigation.gpx.binding11.*;
import slash.navigation.gpx.garmin3.AutoroutePointT;
import slash.navigation.gpx.garmin3.RoutePointExtensionT;
import slash.navigation.gpx.trackpoint2.TrackPointExtensionT;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static slash.common.io.Transfer.*;
import static slash.navigation.base.RouteCharacteristics.*;
import static slash.navigation.common.NavigationConversion.*;
import static slash.navigation.gpx.GpxUtil.marshal11;
import static slash.navigation.gpx.GpxUtil.unmarshal11;

/**
 * Reads and writes GPS Exchange Format 1.1 (.gpx) files.
 *
 * @author Christian Pesch
 */

public class Gpx11Format extends GpxFormat {
    static final String VERSION = "1.1";

    public String getName() {
        return "GPS Exchange Format " + VERSION + " (*" + getExtension() + ")";
    }

    void process(GpxType gpxType,ParserContext<GpxRoute> context) {
        if (gpxType == null || !VERSION.equals(gpxType.getVersion()))
            return;

        boolean hasSpeedInKiloMeterPerHourInsteadOfMeterPerSecond = gpxType.getCreator() != null &&
                ("Whatever".equals(gpxType.getCreator()));
        GpxRoute wayPointsAsRoute = extractWayPoints(gpxType, hasSpeedInKiloMeterPerHourInsteadOfMeterPerSecond);
        if (wayPointsAsRoute != null)
            context.appendRoute(wayPointsAsRoute);
        context.appendRoutes(extractRoutes(gpxType, hasSpeedInKiloMeterPerHourInsteadOfMeterPerSecond));
        context.appendRoutes(extractTracks(gpxType, hasSpeedInKiloMeterPerHourInsteadOfMeterPerSecond));
    }

    public void read(InputStream source, CompactCalendar startDate, ParserContext<GpxRoute> context) throws Exception {
        GpxType gpxType = unmarshal11(source);
        process(gpxType, context);
    }

    private List<GpxRoute> extractRoutes(GpxType gpxType, boolean hasSpeedInKiloMeterPerHourInsteadOfMeterPerSecond) {
        List<GpxRoute> result = new ArrayList<GpxRoute>();

        for (RteType rteType : gpxType.getRte()) {
            String name = rteType.getName();
            String desc = rteType.getDesc();
            List<String> descriptions = asDescription(desc);
            List<GpxPosition> positions = extractRoute(rteType, hasSpeedInKiloMeterPerHourInsteadOfMeterPerSecond);
            result.add(new GpxRoute(this, Route, name, descriptions, positions, gpxType, rteType));

            // Garmin Extensions v3
            if (rteType.getExtensions() != null && rteType.getExtensions().getAny().size() > 0) {
                List<GpxPosition> extendedPositions = extractRouteWithGarminExtensions(rteType, hasSpeedInKiloMeterPerHourInsteadOfMeterPerSecond);
                result.add(new GpxRoute(this, Track, name, descriptions, extendedPositions, gpxType, rteType));
            }
        }

        return result;
    }

    private GpxRoute extractWayPoints(GpxType gpxType, boolean hasSpeedInKiloMeterPerHourInsteadOfMeterPerSecond) {
        String name = gpxType.getMetadata() != null ? gpxType.getMetadata().getName() : null;
        String desc = gpxType.getMetadata() != null ? gpxType.getMetadata().getDesc() : null;
        List<String> descriptions = asDescription(desc);
        List<GpxPosition> positions = extractWayPoints(gpxType.getWpt(), hasSpeedInKiloMeterPerHourInsteadOfMeterPerSecond);
        return positions.size() == 0 ? null : new GpxRoute(this, Waypoints, name, descriptions, positions, gpxType);
    }

    private List<GpxRoute> extractTracks(GpxType gpxType, boolean hasSpeedInKiloMeterPerHourInsteadOfMeterPerSecond) {
        List<GpxRoute> result = new ArrayList<GpxRoute>();

        for (TrkType trkType : gpxType.getTrk()) {
            String name = trkType.getName();
            String desc = trkType.getDesc();
            List<String> descriptions = asDescription(desc);
            List<GpxPosition> positions = extractTrack(trkType, hasSpeedInKiloMeterPerHourInsteadOfMeterPerSecond);
            result.add(new GpxRoute(this, Track, name, descriptions, positions, gpxType, trkType));
        }

        return result;
    }

    private List<GpxPosition> extractRoute(RteType rteType, boolean hasSpeedInKiloMeterPerHourInsteadOfMeterPerSecond) {
        List<GpxPosition> positions = new ArrayList<GpxPosition>();
        if (rteType != null) {
            for (WptType wptType : rteType.getRtept()) {
                positions.add(new GpxPosition(wptType.getLon(), wptType.getLat(), wptType.getEle(), getSpeed(wptType, hasSpeedInKiloMeterPerHourInsteadOfMeterPerSecond), getHeading(wptType), parseTime(wptType.getTime()), asDescription(wptType.getName(), wptType.getDesc()), wptType.getHdop(), wptType.getPdop(), wptType.getVdop(), wptType.getSat(), wptType));
            }
        }
        return positions;
    }

    private List<GpxPosition> extractRouteWithGarminExtensions(RteType rteType, boolean hasSpeedInKiloMeterPerHourInsteadOfMeterPerSecond) {
        List<GpxPosition> positions = new ArrayList<GpxPosition>();
        if (rteType != null) {
            for (WptType wptType : rteType.getRtept()) {
                positions.add(new GpxPosition(wptType.getLon(), wptType.getLat(), wptType.getEle(), getSpeed(wptType, hasSpeedInKiloMeterPerHourInsteadOfMeterPerSecond), getHeading(wptType), parseTime(wptType.getTime()), asDescription(wptType.getName(), wptType.getDesc()), wptType.getHdop(), wptType.getPdop(), wptType.getVdop(), wptType.getSat(), wptType));

                ExtensionsType extensions = wptType.getExtensions();
                if (extensions != null) {
                    for (Object any : extensions.getAny()) {
                        if (any instanceof JAXBElement) {
                            Object anyValue = ((JAXBElement) any).getValue();
                            if (anyValue instanceof RoutePointExtensionT) {
                                RoutePointExtensionT routePoint = (RoutePointExtensionT) anyValue;
                                for (AutoroutePointT autoroutePoint : routePoint.getRpt()) {
                                    positions.add(new GpxPosition(autoroutePoint.getLon(), autoroutePoint.getLat(), null, null, null, null, null, null, null, null, null, null));
                                }
                            }
                        }
                    }
                }
            }
        }
        return positions;
    }

    private List<GpxPosition> extractWayPoints(List<WptType> wptTypes, boolean hasSpeedInKiloMeterPerHourInsteadOfMeterPerSecond) {
        List<GpxPosition> positions = new ArrayList<GpxPosition>();
        for (WptType wptType : wptTypes) {
            positions.add(new GpxPosition(wptType.getLon(), wptType.getLat(), wptType.getEle(), getSpeed(wptType, hasSpeedInKiloMeterPerHourInsteadOfMeterPerSecond), getHeading(wptType), parseTime(wptType.getTime()), asDescription(wptType.getName(), wptType.getDesc()), wptType.getHdop(), wptType.getPdop(), wptType.getVdop(), wptType.getSat(), wptType));
        }
        return positions;
    }

    private List<GpxPosition> extractTrack(TrkType trkType, boolean hasSpeedInKiloMeterPerHourInsteadOfMeterPerSecond) {
        List<GpxPosition> positions = new ArrayList<GpxPosition>();
        if (trkType != null) {
            for (TrksegType trkSegType : trkType.getTrkseg()) {
                for (WptType wptType : trkSegType.getTrkpt()) {
                    positions.add(new GpxPosition(wptType.getLon(), wptType.getLat(), wptType.getEle(), getSpeed(wptType, hasSpeedInKiloMeterPerHourInsteadOfMeterPerSecond), getHeading(wptType), parseTime(wptType.getTime()), asDescription(wptType.getName(), wptType.getDesc()), wptType.getHdop(), wptType.getPdop(), wptType.getVdop(), wptType.getSat(), wptType));
                }
            }
        }
        return positions;
    }

    private Double getSpeed(WptType wptType, boolean hasSpeedInKiloMeterPerHourInsteadOfMeterPerSecond) {
        Double result = null;
        ExtensionsType extensions = wptType.getExtensions();
        if (extensions != null) {
            for (Object any : extensions.getAny()) {
                if (any instanceof JAXBElement) {
                    Object anyValue = ((JAXBElement) any).getValue();
                    if (anyValue instanceof TrackPointExtensionT) {
                        TrackPointExtensionT trackPoint = (TrackPointExtensionT) anyValue;
                        result = trackPoint.getSpeed();
                        // everything is converted from m/s to Km/h except for the exceptional case
                        if (!hasSpeedInKiloMeterPerHourInsteadOfMeterPerSecond)
                            result = asKmh(result);
                    }

                } else if (any instanceof Element) {
                    Element element = (Element) any;

                    // TrackPointExtension v1
                    if ("TrackPointExtension".equals(element.getLocalName())) {
                        Node firstChild = element.getFirstChild();
                        if (firstChild != null && "speed".equals(firstChild.getLocalName())) {
                            result = parseDouble(element.getTextContent());
                            // everything is converted from m/s to Km/h except for the exceptional case
                            if (!hasSpeedInKiloMeterPerHourInsteadOfMeterPerSecond)
                                result = asKmh(result);
                        }

                        // generic reading of speed elements
                    } else if ("speed".equals(element.getLocalName())) {
                        result = parseDouble(element.getTextContent());
                        // everything is converted from m/s to Km/h except for the exceptional case
                        if(!hasSpeedInKiloMeterPerHourInsteadOfMeterPerSecond)
                            result = asKmh(result);
                    }
                }
            }
        }
        if (result == null)
            result = parseSpeed(wptType.getCmt());
        if (result == null)
            result = parseSpeed(wptType.getName());
        if (result == null)
            result = parseSpeed(wptType.getDesc());
        return result;
    }

    @SuppressWarnings("unchecked")
    private void setSpeed(WptType wptType, Double speed) {
        if (wptType.getExtensions() == null)
            wptType.setExtensions(new ObjectFactory().createExtensionsType());
        @SuppressWarnings("ConstantConditions")
        List<Object> anys = wptType.getExtensions().getAny();

        boolean foundSpeed = false;
        Iterator<Object> iterator = anys.iterator();
        while (iterator.hasNext()) {
            Object any = iterator.next();

            // this is parsed
            if (any instanceof Element) {
                Element element = (Element) any;
                if ("speed".equals(element.getLocalName())) {
                    if (foundSpeed || speed == null)
                        iterator.remove();
                    else {
                        element.setTextContent(formatSpeedAsString(asMs(speed)));
                        foundSpeed = true;
                    }
                }
            }

            // this is if I create the extensions with JAXB
            if (any instanceof JAXBElement) {
                JAXBElement<String> element = (JAXBElement<String>) any;
                if ("speed".equals(element.getName().getLocalPart())) {
                    if (foundSpeed || speed == null)
                        iterator.remove();
                    else {
                        element.setValue(formatSpeedAsString(asMs(speed)));
                        foundSpeed = true;
                    }
                }
            }
        }
        if (!foundSpeed && speed != null) {
            slash.navigation.gpx.trekbuddy.ObjectFactory tbFactory = new slash.navigation.gpx.trekbuddy.ObjectFactory();
            anys.add(tbFactory.createSpeed(formatSpeed(asMs(speed))));
        }

        if (anys.size() == 0)
            wptType.setExtensions(null);
    }

    private Double getHeading(WptType wptType) {
        Double result = null;
        ExtensionsType extensions = wptType.getExtensions();
        if (extensions != null) {
            for (Object any : extensions.getAny()) {
                if (any instanceof JAXBElement) {
                    Object anyValue = ((JAXBElement) any).getValue();
                    if (anyValue instanceof TrackPointExtensionT) {
                        TrackPointExtensionT trackPoint = (TrackPointExtensionT) anyValue;
                        result = formatDouble(trackPoint.getCourse());
                    }

                } else if (any instanceof Element) {
                    Element element = (Element) any;
                    if ("course".equals(element.getLocalName()))
                        result = parseDouble(element.getTextContent());
                }
            }
        }
        if (result == null)
            result = parseHeading(wptType.getCmt());
        return result;
    }

    @SuppressWarnings("unchecked")
    private void setHeading(WptType wptType, Double heading) {
        if (wptType.getExtensions() == null)
            wptType.setExtensions(new ObjectFactory().createExtensionsType());
        @SuppressWarnings("ConstantConditions")
        List<Object> anys = wptType.getExtensions().getAny();

        boolean foundHeading = false;
        Iterator<Object> iterator = anys.iterator();
        while (iterator.hasNext()) {
            Object any = iterator.next();

            // this is parsed
            if (any instanceof Element) {
                Element element = (Element) any;
                if ("course".equals(element.getLocalName())) {
                    if (foundHeading || heading == null)
                        iterator.remove();
                    else {
                        element.setTextContent(formatHeadingAsString(heading));
                        foundHeading = true;
                    }
                }
            }

            // this is if I create the extensions with JAXB
            if (any instanceof JAXBElement) {
                JAXBElement<String> element = (JAXBElement<String>) any;
                if ("course".equals(element.getName().getLocalPart())) {
                    if (foundHeading || heading == null)
                        iterator.remove();
                    else {
                        element.setValue(formatHeadingAsString(heading));
                        foundHeading = true;
                    }
                }
            }
        }
        if (!foundHeading && heading != null) {
            slash.navigation.gpx.trekbuddy.ObjectFactory tbFactory = new slash.navigation.gpx.trekbuddy.ObjectFactory();
            anys.add(tbFactory.createCourse(formatHeading(heading)));
        }

        if (anys.size() == 0)
            wptType.setExtensions(null);
    }

    private WptType createWptType(GpxPosition position) {
        BigDecimal latitude = formatPosition(position.getLatitude());
        BigDecimal longitude = formatPosition(position.getLongitude());
        if (latitude == null || longitude == null)
            return null;
        WptType wptType = position.getOrigin(WptType.class);
        if (wptType == null)
            wptType = new ObjectFactory().createWptType();
        wptType.setLat(latitude);
        wptType.setLon(longitude);
        wptType.setEle(isWriteElevation() ? formatElevation(position.getElevation()) : null);
        setSpeed(wptType, isWriteSpeed() ? position.getSpeed() : null);
        setHeading(wptType, isWriteHeading() ? position.getHeading() : null);
        wptType.setTime(isWriteTime() ? formatTime(position.getTime()) : null);
        wptType.setName(isWriteName() ? asName(position.getDescription()) : null);
        wptType.setDesc(isWriteName() ? asDesc(position.getDescription(), wptType.getDesc()) : null);
        wptType.setHdop(isWriteAccuracy() && position.getHdop() != null ? formatBigDecimal(position.getHdop(), 6) : null);
        wptType.setPdop(isWriteAccuracy() && position.getPdop() != null ? formatBigDecimal(position.getPdop(), 6) : null);
        wptType.setVdop(isWriteAccuracy() && position.getVdop() != null ? formatBigDecimal(position.getVdop(), 6) : null);
        wptType.setSat(isWriteAccuracy() && position.getSatellites() != null ? formatInt(position.getSatellites()) : null);
        return wptType;
    }

    private List<WptType> createWayPoints(GpxRoute route, int startIndex, int endIndex) {
        List<WptType> wptTypes = new ArrayList<WptType>();
        List<GpxPosition> positions = route.getPositions();
        for (int i = startIndex; i < endIndex; i++) {
            GpxPosition position = positions.get(i);
            WptType wptType = createWptType(position);
            if (wptType != null)
                wptTypes.add(wptType);
        }
        return wptTypes;
    }

    private List<RteType> createRoute(GpxRoute route, int startIndex, int endIndex) {
        ObjectFactory objectFactory = new ObjectFactory();
        List<RteType> rteTypes = new ArrayList<RteType>();
        RteType rteType = route.getOrigin(RteType.class);
        if (rteType != null)
            rteType.getRtept().clear();
        else
            rteType = objectFactory.createRteType();
        if (isWriteMetaData()) {
            rteType.setName(asRouteName(route.getName()));
            rteType.setDesc(asDescription(route.getDescription()));
        }
        rteTypes.add(rteType);
        List<GpxPosition> positions = route.getPositions();
        for (int i = startIndex; i < endIndex; i++) {
            GpxPosition position = positions.get(i);
            WptType wptType = createWptType(position);
            if (wptType != null)
                rteType.getRtept().add(wptType);
        }
        return rteTypes;
    }

    private List<TrkType> createTrack(GpxRoute route, int startIndex, int endIndex) {
        ObjectFactory objectFactory = new ObjectFactory();
        List<TrkType> trkTypes = new ArrayList<TrkType>();
        TrkType trkType = route.getOrigin(TrkType.class);
        if (trkType != null)
            trkType.getTrkseg().clear();
        else
            trkType = objectFactory.createTrkType();
        if (isWriteMetaData()) {
            trkType.setName(asRouteName(route.getName()));
            trkType.setDesc(asDescription(route.getDescription()));
        }
        trkTypes.add(trkType);
        TrksegType trksegType = objectFactory.createTrksegType();
        List<GpxPosition> positions = route.getPositions();
        for (int i = startIndex; i < endIndex; i++) {
            GpxPosition position = positions.get(i);
            WptType wptType = createWptType(position);
            if (wptType != null)
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

    private MetadataType createMetaData(GpxRoute route, GpxType gpxType) {
        ObjectFactory objectFactory = new ObjectFactory();

        MetadataType metadataType = gpxType.getMetadata();
        if (metadataType == null)
            metadataType = objectFactory.createMetadataType();

        if (isWriteMetaData()) {
            metadataType.setName(asRouteName(route.getName()));
            metadataType.setDesc(asDescription(route.getDescription()));
        }
        return metadataType;
    }

    private GpxType createGpxType(GpxRoute route, int startIndex, int endIndex) {
        ObjectFactory objectFactory = new ObjectFactory();

        GpxType gpxType = recycleGpxType(route);
        if (gpxType == null)
            gpxType = objectFactory.createGpxType();
        gpxType.setCreator(GENERATED_BY);
        gpxType.setVersion(VERSION);

        if (route.getCharacteristics().equals(Waypoints))
            gpxType.setMetadata(createMetaData(route, gpxType));

        gpxType.getWpt().addAll(createWayPoints(route, startIndex, endIndex));
        gpxType.getRte().addAll(createRoute(route, startIndex, endIndex));
        gpxType.getTrk().addAll(createTrack(route, startIndex, endIndex));
        return gpxType;
    }

    private GpxType createGpxType(List<GpxRoute> routes) {
        ObjectFactory objectFactory = new ObjectFactory();

        GpxType gpxType = null;
        for (GpxRoute route : routes) {
            gpxType = recycleGpxType(route);
            if (gpxType != null)
                break;
        }
        if (gpxType == null)
            gpxType = objectFactory.createGpxType();
        gpxType.setCreator(GENERATED_BY);
        gpxType.setVersion(VERSION);

        for (GpxRoute route : routes) {
            switch (route.getCharacteristics()) {
                case Waypoints:
                    gpxType.setMetadata(createMetaData(route, gpxType));
                    gpxType.getWpt().addAll(createWayPoints(route, 0, route.getPositionCount()));
                    break;
                case Route:
                    gpxType.getRte().addAll(createRoute(route, 0, route.getPositionCount()));
                    break;
                case Track:
                    gpxType.getTrk().addAll(createTrack(route, 0, route.getPositionCount()));
                    break;
                default:
                    throw new IllegalArgumentException("Unknown RouteCharacteristics " + route.getCharacteristics());
            }
        }
        return gpxType;
    }

    public void write(GpxRoute route, OutputStream target, int startIndex, int endIndex) {
        try {
            marshal11(createGpxType(route, startIndex, endIndex), target);
        } catch (JAXBException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public void write(List<GpxRoute> routes, OutputStream target) {
        try {
            marshal11(createGpxType(routes), target);
        } catch (JAXBException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
