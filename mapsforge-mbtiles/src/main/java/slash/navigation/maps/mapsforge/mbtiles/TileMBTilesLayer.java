package slash.navigation.maps.mapsforge.mbtiles;

import org.mapsforge.core.graphics.GraphicFactory;
import org.mapsforge.core.graphics.TileBitmap;
import org.mapsforge.core.model.Tile;
import org.mapsforge.map.layer.TileLayer;
import org.mapsforge.map.layer.cache.TileCache;
import org.mapsforge.map.model.DisplayModel;
import org.mapsforge.map.model.IMapViewPosition;
import org.mapsforge.map.model.common.Observer;

public class TileMBTilesLayer extends TileLayer<RendererJob> implements Observer {
    private final DatabaseRenderer databaseRenderer;
    private MapWorkerPool mapWorkerPool;

    public TileMBTilesLayer(TileCache tileCache, IMapViewPosition mapViewPosition, boolean isTransparent,
                            MBTilesFile file, GraphicFactory graphicFactory) {
        super(tileCache, mapViewPosition, graphicFactory.createMatrix(), isTransparent);
        this.databaseRenderer = new DatabaseRenderer(file, graphicFactory);
    }

    public synchronized void setDisplayModel(DisplayModel displayModel) {
        super.setDisplayModel(displayModel);
        if (displayModel != null) {
            if (this.mapWorkerPool == null) {
                this.mapWorkerPool = new MapWorkerPool(this.tileCache, this.jobQueue, this.databaseRenderer, this);
            }
            this.mapWorkerPool.start();
        } else {
            // if we do not have a displayModel any more we can stop rendering.
            if (this.mapWorkerPool != null) {
                this.mapWorkerPool.stop();
            }
        }
    }

    protected RendererJob createJob(Tile tile) {
        return new RendererJob(tile, databaseRenderer, this.isTransparent);
    }

    /**
     * Whether the tile is stale and should be refreshed.
     * <p/>
     * This method is called from {@link #draw(org.mapsforge.core.model.BoundingBox, byte, org.mapsforge.core.graphics.Canvas, org.mapsforge.core.model.Point)} to determine whether the tile needs to
     * be refreshed.
     * <p/>
     * A tile is considered stale if the timestamp of the layer's {@link #databaseRenderer} is more recent than the
     * {@code bitmap}'s {@link org.mapsforge.core.graphics.TileBitmap#getTimestamp()}.
     * <p/>
     * When a tile has become stale, the layer will first display the tile referenced by {@code bitmap} and attempt to
     * obtain a fresh copy in the background. When a fresh copy becomes available, the layer will replace is and update
     * the cache. If a fresh copy cannot be obtained for whatever reason, the stale tile will continue to be used until
     * another {@code #draw(BoundingBox, byte, Canvas, Point)} operation requests it again.
     *
     * @param tile   A tile.
     * @param bitmap The bitmap for {@code tile} currently held in the layer's cache.
     */
    protected boolean isTileStale(Tile tile, TileBitmap bitmap) {
        return this.databaseRenderer.getDataTimestamp(tile) > bitmap.getTimestamp();
    }

    protected void onAdd() {
        this.mapWorkerPool.start();
        if (tileCache != null) {
            tileCache.addObserver(this);
        }

        super.onAdd();
    }

    protected void onRemove() {
        this.mapWorkerPool.stop();
        if (tileCache != null) {
            tileCache.removeObserver(this);
        }

        super.onRemove();
    }

    public void onChange() {
        this.requestRedraw();
    }
}
