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
package slash.navigation.tcx;

import org.junit.Test;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;

public class TcxFormatTest {
    private TcxFormat format = new Tcx2Format();

    @Test
    public void testCreateUniqueRouteName() {
        assertEquals("a", format.createUniqueRouteName("a", Collections.<String>emptySet()));
        assertEquals("abcdefghijklmno", format.createUniqueRouteName("abcdefghijklmnopqrstuvwxyz", Collections.<String>emptySet()));

        Set<String> routeNames = new HashSet<>();
        routeNames.add("abcdefghijklmno");
        assertEquals("abcdefghijk (2)", format.createUniqueRouteName("abcdefghijklmnopqrstuvwxyz", routeNames));

        routeNames.add("abcdefghijk (2)");
        routeNames.add("abcdefghijk (3)");
        assertEquals("abcdefghijk (4)", format.createUniqueRouteName("abcdefghijklmnopqrstuvwxyz", routeNames));
    }
}
