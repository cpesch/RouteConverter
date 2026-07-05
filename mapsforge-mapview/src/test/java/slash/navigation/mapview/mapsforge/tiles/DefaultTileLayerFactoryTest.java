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
package slash.navigation.mapview.mapsforge.tiles;

import org.junit.Before;
import org.junit.Test;
import org.mapsforge.map.layer.Layer;
import org.mapsforge.map.layer.download.TileDownloadLayer;
import org.mapsforge.map.layer.hills.HillsRenderConfig;
import org.mapsforge.map.model.DisplayModel;
import org.mapsforge.map.model.MapViewPosition;
import org.mapsforge.map.rendertheme.XmlRenderThemeMenuCallback;
import slash.navigation.maps.mapsforge.MapsforgeMapManager;
import slash.navigation.maps.mapsforge.impl.TileDownloadMap;
import slash.navigation.maps.mapsforge.models.TileServerMapSource;
import slash.navigation.maps.tileserver.TileServer;

import java.util.List;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mapsforge.map.awt.graphics.AwtGraphicFactory.INSTANCE;
import static slash.navigation.maps.mapsforge.MapType.Download;

public class DefaultTileLayerFactoryTest {
    private DefaultTileLayerFactory factory;

    @Before
    public void setUp() {
        MapViewPosition mapViewPosition = new MapViewPosition(new DisplayModel());
        factory = new DefaultTileLayerFactory(mock(MapsforgeMapManager.class), mapViewPosition,
                mock(HillsRenderConfig.class), mock(XmlRenderThemeMenuCallback.class), INSTANCE);
    }

    private static TileServer tileServer() {
        return new TileServer("test", "Test", "https://{$serverpart}/{$z}/{$x}/{$y}.png",
                List.of("tiles.example.org"), true, 0, 18, "cc", "Copyright");
    }

    @Test
    public void createOverlayLayerBuildsADownloadLayer() {
        TileDownloadLayer layer = factory.createOverlayLayer(tileServer());

        assertNotNull(layer);
    }

    @Test
    public void createLayerForMapDispatchesDownloadToDownloadLayer() {
        TileDownloadMap map = mock(TileDownloadMap.class);
        when(map.getType()).thenReturn(Download);
        when(map.getUrl()).thenReturn("download-cache");
        when(map.getTileSource()).thenReturn(new TileServerMapSource(tileServer()));

        Layer layer = factory.createLayerForMap(map);

        assertTrue("Download map must produce a TileDownloadLayer", layer instanceof TileDownloadLayer);
    }
}
