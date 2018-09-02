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
import slash.navigation.base.BaseNavigationPosition;
import slash.navigation.base.NavigationFormatParser;
import slash.navigation.base.ParserResult;
import slash.navigation.base.Wgs84Position;
import slash.navigation.base.Wgs84Route;
import slash.navigation.common.NavigationPosition;
import slash.navigation.converter.gui.RouteConverter;
import slash.navigation.converter.gui.models.PositionsModel;
import slash.navigation.gui.Application;
import slash.navigation.gui.events.ContinousRange;
import slash.navigation.gui.events.RangeOperation;
import slash.navigation.gui.notifications.NotificationManager;
import slash.navigation.photo.PhotoFormat;
import slash.navigation.photo.PhotoNavigationFormatRegistry;
import slash.navigation.photo.PhotoPosition;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

import static java.lang.Math.min;
import static java.lang.String.format;
import static java.lang.System.currentTimeMillis;
import static java.util.Collections.singletonList;
import static javax.swing.JOptionPane.ERROR_MESSAGE;
import static javax.swing.JOptionPane.showMessageDialog;
import static javax.swing.SwingUtilities.invokeLater;
import static javax.swing.event.TableModelEvent.ALL_COLUMNS;
import static slash.common.helpers.ExceptionHelper.getLocalizedMessage;
import static slash.common.helpers.ExceptionHelper.printStackTrace;
import static slash.common.helpers.ThreadHelper.createSingleThreadExecutor;
import static slash.common.io.Directories.ensureDirectory;
import static slash.common.io.Files.collectFiles;
import static slash.common.type.CompactCalendar.fromMillis;
import static slash.navigation.base.WaypointType.Photo;
import static slash.navigation.converter.gui.helpers.TagStrategy.Create_Tagged_Photo_In_Subdirectory;
import static slash.navigation.gui.events.Range.asRange;
import static slash.navigation.gui.helpers.JTableHelper.scrollToPosition;
import static slash.navigation.photo.TagState.NotTaggable;
import static slash.navigation.photo.TagState.Taggable;
import static slash.navigation.photo.TagState.Tagged;

/**
 * Helps to tag photos with GPS data.
 *
 * @author Christian Pesch
 */

public class GeoTagger {
    private static final Preferences preferences = Preferences.userNodeForPackage(GeoTagger.class);
    private static final Logger log = Logger.getLogger(GeoTagger.class.getName());

    private static final String CLOSEST_POSITION_BY_COORDINATES_THRESHOLD_PREFERENCE = "closestPositionByCoordinatesThreshold";
    private static final String CLOSEST_POSITION_BY_TIME_THRESHOLD_PREFERENCE = "closestPositionByTimeThreshold";

    private final JFrame frame;
    private final JTable photosView;
    private final PositionsModel photosModel;

    private final ExecutorService executor = createSingleThreadExecutor("GeoTagger");
    private static final Object notificationMutex = new Object();
    private boolean running = true;

    public GeoTagger(JTable photosView, PositionsModel photosModel, JFrame frame) {
        this.photosView = photosView;
        this.photosModel = photosModel;
        this.frame = frame;
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

    private interface Operation {
        String getName();
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

    public void addPhotos(final List<File> filesAndDirectories) {
        synchronized (notificationMutex) {
            this.running = true;
        }

        final CancelAction cancelAction = new CancelAction();
        executor.execute(new Runnable() {
            public void run() {
                final int[] count = new int[1];
                try {
                    final List<File> files = collectFiles(filesAndDirectories);

                    final Exception[] lastException = new Exception[1];
                    lastException[0] = null;

                    for (File file : files) {
                        try {
                            final PhotoPosition position = extractPhotoPosition(file);

                            invokeLater(new Runnable() {
                                public void run() {
                                    photosModel.add(photosModel.getRowCount(), new ArrayList<BaseNavigationPosition>(singletonList(position)));
                                    scrollToPosition(photosView, photosModel.getRowCount() - 1);
                                }
                            });

                            synchronized (notificationMutex) {
                                if (cancelAction.isCanceled() || !running)
                                    break;
                            }
                        } catch (Exception e) {
                            log.warning(format("Error while running operation AddPhotos on file %s: %s, %s", file, e, printStackTrace(e)));
                            lastException[0] = e;
                        }
                        getNotificationManager().showNotification(MessageFormat.format(
                                RouteConverter.getBundle().getString("add-photos-progress"), count[0]++, files.size()), cancelAction);
                    }

                    if (lastException[0] != null)
                        showMessageDialog(frame,
                                MessageFormat.format(RouteConverter.getBundle().getString("add-photos-error"), getLocalizedMessage(lastException[0])),
                                frame.getTitle(), ERROR_MESSAGE);

                } finally {
                    invokeLater(new Runnable() {
                        public void run() {
                            getNotificationManager().showNotification(MessageFormat.format(
                                    RouteConverter.getBundle().getString("add-photos-finished"), count[0]), null);
                        }
                    });
                }
            }
        });
    }

    private PhotoPosition extractPhotoPosition(File file) throws IOException {
        PhotoPosition position = extractMetadata(file);
        updateClosestPositionForTagging(position);
        return position;
    }

    private void updateClosestPositionForTagging(PhotoPosition position) {
        position.setTagState(NotTaggable);
        position.setClosestPositionForTagging(null);

        PositionsModel originalPositionsModel = RouteConverter.getInstance().getConvertPanel().getPositionsModel();
        int index = getClosestPositionByCoordinates(position);
        if (index != -1) {
            log.info("Tagging with closest position " + index + " by coordinates: " + position);
            position.setTagState(Tagged);
            position.setClosestPositionForTagging(originalPositionsModel.getPosition(index));

        } else {
            index = getClosestPositionByTime(position);
            if (index != -1) {
                log.info("Tagging with closest position " + index + " by time: " + position);
                position.setTagState(Taggable);
                position.setClosestPositionForTagging(originalPositionsModel.getPosition(index));
            }
        }
    }

    private int getClosestPositionByCoordinates(NavigationPosition position) {
        PositionsModel originalPositionsModel = RouteConverter.getInstance().getConvertPanel().getPositionsModel();
        double threshold = preferences.getDouble(CLOSEST_POSITION_BY_COORDINATES_THRESHOLD_PREFERENCE, 25);
        return position.hasCoordinates() ? originalPositionsModel.getClosestPosition(position.getLongitude(), position.getLatitude(), threshold) : -1;
    }

    private int getClosestPositionByTime(NavigationPosition position) {
        if (!position.hasTime())
            return -1;

        RouteConverter r = RouteConverter.getInstance();
        PositionsModel originalPositionsModel = r.getConvertPanel().getPositionsModel();
        CompactCalendar time = position.getTime();
        if (!time.getTimeZoneId().equals(r.getPhotoTimeZone().getTimeZoneId()))
            time = time.asUTCTimeInTimeZone(r.getPhotoTimeZone().getTimeZone());
        long threshold = preferences.getLong(CLOSEST_POSITION_BY_TIME_THRESHOLD_PREFERENCE, 5 * 1000);
        return originalPositionsModel.getClosestPosition(time, threshold);
    }

    private PhotoPosition extractMetadata(File file) throws IOException {
        long start = currentTimeMillis();
        try {
            NavigationFormatParser parser = new NavigationFormatParser(new PhotoNavigationFormatRegistry());
            ParserResult parserResult = parser.read(file);
            if (parserResult.isSuccessful()) {
                Wgs84Route route = Wgs84Route.class.cast(parserResult.getTheRoute());
                if (route.getPositionCount() > 0)
                    return (PhotoPosition) route.getPosition(0);
            }
            return new PhotoPosition(NotTaggable, fromMillis(file.lastModified()), "No Metadata found", file);
        } finally {
            long end = currentTimeMillis();
            log.info("Extracting metadata from " + file + " took " + (end - start) + " milliseconds");
        }
    }

    private void executeOperation(final JTable positionsTable,
                                  final PositionsModel positionsModel,
                                  final int[] rows,
                                  final Operation operation) {
        synchronized (notificationMutex) {
            this.running = true;
        }

        final CancelAction cancelAction = new CancelAction();
        executor.execute(new Runnable() {
            public void run() {
                final int[] count = new int[1];
                try {
                    invokeLater(new Runnable() {
                        public void run() {
                            if (positionsTable != null && rows.length > 0)
                                scrollToPosition(positionsTable, rows[0]);
                        }
                    });

                    final Exception[] lastException = new Exception[1];
                    lastException[0] = null;
                    final int maximumRangeLength = rows.length > 99 ? rows.length / 100 : rows.length;

                    new ContinousRange(rows, new RangeOperation() {
                        public void performOnIndex(final int index) {
                            NavigationPosition position = positionsModel.getPosition(index);
                            try {
                                operation.run(index, position);
                            } catch (Exception e) {
                                log.warning(format("Error while running operation %s on position %d: %s, %s", operation, index, e, printStackTrace(e)));
                                lastException[0] = e;
                            }
                            String progressMessage = RouteConverter.getBundle().getString(operation.getMessagePrefix() + "progress");
                            getNotificationManager().showNotification(MessageFormat.format(progressMessage, count[0]++, rows.length), cancelAction);
                        }

                        public void performOnRange(final int firstIndex, final int lastIndex) {
                            invokeLater(new Runnable() {
                                public void run() {
                                    positionsModel.fireTableRowsUpdated(firstIndex, lastIndex, ALL_COLUMNS);
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
                            String finishedMessage = RouteConverter.getBundle().getString(operation.getMessagePrefix() + "finished");
                            getNotificationManager().showNotification(MessageFormat.format(finishedMessage, count[0]), null);
                        }
                    });
                }
            }
        });
    }


    private void updateMetaData(PhotoPosition position, NavigationPosition closestPositionForTagging,
                                TagStrategy tagStrategy) throws IOException {
        File source = position.getOrigin(File.class);
        File target = null;

        long start = currentTimeMillis();
        try {
            position.setLongitude(closestPositionForTagging.getLongitude());
            position.setLatitude(closestPositionForTagging.getLatitude());
            position.setElevation(closestPositionForTagging.getElevation());
            position.setSpeed(closestPositionForTagging.getSpeed());
            position.setWaypointType(Photo);

            if (tagStrategy.equals(Create_Tagged_Photo_In_Subdirectory)) {
                File subDirectory = createSubDirectory(source, "tagged");
                target = new File(subDirectory, source.getName());

            } else {
                File subDirectory = createSubDirectory(source, "bak");
                File sourceBackup = new File(subDirectory, source.getName());
                if (!source.renameTo(sourceBackup))
                    throw new IOException(format("Cannot rename %s to %s", source.getPath(), subDirectory.getPath()));
                target = source;
                source = sourceBackup;
            }

            new PhotoFormat().write(position, source, new FileOutputStream(target));

            position.setTagState(Tagged);

            if (closestPositionForTagging instanceof Wgs84Position) {
                Wgs84Position wgs84Position = Wgs84Position.class.cast(closestPositionForTagging);
                wgs84Position.setDescription(source.getAbsolutePath());
                wgs84Position.setWaypointType(Photo);
                wgs84Position.setOrigin(source);

                PositionsModel originalPositionsModel = RouteConverter.getInstance().getConvertPanel().getPositionsModel();
                int index = originalPositionsModel.getIndex(wgs84Position);
                originalPositionsModel.fireTableRowsUpdated(index, index, ALL_COLUMNS);
            }

        } finally {
            long end = currentTimeMillis();
            log.info("Updating metadata of " + target + " took " + (end - start) + " milliseconds");
        }
    }

    private File createSubDirectory(File source, String name) {
        File subDirectory = new File(source.getParentFile(), name);
        return ensureDirectory(subDirectory);
    }

    public void updateClosestPositionsForTagging() {
        int[] rows = asRange(0, photosModel.getRowCount() - 1);
        executeOperation(photosView, photosModel, rows, new Operation() {
            public String getName() {
                return "UpdateClosestPositionForTagging";
            }

            public boolean run(int index, NavigationPosition navigationPosition) {
                if (!(navigationPosition instanceof PhotoPosition))
                    return false;

                PhotoPosition position = PhotoPosition.class.cast(navigationPosition);
                if (position.getTagState().equals(Tagged))
                    return false;

                updateClosestPositionForTagging(position);
                return true;
            }

            public String getMessagePrefix() {
                return "update-closest-position-";
            }
        });
    }

    public void tagPhotos() {
        int[] rows = photosView.getSelectedRows();
        if (rows.length > 0) {
            final TagStrategy tagStrategy = RouteConverter.getInstance().getTagStrategyPreference();

            executeOperation(photosView, photosModel, rows, new Operation() {
                public String getName() {
                    return "TagPhotosTagger";
                }

                public boolean run(int index, NavigationPosition navigationPosition) throws Exception {
                    if (!(navigationPosition instanceof PhotoPosition))
                        return false;

                    PhotoPosition position = PhotoPosition.class.cast(navigationPosition);
                    if (!position.getTagState().equals(Taggable))
                        return false;

                    NavigationPosition closestPositionForTagging = position.getClosestPositionForTagging();
                    if (closestPositionForTagging == null)
                        return false;

                    updateMetaData(position, closestPositionForTagging, tagStrategy);
                    return true;
                }

                public String getMessagePrefix() {
                    return "tag-photos-";
                }
            });
        }
    }
}
