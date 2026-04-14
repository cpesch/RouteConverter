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
package slash.navigation.mapview.mapsforge;

import org.junit.Before;
import org.junit.Test;
import org.mapsforge.map.rendertheme.XmlRenderThemeMenuCallback;
import org.mapsforge.map.rendertheme.XmlRenderThemeStyleLayer;
import org.mapsforge.map.rendertheme.XmlRenderThemeStyleMenu;
import slash.navigation.common.BoundingBox;
import slash.navigation.common.SimpleNavigationPosition;
import slash.navigation.maps.item.ItemTableModel;
import slash.navigation.maps.mapsforge.MapsforgeMapManager;
import slash.navigation.maps.tileserver.TileServerMapManager;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static slash.navigation.maps.mapsforge.helpers.MapUtil.toBoundingBox;

public class MapsforgeMapViewTest {
    private final MapsforgeMapView mapView = new MapsforgeMapView();
    private MapsforgeMapManager mapManager;

    @Before
    public void setUp() throws Exception {
        TileServerMapManager tileServerMapManager = mock(TileServerMapManager.class);
        when(tileServerMapManager.getAvailableMapsModel()).thenReturn(new ItemTableModel<>(1));

        mapManager = new MapsforgeMapManager(mock(), tileServerMapManager);
        mapManager.getAppliedThemeModel().setItem(mapManager.getAvailableThemesModel().getItem(0));

        MapViewCallbackOpenSource callback = mock(MapViewCallbackOpenSource.class);
        when(callback.getMapsforgeMapManager()).thenReturn(mapManager);

        Field mapViewCallback = MapsforgeMapView.class.getDeclaredField("mapViewCallback");
        mapViewCallback.setAccessible(true);
        mapViewCallback.set(mapView, callback);
    }

    @Test
    public void testBoundingBox() {
        BoundingBox from = new BoundingBox(new SimpleNavigationPosition(10.18587, 53.49249), new SimpleNavigationPosition(10.06767, 53.40451));
        assertEquals(from, from);
        org.mapsforge.core.model.BoundingBox to = mapView.asMapsforgeBoundingBox(from);
        BoundingBox roundtrip = toBoundingBox(to);
        assertEquals(roundtrip, from);
    }

    @Test
    public void testThemeStylesAreDuplicatedWhenMenuContainsEquivalentLayers() throws Exception {
        XmlRenderThemeStyleMenu renderThemeStyleMenu = mock(XmlRenderThemeStyleMenu.class);
        XmlRenderThemeStyleLayer hiking = createStyleLayer("hiking", "Hiking", "paths");
        XmlRenderThemeStyleLayer hikingDuplicate = createStyleLayer("hiking", "Hiking", "paths");
        XmlRenderThemeStyleLayer cycling = createStyleLayer("cycling", "Cycling", "cycleways");

        Map<String, XmlRenderThemeStyleLayer> layers = new LinkedHashMap<>();
        layers.put("hiking", hiking);
        layers.put("hiking-alias", hikingDuplicate);
        layers.put("cycling", cycling);

        when(renderThemeStyleMenu.getLayers()).thenReturn(layers);
        when(renderThemeStyleMenu.getDefaultValue()).thenReturn("hiking");
        when(renderThemeStyleMenu.getDefaultLanguage()).thenReturn("en");
        when(renderThemeStyleMenu.getLayer("hiking")).thenReturn(hiking);

        createMenuCallback().getCategories(renderThemeStyleMenu);

        assertEquals("Equivalent styles should only appear once after a map/theme switch", 2,
                mapManager.getAvailableThemeStylesModel().getRowCount());
    }

    private XmlRenderThemeMenuCallback createMenuCallback() throws Exception {
        Class<?> menuCallbackClass = Class.forName(MapsforgeMapView.class.getName() + "$MenuCallback");
        Constructor<?> constructor = menuCallbackClass.getDeclaredConstructor(MapsforgeMapView.class);
        constructor.setAccessible(true);
        return (XmlRenderThemeMenuCallback) constructor.newInstance(mapView);
    }

    private XmlRenderThemeStyleLayer createStyleLayer(String id, String title, String... categories) {
        XmlRenderThemeStyleLayer layer = mock(XmlRenderThemeStyleLayer.class);
        when(layer.getId()).thenReturn(id);
        when(layer.getTitle(anyString())).thenReturn(title);
        when(layer.getCategories()).thenReturn(new LinkedHashSet<>(Set.of(categories)));
        when(layer.getOverlays()).thenReturn(Collections.emptyList());
        return layer;
    }
}
