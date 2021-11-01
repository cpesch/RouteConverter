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

import org.mapsforge.map.layer.Layer;
import slash.navigation.common.DistanceAndTime;
import slash.navigation.common.NavigationPosition;
import slash.navigation.mapview.mapsforge.updater.ObjectWithLayer;
import slash.navigation.mapview.mapsforge.updater.PairWithLayer;
import slash.navigation.mapview.mapsforge.updater.PositionWithLayer;

import java.util.*;
import java.util.stream.Collectors;

/**
 * A helper for extracting parts of {@link List}s of {@link ObjectWithLayer} and descendants.
 *
 * @author Christian Pesch
 */

public class WithLayerHelper {
    public static List<NavigationPosition> toPositions(List<PositionWithLayer> positionWithLayers) {
        return positionWithLayers.stream().
                map(PositionWithLayer::getPosition).
                filter(NavigationPosition::hasCoordinates).
                collect(Collectors.toList());
    }

    public static List<NavigationPosition> toPositions2(List<PairWithLayer> pairWithLayers) {
        Set<NavigationPosition> updated = new HashSet<>();
        for (PairWithLayer pair : pairWithLayers) {
            updated.add(pair.getFirst());
            updated.add(pair.getSecond());
        }
        return new ArrayList<>(updated);
    }

    public static List<Layer> toLayers(List<? extends ObjectWithLayer> withLayers) {
        return withLayers.stream().
                map(ObjectWithLayer::getLayer).
                collect(Collectors.toList());
    }

    public static Map<Integer, DistanceAndTime> toDistanceAndTimes(List<PairWithLayer> pairWithLayers) {
        Map<Integer, DistanceAndTime> indexToDistanceAndTime = new TreeMap<>();
        for (PairWithLayer pairWithLayer : pairWithLayers) {
            indexToDistanceAndTime.put(pairWithLayer.getRow() + 1, pairWithLayer.getDistanceAndTime());
        }
        return indexToDistanceAndTime;
    }
}
