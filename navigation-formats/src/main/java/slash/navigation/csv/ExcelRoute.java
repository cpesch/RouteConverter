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

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
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

import static slash.navigation.base.RouteCharacteristics.Track;
import static slash.navigation.base.RouteComments.createRouteName;
import static slash.navigation.csv.ColumnTypeToRowIndexMapping.DEFAULT;

/**
 * An Excel route.
 *
 * @author Christian Pesch
 */

public class ExcelRoute extends BaseRoute<ExcelPosition, ExcelFormat> {
    private Sheet sheet;
    private ColumnTypeToRowIndexMapping mapping = DEFAULT;
    private List<ExcelPosition> positions;

    public ExcelRoute(ExcelFormat format, Sheet sheet, ColumnTypeToRowIndexMapping mapping, List<ExcelPosition> positions) {
        super(format, Track);
        this.sheet = sheet;
        this.mapping = mapping;
        this.positions = positions;
    }

    public ExcelRoute(ExcelFormat format, String name, List<ExcelPosition> positions) {
        this(format, format.createSheet(name), DEFAULT, positions);
        populateHeader(sheet.createRow(0));
    }

    private void populateHeader(Row row) {
        for (Integer index : mapping.getIndices()) {
            ColumnType columnType = mapping.getColumnType(index);
            Cell cell = row.createCell(index);
            cell.setCellValue(columnType.name());
        }
    }

    public String getName() {
        String name = sheet.getSheetName();
        return name != null ? name : createRouteName(getPositions());
    }

    public void setName(String name) {
        Workbook workbook = sheet.getWorkbook();
        workbook.setSheetName(workbook.getSheetIndex(sheet), name);
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

    Workbook getWorkbook() {
        return sheet.getWorkbook();
    }

    protected void move(int index, int upOrDown) {
        if(upOrDown == -1) {
            // TODO replace index row with index-1 row
        } else {
            // TODO replace index row with index+1 row
            ExcelPosition next = getPosition(index + 1);
            ExcelPosition toMove = getPosition(index);
        }
        super.move(index, upOrDown);
    }

    public void add(int index, ExcelPosition position) {
        // shift all rows from index (+1 for header) one position to the back
        sheet.shiftRows(index + 1, getPositionCount(), 1);
        positions.add(index, position);
    }

    public ExcelPosition createPosition(Double longitude, Double latitude, Double elevation, Double speed, CompactCalendar time, String description) {
        ExcelPosition position = new ExcelPosition(sheet.createRow(getPositionCount() + 1), mapping);
        position.setLongitude(longitude);
        position.setLatitude(latitude);
        position.setElevation(elevation);
        position.setSpeed(speed);
        position.setTime(time);
        position.setDescription(description);
        return position;
    }

    protected BcrRoute asBcrFormat(BcrFormat format) {
        List<BcrPosition> bcrPositions = new ArrayList<>();
        for (ExcelPosition position : getPositions()) {
            bcrPositions.add(position.asMTPPosition());
        }
        return new BcrRoute(format, getName(), getDescription(), bcrPositions);
    }

    protected ExcelRoute asExcelFormat(ExcelFormat format) {
        List<ExcelPosition> excelPositions = new ArrayList<>(getPositions());
        return new ExcelRoute(format, getName(), excelPositions);
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

        return !(getName() != null ? !getName().equals(that.getName()) : that.getName() != null) &&
                !(mapping != null ? !mapping.equals(that.mapping) : that.mapping != null) &&
                !(positions != null ? !positions.equals(that.positions) : that.positions != null);
    }

    public int hashCode() {
        int result = getName() != null ? getName().hashCode() : 0;
        result = 31 * result + (mapping != null ? mapping.hashCode() : 0);
        result = 31 * result + (positions != null ? positions.hashCode() : 0);
        return result;
    }
}
