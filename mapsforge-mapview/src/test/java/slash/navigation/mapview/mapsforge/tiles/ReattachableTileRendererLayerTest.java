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

import org.junit.After;
import org.junit.Test;
import org.mapsforge.map.datastore.MultiMapDataStore;
import org.mapsforge.map.layer.TileLayer;
import org.mapsforge.map.layer.cache.InMemoryTileCache;
import org.mapsforge.map.layer.renderer.TileRendererLayer;
import org.mapsforge.map.model.DisplayModel;
import org.mapsforge.map.model.MapViewPosition;

import java.lang.reflect.Field;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.mapsforge.map.awt.graphics.AwtGraphicFactory.INSTANCE;

public class ReattachableTileRendererLayerTest {
    private final DisplayModel displayModel = new DisplayModel();
    private TileRendererLayer layer;

    private TileRendererLayer plainLayer() {
        return new TileRendererLayer(new InMemoryTileCache(1), new MultiMapDataStore(), new MapViewPosition(displayModel),
                true, true, true, INSTANCE, null);
    }

    private TileRendererLayer reattachableLayer() {
        return new ReattachableTileRendererLayer(new InMemoryTileCache(1), new MultiMapDataStore(), new MapViewPosition(displayModel),
                true, true, true, INSTANCE, null);
    }

    @After
    public void tearDown() {
        if (layer != null)
            layer.setDisplayModel(null); // stops the MapWorkerPool
    }

    private static Object field(Object object, Class<?> declaringClass, String name) throws Exception {
        Field field = declaringClass.getDeclaredField(name);
        field.setAccessible(true);
        return field.get(object);
    }

    /** the queue {@code TileLayer.draw()} adds tile jobs to */
    private static Object layerQueue(TileRendererLayer layer) throws Exception {
        return field(layer, TileLayer.class, "jobQueue");
    }

    /** the queue the layer's MapWorkerPool takes tile jobs from */
    private static Object workerPoolQueue(TileRendererLayer layer) throws Exception {
        Object mapWorkerPool = field(layer, TileRendererLayer.class, "mapWorkerPool");
        assertNotNull("MapWorkerPool is created on the first setDisplayModel()", mapWorkerPool);
        return field(mapWorkerPool, mapWorkerPool.getClass(), "jobQueue");
    }

    @Test
    public void workerPoolStaysOnTheLayersQueueAfterReAdd() throws Exception {
        layer = reattachableLayer();

        layer.setDisplayModel(displayModel); // Layers.add()
        Object firstQueue = layerQueue(layer);
        assertSame(firstQueue, workerPoolQueue(layer));

        layer.setDisplayModel(displayModel); // Layers.remove() + Layers.add()
        assertSame("draw() and the worker pool must use the same queue after a re-add", firstQueue, layerQueue(layer));
        assertSame(firstQueue, workerPoolQueue(layer));
    }

    /**
     * Documents the mapsforge bug (mapsforge/mapsforge#1817) the subclass works around. When this test fails after a mapsforge
     * upgrade, the bug is fixed upstream and ReattachableTileRendererLayer can be dropped.
     */
    @Test
    public void plainTileRendererLayerLosesItsWorkerPoolQueueOnReAdd() throws Exception {
        layer = plainLayer();

        layer.setDisplayModel(displayModel);
        Object firstQueue = layerQueue(layer);
        assertSame(firstQueue, workerPoolQueue(layer));

        layer.setDisplayModel(displayModel);
        assertNotSame("mapsforge replaces the layer's queue on every setDisplayModel()", firstQueue, layerQueue(layer));
        assertSame("but keeps the worker pool on the first one", firstQueue, workerPoolQueue(layer));
    }
}
