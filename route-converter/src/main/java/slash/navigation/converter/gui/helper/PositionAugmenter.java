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

import slash.common.io.ContinousRange;
import slash.common.io.RangeOperation;
import slash.common.io.Transfer;
import slash.navigation.base.BaseNavigationPosition;
import slash.navigation.completer.CompletePositionService;
import slash.navigation.converter.gui.RouteConverter;
import slash.navigation.converter.gui.models.PositionColumns;
import slash.navigation.converter.gui.models.PositionsModel;
import slash.navigation.geonames.GeoNamesService;
import slash.navigation.googlemaps.GoogleMapsPosition;
import slash.navigation.googlemaps.GoogleMapsService;
import slash.navigation.gui.Constants;
import slash.navigation.util.RouteComments;

import javax.swing.*;
import java.text.MessageFormat;

/**
 * Helps to augment positions with elevation, postal address and populated place information.
 *
 * @author Christian Pesch
 */

public class PositionAugmenter {
    private final JFrame frame;

    public PositionAugmenter(JFrame frame) {
        this.frame = frame;
    }


    private interface OverwritePredicate {
        boolean shouldOverwrite(BaseNavigationPosition position);
    }

    private static final OverwritePredicate TAUTOLOGY_PREDICATE = new OverwritePredicate() {
        public boolean shouldOverwrite(BaseNavigationPosition position) {
            return true;
        }
    };

    private static final OverwritePredicate COORDINATE_PREDICATE = new OverwritePredicate() {
        public boolean shouldOverwrite(BaseNavigationPosition position) {
            return position.hasCoordinates();
        }
    };


    private interface Operation {
        String getName();
        int getColumnIndex();
        boolean run(int index, BaseNavigationPosition position) throws Exception;
        String getErrorMessage();
        void postRunning();
    }

    private void executeOperation(final JTable positionsTable,
                                  final PositionsModel positionsModel,
                                  final int[] rows,
                                  final boolean slowOperation,
                                  final OverwritePredicate predicate,
                                  final Operation operation) {
        Constants.startWaitCursor(frame.getRootPane());

        new Thread(new Runnable() {
            public void run() {
                try {
                    final Exception[] lastException = new Exception[1];
                    lastException[0] = null;
                    final int maximumRangeLength = rows.length > 99 ? rows.length / (slowOperation ? 100 : 10) : rows.length;

                    new ContinousRange(rows, new RangeOperation() {
                        public void performOnIndex(int index) {
                            BaseNavigationPosition position = positionsModel.getPosition(index);
                            if (predicate.shouldOverwrite(position)) {
                                try {
                                    // ignoring the result since the performance boost of the continous
                                    // range operations outweights the possible optimization 
                                    operation.run(index, position);
                                } catch (Exception e) {
                                    lastException[0] = e;
                                }
                            }
                        }

                        public void performOnRange(final int firstIndex, final int lastIndex) {
                            SwingUtilities.invokeLater(new Runnable() {
                                public void run() {
                                    positionsModel.fireTableRowsUpdated(firstIndex, lastIndex, operation.getColumnIndex());
                                    if (positionsTable != null)
                                        JTableHelper.scrollToPosition(positionsTable, Math.min(lastIndex + maximumRangeLength, positionsModel.getRowCount()));
                                }
                            });
                        }
                    }).performMonotonicallyIncreasing(maximumRangeLength);

                    if (lastException[0] != null)
                        JOptionPane.showMessageDialog(frame,
                                MessageFormat.format(operation.getErrorMessage(), lastException[0].getMessage()),
                                frame.getTitle(), JOptionPane.ERROR_MESSAGE);
                }
                finally {
                    operation.postRunning();

                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            Constants.stopWaitCursor(frame.getRootPane());
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
                        return PositionColumns.LONGITUDE_COLUMN_INDEX;  // + PositionColumns.LATITUDE_COLUMN_INDEX
                    }

                    public boolean run(int index, BaseNavigationPosition position) throws Exception {
                        GoogleMapsPosition coordinates = googleMapsService.getPositionFor(position.getComment());
                        if (coordinates != null) {
                            positionsModel.edit(coordinates.getLongitude(), index, PositionColumns.LONGITUDE_COLUMN_INDEX, false, true);
                            positionsModel.edit(coordinates.getLatitude(), index, PositionColumns.LATITUDE_COLUMN_INDEX, false, true);
                        }
                        return coordinates != null;
                    }

                    public String getErrorMessage() {
                        return RouteConverter.getBundle().getString("add-coordinates-error");
                    }

                    public void postRunning() {
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
                    private CompletePositionService completePositionService = new CompletePositionService();

                    public String getName() {
                        return "ElevationPositionAugmenter";
                    }

                    public int getColumnIndex() {
                        return PositionColumns.ELEVATION_COLUMN_INDEX;
                    }

                    public boolean run(int index, BaseNavigationPosition position) throws Exception {
                        Integer elevation = completePositionService.getElevationFor(position.getLongitude(), position.getLatitude());
                        if (elevation != null)
                            positionsModel.edit(elevation.doubleValue(), index, PositionColumns.ELEVATION_COLUMN_INDEX, false, true);
                        return elevation != null;
                    }

                    public String getErrorMessage() {
                        return RouteConverter.getBundle().getString("add-elevation-error");
                    }

                    public void postRunning() {
                        completePositionService.close();
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
                        return PositionColumns.DESCRIPTION_COLUMN_INDEX;
                    }

                    public boolean run(int index, BaseNavigationPosition position) throws Exception {
                        String comment = geonamesService.getNearByFor(position.getLongitude(), position.getLatitude());
                        if (comment != null)
                            positionsModel.edit(comment, index, PositionColumns.DESCRIPTION_COLUMN_INDEX, false, true);
                        return comment != null;
                    }

                    public String getErrorMessage() {
                        return RouteConverter.getBundle().getString("add-populated-place-error");
                    }

                    public void postRunning() {
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
                        return PositionColumns.DESCRIPTION_COLUMN_INDEX;
                    }

                    public boolean run(int index, BaseNavigationPosition position) throws Exception {
                        String comment = googleMapsService.getLocationFor(position.getLongitude(), position.getLatitude());
                        if (comment != null)
                            positionsModel.edit(comment, index, PositionColumns.DESCRIPTION_COLUMN_INDEX, false, true);
                        return comment != null;
                    }

                    public String getErrorMessage() {
                        return RouteConverter.getBundle().getString("add-postal-address-error");
                    }

                    public void postRunning() {
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
                        return PositionColumns.SPEED_COLUMN_INDEX;
                    }

                    public boolean run(int index, BaseNavigationPosition position) throws Exception {
                        BaseNavigationPosition predecessor = index > 0 && index < positionsModel.getRowCount() ? positionsModel.getPosition(index - 1) : null;
                        if (predecessor != null) {
                            Double previousSpeed = position.getSpeed();
                            Double nextSpeed = position.calculateSpeed(predecessor);
                            boolean changed = nextSpeed != null && !nextSpeed.equals(previousSpeed);
                            if (changed)
                                positionsModel.edit(nextSpeed, index, PositionColumns.SPEED_COLUMN_INDEX, false, true);
                            return changed;
                        }
                        return false;
                    }

                    public String getErrorMessage() {
                        return RouteConverter.getBundle().getString("add-speed-error");
                    }

                    public void postRunning() {
                    }
                }
        );
    }

    public void addSpeeds(JTable positionsTable, PositionsModel positionsModel, int[] selectedRows) {
        processSpeeds(positionsTable, positionsModel, selectedRows, COORDINATE_PREDICATE);
    }


    private void processIndices(final JTable positionsTable,
                                final PositionsModel positionsModel,
                                final int[] rows,
                                final int digitCount,
                                final boolean spaceBetweenNumberAndComment,
                                final OverwritePredicate predicate) {
        executeOperation(positionsTable, positionsModel, rows, false, predicate,
                new Operation() {
                    public String getName() {
                        return "IndexPositionAugmenter";
                    }

                    public int getColumnIndex() {
                        return PositionColumns.DESCRIPTION_COLUMN_INDEX;
                    }

                    public boolean run(int index, BaseNavigationPosition position) throws Exception {
                        String previousComment = position.getComment();
                        String nextComment = RouteComments.getNumberedPosition(position, index, digitCount, spaceBetweenNumberAndComment);
                        boolean changed = nextComment != null && !nextComment.equals(previousComment);
                        if (changed)
                            positionsModel.edit(nextComment, index, PositionColumns.DESCRIPTION_COLUMN_INDEX, false, true);
                        return changed;
                    }

                    public String getErrorMessage() {
                        return RouteConverter.getBundle().getString("add-index-error");
                    }

                    public void postRunning() {
                    }
                }
        );
    }

    public void addIndices(JTable positionsTable, PositionsModel positionsModel, int[] selectedRows,
                           boolean prefixNumberWithZeros,
                           boolean spaceBetweenNumberAndComment) {
        int maximumIndex = 0;
        if (prefixNumberWithZeros) {
            for (int index : selectedRows) {
                if (index > maximumIndex)
                    maximumIndex = index;
            }
        }
        int digitCount = prefixNumberWithZeros ? Transfer.widthInDigits(maximumIndex + 1) : 0;

        processIndices(positionsTable, positionsModel, selectedRows,
                digitCount, spaceBetweenNumberAndComment, COORDINATE_PREDICATE);
    }
}
