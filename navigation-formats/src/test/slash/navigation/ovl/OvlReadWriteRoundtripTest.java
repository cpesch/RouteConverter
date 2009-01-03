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

package slash.navigation.ovl;

import slash.navigation.NavigationFileParser;
import slash.navigation.ReadWriteBase;

import java.io.IOException;

public class OvlReadWriteRoundtripTest extends ReadWriteBase {
    private void checkUnprocessedValue(OvlSection section, String name, String value) {
        assertNotNull(section);
        assertEquals(value, section.get(name));
    }

    public void testOvlReadWriteRoundtrip() throws IOException {
        readWriteRoundtrip(TEST_PATH + "from.ovl", new NavigationFileParserCallback() {
            public void test(NavigationFileParser source, NavigationFileParser target) {
                OvlRoute sourceRoute = (OvlRoute) source.getAllRoutes().get(0);
                checkUnprocessedValue(sourceRoute.getSymbol(), "Extra", "1");
                checkUnprocessedValue(sourceRoute.getOverlay(), "Plus", "2");
                checkUnprocessedValue(sourceRoute.getMapLage(), "Encore", "3");
                OvlRoute targetRoute = (OvlRoute) target.getAllRoutes().get(0);
                checkUnprocessedValue(targetRoute.getSymbol(), "Extra", "1");
                checkUnprocessedValue(targetRoute.getOverlay(), "Plus", "2");
                checkUnprocessedValue(targetRoute.getMapLage(), "Encore", "3");
            }
        });
    }
}
