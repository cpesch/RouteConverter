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
 * Stores the current track state and minimizes {@link TrackOperation}s.
 * Used to reduce the number of interactions between event listener and map UI.
 *
 * @author Christian Pesch
 * @see TrackOperation
 */

public class TrackUpdater implements EventMapUpdater {
    private final PositionsModel positionsModel;
    private final TrackOperation trackOperation;
    private final List<PairWithLayer> pairWithLayers = new ArrayList<>();

    public TrackUpdater(PositionsModel positionsModel, TrackOperation trackOperation) {
        this.positionsModel = positionsModel;
        this.trackOperation = trackOperation;
    }

    public void handleAdd(int firstRow, int lastRow) {
        int beforeFirstRow = firstRow > 0 ? firstRow - 1 : firstRow;
        int validLastRow = min(lastRow, positionsModel.getRowCount() - 1);
        int afterLastRow = lastRow < positionsModel.getRowCount() - 1 ? lastRow + 1 : validLastRow;

        List<PairWithLayer> removed = new ArrayList<>();
        if (beforeFirstRow < pairWithLayers.size())
            removed.add(pairWithLayers.remove(beforeFirstRow));

        List<PairWithLayer> added = new ArrayList<>();
        for (int i = beforeFirstRow; i < afterLastRow; i++) {
            PairWithLayer pairWithLayer = new PairWithLayer(positionsModel.getPosition(i), positionsModel.getPosition(i + 1));
            pairWithLayers.add(i, pairWithLayer);
            added.add(pairWithLayer);
        }

        if (!removed.isEmpty())
            trackOperation.remove(removed);
        if (!added.isEmpty())
            trackOperation.add(added);
    }

    public void handleUpdate(int firstRow, int lastRow) {
        int beforeFirstRow = firstRow > 0 ? firstRow - 1 : firstRow;
        int validLastRow = min(lastRow, positionsModel.getRowCount() - 1);
        int afterLastRow = lastRow < positionsModel.getRowCount() - 1 ? lastRow + 1 : validLastRow;

        List<PairWithLayer> updated = new ArrayList<>();
        for (int i = beforeFirstRow; i < afterLastRow; i++) {
            PairWithLayer pairWithLayer = new PairWithLayer(positionsModel.getPosition(i), positionsModel.getPosition(i + 1));
            pairWithLayer.setLayer(pairWithLayers.get(i).getLayer());
            pairWithLayers.set(i, pairWithLayer);
            updated.add(pairWithLayers.get(i));
        }

        if (!updated.isEmpty())
            trackOperation.update(updated);
    }

    public void handleRemove(int firstRow, int lastRow) {
        int beforeFirstRow = firstRow > 0 ? firstRow - 1 : firstRow;
        int validLastRow = min(lastRow, pairWithLayers.size() - 1);

        List<PairWithLayer> added = new ArrayList<>();
        if (beforeFirstRow < firstRow && validLastRow == lastRow) {
            PairWithLayer pairWithLayer = new PairWithLayer(pairWithLayers.get(beforeFirstRow).getFirst(), pairWithLayers.get(validLastRow).getSecond());
            added.add(pairWithLayer);
        }

        List<PairWithLayer> removed = new ArrayList<>();
        for (int i = validLastRow; i >= beforeFirstRow; i--)
            removed.add(pairWithLayers.remove(i));

        for (PairWithLayer pairWithLayer : added)
            pairWithLayers.add(beforeFirstRow, pairWithLayer);

        if (!added.isEmpty())
            trackOperation.add(added);
        if (!removed.isEmpty())
            trackOperation.remove(removed);
    }

    List<PairWithLayer> getPairWithLayers() {
        return pairWithLayers;
    }
}
