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
import slash.common.log.LoggingHelper;
import slash.common.system.Version;
import slash.navigation.babel.BabelException;
import slash.navigation.base.RouteCharacteristics;
import slash.navigation.common.*;
import slash.navigation.converter.gui.actions.*;
import slash.navigation.converter.gui.dnd.PanelDropHandler;
import slash.navigation.converter.gui.helpers.*;
import slash.navigation.converter.gui.mapview.MapView;
import slash.navigation.converter.gui.mapview.MapViewCallback;
import slash.navigation.converter.gui.models.*;
import slash.navigation.converter.gui.panels.BrowsePanel;
import slash.navigation.converter.gui.panels.ConvertPanel;
import slash.navigation.converter.gui.panels.PanelInTab;
import slash.navigation.converter.gui.profileview.ProfileModeMenu;
import slash.navigation.converter.gui.profileview.ProfileView;
import slash.navigation.datasources.DataSource;
import slash.navigation.datasources.DataSourceManager;
import slash.navigation.download.Download;
import slash.navigation.download.DownloadManager;
import slash.navigation.download.FileAndChecksum;
import slash.navigation.earthtools.EarthToolsService;
import slash.navigation.feedback.domain.RouteFeedback;
import slash.navigation.geonames.GeoNamesService;
import slash.navigation.googlemaps.GoogleMapsService;
import slash.navigation.gui.Application;
import slash.navigation.gui.SingleFrameApplication;
import slash.navigation.gui.actions.ActionManager;
import slash.navigation.gui.actions.ExitAction;
import slash.navigation.gui.actions.FrameAction;
import slash.navigation.gui.actions.HelpTopicsAction;
import slash.navigation.hgt.HgtFiles;
import slash.navigation.hgt.HgtFilesService;
import slash.navigation.rest.Credentials;
import slash.navigation.routing.RoutingService;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.xml.bind.UnmarshalException;
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
import java.util.*;
import java.util.List;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

import static com.intellij.uiDesigner.core.GridConstraints.*;
import static java.awt.event.KeyEvent.VK_F1;
import static java.awt.event.KeyEvent.VK_HELP;
import static java.lang.Integer.MAX_VALUE;
import static java.util.Arrays.asList;
import static java.util.Locale.*;
import static javax.help.CSH.setHelpIDString;
import static javax.swing.JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT;
import static javax.swing.JOptionPane.*;
import static javax.swing.JSplitPane.DIVIDER_LOCATION_PROPERTY;
import static javax.swing.KeyStroke.getKeyStroke;
import static javax.swing.SwingUtilities.invokeLater;
import static slash.common.helpers.ExceptionHelper.getLocalizedMessage;
import static slash.common.io.Directories.getApplicationDirectory;
import static slash.common.io.Files.*;
import static slash.common.system.Platform.*;
import static slash.common.system.Version.parseVersionFromManifest;
import static slash.feature.client.Feature.initializePreferences;
import static slash.navigation.common.NumberPattern.Number_Space_Then_Description;
import static slash.navigation.common.NumberingStrategy.Absolute_Position_Within_Position_List;
import static slash.navigation.converter.gui.helpers.ExternalPrograms.startMail;
import static slash.navigation.gui.helpers.JMenuHelper.findMenuComponent;
import static slash.navigation.gui.helpers.UIHelper.*;

/**
 * A small graphical user interface for the route conversion.
 *
 * @author Christian Pesch
 */

public class RouteConverter extends SingleFrameApplication {
    protected static final Logger log = Logger.getLogger(RouteConverter.class.getName());
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

    public static Preferences getPreferences() {
        return preferences;
    }

    public static String getTitle() {
        Version version = parseVersionFromManifest();
        return MessageFormat.format(getBundle().getString("title"), RouteConverter.getInstance().getEdition(), version.getVersion(), version.getDate());
    }

    public String getEdition() {
        return "Online";
    }

    public static final String AUTOMATIC_UPDATE_CHECK_PREFERENCE = "automaticUpdateCheck";
    public static final String SHOW_ALL_POSITIONS_AFTER_LOADING_PREFERENCE = "showAllPositionsAfterLoading";
    public static final String RECENTER_AFTER_ZOOMING_PREFERENCE = "recenterAfterZooming";
    public static final String SHOW_COORDINATES_PREFERENCE = "showCoordinates";
    public static final String SHOW_WAYPOINT_DESCRIPTION_PREFERENCE = "showWaypointDescription";
    public static final String NUMBER_PATTERN_PREFERENCE = "numberPattern";
    public static final String NUMBERING_STRATEGY_PREFERENCE = "numberingStrategy";
    public static final String TIME_ZONE_PREFERENCE = "timeZone";
    private static final String SELECT_BY_DISTANCE_PREFERENCE = "selectByDistance";
    private static final String SELECT_BY_ORDER_PREFERENCE = "selectByOrder";
    private static final String SELECT_BY_SIGNIFICANCE_PREFERENCE = "selectBySignificance";
    private static final String SEARCH_POSITION_PREFERENCE = "searchPosition";
    private static final String MAP_DIVIDER_LOCATION_PREFERENCE = "mapDividerLocation";
    private static final String PROFILE_DIVIDER_LOCATION_PREFERENCE = "profileDividerLocation";

    private static final String DEBUG_PREFERENCE = "debug";
    private static final String USERNAME_PREFERENCE = "userName";
    private static final String PASSWORD_PREFERENCE = "userAuthentication";
    private static final String CATEGORY_PREFERENCE = "category";
    private static final String UPLOAD_ROUTE_PREFERENCE = "uploadRoute";
    private static final String SHOWED_MISSING_TRANSLATOR_PREFERENCE = "showedMissingTranslator";

    private RouteServiceOperator routeServiceOperator;
    private UpdateChecker updateChecker;
    private DataSourceManager dataSourceManager;
    private HgtFilesService hgtFilesService;
    private ElevationServiceFacade elevationServiceFacade = new ElevationServiceFacade();
    private RoutingServiceFacade routingServiceFacade = new RoutingServiceFacade();
    private InsertPositionFacade insertPositionFacade = new InsertPositionFacade();
    private UnitSystemModel unitSystemModel = new UnitSystemModel();
    private ProfileModeModel profileModeModel = new ProfileModeModel();

    protected JPanel contentPane;
    private JSplitPane mapSplitPane, profileSplitPane;
    private JTabbedPane tabbedPane;
    private JPanel convertPanel, browsePanel, mapPanel, profilePanel;
    private MapView mapView;
    private ProfileView profileView;
    private static final GridConstraints MAP_PANEL_CONSTRAINTS = new GridConstraints(0, 0, 1, 1, ANCHOR_CENTER, FILL_BOTH,
            SIZEPOLICY_CAN_SHRINK | SIZEPOLICY_CAN_GROW, SIZEPOLICY_CAN_SHRINK | SIZEPOLICY_CAN_GROW,
            new Dimension(0, 0), new Dimension(0, 0), new Dimension(MAX_VALUE, MAX_VALUE), 0, true);
    private static final GridConstraints PROFILE_PANEL_CONSTRAINTS = new GridConstraints(0, 0, 1, 1, ANCHOR_CENTER, FILL_BOTH,
            SIZEPOLICY_CAN_SHRINK | SIZEPOLICY_CAN_GROW, SIZEPOLICY_CAN_SHRINK | SIZEPOLICY_CAN_GROW,
            new Dimension(0, 0), new Dimension(0, 0), new Dimension(MAX_VALUE, 300), 0, true);

    private LazyTabInitializer tabInitializer;

    // application lifecycle callbacks

    protected void startup() {
        initializeLogging();
        show();
        checkForMissingTranslator();
        updateChecker.implicitCheck(getFrame());
    }

    protected void parseInitialArgs(String[] args) {
        log.info("Processing initial arguments: " + Arrays.toString(args));
        if (args.length > 0) {
            List<URL> urls = toUrls(args);
            log.info("Processing urls: " + urls);
            getConvertPanel().openUrls(urls);
        } else {
            getConvertPanel().newFile();
        }
    }

    protected void parseNewActivationArgs(final String[] args) {
        log.info("Processing new activation arguments: " + Arrays.toString(args));
        if (args.length > 0) {
            invokeLater(new Runnable() {
                public void run() {
                    List<URL> urls = toUrls(args);
                    log.info("Processing urls: " + urls);
                    getConvertPanel().openUrls(urls);

                    frame.setVisible(true);
                    frame.toFront();
                }
            });
        }
    }

    // helper

    private void initializeLogging() {
        LoggingHelper loggingHelper = LoggingHelper.getInstance();
        loggingHelper.logToFile();
        if (preferences.getBoolean(DEBUG_PREFERENCE, false)) {
            loggingHelper.logToConsole();
        }
        log.info("Started " + getTitle() + " for " + parseVersionFromManifest().getOperationSystem() + " with locale " + Locale.getDefault() +
                " on " + getJava() + " and " + getPlatform() + " with " + getMaximumMemory() + " MByte heap");
    }

    private List<String> getLanguagesWithActiveTranslators() {
        List<Locale> localesOfActiveTranslators = asList(CHINA, CROATIA, CZECH, FRANCE, GERMANY, ITALY, NEDERLANDS,
                POLAND, PORTUGAL, RUSSIA, SERBIA, SLOVAKIA, SPAIN, US);
        List<String> results = new ArrayList<>();
        for (Locale locale : localesOfActiveTranslators) {
            results.add(locale.getLanguage());
        }
        return results;
    }

    private void checkForMissingTranslator() {
        List<String> activeLanguages = getLanguagesWithActiveTranslators();
        String language = Locale.getDefault().getLanguage();
        if (!activeLanguages.contains(language) && !preferences.getBoolean(SHOWED_MISSING_TRANSLATOR_PREFERENCE, false)) {
            JLabel labelTranslatorMissing = new JLabel(MessageFormat.format(getBundle().getString("translator-missing"), language));
            labelTranslatorMissing.addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent me) {
                    startMail(frame);
                }
            });
            showMessageDialog(frame, labelTranslatorMissing, frame.getTitle(), QUESTION_MESSAGE);
            preferences.putBoolean(SHOWED_MISSING_TRANSLATOR_PREFERENCE, true);
        }
    }

    private void show() {
        patchUIManager(getBundle(),
                "OptionPane.yesButtonText", "OptionPane.noButtonText", "OptionPane.cancelButtonText",
                "FileChooser.openButtonText", "FileChooser.saveButtonText", "FileChooser.cancelButtonText",
                "FileChooser.acceptAllFileFilterText");
        initializePreferences(preferences);

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

        initializeServices();
        initializeActions();
        initializeDatasources();

        openMapView();
        openProfileView();

        getContext().getActionManager().logUsage();
    }

    private MapView createMapView(String className) {
        try {
            log.info("Before creating map view from " + className);
            Class<?> aClass = Class.forName(className);
            log.info("After creating class " + aClass);
            MapView mapView = (MapView) aClass.newInstance();
            log.info("After creating map view " + mapView);
            return mapView;
        } catch (Throwable t) {
            log.info("Cannot create " + className + ": " + t);
            return null;
        }
    }

    private void openFrame() {
        createFrame(getTitle(), "slash/navigation/converter/gui/RouteConverter.png", contentPane, null, new FrameMenu().createMenuBar());

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
        log.info("JavaFX: " + isJavaFX() + " java.vendor: " + System.getProperty("java.vendor") +
                " is Oracle: " + System.getProperty("java.vendor").contains("Oracle") +
                " java.version" + System.getProperty("java.version") +
                " later than 1.7.0_40: " + (System.getProperty("java.version").compareTo("1.7.0_40") >= 0));
        mapView = createMapView("slash.navigation.converter.gui.mapview.JavaFXWebViewMapView");
        log.info("map view 1 is " + mapView);
        if (getMapView() == null)
            mapView = createMapView("slash.navigation.converter.gui.mapview.EclipseSWTMapView");
        log.info("map view 2 is " + mapView);
        if (getMapView() == null)
            mapView = createMapView("slash.navigation.converter.gui.mapview.MapsforgeMapView");
        log.info("map view 3 is " + mapView);
        if (getMapView() == null)
            return;
        log.info("Using map view " + getMapView());

        invokeLater(new Runnable() {
            public void run() {
                getMapView().initialize(getPositionsModel(),
                        getPositionsSelectionModel(),
                        getConvertPanel().getCharacteristicsModel(),
                        getMapViewCallback(),
                        preferences.getBoolean(SHOW_ALL_POSITIONS_AFTER_LOADING_PREFERENCE, true),
                        preferences.getBoolean(RECENTER_AFTER_ZOOMING_PREFERENCE, false),
                        preferences.getBoolean(SHOW_COORDINATES_PREFERENCE, false),
                        preferences.getBoolean(SHOW_WAYPOINT_DESCRIPTION_PREFERENCE, false),
                        getUnitSystemModel());

                @SuppressWarnings({"ThrowableResultOfMethodCallIgnored"})
                Throwable cause = getMapView().getInitializationCause();
                if (getMapView().getComponent() == null || cause != null) {
                    StringWriter stackTrace = new StringWriter();
                    cause.printStackTrace(new PrintWriter(stackTrace));
                    mapPanel.add(new JLabel(MessageFormat.format(getBundle().getString("initialize-map-error"),
                            stackTrace.toString().replaceAll("\n", "<p>"))), MAP_PANEL_CONSTRAINTS);
                } else {
                    mapPanel.add(getMapView().getComponent(), MAP_PANEL_CONSTRAINTS);
                }
                mapPanel.setVisible(true);

                int location = preferences.getInt(MAP_DIVIDER_LOCATION_PREFERENCE, -1);
                if (location < 1)
                    location = 300;
                mapSplitPane.setDividerLocation(location);
                log.info("Initialized map divider to " + location);
                mapSplitPane.addPropertyChangeListener(new MapSplitPaneListener(location));

                getConvertPanel().initializeMapView(getMapView());
            }
        });
    }

    protected MapView getMapView() {
        return mapView;
    }

    private void openProfileView() {
        invokeLater(new Runnable() {
            public void run() {
                profileView = new ProfileView();
                profileView.initialize(getPositionsModel(),
                        getPositionsSelectionModel(),
                        getUnitSystemModel(),
                        getProfileModeModel());
                profilePanel.add(profileView.getComponent(), PROFILE_PANEL_CONSTRAINTS);
                profilePanel.setTransferHandler(new PanelDropHandler());
                profilePanel.setVisible(true);

                int location = preferences.getInt(PROFILE_DIVIDER_LOCATION_PREFERENCE, -1);
                if (location < 2)
                    location = 888;
                profileSplitPane.setDividerLocation(location);
                log.info("Initialized profile divider to " + location);
                profileSplitPane.addPropertyChangeListener(new ProfileSplitPaneListener(location));
            }
        });
    }

    protected void shutdown() {
        if (isMapViewAvailable())
            getMapView().dispose();
        getConvertPanel().dispose();
        getHgtFilesService().dispose();
        getBatchPositionAugmenter().dispose();
        getDataSourceManager().dispose();
        getDownloadManager().saveQueue();
        super.shutdown();

        log.info("Shutdown " + getTitle() + " for " + parseVersionFromManifest().getOperationSystem() + " with locale " + Locale.getDefault() +
                " on " + getJava() + " and " + getPlatform() + " with " + getMaximumMemory() + " MByte heap");
    }

    public double getSelectByDistancePreference() {
        return preferences.getDouble(SELECT_BY_DISTANCE_PREFERENCE, 1000);
    }

    public void setSelectByDistancePreference(double selectByDistancePreference) {
        preferences.putDouble(SELECT_BY_DISTANCE_PREFERENCE, selectByDistancePreference);
    }

    public int getSelectByOrderPreference() {
        return preferences.getInt(SELECT_BY_ORDER_PREFERENCE, 5);
    }

    public void setSelectByOrderPreference(int selectByOrderPreference) {
        preferences.putInt(SELECT_BY_ORDER_PREFERENCE, selectByOrderPreference);
    }

    public double getSelectBySignificancePreference() {
        return preferences.getDouble(SELECT_BY_SIGNIFICANCE_PREFERENCE, 20);
    }

    public void setSelectBySignificancePreference(double selectBySignificancePreference) {
        preferences.putDouble(SELECT_BY_SIGNIFICANCE_PREFERENCE, selectBySignificancePreference);
    }

    public String getSearchPositionPreference() {
        return preferences.get(SEARCH_POSITION_PREFERENCE, "");
    }

    public void setSearchPositionPreference(String searchPositionPreference) {
        preferences.put(SEARCH_POSITION_PREFERENCE, searchPositionPreference);
    }

    public Credentials getCredentials() {
        // important: return the current values since the Credentials is passed to the RemoteCatalog
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
        return findExistingPath(path);
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

    public NumberingStrategy getNumberingStrategyPreference() {
        try {
            return NumberingStrategy.valueOf(preferences.get(NUMBERING_STRATEGY_PREFERENCE, Absolute_Position_Within_Position_List.toString()));
        } catch (IllegalArgumentException e) {
            return Absolute_Position_Within_Position_List;
        }
    }

    public void setNumberingStrategyPreference(NumberingStrategy numberingStrategy) {
        preferences.put(NUMBERING_STRATEGY_PREFERENCE, numberingStrategy.toString());
    }

    public String getTimeZonePreference() {
        return preferences.get(TIME_ZONE_PREFERENCE, TimeZone.getDefault().getID());
    }

    public void setTimeZonePreference(String timeZoneId) {
        preferences.put(TIME_ZONE_PREFERENCE, timeZoneId);
    }

    // helpers for external components

    public RouteServiceOperator getRouteServiceOperator() {
        return routeServiceOperator;
    }

    public UnitSystemModel getUnitSystemModel() {
        return unitSystemModel;
    }

    public ProfileModeModel getProfileModeModel() {
        return profileModeModel;
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
                StringWriter stackTrace = new StringWriter();
                throwable.printStackTrace(new PrintWriter(stackTrace));
                log.severe("Open error from " + path + ": " + throwable + "\n" + stackTrace.toString());
                JLabel labelOpenError = new JLabel(MessageFormat.format(getBundle().getString("open-error"), shortenPath(path, 60), getLocalizedMessage(throwable)));
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
                StringWriter stackTrace = new StringWriter();
                throwable.printStackTrace(new PrintWriter(stackTrace));
                String dialogUrls = printArrayToDialogString(urls.toArray(new URL[urls.size()]));
                log.severe("Open error from " + dialogUrls + ": " + throwable + "\n" + stackTrace.toString());
                JLabel labelOpenError = new JLabel(MessageFormat.format(getBundle().getString("open-error"), dialogUrls, getLocalizedMessage(throwable)));
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
        getRouteServiceOperator().executeOperation(new RouteServiceOperator.Operation() {
            public String getName() {
                return "SendErrorReport";
            }

            public void run() throws IOException {
                getRouteServiceOperator().getRouteFeedback().sendErrorReport(log, description, file);
            }
        });
    }

    public void sendChecksums(final Download download) {
        final DataSource dataSource = RouteConverter.getInstance().getDataSourceManager().
                getDataSourceService().getDataSourceByUrlPrefix(download.getUrl());
        if (dataSource == null)
            return;

        final Map<FileAndChecksum, List<FileAndChecksum>> fileToFragments = new HashMap<>();
        fileToFragments.put(download.getFile(), download.getFragments());

        getRouteServiceOperator().executeOperation(new RouteServiceOperator.Operation() {
            public String getName() {
                return "SendChecksums";
            }

            public void run() throws IOException {
                getRouteServiceOperator().getRouteFeedback().sendChecksums(dataSource, fileToFragments, download.getUrl());
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
            getMapView().setSelectedPositions(selectedPositions, true);
        if (profileView != null)
            profileView.setSelectedPositions(selectedPositions, true);
    }

    public ElevationServiceFacade getElevationServiceFacade() {
        return elevationServiceFacade;
    }

    public InsertPositionFacade getInsertPositionFacade() {
        return insertPositionFacade;
    }

    public RoutingServiceFacade getRoutingServiceFacade() {
        return routingServiceFacade;
    }

    protected HgtFilesService getHgtFilesService() {
        return hgtFilesService;
    }

    public DataSourceManager getDataSourceManager() {
        return dataSourceManager;
    }

    private DownloadManager getDownloadManager() {
        return getDataSourceManager().getDownloadManager();
    }

    private BatchPositionAugmenter batchPositionAugmenter = null;

    public synchronized BatchPositionAugmenter getBatchPositionAugmenter() {
        if (batchPositionAugmenter == null) {
            batchPositionAugmenter = new BatchPositionAugmenter(getPositionsView(), getPositionsModel(), frame);
        }
        return batchPositionAugmenter;
    }

    protected MapViewCallback getMapViewCallback() {
        return new MapViewCallbackImpl();
    }

    public JTable getPositionsView() {
        return getConvertPanel().getPositionsView();
    }

    public int selectPositionsWithinDistanceToPredecessor(double distance) {
        return getConvertPanel().selectPositionsWithinDistanceToPredecessor(distance);
    }

    public int[] selectAllButEveryNthPosition(int order) {
        return getConvertPanel().selectAllButEveryNthPosition(order);
    }

    public int selectInsignificantPositions(double threshold) {
        return getConvertPanel().selectInsignificantPositions(threshold);
    }

    public void clearSelection() {
        getConvertPanel().clearSelection();
    }

    // map view related helpers

    public boolean isMapViewAvailable() {
        return getMapView() != null;
    }

    public boolean isMapViewInitialized() {
        return isMapViewAvailable() && getMapView().isInitialized();
    }

    public NavigationPosition getMapCenter() {
        return isMapViewAvailable() ? getMapView().getCenter() : new SimpleNavigationPosition(-41.0, 41.0);
    }

    public void setShowAllPositionsAfterLoading(boolean showAllPositionsAfterLoading) {
        if (isMapViewAvailable())
            getMapView().setShowAllPositionsAfterLoading(showAllPositionsAfterLoading);
    }

    public void setRecenterAfterZooming(boolean recenterAfterZooming) {
        if (isMapViewAvailable())
            getMapView().setRecenterAfterZooming(recenterAfterZooming);
    }

    public void setShowCoordinates(boolean showCoordinates) {
        if (isMapViewAvailable())
            getMapView().setShowCoordinates(showCoordinates);
    }

    public void setShowWaypointDescription(boolean showWaypointDescription) {
        if (isMapViewAvailable())
            getMapView().setShowWaypointDescription(showWaypointDescription);
    }

    public void showMapBorder(BoundingBox mapBoundingBox) {
        if (isMapViewAvailable())
            getMapView().showMapBorder(mapBoundingBox);
    }

    // profile view related helpers

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
        mapPanel.setVisible(false);
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
        profilePanel = new JPanel();
        profilePanel.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        profilePanel.setMinimumSize(new Dimension(0, 0));
        profilePanel.setPreferredSize(new Dimension(0, 0));
        profilePanel.setVisible(false);
        profileSplitPane.setRightComponent(profilePanel);
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return contentPane;
    }

    private class LazyTabInitializer implements ChangeListener {
        private Map<Component, Runnable> lazyInitializers = new HashMap<>();
        private Map<Component, PanelInTab> initialized = new HashMap<>();

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
                    getMapView().resize();
                    preferences.putInt(MAP_DIVIDER_LOCATION_PREFERENCE, mapSplitPane.getDividerLocation());
                    log.info("Changed map divider to " + mapSplitPane.getDividerLocation());
                    enableActions();
                }
            }
        }

        private void enableActions() {
            ActionManager actionManager = getContext().getActionManager();
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
                            getMapView().getComponent().setVisible(false);
                        else if ((Integer) e.getOldValue() == 1)
                            getMapView().getComponent().setVisible(true);
                        getMapView().resize();
                    }
                    preferences.putInt(PROFILE_DIVIDER_LOCATION_PREFERENCE, profileSplitPane.getDividerLocation());
                    log.finer("Changed profile divider to " + profileSplitPane.getDividerLocation());
                    enableActions();
                }
            }
        }

        private void enableActions() {
            ActionManager actionManager = getContext().getActionManager();
            actionManager.enable("maximize-map", location < frame.getHeight() - 10);
            actionManager.enable("maximize-positionlist", location < frame.getHeight() - 10);
            actionManager.enable("show-profile", location > frame.getHeight() - 80);
        }
    }

    protected void initializeServices() {
        System.setProperty("rest", parseVersionFromManifest().getVersion());
        RouteFeedback routeFeedback = new RouteFeedback(System.getProperty("feedback", "http://www.routeconverter.com/feedback/"), getApiUrl(), RouteConverter.getInstance().getCredentials());
        routeServiceOperator = new RouteServiceOperator(getFrame(), routeFeedback);
        updateChecker = new UpdateChecker(routeFeedback);
        DownloadManager downloadManager = new DownloadManager(new File(getApplicationDirectory(), "download-queue.xml"));
        downloadManager.addDownloadListener(new ChecksumSender());
        downloadManager.addDownloadListener(new DownloadNotifier());
        dataSourceManager = new DataSourceManager(downloadManager);
        hgtFilesService = new HgtFilesService(dataSourceManager);
    }

    protected void initializeActions() {
        final ActionManager actionManager = getContext().getActionManager();
        actionManager.register("exit", new ExitAction());
        actionManager.register("print-map", new PrintMapAction(false));
        actionManager.register("print-map-and-route", new PrintMapAction(true));
        actionManager.register("print-profile", new PrintProfileAction());
        actionManager.register("find-place", new FindPlaceAction());
        actionManager.register("show-map-and-positionlist", new ShowMapAndPositionListAction());
        actionManager.register("show-profile", new ShowProfileAction());
        actionManager.register("maximize-map", new MoveSplitPaneDividersAction(mapSplitPane, MAX_VALUE, profileSplitPane, MAX_VALUE));
        actionManager.register("maximize-positionlist", new MoveSplitPaneDividersAction(mapSplitPane, 0, profileSplitPane, MAX_VALUE));
        actionManager.register("show-all-positions-on-map", new ShowAllPositionsOnMapAction());
        actionManager.register("insert-positions", new InsertPositionsAction());
        actionManager.register("delete-positions", new DeletePositionsAction());
        actionManager.register("revert-positions", new RevertPositionListAction());
        actionManager.register("convert-route-to-track", new ConvertRouteToTrackAction());
        actionManager.register("convert-track-to-route", new ConvertTrackToRouteAction());
        actionManager.register("show-downloads", new ShowDownloadsAction());
        actionManager.register("show-options", new ShowOptionsAction());
        actionManager.register("complete-flight-plan", new CompleteFlightPlanAction());
        actionManager.register("help-topics", new HelpTopicsAction());
        actionManager.register("check-for-update", new CheckForUpdateAction(updateChecker));
        actionManager.register("send-error-report", new SendErrorReportAction());
        actionManager.register("show-about", new ShowAboutAction());
        JMenu mergeMenu = findMenuComponent(getContext().getMenuBar(), "positionlist", "merge-positionlist", JMenu.class);
        new MergePositionListMenu(mergeMenu, getPositionsView(), getConvertPanel().getFormatAndRoutesModel());

        setHelpIDString(frame.getRootPane(), "top");
        setHelpIDString(convertPanel, "convert");
        setHelpIDString(browsePanel, "browse");
        setHelpIDString(mapPanel, "map");

        // delay JavaHelp initialization
        ActionListener actionListener = new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                actionManager.run("help-topics", event);
            }
        };
        frame.getRootPane().registerKeyboardAction(actionListener,
                getKeyStroke(VK_HELP, 0), WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        frame.getRootPane().registerKeyboardAction(actionListener,
                getKeyStroke(VK_F1, 0), WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        new ProfileModeMenu(getContext().getMenuBar(), getProfileModeModel());
        new UndoMenuSynchronizer(getContext().getMenuBar(), getContext().getUndoManager());
        new ReopenMenuSynchronizer(getContext().getMenuBar(), getConvertPanel(), getRecentUrlsModel());
    }

    public String getApiUrl() {
        return System.getProperty("api", "http://api.routeconverter.com/");
    }

    private File getDataSourcesDirectory() {
        return getApplicationDirectory("datasources");
    }

    private void initializeDatasources() {
        try {
            getDataSourceManager().initialize(getEdition().toLowerCase(), getDataSourcesDirectory());
        } catch (Exception e) {
            log.warning("Could not initialize datasource manager: " + e);
            getContext().getNotificationManager().showNotification(MessageFormat.format(
                    getBundle().getString("datasource-initialization-error"), getLocalizedMessage(e)), null);

            if (e instanceof UnmarshalException) {
                log.info("Deleting old datasources");
                try {
                    recursiveDelete(getDataSourcesDirectory());
                } catch (IOException e2) {
                    log.warning("Could not delete old datasources: " + e2);
                }
            }
        }

        initializeElevationServices();
        initializeRoutingServices();

        new Thread(new Runnable() {
            public void run() {
                getDownloadManager().loadQueue();

                scanLocalMapsAndThemes();

                try {
                    getDataSourceManager().update(getEdition().toLowerCase(), getApiUrl(), getDataSourcesDirectory());
                } catch (Exception e) {
                    log.warning("Could not update datasource manager: " + e);
                    getContext().getNotificationManager().showNotification(MessageFormat.format(
                            getBundle().getString("datasource-update-error"), getLocalizedMessage(e)), null);
                }

                initializeElevationServices();
                initializeRoutingServices();

                scanRemoteMapsAndThemes();
            }
        }, "DataSourceInitializer").start();
    }

    protected void initializeElevationServices() {
        getElevationServiceFacade().clear();

        AutomaticElevationService automaticElevationService = new AutomaticElevationService(getElevationServiceFacade());
        getElevationServiceFacade().addElevationService(automaticElevationService);
        getElevationServiceFacade().setPreferredElevationService(automaticElevationService);

        getElevationServiceFacade().addElevationService(new EarthToolsService());
        getElevationServiceFacade().addElevationService(new GeoNamesService());
        getElevationServiceFacade().addElevationService(new GoogleMapsService());

        getHgtFilesService().initialize();
        for (HgtFiles hgtFile : getHgtFilesService().getHgtFiles()) {
            getElevationServiceFacade().addElevationService(hgtFile);
        }
    }

    protected void initializeRoutingServices() {
        getRoutingServiceFacade().clear();
        RoutingService service = new GoogleDirectionsService(getMapView());
        getRoutingServiceFacade().addRoutingService(service);
        getRoutingServiceFacade().setPreferredRoutingService(service);
    }

    protected void scanLocalMapsAndThemes() {
    }

    protected void scanRemoteMapsAndThemes() {
    }

    private class PrintMapAction extends FrameAction {
        private boolean withRoute;

        private PrintMapAction(boolean withRoute) {
            this.withRoute = withRoute;
        }

        public void run() {
            String title = getConvertPanel().getUrlModel().getShortUrl() + " / " + getConvertPanel().getFormatAndRoutesModel().getSelectedRoute().getName();
            getMapView().print(title, withRoute);
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

    private class ShowAllPositionsOnMapAction extends FrameAction {
        public void run() {
            if (isMapViewAvailable())
                getMapView().showAllPositions();
        }
    }

    private class PrintProfileAction extends FrameAction {
        public void run() {
            profileView.print();
        }
    }

}
