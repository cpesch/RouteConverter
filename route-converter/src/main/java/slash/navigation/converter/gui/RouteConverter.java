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

package slash.navigation.converter.gui;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import slash.common.io.Files;
import slash.common.log.LoggingHelper;
import slash.common.system.Platform;
import slash.common.system.Version;
import slash.common.type.CompactCalendar;
import slash.navigation.babel.BabelException;
import slash.navigation.base.NavigationPosition;
import slash.navigation.base.RouteCharacteristics;
import slash.navigation.base.Wgs84Position;
import slash.navigation.common.NumberPattern;
import slash.navigation.completer.CompletePositionService;
import slash.navigation.converter.gui.actions.AboutAction;
import slash.navigation.converter.gui.actions.CheckForUpdateAction;
import slash.navigation.converter.gui.actions.CompleteFlightPlanAction;
import slash.navigation.converter.gui.actions.ConvertRouteToTrackAction;
import slash.navigation.converter.gui.actions.ConvertTrackToRouteAction;
import slash.navigation.converter.gui.actions.DeletePositionsAction;
import slash.navigation.converter.gui.actions.FindPlaceAction;
import slash.navigation.converter.gui.actions.InsertPositionsAction;
import slash.navigation.converter.gui.actions.MoveSplitPaneDividersAction;
import slash.navigation.converter.gui.actions.OptionsAction;
import slash.navigation.converter.gui.actions.RevertPositionListAction;
import slash.navigation.converter.gui.actions.SendErrorReportAction;
import slash.navigation.converter.gui.augment.PositionAugmenter;
import slash.navigation.converter.gui.dnd.PanelDropHandler;
import slash.navigation.converter.gui.helper.BatchPositionAugmenter;
import slash.navigation.converter.gui.helper.FrameMenu;
import slash.navigation.converter.gui.helper.MergePositionListMenu;
import slash.navigation.converter.gui.helper.ReopenMenuSynchronizer;
import slash.navigation.converter.gui.helper.RouteServiceOperator;
import slash.navigation.converter.gui.helper.ShowProfileMenu;
import slash.navigation.converter.gui.helper.SinglePositionAugmenter;
import slash.navigation.converter.gui.helper.UndoMenuSynchronizer;
import slash.navigation.converter.gui.mapview.EclipseSWTMapView;
import slash.navigation.converter.gui.mapview.MapView;
import slash.navigation.converter.gui.mapview.MapViewListener;
import slash.navigation.converter.gui.mapview.TravelMode;
import slash.navigation.converter.gui.models.PositionsModel;
import slash.navigation.converter.gui.models.PositionsSelectionModel;
import slash.navigation.converter.gui.models.RecentUrlsModel;
import slash.navigation.converter.gui.models.UnitSystemModel;
import slash.navigation.converter.gui.panels.BrowsePanel;
import slash.navigation.converter.gui.panels.ConvertPanel;
import slash.navigation.converter.gui.panels.PanelInTab;
import slash.navigation.converter.gui.profileview.ProfileMode;
import slash.navigation.converter.gui.profileview.ProfileView;
import slash.navigation.feedback.domain.RouteFeedback;
import slash.navigation.gui.Application;
import slash.navigation.gui.SingleFrameApplication;
import slash.navigation.gui.actions.ActionManager;
import slash.navigation.gui.actions.ExitAction;
import slash.navigation.gui.actions.FrameAction;
import slash.navigation.gui.actions.HelpTopicsAction;
import slash.navigation.rest.Credentials;

import javax.help.CSH;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.EventObject;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.TimeZone;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

import static com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER;
import static com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH;
import static com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW;
import static com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK;
import static java.awt.event.KeyEvent.VK_F1;
import static java.awt.event.KeyEvent.VK_HELP;
import static java.lang.Integer.MAX_VALUE;
import static java.util.Arrays.asList;
import static java.util.Locale.CHINA;
import static java.util.Locale.FRANCE;
import static java.util.Locale.GERMANY;
import static java.util.Locale.ITALY;
import static java.util.Locale.US;
import static javax.swing.JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT;
import static javax.swing.JOptionPane.ERROR_MESSAGE;
import static javax.swing.JOptionPane.QUESTION_MESSAGE;
import static javax.swing.JOptionPane.WARNING_MESSAGE;
import static javax.swing.JOptionPane.showMessageDialog;
import static javax.swing.JSplitPane.DIVIDER_LOCATION_PROPERTY;
import static javax.swing.SwingUtilities.invokeLater;
import static slash.common.io.Files.printArrayToDialogString;
import static slash.common.io.Files.shortenPath;
import static slash.common.io.Files.toUrls;
import static slash.common.system.Platform.getJava;
import static slash.common.system.Platform.getMaximumMemory;
import static slash.common.system.Platform.getPlatform;
import static slash.common.system.Platform.isCurrentAtLeastMinimumVersion;
import static slash.common.system.Version.parseVersionFromManifest;
import static slash.feature.client.Feature.hasFeature;
import static slash.feature.client.Feature.initializePreferences;
import static slash.navigation.common.DegreeFormat.Degrees;
import static slash.navigation.common.NumberPattern.Number_Space_Then_Description;
import static slash.navigation.converter.gui.helper.ExternalPrograms.startBrowserForJava;
import static slash.navigation.converter.gui.helper.ExternalPrograms.startMail;
import static slash.navigation.converter.gui.helper.JMenuHelper.findItem;
import static slash.navigation.converter.gui.helper.JMenuHelper.findMenu;
import static slash.navigation.converter.gui.helper.JMenuHelper.findMenuComponent;
import static slash.navigation.converter.gui.mapview.TravelMode.Driving;
import static slash.navigation.converter.gui.profileview.ProfileMode.Elevation;
import static slash.navigation.converter.gui.profileview.ProfileMode.Speed;
import static slash.navigation.gui.helpers.UIHelper.CROATIA;
import static slash.navigation.gui.helpers.UIHelper.CZECH;
import static slash.navigation.gui.helpers.UIHelper.SERBIA;
import static slash.navigation.gui.helpers.UIHelper.SLOVAKIA;
import static slash.navigation.gui.helpers.UIHelper.SPAIN;
import static slash.navigation.gui.helpers.UIHelper.startWaitCursor;
import static slash.navigation.gui.helpers.UIHelper.stopWaitCursor;

/**
 * A small graphical user interface for the route conversion.
 *
 * @author Christian Pesch
 */

public class RouteConverter extends SingleFrameApplication {
    private static final Logger log = Logger.getLogger(RouteConverter.class.getName());
    private static final Preferences preferences = Preferences.userNodeForPackage(RouteConverter.class);

    public static void main(String[] args) {
        launch(RouteConverter.class, args);
    }

    public static RouteConverter getInstance() {
        return (RouteConverter) Application.getInstance();
    }

    public static ResourceBundle getBundle() {
        return getInstance().getContext().getBundle();
    }

    public static String getTitle() {
        Version version = parseVersionFromManifest();
        return MessageFormat.format(getBundle().getString("title"), version.getVersion(), version.getDate());
    }

    private static String getRouteConverter() {
        Version version = parseVersionFromManifest();
        return version.getOperationSystem() + " (" + version.getBits() + "-bit)";
    }

    public static final String AUTOMATIC_UPDATE_CHECK_PREFERENCE = "automaticUpdateCheck";
    public static final String RECENTER_AFTER_ZOOMING_PREFERENCE = "recenterAfterZooming";
    public static final String SHOW_COORDINATES_PREFERENCE = "showCoordinates";
    public static final String SHOW_WAYPOINT_DESCRIPTION_PREFERENCE = "showWaypointDescription";
    public static final String TRAVEL_MODE_PREFERENCE = "travelMode";
    public static final String AVOID_HIGHWAYS_PREFERENCE = "avoidHighways";
    public static final String AVOID_TOLLS_PREFERENCE = "avoidTolls";
    public static final String NUMBER_PATTERN_PREFERENCE = "numberPattern";
    public static final String TIME_ZONE_PREFERENCE = "timeZone";
    private static final String SELECT_BY_DISTANCE_PREFERENCE = "selectByDistance";
    private static final String SELECT_BY_ORDER_PREFERENCE = "selectByOrder";
    private static final String SELECT_BY_SIGNIFICANCE_PREFERENCE = "selectBySignificance";
    private static final String SEARCH_POSITION_PREFERENCE = "searchPosition";
    private static final String MAP_DIVIDER_LOCATION_PREFERENCE = "mapDividerLocation";
    private static final String PROFILE_DIVIDER_LOCATION_PREFERENCE = "profileDividerLocation";
    private static final String PROFILE_MODE_PREFERENCE = "profileMode";

    private static final String DEBUG_PREFERENCE = "debug";
    private static final String USERNAME_PREFERENCE = "userName";
    private static final String PASSWORD_PREFERENCE = "userAuthentication";
    private static final String CATEGORY_PREFERENCE = "category";
    private static final String UPLOAD_ROUTE_PREFERENCE = "uploadRoute";

    private RouteFeedback routeFeedback;
    private RouteServiceOperator routeServiceOperator;
    private UpdateChecker updateChecker;
    private CompletePositionService completePositionService = new CompletePositionService();
    private UnitSystemModel unitSystemModel = new UnitSystemModel();

    protected JPanel contentPane;
    private JSplitPane mapSplitPane, profileSplitPane;
    private JTabbedPane tabbedPane;
    private JPanel convertPanel, browsePanel, mapPanel, elevationPanel;
    private MapView mapView;
    private ProfileView profileView;
    private static final GridConstraints MAP_PANEL_CONSTRAINTS = new GridConstraints(0, 0, 1, 1, ANCHOR_CENTER, FILL_BOTH,
            SIZEPOLICY_CAN_SHRINK | SIZEPOLICY_CAN_GROW, SIZEPOLICY_CAN_SHRINK | SIZEPOLICY_CAN_GROW,
            new Dimension(0, 0), new Dimension(0, 0), new Dimension(2000, 2640), 0, true);
    private static final GridConstraints ELEVATION_PANEL_CONSTRAINTS = new GridConstraints(0, 0, 1, 1, ANCHOR_CENTER, FILL_BOTH,
            SIZEPOLICY_CAN_SHRINK | SIZEPOLICY_CAN_GROW, SIZEPOLICY_CAN_SHRINK | SIZEPOLICY_CAN_GROW,
            new Dimension(0, 0), new Dimension(0, 0), new Dimension(2000, 300), 0, true);

    private LazyTabInitializer tabInitializer;
    private String[] args;

    // application lifecycle callbacks

    protected void initialize(String[] args) {
        LoggingHelper loggingHelper = LoggingHelper.getInstance();
        loggingHelper.logToFile();
        if (preferences.getBoolean(DEBUG_PREFERENCE, false)) {
            loggingHelper.logToConsole();
        }
        this.args = args;
    }

    protected void startup() {
        log.info("Started " + getTitle() + " for " + getRouteConverter() + " with locale " + Locale.getDefault() +
                " on " + getJava() + " and " + getPlatform() + " with " + getMaximumMemory() + " MByte heap");
        show();
        checkForTooOldJreVersion();
        checkForMissingTranslator();
        updateChecker.implicitCheck(getFrame());
        parseArgs(args);
    }

    private void checkForTooOldJreVersion() {
        String currentVersion = System.getProperty("java.version");
        String minimumVersion = "1.6.0_14";
        if (!isCurrentAtLeastMinimumVersion(currentVersion, minimumVersion)) {
            JLabel label = new JLabel(MessageFormat.format(getBundle().getString("jre-too-old-warning"), currentVersion, minimumVersion));
            label.addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent me) {
                    startBrowserForJava(frame);
                }
            });
            showMessageDialog(frame, label, frame.getTitle(), WARNING_MESSAGE);
        }
    }

    private void checkForMissingTranslator() {
        List<Locale> activeTranslators = asList(CHINA, CROATIA, CZECH, FRANCE, GERMANY, ITALY, SERBIA, SLOVAKIA, SPAIN, US);
        if (!activeTranslators.contains(Locale.getDefault())) {
            JLabel labelTranslatorMissing = new JLabel(MessageFormat.format(getBundle().getString("translator-missing"), Locale.getDefault().getLanguage()));
            labelTranslatorMissing.addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent me) {
                    startMail(frame);
                }
            });
            showMessageDialog(frame, labelTranslatorMissing, frame.getTitle(), QUESTION_MESSAGE);
        }
    }

    private void parseArgs(String[] args) {
        log.info("Processing arguments: " + Arrays.toString(args));
        if (args.length > 0) {
            List<URL> urls = toUrls(args);
            log.info("Processing urls: " + urls);
            getConvertPanel().openUrls(urls);
        } else {
            getConvertPanel().newFile();
        }
    }

    private void patchUIManager(String key) {
        try {
            String text = getBundle().getString(key);
            if (text != null)
                UIManager.getDefaults().put(key, text);
        } catch (MissingResourceException e) {
            // intentionally left empty
        }
    }

    private void show() {
        patchUIManager("OptionPane.yesButtonText");
        patchUIManager("OptionPane.noButtonText");
        patchUIManager("OptionPane.cancelButtonText");
        patchUIManager("FileChooser.openButtonText");
        patchUIManager("FileChooser.saveButtonText");
        patchUIManager("FileChooser.cancelButtonText");
        patchUIManager("FileChooser.acceptAllFileFilterText");

        createFrame(getTitle(), "slash/navigation/converter/gui/RouteConverter.png", contentPane, null, new FrameMenu().createMenuBar());

        addExitListener(new ExitListener() {
            public boolean canExit(EventObject event) {
                return getConvertPanel().confirmDiscard();
            }

            public void willExit(EventObject event) {
            }
        });

        tabInitializer = new LazyTabInitializer();
        tabbedPane.addChangeListener(tabInitializer);

        openFrame();

        mapView = new EclipseSWTMapView();
        if (mapView.isSupportedPlatform()) {
            mapPanel.setVisible(true);
            openMapView();
        } else {
            mapPanel.setVisible(false);
        }
        openProfileView();

        initializeRouteConverterServices();
        initializeActions();
        initializePreferences(preferences);
    }

    private void openFrame() {
        new Thread(new Runnable() {
            public void run() {
                invokeLater(new Runnable() {
                    public void run() {
                        openFrame(contentPane);
                    }
                });
            }
        }, "FrameOpener").start();
    }

    private void openMapView() {
        if (!hasFeature("degree-format"))
            getUnitSystemModel().setDegreeFormat(Degrees);
        invokeLater(new Runnable() {
            public void run() {
                mapView.initialize(getPositionsModel(),
                        getPositionsSelectionModel(),
                        getConvertPanel().getCharacteristicsModel(),
                        getPositionAugmenter(),
                        hasFeature("recenter-after-zooming") && preferences.getBoolean(RECENTER_AFTER_ZOOMING_PREFERENCE, false),
                        hasFeature("show-coordinates") && preferences.getBoolean(SHOW_COORDINATES_PREFERENCE, false),
                        hasFeature("show-waypoint-description") && preferences.getBoolean(SHOW_WAYPOINT_DESCRIPTION_PREFERENCE, false),
                        getTravelModePreference(),
                        preferences.getBoolean(AVOID_HIGHWAYS_PREFERENCE, true),
                        preferences.getBoolean(AVOID_TOLLS_PREFERENCE, true),
                        getUnitSystemModel());

                @SuppressWarnings({"ThrowableResultOfMethodCallIgnored"})
                Throwable cause = mapView.getInitializationCause();
                if (mapView.getComponent() == null || cause != null) {
                    StringWriter stackTrace = new StringWriter();
                    cause.printStackTrace(new PrintWriter(stackTrace));
                    mapPanel.add(new JLabel(MessageFormat.format(getBundle().getString("start-browser-error"),
                            parseVersionFromManifest().getBits(), Platform.getBits(),
                            stackTrace.toString().replaceAll("\n", "<p>"))), MAP_PANEL_CONSTRAINTS);
                } else {
                    mapPanel.add(mapView.getComponent(), MAP_PANEL_CONSTRAINTS);
                }

                int location = preferences.getInt(MAP_DIVIDER_LOCATION_PREFERENCE, -1);
                if (location < 1)
                    location = 300;
                mapSplitPane.setDividerLocation(location);
                log.fine("Initialized map divider to " + location);
                mapSplitPane.addPropertyChangeListener(new MapSplitPaneListener(location));
            }
        });
    }

    private void openProfileView() {
        invokeLater(new Runnable() {
            public void run() {
                profileView = new ProfileView();
                profileView.initialize(getPositionsModel(),
                        getPositionsSelectionModel(),
                        getUnitSystemModel(),
                        getProfileModePreference());
                elevationPanel.add(profileView.getComponent(), ELEVATION_PANEL_CONSTRAINTS);
                elevationPanel.setTransferHandler(new PanelDropHandler());
                elevationPanel.setVisible(true);

                int location = preferences.getInt(PROFILE_DIVIDER_LOCATION_PREFERENCE, -1);
                if (location < 2)
                    location = 888;
                profileSplitPane.setDividerLocation(location);
                log.fine("Initialized elevation divider to " + location);
                profileSplitPane.addPropertyChangeListener(new ProfileSplitPaneListener(location));
            }
        });
    }

    protected void shutdown() {
        if (mapView != null)
            mapView.dispose();
        getConvertPanel().dispose();
        completePositionService.dispose();
        super.shutdown();

        log.info("Shutdown " + getTitle() + " for " + getRouteConverter() + " with locale " + Locale.getDefault() +
                " on " + getJava() + " and " + getPlatform() + " with " + getMaximumMemory() + " MByte heap");
    }

    boolean isAutomaticUpdateCheck() {
        return preferences.getBoolean(AUTOMATIC_UPDATE_CHECK_PREFERENCE, true);
    }

    public int getSelectByDistancePreference() {
        return preferences.getInt(SELECT_BY_DISTANCE_PREFERENCE, 1000);
    }

    public void setSelectByDistancePreference(int selectByDistancePreference) {
        preferences.putInt(SELECT_BY_DISTANCE_PREFERENCE, selectByDistancePreference);
    }

    public int getSelectByOrderPreference() {
        return preferences.getInt(SELECT_BY_ORDER_PREFERENCE, 5);
    }

    public void setSelectByOrderPreference(int selectByOrderPreference) {
        preferences.putInt(SELECT_BY_ORDER_PREFERENCE, selectByOrderPreference);
    }

    public int getSelectBySignificancePreference() {
        return preferences.getInt(SELECT_BY_SIGNIFICANCE_PREFERENCE, 20);
    }

    public void setSelectBySignificancePreference(int selectBySignificancePreference) {
        preferences.putInt(SELECT_BY_SIGNIFICANCE_PREFERENCE, selectBySignificancePreference);
    }

    public String getSearchPositionPreference() {
        return preferences.get(SEARCH_POSITION_PREFERENCE, "");
    }

    public void setSearchPositionPreference(String searchPositionPreference) {
        preferences.put(SEARCH_POSITION_PREFERENCE, searchPositionPreference);
    }

    public Credentials getCredentials() {
        return new Credentials() {
            public String getUserName() {
                return preferences.get(USERNAME_PREFERENCE, "");
            }

            public String getPassword() {
                return new String(preferences.getByteArray(PASSWORD_PREFERENCE, new byte[0]));
            }
        };
    }

    public void setUserNamePreference(String userNamePreference, String passwordPreference) {
        preferences.put(USERNAME_PREFERENCE, userNamePreference);
        preferences.putByteArray(PASSWORD_PREFERENCE, passwordPreference.getBytes());
    }

    public File getUploadRoutePreference() {
        File path = new File(preferences.get(UPLOAD_ROUTE_PREFERENCE, ""));
        return Files.findExistingPath(path);
    }

    public void setUploadRoutePreference(File path) {
        preferences.put(UPLOAD_ROUTE_PREFERENCE, path.getPath());
    }

    public String getCategoryPreference() {
        return preferences.get(CATEGORY_PREFERENCE, "");
    }

    public void setCategoryPreference(String category) {
        preferences.put(CATEGORY_PREFERENCE, category);
    }

    public TravelMode getTravelModePreference() {
        return TravelMode.fromValue(preferences.get(TRAVEL_MODE_PREFERENCE, Driving.toString()));
    }

    public ProfileMode getProfileModePreference() {
        return ProfileMode.fromValue(preferences.get(PROFILE_MODE_PREFERENCE, Elevation.toString()));
    }

    public NumberPattern getNumberPatternPreference() {
        try {
            return NumberPattern.valueOf(preferences.get(NUMBER_PATTERN_PREFERENCE, Number_Space_Then_Description.toString()));
        } catch (IllegalArgumentException e) {
            return Number_Space_Then_Description;
        }
    }

    public void setNumberPatternPreference(NumberPattern numberPattern) {
        preferences.put(NUMBER_PATTERN_PREFERENCE, numberPattern.toString());
    }

    public String getTimeZonePreference() {
        return preferences.get(TIME_ZONE_PREFERENCE, TimeZone.getDefault().getID());
    }

    public void setTimeZonePreference(String timeZoneId) {
        preferences.put(TIME_ZONE_PREFERENCE, timeZoneId);
    }

    // helpers for external components

    public RouteServiceOperator getOperator() {
        return routeServiceOperator;
    }

    public UnitSystemModel getUnitSystemModel() {
        return unitSystemModel;
    }

    // dialogs for external components

    public void handleBabelError(final BabelException e) {
        invokeLater(new Runnable() {
            public void run() {
                showMessageDialog(frame,
                        MessageFormat.format(getBundle().getString("babel-error"), e.getBabelPath()), frame.getTitle(),
                        ERROR_MESSAGE);
            }
        });
    }

    public void handleOutOfMemoryError() {
        // get some air to breath
        System.gc();
        System.runFinalization();

        final long limitBefore = getMaximumMemory();
        final long limitAfter = limitBefore * 2;
        invokeLater(new Runnable() {
            public void run() {
                showMessageDialog(frame,
                        MessageFormat.format(getBundle().getString("out-of-memory-error"), limitBefore, limitAfter),
                        frame.getTitle(), ERROR_MESSAGE);
            }
        });
    }

    public void handleOpenError(final Throwable throwable, final String path) {
        invokeLater(new Runnable() {
            public void run() {
                throwable.printStackTrace();
                log.severe("Open error: " + throwable.getMessage());
                JLabel labelOpenError = new JLabel(MessageFormat.format(getBundle().getString("open-error"), shortenPath(path, 60), throwable.getLocalizedMessage()));
                labelOpenError.addMouseListener(new MouseAdapter() {
                    public void mouseClicked(MouseEvent me) {
                        startMail(frame);
                    }
                });
                showMessageDialog(frame, labelOpenError, frame.getTitle(), ERROR_MESSAGE);
            }
        });
    }

    public void handleOpenError(final Throwable throwable, final List<URL> urls) {
        invokeLater(new Runnable() {
            public void run() {
                throwable.printStackTrace();
                log.severe("Open error: " + throwable.getMessage());
                JLabel labelOpenError = new JLabel(MessageFormat.format(getBundle().getString("open-error"), printArrayToDialogString(urls.toArray(new URL[urls.size()])), throwable.getLocalizedMessage()));
                labelOpenError.addMouseListener(new MouseAdapter() {
                    public void mouseClicked(MouseEvent me) {
                        startMail(frame);
                    }
                });
                showMessageDialog(frame, labelOpenError, frame.getTitle(), ERROR_MESSAGE);
            }
        });
    }

    public void handleUnsupportedFormat(final String path) {
        invokeLater(new Runnable() {
            public void run() {
                log.severe("Unsupported format: " + path);
                showMessageDialog(frame,
                        MessageFormat.format(getBundle().getString("unsupported-format"), shortenPath(path, 60)),
                        frame.getTitle(), WARNING_MESSAGE);
            }
        });
    }

    public void handleFileNotFound(final String path) {
        invokeLater(new Runnable() {
            public void run() {
                log.severe("File not found: " + path);
                showMessageDialog(frame,
                        MessageFormat.format(getBundle().getString("file-not-found"), shortenPath(path, 60)),
                        frame.getTitle(), WARNING_MESSAGE);
            }
        });
    }

    // helpers for external components

    public void sendErrorReport(final String log, final String description, final File file) {
        getOperator().executeOperation(new RouteServiceOperator.Operation() {
            public String getName() {
                return "SendErrorReport";
            }

            public void run() throws IOException {
                routeFeedback.sendErrorReport(log, description, file);
            }
        });
    }

    public void addFilesToCatalog(List<File> files) {
        getBrowsePanel().addFilesToCatalog(files);
    }

    public void addUrlToCatalog(String string) {
        getBrowsePanel().addUrlToCatalog(string);
    }

    public void openPositionList(List<URL> urls) {
        getConvertPanel().openPositionList(urls);
    }

    public void revertPositions() {
        getPositionsModel().revert();
        getConvertPanel().clearSelection();
    }

    public void renameRoute(String name) {
        getConvertPanel().renameRoute(name);
    }

    public void setRouteCharacteristics(RouteCharacteristics characteristics) {
        getConvertPanel().getCharacteristicsModel().setSelectedItem(characteristics);
    }

    public void selectPositions(int[] selectedPositions, int centerPosition) {
        if (isMapViewAvailable()) {
            if (centerPosition >= 0 && centerPosition < getPositionsModel().getRowCount()) {
                NavigationPosition center = getPositionsModel().getPosition(centerPosition);
                mapView.setCenter(center);
            }
            mapView.setSelectedPositions(selectedPositions, true);
        }
        if (profileView != null)
            profileView.setSelectedPositions(selectedPositions, true);
    }

    public void insertAllWaypoints() {
        if (isMapViewAvailable()) {
            int[] selectedRows = getPositionsView().getSelectedRows();
            getConvertPanel().clearSelection();
            mapView.insertAllWaypoints(selectedRows);
        }
    }

    public void insertOnlyTurnpoints() {
        if (isMapViewAvailable()) {
            int[] selectedRows = getPositionsView().getSelectedRows();
            getConvertPanel().clearSelection();
            mapView.insertOnlyTurnpoints(selectedRows);
        }
    }

    private BatchPositionAugmenter batchPositionAugmenter = null;

    public synchronized BatchPositionAugmenter getBatchPositionAugmenter() {
        if (batchPositionAugmenter == null) {
            batchPositionAugmenter = new BatchPositionAugmenter(frame, completePositionService);
        }
        return batchPositionAugmenter;
    }

    private SinglePositionAugmenter positionAugmenter = null;

    private synchronized PositionAugmenter getPositionAugmenter() {
        if (positionAugmenter == null) {
            positionAugmenter = new SinglePositionAugmenter(getPositionsModel(), completePositionService);
        }
        return positionAugmenter;
    }

    public void complementElevation(int row, Double longitude, Double latitude) {
        getPositionAugmenter().complementElevation(row, longitude, latitude);
    }

    @SuppressWarnings({"UnusedDeclaration"})
    public void complementComment(int row, Double longitude, Double latitude) {
        getPositionAugmenter().complementComment(row, longitude, latitude);
    }

    public void complementTime(int row, CompactCalendar time) {
        getPositionAugmenter().complementTime(row, time, true);
    }

    public JTable getPositionsView() {
        return getConvertPanel().getPositionsView();
    }

    public int selectPositionsWithinDistanceToPredecessor(int distance) {
        return getConvertPanel().selectPositionsWithinDistanceToPredecessor(distance);
    }

    public int[] selectAllButEveryNthPosition(int order) {
        return getConvertPanel().selectAllButEveryNthPosition(order);
    }

    public int selectInsignificantPositions(int significance) {
        return getConvertPanel().selectInsignificantPositions(significance);
    }

    public void clearSelection() {
        getConvertPanel().clearSelection();
    }

    // map view related helpers

    public boolean isMapViewAvailable() {
        return mapView != null && mapView.isInitialized();
    }

    public NavigationPosition getMapCenter() {
        return mapView != null ? mapView.getCenter() : new Wgs84Position(-41.0, 41.0, null, null, null, null);
    }

    private void setProfileMode(ProfileMode profileMode) {
        preferences.put(PROFILE_MODE_PREFERENCE, profileMode.toString());
        profileView.setProfileMode(profileMode);
    }

    public void addMapViewListener(MapViewListener mapViewListener) {
        if (mapView != null)
            mapView.addMapViewListener(mapViewListener);
    }

    public void setTravelMode(TravelMode travelMode) {
        preferences.put(TRAVEL_MODE_PREFERENCE, travelMode.toString());
        if (mapView != null)
            mapView.setTravelMode(travelMode);
    }

    public void setRecenterAfterZooming(boolean recenterAfterZooming) {
        if (mapView != null)
            mapView.setRecenterAfterZooming(recenterAfterZooming);
    }

    public void setShowCoordinates(boolean showCoordinates) {
        if (mapView != null)
            mapView.setShowCoordinates(showCoordinates);
    }

    public void setShowWaypointDescription(boolean showWaypointDescription) {
        if (mapView != null)
            mapView.setShowWaypointDescription(showWaypointDescription);
    }

    public void setAvoidHighways(boolean avoidHighways) {
        if (mapView != null)
            mapView.setAvoidHighways(avoidHighways);
    }

    public void setAvoidTolls(boolean avoidTolls) {
        if (mapView != null)
            mapView.setAvoidTolls(avoidTolls);
    }

    // elevation view related helpers

    public PositionsModel getPositionsModel() {
        return getConvertPanel().getFormatAndRoutesModel().getPositionsModel();
    }

    public PositionsSelectionModel getPositionsSelectionModel() {
        return getConvertPanel().getPositionsSelectionModel();
    }

    private RecentUrlsModel getRecentUrlsModel() {
        return getConvertPanel().getRecentUrlsModel();
    }

    // tab related helpers

    public boolean isBrowsePanelSelected() {
        return tabbedPane.getSelectedComponent().equals(browsePanel);
    }

    public boolean isConvertPanelSelected() {
        return tabbedPane.getSelectedComponent().equals(convertPanel);
    }

    private BrowsePanel getBrowsePanel() {
        return tabInitializer.getBrowsePanel();
    }

    private ConvertPanel getConvertPanel() {
        return tabInitializer.getConvertPanel();
    }

    {
// GUI initializer generated by IntelliJ IDEA GUI Designer
// >>> IMPORTANT!! <<<
// DO NOT EDIT OR ADD ANY CODE HERE!
        $$$setupUI$$$();
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        contentPane = new JPanel();
        contentPane.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        profileSplitPane = new JSplitPane();
        profileSplitPane.setContinuousLayout(true);
        profileSplitPane.setDividerLocation(888);
        profileSplitPane.setDividerSize(10);
        profileSplitPane.setOneTouchExpandable(true);
        profileSplitPane.setOrientation(0);
        profileSplitPane.setResizeWeight(0.0);
        contentPane.add(profileSplitPane, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        mapSplitPane = new JSplitPane();
        mapSplitPane.setContinuousLayout(true);
        mapSplitPane.setDividerLocation(341);
        mapSplitPane.setDividerSize(10);
        mapSplitPane.setMinimumSize(new Dimension(-1, -1));
        mapSplitPane.setOneTouchExpandable(true);
        mapSplitPane.setOpaque(true);
        mapSplitPane.setResizeWeight(1.0);
        profileSplitPane.setLeftComponent(mapSplitPane);
        mapPanel = new JPanel();
        mapPanel.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        mapPanel.setMinimumSize(new Dimension(-1, -1));
        mapPanel.setPreferredSize(new Dimension(300, 560));
        mapSplitPane.setLeftComponent(mapPanel);
        tabbedPane = new JTabbedPane();
        tabbedPane.setTabPlacement(1);
        mapSplitPane.setRightComponent(tabbedPane);
        convertPanel = new JPanel();
        convertPanel.setLayout(new BorderLayout(0, 0));
        tabbedPane.addTab(ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("convert-tab"), convertPanel);
        browsePanel = new JPanel();
        browsePanel.setLayout(new BorderLayout(0, 0));
        tabbedPane.addTab(ResourceBundle.getBundle("slash/navigation/converter/gui/RouteConverter").getString("browse-tab"), browsePanel);
        elevationPanel = new JPanel();
        elevationPanel.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        elevationPanel.setMinimumSize(new Dimension(0, 0));
        elevationPanel.setPreferredSize(new Dimension(0, 0));
        elevationPanel.setVisible(false);
        profileSplitPane.setRightComponent(elevationPanel);
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return contentPane;
    }

    private class LazyTabInitializer implements ChangeListener {
        private Map<Component, Runnable> lazyInitializers = new HashMap<Component, Runnable>();
        private Map<Component, PanelInTab> initialized = new HashMap<Component, PanelInTab>();

        LazyTabInitializer() {
            lazyInitializers.put(convertPanel, new Runnable() {
                public void run() {
                    PanelInTab panel = new ConvertPanel();
                    convertPanel.add(panel.getRootComponent());
                    initialized.put(convertPanel, panel);
                }
            });
            lazyInitializers.put(browsePanel, new Runnable() {
                public void run() {
                    PanelInTab panel = new BrowsePanel();
                    browsePanel.add(panel.getRootComponent());
                    initialized.put(browsePanel, panel);
                }
            });
        }

        private synchronized BrowsePanel getBrowsePanel() {
            initialize(browsePanel);
            return (BrowsePanel) initialized.get(browsePanel);
        }

        private synchronized ConvertPanel getConvertPanel() {
            initialize(convertPanel);
            return (ConvertPanel) initialized.get(convertPanel);
        }

        private void initialize(Component selected) {
            Runnable runnable = lazyInitializers.get(selected);
            if (runnable != null) {
                lazyInitializers.remove(selected);

                startWaitCursor(frame.getRootPane());
                try {
                    runnable.run();
                } finally {
                    stopWaitCursor(frame.getRootPane());
                }
            }
        }

        public void stateChanged(ChangeEvent e) {
            Component selected = ((JTabbedPane) e.getSource()).getSelectedComponent();
            initialize(selected);

            final PanelInTab panel = isBrowsePanelSelected() ? getBrowsePanel() : getConvertPanel();
            frame.getRootPane().setDefaultButton(panel.getDefaultButton());
            invokeLater(new Runnable() {
                public void run() {
                    panel.getFocusComponent().grabFocus();
                    panel.getFocusComponent().requestFocus();
                }
            });
        }
    }

    private class MapSplitPaneListener implements PropertyChangeListener {
        private int location;

        private MapSplitPaneListener(int location) {
            this.location = location;
            enableActions();
        }

        public void propertyChange(PropertyChangeEvent e) {
            if (!isMapViewAvailable())
                return;

            if (e.getPropertyName().equals(DIVIDER_LOCATION_PROPERTY)) {
                if (mapSplitPane.getDividerLocation() != location) {
                    location = mapSplitPane.getDividerLocation();
                    mapView.resize();
                    preferences.putInt(MAP_DIVIDER_LOCATION_PREFERENCE, mapSplitPane.getDividerLocation());
                    log.fine("Changed map divider to " + mapSplitPane.getDividerLocation());
                    enableActions();
                }
            }
        }

        private void enableActions() {
            ActionManager actionManager = Application.getInstance().getContext().getActionManager();
            actionManager.enable("maximize-map", location < mapSplitPane.getMaximumDividerLocation() - 10);
            actionManager.enable("maximize-positionlist", location > mapSplitPane.getMinimumDividerLocation() + 10);
            actionManager.enable("show-map-and-positionlist", location == 1 || location > mapSplitPane.getMaximumDividerLocation() + tabbedPane.getMinimumSize().width - 1);
        }
    }

    private class ProfileSplitPaneListener implements PropertyChangeListener {
        private int location;

        private ProfileSplitPaneListener(int location) {
            this.location = location;
            enableActions();
        }

        public void propertyChange(PropertyChangeEvent e) {
            if (e.getPropertyName().equals(DIVIDER_LOCATION_PROPERTY)) {
                if (profileSplitPane.getDividerLocation() != location) {
                    location = profileSplitPane.getDividerLocation();
                    if (isMapViewAvailable()) {
                        // make sure the one touch expandable to minimize the map works fine
                        if (location == 1)
                            mapView.getComponent().setVisible(false);
                        else if ((Integer) e.getOldValue() == 1)
                            mapView.getComponent().setVisible(true);
                        mapView.resize();
                    }
                    preferences.putInt(PROFILE_DIVIDER_LOCATION_PREFERENCE, profileSplitPane.getDividerLocation());
                    log.fine("Changed profile divider to " + profileSplitPane.getDividerLocation());
                    enableActions();
                }
            }
        }

        private void enableActions() {
            ActionManager actionManager = Application.getInstance().getContext().getActionManager();
            actionManager.enable("maximize-map", location < frame.getHeight() - 10);
            actionManager.enable("maximize-positionlist", location < frame.getHeight() - 10);
            actionManager.enable("show-profile", location > frame.getHeight() - 80);
        }
    }

    private void initializeRouteConverterServices() {
        System.setProperty("rest", parseVersionFromManifest().getVersion());
        routeFeedback = new RouteFeedback(System.getProperty("feedback", "http://www.routeconverter.com/feedback/"), RouteConverter.getInstance().getCredentials());
        routeServiceOperator = new RouteServiceOperator(getFrame(), routeFeedback);
        updateChecker = new UpdateChecker(routeFeedback);
    }

    private void initializeActions() {
        final ActionManager actionManager = getInstance().getContext().getActionManager();
        actionManager.register("exit", new ExitAction());
        actionManager.register("print-map", new PrintMapAction(false));
        actionManager.register("print-map-and-route", new PrintMapAction(true));
        actionManager.register("print-elevation-profile", new PrintElevationProfileAction());
        actionManager.register("find-place", new FindPlaceAction());
        actionManager.register("show-map-and-positionlist", new ShowMapAndPositionListAction());
        actionManager.register("show-profile", new ShowProfileAction());
        actionManager.register("maximize-map", new MoveSplitPaneDividersAction(mapSplitPane, MAX_VALUE, profileSplitPane, MAX_VALUE));
        actionManager.register("maximize-positionlist", new MoveSplitPaneDividersAction(mapSplitPane, 0, profileSplitPane, MAX_VALUE));
        actionManager.register("show-elevation", new SetProfileModeAction(Elevation));
        actionManager.register("show-speed", new SetProfileModeAction(Speed));
        actionManager.register("insert-positions", new InsertPositionsAction());
        actionManager.register("delete-positions", new DeletePositionsAction());
        actionManager.register("revert-positions", new RevertPositionListAction());
        actionManager.register("convert-route-to-track", new ConvertRouteToTrackAction());
        actionManager.register("convert-track-to-route", new ConvertTrackToRouteAction());
        actionManager.register("options", new OptionsAction());
        actionManager.register("complete-flight-plan", new CompleteFlightPlanAction());
        actionManager.register("help-topics", new HelpTopicsAction());
        actionManager.register("check-for-update", new CheckForUpdateAction(updateChecker));
        actionManager.register("send-error-report", new SendErrorReportAction());
        actionManager.register("about", new AboutAction());
        JMenu mergeMenu = (JMenu) findMenuComponent(getContext().getMenuBar(), "positionlist", "merge-positionlist");
        new MergePositionListMenu(mergeMenu, getPositionsView(), getConvertPanel().getFormatAndRoutesModel());

        CSH.setHelpIDString(frame.getRootPane(), "top");
        CSH.setHelpIDString(convertPanel, "convert");
        CSH.setHelpIDString(browsePanel, "browse");
        CSH.setHelpIDString(mapPanel, "map");

        // delay JavaHelp initialization
        ActionListener actionListener = new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                actionManager.run("help-topics", event);
            }
        };
        frame.getRootPane().registerKeyboardAction(actionListener,
                KeyStroke.getKeyStroke(VK_HELP, 0), WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        frame.getRootPane().registerKeyboardAction(actionListener,
                KeyStroke.getKeyStroke(VK_F1, 0), WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        new ShowProfileMenu(getContext().getMenuBar(), getProfileModePreference());
        new UndoMenuSynchronizer(getInstance().getContext().getUndoManager(),
                findItem(getContext().getMenuBar(), "edit", "undo"),
                findItem(getContext().getMenuBar(), "edit", "redo"));
        new ReopenMenuSynchronizer(getConvertPanel(), getRecentUrlsModel(), findMenu(getContext().getMenuBar(), "file", "reopen"));
    }

    private class PrintMapAction extends FrameAction {
        private boolean withRoute;

        private PrintMapAction(boolean withRoute) {
            this.withRoute = withRoute;
        }

        public void run() {
            String title = getConvertPanel().getUrlModel().getShortUrl() + " / " + getConvertPanel().getFormatAndRoutesModel().getSelectedRoute().getName();
            mapView.print(title, withRoute);
        }
    }

    private class ShowMapAndPositionListAction extends FrameAction {
        public void run() {
            mapSplitPane.setDividerLocation(getConvertPanel().getRootComponent().getMinimumSize().width);
            profileSplitPane.setDividerLocation(preferences.getInt(PROFILE_DIVIDER_LOCATION_PREFERENCE, -1));
        }
    }

    private class ShowProfileAction extends FrameAction {
        public void run() {
            int location = preferences.getInt(PROFILE_DIVIDER_LOCATION_PREFERENCE, -1);
            if (location > frame.getHeight() - 200)
                location = frame.getHeight() - 200;
            profileSplitPane.setDividerLocation(location);
        }
    }

    private class SetProfileModeAction extends FrameAction {
        private final ProfileMode profileMode;

        private SetProfileModeAction(ProfileMode profileMode) {
            this.profileMode = profileMode;
        }

        public void run() {
            setProfileMode(profileMode);
        }
    }

    private class PrintElevationProfileAction extends FrameAction {
        public void run() {
            profileView.print();
        }
    }
}
