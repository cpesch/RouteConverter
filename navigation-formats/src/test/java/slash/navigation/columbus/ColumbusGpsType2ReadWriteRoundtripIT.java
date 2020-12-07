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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static slash.common.TestCase.assertDoubleEquals;
import static slash.navigation.base.ConvertBase.ignoreLocalTimeZone;
import static slash.navigation.base.NavigationTestCase.TEST_PATH;
import static slash.navigation.base.ReadWriteBase.readWriteRoundtrip;

public class ColumbusGpsType2ReadWriteRoundtripIT {

    @Test
    public void testTypeARoundtrip() throws Exception {
        ignoreLocalTimeZone(() -> {
            readWriteRoundtrip(TEST_PATH + "from-columbusv1000-type2.csv", new ReadWriteTestCallback() {
                public void test(ParserResult source, ParserResult target) {
                    SimpleRoute sourceRoute = (SimpleRoute) source.getAllRoutes().get(0);
                    SimpleRoute targetRoute = (SimpleRoute) target.getAllRoutes().get(0);
                    for (int i = 0; i < sourceRoute.getPositionCount(); i++) {
                        Wgs84Position sourcePosition = (Wgs84Position) sourceRoute.getPosition(i);
                        Wgs84Position targetPosition = (Wgs84Position) targetRoute.getPosition(i);
                        assertEquals(targetPosition.getElevation(), sourcePosition.getElevation());
                        assertEquals(targetPosition.getSpeed(), sourcePosition.getSpeed());
                        assertEquals(targetPosition.getHeading(), sourcePosition.getHeading());
                        assertEquals(targetPosition.getPressure(), sourcePosition.getPressure());
                        assertEquals(targetPosition.getTemperature(), sourcePosition.getTemperature());
                        assertEquals(targetPosition.getHeartBeat(), sourcePosition.getHeartBeat());
                    }
                }
            });
        });
    }

    @Test
    public void testTypeBRoundtrip() throws Exception {
        ignoreLocalTimeZone(() -> {
            readWriteRoundtrip(TEST_PATH + "from-columbusv1000-type2b.csv", new ReadWriteTestCallback() {
                public void test(ParserResult source, ParserResult target) {
                    SimpleRoute sourceRoute = (SimpleRoute) source.getAllRoutes().get(0);
                    SimpleRoute targetRoute = (SimpleRoute) target.getAllRoutes().get(0);
                    for (int i = 0; i < sourceRoute.getPositionCount(); i++) {
                        Wgs84Position sourcePosition = (Wgs84Position) sourceRoute.getPosition(i);
                        Wgs84Position targetPosition = (Wgs84Position) targetRoute.getPosition(i);
                        assertEquals(targetPosition.getElevation(), sourcePosition.getElevation());
                        assertEquals(targetPosition.getSpeed(), sourcePosition.getSpeed());
                        assertEquals(targetPosition.getHeading(), sourcePosition.getHeading());
                        // since always Type A is written and Type A always stores at least a zero
                        assertDoubleEquals(0.0, targetPosition.getPressure());
                        assertDoubleEquals(0.0, targetPosition.getTemperature());
                        assertNull(targetPosition.getHeartBeat());
                    }
                }
            });
        });
    }
}
