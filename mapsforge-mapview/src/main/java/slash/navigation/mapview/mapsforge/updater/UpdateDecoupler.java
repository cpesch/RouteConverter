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
import java.util.logging.Logger;

import static java.lang.Integer.MAX_VALUE;
import static java.lang.String.format;
import static java.util.logging.Level.SEVERE;
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
    private static final Logger log = Logger.getLogger(UpdateDecoupler.class.getName());

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
            try {
                // remove all from previous event map updater
                eventMapUpdater.handleRemove(0, MAX_VALUE);

                // select current event map updater and let him add all
                eventMapUpdater = updaterFactory.apply(positionsModel.getRoute().getCharacteristics());
                eventMapUpdater.handleAdd(0, positionsModel.getRowCount() - 1);
            } catch (RuntimeException e) {
                log.log(SEVERE, "Cannot replace route: " + e, e);
                rebuild(e);
            }
        });
    }

    public void handleUpdate(final int eventType, final int firstRow, final int lastRow) {
        executor.execute(() -> {
            try {
                switch (eventType) {
                    case INSERT -> eventMapUpdater.handleAdd(firstRow, lastRow);
                    case UPDATE -> eventMapUpdater.handleUpdate(firstRow, lastRow);
                    case DELETE -> eventMapUpdater.handleRemove(firstRow, lastRow);
                }
            } catch (RuntimeException e) {
                log.log(SEVERE, format("Cannot handle event type %d for rows %d..%d: %s", eventType, firstRow, lastRow, e), e);
                rebuild(e);
            }
        });
    }

    // Self-heals a desynced updater instead of leaving it broken for the rest of the session:
    // a queued event racing a later file's already-applied rows previously desynced the
    // updater's own state permanently (see TrackUpdater/WaypointUpdater). The failing event is
    // not fed to the crash reporter since this rebuild is expected to recover on its own.
    private void rebuild(RuntimeException cause) {
        try {
            eventMapUpdater.handleRemove(0, MAX_VALUE);
            int rowCount = positionsModel.getRowCount();
            if (rowCount > 0)
                eventMapUpdater.handleAdd(0, rowCount - 1);
        } catch (RuntimeException e) {
            log.log(SEVERE, "Cannot rebuild after " + cause + ": " + e, e);
        }
    }

    public void dispose() {
        executor.shutdownNow();
    }
}
