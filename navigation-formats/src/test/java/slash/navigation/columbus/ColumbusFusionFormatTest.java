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

import org.junit.Test;
import slash.navigation.base.WaypointType;
import slash.navigation.base.Wgs84Position;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ColumbusFusionFormatTest {
    private final ColumbusFusionFormat format = new ColumbusFusionFormat();

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
    public void testSupportsReadingOnly() {
        assertTrue(format.isSupportsReading());
        assertFalse(format.isSupportsWriting());
    }

    @Test
    public void testIsValidHeaderDirective() {
        assertTrue(format.isValidLine("# Format=ColumbusFusion; Version=1.0; Type=GNSS"));
        assertFalse(format.isValidLine("# Format=ColumbusFusion; Version=1.0; Type=GNSS+IMU"));
        assertFalse(format.isValidLine("# Format=ColumbusFusion; Version=1.0; Type=IMU"));

        assertTrue(format.isValidLine("tag,date,time,lat,lon,alt,speed,heading"));
        assertTrue(format.isValidLine("tag,date,time,lat,lon,alt,speed,heading,sat,hdop"));
        assertTrue(format.isValidLine("tag,date,time,lat,lon,alt,speed,heading,sat,hdop,fix"));

        assertEquals(ColumbusFusionFormat.LAYOUT_GNSS,
                format.detectLayout("tag,date,time,lat,lon,alt,speed,heading"));
        assertEquals(ColumbusFusionFormat.LAYOUT_GNSS_SAT,
                format.detectLayout("tag,date,time,lat,lon,alt,speed,heading,sat,hdop"));
        assertEquals(ColumbusFusionFormat.LAYOUT_GNSS_SAT_FIX,
                format.detectLayout("tag,date,time,lat,lon,alt,speed,heading,sat,hdop,fix"));
    }

    @Test
    public void testParseGnssPosition() {
        Wgs84Position position = format.parsePosition("T,20240115,101530,52.5200,13.4050,34.0,12.3,88.0", null);
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
                reader("T,20240115,101530,52.5200,13.4050,34.0,12.3,88.0,7,1.2"),
                ColumbusFusionFormat.LAYOUT_GNSS_SAT);
        assertEquals(1, positions.size());
        assertEquals(Integer.valueOf(7), positions.get(0).getSatellites());
        assertEquals(1.2, positions.get(0).getHdop(), 0.0);
        assertNull(positions.get(0).getFixQuality());
    }

    @Test
    public void testParseReservedFixColumn() throws IOException {
        List<Wgs84Position> positions = format.parseBody(
                reader("T,20240115,101530,52.5200,13.4050,34.0,12.3,88.0,7,1.2,3"),
                ColumbusFusionFormat.LAYOUT_GNSS_SAT_FIX);
        assertEquals(1, positions.size());
        assertEquals(Integer.valueOf(3), positions.get(0).getFixQuality());
    }

    @Test
    public void testCarryForwardTagAndDate() throws IOException {
        List<Wgs84Position> positions = format.parseBody(reader(
                "T,20240115,101530,52.5200,13.4050,34.0,12.3,88.0",
                ",,101531,52.5201,13.4051,34.1,12.4,88.1"
        ), ColumbusFusionFormat.LAYOUT_GNSS);
        assertEquals(2, positions.size());
        assertEquals(WaypointType.Waypoint, positions.get(1).getWaypointType());
        assertEquals(format.formatDate(positions.get(0).getTime()), format.formatDate(positions.get(1).getTime()));
    }

    @Test
    public void testNegativeCoordinates() {
        Wgs84Position position = format.parsePosition("T,20240115,101530,-33.8688,-70.9,10.0,5.0,270.0", null);
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
                "T,20240115,101530,52.5200,13.4050,34.0,12.3,88.0",
                "C,20240115,101540,52.5210,13.4060,35.0,0.0,0.0",
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
}
