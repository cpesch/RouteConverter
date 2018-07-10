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

package slash.navigation.mapview.mapsforge.overlays;

import org.mapsforge.core.graphics.Bitmap;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.map.layer.overlay.Marker;
import slash.navigation.converter.gui.models.PositionColumnValues;
import slash.navigation.converter.gui.models.PositionsModel;
import slash.navigation.mapview.mapsforge.updater.PositionWithLayer;

import java.util.Arrays;
import java.util.logging.Logger;

import static java.util.Arrays.asList;
import static javax.swing.SwingUtilities.invokeLater;
import static slash.navigation.converter.gui.models.PositionColumns.LATITUDE_COLUMN_INDEX;
import static slash.navigation.converter.gui.models.PositionColumns.LONGITUDE_COLUMN_INDEX;

/**
 * A {@code Marker} that supports dragging.
 *
 * @author Christian Pesch
 */
public class DraggableMarker extends Marker {
    private static final Logger log = Logger.getLogger(DraggableMarker.class.getName());
    private final PositionsModel positionsModel;
    private final PositionWithLayer positionWithLayer;

    public DraggableMarker(PositionsModel positionsModel, PositionWithLayer positionWithLayer, LatLong latLong, Bitmap bitmap, int horizontalOffset, int verticalOffset) {
        super(latLong, bitmap, horizontalOffset, verticalOffset);
        this.positionsModel = positionsModel;
        this.positionWithLayer = positionWithLayer;
    }

    public boolean onTap(LatLong tapLatLong, org.mapsforge.core.model.Point viewPosition, org.mapsforge.core.model.Point tapPoint) {
        return contains(viewPosition, tapPoint);
    }

    public void onDrop(final LatLong latLong) {
        final int index = positionsModel.getIndex(positionWithLayer.getPosition());
        if(index == -1) {
            log.warning("Marker without position " + this);
            return;
        }

        invokeLater(new Runnable() {
            public void run() {
                positionsModel.edit(index, new PositionColumnValues(asList(LONGITUDE_COLUMN_INDEX, LATITUDE_COLUMN_INDEX),
                        Arrays.<Object>asList(latLong.longitude, latLong.latitude)), true, true);
            }
        });
    }
}
