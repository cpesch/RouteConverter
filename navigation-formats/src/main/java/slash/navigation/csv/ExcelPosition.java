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
import slash.common.type.CompactCalendar;
import slash.navigation.base.BaseNavigationPosition;

import static slash.navigation.csv.ColumnType.Latitude;
import static slash.navigation.csv.ColumnType.Longitude;
import static slash.navigation.csv.ColumnTypeToRowIndexMapping.DEFAULT;

/**
 * A position from Excel 97-2008 (.xls) and Excel 2008 (.xlsx) files.
 */

public class ExcelPosition extends BaseNavigationPosition {
    private ColumnTypeToRowIndexMapping mapping = DEFAULT;
    private Row row;

    public ExcelPosition(ColumnTypeToRowIndexMapping mapping, Row row) {
        this.mapping = mapping;
        this.row = row;
    }

    private Cell getCell(ColumnType type) {
        int index = mapping.getIndex(type);
        return row.getCell(index);
    }


    public Double getLongitude() {
        Cell cell = getCell(Longitude);
        // TODO what about , and . ?
        return cell.getNumericCellValue();
    }

    public void setLongitude(Double longitude) {
        getCell(Longitude).setCellValue(longitude);
    }

    public Double getLatitude() {
        Cell cell = getCell(Latitude);
        return cell.getNumericCellValue();
    }

    public void setLatitude(Double latitude) {
        getCell(Latitude).setCellValue(latitude);
    }

    public Double getElevation() {
        return null;
    }

    public void setElevation(Double elevation) {

    }

    public CompactCalendar getTime() {
        return null;
    }

    public void setTime(CompactCalendar time) {

    }

    public Double getSpeed() {
        return null;
    }

    public void setSpeed(Double speed) {

    }

    public String getDescription() {
        return null;
    }

    public void setDescription(String description) {

    }
}
