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

import slash.navigation.BaseNavigationFormat;
import slash.navigation.RouteCharacteristics;
import slash.navigation.gpx.binding10.Gpx;
import slash.navigation.gpx.binding10.ObjectFactory;
import slash.navigation.util.Conversion;
import slash.navigation.util.CompactCalendar;

import javax.xml.bind.JAXBException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Reads and writes GPS Exchange Format 1.0 (.gpx) files.
 *
 * @author Christian Pesch
 */

public class Gpx10Format extends GpxFormat {
    private static final Logger log = Logger.getLogger(Gpx10Format.class.getName());
    protected static final String VERSION = "1.0";
    private boolean reuseReadObjectsForWriting;

    public Gpx10Format(boolean reuseReadObjectsForWriting) {
        this.reuseReadObjectsForWriting = reuseReadObjectsForWriting;
    }

    public Gpx10Format() {
        this(true);
    }

    public String getName() {
        return "GPS Exchange Format " + VERSION + " (*" + getExtension() + ")";
    }

    protected List<GpxRoute> process(Gpx gpx) {
        if (gpx == null || !VERSION.equals(gpx.getVersion()))
            return null;

        List<GpxRoute> result = new ArrayList<GpxRoute>();
        GpxRoute wayPointsAsRoute = extractWayPoints(gpx);
        if (wayPointsAsRoute != null)
            result.add(wayPointsAsRoute);
        result.addAll(extractRoutes(gpx));
        result.addAll(extractTracks(gpx));
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

    private List<GpxRoute> extractRoutes(Gpx gpx) {
        List<GpxRoute> result = new ArrayList<GpxRoute>();

        for (Gpx.Rte rte : gpx.getRte()) {
            String name = rte.getName();
            String desc = rte.getDesc();
            List<String> descriptions = asDescription(desc);
            List<GpxPosition> positions = extractRoute(rte);
            result.add(new GpxRoute(this, RouteCharacteristics.Route, name, descriptions, positions, gpx, rte));
        }

        return result;
    }

    private GpxRoute extractWayPoints(Gpx gpx) {
        String name = gpx.getName();
        List<String> descriptions = asDescription(gpx.getDesc());
        List<GpxPosition> positions = extractWayPoints(gpx.getWpt());
        return positions.size() == 0 ? null : new GpxRoute(this, isTripmasterTrack(positions) ? RouteCharacteristics.Track : RouteCharacteristics.Waypoints, name, descriptions, positions, gpx);
    }

    boolean isTripmasterTrack(List<GpxPosition> positions) {
        for (GpxPosition position : positions) {
            if (position.getReason() == null)
                return false;
        }
        return true;
    }

    private List<GpxRoute> extractTracks(Gpx gpx) {
        List<GpxRoute> result = new ArrayList<GpxRoute>();

        for (Gpx.Trk trk : gpx.getTrk()) {
            String name = trk.getName();
            String desc = trk.getDesc();
            List<String> descriptions = asDescription(desc);
            List<GpxPosition> positions = extractTrack(trk);
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

    private List<GpxPosition> extractRoute(Gpx.Rte rte) {
        List<GpxPosition> positions = new ArrayList<GpxPosition>();
        if (rte != null) {
            for (Gpx.Rte.Rtept rtept : rte.getRtept()) {
                positions.add(new GpxPosition(rtept.getLon(), rtept.getLat(), rtept.getEle(), parseSpeed(rtept.getCmt()), parseTime(rtept.getTime()), asComment(rtept.getName(), rtept.getDesc()), rtept));
            }
        }
        return positions;
    }

    private List<GpxPosition> extractWayPoints(List<Gpx.Wpt> wpts) {
        List<GpxPosition> positions = new ArrayList<GpxPosition>();
        for (Gpx.Wpt wpt : wpts) {
            positions.add(new GpxPosition(wpt.getLon(), wpt.getLat(), wpt.getEle(), parseSpeed(wpt.getCmt()), parseTime(wpt.getTime()), asWayPointComment(wpt.getName(), wpt.getDesc()), wpt));
        }
        return positions;
    }

    private List<GpxPosition> extractTrack(Gpx.Trk trk) {
        List<GpxPosition> positions = new ArrayList<GpxPosition>();
        if (trk != null) {
            for (Gpx.Trk.Trkseg trkSeg : trk.getTrkseg()) {
                for (Gpx.Trk.Trkseg.Trkpt trkPt : trkSeg.getTrkpt()) {
                    Double speed = asKmh(Conversion.formatDouble(trkPt.getSpeed()));
                    if(speed == null && trkPt.getCmt() != null)
                        speed = parseSpeed(trkPt.getCmt());
                    positions.add(new GpxPosition(trkPt.getLon(), trkPt.getLat(), trkPt.getEle(), speed, parseTime(trkPt.getTime()), asComment(trkPt.getName(), trkPt.getDesc()), trkPt));
                }
            }
        }
        return positions;
    }

    private String formatSpeed(String comment, Double speed) {
        if (speed == null || speed == 0.0)
            return comment;
        return (comment != null ? comment + " " : "") +
                "Speed: " + Conversion.formatDoubleAsString(speed) + " Km/h";
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
            wpt.setLat(Conversion.formatDouble(position.getLatitude(), 7));
            wpt.setLon(Conversion.formatDouble(position.getLongitude(), 7));
            wpt.setTime(isWriteTime() ? formatTime(position.getTime()) : null);
            wpt.setEle(isWriteElevation() ? Conversion.formatDouble(position.getElevation(), 2) : null);
            if (isWriteSpeed() && reuseReadObjectsForWriting)
                wpt.setCmt(formatSpeed(wpt.getCmt(), position.getSpeed()));
            wpt.setName(isWriteName() ? asName(position.getComment()) : null);
            wpt.setDesc(isWriteName() ? asDesc(position.getComment(), wpt.getDesc()) : null);
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
            rtept.setLat(Conversion.formatDouble(position.getLatitude(), 7));
            rtept.setLon(Conversion.formatDouble(position.getLongitude(), 7));
            rtept.setTime(isWriteTime() ? formatTime(position.getTime()) : null);
            rtept.setEle(isWriteElevation() ? Conversion.formatDouble(position.getElevation(), 2) : null);
            if (isWriteSpeed() && reuseReadObjectsForWriting)
                rtept.setCmt(formatSpeed(rtept.getCmt(), position.getSpeed()));
            rtept.setName(isWriteName() ? asName(position.getComment()) : null);
            rtept.setDesc(isWriteName() ? asDesc(position.getComment(), rtept.getDesc()) : null);
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
            trkpt.setLat(Conversion.formatDouble(position.getLatitude(), 7));
            trkpt.setLon(Conversion.formatDouble(position.getLongitude(), 7));
            trkpt.setTime(isWriteTime() ? formatTime(position.getTime()) : null);
            trkpt.setEle(isWriteElevation() ? Conversion.formatDouble(position.getElevation(), 2) : null);
            trkpt.setSpeed(isWriteSpeed() && position.getSpeed() != null ?
                    Conversion.formatDouble(Conversion.kmhToMs(position.getSpeed()), 3) : null);
            trkpt.setName(isWriteName() ? asName(position.getComment()) : null);
            trkpt.setDesc(isWriteName() ? asDesc(position.getComment(), trkpt.getDesc()) : null);
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

    private Gpx createGpx(GpxRoute route, int startIndex, int endIndex) {
        Gpx gpx = recycleGpx(route);
        if (gpx == null || !reuseReadObjectsForWriting)
            gpx = new ObjectFactory().createGpx();
        gpx.setCreator(BaseNavigationFormat.GENERATED_BY);
        gpx.setVersion(VERSION);
        gpx.setName(route.getName());
        gpx.setDesc(asDescription(route.getDescription()));

        gpx.getWpt().addAll(createWayPoints(route, startIndex, endIndex));
        gpx.getRte().addAll(createRoute(route, startIndex, endIndex));
        gpx.getTrk().addAll(createTrack(route, startIndex, endIndex));
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

    public void write(GpxRoute route, File target, int startIndex, int endIndex) {
        try {
            GpxUtil.marshal10(createGpx(route, startIndex, endIndex), target);
        } catch (JAXBException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public void write(List<GpxRoute> routes, File target) {
        try {
            GpxUtil.marshal10(createGpx(routes), target);
        } catch (JAXBException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
