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

package slash.navigation.gpx;

import org.junit.Test;
import slash.navigation.base.AllNavigationFormatRegistry;
import slash.navigation.base.NavigationFormat;
import slash.navigation.base.NavigationFormatParser;
import slash.navigation.base.ParserResult;
import slash.navigation.base.RouteCharacteristics;
import slash.navigation.nmea.NmeaFormat;
import slash.navigation.nmea.NmeaPosition;
import slash.navigation.nmea.NmeaRoute;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;
import static slash.common.TestCase.assertDoubleEquals;
import static slash.navigation.base.NavigationTestCase.TEST_PATH;
import static slash.navigation.base.NavigationTestCase.readGpxFile;

/**
 * Read-only integration tests for the Columbus GNSS device sample files in
 * GPX (old bare-extension firmware, new cb: namespace firmware) and NMEA log
 * form. Fixtures resolve via {@link slash.navigation.base.NavigationTestCase#TEST_PATH};
 * a test skips rather than fails when its fixture is absent, matching the
 * existing *IT convention so public CI without the private sample set stays green.
 */
public class ColumbusGnssGpxReadIT {

    private GpxRoute readColumbusGpxTrack(String fileName) throws Exception {
        File file = new File(TEST_PATH + fileName);
        assumeTrue("Sample file not found: " + file.getAbsolutePath(), file.exists());

        List<GpxRoute> routes = readGpxFile(new Gpx11Format(), TEST_PATH + fileName);
        assertNotNull(routes);
        for (GpxRoute route : routes) {
            if (route.getCharacteristics() == RouteCharacteristics.Track)
                return route;
        }
        throw new AssertionError("No track found in " + fileName);
    }

    @Test
    public void testOldFirmwareBareExtensions() throws Exception {
        GpxRoute route = readColumbusGpxTrack("from-columbusgnss.gpx");
        assertEquals(1165, route.getPositionCount());

        GpxPosition first = route.getPosition(0);
        assertDoubleEquals(18.1, first.getSpeed());
        assertDoubleEquals(82.2, first.getHeading());
        assertDoubleEquals(0.8, first.getHdop());
    }

    @Test
    public void testNewFirmwareCbNamespace() throws Exception {
        GpxRoute route = readColumbusGpxTrack("from-columbusgnss-cb.gpx");
        assertEquals(922, route.getPositionCount());

        GpxPosition first = route.getPosition(0);
        assertDoubleEquals(3.4, first.getSpeed());
        assertDoubleEquals(120.2, first.getHeading());
        assertDoubleEquals(1.62, first.getHdop());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testNmeaLog() throws IOException {
        File file = new File(TEST_PATH + "from-columbusgnss-nmea.log");
        assumeTrue("Sample file not found: " + file.getAbsolutePath(), file.exists());

        NavigationFormatParser parser = new NavigationFormatParser(new AllNavigationFormatRegistry());
        ParserResult result = parser.read(file, Collections.<NavigationFormat>singletonList(new NmeaFormat()));
        assertTrue("Could not read from-columbusgnss-nmea.log", result.isSuccessful());
        NmeaRoute route = (NmeaRoute) result.getAllRoutes().get(0);
        assertEquals(550, route.getPositionCount());

        NmeaPosition first = (NmeaPosition) route.getPosition(0);
        assertNotNull(first.getHeading());

        boolean foundHdopAndFixQuality = false;
        for (int i = 0; i < route.getPositionCount(); i++) {
            NmeaPosition position = (NmeaPosition) route.getPosition(i);
            Double hdop = position.getHdop();
            Integer fixQuality = position.getFixQuality();
            if (hdop != null && fixQuality != null && fixQuality >= 0 && fixQuality <= 7) {
                foundHdopAndFixQuality = true;
                break;
            }
        }
        assertTrue("Expected at least one position with non-null hdop and fixQuality in 0..7", foundHdopAndFixQuality);
    }
}
