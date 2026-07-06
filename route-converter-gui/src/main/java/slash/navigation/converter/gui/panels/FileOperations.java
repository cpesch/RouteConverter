/*
    This file is part of BaseRouteConverter.

    BaseRouteConverter is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    BaseRouteConverter is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with BaseRouteConverter; if not, write to the Free Software
    Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA

    Copyright (C) 2007 Christian Pesch. All Rights Reserved.
*/
package slash.navigation.converter.gui.panels;

import slash.navigation.babel.BabelException;
import slash.navigation.base.*;
import slash.navigation.converter.gui.BaseRouteConverter;
import slash.navigation.converter.gui.dialogs.MaximumPositionCountDialog;
import slash.navigation.copilot.CoPilotFormat;
import slash.navigation.fpl.GarminFlightPlanFormat;
import slash.navigation.fpl.GarminFlightPlanRoute;
import slash.navigation.gpx.Gpx11Format;
import slash.navigation.gpx.GpxRoute;
import slash.navigation.nmn.Nmn7Format;
import slash.navigation.nmn.NmnFormat;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static slash.navigation.gui.helpers.WindowHelper.showError;
import static javax.swing.SwingUtilities.invokeAndWait;
import static javax.swing.SwingUtilities.invokeLater;
import static slash.common.helpers.ExceptionHelper.getLocalizedMessage;
import static slash.common.helpers.ExceptionHelper.printStackTrace;
import static slash.common.helpers.ThreadHelper.createSingleThreadExecutor;
import static slash.common.io.Files.*;
import static slash.navigation.gui.helpers.DialogStrings.asDialogString;
import static slash.navigation.base.NavigationFormatConverter.convertRoute;
import static slash.navigation.base.NavigationFormatParser.getNumberOfFilesToWriteFor;
import static slash.navigation.gui.helpers.UIHelper.startWaitCursor;
import static slash.navigation.gui.helpers.UIHelper.stopWaitCursor;
import static slash.navigation.gui.helpers.WindowHelper.handleOutOfMemoryError;

/**
 * Reading, appending, creating and writing of position list files for the {@link ConvertPanel},
 * kept out of the panel so the file-handling flow lives in one place. Runs the reads on its own
 * single background thread and updates the panel's models on the event thread.
 *
 * @author Christian Pesch
 */

class FileOperations {
    private static final Logger log = Logger.getLogger(FileOperations.class.getName());
    private static final Preferences preferences = Preferences.userNodeForPackage(ConvertPanel.class);
    private static final String READ_PATH_PREFERENCE = "readPath";
    private static final String WRITE_PATH_PREFERENCE = "writePath";
    private static final String DUPLICATE_FIRST_POSITION_PREFERENCE = "duplicateFirstPosition";

    private final ConvertPanel panel;
    private final ExecutorService openExecutor = createSingleThreadExecutor("OpenPositionList");

    FileOperations(ConvertPanel panel) {
        this.panel = panel;
    }

    @SuppressWarnings("unchecked")
    void newFile() {
        if (!panel.confirmDiscard())
            return;

        BaseRouteConverter r = BaseRouteConverter.getInstance();
        startWaitCursor(r.getFrame().getRootPane());
        try {
            Gpx11Format gpxFormat = new Gpx11Format();
            GpxRoute gpxRoute = new GpxRoute(gpxFormat);
            gpxRoute.setName(MessageFormat.format(BaseRouteConverter.getBundle().getString("new-positionlist-name"), 1));
            panel.formatAndRoutesModel.setRoutes(new FormatAndRoutes(gpxFormat, gpxRoute));
            panel.urlModel.clear();
            panel.prepareForNewPositionList();
        } finally {
            stopWaitCursor(r.getFrame().getRootPane());
        }
    }

    void openPositionList(final List<URL> urls, final List<NavigationFormat> formats) {
        final BaseRouteConverter r = BaseRouteConverter.getInstance();

        final URL url = urls.get(0);
        final String path = createReadablePath(url);
        preferences.put(READ_PATH_PREFERENCE, path);

        startWaitCursor(r.getFrame().getRootPane());
        openExecutor.execute(() -> {
            NavigationFormatParser parser = new NavigationFormatParser(panel.getNavigationFormatRegistry());
            NavigationFormatParserListener listener = format -> invokeLater(() -> panel.formatAndRoutesModel.setFormat(format));
            parser.addNavigationFileParserListener(listener);

            try {
                invokeAndWait(() -> {
                    Gpx11Format gpxFormat = new Gpx11Format();
                    panel.formatAndRoutesModel.setRoutes(new FormatAndRoutes(gpxFormat, new GpxRoute(gpxFormat)));
                    panel.urlModel.clear();
                });

                final ParserResult result = parser.read(url, formats);
                if (result.isSuccessful()) {
                    log.info("Opened: " + path);
                    final NavigationFormat format = result.getFormat();
                    panel.countRead(format);
                    if (!ConvertPanel.checkReadFormat(format))
                        return;
                    invokeLater(() -> {
                        panel.formatAndRoutesModel.setRoutes(new FormatAndRoutes(format, result.getAllRoutes()));
                        panel.urlModel.setString(path);
                        panel.recentUrlsModel.addUrl(url);

                        if (urls.size() > 1) {
                            List<URL> append = new ArrayList<>(urls);
                            append.remove(0);
                            // this way the route is always marked as modified :-(
                            appendPositionList(-1, append);
                        }
                    });

                } else {
                    invokeLater(() -> {
                        Gpx11Format gpxFormat = new Gpx11Format();
                        panel.formatAndRoutesModel.setRoutes(new FormatAndRoutes(gpxFormat, new GpxRoute(gpxFormat)));
                    });
                    r.handleUnsupportedFormat(path);
                }
            } catch (BabelException e) {
                r.handleBabelError(e);
            } catch (OutOfMemoryError e) {
                handleOutOfMemoryError(e);
            } catch (FileNotFoundException e) {
                r.handleFileNotFound(path);
            } catch (Throwable t) {
                r.handleOpenError(t, path);
            } finally {
                parser.removeNavigationFileParserListener(listener);
                invokeLater(() -> stopWaitCursor(r.getFrame().getRootPane()));
            }
        });
    }

    void saveFile(File file, NavigationFormat format,
                  boolean exportSelectedRoute, boolean confirmOverwrite, boolean openAfterSave) {
        BaseRouteConverter r = BaseRouteConverter.getInstance();
        if (file.getParent() != null)
            preferences.put(WRITE_PATH_PREFERENCE + format.getClass().getSimpleName(), file.getParent());

        boolean duplicateFirstPosition = format instanceof NmnFormat && !(format instanceof Nmn7Format) || format instanceof CoPilotFormat;
        BaseRoute route = panel.formatAndRoutesModel.getSelectedRoute();
        int fileCount = getNumberOfFilesToWriteFor(route, format, duplicateFirstPosition);

        if (fileCount > 1) {
            int order = route.getPositionCount() / format.getMaximumPositionCount() + 1;
            int reducedPositionCount = route.getPositionCount() / order;

            MaximumPositionCountDialog dialog = new MaximumPositionCountDialog(file, route.getPositionCount(), fileCount, reducedPositionCount, format);
            dialog.showWithPreferences();

            switch (dialog.getResult()) {
                case Split:
                    break;
                case Reduce:
                    r.selectAllButEveryNthPosition(order);
                    r.getContext().getActionManager().run("delete-position");
                    fileCount = 1;
                    break;
                case Ignore:
                    fileCount = 1;
                    break;
                default:
                    return;
            }
        }

        File[] targets = createTargetFiles(file, fileCount, format.getExtension(), 255);
        if (confirmOverwrite) {
            for (File target : targets) {
                if (target.exists()) {
                    String path = createReadablePath(target);
                    if (panel.confirmOverwrite(path))
                        return;
                    break;
                }
            }
        }

        saveFiles(targets, format, route, exportSelectedRoute, confirmOverwrite, openAfterSave);
    }

    private void saveFiles(File[] files, NavigationFormat format, BaseRoute route,
                           boolean exportSelectedRoute, boolean confirmOverwrite, boolean openAfterSave) {
        final BaseRouteConverter r = BaseRouteConverter.getInstance();
        String targetsAsString = asDialogString(asList(files), true);
        startWaitCursor(r.getFrame().getRootPane());
        try {
            if (!ConvertPanel.checkWriteFormat(format))
                return;
            if (format.isSupportsMultipleRoutes()) {
                List<BaseRoute> routes = exportSelectedRoute ? singletonList(route) : panel.formatAndRoutesModel.getRoutes();
                new NavigationFormatParser(panel.getNavigationFormatRegistry()).write(routes, (MultipleRoutesFormat) format, files[0]);
            } else {
                boolean duplicateFirstPosition = preferences.getBoolean(DUPLICATE_FIRST_POSITION_PREFERENCE, true);
                ParserCallback parserCallback = (aRoute, aFormat) -> {
                    if (aFormat instanceof GarminFlightPlanFormat) {
                        GarminFlightPlanRoute garminFlightPlanRoute = (GarminFlightPlanRoute) aRoute;
                        panel.completeGarminFlightPlan(garminFlightPlanRoute);
                    }
                };
                new NavigationFormatParser(panel.getNavigationFormatRegistry()).write(route, format, duplicateFirstPosition, true, parserCallback, files);
            }
            panel.formatAndRoutesModel.setModified(false);
            panel.recentFormatsModel.addFormat(format);
            panel.countWrite(format);
            log.info(format("Saved: %s", targetsAsString));

            if (!exportSelectedRoute && format.isSupportsReading()) {
                if (openAfterSave) {
                    openPositionList(toUrls(files), panel.getNavigationFormatRegistry().getReadFormatsWithPreferredFormat(format));
                    log.info(format("Open after save: %s", files[0]));
                }
                if (confirmOverwrite) {
                    URL url = files[0].toURI().toURL();
                    String path = createReadablePath(url);
                    panel.urlModel.setString(path);
                    panel.recentUrlsModel.addUrl(url);
                }
            }
        } catch (Throwable t) {
            log.severe(format("Error saving %s in %s: %s, %s", files[0], format, t, printStackTrace(t)));

            String source = panel.urlModel.getShortUrl();
            // if there is no source a new file is saved
            if (source == null)
                source = route.getName();

            showError(r.getFrame(),
                    MessageFormat.format(BaseRouteConverter.getBundle().getString("save-error"), source, targetsAsString, getLocalizedMessage(t)),
                    r.getFrame().getTitle());
        } finally {
            stopWaitCursor(r.getFrame().getRootPane());
        }
    }

    void appendPositionList(final int row, final List<URL> urls) {
        final BaseRouteConverter r = BaseRouteConverter.getInstance();
        openExecutor.execute(() -> {
            try {
                for (URL url : urls) {
                    String path = createReadablePath(url);

                    NavigationFormatParser parser = new NavigationFormatParser(panel.getNavigationFormatRegistry());
                    final ParserResult result = parser.read(url);
                    if (result.isSuccessful()) {
                        log.info("Appended: " + path);
                        panel.countRead(result.getFormat());

                        final String finalPath = path;
                        // avoid parallelism to ensure the URLs are processed in order
                        invokeAndWait(() -> {
                            // when called from openPositionList() and the format supports more than one position list:
                            // append the position lists at the end
                            NavigationFormat<BaseRoute> format = panel.getFormatAndRoutesModel().getFormat();
                            if (row == -1 && format.isSupportsMultipleRoutes()) {
                                try {
                                    List<BaseRoute> routes = convertRoute(result.getAllRoutes(), format);
                                    for (BaseRoute route : routes) {
                                        int appendIndex = panel.getFormatAndRoutesModel().getSize();
                                        panel.getFormatAndRoutesModel().addPositionList(appendIndex, route);
                                    }
                                } catch (IOException e) {
                                    r.handleOpenError(e, finalPath);
                                }
                            } else {
                                // insert all position lists, which are in reverse order, at the given row or at the end
                                try {
                                    int insertRow = row > 0 ? row : panel.positionsModel.getRowCount();
                                    for (BaseRoute route : result.getAllRoutes()) {
                                        //noinspection unchecked
                                        panel.positionsModel.add(insertRow, route);
                                    }
                                } catch (FileNotFoundException e) {
                                    r.handleFileNotFound(finalPath);
                                } catch (IOException e) {
                                    r.handleOpenError(e, finalPath);
                                }
                            }
                        });

                    } else {
                        r.handleUnsupportedFormat(path);
                    }
                }
            } catch (BabelException e) {
                r.handleBabelError(e);
            } catch (OutOfMemoryError e) {
                handleOutOfMemoryError(e);
            } catch (Throwable t) {
                log.severe("Append error: " + t);
                r.handleOpenError(t, urls);
            }
        });
    }

}
