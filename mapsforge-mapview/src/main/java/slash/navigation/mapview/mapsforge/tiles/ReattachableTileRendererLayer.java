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

import org.mapsforge.core.graphics.GraphicFactory;
import org.mapsforge.map.datastore.MapDataStore;
import org.mapsforge.map.layer.Layers;
import org.mapsforge.map.layer.TileLayer;
import org.mapsforge.map.layer.cache.TileCache;
import org.mapsforge.map.layer.hills.HillsRenderConfig;
import org.mapsforge.map.layer.queue.JobQueue;
import org.mapsforge.map.layer.renderer.MapWorkerPool;
import org.mapsforge.map.layer.renderer.RendererJob;
import org.mapsforge.map.layer.renderer.TileRendererLayer;
import org.mapsforge.map.model.DisplayModel;
import org.mapsforge.map.model.MapViewPosition;

/**
 * A {@link TileRendererLayer} that keeps rendering after it has been removed from and re-added to
 * the {@link Layers} of a map view.
 * <p>
 * Works around a mapsforge (0.30.0) bug: {@link Layers#add} calls {@link #setDisplayModel} on every add,
 * and {@link TileLayer#setDisplayModel} replaces the layer's {@link JobQueue} with a fresh instance each
 * time, but {@link TileRendererLayer#setDisplayModel} creates its {@link MapWorkerPool} only once, bound
 * to the queue that existed on the first add. After a re-add, {@code TileLayer.draw()} queues tile jobs
 * into the new queue while the worker pool waits on the old one forever, so the layer never renders
 * another tile. This is what left the world map background blank (issue #376).
 * <p>
 * Keeping the first queue for the whole lifetime of the layer keeps {@code draw()} and the worker pool
 * on the same queue. The queue only captures the map view position and display model, which stay the
 * same objects for a map view, so reusing it is safe.
 */
public class ReattachableTileRendererLayer extends TileRendererLayer {
    private JobQueue<RendererJob> firstJobQueue;

    public ReattachableTileRendererLayer(TileCache tileCache, MapDataStore mapDataStore, MapViewPosition mapViewPosition,
                                         boolean isTransparent, boolean renderLabels, boolean cacheLabels,
                                         GraphicFactory graphicFactory, HillsRenderConfig hillsRenderConfig) {
        super(tileCache, mapDataStore, mapViewPosition, isTransparent, renderLabels, cacheLabels, graphicFactory, hillsRenderConfig);
    }

    @Override
    public synchronized void setDisplayModel(DisplayModel displayModel) {
        super.setDisplayModel(displayModel);
        if (displayModel == null)
            return;

        if (firstJobQueue == null)
            firstJobQueue = jobQueue;
        else
            jobQueue = firstJobQueue;
    }
}
