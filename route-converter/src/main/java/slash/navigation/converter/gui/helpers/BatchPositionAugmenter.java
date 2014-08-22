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
import slash.navigation.common.LongitudeAndLatitude;
import slash.navigation.common.NavigationPosition;
import slash.navigation.common.NumberPattern;
import slash.navigation.converter.gui.RouteConverter;
import slash.navigation.converter.gui.models.PositionColumnValues;
import slash.navigation.converter.gui.models.PositionsModel;
import slash.navigation.geonames.GeoNamesService;
import slash.navigation.googlemaps.GoogleMapsService;
import slash.navigation.gui.Application;
import slash.navigation.gui.events.ContinousRange;
import slash.navigation.gui.events.RangeOperation;
import slash.navigation.gui.notifications.NotificationManager;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.logging.Logger;

import static java.lang.Math.min;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static javax.swing.JOptionPane.ERROR_MESSAGE;
import static javax.swing.JOptionPane.showMessageDialog;
import static javax.swing.SwingUtilities.invokeLater;
import static javax.swing.event.TableModelEvent.ALL_COLUMNS;
import static slash.common.io.Transfer.widthInDigits;
import static slash.navigation.base.RouteCalculations.intrapolateTime;
import static slash.navigation.base.RouteComments.formatNumberedPosition;
import static slash.navigation.base.RouteComments.getNumberedPosition;
import static slash.navigation.converter.gui.models.PositionColumns.*;
import static slash.navigation.gui.helpers.JTableHelper.scrollToPosition;
import static slash.navigation.gui.helpers.UIHelper.startWaitCursor;
import static slash.navigation.gui.helpers.UIHelper.stopWaitCursor;

/**
 * Helps to augment a batch of positions with geocoded coordinates, elevation,
 * position number for its description, postal address, populated place and speed
 * information.
 *
 * @author Christian Pesch
 */

public class BatchPositionAugmenter {
    private static final Logger log = Logger.getLogger(BatchPositionAugmenter.class.getName());

    private final JFrame frame;
    private final JTable positionsView;
    private final PositionsModel positionsModel;

    private final ExecutorService executor = newSingleThreadExecutor();
    private final ElevationServiceFacade elevationServiceFacade = RouteConverter.getInstance().getElevationServiceFacade();
    private final GoogleMapsService googleMapsService = new GoogleMapsService();
    private final GeoNamesService geonamesService = new GeoNamesService();
    private static final Object notificationMutex = new Object();
    private boolean running = true;

    public BatchPositionAugmenter(JTable positionsView, PositionsModel positionsModel, JFrame frame) {
        this.positionsView = positionsView;
        this.positionsModel = positionsModel;
        this.frame = frame;
    }

    public void interrupt() {
        synchronized (notificationMutex) {
            this.running = false;
        }
    }

    public void dispose() {
        executor.shutdownNow();
    }


    private interface OverwritePredicate {
        boolean shouldOverwrite(NavigationPosition position);
    }

    private static final OverwritePredicate TAUTOLOGY_PREDICATE = new OverwritePredicate() {
        public boolean shouldOverwrite(NavigationPosition position) {
            return true;
        }
    };

    private static final OverwritePredicate COORDINATE_PREDICATE = new OverwritePredicate() {
        public boolean shouldOverwrite(NavigationPosition position) {
            return position.hasCoordinates();
        }
    };


    private interface Operation {
        String getName();
        int getColumnIndex();
        void performOnStart();
        boolean run(int index, NavigationPosition position) throws Exception;
        String getErrorMessage();
    }


    private NotificationManager getNotificationManager() {
        return Application.getInstance().getContext().getNotificationManager();
    }

    private class CancelAction extends AbstractAction {
        private boolean canceled = false;

        public boolean isCanceled() {
            return canceled;
        }

        public void actionPerformed(ActionEvent e) {
            this.canceled = true;
        }
    }

    private void executeOperation(final JTable positionsTable,
                                  final PositionsModel positionsModel,
                                  final int[] rows,
                                  final boolean slowOperation,
                                  final OverwritePredicate predicate,
                                  final Operation operation) {
        synchronized (notificationMutex) {
            this.running = true;
        }

        startWaitCursor(frame.getRootPane());
        final CancelAction cancelAction = new CancelAction();
        executor.execute(new Runnable() {
            public void run() {
                try {
                    operation.performOnStart();

                    final Exception[] lastException = new Exception[1];
                    lastException[0] = null;
                    final int maximumRangeLength = rows.length > 99 ? rows.length / (slowOperation ? 100 : 10) : rows.length;

                    new ContinousRange(rows, new RangeOperation() {
                        private int count = 1;

                        public void performOnIndex(final int index) {
                            NavigationPosition position = positionsModel.getPosition(index);
                            if (predicate.shouldOverwrite(position)) {
                                try {
                                    // ignoring the result since the performance boost of the continous
                                    // range operations outweights the possible optimization
                                    operation.run(index, position);
                                } catch (Exception e) {
                                    log.warning(format("Error while running operation %s on position %d: %s", operation, index, e));
                                    lastException[0] = e;
                                }
                            }

                            invokeLater(new Runnable() {
                                public void run() {
                                    int percent = count++ * 100 / rows.length;
                                    getNotificationManager().showNotification(MessageFormat.format(
                                            RouteConverter.getBundle().getString("augment-progressed"), percent), cancelAction);
                                }
                            });
                        }

                        public void performOnRange(final int firstIndex, final int lastIndex) {
                            invokeLater(new Runnable() {
                                public void run() {
                                    positionsModel.fireTableRowsUpdated(firstIndex, lastIndex, operation.getColumnIndex());
                                    if (positionsTable != null) {
                                        scrollToPosition(positionsTable, min(lastIndex + maximumRangeLength, positionsModel.getRowCount()));
                                    }
                                }
                            });
                        }

                        public boolean isInterrupted() {
                            synchronized (notificationMutex) {
                                return cancelAction.isCanceled() || !running;
                            }
                        }
                    }).performMonotonicallyIncreasing(maximumRangeLength);

                    if (lastException[0] != null)
                        showMessageDialog(frame,
                                MessageFormat.format(operation.getErrorMessage(), lastException[0].getLocalizedMessage()),
                                frame.getTitle(), ERROR_MESSAGE);
                } finally {
                    invokeLater(new Runnable() {
                        public void run() {
                            stopWaitCursor(frame.getRootPane());
                            getNotificationManager().showNotification(MessageFormat.format(
                                    RouteConverter.getBundle().getString("augment-completed"), rows.length), null);
                        }
                    });
                }
            }
        });
    }

    private void processCoordinates(final JTable positionsTable,
                                    final PositionsModel positionsModel,
                                    final int[] rows,
                                    final OverwritePredicate predicate) {
        executeOperation(positionsTable, positionsModel, rows, true, predicate,
                new Operation() {
                    private GoogleMapsService googleMapsService = new GoogleMapsService();

                    public String getName() {
                        return "CoordinatesPositionAugmenter";
                    }

                    public int getColumnIndex() {
                        return ALL_COLUMNS; // LONGITUDE_COLUMN_INDEX + LATITUDE_COLUMN_INDEX;
                    }

                    public void performOnStart() {
                    }

                    public boolean run(int index, NavigationPosition position) throws Exception {
                        NavigationPosition coordinates = googleMapsService.getPositionFor(position.getDescription());
                        if (coordinates != null)
                            positionsModel.edit(index,
                                    new PositionColumnValues(asList(LONGITUDE_COLUMN_INDEX, LATITUDE_COLUMN_INDEX),
                                            Arrays.<Object>asList(coordinates.getLongitude(), coordinates.getLatitude())), false, true);
                        return coordinates != null;
                    }

                    public String getErrorMessage() {
                        return RouteConverter.getBundle().getString("add-coordinates-error");
                    }
                }
        );
    }

    public void addCoordinates(int[] rows) {
        processCoordinates(positionsView, positionsModel, rows, TAUTOLOGY_PREDICATE);
    }


    private void processElevations(final JTable positionsTable,
                                   final PositionsModel positionsModel,
                                   final int[] rows,
                                   final OverwritePredicate predicate) {
        executeOperation(positionsTable, positionsModel, rows, true, predicate,
                new Operation() {
                    public String getName() {
                        return "ElevationPositionAugmenter";
                    }

                    public int getColumnIndex() {
                        return ELEVATION_COLUMN_INDEX;
                    }

                    public void performOnStart() {
                        downloadElevationData(rows);
                    }

                    public boolean run(int index, NavigationPosition position) throws Exception {
                        Double previousElevation = position.getElevation();
                        Double nextElevation = elevationServiceFacade.getElevationFor(position.getLongitude(), position.getLatitude());
                        boolean changed = nextElevation == null || !nextElevation.equals(previousElevation);
                        if (changed)
                            positionsModel.edit(index, new PositionColumnValues(ELEVATION_COLUMN_INDEX, nextElevation), false, true);
                        return changed;
                    }

                    public String getErrorMessage() {
                        return RouteConverter.getBundle().getString("add-elevation-error");
                    }
                }
        );
    }

    private void downloadElevationData(int[] rows) {
        if (!elevationServiceFacade.isDownload())
            return;
        List<LongitudeAndLatitude> longitudeAndLatitudes = new ArrayList<>();
        for (int row : rows) {
            NavigationPosition position = positionsModel.getPosition(row);
            if (position.hasCoordinates())
                longitudeAndLatitudes.add(new LongitudeAndLatitude(position.getLongitude(), position.getLatitude()));
        }
        elevationServiceFacade.downloadElevationDataFor(longitudeAndLatitudes);
    }

    public void addElevations(int[] rows) {
        processElevations(positionsView, positionsModel, rows, COORDINATE_PREDICATE);
    }


    private void addPopulatedPlaces(final JTable positionsTable,
                                    final PositionsModel positionsModel,
                                    final int[] rows,
                                    final OverwritePredicate predicate) {
        executeOperation(positionsTable, positionsModel, rows, true, predicate,
                new Operation() {
                    private GeoNamesService geonamesService = new GeoNamesService();

                    public String getName() {
                        return "PopulatedPlacePositionAugmenter";
                    }

                    public int getColumnIndex() {
                        return DESCRIPTION_COLUMN_INDEX;
                    }

                    public void performOnStart() {
                    }

                    public boolean run(int index, NavigationPosition position) throws Exception {
                        String description = geonamesService.getNearByFor(position.getLongitude(), position.getLatitude());
                        if (description != null)
                            positionsModel.edit(index, new PositionColumnValues(DESCRIPTION_COLUMN_INDEX, description), false, true);
                        return description != null;
                    }

                    public String getErrorMessage() {
                        return RouteConverter.getBundle().getString("add-populated-place-error");
                    }
                }
        );
    }

    public void addPopulatedPlaces(int[] rows) {
        addPopulatedPlaces(positionsView, positionsModel, rows, COORDINATE_PREDICATE);
    }


    private void addPostalAddresses(final JTable positionsTable,
                                    final PositionsModel positionsModel,
                                    final int[] rows,
                                    final OverwritePredicate predicate) {
        executeOperation(positionsTable, positionsModel, rows, true, predicate,
                new Operation() {
                    private GoogleMapsService googleMapsService = new GoogleMapsService();

                    public String getName() {
                        return "PostalAddressPositionAugmenter";
                    }

                    public int getColumnIndex() {
                        return DESCRIPTION_COLUMN_INDEX;
                    }

                    public void performOnStart() {
                    }

                    public boolean run(int index, NavigationPosition position) throws Exception {
                        String description = googleMapsService.getLocationFor(position.getLongitude(), position.getLatitude());
                        if (description != null)
                            positionsModel.edit(index, new PositionColumnValues(DESCRIPTION_COLUMN_INDEX, description), false, true);
                        return description != null;
                    }

                    public String getErrorMessage() {
                        return RouteConverter.getBundle().getString("add-postal-address-error");
                    }
                }
        );
    }

    public void addPostalAddresses(int[] rows) {
        addPostalAddresses(positionsView, positionsModel, rows, COORDINATE_PREDICATE);
    }


    private void processSpeeds(final JTable positionsTable,
                               final PositionsModel positionsModel,
                               final int[] rows,
                               final OverwritePredicate predicate) {
        executeOperation(positionsTable, positionsModel, rows, false, predicate,
                new Operation() {
                    public String getName() {
                        return "SpeedPositionAugmenter";
                    }

                    public int getColumnIndex() {
                        return SPEED_COLUMN_INDEX;
                    }

                    public void performOnStart() {
                    }

                    public boolean run(int index, NavigationPosition position) throws Exception {
                        NavigationPosition predecessor = index > 0 && index < positionsModel.getRowCount() ? positionsModel.getPosition(index - 1) : null;
                        if (predecessor != null) {
                            Double previousSpeed = position.getSpeed();
                            Double nextSpeed = position.calculateSpeed(predecessor);
                            boolean changed = nextSpeed == null || !nextSpeed.equals(previousSpeed);
                            if (changed)
                                positionsModel.edit(index, new PositionColumnValues(SPEED_COLUMN_INDEX, nextSpeed), false, true);
                            return changed;
                        }
                        return false;
                    }

                    public String getErrorMessage() {
                        return RouteConverter.getBundle().getString("add-speed-error");
                    }
                }
        );
    }

    public void addSpeeds(int[] rows) {
        processSpeeds(positionsView, positionsModel, rows, COORDINATE_PREDICATE);
    }

    private NavigationPosition findPredecessorWithTime(PositionsModel positionsModel, int index) {
        while (index-- > 0) {
            NavigationPosition position = positionsModel.getPosition(index);
            if (position.hasTime())
                return position;
        }
        return null;
    }

    private NavigationPosition findSuccessorWithTime(PositionsModel positionsModel, int index) {
        while (index++ < positionsModel.getRowCount() - 1) {
            NavigationPosition position = positionsModel.getPosition(index);
            if (position.hasTime())
                return position;
        }
        return null;
    }

    private void processTimes(final JTable positionsTable,
                              final PositionsModel positionsModel,
                              final int[] rows,
                              final OverwritePredicate predicate) {
        executeOperation(positionsTable, positionsModel, rows, false, predicate,
                new Operation() {
                    public String getName() {
                        return "TimePositionAugmenter";
                    }

                    public int getColumnIndex() {
                        return DATE_TIME_COLUMN_INDEX;
                    }

                    public void performOnStart() {
                    }

                    public boolean run(int index, NavigationPosition position) throws Exception {
                        NavigationPosition predecessor = findPredecessorWithTime(positionsModel, index);
                        NavigationPosition successor = findSuccessorWithTime(positionsModel, index);
                        if (predecessor != null && successor != null) {
                            CompactCalendar previousTime = position.getTime();
                            CompactCalendar nextTime = intrapolateTime(position, predecessor, successor);
                            boolean changed = nextTime == null || !nextTime.equals(previousTime);
                            if (changed)
                                positionsModel.edit(index, new PositionColumnValues(DATE_TIME_COLUMN_INDEX, nextTime), false, true);
                            return changed;
                        }
                        return false;
                    }

                    public String getErrorMessage() {
                        return RouteConverter.getBundle().getString("add-time-error");
                    }
                }
        );
    }

    public void addTimes(int[] rows) {
        processTimes(positionsView, positionsModel, rows, COORDINATE_PREDICATE);
    }


    private void processNumbers(final JTable positionsTable,
                                final PositionsModel positionsModel,
                                final int[] rows,
                                final int digitCount,
                                final NumberPattern numberPattern,
                                final OverwritePredicate predicate) {
        executeOperation(positionsTable, positionsModel, rows, false, predicate,
                new Operation() {
                    public String getName() {
                        return "NumberPositionAugmenter";
                    }

                    public int getColumnIndex() {
                        return DESCRIPTION_COLUMN_INDEX;
                    }

                    public void performOnStart() {
                    }

                    public boolean run(int index, NavigationPosition position) throws Exception {
                        String previousDescription = position.getDescription();
                        String nextDescription = getNumberedPosition(position, index, digitCount, numberPattern);
                        boolean changed = nextDescription == null || !nextDescription.equals(previousDescription);
                        if (changed)
                            positionsModel.edit(index, new PositionColumnValues(DESCRIPTION_COLUMN_INDEX, nextDescription), false, true);
                        return changed;
                    }

                    public String getErrorMessage() {
                        return RouteConverter.getBundle().getString("add-number-error");
                    }
                }
        );
    }

    public void addNumbers(int[] rows) {
        int digitCount = widthInDigits(positionsModel.getRowCount() + 1);
        NumberPattern numberPattern = RouteConverter.getInstance().getNumberPatternPreference();
        processNumbers(positionsView, positionsModel, rows, digitCount, numberPattern, COORDINATE_PREDICATE);
    }


    private void addData(final JTable positionsTable,
                         final PositionsModel positionsModel,
                         final int[] rows,
                         final OverwritePredicate predicate,
                         final boolean complementDescription,
                         final boolean complementTime,
                         final boolean complementElevation) {
        executeOperation(positionsTable, positionsModel, rows, true, predicate,
                new Operation() {
                    public String getName() {
                        return "DataPositionAugmenter";
                    }

                    public int getColumnIndex() {
                        return ALL_COLUMNS; // might be DESCRIPTION_COLUMN_INDEX, ELEVATION_COLUMN_INDEX, DATE_TIME_COLUMN_INDEX
                    }

                    public void performOnStart() {
                        downloadElevationData(rows);
                    }

                    public boolean run(int index, NavigationPosition position) throws Exception {
                        List<Integer> columnIndices = new ArrayList<>(3);
                        List<Object> columnValues = new ArrayList<>(3);

                        if (complementDescription) {
                            String nextDescription = getLocationFor(position);
                            if (nextDescription == null)
                                nextDescription = getNearByFor(position);
                            if (nextDescription != null) {
                                nextDescription = createDescription(index + 1, nextDescription);
                            }
                            String previousDescription = position.getDescription();
                            boolean changed = nextDescription == null || !nextDescription.equals(previousDescription);
                            if (changed) {
                                columnIndices.add(DESCRIPTION_COLUMN_INDEX);
                                columnValues.add(nextDescription);
                            }
                        }

                        if (complementElevation) {
                            Double previousElevation = position.getElevation();
                            Double nextElevation = elevationServiceFacade.getElevationFor(position.getLongitude(), position.getLatitude());
                            boolean changed = nextElevation == null || !nextElevation.equals(previousElevation);
                            if (changed) {
                                columnIndices.add(ELEVATION_COLUMN_INDEX);
                                columnValues.add(nextElevation);
                            }
                        }

                        if (complementTime) {
                            NavigationPosition predecessor = findPredecessorWithTime(positionsModel, index);
                            NavigationPosition successor = findSuccessorWithTime(positionsModel, index);
                            if (predecessor != null && successor != null) {
                                CompactCalendar previousTime = position.getTime();
                                CompactCalendar nextTime = intrapolateTime(position, predecessor, successor);
                                boolean changed = nextTime == null || !nextTime.equals(previousTime);
                                if (changed) {
                                    columnIndices.add(DATE_TIME_COLUMN_INDEX);
                                    columnValues.add(nextTime);
                                }
                            }
                        }

                        positionsModel.edit(index, new PositionColumnValues(columnIndices, columnValues), false, true);
                        return complementDescription && columnIndices.contains(DESCRIPTION_COLUMN_INDEX) &&
                                complementElevation && columnIndices.contains(ELEVATION_COLUMN_INDEX) &&
                                complementTime && columnIndices.contains(DATE_TIME_COLUMN_INDEX);
                    }

                    public String getErrorMessage() {
                        return RouteConverter.getBundle().getString("add-data-error");
                    }
                }
        );
    }

    private String getLocationFor(NavigationPosition position) {
        try {
            return googleMapsService.getLocationFor(position.getLongitude(), position.getLatitude());
        } catch (IOException e) {
            return null;
        }
    }

    private String getNearByFor(NavigationPosition position) {
        try {
            return geonamesService.getNearByFor(position.getLongitude(), position.getLatitude());
        } catch (IOException e) {
            return null;
        }
    }

    public void addData(int[] rows, boolean description, boolean time, boolean elevation) {
        addData(positionsView, positionsModel, rows, COORDINATE_PREDICATE, description, time, elevation);
    }


    public String createDescription(int index, String description) {
        if (description == null)
            description = RouteConverter.getBundle().getString("new-position-name");
        NumberPattern numberPattern = RouteConverter.getInstance().getNumberPatternPreference();
        String number = Integer.toString(index);
        return formatNumberedPosition(numberPattern, number, description);
    }
}
