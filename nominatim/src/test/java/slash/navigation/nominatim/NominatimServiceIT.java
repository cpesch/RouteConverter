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
package slash.navigation.nominatim;

import org.junit.Test;
import slash.navigation.common.NavigationPosition;
import slash.navigation.common.SimpleNavigationPosition;

import java.io.IOException;
import java.util.List;

import static java.util.Arrays.asList;
import static org.junit.Assert.*;

public class NominatimServiceIT {
    private NominatimService service = new NominatimService();

    @Test
    public void getPositionsFor() throws IOException {
        List<SimpleNavigationPosition> expected = asList(
                new SimpleNavigationPosition(10.1988085, 50.0001708, null, "B\u00fchlstra\u00dfe, Grafenrheinfeld, Landkreis Schweinfurt, Bayern, 97506, Deutschland (residential)"),
                new SimpleNavigationPosition(10.2001313, 50.0016142, null, "B\u00fchlstra\u00dfe, Grafenrheinfeld, Landkreis Schweinfurt, Bayern, 97506, Deutschland (living_street)"),
                new SimpleNavigationPosition(10.1999752, 49.9999416, null, "B\u00fchlstra\u00dfe, Grafenrheinfeld, Landkreis Schweinfurt, Bayern, 97506, Deutschland (service)")
        );
        List<NavigationPosition> actual = service.getPositionsFor("B\u00fchlstra\u00dfe, 97506 Grafenrheinfeld, Germany");
        assertEquals(expected, actual);
    }

    @Test
    public void getAddressFor() throws IOException {
        assertTrue(service.getAddressFor(new SimpleNavigationPosition(10.2, 50.001)).contains("Grafenrheinfeld"));
        assertTrue(service.getAddressFor(new SimpleNavigationPosition(11.06561, 47.42428)).contains("Garmisch-Partenkirchen"));
        assertNull(service.getAddressFor(new SimpleNavigationPosition(0.0, 0.0)));
        assertNull(service.getAddressFor(new SimpleNavigationPosition(0.0, 90.0)));
        assertNull(service.getAddressFor(new SimpleNavigationPosition(0.0, -90.0)));
        assertEquals("Cantón San Cristóbal, Galápagos, Ecuador", service.getAddressFor(new SimpleNavigationPosition(-90.0, 0.0)));
        assertNull(service.getAddressFor(new SimpleNavigationPosition(-90.0, -90.0)));
        assertNull(service.getAddressFor(new SimpleNavigationPosition(90.0, 90.0)));
    }


    @Test
    public void getAddressForCity() throws IOException {
        // https://nominatim.openstreetmap.org/reverse?lat=47.3&lon=9.0&format=xml
        assertEquals("M\u00fcslieggstrasse, 8733 Eschenbach (SG), Sankt Gallen, Schweiz/Suisse/Svizzera/Svizra", service.getAddressFor(new SimpleNavigationPosition(9.0, 47.3)));
    }

    @Test
    public void getAddressForTown() throws IOException {
        // https://nominatim.openstreetmap.org/reverse?lat=51.3141479&lon=10.1880035&format=xml
        assertEquals("Picksweg, 37308 Heilbad Heiligenstadt, Th\u00fcringen, Deutschland", service.getAddressFor(new SimpleNavigationPosition(10.1880035, 51.3141479)));
    }

    @Test
    public void getAddressForVillage() throws IOException {
        // https://nominatim.openstreetmap.org/reverse?&lat=51.610954&lon=10.210236&format=xml
        assertEquals("Am Schützenplatz, 37434 Gieboldehausen, Niedersachsen, Deutschland", service.getAddressFor(new SimpleNavigationPosition(10.210236, 51.610954)));
    }
}
