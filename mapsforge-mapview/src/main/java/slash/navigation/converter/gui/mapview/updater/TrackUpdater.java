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

import slash.navigation.common.NavigationPosition;
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
    private final List<NavigationPosition> currentTrack = new ArrayList<NavigationPosition>();

    public TrackUpdater(PositionsModel positionsModel, TrackOperation trackOperation) {
        this.positionsModel = positionsModel;
        this.trackOperation = trackOperation;
    }

    public void handleAdd(int firstRow, int lastRow) {
        for (int i = firstRow; i <= lastRow; i++)
            currentTrack.add(i, positionsModel.getPosition(i));

        int startIndex = firstRow > 0 ? firstRow - 1 : firstRow;
        int endIndex = lastRow < currentTrack.size() - 1 ? lastRow + 1 : lastRow;

        List<PositionPair> added = new ArrayList<PositionPair>();
        for (int i = startIndex; i < endIndex; i++)
            added.add(new PositionPair(positionsModel.getPosition(i), positionsModel.getPosition(i + 1)));

        List<PositionPair> removed = new ArrayList<PositionPair>();
        if (firstRow > 0 && lastRow < currentTrack.size() - 1)
            removed.add(new PositionPair(positionsModel.getPosition(firstRow - 1), positionsModel.getPosition(lastRow + 1)));

        if (!removed.isEmpty())
            trackOperation.remove(removed);
        if (!added.isEmpty())
            trackOperation.add(added);
    }

    public void handleUpdate(int firstRow, int lastRow) {
        int startIndex = firstRow > 0 ? firstRow - 1 : firstRow;
        int endIndex = lastRow < currentTrack.size() - 1 ? lastRow + 1 : min(lastRow, positionsModel.getRowCount() - 1);

        List<PositionPair> removed = new ArrayList<PositionPair>();
        for (int i = startIndex; i < endIndex; i++)
            removed.add(new PositionPair(positionsModel.getPosition(i), positionsModel.getPosition(i + 1)));

        List<PositionPair> added = new ArrayList<PositionPair>();
        for (int i = startIndex; i < endIndex; i++)
            added.add(new PositionPair(positionsModel.getPosition(i), positionsModel.getPosition(i + 1)));

        if (!removed.isEmpty())
            trackOperation.remove(removed);
        if (!added.isEmpty())
            trackOperation.add(added);
    }

    public void handleRemove(int firstRow, int lastRow) {
        int startIndex = firstRow > 0 ? firstRow - 1 : firstRow;
        int endIndex = lastRow < currentTrack.size() - 1 ? lastRow + 1 : lastRow;

        List<PositionPair> removed = new ArrayList<PositionPair>();
        for (int i = startIndex; i < endIndex; i++)
            removed.add(new PositionPair(positionsModel.getPosition(i), positionsModel.getPosition(i + 1)));

        List<PositionPair> added = new ArrayList<PositionPair>();
        if (firstRow > 0 && lastRow < currentTrack.size() - 1)
            added.add(new PositionPair(positionsModel.getPosition(firstRow - 1), positionsModel.getPosition(lastRow + 1)));

        for (int i = lastRow; i >= firstRow; i--)
            currentTrack.remove(i);

        if (!added.isEmpty())
            trackOperation.add(added);
        if (!removed.isEmpty())
            trackOperation.remove(removed);
    }

    List<PositionPair> getCurrentTrack() {
        List<PositionPair> pairs = new ArrayList<PositionPair>();
        for (int i = 0; i < currentTrack.size(); i++) {
            NavigationPosition first = currentTrack.get(i);
            if (i + 1 < currentTrack.size()) {
                NavigationPosition second = currentTrack.get(i + 1);
                pairs.add(new PositionPair(first, second));
            }
        }
        return pairs;
    }
}
