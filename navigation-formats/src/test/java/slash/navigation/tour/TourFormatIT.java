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

package slash.navigation.tour;

import org.junit.Test;
import slash.navigation.base.ParserContext;
import slash.navigation.base.ParserContextImpl;

import java.io.FileInputStream;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static slash.navigation.base.NavigationTestCase.TEST_PATH;

public class TourFormatIT {
    private TourFormat format = new TourFormat();

    @Test
    public void testPositionInListOrder() throws Exception {
        ParserContext<TourRoute> context = new ParserContextImpl<>();
        format.read(new FileInputStream(TEST_PATH + "from.tour"), context);
        List<TourRoute> routeList = context.getRoutes();
        assertEquals(1, routeList.size());
        TourRoute route = routeList.get(0);
        assertEquals("10787 Berlin, Hardenbergstra\u00dfe 8, Zoologischer Garten", route.getPosition(0).getDescription());
        assertEquals("10117 Berlin/Mitte, Platz Vor Dem Brandenburger Tor 1, Home", route.getPosition(1).getDescription());
        assertEquals("10789 Berlin, Breitscheidplatz, Kaiser-Wilhelm-Ged\u00e4chtniskirche", route.getPosition(2).getDescription());
    }
}