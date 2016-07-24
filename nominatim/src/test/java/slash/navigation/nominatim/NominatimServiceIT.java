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
import static org.junit.Assert.assertEquals;

public class NominatimServiceIT {
    private NominatimService service = new NominatimService();

    @Test
    public void getPositionsFor() throws IOException {
        List<SimpleNavigationPosition> expected = asList(
                new SimpleNavigationPosition(10.2004439, 50.0010543, null, "B\u00fchlstra\u00dfe, Grafenrheinfeld, Landkreis Schweinfurt, Unterfranken, Bayern, 97506, Deutschland, Europe (residential)"),
                new SimpleNavigationPosition(10.2001313, 50.0016142, null, "B\u00fchlstra\u00dfe, Grafenrheinfeld, Landkreis Schweinfurt, Unterfranken, Bayern, 97506, Deutschland, Europe (living_street)"),
                new SimpleNavigationPosition(10.1999752, 49.9999416, null, "B\u00fchlstra\u00dfe, Grafenrheinfeld, Landkreis Schweinfurt, Unterfranken, Bayern, 97506, Deutschland, Europe (service)")
        );
        List<NavigationPosition> actual = service.getPositionsFor("B\u00fchlstra\u00dfe, 97506 Grafenrheinfeld, Germany");
        assertEquals(expected, actual);
    }
}
