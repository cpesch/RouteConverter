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
import slash.navigation.common.NumberPattern;
import slash.navigation.completer.CompletePositionService;
import slash.navigation.converter.gui.RouteConverter;
import slash.navigation.converter.gui.augment.PositionAugmenter;
import slash.navigation.converter.gui.models.PositionsModel;

import java.io.IOException;
import java.util.Calendar;
import java.util.concurrent.ExecutorService;
import java.util.logging.Logger;

import static java.util.Arrays.asList;
import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static javax.swing.SwingUtilities.invokeLater;
import static slash.common.io.Transfer.isEmpty;
import static slash.common.type.CompactCalendar.UTC;
import static slash.common.type.CompactCalendar.fromCalendar;
import static slash.navigation.base.RouteCalculations.extrapolateTime;
import static slash.navigation.base.RouteComments.formatNumberedPosition;
import static slash.navigation.converter.gui.models.PositionColumns.*;

/**
 * Helps to augment a newly created position with elevation, postal address
 * and time information.
 *
 * @author Christian Pesch
 */

public class SinglePositionAugmenter implements PositionAugmenter {
    private static final Logger log = Logger.getLogger(SinglePositionAugmenter.class.getName());
    private final ExecutorService executorService = newSingleThreadExecutor();
    private CompletePositionService completePositionService;
    private PositionsModel positionsModel;

    public SinglePositionAugmenter(PositionsModel positionsModel, CompletePositionService completePositionService) {
        this.positionsModel = positionsModel;
        this.completePositionService = completePositionService;
    }

    public void interrupt() {
        executorService.shutdownNow();
    }

    public String createDescription(int index) {
        String description = RouteConverter.getBundle().getString("new-position-name");
        return createDescription(index, description);
    }

    public String createDescription(int index, String description) {
        NumberPattern numberPattern = RouteConverter.getInstance().getNumberPatternPreference();
        String number = Integer.toString(index);
        return formatNumberedPosition(numberPattern, number, description);
    }

    public void complementElevation(final int row, final Double longitude, final Double latitude) {
        executorService.execute(new Runnable() {
            public void run() {
                completePositionService.downloadElevationDataFor(asList(new LongitudeAndLatitude(longitude, latitude)));

                final Double[] elevation = new Double[1];
                try {
                    elevation[0] = completePositionService.getElevationFor(longitude, latitude);
                } catch (IOException e) {
                    log.warning("Cannot retrieve elevation for " + longitude + "/" + latitude + ": " + e.getMessage());
                }

                if (elevation[0] != null) {
                    invokeLater(new Runnable() {
                        public void run() {
                            if (!isEmpty(elevation[0])) {
                                positionsModel.edit(row, ELEVATION_COLUMN_INDEX, elevation[0], -1, null, true, false);
                            }
                        }
                    });
                }
            }
        });
    }

    public void complementDescription(final int row, final Double longitude, final Double latitude) {
        executorService.execute(new Runnable() {
            public void run() {
                final String[] description = new String[1];
                try {
                    description[0] = completePositionService.getDescriptionFor(longitude, latitude);
                } catch (IOException e) {
                    log.warning("Cannot retrieve description for " + longitude + "/" + latitude + ": " + e.getMessage());
                }

                if (description[0] != null) {
                    invokeLater(new Runnable() {
                        public void run() {
                            if (description[0] != null) {
                                String newDescription = createDescription(row + 1, description[0]);
                                positionsModel.edit(row, DESCRIPTION_COLUMN_INDEX, newDescription, -1, null, true, false);
                            }
                        }
                    });
                }
            }
        });
    }

    public void complementTime(int row, CompactCalendar time, boolean allowCurrentTime) {
        if (time != null)
            return;

        // do not put this in executorService since when called in batches, the edit() must happen before the
        // next time can be complemented
        CompactCalendar interpolated = row - 2 >= 0 ? extrapolateTime(positionsModel.getPosition(row),
                positionsModel.getPosition(row - 1), positionsModel.getPosition(row - 2)) : null;
        // since interpolation is just between the previous positions this leads to errors when inserting
        // more than one position for which no time can be interpolated from the previous positions
        if (interpolated == null && allowCurrentTime)
            interpolated = fromCalendar(Calendar.getInstance(UTC));
        positionsModel.edit(row, TIME_COLUMN_INDEX, interpolated, -1, null, true, false);
    }
}
