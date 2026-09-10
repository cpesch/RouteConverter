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
package org.mapsforge.map.layer;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mapsforge.core.graphics.Bitmap;
import org.mapsforge.core.graphics.Canvas;
import org.mapsforge.core.graphics.GraphicFactory;
import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.Dimension;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.MapPosition;
import org.mapsforge.core.model.Point;
import org.mapsforge.core.model.Rotation;
import org.mapsforge.map.awt.graphics.AwtGraphicFactory;
import org.mapsforge.map.datastore.MultiMapDataStore;
import org.mapsforge.map.layer.cache.InMemoryTileCache;
import org.mapsforge.map.layer.cache.TileCache;
import org.mapsforge.map.layer.renderer.MapWorkerPool;
import org.mapsforge.map.layer.renderer.RendererJob;
import org.mapsforge.map.layer.renderer.TileRendererLayer;
import org.mapsforge.map.model.DisplayModel;
import org.mapsforge.map.model.MapViewPosition;
import org.mapsforge.map.rendertheme.internal.MapsforgeThemes;
import org.mapsforge.map.util.LayerUtil;
import org.mapsforge.map.util.MapPositionUtil;
import slash.navigation.mapview.mapsforge.tiles.ReattachableTileRendererLayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Proves a {@link TileRendererLayer} keeps rendering after it has been removed from and re-added to
 * {@link Layers}, and after a theme switch while attached.
 * <p>
 * A plain {@link TileRendererLayer} fails {@link #rendersAfterRemoveAndReAdd()} (mapsforge/mapsforge#1817):
 * its {@link MapWorkerPool} keeps waiting on the {@link org.mapsforge.map.layer.queue.JobQueue} of the
 * first {@code setDisplayModel()} call, while {@link TileLayer#draw} queues jobs into the fresh queue
 * {@code Layers.add()} installs on every add. {@link ReattachableTileRendererLayer} works around this.
 */
public class TileRendererLayerLifecycleTest {
    private static final int WIDTH = 1024;
    private static final int HEIGHT = 768;
    private static final GraphicFactory GRAPHIC_FACTORY = AwtGraphicFactory.INSTANCE;

    private DisplayModel displayModel;
    private InMemoryTileCache cache;
    private Layers layers;
    private ReattachableTileRendererLayer layer;
    private Canvas canvas;
    private BoundingBox boundingBox;
    private byte zoomLevel;
    private Point topLeftPoint;
    private List<TilePosition> tilePositions;

    @Before
    public void setUp() {
        displayModel = new DisplayModel();
        MapViewPosition mapViewPosition = new MapViewPosition(displayModel);
        mapViewPosition.setMapPosition(new MapPosition(new LatLong(54.0, 10.0), (byte) 7), false);

        cache = new InMemoryTileCache(64);
        layers = new Layers(() -> {
        }, displayModel);

        layer = new ReattachableTileRendererLayer(cache, new MultiMapDataStore(), mapViewPosition,
                true, true, true, GRAPHIC_FACTORY, null);
        layer.setXmlRenderTheme(MapsforgeThemes.OSMARENDER);

        Dimension dimension = new Dimension(WIDTH, HEIGHT);
        MapPosition mapPosition = mapViewPosition.getMapPosition();
        boundingBox = MapPositionUtil.getBoundingBox(mapPosition, Rotation.NULL_ROTATION,
                displayModel.getTileSize(), dimension, 0.5f, 0.5f);
        zoomLevel = mapPosition.zoomLevel;
        topLeftPoint = MapPositionUtil.getTopLeftPoint(mapPosition, dimension, displayModel.getTileSize());
        tilePositions = LayerUtil.getTilePositions(boundingBox, zoomLevel, topLeftPoint, displayModel.getTileSize());

        Bitmap bitmap = GRAPHIC_FACTORY.createBitmap(WIDTH, HEIGHT);
        canvas = GRAPHIC_FACTORY.createCanvas();
        canvas.setBitmap(bitmap);
    }

    @After
    public void tearDown() throws InterruptedException {
        if (layers.contains(layer))
            layers.remove(layer);
        layer.setDisplayModel(null);
        cache.destroy();
        assertNoLeakedMapWorkerPoolThreads();
    }

    @Test
    public void rendersAfterSingleAdd() throws InterruptedException {
        layers.add(layer);

        draw();

        assertAllTilesRendered();
    }

    @Test
    public void rendersAfterRemoveAndReAdd() throws InterruptedException {
        layers.add(layer);
        layers.remove(layer);
        layers.add(0, layer);

        draw();

        // with a plain TileRendererLayer this fails (mapsforge/mapsforge#1817): zero tiles ever render
        assertAllTilesRendered();
    }

    @Test
    public void rendersAfterThemeSwitchWhileAttached() throws InterruptedException {
        layers.add(layer);
        draw();
        assertAllTilesRendered();

        cache.purge();
        layer.setXmlRenderTheme(MapsforgeThemes.DEFAULT);
        draw();

        assertAllTilesRendered();
    }

    @Test
    public void rendersAfterRemoveReAddWithJobsInFlight() throws InterruptedException {
        layers.add(layer);
        draw(); // do not wait for the jobs to finish before the rebuild

        layers.remove(layer);
        layers.add(0, layer);
        draw();

        assertAllTilesRendered();
    }

    private void draw() {
        layer.draw(boundingBox, zoomLevel, canvas, topLeftPoint, Rotation.NULL_ROTATION);
    }

    private void assertAllTilesRendered() throws InterruptedException {
        // generous headroom: cold theme parsing + rendering can take well over 5s on a loaded
        // Windows CI runner, which flaked here despite rendering completing correctly
        long deadline = System.currentTimeMillis() + 15000;
        List<RendererJob> missing;
        do {
            missing = new ArrayList<>();
            for (TilePosition tilePosition : tilePositions) {
                RendererJob job = new RendererJob(tilePosition.tile, layer.getMapDataStore(), layer.getRenderThemeFuture(),
                        displayModel, 1f, true, false);
                if (!cache.containsKey(job))
                    missing.add(job);
            }
            if (missing.isEmpty())
                return;
            Thread.sleep(100);
        } while (System.currentTimeMillis() < deadline);

        fail(missing.size() + " of " + tilePositions.size() + " tiles not rendered");
    }

    private static void assertNoLeakedMapWorkerPoolThreads() throws InterruptedException {
        long deadline = System.currentTimeMillis() + 2000;
        do {
            if (!anyMapWorkerPoolThreadRunning())
                return;
            Thread.sleep(100);
        } while (System.currentTimeMillis() < deadline);

        assertTrue("MapWorkerPool thread still running after teardown", !anyMapWorkerPoolThreadRunning());
    }

    private static boolean anyMapWorkerPoolThreadRunning() {
        for (Map.Entry<Thread, StackTraceElement[]> entry : Thread.getAllStackTraces().entrySet()) {
            for (StackTraceElement element : entry.getValue()) {
                if (MapWorkerPool.class.getName().equals(element.getClassName()) && "run".equals(element.getMethodName()))
                    return true;
            }
        }
        return false;
    }
}
