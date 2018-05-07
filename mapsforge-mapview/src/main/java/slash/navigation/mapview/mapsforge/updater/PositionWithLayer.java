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

import java.util.Objects;

/**
 * A {@link NavigationPosition} with it's {@link Layer} component.
 *
 * @author Christian Pesch
 */

public class PositionWithLayer implements ObjectWithLayer {
    private final NavigationPosition position;
    private Layer layer;

    public PositionWithLayer(NavigationPosition position) {
        this.position = position;
    }

    public NavigationPosition getPosition() {
        return position;
    }

    public boolean hasCoordinates() {
        return getPosition().hasCoordinates();
    }

    public Layer getLayer() {
        return layer;
    }

    public void setLayer(Layer layer) {
        this.layer = layer;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PositionWithLayer that = (PositionWithLayer) o;
        return Objects.equals(getPosition(), that.getPosition()) &&
                Objects.equals(getLayer(), that.getLayer());
    }

    public int hashCode() {
        return Objects.hash(getPosition(), getLayer());
    }

    public String toString() {
        return getClass().getSimpleName() + "@" + hashCode() + "[position=" + getPosition() + ", layer=" + getLayer() + "]";
    }}
