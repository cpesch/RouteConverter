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

package slash.navigation.bcr;

import org.junit.Test;
import slash.navigation.base.ParserResult;
import slash.navigation.base.ReadWriteTestCallback;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static slash.navigation.base.NavigationTestCase.TEST_PATH;
import static slash.navigation.base.ReadWriteBase.readWriteRoundtrip;
import static slash.navigation.bcr.BcrFormat.CLIENT_TITLE;
import static slash.navigation.bcr.BcrFormat.COORDINATES_TITLE;
import static slash.navigation.bcr.BcrFormat.DESCRIPTION_TITLE;
import static slash.navigation.bcr.BcrFormat.ROUTE_TITLE;

public class BcrReadWriteRoundtripIT {
    private void checkUnprocessedValue(BcrRoute route, String section, String name, String value) {
        BcrSection bs = route.findSection(section);
        assertNotNull(bs);
        assertEquals(value, bs.get(name));
    }

    @Test
    public void testMotorradTourenplanerRoundtrip() throws IOException {
        readWriteRoundtrip(TEST_PATH + "from-mtp0809.bcr", new ReadWriteTestCallback() {
            public void test(ParserResult source, ParserResult target) {
                BcrRoute sourceRoute = (BcrRoute) source.getAllRoutes().get(0);
                checkUnprocessedValue(sourceRoute, CLIENT_TITLE, "EXTRA", "1");
                checkUnprocessedValue(sourceRoute, COORDINATES_TITLE, "PLUS", "2");
                checkUnprocessedValue(sourceRoute, DESCRIPTION_TITLE, "ENCORE", "3");
                checkUnprocessedValue(sourceRoute, ROUTE_TITLE, "CORRUSED", "0");
                BcrRoute targetRoute = (BcrRoute) target.getAllRoutes().get(0);
                checkUnprocessedValue(targetRoute, CLIENT_TITLE, "EXTRA", "1");
                checkUnprocessedValue(targetRoute, COORDINATES_TITLE, "PLUS", "2");
                checkUnprocessedValue(targetRoute, DESCRIPTION_TITLE, "ENCORE", "3");
                checkUnprocessedValue(targetRoute, ROUTE_TITLE, "CORRUSED", "0");
            }
        });
    }

    @Test
    public void testMapAndGuideIntranetRoundtrip() throws IOException {
        readWriteRoundtrip(TEST_PATH + "from-mgintra09.bcr", new ReadWriteTestCallback() {
            public void test(ParserResult source, ParserResult target) {
                BcrRoute sourceRoute = (BcrRoute) source.getAllRoutes().get(0);
                checkUnprocessedValue(sourceRoute, "STAYTIME", "STATION1", "0");
                checkUnprocessedValue(sourceRoute, "STAYTIME", "STATION2", "1");
                BcrRoute targetRoute = (BcrRoute) target.getAllRoutes().get(0);
                checkUnprocessedValue(targetRoute, "STAYTIME", "STATION1", "0");
                checkUnprocessedValue(targetRoute, "STAYTIME", "STATION2", "1");
            }
        });
    }
}
