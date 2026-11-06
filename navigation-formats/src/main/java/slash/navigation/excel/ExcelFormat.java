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
package slash.navigation.excel;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import slash.navigation.base.BaseNavigationFormat;
import slash.navigation.base.MultipleRoutesFormat;
import slash.navigation.base.ParserContext;
import slash.navigation.base.RouteCharacteristics;
import slash.navigation.common.NavigationPosition;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import static java.lang.String.format;
import static slash.common.io.Transfer.trim;
import static slash.navigation.excel.ColumnType.Coordinates;
import static slash.navigation.excel.ColumnType.Unsupported;

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
        return false;
    }

    @SuppressWarnings("unchecked")
    public <P extends NavigationPosition> ExcelRoute createRoute(RouteCharacteristics characteristics, String name, List<P> positions) {
        ExcelRoute route = new ExcelRoute(this, name, new ArrayList<>());
        List<ExcelPosition> excelPositions = (List<ExcelPosition>) positions;
        for (int i = 0; i < excelPositions.size(); i++)
            route.add(i, excelPositions.get(i));
        return route;
    }

    abstract Sheet createSheet(String name);

    void parseWorkbook(Workbook workbook, ParserContext<ExcelRoute> context) {
        ExcelWorkbook excelWorkbook = new ExcelWorkbook(workbook);
        for (int i = 0, c = workbook.getNumberOfSheets(); i < c; i++) {
            Sheet sheet = workbook.getSheetAt(i);
            parseSheet(sheet, excelWorkbook, context);
        }
    }

    private static final int HEADER_SEARCH_ROW_LIMIT = 10;

    private void parseSheet(Sheet sheet, ExcelWorkbook excelWorkbook, ParserContext<ExcelRoute> context) {
        if (sheet.getPhysicalNumberOfRows() < 2)
            return;

        int headerRowIndex = findHeaderRowIndex(sheet);
        Row header = sheet.getRow(headerRowIndex);
        log.info(format("Parsing sheet '%s' with %d rows and %d columns", sheet.getSheetName(), sheet.getPhysicalNumberOfRows(), header.getLastCellNum()));

        ColumnTypeToRowIndexMapping mapping = parseHeader(header);

        List<ExcelPosition> positions = new ArrayList<>();
        for (int i = headerRowIndex + 1, c = findLastDataRowIndex(sheet); i <= c; i++) {
            Row row = sheet.getRow(i);
            // in case there is just a formatting in a line, the row is null or has no cells
            if (row != null && row.getPhysicalNumberOfCells() > 0)
                positions.add(new ExcelPosition(row, mapping));
        }

        context.appendRoute(new ExcelRoute(this, sheet, mapping, positions, excelWorkbook));
    }

    // brings every listed route's sheet into the workbook of the route with the most routes (typically the
    // file the lists were read from): other sheets, column widths, formulas, merged cells and print setup
    // of the user's file survive since no new workbook is built. Used by write(List<ExcelRoute>, OutputStream)
    // of both MicrosoftExcel97Format and MicrosoftExcel2008Format before the resulting workbook is written.
    Workbook prepareWorkbook(List<ExcelRoute> routes) {
        ExcelWorkbook base = findBaseWorkbook(routes);

        // a list no longer in the model (e.g. deleted in the GUI) is moved out of the file being written,
        // into a workbook of its own; the route object stays complete, so undo followed by another save
        // brings it back
        for (ExcelRoute route : new ArrayList<>(base.getRoutes())) {
            if (!containsSame(routes, route))
                route.moveTo(new ExcelWorkbook(createWorkbookOfSameType(base.getWorkbook())), route.getName());
        }

        // a list read from another file, or split off into a route of its own, is moved into this workbook
        for (ExcelRoute route : routes) {
            if (route.getExcelWorkbook() != base)
                route.moveTo(base, route.getName());
        }

        // sheet order follows the list order; sheets that are not position lists (e.g. a notes sheet)
        // keep their index
        Workbook workbook = base.getWorkbook();
        List<Integer> positionListIndices = new ArrayList<>();
        for (ExcelRoute route : routes)
            positionListIndices.add(workbook.getSheetIndex(route.getName()));
        Collections.sort(positionListIndices);

        for (int i = 0; i < routes.size(); i++)
            workbook.setSheetOrder(routes.get(i).getName(), positionListIndices.get(i));

        return workbook;
    }

    private static ExcelWorkbook findBaseWorkbook(List<ExcelRoute> routes) {
        for (ExcelRoute route : routes) {
            if (route.getExcelWorkbook().getRoutes().size() > 1)
                return route.getExcelWorkbook();
        }
        return routes.get(0).getExcelWorkbook();
    }

    private static boolean containsSame(List<ExcelRoute> routes, ExcelRoute route) {
        for (ExcelRoute candidate : routes) {
            if (candidate == route)
                return true;
        }
        return false;
    }

    private static Workbook createWorkbookOfSameType(Workbook workbook) {
        return workbook instanceof HSSFWorkbook ? new HSSFWorkbook() : new XSSFWorkbook();
    }

    // the last row with cells; empty rows behind it are left over from editing and are not positions
    private int findLastDataRowIndex(Sheet sheet) {
        int index = sheet.getLastRowNum();
        while (index >= 0) {
            Row row = sheet.getRow(index);
            if (row != null && row.getPhysicalNumberOfCells() > 0)
                break;
            index--;
        }
        return index;
    }

    // finds the header row among the first rows, skipping an optional title row above it
    private int findHeaderRowIndex(Sheet sheet) {
        int limit = Math.min(HEADER_SEARCH_ROW_LIMIT, sheet.getPhysicalNumberOfRows());
        for (int i = 0; i < limit; i++) {
            Row row = sheet.getRow(i);
            if (row != null && countKnownColumns(row) >= 2)
                return i;
        }
        return 0;
    }

    private int countKnownColumns(Row row) {
        int count = 0;
        for (int i = 0, c = row.getLastCellNum(); i < c; i++) {
            Cell cell = row.getCell(i);
            if (cell == null || !cell.getCellType().equals(CellType.STRING))
                continue;
            ColumnType columnType = parseColumnType(cell.getStringCellValue());
            if (columnType == Coordinates)
                count += 2;
            else if (columnType != Unsupported)
                count++;
        }
        return count;
    }

    private ColumnTypeToRowIndexMapping parseHeader(Row row) {
        ColumnTypeToRowIndexMapping result = new ColumnTypeToRowIndexMapping(row.getRowNum());
        for (int i = 0, c = row.getLastCellNum(); i < c; i++) {
            Cell cell = row.getCell(i);
            // empty cell
            if(cell == null)
                continue;
            String cellValue = cell.getStringCellValue();
            ColumnType columnType = parseColumnType(cellValue);
            log.info(format("Column %d with name '%s' is identified as %s", i, cellValue, columnType));
            result.add(i, columnType);
        }
        return result;
    }

    private ColumnType parseColumnType(String value) {
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
