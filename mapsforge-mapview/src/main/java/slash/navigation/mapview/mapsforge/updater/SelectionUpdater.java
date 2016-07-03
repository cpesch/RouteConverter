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

import slash.navigation.common.NavigationPosition;
import slash.navigation.converter.gui.models.PositionsModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Stores the current selection state and minimizes {@link SelectionOperation}s.
 * Used to reduce the number of interactions between event listener and map UI.
 *
 * @author Christian Pesch
 * @see SelectionOperation
 */

public class SelectionUpdater {
    private final PositionsModel positionsModel;
    private final SelectionOperation selectionOperation;
    private final List<PositionWithLayer> positionWithLayers = new ArrayList<>();

    public SelectionUpdater(PositionsModel positionsModel, SelectionOperation selectionOperation) {
        this.positionsModel = positionsModel;
        this.selectionOperation = selectionOperation;
    }

    public void setSelectedPositions(int[] selectedPositions, boolean replaceSelection) {
        if (replaceSelection) {
            replaceSelection(selectedPositions);
        } else {
            updateSelection(selectedPositions);
        }
    }

    public void updatedPositions(List<NavigationPosition> positions) {
        List<PositionWithLayer> updated = new ArrayList<>();
        for (PositionWithLayer positionWithLayer : positionWithLayers) {
            NavigationPosition position = positionWithLayer.getPosition();
            if (positions.contains(position))
                updated.add(positionWithLayer);
        }
        applyDelta(updated, updated);
    }

    public void removedPositions(List<NavigationPosition> positions) {
        List<PositionWithLayer> removed = new ArrayList<>();
        for (PositionWithLayer positionWithLayer : positionWithLayers) {
            NavigationPosition position = positionWithLayer.getPosition();
            if (positions.contains(position) && positionsModel.getIndex(position) == -1)
                removed.add(positionWithLayer);
        }
        applyDelta(Collections.<PositionWithLayer>emptyList(), removed);
    }

    private void replaceSelection(int[] selectedPositions) {
        applyDelta(asPositionWithLayers(selectedPositions), positionWithLayers);
    }

    private void updateSelection(int[] selectedPositions) {
        List<PositionWithLayer> selected = asPositionWithLayers(selectedPositions);

        List<PositionWithLayer> added = new ArrayList<>();
        for (PositionWithLayer positionWithLayer : selected) {
            if (!positionWithLayers.contains(positionWithLayer))
                added.add(positionWithLayer);
        }
        List<PositionWithLayer> removed = new ArrayList<>();
        for (PositionWithLayer positionWithLayer : positionWithLayers) {
            if (!selected.contains(positionWithLayer))
                removed.add(positionWithLayer);
        }

        applyDelta(added, removed);
    }

    private void applyDelta(List<PositionWithLayer> added, List<PositionWithLayer> removed) {
        if (!removed.isEmpty()) {
            selectionOperation.remove(removed);
            positionWithLayers.removeAll(removed);
        }
        if (!added.isEmpty()) {
            selectionOperation.add(added);
            positionWithLayers.addAll(added);
        }
    }

    private List<PositionWithLayer> asPositionWithLayers(int[] indices) {
        List<PositionWithLayer> result = new ArrayList<>();
        for (int selectedPosition : indices) {
            if (selectedPosition < positionsModel.getRowCount())
                result.add(new PositionWithLayer(positionsModel.getPosition(selectedPosition)));
        }
        return result;
    }

    public List<PositionWithLayer> getPositionWithLayers() {
        return positionWithLayers;
    }
}
