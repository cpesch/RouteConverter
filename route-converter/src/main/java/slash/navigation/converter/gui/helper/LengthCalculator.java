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

package slash.navigation.converter.gui.helper;

import slash.common.type.CompactCalendar;
import slash.navigation.base.BaseNavigationPosition;
import slash.navigation.base.RouteCharacteristics;
import slash.navigation.converter.gui.RouteConverter;
import slash.navigation.converter.gui.mapview.AbstractMapViewListener;
import slash.navigation.converter.gui.models.CharacteristicsModel;
import slash.navigation.converter.gui.models.PositionColumns;
import slash.navigation.converter.gui.models.PositionsModel;

import javax.swing.event.ListDataEvent;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;

import static slash.common.helpers.ThreadHelper.safeJoin;
import static slash.navigation.base.RouteCharacteristics.Route;
import static slash.navigation.base.RouteCharacteristics.Waypoints;

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
    private boolean running = true, recalculate = false;

    public LengthCalculator() {
        initialize();
    }

    private RouteCharacteristics getCharacteristics() {
        return positionsModel.getRoute().getCharacteristics();
    }

    public void initialize(PositionsModel positionsModel, CharacteristicsModel characteristicsModel) {
        this.positionsModel = positionsModel;

        positionsModel.addTableModelListener(new TableModelListener() {
            public void tableChanged(TableModelEvent e) {
                // ignored updates on columns not relevant for length calculation
                if (e.getType() == TableModelEvent.UPDATE &&
                        !(e.getColumn() == PositionColumns.LONGITUDE_COLUMN_INDEX ||
                                e.getColumn() == PositionColumns.LATITUDE_COLUMN_INDEX ||
                                e.getColumn() == TableModelEvent.ALL_COLUMNS))
                    return;

                calculateDistance();
            }
        });

        characteristicsModel.addListDataListener(new AbstractListDataListener() {
            public void process(ListDataEvent e) {
                // ignore events following setRoute()
                if (e.getType() == ListDataEvent.CONTENTS_CHANGED && e.getIndex0() == CharacteristicsModel.IGNORE && e.getIndex1() == CharacteristicsModel.IGNORE)
                    return;
                calculateDistance();
            }
        });

        RouteConverter.getInstance().addMapViewListener(new AbstractMapViewListener() {
            public void calculatedDistance(int meters, int seconds) {
                fireCalculatedDistance(meters, seconds);
            }
        });
    }

    private final List<LengthCalculatorListener> lengthCalculatorListeners = new CopyOnWriteArrayList<LengthCalculatorListener>();

    public void addLengthCalculatorListener(LengthCalculatorListener listener) {
        lengthCalculatorListeners.add(listener);
    }

    private void fireCalculatedDistance(int meters, int seconds) {
        for (LengthCalculatorListener listener : lengthCalculatorListeners) {
            listener.calculatedDistance(meters, seconds);
        }
    }

    private void calculateDistance() {
        if (getCharacteristics().equals(Waypoints)) {
            fireCalculatedDistance(0, 0);
            return;
        }

        if (getCharacteristics().equals(Route) && RouteConverter.getInstance().isMapViewAvailable())
            return;

        synchronized (notificationMutex) {
            recalculate = true;
            notificationMutex.notifyAll();
        }
    }

    private void recalculateDistance() {
        fireCalculatedDistance(0, 0);

        double distanceMeters = 0.0;
        long totalTimeMilliSeconds = 0;
        CompactCalendar minimumTime = null, maximumTime = null;
        BaseNavigationPosition previous = null;
        for (int i = 0; i < positionsModel.getRowCount(); i++) {
            BaseNavigationPosition next = positionsModel.getPosition(i);
            if (previous != null) {
                Double distance = previous.calculateDistance(next);
                if (distance != null)
                    distanceMeters += distance;
                Long time = previous.calculateTime(next);
                if (time != null && time > 0)
                    totalTimeMilliSeconds += time;
            }

            CompactCalendar time = next.getTime();
            if (time != null) {
                if (minimumTime == null || time.before(minimumTime))
                    minimumTime = time;
                if (maximumTime == null || time.after(maximumTime))
                    maximumTime = time;
            }

            if (i > 0 && i % 100 == 0)
                fireCalculatedDistance((int) distanceMeters, totalTimeMilliSeconds > 0 ? (int) (totalTimeMilliSeconds / 1000) : 0);

            previous = next;
        }

        int summedUp = totalTimeMilliSeconds > 0 ? (int) totalTimeMilliSeconds / 1000 : 0;
        int maxMinusMin = minimumTime != null ? (int) ((maximumTime.getTimeInMillis() - minimumTime.getTimeInMillis()) / 1000) : 0;
        fireCalculatedDistance((int) distanceMeters, Math.max(maxMinusMin, summedUp));
    }

    private void initialize() {
        lengthCalculator = new Thread(new Runnable() {
            public void run() {
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
            }
        }, "LengthCalculator");
        lengthCalculator.start();
    }

    public void dispose() {
        long start = System.currentTimeMillis();
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
            long end = System.currentTimeMillis();
            log.info("LengthCalculator stopped after " + (end - start) + " ms");
        }
    }
}
