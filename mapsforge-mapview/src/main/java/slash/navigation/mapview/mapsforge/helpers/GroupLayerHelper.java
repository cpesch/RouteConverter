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

import org.mapsforge.map.layer.GroupLayer;
import org.mapsforge.map.layer.Layer;
import org.mapsforge.map.layer.Layers;
import org.mapsforge.map.layer.overlay.Marker;
import org.mapsforge.map.model.DisplayModel;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import static java.util.Collections.singletonList;

/**
 * A helper for putting {@link Layer}s into the child list of a {@link GroupLayer} and taking them
 * out again.
 *
 * @author Christian Pesch
 */

public class GroupLayerHelper {
    private GroupLayerHelper() {
    }

    /**
     * Adds layers to the child list of a {@link GroupLayer}. Always use this instead of touching
     * {@link GroupLayer#layers} directly: {@link Layers#add} is the only place that assigns the
     * {@link DisplayModel}, and {@link GroupLayer#setDisplayModel} only reaches the children that
     * are present when it is called. A child added afterwards would keep a null display model and
     * let {@link Marker#draw} kill the LayerManager thread with a NullPointerException.
     */
    public static void addToGroupLayer(GroupLayer groupLayer, DisplayModel displayModel, Collection<? extends Layer> layers) {
        if (layers.isEmpty())
            return;

        synchronized (groupLayer) {
            for (Layer layer : layers)
                layer.setDisplayModel(displayModel);
            groupLayer.layers.addAll(layers);
        }
    }

    public static void addToGroupLayer(GroupLayer groupLayer, DisplayModel displayModel, Layer layer) {
        addToGroupLayer(groupLayer, displayModel, singletonList(layer));
    }

    /**
     * Removes the given layers from a {@link GroupLayer}'s child list, leaving the rest of the
     * group untouched. Clearing the whole group instead would drop the layers of every position
     * that was not part of this update.
     */
    public static void removeFromGroupLayer(GroupLayer groupLayer, Collection<? extends Layer> layers) {
        // removeAll(Collection) is O(n) only if the argument's contains() is O(1)
        Set<Layer> toRemove = new HashSet<>(layers.size());
        for (Layer layer : layers) {
            if (layer != null)
                toRemove.add(layer);
        }
        if (toRemove.isEmpty())
            return;

        synchronized (groupLayer) {
            groupLayer.layers.removeAll(toRemove);
        }
    }
}
