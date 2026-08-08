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
    // own-state row count, tracked independently of positionsModel.getRowCount(): a queued
    // event can execute after a later file has already advanced the live model, so reading
    // the live row count at execution time can misclassify a pure append as a middle-insert
    private int rowCount = 0;

    public TrackUpdater(PositionsModel positionsModel, TrackOperation trackOperation) {
        this.positionsModel = positionsModel;
        this.trackOperation = trackOperation;
    }

    public synchronized void handleAdd(int firstRow, int lastRow) {
        int oldRowCount = rowCount;
        int newRowCount = oldRowCount + (lastRow - firstRow + 1);
        rowCount = newRowCount;

        // do not remove anything if a new position is prepended or appended to the track
        boolean middleInsert = firstRow > 0 && lastRow < newRowCount - 1;

        List<PairWithLayer> removed = new ArrayList<>();
        if (middleInsert)
            removed.add(pairWithLayers.remove(firstRow - 1));

        List<PairWithLayer> added = new ArrayList<>();
        int from = firstRow > 0 ? firstRow - 1 : firstRow;
        int to = min(lastRow, newRowCount - 2);
        for (int i = from; i <= to; i++) {
            PairWithLayer pairWithLayer = new PairWithLayer(positionsModel.getPosition(i), positionsModel.getPosition(i + 1), i);
            pairWithLayers.add(i, pairWithLayer);
            added.add(pairWithLayer);
        }

        if (!removed.isEmpty())
            trackOperation.remove(removed);
        if (!added.isEmpty())
            trackOperation.add(added);
    }

    public synchronized void handleUpdate(int firstRow, int lastRow) {
        if (pairWithLayers.isEmpty())
            return;

        int beforeFirstRow = firstRow > 0 ? firstRow - 1 : 0;
        int afterLastRow = min(lastRow, pairWithLayers.size() - 1);
        if (afterLastRow < beforeFirstRow)
            return;

        List<PairWithLayer> updated = new ArrayList<>();
        for (int i = beforeFirstRow; i <= afterLastRow; i++) {
            PairWithLayer pairWithLayer = new PairWithLayer(positionsModel.getPosition(i), positionsModel.getPosition(i + 1), i);
            pairWithLayer.setLayer(pairWithLayers.get(i).getLayer());
            pairWithLayers.set(i, pairWithLayer);
            updated.add(pairWithLayer);
        }

        if (!updated.isEmpty())
            trackOperation.update(updated);
    }

    public synchronized void handleRemove(int firstRow, int lastRow) {
        if (pairWithLayers.isEmpty()) {
            rowCount = 0;
            return;
        }

        int beforeFirstRow = firstRow > 0 ? firstRow - 1 : firstRow;
        int validLastRow = min(lastRow, pairWithLayers.size() - 1);
        if (validLastRow < beforeFirstRow) {
            rowCount = pairWithLayers.size() + 1;
            return;
        }

        List<PairWithLayer> added = new ArrayList<>();
        if (beforeFirstRow < firstRow && validLastRow == lastRow) {
            PairWithLayer pairWithLayer = new PairWithLayer(pairWithLayers.get(beforeFirstRow).getFirst(), pairWithLayers.get(validLastRow).getSecond(), beforeFirstRow);
            added.add(pairWithLayer);
        }

        List<PairWithLayer> removed = new ArrayList<>();
        for (int i = validLastRow; i >= beforeFirstRow; i--) {
            PairWithLayer remove = pairWithLayers.remove(i);
            remove.setRow(i);
            removed.add(remove);
        }

        for (PairWithLayer pairWithLayer : added)
            pairWithLayers.add(beforeFirstRow, pairWithLayer);

        rowCount = pairWithLayers.isEmpty() ? 0 : pairWithLayers.size() + 1;

        if (!removed.isEmpty())
            trackOperation.remove(removed);
        if (!added.isEmpty())
            trackOperation.add(added);
    }

    /*for tests*/synchronized List<PairWithLayer> getPairWithLayers() {
        return pairWithLayers;
    }
}
