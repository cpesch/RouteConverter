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
    along with Foobar; if not, write to the Free Software
    Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA

    Copyright (C) 2007 Christian Pesch. All Rights Reserved.
*/

package slash.navigation.converter.gui;

import slash.navigation.BaseNavigationPosition;
import slash.navigation.RouteComments;
import slash.navigation.gui.Constants;
import slash.navigation.converter.gui.models.PositionsModel;
import slash.navigation.geonames.GeoNamesService;
import slash.navigation.util.Conversion;

import javax.swing.*;
import java.io.IOException;
import java.text.MessageFormat;

/**
 * Helps to augment positions with elevation and description information.
 *
 * @author Christian Pesch
 */

public class PositionAugmenter {
    private RouteConverter routeConverter;

    public PositionAugmenter(RouteConverter routeConverter) {
        this.routeConverter = routeConverter;
    }

    private interface OverwritePredicate {
        boolean shouldOverwrite(BaseNavigationPosition position);
    }

    private static final OverwritePredicate TAUTOLOGY_PREDICATE = new OverwritePredicate() {
        public boolean shouldOverwrite(BaseNavigationPosition position) {
            return true;
        }
    };

    private boolean addElevation(GeoNamesService service, final BaseNavigationPosition position) throws IOException {
        final Integer elevation = service.getElevationFor(position.getLongitude(), position.getLatitude());
        if (elevation != null)
            position.setElevation(elevation.doubleValue());
        return elevation != null;
    }

    private boolean addDescription(GeoNamesService service, BaseNavigationPosition position) throws IOException {
        String description = service.getNearByFor(position.getLongitude(), position.getLatitude());
        if (description != null)
            position.setComment(description);
        return description != null;
    }

    private boolean hasLongitudeAndLatitude(BaseNavigationPosition position) {
        return position.getLongitude() != null && position.getLatitude() != null;
    }


    private void addElevations(final PositionsModel positionsModel, final OverwritePredicate predicate) {
        Constants.startWaitCursor(routeConverter.getFrame().getRootPane());

        new Thread(new Runnable() {
            public void run() {
                try {
                    IOException exception = null;
                    GeoNamesService service = new GeoNamesService();
                    for (int i = 0, c = positionsModel.getRowCount(); i < c; i++) {
                        BaseNavigationPosition position = positionsModel.getPosition(i);
                        if (hasLongitudeAndLatitude(position) && predicate.shouldOverwrite(position)) {
                            try {
                                if (addElevation(service, position)) {
                                    final int index = i;
                                    SwingUtilities.invokeLater(new Runnable() {
                                        public void run() {
                                            positionsModel.fireTableRowsUpdated(index, index);
                                        }
                                    });
                                }
                            } catch (IOException e) {
                                exception = e;
                            }
                        }
                    }
                    if (exception != null)
                        JOptionPane.showMessageDialog(routeConverter.getFrame(),
                                MessageFormat.format(RouteConverter.BUNDLE.getString("add-elevation-error"), exception.getMessage()),
                                routeConverter.getFrame().getTitle(), JOptionPane.ERROR_MESSAGE);
                }
                finally {
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            Constants.stopWaitCursor(routeConverter.getFrame().getRootPane());
                        }
                    });
                }
            }
        }, "ElevationPositionAugmenter").start();
    }

    public void addElevations(PositionsModel positionsModel) {
        addElevations(positionsModel, TAUTOLOGY_PREDICATE);
    }

    public void complementElevations(PositionsModel positionsModel) {
        addElevations(positionsModel, new OverwritePredicate() {
            public boolean shouldOverwrite(BaseNavigationPosition position) {
                return position.getElevation() == null || position.getElevation() == 0.0;
            }
        });
    }


    private void addDescriptions(final PositionsModel positionsModel, final OverwritePredicate predicate) {
        Constants.startWaitCursor(routeConverter.getFrame().getRootPane());

        new Thread(new Runnable() {
            public void run() {
                try {
                    IOException exception = null;
                    GeoNamesService service = new GeoNamesService();
                    for (int i = 0, c = positionsModel.getRowCount(); i < c; i++) {
                        BaseNavigationPosition position = positionsModel.getPosition(i);
                        if (hasLongitudeAndLatitude(position) && predicate.shouldOverwrite(position)) {
                            try {
                                if (addDescription(service, position)) {
                                    final int index = i;
                                    SwingUtilities.invokeLater(new Runnable() {
                                        public void run() {
                                            positionsModel.fireTableRowsUpdated(index, index);
                                        }
                                    });
                                }
                            } catch (IOException e) {
                                exception = e;
                            }
                        }
                    }
                    if (exception != null)
                        JOptionPane.showMessageDialog(routeConverter.getFrame(),
                                MessageFormat.format(RouteConverter.BUNDLE.getString("add-description-error"), exception.getMessage()),
                                routeConverter.getFrame().getTitle(), JOptionPane.ERROR_MESSAGE);
                }
                finally {
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            Constants.stopWaitCursor(routeConverter.getFrame().getRootPane());
                        }
                    });
                }
            }
        }, "DescriptionPositionAugmenter").start();
    }

    public void addDescriptions(PositionsModel positionsModel) {
        addDescriptions(positionsModel, TAUTOLOGY_PREDICATE);
    }

    public void complementDescriptions(PositionsModel positionsModel) {
        addDescriptions(positionsModel, new OverwritePredicate() {
            public boolean shouldOverwrite(BaseNavigationPosition position) {
                return Conversion.trim(position.getComment()) == null ||
                        RouteComments.isPositionComment(position.getComment());
            }
        });
    }
}
