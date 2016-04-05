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

package slash.navigation.mm;

import slash.navigation.base.NavigationTestCase;
import slash.navigation.base.ParserContextImpl;
import slash.navigation.base.Wgs84Position;

public class MagicMaps2GoFormatTest extends NavigationTestCase {
    MagicMaps2GoFormat format = new MagicMaps2GoFormat();

    public void testIsValidLine() {
        assertTrue(format.isPosition("52.4135141 13.3115464 40.8000000 31.05.09 07:05:58"));
    }

    public void testParsePosition() {
        Wgs84Position position = format.parsePosition("52.4135141 13.3115464 40.8000000 31.05.09 07:05:58", new ParserContextImpl());
        assertNotNull(position);
        assertNearBy(13.3115464, position.getLongitude());
        assertNearBy(52.4135141, position.getLatitude());
        assertEquals(40.8000000, position.getElevation());
        assertEquals(calendar(2009, 5, 31, 7, 5, 58).getTime(), position.getTime().getTime());
        assertNull(position.getDescription());
    }
}