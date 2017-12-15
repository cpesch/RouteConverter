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
import slash.navigation.base.BaseNavigationFormat;
import slash.navigation.base.MultipleRoutesFormat;
import slash.navigation.base.ParserContext;
import slash.navigation.base.RouteCharacteristics;
import slash.navigation.common.NavigationPosition;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import static java.lang.String.format;
import static slash.common.io.Transfer.trim;
import static slash.navigation.csv.ColumnType.Unsupported;

/**
 * The base of all Excel formats.
 *
 * @author Christian Pesch
 */

public abstract class ExcelFormat extends BaseNavigationFormat<ExcelRoute> implements MultipleRoutesFormat<ExcelRoute> {
    private static final Logger log = Logger.getLogger(ExcelFormat.class.getName());

    public int getMaximumPositionCount() {
        return UNLIMITED_MAXIMUM_POSITION_COUNT;
    }

    public boolean isSupportsMultipleRoutes() {
        return true;
    }

    public boolean isWritingRouteCharacteristics() {
        return true;
    }

    @SuppressWarnings("unchecked")
    public <P extends NavigationPosition> ExcelRoute createRoute(RouteCharacteristics characteristics, String name, List<P> positions) {
        return new ExcelRoute(this, name, (List<ExcelPosition>) positions);
    }

    void parseWorkbook(Workbook workbook, ParserContext<ExcelRoute> context) {
        for (int i = 0, c = workbook.getNumberOfSheets(); i < c; i++) {
            Sheet sheet = workbook.getSheetAt(i);
            parseSheet(sheet, context);
        }
    }

    private void parseSheet(Sheet sheet, ParserContext<ExcelRoute> context) {
        if (sheet.getPhysicalNumberOfRows() < 2)
            return;

        Row header = sheet.getRow(0);
        log.info(format("Parsing sheet '%s' with %d rows and %d columns", sheet.getSheetName(), sheet.getPhysicalNumberOfRows(), header.getPhysicalNumberOfCells()));

        ColumnTypeToRowIndexMapping mapping = parseHeader(header);

        List<ExcelPosition> positions = new ArrayList<>();
        for (int i = 1, c = sheet.getPhysicalNumberOfRows(); i < c; i++)
            positions.add(new ExcelPosition(sheet.getRow(i)));

        context.appendRoute(new ExcelRoute(this, sheet.getSheetName(), mapping, positions));
    }

    private ColumnTypeToRowIndexMapping parseHeader(Row row) {
        ColumnTypeToRowIndexMapping result = new ColumnTypeToRowIndexMapping();
        for (int i = 0, c = row.getPhysicalNumberOfCells(); i < c; i++) {
            Cell cell = row.getCell(i);
            String cellValue = cell.getStringCellValue();
            ColumnType columnType = parseCellValue(cellValue);
            log.info(format("Column %d with name '%s' is identified as %s", i, cellValue, columnType));
            result.add(i, columnType);
        }
        return result;
    }

    private ColumnType parseCellValue(String value) {
        value = trim(value);
        if (value != null) {
            for (ColumnType columnType : ColumnType.values()) {
                if (columnType.toString().equalsIgnoreCase(value))
                    return columnType;
                for (String alternativeName : columnType.getAlternativeNames())
                    if (alternativeName.equalsIgnoreCase(value))
                        return columnType;
            }
        }
        return Unsupported;
    }

    private void populateHeader(Row row, ColumnTypeToRowIndexMapping mapping) {
        for (Integer index : mapping.getIndices()) {
            ColumnType columnType = mapping.getColumnType(index);
            Cell cell = row.createCell(index);
            cell.setCellValue(columnType.name());
        }
    }

    private void populateRow(Row row, ExcelPosition position) {
        ColumnTypeToRowIndexMapping mapping = position.getMapping();
        for (Integer index : mapping.getIndices()) {
            Cell cell = row.createCell(index);
            ColumnType columnType = mapping.getColumnType(index);
            // TODO silly conversion - the ExcelPosition already has a Row
            switch (columnType) {
                case Latitude:
                    cell.setCellValue(position.getLatitude());
                    break;
                case Longitude:
                    cell.setCellValue(position.getLongitude());
                    break;
                case Elevation:
                    cell.setCellValue(position.getElevation());
                    break;
                case Speed:
                    cell.setCellValue(position.getSpeed());
                    break;
                case Time:
                    cell.setCellValue(position.getTime().getTime());
                    break;
                case Description:
                    cell.setCellValue(position.getDescription());
                    break;
                default:
                    cell.setCellValue("Unsupported");
            }
        }
    }

    void populateWorkbook(Workbook workbook, ExcelRoute route, int startIndex, int endIndex) {
        Sheet sheet = workbook.createSheet(route.getName());
        populateHeader(sheet.createRow(0), route.getMapping());
        for (int i = startIndex; i < endIndex; i++) {
            ExcelPosition position = route.getPosition(i);
            populateRow(sheet.createRow(i + 1), position);
        }
    }

    void populateWorkbook(Workbook workbook, List<ExcelRoute> routes) {
        for (ExcelRoute route : routes) {
            populateWorkbook(workbook, route, 0, route.getPositionCount());
        }
    }
}
