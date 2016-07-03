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
import slash.navigation.converter.gui.helpers.LengthCalculator;
import slash.navigation.converter.gui.helpers.LengthCalculatorListener;

import javax.swing.*;
import javax.swing.event.TableModelEvent;

import static javax.swing.SwingUtilities.invokeLater;
import static javax.swing.event.TableModelEvent.UPDATE;
import static slash.common.io.Transfer.formatDuration;
import static slash.navigation.base.RouteCharacteristics.Waypoints;
import static slash.navigation.converter.gui.helpers.PositionHelper.formatDistance;
import static slash.navigation.converter.gui.models.PositionColumns.LATITUDE_COLUMN_INDEX;
import static slash.navigation.converter.gui.models.PositionColumns.LONGITUDE_COLUMN_INDEX;
import static slash.navigation.gui.helpers.JTableHelper.isFirstToLastRow;

/**
 * A bidirectional adapter that extracts the route length and duration
 * of a {@link PositionsModel} for display.
 *
 * @author Christian Pesch
 */

public class LengthToJLabelAdapter extends PositionsModelToDocumentAdapter {
    private final JLabel labelLength;
    private final JLabel labelDuration;

    public LengthToJLabelAdapter(PositionsModel positionsModel,
                                 LengthCalculator lengthCalculator,
                                 JLabel labelLength, JLabel labelDuration) {
        super(positionsModel);
        this.labelLength = labelLength;
        this.labelDuration = labelDuration;

        lengthCalculator.addLengthCalculatorListener(new LengthCalculatorListener() {
            public void calculatedDistance(final double meters, final long seconds) {
                invokeLater(new Runnable() {
                    public void run() {
                        updateLabel(meters, seconds);
                    }
                });
            }
        });
    }

    protected String getDelegateValue() {
        throw new UnsupportedOperationException();
    }

    private void updateLabel(double meters, long seconds) {
        labelLength.setText(meters > 0 ? formatDistance(meters) : "-");
        long milliseconds = seconds * 1000;
        labelDuration.setText(milliseconds > 0 ? formatDuration(milliseconds) : "-");
    }

    protected void updateAdapterFromDelegate(TableModelEvent e) {
        // ignored updates on columns not displayed
        if (e.getType() == UPDATE &&
                !isFirstToLastRow(e) &&
                !(e.getColumn() == LONGITUDE_COLUMN_INDEX || e.getColumn() == LATITUDE_COLUMN_INDEX))
            return;
        if (getDelegate().isContinousRange())
            return;

        BaseRoute route = getDelegate().getRoute();
        if (route != null && route.getCharacteristics() == Waypoints) {
            updateLabel(0, 0);
        }
    }
}