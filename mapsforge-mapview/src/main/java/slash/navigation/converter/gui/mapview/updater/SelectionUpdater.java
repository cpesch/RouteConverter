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
package slash.navigation.converter.gui.mapview.updater;

import slash.navigation.base.NavigationPosition;
import slash.navigation.converter.gui.models.PositionsModel;

import java.util.ArrayList;
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
    private final List<NavigationPosition> currentSelection = new ArrayList<NavigationPosition>();

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

    List<NavigationPosition> getCurrentSelection() {
        return currentSelection;
    }

    private void replaceSelection(int[] selectedPositions) {
        List<NavigationPosition> added = asPositions(selectedPositions);
        applyDelta(added, currentSelection);
    }

    private void updateSelection(int[] selectedPositions) {
        List<NavigationPosition> positions = asPositions(selectedPositions);

        List<NavigationPosition> added = new ArrayList<NavigationPosition>();
        for (NavigationPosition position : positions) {
            if (!currentSelection.contains(position))
                added.add(position);
        }
        List<NavigationPosition> removed = new ArrayList<NavigationPosition>();
        for (NavigationPosition position : currentSelection) {
            if (!positions.contains(position))
                removed.add(position);
        }

        applyDelta(added, removed);
    }

    private List<NavigationPosition> asPositions(int[] indices) {
        List<NavigationPosition> result = new ArrayList<NavigationPosition>();
        for (int selectedPosition : indices) {
            NavigationPosition position = positionsModel.getPosition(selectedPosition);
            result.add(position);
        }
        return result;
    }

    private void applyDelta(List<NavigationPosition> added, List<NavigationPosition> removed) {
        if (!removed.isEmpty()) {
            selectionOperation.remove(removed);
            currentSelection.removeAll(removed);
        }
        if (!added.isEmpty()) {
            selectionOperation.add(added);
            currentSelection.addAll(added);
        }
    }
}
