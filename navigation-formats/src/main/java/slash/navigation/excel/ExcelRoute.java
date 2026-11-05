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
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import slash.common.type.CompactCalendar;
import slash.navigation.base.*;
import slash.navigation.bcr.BcrFormat;
import slash.navigation.bcr.BcrPosition;
import slash.navigation.bcr.BcrRoute;
import slash.navigation.csv.CsvFormat;
import slash.navigation.csv.CsvPosition;
import slash.navigation.csv.CsvRoute;
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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

import static org.apache.poi.ss.usermodel.CellType.FORMULA;
import static slash.common.io.Transfer.toDouble;
import static slash.navigation.base.RouteCharacteristics.Track;
import static slash.navigation.base.RouteComments.createRouteName;
import static slash.navigation.excel.ColumnTypeToRowIndexMapping.DEFAULT;

/**
 * An Excel route.
 *
 * @author Christian Pesch
 */

public class ExcelRoute extends BaseRoute<ExcelPosition, ExcelFormat> {
    private final Sheet sheet;
    private ColumnTypeToRowIndexMapping mapping = DEFAULT;
    private final List<ExcelPosition> positions;

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

    private int getRowNum(int index) {
        return getPosition(index).getRow().getRowNum();
    }

    private void shiftRowToTheEnd(int rowNum) {
        sheet.shiftRows(rowNum, rowNum, sheet.getLastRowNum() - rowNum + 1);
    }

    private void shiftRowFromEndTo(int rowNum) {
        int lastRowNum = sheet.getLastRowNum();
        sheet.shiftRows(lastRowNum, lastRowNum, rowNum - lastRowNum);
        removeEmptyRow(lastRowNum);
    }

    // HSSF keeps the source of a shift as an empty row, which would be written and read back as an empty position
    private void removeEmptyRow(int rowNum) {
        Row row = sheet.getRow(rowNum);
        if (row != null && row.getPhysicalNumberOfCells() == 0)
            sheet.removeRow(row);
    }

    // HSSF moves the cells of shifted rows between Row objects that stay at their row number and XSSF
    // replaces the Row objects of the sheet: bind the positions to the rows again, in the order of the sheet
    private void bindPositionsToRows() {
        int index = 0;
        for (int rowNum = mapping.getHeaderRowIndex() + 1, lastRowNum = sheet.getLastRowNum();
             rowNum <= lastRowNum && index < positions.size(); rowNum++) {
            Row row = sheet.getRow(rowNum);
            // in case there is just a formatting in a line, the row is null and no position
            if (row != null)
                positions.get(index++).setRow(row);
        }
    }

    public void top(int index, int topOffset) {
        if (index > topOffset) {
            int rowForIndex = getRowNum(index);
            int rowForTopOffset = getRowNum(topOffset);

            // shift index row to the end
            shiftRowToTheEnd(rowForIndex);

            // move all rows between topOffset and index one down
            sheet.shiftRows(rowForTopOffset, rowForIndex - 1, 1);

            // move index row from the end to topOffset row
            shiftRowFromEndTo(rowForTopOffset);
        }

        super.top(index, topOffset);
        bindPositionsToRows();
    }

    public void bottom(int index, int bottomOffset) {
        int bottomIndex = getPositionCount() - 1 - bottomOffset;
        if (index < bottomIndex) {
            int rowForIndex = getRowNum(index);
            int rowForBottomIndex = getRowNum(bottomIndex);

            // shift index row to the end
            shiftRowToTheEnd(rowForIndex);

            // move all rows between index and bottomIndex one up
            sheet.shiftRows(rowForIndex + 1, rowForBottomIndex, -1);

            // move index row from the end to bottomIndex row
            shiftRowFromEndTo(rowForBottomIndex);
        }

        super.bottom(index, bottomOffset);
        bindPositionsToRows();
    }

    public void move(int firstIndex, int secondIndex) {
        if (firstIndex != secondIndex) {
            int rowForFirstIndex = getRowNum(firstIndex);
            int rowForSecondIndex = getRowNum(secondIndex);

            // shift secondIndex row to the end
            shiftRowToTheEnd(rowForSecondIndex);

            // move firstIndex row to secondIndex row
            sheet.shiftRows(rowForFirstIndex, rowForFirstIndex, rowForSecondIndex - rowForFirstIndex);

            // move secondIndex row from the end to firstIndex row
            shiftRowFromEndTo(rowForFirstIndex);
        }

        super.move(firstIndex, secondIndex);
        bindPositionsToRows();
    }

    public void add(int index, ExcelPosition position) {
        // a position from another sheet, e.g. one removed before and added again by undo, gets a row of its own
        if (position.getRow().getSheet() != sheet)
            attach(position);

        // shift all rows from index one position down
        int sourceRowNum = position.getRow().getRowNum();
        int rowForIndex = index < getPositionCount() ? getRowNum(index) : sourceRowNum;
        sheet.shiftRows(rowForIndex, sheet.getLastRowNum(), 1);

        // shift row to add to the desired position
        int shiftedSourceRowNum = sourceRowNum + 1;
        sheet.shiftRows(shiftedSourceRowNum, shiftedSourceRowNum, rowForIndex - shiftedSourceRowNum);
        removeEmptyRow(shiftedSourceRowNum);

        positions.add(index, position);
        bindPositionsToRows();
    }

    // bindPositionsToRows() keeps position identity (undo and the GUI hold position objects): detach every
    // row, which copies its data elsewhere and lets the sheet row be overwritten, then re-attach the rows
    // to the sheet in the order of the given list
    public void order(List<ExcelPosition> positions) {
        List<ExcelPosition> existing = new ArrayList<>(getPositions());
        for (ExcelPosition position : existing)
            detach(position);
        removeDataRows();

        super.order(positions);

        for (ExcelPosition position : getPositions())
            attach(position);
        bindPositionsToRows();
    }

    private void removeDataRows() {
        for (int rowNum = sheet.getLastRowNum(); rowNum > mapping.getHeaderRowIndex(); rowNum--) {
            Row row = sheet.getRow(rowNum);
            if (row != null)
                sheet.removeRow(row);
        }
    }

    // revert(int[]) doesn't go through order() in BaseRoute; build the same reordering it would
    // produce and run it through order() so the sheet rows are kept in sync, too
    public void revert(int[] indices) {
        int[] sorted = indices.clone();
        Arrays.sort(sorted);

        List<ExcelPosition> existing = getPositions();
        List<ExcelPosition> reverted = new ArrayList<>();
        for (int row : sorted)
            reverted.add(0, existing.get(row));

        List<ExcelPosition> result = new ArrayList<>(existing);
        int index = 0;
        for (int row : sorted)
            result.set(row, reverted.get(index++));

        order(result);
    }

    // removeDuplicates() in BaseRoute shrinks the position list directly; route the removal through
    // remove(index) so the sheet row is removed, too
    public void removeDuplicates() {
        List<ExcelPosition> positions = getPositions();
        ExcelPosition previous = null;
        int index = 0;
        while (index < positions.size()) {
            ExcelPosition next = positions.get(index);
            if (previous != null && (!next.hasCoordinates() || toDouble(next.calculateDistance(previous)) <= 0.0)) {
                remove(index);
            } else
                index++;
            previous = next;
        }
    }

    public ExcelPosition remove(int index) {
        return remove(index, index + 1).get(0);
    }

    public List<ExcelPosition> remove(int fromIndex, int toIndex) {
        if (fromIndex >= toIndex)
            return super.remove(fromIndex, toIndex);

        int firstRowNum = getRowNum(fromIndex);
        int lastRemovedRowNum = getRowNum(toIndex - 1);
        int count = lastRemovedRowNum - firstRowNum + 1;

        // the shifts below overwrite the rows, keep their data for undo
        for (int i = fromIndex; i < toIndex; i++)
            detach(getPosition(i));

        // shift all rows behind the removed rows forward
        int lastRowNum = sheet.getLastRowNum();
        if (lastRemovedRowNum < lastRowNum)
            sheet.shiftRows(lastRemovedRowNum + 1, lastRowNum, -count);

        // remove the rows that are left at the end
        for (int rowNum = lastRowNum - count + 1; rowNum <= lastRowNum; rowNum++) {
            Row row = sheet.getRow(rowNum);
            if (row != null)
                sheet.removeRow(row);
        }

        List<ExcelPosition> removed = super.remove(fromIndex, toIndex);
        bindPositionsToRows();
        return removed;
    }

    // a sheet outside of the workbook that holds the rows of removed positions
    private Sheet detachedSheet;
    private int detachedRowNum;
    private final Map<StyleKey, CellStyle> detachedStyles = new HashMap<>();
    private final Map<StyleKey, CellStyle> attachedStyles = new HashMap<>();

    // a style of a workbook; POI returns a new style object for each cell
    private record StyleKey(Workbook workbook, short index) {
    }

    private Row createDetachedRow() {
        Workbook workbook = sheet.getWorkbook();
        if (detachedSheet == null || detachedRowNum > workbook.getSpreadsheetVersion().getLastRowIndex()) {
            Workbook detachedWorkbook = workbook instanceof HSSFWorkbook ? new HSSFWorkbook() : new XSSFWorkbook();
            detachedSheet = detachedWorkbook.createSheet();
            detachedRowNum = 0;
            detachedStyles.clear();
            attachedStyles.clear();
        }
        return detachedSheet.createRow(detachedRowNum++);
    }

    private void detach(ExcelPosition position) {
        Row row = createDetachedRow();
        copyCells(position.getRow(), row, detachedStyles);
        position.setRow(row);
    }

    private void attach(ExcelPosition position) {
        Row row = sheet.createRow(sheet.getLastRowNum() + 1);
        if (position.getMapping() == mapping) {
            copyCells(position.getRow(), row, attachedStyles);
            position.setRow(row);
        } else {
            // the columns of a position from another route differ, transfer the values it has
            ExcelPosition source = new ExcelPosition(position.getRow(), position.getMapping());
            position.setRow(row);
            position.setMapping(mapping);
            transfer(source.getLongitude(), position::setLongitude);
            transfer(source.getLatitude(), position::setLatitude);
            transfer(source.getElevation(), position::setElevation);
            transfer(source.getSpeed(), position::setSpeed);
            transfer(source.getTime(), position::setTime);
            transfer(source.getDescription(), position::setDescription);
            transfer(source.getHeading(), position::setHeading);
            transfer(source.getHeartBeat(), position::setHeartBeat);
            transfer(source.getPressure(), position::setPressure);
            transfer(source.getTemperature(), position::setTemperature);
        }
    }

    private static <T> void transfer(T value, Consumer<T> setter) {
        if (value != null)
            setter.accept(value);
    }

    private static void copyCells(Row source, Row target, Map<StyleKey, CellStyle> styles) {
        Workbook workbook = target.getSheet().getWorkbook();
        for (Cell cell : source) {
            Cell copy = target.createCell(cell.getColumnIndex());
            CellType cellType = cell.getCellType();
            if (cellType == FORMULA)
                // a formula may refer to cells of the sheet that it is copied away from
                cellType = cell.getCachedFormulaResultType();
            switch (cellType) {
                case NUMERIC -> copy.setCellValue(cell.getNumericCellValue());
                case STRING -> copy.setCellValue(cell.getStringCellValue());
                case BOOLEAN -> copy.setCellValue(cell.getBooleanCellValue());
                case ERROR -> copy.setCellErrorValue(cell.getErrorCellValue());
                default -> copy.setBlank();
            }

            // keep the date format of time cells; share styles since the number of styles of a workbook is limited
            CellStyle style = cell.getCellStyle();
            if (style.getIndex() != 0) {
                copy.setCellStyle(styles.computeIfAbsent(new StyleKey(source.getSheet().getWorkbook(), style.getIndex()), key -> {
                    CellStyle clone = workbook.createCellStyle();
                    if (clone.getClass().equals(style.getClass()))
                        clone.cloneStyleFrom(style);
                    else
                        clone.setDataFormat(workbook.createDataFormat().getFormat(style.getDataFormatString()));
                    return clone;
                }));
            }
        }
    }

    public ExcelPosition createPosition(Double longitude, Double latitude, Double elevation, Double speed, CompactCalendar time, String description) {
        ExcelPosition position = new ExcelPosition(sheet.createRow(sheet.getLastRowNum() + 1), mapping);
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

    protected CsvRoute asCsvFormat(CsvFormat format) {
        List<CsvPosition> positions = new ArrayList<>();
        for (ExcelPosition position : getPositions()) {
            positions.add(position.asCsvPosition());
        }
        return new CsvRoute(format, getName(), positions);
    }

    protected ExcelRoute asExcelFormat(ExcelFormat format) {
        List<ExcelPosition> excelPositions = new ArrayList<>(getPositions());
        return new ExcelRoute(format, getName(), excelPositions);
    }

    protected GoPalRoute asGoPalRouteFormat(GoPalRouteFormat<GoPalRoute> format) {
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

    protected SimpleRoute<?, ?> asPhotoFormat(PhotoFormat format) {
        List<Wgs84Position> wgs84Positions = new ArrayList<>();
        for (ExcelPosition position : getPositions()) {
            wgs84Positions.add(position.asWgs84Position());
        }
        return new Wgs84Route(format, getCharacteristics(), getName(), wgs84Positions);
    }

    protected SimpleRoute<?, ?> asSimpleFormat(SimpleFormat<?> format) {
        List<Wgs84Position> positions = new ArrayList<>();
        for (ExcelPosition position : getPositions()) {
            positions.add(position.asWgs84Position());
        }
        return new Wgs84Route(format, getCharacteristics(), getName(), positions);
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
                Objects.equals(mapping, that.mapping) &&
                Objects.equals(positions, that.positions);
    }

    public int hashCode() {
        int result = getName() != null ? getName().hashCode() : 0;
        result = 31 * result + (mapping != null ? mapping.hashCode() : 0);
        result = 31 * result + (positions != null ? positions.hashCode() : 0);
        return result;
    }
}
