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

import slash.common.io.CompactCalendar;
import slash.common.io.Transfer;
import slash.navigation.base.BaseNavigationFormat;
import slash.navigation.base.RouteCharacteristics;
import slash.navigation.gpx.binding10.Gpx;
import slash.navigation.gpx.binding10.ObjectFactory;
import slash.navigation.util.Conversion;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

/**
 * Reads and writes GPS Exchange Format 1.0 (.gpx) files.
 *
 * @author Christian Pesch
 */

public class Gpx10Format extends GpxFormat {
    private static final Logger log = Logger.getLogger(Gpx10Format.class.getName());
    static final String VERSION = "1.0";
    private final boolean reuseReadObjectsForWriting;

    public Gpx10Format(boolean reuseReadObjectsForWriting) {
        this.reuseReadObjectsForWriting = reuseReadObjectsForWriting;
    }

    public Gpx10Format() {
        this(true);
    }

    public String getName() {
        return "GPS Exchange Format " + VERSION + " (*" + getExtension() + ")";
    }

    List<GpxRoute> process(Gpx gpx) {
        if (gpx == null || !VERSION.equals(gpx.getVersion()))
            return null;

        boolean hasSpeedInKilometerPerHourInsteadOfMeterPerSecond = gpx.getCreator() != null &&
                ("Mobile Action http://www.mobileaction.com/".equals(gpx.getCreator()) ||
                 "Holux Utility".equals(gpx.getCreator()));
        List<GpxRoute> result = new ArrayList<GpxRoute>();
        GpxRoute wayPointsAsRoute = extractWayPoints(gpx, hasSpeedInKilometerPerHourInsteadOfMeterPerSecond);
        if (wayPointsAsRoute != null)
            result.add(wayPointsAsRoute);
        result.addAll(extractRoutes(gpx, hasSpeedInKilometerPerHourInsteadOfMeterPerSecond));
        result.addAll(extractTracks(gpx, hasSpeedInKilometerPerHourInsteadOfMeterPerSecond));
        return result;
    }

    public List<GpxRoute> read(InputStream source, CompactCalendar startDate) throws IOException {
        try {
            Gpx gpx = GpxUtil.unmarshal10(source);
            return process(gpx);
        } catch (Throwable t) {
            log.fine("Error reading " + source + ": " + t.getMessage());
            return null;
        }
    }

    private List<GpxRoute> extractRoutes(Gpx gpx, boolean hasSpeedInKilometerPerHourInsteadOfMeterPerSecond) {
        List<GpxRoute> result = new ArrayList<GpxRoute>();

        for (Gpx.Rte rte : gpx.getRte()) {
            String name = rte.getName();
            String desc = rte.getDesc();
            List<String> descriptions = asDescription(desc);
            List<GpxPosition> positions = extractRoute(rte, hasSpeedInKilometerPerHourInsteadOfMeterPerSecond);
            result.add(new GpxRoute(this, RouteCharacteristics.Route, name, descriptions, positions, gpx, rte));
        }

        return result;
    }

    private GpxRoute extractWayPoints(Gpx gpx, boolean hasSpeedInKilometerPerHourInsteadOfMeterPerSecond) {
        String name = gpx.getName();
        List<String> descriptions = asDescription(gpx.getDesc());
        List<GpxPosition> positions = extractWayPoints(gpx.getWpt(), hasSpeedInKilometerPerHourInsteadOfMeterPerSecond);
        return positions.size() == 0 ? null : new GpxRoute(this, isTripmasterTrack(positions) ? RouteCharacteristics.Track : RouteCharacteristics.Waypoints, name, descriptions, positions, gpx);
    }

    boolean isTripmasterTrack(List<GpxPosition> positions) {
        for (GpxPosition position : positions) {
            if (position.getReason() == null)
                return false;
        }
        return true;
    }

    private List<GpxRoute> extractTracks(Gpx gpx, boolean hasSpeedInKilometerPerHourInsteadOfMeterPerSecond) {
        List<GpxRoute> result = new ArrayList<GpxRoute>();

        for (Gpx.Trk trk : gpx.getTrk()) {
            String name = trk.getName();
            String desc = trk.getDesc();
            List<String> descriptions = asDescription(desc);
            List<GpxPosition> positions = extractTrack(trk, hasSpeedInKilometerPerHourInsteadOfMeterPerSecond);
            if (positions.size() > 0)
                result.add(new GpxRoute(this, RouteCharacteristics.Track, name, descriptions, positions, gpx, trk));
        }

        return result;
    }

    private Double asKmh(Double metersPerSecond) {
        if (metersPerSecond == null)
            return null;
        return Conversion.msToKmh(metersPerSecond);
    }

    private List<GpxPosition> extractRoute(Gpx.Rte rte, boolean hasSpeedInKilometerPerHourInsteadOfMeterPerSecond) {
        List<GpxPosition> positions = new ArrayList<GpxPosition>();
        if (rte != null) {
            for (Gpx.Rte.Rtept rtept : rte.getRtept()) {
                positions.add(new GpxPosition(rtept.getLon(), rtept.getLat(), rtept.getEle(), getSpeed(rtept.getSpeed(), rtept.getCmt(), hasSpeedInKilometerPerHourInsteadOfMeterPerSecond), Transfer.formatDouble(rtept.getCourse()), parseTime(rtept.getTime()), asComment(rtept.getName(), rtept.getDesc()), rtept.getHdop(), rtept.getPdop(), rtept.getVdop(), rtept.getSat(), rtept));
            }
        }
        return positions;
    }

    private List<GpxPosition> extractWayPoints(List<Gpx.Wpt> wpts, boolean hasSpeedInKilometerPerHourInsteadOfMeterPerSecond) {
        List<GpxPosition> positions = new ArrayList<GpxPosition>();
        for (Gpx.Wpt wpt : wpts) {
            positions.add(new GpxPosition(wpt.getLon(), wpt.getLat(), wpt.getEle(), getSpeed(wpt.getSpeed(), wpt.getCmt(), hasSpeedInKilometerPerHourInsteadOfMeterPerSecond), Transfer.formatDouble(wpt.getCourse()), parseTime(wpt.getTime()), asWayPointComment(wpt.getName(), wpt.getDesc()), wpt.getHdop(), wpt.getPdop(), wpt.getVdop(), wpt.getSat(), wpt));
        }
        return positions;
    }

    private List<GpxPosition> extractTrack(Gpx.Trk trk, boolean hasSpeedInKilometerPerHourInsteadOfMeterPerSecond) {
        List<GpxPosition> positions = new ArrayList<GpxPosition>();
        if (trk != null) {
            for (Gpx.Trk.Trkseg trkSeg : trk.getTrkseg()) {
                for (Gpx.Trk.Trkseg.Trkpt trkPt : trkSeg.getTrkpt()) {
                    positions.add(new GpxPosition(trkPt.getLon(), trkPt.getLat(), trkPt.getEle(), getSpeed(trkPt.getSpeed(), trkPt.getCmt(), hasSpeedInKilometerPerHourInsteadOfMeterPerSecond), Transfer.formatDouble(trkPt.getCourse()), parseTime(trkPt.getTime()), asComment(trkPt.getName(), trkPt.getDesc()), trkPt.getHdop(), trkPt.getPdop(), trkPt.getVdop(), trkPt.getSat(), trkPt));
                }
            }
        }
        return positions;
    }

    private Double getSpeed(BigDecimal speed, String comment, boolean hasSpeedInKilometerPerHourInsteadOfMeterPerSecond) {
        Double result = Transfer.formatDouble(speed);
        if(!hasSpeedInKilometerPerHourInsteadOfMeterPerSecond)
            result = asKmh(result);
        if (result == null)
            result = parseSpeed(comment);
        return result;
    }

    private String formatSpeed(String comment, Double speed) {
        if (speed == null || speed == 0.0)
            return comment;
        return (comment != null ? comment + " " : "") +
                "Speed: " + Transfer.formatSpeedAsString(speed) + " Km/h";
    }

    private String formatHeading(String comment, Double heading) {
        if (heading == null || heading == 0.0)
            return comment;
        return (comment != null ? comment + " " : "") +
                "Heading: " + Transfer.formatHeadingAsString(heading);
    }

    private List<Gpx.Wpt> createWayPoints(GpxRoute route, int startIndex, int endIndex) {
        ObjectFactory objectFactory = new ObjectFactory();
        List<Gpx.Wpt> wpts = new ArrayList<Gpx.Wpt>();
        List<GpxPosition> positions = route.getPositions();
        for (int i = startIndex; i < endIndex; i++) {
            GpxPosition position = positions.get(i);
            Gpx.Wpt wpt = position.getOrigin(Gpx.Wpt.class);
            if (wpt == null || !reuseReadObjectsForWriting)
                wpt = objectFactory.createGpxWpt();
            wpt.setLat(Transfer.formatPosition(position.getLatitude()));
            wpt.setLon(Transfer.formatPosition(position.getLongitude()));
            wpt.setTime(isWriteTime() ? formatTime(position.getTime()) : null);
            wpt.setEle(isWriteElevation() ? Transfer.formatElevation(position.getElevation()) : null);
            wpt.setCourse(isWriteHeading() ? Transfer.formatHeading(position.getHeading()) : null);
            wpt.setSpeed(isWriteSpeed() && position.getSpeed() != null ? Transfer.formatDouble(Conversion.kmhToMs(position.getSpeed()), 3) : null);
            if (isWriteSpeed() && reuseReadObjectsForWriting)
                wpt.setCmt(formatSpeed(wpt.getCmt(), position.getSpeed()));
            if (isWriteHeading() && reuseReadObjectsForWriting)
                wpt.setCmt(formatHeading(wpt.getCmt(), position.getHeading()));
            wpt.setName(isWriteName() ? asName(position.getComment()) : null);
            wpt.setDesc(isWriteName() ? asDesc(position.getComment(), wpt.getDesc()) : null);
            wpt.setHdop(isWriteAccuracy() && position.getHdop() != null ? Transfer.formatDouble(position.getHdop(), 6) : null);
            wpt.setPdop(isWriteAccuracy() && position.getPdop() != null ? Transfer.formatDouble(position.getPdop(), 6) : null);
            wpt.setVdop(isWriteAccuracy() && position.getVdop() != null ? Transfer.formatDouble(position.getVdop(), 6) : null);
            wpt.setSat(isWriteAccuracy() && position.getSatellites() != null ? Transfer.formatInt(position.getSatellites()) : null);
            wpts.add(wpt);
        }
        return wpts;
    }

    private List<Gpx.Rte> createRoute(GpxRoute route, int startIndex, int endIndex) {
        ObjectFactory objectFactory = new ObjectFactory();
        List<Gpx.Rte> rtes = new ArrayList<Gpx.Rte>();

        Gpx.Rte rte = route.getOrigin(Gpx.Rte.class);
        if (rte != null && reuseReadObjectsForWriting)
            rte.getRtept().clear();
        else
            rte = objectFactory.createGpxRte();

        if (isWriteName()) {
            rte.setName(route.getName());
            rte.setDesc(asDescription(route.getDescription()));
        }
        rtes.add(rte);
        List<GpxPosition> positions = route.getPositions();
        for (int i = startIndex; i < endIndex; i++) {
            GpxPosition position = positions.get(i);
            Gpx.Rte.Rtept rtept = position.getOrigin(Gpx.Rte.Rtept.class);
            if (rtept == null || !reuseReadObjectsForWriting)
                rtept = objectFactory.createGpxRteRtept();
            rtept.setLat(Transfer.formatPosition(position.getLatitude()));
            rtept.setLon(Transfer.formatPosition(position.getLongitude()));
            rtept.setTime(isWriteTime() ? formatTime(position.getTime()) : null);
            rtept.setEle(isWriteElevation() ? Transfer.formatElevation(position.getElevation()) : null);
            rtept.setCourse(isWriteHeading() ? Transfer.formatHeading(position.getHeading()) : null);
            rtept.setSpeed(isWriteSpeed() && position.getSpeed() != null ? Transfer.formatDouble(Conversion.kmhToMs(position.getSpeed()), 3) : null);
            if (isWriteSpeed() && reuseReadObjectsForWriting)
                rtept.setCmt(formatSpeed(rtept.getCmt(), position.getSpeed()));
            if (isWriteHeading() && reuseReadObjectsForWriting)
                rtept.setCmt(formatHeading(rtept.getCmt(), position.getHeading()));
            rtept.setName(isWriteName() ? asName(position.getComment()) : null);
            rtept.setDesc(isWriteName() ? asDesc(position.getComment(), rtept.getDesc()) : null);
            rtept.setHdop(isWriteAccuracy() && position.getHdop() != null ? Transfer.formatDouble(position.getHdop(), 6) : null);
            rtept.setPdop(isWriteAccuracy() && position.getPdop() != null ? Transfer.formatDouble(position.getPdop(), 6) : null);
            rtept.setVdop(isWriteAccuracy() && position.getVdop() != null ? Transfer.formatDouble(position.getVdop(), 6) : null);
            rtept.setSat(isWriteAccuracy() && position.getSatellites() != null ? Transfer.formatInt(position.getSatellites()) : null);
            rte.getRtept().add(rtept);
        }
        return rtes;
    }

    private List<Gpx.Trk> createTrack(GpxRoute route, int startIndex, int endIndex) {
        ObjectFactory objectFactory = new ObjectFactory();
        List<Gpx.Trk> trks = new ArrayList<Gpx.Trk>();

        Gpx.Trk trk = route.getOrigin(Gpx.Trk.class);
        if (trk != null && reuseReadObjectsForWriting)
            trk.getTrkseg().clear();
        else
            trk = objectFactory.createGpxTrk();

        if (isWriteName()) {
            trk.setName(route.getName());
            trk.setDesc(asDescription(route.getDescription()));
        }
        trks.add(trk);
        Gpx.Trk.Trkseg trkseg = objectFactory.createGpxTrkTrkseg();
        List<GpxPosition> positions = route.getPositions();
        for (int i = startIndex; i < endIndex; i++) {
            GpxPosition position = positions.get(i);
            Gpx.Trk.Trkseg.Trkpt trkpt = position.getOrigin(Gpx.Trk.Trkseg.Trkpt.class);
            if (trkpt == null || !reuseReadObjectsForWriting)
                trkpt = objectFactory.createGpxTrkTrksegTrkpt();
            trkpt.setLat(Transfer.formatPosition(position.getLatitude()));
            trkpt.setLon(Transfer.formatPosition(position.getLongitude()));
            trkpt.setTime(isWriteTime() ? formatTime(position.getTime()) : null);
            trkpt.setEle(isWriteElevation() ? Transfer.formatElevation(position.getElevation()) : null);
            trkpt.setCourse(isWriteHeading() ? Transfer.formatHeading(position.getHeading()) : null);
            trkpt.setSpeed(isWriteSpeed() && position.getSpeed() != null ?
                    Transfer.formatDouble(Conversion.kmhToMs(position.getSpeed()), 3) : null);
            trkpt.setName(isWriteName() ? asName(position.getComment()) : null);
            trkpt.setDesc(isWriteName() ? asDesc(position.getComment(), trkpt.getDesc()) : null);
            trkpt.setHdop(isWriteAccuracy() && position.getHdop() != null ? Transfer.formatDouble(position.getHdop(), 6) : null);
            trkpt.setPdop(isWriteAccuracy() && position.getPdop() != null ? Transfer.formatDouble(position.getPdop(), 6) : null);
            trkpt.setVdop(isWriteAccuracy() && position.getVdop() != null ? Transfer.formatDouble(position.getVdop(), 6) : null);
            trkpt.setSat(isWriteAccuracy() && position.getSatellites() != null ? Transfer.formatInt(position.getSatellites()) : null);
            trkseg.getTrkpt().add(trkpt);
        }
        trk.getTrkseg().add(trkseg);
        return trks;
    }

    private Gpx recycleGpx(GpxRoute route) {
        Gpx gpx = route.getOrigin(Gpx.class);
        if (gpx != null) {
            gpx.getRte().clear();
            gpx.getTrk().clear();
            gpx.getWpt().clear();
        }
        return gpx;
    }

    private Gpx createGpx(GpxRoute route, int startIndex, int endIndex, List<RouteCharacteristics> characteristics) {
        Gpx gpx = recycleGpx(route);
        if (gpx == null || !reuseReadObjectsForWriting)
            gpx = new ObjectFactory().createGpx();
        gpx.setCreator(BaseNavigationFormat.GENERATED_BY);
        gpx.setVersion(VERSION);
        gpx.setName(route.getName());
        gpx.setDesc(asDescription(route.getDescription()));

        for (RouteCharacteristics characteristic : characteristics) {
            switch (characteristic) {
                case Waypoints:
                    gpx.getWpt().addAll(createWayPoints(route, startIndex, endIndex));
                    break;
                case Route:
                    gpx.getRte().addAll(createRoute(route, startIndex, endIndex));
                    break;
                case Track:
                    gpx.getTrk().addAll(createTrack(route, startIndex, endIndex));
                    break;
                default:
                    throw new IllegalArgumentException("Unknown RouteCharacteristics " + characteristic);
            }
        }
        return gpx;
    }

    private Gpx createGpx(List<GpxRoute> routes) {
        ObjectFactory objectFactory = new ObjectFactory();
        Gpx gpx = null;
        for(GpxRoute route : routes) {
            gpx = recycleGpx(route);
            if(gpx != null)
                break;
        }
        if (gpx == null || !reuseReadObjectsForWriting)
            gpx = objectFactory.createGpx();
        gpx.setCreator(BaseNavigationFormat.GENERATED_BY);
        gpx.setVersion(VERSION);

        for (GpxRoute route : routes) {
            switch (route.getCharacteristics()) {
                case Waypoints:
                    gpx.getWpt().addAll(createWayPoints(route, 0, route.getPositionCount()));
                    break;
                case Route:
                    gpx.getRte().addAll(createRoute(route, 0, route.getPositionCount()));
                    break;
                case Track:
                    gpx.getTrk().addAll(createTrack(route, 0, route.getPositionCount()));
                    break;
                default:
                    throw new IllegalArgumentException("Unknown RouteCharacteristics " + route.getCharacteristics());
            }
        }
        return gpx;
    }

    public void write(GpxRoute route, OutputStream target, int startIndex, int endIndex) {
        write(route, target, startIndex, endIndex, Arrays.asList(RouteCharacteristics.Route, RouteCharacteristics.Track, RouteCharacteristics.Waypoints));
    }

    public void write(GpxRoute route, OutputStream target, int startIndex, int endIndex, List<RouteCharacteristics> characteristics) {
        try {
            GpxUtil.marshal10(createGpx(route, startIndex, endIndex, characteristics), target);
        } catch (JAXBException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public void write(List<GpxRoute> routes, OutputStream target) {
        try {
            GpxUtil.marshal10(createGpx(routes), target);
        } catch (JAXBException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
