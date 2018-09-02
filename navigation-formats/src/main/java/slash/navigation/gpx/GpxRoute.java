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
import slash.navigation.csv.CsvFormat;
import slash.navigation.csv.CsvPosition;
import slash.navigation.csv.CsvRoute;
import slash.navigation.excel.ExcelFormat;
import slash.navigation.excel.ExcelPosition;
import slash.navigation.excel.ExcelRoute;
import slash.navigation.gopal.GoPalPosition;
import slash.navigation.gopal.GoPalRoute;
import slash.navigation.gopal.GoPalRouteFormat;
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

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static slash.common.type.CompactCalendar.now;
import static slash.common.type.ISO8601.formatDate;
import static slash.navigation.base.RouteCharacteristics.Waypoints;

/**
 * A GPS Exchange Format (.gpx) route.
 *
 * @author Christian Pesch
 */

public class GpxRoute extends BaseRoute<GpxPosition, GpxFormat> {
    private String name;
    private List<String> description;
    private List<GpxPosition> positions;
    private List<Object> origins;

    public GpxRoute(GpxFormat format, RouteCharacteristics characteristics,
                    String name, List<String> description, List<GpxPosition> positions,
                    Object... origins) {
        super(format, characteristics);
        this.name = name;
        this.description = description;
        this.positions = positions;
        this.origins = asList(origins);
    }

    public GpxRoute(GpxFormat format, RouteCharacteristics characteristics,
                    String name, List<String> description, List<GpxPosition> positions) {
        this(format, characteristics, name, description, positions, new Object[0]);
    }

    public GpxRoute(GpxFormat format) {
        this(format, Waypoints, "?", singletonList("Created at " + formatDate(now())), new ArrayList<GpxPosition>());
    }


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getDescription() {
        return description;
    }

    public List<GpxPosition> getPositions() {
        return positions;
    }

    List<Object> getOrigins() {
        return origins;
    }

    @SuppressWarnings({"unchecked"})
    public <T> T getOrigin(Class<T> resultClass) {
        for (Object origin : origins) {
            if (resultClass.isInstance(origin))
                return (T) origin;
        }
        return null;
    }

    public int getPositionCount() {
        return positions.size();
    }

    public void add(int index, GpxPosition position) {
        positions.add(index, position);
    }

    public GpxPosition createPosition(Double longitude, Double latitude, Double elevation, Double speed, CompactCalendar time, String description) {
        return new GpxPosition(longitude, latitude, elevation, speed, time, description);
    }

    protected BcrRoute asBcrFormat(BcrFormat format) {
        List<BcrPosition> bcrPositions = new ArrayList<>();
        for (GpxPosition position : positions) {
            BcrPosition bcrPosition = position.asMTPPosition();
            // shortens description to better fit to Map&Guide Tourenplaner station list
            String location = position.getCity();
            if (location != null)
                bcrPosition.setDescription(location);
            bcrPositions.add(bcrPosition);
        }
        return new BcrRoute(format, getName(), getDescription(), bcrPositions);
    }

    protected CsvRoute asCsvFormat(CsvFormat format) {
        List<CsvPosition> positions = new ArrayList<>();
        for (GpxPosition position : getPositions()) {
            positions.add(position.asCsvPosition());
        }
        return new CsvRoute(format, getName(), positions);
    }

    protected ExcelRoute asExcelFormat(ExcelFormat format) {
        List<ExcelPosition> excelPositions = new ArrayList<>();
        ExcelRoute route = new ExcelRoute(format, getName(), excelPositions);
        for (GpxPosition position : getPositions()) {
            ExcelPosition excelPosition = route.createPosition(position.getLongitude(), position.getLatitude(), position.getElevation(), position.getSpeed(), position.getTime(), position.getDescription());
            excelPositions.add(excelPosition);
        }
        return route;
    }

    protected GoPalRoute asGoPalRouteFormat(GoPalRouteFormat format) {
        List<GoPalPosition> gopalPositions = new ArrayList<>();
        for (GpxPosition position : positions) {
            gopalPositions.add(position.asGoPalRoutePosition());
        }
        return new GoPalRoute(format, getName(), gopalPositions);
    }

    protected GpxRoute asGpxFormat(GpxFormat format) {
        List<GpxPosition> gpxPositions = new ArrayList<>(getPositions());
        return new GpxRoute(format, getCharacteristics(), getName(), getDescription(), gpxPositions);
    }

    protected SimpleRoute asPhotoFormat(PhotoFormat format) {
        List<Wgs84Position> wgs84Positions = new ArrayList<>();
        for (GpxPosition position : positions) {
            wgs84Positions.add(position.asWgs84Position());
        }
        return new Wgs84Route(format, getCharacteristics(), wgs84Positions);
    }

    protected KmlRoute asKmlFormat(BaseKmlFormat format) {
        List<KmlPosition> kmlPositions = new ArrayList<>();
        for (GpxPosition position : positions) {
            kmlPositions.add(position.asKmlPosition());
        }
        return new KmlRoute(format, getCharacteristics(), getName(), getDescription(), kmlPositions);
    }

    protected NmeaRoute asNmeaFormat(BaseNmeaFormat format) {
        List<NmeaPosition> nmeaPositions = new ArrayList<>();
        for (GpxPosition position : positions) {
            nmeaPositions.add(position.asNmeaPosition());
        }
        return new NmeaRoute(format, getCharacteristics(), nmeaPositions);
    }

    protected NmnRoute asNmnFormat(NmnFormat format) {
        List<NmnPosition> nmnPositions = new ArrayList<>();
        for (GpxPosition position : positions) {
            nmnPositions.add(position.asNmnPosition());
        }
        return new NmnRoute(format, getCharacteristics(), getName(), nmnPositions);
    }

    protected SimpleRoute asSimpleFormat(SimpleFormat format) {
        List<Wgs84Position> wgs84Positions = new ArrayList<>();
        for (GpxPosition position : positions) {
            wgs84Positions.add(position.asWgs84Position());
        }
        return new Wgs84Route(format, getCharacteristics(), wgs84Positions);
    }

    protected TcxRoute asTcxFormat(TcxFormat format) {
        List<Wgs84Position> wgs84Positions = new ArrayList<>();
        for (GpxPosition position : positions) {
            wgs84Positions.add(position.asWgs84Position());
        }
        return new TcxRoute(format, getCharacteristics(), getName(), wgs84Positions);
    }

    protected TomTomRoute asTomTomRouteFormat(TomTomRouteFormat format) {
        List<TomTomPosition> tomTomPositions = new ArrayList<>();
        for (GpxPosition position : positions) {
            tomTomPositions.add(position.asTomTomRoutePosition());
        }
        return new TomTomRoute(format, getCharacteristics(), getName(), tomTomPositions);
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final GpxRoute gpxRoute = (GpxRoute) o;

        return !(description != null ? !description.equals(gpxRoute.description) : gpxRoute.description != null) &&
                !(name != null ? !name.equals(gpxRoute.name) : gpxRoute.name != null) &&
                getCharacteristics().equals(gpxRoute.getCharacteristics()) &&
                positions.equals(gpxRoute.positions);
    }

    public int hashCode() {
        int result;
        result = (name != null ? name.hashCode() : 0);
        result = 29 * result + (description != null ? description.hashCode() : 0);
        result = 29 * result + getCharacteristics().hashCode();
        result = 29 * result + positions.hashCode();
        return result;
    }
}
