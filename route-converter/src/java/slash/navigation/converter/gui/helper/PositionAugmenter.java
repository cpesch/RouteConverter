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
import slash.navigation.RouteComments;
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


    private boolean addElevation(GeoNamesService service, final BaseNavigationPosition position) throws IOException {
        final Integer elevation = service.getElevationFor(position.getLongitude(), position.getLatitude());
        if (elevation != null)
            position.setElevation(elevation.doubleValue());
        return elevation != null;
    }

    private boolean addComment(GeoNamesService service, BaseNavigationPosition position) throws IOException {
        String comment = service.getNearByFor(position.getLongitude(), position.getLatitude());
        if (comment != null)
            position.setComment(comment);
        return comment != null;
    }

    private boolean hasLongitudeAndLatitude(BaseNavigationPosition position) {
        return position.getLongitude() != null && position.getLatitude() != null;
    }


    private void addElevations(final PositionsModel positionsModel,
                               final int[] rows,
                               final OverwritePredicate predicate) {
        Constants.startWaitCursor(frame.getRootPane());

        new Thread(new Runnable() {
            public void run() {
                try {
                    IOException exception = null;
                    GeoNamesService service = new GeoNamesService();
                    for (final int row : rows) {
                        BaseNavigationPosition position = positionsModel.getPosition(row);
                        if (hasLongitudeAndLatitude(position) && predicate.shouldOverwrite(position)) {
                            try {
                                if (addElevation(service, position)) {
                                    SwingUtilities.invokeLater(new Runnable() {
                                        public void run() {
                                            positionsModel.fireTableRowsUpdated(row, row);
                                        }
                                    });
                                }
                            } catch (IOException e) {
                                exception = e;
                            }
                        }
                    }
                    if (exception != null)
                        JOptionPane.showMessageDialog(frame,
                                MessageFormat.format(RouteConverter.BUNDLE.getString("add-elevation-error"), exception.getMessage()),
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
        }, "ElevationPositionAugmenter").start();
    }

    private void addAllElevations(PositionsModel positionsModel, OverwritePredicate predicate) {
        int[] selectedRows = new int[positionsModel.getRowCount()];
        for (int i = 0; i < selectedRows.length; i++)
            selectedRows[i] = i;
        addElevations(positionsModel, selectedRows, predicate);
    }

    public void addElevations(PositionsModel positionsModel) {
        addAllElevations(positionsModel, TAUTOLOGY_PREDICATE);
    }

    public void addElevations(PositionsModel positionsModel, int[] selectedRows) {
        addElevations(positionsModel, selectedRows, TAUTOLOGY_PREDICATE);
    }

    public void complementElevations(PositionsModel positionsModel) {
        addAllElevations(positionsModel, NO_ELEVATION_PREDICATE);
    }

    public void complementElevations(PositionsModel positionsModel, int[] selectedRows) {
        addElevations(positionsModel, selectedRows, NO_ELEVATION_PREDICATE);
    }


    private void addComments(final PositionsModel positionsModel,
                             final int[] rows,
                             final OverwritePredicate predicate) {
        Constants.startWaitCursor(frame.getRootPane());

        new Thread(new Runnable() {
            public void run() {
                try {
                    IOException exception = null;
                    GeoNamesService service = new GeoNamesService();
                    for (final int row : rows) {
                        BaseNavigationPosition position = positionsModel.getPosition(row);
                        if (hasLongitudeAndLatitude(position) && predicate.shouldOverwrite(position)) {
                            try {
                                if (addComment(service, position)) {
                                    SwingUtilities.invokeLater(new Runnable() {
                                        public void run() {
                                            positionsModel.fireTableRowsUpdated(row, row);
                                        }
                                    });
                                }
                            } catch (IOException e) {
                                exception = e;
                            }
                        }
                    }
                    if (exception != null)
                        JOptionPane.showMessageDialog(frame,
                                MessageFormat.format(RouteConverter.BUNDLE.getString("add-comment-error"), exception.getMessage()),
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
        }, "CommentPositionAugmenter").start();
    }

    private void addAllComments(PositionsModel positionsModel, OverwritePredicate predicate) {
        int[] selectedRows = new int[positionsModel.getRowCount()];
        for (int i = 0; i < selectedRows.length; i++)
            selectedRows[i] = i;
        addComments(positionsModel, selectedRows, predicate);
    }

    public void addComments(PositionsModel positionsModel) {
        addAllComments(positionsModel, TAUTOLOGY_PREDICATE);
    }

    public void addComments(PositionsModel positionsModel, int[] selectedRows) {
        addComments(positionsModel, selectedRows, TAUTOLOGY_PREDICATE);
    }

    public void complementComments(PositionsModel positionsModel) {
        addAllComments(positionsModel, NO_COMMENT_PREDICATE);
    }

    public void complementComments(PositionsModel positionsModel, int[] selectedRows) {
        addComments(positionsModel, selectedRows, NO_COMMENT_PREDICATE);
    }
}
