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
package slash.navigation.converter.gui.mapview;

import slash.navigation.converter.gui.models.PositionsModel;

import java.util.ArrayList;
import java.util.List;

/**
 * Stores the current track state and minimizes {@link TrackOperation}s.
 * Used to reduce the number of interactions between event listener and map UI.
 *
 * @author Christian Pesch
 * @see TrackOperation
 */

public class TrackUpdater {
    private final PositionsModel positionsModel;
    private final TrackOperation trackOperation;
    private final List<PositionPair> currentTrack = new ArrayList<PositionPair>();

    public TrackUpdater(PositionsModel positionsModel, TrackOperation trackOperation) {
        this.positionsModel = positionsModel;
        this.trackOperation = trackOperation;
    }

    public void handleAdd(int firstRow, int lastRow) {
        for (int i = firstRow; i < lastRow; i++) {
            throw new UnsupportedOperationException();
        }
    }

    public void handleUpdate(int firstRow, int lastRow) {
        for (int i = firstRow; i < lastRow; i++) {
            throw new UnsupportedOperationException();
        }
    }

    public void handleRemove(int firstRow, int lastRow) {
        for (int i = firstRow; i < lastRow; i++) {
            throw new UnsupportedOperationException();
        }
    }

    List<PositionPair> getCurrentTrack() {
        return currentTrack;
    }
}
