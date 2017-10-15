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
import slash.navigation.base.ParserContext;
import slash.navigation.gpx.binding11.ExtensionsType;
import slash.navigation.gpx.binding11.GpxType;
import slash.navigation.gpx.binding11.MetadataType;
import slash.navigation.gpx.binding11.ObjectFactory;
import slash.navigation.gpx.binding11.RteType;
import slash.navigation.gpx.binding11.TrkType;
import slash.navigation.gpx.binding11.TrksegType;
import slash.navigation.gpx.binding11.WptType;
import slash.navigation.gpx.garmin3.AutoroutePointT;
import slash.navigation.gpx.garmin3.RoutePointExtensionT;
import slash.navigation.gpx.trackpoint2.TrackPointExtensionT;
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

import static java.lang.Double.isNaN;
import static slash.common.io.Transfer.formatDouble;
import static slash.common.io.Transfer.formatInt;
import static slash.common.io.Transfer.formatXMLTime;
import static slash.common.io.Transfer.isEmpty;
import static slash.common.io.Transfer.parseDouble;
import static slash.common.io.Transfer.parseXMLTime;
import static slash.navigation.base.RouteCharacteristics.Route;
import static slash.navigation.base.RouteCharacteristics.Track;
import static slash.navigation.base.RouteCharacteristics.Waypoints;
import static slash.navigation.common.NavigationConversion.formatBigDecimal;
import static slash.navigation.common.NavigationConversion.formatElevation;
import static slash.navigation.common.NavigationConversion.formatHeading;
import static slash.navigation.common.NavigationConversion.formatHeadingAsString;
import static slash.navigation.common.NavigationConversion.formatPosition;
import static slash.navigation.common.NavigationConversion.formatSpeedAsDouble;
import static slash.navigation.common.NavigationConversion.formatSpeedAsString;
import static slash.navigation.common.NavigationConversion.formatTemperatureAsDouble;
import static slash.navigation.common.NavigationConversion.formatTemperatureAsString;
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

    public void read(InputStream source, ParserContext<GpxRoute> context) throws Exception {
        GpxType gpxType = unmarshal11(source);
        process(gpxType, context);
    }

    private List<GpxRoute> extractRoutes(GpxType gpxType) {
        List<GpxRoute> result = new ArrayList<>();

        for (RteType rteType : gpxType.getRte()) {
            String name = rteType.getName();
            String desc = rteType.getDesc();
            List<String> descriptions = asDescription(desc);
            List<GpxPosition> positions = extractRoute(rteType);
            result.add(new GpxRoute(this, Route, name, descriptions, positions, gpxType, rteType));

            // Garmin Extensions v3
            if (rteType.getExtensions() != null && rteType.getExtensions().getAny().size() > 0) {
                List<GpxPosition> extendedPositions = extractRouteWithGarminExtensions(rteType);
                result.add(new GpxRoute(this, Track, name, descriptions, extendedPositions, gpxType, rteType));
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
            result.add(new GpxRoute(this, Track, name, descriptions, positions, gpxType, trkType));
        }

        return result;
    }

    private List<GpxPosition> extractRoute(RteType rteType) {
        List<GpxPosition> positions = new ArrayList<>();
        if (rteType != null) {
            for (WptType wptType : rteType.getRtept()) {
                positions.add(new GpxPosition(wptType.getLon(), wptType.getLat(), wptType.getEle(), getSpeed(wptType), getHeading(wptType), getTemperature(wptType), parseXMLTime(wptType.getTime()), asDescription(wptType.getName(), wptType.getDesc()), wptType.getHdop(), wptType.getPdop(), wptType.getVdop(), wptType.getSat(), wptType));
            }
        }
        return positions;
    }

    private List<GpxPosition> extractRouteWithGarminExtensions(RteType rteType) {
        List<GpxPosition> positions = new ArrayList<>();
        if (rteType != null) {
            for (WptType wptType : rteType.getRtept()) {
                positions.add(new GpxPosition(wptType.getLon(), wptType.getLat(), wptType.getEle(), getSpeed(wptType), getHeading(wptType), getTemperature(wptType), parseXMLTime(wptType.getTime()), asDescription(wptType.getName(), wptType.getDesc()), wptType.getHdop(), wptType.getPdop(), wptType.getVdop(), wptType.getSat(), wptType));

                ExtensionsType extensions = wptType.getExtensions();
                if (extensions != null) {
                    for (Object any : extensions.getAny()) {
                        if (any instanceof JAXBElement) {
                            Object anyValue = ((JAXBElement) any).getValue();
                            if (anyValue instanceof RoutePointExtensionT) {
                                RoutePointExtensionT routePoint = (RoutePointExtensionT) anyValue;
                                for (AutoroutePointT autoroutePoint : routePoint.getRpt()) {
                                    positions.add(new GpxPosition(autoroutePoint.getLon(), autoroutePoint.getLat(), null, null, null, null, null, null, null, null, null, null, null));
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
            positions.add(new GpxPosition(wptType.getLon(), wptType.getLat(), wptType.getEle(), getSpeed(wptType), getHeading(wptType), getTemperature(wptType), parseXMLTime(wptType.getTime()), asDescription(wptType.getName(), wptType.getDesc()), wptType.getHdop(), wptType.getPdop(), wptType.getVdop(), wptType.getSat(), wptType));
        }
        return positions;
    }

    private List<GpxPosition> extractTrack(TrkType trkType) {
        List<GpxPosition> positions = new ArrayList<>();
        if (trkType != null) {
            for (TrksegType trkSegType : trkType.getTrkseg()) {
                for (WptType wptType : trkSegType.getTrkpt()) {
                    positions.add(new GpxPosition(wptType.getLon(), wptType.getLat(), wptType.getEle(), getSpeed(wptType), getHeading(wptType), getTemperature(wptType), parseXMLTime(wptType.getTime()), asDescription(wptType.getName(), wptType.getDesc()), wptType.getHdop(), wptType.getPdop(), wptType.getVdop(), wptType.getSat(), wptType));
                }
            }
        }
        return positions;
    }

    private Double getSpeed(WptType wptType) {
        Double result = null;
        ExtensionsType extensions = wptType.getExtensions();
        if (extensions != null) {
            for (Object any : extensions.getAny()) {
                if (any instanceof JAXBElement) {
                    Object anyValue = ((JAXBElement) any).getValue();
                    if (anyValue instanceof TrackPointExtensionT) {
                        TrackPointExtensionT trackPoint = (TrackPointExtensionT) anyValue;
                        result = asKmh(trackPoint.getSpeed());
                    }

                } else if (any instanceof Element) {
                    Element element = (Element) any;

                    // TrackPointExtension v1
                    if ("TrackPointExtension".equals(element.getLocalName())) {
                        Node firstChild = element.getFirstChild();
                        if (firstChild != null && "speed".equals(firstChild.getLocalName()))
                            result = asKmh(parseDouble(firstChild.getTextContent()));

                    // generic reading of speed elements
                    } else if ("speed".equals(element.getLocalName()))
                        result = asKmh(parseDouble(element.getTextContent()));
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
    private JAXBElement<String> asJABElement(Object any) {
        return (JAXBElement<String>) any;
    }

    private boolean isRemoveEmptyTrackPointExtension(TrackPointExtensionT trackPoint) {
        return isEmpty(trackPoint.getAtemp()) && isEmpty(trackPoint.getCourse()) && isEmpty(trackPoint.getSpeed());
    }

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
                    if (foundSpeed || speed == null || isNaN(speed))
                        iterator.remove();
                    else {
                        element.setTextContent(formatSpeedAsString(asMs(speed)));
                        foundSpeed = true;
                    }
                }
            }

            // this is if I create the extensions with JAXB
            if (any instanceof JAXBElement) {
                Object anyValue = ((JAXBElement) any).getValue();
                if (anyValue instanceof TrackPointExtensionT) {
                    TrackPointExtensionT trackPoint = (TrackPointExtensionT) anyValue;
                    if (foundSpeed || speed == null || isNaN(speed)) {
                        if (isRemoveEmptyTrackPointExtension(trackPoint))
                            iterator.remove();
                    }
                    else {
                        trackPoint.setSpeed(formatSpeedAsDouble(asMs(speed)));
                        foundSpeed = true;
                    }
                }
            }
        }

        if (!foundSpeed && speed != null && !isNaN(speed)) {
            slash.navigation.gpx.trackpoint2.ObjectFactory trackpoint2Factory = new slash.navigation.gpx.trackpoint2.ObjectFactory();
            TrackPointExtensionT trackPointExtensionT = trackpoint2Factory.createTrackPointExtensionT();
            trackPointExtensionT.setSpeed(formatSpeedAsDouble(asMs(speed)));
            anys.add(trackpoint2Factory.createTrackPointExtension(trackPointExtensionT));
        }
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
                    if (foundHeading || heading == null || isNaN(heading))
                        iterator.remove();
                    else {
                        element.setTextContent(formatHeadingAsString(heading));
                        foundHeading = true;
                    }
                }
            }

            // this is if I create the extensions with JAXB
            if (any instanceof JAXBElement) {
                Object anyValue = ((JAXBElement) any).getValue();
                if (anyValue instanceof TrackPointExtensionT) {
                    TrackPointExtensionT trackPoint = (TrackPointExtensionT) anyValue;
                    if (foundHeading || heading == null || isNaN(heading)) {
                        if (isRemoveEmptyTrackPointExtension(trackPoint))
                            iterator.remove();
                    }
                    else {
                        trackPoint.setCourse(formatHeading(heading));
                        foundHeading = true;
                    }
                }
            }
        }

        if (!foundHeading && heading != null && !isNaN(heading)) {
            slash.navigation.gpx.trackpoint2.ObjectFactory trackpoint2Factory = new slash.navigation.gpx.trackpoint2.ObjectFactory();
            TrackPointExtensionT trackPointExtensionT = trackpoint2Factory.createTrackPointExtensionT();
            trackPointExtensionT.setCourse(formatHeading(heading));
            anys.add(trackpoint2Factory.createTrackPointExtension(trackPointExtensionT));
        }
    }

    private Double getTemperature(WptType wptType) {
        Double result = null;
        ExtensionsType extensions = wptType.getExtensions();
        if (extensions != null) {
            for (Object any : extensions.getAny()) {
                if (any instanceof JAXBElement) {
                    Object anyValue = ((JAXBElement) any).getValue();
                    if (anyValue instanceof TrackPointExtensionT) {
                        TrackPointExtensionT trackPoint = (TrackPointExtensionT) anyValue;
                        result = trackPoint.getAtemp();
                    }

                } else if (any instanceof Element) {
                    Element element = (Element) any;

                    // TrackPointExtension v1
                    if ("TrackPointExtension".equals(element.getLocalName())) {
                        Node firstChild = element.getFirstChild();
                        if (firstChild != null && "atemp".equals(firstChild.getLocalName())) {
                            result = parseDouble(firstChild.getTextContent());
                        }
                    }
                }
            }
        }
        return result;
    }

    private void setTemperature(WptType wptType, Double temperature) {
        if (wptType.getExtensions() == null)
            wptType.setExtensions(new ObjectFactory().createExtensionsType());
        @SuppressWarnings("ConstantConditions")
        List<Object> anys = wptType.getExtensions().getAny();

        boolean foundTemperature = false;
        Iterator<Object> iterator = anys.iterator();
        while (iterator.hasNext()) {
            Object any = iterator.next();

            // this is parsed
            if (any instanceof Element) {
                Element element = (Element) any;
                if ("atemp".equals(element.getLocalName())) {
                    if (foundTemperature || temperature == null || isNaN(temperature))
                        iterator.remove();
                    else {
                        element.setTextContent(formatTemperatureAsString(temperature));
                        foundTemperature = true;
                    }
                }
            }

            // this is if I create the extensions with JAXB
            if (any instanceof JAXBElement) {
                Object anyValue = ((JAXBElement) any).getValue();
                if (anyValue instanceof TrackPointExtensionT) {
                    TrackPointExtensionT trackPoint = (TrackPointExtensionT) anyValue;
                    if (foundTemperature || temperature == null || isNaN(temperature)) {
                        if (isRemoveEmptyTrackPointExtension(trackPoint))
                            iterator.remove();
                    }
                    else {
                        trackPoint.setAtemp(formatTemperatureAsDouble(temperature));
                        foundTemperature = true;
                    }
                }
            }
        }

        if (!foundTemperature && temperature != null && !isNaN(temperature)) {
            slash.navigation.gpx.trackpoint2.ObjectFactory trackpoint2Factory = new slash.navigation.gpx.trackpoint2.ObjectFactory();
            TrackPointExtensionT trackPointExtensionT = trackpoint2Factory.createTrackPointExtensionT();
            trackPointExtensionT.setAtemp(formatTemperatureAsDouble(temperature));
            anys.add(trackpoint2Factory.createTrackPointExtension(trackPointExtensionT));
        }
    }

    private void setExtension(WptType wptType, String extensionNameToRemove, String extensionNameToAdd, Object extensionToAdd) {
        if (wptType.getExtensions() == null)
            wptType.setExtensions(new ObjectFactory().createExtensionsType());
        @SuppressWarnings("ConstantConditions")
        List<Object> anys = wptType.getExtensions().getAny();

        boolean foundElement = false;
        Iterator<Object> iterator = anys.iterator();
        while (iterator.hasNext()) {
            Object any = iterator.next();

            if (any instanceof JAXBElement) {
                JAXBElement<String> element = asJABElement(any);
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
        ViaPointExtensionT viaPointExtensionT = tripFactory.createViaPointExtensionT();
        viaPointExtensionT.setCalculationMode("ShorterDistance");
        viaPointExtensionT.setElevationMode("Standard");
        setExtension(wptType, "ViaPoint", "ShapingPoint", tripFactory.createShapingPoint(tripFactory.createShapingPointExtensionT()));
    }

    private void clearDistance(TrkType trkType) {
        if (trkType.getExtensions() == null)
            return;

        @SuppressWarnings("ConstantConditions")
        List<Object> anys = trkType.getExtensions().getAny();
        if (anys == null)
            return;

        Iterator<Object> iterator = anys.iterator();
        while (iterator.hasNext()) {
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
        if (wptType == null)
            wptType = new ObjectFactory().createWptType();
        wptType.setLat(latitude);
        wptType.setLon(longitude);
        wptType.setEle(isWriteElevation() ? formatElevation(position.getElevation()) : null);
        setSpeed(wptType, isWriteSpeed() ? position.getSpeed() : null);
        setHeading(wptType, isWriteHeading() ? position.getHeading() : null);
        setTemperature(wptType, isWriteTemperature() ? position.getTemperature() : null);
        wptType.setTime(isWriteTime() ? formatXMLTime(position.getTime()) : null);
        wptType.setName(isWriteName() ? asName(position.getDescription()) : null);
        wptType.setDesc(isWriteName() ? asDesc(position.getDescription(), wptType.getDesc()) : null);
        wptType.setHdop(isWriteAccuracy() && position.getHdop() != null ? formatBigDecimal(position.getHdop(), 6) : null);
        wptType.setPdop(isWriteAccuracy() && position.getPdop() != null ? formatBigDecimal(position.getPdop(), 6) : null);
        wptType.setVdop(isWriteAccuracy() && position.getVdop() != null ? formatBigDecimal(position.getVdop(), 6) : null);
        wptType.setSat(isWriteAccuracy() && position.getSatellites() != null ? formatInt(position.getSatellites()) : null);
        if (wptType.getExtensions() != null && wptType.getExtensions().getAny().size() == 0)
            wptType.setExtensions(null);
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
                    if (i == startIndex || i == endIndex - 1)
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
