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
import slash.common.io.CompactCalendar;
import slash.common.io.Files;
import slash.common.io.Platform;
import slash.common.io.Version;
import slash.common.log.LoggingHelper;
import slash.navigation.babel.BabelException;
import slash.navigation.base.BaseNavigationPosition;
import slash.navigation.base.NavigationFormat;
import slash.navigation.base.RouteCharacteristics;
import slash.navigation.base.Wgs84Position;
import slash.navigation.converter.gui.actions.*;
import slash.navigation.converter.gui.augment.PositionAugmenter;
import slash.navigation.converter.gui.dnd.PanelDropHandler;
import slash.navigation.converter.gui.elevationview.ElevationView;
import slash.navigation.converter.gui.helper.*;
import slash.navigation.converter.gui.mapview.MapView;
import slash.navigation.converter.gui.mapview.MapViewListener;
import slash.navigation.converter.gui.mapview.TravelMode;
import slash.navigation.converter.gui.models.PositionsModel;
import slash.navigation.converter.gui.models.PositionsSelectionModel;
import slash.navigation.converter.gui.models.RecentUrlsModel;
import slash.navigation.converter.gui.models.UnitModel;
import slash.navigation.converter.gui.panels.BrowsePanel;
import slash.navigation.converter.gui.panels.ConvertPanel;
import slash.navigation.feedback.domain.RouteFeedback;
import slash.navigation.gpx.Gpx11Format;
import slash.navigation.gui.*;
import slash.navigation.rest.Credentials;
import slash.navigation.util.NumberPattern;

import javax.help.CSH;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.text.MessageFormat;
import java.util.*;
import java.util.List;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

import static java.lang.Integer.MAX_VALUE;
import static javax.swing.JOptionPane.ERROR_MESSAGE;
import static slash.common.io.Platform.*;
import static slash.common.io.Version.parseVersionFromManifest;
import static slash.navigation.converter.gui.helper.JMenuHelper.findMenuComponent;
import static slash.navigation.converter.gui.mapview.TravelMode.Driving;
import static slash.navigation.gui.Constants.startWaitCursor;
import static slash.navigation.gui.Constants.stopWaitCursor;
import static slash.navigation.util.NumberPattern.NUMBER_SPACE_THEN_DESCRIPTION;

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

    private static final String OPEN_PATH_PREFERENCE = "source";
    private static final String OPEN_FORMAT_PREFERENCE = "sourceFormat";
    private static final String SAVE_PATH_PREFERENCE = "target";
    private static final String TARGET_FORMAT_PREFERENCE = "targetFormat";
    private static final String ADD_POSITION_LONGITUDE_PREFERENCE = "addPositionLongitude";
    private static final String ADD_POSITION_LATITUDE_PREFERENCE = "addPositionLatitude";
    public static final String AUTOMATIC_UPDATE_CHECK_PREFERENCE = "automaticUpdateCheck";
    public static final String RECENTER_AFTER_ZOOMING_PREFERENCE = "recenterAfterZooming";
    public static final String TRAVEL_MODEL_PREFERENCE = "travelMode";
    public static final String AVOID_HIGHWAYS_PREFERENCE = "avoidHighways";
    public static final String AVOID_TOLLS_PREFERENCE = "avoidTolls";
    public static final String NUMBER_PATTERN_PREFERENCE = "numberPattern";
    public static final String TIME_ZONE_PREFERENCE = "timeZone";
    private static final String SELECT_BY_DISTANCE_PREFERENCE = "selectByDistance";
    private static final String SELECT_BY_ORDER_PREFERENCE = "selectByOrder";
    private static final String SELECT_BY_SIGNIFICANCE_PREFERENCE = "selectBySignificance";
    private static final String SEARCH_POSITION_PREFERENCE = "searchPosition";
    private static final String MAP_DIVIDER_LOCATION_PREFERENCE = "mapDividerLocation";
    private static final String ELEVATION_DIVIDER_LOCATION_PREFERENCE = "elevationDividerLocation";

    private static final String DEBUG_PREFERENCE = "debug";
    private static final String USERNAME_PREFERENCE = "userName";
    private static final String PASSWORD_PREFERENCE = "userAuthentication";
    private static final String CATEGORY_PREFERENCE = "category";
    private static final String UPLOAD_ROUTE_PREFERENCE = "uploadRoute";

    private RouteFeedback routeFeedback;
    private RouteServiceOperator routeServiceOperator;
    private UpdateChecker updateChecker;
    private UnitModel unitModel = new UnitModel();

    protected JPanel contentPane;
    private JSplitPane mapSplitPane, elevationSplitPane;
    private JTabbedPane tabbedPane;
    private JPanel convertPanel, browsePanel, mapPanel, elevationPanel;
    private MapView mapView;
    private ElevationView elevationView;
    private static final GridConstraints MAP_PANEL_CONSTRAINTS = new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
            new Dimension(0, 0), new Dimension(0, 0), new Dimension(2000, 2640), 0, true);
    private static final GridConstraints ELEVATION_PANEL_CONSTRAINTS = new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
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
        checkJreVersion();
        updateChecker.implicitCheck(getFrame());
        parseArgs(args);
    }

    private void checkJreVersion() {
        String currentVersion = System.getProperty("java.version");
        String minimumVersion = "1.6.0_14";
        if (!Platform.isCurrentAtLeastMinimumVersion(currentVersion, minimumVersion)) {
            JLabel label = new JLabel(MessageFormat.format(getBundle().getString("jre-too-old-warning"), currentVersion, minimumVersion));
            label.addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent me) {
                    new ExternalPrograms().startBrowserForJava(frame);
                }
            });
            JOptionPane.showMessageDialog(frame, label, frame.getTitle(), JOptionPane.WARNING_MESSAGE);
        }
    }

    private void parseArgs(String[] args) {
        log.info("Processing arguments: " + Arrays.toString(args));
        if (args.length > 0) {
            List<URL> urls = Files.toUrls(args);
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

        mapView = createMapView("slash.navigation.converter.gui.mapview.EclipseSWTMapView");
        if (mapView != null && mapView.isSupportedPlatform()) {
            mapPanel.setVisible(true);
            openMapView();
        } else {
            mapPanel.setVisible(false);
        }
        openElevationView();

        initializeRouteConverterServices();
        initializeActions();
    }

    private void openFrame() {
        new Thread(new Runnable() {
            public void run() {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        openFrame(contentPane);
                    }
                });
            }
        }, "FrameOpener").start();
    }

    private MapView createMapView(String className) {
        try {
            Class<?> clazz = Class.forName(className);
            return (MapView) clazz.newInstance();
        } catch (Exception e) {
            log.info("Cannot instantiate " + className + ": " + e.getMessage());
        }
        return null;
    }

    private void openMapView() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                mapView.initialize(getPositionsModel(),
                        getPositionsSelectionModel(),
                        getConvertPanel().getCharacteristicsModel(),
                        getPositionAugmenter(),
                        preferences.getBoolean(RECENTER_AFTER_ZOOMING_PREFERENCE, false),
                        getTravelModePreference(),
                        preferences.getBoolean(AVOID_HIGHWAYS_PREFERENCE, true),
                        preferences.getBoolean(AVOID_TOLLS_PREFERENCE, true));

                @SuppressWarnings({"ThrowableResultOfMethodCallIgnored"})
                Throwable cause = mapView.getInitializationCause();
                boolean enablePrintActions = false;
                if (mapView.getComponent() == null || cause != null) {
                    StringWriter stackTrace = new StringWriter();
                    cause.printStackTrace(new PrintWriter(stackTrace));
                    mapPanel.add(new JLabel(MessageFormat.format(getBundle().getString("start-browser-error"), stackTrace.toString().replaceAll("\n", "<p>"))), MAP_PANEL_CONSTRAINTS);
                } else {
                    mapPanel.add(mapView.getComponent(), MAP_PANEL_CONSTRAINTS);
                    enablePrintActions = true;
                }

                int location = preferences.getInt(MAP_DIVIDER_LOCATION_PREFERENCE, -1);
                if (location < 1)
                    location = 300;
                mapSplitPane.setDividerLocation(location);
                log.fine("Initialized map divider to " + location);
                mapSplitPane.addPropertyChangeListener(new MapSplitPaneListener(location));

                ActionManager actionManager = Application.getInstance().getContext().getActionManager();
                actionManager.enable("print-map", enablePrintActions);
                actionManager.enable("print-map-and-route", enablePrintActions);
                actionManager.enable("print-elevation-profile", enablePrintActions);
            }
        });
    }

    private void openElevationView() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                elevationView = new ElevationView();
                elevationView.initialize(getPositionsModel(), getPositionsSelectionModel());
                elevationView.setUnit(getUnitModel().getCurrent());
                elevationPanel.add(elevationView.getComponent(), ELEVATION_PANEL_CONSTRAINTS);
                elevationPanel.setTransferHandler(new PanelDropHandler());
                elevationPanel.setVisible(true);

                int location = preferences.getInt(ELEVATION_DIVIDER_LOCATION_PREFERENCE, -1);
                if (location < 2)
                    location = 888;
                elevationSplitPane.setDividerLocation(location);
                log.fine("Initialized elevation divider to " + location);
                elevationSplitPane.addPropertyChangeListener(new ElevationSplitPaneListener(location));

                getUnitModel().addChangeListener(new ChangeListener() {
                    public void stateChanged(ChangeEvent e) {
                        elevationView.setUnit(getUnitModel().getCurrent());
                    }
                });
            }
        });
    }

    protected void shutdown() {
        if (mapView != null)
            mapView.dispose();
        getConvertPanel().dispose();
        if (positionAugmenter != null)
            positionAugmenter.close();
        super.shutdown();

        log.info("Shutdown " + getTitle() + " for " + getRouteConverter() + " with locale " + Locale.getDefault() +
                " on " + getJava() + " and " + getPlatform() + " with " + getMaximumMemory() + " MByte heap");
    }

    // Preferences handling

    public Preferences getPreferences() {
        return preferences;
    }

    public String getOpenFormatPreference() {
        return preferences.get(OPEN_FORMAT_PREFERENCE, Gpx11Format.class.getName());
    }

    public void setOpenFormatPreference(String format) {
        preferences.put(OPEN_FORMAT_PREFERENCE, format);
    }

    public String getOpenPathPreference() {
        return preferences.get(OPEN_PATH_PREFERENCE, "");
    }

    public void setOpenPathPreference(String file) {
        preferences.put(OPEN_PATH_PREFERENCE, file);
    }

    public String getSaveFormatPreference() {
        return preferences.get(TARGET_FORMAT_PREFERENCE, Gpx11Format.class.getName());
    }

    public void setSaveFormatPreference(String format) {
        preferences.put(TARGET_FORMAT_PREFERENCE, format);
    }

    public String getSavePathPreference(NavigationFormat format) {
        return preferences.get(SAVE_PATH_PREFERENCE + format.getName(), "");
    }

    public void setSavePathPreference(NavigationFormat format, String parent) {
        if (parent != null)
            preferences.put(SAVE_PATH_PREFERENCE + format.getName(), parent);
    }

    private BaseNavigationPosition getLastMapCenter() {
        double longitude = preferences.getDouble(ADD_POSITION_LONGITUDE_PREFERENCE, -41.0);
        double latitude = preferences.getDouble(ADD_POSITION_LATITUDE_PREFERENCE, 41.0);
        return new Wgs84Position(longitude, latitude, null, null, null, null);
    }

    public void setLastMapCenter(Double longitude, Double latitude) {
        preferences.putDouble(ADD_POSITION_LONGITUDE_PREFERENCE, longitude);
        preferences.putDouble(ADD_POSITION_LATITUDE_PREFERENCE, latitude);
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
        return TravelMode.fromString(preferences.get(TRAVEL_MODEL_PREFERENCE, Driving.toString()));
    }

    public NumberPattern getNumberPatternPreference() {
        return NumberPattern.valueOf(preferences.get(NUMBER_PATTERN_PREFERENCE, NUMBER_SPACE_THEN_DESCRIPTION.toString()));
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

    public UnitModel getUnitModel() {
        return unitModel;
    }

    // dialogs for external components

    public void handleBabelError(final BabelException e) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                JOptionPane.showMessageDialog(frame,
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
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                JOptionPane.showMessageDialog(frame,
                        MessageFormat.format(getBundle().getString("out-of-memory-error"), limitBefore, limitAfter),
                        frame.getTitle(), ERROR_MESSAGE);
            }
        });
    }

    public void handleOpenError(final Throwable throwable, final String path) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                throwable.printStackTrace();
                log.severe("Open error: " + throwable.getMessage());
                JLabel labelOpenError = new JLabel(MessageFormat.format(getBundle().getString("open-error"), Files.shortenPath(path, 60), throwable.getMessage()));
                labelOpenError.addMouseListener(new MouseAdapter() {
                    public void mouseClicked(MouseEvent me) {
                        new ExternalPrograms().startMail(frame);
                    }
                });
                JOptionPane.showMessageDialog(frame, labelOpenError, frame.getTitle(), ERROR_MESSAGE);
            }
        });
    }

    public void handleOpenError(final Throwable throwable, final List<URL> urls) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                throwable.printStackTrace();
                log.severe("Open error: " + throwable.getMessage());
                JLabel labelOpenError = new JLabel(MessageFormat.format(getBundle().getString("open-error"), Files.printArrayToDialogString(urls.toArray(new URL[urls.size()])), throwable.getMessage()));
                labelOpenError.addMouseListener(new MouseAdapter() {
                    public void mouseClicked(MouseEvent me) {
                        new ExternalPrograms().startMail(frame);
                    }
                });
                JOptionPane.showMessageDialog(frame, labelOpenError, frame.getTitle(), ERROR_MESSAGE);
            }
        });
    }

    public void handleUnsupportedFormat(final String path) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                log.severe("Unsupported format: " + path);
                JOptionPane.showMessageDialog(frame,
                        MessageFormat.format(getBundle().getString("unsupported-format"), Files.shortenPath(path, 60)),
                        frame.getTitle(), JOptionPane.WARNING_MESSAGE);
            }
        });
    }

    public void handleFileNotFound(final String path) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                log.severe("File not found: " + path);
                JOptionPane.showMessageDialog(frame,
                        MessageFormat.format(getBundle().getString("file-not-found"), Files.shortenPath(path, 60)),
                        frame.getTitle(), JOptionPane.WARNING_MESSAGE);
            }
        });
    }

    // helpers for external components

    public void sendErrorReport(final String log, final String description, final File file) {
        getOperator().executeOnRouteService(new RouteServiceOperator.Operation() {
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

    public void selectPositions(int[] selectedPositions) {
        if (isMapViewAvailable())
            mapView.setSelectedPositions(selectedPositions, true);
        if (elevationView != null)
            elevationView.setSelectedPositions(selectedPositions, true);
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

    private SinglePositionAugmenter positionAugmenter = null;

    private synchronized PositionAugmenter getPositionAugmenter() {
        if (positionAugmenter == null) {
            positionAugmenter = new SinglePositionAugmenter(getPositionsModel());
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

    public void complementTime(final int row, final CompactCalendar time) {
        getPositionAugmenter().complementTime(row, time);
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

    public BaseNavigationPosition getMapCenter() {
        return isMapViewAvailable() ? mapView.getCenter() : getLastMapCenter();
    }

    public void addMapViewListener(MapViewListener mapViewListener) {
        if (isMapViewAvailable())
            mapView.addMapViewListener(mapViewListener);
    }

    public void setTravelMode(TravelMode travelMode) {
        preferences.put(TRAVEL_MODEL_PREFERENCE, travelMode.toString());
        if (mapView != null)
            mapView.setTravelMode(travelMode);
    }

    public void setAvoidHighways(boolean avoidHighways) {
        if (mapView != null)
            mapView.setAvoidHighways(avoidHighways);
    }

    public void setAvoidTolls(boolean avoidTolls) {
        if (mapView != null)
            mapView.setAvoidTolls(avoidTolls);
    }

    public void setRecenterAfterZooming(boolean recenterAfterZooming) {
        if (mapView != null)
            mapView.setRecenterAfterZooming(recenterAfterZooming);
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
        elevationSplitPane = new JSplitPane();
        elevationSplitPane.setContinuousLayout(true);
        elevationSplitPane.setDividerLocation(888);
        elevationSplitPane.setDividerSize(10);
        elevationSplitPane.setOneTouchExpandable(true);
        elevationSplitPane.setOrientation(0);
        elevationSplitPane.setResizeWeight(0.0);
        contentPane.add(elevationSplitPane, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        mapSplitPane = new JSplitPane();
        mapSplitPane.setContinuousLayout(true);
        mapSplitPane.setDividerLocation(341);
        mapSplitPane.setDividerSize(10);
        mapSplitPane.setMinimumSize(new Dimension(-1, -1));
        mapSplitPane.setOneTouchExpandable(true);
        mapSplitPane.setOpaque(true);
        mapSplitPane.setResizeWeight(1.0);
        elevationSplitPane.setLeftComponent(mapSplitPane);
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
        elevationSplitPane.setRightComponent(elevationPanel);
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return contentPane;
    }

    private class LazyTabInitializer implements ChangeListener {
        private Map<Component, Runnable> lazyInitializers = new HashMap<Component, Runnable>();
        private Map<Component, Object> initialized = new HashMap<Component, Object>();

        LazyTabInitializer() {
            lazyInitializers.put(convertPanel, new Runnable() {
                public void run() {
                    ConvertPanel panel = new ConvertPanel();
                    convertPanel.add(panel.getRootComponent());
                    initialized.put(convertPanel, panel);
                }
            });
            lazyInitializers.put(browsePanel, new Runnable() {
                public void run() {
                    BrowsePanel panel = new BrowsePanel();
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

            if (isBrowsePanelSelected())
                frame.getRootPane().setDefaultButton(getBrowsePanel().getDefaultButton());
            else
                frame.getRootPane().setDefaultButton(getConvertPanel().getDefaultButton());
        }
    }

    private class MapSplitPaneListener implements PropertyChangeListener {
        private int location;

        private MapSplitPaneListener(int location) {
            this.location = location;
        }

        public void propertyChange(PropertyChangeEvent e) {
            if (!isMapViewAvailable())
                return;

            if (e.getPropertyName().equals(JSplitPane.DIVIDER_LOCATION_PROPERTY)) {
                if (mapSplitPane.getDividerLocation() != location) {
                    location = mapSplitPane.getDividerLocation();
                    mapView.resize();
                    preferences.putInt(MAP_DIVIDER_LOCATION_PREFERENCE, mapSplitPane.getDividerLocation());
                    log.fine("Changed map divider to " + mapSplitPane.getDividerLocation());

                    ActionManager actionManager = Application.getInstance().getContext().getActionManager();
                    actionManager.enable("maximize-map", location < mapSplitPane.getMaximumDividerLocation() - 10);
                    actionManager.enable("maximize-positionlist", location > mapSplitPane.getMinimumDividerLocation() + 10);
                    actionManager.enable("show-map-and-positionlist", location == 1 || location > mapSplitPane.getMaximumDividerLocation() + tabbedPane.getMinimumSize().width - 1);
                }
            }
        }
    }

    private class ElevationSplitPaneListener implements PropertyChangeListener {
        private int location;

        private ElevationSplitPaneListener(int location) {
            this.location = location;
        }

        public void propertyChange(PropertyChangeEvent e) {
            if (e.getPropertyName().equals(JSplitPane.DIVIDER_LOCATION_PROPERTY)) {
                if (elevationSplitPane.getDividerLocation() != location) {
                    location = elevationSplitPane.getDividerLocation();
                    if (isMapViewAvailable()) {
                        // make sure the one touch expandable to minimize the map works fine
                        if (location == 1)
                            mapView.getComponent().setVisible(false);
                        else if ((Integer) e.getOldValue() == 1)
                            mapView.getComponent().setVisible(true);
                        mapView.resize();
                    }
                    preferences.putInt(ELEVATION_DIVIDER_LOCATION_PREFERENCE, elevationSplitPane.getDividerLocation());
                    log.fine("Changed elevation divider to " + elevationSplitPane.getDividerLocation());

                    ActionManager actionManager = Application.getInstance().getContext().getActionManager();
                    actionManager.enable("maximize-map", location < frame.getHeight() - 10);
                    actionManager.enable("maximize-positionlist", location < frame.getHeight() - 10);
                    actionManager.enable("show-elevation-profile", location > frame.getHeight() - 80);
                }
            }
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
        actionManager.register("show-elevation-profile", new ShowElevationProfileAction());
        actionManager.register("maximize-map", new MoveSplitPaneDividersAction(mapSplitPane, MAX_VALUE, elevationSplitPane, MAX_VALUE));
        actionManager.register("maximize-positionlist", new MoveSplitPaneDividersAction(mapSplitPane, 0, elevationSplitPane, MAX_VALUE));
        actionManager.register("insert-positions", new InsertPositionsAction());
        actionManager.register("delete-positions", new DeletePositionsAction());
        actionManager.register("revert-positions", new RevertPositionListAction());
        actionManager.register("convert-route-to-track", new ConvertRouteToTrackAction());
        actionManager.register("convert-track-to-route", new ConvertTrackToRouteAction());
        actionManager.register("options", new OptionsAction());
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
                KeyStroke.getKeyStroke(KeyEvent.VK_HELP, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        frame.getRootPane().registerKeyboardAction(actionListener,
                KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        new UndoMenuSynchronizer(getInstance().getContext().getUndoManager(),
                JMenuHelper.findItem(frame.getJMenuBar(), "edit", "undo"),
                JMenuHelper.findItem(frame.getJMenuBar(), "edit", "redo"));
        new ReopenMenuSynchronizer(getConvertPanel(), getRecentUrlsModel(), JMenuHelper.findMenu(frame.getJMenuBar(), "file", "reopen"));
    }

    private class PrintMapAction extends FrameAction {
        private boolean withRoute;

        private PrintMapAction(boolean withRoute) {
            this.withRoute = withRoute;
        }

        public void run() {
            mapView.print(withRoute);
        }
    }

    private class ShowMapAndPositionListAction extends FrameAction {
        public void run() {
            mapSplitPane.setDividerLocation(getConvertPanel().getRootComponent().getMinimumSize().width);
            elevationSplitPane.setDividerLocation(preferences.getInt(ELEVATION_DIVIDER_LOCATION_PREFERENCE, -1));
        }
    }

    private class ShowElevationProfileAction extends FrameAction {
        public void run() {
            int location = preferences.getInt(ELEVATION_DIVIDER_LOCATION_PREFERENCE, -1);
            if (location > frame.getHeight() - 200)
                location = frame.getHeight() - 200;
            elevationSplitPane.setDividerLocation(location);
        }
    }

    private class PrintElevationProfileAction extends FrameAction {
        public void run() {
            elevationView.print();
        }
    }
}
