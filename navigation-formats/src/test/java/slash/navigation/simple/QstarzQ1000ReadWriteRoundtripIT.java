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

package slash.navigation.simple;

import org.junit.Test;
import slash.navigation.base.ParserResult;
import slash.navigation.base.ReadWriteTestCallback;
import slash.navigation.base.SimpleRoute;
import slash.navigation.base.Wgs84Position;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static slash.common.TestCase.assertDoubleEquals;
import static slash.common.io.Transfer.roundFraction;
import static slash.navigation.base.NavigationTestCase.TEST_PATH;
import static slash.navigation.base.ReadWriteBase.readWriteRoundtrip;

public class QstarzQ1000ReadWriteRoundtripIT {

    @Test
    public void testRoundtrip() throws IOException {
        readWriteRoundtrip(TEST_PATH + "from-qstarz-q1000.csv", new ReadWriteTestCallback() {
            public void test(ParserResult source, ParserResult target) {
                SimpleRoute sourceRoute = (SimpleRoute) source.getAllRoutes().get(0);
                SimpleRoute targetRoute = (SimpleRoute) target.getAllRoutes().get(0);
                for(int i=0; i < sourceRoute.getPositionCount(); i++) {
                    Wgs84Position sourcePosition = (Wgs84Position) sourceRoute.getPosition(i);
                    Wgs84Position targetPosition= (Wgs84Position) targetRoute.getPosition(i);
                    assertDoubleEquals(roundFraction(targetPosition.getElevation(), 0), roundFraction(sourcePosition.getElevation(), 0));
                    assertDoubleEquals(roundFraction(targetPosition.getSpeed(), 1), roundFraction(sourcePosition.getSpeed(), 1));
                    assertEquals(targetPosition.getHdop(), sourcePosition.getHdop());
                    assertEquals(targetPosition.getSatellites(), sourcePosition.getSatellites());
                }
            }
        });
    }
}