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
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.Test;
import slash.common.type.CompactCalendar;
import slash.navigation.base.ParserContextImpl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;
import static slash.common.TestCase.calendar;

/**
 * Tests the header search below a title row and the combined coordinates column of {@link ExcelFormat}.
 *
 * @author Christian Pesch
 */
public class ExcelFormatTest {

    private static Workbook createWorkbookWithTitleRow() {
        Workbook workbook = new HSSFWorkbook();
        Sheet sheet = workbook.createSheet("Data");

        Row title = sheet.createRow(0);
        title.createCell(0).setCellValue("Streckendetails:Tracker-1 2026-10-11 00:00:00 - 2026-10-13 23:59:59");

        Row header = sheet.createRow(1);
        header.createCell(0).setCellValue("Positionszeit");
        header.createCell(1).setCellValue("Speed");
        header.createCell(2).setCellValue("Koordinaten");
        // trailing column that stays empty in the data rows
        header.createCell(3).setCellValue("Die Adresse");

        Row first = sheet.createRow(2);
        first.createCell(0).setCellValue("2026-10-12 17:24:18");
        first.createCell(1).setCellValue(0.0);
        first.createCell(2).setCellValue("52.5000,13.4000");

        Row second = sheet.createRow(3);
        second.createCell(0).setCellValue("2026-10-12 17:28:22");
        second.createCell(1).setCellValue(7.0);
        second.createCell(2).setCellValue("52.5002,13.4003");

        Row third = sheet.createRow(4);
        third.createCell(0).setCellValue("2026-10-12 17:28:32");
        third.createCell(1).setCellValue(9.0);
        third.createCell(2).setCellValue("52.5004,13.4006");
        return workbook;
    }

    @Test
    public void findsHeaderBelowATitleRowAndReadsCombinedCoordinates() {
        Workbook workbook = createWorkbookWithTitleRow();

        ParserContextImpl<ExcelRoute> context = new ParserContextImpl<>();
        new MicrosoftExcel97Format().parseWorkbook(workbook, context);

        assertEquals(1, context.getRoutes().size());
        List<ExcelPosition> positions = context.getRoutes().get(0).getPositions();
        assertEquals(3, positions.size());

        ExcelPosition firstPosition = positions.get(0);
        assertEquals(52.5, firstPosition.getLatitude(), 0.0);
        assertEquals(13.4, firstPosition.getLongitude(), 0.0);
        assertEquals(calendar(2026, 10, 12, 17, 24, 18), firstPosition.getTime());
        assertEquals(0.0, firstPosition.getSpeed(), 0.0);

        ExcelPosition secondPosition = positions.get(1);
        assertEquals(52.5002, secondPosition.getLatitude(), 0.0);
        assertEquals(13.4003, secondPosition.getLongitude(), 0.0);
        assertEquals(calendar(2026, 10, 12, 17, 28, 22), secondPosition.getTime());
        assertEquals(7.0, secondPosition.getSpeed(), 0.0);
    }

    private static Workbook createWorkbookWithEnglishHeaders() {
        Workbook workbook = new HSSFWorkbook();
        Sheet sheet = workbook.createSheet("Data");

        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("Position time");
        header.createCell(1).setCellValue("Speed");
        header.createCell(2).setCellValue("Coordinates");

        Row first = sheet.createRow(1);
        first.createCell(0).setCellValue("2026-10-12 17:24:18");
        first.createCell(1).setCellValue(0.0);
        first.createCell(2).setCellValue("52.5000,13.4000");
        return workbook;
    }

    @Test
    public void readsEnglishCoordinatesAndPositionTimeHeaders() {
        Workbook workbook = createWorkbookWithEnglishHeaders();

        ParserContextImpl<ExcelRoute> context = new ParserContextImpl<>();
        new MicrosoftExcel97Format().parseWorkbook(workbook, context);

        assertEquals(1, context.getRoutes().size());
        List<ExcelPosition> positions = context.getRoutes().get(0).getPositions();
        assertEquals(1, positions.size());

        ExcelPosition position = positions.get(0);
        assertEquals(52.5, position.getLatitude(), 0.0);
        assertEquals(13.4, position.getLongitude(), 0.0);
        assertEquals(calendar(2026, 10, 12, 17, 24, 18), position.getTime());
    }

    @Test
    public void settingAMissingColumnAddsItToTheHeaderBelowTheTitleRow() throws IOException {
        MicrosoftExcel97Format format = new MicrosoftExcel97Format();
        ParserContextImpl<ExcelRoute> context = new ParserContextImpl<>();
        format.parseWorkbook(createWorkbookWithTitleRow(), context);
        ExcelRoute route = context.getRoutes().get(0);

        route.getPositions().get(0).setLatitude(10.0);

        Sheet sheet = route.getWorkbook().getSheetAt(0);
        assertEquals(1, sheet.getRow(0).getLastCellNum());
        assertEquals("Die Adresse", sheet.getRow(1).getCell(3).getStringCellValue());
        assertEquals("Latitude", sheet.getRow(1).getCell(4).getStringCellValue());

        ByteArrayOutputStream target = new ByteArrayOutputStream();
        format.write(route, target, 0, route.getPositionCount());
        ParserContextImpl<ExcelRoute> reread = new ParserContextImpl<>();
        format.read(new ByteArrayInputStream(target.toByteArray()), reread);

        List<ExcelPosition> positions = reread.getRoutes().get(0).getPositions();
        assertEquals(10.0, positions.get(0).getLatitude(), 0.0);
        assertEquals(13.4, positions.get(0).getLongitude(), 0.0);
        assertEquals(52.5002, positions.get(1).getLatitude(), 0.0);
    }

    @Test
    public void movingAPositionToTheTopKeepsTitleAndHeaderRows() throws IOException {
        MicrosoftExcel97Format format = new MicrosoftExcel97Format();
        ParserContextImpl<ExcelRoute> context = new ParserContextImpl<>();
        format.parseWorkbook(createWorkbookWithTitleRow(), context);
        ExcelRoute route = context.getRoutes().get(0);

        route.top(2, 0);

        Sheet sheet = route.getWorkbook().getSheetAt(0);
        assertTrue(sheet.getRow(0).getCell(0).getStringCellValue().startsWith("Streckendetails:"));
        assertEquals("Positionszeit", sheet.getRow(1).getCell(0).getStringCellValue());
        assertEquals("52.5004,13.4006", sheet.getRow(2).getCell(2).getStringCellValue());
        assertEquals("52.5000,13.4000", sheet.getRow(3).getCell(2).getStringCellValue());
        assertEquals("52.5002,13.4003", sheet.getRow(4).getCell(2).getStringCellValue());

        ByteArrayOutputStream target = new ByteArrayOutputStream();
        format.write(route, target, 0, route.getPositionCount());
        ParserContextImpl<ExcelRoute> reread = new ParserContextImpl<>();
        format.read(new ByteArrayInputStream(target.toByteArray()), reread);

        List<ExcelPosition> positions = reread.getRoutes().get(0).getPositions();
        assertEquals(52.5004, positions.get(0).getLatitude(), 0.0);
        assertEquals(52.5, positions.get(1).getLatitude(), 0.0);
        assertEquals(52.5002, positions.get(2).getLatitude(), 0.0);
    }

    private static final double[] LATITUDES = {52.5, 52.5002, 52.5004};
    private static final double[] LONGITUDES = {13.4, 13.4003, 13.4006};

    private static Workbook createWorkbookWithoutTitleRow() {
        return createWorkbookWithoutTitleRow(new HSSFWorkbook());
    }

    private interface RouteOperation {
        void apply(ExcelRoute route);
    }

    private static void assertLatitudesAfter(Workbook workbook, RouteOperation operation, double... expectedLatitudes) throws IOException {
        MicrosoftExcel97Format format = new MicrosoftExcel97Format();
        ParserContextImpl<ExcelRoute> context = new ParserContextImpl<>();
        format.parseWorkbook(workbook, context);
        ExcelRoute route = context.getRoutes().get(0);

        operation.apply(route);

        Sheet sheet = route.getWorkbook().getSheetAt(0);
        // no empty row left over from shifting rows
        for (Row row : sheet)
            assertTrue("empty row " + row.getRowNum(), row.getPhysicalNumberOfCells() > 0);

        ByteArrayOutputStream target = new ByteArrayOutputStream();
        format.write(route, target, 0, route.getPositionCount());
        ParserContextImpl<ExcelRoute> reread = new ParserContextImpl<>();
        format.read(new ByteArrayInputStream(target.toByteArray()), reread);

        List<ExcelPosition> positions = reread.getRoutes().get(0).getPositions();
        assertEquals(expectedLatitudes.length, positions.size());
        for (int i = 0; i < expectedLatitudes.length; i++)
            assertEquals(expectedLatitudes[i], positions.get(i).getLatitude(), 0.0);
    }

    private static void addPosition(ExcelRoute route, int index) {
        route.add(index, route.createPosition(13.5, 52.6, null, null, null, null));
    }

    @Test
    public void ignoresEmptyRowsBehindTheLastPosition() {
        Workbook workbook = createWorkbookWithoutTitleRow();
        workbook.getSheetAt(0).createRow(4);
        workbook.getSheetAt(0).createRow(5);

        ParserContextImpl<ExcelRoute> context = new ParserContextImpl<>();
        new MicrosoftExcel97Format().parseWorkbook(workbook, context);

        List<ExcelPosition> positions = context.getRoutes().get(0).getPositions();
        assertEquals(3, positions.size());
        assertEquals(52.5004, positions.get(2).getLatitude(), 0.0);
    }

    @Test
    public void readsPositionsBehindAGapRow() {
        Workbook workbook = createWorkbookWithoutTitleRow();
        Row row = workbook.getSheetAt(0).createRow(5);
        row.createCell(0).setCellValue(52.5006);
        row.createCell(1).setCellValue(13.4009);

        ParserContextImpl<ExcelRoute> context = new ParserContextImpl<>();
        new MicrosoftExcel97Format().parseWorkbook(workbook, context);

        List<ExcelPosition> positions = context.getRoutes().get(0).getPositions();
        assertEquals(4, positions.size());
        assertEquals(52.5006, positions.get(3).getLatitude(), 0.0);
    }

    @Test
    public void topLeavesNoEmptyRowWithoutTitleRow() throws IOException {
        assertLatitudesAfter(createWorkbookWithoutTitleRow(), route -> route.top(2, 0), 52.5004, 52.5, 52.5002);
    }

    @Test
    public void bottomLeavesNoEmptyRowWithoutTitleRow() throws IOException {
        assertLatitudesAfter(createWorkbookWithoutTitleRow(), route -> route.bottom(0, 0), 52.5002, 52.5004, 52.5);
    }

    @Test
    public void moveLeavesNoEmptyRowWithoutTitleRow() throws IOException {
        assertLatitudesAfter(createWorkbookWithoutTitleRow(), route -> route.move(0, 1), 52.5002, 52.5, 52.5004);
    }

    @Test
    public void removeLeavesNoEmptyRowWithoutTitleRow() throws IOException {
        assertLatitudesAfter(createWorkbookWithoutTitleRow(), route -> route.remove(1), 52.5, 52.5004);
    }

    @Test
    public void addLeavesNoEmptyRowWithoutTitleRow() throws IOException {
        assertLatitudesAfter(createWorkbookWithoutTitleRow(), route -> addPosition(route, 1), 52.5, 52.6, 52.5002, 52.5004);
    }

    @Test
    public void topLeavesNoEmptyRowWithTitleRow() throws IOException {
        assertLatitudesAfter(createWorkbookWithTitleRow(), route -> route.top(2, 0), 52.5004, 52.5, 52.5002);
    }

    @Test
    public void bottomLeavesNoEmptyRowWithTitleRow() throws IOException {
        assertLatitudesAfter(createWorkbookWithTitleRow(), route -> route.bottom(0, 0), 52.5002, 52.5004, 52.5);
    }

    @Test
    public void moveLeavesNoEmptyRowWithTitleRow() throws IOException {
        assertLatitudesAfter(createWorkbookWithTitleRow(), route -> route.move(0, 1), 52.5002, 52.5, 52.5004);
    }

    @Test
    public void removeLeavesNoEmptyRowWithTitleRow() throws IOException {
        assertLatitudesAfter(createWorkbookWithTitleRow(), route -> route.remove(1), 52.5, 52.5004);
    }

    @Test
    public void addLeavesNoEmptyRowWithTitleRow() throws IOException {
        assertLatitudesAfter(createWorkbookWithTitleRow(), route -> addPosition(route, 1), 52.5, 52.6, 52.5002, 52.5004);
    }

    // in-memory positions must show the same order as the sheet that is written

    private static Workbook createWorkbookWithoutTitleRow(Workbook workbook) {
        Sheet sheet = workbook.createSheet("Data");

        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("Latitude");
        header.createCell(1).setCellValue("Longitude");

        for (int i = 0; i < LATITUDES.length; i++) {
            Row row = sheet.createRow(i + 1);
            row.createCell(0).setCellValue(LATITUDES[i]);
            row.createCell(1).setCellValue(LONGITUDES[i]);
        }
        return workbook;
    }

    private static void assertLatitudes(List<ExcelPosition> positions, double... expectedLatitudes) {
        assertEquals(expectedLatitudes.length, positions.size());
        for (int i = 0; i < expectedLatitudes.length; i++)
            assertEquals("latitude " + i, expectedLatitudes[i], positions.get(i).getLatitude(), 0.0);
    }

    private static void assertInMemoryAndRereadLatitudesAfter(ExcelFormat format, Workbook workbook, RouteOperation operation,
                                                              double... expectedLatitudes) throws IOException {
        ParserContextImpl<ExcelRoute> context = new ParserContextImpl<>();
        format.parseWorkbook(workbook, context);
        ExcelRoute route = context.getRoutes().get(0);

        operation.apply(route);

        assertLatitudes(route.getPositions(), expectedLatitudes);

        ByteArrayOutputStream target = new ByteArrayOutputStream();
        format.write(route, target, 0, route.getPositionCount());
        ParserContextImpl<ExcelRoute> reread = new ParserContextImpl<>();
        format.read(new ByteArrayInputStream(target.toByteArray()), reread);

        assertLatitudes(reread.getRoutes().get(0).getPositions(), expectedLatitudes);
    }

    private static void assertXlsAndXlsx(RouteOperation operation, double... expectedLatitudes) throws IOException {
        assertInMemoryAndRereadLatitudesAfter(new MicrosoftExcel97Format(), createWorkbookWithoutTitleRow(new HSSFWorkbook()), operation, expectedLatitudes);
        assertInMemoryAndRereadLatitudesAfter(new MicrosoftExcel2008Format(), createWorkbookWithoutTitleRow(new XSSFWorkbook()), operation, expectedLatitudes);
    }

    // a formatting-only line between data rows: the row exists but has no cells, unlike a row that was never created
    private static Workbook createWorkbookWithoutTitleRowAndGapRow(Workbook workbook) {
        Sheet sheet = workbook.createSheet("Data");

        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("Latitude");
        header.createCell(1).setCellValue("Longitude");

        Row first = sheet.createRow(1);
        first.createCell(0).setCellValue(LATITUDES[0]);
        first.createCell(1).setCellValue(LONGITUDES[0]);

        sheet.createRow(2);

        Row second = sheet.createRow(3);
        second.createCell(0).setCellValue(LATITUDES[1]);
        second.createCell(1).setCellValue(LONGITUDES[1]);

        Row third = sheet.createRow(4);
        third.createCell(0).setCellValue(LATITUDES[2]);
        third.createCell(1).setCellValue(LONGITUDES[2]);
        return workbook;
    }

    private static void assertXlsAndXlsxWithGapRow(RouteOperation operation, double... expectedLatitudes) throws IOException {
        assertInMemoryAndRereadLatitudesAfter(new MicrosoftExcel97Format(), createWorkbookWithoutTitleRowAndGapRow(new HSSFWorkbook()), operation, expectedLatitudes);
        assertInMemoryAndRereadLatitudesAfter(new MicrosoftExcel2008Format(), createWorkbookWithoutTitleRowAndGapRow(new XSSFWorkbook()), operation, expectedLatitudes);
    }

    @Test
    public void parsingSkipsAGapRowBetweenDataRows() {
        Workbook workbook = createWorkbookWithoutTitleRowAndGapRow(new HSSFWorkbook());
        ParserContextImpl<ExcelRoute> context = new ParserContextImpl<>();
        new MicrosoftExcel97Format().parseWorkbook(workbook, context);

        assertLatitudes(context.getRoutes().get(0).getPositions(), LATITUDES);
    }

    @Test
    public void topKeepsInMemoryOrderInSyncWithSheetWithGapRow() throws IOException {
        assertXlsAndXlsxWithGapRow(route -> route.top(2, 0), LATITUDES[2], LATITUDES[0], LATITUDES[1]);
    }

    @Test
    public void bottomKeepsInMemoryOrderInSyncWithSheetWithGapRow() throws IOException {
        assertXlsAndXlsxWithGapRow(route -> route.bottom(0, 0), LATITUDES[1], LATITUDES[2], LATITUDES[0]);
    }

    @Test
    public void moveKeepsInMemoryOrderInSyncWithSheetWithGapRow() throws IOException {
        assertXlsAndXlsxWithGapRow(route -> route.move(0, 1), LATITUDES[1], LATITUDES[0], LATITUDES[2]);
    }

    @Test
    public void removeKeepsInMemoryOrderInSyncWithSheetWithGapRow() throws IOException {
        assertXlsAndXlsxWithGapRow(route -> route.remove(1), LATITUDES[0], LATITUDES[2]);
    }

    @Test
    public void addKeepsInMemoryOrderInSyncWithSheetWithGapRow() throws IOException {
        assertXlsAndXlsxWithGapRow(route -> addPosition(route, 1), LATITUDES[0], 52.6, LATITUDES[1], LATITUDES[2]);
    }

    @Test
    public void topKeepsInMemoryOrderInSyncWithSheet() throws IOException {
        assertXlsAndXlsx(route -> route.top(2, 0), 52.5004, 52.5, 52.5002);
    }

    @Test
    public void bottomKeepsInMemoryOrderInSyncWithSheet() throws IOException {
        assertXlsAndXlsx(route -> route.bottom(0, 0), 52.5002, 52.5004, 52.5);
    }

    @Test
    public void moveKeepsInMemoryOrderInSyncWithSheet() throws IOException {
        assertXlsAndXlsx(route -> route.move(0, 1), 52.5002, 52.5, 52.5004);
    }

    // move() swaps the two given positions, like BaseRoute#move(), instead of shifting the
    // positions between them: the row at index 1 stays where it is
    @Test
    public void moveWithGapKeepsInMemoryOrderInSyncWithSheet() throws IOException {
        assertXlsAndXlsx(route -> route.move(0, 2), 52.5004, 52.5002, 52.5);
    }

    @Test
    public void topWithIndexEqualToOffsetIsANoOp() throws IOException {
        assertXlsAndXlsx(route -> route.top(0, 0), 52.5, 52.5002, 52.5004);
    }

    @Test
    public void bottomWithIndexEqualToOffsetIsANoOp() throws IOException {
        assertXlsAndXlsx(route -> route.bottom(2, 0), 52.5, 52.5002, 52.5004);
    }

    @Test
    public void moveWithEqualIndicesIsANoOp() throws IOException {
        assertXlsAndXlsx(route -> route.move(1, 1), 52.5, 52.5002, 52.5004);
    }

    @Test
    public void removeKeepsInMemoryOrderInSyncWithSheet() throws IOException {
        assertXlsAndXlsx(route -> route.remove(1), 52.5, 52.5004);
    }

    @Test
    public void removeRangeKeepsInMemoryOrderInSyncWithSheet() throws IOException {
        assertXlsAndXlsx(route -> route.remove(0, 2), 52.5004);
        assertXlsAndXlsx(route -> route.remove(1, 3), 52.5);
    }

    @Test
    public void addKeepsInMemoryOrderInSyncWithSheet() throws IOException {
        assertXlsAndXlsx(route -> addPosition(route, 1), 52.5, 52.6, 52.5002, 52.5004);
    }

    @Test
    public void subsequentOperationsKeepInMemoryOrderInSyncWithSheet() throws IOException {
        assertXlsAndXlsx(route -> {
            route.top(2, 0);
            route.move(1, 2);
            route.bottom(0, 0);
        }, 52.5002, 52.5, 52.5004);
        assertXlsAndXlsx(route -> {
            route.bottom(0, 0);
            route.remove(0);
            addPosition(route, 0);
        }, 52.6, 52.5004, 52.5);
    }

    @Test
    public void movedPositionKeepsItsData() throws IOException {
        assertXlsAndXlsx(route -> {
            ExcelPosition last = route.getPosition(2);
            route.top(2, 0);
            assertSame(last, route.getPosition(0));
            assertEquals(52.5004, last.getLatitude(), 0.0);

            last.setLatitude(53.0);
            route.bottom(0, 0);
            assertSame(last, route.getPosition(2));
        }, 52.5, 52.5002, 53.0);
    }

    @Test
    public void removedPositionsKeepTheirDataAndCanBeAddedAgain() throws IOException {
        assertXlsAndXlsx(route -> {
            List<ExcelPosition> removed = route.remove(0, 2);
            assertLatitudes(removed, 52.5, 52.5002);
            assertLatitudes(route.getPositions(), 52.5004);

            // undo adds the removed positions again, from the last to the first
            for (int i = removed.size() - 1; i >= 0; i--)
                route.add(0, removed.get(i));
            assertSame(removed.get(0), route.getPosition(0));
            assertSame(removed.get(1), route.getPosition(1));
        }, 52.5, 52.5002, 52.5004);
    }

    @Test
    public void removedPositionKeepsItsTime() throws IOException {
        CompactCalendar time = calendar(2026, 11, 5, 9, 32, 14);
        assertXlsAndXlsx(route -> {
            route.add(1, route.createPosition(13.5, 52.6, null, null, time, null));
            ExcelPosition removed = route.remove(1);
            route.add(1, removed);
            assertEquals(time, route.getPosition(1).getTime());
        }, 52.5, 52.6, 52.5002, 52.5004);
    }

    @Test
    public void addsAPositionFromAnotherWorkbook() throws IOException {
        assertXlsAndXlsx(route -> route.add(1, new ExcelPosition(13.5, 52.7, null, null, null, "X")), 52.5, 52.7, 52.5002, 52.5004);
    }

    // both routes share the DEFAULT mapping instance, so attach() copies cells instead of transferring fields;
    // one route is HSSF-backed and the other XSSF-backed, forcing copyCells' cell style class mismatch branch
    @Test
    public void attachClonesTimeCellStyleAcrossWorkbookTypes() {
        CompactCalendar time = calendar(2026, 11, 5, 9, 32, 14);

        ExcelRoute hssfRoute = new ExcelRoute(new MicrosoftExcel97Format(), "Data", new ArrayList<>());
        ExcelPosition position = hssfRoute.createPosition(13.5, 52.7, null, null, time, "X");

        ExcelRoute xssfRoute = new ExcelRoute(new MicrosoftExcel2008Format(), "Data", new ArrayList<>());
        xssfRoute.add(0, position);

        ExcelPosition attached = xssfRoute.getPosition(0);
        assertEquals(time, attached.getTime());

        Cell timeCell = attached.getRow().getCell(ColumnTypeToRowIndexMapping.DEFAULT.getIndex(ColumnType.Time));
        assertNotEquals(0, timeCell.getCellStyle().getIndex());
        assertEquals("m/d/yy h:mm", timeCell.getCellStyle().getDataFormatString());
    }

    // the target route's mapping only has Latitude/Longitude columns, so position.getMapping() != mapping and
    // attach() takes the field-transfer branch instead of copyCells; assert every transferable field survives it
    @Test
    public void attachTransfersAllSensorFieldsWhenMappingDiffers() throws IOException {
        CompactCalendar time = calendar(2026, 11, 5, 9, 32, 14);
        assertXlsAndXlsx(route -> {
            // Heartbeat is deliberately left out: it has no entry in ColumnTypeToRowIndexMapping.DEFAULT, so
            // setting it on a DEFAULT-mapped position would add a column to that shared static mapping instance
            ExcelPosition position = new ExcelPosition(13.5, 52.7, 123.0, 45.0, time, "X");
            position.setHeading(90.0);
            position.setPressure(1013.0);
            position.setTemperature(21.5);

            route.add(1, position);
            ExcelPosition added = route.getPosition(1);

            assertEquals(13.5, added.getLongitude(), 0.0);
            assertEquals(52.7, added.getLatitude(), 0.0);
            assertEquals(123.0, added.getElevation(), 0.0);
            assertEquals(45.0, added.getSpeed(), 0.0);
            assertEquals(time, added.getTime());
            assertEquals("X", added.getDescription());
            assertEquals(90.0, added.getHeading(), 0.0);
            assertEquals(1013.0, added.getPressure(), 0.0);
            assertEquals(21.5, added.getTemperature(), 0.0);
        }, 52.5, 52.7, 52.5002, 52.5004);
    }
}
