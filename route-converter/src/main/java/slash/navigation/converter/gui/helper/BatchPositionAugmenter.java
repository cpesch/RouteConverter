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
import slash.navigation.base.NavigationPosition;
import slash.navigation.common.BasicPosition;
import slash.navigation.completer.CompletePositionService;
import slash.navigation.converter.gui.RouteConverter;
import slash.navigation.converter.gui.models.PositionsModel;
import slash.navigation.geonames.GeoNamesService;
import slash.navigation.googlemaps.GoogleMapsService;
import slash.navigation.gui.events.ContinousRange;
import slash.navigation.gui.events.RangeOperation;
import slash.navigation.common.NumberPattern;

import javax.swing.*;
import java.text.MessageFormat;
import java.util.logging.Logger;

import static java.lang.String.format;
import static javax.swing.JOptionPane.ERROR_MESSAGE;
import static javax.swing.SwingUtilities.invokeLater;
import static javax.swing.event.TableModelEvent.ALL_COLUMNS;
import static slash.common.io.Transfer.widthInDigits;
import static slash.navigation.converter.gui.helper.JTableHelper.scrollToPosition;
import static slash.navigation.converter.gui.models.PositionColumns.DESCRIPTION_COLUMN_INDEX;
import static slash.navigation.converter.gui.models.PositionColumns.ELEVATION_COLUMN_INDEX;
import static slash.navigation.converter.gui.models.PositionColumns.LATITUDE_COLUMN_INDEX;
import static slash.navigation.converter.gui.models.PositionColumns.LONGITUDE_COLUMN_INDEX;
import static slash.navigation.converter.gui.models.PositionColumns.SPEED_COLUMN_INDEX;
import static slash.navigation.converter.gui.models.PositionColumns.TIME_COLUMN_INDEX;
import static slash.navigation.gui.helpers.UIHelper.startWaitCursor;
import static slash.navigation.gui.helpers.UIHelper.stopWaitCursor;
import static slash.navigation.util.Positions.intrapolateTime;
import static slash.navigation.util.RouteComments.getNumberedPosition;

/**
 * Helps to augment a batch of positions with geocoded coordinates, elevation,
 * position number for its comment, postal address, populated place and speed
 * information.
 *
 * @author Christian Pesch
 */

public class BatchPositionAugmenter {
    private static final Logger log = Logger.getLogger(BatchPositionAugmenter.class.getName());
    private static final Object mutex = new Object();
    private JFrame frame;
    private CompletePositionService completePositionService;
    private boolean running = true;

    public BatchPositionAugmenter(JFrame frame, CompletePositionService completePositionService) {
        this.frame = frame;
        this.completePositionService = completePositionService;
    }

    public void interrupt() {
        synchronized (mutex) {
            this.running = false;
        }
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
        boolean run(int index, NavigationPosition position) throws Exception;
        String getErrorMessage();
    }

    private void executeOperation(final JTable positionsTable,
                                  final PositionsModel positionsModel,
                                  final int[] rows,
                                  final boolean slowOperation,
                                  final OverwritePredicate predicate,
                                  final Operation operation) {
        synchronized (mutex) {
            this.running = true;
        }

        startWaitCursor(frame.getRootPane());
        final ProgressMonitor progress = new ProgressMonitor(frame, "", RouteConverter.getBundle().getString("progress-started"), 0, 100);
        new Thread(new Runnable() {
            public void run() {
                try {
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
                                    e.printStackTrace();
                                    log.warning(format("Error while running operation %s on position %d: %s", operation, index, e));
                                    lastException[0] = e;
                                }
                            }

                            invokeLater(new Runnable() {
                                public void run() {
                                    int percent = count++ * 100 / rows.length;
                                    progress.setNote(MessageFormat.format(
                                            RouteConverter.getBundle().getString("progress-processing-position"),
                                            index, percent));
                                    progress.setProgress(percent);
                                }
                            });
                        }

                        public void performOnRange(final int firstIndex, final int lastIndex) {
                            invokeLater(new Runnable() {
                                public void run() {
                                    positionsModel.fireTableRowsUpdated(firstIndex, lastIndex, operation.getColumnIndex());
                                    if (positionsTable != null) {
                                        scrollToPosition(positionsTable, Math.min(lastIndex + maximumRangeLength, positionsModel.getRowCount()));
                                    }
                                }
                            });
                        }

                        public boolean isInterrupted() {
                            synchronized (mutex) {
                                return progress.isCanceled() || !running;
                            }
                        }
                    }).performMonotonicallyIncreasing(maximumRangeLength);

                    if (lastException[0] != null)
                        JOptionPane.showMessageDialog(frame,
                                MessageFormat.format(operation.getErrorMessage(), lastException[0].getLocalizedMessage()),
                                frame.getTitle(), ERROR_MESSAGE);
                } finally {
                    invokeLater(new Runnable() {
                        public void run() {
                            stopWaitCursor(frame.getRootPane());
                            progress.setNote(RouteConverter.getBundle().getString("progress-finished"));
                            progress.setProgress(progress.getMaximum());
                        }
                    });
                }
            }
        }, operation.getName()).start();
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

                    public boolean run(int index, NavigationPosition position) throws Exception {
                        BasicPosition coordinates = googleMapsService.getPositionFor(position.getComment());
                        if (coordinates != null) {
                            positionsModel.edit(index, LONGITUDE_COLUMN_INDEX, coordinates.getLongitude(),
                                    LATITUDE_COLUMN_INDEX, coordinates.getLatitude(), false, true);
                        }
                        return coordinates != null;
                    }

                    public String getErrorMessage() {
                        return RouteConverter.getBundle().getString("add-coordinates-error");
                    }
                }
        );
    }

    public void addCoordinates(JTable positionsTable, PositionsModel positionsModel, int[] selectedRows) {
        processCoordinates(positionsTable, positionsModel, selectedRows, TAUTOLOGY_PREDICATE);
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

                    public boolean run(int index, NavigationPosition position) throws Exception {
                        Double previousElevation = position.getElevation();
                        Double nextElevation = completePositionService.getElevationFor(position.getLongitude(), position.getLatitude());
                        boolean changed = nextElevation != null && !nextElevation.equals(previousElevation);
                        if (changed)
                            positionsModel.edit(index, ELEVATION_COLUMN_INDEX, nextElevation, -1, null, false, true);
                        return changed;
                    }

                    public String getErrorMessage() {
                        return RouteConverter.getBundle().getString("add-elevation-error");
                    }
                }
        );
    }

    public void addElevations(JTable positionsTable, PositionsModel positionsModel, int[] selectedRows) {
        processElevations(positionsTable, positionsModel, selectedRows, COORDINATE_PREDICATE);
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

                    public boolean run(int index, NavigationPosition position) throws Exception {
                        String comment = geonamesService.getNearByFor(position.getLongitude(), position.getLatitude());
                        if (comment != null)
                            positionsModel.edit(index, DESCRIPTION_COLUMN_INDEX, comment, -1, null, false, true);
                        return comment != null;
                    }

                    public String getErrorMessage() {
                        return RouteConverter.getBundle().getString("add-populated-place-error");
                    }
                }
        );
    }

    public void addPopulatedPlaces(JTable positionsTable, PositionsModel positionsModel, int[] selectedRows) {
        addPopulatedPlaces(positionsTable, positionsModel, selectedRows, COORDINATE_PREDICATE);
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

                    public boolean run(int index, NavigationPosition position) throws Exception {
                        String comment = googleMapsService.getLocationFor(position.getLongitude(), position.getLatitude());
                        if (comment != null)
                            positionsModel.edit(index, DESCRIPTION_COLUMN_INDEX, comment, -1, null, false, true);
                        return comment != null;
                    }

                    public String getErrorMessage() {
                        return RouteConverter.getBundle().getString("add-postal-address-error");
                    }
                }
        );
    }

    public void addPostalAddresses(JTable positionsTable, PositionsModel positionsModel, int[] selectedRows) {
        addPostalAddresses(positionsTable, positionsModel, selectedRows, COORDINATE_PREDICATE);
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

                    public boolean run(int index, NavigationPosition position) throws Exception {
                        NavigationPosition predecessor = index > 0 && index < positionsModel.getRowCount() ? positionsModel.getPosition(index - 1) : null;
                        if (predecessor != null) {
                            Double previousSpeed = position.getSpeed();
                            Double nextSpeed = position.calculateSpeed(predecessor);
                            boolean changed = nextSpeed != null && !nextSpeed.equals(previousSpeed);
                            if (changed)
                                positionsModel.edit(index, SPEED_COLUMN_INDEX, nextSpeed, -1, null, false, true);
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

    public void addSpeeds(JTable positionsTable, PositionsModel positionsModel, int[] selectedRows) {
        processSpeeds(positionsTable, positionsModel, selectedRows, COORDINATE_PREDICATE);
    }

    private NavigationPosition findPredecessorWithTime(PositionsModel positionsModel, int index) {
        while(index-- > 0) {
            NavigationPosition position = positionsModel.getPosition(index);
            if(position.getTime() != null)
                return position;
        }
        return null;
    }

    private NavigationPosition findSuccessorWithTime(PositionsModel positionsModel, int index) {
        while(index++ < positionsModel.getRowCount() - 1) {
            NavigationPosition position = positionsModel.getPosition(index);
            if(position.getTime() != null)
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
                        return TIME_COLUMN_INDEX;
                    }

                    public boolean run(int index, NavigationPosition position) throws Exception {
                        NavigationPosition predecessor = findPredecessorWithTime(positionsModel, index);
                        NavigationPosition successor = findSuccessorWithTime(positionsModel, index);
                        if (predecessor != null && successor != null) {
                            CompactCalendar previousTime = position.getTime();
                            CompactCalendar nextTime = intrapolateTime(position, predecessor, successor);
                            boolean changed = nextTime != null && !nextTime.equals(previousTime);
                            if (changed)
                                positionsModel.edit(index, TIME_COLUMN_INDEX, nextTime, -1, null, false, true);
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

    public void addTimes(JTable positionsTable, PositionsModel positionsModel, int[] selectedRows) {
        processTimes(positionsTable, positionsModel, selectedRows, COORDINATE_PREDICATE);
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

                    public boolean run(int index, NavigationPosition position) throws Exception {
                        String previousComment = position.getComment();
                        String nextComment = getNumberedPosition(position, index, digitCount, numberPattern);
                        boolean changed = nextComment != null && !nextComment.equals(previousComment);
                        if (changed)
                            positionsModel.edit(index, DESCRIPTION_COLUMN_INDEX, nextComment, -1, null, false, true);
                        return changed;
                    }

                    public String getErrorMessage() {
                        return RouteConverter.getBundle().getString("add-number-error");
                    }
                }
        );
    }

    public void addNumbers(JTable positionsTable, PositionsModel positionsModel, int[] selectedRows) {
        int digitCount = widthInDigits(positionsModel.getRowCount() + 1);
        NumberPattern numberPattern = RouteConverter.getInstance().getNumberPatternPreference();
        processNumbers(positionsTable, positionsModel, selectedRows, digitCount, numberPattern, COORDINATE_PREDICATE);
    }
}
