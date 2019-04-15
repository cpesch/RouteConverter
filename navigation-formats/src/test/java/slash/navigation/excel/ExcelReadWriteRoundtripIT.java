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

package slash.navigation.excel;

import org.junit.Test;
import slash.navigation.base.ParserResult;
import slash.navigation.base.ReadWriteTestCallback;

import static org.junit.Assert.assertEquals;
import static slash.navigation.base.ConvertBase.ignoreLocalTimeZone;
import static slash.navigation.base.NavigationTestCase.TEST_PATH;
import static slash.navigation.base.ReadWriteBase.readWriteRoundtrip;

public class ExcelReadWriteRoundtripIT {

    private void checkRoutes(ExcelRoute sourceRoute, ExcelRoute targetRoute) {
        for (int i = 0; i < sourceRoute.getPositionCount(); i++) {
            ExcelPosition sourcePosition = sourceRoute.getPosition(i);
            ExcelPosition targetPosition = targetRoute.getPosition(i);
            assertEquals(targetPosition.getLongitude(), sourcePosition.getLongitude());
            assertEquals(targetPosition.getLatitude(), sourcePosition.getLatitude());
            assertEquals(targetPosition.getElevation(), sourcePosition.getElevation());
            assertEquals(targetPosition.getSpeed(), sourcePosition.getSpeed());
            assertEquals(targetPosition.getTime(), sourcePosition.getTime());
            assertEquals(targetPosition.getDescription(), sourcePosition.getDescription());
        }
    }

    @Test
    public void testXlsRoundtrip() throws Exception {
        ignoreLocalTimeZone(() -> {
            readWriteRoundtrip(TEST_PATH + "from.xls", new ReadWriteTestCallback() {
                public void test(ParserResult source, ParserResult target) {
                    ExcelRoute sourceRoute = (ExcelRoute) source.getAllRoutes().get(0);
                    ExcelRoute targetRoute = (ExcelRoute) target.getAllRoutes().get(0);
                    checkRoutes(sourceRoute, targetRoute);
                }
            });
        });
    }

    @Test
    public void testXlsxRoundtrip() throws Exception {
        ignoreLocalTimeZone(() -> {
            readWriteRoundtrip(TEST_PATH + "from.xlsx", new ReadWriteTestCallback() {
                public void test(ParserResult source, ParserResult target) {
                    ExcelRoute sourceRoute = (ExcelRoute) source.getAllRoutes().get(0);
                    ExcelRoute targetRoute = (ExcelRoute) target.getAllRoutes().get(0);
                    checkRoutes(sourceRoute, targetRoute);
                }
            });
        });
    }
}