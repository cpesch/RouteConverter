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
import slash.navigation.common.DistanceAndTime;
import slash.navigation.common.DistanceAndTimeAggregator;

import javax.swing.*;
import javax.swing.event.TableModelEvent;

import static slash.common.io.Transfer.formatDuration;
import static slash.navigation.base.RouteCharacteristics.Waypoints;
import static slash.navigation.common.DistanceAndTime.ZERO;
import static slash.navigation.converter.gui.helpers.PositionHelper.formatDistance;

/**
 * A bidirectional adapter that extracts the route length and duration
 * of a {@link PositionsModel} for display.
 *
 * @author Christian Pesch
 */

public class LengthToJLabelAdapter extends PositionsModelToDocumentAdapter {
    private final DistanceAndTimeAggregator distanceAndTimeAggregator;
    private final JLabel labelLength;
    private final JLabel labelDuration;

    public LengthToJLabelAdapter(PositionsModel positionsModel, DistanceAndTimeAggregator distanceAndTimeAggregator,
                                 JLabel labelLength, JLabel labelDuration) {
        super(positionsModel);
        this.distanceAndTimeAggregator = distanceAndTimeAggregator;
        this.labelLength = labelLength;
        this.labelDuration = labelDuration;
    }

    protected String getDelegateValue() {
        throw new UnsupportedOperationException();
    }

    private void updateLabel(DistanceAndTime distanceAndTime) {
        labelLength.setText(distanceAndTime.getDistance() > 0 ? formatDistance(distanceAndTime.getDistance()) : "-");
        labelDuration.setText(distanceAndTime.getTimeInMillis() > 0 ? formatDuration(distanceAndTime.getTimeInMillis()) : "-");
    }

    protected void updateAdapterFromDelegate(TableModelEvent e) {
        @SuppressWarnings("rawtypes")
        BaseRoute route = getDelegate().getRoute();
        if (route != null && !route.getCharacteristics().equals(Waypoints)) {
            updateLabel(distanceAndTimeAggregator.getTotalDistanceAndTime());
        } else {
            updateLabel(ZERO);
        }
    }
}
