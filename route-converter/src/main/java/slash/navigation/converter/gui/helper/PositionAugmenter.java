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
import slash.navigation.converter.gui.RouteConverter;
import slash.navigation.converter.gui.models.PositionsModel;
import slash.navigation.geonames.GeoNamesService;
import slash.navigation.googlemaps.GoogleMapsPosition;
import slash.navigation.googlemaps.GoogleMapsService;
import slash.navigation.gui.Constants;
import slash.navigation.util.RouteComments;

import javax.swing.*;
import java.io.IOException;
import java.text.MessageFormat;

/**
 * Helps to augment positions with elevation, postal address and populated place information.
 *
 * @author Christian Pesch
 */

public class PositionAugmenter {
    private static final int SLOW_OPERATIONS_IN_A_ROW = 10;
    private static final int FAST_OPERATIONS_IN_A_ROW = 100;

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

    private static final OverwritePredicate NO_COORDINATE_PREDICATE = new OverwritePredicate() {
        public boolean shouldOverwrite(BaseNavigationPosition position) {
            return !position.hasCoordinates();
        }
    };

    private static final OverwritePredicate NO_ELEVATION_PREDICATE = new OverwritePredicate() {
        public boolean shouldOverwrite(BaseNavigationPosition position) {
            return position.hasCoordinates() &&
                    (position.getElevation() == null || position.getElevation() == 0.0);
        }
    };

    private static final OverwritePredicate NO_COMMENT_PREDICATE = new OverwritePredicate() {
        public boolean shouldOverwrite(BaseNavigationPosition position) {
            return position.hasCoordinates() &&
                    (Transfer.trim(position.getComment()) == null ||
                            RouteComments.isPositionComment(position.getComment()));
        }
    };

    private static final OverwritePredicate NO_SPEED_PREDICATE = new OverwritePredicate() {
        public boolean shouldOverwrite(BaseNavigationPosition position) {
            return position.hasCoordinates() &&
                    (position.getSpeed() == null || position.getSpeed() == 0.0);
        }
    };


    private int[] selectAllRows(PositionsModel positionsModel) {
        int[] selectedRows = new int[positionsModel.getRowCount()];
        for (int i = 0; i < selectedRows.length; i++)
            selectedRows[i] = i;
        return selectedRows;
    }


    private interface Operation {
        String getName();
        boolean run(BaseNavigationPosition position) throws Exception;
        String getErrorMessage();
    }

    private void executeOperation(final JTable positionsTable,
                                  final PositionsModel positionsModel,
                                  final int[] rows,
                                  final int operationsInARow,
                                  final OverwritePredicate predicate,
                                  final Operation operation) {
        Constants.startWaitCursor(frame.getRootPane());

        new Thread(new Runnable() {
            public void run() {
                try {
                    final Exception[] lastException = new Exception[1];
                    lastException[0] = null;

                    new ContinousRange(rows, new RangeOperation() {
                        public void performOnIndex(int index) {
                            BaseNavigationPosition position = positionsModel.getPosition(index);
                            if (predicate.shouldOverwrite(position)) {
                                try {
                                    // ignoring the result since the performance boost of the continous
                                    // range operations outweights the possible optimization 
                                    operation.run(position);
                                } catch (Exception e) {
                                    lastException[0] = e;
                                }
                            }
                        }

                        public void performOnRange(final int firstIndex, final int lastIndex) {
                            SwingUtilities.invokeLater(new Runnable() {
                                public void run() {
                                    positionsModel.fireTableRowsUpdated(firstIndex, lastIndex);

                                    if (positionsTable != null)
                                        JTableHelper.scrollToPosition(positionsTable, Math.min(lastIndex + SLOW_OPERATIONS_IN_A_ROW, positionsModel.getRowCount()));
                                }
                            });
                        }
                    }).performMonotonicallyIncreasing(operationsInARow);

                    if (lastException[0] != null)
                        JOptionPane.showMessageDialog(frame,
                                MessageFormat.format(operation.getErrorMessage(), lastException[0].getMessage()),
                                frame.getTitle(), JOptionPane.ERROR_MESSAGE);
                }
                finally {
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            Constants.stopWaitCursor(frame.getRootPane());
                        }
                    });
                }
            }
        }, operation.getName()).start();
    }


    private boolean addCoordinates(GoogleMapsService service, BaseNavigationPosition position) throws IOException {
        GoogleMapsPosition coordinates = service.getPositionFor(position.getComment());
        if (coordinates != null) {
            position.setLongitude(coordinates.getLongitude());
            position.setLatitude(coordinates.getLatitude());
        }
        return coordinates != null;
    }

    private void processCoordinates(final JTable positionsTable,
                                   final PositionsModel positionsModel,
                                   final int[] rows,
                                   final OverwritePredicate predicate) {
        executeOperation(positionsTable, positionsModel, rows, SLOW_OPERATIONS_IN_A_ROW, predicate,
                new Operation() {
                    private GoogleMapsService service = new GoogleMapsService();

                    public String getName() {
                        return "CoordinatesPositionAugmenter";
                    }

                    public boolean run(BaseNavigationPosition position) throws Exception {
                        return addCoordinates(service, position);
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

    public void complementCoordinates(PositionsModel positionsModel) {
        complementCoordinates(null, positionsModel);
    }

    public void complementCoordinates(JTable positionsTable, PositionsModel positionsModel) {
        processCoordinates(positionsTable, positionsModel, selectAllRows(positionsModel), NO_COORDINATE_PREDICATE);
    }


    private boolean addElevation(GeoNamesService service, BaseNavigationPosition position) throws IOException {
        Integer elevation = service.getElevationFor(position.getLongitude(), position.getLatitude());
        if (elevation != null)
            position.setElevation(elevation.doubleValue());
        return elevation != null;
    }

    private void processElevations(final JTable positionsTable,
                                   final PositionsModel positionsModel,
                                   final int[] rows,
                                   final OverwritePredicate predicate) {
        executeOperation(positionsTable, positionsModel, rows, SLOW_OPERATIONS_IN_A_ROW, predicate,
                new Operation() {
                    private GeoNamesService service = new GeoNamesService();

                    public String getName() {
                        return "ElevationPositionAugmenter";
                    }

                    public boolean run(BaseNavigationPosition position) throws Exception {
                        return addElevation(service, position);
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

    public void complementElevations(PositionsModel positionsModel) {
        complementElevations(null, positionsModel);
    }

    public void complementElevations(JTable positionsTable, PositionsModel positionsModel) {
        processElevations(positionsTable, positionsModel, selectAllRows(positionsModel), NO_ELEVATION_PREDICATE);
    }


    private boolean addPopulatedPlace(GeoNamesService service, BaseNavigationPosition position) throws IOException {
        String comment = service.getNearByFor(position.getLongitude(), position.getLatitude());
        if (comment != null)
            position.setComment(comment);
        return comment != null;
    }

    private void addPopulatedPlaces(final JTable positionsTable,
                                    final PositionsModel positionsModel,
                                    final int[] rows,
                                    final OverwritePredicate predicate) {
        executeOperation(positionsTable, positionsModel, rows, SLOW_OPERATIONS_IN_A_ROW, predicate,
                new Operation() {
                    private GeoNamesService service = new GeoNamesService();

                    public String getName() {
                        return "PopulatedPlacePositionAugmenter";
                    }

                    public boolean run(BaseNavigationPosition position) throws Exception {
                        return addPopulatedPlace(service, position);
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

    public void complementPopulatedPlaces(JTable positionsTable, PositionsModel positionsModel) {
        addPopulatedPlaces(positionsTable, positionsModel, selectAllRows(positionsModel), NO_COMMENT_PREDICATE);
    }


    private boolean addPostalAddress(GoogleMapsService service, BaseNavigationPosition position) throws IOException {
        String comment = service.getLocationFor(position.getLongitude(), position.getLatitude());
        if (comment != null)
            position.setComment(comment);
        return comment != null;
    }

    private void addPostalAddresses(final JTable positionsTable,
                                    final PositionsModel positionsModel,
                                    final int[] rows,
                                    final OverwritePredicate predicate) {
        executeOperation(positionsTable, positionsModel, rows, SLOW_OPERATIONS_IN_A_ROW, predicate,
                new Operation() {
                    private GoogleMapsService service = new GoogleMapsService();

                    public String getName() {
                        return "PostalAddressPositionAugmenter";
                    }

                    public boolean run(BaseNavigationPosition position) throws Exception {
                        return addPostalAddress(service, position);
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

    public void complementPostalAddresses(JTable positionsTable, PositionsModel positionsModel) {
        addPostalAddresses(positionsTable, positionsModel, selectAllRows(positionsModel), NO_COMMENT_PREDICATE);
    }


    private void processSpeeds(final JTable positionsTable,
                               final PositionsModel positionsModel,
                               final int[] rows,
                               final OverwritePredicate predicate) {
        executeOperation(positionsTable, positionsModel, rows, FAST_OPERATIONS_IN_A_ROW, predicate,
                new Operation() {
                    public String getName() {
                        return "SpeedPositionAugmenter";
                    }

                    public boolean run(BaseNavigationPosition position) throws Exception {
                        BaseNavigationPosition predecessor = positionsModel.getPredecessor(position);
                        if (predecessor != null) {
                            Double speed = position.calculateSpeed(predecessor);
                            position.setSpeed(speed);
                            return true;
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

    public void complementSpeeds(JTable positionsTable, PositionsModel positionsModel) {
        processSpeeds(positionsTable, positionsModel, selectAllRows(positionsModel), NO_SPEED_PREDICATE);
    }


    private void processIndices(final JTable positionsTable,
                                final PositionsModel positionsModel,
                                final int[] rows,
                                final int digitCount,
                                final boolean spaceBetweenNumberAndComment,
                                final OverwritePredicate predicate) {
        executeOperation(positionsTable, positionsModel, rows, FAST_OPERATIONS_IN_A_ROW, predicate,
                new Operation() {
                    public String getName() {
                        return "IndexPositionAugmenter";
                    }

                    public boolean run(BaseNavigationPosition position) throws Exception {
                        int index = positionsModel.getIndex(position);
                        RouteComments.numberPosition(position, index, digitCount, spaceBetweenNumberAndComment);
                        return true;
                    }

                    public String getErrorMessage() {
                        return RouteConverter.getBundle().getString("add-index-error");
                    }
                }
        );
    }

    public void addIndices(JTable positionsTable, PositionsModel positionsModel, int[] selectedRows,
                           boolean prefixNumberWithZeros,
                           boolean spaceBetweenNumberAndComment) {
        int maximumIndex = 0;
        if(prefixNumberWithZeros) {
            for (int index : selectedRows) {
                if (index > maximumIndex)
                    maximumIndex = index;
            }
        }
        int digitCount = prefixNumberWithZeros ? Transfer.widthInDigits(maximumIndex) : 0;

        processIndices(positionsTable, positionsModel, selectedRows,
                digitCount, spaceBetweenNumberAndComment, COORDINATE_PREDICATE);
    }

}
