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

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import slash.common.type.CompactCalendar;
import slash.navigation.base.BaseNavigationPosition;

import static slash.common.io.Transfer.toDouble;
import static slash.common.type.CompactCalendar.fromDate;
import static slash.navigation.csv.ColumnType.*;
import static slash.navigation.csv.ColumnTypeToRowIndexMapping.DEFAULT;

/**
 * A position from Excel 97-2008 (.xls) and Excel 2008 (.xlsx) files.
 */

public class ExcelPosition extends BaseNavigationPosition {
    private ColumnTypeToRowIndexMapping mapping = DEFAULT;
    private Row row;

    public ExcelPosition(Row row, ColumnTypeToRowIndexMapping mapping) {
        this.row = row;
        this.mapping = mapping;
    }

    public ExcelPosition(Double longitude, Double latitude, Double elevation, Double speed, CompactCalendar time, String description) {
        Workbook workbook = new HSSFWorkbook();
        Sheet sheet = workbook.createSheet("new sheet");
        this.row = sheet.createRow(0);
        setLongitude(longitude);
        setLatitude(latitude);
        setElevation(elevation);
        setSpeed(speed);
        setTime(time);
        setDescription(description);
    }

    Row getRow() {
        return row;
    }

    private Cell getCell(ColumnType type) {
        Integer index = mapping.getIndex(type);
        return index != null ? row.getCell(index) : null;
    }

    private Double getCellAsDouble(ColumnType type) {
        Cell cell = getCell(type);
        return cell != null ? cell.getNumericCellValue() : null;
    }

    private String getCellAsString(ColumnType type) {
        Cell cell = getCell(type);
        return cell != null ? cell.getStringCellValue() : null;
    }

    private CompactCalendar getCellAsTime(ColumnType type) {
        Cell cell = getCell(type);
        return cell != null ? fromDate(cell.getDateCellValue()) : null;
    }

    private Cell getOrCreateCell(ColumnType type) {
        Integer index = mapping.getIndex(type);
        if (index == null)
            return null;
        Cell cell = row.getCell(index);
        if (cell == null) {
            cell = row.createCell(index, type.getCellType());

            if(type.equals(Time)) {
                Workbook workbook = row.getSheet().getWorkbook();
                CellStyle cellStyle = workbook.createCellStyle();
                CreationHelper createHelper = workbook.getCreationHelper();
                cellStyle.setDataFormat(createHelper.createDataFormat().getFormat("m/d/yy h:mm"));
                cell.setCellStyle(cellStyle);
            }
        }
        return cell;
    }

    private void setCellAsDouble(ColumnType type, Double value) {
        Cell cell = getOrCreateCell(type);
        if (cell != null)
            cell.setCellValue(toDouble(value));
    }

    private void setCellAsString(ColumnType type, String value) {
        Cell cell = getOrCreateCell(type);
        if (cell != null)
            cell.setCellValue(value);
    }

    private void setCellAsTime(ColumnType type, CompactCalendar value) {
        Cell cell = getOrCreateCell(type);
        if (cell != null) {
            if (value != null)
                cell.setCellValue(value.getTime());
            else
                cell.setCellValue(0);
        }
    }

    public Double getLongitude() {
        return getCellAsDouble(Longitude);
    }

    public void setLongitude(Double longitude) {
        setCellAsDouble(Longitude, longitude);
    }

    public Double getLatitude() {
        return getCellAsDouble(Latitude);
    }

    public void setLatitude(Double latitude) {
        setCellAsDouble(Latitude, latitude);
    }

    public Double getElevation() {
        return getCellAsDouble(Elevation);
    }

    public void setElevation(Double elevation) {
        setCellAsDouble(Elevation, elevation);
    }

    public CompactCalendar getTime() {
        return getCellAsTime(Time);
    }

    public void setTime(CompactCalendar time) {
        setCellAsTime(Time, time);
    }

    public Double getSpeed() {
        return getCellAsDouble(Speed);
    }

    public void setSpeed(Double speed) {
        setCellAsDouble(Speed, speed);
    }

    public String getDescription() {
        return getCellAsString(Description);
    }

    public void setDescription(String description) {
        setCellAsString(Description, description);
    }
}
