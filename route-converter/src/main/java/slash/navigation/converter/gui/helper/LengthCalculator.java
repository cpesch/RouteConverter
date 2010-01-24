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

import slash.common.io.CompactCalendar;
import slash.navigation.BaseNavigationPosition;
import slash.navigation.RouteCharacteristics;
import slash.navigation.converter.gui.RouteConverter;
import slash.navigation.converter.gui.mapview.MapViewListener;
import slash.navigation.converter.gui.models.CharacteristicsModel;
import slash.navigation.converter.gui.models.PositionsModel;

import javax.swing.event.ListDataEvent;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Helps to calculate the length of position list of type route and track.
 *
 * @author Christian Pesch
 */

public class LengthCalculator {
    private PositionsModel positionsModel;

    private RouteCharacteristics getCharacteristics() {
        return positionsModel.getRoute().getCharacteristics();
    }

    public void initialize(PositionsModel positionsModel, CharacteristicsModel characteristicsModel) {
        this.positionsModel = positionsModel;

        positionsModel.addTableModelListener(new TableModelListener() {
            public void tableChanged(TableModelEvent e) {
                calculateDistance();
            }
        });

        characteristicsModel.addListDataListener(new AbstractListDataListener() {
            public void process(ListDataEvent e) {
                calculateDistance();
            }
        });

        RouteConverter.getInstance().addMapViewListener(new MapViewListener() {
            public void calculatedDistance(int meters, int seconds) {
                fireCalculatedDistance(meters, seconds);
            }

            public void receivedCallback(int port) {
            }
        });
    }

    private final List<LengthCalculatorListener> lengthCalculatorListeners = new CopyOnWriteArrayList<LengthCalculatorListener>();

    public void addLengthCalculatorListener(LengthCalculatorListener listener) {
        lengthCalculatorListeners.add(listener);
    }

    public void removeLengthCalculatorListener(LengthCalculatorListener listener) {
        lengthCalculatorListeners.remove(listener);
    }

    private void fireCalculatedDistance(int meters, int seconds) {
        for (LengthCalculatorListener listener : lengthCalculatorListeners) {
            listener.calculatedDistance(meters, seconds);
        }
    }

    private void calculateDistance() {
        if (getCharacteristics().equals(RouteCharacteristics.Waypoints)) {
            fireCalculatedDistance(0, 0);
            return;
        }

        if(getCharacteristics().equals(RouteCharacteristics.Route) && RouteConverter.getInstance().isMapViewAvailable())
            return;

        new Thread(new Runnable() {
            public void run() {
                fireCalculatedDistance(0, 0);

                int meters = 0;
                long delta = 0;
                Calendar minimumTime = null, maximumTime = null;
                BaseNavigationPosition previous = null;
                for (int i = 0; i < positionsModel.getRowCount(); i++) {
                    BaseNavigationPosition next = positionsModel.getPosition(i);
                    if (previous != null) {
                        Double distance = previous.calculateDistance(next);
                        if (distance != null)
                            meters += distance;
                        Long time = previous.calculateTime(next);
                        if (time != null)
                            delta += time;
                    }

                    CompactCalendar time = next.getTime();
                    if (time != null) {
                        Calendar calendar = time.getCalendar();
                        if (minimumTime == null || calendar.before(minimumTime))
                            minimumTime = calendar;
                        if (maximumTime == null || calendar.after(maximumTime))
                            maximumTime = calendar;
                    }

                    if (i % 100 == 0)
                        fireCalculatedDistance(meters, delta > 0 ? (int) (delta / 1000) : 0);

                    previous = next;
                }

                int summedUp = delta > 0 ? (int) delta / 1000 : 0;
                int maxMinusMin = minimumTime != null ? (int) ((maximumTime.getTimeInMillis() - minimumTime.getTimeInMillis()) / 1000) : 0;
                fireCalculatedDistance(meters, Math.max(maxMinusMin, summedUp));
            }
        }, "BeelineLengthCalculator").start();
    }
}
