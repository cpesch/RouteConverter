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

package slash.navigation.converter.gui.helpers;

import slash.common.type.CompactCalendar;
import slash.navigation.converter.gui.RouteConverter;
import slash.navigation.converter.gui.mapview.MapView;
import slash.navigation.converter.gui.mapview.MapViewCallback;
import slash.navigation.converter.gui.models.PositionsModel;
import slash.navigation.routing.RoutingService;
import slash.navigation.routing.TravelMode;

import javax.swing.event.ChangeListener;
import java.util.Calendar;
import java.util.logging.Logger;

import static slash.common.type.CompactCalendar.UTC;
import static slash.common.type.CompactCalendar.fromCalendar;
import static slash.navigation.base.RouteCalculations.extrapolateTime;
import static slash.navigation.converter.gui.models.PositionColumns.TIME_COLUMN_INDEX;

/**
 * Implements the callbacks from the {@link MapView} to the other RouteConverter services.
 *
 * @author Christian Pesch
 */

public class MapViewCallbackImpl implements MapViewCallback {
    private static final Logger log = Logger.getLogger(MapViewCallbackImpl.class.getName());
    private PositionsModel positionsModel;

    public MapViewCallbackImpl(PositionsModel positionsModel) {
        this.positionsModel = positionsModel;
    }

    public String createDescription(int index, String description) {
        return RouteConverter.getInstance().getBatchPositionAugmenter().createDescription(index, description);
    }

    public void complementElevation(final int row, final Double longitude, final Double latitude) {
        RouteConverter.getInstance().getBatchPositionAugmenter().addElevations(RouteConverter.getInstance().getPositionsView(), positionsModel, new int[]{row});
    }

    public void complementDescription(final int row, final Double longitude, final Double latitude) {
        RouteConverter.getInstance().getBatchPositionAugmenter().addDescriptions(RouteConverter.getInstance().getPositionsView(), positionsModel, new int[]{row});
    }

    public void complementTime(int row, CompactCalendar time, boolean allowCurrentTime) { // TODO check with BatchPositionAugmenter
        if (time != null)
            return;

        // do not put this in executorService since when called in batches, the edit() must happen before the
        // next time can be complemented
        CompactCalendar interpolated = row - 2 >= 0 ? extrapolateTime(positionsModel.getPosition(row),
                positionsModel.getPosition(row - 1), positionsModel.getPosition(row - 2)) : null;
        // since interpolation is just between the previous positions this leads to errors when inserting
        // more than one position for which no time can be interpolated from the previous positions
        if (interpolated == null && allowCurrentTime)
            interpolated = fromCalendar(Calendar.getInstance(UTC));
        positionsModel.edit(row, TIME_COLUMN_INDEX, interpolated, -1, null, true, false);
    }

    public RoutingService getRoutingService() {
        return RouteConverter.getInstance().getRoutingServiceFacade().getRoutingService();
    }

    public TravelMode getTravelMode() {
        return RouteConverter.getInstance().getRoutingServiceFacade().getTravelMode();
    }

    public boolean isAvoidHighways() {
        return RouteConverter.getInstance().getRoutingServiceFacade().isAvoidHighways();
    }

    public boolean isAvoidTolls() {
        return RouteConverter.getInstance().getRoutingServiceFacade().isAvoidTolls();
    }

    public void addChangeListener(ChangeListener l) {
        RouteConverter.getInstance().getRoutingServiceFacade().addChangeListener(l);
    }
}
