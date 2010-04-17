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

public class TavellogTest extends NavigationTestCase {
    private Kml20Format format = new Kml20Format();
    private static final String TAVELLOG_DESCRIPTION = "<description><![CDATA[<html><body>Time: 2009/02/07 21:45:55<BR>Altitude: 62.20<BR>Speed: 15.37<BR></body></html>]]></description>";

    public void testParseTime() {
        assertEquals(calendar(2009, 2, 7, 21, 45, 55), format.parseTime(TAVELLOG_DESCRIPTION));
    }

    public void testParseSpeed() {
        assertEquals(15.37, format.parseSpeed(TAVELLOG_DESCRIPTION));
    }

    public void testParseElevation() {
        assertEquals(62.20, format.parseElevation(TAVELLOG_DESCRIPTION));
    }
}
