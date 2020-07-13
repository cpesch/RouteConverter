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
import slash.navigation.base.RouteCharacteristics;
import slash.navigation.common.DistanceAndTime;
import slash.navigation.common.DistanceAndTimeAggregator;
import slash.navigation.common.NavigationPosition;
import slash.navigation.converter.gui.models.CharacteristicsModel;
import slash.navigation.converter.gui.models.PositionsModel;

import javax.swing.event.ListDataEvent;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;

import static java.lang.Math.max;
import static java.lang.System.currentTimeMillis;
import static javax.swing.event.TableModelEvent.ALL_COLUMNS;
import static javax.swing.event.TableModelEvent.UPDATE;
import static slash.common.helpers.ThreadHelper.safeJoin;
import static slash.common.io.Transfer.isEmpty;
import static slash.navigation.base.RouteCharacteristics.Route;
import static slash.navigation.base.RouteCharacteristics.Waypoints;
import static slash.navigation.common.DistanceAndTime.ZERO;
import static slash.navigation.converter.gui.models.PositionColumns.LATITUDE_COLUMN_INDEX;
import static slash.navigation.converter.gui.models.PositionColumns.LONGITUDE_COLUMN_INDEX;
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
    private Thread lengthCalculator;
    private final Object notificationMutex = new Object();
    private boolean running = true, recalculate;

    public LengthCalculator() {
        initialize();
    }

    private RouteCharacteristics getCharacteristics() {
        return positionsModel.getRoute().getCharacteristics();
    }

    public void initialize(PositionsModel positionsModel, CharacteristicsModel characteristicsModel) {
        this.positionsModel = positionsModel;

        positionsModel.addTableModelListener(e -> {
            // ignored updates on columns not relevant for length calculation
            if (e.getType() == UPDATE &&
                    !isFirstToLastRow(e) &&
                    !(e.getColumn() == LONGITUDE_COLUMN_INDEX ||
                            e.getColumn() == LATITUDE_COLUMN_INDEX ||
                            e.getColumn() == ALL_COLUMNS))
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

    private final List<LengthCalculatorListener> lengthCalculatorListeners = new CopyOnWriteArrayList<>();

    public void addLengthCalculatorListener(LengthCalculatorListener listener) {
        lengthCalculatorListeners.add(listener);
    }

    private void fireCalculatedDistance(DistanceAndTime distanceAndTime) {
        for (LengthCalculatorListener listener : lengthCalculatorListeners) {
            listener.calculatedDistanceAndTime(distanceAndTime);
        }
    }

    public void calculateDistanceFromRouting(Map<Integer, DistanceAndTime> indexToDistanceAndTime) {
        fireCalculatedDistance(DistanceAndTimeAggregator.max(indexToDistanceAndTime));
    }


    private void calculateDistance() {
        if (getCharacteristics().equals(Waypoints)) {
            fireCalculatedDistance(ZERO);
            return;
        }
        if (getCharacteristics().equals(Route))
            return;

        synchronized (notificationMutex) {
            recalculate = true;
            notificationMutex.notifyAll();
        }
    }

    private void recalculateDistance() {
        fireCalculatedDistance(ZERO);

        double aggregatedDistance = 0.0;
        long aggregatedTime = 0;
        CompactCalendar minimumTime = null, maximumTime = null;
        NavigationPosition previous = null;
        for (int i = 0; i < positionsModel.getRowCount(); i++) {
            NavigationPosition next = positionsModel.getPosition(i);
            if (previous != null) {
                Double distance = previous.calculateDistance(next);
                if (!isEmpty(distance))
                    aggregatedDistance += distance;
                Long time = previous.calculateTime(next);
                if (time != null && time > 0)
                    aggregatedTime += time;
            }

            CompactCalendar time = next.getTime();
            if (time != null) {
                if (minimumTime == null || time.before(minimumTime))
                    minimumTime = time;
                if (maximumTime == null || time.after(maximumTime))
                    maximumTime = time;
            }

            if (i > 0 && i % 100 == 0)
                fireCalculatedDistance(new DistanceAndTime(aggregatedDistance, aggregatedTime > 0 ? aggregatedTime : 0));

            previous = next;
        }

        long summedUp = aggregatedTime > 0 ? aggregatedTime : 0;
        long maxMinusMin = minimumTime != null ? (maximumTime.getTimeInMillis() - minimumTime.getTimeInMillis()) : 0;
        fireCalculatedDistance(new DistanceAndTime(aggregatedDistance, max(maxMinusMin, summedUp)));
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
                    if (!recalculate)
                        continue;
                    recalculate = false;
                }
                recalculateDistance();
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
