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

import slash.navigation.NavigationFileParser;
import slash.navigation.ReadWriteBase;

import java.io.IOException;

public class BcrReadWriteRoundtripTest extends ReadWriteBase {
    private void checkUnprocessedValue(BcrRoute route, String section, String name, String value) {
        BcrSection bs = route.findSection(section);
        assertNotNull(bs);
        assertEquals(value, bs.get(name));
    }

    public void testBcrReadWriteRoundtrip() throws IOException {
        readWriteRoundtrip(TEST_PATH + "from-mtp0809.bcr", new NavigationFileParserCallback() {
            public void test(NavigationFileParser source, NavigationFileParser target) {
                BcrRoute sourceRoute = (BcrRoute) source.getAllRoutes().get(0);
                checkUnprocessedValue(sourceRoute, BcrFormat.CLIENT_TITLE, "EXTRA", "1");
                checkUnprocessedValue(sourceRoute, BcrFormat.COORDINATES_TITLE, "PLUS", "2");
                checkUnprocessedValue(sourceRoute, BcrFormat.DESCRIPTION_TITLE, "ENCORE", "3");
                checkUnprocessedValue(sourceRoute, BcrFormat.ROUTE_TITLE, "CORRUSED", "0");
                BcrRoute targetRoute = (BcrRoute) target.getAllRoutes().get(0);
                checkUnprocessedValue(targetRoute, BcrFormat.CLIENT_TITLE, "EXTRA", "1");
                checkUnprocessedValue(targetRoute, BcrFormat.COORDINATES_TITLE, "PLUS", "2");
                checkUnprocessedValue(targetRoute, BcrFormat.DESCRIPTION_TITLE, "ENCORE", "3");
                checkUnprocessedValue(targetRoute, BcrFormat.ROUTE_TITLE, "CORRUSED", "0");
            }
        });
    }
}
