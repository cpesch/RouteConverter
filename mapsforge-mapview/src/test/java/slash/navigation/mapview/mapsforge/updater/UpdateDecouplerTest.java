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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import slash.navigation.base.BaseRoute;
import slash.navigation.base.RouteCharacteristics;
import slash.navigation.converter.gui.models.PositionsModel;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;

import static java.lang.Integer.MAX_VALUE;
import static javax.swing.event.TableModelEvent.*;
import static org.mockito.Mockito.*;
import static slash.navigation.base.RouteCharacteristics.Route;
import static slash.navigation.base.RouteCharacteristics.Waypoints;

public class UpdateDecouplerTest {
    private final EventMapUpdater waypointUpdater = mock(EventMapUpdater.class);
    private final EventMapUpdater routeUpdater = mock(EventMapUpdater.class);
    private PositionsModel positionsModel;
    private ExecutorService executor;
    private UpdateDecoupler decoupler;

    @Before
    public void setUp() {
        positionsModel = mock(PositionsModel.class);
        executor = Executors.newSingleThreadExecutor();
        Function<RouteCharacteristics, EventMapUpdater> factory = characteristics ->
                characteristics == Route ? routeUpdater : waypointUpdater;
        decoupler = new UpdateDecoupler(positionsModel, factory, executor);
    }

    @After
    public void tearDown() {
        decoupler.dispose();
    }

    private void awaitQueuedWork() throws InterruptedException, ExecutionException {
        // single-thread executor runs FIFO, so a queued no-op completes after the prior task
        executor.submit(() -> {
        }).get();
    }

    @Test
    public void insertAddsToCurrentUpdater() throws Exception {
        decoupler.handleUpdate(INSERT, 2, 5);
        awaitQueuedWork();

        verify(waypointUpdater).handleAdd(2, 5);
    }

    @Test
    public void updateUpdatesCurrentUpdater() throws Exception {
        decoupler.handleUpdate(UPDATE, 1, 3);
        awaitQueuedWork();

        verify(waypointUpdater).handleUpdate(1, 3);
    }

    @Test
    public void deleteRemovesFromCurrentUpdater() throws Exception {
        decoupler.handleUpdate(DELETE, 0, 4);
        awaitQueuedWork();

        verify(waypointUpdater).handleRemove(0, 4);
    }

    @Test
    public void publicConstructorRunsWorkOnABackgroundExecutor() {
        UpdateDecoupler backgroundDecoupler = new UpdateDecoupler(positionsModel, characteristics -> waypointUpdater);
        try {
            backgroundDecoupler.handleUpdate(INSERT, 0, 2);
            // its own single-thread executor runs the work asynchronously
            verify(waypointUpdater, timeout(2000)).handleAdd(0, 2);
        } finally {
            backgroundDecoupler.dispose();
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void replaceRouteClearsPreviousUpdaterAndFillsSelectedOne() throws Exception {
        BaseRoute route = mock(BaseRoute.class);
        when(route.getCharacteristics()).thenReturn(Route);
        when(positionsModel.getRoute()).thenReturn(route);
        when(positionsModel.getRowCount()).thenReturn(10);

        decoupler.replaceRoute();
        awaitQueuedWork();

        // starts on the Waypoints updater, so that one is cleared, and the Route updater is filled
        verify(waypointUpdater).handleRemove(0, MAX_VALUE);
        verify(routeUpdater).handleAdd(0, 9);
    }
}
