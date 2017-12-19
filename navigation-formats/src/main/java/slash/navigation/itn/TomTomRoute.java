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
package slash.navigation.itn;

import slash.common.type.CompactCalendar;
import slash.navigation.base.BaseRoute;
import slash.navigation.base.RouteCharacteristics;
import slash.navigation.base.SimpleFormat;
import slash.navigation.base.SimpleRoute;
import slash.navigation.base.Wgs84Position;
import slash.navigation.base.Wgs84Route;
import slash.navigation.bcr.BcrFormat;
import slash.navigation.bcr.BcrPosition;
import slash.navigation.bcr.BcrRoute;
import slash.navigation.csv.ExcelFormat;
import slash.navigation.csv.ExcelPosition;
import slash.navigation.csv.ExcelRoute;
import slash.navigation.gopal.GoPalPosition;
import slash.navigation.gopal.GoPalRoute;
import slash.navigation.gopal.GoPalRouteFormat;
import slash.navigation.gpx.GpxFormat;
import slash.navigation.gpx.GpxPosition;
import slash.navigation.gpx.GpxRoute;
import slash.navigation.photo.PhotoFormat;
import slash.navigation.kml.BaseKmlFormat;
import slash.navigation.kml.KmlPosition;
import slash.navigation.kml.KmlRoute;
import slash.navigation.nmea.BaseNmeaFormat;
import slash.navigation.nmea.NmeaPosition;
import slash.navigation.nmea.NmeaRoute;
import slash.navigation.nmn.NmnFormat;
import slash.navigation.nmn.NmnPosition;
import slash.navigation.nmn.NmnRoute;
import slash.navigation.tcx.TcxFormat;
import slash.navigation.tcx.TcxRoute;

import java.util.ArrayList;
import java.util.List;

import static slash.navigation.base.RouteComments.createRouteName;

/**
 * Represents the route in a Tom Tom Route (.itn) file.
 *
 * @author Christian Pesch
 */

public class TomTomRoute extends BaseRoute<TomTomPosition, TomTomRouteFormat> {
    private String name;
    private List<TomTomPosition> positions;

    public TomTomRoute(TomTomRouteFormat format, RouteCharacteristics characteristics, String name, List<TomTomPosition> positions) {
        super(format, characteristics);
        this.name = name;
        this.positions = positions;
    }

    public TomTomRoute(RouteCharacteristics characteristics, String name, List<TomTomPosition> positions) {
        this(new TomTom5RouteFormat(), characteristics, name, positions);
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

    public List<TomTomPosition> getPositions() {
        return positions;
    }

    public int getPositionCount() {
        return positions.size();
    }

    public void add(int index, TomTomPosition position) {
        positions.add(index, position);
    }

    public TomTomPosition createPosition(Double longitude, Double latitude, Double elevation, Double speed, CompactCalendar time, String description) {
        return new TomTomPosition(longitude, latitude, elevation, speed, time, description);
    }

    protected BcrRoute asBcrFormat(BcrFormat format) {
        List<BcrPosition> bcrPositions = new ArrayList<>();
        for (TomTomPosition tomTomPosition : positions) {
            BcrPosition bcrPosition = tomTomPosition.asMTPPosition();
            // shortens description to better fit to Map&Guide Tourenplaner station list
            String location = tomTomPosition.getCity();
            if (location != null)
                bcrPosition.setDescription(location);
            bcrPositions.add(bcrPosition);
        }
        return new BcrRoute(format, getName(), getDescription(), bcrPositions);
    }

    protected ExcelRoute asExcelFormat(ExcelFormat format) {
        List<ExcelPosition> excelPositions = new ArrayList<>();
        ExcelRoute route = new ExcelRoute(format, getName(), excelPositions);
        for (TomTomPosition position : getPositions()) {
            ExcelPosition excelPosition = route.createPosition(position.getLongitude(), position.getLatitude(), position.getElevation(), position.getSpeed(), position.getTime(), position.getDescription());
            excelPositions.add(excelPosition);
        }
        return route;
    }

    protected GoPalRoute asGoPalRouteFormat(GoPalRouteFormat format) {
        List<GoPalPosition> gopalPositions = new ArrayList<>();
        for (TomTomPosition position : positions) {
            gopalPositions.add(position.asGoPalRoutePosition());
        }
        return new GoPalRoute(format, getName(), gopalPositions);
    }

    protected GpxRoute asGpxFormat(GpxFormat format) {
        List<GpxPosition> gpxPositions = new ArrayList<>();
        for (TomTomPosition position : positions) {
            gpxPositions.add(position.asGpxPosition());
        }
        return new GpxRoute(format, getCharacteristics(), getName(), getDescription(), gpxPositions);
    }

    protected SimpleRoute asPhotoFormat(PhotoFormat format) {
        List<Wgs84Position> wgs84Positions = new ArrayList<>();
        for (TomTomPosition position : positions) {
            wgs84Positions.add(position.asWgs84Position());
        }
        return new Wgs84Route(format, getCharacteristics(), wgs84Positions);
    }

    protected KmlRoute asKmlFormat(BaseKmlFormat format) {
        List<KmlPosition> kmlPositions = new ArrayList<>();
        for (TomTomPosition tomTomPosition : positions) {
            kmlPositions.add(tomTomPosition.asKmlPosition());
        }
        return new KmlRoute(format, getCharacteristics(), getName(), getDescription(), kmlPositions);
    }

    protected NmeaRoute asNmeaFormat(BaseNmeaFormat format) {
        List<NmeaPosition> nmeaPositions = new ArrayList<>();
        for (TomTomPosition position : positions) {
            nmeaPositions.add(position.asNmeaPosition());
        }
        return new NmeaRoute(format, getCharacteristics(), nmeaPositions);
    }

    protected NmnRoute asNmnFormat(NmnFormat format) {
        List<NmnPosition> nmnPositions = new ArrayList<>();
        for (TomTomPosition position : positions) {
            nmnPositions.add(position.asNmnPosition());
        }
        return new NmnRoute(format, getCharacteristics(), name, nmnPositions);
    }

    protected SimpleRoute asSimpleFormat(SimpleFormat format) {
        List<Wgs84Position> simplePositions = new ArrayList<>();
        for (TomTomPosition tomTomPosition : positions) {
            simplePositions.add(tomTomPosition.asWgs84Position());
        }
        return new Wgs84Route(format, getCharacteristics(), simplePositions);
    }

    protected TcxRoute asTcxFormat(TcxFormat format) {
        List<Wgs84Position> wgs84Positions = new ArrayList<>();
        for (TomTomPosition position : positions) {
            wgs84Positions.add(position.asWgs84Position());
        }
        return new TcxRoute(format, getCharacteristics(), getName(), wgs84Positions);
    }

    protected TomTomRoute asTomTomRouteFormat(TomTomRouteFormat format) {
        List<TomTomPosition> tomTomPositions = new ArrayList<>(getPositions());
        return new TomTomRoute(format, getCharacteristics(), getName(), tomTomPositions);
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TomTomRoute route = (TomTomRoute) o;

        return !(name != null ? !name.equals(route.name) : route.name != null) &&
                getCharacteristics().equals(route.getCharacteristics()) &&
                positions.equals(route.positions);
    }

    public int hashCode() {
        int result;
        result = (name != null ? name.hashCode() : 0);
        result = 29 * result + getCharacteristics().hashCode();
        result = 29 * result + positions.hashCode();
        return result;
    }
}
