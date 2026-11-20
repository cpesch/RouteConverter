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
import slash.navigation.base.ParserContextImpl;
import slash.navigation.gpx.GpxPosition;
import slash.navigation.gpx.GpxRoute;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;
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
    public void rejectsFileWithoutManufacturerRecord() throws IOException {
        assertTrue(read(igc("HFDTE280919", FIX_10, FIX_11)).isEmpty());
    }

    @Test
    public void rejectsFileWithManufacturerRecordNotFirst() throws IOException {
        assertTrue(read(igc("HFDTE280919", A_RECORD, FIX_10)).isEmpty());
    }

    @Test
    public void rejectsTooShortManufacturerRecord() throws IOException {
        assertTrue(read(igc("AXX", "HFDTE280919", FIX_10)).isEmpty());
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
    }

    @Test
    public void rejectsMalformedFixRecordWithoutThrowing() throws IOException {
        assertTrue(read(igc(A_RECORD, "HFDTE280919", FIX_10, "B10010048072")).isEmpty());
    }

    @Test
    public void rejectsFixBeforeDate() throws IOException {
        assertTrue(read(igc(A_RECORD, FIX_10)).isEmpty());
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

    // write

    @Test
    public void writesHeaderFixesAndBrandedTrailer() throws IOException {
        GpxRoute route = track("GNSSALTTRK",
                position(LONGITUDE, LATITUDE, 1050.0, "2019-09-28T10:00:00Z"),
                position(11 + 16.2 / 60, 48 + 7.2 / 60, 1060.9, "2019-09-28T10:01:00.750Z"));

        List<String> lines = write(route);

        assertEquals(7, lines.size());
        assertEquals("AXXXZZZRouteConverter", lines.get(0));
        assertEquals("HFDTE280919", lines.get(1));
        assertEquals("HFPLTPILOT:Unknown", lines.get(2));
        assertEquals("B1000004807123N01116123EA0000001050", lines.get(3));
        assertEquals("B1001004807200N01116200EA0000001060", lines.get(4));
        assertTrue(lines.get(5).startsWith("LXXXGenerated by RouteConverter "), lines.get(5));
        assertEquals("GRouteConverterSecurityRecordGuaranteedToFailVALIChecks", lines.get(6));
    }

    @Test
    public void writesTrackNotNamedLikeIgcTracksWithGnssAltitude() throws IOException {
        List<String> lines = write(track("Some track", position(LONGITUDE, LATITUDE, 1050.0, "2019-09-28T10:00:00Z")));
        assertEquals("B1000004807123N01116123EA0000001050", lines.get(3));
        assertEquals("B1000004807123N01116123EA0000001050", write(track(null,
                position(LONGITUDE, LATITUDE, 1050.0, "2019-09-28T10:00:00Z"))).get(3));
    }

    @Test
    public void writesPressureTrackWithPressureAltitude() throws IOException {
        List<String> lines = write(track("PRESALTTRK", position(LONGITUDE, LATITUDE, 1000.0, "2019-09-28T10:00:00Z")));
        assertEquals("B1000004807123N01116123EA0100000000", lines.get(3));
    }

    @Test
    public void writesZeroForMissingAndSignedForNegativeAltitudes() throws IOException {
        List<String> lines = write(track("GNSSALTTRK",
                position(LONGITUDE, LATITUDE, null, "2019-09-28T10:00:00Z"),
                position(LONGITUDE, LATITUDE, -12.0, "2019-09-28T10:00:01Z")));
        assertEquals("B1000004807123N01116123EA0000000000", lines.get(3));
        assertEquals("B1000014807123N01116123EA00000-0012", lines.get(4));
    }

    @Test
    public void writesSouthernAndWesternHemisphere() throws IOException {
        List<String> lines = write(track("GNSSALTTRK", position(-70.25, -33.5, 5.0, "2019-09-28T10:00:00Z")));
        assertEquals("B1000003330000S07015000WA0000000005", lines.get(3));
    }

    @Test
    public void writesDateOfFirstPointAndTimeInUtc() throws IOException {
        List<String> lines = write(track("GNSSALTTRK",
                position(LONGITUDE, LATITUDE, 1.0, "2019-09-28T23:59:59Z"),
                position(LONGITUDE, LATITUDE, 1.0, "2019-09-29T00:00:01Z")));
        assertEquals("HFDTE280919", lines.get(1));
        assertEquals("B235959", lines.get(3).substring(0, 7));
        assertEquals("B000001", lines.get(4).substring(0, 7));
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
        assertEquals(6, lines.size());
        assertEquals("B100002", lines.get(3).substring(0, 7));
    }

    @Test
    public void writesTrackWithoutPositionsAsHeaderOnly() throws IOException {
        List<String> lines = write(track("GNSSALTTRK"));
        assertEquals(5, lines.size());
        assertEquals("HFPLTPILOT:Unknown", lines.get(2));
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
        assertEquals("HFDTE280919", lines.get(1));
        assertEquals(PILOT, lines.get(2));
        assertEquals(GLIDER, lines.get(3));
        assertTrue(lines.get(4).startsWith("B100000"));
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
