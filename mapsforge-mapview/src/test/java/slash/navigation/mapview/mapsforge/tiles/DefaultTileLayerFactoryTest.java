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
import org.mapsforge.core.graphics.TileBitmap;
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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;

import static org.junit.Assert.assertEquals;
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

    /**
     * OutdoorActive switched the "OAC Summer"/"OSM Summer" tiles from PNG to WebP while keeping the
     * URLs ending in "t.png". ImageIO picks its decoder by magic bytes, not by file extension, so the
     * only thing needed is a WebP ImageReaderSpi on the classpath (imageio-webp, runtime scope).
     * Without it this fails with IOException("ImageIO failed to read inputStream") and the map stays blank.
     */
    @Test
    public void createTileBitmapDecodesWebpDespitePngNamedUrl() throws IOException {
        // lossy VP8 ("VP8 " fourcc) — what w0.oastatic.com actually serves
        assertDecodesAs2x2(new byte[]{
                (byte) 0x52, (byte) 0x49, (byte) 0x46, (byte) 0x46, (byte) 0x38, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x57, (byte) 0x45, (byte) 0x42, (byte) 0x50, (byte) 0x56, (byte) 0x50, (byte) 0x38, (byte) 0x20,
                (byte) 0x2C, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x90, (byte) 0x01, (byte) 0x00, (byte) 0x9D,
                (byte) 0x01, (byte) 0x2A, (byte) 0x02, (byte) 0x00, (byte) 0x02, (byte) 0x00, (byte) 0x02, (byte) 0x00,
                (byte) 0x34, (byte) 0x25, (byte) 0xA0, (byte) 0x02, (byte) 0x74, (byte) 0xBA, (byte) 0x00, (byte) 0x03,
                (byte) 0x98, (byte) 0x00, (byte) 0xFE, (byte) 0xEF, (byte) 0x76, (byte) 0xD7, (byte) 0xE3, (byte) 0x79,
                (byte) 0xBC, (byte) 0xD6, (byte) 0xCE, (byte) 0x3F, (byte) 0xFE, (byte) 0xC1, (byte) 0xDF, (byte) 0xFE,
                (byte) 0x83, (byte) 0xBF, (byte) 0xFD, (byte) 0x07, (byte) 0x7F, (byte) 0xB2, (byte) 0x40, (byte) 0x00
        });
    }

    @Test
    public void createTileBitmapDecodesLosslessWebp() throws IOException {
        // lossless VP8L ("VP8L" fourcc) — a tile server may switch to it without notice
        assertDecodesAs2x2(new byte[]{
                (byte) 0x52, (byte) 0x49, (byte) 0x46, (byte) 0x46, (byte) 0x2C, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x57, (byte) 0x45, (byte) 0x42, (byte) 0x50, (byte) 0x56, (byte) 0x50, (byte) 0x38, (byte) 0x4C,
                (byte) 0x1F, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x2F, (byte) 0x01, (byte) 0x40, (byte) 0x00,
                (byte) 0x00, (byte) 0x1F, (byte) 0x20, (byte) 0x10, (byte) 0x48, (byte) 0xDA, (byte) 0x1F, (byte) 0x7A,
                (byte) 0x8D, (byte) 0xF9, (byte) 0x17, (byte) 0x10, (byte) 0x14, (byte) 0xF9, (byte) 0x3F, (byte) 0xDA,
                (byte) 0xFC, (byte) 0x07, (byte) 0x5F, (byte) 0x24, (byte) 0xE0, (byte) 0x07, (byte) 0x08, (byte) 0x11,
                (byte) 0xFD, (byte) 0x0F, (byte) 0x01, (byte) 0x00
        });
    }

    private static void assertDecodesAs2x2(byte[] tileBytes) throws IOException {
        TileBitmap bitmap = INSTANCE.createTileBitmap(new ByteArrayInputStream(tileBytes), 2, false);

        assertNotNull("WebP-decoded tile bitmap must not be null once imageio-webp is on the classpath", bitmap);
        assertEquals(2, bitmap.getWidth());
        assertEquals(2, bitmap.getHeight());
    }
}
