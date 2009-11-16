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

package slash.navigation;

import slash.navigation.gpx.Gpx10Format;

public class BaseNavigationFormatTest extends NavigationTestCase {
    Gpx10Format format = new Gpx10Format();

    private void check(String name, String desc, String expectedComment, String expectedName, String expectedDesc) {
        String comment = format.asComment(name, desc);
        assertEquals(expectedComment, comment);
        String actualName = format.asName(comment);
        assertEquals(expectedName, actualName);
        String actualDesc = format.asDesc(comment, desc);
        assertEquals(expectedDesc, actualDesc);
    }

    private void check(String name, String desc, String expectedComment) {
        check(name, desc, expectedComment, name, desc);
    }

    public void testNameAndDescRoundtrip() {
        check("name", "description", "name; description");
    }

    public void testNameRoundtrip() {
        check("name", null, "name");
    }

    public void testDescRoundtrip() {
        check(null, "description", "description", "description", null);
    }

    public void testNameWithSemicolonRoundtrip() {
        check("name; description", null, "name; description", "name", "description");
    }

    public void testNameWithoutSemicolonAndDescription() {
        assertEquals(null, format.asDesc("name", "desc"));
    }
}