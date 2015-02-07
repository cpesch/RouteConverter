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
package slash.navigation.download.tools.helpers;

import org.junit.Test;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;

public class AnchorFilterTest {
    private AnchorFilter filter = new AnchorFilter();

    @Test
    public void testFilterAbsoluteURLs() {
        String ABSOLUTE_URL = "http://www.routeconverter.com";
        String ANOTHER_ABSOLUTE_URL = "http://www.routeconverter.de";
        String RELATIVE_URL = "page.html";
        assertEquals(asList(RELATIVE_URL), filter.filterAnchors(ABSOLUTE_URL, asList(ABSOLUTE_URL, ANOTHER_ABSOLUTE_URL, RELATIVE_URL), null));
    }

    @Test
    public void testFilterIndexHtml() {
        String ABSOLUTE_URL = "http://www.routeconverter.com";
        String URI = "page.html";
        String INDEX_URI = "index.html";
        assertEquals(asList(URI), filter.filterAnchors(ABSOLUTE_URL, asList(URI, INDEX_URI), null));
    }
}
