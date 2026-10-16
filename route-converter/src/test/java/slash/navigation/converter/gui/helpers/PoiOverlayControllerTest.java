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
package slash.navigation.converter.gui.helpers;

import org.junit.After;
import org.junit.Test;
import slash.navigation.common.BoundingBox;
import slash.navigation.geocoding.SimpleCategorizedNavigationPosition;
import slash.navigation.mapview.MapView;
import slash.navigation.mapview.PoiMarker;
import slash.navigation.pois.mapsforge.CategoryPoi;
import slash.navigation.pois.mapsforge.CategoryPoiResult;
import slash.navigation.pois.mapsforge.MapsforgePoiOverlayService;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.prefs.Preferences;

import static java.awt.EventQueue.invokeAndWait;
import static java.util.Collections.emptyList;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static slash.navigation.converter.gui.helpers.PoiOverlayController.LIMIT_PER_GROUP;
import static slash.navigation.converter.gui.helpers.PoiOverlayController.State.*;

public class PoiOverlayControllerTest {
    // about 10 km across, well below the zoom-in limit
    private static final BoundingBox CITY = new BoundingBox(13.45, 52.55, 13.35, 52.50);
    private static final BoundingBox COUNTRY = new BoundingBox(15.0, 55.0, 6.0, 47.0);

    private final Preferences preferences = Preferences.userRoot().node("slash/navigation/test/" + UUID.randomUUID());
    private final MapsforgePoiOverlayService service = mock(MapsforgePoiOverlayService.class);
    private final MapView mapView = mock(MapView.class);

    @After
    public void removePreferences() throws Exception {
        preferences.removeNode();
    }

    private PoiOverlayController createController() {
        return new PoiOverlayController(service, () -> mapView, Runnable::run, preferences);
    }

    private static CategoryPoi poi(String name, String category, int group) {
        return new CategoryPoi(new SimpleCategorizedNavigationPosition(13.4, 52.52, null, name, category), group);
    }

    private static void drainEventQueue() throws Exception {
        invokeAndWait(() -> {
        });
    }

    @SuppressWarnings("unchecked")
    private List<PoiMarker> shownMarkers() {
        org.mockito.ArgumentCaptor<List<PoiMarker>> captor = org.mockito.ArgumentCaptor.forClass(List.class);
        verify(mapView, atLeastOnce()).showPoiOverlay(captor.capture());
        return captor.getValue();
    }

    @Test
    public void searchesNothingWhileOffOrWithoutCategories() throws Exception {
        when(mapView.getBoundingBox()).thenReturn(CITY);
        PoiOverlayController controller = createController();

        controller.refresh();
        controller.setEnabled(true);
        drainEventQueue();

        assertEquals(OFF, controller.getState());
        verifyNoInteractions(service);
        verify(mapView, never()).showPoiOverlay(any());
    }

    @Test
    public void showsPoisOfSelectedCategoriesColoredByGroup() throws Exception {
        when(mapView.getBoundingBox()).thenReturn(CITY);
        when(service.search(eq(CITY), eq(Set.of("Banks")), eq(LIMIT_PER_GROUP))).thenReturn(
                new CategoryPoiResult(List.of(poi("Sparkasse", "bank", 0), poi("toilets", null, 1)), false));
        PoiOverlayController controller = createController();

        controller.setEnabled(true);
        controller.setSelectedKeys(Set.of("Banks"));
        drainEventQueue();

        assertEquals(SHOWN, controller.getState());
        assertEquals(2, controller.getCount());
        List<PoiMarker> markers = shownMarkers();
        assertEquals("Sparkasse (bank)", markers.get(0).label());
        assertEquals("Sparkasse", markers.get(0).position().getDescription());
        assertEquals(PoiOverlayController.getGroupColor(0), markers.get(0).color());
        assertEquals("toilets", markers.get(1).label());
        assertEquals(PoiOverlayController.getGroupColor(1), markers.get(1).color());
    }

    @Test
    public void searchesAgainOnlyWhenViewportOrCategoriesChange() throws Exception {
        when(mapView.getBoundingBox()).thenReturn(CITY);
        when(service.search(any(), anySet(), anyInt())).thenReturn(CategoryPoiResult.EMPTY);
        PoiOverlayController controller = createController();
        controller.setEnabled(true);
        controller.setSelectedKeys(Set.of("Banks"));

        controller.refresh();
        controller.refresh();
        verify(service, times(1)).search(any(), anySet(), anyInt());

        controller.setSelectedKeys(Set.of("Banks", "Shop"));
        verify(service, times(2)).search(any(), anySet(), anyInt());
    }

    @Test
    public void reportsTruncationAndMissingData() throws Exception {
        when(mapView.getBoundingBox()).thenReturn(CITY);
        when(service.search(any(), anySet(), anyInt())).thenReturn(new CategoryPoiResult(List.of(poi("Bank", null, 0)), true));
        PoiOverlayController controller = createController();
        controller.setEnabled(true);
        controller.setSelectedKeys(Set.of("Banks"));
        drainEventQueue();
        assertEquals(TRUNCATED, controller.getState());

        when(service.search(any(), anySet(), anyInt())).thenReturn(null);
        controller.setSelectedKeys(Set.of("Shop"));
        drainEventQueue();
        assertEquals(NO_DATA, controller.getState());
        assertEquals(emptyList(), shownMarkers());
    }

    @Test
    public void asksToZoomInInsteadOfSearchingLargeViewport() throws Exception {
        when(mapView.getBoundingBox()).thenReturn(COUNTRY);
        PoiOverlayController controller = createController();
        controller.setEnabled(true);
        controller.setSelectedKeys(Set.of("Banks"));
        drainEventQueue();

        assertEquals(ZOOM_IN, controller.getState());
        verifyNoInteractions(service);
        assertTrue(PoiOverlayController.isTooLarge(COUNTRY));
        assertFalse(PoiOverlayController.isTooLarge(CITY));
    }

    @Test
    public void clearsOverlayOnceWhenSwitchedOff() throws Exception {
        when(mapView.getBoundingBox()).thenReturn(CITY);
        when(service.search(any(), anySet(), anyInt())).thenReturn(new CategoryPoiResult(List.of(poi("Bank", null, 0)), false));
        PoiOverlayController controller = createController();
        controller.setEnabled(true);
        controller.setSelectedKeys(Set.of("Banks"));
        drainEventQueue();

        controller.setEnabled(false);
        controller.refresh();
        controller.refresh();

        assertEquals(OFF, controller.getState());
        verify(mapView, times(2)).showPoiOverlay(any());
        assertEquals(emptyList(), shownMarkers());
    }

    @Test
    public void restoresEnabledStateAndCategoriesFromPreferences() {
        PoiOverlayController controller = createController();
        controller.setEnabled(true);
        controller.setSelectedKeys(Set.of("Banks", "Toilets "));

        PoiOverlayController restored = createController();

        assertTrue(restored.isEnabled());
        assertEquals(Set.of("Banks", "Toilets "), restored.getSelectedKeys());
    }
}
