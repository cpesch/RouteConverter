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
package slash.navigation.csv;

import org.apache.poi.ss.usermodel.Row;
import slash.common.type.CompactCalendar;
import slash.navigation.base.*;
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

import static slash.navigation.base.RouteComments.createRouteName;
import static slash.navigation.csv.ColumnTypeToRowIndexMapping.DEFAULT;

/**
 * An Excel route.
 *
 * @author Christian Pesch
 */

public class ExcelRoute extends BaseRoute<ExcelPosition, ExcelFormat> {
    private String name;
    private Row header;
    private List<ExcelPosition> positions;

    public ExcelRoute(ExcelFormat format, RouteCharacteristics characteristics, String name, Row header, List<ExcelPosition> positions) {
        super(format, characteristics);
        this.name = name;
        this.header = header;
        this.positions = positions;
    }

    public ExcelRoute(ExcelFormat format, RouteCharacteristics characteristics, String name, List<Wgs84Position> wgs84Positions) {
        super(format, characteristics);
        this.name = name;

        // TODO might want to extend this simple mapping - and use it in as... methods
        for(Wgs84Position wgs84Position : wgs84Positions)
            positions.add(createPosition(wgs84Position.getLongitude(), wgs84Position.getLatitude(), wgs84Position.getElevation(), wgs84Position.getSpeed(), wgs84Position.getTime(), wgs84Position.getDescription()));
    }


    public String getName() {
        return name != null ? name : createRouteName(getPositions());
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getDescription() {
        return null;
    }

    public List<ExcelPosition> getPositions() {
        return positions;
    }

    public int getPositionCount() {
        return positions.size();
    }

    public void add(int index, ExcelPosition position) {
        // TODO each Row knows its rowIndex - adjust it?
        positions.add(index, position);
    }

    public ExcelPosition createPosition(Double longitude, Double latitude, Double elevation, Double speed, CompactCalendar time, String description) {
        Row row = null; // TODO longitude, latitude, elevation, speed, time, description);
        return new ExcelPosition(DEFAULT, row);
    }

    protected BcrRoute asBcrFormat(BcrFormat format) {
        List<BcrPosition> bcrPositions = new ArrayList<>();
        for (ExcelPosition position : getPositions()) {
            bcrPositions.add(position.asMTPPosition());
        }
        return new BcrRoute(format, getName(), getDescription(), bcrPositions);
    }

    protected ExcelRoute asExcelFormat(ExcelFormat format) {
        return new ExcelRoute(format, getCharacteristics(), getName(), header, positions);
    }

    protected GoPalRoute asGoPalRouteFormat(GoPalRouteFormat format) {
        List<GoPalPosition> gopalPositions = new ArrayList<>();
        for (ExcelPosition position : getPositions()) {
            gopalPositions.add(position.asGoPalRoutePosition());
        }
        return new GoPalRoute(format, getName(), gopalPositions);
    }

    protected GpxRoute asGpxFormat(GpxFormat format) {
        List<GpxPosition> gpxPositions = new ArrayList<>();
        for (ExcelPosition position : getPositions()) {
            gpxPositions.add(position.asGpxPosition());
        }
        return new GpxRoute(format, getCharacteristics(), getName(), getDescription(), gpxPositions);
    }

    protected KmlRoute asKmlFormat(BaseKmlFormat format) {
        List<KmlPosition> kmlPositions = new ArrayList<>();
        for (ExcelPosition position : getPositions()) {
            kmlPositions.add(position.asKmlPosition());
        }
        return new KmlRoute(format, getCharacteristics(), getName(), getDescription(), kmlPositions);
    }

    protected NmeaRoute asNmeaFormat(BaseNmeaFormat format) {
        List<NmeaPosition> nmeaPositions = new ArrayList<>();
        for (ExcelPosition position : getPositions()) {
            nmeaPositions.add(position.asNmeaPosition());
        }
        return new NmeaRoute(format, getCharacteristics(), nmeaPositions);
    }

    protected NmnRoute asNmnFormat(NmnFormat format) {
        List<NmnPosition> nmnPositions = new ArrayList<>();
        for (ExcelPosition position : getPositions()) {
            nmnPositions.add(position.asNmnPosition());
        }
        return new NmnRoute(format, getCharacteristics(), getName(), nmnPositions);
    }

    protected SimpleRoute asPhotoFormat(PhotoFormat format) {
        List<Wgs84Position> wgs84Positions = new ArrayList<>();
        for (ExcelPosition position : getPositions()) {
            wgs84Positions.add(position.asWgs84Position());
        }
        return new Wgs84Route(format, getCharacteristics(), wgs84Positions);
    }

    protected SimpleRoute asSimpleFormat(SimpleFormat format) {
        List<Wgs84Position> positions = new ArrayList<>();
        for (ExcelPosition position : getPositions()) {
            positions.add(position.asWgs84Position());
        }
        return new Wgs84Route(format, getCharacteristics(), positions);
    }

    protected TcxRoute asTcxFormat(TcxFormat format) {
        List<Wgs84Position> wgs84Positions = new ArrayList<>();
        for (ExcelPosition position : getPositions()) {
            wgs84Positions.add(position.asWgs84Position());
        }
        return new TcxRoute(format, getCharacteristics(), getName(), wgs84Positions);
    }

    protected TomTomRoute asTomTomRouteFormat(TomTomRouteFormat format) {
        List<TomTomPosition> tomTomPositions = new ArrayList<>();
        for (ExcelPosition position : getPositions()) {
            tomTomPositions.add(position.asTomTomRoutePosition());
        }
        return new TomTomRoute(format, getCharacteristics(), getName(), tomTomPositions);
    }


    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ExcelRoute that = (ExcelRoute) o;

        return !(header != null ? !header.equals(that.header) : that.header != null) &&
                !(positions != null ? !positions.equals(that.positions) : that.positions != null);
    }

    public int hashCode() {
        int result = header != null ? header.hashCode() : 0;
        result = 31 * result + (positions != null ? positions.hashCode() : 0);
        return result;
    }
}
