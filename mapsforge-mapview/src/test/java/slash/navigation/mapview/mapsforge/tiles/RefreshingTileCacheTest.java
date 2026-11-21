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
import org.mapsforge.core.model.Tile;
import org.mapsforge.map.layer.cache.InMemoryTileCache;
import org.mapsforge.map.layer.queue.Job;

import java.util.concurrent.atomic.AtomicLong;

import static java.util.concurrent.TimeUnit.DAYS;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mapsforge.map.awt.graphics.AwtGraphicFactory.INSTANCE;

public class RefreshingTileCacheTest {
    private static final long MAXIMUM_AGE = DAYS.toMillis(7);
    private static final long NOW = DAYS.toMillis(100);

    private final AtomicLong clock = new AtomicLong(NOW);
    private final Job job = new Job(new Tile(1, 2, (byte) 3, 256), false);
    private RefreshingTileCache cache;

    @Before
    public void setUp() {
        cache = new RefreshingTileCache(new InMemoryTileCache(16), MAXIMUM_AGE, clock::get);
    }

    private static TileBitmap bitmapFrom(long timestamp) {
        TileBitmap bitmap = INSTANCE.createTileBitmap(256, false);
        bitmap.setTimestamp(timestamp);
        return bitmap;
    }

    @Test
    public void missingTileIsNotContained() {
        assertFalse(cache.containsKey(job));
        assertNull(cache.getImmediately(job));
        assertFalse(cache.containsKey(job));
    }

    @Test
    public void freshTileIsContained() {
        TileBitmap bitmap = bitmapFrom(NOW - MAXIMUM_AGE + 1000);
        cache.put(job, bitmap);

        assertSame(bitmap, cache.getImmediately(job));
        assertTrue(cache.containsKey(job));
    }

    @Test
    public void tileOlderThanMaximumAgeIsStillServedButReportedMissingSoItIsDownloadedAgain() {
        TileBitmap old = bitmapFrom(NOW - MAXIMUM_AGE - 1000);
        cache.put(job, old);

        assertSame("the old tile stays visible until the fresh one arrives", old, cache.getImmediately(job));
        assertFalse("TileLayer and TileDownloadThread only download a tile the cache doesn't contain", cache.containsKey(job));
    }

    @Test
    public void downloadedTileReplacesTheOldOne() {
        cache.put(job, bitmapFrom(NOW - MAXIMUM_AGE - 1000));
        cache.get(job);
        assertFalse(cache.containsKey(job));

        TileBitmap fresh = bitmapFrom(NOW);
        cache.put(job, fresh);

        assertTrue(cache.containsKey(job));
        assertSame(fresh, cache.get(job));
        assertTrue(cache.containsKey(job));
    }

    @Test
    public void tileTurnsStaleAsTimePasses() {
        cache.put(job, bitmapFrom(NOW));
        cache.get(job);
        assertTrue(cache.containsKey(job));

        clock.set(NOW + MAXIMUM_AGE + 1000);
        cache.get(job);

        assertFalse(cache.containsKey(job));
    }
}
