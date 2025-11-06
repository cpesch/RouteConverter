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
package slash.navigation.mapview.mapsforge.helpers;

import org.mapsforge.core.graphics.HillshadingBitmap;
import org.mapsforge.map.layer.hills.ShadeTileSource;
import org.mapsforge.map.layer.hills.ShadingAlgorithm;

import java.util.concurrent.ExecutionException;

/**
 * A {@link ShadeTileSource} that delegates to a {@link ShadeTileSource delegate}.
 *
 * @author Christian Pesch
 */

public class DelegatingShadeTileSource implements ShadeTileSource {
    private ShadeTileSource delegate;

    public void setDelegate(ShadeTileSource delegate) {
        this.delegate = delegate;
    }

    public void prepareOnThread() {
        if(delegate != null) delegate.prepareOnThread();
    }
    public HillshadingBitmap getHillshadingBitmap(int latitudeOfSouthWestCorner, int longituedOfSouthWestCorner, int zoomLevel, double pxPerLat, double pxPerLon, int color) throws ExecutionException, InterruptedException {
        return delegate != null ? delegate.getHillshadingBitmap(latitudeOfSouthWestCorner, longituedOfSouthWestCorner, zoomLevel, pxPerLat, pxPerLon, color) : null;
    }
    public void applyConfiguration(boolean allowParallel) {
        if(delegate != null) delegate.applyConfiguration(allowParallel);
    }
    public ShadingAlgorithm getAlgorithm() {
        return delegate != null ? delegate.getAlgorithm() : null;
    }
    public void setAlgorithm(ShadingAlgorithm algorithm) {
        if(delegate != null) delegate.setAlgorithm(algorithm);
    }
    public boolean isZoomLevelSupported(int zoomLevel, int lat, int lon) {
        return delegate != null && delegate.isZoomLevelSupported(zoomLevel, lat, lon);
    }
    public void interruptAndDestroy() {
        if (delegate != null) delegate.interruptAndDestroy();
    }
}
