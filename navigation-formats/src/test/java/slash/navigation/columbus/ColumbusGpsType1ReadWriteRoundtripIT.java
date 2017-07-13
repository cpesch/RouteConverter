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

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static slash.navigation.base.NavigationTestCase.TEST_PATH;
import static slash.navigation.base.ReadWriteBase.readWriteRoundtrip;

public class ColumbusGpsType1ReadWriteRoundtripIT {

    @Test
    public void testTypeARoundtrip() throws IOException {
        readWriteRoundtrip(TEST_PATH + "from-columbusv900-professional.csv", new ReadWriteTestCallback() {
            public void test(ParserResult source, ParserResult target) {
                SimpleRoute sourceRoute = (SimpleRoute) source.getAllRoutes().get(0);
                SimpleRoute targetRoute = (SimpleRoute) target.getAllRoutes().get(0);
                for(int i=0; i < sourceRoute.getPositionCount(); i++) {
                    Wgs84Position sourcePosition = (Wgs84Position) sourceRoute.getPosition(i);
                    Wgs84Position targetPosition= (Wgs84Position) targetRoute.getPosition(i);
                    assertEquals(targetPosition.getHeading(), sourcePosition.getHeading());
                    assertEquals(targetPosition.getHdop(), sourcePosition.getHdop());
                    assertEquals(targetPosition.getVdop(), sourcePosition.getVdop());
                    assertEquals(targetPosition.getPdop(), sourcePosition.getPdop());
                }
            }
        });
    }

    @Test
    public void testTypeBRoundtrip() throws IOException {
        readWriteRoundtrip(TEST_PATH + "from-columbusv900-standard.csv", new ReadWriteTestCallback() {
            public void test(ParserResult source, ParserResult target) {
                SimpleRoute sourceRoute = (SimpleRoute) source.getAllRoutes().get(0);
                SimpleRoute targetRoute = (SimpleRoute) target.getAllRoutes().get(0);
                for (int i = 0; i < sourceRoute.getPositionCount(); i++) {
                    Wgs84Position sourcePosition = (Wgs84Position) sourceRoute.getPosition(i);
                    Wgs84Position targetPosition = (Wgs84Position) targetRoute.getPosition(i);
                    assertEquals(targetPosition.getHeading(), sourcePosition.getHeading());
                    assertNull(sourcePosition.getHdop());
                    assertNull(targetPosition.getHdop());
                    assertNull(targetPosition.getVdop());
                    assertNull(targetPosition.getPdop());
                }
            }
        });
    }
}