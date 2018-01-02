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
import static slash.navigation.csv.ColumnTypeToRowIndexMapping.DEFAULT;

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
        return new ExcelRoute(this, createSheet(name), DEFAULT, (List<ExcelPosition>) positions);
    }

    abstract Sheet createSheet(String name);

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
        for (int i = 1, c = sheet.getPhysicalNumberOfRows(); i < c; i++) {
            Row row = sheet.getRow(i);
            // in case there is just a formatting in a line, the row is null
            if (row != null)
                positions.add(new ExcelPosition(row, mapping));
        }

        context.appendRoute(new ExcelRoute(this, sheet, mapping, positions));
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
            value = value.replaceAll(" ", "");
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
}
