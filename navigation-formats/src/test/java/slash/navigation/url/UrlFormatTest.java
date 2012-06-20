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

package slash.navigation.url;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class UrlFormatTest {
    private static final String GOOGLE_MAPS_EMAIL = "Betreff: Route nach/zu Riehler Strasse 190 50735 Koeln (Google Maps)\n" +
            "\n" +
            "> Routenplaner\n" +
            "> Link:\n" +
            "http://maps.google.de/maps?f=d&hl=de&geocode=&saddr=H%C3%B6lderlinstra%C3%9Fe,+51545+Br%C3%B6l,+Oberbergischer+Kreis,+Nordrhein-Westfalen,+Deutschland&daddr=L339%2FWuppertaler+Stra%C3%9Fe+%4050.918890,+7.560880+to%3AK%C3%B6ln,+Riehler+Str.+190&mrcr=2&mra=mr&sll=50.954318,7.311401&sspn=0.142091,0.32135&ie=UTF8&ll=50.952371,7.261276&spn=0.284193,0.6427&z=11&om=1 \n" +
            ">\n" +
            "> Startadresse: Hoelderlinstrasse 51545 Broel\n" +
            "> Zieladresse: Riehler Strasse 190 50735 Koeln";

    private static final String ROUTE_CATALOG_URL = "don't care\n" +
            "http://www.routeconverter.de/catalog/files/63/ don't care\n";

    private static final String TWO_URLS = "don't care\n" +
            "http://www.routeconverter.de/catalog/files/63/ don't care\n" +
            "http://some.other.url/ don't care\n";

    private static final String FILE = "file:///CWD/../RouteSamples/trunk/test/from11.gpx";

    private UrlFormat format = new UrlFormat();

    @Test
    public void testFindGoogleMapsURLFromEmail() {
        String url = format.findURL(GOOGLE_MAPS_EMAIL);
        assertNotNull(url);
        assertTrue(url.startsWith("http://maps.google.de"));
        assertTrue(url.endsWith("&om=1"));
    }

    @Test
    public void testFindRouteCatalogURL() {
        String url = format.findURL(ROUTE_CATALOG_URL);
        assertNotNull(url);
        assertTrue(url.startsWith("http://www.routeconverter.de"));
        assertTrue(url.endsWith("63/"));
    }

    @Test
    public void testFindFirstURL() {
        String url = format.findURL(TWO_URLS);
        assertNotNull(url);
        assertTrue(url.startsWith("http://www.routeconverter.de"));
        assertTrue(url.endsWith("63/"));
    }

    @Test
    public void testFindFileURL() {
        String url = format.findURL(FILE);
        assertNotNull(url);
        assertEquals(FILE, url);
    }
}
