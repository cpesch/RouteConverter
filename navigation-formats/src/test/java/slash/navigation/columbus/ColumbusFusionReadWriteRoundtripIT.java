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

package slash.navigation.columbus;

import org.junit.Test;
import slash.navigation.base.ParserResult;
import slash.navigation.base.ReadWriteTestCallback;
import slash.navigation.base.SimpleRoute;
import slash.navigation.base.Wgs84Position;

import java.io.File;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assume.assumeTrue;
import static slash.navigation.base.ConvertBase.ignoreLocalTimeZone;
import static slash.navigation.base.NavigationTestCase.TEST_PATH;
import static slash.navigation.base.ReadWriteBase.readWriteRoundtrip;

public class ColumbusFusionReadWriteRoundtripIT {

    private void roundtrip(String testFileName, boolean expectSatellitesAndHdop, boolean expectFixQuality) throws Exception {
        File file = new File(TEST_PATH + testFileName);
        assumeTrue("Sample file not found: " + file.getAbsolutePath(), file.exists());

        ignoreLocalTimeZone(() -> {
            readWriteRoundtrip(TEST_PATH + testFileName, new ReadWriteTestCallback() {
                public void test(ParserResult source, ParserResult target) {
                    SimpleRoute sourceRoute = (SimpleRoute) source.getAllRoutes().get(0);
                    SimpleRoute targetRoute = (SimpleRoute) target.getAllRoutes().get(0);
                    assertEquals(sourceRoute.getPositionCount(), targetRoute.getPositionCount());

                    for (int i = 0; i < sourceRoute.getPositionCount(); i++) {
                        Wgs84Position sourcePosition = (Wgs84Position) sourceRoute.getPosition(i);
                        Wgs84Position targetPosition = (Wgs84Position) targetRoute.getPosition(i);
                        assertEquals(sourcePosition.getLatitude(), targetPosition.getLatitude());
                        assertEquals(sourcePosition.getLongitude(), targetPosition.getLongitude());
                        assertEquals(sourcePosition.getElevation(), targetPosition.getElevation());
                        assertEquals(sourcePosition.getSpeed(), targetPosition.getSpeed());
                        assertEquals(sourcePosition.getHeading(), targetPosition.getHeading());
                        assertEquals(sourcePosition.getTime(), targetPosition.getTime());
                        assertEquals(sourcePosition.getWaypointType(), targetPosition.getWaypointType());

                        if (expectSatellitesAndHdop) {
                            assertEquals(sourcePosition.getSatellites(), targetPosition.getSatellites());
                            assertEquals(sourcePosition.getHdop(), targetPosition.getHdop());
                        } else {
                            assertNull(targetPosition.getSatellites());
                            assertNull(targetPosition.getHdop());
                        }

                        if (expectFixQuality)
                            assertEquals(sourcePosition.getFixQuality(), targetPosition.getFixQuality());
                        else
                            assertNull(targetPosition.getFixQuality());
                    }
                }
            });
        });
    }

    @Test
    public void testGnssRoundtrip() throws Exception {
        roundtrip("from-columbusfusion-gnss.csv", false, false);
    }

    @Test
    public void testGnssFiveSecondsRoundtrip() throws Exception {
        roundtrip("from-columbusfusion-gnss-5s.csv", false, false);
    }

    @Test
    public void testGnssSatRoundtrip() throws Exception {
        roundtrip("from-columbusfusion-gnss-sat.csv", true, false);
    }

    @Test
    public void testGnssSatFixRoundtrip() throws Exception {
        roundtrip("from-columbusfusion-gnss-sat-fix.csv", true, true);
    }
}
