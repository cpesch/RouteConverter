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

package slash.navigation.nmn;

import org.junit.Test;
import slash.navigation.base.AllNavigationFormatRegistry;
import slash.navigation.base.NavigationFormatParser;
import slash.navigation.base.ParserResult;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static slash.navigation.base.NavigationTestCase.SAMPLE_PATH;

public class Nmn6FormatIT {
    private NavigationFormatParser parser = new NavigationFormatParser(new AllNavigationFormatRegistry());

    @Test
    public void testIsNmn6FavoritesWithValidPositionsOnly() throws IOException {
        File source = new File(SAMPLE_PATH + "Mue Goe A38-stripped.rte");
        ParserResult result = parser.read(source);
        assertNotNull(result);
        assertEquals(Nmn6Format.class, result.getFormat().getClass());
    }

    @Test
    public void testIsNmn6WithFirstValidLineButNotPosition() throws IOException {
        File source = new File(SAMPLE_PATH + "Mue Goe A38.rte");
        ParserResult result = parser.read(source);
        assertNotNull(result);
        assertEquals(Nmn6Format.class, result.getFormat().getClass());
    }
}