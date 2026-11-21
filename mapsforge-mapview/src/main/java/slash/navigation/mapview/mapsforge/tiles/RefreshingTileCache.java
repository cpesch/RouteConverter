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

import org.mapsforge.core.graphics.TileBitmap;
import org.mapsforge.map.layer.cache.TileCache;
import org.mapsforge.map.layer.queue.Job;
import org.mapsforge.map.model.common.Observer;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

/**
 * A {@link TileCache} that has tiles older than a maximum age downloaded again.
 * <p/>
 * mapsforge's {@code TileLayer} queues a stale tile only if the cache does not contain it, and the
 * {@code TileDownloadThread} skips every job the cache contains. So a cached tile is never refreshed and,
 * with a persistent cache, would be kept forever. This cache reports a tile older than the maximum age
 * as missing from {@link #containsKey(Job)} while it still serves it from {@link #get(Job)} and
 * {@link #getImmediately(Job)}: the old tile stays visible, is downloaded once more, and the fresh one
 * replaces it via {@link #put(Job, TileBitmap)}.
 * <p/>
 * The age is measured from the tile's timestamp: the download time for a tile downloaded in this
 * session, the file modification time for a tile read from a persistent {@code FileSystemTileCache}.
 *
 * @author Christian Pesch
 */

class RefreshingTileCache implements TileCache {
    private final TileCache delegate;
    private final long maximumAge;
    private final LongSupplier clock;
    private final Set<String> staleKeys = ConcurrentHashMap.newKeySet();

    RefreshingTileCache(TileCache delegate, long maximumAge) {
        this(delegate, maximumAge, System::currentTimeMillis);
    }

    RefreshingTileCache(TileCache delegate, long maximumAge, LongSupplier clock) {
        this.delegate = delegate;
        this.maximumAge = maximumAge;
        this.clock = clock;
    }

    private TileBitmap track(Job key, TileBitmap bitmap) {
        if (bitmap != null) {
            if (bitmap.getTimestamp() + maximumAge < clock.getAsLong())
                staleKeys.add(key.getKey());
            else
                staleKeys.remove(key.getKey());
        }
        return bitmap;
    }

    public boolean containsKey(Job key) {
        return !staleKeys.contains(key.getKey()) && delegate.containsKey(key);
    }

    public TileBitmap get(Job key) {
        return track(key, delegate.get(key));
    }

    public TileBitmap getImmediately(Job key) {
        return track(key, delegate.getImmediately(key));
    }

    public void put(Job key, TileBitmap bitmap) {
        delegate.put(key, bitmap);
        staleKeys.remove(key.getKey());
    }

    public void destroy() {
        staleKeys.clear();
        delegate.destroy();
    }

    public void purge() {
        staleKeys.clear();
        delegate.purge();
    }

    public int getCapacity() {
        return delegate.getCapacity();
    }

    public int getCapacityFirstLevel() {
        return delegate.getCapacityFirstLevel();
    }

    public void setWorkingSet(Set<Job> workingSet) {
        delegate.setWorkingSet(workingSet);
    }

    public void addObserver(Observer observer) {
        delegate.addObserver(observer);
    }

    public void removeObserver(Observer observer) {
        delegate.removeObserver(observer);
    }
}
