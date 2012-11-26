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

package slash.navigation.klicktel;

import slash.common.type.CompactCalendar;
import slash.navigation.base.BaseRoute;
import slash.navigation.base.SimpleFormat;
import slash.navigation.base.SimpleRoute;
import slash.navigation.base.Wgs84Position;
import slash.navigation.base.Wgs84Route;
import slash.navigation.bcr.BcrFormat;
import slash.navigation.bcr.BcrPosition;
import slash.navigation.bcr.BcrRoute;
import slash.navigation.gopal.GoPalPosition;
import slash.navigation.gopal.GoPalRoute;
import slash.navigation.gopal.GoPalRouteFormat;
import slash.navigation.gpx.GpxFormat;
import slash.navigation.gpx.GpxPosition;
import slash.navigation.gpx.GpxRoute;
import slash.navigation.itn.TomTomPosition;
import slash.navigation.itn.TomTomRoute;
import slash.navigation.itn.TomTomRouteFormat;
import slash.navigation.klicktel.binding.KDRoute;
import slash.navigation.kml.BaseKmlFormat;
import slash.navigation.kml.KmlPosition;
import slash.navigation.kml.KmlRoute;
import slash.navigation.nmea.BaseNmeaFormat;
import slash.navigation.nmea.NmeaPosition;
import slash.navigation.nmea.NmeaRoute;
import slash.navigation.nmn.NmnFormat;
import slash.navigation.nmn.NmnPosition;
import slash.navigation.nmn.NmnRoute;
import slash.navigation.tcx.Tcx1Format;
import slash.navigation.tcx.Tcx2Format;

import java.util.ArrayList;
import java.util.List;

import static slash.navigation.base.RouteCharacteristics.Route;
import static slash.navigation.util.RouteComments.createRouteName;

/**
 * A klickTel Routenplaner 2009 (.krt) route.
 *
 * @author Christian Pesch
 */

public class KlickTelRoute extends BaseRoute<Wgs84Position, KlickTelRouteFormat> {
    private String name;
    private KDRoute.RouteOptions options;
    private List<Wgs84Position> positions;

    public KlickTelRoute(String name, List<Wgs84Position> positions) {
        this(name, defaultOptions(), positions);
    }

    private static KDRoute.RouteOptions defaultOptions() {
        return new KDRoute.RouteOptions();
    }

    public KlickTelRoute(String name, KDRoute.RouteOptions options, List<Wgs84Position> positions) {
        super(new KlickTelRouteFormat(), Route);
        this.options = options;
        this.positions = positions;
        setName(name);
    }

    public String getName() {
        return name != null ? name : createRouteName(positions);
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getDescription() {
        return null;
    }

    public KDRoute.RouteOptions getOptions() {
        return options;
    }

    public List<Wgs84Position> getPositions() {
        return positions;
    }

    public int getPositionCount() {
        return positions.size();
    }

    public void add(int index, Wgs84Position position) {
        positions.add(index, position);
    }

    public Wgs84Position createPosition(Double longitude, Double latitude, Double elevation, Double speed, CompactCalendar time, String comment) {
        return new Wgs84Position(longitude, latitude, elevation, speed, time, comment);
    }

    protected BcrRoute asBcrFormat(BcrFormat format) {
        List<BcrPosition> bcrPositions = new ArrayList<BcrPosition>();
        for (Wgs84Position position : positions) {
            bcrPositions.add(position.asMTPPosition());
        }
        return new BcrRoute(format, getName(), getDescription(), bcrPositions);
    }

    protected GoPalRoute asGoPalRouteFormat(GoPalRouteFormat format) {
        List<GoPalPosition> gopalPositions = new ArrayList<GoPalPosition>();
        for (Wgs84Position position : positions) {
            gopalPositions.add(position.asGoPalRoutePosition());
        }
        return new GoPalRoute(format, getName(), gopalPositions);
    }

    protected GpxRoute asGpxFormat(GpxFormat format) {
        List<GpxPosition> gpxPositions = new ArrayList<GpxPosition>();
        for (Wgs84Position position : positions) {
            gpxPositions.add(position.asGpxPosition());
        }
        return new GpxRoute(format, getCharacteristics(), getName(), getDescription(), gpxPositions);
    }

    protected KmlRoute asKmlFormat(BaseKmlFormat format) {
        List<KmlPosition> kmlPositions = new ArrayList<KmlPosition>();
        for (Wgs84Position position : positions) {
            kmlPositions.add(position.asKmlPosition());
        }
        return new KmlRoute(format, getCharacteristics(), getName(), getDescription(), kmlPositions);
    }

    protected NmeaRoute asNmeaFormat(BaseNmeaFormat format) {
        List<NmeaPosition> nmeaPositions = new ArrayList<NmeaPosition>();
        for (Wgs84Position position : positions) {
            nmeaPositions.add(position.asNmeaPosition());
        }
        return new NmeaRoute(format, getCharacteristics(), nmeaPositions);
    }

    protected NmnRoute asNmnFormat(NmnFormat format) {
        List<NmnPosition> nmnPositions = new ArrayList<NmnPosition>();
        for (Wgs84Position Wgs84Position : positions) {
            nmnPositions.add(Wgs84Position.asNmnPosition());
        }
        return new NmnRoute(format, getCharacteristics(), name, nmnPositions);
    }

    protected SimpleRoute asSimpleFormat(SimpleFormat format) {
        List<Wgs84Position> Wgs84Positions = new ArrayList<Wgs84Position>();
        for (Wgs84Position position : positions) {
            Wgs84Positions.add(position.asWgs84Position());
        }
        return new Wgs84Route(format, getCharacteristics(), Wgs84Positions);
    }

    protected TomTomRoute asTomTomRouteFormat(TomTomRouteFormat format) {
        List<TomTomPosition> tomTomPositions = new ArrayList<TomTomPosition>();
        for (Wgs84Position position : positions) {
            tomTomPositions.add(position.asTomTomRoutePosition());
        }
        return new TomTomRoute(format, getCharacteristics(), getName(), tomTomPositions);
    }

    public GpxRoute asTcx1Format() {
        return asGpxFormat(new Tcx1Format());
    }

    public GpxRoute asTcx2Format() {
        return asGpxFormat(new Tcx2Format());
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        KlickTelRoute klicktelRoute = (KlickTelRoute) o;

        return !(name != null ? !name.equals(klicktelRoute.name) : klicktelRoute.name != null) &&
                !(positions != null ? !positions.equals(klicktelRoute.positions) : klicktelRoute.positions != null);
    }

    public int hashCode() {
        int result = (name != null ? name.hashCode() : 0);
        result = 31 * result + (positions != null ? positions.hashCode() : 0);
        return result;
    }
}