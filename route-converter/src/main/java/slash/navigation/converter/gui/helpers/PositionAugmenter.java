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
import slash.navigation.common.NumberingStrategy;
import slash.navigation.converter.gui.RouteConverter;
import slash.navigation.converter.gui.models.PositionColumnValues;
import slash.navigation.converter.gui.models.PositionsModel;
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

import static java.lang.Math.abs;
import static java.lang.Math.min;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static javax.swing.JOptionPane.ERROR_MESSAGE;
import static javax.swing.JOptionPane.showMessageDialog;
import static javax.swing.SwingUtilities.invokeLater;
import static javax.swing.event.TableModelEvent.ALL_COLUMNS;
import static slash.common.helpers.ExceptionHelper.getLocalizedMessage;
import static slash.common.helpers.ExceptionHelper.printStackTrace;
import static slash.common.helpers.ThreadHelper.createSingleThreadExecutor;
import static slash.common.io.Transfer.widthInDigits;
import static slash.common.type.CompactCalendar.fromMillis;
import static slash.navigation.base.RouteComments.formatNumberedPosition;
import static slash.navigation.base.RouteComments.getNumberedPosition;
import static slash.navigation.common.NumberingStrategy.Absolute_Position_Within_Position_List;
import static slash.navigation.converter.gui.helpers.PositionHelper.formatElevation;
import static slash.navigation.converter.gui.helpers.PositionHelper.formatSpeed;
import static slash.navigation.converter.gui.models.PositionColumns.DATE_TIME_COLUMN_INDEX;
import static slash.navigation.converter.gui.models.PositionColumns.DESCRIPTION_COLUMN_INDEX;
import static slash.navigation.converter.gui.models.PositionColumns.ELEVATION_COLUMN_INDEX;
import static slash.navigation.converter.gui.models.PositionColumns.LATITUDE_COLUMN_INDEX;
import static slash.navigation.converter.gui.models.PositionColumns.LONGITUDE_COLUMN_INDEX;
import static slash.navigation.converter.gui.models.PositionColumns.SPEED_COLUMN_INDEX;
import static slash.navigation.gui.helpers.JTableHelper.scrollToPosition;

/**
 * Helps to augment a positions with coordinates, elevation, position number for its description,
 * postal address, populated place and speed information.
 *
 * @author Christian Pesch
 */

public class PositionAugmenter {
    private static final Logger log = Logger.getLogger(PositionAugmenter.class.getName());

    private final JFrame frame;
    private final JTable positionsView;
    private final PositionsModel positionsModel;

    private final ExecutorService executor = createSingleThreadExecutor("AugmentPositions");
    private final ElevationServiceFacade elevationServiceFacade;
    private final GeocodingServiceFacade geocodingServiceFacade;
    private static final Object notificationMutex = new Object();
    private boolean running = true;

    public PositionAugmenter(JTable positionsView, PositionsModel positionsModel, JFrame frame,
                             ElevationServiceFacade elevationServiceFacade,
                             GeocodingServiceFacade geocodingServiceFacade) {
        this.positionsView = positionsView;
        this.positionsModel = positionsModel;
        this.frame = frame;
        this.elevationServiceFacade = elevationServiceFacade;
        this.geocodingServiceFacade = geocodingServiceFacade;
    }

    public void interrupt() {
        synchronized (notificationMutex) {
            this.running = false;
        }
    }

    public void dispose() {
        interrupt();
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
        String getMessagePrefix();
    }

    private NotificationManager getNotificationManager() {
        return Application.getInstance().getContext().getNotificationManager();
    }

    private static class CancelAction extends AbstractAction {
        private boolean canceled;

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

        final CancelAction cancelAction = new CancelAction();
        executor.execute(new Runnable() {
            public void run() {
                final int[] count = new int[1];
                count[0] = 0;

                try {
                    invokeLater(new Runnable() {
                        public void run() {
                            if (positionsTable != null && rows.length > 0)
                                scrollToPosition(positionsTable, rows[0]);
                        }
                    });
                    operation.performOnStart();

                    final Exception[] lastException = new Exception[1];
                    lastException[0] = null;
                    final int maximumRangeLength = rows.length > 99 ? rows.length / (slowOperation ? 100 : 10) : rows.length;

                    new ContinousRange(rows, new RangeOperation() {
                        public void performOnIndex(final int index) {
                            NavigationPosition position = positionsModel.getPosition(index);
                            if (predicate.shouldOverwrite(position)) {
                                try {
                                    // ignoring the result since the performance boost of the continous
                                    // range operations outweights the possible optimization
                                    operation.run(index, position);
                                } catch (Exception e) {
                                    log.warning(format("Error while running operation %s on position %d: %s, %s", operation, index, e, printStackTrace(e)));
                                    lastException[0] = e;
                                }
                            }
                            getNotificationManager().showNotification(MessageFormat.format(
                                    RouteConverter.getBundle().getString("augmenting-progress"), count[0]++, rows.length), cancelAction);
                        }

                        public void performOnRange(final int firstIndex, final int lastIndex) {
                            invokeLater(new Runnable() {
                                public void run() {
                                    positionsModel.fireTableRowsUpdated(firstIndex, lastIndex, operation.getColumnIndex());
                                    if (positionsTable != null) {
                                        scrollToPosition(positionsTable, min(lastIndex + maximumRangeLength, positionsModel.getRowCount() - 1));
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

                    if (lastException[0] != null) {
                        String errorMessage = RouteConverter.getBundle().getString(operation.getMessagePrefix() + "error");
                        showMessageDialog(frame,
                                MessageFormat.format(errorMessage, getLocalizedMessage(lastException[0])), frame.getTitle(), ERROR_MESSAGE);
                    }
                } finally {
                    invokeLater(new Runnable() {
                        public void run() {
                            getNotificationManager().showNotification(MessageFormat.format(
                                    RouteConverter.getBundle().getString("augmenting-finished"), count[0]), null);
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
                    public String getName() {
                        return "CoordinatesPositionAugmenter";
                    }

                    public int getColumnIndex() {
                        return ALL_COLUMNS; // LONGITUDE_COLUMN_INDEX + LATITUDE_COLUMN_INDEX;
                    }

                    public void performOnStart() {
                    }

                    public boolean run(int index, NavigationPosition position) throws Exception {
                        NavigationPosition coordinates = RouteConverter.getInstance().getGeocodingServiceFacade().getPositionFor(position.getDescription());
                        if (coordinates != null)
                            positionsModel.edit(index,
                                    new PositionColumnValues(asList(LONGITUDE_COLUMN_INDEX, LATITUDE_COLUMN_INDEX),
                                            Arrays.<Object>asList(coordinates.getLongitude(), coordinates.getLatitude())), false, true);
                        return coordinates != null;
                    }

                    public String getMessagePrefix() {
                        return "add-coordinates-";
                    }
                }
        );
    }

    public void addCoordinates() {
        int[] rows = positionsView.getSelectedRows();
        if (rows.length > 0)
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
                        downloadElevationData(rows, true);
                    }

                    public boolean run(int index, NavigationPosition position) throws Exception {
                        String previousElevation = formatElevation(position.getElevation());
                        String nextElevation = getElevationFor(position);
                        boolean changed = nextElevation != null && !nextElevation.equals(previousElevation);
                        if (changed)
                            positionsModel.edit(index, new PositionColumnValues(ELEVATION_COLUMN_INDEX, nextElevation), false, true);
                        return changed;
                    }

                    public String getMessagePrefix() {
                        return "add-elevation-";
                    }
                }
        );
    }

    private String getElevationFor(NavigationPosition position) throws IOException {
        if(!position.hasCoordinates())
            return null;

        Double elevation = elevationServiceFacade.getElevationFor(position.getLongitude(), position.getLatitude());
        if(elevation == null)
            return null;

        return formatElevation(elevation);
    }

    private void downloadElevationData(int[] rows, boolean waitForDownload) {
        if (!elevationServiceFacade.isDownload())
            return;
        List<LongitudeAndLatitude> longitudeAndLatitudes = new ArrayList<>();
        for (int row : rows) {
            NavigationPosition position = positionsModel.getPosition(row);
            if (position.hasCoordinates())
                longitudeAndLatitudes.add(new LongitudeAndLatitude(position.getLongitude(), position.getLatitude()));
        }
        elevationServiceFacade.downloadElevationDataFor(longitudeAndLatitudes, waitForDownload);
    }

    public void addElevations() {
        int[] rows = positionsView.getSelectedRows();
        if (rows.length > 0)
            processElevations(positionsView, positionsModel, rows, COORDINATE_PREDICATE);
    }

    private void addAddresses(final JTable positionsTable,
                              final PositionsModel positionsModel,
                              final int[] rows,
                              final OverwritePredicate predicate) {
        executeOperation(positionsTable, positionsModel, rows, true, predicate,
                new Operation() {
                    public String getName() {
                        return "AddressPositionAugmenter";
                    }

                    public int getColumnIndex() {
                        return DESCRIPTION_COLUMN_INDEX;
                    }

                    public void performOnStart() {
                    }

                    public boolean run(int index, NavigationPosition position) throws Exception {
                        String description = geocodingServiceFacade.getAddressFor(position);
                        if (description != null)
                            positionsModel.edit(index, new PositionColumnValues(DESCRIPTION_COLUMN_INDEX, description), false, true);
                        return description != null;
                    }

                    public String getMessagePrefix() {
                        return "add-address-";
                    }
                }
        );
    }

    public void addAddresses() {
        int[] rows = positionsView.getSelectedRows();
        if (rows.length > 0)
            addAddresses(positionsView, positionsModel, rows, COORDINATE_PREDICATE);
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

                    public boolean run(int index, NavigationPosition position) {
                        NavigationPosition predecessor = index > 0 && index < positionsModel.getRowCount() ? positionsModel.getPosition(index - 1) : null;
                        if (predecessor != null) {
                            String previousSpeed = formatSpeed(position.getSpeed());
                            String nextSpeed = formatSpeed(position.calculateSpeed(predecessor));
                            boolean changed = nextSpeed != null && !nextSpeed.equals(previousSpeed);
                            if (changed)
                                positionsModel.edit(index, new PositionColumnValues(SPEED_COLUMN_INDEX, nextSpeed), false, true);
                            return changed;
                        }
                        return false;
                    }

                    public String getMessagePrefix() {
                        return "add-speed-";
                    }
                }
        );
    }

    public void addSpeeds() {
        int[] rows = positionsView.getSelectedRows();
        if (rows.length > 0)
            processSpeeds(positionsView, positionsModel, rows, COORDINATE_PREDICATE);
    }

    int findPredecessorWithTime(PositionsModel positionsModel, int index) {
        while (index >= 0) {
            NavigationPosition position = positionsModel.getPosition(index);
            if (position.hasTime())
                return index;
            index--;
        }
        return -1;
    }

    int findSuccessorWithTime(PositionsModel positionsModel, int index) {
        while (index < positionsModel.getRowCount()) {
            NavigationPosition position = positionsModel.getPosition(index);
            if (position.hasTime())
                return index;
            index++;
        }
        return -1;
    }

    private CompactCalendar interpolateTime(PositionsModel positionsModel, int positionIndex,
                                            int predecessorIndex, int successorIndex) {
        NavigationPosition predecessor = positionsModel.getPosition(predecessorIndex);
        if (!predecessor.hasTime())
            return null;
        NavigationPosition successor = positionsModel.getPosition(successorIndex);
        if (!successor.hasTime())
            return null;

        long timeDelta = abs(predecessor.calculateTime(successor));
        if (timeDelta == 0)
            return null;

        double distanceToPredecessor = positionsModel.getRoute().getDistance(predecessorIndex, positionIndex);
        double distanceToSuccessor = positionsModel.getRoute().getDistance(positionIndex, successorIndex);
        if (distanceToPredecessor == 0.0)
            return null;
        double distanceRatio = distanceToPredecessor / (distanceToPredecessor + distanceToSuccessor);

        long time = (long) (predecessor.getTime().getTimeInMillis() + (double) timeDelta * distanceRatio);
        return fromMillis(time);
    }

    private void processTimes(final JTable positionsTable,
                              final PositionsModel positionsModel,
                              final int[] rows,
                              final OverwritePredicate predicate) {
        executeOperation(positionsTable, positionsModel, rows, false, predicate,
                new Operation() {
                    private int predecessorIndex, successorIndex;

                    public String getName() {
                        return "TimePositionAugmenter";
                    }

                    public int getColumnIndex() {
                        return DATE_TIME_COLUMN_INDEX;
                    }

                    public void performOnStart() {
                        predecessorIndex = findPredecessorWithTime(positionsModel, rows[0]);
                        successorIndex = findSuccessorWithTime(positionsModel, rows[rows.length-1]);
                    }

                    public boolean run(int index, NavigationPosition position) {
                        if (predecessorIndex != -1 && successorIndex != -1) {
                            CompactCalendar previousTime = position.getTime();
                            CompactCalendar nextTime = interpolateTime(positionsModel, index, predecessorIndex, successorIndex);
                            boolean changed = nextTime != null && !nextTime.equals(previousTime);
                            if (changed)
                                positionsModel.edit(index, new PositionColumnValues(DATE_TIME_COLUMN_INDEX, nextTime), false, true);
                            return changed;
                        }
                        return false;
                    }

                    public String getMessagePrefix() {
                        return "add-time-";
                    }
                }
        );
    }

    public void addTimes() {
        int[] rows = positionsView.getSelectedRows();
        if (rows.length > 0)
            processTimes(positionsView, positionsModel, rows, COORDINATE_PREDICATE);
    }


    private int findRelativeIndex(int[] selectedIndices, int indexToSearch) {
        for (int i = 0; i < selectedIndices.length; i++)
            if (selectedIndices[i] == indexToSearch)
                return i;
        return indexToSearch;
    }

    private void processNumbers(final JTable positionsTable,
                                final PositionsModel positionsModel,
                                final int[] rows,
                                final int digitCount,
                                final NumberPattern numberPattern,
                                final NumberingStrategy numberingStrategy,
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

                    public boolean run(int index, NavigationPosition position) {
                        String previousDescription = position.getDescription();
                        int number = numberingStrategy.equals(Absolute_Position_Within_Position_List) ? index : findRelativeIndex(rows, index);
                        String nextDescription = getNumberedPosition(position, number, digitCount, numberPattern);
                        boolean changed = nextDescription != null && !nextDescription.equals(previousDescription);
                        if (changed)
                            positionsModel.edit(index, new PositionColumnValues(DESCRIPTION_COLUMN_INDEX, nextDescription), false, true);
                        return changed;
                    }

                    public String getMessagePrefix() {
                        return "add-number-";
                    }
                }
        );
    }

    public void addNumbers() {
        int[] rows = positionsView.getSelectedRows();
        if (rows.length == 0)
            return;

        int digitCount = widthInDigits(positionsModel.getRowCount() + 1);
        NumberPattern numberPattern = RouteConverter.getInstance().getNumberPatternPreference();
        NumberingStrategy numberingStrategy = RouteConverter.getInstance().getNumberingStrategyPreference();
        processNumbers(positionsView, positionsModel, rows, digitCount, numberPattern, numberingStrategy, COORDINATE_PREDICATE);
    }


    private void addData(final JTable positionsTable,
                         final PositionsModel positionsModel,
                         final int[] rows,
                         final OverwritePredicate predicate,
                         final boolean complementDescription,
                         final boolean complementTime,
                         final boolean complementElevation,
                         final boolean waitForDownload,
                         final boolean trackUndo) {
        executeOperation(positionsTable, positionsModel, rows, true, predicate,
                new Operation() {
                    private int predecessorIndex, successorIndex;

                    public String getName() {
                        return "DataPositionAugmenter";
                    }

                    public int getColumnIndex() {
                        if(complementDescription && !complementElevation && !complementTime)
                            return DESCRIPTION_COLUMN_INDEX;
                        if(!complementDescription && complementElevation && !complementTime)
                            return ELEVATION_COLUMN_INDEX;
                        if(!complementDescription && !complementElevation && !complementTime)
                            return DATE_TIME_COLUMN_INDEX;
                        return ALL_COLUMNS; // might be DESCRIPTION_COLUMN_INDEX, ELEVATION_COLUMN_INDEX, DATE_TIME_COLUMN_INDEX
                    }

                    public void performOnStart() {
                        predecessorIndex = findPredecessorWithTime(positionsModel, rows[0]);
                        successorIndex = findSuccessorWithTime(positionsModel, rows[rows.length-1]);
                        downloadElevationData(rows, waitForDownload);
                    }

                    public boolean run(int index, NavigationPosition position) throws Exception {
                        List<Integer> columnIndices = new ArrayList<>(3);
                        List<Object> columnValues = new ArrayList<>(3);

                        if (complementDescription) {
                            String nextDescription = waitForDownload ? geocodingServiceFacade.getAddressFor(position) : null;
                            if (nextDescription != null)
                                nextDescription = createDescription(index + 1, nextDescription);
                            String previousDescription = position.getDescription();
                            boolean changed = nextDescription != null && !nextDescription.equals(previousDescription);
                            if (changed) {
                                columnIndices.add(DESCRIPTION_COLUMN_INDEX);
                                columnValues.add(nextDescription);
                            }
                        }

                        if (complementElevation) {
                            String previousElevation = formatElevation(position.getElevation());
                            String nextElevation = waitForDownload || elevationServiceFacade.isDownload() ?
                                    getElevationFor(position) : null;
                            boolean changed = nextElevation != null && !nextElevation.equals(previousElevation);
                            if (changed) {
                                columnIndices.add(ELEVATION_COLUMN_INDEX);
                                columnValues.add(nextElevation);
                            }
                        }

                        if (complementTime) {
                            if (predecessorIndex != -1 && successorIndex != -1) {
                                CompactCalendar previousTime = position.getTime();
                                CompactCalendar nextTime = interpolateTime(positionsModel, index, predecessorIndex, successorIndex);
                                boolean changed = nextTime != null && !nextTime.equals(previousTime);
                                if (changed) {
                                    columnIndices.add(DATE_TIME_COLUMN_INDEX);
                                    columnValues.add(nextTime);
                                }
                            }
                        }

                        positionsModel.edit(index, new PositionColumnValues(columnIndices, columnValues), false, trackUndo);
                        return complementDescription && columnIndices.contains(DESCRIPTION_COLUMN_INDEX) &&
                                complementElevation && columnIndices.contains(ELEVATION_COLUMN_INDEX) &&
                                complementTime && columnIndices.contains(DATE_TIME_COLUMN_INDEX);
                    }

                    public String getMessagePrefix() {
                        String messageKey = "add-data-";
                        if (complementDescription)
                            messageKey = "add-description-";
                        else if (complementElevation)
                            messageKey = "add-elevation-";
                        else if (complementTime)
                            messageKey = "add-time-";
                        return messageKey;
                    }
                }
        );
    }

    public void addData(int[] rows, boolean description, boolean time, boolean elevation, boolean waitForDownload, boolean trackUndo) {
        addData(positionsView, positionsModel, rows, COORDINATE_PREDICATE, description, time, elevation, waitForDownload, trackUndo);
    }


    public String createDescription(int index, String description) {
        if (description == null)
            description = RouteConverter.getBundle().getString("new-position-name");
        NumberPattern numberPattern = RouteConverter.getInstance().getNumberPatternPreference();
        String number = Integer.toString(index);
        return formatNumberedPosition(numberPattern, number, description);
    }
}
