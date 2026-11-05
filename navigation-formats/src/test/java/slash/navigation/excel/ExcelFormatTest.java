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
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.junit.Test;
import slash.navigation.base.ParserContextImpl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
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
        Workbook workbook = new HSSFWorkbook();
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
}
