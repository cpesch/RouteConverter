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

import slash.navigation.itn.ItnPosition;
import slash.navigation.NavigationTestCase;
import slash.navigation.RouteCharacteristics;

import java.io.IOException;

public class PilogTest extends NavigationTestCase {

    public void testPilogItnPosition() {
        ItnPosition position = new ItnPosition(0, 0, "080629 07:33:00: + Eschelbach (Rhein-Neckar-Kreis, Baden-Württemberg); L612 @196.9m (s=66 d=91)");
        assertEquals("s=66 d=91", position.getReason());
        assertEquals("Eschelbach (Rhein-Neckar-Kreis, Baden-Württemberg); L612", position.getCity());
        assertEquals(196.9, position.getElevation());
        assertEquals(calendar(2008, 6, 29, 7, 33, 0), position.getTime());
    }

    private void readFiles(String extension, int routeCount, boolean expectElevation, boolean expectTime, RouteCharacteristics... characteristics) throws IOException {
        readFiles("pilog", extension, routeCount, expectElevation, expectTime, characteristics);
    }

    public void testAllPilogItnTracks() throws IOException {
        readFiles(".itn", 1, true, true, RouteCharacteristics.Track);
    }
}