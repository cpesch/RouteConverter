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
package slash.navigation.mapview.mapsforge.updater;

import org.mapsforge.map.layer.Layer;
import slash.navigation.common.NavigationPosition;
import slash.navigation.common.DistanceAndTime;

/**
 * A pair of {@link NavigationPosition}s with a {@link Layer}, a distance and a time.
 *
 * @author Christian Pesch
 */

public class PairWithLayer implements ObjectWithLayer {
    private final NavigationPosition first;
    private final NavigationPosition second;
    private Layer layer;
    private DistanceAndTime distanceAndTime;

    public PairWithLayer(NavigationPosition first, NavigationPosition second) {
        this.first = first;
        this.second = second;
    }

    public NavigationPosition getFirst() {
        return first;
    }

    public NavigationPosition getSecond() {
        return second;
    }

    public boolean hasCoordinates() {
        return getFirst().hasCoordinates() && getSecond().hasCoordinates();
    }

    public Layer getLayer() {
        return layer;
    }

    public void setLayer(Layer layer) {
        this.layer = layer;
    }

    public DistanceAndTime getDistanceAndTime() {
        return distanceAndTime;
    }

    public void setDistanceAndTime(DistanceAndTime distanceAndTime) {
        this.distanceAndTime = distanceAndTime;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PairWithLayer that = (PairWithLayer) o;

        return first.equals(that.first) && second.equals(that.second) &&
                (layer != null ? layer.equals(that.layer) : that.layer == null);
    }

    public int hashCode() {
        int result = first.hashCode();
        result = 31 * result + second.hashCode();
        result = 31 * result + (layer != null ? layer.hashCode() : 0);
        return result;
    }

    public String toString() {
        return getClass().getSimpleName() + "@" + hashCode() + "[first=" + getFirst() + ", second=" + getSecond() + ", layer=" + getLayer() + ", distanceAndTime=" + getDistanceAndTime() + "]";
    }
}
