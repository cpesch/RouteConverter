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

import slash.navigation.base.RouteCharacteristics;
import slash.navigation.common.DistanceAndTime;
import slash.navigation.common.DistanceAndTimeAggregator;
import slash.navigation.common.NavigationPosition;
import slash.navigation.converter.gui.models.CharacteristicsModel;
import slash.navigation.converter.gui.models.PositionsModel;
import slash.navigation.gui.helpers.AbstractListDataListener;

import javax.swing.event.ListDataEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import static java.lang.System.currentTimeMillis;
import static javax.swing.event.TableModelEvent.ALL_COLUMNS;
import static javax.swing.event.TableModelEvent.UPDATE;
import static slash.common.helpers.ThreadHelper.safeJoin;
import static slash.navigation.base.RouteCharacteristics.Track;
import static slash.navigation.converter.gui.models.PositionColumns.*;
import static slash.navigation.gui.events.IgnoreEvent.isIgnoreEvent;
import static slash.navigation.gui.helpers.JTableHelper.isFirstToLastRow;

/**
 * Helps to calculate the length of position list of type route and track.
 *
 * @author Christian Pesch
 */

public class LengthCalculator {
    private static final Logger log = Logger.getLogger(LengthCalculator.class.getName());

    private PositionsModel positionsModel;
    private DistanceAndTimeAggregator distanceAndTimeAggregator;
    private Thread lengthCalculator;
    private final Object notificationMutex = new Object();
    private boolean running = true, clear, recalculate;

    public LengthCalculator() {
        initialize();
    }

    private RouteCharacteristics getCharacteristics() {
        return positionsModel.getRoute().getCharacteristics();
    }

    public void initialize(PositionsModel positionsModel, CharacteristicsModel characteristicsModel, DistanceAndTimeAggregator distanceAndTimeAggregator) {
        this.positionsModel = positionsModel;
        this.distanceAndTimeAggregator = distanceAndTimeAggregator;

        positionsModel.addTableModelListener(e -> {
            // ignored updates on columns not relevant for length calculation
            if (e.getType() == UPDATE &&
                    !isFirstToLastRow(e) &&
                    !(e.getColumn() == LONGITUDE_COLUMN_INDEX ||
                            e.getColumn() == LATITUDE_COLUMN_INDEX ||
                            e.getColumn() == ALL_COLUMNS))
                return;
            // ignore distance and time column updates from the DistanceAndTimeAggregator
            if (e.getColumn() == DISTANCE_COLUMN_INDEX || e.getColumn() == DISTANCE_DIFFERENCE_COLUMN_INDEX || e.getColumn() == TIME_COLUMN_INDEX)
                return;
            if (getPositionsModel().isContinousRange())
                return;
            calculateDistance();
        });

        characteristicsModel.addListDataListener(new AbstractListDataListener() {
            public void process(ListDataEvent e) {
                // ignore events following setRoute()
                if (isIgnoreEvent(e))
                    return;
                calculateDistance();
            }
        });
    }

    private PositionsModel getPositionsModel() {
        return positionsModel;
    }

    private void calculateDistance() {
        synchronized (notificationMutex) {
            this.clear = true;
            this.recalculate = getCharacteristics().equals(Track);
            notificationMutex.notifyAll();
        }
    }

    private void clearDistance() {
        distanceAndTimeAggregator.clearDistancesAndTimes();
    }

    private void addDistance() {
        Map<Integer,DistanceAndTime> result = new HashMap<>();
        NavigationPosition previous = null;
        for (int i = 0; i < positionsModel.getRowCount(); i++) {
            NavigationPosition next = positionsModel.getPosition(i);
            if (previous != null) {
                result.put(i, new DistanceAndTime(previous.calculateDistance(next), previous.calculateTime(next)));
            }
            previous = next;
        }
        distanceAndTimeAggregator.addDistancesAndTimes(result);
    }

    private void initialize() {
        lengthCalculator = new Thread(() -> {
            while (true) {
                synchronized (notificationMutex) {
                    try {
                        notificationMutex.wait(1000);
                    } catch (InterruptedException e) {
                        // ignore this
                    }

                    if (!running)
                        return;

                    if (clear)
                        clearDistance();
                    clear = false;

                    if (recalculate)
                        addDistance();
                    recalculate = false;
                }
            }
        }, "LengthCalculator");
        lengthCalculator.start();
    }

    public void dispose() {
        long start = currentTimeMillis();
        synchronized (notificationMutex) {
            running = false;
            notificationMutex.notifyAll();
        }

        if (lengthCalculator != null) {
            try {
                safeJoin(lengthCalculator);
            } catch (InterruptedException e) {
                // intentionally left empty
            }
            long end = currentTimeMillis();
            log.info("LengthCalculator stopped after " + (end - start) + " ms");
        }
    }
}
