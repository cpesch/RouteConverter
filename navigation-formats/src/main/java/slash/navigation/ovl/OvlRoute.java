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

package slash.navigation.ovl;

import slash.common.type.CompactCalendar;
import slash.navigation.base.*;
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
import slash.navigation.tcx.TcxFormat;
import slash.navigation.tcx.TcxRoute;

import java.util.ArrayList;
import java.util.List;

import static slash.navigation.base.RouteComments.createRouteName;

/**
 * A Top50 OVL ASCII (.ovl) route.
 *
 * @author Christian Pesch
 */

public class OvlRoute extends BaseRoute<Wgs84Position, OvlFormat> {
    private OvlSection symbol, overlay, mapLage;
    private List<Wgs84Position> positions;

    OvlRoute(OvlFormat format, RouteCharacteristics characteristics, String name,
             OvlSection symbol, OvlSection overlay, OvlSection mapLage,
             List<Wgs84Position> positions) {
        super(format, characteristics);
        this.symbol = symbol;
        this.overlay = overlay;
        this.mapLage = mapLage;
        this.positions = positions;
        setName(name);
    }

    public OvlRoute(RouteCharacteristics characteristics, String name, List<Wgs84Position> positions) {
        this(new OvlFormat(), characteristics, name, new OvlSection(OvlFormat.SYMBOL_TITLE),
                new OvlSection(OvlFormat.OVERLAY_TITLE), new OvlSection(OvlFormat.MAPLAGE_TITLE), positions);
        setName(name);
    }

    OvlSection getSymbol() {
        return symbol;
    }

    OvlSection getOverlay() {
        return overlay;
    }

    OvlSection getMapLage() {
        return mapLage;
    }

    public String getName() {
        String name = symbol.getText();
        return name != null ? name : createRouteName(positions);
    }

    public void setName(String name) {
        symbol.setText(name);
    }

    public List<String> getDescription() {
        return null;
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

    public Wgs84Position createPosition(Double longitude, Double latitude, Double elevation, Double speed, CompactCalendar time, String description) {
        return new Wgs84Position(longitude, latitude, elevation, speed, time, description);
    }

    protected BcrRoute asBcrFormat(BcrFormat format) {
        List<BcrPosition> bcrPositions = new ArrayList<>();
        for (Wgs84Position position : positions) {
            bcrPositions.add(position.asMTPPosition());
        }
        return new BcrRoute(format, getName(), getDescription(), bcrPositions);
    }

    protected ExcelRoute asExcelFormat(ExcelFormat format) {
        List<ExcelPosition> excelPositions = new ArrayList<>();
        ExcelRoute route = new ExcelRoute(format, getName(), excelPositions);
        for (Wgs84Position position : getPositions()) {
            ExcelPosition excelPosition = route.createPosition(position.getLongitude(), position.getLatitude(), position.getElevation(), position.getSpeed(), position.getTime(), position.getDescription());
            excelPositions.add(excelPosition);
        }
        return route;
    }

    protected GoPalRoute asGoPalRouteFormat(GoPalRouteFormat format) {
        List<GoPalPosition> gopalPositions = new ArrayList<>();
        for (Wgs84Position position : positions) {
            gopalPositions.add(position.asGoPalRoutePosition());
        }
        return new GoPalRoute(format, getName(), gopalPositions);
    }

    protected GpxRoute asGpxFormat(GpxFormat format) {
        List<GpxPosition> gpxPositions = new ArrayList<>();
        for (Wgs84Position position : positions) {
            gpxPositions.add(position.asGpxPosition());
        }
        return new GpxRoute(format, getCharacteristics(), getName(), getDescription(), gpxPositions);
    }

    protected SimpleRoute asPhotoFormat(PhotoFormat format) {
        List<Wgs84Position> wgs84Positions = new ArrayList<>();
        for (Wgs84Position position : positions) {
            wgs84Positions.add(position.asWgs84Position());
        }
        return new Wgs84Route(format, getCharacteristics(), wgs84Positions);
    }

    protected KmlRoute asKmlFormat(BaseKmlFormat format) {
        List<KmlPosition> kmlPositions = new ArrayList<>();
        for (Wgs84Position position : positions) {
            kmlPositions.add(position.asKmlPosition());
        }
        return new KmlRoute(format, getCharacteristics(), getName(), getDescription(), kmlPositions);
    }

    protected NmeaRoute asNmeaFormat(BaseNmeaFormat format) {
        List<NmeaPosition> nmeaPositions = new ArrayList<>();
        for (Wgs84Position position : positions) {
            nmeaPositions.add(position.asNmeaPosition());
        }
        return new NmeaRoute(format, getCharacteristics(), nmeaPositions);
    }

    protected NmnRoute asNmnFormat(NmnFormat format) {
        List<NmnPosition> nmnPositions = new ArrayList<>();
        for (Wgs84Position Wgs84Position : positions) {
            nmnPositions.add(Wgs84Position.asNmnPosition());
        }
        return new NmnRoute(format, getCharacteristics(), getName(), nmnPositions);
    }

    protected SimpleRoute asSimpleFormat(SimpleFormat format) {
        List<Wgs84Position> positions = new ArrayList<>();
        for (Wgs84Position Wgs84Position : this.positions) {
            positions.add(Wgs84Position.asWgs84Position());
        }
        return new Wgs84Route(format, getCharacteristics(), positions);
    }

    protected TcxRoute asTcxFormat(TcxFormat format) {
        List<Wgs84Position> wgs84Positions = new ArrayList<>();
        for (Wgs84Position position : positions) {
            wgs84Positions.add(position.asWgs84Position());
        }
        return new TcxRoute(format, getCharacteristics(), getName(), wgs84Positions);
    }

    protected TomTomRoute asTomTomRouteFormat(TomTomRouteFormat format) {
        List<TomTomPosition> tomTomPositions = new ArrayList<>();
        for (Wgs84Position position : positions) {
            tomTomPositions.add(position.asTomTomRoutePosition());
        }
        return new TomTomRoute(format, getCharacteristics(), getName(), tomTomPositions);
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        OvlRoute ovlRoute = (OvlRoute) o;

        return !(mapLage != null ? !mapLage.equals(ovlRoute.mapLage) : ovlRoute.mapLage != null) &&
                !(overlay != null ? !overlay.equals(ovlRoute.overlay) : ovlRoute.overlay != null) &&
                !(positions != null ? !positions.equals(ovlRoute.positions) : ovlRoute.positions != null) &&
                !(symbol != null ? !symbol.equals(ovlRoute.symbol) : ovlRoute.symbol != null);
    }

    public int hashCode() {
        int result;
        result = (symbol != null ? symbol.hashCode() : 0);
        result = 31 * result + (overlay != null ? overlay.hashCode() : 0);
        result = 31 * result + (mapLage != null ? mapLage.hashCode() : 0);
        result = 31 * result + (positions != null ? positions.hashCode() : 0);
        return result;
    }
}
