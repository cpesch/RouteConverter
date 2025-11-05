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
 * A {@link ShadeTileSource} implementation that does nothing.
 *
 * @author Christian Pesch
 */

public class NoOpShadeTileSource implements ShadeTileSource {
    public void prepareOnThread() {}
    public HillshadingBitmap getHillshadingBitmap(int latitudeOfSouthWestCorner, int longituedOfSouthWestCorner, int zoomLevel, double pxPerLat, double pxPerLon, int color) throws ExecutionException, InterruptedException { return null; }
    public void applyConfiguration(boolean allowParallel) {}
    public ShadingAlgorithm getAlgorithm() {
        return null;
    }
    public void setAlgorithm(ShadingAlgorithm algorithm) {}
    public boolean isZoomLevelSupported(int zoomLevel, int lat, int lon) {
        return false;
    }
    public void interruptAndDestroy() {}
}
