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

package slash.navigation.csv;

import org.junit.Test;
import slash.navigation.base.*;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static slash.common.io.Files.collectFiles;
import static slash.navigation.base.NavigationTestCase.*;
import static slash.navigation.base.RouteCharacteristics.*;

public class Flightradar24Test {
    @Test
    public void testAllFlightradar24CsvTracks() throws IOException {
        readFiles("flightradar24", ".csv", 1, true, true, Track);
    }

    @Test
    public void testAllFlighradar24CsvToKmlTracks() throws IOException {
        // The main track from the KML and the CSV file must have the same content.
        List<File> files = collectFiles(new File(SAMPLE_PATH), ".csv");
        for (File file : files) {
            if (file.getName().startsWith("flightradar24")) {
                NavigationFormatParser parser = new NavigationFormatParser(new AllNavigationFormatRegistry());
                ParserResult csvResult = parser.read(file);
                ParserResult kmlResult = parser.read(new File(file.getAbsolutePath().replace(".csv", ".kml")));

                assertEquals(1, csvResult.getAllRoutes().size());
                assertFalse(kmlResult.getAllRoutes().isEmpty());
                checkEquals(csvResult.getAllRoutes().get(0), kmlResult.getAllRoutes().get(0));
            }
        }
    }

    private void checkEquals(BaseRoute<?, ?> csvRoute, BaseRoute<?, ?> kmlRoute) {
        List<? extends BaseNavigationPosition> kmlPositions = kmlRoute.getPositions();
        List<? extends BaseNavigationPosition> csvPositions = csvRoute.getPositions();
        assertEquals(kmlPositions.size(), csvPositions.size());
        assertTrue(kmlRoute.getName().contains(csvRoute.getName()));
        for (int i = 0; i< kmlPositions.size(); i++) {
            BaseNavigationPosition kmlPos = kmlPositions.get(i);
            BaseNavigationPosition csvPos = csvPositions.get(i);

            assertEquals(kmlPos.getTime(), csvPos.getTime());
            assertEquals(kmlPos.getLatitude(), csvPos.getLatitude());
            assertEquals(kmlPos.getLongitude(), csvPos.getLongitude());
            assertEquals(round(kmlPos.getElevation()), round(csvPos.getElevation()));
        }
    }

    private Double round(double toRound) {
        return Math.round(toRound * 1000.) / 1000.;
    }
}