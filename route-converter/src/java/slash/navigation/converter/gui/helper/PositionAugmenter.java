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

import slash.navigation.BaseNavigationPosition;
import slash.navigation.util.RouteComments;
import slash.navigation.converter.gui.models.PositionsModel;
import slash.navigation.converter.gui.RouteConverter;
import slash.navigation.geonames.GeoNamesService;
import slash.navigation.gui.Constants;
import slash.navigation.util.Conversion;

import javax.swing.*;
import java.io.IOException;
import java.text.MessageFormat;

/**
 * Helps to augment positions with elevation and comment information.
 *
 * @author Christian Pesch
 */

public class PositionAugmenter {
    private JFrame frame;

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

    private static final OverwritePredicate NO_ELEVATION_PREDICATE = new OverwritePredicate() {
        public boolean shouldOverwrite(BaseNavigationPosition position) {
            return position.getElevation() == null || position.getElevation() == 0.0;
        }
    };

    private static final OverwritePredicate NO_COMMENT_PREDICATE = new OverwritePredicate() {
        public boolean shouldOverwrite(BaseNavigationPosition position) {
            return Conversion.trim(position.getComment()) == null ||
                    RouteComments.isPositionComment(position.getComment());
        }
    };

    private static final OverwritePredicate NO_SPEED_PREDICATE = new OverwritePredicate() {
        public boolean shouldOverwrite(BaseNavigationPosition position) {
            return position.getSpeed() == null || position.getSpeed() == 0.0;
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

    private void executeOperation(final PositionsModel positionsModel,
                                  final int[] rows,
                                  final OverwritePredicate predicate,
                                  final Operation operation) {
        Constants.startWaitCursor(frame.getRootPane());

        new Thread(new Runnable() {
            public void run() {
                try {
                    Exception exception = null;
                    for (final int row : rows) {
                        BaseNavigationPosition position = positionsModel.getPosition(row);
                        if (position.hasCoordinates() && predicate.shouldOverwrite(position)) {
                            try {
                                if (operation.run(position)) {
                                    SwingUtilities.invokeLater(new Runnable() {
                                        public void run() {
                                            positionsModel.fireTableRowsUpdated(row, row);
                                        }
                                    });
                                }
                            } catch (Exception e) {
                                exception = e;
                            }
                        }
                    }
                    if (exception != null)
                        JOptionPane.showMessageDialog(frame,
                                MessageFormat.format(operation.getErrorMessage(), exception.getMessage()),
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


    private boolean addElevation(GeoNamesService service, final BaseNavigationPosition position) throws IOException {
        final Integer elevation = service.getElevationFor(position.getLongitude(), position.getLatitude());
        if (elevation != null)
            position.setElevation(elevation.doubleValue());
        return elevation != null;
    }

    private void processElevations(final PositionsModel positionsModel,
                                   final int[] rows,
                                   final OverwritePredicate predicate) {
        executeOperation(positionsModel, rows, predicate,
                new Operation() {
                    private GeoNamesService service = new GeoNamesService();

                    public String getName() {
                        return "ElevationPositionAugmenter";
                    }

                    public boolean run(BaseNavigationPosition position) throws Exception {
                        return addElevation(service, position);
                    }

                    public String getErrorMessage() {
                        return RouteConverter.BUNDLE.getString("add-elevation-error");
                    }
                }
        );
    }

    public void addElevations(PositionsModel positionsModel, int[] selectedRows) {
        processElevations(positionsModel, selectedRows, TAUTOLOGY_PREDICATE);
    }

    public void complementElevations(PositionsModel positionsModel) {
        processElevations(positionsModel, selectAllRows(positionsModel), NO_ELEVATION_PREDICATE);
    }


    private boolean addComment(GeoNamesService service, BaseNavigationPosition position) throws IOException {
        String comment = service.getNearByFor(position.getLongitude(), position.getLatitude());
        if (comment != null)
            position.setComment(comment);
        return comment != null;
    }

    private void addComments(final PositionsModel positionsModel,
                             final int[] rows,
                             final OverwritePredicate predicate) {
        executeOperation(positionsModel, rows, predicate,
                new Operation() {
                    private GeoNamesService service = new GeoNamesService();

                    public String getName() {
                        return "CommentPositionAugmenter";
                    }

                    public boolean run(BaseNavigationPosition position) throws Exception {
                        return addComment(service, position);
                    }

                    public String getErrorMessage() {
                        return RouteConverter.BUNDLE.getString("add-comment-error");
                    }
                }
        );
    }

    public void addComments(PositionsModel positionsModel, int[] selectedRows) {
        addComments(positionsModel, selectedRows, TAUTOLOGY_PREDICATE);
    }

    public void complementComments(PositionsModel positionsModel) {
        addComments(positionsModel, selectAllRows(positionsModel), NO_COMMENT_PREDICATE);
    }


    private void processSpeeds(final PositionsModel positionsModel,
                               final int[] rows,
                               final OverwritePredicate predicate) {
        executeOperation(positionsModel, rows, predicate,
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
                        return RouteConverter.BUNDLE.getString("add-speed-error");
                    }
                }
        );
    }

    public void addSpeeds(PositionsModel positionsModel, int[] selectedRows) {
        processSpeeds(positionsModel, selectedRows, TAUTOLOGY_PREDICATE);
    }

    public void complementSpeeds(PositionsModel positionsModel) {
        processSpeeds(positionsModel, selectAllRows(positionsModel), NO_SPEED_PREDICATE);
    }
}
