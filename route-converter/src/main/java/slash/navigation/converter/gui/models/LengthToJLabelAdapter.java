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
import slash.navigation.base.RouteCharacteristics;
import slash.navigation.converter.gui.RouteConverter;
import slash.navigation.converter.gui.helper.LengthCalculator;
import slash.navigation.converter.gui.helper.LengthCalculatorListener;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import java.text.MessageFormat;
import java.util.Calendar;
import java.util.Date;

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
            public void calculatedDistance(final int meters, final int seconds) {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        updateLabel(meters, seconds * 1000);
                    }
                });
            }
        });
    }


    protected String getDelegateValue() {
        throw new UnsupportedOperationException();
    }

    private void updateLabel(int meters, long milliSeconds) {
        labelLength.setText(meters > 0 ? MessageFormat.format(RouteConverter.getBundle().getString("length-value"), meters / 1000.0) : "-");
        Calendar calendar = Calendar.getInstance();
        calendar.clear();
        calendar.add(Calendar.MILLISECOND, (int) milliSeconds);
        Date date = calendar.getTime();
        labelDuration.setText(MessageFormat.format(RouteConverter.getBundle().getString("duration-value"), date));
    }

    protected void updateAdapterFromDelegate(TableModelEvent e) {
        // ignored updates on columns not displayed
        if (e.getType() == TableModelEvent.UPDATE &&
                !(e.getColumn() == PositionColumns.LONGITUDE_COLUMN_INDEX ||
                        e.getColumn() == PositionColumns.LATITUDE_COLUMN_INDEX))
            return;

        BaseRoute route = getDelegate().getRoute();
        if (route != null && route.getCharacteristics() == RouteCharacteristics.Waypoints) {
            updateLabel(0, 0);
        }
    }
}