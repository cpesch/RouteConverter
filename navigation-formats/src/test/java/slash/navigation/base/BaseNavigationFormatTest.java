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

package slash.navigation.base;

import org.junit.Test;
import slash.navigation.gpx.Gpx10Format;

import static org.junit.Assert.assertEquals;

public class BaseNavigationFormatTest {
    Gpx10Format format = new Gpx10Format();

    private void check(String name, String desc, String expectedComment, String expectedName, String expectedDesc, String expectedDesc2) {
        String comment = format.asComment(name, desc);
        assertEquals(expectedComment, comment);
        String actualName = format.asName(comment);
        assertEquals(expectedName, actualName);
        String actualDesc = format.asDesc(comment, desc);
        assertEquals(expectedDesc, actualDesc);
        String actualDesc2 = format.asDesc(comment);
        assertEquals(expectedDesc2, actualDesc2);
    }

    private void check(String name, String desc, String expectedComment, String expectedName, String expectedDesc) {
        check(name, desc, expectedComment, expectedName, expectedDesc, expectedDesc);
    }

    private void check(String name, String desc, String expectedComment) {
        check(name, desc, expectedComment, name, desc);
    }

    @Test
    public void nameAndDescRoundtrip() {
        check("name", "description", "name; description");
    }

    @Test
    public void nameRoundtrip() {
        check("name", null, "name");
    }

    @Test
    public void descRoundtrip() {
        check(null, "description", "description", "description", "description", null);
    }

    @Test
    public void nameWithSemicolonRoundtrip() {
        check("name; description", null, "name; description", "name", "description");
    }

    @Test
    public void nameWithoutSemicolonAndDescription() {
        assertEquals("desc", format.asDesc("name", "desc"));
        assertEquals(null, format.asDesc("name"));
    }
}