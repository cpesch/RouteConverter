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

package slash.navigation.kml;

import slash.navigation.base.NavigationTestCase;

public class Wbt201LogTest extends NavigationTestCase {
    private Kml20Format format = new Kml20Format();
    private static final String WBT201LOG_DESCRIPTION = "<description><![CDATA[\n" +
            "\t\t\t\t\t<br>Lat.=7.5878249&deg;</br><br>Long.=51.1295766&deg;</br><br>Alt.=390m (1279ft)</br><br>Speed=4Km/h (2Mile/h)</br><br>Course=270&deg;</br><br>Elapsed Time: +00:00:11</br><br>Total distance: 0.02Km (0.01Mile)</br>\n" +
            "\t\t\t\t]]></description>";
    private static final String WBT201LOG_DESCRIPTION2 = "<descripti6&deg;</br><br>Alt.=390m (1279ft)</br><br>Speed=4Km/h (2ion>";

    public void testParseSpeed() {
        assertEquals(4.0, format.parseSpeed(WBT201LOG_DESCRIPTION));
        assertEquals(4.0, format.parseSpeed(WBT201LOG_DESCRIPTION2));
    }
}