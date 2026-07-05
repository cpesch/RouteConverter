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

import slash.navigation.base.RouteCharacteristics;
import slash.navigation.converter.gui.models.PositionsModel;

import java.util.concurrent.ExecutorService;
import java.util.function.Function;

import static java.lang.Integer.MAX_VALUE;
import static javax.swing.event.TableModelEvent.*;
import static slash.common.helpers.ThreadHelper.createSingleThreadExecutor;
import static slash.navigation.base.RouteCharacteristics.Waypoints;

/**
 * Decouples position list changes from the map updating: serializes the work onto a single
 * background thread and dispatches it to the {@link EventMapUpdater} for the list's current
 * characteristics. Keeps the event listeners off the rendering work.
 *
 * @author Christian Pesch
 */

public class UpdateDecoupler {
    private final ExecutorService executor;
    private final PositionsModel positionsModel;
    private final Function<RouteCharacteristics, EventMapUpdater> updaterFactory;
    private EventMapUpdater eventMapUpdater;

    public UpdateDecoupler(PositionsModel positionsModel, Function<RouteCharacteristics, EventMapUpdater> updaterFactory) {
        this(positionsModel, updaterFactory, createSingleThreadExecutor("UpdateDecoupler"));
    }

    /*for tests*/ UpdateDecoupler(PositionsModel positionsModel, Function<RouteCharacteristics, EventMapUpdater> updaterFactory,
                                  ExecutorService executor) {
        this.positionsModel = positionsModel;
        this.updaterFactory = updaterFactory;
        this.executor = executor;
        this.eventMapUpdater = updaterFactory.apply(Waypoints);
    }

    public void replaceRoute() {
        executor.execute(() -> {
            // remove all from previous event map updater
            eventMapUpdater.handleRemove(0, MAX_VALUE);

            // select current event map updater and let him add all
            eventMapUpdater = updaterFactory.apply(positionsModel.getRoute().getCharacteristics());
            eventMapUpdater.handleAdd(0, positionsModel.getRowCount() - 1);
        });
    }

    public void handleUpdate(final int eventType, final int firstRow, final int lastRow) {
        executor.execute(() -> {
            switch (eventType) {
                case INSERT -> eventMapUpdater.handleAdd(firstRow, lastRow);
                case UPDATE -> eventMapUpdater.handleUpdate(firstRow, lastRow);
                case DELETE -> eventMapUpdater.handleRemove(firstRow, lastRow);
            }
        });
    }

    public void dispose() {
        executor.shutdownNow();
    }
}
