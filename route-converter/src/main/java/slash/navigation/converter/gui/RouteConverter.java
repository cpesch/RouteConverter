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
import slash.common.helpers.APIKeyRegistry;
import slash.common.log.LoggingHelper;
import slash.common.system.Version;
import slash.navigation.babel.BabelException;
import slash.navigation.base.NavigationFormatRegistry;
import slash.navigation.base.RouteCharacteristics;
import slash.navigation.columbus.ColumbusV1000Device;
import slash.navigation.common.*;
import slash.navigation.converter.gui.actions.*;
import slash.navigation.converter.gui.dnd.PanelDropHandler;
import slash.navigation.converter.gui.helpers.*;
import slash.navigation.converter.gui.models.*;
import slash.navigation.converter.gui.panels.*;
import slash.navigation.converter.gui.profileview.ProfileView;
import slash.navigation.converter.gui.profileview.XAxisModeMenu;
import slash.navigation.converter.gui.profileview.YAxisModeMenu;
import slash.navigation.datasources.DataSource;
import slash.navigation.datasources.DataSourceManager;
import slash.navigation.download.Download;
import slash.navigation.download.DownloadManager;
import slash.navigation.download.FileAndChecksum;
import slash.navigation.feedback.domain.RouteFeedback;
import slash.navigation.gui.Application;
import slash.navigation.gui.SingleFrameApplication;
import slash.navigation.gui.actions.*;
import slash.navigation.gui.models.BooleanModel;
import slash.navigation.maps.tileserver.TileServerMapManager;
import slash.navigation.mapview.MapView;
import slash.navigation.mapview.MapViewCallback;
import slash.navigation.rest.Credentials;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.text.MessageFormat;
import java.util.List;
import java.util.*;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

import static com.intellij.uiDesigner.core.GridConstraints.*;
import static java.awt.event.KeyEvent.VK_F1;
import static java.awt.event.KeyEvent.VK_HELP;
import static java.lang.Integer.MAX_VALUE;
import static java.lang.Math.abs;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.Locale.GERMANY;
import static java.util.Locale.US;
import static javax.help.CSH.setHelpIDString;
import static javax.swing.JOptionPane.*;
import static javax.swing.JSplitPane.DIVIDER_LOCATION_PROPERTY;
import static javax.swing.KeyStroke.getKeyStroke;
import static javax.swing.SwingUtilities.invokeLater;
import static slash.common.helpers.ExceptionHelper.getLocalizedMessage;
import static slash.common.helpers.ExceptionHelper.printStackTrace;
import static slash.common.helpers.LocaleHelper.*;
import static slash.common.io.Directories.getApplicationDirectory;
import static slash.common.io.Directories.getTemporaryDirectory;
import static slash.common.io.Files.*;
import static slash.common.system.Platform.*;
import static slash.common.system.Version.parseVersionFromManifest;
import static slash.feature.client.Feature.initializePreferences;
import static slash.navigation.common.NumberPattern.Number_Space_Then_Description;
import static slash.navigation.common.NumberingStrategy.Absolute_Position_Within_Position_List;
import static slash.navigation.converter.gui.helpers.ExternalPrograms.startBrowserForTranslation;
import static slash.navigation.converter.gui.helpers.MapViewImplementation.JavaFX8;
import static slash.navigation.converter.gui.helpers.TagStrategy.Create_Backup_In_Subdirectory;
import static slash.navigation.converter.gui.models.LocalActionConstants.POSITIONS;
import static slash.navigation.datasources.DataSourceManager.FORMAT_XML;
import static slash.navigation.datasources.DataSourceManager.V1;
import static slash.navigation.download.Action.Copy;
import static slash.navigation.download.Action.Extract;
import static slash.navigation.gui.helpers.JMenuHelper.findItem;
import static slash.navigation.gui.helpers.JMenuHelper.findMenu;
import static slash.navigation.gui.helpers.UIHelper.*;

/**
 * A small graphical user interface for the route conversion.
 *
 * @author Christian Pesch
 */

public abstract class RouteConverter extends SingleFrameApplication {
    protected static final Logger log = Logger.getLogger(RouteConverter.class.getName());
    private static final Preferences preferences = Preferences.userNodeForPackage(RouteConverter.class);

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

    protected String getProduct() {
        return "RouteConverter";
    }

    public abstract String getEdition();

    public abstract String getEditionId();

    private static final String MAP_VIEW_PREFERENCE = "mapView";
    private static final String SHOW_ALL_POSITIONS_AFTER_LOADING_PREFERENCE = "showAllPositionsAfterLoading";
    private static final String RECENTER_AFTER_ZOOMING_PREFERENCE = "recenterAfterZooming";
    private static final String TIME_ZONE_PREFERENCE = "timeZone";
    private static final String NUMBER_PATTERN_PREFERENCE = "numberPattern";
    private static final String NUMBERING_STRATEGY_PREFERENCE = "numberingStrategy";
    private static final String SELECT_BY_DISTANCE_PREFERENCE = "selectByDistance";
    private static final String SELECT_BY_ORDER_PREFERENCE = "selectByOrder";
    private static final String SELECT_BY_SIGNIFICANCE_PREFERENCE = "selectBySignificance";
    private static final String FIND_PLACE_PREFERENCE = "findPlace";
    private static final String PHOTO_TIMEZONE_PREFERENCE = "photoTimeZone";
    private static final String TAG_STRATEGY_PREFERENCE = "tagStrategy";

    private static final String MAP_DIVIDER_LOCATION_PREFERENCE = "mapDividerLocation";
    private static final String MAP_DIVIDER_RATIO_PREFERENCE = "mapDividerRatio";
    private static final String PROFILE_DIVIDER_LOCATION_PREFERENCE = "profileDividerLocation";
    private static final String PROFILE_DIVIDER_RATIO_PREFERENCE = "profileDividerRatio";

    private static final String USERNAME_PREFERENCE = "userName";
    private static final String PASSWORD_PREFERENCE = "userAuthentication";
    private static final String CATEGORY_PREFERENCE = "category";
    private static final String ADD_PHOTO_PREFERENCE = "addPhoto";
    private static final String ADD_AUDIO_PREFERENCE = "addAudio";
    private static final String UPLOAD_ROUTE_PREFERENCE = "uploadRoute";

    private static final String SHOWED_MISSING_TRANSLATOR_PREFERENCE = "showedMissingTranslator-2.30"; // versioned preference
    public static final String AUTOMATIC_UPDATE_CHECK_PREFERENCE = "automaticUpdateCheck-2.30";

    private NavigationFormatRegistry navigationFormatRegistry = new NavigationFormatRegistry();
    private RouteServiceOperator routeServiceOperator;
    private UpdateChecker updateChecker;
    private DataSourceManager dataSourceManager;
    private ElevationServiceFacade elevationServiceFacade = new ElevationServiceFacade();
    private GeocodingServiceFacade geocodingServiceFacade = new GeocodingServiceFacade();
    private InsertPositionFacade insertPositionFacade = new InsertPositionFacade();
    private BooleanModel showAllPositionsAfterLoading = new BooleanModel(SHOW_ALL_POSITIONS_AFTER_LOADING_PREFERENCE, true);
    private BooleanModel recenterAfterZooming = new BooleanModel(RECENTER_AFTER_ZOOMING_PREFERENCE, true);
    private TimeZoneModel timeZoneModel = new TimeZoneModel(TIME_ZONE_PREFERENCE, TimeZone.getDefault());
    private TimeZoneModel photoTimeZoneModel = new TimeZoneModel(PHOTO_TIMEZONE_PREFERENCE, timeZoneModel.getTimeZone());
    private final UnitSystemModel unitSystemModel = new UnitSystemModel();
    private final CharacteristicsModel characteristicsModel = new CharacteristicsModel();
    private final RoutingServiceFacade routingServiceFacade = new RoutingServiceFacade();
    private final MapPreferencesModel mapPreferencesModel = new MapPreferencesModel(getRoutingServiceFacade().getRoutingPreferencesModel(), getCharacteristicsModel(), getUnitSystemModel());
    private GoogleMapsServerModel googleMapsServerModel = new GoogleMapsServerModel();
    private ProfileModeModel profileModeModel = new ProfileModeModel();
    private TileServerMapManager tileServerMapManager;
    private DistanceAndTimeAggregator distanceAndTimeAggregator = new DistanceAndTimeAggregator();

    protected JPanel contentPane;
    private JSplitPane mapSplitPane, profileSplitPane;
    private JTabbedPane tabbedPane;
    private JPanel convertPanel, pointOfInterestPanel, photoPanel, browsePanel, mapPanel, profilePanel;
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
        checkJavaPrequisites();
        checkForGoogleMapsAPIKey();
        show();
        checkForMissingTranslator();
        updateChecker.implicitCheck(getFrame());
    }

    protected void checkJavaPrequisites() {
        String currentVersion = System.getProperty("java.version");
        if (!isCurrentAtLeastMinimumVersion(currentVersion, "1.7.0_40")) {
            showMessageDialog(null, "Java " + currentVersion + " does not support JavaFX. Please install Java 8 or 10.", "RouteConverter", ERROR_MESSAGE);
            System.exit(7);
        }

        if (!isCurrentAtLeastMinimumVersion(currentVersion, "1.8.0")) {
            showMessageDialog(null, "Java " + currentVersion + " is too old for FIT and EclipseLink. Please install Java 8 or 11.", "RouteConverter", ERROR_MESSAGE);
            System.exit(8);
        }

        if (isWindows() && (currentVersion.equals("1.8.0_161") || currentVersion.equals("1.8.0_162") || currentVersion.equals("1.8.0_171") || currentVersion.equals("1.8.0_172"))) {
            showMessageDialog(null, "Java " + currentVersion + " contains a fatal bug in JavaFX on Windows. Please install Java 8 Update 181 or Java 11.", "RouteConverter", ERROR_MESSAGE);
            System.exit(9);
        }

        if (isJava15OrLater()) {
            showMessageDialog(null, "Java " + currentVersion + " contains breaking changes. Please install Java 8 or 11.", "RouteConverter", ERROR_MESSAGE);
            System.exit(10);
        }
    }

    protected abstract void checkForGoogleMapsAPIKey();

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

    // helper

    private void initializeLogging() {
        LoggingHelper loggingHelper = LoggingHelper.getInstance();
        loggingHelper.logToFileAndConsole();
        log.info(format("Started %s for %s with locale %s on %s and %s with %d MByte maximum heap",
                getTitle(), parseVersionFromManifest().getOperationSystem(), Locale.getDefault(), getJava(), getPlatform(), getMaximumMemory()));
        log.info(format("java.io.tmpdir: %s, user.home: %s, Application directory: %s, Temporary directory: %s",
                System.getProperty("java.io.tmpdir"), System.getProperty("user.home"), getApplicationDirectory(), getTemporaryDirectory()));
    }

    private List<String> getLanguagesWithActiveTranslators() {
        List<Locale> localesOfActiveTranslators = asList(CATALAN, DENMARK, GERMANY, SERBIA, US);
        List<String> results = new ArrayList<>();
        for (Locale locale : localesOfActiveTranslators) {
            results.add(locale.getLanguage());
        }
        return results;
    }

    protected void checkForMissingTranslator() {
        List<String> activeLanguages = getLanguagesWithActiveTranslators();
        String language = Locale.getDefault().getLanguage();
        if (!activeLanguages.contains(language) && !preferences.getBoolean(SHOWED_MISSING_TRANSLATOR_PREFERENCE, false)) {
            JLabel labelTranslatorMissing = new JLabel(MessageFormat.format(getBundle().getString("translator-missing"), language));
            labelTranslatorMissing.addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent me) {
                    startBrowserForTranslation(getFrame());
                }
            });
            showMessageDialog(getFrame(), labelTranslatorMissing, getTitle(), QUESTION_MESSAGE);
            preferences.putBoolean(SHOWED_MISSING_TRANSLATOR_PREFERENCE, true);
        }
    }

    private void show() {
        patchUIManager(getBundle(),
                "OptionPane.yesButtonText", "OptionPane.noButtonText", "OptionPane.cancelButtonText",
                "FileChooser.openButtonText", "FileChooser.saveButtonText", "FileChooser.cancelButtonText",
                "FileChooser.acceptAllFileFilterText", "FileChooser.lookInLabelText", "FileChooser.fileNameLabelText",
                "FileChooser.filesOfTypeLabelText", "ColorChooser.rgbRedText", "ColorChooser.rgbGreenText",
                "ColorChooser.rgbBlueText", "ColorChooser.rgbAlphaText");
        initializePreferences(preferences);

        addExitListener(event -> getConvertPanel() == null || getConvertPanel().confirmDiscard());

        tabInitializer = new LazyTabInitializer();
        tabbedPane.addChangeListener(tabInitializer);

        openFrame();

        initializeServices();
        initializeActions();
        initializeDatasources();
        initializeDividers();

        openMapAndProfileView();

        initializeMenus();
        initializeHelp();
        getContext().getActionManager().logUsage();
        APIKeyRegistry.getInstance().logUsage();
    }

    private MapView createMapView(String className) {
        try {
            Class<?> aClass = Class.forName(className);
            return (MapView) aClass.getDeclaredConstructor().newInstance();
        } catch (Throwable t) {
            log.info("Cannot create " + className + ": " + t);
            return null;
        }
    }

    private void openFrame() {
        createFrame(getTitle(), "/slash/navigation/converter/gui/" + getProduct() + ".png", contentPane, null, new FrameMenu().createMenuBar());
        if (isMac())
            new ApplicationMenu().addApplicationMenuItems();

        new Thread(() -> invokeLater(() -> openFrame(contentPane)), "FrameOpener").start();
    }

    private void openMapAndProfileView() {
        try {
            getDownloadManager().removeDownload(getApiUrl() + V1 + "tileservers/" + FORMAT_XML);
            getDownloadManager().removeDownload(getApiUrl() + V1 + "tileservers-offline/" + FORMAT_XML);
            getDownloadManager().removeDownload(getApiUrl() + V1 + "datasources/openandromaps-themes/" + FORMAT_XML);

            File mapServers = new File(getApplicationDirectory("tileservers"), "mapservers.xml");
            getDownloadManager().executeDownload("RouteConverter Map Servers", getApiUrl() + V1 + "mapservers/" + FORMAT_XML, Copy, mapServers, () -> {

                File overlayServers = new File(getApplicationDirectory("tileservers"), "overlayservers.xml");
                getDownloadManager().executeDownload("RouteConverter Overlay Servers", getApiUrl() + V1 + "overlayservers/" + FORMAT_XML, Copy, overlayServers, () -> {

                    getTileServerMapManager().scanTileServers();

                    invokeLater(() -> {
                        setMapView(getMapViewPreference());

                        invokeLater(() -> {
                            openProfileView();

                            adjustDividersForScreenMovement();
                            invokeLater(this::initializeDividerListeners);
                        });
                    });
                });
            });
        } catch (Exception e) {
            log.warning("Could not download tile servers: " + e);
            e.printStackTrace();
        }
    }

    public synchronized void setMapView(MapViewImplementation mapViewImplementation) {
        log.info("Using map view: " + mapViewImplementation);
        setMapViewPreference(mapViewImplementation);

        if (isMapViewAvailable()) {
            mapPanel.removeAll();
            mapView.dispose();
        }

        mapView = createMapView(mapViewImplementation.getClassName());
        if (mapView == null) {
            mapPanel.add(new JLabel(MessageFormat.format(getBundle().getString("initialize-map-error"),
                    printStackTrace(new UnsupportedOperationException()).replaceAll("\n", "<p>"))), MAP_PANEL_CONSTRAINTS);

        } else {
            getMapView().initialize(getConvertPanel().getPositionsModel(), mapPreferencesModel, getMapViewCallback());

            @SuppressWarnings({"ThrowableResultOfMethodCallIgnored"})
            Throwable cause = getMapView().getInitializationCause();
            if (getMapView().getComponent() == null || cause != null) {
                mapPanel.add(new JLabel(MessageFormat.format(getBundle().getString("initialize-map-error"),
                        printStackTrace(cause).replaceAll("\n", "<p>"))), MAP_PANEL_CONSTRAINTS);
            } else {
                mapPanel.add(getMapView().getComponent(), MAP_PANEL_CONSTRAINTS);
            }
        }
        mapPanel.revalidate();
    }

    public MapView getMapView() {
        return mapView;
    }

    private void openProfileView() {
        profileView = new ProfileView();
        profileView.initialize(getConvertPanel().getPositionsModel(),
                getConvertPanel().getPositionsSelectionModel(),
                getUnitSystemModel(),
                getProfileModeModel());
        profilePanel.add(profileView.getComponent(), PROFILE_PANEL_CONSTRAINTS);
        profilePanel.setTransferHandler(new PanelDropHandler());
        profilePanel.revalidate();
    }

    private void initializeDividers() {
        int mapDividerLocation = preferences.getInt(MAP_DIVIDER_LOCATION_PREFERENCE, -1);
        if (mapDividerLocation < 1) {
            mapDividerLocation = 300;
        }
        mapSplitPane.setDividerLocation(mapDividerLocation);
        log.info("Initialized map divider to " + mapDividerLocation);

        int profileDividerLocation = preferences.getInt(PROFILE_DIVIDER_LOCATION_PREFERENCE, -1);
        if (profileDividerLocation < 2) {
            profileDividerLocation = 888;
        }
        profileSplitPane.setDividerLocation(profileDividerLocation);
        log.info("Initialized profile divider to " + profileDividerLocation);
    }

    private void adjustDividersForScreenMovement() {
        double mapDividerRatio = preferences.getDouble(MAP_DIVIDER_RATIO_PREFERENCE, -1.0);
        int mapDividerLocation = (int) (contentPane.getWidth() * mapDividerRatio);

        if (mapDividerRatio > 0 && abs(mapDividerLocation - mapSplitPane.getDividerLocation()) > 10) {
            mapSplitPane.setDividerLocation(mapDividerLocation);
            log.info("Adjusted map divider to " + mapDividerLocation);
        }

        double profileDividerRatio = preferences.getDouble(PROFILE_DIVIDER_RATIO_PREFERENCE, -1.0);
        int profileDividerLocation = (int) (contentPane.getHeight() * profileDividerRatio);

        if (profileDividerRatio > 0 && abs(profileDividerLocation - profileSplitPane.getDividerLocation()) > 10) {
            profileSplitPane.setDividerLocation(profileDividerLocation);
            log.info("Adjusted profile divider to " + profileDividerLocation);
        }
    }

    private void initializeDividerListeners() {
        mapSplitPane.addPropertyChangeListener(new MapSplitPaneListener());
        profileSplitPane.addPropertyChangeListener(new ProfileSplitPaneListener(profileSplitPane.getDividerLocation()));
    }

    protected void shutdown() {
        if (isMapViewAvailable())
            getMapView().dispose();
        if (positionAugmenter != null)
            positionAugmenter.dispose();
        if (audioPlayer != null)
            audioPlayer.dispose();
        if (geoTagger != null)
            geoTagger.dispose();
        getDataSourceManager().dispose();
        getDownloadManager().saveQueue();
        getTileServerMapManager().dispose();
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

    public String getFindPlacePreference() {
        return preferences.get(FIND_PLACE_PREFERENCE, "");
    }

    public void setFindPlacePreference(String searchPositionPreference) {
        preferences.put(FIND_PLACE_PREFERENCE, searchPositionPreference);
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

    public File getAddPhotoPreference() { // for TimeAlbum
        File path = new File(preferences.get(ADD_PHOTO_PREFERENCE, ""));
        return findExistingPath(path);
    }

    public void setAddPhotoPreference(File path) { // for TimeAlbum
        preferences.put(ADD_PHOTO_PREFERENCE, path.getPath());
    }

    public File getAddAudioPreference() { // for TimeAlbum
        File path = new File(preferences.get(ADD_AUDIO_PREFERENCE, ""));
        return findExistingPath(path);
    }

    public void setAddAudioPreference(File path) { // for TimeAlbum
        preferences.put(ADD_AUDIO_PREFERENCE, path.getPath());
    }

    public TagStrategy getTagStrategyPreference() { // for TimeAlbum
        try {
            return TagStrategy.valueOf(preferences.get(TAG_STRATEGY_PREFERENCE, Create_Backup_In_Subdirectory.toString()));
        } catch (IllegalArgumentException e) {
            return Create_Backup_In_Subdirectory;
        }
    }

    public void setTagStrategyPreference(TagStrategy tagStrategy) { // for TimeAlbum
        preferences.put(TAG_STRATEGY_PREFERENCE, tagStrategy.toString());
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

    // helpers for external components

    public NavigationFormatRegistry getNavigationFormatRegistry() {
        return navigationFormatRegistry;
    }

    public RouteServiceOperator getRouteServiceOperator() {
        return routeServiceOperator;
    }

    public CharacteristicsModel getCharacteristicsModel() {
        return characteristicsModel;
    }

    public UnitSystemModel getUnitSystemModel() {
        return unitSystemModel;
    }

    public GoogleMapsServerModel getGoogleMapsServerModel() { // for RouteConverterGoogle
        return googleMapsServerModel;
    }

    private ProfileModeModel getProfileModeModel() {
        return profileModeModel;
    }

    public TileServerMapManager getTileServerMapManager() {
        return tileServerMapManager;
    }

    // dialogs for external components

    public void handleBabelError(final BabelException e) {
        invokeLater(() -> showMessageDialog(frame,
                MessageFormat.format(getBundle().getString("babel-error"), e.getBabelPath()), frame.getTitle(),
                ERROR_MESSAGE));
    }

    public void handleOpenError(final Throwable throwable, final String path) {
        invokeLater(() -> {
            log.severe("Open error from " + path + ": " + throwable + "\n" + printStackTrace(throwable));
            showMessageDialog(frame, new JLabel(MessageFormat.format(getBundle().getString("open-error"), shortenPath(path, 60), getLocalizedMessage(throwable))),
                    frame.getTitle(), ERROR_MESSAGE);
        });
    }

    public void handleOpenError(final Throwable throwable, final List<URL> urls) {
        invokeLater(() -> {
            String dialogUrls = asDialogString(urls, true);
            log.severe("Open error from " + dialogUrls + ": " + throwable + "\n" + printStackTrace(throwable));
            showMessageDialog(frame, new JLabel(MessageFormat.format(getBundle().getString("open-error"), dialogUrls, getLocalizedMessage(throwable))),
                    frame.getTitle(), ERROR_MESSAGE);
        });
    }

    public void handleUnsupportedFormat(final String path) {
        invokeLater(() -> {
            log.severe("Unsupported format: " + path);
            showMessageDialog(frame,
                    MessageFormat.format(getBundle().getString("unsupported-format"), shortenPath(path, 60)),
                    frame.getTitle(), WARNING_MESSAGE);
        });
    }

    public void handleFileNotFound(final String path) {
        invokeLater(() -> {
            log.severe("File not found: " + path);
            showMessageDialog(frame,
                    MessageFormat.format(getBundle().getString("file-not-found"), shortenPath(path, 60)),
                    frame.getTitle(), WARNING_MESSAGE);
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
        if (dataSource == null) {
            return;
        }

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

    public void openPositionList(List<URL> urls, boolean selectConvertPanel) {
        if (selectConvertPanel)
            tabbedPane.setSelectedComponent(convertPanel);
        getConvertPanel().openPositionList(urls);
    }

    public UrlDocument getUrlModel() {
        return getConvertPanel().getUrlModel();
    }

    public void revertPositions() {
        getConvertPanel().getPositionsModel().revert();
        getConvertPanel().clearSelection();
    }

    public void renamePositionList(String name) {
        getConvertPanel().renamePositionList(name);
    }

    public void setRouteCharacteristics(RouteCharacteristics characteristics) {
        getCharacteristicsModel().setSelectedItem(characteristics);
    }

    public void selectPositionsInMap(int[] selectedPositions) {
        if (isMapViewAvailable()) {
            getMapView().setSelectedPositions(selectedPositions, true);
        }
        if (profileView != null) {
            profileView.setSelectedPositions(selectedPositions, true);
        }
    }

    public void selectPositionsInMap(List<NavigationPosition> selectedPositions) {
        if (isMapViewAvailable()) {
            getMapView().setSelectedPositions(selectedPositions);
        }
    }

    public ElevationServiceFacade getElevationServiceFacade() {
        return elevationServiceFacade;
    }

    public GeocodingServiceFacade getGeocodingServiceFacade() {
        return geocodingServiceFacade;
    }

    public InsertPositionFacade getInsertPositionFacade() {
        return insertPositionFacade;
    }

    public RoutingServiceFacade getRoutingServiceFacade() {
        return routingServiceFacade;
    }

    public DistanceAndTimeAggregator getDistanceAndTimeAggregator() {
        return distanceAndTimeAggregator;
    }

    public DataSourceManager getDataSourceManager() {
        return dataSourceManager;
    }

    public DownloadManager getDownloadManager() {
        return getDataSourceManager().getDownloadManager();
    }

    private PositionAugmenter positionAugmenter;

    public synchronized PositionAugmenter getPositionAugmenter() {
        if (positionAugmenter == null) {
            positionAugmenter = new PositionAugmenter(getConvertPanel().getPositionsView(), getConvertPanel().getPositionsModel(),
                    getFrame(), elevationServiceFacade, geocodingServiceFacade);
        }
        return positionAugmenter;
    }

    private AudioPlayer audioPlayer; // for TimeAlbum

    public synchronized AudioPlayer getAudioPlayer() {
        if (audioPlayer == null) {
            audioPlayer = new AudioPlayer(getFrame());
        }
        return audioPlayer;
    }

    private GeoTagger geoTagger; // for TimeAlbum

    public GeoTagger getGeoTagger() {
        if (geoTagger == null) {
            geoTagger = new GeoTagger(getPhotoPanel().getPhotosView(), getPhotoPanel().getPhotosModel(), getFrame());
        }
        return geoTagger;
    }

    public TimeZoneModel getPhotoTimeZone() { // for TimeAlbum
        return photoTimeZoneModel;
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

    public NavigationPosition getMapCenter() {
        return isMapViewAvailable() ? getMapView().getCenter() : new SimpleNavigationPosition(-41.0, 41.0);
    }

    public BooleanModel getShowAllPositionsAfterLoading() {
        return showAllPositionsAfterLoading;
    }

    public BooleanModel getRecenterAfterZooming() {
        return recenterAfterZooming;
    }

    public MapPreferencesModel getMapPreferencesModel() {
        return mapPreferencesModel;
    }

    public TimeZoneModel getTimeZone() {
        return timeZoneModel;
    }

    public void showMapBorder(BoundingBox mapBoundingBox) {
        if (isMapViewAvailable()) {
            getMapView().showMapBorder(mapBoundingBox);
        }
    }

    public void showPositionMagnifier(List<NavigationPosition> positions) {
        if (isMapViewAvailable()) {
            getMapView().showPositionMagnifier(positions);
        }
    }

    public List<MapViewImplementation> getAvailableMapViews() {
        return singletonList(JavaFX8);
    }

    private MapViewImplementation getPreferredMapView() {
        return getAvailableMapViews().get(0);
    }

    public MapViewImplementation getMapViewPreference() {
        MapViewImplementation preferred = getPreferredMapView();
        try {
            MapViewImplementation mapView = MapViewImplementation.valueOf(getPreferences().get(MAP_VIEW_PREFERENCE, preferred.toString()));
            if (getAvailableMapViews().contains(mapView)) {
                return mapView;
            }
        } catch (IllegalArgumentException e) {
            // intentionally left empty
        }
        // really want the first available map view
        return RouteConverter.this.getPreferredMapView();
    }

    private void setMapViewPreference(MapViewImplementation mapView) {
        getPreferences().put(MAP_VIEW_PREFERENCE, mapView.name());
    }

    protected abstract MapViewCallback getMapViewCallback();

    // tab related helpers

    public boolean isConvertPanelSelected() {
        return tabbedPane.getSelectedComponent().equals(convertPanel);
    }

    protected boolean isPointsOfInterestEnabled() {
        return false;
    }

    public boolean isPointsOfInterestPanelSelected() {
        return tabbedPane.getSelectedComponent().equals(pointOfInterestPanel);
    }

    protected boolean isPhotosEnabled() {
        return false;
    }

    public boolean isPhotosPanelSelected() {
        return tabbedPane.getSelectedComponent().equals(photoPanel);
    }

    public boolean isBrowsePanelSelected() {
        return tabbedPane.getSelectedComponent().equals(browsePanel);
    }

    public ConvertPanel getConvertPanel() {
        return tabInitializer.getConvertPanel();
    }

    public PointOfInterestPanel getPointOfInterestPanel() {
        return tabInitializer.getPointsOfInterestPanel();
    }

    public PhotoPanel getPhotoPanel() {
        return tabInitializer.getPhotoPanel();
    }

    public BrowsePanel getBrowsePanel() {
        return tabInitializer.getBrowsePanel();
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
        mapSplitPane.setDividerLocation(0);
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
        mapPanel.setVisible(true);
        mapSplitPane.setLeftComponent(mapPanel);
        tabbedPane = new JTabbedPane();
        tabbedPane.setTabPlacement(1);
        mapSplitPane.setRightComponent(tabbedPane);
        convertPanel = new JPanel();
        convertPanel.setLayout(new BorderLayout(0, 0));
        tabbedPane.addTab(this.$$$getMessageFromBundle$$$("slash/navigation/converter/gui/RouteConverter", "convert-tab"), convertPanel);
        pointOfInterestPanel = new JPanel();
        pointOfInterestPanel.setLayout(new BorderLayout(0, 0));
        tabbedPane.addTab(this.$$$getMessageFromBundle$$$("slash/navigation/converter/gui/RouteConverter", "points-of-interest-tab"), pointOfInterestPanel);
        photoPanel = new JPanel();
        photoPanel.setLayout(new BorderLayout(0, 0));
        tabbedPane.addTab(this.$$$getMessageFromBundle$$$("slash/navigation/converter/gui/RouteConverter", "photos-tab"), photoPanel);
        browsePanel = new JPanel();
        browsePanel.setLayout(new BorderLayout(0, 0));
        tabbedPane.addTab(this.$$$getMessageFromBundle$$$("slash/navigation/converter/gui/RouteConverter", "browse-tab"), browsePanel);
        profilePanel = new JPanel();
        profilePanel.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        profilePanel.setMinimumSize(new Dimension(0, 0));
        profilePanel.setPreferredSize(new Dimension(0, 0));
        profilePanel.setVisible(true);
        profileSplitPane.setRightComponent(profilePanel);
    }

    private static Method $$$cachedGetBundleMethod$$$ = null;

    private String $$$getMessageFromBundle$$$(String path, String key) {
        ResourceBundle bundle;
        try {
            Class<?> thisClass = this.getClass();
            if ($$$cachedGetBundleMethod$$$ == null) {
                Class<?> dynamicBundleClass = thisClass.getClassLoader().loadClass("com.intellij.DynamicBundle");
                $$$cachedGetBundleMethod$$$ = dynamicBundleClass.getMethod("getBundle", String.class, Class.class);
            }
            bundle = (ResourceBundle) $$$cachedGetBundleMethod$$$.invoke(null, path, thisClass);
        } catch (Exception e) {
            bundle = ResourceBundle.getBundle(path);
        }
        return bundle.getString(key);
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
            lazyInitializers.put(convertPanel, () -> {
                PanelInTab panel = new ConvertPanel();
                convertPanel.add(panel.getRootComponent());
                initialized.put(convertPanel, panel);
            });
            addTab(pointOfInterestPanel, PointOfInterestPanel.class, isPointsOfInterestEnabled());
            addTab(photoPanel, PhotoPanel.class, isPhotosEnabled());
            lazyInitializers.put(browsePanel, () -> {
                PanelInTab panel = new BrowsePanel();
                browsePanel.add(panel.getRootComponent());
                initialized.put(browsePanel, panel);
            });
        }

        private void addTab(final JPanel panel, final Class<? extends PanelInTab> panelInTabClass, boolean includePanel) {
            if (includePanel)
                lazyInitializers.put(panel, () -> {
                    PanelInTab panelInTab;
                    try {
                        panelInTab = panelInTabClass.getDeclaredConstructor().newInstance();
                    } catch (InstantiationException | InvocationTargetException | IllegalAccessException | NoSuchMethodException e) {
                        throw new RuntimeException(e);
                    }
                    panel.add(panelInTab.getRootComponent());
                    initialized.put(panel, panelInTab);
                });
            else {
                for (int i = 0; i < tabbedPane.getTabCount(); i++) {
                    if (tabbedPane.getComponentAt(i) == panel) {
                        tabbedPane.removeTabAt(i);
                        break;
                    }
                }
            }
        }

        private synchronized ConvertPanel getConvertPanel() {
            initialize(convertPanel);
            return (ConvertPanel) initialized.get(convertPanel);
        }

        private synchronized PointOfInterestPanel getPointsOfInterestPanel() {
            initialize(pointOfInterestPanel);
            return (PointOfInterestPanel) initialized.get(pointOfInterestPanel);
        }

        private synchronized PhotoPanel getPhotoPanel() {
            initialize(photoPanel);
            return (PhotoPanel) initialized.get(photoPanel);
        }

        private synchronized BrowsePanel getBrowsePanel() {
            initialize(browsePanel);
            return (BrowsePanel) initialized.get(browsePanel);
        }

        private void initialize(Component selected) {
            Runnable runnable = lazyInitializers.get(selected);
            if (runnable != null) {
                lazyInitializers.remove(selected);

                startWaitCursor(frame.getRootPane());
                try {
                    runnable.run();
                } catch (Exception e) {
                    log.severe("Cannot initialize tab " + selected + ": " + getLocalizedMessage(e));
                    e.printStackTrace();
                } finally {
                    stopWaitCursor(frame.getRootPane());
                }
            }
        }

        public void stateChanged(ChangeEvent e) {
            Component selected = ((JTabbedPane) e.getSource()).getSelectedComponent();
            initialize(selected);

            final PanelInTab panel = initialized.get(selected);
            final ActionManager actionManager = getContext().getActionManager();
            actionManager.setLocalName(panel.getLocalName());
            frame.getRootPane().setDefaultButton(panel.getDefaultButton());
            panel.initializeSelection();
            invokeLater(() -> {
                panel.getFocusComponent().grabFocus();
                panel.getFocusComponent().requestFocus();
            });
        }
    }

    private class MapSplitPaneListener implements PropertyChangeListener {
        private int location = -1;

        public void propertyChange(PropertyChangeEvent e) {
            if (!isMapViewAvailable()) {
                return;
            }

            if (e.getPropertyName().equals(DIVIDER_LOCATION_PROPERTY)) {
                if (mapSplitPane.getDividerLocation() != location) {
                    dividerChanged(mapSplitPane.getDividerLocation());
                }
            }
        }

        private void dividerChanged(int newValue) {
            this.location = newValue;
            getMapView().resize();
            preferences.putInt(MAP_DIVIDER_LOCATION_PREFERENCE, newValue);
            double newRatio = new Integer(newValue).doubleValue() / contentPane.getWidth();
            preferences.putDouble(MAP_DIVIDER_RATIO_PREFERENCE, newRatio);
            log.fine("Changed map divider to " + newValue + " and ratio " + newRatio);
            enableActions();
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
                    dividerChanged((Integer) e.getOldValue(), profileSplitPane.getDividerLocation());
                }
            }
        }

        private void dividerChanged(int oldValue, int newValue) {
            this.location = newValue;
            if (isMapViewAvailable()) {
                // make sure the one touch expandable to minimize the map works fine
                if (location == 1) {
                    getMapView().getComponent().setVisible(false);
                } else if (oldValue == 1) {
                    getMapView().getComponent().setVisible(true);
                }
                getMapView().resize();
            }
            preferences.putInt(PROFILE_DIVIDER_LOCATION_PREFERENCE, newValue);
            double newRatio = new Integer(newValue).doubleValue() / contentPane.getHeight();
            preferences.putDouble(PROFILE_DIVIDER_RATIO_PREFERENCE, newRatio);
            log.fine("Changed profile divider to " + newValue + " and ratio " + newRatio);
            enableActions();
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
        RouteFeedback routeFeedback = new RouteFeedback(System.getProperty("feedback", "https://www.routeconverter.com/feedback/"), getApiUrl(), RouteConverter.getInstance().getCredentials());
        routeServiceOperator = new RouteServiceOperator(getFrame(), routeFeedback);
        updateChecker = new UpdateChecker(routeFeedback);
        DownloadManager downloadManager = new DownloadManager(new File(getApplicationDirectory(), getEditionId() + "-queue.xml"));
        downloadManager.addDownloadListener(new ChecksumSender());
        downloadManager.addDownloadListener(new DownloadNotifier());
        dataSourceManager = new DataSourceManager(downloadManager);
        timeZoneModel.addChangeListener(e -> {
            ColumbusV1000Device.setTimeZone(timeZoneModel.getTimeZoneId()); // for TimeAlbum
        });
        tileServerMapManager = new TileServerMapManager(getTileServersDirectory());
        routingServiceFacade.addRoutingServiceFacadeListener(new RoutingServiceFacadeNotifier());
     }

    protected void initializeActions() {
        ActionManager actionManager = getContext().getActionManager();
        actionManager.setLocalName(POSITIONS);
        actionManager.register("exit", new ExitAction());
        actionManager.register("print-map", new PrintMapAction());
        actionManager.register("print-profile", new PrintProfileAction());
        actionManager.register("find-place", new FindPlaceAction());
        actionManager.register("show-map-and-positionlist", new ShowMapAndPositionListAction());
        actionManager.register("show-profile", new ShowProfileAction());
        actionManager.register("maximize-map", new MoveSplitPaneDividersAction(mapSplitPane, MAX_VALUE, profileSplitPane, MAX_VALUE));
        actionManager.register("maximize-positionlist", new MoveSplitPaneDividersAction(mapSplitPane, 0, profileSplitPane, MAX_VALUE));
        actionManager.register("show-all-positions-on-map", new ShowAllPositionsOnMapAction());
        actionManager.registerGlobal("delete");
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
        actionManager.register("show-about", createAboutAction());
    }

    private void initializeMenus() {
        JMenu xAxisModeMenu = findMenu(getContext().getMenuBar(), "view", "show-profile-x-axis");
        new XAxisModeMenu(xAxisModeMenu, getProfileModeModel());
        JMenu yAxisModeMenu = findMenu(getContext().getMenuBar(), "view", "show-profile-y-axis");
        new YAxisModeMenu(yAxisModeMenu, getProfileModeModel());
        JMenuItem undoMenuItem = findItem(getContext().getMenuBar(), "edit", "undo");
        JMenuItem redoMenuItem = findItem(getContext().getMenuBar(), "edit", "redo");
        new UndoMenu(undoMenuItem, redoMenuItem, getContext().getUndoManager());
        JMenu reopenMenu = findMenu(getContext().getMenuBar(), "file", "reopen");
        new ReopenMenu(reopenMenu, getConvertPanel().getRecentUrlsModel());
    }

    protected SingletonDialogAction createAboutAction() {
        return new ShowAboutRouteConverterAction();
    }

    private void initializeHelp() {
        getContext().setHelpBrokerUrl(System.getProperty("help", "https://www.routeconverter.com/javahelp.hs"));

        // delay JavaHelp initialization
        ActionListener actionListener = event -> getContext().getActionManager().run("help-topics", event);
        frame.getRootPane().registerKeyboardAction(actionListener,
                getKeyStroke(VK_HELP, 0), WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        frame.getRootPane().registerKeyboardAction(actionListener,
                getKeyStroke(VK_F1, 0), WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        setHelpIDString(frame.getRootPane(), "home");
        setHelpIDString(browsePanel, "browse-route-catalog");
        setHelpIDString(convertPanel, "convert-gps-data");
        setHelpIDString(mapPanel, "map");
        setHelpIDString(profilePanel, "profile-graph");
    }

    public String getApiUrl() {
        return System.getProperty("api", "https://api.routeconverter.com/");
    }

    private File getTileServersDirectory() {
        return getApplicationDirectory("tileservers");
    }

    private File getDataSourcesDirectory() {
        return getApplicationDirectory("datasources");
    }

    private void initializeDatasources() {
        try {
            getDataSourceManager().initialize(getEditionId(), getDataSourcesDirectory());
        } catch (Exception e) {
            log.warning("Could not initialize datasource manager: " + e);
            getContext().getNotificationManager().showNotification(MessageFormat.format(
                    getBundle().getString("datasource-initialization-error"), getLocalizedMessage(e)), null);
        }

        initializeElevationServices();
        initializeGeocodingServices();
        initializeRoutingServices();

        // make sure the queue is loaded before any components uses it
        try {
            getDownloadManager().loadQueue();
        } catch (Exception e) {
            log.warning("Could not load download manager queue: " + e);
            getContext().getNotificationManager().showNotification(MessageFormat.format(
                    getBundle().getString("datasource-initialization-error"), getLocalizedMessage(e)), null);
        }

        new Thread(() -> {
            scanLocalMapsAndThemes();
            scanRemoteMapsAndThemes();
            installBackgroundMap();

            try {
                getDataSourceManager().update(getEditionId(), getApiUrl(), getDataSourcesDirectory());
            } catch (Exception e) {
                log.warning(format("Could not update datasource manager: %s, %s", e, printStackTrace(e)));
                getContext().getNotificationManager().showNotification(MessageFormat.format(
                        getBundle().getString("datasource-update-error"), getLocalizedMessage(e)), null);
            }

            updateElevationServices();
            updateRoutingServices();
            downloadDependencies();

            scanRemoteMapsAndThemes();
            scanForFilesMissingInQueue();
            scanForOutdatedFilesInQueue();
        }, "DataSourceUpdater").start();
    }

    protected abstract void initializeElevationServices();

    protected abstract void updateElevationServices();

    protected abstract void initializeGeocodingServices();

    protected abstract void initializeRoutingServices();

    protected abstract void updateRoutingServices();

    protected void downloadDependencies() {
        if (isMac() || isWindows())
            getDownloadManager().executeDownload("GPSBabel for " + getOperationSystem(),
                    "https://static.routeconverter.com/thirdparty/" + "gpsbabel-" + getOperationSystem() + ".zip",
                    Extract, getApplicationDirectory("thirdparty/gpsbabel"), null);
    }

    protected abstract void scanLocalMapsAndThemes();
    protected abstract void installBackgroundMap();
    protected abstract void scanRemoteMapsAndThemes();

    private void scanForFilesMissingInQueue() {
        // scan for files that are not in the queue but in the file system and put them in the queue if they're in a datasource
        try {
            getDataSourceManager().scanForFilesMissingInQueue();
        } catch (IOException e) {
            log.warning("Could not scan for files missing in queue: " + e);
            getContext().getNotificationManager().showNotification(MessageFormat.format(
                    getBundle().getString("scan-error"), getLocalizedMessage(e)), null);
        }
    }

    private void scanForOutdatedFilesInQueue() {
        // scan over queue to search for downloads that need to be updated and mark them as outdated
        try {
            getDownloadManager().scanForOutdatedFilesInQueue();
        } catch (IOException e) {
            log.warning("Could not scan for outdates files in queue: " + e);
            getContext().getNotificationManager().showNotification(MessageFormat.format(
                    getBundle().getString("scan-error"), getLocalizedMessage(e)), null);
        }
    }


    private class PrintMapAction extends FrameAction {
        public void run() {
            String title = getConvertPanel().getUrlModel().getShortUrl() + " / " + getConvertPanel().getFormatAndRoutesModel().getSelectedRoute().getName();
            getMapView().print(title);
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
            if (location > frame.getHeight() - 200) {
                location = frame.getHeight() - 200;
            }
            profileSplitPane.setDividerLocation(location);
        }
    }

    private class ShowAllPositionsOnMapAction extends FrameAction {
        public void run() {
            if (isMapViewAvailable()) {
                getMapView().showAllPositions();
            }
        }
    }

    private class PrintProfileAction extends FrameAction {
        public void run() {
            profileView.print();
        }
    }

}
