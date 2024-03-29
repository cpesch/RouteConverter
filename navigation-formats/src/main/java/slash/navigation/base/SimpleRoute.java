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

package slash.navigation.base;

import slash.navigation.bcr.BcrFormat;
import slash.navigation.bcr.BcrPosition;
import slash.navigation.bcr.BcrRoute;
import slash.navigation.csv.CsvFormat;
import slash.navigation.csv.CsvPosition;
import slash.navigation.csv.CsvRoute;
import slash.navigation.excel.ExcelFormat;
import slash.navigation.excel.ExcelPosition;
import slash.navigation.excel.ExcelRoute;
import slash.navigation.gopal.GoPalPosition;
import slash.navigation.gopal.GoPalRoute;
import slash.navigation.gopal.GoPalRouteFormat;
import slash.navigation.gpx.GpxFormat;
import slash.navigation.gpx.GpxPosition;
import slash.navigation.gpx.GpxRoute;
import slash.navigation.itn.TomTomPosition;
import slash.navigation.itn.TomTomRoute;
import slash.navigation.itn.TomTomRouteFormat;
import slash.navigation.kml.BaseKmlFormat;
import slash.navigation.kml.KmlPosition;
import slash.navigation.kml.KmlRoute;
import slash.navigation.nmea.BaseNmeaFormat;
import slash.navigation.nmea.NmeaPosition;
import slash.navigation.nmea.NmeaRoute;
import slash.navigation.nmn.NmnFormat;
import slash.navigation.nmn.NmnPosition;
import slash.navigation.nmn.NmnRoute;
import slash.navigation.photo.PhotoFormat;
import slash.navigation.tcx.TcxFormat;
import slash.navigation.tcx.TcxRoute;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static slash.navigation.base.RouteComments.createRouteName;

/**
 * Represents the simple most route.
 *
 * @author Christian Pesch
 */

public abstract class SimpleRoute<P extends BaseNavigationPosition, F extends BaseNavigationFormat> extends BaseRoute<P, F> {
    protected String name;
    protected List<P> positions;

    public SimpleRoute(F format, RouteCharacteristics characteristics, List<P> positions) {
        this(format, characteristics, null, positions);
    }

    public SimpleRoute(F format, RouteCharacteristics characteristics, String name, List<P> positions) {
        super(format, characteristics);
        this.name = name;
        this.positions = positions;
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

    public List<P> getPositions() {
        return positions;
    }

    public int getPositionCount() {
        return positions.size();
    }

    public void add(int index, P position) {
        positions.add(index, position);
    }

    protected BcrRoute asBcrFormat(BcrFormat format) {
        List<BcrPosition> positions = new ArrayList<>();
        for (P position : getPositions()) {
            positions.add(position.asMTPPosition());
        }
        return new BcrRoute(format, getName(), getDescription(), positions);
    }

    protected CsvRoute asCsvFormat(CsvFormat format) {
        List<CsvPosition> positions = new ArrayList<>();
        for (P position : getPositions()) {
            positions.add(position.asCsvPosition());
        }
        return new CsvRoute(format, getName(), positions);
    }

    protected ExcelRoute asExcelFormat(ExcelFormat format) {
        List<ExcelPosition> positions = new ArrayList<>();
        ExcelRoute route = new ExcelRoute(format, getName(), positions);
        for (P position : getPositions()) {
            ExcelPosition excelPosition = route.createPosition(position.getLongitude(), position.getLatitude(), position.getElevation(), position.getSpeed(), position.getTime(), position.getDescription());
            positions.add(excelPosition);
        }
        return route;
    }

    protected GoPalRoute asGoPalRouteFormat(GoPalRouteFormat format) {
        List<GoPalPosition> positions = new ArrayList<>();
        for (P position : getPositions()) {
            positions.add(position.asGoPalRoutePosition());
        }
        return new GoPalRoute(format, getName(), positions);
    }

    protected GpxRoute asGpxFormat(GpxFormat format) {
        List<GpxPosition> positions = new ArrayList<>();
        for (P position : getPositions()) {
            positions.add(position.asGpxPosition());
        }
        return new GpxRoute(format, getCharacteristics(), getName(), getDescription(), positions);
    }

    protected SimpleRoute asPhotoFormat(PhotoFormat format) {
        List<Wgs84Position> positions = new ArrayList<>();
        for (P position : getPositions()) {
            positions.add(position.asWgs84Position());
        }
        return new Wgs84Route(format, getCharacteristics(), getName(), positions);
    }

    protected KmlRoute asKmlFormat(BaseKmlFormat format) {
        List<KmlPosition> positions = new ArrayList<>();
        for (P position : getPositions()) {
            positions.add(position.asKmlPosition());
        }
        return new KmlRoute(format, getCharacteristics(), getName(), getDescription(), positions);
    }

    protected NmeaRoute asNmeaFormat(BaseNmeaFormat format) {
        List<NmeaPosition> positions = new ArrayList<>();
        for (P position : getPositions()) {
            positions.add(position.asNmeaPosition());
        }
        return new NmeaRoute(format, getCharacteristics(), positions);
    }

    protected NmnRoute asNmnFormat(NmnFormat format) {
        List<NmnPosition> positions = new ArrayList<>();
        for (P position : getPositions()) {
            positions.add(position.asNmnPosition());
        }
        return new NmnRoute(format, getCharacteristics(), name, positions);
    }

    protected SimpleRoute asSimpleFormat(SimpleFormat format) {
        List<Wgs84Position> positions = new ArrayList<>();
        for (P position : getPositions()) {
            positions.add(position.asWgs84Position());
        }
        return new Wgs84Route(format, getCharacteristics(), getName(), positions);
    }

    protected TcxRoute asTcxFormat(TcxFormat format) {
        List<Wgs84Position> positions = new ArrayList<>();
        for (P position : getPositions()) {
            positions.add(position.asWgs84Position());
        }
        return new TcxRoute(format, getCharacteristics(), getName(), positions);
    }

    protected TomTomRoute asTomTomRouteFormat(TomTomRouteFormat format) {
        List<TomTomPosition> positions = new ArrayList<>();
        for (P position : getPositions()) {
            positions.add(position.asTomTomRoutePosition());
        }
        return new TomTomRoute(format, getCharacteristics(), getName(), positions);
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SimpleRoute route = (SimpleRoute) o;

        return Objects.equals(name, route.name) &&
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
