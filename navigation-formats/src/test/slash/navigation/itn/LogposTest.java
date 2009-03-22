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

import slash.navigation.NavigationTestCase;
import slash.navigation.RouteCharacteristics;

import java.io.IOException;

public class LogposTest extends NavigationTestCase {

    public void testLogposPosition() {
        TomTomPosition position = new TomTomPosition(0, 0, "080530 08:11:44: + Neuhaus Im Solling (Holzminden); B497 In Der Fahrt; 3  (s=69 d=207)");
        assertEquals("s=69 d=207", position.getReason());
        assertEquals("Neuhaus Im Solling (Holzminden); B497 In Der Fahrt; 3", position.getCity());
        assertEquals(69.0, position.getSpeed());
        assertNull(position.getElevation());
        assertEquals(calendar(2008, 5, 30, 8, 11, 44), position.getTime());
    }

    private void readFiles(String extension, int routeCount, boolean expectElevation, boolean expectTime, RouteCharacteristics... characteristics) throws IOException {
        readFiles("logpos", extension, routeCount, expectElevation, expectTime, characteristics);
    }

    public void testAllLogposTracks() throws IOException {
        readFiles(".itn", 1, false, true, RouteCharacteristics.Track);
    }
}