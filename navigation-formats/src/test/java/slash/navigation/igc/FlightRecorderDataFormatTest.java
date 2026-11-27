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

package slash.navigation.igc;

import org.junit.jupiter.api.Test;
import slash.navigation.base.NavigationFormatParser;
import slash.navigation.base.NavigationFormatRegistry;
import slash.navigation.base.ParserContextImpl;
import slash.navigation.base.ParserResult;
import slash.navigation.base.UnreadableFileException;
import slash.navigation.gpx.GpxPosition;
import slash.navigation.gpx.GpxRoute;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static slash.common.type.CompactCalendar.fromMillis;
import static slash.navigation.base.RouteCharacteristics.Route;
import static slash.navigation.base.RouteCharacteristics.Track;

public class FlightRecorderDataFormatTest {
    private static final double LATITUDE = 48 + 7.123 / 60;
    private static final double LONGITUDE = 11 + 16.123 / 60;
    private static final String A_RECORD = "AXXXZZZTestRecorder";
    private static final String PILOT = "HFPLTPILOT:Max Muster";
    private static final String GLIDER = "HFGTYGLIDERTYPE:Test Glider";
    private static final String FIX_10 = "B1000004807123N01116123EA0100001050";
    private static final String FIX_11 = "B1001004807200N01116200EA0101001060";

    private final FlightRecorderDataFormat format = new FlightRecorderDataFormat();

    private static String igc(String... lines) {
        return String.join("\r\n", lines) + "\r\n";
    }

    private List<GpxRoute> read(String content) throws IOException {
        ParserContextImpl<GpxRoute> context = new ParserContextImpl<>();
        format.read(new ByteArrayInputStream(content.getBytes(ISO_8859_1)), context);
        return context.getRoutes();
    }

    private String unreadable(String content) {
        return assertThrows(UnreadableFileException.class, () -> read(content)).getMessage();
    }

    private static long millis(String instant) {
        return Instant.parse(instant).toEpochMilli();
    }

    private static List<String> lines(byte[] bytes) {
        return asList(new String(bytes, ISO_8859_1).split("\r\n"));
    }

    private List<String> write(GpxRoute route) throws IOException {
        ByteArrayOutputStream target = new ByteArrayOutputStream();
        format.write(route, target, 0, route.getPositionCount());
        return lines(target.toByteArray());
    }

    private GpxRoute track(String name, GpxPosition... positions) {
        return format.createRoute(Track, name, new ArrayList<>(asList(positions)));
    }

    private static GpxPosition position(double longitude, double latitude, Double elevation, String time) {
        return new GpxPosition(longitude, latitude, elevation, null, time != null ? fromMillis(millis(time)) : null, null);
    }

    // format properties

    @Test
    public void properties() {
        assertEquals("FAI/IGC Flight Recorder Data (*.igc)", format.getName());
        assertEquals(".igc", format.getExtension());
        assertTrue(format.isSupportsReading());
        assertTrue(format.isSupportsWriting());
        assertEquals(false, format.isSupportsMultipleRoutes());
    }

    // guard

    @Test
    public void readsFileWithoutManufacturerRecord() throws IOException {
        assertEquals(2, read(igc("HFDTE280919", FIX_10, FIX_11)).size());
        assertEquals(2, read(igc("HFDTE280919", A_RECORD, FIX_10)).size());
        assertEquals(2, read(igc("AXX", "HFDTE280919", FIX_10)).size());
    }

    @Test
    public void rejectsFileWithoutFixRecord() throws IOException {
        assertTrue(read(igc(A_RECORD, "HFDTE280919", PILOT)).isEmpty());
    }

    @Test
    public void rejectsOtherContentWithoutThrowing() throws IOException {
        assertTrue(read("<?xml version=\"1.0\"?><gpx/>").isEmpty());
        assertTrue(read("").isEmpty());
        assertTrue(read("\r\n\r\n").isEmpty());
        assertTrue(read("$GPGGA,123519,4807.038,N,01131.000,E,1,08,0.9,545.4,M,46.9,M,,*47").isEmpty());
        assertTrue(read(igc(A_RECORD)).isEmpty());
        assertTrue(read(igc(A_RECORD, "Hello", "Bye")).isEmpty());
    }

    @Test
    public void reportsFileWithOnlyMalformedFixRecords() {
        assertEquals("IGC file cannot be read: it has no readable fix (B) record, 1 unreadable lines, the first: " +
                "fix (B) record parse error: B10010048072", unreadable(igc(A_RECORD, "HFDTE280919", "B10010048072")));
    }

    @Test
    public void reportsEveryTruncationThatLosesAllFixes() throws IOException {
        String valid = igc(A_RECORD, "HFDTE280919", PILOT, FIX_10);
        for (int length = 0; length < valid.length(); length++) {
            String truncated = valid.substring(0, length);
            List<GpxRoute> routes;
            try {
                routes = read(truncated);
            } catch (UnreadableFileException e) {
                assertTrue(e.getMessage().startsWith("IGC file cannot be read: "), "length " + length + ": " + e.getMessage());
                continue;
            }
            // without the date record or the fix: nothing; with the whole fix: the fix
            if (!routes.isEmpty())
                assertEquals(1, routes.get(routes.size() - 1).getPositionCount(), "length " + length);
            else
                assertFalse(truncated.contains(FIX_10), "length " + length);
        }
    }

    @Test
    public void parserReportsAnUnreadableFileInsteadOfAnEmptyRoute() throws IOException {
        ParserResult result = new NavigationFormatParser(new NavigationFormatRegistry()).read(
                new ByteArrayInputStream(igc(A_RECORD, "HFDTE280919", "B1000004807123N01116123EA01000").getBytes(ISO_8859_1)));
        assertFalse(result.isSuccessful());
        assertTrue(result.getFailure().getMessage().startsWith("IGC file cannot be read: it has no readable fix (B) record"),
                result.getFailure().getMessage());
    }

    @Test
    public void skipsMalformedAndUnknownLinesAndKeepsTheFixes() throws IOException {
        List<GpxRoute> routes = read(igc(A_RECORD, "HFDTE280919", FIX_10, "B10010048072", "MUnknown", "K", "lower case",
                FIX_11, "B1002004807200N01116200EA01010010"));
        assertEquals(2, routes.size());
        assertEquals(2, routes.get(0).getPositionCount());
    }

    @Test
    public void toleratesByteOrderMarkCtrlZAndLeadingBlanks() throws IOException {
        String bom = "\u00ef\u00bb\u00bf";
        assertEquals(2, read(bom + igc(A_RECORD, "HFDTE280919", FIX_10)).size());
        assertEquals(2, read(igc("  " + A_RECORD, "\t HFDTE280919", " " + FIX_10) + "\u001a").size());
        assertEquals(2, read(igc(A_RECORD, "HFDTE280919", FIX_10, "\u001a")).size());
    }

    @Test
    public void usesFirstDateForFixesInFrontOfIt() throws IOException {
        List<GpxRoute> routes = read(igc(A_RECORD, FIX_10, "HFDTE280919", FIX_11));
        assertEquals(2, routes.get(0).getPositionCount());
        assertEquals(millis("2019-09-28T10:00:00Z"), routes.get(0).getPosition(0).getTime().getTimeInMillis());
    }

    @Test
    public void skipsFixesWithCoordinatesOutOfRange() throws IOException {
        List<GpxRoute> routes = read(igc(A_RECORD, "HFDTE280919", FIX_10,
                "B1000104860000N01116123EA0100001050", // 48 degrees 60 minutes
                "B1000204807123N18100000EA0100001050", // 181 degrees
                "B1000309100000N01116123EA0100001050", // 91 degrees
                "B1000409000001N01116123EA0100001050", // 90 degrees 0.001 minutes
                "B1000509000000N18000000EA0100001050"));
        assertEquals(2, routes.get(0).getPositionCount());
        assertEquals(90.0, routes.get(0).getPosition(1).getLatitude(), 0.0);
        assertEquals(180.0, routes.get(0).getPosition(1).getLongitude(), 0.0);
    }

    @Test
    public void reportsFixWithoutDate() {
        assertEquals("IGC file cannot be read: it has fix (B) records but no date (HFDTE) record", unreadable(igc(A_RECORD, FIX_10)));
        assertEquals("IGC file cannot be read: its date (HFDTE) record is invalid: bad date: HFDTE320119",
                unreadable(igc(A_RECORD, "HFDTE320119", FIX_10)));
    }

    @Test
    public void ignoresBlankLinesBeforeManufacturerRecord() throws IOException {
        assertEquals(2, read(igc("", A_RECORD, "HFDTE280919", FIX_10)).size());
    }

    // dates

    @Test
    public void readsDateRecordOfOlderFiles() throws IOException {
        List<GpxRoute> routes = read(igc(A_RECORD, "HFDTE280919", FIX_10));
        assertEquals(millis("2019-09-28T10:00:00Z"), routes.get(0).getPosition(0).getTime().getTimeInMillis());
    }

    @Test
    public void readsDateRecordOfNewerFiles() throws IOException {
        List<GpxRoute> routes = read(igc(A_RECORD, "HFDTEDATE:280919,01", FIX_10));
        assertEquals(millis("2019-09-28T10:00:00Z"), routes.get(0).getPosition(0).getTime().getTimeInMillis());
    }

    @Test
    public void readsDateOfLastCenturyForTwoDigitYearsFromSeventy() throws IOException {
        List<GpxRoute> routes = read(igc(A_RECORD, "HFDTE280998", FIX_10));
        assertEquals(millis("1998-09-28T10:00:00Z"), routes.get(0).getPosition(0).getTime().getTimeInMillis());
    }

    @Test
    public void advancesDateWhenPassingMidnight() throws IOException {
        List<GpxRoute> routes = read(igc(A_RECORD, "HFDTE280919",
                "B235958" + "4807123N01116123EA0100001050",
                "B000002" + "4807123N01116123EA0100001050",
                "B000102" + "4807123N01116123EA0100001050"));
        GpxRoute route = routes.get(0);
        assertEquals(millis("2019-09-28T23:59:58Z"), route.getPosition(0).getTime().getTimeInMillis());
        assertEquals(millis("2019-09-29T00:00:02Z"), route.getPosition(1).getTime().getTimeInMillis());
        assertEquals(millis("2019-09-29T00:01:02Z"), route.getPosition(2).getTime().getTimeInMillis());
    }

    @Test
    public void keepsDateForSmallStepsBackInTime() throws IOException {
        GpxRoute route = read(igc(A_RECORD, "HFDTE280919",
                "B100000" + "4807123N01116123EA0100001050",
                "B100005" + "4807123N01116123EA0100001050",
                "B100004" + "4807123N01116123EA0100001050",
                "B100010" + "4807123N01116123EA0100001050")).get(0);
        assertEquals(millis("2019-09-28T10:00:10Z"), route.getPosition(3).getTime().getTimeInMillis());
    }

    @Test
    public void advancesDateOnlyForStepsBackOfMoreThanTwelveHours() throws IOException {
        GpxRoute route = read(igc(A_RECORD, "HFDTE280919",
                "B200000" + "4807123N01116123EA0100001050",
                "B075900" + "4807123N01116123EA0100001050",   // 12 h 1 min back: midnight
                "B000000" + "4807123N01116123EA0100001050",   // 7 h 59 min back: jitter
                "B195900" + "4807123N01116123EA0100001050")).get(0);
        assertEquals(millis("2019-09-29T07:59:00Z"), route.getPosition(1).getTime().getTimeInMillis());
        assertEquals(millis("2019-09-29T00:00:00Z"), route.getPosition(2).getTime().getTimeInMillis());
        assertEquals(millis("2019-09-29T19:59:00Z"), route.getPosition(3).getTime().getTimeInMillis());
    }

    // fixes

    @Test
    public void readsFixesAsTwoTracks() throws IOException {
        List<GpxRoute> routes = read(igc(A_RECORD, "HFDTE280919", FIX_10, FIX_11));

        assertEquals(2, routes.size());
        GpxRoute pressure = routes.get(0), gnss = routes.get(1);
        assertEquals("PRESALTTRK", pressure.getName());
        assertEquals("GNSSALTTRK", gnss.getName());
        assertEquals(Track, pressure.getCharacteristics());
        assertEquals(Track, gnss.getCharacteristics());
        assertEquals(2, pressure.getPositionCount());
        assertEquals(2, gnss.getPositionCount());

        GpxPosition first = pressure.getPosition(0);
        assertEquals(LONGITUDE, first.getLongitude(), 1e-9);
        assertEquals(LATITUDE, first.getLatitude(), 1e-9);
        assertEquals(1000.0, first.getElevation(), 0.0);
        assertEquals(1050.0, gnss.getPosition(0).getElevation(), 0.0);
        assertEquals(1010.0, pressure.getPosition(1).getElevation(), 0.0);
        assertEquals(1060.0, gnss.getPosition(1).getElevation(), 0.0);
        assertEquals(LATITUDE, gnss.getPosition(0).getLatitude(), 1e-9);
        assertEquals(first.getTime(), gnss.getPosition(0).getTime());
        assertNull(first.getSpeed());
        assertNull(first.getHeading());
        assertNull(first.getSatellites());
        assertNull(first.getDescription());
    }

    @Test
    public void readsSouthernAndWesternHemisphere() throws IOException {
        List<GpxRoute> routes = read(igc(A_RECORD, "HFDTE280919", "B1000003330000S07015000WA0100001050"));
        assertEquals(-33.5, routes.get(0).getPosition(0).getLatitude(), 1e-9);
        assertEquals(-70.25, routes.get(0).getPosition(0).getLongitude(), 1e-9);
    }

    @Test
    public void dropsFixesWithValidityVOnlyAtZeroCoordinates() throws IOException {
        List<GpxRoute> routes = read(igc(A_RECORD, "HFDTE280919",
                "B1000000000000N00000000EV0000000000",
                FIX_10.replace("EA0", "EV0"),
                "B1000200000000N00000000EA0100001050",
                "B1000300000000S00000000WV0000000000"));
        GpxRoute route = routes.get(0);
        assertEquals(2, route.getPositionCount());
        assertEquals(LATITUDE, route.getPosition(0).getLatitude(), 1e-9);
        assertEquals(0.0, route.getPosition(1).getLatitude(), 0.0);
    }

    @Test
    public void rejectsFileWithOnlyNoFixRecords() throws IOException {
        assertTrue(read(igc(A_RECORD, "HFDTE280919", "B1000000000000N00000000EV0000000000")).isEmpty());
    }

    @Test
    public void readsNegativeAltitudeAndKeepsFixesWithValidityV() throws IOException {
        List<GpxRoute> routes = read(igc(A_RECORD, "HFDTE280919", "B1000004807123N01116123EV-001200050"));
        assertEquals(1, routes.get(0).getPositionCount());
        assertEquals(-12.0, routes.get(0).getPosition(0).getElevation(), 0.0);
        assertEquals(50.0, routes.get(1).getPosition(0).getElevation(), 0.0);
    }

    @Test
    public void dropsGnssTrackWithoutAltitude() throws IOException {
        List<GpxRoute> routes = read(igc(A_RECORD, "HFDTE280919",
                "B1000004807123N01116123EA0100000000", "B1001004807200N01116200EA0101000000"));
        assertEquals(1, routes.size());
        assertEquals("PRESALTTRK", routes.get(0).getName());
    }

    @Test
    public void dropsPressureTrackWithoutAltitude() throws IOException {
        List<GpxRoute> routes = read(igc(A_RECORD, "HFDTE280919",
                "B1000004807123N01116123EA0000001050", "B1001004807200N01116200EA0000001060"));
        assertEquals(1, routes.size());
        assertEquals("GNSSALTTRK", routes.get(0).getName());
        assertNull(read(igc(A_RECORD, "HFDTE280919",
                "B1000004807123N01116123EA0000000000")).get(0).getPosition(0).getElevation());
    }

    @Test
    public void keepsGnssTrackIfNeitherTrackHasAltitude() throws IOException {
        List<GpxRoute> routes = read(igc(A_RECORD, "HFDTE280919", "B1000004807123N01116123EA0000000000"));
        assertEquals(1, routes.size());
        assertEquals("GNSSALTTRK", routes.get(0).getName());
        assertNull(routes.get(0).getPosition(0).getElevation());
    }

    @Test
    public void ignoresExtensionsLikeGpsBabelDoesByDefault() throws IOException {
        List<GpxRoute> routes = read(igc(A_RECORD, "HFDTE280919", "I023638FXA3940SIU",
                "B1000004807123N01116123EA01000010500507", "G12345", "LXXXcomment", "E100000ATS", "K100000010"));
        assertEquals(2, routes.size());
        assertNull(routes.get(0).getPosition(0).getSatellites());
        assertNull(routes.get(0).getPosition(0).getHdop());
        assertNull(routes.get(0).getPosition(0).getSpeed());
        assertEquals(1000.0, routes.get(0).getPosition(0).getElevation(), 0.0);
    }

    // header

    @Test
    public void storesOtherHeaderRecordsInTrackDescription() throws IOException {
        List<GpxRoute> routes = read(igc(A_RECORD, "HFDTE280919", PILOT, GLIDER, "HOCIDCOMPETITION ID:XY", FIX_10));
        List<String> expected = asList("IGCHDRS~" + PILOT + "~" + GLIDER + "~HOCIDCOMPETITION ID:XY~");
        assertEquals(expected, routes.get(0).getDescription());
        assertEquals(expected, routes.get(1).getDescription());
    }

    @Test
    public void splitsTrackDescriptionAtCommas() throws IOException {
        List<GpxRoute> routes = read(igc(A_RECORD, "HFDTE280919", "HFPLTPILOT:Muster, Max", FIX_10));
        assertEquals(asList("IGCHDRS~HFPLTPILOT:Muster", "Max~"), routes.get(0).getDescription());
    }

    @Test
    public void ignoresHeaderRecordsAfterFirstFix() throws IOException {
        List<GpxRoute> routes = read(igc(A_RECORD, "HFDTE280919", PILOT, FIX_10, GLIDER, FIX_11));
        assertEquals(asList("IGCHDRS~" + PILOT + "~"), routes.get(0).getDescription());
    }

    // tasks

    private static String[] taskFile() {
        return new String[]{A_RECORD, "HFDTE280919", PILOT,
                "C2809190900002809190001" + "01" + "Test task",
                "C4807123N01116123EAirfield",
                "C4807200N01116200EStart gate",
                "C4810000N01120000E",
                "C4812000N01122000EFinish line",
                "C0000000N00000000E",
                FIX_10, FIX_11};
    }

    @Test
    public void readsTaskAsRouteBeforeTracks() throws IOException {
        List<GpxRoute> routes = read(igc(taskFile()));

        assertEquals(3, routes.size());
        GpxRoute task = routes.get(0);
        assertEquals(Route, task.getCharacteristics());
        assertEquals("0001", task.getName());
        assertEquals(asList("IGCDATE280919: Test task"), task.getDescription());
        assertEquals("PRESALTTRK", routes.get(1).getName());
        assertEquals("GNSSALTTRK", routes.get(2).getName());
    }

    @Test
    public void readsTaskPointsWithShortNamesAndDropsUnknownPoint() throws IOException {
        GpxRoute task = read(igc(taskFile())).get(0);

        // the all-zero LANDING point means "unknown" and is dropped
        assertEquals(4, task.getPositionCount());
        assertEquals("TAKEOFF; Airfield", task.getPosition(0).getDescription());
        assertEquals("START; Start gate", task.getPosition(1).getDescription());
        assertEquals("TURN01", task.getPosition(2).getDescription());
        assertEquals("FINISH; Finish line", task.getPosition(3).getDescription());
        assertEquals(LATITUDE, task.getPosition(0).getLatitude(), 1e-9);
        assertEquals(LONGITUDE, task.getPosition(0).getLongitude(), 1e-9);
        assertEquals(48.2, task.getPosition(3).getLatitude(), 1e-9);
        assertEquals(millis("2019-09-28T09:00:00Z"), task.getPosition(0).getTime().getTimeInMillis());
    }

    @Test
    public void numbersTurnPoints() throws IOException {
        GpxRoute task = read(igc(A_RECORD, "HFDTE280919",
                "C2809190900002809190007" + "02",
                "C4807123N01116123E", "C4807200N01116200E",
                "C4808000N01117000E", "C4809000N01118000E",
                "C4810000N01119000E", "C4811000N01120000E",
                FIX_10)).get(0);
        assertEquals("TAKEOFF", task.getPosition(0).getDescription());
        assertEquals("START", task.getPosition(1).getDescription());
        assertEquals("TURN01", task.getPosition(2).getDescription());
        assertEquals("TURN02", task.getPosition(3).getDescription());
        assertEquals("FINISH", task.getPosition(4).getDescription());
        assertEquals("LANDING", task.getPosition(5).getDescription());
        assertEquals("0007", task.getName());
        assertEquals(asList("IGCDATE280919:"), task.getDescription());
    }

    @Test
    public void readsTaskWithoutTurnPoints() throws IOException {
        GpxRoute task = read(igc(A_RECORD, "HFDTE280919",
                "C2809190900002809190001" + "00" + "Task",
                "C4807123N01116123ETakeoff", "C4807200N01116200EStart",
                "C4808000N01117000EFinish", "C4809000N01118000ELanding",
                FIX_10)).get(0);
        assertEquals(4, task.getPositionCount());
        assertEquals("TAKEOFF; Takeoff", task.getPosition(0).getDescription());
        assertEquals("START; Start", task.getPosition(1).getDescription());
        assertEquals("FINISH; Finish", task.getPosition(2).getDescription());
        assertEquals("LANDING; Landing", task.getPosition(3).getDescription());
    }

    @Test
    public void readsSecondTaskAsSecondRoute() throws IOException {
        List<GpxRoute> routes = read(igc(A_RECORD, "HFDTE280919",
                "C2809190900002809190001" + "00" + "Task",
                "C4807123N01116123ETakeoff", "C4807200N01116200EStart",
                "C4808000N01117000EFinish", "C4809000N01118000ELanding",
                "C2809190900002809190002" + "00" + "Tsk2",
                "C4807123N01116123ETakeoff", "C4807200N01116200EStart",
                "C4808000N01117000EFinish", "C4809000N01118000ELanding",
                FIX_10));
        assertEquals(4, routes.size());
        assertEquals("0001", routes.get(0).getName());
        assertEquals("0002", routes.get(1).getName());
        assertEquals(Route, routes.get(1).getCharacteristics());
        assertEquals("LANDING; Landing", routes.get(1).getPosition(3).getDescription());
    }

    @Test
    public void keepsPointsOfIncompleteTaskWhenNextTaskStarts() throws IOException {
        List<GpxRoute> routes = read(igc(A_RECORD, "HFDTE280919",
                "C2809190900002809190001" + "02" + "Task",
                "C4807123N01116123ETakeoff", "C4807200N01116200EStart",
                "C2809190900002809190002" + "00" + "Tsk2",
                "C4807123N01116123ETakeoff", "C4807200N01116200EStart",
                "C4808000N01117000EFinish", "C4809000N01118000ELanding",
                FIX_10));
        assertEquals(4, routes.size());
        assertEquals(2, routes.get(0).getPositionCount());
        assertEquals(4, routes.get(1).getPositionCount());
    }

    @Test
    public void dropsDamagedTaskOnlyAndKeepsFixes() throws IOException {
        List<GpxRoute> routes = read(igc(A_RECORD, "HFDTE280919",
                "C2809190900002809190001" + "00" + "Bad",
                "C4807123N01116123ETakeoff", "Cgarbage", "C4808000N01117000EFinish",
                "C2809190900002809190002" + "00" + "Good",
                "C4807123N01116123ETakeoff", "C4807200N01116200EStart",
                "C4808000N01117000EFinish", "C4809000N01118000ELanding",
                "C4807123N01116123E", // stray point
                FIX_10));
        assertEquals(3, routes.size());
        assertEquals("0002", routes.get(0).getName());
        assertEquals(4, routes.get(0).getPositionCount());
    }

    @Test
    public void readsPostFlightDeclarationFromLogBookRecord() throws IOException {
        List<GpxRoute> routes = read(igc(A_RECORD, "HFDTE280919", FIX_10,
                "LPFCC2809191200002809190002" + "01" + "Post",
                "LPFCC4807123N01116123E", "LPFCC4807200N01116200E", "LPFCC4807300N01116300E", "LPFCC4807400N01116400E", "LPFCC4807500N01116500E",
                "LXXXsome comment"));
        assertEquals(3, routes.size());
        assertEquals(Route, routes.get(0).getCharacteristics());
        assertEquals("0002", routes.get(0).getName());
        assertEquals(5, routes.get(0).getPositionCount());
    }

    // synthetic cases checked against the aerofiles and go-igc readers

    private static final int UNREADABLE = -1;

    private static final Object[][] CASES = {
        {"01-basic-crlf.igc", 3, "2011-01-16T10:00:00Z", "2011-01-16T10:00:10Z"},
        {"02-basic-lf.igc", 3, "2011-01-16T10:00:00Z", "2011-01-16T10:00:10Z"},
        {"03-basic-cr.igc", 3, "2011-01-16T10:00:00Z", "2011-01-16T10:00:10Z"},
        {"04-hfdte-old.igc", 3, "2011-01-16T10:00:00Z", "2011-01-16T10:00:10Z"},
        {"05-hfdte-colon-nocomma.igc", 3, "2011-01-16T10:00:00Z", "2011-01-16T10:00:10Z"},
        {"06-hfdte-utc-long.igc", 3, "2011-01-16T10:00:00Z", "2011-01-16T10:00:10Z"},
        {"07-hfdte-flightno.igc", 3, "2011-01-16T10:00:00Z", "2011-01-16T10:00:10Z"},
        {"08-hfdte-4digit-year.igc", 3, "2020-01-16T10:00:00Z", "2020-01-16T10:00:10Z"},
        {"09-year-99.igc", 3, "1999-01-01T10:00:00Z", "1999-01-01T10:00:10Z"},
        {"10-year-00.igc", 3, "2000-01-01T10:00:00Z", "2000-01-01T10:00:10Z"},
        {"11-midnight.igc", 4, "2011-01-16T23:59:50Z", "2011-01-17T00:00:05Z"},
        {"12-time-jitter-back.igc", 4, "2011-01-16T10:00:00Z", "2011-01-16T10:00:10Z"},
        {"13-time-duplicate.igc", 3, "2011-01-16T10:00:00Z", "2011-01-16T10:00:01Z"},
        {"14-i-record.igc", 2, "2011-01-16T10:00:00Z", "2011-01-16T10:00:05Z"},
        {"15-lad-lod.igc", 2, "2011-01-16T10:00:00Z", "2011-01-16T10:00:05Z"},
        {"16-v-fixes.igc", 2, "2011-01-16T10:00:05Z", "2011-01-16T10:00:10Z"},
        {"17-alt-pressure-only.igc", 2, "2011-01-16T10:00:00Z", "2011-01-16T10:00:05Z"},
        {"18-alt-gnss-only.igc", 2, "2011-01-16T10:00:00Z", "2011-01-16T10:00:05Z"},
        {"19-alt-zero-both.igc", 2, "2011-01-16T10:00:00Z", "2011-01-16T10:00:05Z"},
        {"20-alt-negative.igc", 2, "2011-01-16T10:00:00Z", "2011-01-16T10:00:05Z"},
        {"21-alt-real-zero.igc", 3, "2011-01-16T10:00:00Z", "2011-01-16T10:00:10Z"},
        {"21b-alt-real-zero-distinct-coords.igc", 3, "2011-01-16T10:00:00Z", "2011-01-16T10:00:10Z"},
        {"22-unknown-record-M.igc", 2, "2011-01-16T10:00:00Z", "2011-01-16T10:00:05Z"},
        {"23-short-line.igc", 2, "2011-01-16T10:00:00Z", "2011-01-16T10:00:05Z"},
        {"24-lowercase.igc", 2, "2011-01-16T10:00:00Z", "2011-01-16T10:00:05Z"},
        {"25-truncated-last-b.igc", 2, "2011-01-16T10:00:00Z", "2011-01-16T10:00:05Z"},
        {"26-bad-b-middle.igc", 2, "2011-01-16T10:00:00Z", "2011-01-16T10:00:10Z"},
        {"27-bom.igc", 2, "2011-01-16T10:00:00Z", "2011-01-16T10:00:05Z"},
        {"28-ctrlz-eof.igc", 2, "2011-01-16T10:00:00Z", "2011-01-16T10:00:05Z"},
        {"29-leading-blank-lines.igc", 2, "2011-01-16T10:00:00Z", "2011-01-16T10:00:05Z"},
        {"30-leading-space-A.igc", 2, "2011-01-16T10:00:00Z", "2011-01-16T10:00:05Z"},
        {"31-no-A-record.igc", 2, "2011-01-16T10:00:00Z", "2011-01-16T10:00:05Z"},
        {"32-b-before-date.igc", 2, "2011-01-16T10:00:00Z", "2011-01-16T10:00:05Z"},
        {"33-no-date.igc", UNREADABLE, null, null},
        {"34-b-trailing-spaces.igc", 2, "2011-01-16T10:00:00Z", "2011-01-16T10:00:05Z"},
        {"35-b-with-spaces-in-coords.igc", UNREADABLE, null, null},
        {"36-non-ascii-header.igc", 2, "2011-01-16T10:00:00Z", "2011-01-16T10:00:05Z"},
        {"37-utf8-header.igc", 2, "2011-01-16T10:00:00Z", "2011-01-16T10:00:05Z"},
        {"38-hfdtm-non-wgs84.igc", 2, "2011-01-16T10:00:00Z", "2011-01-16T10:00:05Z"},
        {"39-lat-lon-limits.igc", 3, "2011-01-16T10:00:00Z", "2011-01-16T10:00:10Z"},
        {"40-lat-invalid-minutes.igc", UNREADABLE, null, null},
        {"41-task-ok.igc", 2, "2011-01-16T10:00:00Z", "2011-01-16T10:00:05Z"},
        {"42-task-zero-tp.igc", 2, "2011-01-16T10:00:00Z", "2011-01-16T10:00:05Z"},
        {"42b-two-tasks-zero-tp.igc", 2, "2011-01-16T10:00:00Z", "2011-01-16T10:00:05Z"},
        {"43-task-incomplete.igc", 2, "2011-01-16T10:00:00Z", "2011-01-16T10:00:05Z"},
        {"44-task-bad-id.igc", 2, "2011-01-16T10:00:00Z", "2011-01-16T10:00:05Z"},
        {"45-task-no-task-declared.igc", 2, "2011-01-16T10:00:00Z", "2011-01-16T10:00:05Z"},
        {"46-task-no-id-leading-c.igc", 1, "2011-01-16T10:00:00Z", "2011-01-16T10:00:00Z"},
        {"47-task-short-name.igc", 1, "2011-01-16T10:00:00Z", "2011-01-16T10:00:00Z"},
        {"48-many-misc-records.igc", 2, "2011-01-16T10:00:00Z", "2011-01-16T10:00:05Z"},
        {"49-long-headers.igc", 2, "2011-01-16T10:00:00Z", "2011-01-16T10:00:05Z"},
        {"50-two-dates.igc", 3, "2011-01-16T10:00:00Z", "2011-01-17T10:00:10Z"},
        {"51-validity-other.igc", 2, "2011-01-16T10:00:00Z", "2011-01-16T10:00:05Z"},
        {"52-alt-too-long.igc", 1, "2011-01-16T10:00:00Z", "2011-01-16T10:00:00Z"},
        {"53-b-extra-ext-without-I.igc", 2, "2011-01-16T10:00:00Z", "2011-01-16T10:00:05Z"},
        {"54-no-fixes.igc", 0, null, null},
        {"55-only-A.igc", 0, null, null},
        {"56-empty.igc", 0, null, null},
        {"57-not-igc-text.igc", 0, null, null},
        {"58-bad-date.igc", UNREADABLE, null, null},
        {"59-hfdte-short.igc", UNREADABLE, null, null},
        {"60-H-subtype-other.igc", 2, "2011-01-16T10:00:00Z", "2011-01-16T10:00:05Z"},
    };

    @Test
    public void readsSyntheticCases() throws IOException {
        for (Object[] c : CASES) {
            String name = (String) c[0];
            int count = (Integer) c[1];
            List<GpxRoute> routes;
            try (InputStream in = getClass().getResourceAsStream("cases/" + name)) {
                ParserContextImpl<GpxRoute> context = new ParserContextImpl<>();
                format.read(in, context);
                routes = context.getRoutes();
            } catch (UnreadableFileException e) {
                // UNREADABLE: the file is an IGC file, but nothing can be read from it (D19)
                assertEquals(UNREADABLE, count, name + ": " + e.getMessage());
                continue;
            }
            assertTrue(count != UNREADABLE, name + " is readable");
            if (count == 0) {
                assertTrue(routes.isEmpty(), name);
                continue;
            }
            GpxRoute track = routes.get(routes.size() - 1);
            assertEquals(Track, track.getCharacteristics(), name);
            assertEquals(count, track.getPositionCount(), name);
            assertEquals(millis((String) c[2]), track.getPosition(0).getTime().getTimeInMillis(), name);
            assertEquals(millis((String) c[3]), track.getPosition(count - 1).getTime().getTimeInMillis(), name);
        }
    }

    // write

    private static List<String> fixes(List<String> lines) {
        return lines.stream().filter(l -> l.startsWith("B")).collect(java.util.stream.Collectors.toList());
    }

    private static String headerValue(List<String> lines, String code) {
        for (String line : lines) {
            if (line.startsWith("HF" + code))
                return line;
        }
        return null;
    }

    @Test
    public void writesHeaderFixesAndBrandedTrailer() throws IOException {
        GpxRoute route = track("GNSSALTTRK",
                position(LONGITUDE, LATITUDE, 1050.0, "2019-09-28T10:00:00Z"),
                position(11 + 16.2 / 60, 48 + 7.2 / 60, 1060.9, "2019-09-28T10:01:00.750Z"));

        List<String> lines = write(route);

        int b = lines.size() - 4;
        assertEquals("AXXXZZZRouteConverter", lines.get(0));
        assertEquals("HFDTEDATE:280919,01", lines.get(1));
        assertEquals("B1000004807123N01116123EA0000001050", lines.get(b));
        // 10:01:00.750 is rounded to the next second, the altitude 1060.9 to the next meter
        assertEquals("B1001014807200N01116200EA0000001061", lines.get(b + 1));
        assertTrue(lines.get(b + 2).startsWith("LXXXGenerated by RouteConverter "), lines.get(b + 2));
        assertEquals("GRouteConverterSecurityRecordGuaranteedToFailVALIChecks", lines.get(b + 3));
    }

    @Test
    public void writesTheRequiredHeaderRecordsOfTheSpecification() throws IOException {
        List<String> lines = write(track("GNSSALTTRK", position(LONGITUDE, LATITUDE, 1050.0, "2019-09-28T10:00:00Z")));
        assertEquals("HFDTEDATE:280919,01", lines.get(1));
        for (String code : asList("FXA", "PLT", "CM2", "GTY", "GID", "DTM", "RFW", "RHW", "FTY", "GPS", "PRS"))
            assertTrue(headerValue(lines, code) != null, code);
        assertEquals("HFPLTPILOTINCHARGE:Unknown", headerValue(lines, "PLT"));
        assertEquals("HFDTM100GPSDATUM:WGS-1984", headerValue(lines, "DTM"));
        assertTrue(headerValue(lines, "FTY").startsWith("HFFTYFRTYPE:RouteConverter,"));
    }

    @Test
    public void writtenFileReadsBackWithItsHeader() throws IOException {
        ByteArrayOutputStream target = new ByteArrayOutputStream();
        format.write(track("GNSSALTTRK", position(LONGITUDE, LATITUDE, 1050.0, "2019-09-28T10:00:00Z")), target, 0, 1);
        List<GpxRoute> routes = read(new String(target.toByteArray(), ISO_8859_1));
        assertEquals(1, routes.size());
        assertEquals(millis("2019-09-28T10:00:00Z"), routes.get(0).getPosition(0).getTime().getTimeInMillis());
    }

    @Test
    public void roundsSubSecondsAndAdvancesTheDateWithTheRoundedFirstFix() throws IOException {
        List<String> lines = write(track("GNSSALTTRK",
                position(LONGITUDE, LATITUDE, 1.0, "2019-09-28T23:59:59.600Z"),
                position(LONGITUDE, LATITUDE, 1.0, "2019-09-29T00:00:01.400Z"),
                position(LONGITUDE, LATITUDE, 1.0, "2019-09-29T00:00:02.500Z")));
        assertEquals("HFDTEDATE:290919,01", lines.get(1));
        int b = lines.size() - 5;
        assertEquals("B000000", lines.get(b).substring(0, 7));
        assertEquals("B000001", lines.get(b + 1).substring(0, 7));
        assertEquals("B000003", lines.get(b + 2).substring(0, 7));
    }

    @Test
    public void clampsAltitudesToTheFieldWidth() throws IOException {
        List<String> lines = write(track("GNSSALTTRK",
                position(LONGITUDE, LATITUDE, 123456.0, "2019-09-28T10:00:00Z"),
                position(LONGITUDE, LATITUDE, -12345.0, "2019-09-28T10:00:01Z"),
                position(LONGITUDE, LATITUDE, 99999.4, "2019-09-28T10:00:02Z"),
                position(LONGITUDE, LATITUDE, -9999.0, "2019-09-28T10:00:03Z"),
                position(LONGITUDE, LATITUDE, -0.4, "2019-09-28T10:00:04Z"),
                position(LONGITUDE, LATITUDE, -12.5, "2019-09-28T10:00:05Z")));
        int b = lines.size() - 8;
        assertEquals("B1000004807123N01116123EA0000099999", lines.get(b));
        assertEquals("B1000014807123N01116123EA00000-9999", lines.get(b + 1));
        assertEquals("B1000024807123N01116123EA0000099999", lines.get(b + 2));
        assertEquals("B1000034807123N01116123EA00000-9999", lines.get(b + 3));
        assertEquals("B1000044807123N01116123EA0000000000", lines.get(b + 4));
        assertEquals("B1000054807123N01116123EA00000-0013", lines.get(b + 5));
        for (int i = b; i < b + 6; i++)
            assertEquals(35, lines.get(i).length());
    }

    @Test
    public void writesHeaderFieldsWithCommasAsOneLine() throws IOException {
        GpxRoute route = read(igc(A_RECORD, "HFDTE280919", "HFPLTPILOT:Muster, Max", FIX_10)).get(1);
        List<String> lines = write(route);
        assertEquals("HFPLTPILOT:Muster, Max", headerValue(lines, "PLT"));
        assertEquals(1, lines.stream().filter(l -> l.contains("Muster")).count());
    }

    @Test
    public void writesTrackNotNamedLikeIgcTracksWithGnssAltitude() throws IOException {
        List<String> lines = write(track("Some track", position(LONGITUDE, LATITUDE, 1050.0, "2019-09-28T10:00:00Z")));
        assertEquals("B1000004807123N01116123EA0000001050", fixes(lines).get(0));
        assertEquals("B1000004807123N01116123EA0000001050", fixes(write(track(null,
                position(LONGITUDE, LATITUDE, 1050.0, "2019-09-28T10:00:00Z")))).get(0));
    }

    @Test
    public void writesPressureTrackWithPressureAltitude() throws IOException {
        List<String> lines = write(track("PRESALTTRK", position(LONGITUDE, LATITUDE, 1000.0, "2019-09-28T10:00:00Z")));
        assertEquals("B1000004807123N01116123EA0100000000", fixes(lines).get(0));
    }

    @Test
    public void writesZeroForMissingAndSignedForNegativeAltitudes() throws IOException {
        List<String> lines = write(track("GNSSALTTRK",
                position(LONGITUDE, LATITUDE, null, "2019-09-28T10:00:00Z"),
                position(LONGITUDE, LATITUDE, -12.0, "2019-09-28T10:00:01Z")));
        assertEquals("B1000004807123N01116123EA0000000000", lines.get(lines.size() - 4));
        assertEquals("B1000014807123N01116123EA00000-0012", lines.get(lines.size() - 3));
    }

    @Test
    public void writesSouthernAndWesternHemisphere() throws IOException {
        List<String> lines = write(track("GNSSALTTRK", position(-70.25, -33.5, 5.0, "2019-09-28T10:00:00Z")));
        assertEquals("B1000003330000S07015000WA0000000005", fixes(lines).get(0));
    }

    @Test
    public void writesDateOfFirstPointAndTimeInUtc() throws IOException {
        List<String> lines = write(track("GNSSALTTRK",
                position(LONGITUDE, LATITUDE, 1.0, "2019-09-28T23:59:59Z"),
                position(LONGITUDE, LATITUDE, 1.0, "2019-09-29T00:00:01Z")));
        assertEquals("HFDTEDATE:280919,01", lines.get(1));
        assertEquals("B235959", fixes(lines).get(0).substring(0, 7));
        assertEquals("B000001", fixes(lines).get(1).substring(0, 7));
    }

    @Test
    public void writesRequestedRangeAndSkipsPositionsWithoutCoordinates() throws IOException {
        GpxRoute route = track("GNSSALTTRK",
                position(LONGITUDE, LATITUDE, 1.0, "2019-09-28T10:00:00Z"),
                new GpxPosition(null, null, 2.0, null, fromMillis(millis("2019-09-28T10:00:01Z")), null),
                position(LONGITUDE, LATITUDE, 3.0, "2019-09-28T10:00:02Z"));
        ByteArrayOutputStream target = new ByteArrayOutputStream();
        format.write(route, target, 1, 3);
        List<String> lines = lines(target.toByteArray());
        assertEquals(1, fixes(lines).size());
        assertEquals("B100002", fixes(lines).get(0).substring(0, 7));
    }

    @Test
    public void writesTrackWithoutPositionsAsHeaderOnly() throws IOException {
        List<String> lines = write(track("GNSSALTTRK"));
        assertEquals(0, fixes(lines).size());
        assertEquals("HFPLTPILOTINCHARGE:Unknown", headerValue(lines, "PLT"));
    }

    @Test
    public void refusesToWriteTrackWithoutTimestamps() {
        GpxRoute route = track("GNSSALTTRK", position(LONGITUDE, LATITUDE, 1050.0, null));
        assertThrows(IOException.class, () -> write(route));
    }

    @Test
    public void refusesToWriteIfAnyPositionLacksTimestamp() {
        GpxRoute route = track("GNSSALTTRK",
                position(LONGITUDE, LATITUDE, 1050.0, "2019-09-28T10:00:00Z"),
                position(LONGITUDE, LATITUDE, 1050.0, null));
        assertThrows(IOException.class, () -> write(route));
    }

    @Test
    public void writesHeaderRecordsStoredInTrackDescription() throws IOException {
        GpxRoute route = read(igc(A_RECORD, "HFDTE280919", PILOT, GLIDER, FIX_10)).get(1);
        List<String> lines = write(route);
        assertEquals("HFDTEDATE:280919,01", lines.get(1));
        assertEquals(PILOT, headerValue(lines, "PLT"));
        assertEquals(GLIDER, headerValue(lines, "GTY"));
        assertEquals(2, lines.stream().filter(l -> l.startsWith("HFPLT") || l.startsWith("HFGTY")).count());
        assertTrue(fixes(lines).get(0).startsWith("B100000"));
    }

    @Test
    public void readsWhatItWrites() throws IOException {
        List<GpxRoute> original = read(igc(A_RECORD, "HFDTE280919", PILOT, FIX_10, FIX_11));
        ByteArrayOutputStream target = new ByteArrayOutputStream();
        format.write(original.get(1), target, 0, 2);

        List<GpxRoute> routes = read(new String(target.toByteArray(), ISO_8859_1));

        assertEquals(1, routes.size());
        assertEquals("GNSSALTTRK", routes.get(0).getName());
        assertEquals(2, routes.get(0).getPositionCount());
        assertEquals(LATITUDE, routes.get(0).getPosition(0).getLatitude(), 1e-6);
        assertEquals(1050.0, routes.get(0).getPosition(0).getElevation(), 0.0);
        assertEquals(original.get(1).getPosition(1).getTime(), routes.get(0).getPosition(1).getTime());
    }
}
