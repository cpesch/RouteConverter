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

package slash.navigation.converter.gui.models;

import org.junit.Test;
import slash.navigation.common.NavigationPosition;
import slash.navigation.geocoding.CategorizedNavigationPosition;
import slash.navigation.geocoding.GeocodingResult;
import slash.navigation.geocoding.SimpleCategorizedNavigationPosition;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static slash.navigation.converter.gui.models.FindPlaceResultsModel.CATEGORY_COLUMN;
import static slash.navigation.converter.gui.models.FindPlaceResultsModel.GEOCODING_SERVICE_COLUMN;
import static slash.navigation.converter.gui.models.FindPlaceResultsModel.LATITUDE_COLUMN;
import static slash.navigation.converter.gui.models.FindPlaceResultsModel.LONGITUDE_COLUMN;
import static slash.navigation.converter.gui.models.FindPlaceResultsModel.NAME_COLUMN;

public class FindPlaceResultsModelTest {
    @Test
    public void readsCategoryFromCategorizedPosition() {
        FindPlaceResultsModel model = createModel(new GeocodingResult(
                new SimpleCategorizedNavigationPosition(13.4050, 52.5200, null, "Praha", "city"), "Mapsforge Map"));

        assertEquals("Praha", model.getValueAt(0, NAME_COLUMN));
        assertEquals("city", model.getValueAt(0, CATEGORY_COLUMN));
        assertEquals("Mapsforge Map", model.getValueAt(0, GEOCODING_SERVICE_COLUMN));
        assertEquals(5, model.getColumnCount());
    }

    @Test
    public void keepsNonMapsforgeParenthesesInDescription() {
        FindPlaceResultsModel model = createModel(new GeocodingResult(
                new SimpleCategorizedNavigationPosition(11.5761, 48.1371, null, "Munich (Center)", null), "Photon"));

        assertEquals("Munich (Center)", model.getValueAt(0, NAME_COLUMN));
        assertNull(model.getValueAt(0, CATEGORY_COLUMN));
    }

    @Test
    public void keepsCoordinateColumnsAsNavigationPositions() {
        SimpleCategorizedNavigationPosition position = new SimpleCategorizedNavigationPosition(7.4474, 46.9479, null, "Bern (town)", null);
        FindPlaceResultsModel model = createModel(new GeocodingResult(position, "Mapsforge"));

        NavigationPosition longitudePosition = (NavigationPosition) model.getValueAt(0, LONGITUDE_COLUMN);
        NavigationPosition latitudePosition = (NavigationPosition) model.getValueAt(0, LATITUDE_COLUMN);

        assertEquals(position.getLongitude(), longitudePosition.getLongitude());
        assertEquals(position.getLatitude(), longitudePosition.getLatitude());
        assertEquals(position.getDescription(), longitudePosition.getDescription());
        assertTrue(longitudePosition instanceof CategorizedNavigationPosition);
        assertNull(((CategorizedNavigationPosition) longitudePosition).getCategory());
        assertEquals(longitudePosition, latitudePosition);
    }

    private FindPlaceResultsModel createModel(GeocodingResult result) {
        FindPlaceResultsModel model = new FindPlaceResultsModel();
        model.setResults(List.of(result));
        return model;
    }
}

