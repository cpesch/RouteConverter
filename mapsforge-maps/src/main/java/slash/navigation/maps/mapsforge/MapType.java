package slash.navigation.maps.mapsforge;

import org.mapsforge.map.layer.download.TileDownloadLayer;
import org.mapsforge.map.layer.download.tilesource.AbstractTileSource;
import org.mapsforge.map.layer.renderer.TileRendererLayer;
import slash.navigation.maps.mapsforge.mbtiles.TileMBTilesLayer;

import java.io.File;

/**
 * The type of the map.
 *
 * {@link #Download} are downloaded from the {@link AbstractTileSource} and rendered via a {@link TileDownloadLayer}
 * {@link #Mapsforge} are local {@link File}s in a binary map file format and rendered via a {@link TileRendererLayer}
 * {@link #MBTiles} are local {@link File}s which are SQLite databases and rendered via a {@link TileMBTilesLayer}
 */

public enum MapType {
    Download(true, false), Mapsforge(false, true), MBTiles(false, false);

    private final boolean download;
    private final boolean themed;

    MapType(boolean download, boolean themed) {
        this.download = download;
        this.themed = themed;
    }

    public boolean isDownload() {
        return download;
    }

    public boolean isThemed() {
        return themed;
    }
}
