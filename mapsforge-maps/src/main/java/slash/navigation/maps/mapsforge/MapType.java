package slash.navigation.maps.mapsforge;

import org.mapsforge.map.layer.download.TileDownloadLayer;
import org.mapsforge.map.layer.download.tilesource.AbstractTileSource;
import org.mapsforge.map.layer.renderer.TileRendererLayer;

import java.io.File;

/**
 * The type of the map.
 *
 * {@link #TileDownload} are downloaded from the {@link AbstractTileSource} and rendered via a {@link TileDownloadLayer}
 * {@link #MapsforgeFile} are local {@link File} in a binary map file format and rendered via a {@link TileRendererLayer}
 */

public enum MapType {
    TileDownload(true, false), MapsforgeFile(false, true);

    private final boolean download;
    private final boolean themed;

    MapType(boolean download, boolean themed) {
        this.download = download;
        this.themed = false;
    }

    public boolean isDownload() {
        return download;
    }

    public boolean isThemed() {
        return themed;
    }
}
