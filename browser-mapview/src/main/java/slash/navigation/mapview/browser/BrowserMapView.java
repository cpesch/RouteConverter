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

package slash.navigation.mapview.browser;

import slash.common.io.TokenResolver;
import slash.common.type.CompactCalendar;
import slash.navigation.base.*;
import slash.navigation.columbus.ColumbusGpsBinaryFormat;
import slash.navigation.columbus.ColumbusGpsFormat;
import slash.navigation.common.BoundingBox;
import slash.navigation.common.NavigationPosition;
import slash.navigation.common.PositionPair;
import slash.navigation.common.SimpleNavigationPosition;
import slash.navigation.converter.gui.models.*;
import slash.navigation.gui.Application;
import slash.navigation.mapview.AbstractMapViewListener;
import slash.navigation.mapview.MapView;
import slash.navigation.mapview.MapViewCallback;
import slash.navigation.mapview.MapViewListener;
import slash.navigation.mapview.tileserver.TileServerService;
import slash.navigation.mapview.tileserver.binding.CopyrightType;
import slash.navigation.mapview.tileserver.binding.TileServerType;
import slash.navigation.nmn.NavigatingPoiWarnerFormat;

import javax.swing.event.*;
import javax.xml.bind.JAXBException;
import java.awt.*;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.*;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.Boolean.parseBoolean;
import static java.lang.Character.isLetterOrDigit;
import static java.lang.Character.isWhitespace;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.lang.System.currentTimeMillis;
import static java.lang.Thread.sleep;
import static java.util.Arrays.asList;
import static java.util.Arrays.sort;
import static java.util.Calendar.SECOND;
import static java.util.concurrent.Executors.newCachedThreadPool;
import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static javax.swing.JOptionPane.ERROR_MESSAGE;
import static javax.swing.JOptionPane.showMessageDialog;
import static javax.swing.SwingUtilities.invokeLater;
import static javax.swing.event.ListDataEvent.CONTENTS_CHANGED;
import static javax.swing.event.TableModelEvent.*;
import static slash.common.helpers.ExceptionHelper.getLocalizedMessage;
import static slash.common.helpers.ThreadHelper.safeJoin;
import static slash.common.io.Externalization.extractFile;
import static slash.common.io.Transfer.*;
import static slash.common.type.CompactCalendar.fromCalendar;
import static slash.common.type.HexadecimalNumber.encodeByte;
import static slash.navigation.base.RouteCharacteristics.*;
import static slash.navigation.base.WaypointType.*;
import static slash.navigation.converter.gui.models.CharacteristicsModel.IGNORE;
import static slash.navigation.converter.gui.models.FixMapMode.Automatic;
import static slash.navigation.converter.gui.models.FixMapMode.Yes;
import static slash.navigation.converter.gui.models.PositionColumns.*;
import static slash.navigation.googlemaps.GoogleMapsAPIKey.getAPIKey;
import static slash.navigation.gui.events.Range.asRange;
import static slash.navigation.gui.helpers.JTableHelper.isFirstToLastRow;
import static slash.navigation.mapview.MapViewConstants.*;
import static slash.navigation.mapview.browser.TransformUtil.delta;
import static slash.navigation.mapview.browser.TransformUtil.isPositionInChina;

/**
 * Base implementation for a browser-based map view.
 *
 * @author Christian Pesch
 */

public abstract class BrowserMapView implements MapView {
    protected static final Preferences preferences = Preferences.userNodeForPackage(BrowserMapView.class);
    protected static final Logger log = Logger.getLogger(MapView.class.getName());
    private static final String RESOURCES_PACKAGE = "slash/navigation/mapview/browser/";

    private static final String MAP_TYPE_PREFERENCE = "mapType";
    protected static final String DEBUG_PREFERENCE = "debug";
    private static final String CLEAN_ELEVATION_ON_MOVE_PREFERENCE = "cleanElevationOnMove";
    private static final String COMPLEMENT_ELEVATION_ON_MOVE_PREFERENCE = "complementElevationOnMove";
    private static final String CLEAN_TIME_ON_MOVE_PREFERENCE = "cleanTimeOnMove";
    private static final String COMPLEMENT_TIME_ON_MOVE_PREFERENCE = "complementTimeOnMove";
    private static final String MOVE_COMPLETE_SELECTION_PREFERENCE = "moveCompleteSelection";
    private static final String CENTER_LATITUDE_PREFERENCE = "centerLatitude";
    private static final String CENTER_LONGITUDE_PREFERENCE = "centerLongitude";
    private static final String CENTER_ZOOM_PREFERENCE = "centerZoom";

    private PositionsModel positionsModel;
    private PositionsSelectionModel positionsSelectionModel;
    private CharacteristicsModel characteristicsModel;
    private List<NavigationPosition> lastSelectedPositions = new ArrayList<>();
    private int[] selectedPositionIndices = new int[0];
    private List<NavigationPosition> selectedPositions = new ArrayList<>();
    private int lastZoom = -1;

    private ServerSocket callbackListenerServerSocket;
    private Thread positionListUpdater, selectionUpdater, callbackListener, callbackPoller;

    protected final Object notificationMutex = new Object();
    protected boolean initialized = false;
    private boolean running = true, haveToInitializeMapOnFirstStart = true, haveToRepaintSelectionImmediately = false,
            haveToRepaintRouteImmediately = false, haveToRecenterMap = false,
            haveToUpdateRoute = false, haveToReplaceRoute = false,
            haveToRepaintSelection = false, ignoreNextZoomCallback = false;

    private BooleanModel showAllPositionsAfterLoading;
    private BooleanModel recenterAfterZooming;
    private BooleanModel showCoordinates;
    private BooleanModel showWaypointDescription;
    private FixMapModeModel fixMapModeModel;
    private ColorModel routeColorModel;
    private ColorModel trackColorModel;
    private UnitSystemModel unitSystemModel;
    private GoogleMapsServerModel googleMapsServerModel;

    private PositionsModelListener positionsModelListener = new PositionsModelListener();
    private CharacteristicsModelListener characteristicsModelListener = new CharacteristicsModelListener();
    private MapViewCallbackListener mapViewCallbackListener = new MapViewCallbackListener();
    private ShowCoordinatesListener showCoordinatesListener = new ShowCoordinatesListener();
    private ShowWaypointDescriptionListener showWaypointDescriptionListener = new ShowWaypointDescriptionListener();
    private RepaintPositionListListener repaintPositionListListener = new RepaintPositionListListener();
    private UnitSystemListener unitSystemListener = new UnitSystemListener();
    private GoogleMapsServerListener googleMapsServerListener = new GoogleMapsServerListener();

    private String routeUpdateReason = "?", selectionUpdateReason = "?";
    protected MapViewCallback mapViewCallback;
    private PositionReducer positionReducer;
    private final ExecutorService executor = newCachedThreadPool();
    private int overQueryLimitCount = 0, zeroResultsCount = 0;

    // initialization

    public void initialize(PositionsModel positionsModel,
                           PositionsSelectionModel positionsSelectionModel,
                           CharacteristicsModel characteristicsModel,
                           MapViewCallback mapViewCallback,
                           BooleanModel showAllPositionsAfterLoading,
                           BooleanModel recenterAfterZooming,
                           BooleanModel showCoordinates,
                           BooleanModel showWaypointDescription,
                           FixMapModeModel fixMapModeModel,
                           ColorModel aRouteColorModel,
                           ColorModel aTrackColorModel,
                           UnitSystemModel unitSystemModel,
                           GoogleMapsServerModel googleMapsServerModel) {
        this.positionsModel = positionsModel;
        this.positionsSelectionModel = positionsSelectionModel;
        this.characteristicsModel = characteristicsModel;
        this.mapViewCallback = mapViewCallback;
        this.mapViewCallback = mapViewCallback;
        this.showAllPositionsAfterLoading = showAllPositionsAfterLoading;
        this.recenterAfterZooming = recenterAfterZooming;
        this.showCoordinates = showCoordinates;
        this.showWaypointDescription = showWaypointDescription;
        this.fixMapModeModel = fixMapModeModel;
        this.routeColorModel = aRouteColorModel;
        this.trackColorModel = aTrackColorModel;
        this.unitSystemModel = unitSystemModel;
        this.googleMapsServerModel = googleMapsServerModel;

        initializeBrowser();

        positionsModel.addTableModelListener(positionsModelListener);
        characteristicsModel.addListDataListener(characteristicsModelListener);
        mapViewCallback.addRoutingServiceChangeListener(mapViewCallbackListener);
        showCoordinates.addChangeListener(showCoordinatesListener);
        showWaypointDescription.addChangeListener(showWaypointDescriptionListener);
        fixMapModeModel.addChangeListener(repaintPositionListListener);
        routeColorModel.addChangeListener(repaintPositionListListener);
        trackColorModel.addChangeListener(repaintPositionListListener);
        unitSystemModel.addChangeListener(unitSystemListener);
        googleMapsServerModel.addChangeListener(googleMapsServerListener);

        positionReducer = new PositionReducer(new PositionReducer.Callback() {
            public int getZoom() {
                return BrowserMapView.this.getZoom();
            }

            public NavigationPosition getNorthEastBounds() {
                return BrowserMapView.this.getNorthEastBounds();
            }

            public NavigationPosition getSouthWestBounds() {
                return BrowserMapView.this.getSouthWestBounds();
            }
        });
    }

    protected abstract void initializeBrowser();
    protected abstract void initializeWebPage();

    protected String getGoogleMapsServerApiUrl() {
        return googleMapsServerModel.getGoogleMapsServer().getApiUrl();
    }

    protected String prepareWebPage() throws IOException {
        final String language = Locale.getDefault().getLanguage().toLowerCase();
        final String country = Locale.getDefault().getCountry().toLowerCase();
        final TileServerService tileServerService = loadAllTileServers(mapViewCallback.getTileServersDirectory());
        File html = extractFile(RESOURCES_PACKAGE + "routeconverter.html", country, new TokenResolver() {
            public String resolveToken(String tokenName) {
                if (tokenName.equals("language"))
                    return language;
                if (tokenName.equals("country"))
                    return country;
                if (tokenName.equals("mapserverapiurl"))
                    return getGoogleMapsServerApiUrl();
                if (tokenName.equals("mapserverfileurl"))
                    return googleMapsServerModel.getGoogleMapsServer().getFileUrl();
                if (tokenName.equals("maptype"))
                    return getMapType();
                if (tokenName.equals("mapsapikey"))
                    return getAPIKey("map");
                if (tokenName.equals("tileservers1"))
                    return registerTileServers(tileServerService, true);
                if (tokenName.equals("tileservers2"))
                    return registerTileServers(tileServerService, false);
                if (tokenName.equals("menuItems"))
                    return registerMenuItems();
                return tokenName;
            }
        });
        if (html == null)
            throw new IllegalArgumentException("Cannot extract routeconverter.html");

        extractFile(RESOURCES_PACKAGE + "jquery.min.js");
        extractFile(RESOURCES_PACKAGE + "contextmenu.js");
        extractFile(RESOURCES_PACKAGE + "keydragzoom.js");
        extractFile(RESOURCES_PACKAGE + "label.js");
        extractFile(RESOURCES_PACKAGE + "latlngcontrol.js");

        return html.toURI().toURL().toExternalForm();
    }

    protected void tryToInitialize(int count, long start) {
        boolean initialized = getComponent() != null && isMapInitialized();
        synchronized (this) {
            this.initialized = initialized;
        }
        log.fine("Initialized map: " + initialized);

        if (isInitialized()) {
            runBrowserInteractionCallbacksAndTests(start);
        } else {
            long end = currentTimeMillis();
            int timeout = count++ * 100;
            if (timeout > 3000)
                timeout = 3000;
            log.info("Failed to initialize map since " + (end - start) + " ms, sleeping for " + timeout + " ms");

            try {
                sleep(timeout);
            } catch (InterruptedException e) {
                // intentionally left empty
            }
            tryToInitialize(count, start);
        }
    }

    protected void runBrowserInteractionCallbacksAndTests(long start) {
        long end = currentTimeMillis();
        log.fine("Starting browser interaction, callbacks and tests after " + (end - start) + " ms");
        initializeAfterLoading();
        initializeBrowserInteraction();
        initializeCallbackListener();
        checkLocalhostResolution();
        checkCallback();
        setDegreeFormat();
        setShowCoordinates();
        end = currentTimeMillis();
        log.fine("Browser interaction is running after " + (end - start) + " ms");
    }

    protected abstract boolean isMapInitialized();

    protected void initializeAfterLoading() {
        resize();
        update(true, false);
    }

    private Throwable initializationCause = null;

    public Throwable getInitializationCause() {
        return initializationCause;
    }

    protected void setInitializationCause(Throwable initializationCause) {
        this.initializationCause = initializationCause;
    }

    public boolean isInitialized() {
        synchronized (this) {
            return initialized;
        }
    }

    public boolean isDownload() {
        return false;
    }

    protected void initializeBrowserInteraction() {
        getComponent().addComponentListener(new ComponentListener() {
            public void componentResized(ComponentEvent e) {
                resize();
            }

            public void componentMoved(ComponentEvent e) {
            }

            public void componentShown(ComponentEvent e) {
            }

            public void componentHidden(ComponentEvent e) {
            }
        });

        positionListUpdater = new Thread(new Runnable() {
            @SuppressWarnings("unchecked")
            public void run() {
                long lastTime = 0;
                boolean recenter;
                while (true) {
                    List<NavigationPosition> copiedPositions;
                    synchronized (notificationMutex) {
                        try {
                            notificationMutex.wait(1000);
                        } catch (InterruptedException e) {
                            // ignore this
                        }

                        if (!running)
                            return;
                        if (!hasPositions())
                            continue;
                        if (!isVisible())
                            continue;

                        /*
                           Update conditions:

                           - new route was loaded
                             - clear cache
                             - center map
                             - set zoom level according to route bounds
                             - repaint immediately
                           - user has moved position
                             - clear cache
                             - stay on current zoom level
                             - center map to position
                             - repaint
                           - user has removed position
                             - clear cache
                             - stay on current zoom level
                             - repaint
                           - user has zoomed map
                             - repaint if zooming into the map as it reveals more details
                           - user has moved map
                             - repaint if moved
                         */
                        long currentTime = currentTimeMillis();
                        if (haveToRepaintRouteImmediately ||
                                haveToReplaceRoute ||
                                (haveToUpdateRoute && (currentTime - lastTime > 5 * 1000))) {
                            log.info("Woke up to update route: " + routeUpdateReason +
                                    " haveToUpdateRoute:" + haveToUpdateRoute +
                                    " haveToReplaceRoute:" + haveToReplaceRoute +
                                    " haveToRepaintRouteImmediately:" + haveToRepaintRouteImmediately);
                            copiedPositions = new ArrayList<>(positionsModel.getRoute().getPositions());
                            recenter = haveToReplaceRoute;
                            haveToUpdateRoute = false;
                            haveToReplaceRoute = false;
                            haveToRepaintRouteImmediately = false;
                        } else
                            continue;
                    }

                    setCenterOfMap(copiedPositions, recenter);
                    RouteCharacteristics characteristics = positionsModel.getRoute().getCharacteristics();
                    List<NavigationPosition> render = positionReducer.reducePositions(copiedPositions, characteristics, showWaypointDescription.getBoolean());
                    switch (characteristics) {
                        case Route:
                            addDirectionsToMap(render);
                            break;
                        case Track:
                            addPolylinesToMap(render, copiedPositions);
                            break;
                        case Waypoints:
                            addMarkersToMap(render);
                            break;
                        default:
                            throw new IllegalArgumentException("RouteCharacteristics " + characteristics + " is not supported");
                    }
                    log.info("Position list updated for " + render.size() + " positions of type " + characteristics +
                            ", reason: " + routeUpdateReason + ", recentering: " + recenter);
                    lastTime = currentTimeMillis();
                }
            }
        }, "MapViewPositionListUpdater");
        positionListUpdater.start();

        selectionUpdater = new Thread(new Runnable() {
            @SuppressWarnings("unchecked")
            public void run() {
                long lastTime = 0;
                while (true) {
                    int[] copiedSelectedPositionIndices;
                    List<NavigationPosition> copiedPositions;
                    boolean recenter;
                    synchronized (notificationMutex) {
                        try {
                            notificationMutex.wait(250);
                        } catch (InterruptedException e) {
                            // ignore this
                        }

                        if (!running)
                            return;
                        if (!hasPositions())
                            continue;
                        if (!isVisible())
                            continue;

                        long currentTime = currentTimeMillis();
                        if (haveToRecenterMap || haveToRepaintSelectionImmediately ||
                                (haveToRepaintSelection && (currentTime - lastTime > 500))) {
                            log.fine("Woke up to update selected positions: " + selectionUpdateReason +
                                    " haveToRepaintSelection: " + haveToRepaintSelection +
                                    " haveToRepaintSelectionImmediately: " + haveToRepaintSelectionImmediately +
                                    " haveToRecenterMap: " + haveToRecenterMap);
                            recenter = haveToRecenterMap;
                            haveToRecenterMap = false;
                            haveToRepaintSelectionImmediately = false;
                            haveToRepaintSelection = false;
                            copiedSelectedPositionIndices = new int[selectedPositionIndices.length];
                            System.arraycopy(selectedPositionIndices, 0, copiedSelectedPositionIndices, 0, copiedSelectedPositionIndices.length);
                            copiedPositions = new ArrayList<>(positionsModel.getRoute().getPositions());
                        } else
                            continue;
                    }

                    List<NavigationPosition> render = new ArrayList<>(positionReducer.reduceSelectedPositions(copiedPositions, copiedSelectedPositionIndices));
                    render.addAll(selectedPositions);
                    NavigationPosition centerPosition = render.size() > 0 ? new BoundingBox(render).getCenter() : null;
                    selectPositions(render, recenter ? centerPosition : null);
                    log.info("Selected positions updated for " + render.size() + " positions , reason: " +
                            selectionUpdateReason + ", recentering: " + recenter + " to: " + centerPosition);
                    lastTime = currentTimeMillis();
                }
            }
        }, "MapViewSelectionUpdater");
        selectionUpdater.start();
    }

    private ServerSocket createCallbackListenerServerSocket() {
        try {
            ServerSocket serverSocket = new ServerSocket(0, 0, InetAddress.getByAddress(new byte[]{127, 0, 0, 1}));
            serverSocket.setSoTimeout(1000);
            int port = serverSocket.getLocalPort();
            log.info("Map listens on port " + port + " for callbacks");
            setCallbackListenerPort(port);
            return serverSocket;
        } catch (IOException e) {
            log.severe("Cannot open callback listener socket: " + e);
            return null;
        }
    }

    protected void initializeCallbackListener() {
        callbackListenerServerSocket = createCallbackListenerServerSocket();
        if (callbackListenerServerSocket == null)
            return;

        callbackListener = new Thread(new Runnable() {
            public void run() {
                while (true) {
                    synchronized (notificationMutex) {
                        if (!running) {
                            return;
                        }
                    }

                    try {
                        final Socket socket = callbackListenerServerSocket.accept();
                        executor.execute(new Runnable() {
                            public void run() {
                                try {
                                    processStream(socket);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                    log.severe("Cannot process stream from callback listener socket: " + e);
                                }
                            }
                        });
                    } catch (SocketTimeoutException e) {
                        // intentionally left empty
                    } catch (IOException e) {
                        synchronized (notificationMutex) {
                            //noinspection ConstantConditions
                            if (running) {
                                log.severe("Cannot accept callback listener socket: " + e);
                            }
                        }
                    }
                }
            }
        }, "MapViewCallbackListener");
        callbackListener.start();
    }

    protected void initializeCallbackPoller() {
        callbackPoller = new Thread(new Runnable() {
            public void run() {
                while (true) {
                    synchronized (notificationMutex) {
                        if (!running) {
                            return;
                        }
                    }

                    String callbacks = trim(getCallbacks());
                    if (callbacks != null) {
                        String[] lines = callbacks.split("--");
                        for (String line : lines) {
                            processCallback(line);
                        }
                    }

                    try {
                        sleep(250);
                    } catch (InterruptedException e) {
                        // intentionally left empty
                    }
                }
            }
        }, "MapViewCallbackPoller");
        callbackPoller.start();
    }

    protected void checkLocalhostResolution() {
        try {
            InetAddress localhost = InetAddress.getByName("localhost");
            log.info("localhost is resolved to: " + localhost);
            String localhostName = localhost.getHostAddress();
            log.info("IP of localhost is: " + localhostName);
            if (!localhostName.equals("127.0.0.1"))
                throw new Exception("localhost does not resolve to 127.0.0.1");

            InetAddress ip = InetAddress.getByAddress(new byte[]{127, 0, 0, 1});
            log.info("127.0.0.1 is resolved to: " + ip);
            String ipName = localhost.getHostName();
            log.info("Name of 127.0.0.1 is: " + ipName);
            if (!ipName.equals("localhost"))
                throw new Exception("127.0.0.1 does not resolve to localhost");
        } catch (Exception e) {
            final String message = "Probably faulty network setup: " + getLocalizedMessage(e) + ".\nPlease check your network settings.";
            log.severe(message);
            invokeLater(new Runnable() {
                public void run() {
                    showMessageDialog(getComponent(), message, "Error", ERROR_MESSAGE);
                }
            });
        }
    }

    protected void checkCallback() {
        final Boolean[] receivedCallback = new Boolean[1];
        receivedCallback[0] = false;

        final MapViewListener callbackWaiter = new AbstractMapViewListener() {
            public void receivedCallback(int port) {
                synchronized (receivedCallback) {
                    receivedCallback[0] = true;
                    receivedCallback.notifyAll();
                }
            }
        };

        executor.execute(new Runnable() {
            public void run() {
                addMapViewListener(callbackWaiter);
                try {
                    executeScript("checkCallbackListenerPort();");

                    long start = currentTimeMillis();
                    while (true) {
                        synchronized (receivedCallback) {
                            if (receivedCallback[0]) {
                                long end = currentTimeMillis();
                                log.info("Received callback from browser after " + (end - start) + " milliseconds");
                                break;
                            }
                        }

                        if (start + 5000 < currentTimeMillis())
                            break;

                        try {
                            sleep(50);
                        } catch (InterruptedException e) {
                            // intentionally left empty
                        }
                    }

                    synchronized (receivedCallback) {
                        if (!receivedCallback[0]) {
                            setCallbackListenerPort(-1);
                            initializeCallbackPoller();
                            log.warning("Switched from callback to polling the browser");
                        }
                    }
                } finally {
                    removeMapViewListener(callbackWaiter);
                }
            }
        });
    }

    // tile servers

    private static final List<String> GOOGLE_MAP_TYPES = asList("ROADMAP", "SATELLITE", "HYBRID", "TERRAIN");

    private String getMapType() {
        return preferences.get(MAP_TYPE_PREFERENCE, "google.maps.MapTypeId.ROADMAP");
    }

    private boolean isGoogleFixMap() {
        String mapType = getMapType();
        return mapType != null && GOOGLE_MAP_TYPES.contains(mapType.toUpperCase());
    }

    private static final String DOT_XML = ".xml";

    private TileServerService loadAllTileServers(java.io.File directory) {
        TileServerService result = new TileServerService();
        java.io.File[] files = directory.listFiles(new FilenameFilter() {
            public boolean accept(java.io.File dir, String name) {
                return name.endsWith(DOT_XML);
            }
        });

        if (files != null) {
            for (File file : files) {
                try {
                    try (InputStream inputStream = new FileInputStream(file)) {
                        result.load(inputStream);
                    }
                } catch (IOException | JAXBException e) {
                    log.severe("Could not parse tile server definitions from " + file + ": " + getLocalizedMessage(e));
                }
            }
        }
        return result;
    }

    private String registerTileServers(TileServerService tileServerService, boolean register) {
        StringBuilder buffer = new StringBuilder();

        if (register) {
            for (String tileServerId : GOOGLE_MAP_TYPES)
                buffer.append("mapTypeIds.push(google.maps.MapTypeId.").append(tileServerId).append("); ").
                        append("mapCopyrights[google.maps.MapTypeId.").append(tileServerId).append("] = \"Google\";\n");
        }

        for (TileServerType tileServer : tileServerService.getTileServers()) {
            if (tileServer.getActive() != null && !tileServer.getActive())
                continue;

            if (register) {
                CopyrightType copyrightType = tileServer.getCopyright();
                buffer.append("mapTypeIds.push(\"").append(tileServer.getId()).append("\"); ").
                        append("mapCopyrights[\"").append(tileServer.getId()).append("\"] = \"").
                        append(copyrightType != null ? copyrightType.value() : "unknown").append("\";\n");
            }
            else
                buffer.append("map.mapTypes.set(\"").append(tileServer.getId()).append("\", new google.maps.ImageMapType({\n").
                        append("  getTileUrl: function(coordinates, zoom) {\n").
                        append("    return ").append(trim(trimLineFeeds(tileServer.getValue()))).append(";\n").
                        append("  },\n").
                        append("  tileSize: DEFAULT_TILE_SIZE,\n").
                        append("  minZoom: ").append(tileServer.getMinZoom()).append(",\n").
                        append("  maxZoom: ").append(tileServer.getMaxZoom()).append(",\n").
                        append("  alt: \"").append(tileServer.getName()).append("\",\n").
                        append("  name: \"").append(tileServer.getId()).append("\"\n").
                        append("}));\n");
        }

        return buffer.toString();
    }

    private static final String[] MENU_ITEM_KEYS = new String[]{ "center-here-action", "delete-action",
            "new-position-action", "select-position-action", "zoom-in-action", "zoom-out-action" };

    private String registerMenuItems() {
        StringBuilder buffer = new StringBuilder();

        ResourceBundle bundle = Application.getInstance().getContext().getBundle();
        for (String menuItemKey : MENU_ITEM_KEYS)
            buffer.append("menuItems[\"").append(menuItemKey).append("\"] = ").
                    append("\"").append(bundle.getString(menuItemKey)).append("\";\n");

        return buffer.toString();
   }

    private boolean isColumbusTrack() {
        BaseNavigationFormat format = positionsModel.getRoute().getFormat();
        return format instanceof ColumbusGpsFormat || format instanceof ColumbusGpsBinaryFormat;
    }

    // resizing

    private boolean hasBeenResizedToInvisible = false;

    public void resize() {
        if (!isInitialized() || !getComponent().isShowing())
            return;

        new Thread(new Runnable() {
            public void run() {
                synchronized (notificationMutex) {
                    // if map is not visible remember to update and resize it again
                    // once the map becomes visible again
                    if (!isVisible()) {
                        hasBeenResizedToInvisible = true;
                    } else if (hasBeenResizedToInvisible) {
                        hasBeenResizedToInvisible = false;
                        update(true, false);
                    }
                    resizeMap();
                }
            }
        }, "BrowserResizer").start();
    }

    private int lastWidth = -1, lastHeight = -1;

    private void resizeMap() {
        synchronized (notificationMutex) {
            int width = max(getComponent().getWidth(), 0);
            int height = max(getComponent().getHeight(), 0);
            if (width != lastWidth || height != lastHeight) {
                executeScript("resize(" + width + "," + height + ");");
            }
            lastWidth = width;
            lastHeight = height;
        }
    }

    // disposal

    public void dispose() {
        if(positionsModel != null) {
            positionsModel.removeTableModelListener(positionsModelListener);
            characteristicsModel.removeListDataListener(characteristicsModelListener);
            mapViewCallback.removeRoutingServiceChangeListener(mapViewCallbackListener);
            showCoordinates.removeChangeListener(showCoordinatesListener);
            showWaypointDescription.removeChangeListener(showWaypointDescriptionListener);
            fixMapModeModel.removeChangeListener(repaintPositionListListener);
            routeColorModel.removeChangeListener(repaintPositionListListener);
            trackColorModel.removeChangeListener(repaintPositionListListener);
            unitSystemModel.removeChangeListener(unitSystemListener);
            googleMapsServerModel.removeChangeListener(googleMapsServerListener);
        }

        long start = currentTimeMillis();
        synchronized (notificationMutex) {
            running = false;
            notificationMutex.notifyAll();
        }

        if (selectionUpdater != null) {
            try {
                safeJoin(selectionUpdater);
            } catch (InterruptedException e) {
                // intentionally left empty
            }
            long end = currentTimeMillis();
            log.info("PositionUpdater stopped after " + (end - start) + " ms");
        }

        if (positionListUpdater != null) {
            try {
                safeJoin(positionListUpdater);
            } catch (InterruptedException e) {
                // intentionally left empty
            }
            long end = currentTimeMillis();
            log.info("RouteUpdater stopped after " + (end - start) + " ms");
        }

        if (callbackListenerServerSocket != null) {
            try {
                callbackListenerServerSocket.close();
            } catch (IOException e) {
                log.warning("Cannot close callback listener socket:" + e);
            }
            long end = currentTimeMillis();
            log.info("CallbackListenerSocket stopped after " + (end - start) + " ms");
        }

        if (callbackListener != null) {
            try {
                safeJoin(callbackListener);
            } catch (InterruptedException e) {
                // intentionally left empty
            }
            long end = currentTimeMillis();
            log.info("CallbackListener stopped after " + (end - start) + " ms");
        }

        if (callbackPoller != null && callbackPoller.isAlive()) {
            try {
                safeJoin(callbackPoller);
            } catch (InterruptedException e) {
                // intentionally left empty
            }
            long end = currentTimeMillis();
            log.info("CallbackPoller stopped after " + (end - start) + " ms");
        }

        executor.shutdownNow();
        insertWaypointsExecutor.shutdownNow();
        long end = currentTimeMillis();
        log.info("Executors stopped after " + (end - start) + " ms");
    }

    // getter and setter

    protected boolean isVisible() {
        return getComponent().getWidth() > 0;
    }

    private boolean hasPositions() {
        synchronized (notificationMutex) {
            return isInitialized() && positionsModel.getRoute().getPositions() != null;
        }
    }

    private void setCallbackListenerPort(int callbackListenerPort) {
        synchronized (notificationMutex) {
            executeScript("setCallbackListenerPort(" + callbackListenerPort + ")");
        }
    }

    public void setSelectedPositions(int[] selectedPositions, boolean replaceSelection) {
        synchronized (notificationMutex) {
            if (replaceSelection)
                this.selectedPositionIndices = selectedPositions;
            else {
                int[] indices = new int[selectedPositionIndices.length + selectedPositions.length];
                System.arraycopy(selectedPositionIndices, 0, indices, 0, selectedPositionIndices.length);
                System.arraycopy(selectedPositions, 0, indices, selectedPositionIndices.length, selectedPositions.length);
                this.selectedPositionIndices = indices;
            }
            this.selectedPositions = new ArrayList<>();
            haveToRecenterMap = selectedPositions.length > 0;
            haveToRepaintSelection = true;
            selectionUpdateReason = "selected " + selectedPositions.length + " positions; " +
                    "replacing selection: " + replaceSelection;
            notificationMutex.notifyAll();
        }
    }

    public void setSelectedPositions(List<NavigationPosition> selectedPositions) {
        synchronized (notificationMutex) {
            this.selectedPositions = selectedPositions;
            this.selectedPositionIndices = new int[0];
            haveToRecenterMap = selectedPositions.size() > 0;
            haveToRepaintSelection = true;
            selectionUpdateReason = "selected " + selectedPositions.size() + " positions without model";
            notificationMutex.notifyAll();
        }
    }

    protected void setShowCoordinates() {
        executeScript("setShowCoordinates(" + showCoordinates.getBoolean() + ");");
    }

    protected void setDegreeFormat() {
        executeScript("setDegreeFormat('" + unitSystemModel.getDegreeFormat() + "');");
    }

    @SuppressWarnings({"unchecked", "Convert2Diamond"})
    public void showAllPositions() {
        setCenterOfMap(new ArrayList<NavigationPosition>(positionsModel.getRoute().getPositions()), true);
    }

    public void showMapBorder(BoundingBox mapBoundingBox) {
        throw new UnsupportedOperationException();
    }

    public NavigationPosition getCenter() {
        if (isInitialized())
            return getCurrentMapCenter();
        else
            return getLastMapCenter();
    }

    private int getZoom() {
        return preferences.getInt(CENTER_ZOOM_PREFERENCE, 2);
    }

    private void setZoom(int zoom) {
        preferences.putInt(CENTER_ZOOM_PREFERENCE, zoom);
    }

    // bounds and center

    protected abstract NavigationPosition getNorthEastBounds();
    protected abstract NavigationPosition getSouthWestBounds();
    protected abstract NavigationPosition getCurrentMapCenter();
    protected abstract Integer getCurrentZoom();
    protected abstract String getCallbacks();

    private NavigationPosition getLastMapCenter() {
        double latitude = preferences.getDouble(CENTER_LATITUDE_PREFERENCE, 35.0);
        double longitude = preferences.getDouble(CENTER_LONGITUDE_PREFERENCE, -25.0);
        return new SimpleNavigationPosition(longitude, latitude);
    }

    protected NavigationPosition parsePosition(String latLngString) {
        String result = executeScriptWithResult(latLngString);
        if (result == null)
            return null;

        StringTokenizer tokenizer = new StringTokenizer(result, ",");
        if (tokenizer.countTokens() != 2)
            return null;

        String latitude = tokenizer.nextToken();
        String longitude = tokenizer.nextToken();
        return parsePosition(latitude, longitude);
    }

    // WGS/GCJ conversion

    private boolean isFixMap(Double longitude, Double latitude) {
        FixMapMode fixMapMode = fixMapModeModel.getFixMapMode();
        return fixMapMode.equals(Yes) || fixMapMode.equals(Automatic) && isGoogleFixMap() && isPositionInChina(longitude, latitude);
    }

    private NavigationPosition parsePosition(String latitudeString, String longitudeString) {
        Double longitude = parseDouble(longitudeString);
        Double latitude = parseDouble(latitudeString);
        if (longitude != null && latitude != null && isFixMap(longitude, latitude)) {
            double[] delta = delta(latitude, longitude);
            longitude -= delta[1];
            latitude -= delta[0];
        }
        return new SimpleNavigationPosition(longitude, latitude);
    }

    private String asCoordinates(NavigationPosition position) {
        Double longitude = position.getLongitude();
        Double latitude = position.getLatitude();
        if (longitude != null && latitude != null && isFixMap(longitude, latitude)) {
            double[] delta = delta(latitude, longitude);
            longitude += delta[1];
            latitude += delta[0];
        }
        return latitude + "," + longitude;
    }

    // draw on map

    protected void update(boolean haveToReplaceRoute, boolean clearPositionReducer) {
        if (!isInitialized() || !getComponent().isShowing())
            return;

        synchronized (notificationMutex) {
            this.haveToUpdateRoute = true;
            routeUpdateReason = "update route";
            if (haveToReplaceRoute) {
                this.haveToReplaceRoute = true;
                routeUpdateReason = "replace route";
                this.haveToRepaintSelection = true;
                selectionUpdateReason = "replace route";
            }
            if (clearPositionReducer)
                positionReducer.clear();
            notificationMutex.notifyAll();
        }
    }

    private void updateRouteButDontRecenter() {
        // repaint route immediately, simulates update(true) without recentering
        synchronized (notificationMutex) {
            haveToRepaintRouteImmediately = true;
            routeUpdateReason = "update route but don't recenter";
            positionReducer.clear();
            notificationMutex.notifyAll();
        }
    }

    private void updateSelection() {
        synchronized (notificationMutex) {
            haveToRepaintSelection = true;
            selectionUpdateReason = "update selection";
            notificationMutex.notifyAll();
        }
    }

    private void removeDirections() {
        executeScript("removeOverlays();\nremoveDirections();");
    }

    String asColor(Color color) {
        return encodeByte((byte) color.getRed()) + encodeByte((byte) color.getGreen()) + encodeByte((byte) color.getBlue());
    }

    private static final float MINIMUM_OPACITY = 0.3f;

    float asOpacity(Color color) {
        return MINIMUM_OPACITY + color.getAlpha() / 256f * (1 - MINIMUM_OPACITY);
    }

    private void addDirectionsToMap(List<NavigationPosition> positions) {
        resetDirections();

        // avoid throwing javascript exceptions if there is nothing to direct
        if (positions.size() < 2) {
            addMarkersToMap(positions);
            return;
        }

        executeScript("removeOverlays();");

        String color = asColor(routeColorModel.getColor());
        float opacity = asOpacity(routeColorModel.getColor());
        int width = preferences.getInt(ROUTE_LINE_WIDTH_PREFERENCE, 5);
        int maximumRouteSegmentLength = positionReducer.getMaximumSegmentLength(Route);
        int directionsCount = ceiling(positions.size(), maximumRouteSegmentLength, false);
        for (int j = 0; j < directionsCount; j++) {
            StringBuilder waypoints = new StringBuilder();
            int start = max(0, j * maximumRouteSegmentLength - 1);
            int end = min(positions.size(), (j + 1) * maximumRouteSegmentLength) - 1;
            for (int i = start + 1; i < end; i++) {
                NavigationPosition position = positions.get(i);
                waypoints.append("{location: new google.maps.LatLng(").append(asCoordinates(position)).append(")}");
                if (i < end - 1)
                    waypoints.append(",");
            }
            NavigationPosition origin = positions.get(start);
            NavigationPosition destination = positions.get(end);
            StringBuilder buffer = new StringBuilder();
            buffer.append("renderDirections({origin: new google.maps.LatLng(").append(asCoordinates(origin)).append("),");
            buffer.append("destination: new google.maps.LatLng(").append(asCoordinates(destination)).append("),");
            buffer.append("waypoints: [").append(waypoints).append("],").
                    append("travelMode: google.maps.DirectionsTravelMode.").append(mapViewCallback.getTravelMode().getName().toUpperCase()).append(",");
            buffer.append("avoidFerries: ").append(mapViewCallback.isAvoidFerries()).append(",");
            buffer.append("avoidHighways: ").append(mapViewCallback.isAvoidHighways()).append(",");
            buffer.append("avoidTolls: ").append(mapViewCallback.isAvoidTolls()).append(",");
            buffer.append("region: \"").append(Locale.getDefault().getCountry().toLowerCase()).append("\"},");
            int startIndex = positionsModel.getIndex(origin);
            buffer.append(startIndex).append(",");
            boolean lastSegment = (j == directionsCount - 1);
            buffer.append(lastSegment).append(",\"#").append(color).append("\",").append(opacity).append(",").append(width).append(");\n");
            try {
                sleep(preferences.getInt("routeSegmentTimeout", 250));
            } catch (InterruptedException e) {
                // intentionally left empty
            }
            executeScript(buffer.toString());
        }

        try {
            sleep(preferences.getInt("routeCompleteTimeout", 1000));
        } catch (InterruptedException e) {
            // intentionally left empty
        }
    }

    private void addPolylinesToMap(final List<NavigationPosition> reducedPositions, List<NavigationPosition> allPositions) {
        // display markers if there is no polyline to show
        if (reducedPositions.size() < 2) {
            addMarkersToMap(reducedPositions);
            return;
        }

        String color = asColor(trackColorModel.getColor());
        float opacity = asOpacity(trackColorModel.getColor());
        int width = preferences.getInt(TRACK_LINE_WIDTH_PREFERENCE, 2);
        int maximumPolylineSegmentLength = positionReducer.getMaximumSegmentLength(Track);
        int polylinesCount = ceiling(reducedPositions.size(), maximumPolylineSegmentLength, true);
        for (int j = 0; j < polylinesCount; j++) {
            StringBuilder latlngs = new StringBuilder();
            int minimum = max(0, j * maximumPolylineSegmentLength - 1);
            int maximum = min(reducedPositions.size(), (j + 1) * maximumPolylineSegmentLength);
            for (int i = minimum; i < maximum; i++) {
                NavigationPosition position = reducedPositions.get(i);
                latlngs.append("new google.maps.LatLng(").append(asCoordinates(position)).append(")");
                if (i < maximum - 1)
                    latlngs.append(",");
            }
            executeScript("addPolyline([" + latlngs + "],\"#" + color + "\"," + opacity + "," + width + ");");
        }

        addWaypointIconsToMap(allPositions);

        removeDirections();
    }

    private void addWaypointIconsToMap(List<NavigationPosition> positions) {
        if (!isColumbusTrack())
            return;

        List<NavigationPosition> reducedPositions = positionReducer.filterVisiblePositions(positions, getZoom());

        StringBuilder icons = new StringBuilder();
        for (int i = 0, c = reducedPositions.size(); i < c; i++) {
            NavigationPosition position = reducedPositions.get(i);
            Wgs84Position wgs84Position = Wgs84Position.class.cast(position);
            WaypointType waypointType = wgs84Position.getWaypointType();
            if (i == c - 1)
                waypointType = End;
            if (i == 0)
                waypointType = Start;

            if (waypointType != null && waypointType != Waypoint)
                icons.append("addWaypointIcon(new google.maps.LatLng(").append(asCoordinates(position)).append("),\"").
                        append(waypointType).append("\");\n");
        }
        executeScript(icons.toString());
    }

    private void addMarkersToMap(List<NavigationPosition> positions) {
        int maximumMarkerSegmentLength = positionReducer.getMaximumSegmentLength(Waypoints);
        int markersCount = ceiling(positions.size(), maximumMarkerSegmentLength, false);
        for (int j = 0; j < markersCount; j++) {
            StringBuilder buffer = new StringBuilder();
            int maximum = min(positions.size(), (j + 1) * maximumMarkerSegmentLength);
            for (int i = j * maximumMarkerSegmentLength; i < maximum; i++) {
                NavigationPosition position = positions.get(i);
                buffer.append("addMarker(new google.maps.LatLng(").append(asCoordinates(position)).append("),").
                        append("\"").append(escape(position.getDescription())).append("\",").
                        append(showWaypointDescription.getBoolean()).append(");\n");
            }
            executeScript(buffer.toString());
        }
        removeDirections();
    }

    private void setCenterOfMap(List<NavigationPosition> positions, boolean recenter) {
        StringBuilder buffer = new StringBuilder();

        boolean fitBoundsToPositions = positions.size() > 0 && recenter;
        if (fitBoundsToPositions) {
            BoundingBox boundingBox = new BoundingBox(positions);
            buffer.append("fitBounds(new google.maps.LatLng(").append(asCoordinates(boundingBox.getSouthWest())).append("),").
                    append("new google.maps.LatLng(").append(asCoordinates(boundingBox.getNorthEast())).append("));\n");
            ignoreNextZoomCallback = true;
        }

        if (haveToInitializeMapOnFirstStart) {
            NavigationPosition center;
            // if there are positions right at the start center on them else take the last known center and zoom
            if (positions.size() > 0) {
                center = new BoundingBox(positions).getCenter();
            } else {
                int zoom = getZoom();
                buffer.append("setZoom(").append(zoom).append(");\n");
                center = getLastMapCenter();
            }
            buffer.append("setCenter(new google.maps.LatLng(").append(asCoordinates(center)).append("));\n");
        }
        executeScript(buffer.toString());
        haveToInitializeMapOnFirstStart = false;

        if (fitBoundsToPositions) {
            // need to update zoom since fitBounds() changes the zoom level without firing a notification
            Integer zoom = getCurrentZoom();
            if (zoom != null)
                setZoom(zoom);
        }
    }

    private void selectPositions(List<NavigationPosition> selectedPositions, NavigationPosition center) {
        lastSelectedPositions = new ArrayList<>(selectedPositions);

        StringBuilder buffer = new StringBuilder();
        for (int i = 0; i < selectedPositions.size(); i++) {
            NavigationPosition selectedPosition = selectedPositions.get(i);
            buffer.append("selectPosition(new google.maps.LatLng(").append(asCoordinates(selectedPosition)).append("),").
                    append("\"").append(escape(selectedPosition.getDescription())).append("\",").
                    append(i).append(");\n");
        }

        if (center != null && center.hasCoordinates())
            buffer.append("panTo(new google.maps.LatLng(").append(asCoordinates(center)).append("));\n");
        buffer.append("removeSelectedPositions();");
        executeScript(buffer.toString());
    }

    private final Map<Integer, PositionPair> insertWaypointsQueue = new LinkedHashMap<>();
    private final ExecutorService insertWaypointsExecutor = newSingleThreadExecutor();

    private void insertWaypoints(final String mode, int[] startPositions) {
        final Map<Integer, PositionPair> addToQueue = new LinkedHashMap<>();
        Random random = new Random();
        synchronized (notificationMutex) {
            @SuppressWarnings("unchecked")
            List<NavigationPosition> positions = positionsModel.getRoute().getPositions();
            for (int i = 0; i < startPositions.length; i++) {
                // skip the very last position without successor
                if (i == positions.size() - 1 || i == startPositions.length - 1)
                    continue;
                addToQueue.put(random.nextInt(), new PositionPair(positions.get(startPositions[i]), positions.get(startPositions[i] + 1)));
            }
        }

        synchronized (insertWaypointsQueue) {
            insertWaypointsQueue.putAll(addToQueue);
        }

        insertWaypointsExecutor.execute(new Runnable() {
            public void run() {
                for (Map.Entry<Integer, PositionPair> entry : addToQueue.entrySet()) {
                    NavigationPosition origin = entry.getValue().getFirst();
                    NavigationPosition destination = entry.getValue().getSecond();
                    executeScript(mode +
                            "({" + "origin: new google.maps.LatLng(" + asCoordinates(origin) + ")," +
                            "destination: new google.maps.LatLng(" + asCoordinates(destination) + ")," +
                            "travelMode: google.maps.DirectionsTravelMode." + mapViewCallback.getTravelMode().getName().toUpperCase() + "," +
                            "avoidFerries: " + mapViewCallback.isAvoidFerries() + "," +
                            "avoidHighways: " + mapViewCallback.isAvoidHighways() + "," +
                            "avoidTolls: " + mapViewCallback.isAvoidTolls() + "," +
                            "region: \"" + Locale.getDefault().getCountry().toLowerCase() + "\"}," + entry.getKey() + ");\n");
                    try {
                        sleep(preferences.getInt("insertWaypointsSegmentTimeout", 1000));
                    } catch (InterruptedException e) {
                        // intentionally left empty
                    }
                }
            }
        });
    }

    private void insertWaypointsCallback(Integer key, List<String> parameters) {
        PositionPair pair;
        synchronized (insertWaypointsQueue) {
            pair = insertWaypointsQueue.remove(key);
        }

        if (parameters.size() < 5 || pair == null)
            return;

        final NavigationPosition before = pair.getFirst();
        NavigationPosition after = pair.getSecond();
        final BaseRoute route = parseRoute(parameters, before, after);
        @SuppressWarnings("unchecked")
        final List<NavigationPosition> positions = positionsModel.getRoute().getPositions();
        synchronized (notificationMutex) {
            int row = positions.indexOf(before) + 1;
            insertPositions(row, route);
        }
        invokeLater(new Runnable() {
            public void run() {
                int row;
                synchronized (notificationMutex) {
                    row = positions.indexOf(before) + 1;
                }
                complementPositions(row, route);
            }
        });
    }

    // call Google Maps API functions

    @SuppressWarnings("unused")
    public void insertAllWaypoints(int[] startPositions) {
        insertWaypoints("insertAllWaypoints", startPositions);
    }

    @SuppressWarnings("unused")
    public void insertOnlyTurnpoints(int[] startPositions) {
        insertWaypoints("insertOnlyTurnpoints", startPositions);
    }

    // script execution

    private String escape(String string) {
        if (string == null)
            return "";
        StringBuilder buffer = new StringBuilder(string);
        for (int i = 0; i < buffer.length(); i++) {
            char c = buffer.charAt(i);
            if (!(isLetterOrDigit(c) || isWhitespace(c) || c == '\'' || c == ',')) {
                buffer.deleteCharAt(i);
                i--;
            }
        }
        return buffer.toString();
    }

    protected void logJavaScript(String script, Object result) {
        log.info("Executed '" + script + (result != null ? "'\nwith result '" + result : "") + "'");
    }

    protected abstract void executeScript(String script);

    protected abstract String executeScriptWithResult(String script);

    // browser callbacks

    private void processStream(Socket socket) throws IOException {
        List<String> lines = new ArrayList<>();
        boolean processingPost = false, processingBody = false;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()), 64 * 1024)) {
            while (true) {
                try {
                    String line = trim(reader.readLine());
                    if (line == null) {
                        if (processingPost && !processingBody) {
                            processingBody = true;
                            continue;
                        } else
                            break;
                    }
                    if (line.startsWith("POST"))
                        processingPost = true;
                    lines.add(line);
                } catch (IOException e) {
                    log.severe("Cannot read line from callback listener port:" + e);
                    break;
                }
            }

            try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()))) {
                writer.write("HTTP/1.1 200 OK\n");
                writer.write("Content-Type: text/plain\n");
            }
        }

        StringBuilder buffer = new StringBuilder();
        for (String line : lines) {
            buffer.append("  ").append(line).append("\n");
        }
        log.fine("Processing callback @" + currentTimeMillis() + " from port " + socket.getPort() + ": \n" + buffer.toString());

        if (!isAuthenticated(lines))
            return;

        processLines(lines, socket.getPort());
    }

    private boolean isAuthenticated(List<String> lines) {
        Map<String, String> map = asMap(lines);
        String host = trim(map.get("Host"));
        return host != null && host.equals("127.0.0.1:" + getCallbackPort());
    }

    int getCallbackPort() {
        return callbackListenerServerSocket.getLocalPort();
    }

    private static final Pattern NAME_VALUE_PATTERN = Pattern.compile("^(.+?):(.+)$");

    private Map<String, String> asMap(List<String> lines) {
        Map<String, String> map = new HashMap<>();
        for (String line : lines) {
            Matcher matcher = NAME_VALUE_PATTERN.matcher(line);
            if (matcher.matches())
                map.put(matcher.group(1), matcher.group(2));
        }
        return map;
    }

    private static final Pattern CALLBACK_REQUEST_PATTERN = Pattern.compile("^(GET|OPTIONS|POST) /(\\d+)/(.*) HTTP.+$");
    private int lastCallbackNumber = -1;

    void processLines(List<String> lines, int port) {
        boolean hasValidCallbackNumber = false;
        for (String line : lines) {
            Matcher matcher = CALLBACK_REQUEST_PATTERN.matcher(line);
            if (matcher.matches()) {
                int callbackNumber = parseInt(matcher.group(2));
                if (lastCallbackNumber >= callbackNumber) {
                    log.info("Ignoring callback number: " + callbackNumber + " last callback number is: " + lastCallbackNumber + " port is: " + port);
                    break;
                }
                lastCallbackNumber = callbackNumber;
                hasValidCallbackNumber = true;

                String callback = matcher.group(3);
                if (processCallback(callback)) {
                    log.info("Processed " + matcher.group(1) + " callback " + callback + " with number: " + callbackNumber + " from port: " + port);
                    break;
                }
            }

            // process body of POST requests
            if (hasValidCallbackNumber && processCallback(line)) {
                log.info("Processed POST callback " + line + " with number: " + lastCallbackNumber + " from port: " + port);
                break;
            }
        }
    }

    private static final Pattern ADD_POSITION_PATTERN = Pattern.compile("^add-position/(.*)/(.*)$");
    private static final Pattern ADD_POSITION_AT_PATTERN = Pattern.compile("^add-position-at/(.*)/(.*)/(.*)$");
    private static final Pattern MOVE_POSITION_PATTERN = Pattern.compile("^move-position/(.*)/(.*)/(.*)$");
    private static final Pattern DELETE_POSITION_PATTERN = Pattern.compile("^delete-position/(.*)/(.*)/(.*)$");
    private static final Pattern SELECT_POSITION_PATTERN = Pattern.compile("^select-position/(.*)/(.*)/(.*)/(.*)$");
    private static final Pattern SELECT_POSITIONS_PATTERN = Pattern.compile("^select-positions/(.*)/(.*)/(.*)/(.*)/(.*)");
    private static final Pattern MAP_TYPE_CHANGED_PATTERN = Pattern.compile("^map-type-changed/(.*)$");
    private static final Pattern ZOOM_CHANGED_PATTERN = Pattern.compile("^zoom-changed/(.*)$");
    private static final Pattern CENTER_CHANGED_PATTERN = Pattern.compile("^center-changed/(.*)/(.*)/(.*)/(.*)/(.*)/(.*)$");
    private static final Pattern CALLBACK_PORT_PATTERN = Pattern.compile("^callback-port/(\\d+)$");
    private static final Pattern OVER_QUERY_LIMIT_PATTERN = Pattern.compile("^over-query-limit$");
    private static final Pattern ZERO_RESULTS_PATTERN = Pattern.compile("^zero-results$");
    private static final Pattern INSERT_WAYPOINTS_PATTERN = Pattern.compile("^(Insert-All-Waypoints|Insert-Only-Turnpoints): (-?\\d+)/(.*)$");
    private static final Pattern DIRECTIONS_LOAD_PATTERN = Pattern.compile("^directions-load/(-?\\d+)/(.*)$");

    boolean processCallback(String callback) {
        Matcher insertPositionAtMatcher = ADD_POSITION_AT_PATTERN.matcher(callback);
        if (insertPositionAtMatcher.matches()) {
            final int row = parseInt(insertPositionAtMatcher.group(1)) + 1;
            final NavigationPosition position = parsePosition(insertPositionAtMatcher.group(2), insertPositionAtMatcher.group(3));
            invokeLater(new Runnable() {
                public void run() {
                    insertPosition(row, position.getLongitude(), position.getLatitude());
                }
            });
            return true;
        }

        Matcher insertPositionMatcher = ADD_POSITION_PATTERN.matcher(callback);
        if (insertPositionMatcher.matches()) {
            final int row = getAddRow();
            final NavigationPosition position = parsePosition(insertPositionMatcher.group(1), insertPositionMatcher.group(2));
            invokeLater(new Runnable() {
                public void run() {
                    insertPosition(row, position.getLongitude(), position.getLatitude());
                }
            });
            return true;
        }

        Matcher movePositionMatcher = MOVE_POSITION_PATTERN.matcher(callback);
        if (movePositionMatcher.matches()) {
            final int row = getMoveRow(parseInt(movePositionMatcher.group(1)));
            final NavigationPosition position = parsePosition(movePositionMatcher.group(2), movePositionMatcher.group(3));
            invokeLater(new Runnable() {
                public void run() {
                    movePosition(row, position.getLongitude(), position.getLatitude());
                }
            });
            return true;
        }

        Matcher deletePositionMatcher = DELETE_POSITION_PATTERN.matcher(callback);
        if (deletePositionMatcher.matches()) {
            final NavigationPosition position = parsePosition(deletePositionMatcher.group(1), deletePositionMatcher.group(2));
            final Double threshold = parseDouble(deletePositionMatcher.group(3));
            invokeLater(new Runnable() {
                public void run() {
                    deletePosition(position.getLongitude(), position.getLatitude(), threshold);
                }
            });
            return true;
        }

        Matcher selectPositionMatcher = SELECT_POSITION_PATTERN.matcher(callback);
        if (selectPositionMatcher.matches()) {
            final NavigationPosition position = parsePosition(selectPositionMatcher.group(1), selectPositionMatcher.group(2));
            final Double threshold = parseDouble(selectPositionMatcher.group(3));
            final Boolean replaceSelection = parseBoolean(selectPositionMatcher.group(4));
            invokeLater(new Runnable() {
                public void run() {
                    selectPosition(position.getLongitude(), position.getLatitude(), threshold, replaceSelection);
                }
            });
            return true;
        }

        Matcher selectPositionsMatcher = SELECT_POSITIONS_PATTERN.matcher(callback);
        if (selectPositionsMatcher.matches()) {
            NavigationPosition northEast = parsePosition(selectPositionsMatcher.group(1), selectPositionsMatcher.group(2));
            NavigationPosition southWest = parsePosition(selectPositionsMatcher.group(3), selectPositionsMatcher.group(4));
            final BoundingBox boundingBox = new BoundingBox(northEast, southWest);
            final Boolean replaceSelection = parseBoolean(selectPositionsMatcher.group(5));
            invokeLater(new Runnable() {
                public void run() {
                    selectPositions(boundingBox, replaceSelection);
                }
            });
            return true;
        }

        Matcher mapTypeChangedMatcher = MAP_TYPE_CHANGED_PATTERN.matcher(callback);
        if (mapTypeChangedMatcher.matches()) {
            String mapType = decodeUri(mapTypeChangedMatcher.group(1));
            mapTypeChanged(mapType);
            return true;
        }

        Matcher zoomChangedMatcher = ZOOM_CHANGED_PATTERN.matcher(callback);
        if (zoomChangedMatcher.matches()) {
            Integer zoom = parseInteger(zoomChangedMatcher.group(1));
            zoomChanged(zoom);
            return true;
        }

        Matcher centerChangedMatcher = CENTER_CHANGED_PATTERN.matcher(callback);
        if (centerChangedMatcher.matches()) {
            NavigationPosition center = parsePosition(centerChangedMatcher.group(1), centerChangedMatcher.group(2));
            NavigationPosition northEast = parsePosition(centerChangedMatcher.group(3), centerChangedMatcher.group(4));
            NavigationPosition southWest = parsePosition(centerChangedMatcher.group(5), centerChangedMatcher.group(6));
            BoundingBox boundingBox = new BoundingBox(northEast, southWest);
            centerChanged(center, boundingBox);
            return true;
        }

        Matcher callbackPortMatcher = CALLBACK_PORT_PATTERN.matcher(callback);
        if (callbackPortMatcher.matches()) {
            int port = parseInt(callbackPortMatcher.group(1));
            fireReceivedCallback(port);
            return true;
        }

        Matcher overQueryLimitMatcher = OVER_QUERY_LIMIT_PATTERN.matcher(callback);
        if (overQueryLimitMatcher.matches()) {
            overQueryLimitCount++;
            log.warning("Google Directions API is over query limit, count: " + overQueryLimitCount);
            return true;
        }

        Matcher zeroResultsMatcher = ZERO_RESULTS_PATTERN.matcher(callback);
        if (zeroResultsMatcher.matches()) {
            zeroResultsCount++;
            log.warning("Google Directions API returns zero results, count: " + zeroResultsCount);
            return true;
        }

        Matcher directionsLoadMatcher = DIRECTIONS_LOAD_PATTERN.matcher(callback);
        if (directionsLoadMatcher.matches()) {
            Integer startIndex = parseInt(directionsLoadMatcher.group(1));
            List<DistanceAndTime> distanceAndTimes = parseDistanceAndTimeParameters(directionsLoadMatcher.group(2));
            directionsLoadCallback(startIndex, distanceAndTimes);
            return true;
        }

        Matcher insertWaypointsMatcher = INSERT_WAYPOINTS_PATTERN.matcher(callback);
        if (insertWaypointsMatcher.matches()) {
            Integer key = parseInteger(insertWaypointsMatcher.group(2));
            List<String> parameters = parsePositionParameters(insertWaypointsMatcher.group(3));
            insertWaypointsCallback(key, parameters);
            return true;
        }
        return false;
    }

    private void centerChanged(NavigationPosition center, BoundingBox boundingBox) {
        preferences.putDouble(CENTER_LATITUDE_PREFERENCE, center.getLatitude());
        preferences.putDouble(CENTER_LONGITUDE_PREFERENCE, center.getLongitude());

        if (positionReducer.hasFilteredVisibleArea()) {
            if (!positionReducer.isWithinVisibleArea(boundingBox)) {
                synchronized (notificationMutex) {
                    haveToRepaintRouteImmediately = true;
                    routeUpdateReason = "repaint not visible positions";
                    positionReducer.clear();
                    notificationMutex.notifyAll();
                }
            }
        }
    }

    private void zoomChanged(Integer zoom) {
        setZoom(zoom);
        synchronized (notificationMutex) {
            // since setCenter() leads to a callback and thus paints the track twice
            if (ignoreNextZoomCallback)
                ignoreNextZoomCallback = false;
            else if (recenterAfterZooming.getBoolean() ||
                    // directions are automatically scaled by the Google Maps API when zooming
                    !positionsModel.getRoute().getCharacteristics().equals(Route) ||
                    positionReducer.hasFilteredVisibleArea()) {
                haveToRepaintRouteImmediately = true;
                // if enabled, recenter map to selected positions after zooming
                if (recenterAfterZooming.getBoolean())
                    haveToRecenterMap = true;
                haveToRepaintSelectionImmediately = true;
                selectionUpdateReason = "zoomed from " + lastZoom + " to " + zoom;
                lastZoom = zoom;
                notificationMutex.notifyAll();
            }
        }
    }

    private void mapTypeChanged(String mapType) {
        preferences.put(MAP_TYPE_PREFERENCE, mapType);
        if(fixMapModeModel.getFixMapMode().equals(Automatic)) {
            invokeLater(new Runnable() {
                public void run() {
                    update(false, false);
                }
            });
        }
    }

    private boolean isDuplicate(NavigationPosition position, NavigationPosition insert) {
        if (position == null)
            return false;
        Double distance = position.calculateDistance(insert);
        return toDouble(distance) < 10.0;
    }

    private String trimSpaces(String string) {
        if ("-".equals(string))
            return null;
        try {
            return trim(new String(string.getBytes(), UTF8_ENCODING));
        } catch (UnsupportedEncodingException e) {
            return null;
        }
    }

    private List<String> parsePositionParameters(String parameters) {
        List<String> result = new ArrayList<>();
        StringTokenizer tokenizer = new StringTokenizer(parameters, "/");
        while (tokenizer.hasMoreTokens()) {
            String latitude = trim(tokenizer.nextToken());
            if (tokenizer.hasMoreTokens()) {
                String longitude = trim(tokenizer.nextToken());
                if (tokenizer.hasMoreTokens()) {
                    String meters = trim(tokenizer.nextToken());
                    if (tokenizer.hasMoreTokens()) {
                        String seconds = trim(tokenizer.nextToken());
                        if (tokenizer.hasMoreTokens()) {
                            String instructions = trimSpaces(tokenizer.nextToken());
                            result.add(latitude);
                            result.add(longitude);
                            result.add(meters);
                            result.add(seconds);
                            result.add(instructions);
                        }
                    }
                }
            }
        }
        return result;
    }

    private List<DistanceAndTime> parseDistanceAndTimeParameters(String parameters) {
        List<DistanceAndTime> result = new ArrayList<>();
        StringTokenizer tokenizer = new StringTokenizer(parameters, "/");
        while (tokenizer.hasMoreTokens()) {
            String distance = trim(tokenizer.nextToken());
            if (tokenizer.hasMoreTokens()) {
                String time = trim(tokenizer.nextToken());
                result.add(new DistanceAndTime(parseInt(distance), parseInt(time)));
            }
        }
        return result;
    }

    private Double parseSeconds(String string) {
        Double result = parseDouble(string);
        return !isEmpty(result) ? result : null;
    }

    @SuppressWarnings("unchecked")
    private BaseRoute parseRoute(List<String> parameters, NavigationPosition before, NavigationPosition after) {
        BaseRoute route = new NavigatingPoiWarnerFormat().createRoute(Waypoints, null, new ArrayList<NavigationPosition>());
        // count backwards as inserting at position 0
        CompactCalendar time = after.getTime();
        for (int i = parameters.size() - 1; i > 0; i -= 5) {
            String instructions = trim(parameters.get(i));
            Double seconds = parseSeconds(parameters.get(i - 1));
            // Double meters = parseDouble(parameters.get(i - 2));
            NavigationPosition coordinates = parsePosition(parameters.get(i - 4), parameters.get(i - 3));
            if (seconds != null && time != null) {
                Calendar calendar = time.getCalendar();
                calendar.add(SECOND, -seconds.intValue());
                time = fromCalendar(calendar);
            }
            String description = instructions != null ? instructions : null;

            BaseNavigationPosition position = route.createPosition(coordinates.getLongitude(), coordinates.getLatitude(), null, null, seconds != null ? time : null, description);
            if (!isDuplicate(before, position) && !isDuplicate(after, position)) {
                route.add(0, position);
            }
        }
        return route;
    }

    @SuppressWarnings("unchecked")
    private void insertPositions(int row, BaseRoute route) {
        try {
            positionsModel.add(row, route);
        } catch (IOException e) {
            log.severe("Cannot insert route: " + e);
        }
    }

    private void complementPositions(int row, BaseRoute route) {
        int[] rows = asRange(row, row + route.getPositions().size() - 1);
        // do not complement description since this is limited to 2500 calls/day
        mapViewCallback.complementData(rows, false, true, true, false, false);
    }

    private void insertPosition(int row, Double longitude, Double latitude) {
        positionsModel.add(row, longitude, latitude, null, null, null, mapViewCallback.createDescription(positionsModel.getRowCount() + 1, null));
        int[] rows = new int[]{row};
        positionsSelectionModel.setSelectedPositions(rows, true);
        mapViewCallback.complementData(rows, true, true, true, true, false);
    }

    private int getAddRow() {
        NavigationPosition position = lastSelectedPositions.size() > 0 ? lastSelectedPositions.get(lastSelectedPositions.size() - 1) : null;
        // quite crude logic to be as robust as possible on failures
        if (position == null && positionsModel.getRowCount() > 0)
            position = positionsModel.getPosition(positionsModel.getRowCount() - 1);
        return position != null ? positionsModel.getIndex(position) + 1 : 0;
    }

    private int getMoveRow(int index) {
        NavigationPosition position = lastSelectedPositions.get(index);
        final int row;
        synchronized (notificationMutex) {
            row = positionsModel.getRoute().getPositions().indexOf(position);
        }
        return row;
    }

    private void movePosition(int row, Double longitude, Double latitude) {
        NavigationPosition reference = positionsModel.getPosition(row);
        Double diffLongitude = reference != null ? longitude - reference.getLongitude() : 0.0;
        Double diffLatitude = reference != null ? latitude - reference.getLatitude() : 0.0;

        boolean moveCompleteSelection = preferences.getBoolean(MOVE_COMPLETE_SELECTION_PREFERENCE, true);
        boolean cleanElevation = preferences.getBoolean(CLEAN_ELEVATION_ON_MOVE_PREFERENCE, false);
        boolean complementElevation = preferences.getBoolean(COMPLEMENT_ELEVATION_ON_MOVE_PREFERENCE, true);
        boolean cleanTime = preferences.getBoolean(CLEAN_TIME_ON_MOVE_PREFERENCE, false);
        boolean complementTime = preferences.getBoolean(COMPLEMENT_TIME_ON_MOVE_PREFERENCE, true);

        int minimum = row;
        for (int index : selectedPositionIndices) {
            if (index < minimum)
                minimum = index;

            NavigationPosition position = positionsModel.getPosition(index);
            if (position == null)
                continue;

            if (index != row) {
                if (!moveCompleteSelection)
                    continue;

                positionsModel.edit(index, new PositionColumnValues(asList(LONGITUDE_COLUMN_INDEX, LATITUDE_COLUMN_INDEX),
                        Arrays.<Object>asList(position.getLongitude() + diffLongitude, position.getLatitude() + diffLatitude)), false, true);
            } else {
                positionsModel.edit(index, new PositionColumnValues(asList(LONGITUDE_COLUMN_INDEX, LATITUDE_COLUMN_INDEX),
                        Arrays.<Object>asList(longitude, latitude)), false, true);
            }

            if (cleanTime)
                positionsModel.edit(index, new PositionColumnValues(DATE_TIME_COLUMN_INDEX, null), false, false);
            if (cleanElevation)
                positionsModel.edit(index, new PositionColumnValues(ELEVATION_COLUMN_INDEX, null), false, false);

            if (complementTime || complementElevation)
                mapViewCallback.complementData(new int[]{index}, false, complementTime, complementElevation, true, false);
        }

        // updating all rows behind the modified is quite expensive, but necessary due to the distance
        // calculation - if that didn't exist the single update of row would be sufficient
        int size;
        synchronized (notificationMutex) {
            size = positionsModel.getRoute().getPositions().size() - 1;
            haveToRepaintRouteImmediately = true;
            routeUpdateReason = "move position";
            positionReducer.clear();
            haveToRepaintSelectionImmediately = true;
            selectionUpdateReason = "move position";
        }
        positionsModel.fireTableRowsUpdated(minimum, size, ALL_COLUMNS);
    }

    private void selectPosition(Double longitude, Double latitude, Double threshold, boolean replaceSelection) {
        int row = positionsModel.getClosestPosition(longitude, latitude, threshold);
        if (row != -1)
            positionsSelectionModel.setSelectedPositions(new int[]{row}, replaceSelection);
    }

    private void selectPositions(BoundingBox boundingBox, boolean replaceSelection) {
        int[] rows = positionsModel.getContainedPositions(boundingBox);
        if (rows.length > 0) {
            positionsSelectionModel.setSelectedPositions(rows, replaceSelection);
        }
    }

    private void deletePosition(Double longitude, Double latitude, Double threshold) {
        int row = positionsModel.getClosestPosition(longitude, latitude, threshold);
        if (row != -1) {
            positionsModel.remove(new int[]{row});

            executor.execute(new Runnable() {
                public void run() {
                    synchronized (notificationMutex) {
                        haveToRepaintRouteImmediately = true;
                        routeUpdateReason = "delete position";
                        notificationMutex.notifyAll();
                    }
                }
            });
        }
    }

    private Map<Integer, DistanceAndTime> indexToDistanceAndTime = new HashMap<>();

    private void resetDirections() {
        indexToDistanceAndTime.clear();
    }

    private void directionsLoadCallback(final int startIndex, final List<DistanceAndTime> distanceAndTimes) {
        executor.execute(new Runnable() {
            public void run() {
                for (int i = 0; i < distanceAndTimes.size(); i++)
                    indexToDistanceAndTime.put(startIndex + i, distanceAndTimes.get(i));

                Integer[] sorted = indexToDistanceAndTime.keySet().toArray(new Integer[indexToDistanceAndTime.size()]);
                sort(sorted, new Comparator<Integer>() {
                    public int compare(Integer i1, Integer i2) {
                        return i1 - i2;
                    }
                });

                int meters = 0;
                int seconds = 0;
                for(int index : sorted) {
                    DistanceAndTime distanceAndTime = indexToDistanceAndTime.get(index);
                    meters += distanceAndTime.getDistance();
                    seconds += distanceAndTime.getTime();
                }

                fireCalculatedDistance(meters, seconds);
            }
        });
    }

    private static class DistanceAndTime {
        private final int distance;
        private final int time;

        public DistanceAndTime(int distance, int time) {
            this.distance = distance;
            this.time = time;
        }

        public int getDistance() {
            return distance;
        }

        public int getTime() {
            return time;
        }
    }

    // listeners

    private final List<MapViewListener> mapViewListeners = new CopyOnWriteArrayList<>();

    public void addMapViewListener(MapViewListener listener) {
        mapViewListeners.add(listener);
    }

    public void removeMapViewListener(MapViewListener listener) {
        mapViewListeners.remove(listener);
    }

    private void fireCalculatedDistance(int meters, int seconds) {
        for (MapViewListener listener : mapViewListeners) {
            listener.calculatedDistance(meters, seconds);
        }
    }

    private void fireReceivedCallback(int port) {
        for (MapViewListener listener : mapViewListeners) {
            listener.receivedCallback(port);
        }
    }

    private class PositionsModelListener implements TableModelListener {
        public void tableChanged(TableModelEvent e) {
            boolean insertOrDelete = e.getType() == INSERT || e.getType() == DELETE;
            boolean allRowsChanged = isFirstToLastRow(e);
            // used to be limited to single rows which did work reliably but with usability problems
            // if (e.getFirstRow() == e.getLastRow() && insertOrDelete)
            if (!allRowsChanged && insertOrDelete)
                updateRouteButDontRecenter();
            else {
                // ignored updates on columns not displayed
                if (e.getType() == UPDATE &&
                        !(e.getColumn() == DESCRIPTION_COLUMN_INDEX ||
                                e.getColumn() == LONGITUDE_COLUMN_INDEX ||
                                e.getColumn() == LATITUDE_COLUMN_INDEX ||
                                e.getColumn() == ALL_COLUMNS))
                    return;

                if (showAllPositionsAfterLoading.getBoolean())
                    update(allRowsChanged, true);
                else
                    updateRouteButDontRecenter();
            }

            // update position marker on updates of longitude and latitude
            if (e.getType() == UPDATE &&
                    (e.getColumn() == LONGITUDE_COLUMN_INDEX ||
                            e.getColumn() == LATITUDE_COLUMN_INDEX ||
                            e.getColumn() == DESCRIPTION_COLUMN_INDEX ||
                            e.getColumn() == ALL_COLUMNS)) {
                for (int selectedPositionIndex : selectedPositionIndices) {
                    if (selectedPositionIndex >= e.getFirstRow() && selectedPositionIndex <= e.getLastRow()) {
                        updateSelection();
                        break;
                    }
                }
            }
        }
    }

    private class CharacteristicsModelListener implements ListDataListener {
        public void intervalAdded(ListDataEvent e) {
        }

        public void intervalRemoved(ListDataEvent e) {
        }

        public void contentsChanged(ListDataEvent e) {
            // ignore events following setRoute()
            if (e.getType() == CONTENTS_CHANGED && e.getIndex0() == IGNORE && e.getIndex1() == IGNORE)
                return;
            updateRouteButDontRecenter();
        }
    }

    private class MapViewCallbackListener implements ChangeListener {
        public void stateChanged(ChangeEvent e) {
            if (positionsModel.getRoute().getCharacteristics().equals(Route))
                update(false, false);
        }
    }

    private class ShowCoordinatesListener implements ChangeListener {
        public void stateChanged(ChangeEvent e) {
            setShowCoordinates();
        }
    }

    private class ShowWaypointDescriptionListener implements ChangeListener {
        public void stateChanged(ChangeEvent e) {
            if (positionsModel.getRoute().getCharacteristics().equals(Waypoints))
                update(false, false);
        }
    }

    private class RepaintPositionListListener implements ChangeListener {
        public void stateChanged(ChangeEvent e) {
            update(true, false);
        }
    }

    private class GoogleMapsServerListener implements ChangeListener {
        public void stateChanged(ChangeEvent e) {
            initializeWebPage();
        }
    }

    private class UnitSystemListener implements ChangeListener {
        public void stateChanged(ChangeEvent e) {
            setDegreeFormat();
        }
    }
}
