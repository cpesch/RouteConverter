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

import slash.common.type.CompactCalendar;
import slash.navigation.base.BaseRoute;
import slash.navigation.base.SimpleFormat;
import slash.navigation.base.SimpleRoute;
import slash.navigation.bcr.BcrFormat;
import slash.navigation.bcr.BcrPosition;
import slash.navigation.bcr.BcrRoute;
import slash.navigation.excel.ExcelFormat;
import slash.navigation.excel.ExcelRoute;
import slash.navigation.gopal.GoPalRoute;
import slash.navigation.gopal.GoPalRouteFormat;
import slash.navigation.gpx.GpxFormat;
import slash.navigation.gpx.GpxRoute;
import slash.navigation.itn.TomTomRoute;
import slash.navigation.itn.TomTomRouteFormat;
import slash.navigation.kml.BaseKmlFormat;
import slash.navigation.kml.KmlRoute;
import slash.navigation.nmea.BaseNmeaFormat;
import slash.navigation.nmea.NmeaRoute;
import slash.navigation.nmn.NmnFormat;
import slash.navigation.nmn.NmnRoute;
import slash.navigation.photo.PhotoFormat;
import slash.navigation.tcx.TcxFormat;
import slash.navigation.tcx.TcxRoute;

import java.util.ArrayList;
import java.util.List;

import static slash.navigation.base.RouteCharacteristics.Track;
import static slash.navigation.base.RouteComments.createRouteName;

/**
 * A CSV route.
 *
 * @author Christian Pesch
 */

public class CsvRoute extends BaseRoute<CsvPosition, CsvFormat> {
    private String name;
    private List<CsvPosition> positions;

    public CsvRoute(CsvFormat format, String name, List<CsvPosition> positions) {
        super(format, Track);
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
        throw new UnsupportedOperationException(); // TODO implement me
    }

    public List<CsvPosition> getPositions() {
        return positions;
    }

    public int getPositionCount() {
        return positions.size();
    }

    public void add(int index, CsvPosition position) {
        throw new UnsupportedOperationException(); // TODO implement me
    }

    public CsvPosition createPosition(Double longitude, Double latitude, Double elevation, Double speed, CompactCalendar time, String description) {
        return null; // TODO fix me
    }

    protected BcrRoute asBcrFormat(BcrFormat format) {
        return null; // TODO fix me
    }

    protected CsvRoute asCsvFormat(CsvFormat format) {
        List<CsvPosition> positions = new ArrayList<>(getPositions());
        return new CsvRoute(format, getName(), positions);
    }

    protected ExcelRoute asExcelFormat(ExcelFormat format) {
        return null; // TODO fix me
    }

    protected GoPalRoute asGoPalRouteFormat(GoPalRouteFormat format) {
        return null; // TODO fix me
    }

    protected GpxRoute asGpxFormat(GpxFormat format) {
        return null; // TODO fix me
    }

    protected SimpleRoute asPhotoFormat(PhotoFormat format) {
        return null; // TODO fix me
    }

    protected KmlRoute asKmlFormat(BaseKmlFormat format) {
        return null; // TODO fix me
    }

    protected NmeaRoute asNmeaFormat(BaseNmeaFormat format) {
        return null; // TODO fix me
    }

    protected NmnRoute asNmnFormat(NmnFormat format) {
        return null; // TODO fix me
    }

    protected SimpleRoute asSimpleFormat(SimpleFormat format) {
        return null; // TODO fix me
    }

    protected TcxRoute asTcxFormat(TcxFormat format) {
        return null; // TODO fix me
    }

    protected TomTomRoute asTomTomRouteFormat(TomTomRouteFormat format) {
        return null; // TODO fix me
    }
}
