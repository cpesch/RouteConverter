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
import slash.navigation.base.ParserContext;
import slash.navigation.gpx.binding11.*;
import slash.navigation.gpx.garmin3.AutoroutePointT;
import slash.navigation.gpx.garmin3.RoutePointExtensionT;
import slash.navigation.gpx.trip1.ShapingPointExtensionT;
import slash.navigation.gpx.trip1.ViaPointExtensionT;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static slash.common.io.Transfer.*;
import static slash.common.type.CompactCalendar.now;
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

    void process(GpxType gpxType, ParserContext<GpxRoute> context) {
        if (gpxType == null || !VERSION.equals(gpxType.getVersion()))
            return;

        GpxRoute wayPointsAsRoute = extractWayPoints(gpxType);
        if (wayPointsAsRoute != null)
            context.appendRoute(wayPointsAsRoute);
        context.appendRoutes(extractRoutes(gpxType));
        context.appendRoutes(extractTracks(gpxType));
    }

    public void read(InputStream source, ParserContext<GpxRoute> context) throws IOException {
        GpxType gpxType = unmarshal11(source);
        process(gpxType, context);
    }

    private boolean containsRoutePointExtension(RteType rteType) {
        for (WptType wptType : rteType.getRtept()) {
            ExtensionsType extensions = wptType.getExtensions();
            if (extensions != null) {
                for (Object any : extensions.getAny()) {
                    if (any instanceof JAXBElement) {
                        Object anyValue = ((JAXBElement) any).getValue();
                        if(anyValue instanceof RoutePointExtensionT)
                            return true;
                    }
                }
            }
        }
        return false;
    }

    private List<GpxRoute> extractRoutes(GpxType gpxType) {
        List<GpxRoute> result = new ArrayList<>();

        for (RteType rteType : gpxType.getRte()) {
            String name = rteType.getName();
            String desc = rteType.getDesc();
            List<String> descriptions = asDescription(desc);
            List<GpxPosition> positions = extractRoute(rteType);
            result.add(new GpxRoute(this, Route, name, descriptions, positions, gpxType, rteType/*, rteType.getRtept().get(0)*/));

            if (containsRoutePointExtension(rteType)) {
                List<GpxPosition> extendedPositions = extractRouteWithRoutePointExtension(rteType);
                result.add(new GpxRoute(this, Track, name, descriptions, extendedPositions, gpxType, rteType/*, rteType.getRtept().get(0)*/));
            }
        }

        return result;
    }

    private GpxRoute extractWayPoints(GpxType gpxType) {
        String name = gpxType.getMetadata() != null ? gpxType.getMetadata().getName() : null;
        String desc = gpxType.getMetadata() != null ? gpxType.getMetadata().getDesc() : null;
        List<String> descriptions = asDescription(desc);
        List<GpxPosition> positions = extractWayPoints(gpxType.getWpt());
        return positions.size() == 0 ? null : new GpxRoute(this, Waypoints, name, descriptions, positions, gpxType);
    }

    private List<GpxRoute> extractTracks(GpxType gpxType) {
        List<GpxRoute> result = new ArrayList<>();

        for (TrkType trkType : gpxType.getTrk()) {
            String name = trkType.getName();
            String desc = trkType.getDesc();
            List<String> descriptions = asDescription(desc);
            List<GpxPosition> positions = extractTrack(trkType);
            result.add(new GpxRoute(this, Track, name, descriptions, positions, gpxType, trkType, trkType.getTrkseg().get(0)));
        }

        return result;
    }

    private List<GpxPosition> extractRoute(RteType rteType) {
        List<GpxPosition> positions = new ArrayList<>();
        if (rteType != null) {
            for (WptType wptType : rteType.getRtept()) {
                positions.add(new GpxPosition(wptType.getLon(), wptType.getLat(), wptType.getEle(), new GpxPositionExtension(wptType), parseXMLTime(wptType.getTime()), asDescription(wptType.getName(), wptType.getDesc()), wptType.getHdop(), wptType.getPdop(), wptType.getVdop(), wptType.getSat(), wptType));
            }
        }
        return positions;
    }

    private List<GpxPosition> extractRouteWithRoutePointExtension(RteType rteType) {
        List<GpxPosition> positions = new ArrayList<>();
        if (rteType != null) {
            for (WptType wptType : rteType.getRtept()) {
                positions.add(new GpxPosition(wptType.getLon(), wptType.getLat(), wptType.getEle(), new GpxPositionExtension(wptType), parseXMLTime(wptType.getTime()), asDescription(wptType.getName(), wptType.getDesc()), wptType.getHdop(), wptType.getPdop(), wptType.getVdop(), wptType.getSat(), wptType));

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

    private List<GpxPosition> extractWayPoints(List<WptType> wptTypes) {
        List<GpxPosition> positions = new ArrayList<>();
        for (WptType wptType : wptTypes) {
            positions.add(new GpxPosition(wptType.getLon(), wptType.getLat(), wptType.getEle(), new GpxPositionExtension(wptType), parseXMLTime(wptType.getTime()), asDescription(wptType.getName(), wptType.getDesc()), wptType.getHdop(), wptType.getPdop(), wptType.getVdop(), wptType.getSat(), wptType));
        }
        return positions;
    }

    private List<GpxPosition> extractTrack(TrkType trkType) {
        List<GpxPosition> positions = new ArrayList<>();
        if (trkType != null) {
            for (TrksegType trkSegType : trkType.getTrkseg()) {
                for (WptType wptType : trkSegType.getTrkpt()) {
                    positions.add(new GpxPosition(wptType.getLon(), wptType.getLat(), wptType.getEle(), new GpxPositionExtension(wptType), parseXMLTime(wptType.getTime()), asDescription(wptType.getName(), wptType.getDesc()), wptType.getHdop(), wptType.getPdop(), wptType.getVdop(), wptType.getSat(), wptType));
                }
            }
        }
        return positions;
    }

    private void setExtension(WptType wptType, String extensionNameToRemove, String extensionNameToAdd, Object extensionToAdd) {
        if (wptType.getExtensions() == null)
            wptType.setExtensions(new ObjectFactory().createExtensionsType());
        List<Object> anys = wptType.getExtensions().getAny();

        boolean foundElement = false;
        Iterator<Object> iterator = anys.iterator();
        while (iterator.hasNext()) {
            Object any = iterator.next();

            if (any instanceof JAXBElement) {
                @SuppressWarnings("unchecked")
                JAXBElement<String> element = (JAXBElement<String>) any;
                if (extensionNameToRemove.equals(element.getName().getLocalPart())) {
                    iterator.remove();
                } else if (extensionNameToAdd.equals(element.getName().getLocalPart())) {
                    foundElement = true;
                }
            }
        }

        if (!foundElement)
            anys.add(extensionToAdd);
    }

    private void setViaPoint(WptType wptType) {
        slash.navigation.gpx.trip1.ObjectFactory tripFactory = new slash.navigation.gpx.trip1.ObjectFactory();
        ViaPointExtensionT viaPointExtensionT = tripFactory.createViaPointExtensionT();
        viaPointExtensionT.setCalculationMode("ShorterDistance");
        viaPointExtensionT.setElevationMode("Standard");
        setExtension(wptType, "ShapingPoint", "ViaPoint", tripFactory.createViaPoint(viaPointExtensionT));
    }

    private void setShapingPoint(WptType wptType) {
        slash.navigation.gpx.trip1.ObjectFactory tripFactory = new slash.navigation.gpx.trip1.ObjectFactory();
        ShapingPointExtensionT shapingPointExtensionT = tripFactory.createShapingPointExtensionT();
        setExtension(wptType, "ViaPoint", "ShapingPoint", tripFactory.createShapingPoint(shapingPointExtensionT));
    }

    private void clearDistance(TrkType trkType) {
        if (trkType.getExtensions() == null)
            return;

        @SuppressWarnings("ConstantConditions")
        List<Object> anys = trkType.getExtensions().getAny();
        for (Iterator<Object> iterator = anys.iterator(); iterator.hasNext(); ) {
            Object any = iterator.next();

            if (any instanceof Element) {
                Element element = (Element) any;

                // TrackStatsExtension contains Distance element which BaseCamp uses if it's present
                // but which might be inaccurate due to changes to the positions that affect the distance
                if ("TrackStatsExtension".equals(element.getLocalName())) {
                    iterator.remove();
                }
            }
        }
    }

    private WptType createWptType(GpxPosition position) {
        BigDecimal latitude = formatPosition(position.getLatitude());
        BigDecimal longitude = formatPosition(position.getLongitude());
        if (latitude == null || longitude == null)
            return null;
        WptType wptType = position.getOrigin(WptType.class);
        if (wptType == null) {
            wptType = new ObjectFactory().createWptType();

            // when converted, there is no GpxPositionExtension thus it needs to be created and the values
            // transferred to the position extension
            Double heading = position.getHeading();
            Double speed = position.getSpeed();
            Double temperature = position.getTemperature();
            Short heartBeat = position.getHeartBeat();

            position.setPositionExtension(new GpxPositionExtension(wptType));

            position.setHeading(heading);
            position.setSpeed(speed);
            position.setTemperature(temperature);
            position.setHeartBeat(heartBeat);
        }
        wptType.setLat(latitude);
        wptType.setLon(longitude);
        wptType.setEle(isWriteElevation() ? formatElevation(position.getElevation()) : null);
        wptType.setTime(isWriteTime() ? formatXMLTime(position.getTime()) : null);
        wptType.setName(isWriteName() ? asName(position.getDescription()) : null);
        wptType.setDesc(isWriteName() ? asDesc(position.getDescription(), wptType.getDesc()) : null);
        wptType.setHdop(isWriteAccuracy() && position.getHdop() != null ? formatBigDecimal(position.getHdop(), 6) : null);
        wptType.setPdop(isWriteAccuracy() && position.getPdop() != null ? formatBigDecimal(position.getPdop(), 6) : null);
        wptType.setVdop(isWriteAccuracy() && position.getVdop() != null ? formatBigDecimal(position.getVdop(), 6) : null);
        wptType.setSat(isWriteAccuracy() && position.getSatellites() != null ? formatInt(position.getSatellites()) : null);

        // setting the values to null which lead to removeEmptyExtensions() remove them
        if(!isWriteHeading())
            position.setHeading(null);
        if(!isWriteSpeed())
            position.setSpeed(null);
        if (!isWriteTemperature())
            position.setTemperature(null);
        if (!isWriteHeartBeat())
            position.setHeartBeat(null);

        if (!isWriteExtensions())
            wptType.setExtensions(null);

        else if (position.getPositionExtension() != null) {
            position.getPositionExtension().mergeExtensions();
            position.getPositionExtension().removeEmptyExtensions();
        }
        return wptType;
    }

    private List<WptType> createWayPoints(GpxRoute route, int startIndex, int endIndex) {
        List<WptType> wptTypes = new ArrayList<>();
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
        List<RteType> rteTypes = new ArrayList<>();
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
            if (wptType != null) {
                rteType.getRtept().add(wptType);

                if (isWriteTrip()) {
                    boolean first = i == startIndex;
                    boolean last = i == endIndex - 1;
                    if (first || last)
                        setViaPoint(wptType);
                    else
                        setShapingPoint(wptType);
                }
            }
        }
        return rteTypes;
    }

    private List<TrkType> createTrack(GpxRoute route, int startIndex, int endIndex) {
        ObjectFactory objectFactory = new ObjectFactory();
        List<TrkType> trkTypes = new ArrayList<>();
        TrkType trkType = route.getOrigin(TrkType.class);
        if (trkType != null)
            trkType.getTrkseg().clear();
        else
            trkType = objectFactory.createTrkType();
        if (isWriteMetaData()) {
            trkType.setName(asRouteName(route.getName()));
            trkType.setDesc(asDescription(route.getDescription()));
            clearDistance(trkType);
        }
        trkTypes.add(trkType);

        TrksegType trksegType = route.getOrigin(TrksegType.class);
        if (trksegType != null)
            trksegType.getTrkpt().clear();
        else
            trksegType = objectFactory.createTrksegType();
        trkType.getTrkseg().add(trksegType);

        List<GpxPosition> positions = route.getPositions();
        for (int i = startIndex; i < endIndex; i++) {
            GpxPosition position = positions.get(i);
            WptType wptType = createWptType(position);
            if (wptType != null)
                trksegType.getTrkpt().add(wptType);
        }
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

        if (route != null) {
            metadataType.setName(asRouteName(route.getName()));
            metadataType.setDesc(asDescription(route.getDescription()));
        }
        metadataType.setTime(formatXMLTime(now()));
        return metadataType;
    }

    private GpxType createGpxType(GpxRoute route, int startIndex, int endIndex) {
        ObjectFactory objectFactory = new ObjectFactory();

        GpxType gpxType = recycleGpxType(route);
        if (gpxType == null)
            gpxType = objectFactory.createGpxType();
        gpxType.setCreator(getCreator());
        gpxType.setVersion(VERSION);

        if (isWriteMetaData()) {
            gpxType.setMetadata(createMetaData(route, gpxType));
        } else
            gpxType.setMetadata(null);

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
        gpxType.setCreator(getCreator());
        gpxType.setVersion(VERSION);

        GpxRoute routeForMetadata = null;
        for (GpxRoute route : routes) {
            switch (route.getCharacteristics()) {
                case Waypoints:
                    routeForMetadata = route;
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

        if (isWriteMetaData()) {
            gpxType.setMetadata(createMetaData(routeForMetadata, gpxType));
        } else
            gpxType.setMetadata(null);

        return gpxType;
    }

    public void write(GpxRoute route, OutputStream target, int startIndex, int endIndex) throws IOException {
        try {
            marshal11(createGpxType(route, startIndex, endIndex), target);
        } catch (JAXBException e) {
            throw new IOException("Cannot marshall " + route + ": " + e, e);
        }
    }

    public void write(List<GpxRoute> routes, OutputStream target) throws IOException {
        try {
            marshal11(createGpxType(routes), target);
        } catch (JAXBException e) {
            throw new IOException("Cannot marshall " + routes + ": " + e, e);
        }
    }
}
