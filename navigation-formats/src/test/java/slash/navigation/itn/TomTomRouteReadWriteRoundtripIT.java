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

package slash.navigation.itn;

import org.junit.Test;
import slash.navigation.base.BaseNavigationFormat;
import slash.navigation.base.BaseNavigationPosition;
import slash.navigation.base.BaseRoute;
import slash.navigation.base.ParserResult;
import slash.navigation.base.ReadWriteTestCallback;

import java.io.IOException;

import static slash.navigation.base.NavigationTestCase.TEST_PATH;
import static slash.navigation.base.ReadWriteBase.readWriteRoundtrip;
import static slash.navigation.itn.TomTomRouteFormatIT.checkPlaceNamesWithUmlauts;
import static slash.navigation.itn.TomTomRouteFormatIT.checkUmlauts;

public class TomTomRouteReadWriteRoundtripIT {
    @Test
    public void testTomTom5Roundtrip() throws IOException {
        readWriteRoundtrip(TEST_PATH + "from5.itn", new ReadWriteTestCallback() {
            public void test(ParserResult source, ParserResult target) {
                BaseRoute<BaseNavigationPosition, BaseNavigationFormat> sourceRoute = source.getTheRoute();
                checkUmlauts(sourceRoute);

                BaseRoute<BaseNavigationPosition, BaseNavigationFormat> targetRoute = target.getTheRoute();
                checkUmlauts(targetRoute);
            }
        });
    }

    @Test
    public void testTomTom8Roundtrip() throws IOException {
        readWriteRoundtrip(TEST_PATH + "from8.itn", new ReadWriteTestCallback() {
            public void test(ParserResult source, ParserResult target) {
                BaseRoute<BaseNavigationPosition, BaseNavigationFormat> sourceRoute = source.getTheRoute();
                checkUmlauts(sourceRoute);

                BaseRoute<BaseNavigationPosition, BaseNavigationFormat> targetRoute = target.getTheRoute();
                checkUmlauts(targetRoute);
            }
        });
    }

    @Test
    public void testRider2Roundtrip() throws IOException {
        readWriteRoundtrip(TEST_PATH + "from-rider-2.itn", new ReadWriteTestCallback() {
            public void test(ParserResult source, ParserResult target) {
                BaseRoute<BaseNavigationPosition, BaseNavigationFormat> sourceRoute = source.getTheRoute();
                checkPlaceNamesWithUmlauts(sourceRoute);

                BaseRoute<BaseNavigationPosition, BaseNavigationFormat> targetRoute = target.getTheRoute();
                checkPlaceNamesWithUmlauts(targetRoute);
            }
        });
    }

    @Test
    public void testUrbanRiderRoundtrip() throws IOException {
        readWriteRoundtrip(TEST_PATH + "from-urban-rider.itn", new ReadWriteTestCallback() {
            public void test(ParserResult source, ParserResult target) {
                BaseRoute<BaseNavigationPosition, BaseNavigationFormat> sourceRoute = source.getTheRoute();
                checkPlaceNamesWithUmlauts(sourceRoute);

                BaseRoute<BaseNavigationPosition, BaseNavigationFormat> targetRoute = target.getTheRoute();
                checkPlaceNamesWithUmlauts(targetRoute);
            }
        });
    }
}
