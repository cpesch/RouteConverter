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

package slash.navigation.converter.gui.models;

import slash.navigation.base.BaseRoute;

import javax.swing.*;
import javax.swing.event.TableModelEvent;

import static javax.swing.event.TableModelEvent.UPDATE;
import static slash.navigation.converter.gui.helpers.PositionHelper.formatElevation;
import static slash.navigation.converter.gui.models.PositionColumns.ELEVATION_COLUMN_INDEX;
import static slash.navigation.gui.helpers.JTableHelper.isFirstToLastRow;

/**
 * A bidirectional adapter that extracts the elevation ascend and descend
 * of a {@link PositionsModel} for display.
 *
 * @author Christian Pesch
 */

public class ElevationToJLabelAdapter extends PositionsModelToDocumentAdapter {
    private final JLabel labelAscend;
    private final JLabel labelDescend;

    public ElevationToJLabelAdapter(PositionsModel positionsModel,
                                    JLabel labelAscend, JLabel labelDescend) {
        super(positionsModel);
        this.labelAscend = labelAscend;
        this.labelDescend = labelDescend;
        initialize();
    }

    private void initialize() {
        updateAdapterFromDelegate(new TableModelEvent(getDelegate()));
    }

    protected String getDelegateValue() {
        throw new UnsupportedOperationException();
    }

    private void updateLabel(double ascend, double descend) {
        labelAscend.setText(ascend > 0 ? formatElevation(ascend) : "-");
        labelDescend.setText(descend > 0 ? formatElevation(descend) : "-");
    }

    protected void updateAdapterFromDelegate(TableModelEvent e) {
        // ignored updates on columns not relevant for ascend and descent calculation
        if (e.getType() == UPDATE &&
                !isFirstToLastRow(e) &&
                !(e.getColumn() == ELEVATION_COLUMN_INDEX))
            return;
        if (getDelegate().isContinousRange())
            return;

        BaseRoute route = getDelegate().getRoute();
        if (route != null) {
            updateLabel(route.getElevationAscend(0, route.getPositionCount() - 1),
                        route.getElevationDescend(0, route.getPositionCount() - 1));
        } else {
            updateLabel(0, 0);
        }
    }
}