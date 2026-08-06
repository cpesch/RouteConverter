/*
    This file is part of RouteConverter.

    RouteConverter is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    RouteConverter is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with RouteConverter; if not, write to the Free Software
    Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA

    Copyright (C) 2007 Christian Pesch. All Rights Reserved.
*/
package slash.navigation.columbus;

import org.junit.Before;
import org.junit.Test;
import slash.common.prefs.InMemoryPreferences;
import slash.navigation.base.RouteCharacteristics;
import slash.navigation.base.WaypointType;
import slash.navigation.base.Wgs84Position;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static slash.navigation.base.ConvertBase.ignoreLocalTimeZone;
import static slash.navigation.columbus.ColumbusV1000Device.getTimeZone;
import static slash.navigation.columbus.ColumbusV1000Device.getUseLocalTimeZone;
import static slash.navigation.columbus.ColumbusV1000Device.setTimeZone;
import static slash.navigation.columbus.ColumbusV1000Device.setUseLocalTimeZone;

public class ColumbusFusionFormatTest {
    private final ColumbusFusionFormat format = new ColumbusFusionFormat();

    @Before
    public void setUp() {
        ColumbusV1000Device.setPreferences(new InMemoryPreferences());
    }

    private static BufferedReader reader(String... lines) {
        StringBuilder builder = new StringBuilder();
        for (String line : lines)
            builder.append(line).append('\n');
        return new BufferedReader(new StringReader(builder.toString()));
    }

    @Test
    public void testGetName() {
        assertEquals("Columbus Fusion (*.csv)", format.getName());
    }

    @Test
    public void testSupportsReadingAndWriting() {
        assertTrue(format.isSupportsReading());
        assertTrue(format.isSupportsWriting());
    }

    @Test
    public void testIsValidHeaderDirective() {
        assertTrue(format.isValidLine("# Format=ColumbusFusion; Version=1.0; Type=GNSS"));
        assertTrue(format.isValidLine("# Format=ColumbusFusion; Version=1.0; Type=GNSS+IMU"));
        assertTrue(format.isValidLine("# Format=ColumbusFusion; Version=1.0; Type=IMU"));
        assertFalse(format.isValidLine("# Format=ColumbusFusion; Version=1.0; Type=MAGNETOMETER"));

        assertTrue(format.isValidLine("tag,date,time,lat,lon,alt,speed,heading"));
        assertTrue(format.isValidLine("tag,date,time,lat,lon,alt,speed,heading,sat,hdop"));
        assertTrue(format.isValidLine("tag,date,time,lat,lon,alt,speed,heading,sat,hdop,fix"));
        assertTrue(format.isValidLine("tag,date,time,lat,lon,alt,speed,heading,ax,ay,az"));
        assertTrue(format.isValidLine("tag,date,time,ax,ay,az"));

        assertEquals(ColumbusFusionFormat.LAYOUT_GNSS,
                format.detectLayout("tag,date,time,lat,lon,alt,speed,heading"));
        assertEquals(ColumbusFusionFormat.LAYOUT_GNSS_SAT,
                format.detectLayout("tag,date,time,lat,lon,alt,speed,heading,sat,hdop"));
        assertEquals(ColumbusFusionFormat.LAYOUT_GNSS_SAT_FIX,
                format.detectLayout("tag,date,time,lat,lon,alt,speed,heading,sat,hdop,fix"));
        assertEquals(ColumbusFusionFormat.LAYOUT_GNSS_IMU,
                format.detectLayout("tag,date,time,lat,lon,alt,speed,heading,ax,ay,az"));
        assertEquals(ColumbusFusionFormat.LAYOUT_IMU,
                format.detectLayout("tag,date,time,ax,ay,az"));
    }

    // --- acceleration layouts ---

    @Test
    public void testParseGnssImuFirstSamplePerSecond() throws IOException {
        List<Wgs84Position> positions = format.parseBody(reader(
                "T,260707,210037,26.0789597,119.2996178,14.5,0.1,0.0,-0.30,0.05,1.08",
                ",,,,,,,,-0.27,0.05,1.02",
                ",,210038,26.0789597,119.2996178,14.5,0.0,0.0,-0.33,0.09,1.05"
        ), ColumbusFusionFormat.LAYOUT_GNSS_IMU);

        assertEquals(2, positions.size());
        assertEquals(-0.30, positions.get(0).getAccelerationX(), 0.0);
        assertEquals(0.05, positions.get(0).getAccelerationY(), 0.0);
        assertEquals(1.08, positions.get(0).getAccelerationZ(), 0.0);
        assertEquals(-0.33, positions.get(1).getAccelerationX(), 0.0);
        assertEquals(0.09, positions.get(1).getAccelerationY(), 0.0);
        assertEquals(1.05, positions.get(1).getAccelerationZ(), 0.0);
        assertEquals(26.0789597, positions.get(0).getLatitude(), 0.0);
        assertEquals(119.2996178, positions.get(0).getLongitude(), 0.0);
    }

    @Test
    public void testGnssImuManyContinuationRows() throws IOException {
        String[] lines = new String[21];
        lines[0] = "T,260707,210037,26.0789597,119.2996178,14.5,0.1,0.0,-0.30,0.05,1.08";
        for (int i = 1; i < 20; i++)
            lines[i] = ",,,,,,,,-0.27,0.05,1.02";
        lines[20] = ",,210038,26.0789597,119.2996178,14.5,0.0,0.0,-0.33,0.09,1.05";

        List<Wgs84Position> positions = format.parseBody(reader(lines), ColumbusFusionFormat.LAYOUT_GNSS_IMU);

        assertEquals(2, positions.size());
    }

    @Test
    public void testParseImuFile() throws Exception {
        ignoreLocalTimeZone(() -> {
            List<Wgs84Position> positions = format.parseBody(reader(
                    "T,260709,042922,-0.02,0.04,1.09",
                    ",,,-0.02,0.04,1.06",
                    ",,042923,-0.04,0.06,1.09"
            ), ColumbusFusionFormat.LAYOUT_IMU);

            assertEquals(2, positions.size());
            for (Wgs84Position position : positions) {
                assertNull(position.getLongitude());
                assertNull(position.getLatitude());
                assertNull(position.getElevation());
            }
            assertEquals("042922", format.formatTime(positions.get(0).getTime()));
            assertEquals(-0.02, positions.get(0).getAccelerationX(), 0.0);
            assertEquals(0.04, positions.get(0).getAccelerationY(), 0.0);
            assertEquals(1.09, positions.get(0).getAccelerationZ(), 0.0);
            assertEquals(-0.04, positions.get(1).getAccelerationX(), 0.0);
            assertEquals(1.09, positions.get(1).getAccelerationZ(), 0.0);
        });
    }

    @Test
    public void testImuCarryForwardDate() throws Exception {
        ignoreLocalTimeZone(() -> {
            List<Wgs84Position> positions = format.parseBody(reader(
                    "T,260709,042922,-0.02,0.04,1.09",
                    ",,042923,-0.04,0.06,1.09"
            ), ColumbusFusionFormat.LAYOUT_IMU);

            assertEquals(2, positions.size());
            assertEquals("260709", format.formatDate(positions.get(1).getTime()));
            assertEquals(WaypointType.Waypoint, positions.get(1).getWaypointType());
        });
    }

    @Test
    public void testReadGnssImuFile() throws IOException {
        BufferedReader bufferedReader = reader(
                "# Format=ColumbusFusion; Version=1.0; Type=GNSS+IMU",
                "tag,date,time,lat,lon,alt,speed,heading,ax,ay,az",
                "T,260707,210037,26.0789597,119.2996178,14.5,0.1,0.0,-0.30,0.05,1.08",
                ",,,,,,,,-0.27,0.05,1.02",
                "C,260707,210038,26.0789597,119.2996178,14.5,0.0,0.0,-0.33,0.09,1.05"
        );

        assertTrue(format.isValidLine(bufferedReader.readLine()));
        int layout = format.detectLayout(bufferedReader.readLine());
        assertEquals(ColumbusFusionFormat.LAYOUT_GNSS_IMU, layout);

        List<Wgs84Position> positions = format.parseBody(bufferedReader, layout);
        assertEquals(2, positions.size());
        assertEquals(WaypointType.PointOfInterestC, positions.get(1).getWaypointType());
    }

    @Test
    public void testParseGnssPosition() {
        Wgs84Position position = format.parsePosition("T,240115,101530,52.5200,13.4050,34.0,12.3,88.0", null);
        assertEquals(52.5200, position.getLatitude(), 0.0);
        assertEquals(13.4050, position.getLongitude(), 0.0);
        assertEquals(34.0, position.getElevation(), 0.0);
        assertEquals(12.3, position.getSpeed(), 0.0);
        assertEquals(88.0, position.getHeading(), 0.0);
        assertEquals(WaypointType.Waypoint, position.getWaypointType());
    }

    @Test
    public void testParseSatHdopLayout() throws IOException {
        List<Wgs84Position> positions = format.parseBody(
                reader("T,240115,101530,52.5200,13.4050,34.0,12.3,88.0,7,1.2"),
                ColumbusFusionFormat.LAYOUT_GNSS_SAT);
        assertEquals(1, positions.size());
        assertEquals(Integer.valueOf(7), positions.get(0).getSatellites());
        assertEquals(1.2, positions.get(0).getHdop(), 0.0);
        assertNull(positions.get(0).getFixQuality());
    }

    @Test
    public void testParseReservedFixColumn() throws IOException {
        List<Wgs84Position> positions = format.parseBody(
                reader("T,240115,101530,52.5200,13.4050,34.0,12.3,88.0,7,1.2,3"),
                ColumbusFusionFormat.LAYOUT_GNSS_SAT_FIX);
        assertEquals(1, positions.size());
        assertEquals(Integer.valueOf(3), positions.get(0).getFixQuality());
    }

    @Test
    public void testFixQualityAcceptsLowerBoundaryZero() throws IOException {
        List<Wgs84Position> positions = format.parseBody(
                reader("T,240115,101530,52.5200,13.4050,34.0,12.3,88.0,7,1.2,0"),
                ColumbusFusionFormat.LAYOUT_GNSS_SAT_FIX);
        assertEquals(1, positions.size());
        assertEquals(Integer.valueOf(0), positions.get(0).getFixQuality());
    }

    @Test
    public void testFixQualityAcceptsUpperBoundarySeven() throws IOException {
        List<Wgs84Position> positions = format.parseBody(
                reader("T,240115,101530,52.5200,13.4050,34.0,12.3,88.0,7,1.2,7"),
                ColumbusFusionFormat.LAYOUT_GNSS_SAT_FIX);
        assertEquals(1, positions.size());
        assertEquals(Integer.valueOf(7), positions.get(0).getFixQuality());
    }

    @Test
    public void testFixQualityRejectsOutOfDomainValue() throws IOException {
        List<Wgs84Position> positions = format.parseBody(
                reader("T,240115,101530,52.5200,13.4050,34.0,12.3,88.0,7,1.2,9"),
                ColumbusFusionFormat.LAYOUT_GNSS_SAT_FIX);
        assertEquals(1, positions.size());
        assertNull(positions.get(0).getFixQuality());
    }

    @Test
    public void testCarryForwardTagAndDate() throws IOException {
        List<Wgs84Position> positions = format.parseBody(reader(
                "T,240115,101530,52.5200,13.4050,34.0,12.3,88.0",
                ",,101531,52.5201,13.4051,34.1,12.4,88.1"
        ), ColumbusFusionFormat.LAYOUT_GNSS);
        assertEquals(2, positions.size());
        assertEquals(WaypointType.Waypoint, positions.get(1).getWaypointType());
        assertEquals(format.formatDate(positions.get(0).getTime()), format.formatDate(positions.get(1).getTime()));
    }

    @Test
    public void testNegativeCoordinates() {
        Wgs84Position position = format.parsePosition("T,240115,101530,-33.8688,-70.9,10.0,5.0,270.0", null);
        assertEquals(-33.8688, position.getLatitude(), 0.0);
        assertEquals(-70.9, position.getLongitude(), 0.0);
    }

    @Test
    public void testDoesNotMatchType1OrType2Line() {
        String hemisphereStyleLine = "T,150124,101530,N,52.31200,E,13.24300,34.0,12.3,88.0";
        assertFalse(format.isPosition(hemisphereStyleLine));
        assertNull(format.parsePosition(hemisphereStyleLine, null));
    }

    @Test
    public void testReadMultiRowFile() throws IOException {
        BufferedReader bufferedReader = reader(
                "# Format=ColumbusFusion; Version=1.0; Type=GNSS",
                "tag,date,time,lat,lon,alt,speed,heading",
                "T,240115,101530,52.5200,13.4050,34.0,12.3,88.0",
                "C,240115,101540,52.5210,13.4060,35.0,0.0,0.0",
                ",,101550,52.5220,13.4070,36.0,5.0,10.0"
        );

        assertTrue(format.isValidLine(bufferedReader.readLine()));
        int layout = format.detectLayout(bufferedReader.readLine());
        assertEquals(ColumbusFusionFormat.LAYOUT_GNSS, layout);

        List<Wgs84Position> positions = format.parseBody(bufferedReader, layout);
        assertEquals(3, positions.size());
        assertEquals(WaypointType.Waypoint, positions.get(0).getWaypointType());
        assertEquals(WaypointType.PointOfInterestC, positions.get(1).getWaypointType());
        assertEquals(WaypointType.PointOfInterestC, positions.get(2).getWaypointType());
    }

    @Test
    public void testParseSixDigitDate() throws Exception {
        ignoreLocalTimeZone(() -> {
            Wgs84Position position = format.parsePosition("T,240115,101530,52.5200,13.4050,34.0,12.3,88.0", null);
            assertEquals("240115", format.formatDate(position.getTime()));
            assertEquals("101530", format.formatTime(position.getTime()));
        });
    }

    // --- writing ---

    private String write(Wgs84Position position) {
        StringWriter output = new StringWriter();
        PrintWriter writer = new PrintWriter(output);
        format.writePosition(position, writer, 0, true);
        writer.flush();
        return output.toString().trim();
    }

    @Test
    public void testWriteHeaderUsesWidestLayout() {
        String[] headerLines = format.getHeader().split("\n");
        assertEquals(2, headerLines.length);
        assertTrue(format.isValidLine(headerLines[0]));
        assertEquals(ColumbusFusionFormat.LAYOUT_GNSS_SAT_FIX, format.detectLayout(headerLines[1]));
    }

    @Test
    public void testWritePositionRoundtrip() throws Exception {
        ignoreLocalTimeZone(() -> {
            Wgs84Position position = format.parsePosition("T,240115,101530,52.52,13.405,34.0,12.3,88.0,7,1.2,3", null);

            String line = write(position);
            assertEquals("T,240115,101530,52.52,13.405,34.0,12.3,88.0,7,1.2,3", line);
            assertTrue(format.isPosition(line));

            Wgs84Position reread = format.parsePosition(line, null);
            assertEquals(position.getLatitude(), reread.getLatitude());
            assertEquals(position.getLongitude(), reread.getLongitude());
            assertEquals(position.getElevation(), reread.getElevation());
            assertEquals(position.getSpeed(), reread.getSpeed());
            assertEquals(position.getHeading(), reread.getHeading());
            assertEquals(position.getSatellites(), reread.getSatellites());
            assertEquals(position.getHdop(), reread.getHdop());
            assertEquals(position.getFixQuality(), reread.getFixQuality());
            assertEquals(position.getTime().getTimeInMillis(), reread.getTime().getTimeInMillis());
        });
    }

    @Test
    public void testWriteNegativeCoordinates() {
        Wgs84Position position = format.parsePosition("T,240115,101530,-33.8688,-70.9,10.0,5.0,270.0", null);

        String line = write(position);
        assertTrue(line.contains(",-33.8688,-70.9,"));
        assertTrue(format.isPosition(line));
    }

    @Test
    public void testWriteLeavesUnknownColumnsEmpty() {
        Wgs84Position position = new Wgs84Position(13.405, 52.52, null, null, null, null);

        String line = write(position);
        assertEquals("T,,000000,52.52,13.405,,,,,,", line);
        assertTrue(format.isPosition(line));

        Wgs84Position reread = format.parsePosition(line, null);
        assertNull(reread.getElevation());
        assertNull(reread.getSpeed());
        assertNull(reread.getHeading());
        assertNull(reread.getSatellites());
        assertNull(reread.getHdop());
        assertNull(reread.getFixQuality());
        assertNull(reread.getTime());
    }

    @Test
    public void testWriteFallsBackToTrackTagForUnsupportedWaypointTypes() {
        Wgs84Position position = new Wgs84Position(13.405, 52.52, null, null, null, "VOX00014.wav");
        position.setWaypointType(WaypointType.Voice);

        String line = write(position);
        assertTrue(line.startsWith("T,"));
        assertTrue(format.isPosition(line));
    }

    @Test
    public void testWritePreservesSupportedTags() {
        for (WaypointType waypointType : new WaypointType[]{WaypointType.PointOfInterestC,
                WaypointType.PointOfInterestD, WaypointType.Parking, WaypointType.Waypoint}) {
            Wgs84Position position = new Wgs84Position(13.405, 52.52, null, null, null, null);
            position.setWaypointType(waypointType);

            String line = write(position);
            assertTrue(line.startsWith(waypointType.value() + ","));
            assertEquals(waypointType, format.parsePosition(line, null).getWaypointType());
        }
    }

    @Test
    public void testWriteInvertsDeviceLocalTimeZoneConversion() {
        boolean useLocalTimeZone = getUseLocalTimeZone();
        String timeZone = getTimeZone();
        try {
            setUseLocalTimeZone(true);
            setTimeZone("Europe/Berlin");

            // 240115 101530 is device-local time, read as UTC-shifted, so writing must shift back
            Wgs84Position position = format.parsePosition("T,240115,101530,52.52,13.405,34.0,12.3,88.0", null);
            assertTrue(write(position).startsWith("T,240115,101530,"));
        } finally {
            setUseLocalTimeZone(useLocalTimeZone);
            setTimeZone(timeZone);
        }
    }


    // --- writing positions that have no coordinates ---

    private String writeRoute(List<Wgs84Position> positions) {
        StringWriter output = new StringWriter();
        PrintWriter writer = new PrintWriter(output);
        format.writeHeader(writer, format.createRoute(RouteCharacteristics.Track, null, positions));
        for (Wgs84Position position : positions)
            format.writePosition(position, writer, 0, true);
        writer.flush();
        return output.toString();
    }

    private List<Wgs84Position> reread(String written) throws IOException {
        BufferedReader reader = new BufferedReader(new StringReader(written));
        reader.readLine();
        return format.parseBody(reader, format.detectLayout(reader.readLine()));
    }

    @Test
    public void testWriteImuPositionsDoesNotInventCoordinates() throws Exception {
        ignoreLocalTimeZone(() -> {
            List<Wgs84Position> positions = format.parseBody(reader(
                    "T,260709,042922,-0.02,0.04,1.09",
                    ",,042923,-0.04,0.06,1.09"
            ), ColumbusFusionFormat.LAYOUT_IMU);

            String written = writeRoute(positions);
            // a GNSS layout would have to write these as 0.0 - see detectWriteLayout
            assertFalse(written.contains("0.0,0.0"));
            assertEquals(ColumbusFusionFormat.LAYOUT_IMU,
                    format.detectLayout(written.split("\n")[1]));

            List<Wgs84Position> back = reread(written);
            assertEquals(positions.size(), back.size());
            for (int i = 0; i < positions.size(); i++) {
                assertNull(back.get(i).getLatitude());
                assertNull(back.get(i).getLongitude());
                assertEquals(positions.get(i).getAccelerationX(), back.get(i).getAccelerationX());
                assertEquals(positions.get(i).getAccelerationY(), back.get(i).getAccelerationY());
                assertEquals(positions.get(i).getAccelerationZ(), back.get(i).getAccelerationZ());
                assertEquals(positions.get(i).getTime(), back.get(i).getTime());
            }
        });
    }

    @Test
    public void testWriteMixedPositionsKeepsBothCoordinatesAndAcceleration() throws Exception {
        ignoreLocalTimeZone(() -> {
            List<Wgs84Position> positions = format.parseBody(reader(
                    "T,260709,042922,26.0983295,119.2648235,18.9,3.4,120.2,-0.02,0.04,1.09",
                    "T,260709,042923,,,,,,-0.04,0.06,1.09"
            ), ColumbusFusionFormat.LAYOUT_GNSS_IMU);
            assertEquals(2, positions.size());

            String written = writeRoute(positions);
            assertEquals(ColumbusFusionFormat.LAYOUT_GNSS_IMU,
                    format.detectLayout(written.split("\n")[1]));

            List<Wgs84Position> back = reread(written);
            assertEquals(2, back.size());
            assertEquals(positions.get(0).getLatitude(), back.get(0).getLatitude());
            assertEquals(positions.get(0).getLongitude(), back.get(0).getLongitude());
            assertNull(back.get(1).getLatitude());
            assertNull(back.get(1).getLongitude());
            for (int i = 0; i < 2; i++)
                assertEquals(positions.get(i).getAccelerationZ(), back.get(i).getAccelerationZ());
        });
    }

    @Test
    public void testWriteKeepsWidestGnssLayoutWhenEveryPositionHasCoordinates() throws Exception {
        ignoreLocalTimeZone(() -> {
            List<Wgs84Position> positions = format.parseBody(reader(
                    "T,240115,101530,52.52,13.405,34.0,12.3,88.0,7,1.2,3"
            ), ColumbusFusionFormat.LAYOUT_GNSS_SAT_FIX);

            String written = writeRoute(positions);
            assertEquals(ColumbusFusionFormat.LAYOUT_GNSS_SAT_FIX,
                    format.detectLayout(written.split("\n")[1]));
            assertTrue(written.contains("T,240115,101530,52.52,13.405,34.0,12.3,88.0,7,1.2,3"));
        });
    }
}
