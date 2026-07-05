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

import org.mapsforge.map.layer.Layer;
import org.mapsforge.map.layer.cache.FileSystemTileCache;
import org.mapsforge.map.layer.cache.InMemoryTileCache;
import org.mapsforge.map.layer.cache.TileCache;
import org.mapsforge.map.layer.cache.TwoLevelTileCache;
import org.mapsforge.map.layer.download.TileDownloadLayer;
import org.mapsforge.map.layer.hills.HillsRenderConfig;
import org.mapsforge.map.layer.renderer.TileRendererLayer;
import org.mapsforge.map.model.MapViewPosition;
import org.mapsforge.map.reader.MapFile;
import org.mapsforge.map.rendertheme.XmlRenderTheme;
import org.mapsforge.map.rendertheme.XmlRenderThemeMenuCallback;
import slash.navigation.maps.mapsforge.LocalMap;
import slash.navigation.maps.mapsforge.LocalTheme;
import slash.navigation.maps.mapsforge.MapsforgeMapManager;
import slash.navigation.maps.mapsforge.impl.MBTilesFileMap;
import slash.navigation.maps.mapsforge.impl.MapsforgeFileMap;
import slash.navigation.maps.mapsforge.impl.TileDownloadMap;
import slash.navigation.maps.mapsforge.mbtiles.TileMBTilesLayer;
import slash.navigation.maps.mapsforge.models.TileServerMapSource;
import slash.navigation.maps.tileserver.TileServer;

import java.io.File;
import java.util.prefs.Preferences;

import static slash.common.io.Directories.getTemporaryDirectory;
import static slash.common.io.Transfer.encodeUri;
import static slash.navigation.mapview.mapsforge.AwtGraphicMapView.GRAPHIC_FACTORY;

/**
 * Default {@link TileLayerFactory} building the real mapsforge tile layers and caches.
 *
 * @author Christian Pesch
 */

class DefaultTileLayerFactory implements TileLayerFactory {
    private static final Preferences preferences = Preferences.userNodeForPackage(MapsforgeMapView.class);
    private static final String FIRST_LEVEL_TILE_CACHE_SIZE_PREFERENCE = "firstLevelTileCacheSize";
    private static final String SECOND_LEVEL_TILE_CACHE_SIZE_PREFERENCE = "secondLevelTileCacheSize";

    private final MapsforgeMapManager mapManager;
    private final MapViewPosition mapViewPosition;
    private final HillsRenderConfig hillsRenderConfig;
    private final XmlRenderThemeMenuCallback menuCallback;

    DefaultTileLayerFactory(MapsforgeMapManager mapManager, MapViewPosition mapViewPosition,
                            HillsRenderConfig hillsRenderConfig, XmlRenderThemeMenuCallback menuCallback) {
        this.mapManager = mapManager;
        this.mapViewPosition = mapViewPosition;
        this.hillsRenderConfig = hillsRenderConfig;
        this.menuCallback = menuCallback;
    }

    private TileCache createTileCache(String cacheId) {
        TileCache firstLevelTileCache = new InMemoryTileCache(preferences.getInt(FIRST_LEVEL_TILE_CACHE_SIZE_PREFERENCE, 256));
        File cacheDirectory = new File(getTemporaryDirectory(), encodeUri(cacheId));
        TileCache secondLevelTileCache = new FileSystemTileCache(preferences.getInt(SECOND_LEVEL_TILE_CACHE_SIZE_PREFERENCE, 2048), cacheDirectory, GRAPHIC_FACTORY);
        return new TwoLevelTileCache(firstLevelTileCache, secondLevelTileCache);
    }

    private TileRendererLayer createTileRendererLayer(MapFile mapFile, String cacheId) {
        return new TileRendererLayer(createTileCache(cacheId), mapFile,
                mapViewPosition, true, true, true,
                GRAPHIC_FACTORY, hillsRenderConfig);
    }

    private TileRendererLayer createMapLayer(MapFile mapFile, String cacheId) {
        TileRendererLayer tileRendererLayer = createTileRendererLayer(mapFile, cacheId);

        LocalTheme theme = mapManager.getAppliedThemeModel().getItem();
        XmlRenderTheme xmlRenderTheme = theme.getXmlRenderTheme();
        xmlRenderTheme.setMenuCallback(menuCallback);
        tileRendererLayer.setXmlRenderTheme(theme.getXmlRenderTheme());
        return tileRendererLayer;
    }

    public Layer createLayerForMap(LocalMap map) {
        return switch (map.getType()) {
            case Mapsforge -> createMapLayer(((MapsforgeFileMap) map).getMapFile(), map.getUrl());
            case MBTiles -> new TileMBTilesLayer(createTileCache(map.getUrl()), mapViewPosition, true, ((MBTilesFileMap) map).getMBTilesFile(), GRAPHIC_FACTORY);
            case Download -> new TileDownloadLayer(createTileCache(map.getUrl()), mapViewPosition, ((TileDownloadMap) map).getTileSource(), GRAPHIC_FACTORY);
        };
    }

    public TileDownloadLayer createOverlayLayer(TileServer tileServer) {
        TileServerMapSource mapSource = new TileServerMapSource(tileServer);
        mapSource.setAlpha(true);
        return new TileDownloadLayer(createTileCache(tileServer.id()), mapViewPosition, mapSource, GRAPHIC_FACTORY);
    }

    public Layer createBackgroundLayer(File backgroundMap) {
        TileRendererLayer backgroundLayer = createTileRendererLayer(new MapFile(backgroundMap), backgroundMap.getName());
        LocalTheme theme = mapManager.getAppliedThemeModel().getItem();
        backgroundLayer.setXmlRenderTheme(theme.getXmlRenderTheme());
        return backgroundLayer;
    }
}
