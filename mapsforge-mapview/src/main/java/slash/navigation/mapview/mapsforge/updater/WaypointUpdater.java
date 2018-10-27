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

import slash.navigation.converter.gui.models.PositionsModel;

import java.util.ArrayList;
import java.util.List;

import static java.lang.Math.min;

/**
 * Stores the current waypoint state and minimizes {@link WaypointOperation}s.
 * Used to reduce the number of interactions between event listener and map UI.
 *
 * @author Christian Pesch
 * @see WaypointOperation
 */

public class WaypointUpdater implements EventMapUpdater {
    private final PositionsModel positionsModel;
    private final WaypointOperation waypointOperation;
    private final List<PositionWithLayer> positionWithLayers = new ArrayList<>();

    public WaypointUpdater(PositionsModel positionsModel, WaypointOperation waypointOperation) {
        this.positionsModel = positionsModel;
        this.waypointOperation = waypointOperation;
    }

    public synchronized void handleAdd(int firstRow, int lastRow) {
        int validLastRow = min(lastRow, positionsModel.getRowCount() - 1);

        List<PositionWithLayer> added = new ArrayList<>();
        for (int i = firstRow; i <= validLastRow; i++) {
            PositionWithLayer positionWithLayer = new PositionWithLayer(positionsModel.getPosition(i));
            positionWithLayers.add(i, positionWithLayer);
            added.add(positionWithLayer);
        }

        if (!added.isEmpty())
            waypointOperation.add(added);
    }

    public synchronized void handleUpdate(int firstRow, int lastRow) {
        int validLastRow = min(lastRow, positionWithLayers.size() - 1);

        List<PositionWithLayer> updated = new ArrayList<>();
        for (int i = firstRow; i <= validLastRow; i++) {
            updated.add(positionWithLayers.get(i));
        }

        if (!updated.isEmpty())
            waypointOperation.update(updated);
    }

    public synchronized void handleRemove(int firstRow, int lastRow) {
        int validLastRow = min(lastRow, positionWithLayers.size() - 1);

        List<PositionWithLayer> removed = new ArrayList<>();
        for (int i = validLastRow; i >= firstRow; i--) {
            removed.add(positionWithLayers.get(i));
            positionWithLayers.remove(i);
        }

        if (!removed.isEmpty())
            waypointOperation.remove(removed);
    }

    /*package local for tests*/synchronized List<PositionWithLayer> getPositionWithLayers() {
        return positionWithLayers;
    }
}
