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

package slash.navigation.converter.gui.mapview;

import slash.common.type.CompactCalendar;
import slash.navigation.base.BaseNavigationPosition;
import slash.navigation.base.BaseRoute;
import slash.navigation.base.NavigationPosition;
import slash.navigation.base.RouteCharacteristics;
import slash.navigation.base.Wgs84Position;
import slash.navigation.converter.gui.augment.PositionAugmenter;
import slash.navigation.converter.gui.models.CharacteristicsModel;
import slash.navigation.converter.gui.models.PositionsModel;
import slash.navigation.converter.gui.models.PositionsSelectionModel;
import slash.navigation.nmn.NavigatingPoiWarnerFormat;

import javax.swing.*;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.StringTokenizer;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.Integer.MAX_VALUE;
import static java.lang.Math.min;
import static java.lang.System.currentTimeMillis;
import static java.lang.Thread.sleep;
import static java.util.Calendar.SECOND;
import static javax.swing.JOptionPane.showMessageDialog;
import static javax.swing.SwingUtilities.invokeLater;
import static javax.swing.event.ListDataEvent.CONTENTS_CHANGED;
import static javax.swing.event.TableModelEvent.ALL_COLUMNS;
import static javax.swing.event.TableModelEvent.DELETE;
import static javax.swing.event.TableModelEvent.INSERT;
import static javax.swing.event.TableModelEvent.UPDATE;
import static slash.common.helpers.ThreadHelper.safeJoin;
import static slash.common.io.Transfer.UTF8_ENCODING;
import static slash.common.io.Transfer.ceiling;
import static slash.common.io.Transfer.decodeUri;
import static slash.common.io.Transfer.isEmpty;
import static slash.common.io.Transfer.parseDouble;
import static slash.common.io.Transfer.parseInt;
import static slash.common.io.Transfer.trim;
import static slash.common.type.CompactCalendar.fromCalendar;
import static slash.navigation.base.RouteCharacteristics.Route;
import static slash.navigation.base.RouteCharacteristics.Waypoints;
import static slash.navigation.converter.gui.models.CharacteristicsModel.IGNORE;
import static slash.navigation.converter.gui.models.PositionColumns.DESCRIPTION_COLUMN_INDEX;
import static slash.navigation.converter.gui.models.PositionColumns.ELEVATION_COLUMN_INDEX;
import static slash.navigation.converter.gui.models.PositionColumns.LATITUDE_COLUMN_INDEX;
import static slash.navigation.converter.gui.models.PositionColumns.LONGITUDE_COLUMN_INDEX;
import static slash.navigation.converter.gui.models.PositionColumns.TIME_COLUMN_INDEX;
import static slash.navigation.base.Positions.asPosition;
import static slash.navigation.base.Positions.center;
import static slash.navigation.base.Positions.northEast;
import static slash.navigation.base.Positions.southWest;

/**
 * Base implementation for a component that displays the positions of a position list on a map.
 *
 * @author Christian Pesch
 */

public abstract class BaseMapView implements MapView {
    protected static final Preferences preferences = Preferences.userNodeForPackage(MapView.class);
    protected static final Logger log = Logger.getLogger(MapView.class.getName());

    protected static final String MAP_TYPE_PREFERENCE = "mapType";
    private static final String CLEAN_ELEVATION_ON_MOVE_PREFERENCE = "cleanElevationOnMove";
    private static final String CLEAN_TIME_ON_MOVE_PREFERENCE = "cleanTimeOnMove";
    private static final String COMPLEMENT_TIME_ON_MOVE_PREFERENCE = "complementTimeOnMove";
    private static final String CENTER_LATITUDE_PREFERENCE = "centerLatitude";
    private static final String CENTER_LONGITUDE_PREFERENCE = "centerLongitude";
    private static final String CENTER_ZOOM_PREFERENCE = "centerZoom";

    private PositionsModel positionsModel;
    private List<NavigationPosition> positions;
    private PositionsSelectionModel positionsSelectionModel;
    private List<NavigationPosition> lastSelectedPositions;
    private int[] selectedPositionIndices = new int[0];
    private NavigationPosition center;
    private int lastZoom = -1;

    private ServerSocket callbackListenerServerSocket;
    private Thread positionListUpdater, selectionUpdater, callbackListener, callbackPoller;

    protected final Object notificationMutex = new Object();
    protected boolean initialized = false;
    private boolean running = true, recenterAfterZooming, avoidHighways, avoidTolls,
            haveToInitializeMapOnFirstStart = true, haveToRepaintSelectionImmediately = false,
            haveToRepaintRouteImmediately = false, haveToRecenterMap = false,
            haveToUpdateRoute = false, haveToReplaceRoute = false,
            haveToRepaintSelection = false, ignoreNextZoomCallback = false;
    private TravelMode travelMode;
    private String routeUpdateReason = "?", selectionUpdateReason = "?";
    private PositionAugmenter positionAugmenter;
    private PositionReducer positionReducer;
    private ExecutorService executor = Executors.newCachedThreadPool();

    // initialization

    public void initialize(PositionsModel positionsModel,
                           PositionsSelectionModel positionsSelectionModel,
                           CharacteristicsModel characteristicsModel,
                           PositionAugmenter positionAugmenter,
                           boolean recenterAfterZooming,
                           TravelMode travelMode, boolean avoidHighways, boolean avoidTolls) {
        initializeBrowser();
        setModel(positionsModel, positionsSelectionModel, characteristicsModel);
        this.positionAugmenter = positionAugmenter;
        this.recenterAfterZooming = recenterAfterZooming;
        this.travelMode = travelMode;
        this.avoidHighways = avoidHighways;
        this.avoidTolls = avoidTolls;
    }

    protected abstract void initializeBrowser();

    protected void setModel(PositionsModel positionsModel,
                            PositionsSelectionModel positionsSelectionModel,
                            CharacteristicsModel characteristicsModel) {
        this.positionsModel = positionsModel;
        this.positionsSelectionModel = positionsSelectionModel;

        positionsModel.addTableModelListener(new TableModelListener() {
            public void tableChanged(TableModelEvent e) {
                boolean insertOrDelete = e.getType() == INSERT || e.getType() == DELETE;
                boolean allRowsChanged = e.getFirstRow() == 0 && e.getLastRow() == MAX_VALUE;
                // used to be limited to single rows which did work reliably but with usabilty problems
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
                    update(allRowsChanged || insertOrDelete);
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
        });
        characteristicsModel.addListDataListener(new ListDataListener() {
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
        });
        positionReducer = new PositionReducer(new PositionReducer.Callback() {
            public int getZoom() {
                return BaseMapView.this.getZoom();
            }

            public NavigationPosition getNorthEastBounds() {
                return BaseMapView.this.getNorthEastBounds();
            }

            public NavigationPosition getSouthWestBounds() {
                return BaseMapView.this.getSouthWestBounds();
            }
        });
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
                            log.fine("Woke up to update route: " + routeUpdateReason +
                                    " haveToUpdateRoute:" + haveToUpdateRoute +
                                    " haveToReplaceRoute:" + haveToReplaceRoute +
                                    " haveToRepaintRouteImmediately:" + haveToRepaintRouteImmediately);
                            copiedPositions = new ArrayList<NavigationPosition>(positions);
                            recenter = haveToReplaceRoute;
                            haveToUpdateRoute = false;
                            haveToReplaceRoute = false;
                            haveToRepaintRouteImmediately = false;
                        } else
                            continue;
                    }

                    setCenterOfMap(copiedPositions, recenter);
                    RouteCharacteristics characteristics = positionsModel.getRoute().getCharacteristics();
                    List<NavigationPosition> render = positionReducer.reducePositions(copiedPositions, characteristics);
                    switch (characteristics) {
                        case Route:
                            addDirectionsToMap(render);
                            break;
                        case Waypoints:
                            addMarkersToMap(render);
                            break;
                        default:
                            addPolylinesToMap(render);
                    }
                    log.info("Position list updated for " + render.size() + " positions of type " +
                            characteristics + ", recentering: " + recenter);
                    lastTime = currentTimeMillis();
                }
            }
        }, "MapViewPositionListUpdater");
        positionListUpdater.start();

        selectionUpdater = new Thread(new Runnable() {
            public void run() {
                long lastTime = 0;
                while (true) {
                    int[] copiedSelectedPositionIndices;
                    List<NavigationPosition> copiedPositions;
                    boolean recenter;
                    synchronized (notificationMutex) {
                        try {
                            notificationMutex.wait(100);
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
                            copiedPositions = new ArrayList<NavigationPosition>(positions);
                        } else
                            continue;
                    }

                    List<NavigationPosition> render = positionReducer.reduceSelectedPositions(copiedPositions, copiedSelectedPositionIndices);
                    NavigationPosition centerPosition = center != null ? center : render.size() > 0 ? render.get(0) : null;
                    selectPositions(render, recenter ? centerPosition : null);
                    log.info("Selected positions updated for " + render.size() + " positions, recentering: " + recenter + " to: " + centerPosition);
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
            log.severe("Cannot open callback listener socket: " + e.getMessage());
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
                                    log.severe("Cannot process stream from callback listener socket: " + e.getMessage());
                                }
                            }
                        });
                    } catch (SocketTimeoutException e) {
                        // intentionally left empty
                    } catch (IOException e) {
                        synchronized (notificationMutex) {
                            //noinspection ConstantConditions
                            if (running) {
                                log.severe("Cannot accept callback listener socket: " + e.getMessage());
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
            e.printStackTrace();
            final String message = "Probably faulty network setup: " + e.getLocalizedMessage() + ".\nPlease check your network settings.";
            log.severe(message);
            invokeLater(new Runnable() {
                public void run() {
                    showMessageDialog(getComponent(), message, "Error", JOptionPane.ERROR_MESSAGE);
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

    // disposal

    public void dispose() {
        long start = currentTimeMillis();
        synchronized (notificationMutex) {
            running = false;
            notificationMutex.notifyAll();
        }

        if (positionAugmenter != null)
            positionAugmenter.interrupt();

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
                log.warning("Cannot close callback listener socket:" + e.getMessage());
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
        long end = currentTimeMillis();
        log.info("Executors stopped after " + (end - start) + " ms");
    }

    // getter and setter

    protected boolean isVisible() {
        return getComponent().getWidth() > 0;
    }

    private boolean hasPositions() {
        synchronized (notificationMutex) {
            return isInitialized() && positions != null;
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
            haveToRecenterMap = true;
            haveToRepaintSelection = true;
            selectionUpdateReason = "selected " + selectedPositions.length + " positions; " +
                    "replacing selection: " + replaceSelection;
            notificationMutex.notifyAll();
        }
    }

    public void setRecenterAfterZooming(boolean recenterAfterZooming) {
        this.recenterAfterZooming = recenterAfterZooming;
    }

    public void setTravelMode(TravelMode travelMode) {
        this.travelMode = travelMode;
        if (positionsModel.getRoute().getCharacteristics() == Route)
            update(false);
    }

    public void setAvoidHighways(boolean avoidHighways) {
        this.avoidHighways = avoidHighways;
        if (positionsModel.getRoute().getCharacteristics() == Route)
            update(false);
    }

    public void setAvoidTolls(boolean avoidTolls) {
        this.avoidTolls = avoidTolls;
        if (positionsModel.getRoute().getCharacteristics() == Route)
            update(false);
    }

    public NavigationPosition getCenter() {
        if (isInitialized())
            return getCurrentMapCenter();
        else
            return getLastMapCenter();
    }

    public void setCenter(NavigationPosition center) {
        this.center = center;
    }

    private int getZoom() {
        return preferences.getInt(CENTER_ZOOM_PREFERENCE, 1);
    }

    private void setZoom(int zoom) {
        preferences.putInt(CENTER_ZOOM_PREFERENCE, zoom);
    }

    protected abstract NavigationPosition getNorthEastBounds();
    protected abstract NavigationPosition getSouthWestBounds();

    protected abstract NavigationPosition getCurrentMapCenter();
    protected abstract Integer getCurrentZoom();

    protected abstract String getCallbacks();

    private NavigationPosition getLastMapCenter() {
        double latitude = preferences.getDouble(CENTER_LATITUDE_PREFERENCE, 35.0);
        double longitude = preferences.getDouble(CENTER_LONGITUDE_PREFERENCE, -25.0);
        return new Wgs84Position(longitude, latitude, null, null, null, null);
    }

    protected NavigationPosition extractLatLng(String script) {
        String result = executeScriptWithResult(script);
        if (result == null)
            return null;

        StringTokenizer tokenizer = new StringTokenizer(result, ",");
        if (tokenizer.countTokens() != 2)
            return null;

        String latitude = tokenizer.nextToken();
        String longitude = tokenizer.nextToken();
        return asPosition(parseDouble(longitude), parseDouble(latitude));
    }

    // draw on map

    @SuppressWarnings({"unchecked"})
    protected void update(boolean haveToReplaceRoute) {
        if (!isInitialized() || !getComponent().isShowing())
            return;

        synchronized (notificationMutex) {
            this.positions = positionsModel.getRoute() != null ? positionsModel.getRoute().getPositions() : null;
            this.haveToUpdateRoute = true;
            routeUpdateReason = "update route";
            if (haveToReplaceRoute) {
                this.haveToReplaceRoute = true;
                routeUpdateReason = "replace route";
                positionReducer.clear();
                this.haveToRepaintSelection = true;
                selectionUpdateReason = "replace route";
            }
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

    private void removeOverlays() {
        executeScript("removeOverlays();");
    }

    private void removeDirections() {
        executeScript("removeDirections();");
    }

    private void addDirectionsToMap(List<NavigationPosition> positions) {
        executeScript("resetDirections();");

        // avoid throwing javascript exceptions if there is nothing to direct
        if (positions.size() < 2) {
            addMarkersToMap(positions);
            return;
        }

        removeOverlays();

        int maximumRouteSegmentLength = preferences.getInt("maximumRouteSegmentLength", 8);
        int directionsCount = ceiling(positions.size(), maximumRouteSegmentLength, false);
        for (int j = 0; j < directionsCount; j++) {
            StringBuilder buffer = new StringBuilder();
            buffer.append("var latlngs").append(j).append(" = [");

            int start = Math.max(0, j * maximumRouteSegmentLength - 1);
            int end = min(positions.size(), (j + 1) * maximumRouteSegmentLength) - 1;
            for (int i = start + 1; i < end; i++) {
                NavigationPosition position = positions.get(i);
                buffer.append("{location: new google.maps.LatLng(").append(position.getLatitude()).append(",").
                        append(position.getLongitude()).append(")}");
                if (i < end - 1)
                    buffer.append(",");
            }
            buffer.append("];\n");

            NavigationPosition origin = positions.get(start);
            NavigationPosition destination = positions.get(end);
            buffer.append("renderDirections({origin: new google.maps.LatLng(").append(origin.getLatitude()).
                    append(",").append(origin.getLongitude()).append("), ");
            buffer.append("destination: new google.maps.LatLng(").append(destination.getLatitude()).
                    append(",").append(destination.getLongitude()).append("), ");
            buffer.append("waypoints: latlngs").append(j).append(", ").
                    append("travelMode: google.maps.DirectionsTravelMode.").append(travelMode.toString().toUpperCase()).append(", ");
            buffer.append("avoidHighways: ").append(avoidHighways).append(", ");
            buffer.append("avoidTolls: ").append(avoidTolls).append(", ");
            buffer.append("region: \"").append(Locale.getDefault().getCountry().toLowerCase()).append("\"}, ");
            int startIndex = positionsModel.getIndex(origin);
            buffer.append(startIndex).append(", ");
            boolean lastSegment = (j == directionsCount - 1);
            buffer.append(lastSegment).append(");\n");
            if (lastSegment)
                try {
                    sleep(500);
                } catch (InterruptedException e) {
                    // intentionally left empty
                }
            executeScript(buffer.toString());
        }
    }

    private void addPolylinesToMap(final List<NavigationPosition> positions) {
        // display markers if there is no polyline to show
        if (positions.size() < 2) {
            addMarkersToMap(positions);
            return;
        }

        String color = preferences.get("trackLineColor", "0033FF");
        int width = preferences.getInt("trackLineWidth", 2);
        int maximumPolylineSegmentLength = preferences.getInt("maximumTrackSegmentLength", 35);
        int polylinesCount = ceiling(positions.size(), maximumPolylineSegmentLength, true);
        for (int j = 0; j < polylinesCount; j++) {
            StringBuilder buffer = new StringBuilder();
            buffer.append("var latlngs = [");
            int maximum = min(positions.size(), (j + 1) * maximumPolylineSegmentLength + 1);
            for (int i = j * maximumPolylineSegmentLength; i < maximum; i++) {
                NavigationPosition position = positions.get(i);
                buffer.append("new google.maps.LatLng(").append(position.getLatitude()).append(",").
                        append(position.getLongitude()).append(")");
                if (i < maximum - 1)
                    buffer.append(",");
            }
            buffer.append("];\n");
            buffer.append("addPolyline(latlngs,\"#").append(color).append("\",").append(width).append(");");
            executeScript(buffer.toString());
        }
        removeOverlays();
        removeDirections();
    }

    private void addMarkersToMap(List<NavigationPosition> positions) {
        int maximumMarkerSegmentLength = preferences.getInt("maximumWaypointSegmentLength", 10);
        int markersCount = ceiling(positions.size(), maximumMarkerSegmentLength, false);
        for (int j = 0; j < markersCount; j++) {
            StringBuilder buffer = new StringBuilder();
            int maximum = min(positions.size(), (j + 1) * maximumMarkerSegmentLength);
            for (int i = j * maximumMarkerSegmentLength; i < maximum; i++) {
                NavigationPosition position = positions.get(i);
                buffer.append("addMarker(").append(position.getLatitude()).append(",").
                        append(position.getLongitude()).append(",").
                        append("\"").append(escape(position.getComment())).append("\");\n");
            }
            executeScript(buffer.toString());
        }
        removeOverlays();
        removeDirections();
    }

    private void setCenterOfMap(List<NavigationPosition> positions, boolean recenter) {
        StringBuilder buffer = new StringBuilder();

        boolean fitBoundsToPositions = positions.size() > 0 && recenter;
        if (fitBoundsToPositions) {
            NavigationPosition northEast = northEast(positions);
            NavigationPosition southWest = southWest(positions);
            buffer.append("fitBounds(").append(southWest.getLatitude()).append(",").append(southWest.getLongitude()).append(",").
                    append(northEast.getLatitude()).append(",").append(northEast.getLongitude()).append(");\n");
            ignoreNextZoomCallback = true;
        }

        if (haveToInitializeMapOnFirstStart) {
            NavigationPosition center;
            // if there are positions right at the start center on them else take the last known center and zoom
            if (positions.size() > 0) {
                center = center(positions);
            } else {
                int zoom = getZoom();
                buffer.append("setZoom(").append(zoom).append(");\n");
                center = getLastMapCenter();
            }
            buffer.append("setCenter(").append(center.getLatitude()).append(",").append(center.getLongitude()).append(");\n");
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
        lastSelectedPositions = new ArrayList<NavigationPosition>(selectedPositions);

        StringBuilder buffer = new StringBuilder();
        for (int i = 0; i < selectedPositions.size(); i++) {
            NavigationPosition selectedPosition = selectedPositions.get(i);
            buffer.append("selectPosition(").append(selectedPosition.getLatitude()).append(",").
                    append(selectedPosition.getLongitude()).append(",").
                    append("\"").append(escape(selectedPosition.getComment())).append("\",").
                    append(i).append(");\n");
        }

        if (center != null && center.hasCoordinates())
            buffer.append("panTo(").append(center.getLatitude()).append(",").append(center.getLongitude()).append(");\n");
        buffer.append("removeSelectedPositions();");
        executeScript(buffer.toString());
    }

    private final Map<Integer, List<NavigationPosition>> insertWaypointsQueue = new HashMap<Integer, List<NavigationPosition>>();

    private void insertWaypoints(final String mode, int[] startPositions) {
        final Map<Integer, List<NavigationPosition>> addToQueue = new HashMap<Integer, List<NavigationPosition>>();
        Random random = new Random();
        synchronized (notificationMutex) {
            for (int i = 0; i < startPositions.length; i++) {
                // skip the very last position without successor
                if (i == positions.size() - 1 || i == startPositions.length - 1)
                    continue;
                List<NavigationPosition> successorPredecessor = new ArrayList<NavigationPosition>();
                successorPredecessor.add(positions.get(startPositions[i]));
                successorPredecessor.add(positions.get(startPositions[i] + 1));
                addToQueue.put(random.nextInt(), successorPredecessor);
            }
        }

        synchronized (insertWaypointsQueue) {
            insertWaypointsQueue.putAll(addToQueue);
        }

        executor.execute(new Runnable() {
            public void run() {
                for (Integer key : addToQueue.keySet()) {
                    List<NavigationPosition> successorPredecessor = addToQueue.get(key);
                    NavigationPosition from = successorPredecessor.get(0);
                    NavigationPosition to = successorPredecessor.get(1);
                    StringBuilder buffer = new StringBuilder();
                    buffer.append(mode).append("({");
                    buffer.append("origin: new google.maps.LatLng(").append(from.getLatitude()).append(",").append(from.getLongitude()).append("), ");
                    buffer.append("destination: new google.maps.LatLng(").append(to.getLatitude()).append(",").append(to.getLongitude()).append("), ");
                    buffer.append("travelMode: google.maps.DirectionsTravelMode.").append(travelMode.toString().toUpperCase()).append(", ");
                    buffer.append("avoidHighways: ").append(avoidHighways).append(", ");
                    buffer.append("avoidTolls: ").append(avoidTolls).append(", ");
                    buffer.append("region: \"").append(Locale.getDefault().getCountry().toLowerCase()).append("\"}, ").append(key).append(");\n");
                    executeScript(buffer.toString());
                    try {
                        sleep(500);
                    } catch (InterruptedException e) {
                        // don't care if this happens
                    }
                }
            }
        });
    }

    // call Google Maps API functions

    public void insertAllWaypoints(int[] startPositions) {
        insertWaypoints("insertAllWaypoints", startPositions);
    }

    public void insertOnlyTurnpoints(int[] startPositions) {
        insertWaypoints("insertOnlyTurnpoints", startPositions);
    }

    public void print(String title, boolean withDirections) {
        executeScript("printMap(\"" + title + "\", " + withDirections + ");");
    }

    // script execution

    private String escape(String string) {
        if (string == null)
            return "";
        StringBuilder buffer = new StringBuilder(string);
        for (int i = 0; i < buffer.length(); i++) {
            char c = buffer.charAt(i);
            if (!(Character.isLetterOrDigit(c) || Character.isWhitespace(c) || c == '\'' || c == ',')) {
                buffer.deleteCharAt(i);
                i--;
            }
        }
        return buffer.toString();
    }

    protected void logJavaScript(String script, Object result) {
        log.info("script '" + script + (result != null ? "'\nwith result '" + result : "") + "'");
    }

    protected abstract void executeScript(String script);
    protected abstract String executeScriptWithResult(String script);

    // browser callbacks

    private void processStream(Socket socket) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()), 64 * 1024);
        OutputStream outputStream = socket.getOutputStream();

        List<String> lines = new ArrayList<String>();
        boolean processingPost = false, processingBody = false;
        try {
            while (true) {
                try {
                    String line = trim(reader.readLine());
                    // log.fine("read line " + line);
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
                    log.severe("Cannot read line from callback listener port:" + e.getMessage());
                    break;
                }
            }
        } finally {
            reader.close();
            outputStream.close();
        }

        StringBuilder buffer = new StringBuilder();
        for (String line : lines) {
            buffer.append("  ").append(line).append("\n");
        }
        log.fine("processing callback: \n" + buffer.toString());

        if (!isAuthenticated(lines))
            return;

        processLines(lines);
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
        Map<String, String> map = new HashMap<String, String>();
        for (String line : lines) {
            Matcher matcher = NAME_VALUE_PATTERN.matcher(line);
            if (matcher.matches())
                map.put(matcher.group(1), matcher.group(2));
        }
        return map;
    }

    private static final Pattern CALLBACK_REQUEST_PATTERN = Pattern.compile("^(GET|OPTIONS|POST) /(\\d+)/(.*) HTTP.+$");
    private int lastCallbackNumber = -1;

    void processLines(List<String> lines) {
        boolean hasValidCallbackNumber = false;
        for (String line : lines) {
            // log.fine("processing line " + line);
            Matcher matcher = CALLBACK_REQUEST_PATTERN.matcher(line);
            if (matcher.matches()) {
                int callbackNumber = parseInt(matcher.group(2));
                if (lastCallbackNumber >= callbackNumber) {
                    log.info("ignoring callback number: " + callbackNumber + " last callback number is: " + lastCallbackNumber);
                    break;
                }
                lastCallbackNumber = callbackNumber;
                hasValidCallbackNumber = true;

                String callback = matcher.group(3);
                if (processCallback(callback)) {
                    log.fine("processed " + matcher.group(1) + " callback " + callback + " with number: " + callbackNumber);
                    break;
                }
            }

            // process body of POST requests
            if (hasValidCallbackNumber && processCallback(line)) {
                log.fine("processed POST callback " + line + " with number: " + lastCallbackNumber);
                break;
            }
        }
    }

    private static final Pattern DIRECTIONS_LOAD_PATTERN = Pattern.compile("^directions-load/(\\d*)/(\\d*)$");
    private static final Pattern ADD_POSITION_PATTERN = Pattern.compile("^add-position/(.*)/(.*)$");
    private static final Pattern INSERT_POSITION_PATTERN = Pattern.compile("^insert-position/(.*)/(.*)/(.*)$");
    private static final Pattern MOVE_POSITION_PATTERN = Pattern.compile("^move-position/(.*)/(.*)/(.*)$");
    private static final Pattern REMOVE_POSITION_PATTERN = Pattern.compile("^remove-position/(.*)/(.*)/(.*)$");
    private static final Pattern SELECT_POSITION_PATTERN = Pattern.compile("^select-position/(.*)/(.*)/(.*)/(.*)$");
    private static final Pattern SELECT_POSITIONS_PATTERN = Pattern.compile("^select-positions/(.*)/(.*)/(.*)/(.*)/(.*)");
    private static final Pattern MAP_TYPE_CHANGED_PATTERN = Pattern.compile("^map-type-changed/(.*)$");
    private static final Pattern ZOOM_CHANGED_PATTERN = Pattern.compile("^zoom-changed/(.*)$");
    private static final Pattern CENTER_CHANGED_PATTERN = Pattern.compile("^center-changed/(.*)/(.*)$");
    private static final Pattern CALLBACK_PORT_PATTERN = Pattern.compile("^callback-port/(\\d+)$");
    private static final Pattern INSERT_WAYPOINTS_PATTERN = Pattern.compile("^(Insert-All-Waypoints|Insert-Only-Turnpoints): (-?\\d+)/(.*)$");

    boolean processCallback(String callback) {
        Matcher directionsLoadMatcher = DIRECTIONS_LOAD_PATTERN.matcher(callback);
        if (directionsLoadMatcher.matches()) {
            int meters = parseInt(directionsLoadMatcher.group(1));
            int seconds = parseInt(directionsLoadMatcher.group(2));
            fireCalculatedDistance(meters, seconds);
            return true;
        }

        Matcher insertPositionMatcher = INSERT_POSITION_PATTERN.matcher(callback);
        if (insertPositionMatcher.matches()) {
            final int row = parseInt(insertPositionMatcher.group(1)) + 1;
            final Double latitude = parseDouble(insertPositionMatcher.group(2));
            final Double longitude = parseDouble(insertPositionMatcher.group(3));
            invokeLater(new Runnable() {
                public void run() {
                    insertPosition(row, longitude, latitude);
                }
            });
            return true;
        }

        Matcher addPositionMatcher = ADD_POSITION_PATTERN.matcher(callback);
        if (addPositionMatcher.matches()) {
            final int row = getAddRow();
            final Double latitude = parseDouble(addPositionMatcher.group(1));
            final Double longitude = parseDouble(addPositionMatcher.group(2));
            invokeLater(new Runnable() {
                public void run() {
                    insertPosition(row, longitude, latitude);
                }
            });
            return true;
        }

        Matcher movePositionMatcher = MOVE_POSITION_PATTERN.matcher(callback);
        if (movePositionMatcher.matches()) {
            final int row = getMoveRow(parseInt(movePositionMatcher.group(1)));
            final Double latitude = parseDouble(movePositionMatcher.group(2));
            final Double longitude = parseDouble(movePositionMatcher.group(3));
            invokeLater(new Runnable() {
                public void run() {
                    movePosition(row, longitude, latitude);
                }
            });
            return true;
        }

        Matcher removePositionMatcher = REMOVE_POSITION_PATTERN.matcher(callback);
        if (removePositionMatcher.matches()) {
            final Double latitude = parseDouble(removePositionMatcher.group(1));
            final Double longitude = parseDouble(removePositionMatcher.group(2));
            final Double threshold = parseDouble(removePositionMatcher.group(3));
            invokeLater(new Runnable() {
                public void run() {
                    removePosition(longitude, latitude, threshold);
                }
            });
            return true;
        }

        Matcher selectPositionMatcher = SELECT_POSITION_PATTERN.matcher(callback);
        if (selectPositionMatcher.matches()) {
            final Double latitude = parseDouble(selectPositionMatcher.group(1));
            final Double longitude = parseDouble(selectPositionMatcher.group(2));
            final Double threshold = parseDouble(selectPositionMatcher.group(3));
            final Boolean replaceSelection = Boolean.parseBoolean(selectPositionMatcher.group(4));
            invokeLater(new Runnable() {
                public void run() {
                    selectPosition(longitude, latitude, threshold, replaceSelection);
                }
            });
            return true;
        }

        Matcher selectPositionsMatcher = SELECT_POSITIONS_PATTERN.matcher(callback);
        if (selectPositionsMatcher.matches()) {
            final Double latitudeNorthEast = parseDouble(selectPositionsMatcher.group(1));
            final Double longitudeNorthEast = parseDouble(selectPositionsMatcher.group(2));
            final Double latitudeSouthWest = parseDouble(selectPositionsMatcher.group(3));
            final Double longitudeSouthWest = parseDouble(selectPositionsMatcher.group(4));
            final Boolean replaceSelection = Boolean.parseBoolean(selectPositionsMatcher.group(5));
            invokeLater(new Runnable() {
                public void run() {
                    selectPositions(asPosition(longitudeNorthEast, latitudeNorthEast),
                            asPosition(longitudeSouthWest, latitudeSouthWest), replaceSelection);
                }
            });
            return true;
        }

        Matcher mapTypeChangedMatcher = MAP_TYPE_CHANGED_PATTERN.matcher(callback);
        if (mapTypeChangedMatcher.matches()) {
            String mapType = decodeUri(mapTypeChangedMatcher.group(1));
            preferences.put(MAP_TYPE_PREFERENCE, mapType);
            return true;
        }

        Matcher zoomChangedMatcher = ZOOM_CHANGED_PATTERN.matcher(callback);
        if (zoomChangedMatcher.matches()) {
            Integer zoom = parseInt(zoomChangedMatcher.group(1));
            zoomChanged(zoom);
            return true;
        }

        Matcher centerChangedMatcher = CENTER_CHANGED_PATTERN.matcher(callback);
        if (centerChangedMatcher.matches()) {
            Double latitude = parseDouble(centerChangedMatcher.group(1));
            Double longitude = parseDouble(centerChangedMatcher.group(2));
            centerChanged(longitude, latitude);
            return true;
        }

        Matcher callbackPortMatcher = CALLBACK_PORT_PATTERN.matcher(callback);
        if (callbackPortMatcher.matches()) {
            int port = parseInt(callbackPortMatcher.group(1));
            fireReceivedCallback(port);
            return true;
        }

        Matcher insertWaypointsMatcher = INSERT_WAYPOINTS_PATTERN.matcher(callback);
        if (insertWaypointsMatcher.matches()) {
            Integer key = parseInt(insertWaypointsMatcher.group(2));
            List<String> coordinates = parseCoordinates(insertWaypointsMatcher.group(3));

            List<NavigationPosition> successorPredecessor;
            synchronized (insertWaypointsQueue) {
                successorPredecessor = insertWaypointsQueue.remove(key);
            }

            if (coordinates.size() < 5 || successorPredecessor == null)
                return true;

            NavigationPosition before = successorPredecessor.get(0);
            NavigationPosition after = successorPredecessor.get(1);
            final int row;
            synchronized (notificationMutex) {
                row = positions.indexOf(before) + 1;
            }
            final BaseRoute route = parseRoute(coordinates, before, after);
            invokeLater(new Runnable() {
                public void run() {
                    insertPositions(row, route);
                    complementPositions(row, route);
                }
            });
            log.info("processed insert " + callback);
            return false;
        }
        return false;
    }

    private void centerChanged(Double longitude, Double latitude) {
        preferences.putDouble(CENTER_LATITUDE_PREFERENCE, latitude);
        preferences.putDouble(CENTER_LONGITUDE_PREFERENCE, longitude);

        if (positionReducer.hasFilteredVisibleArea()) {
            NavigationPosition mapNorthEast = getNorthEastBounds();
            NavigationPosition mapSouthWest = getSouthWestBounds();

            if (!positionReducer.isWithinVisibleArea(mapNorthEast, mapSouthWest)) {
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
            else {
                haveToRepaintRouteImmediately = true;
                // if enabled, recenter map to selected positions after zooming
                if (recenterAfterZooming)
                    haveToRecenterMap = true;
                haveToRepaintSelectionImmediately = true;
                selectionUpdateReason = "zoomed from " + lastZoom + " to " + zoom;
                lastZoom = zoom;
                notificationMutex.notifyAll();
            }
        }
    }

    private boolean isDuplicate(NavigationPosition position, NavigationPosition insert) {
        if (position == null)
            return false;
        Double distance = position.calculateDistance(insert);
        return distance != null && distance < 10.0;
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

    private List<String> parseCoordinates(String coordinates) {
        List<String> result = new ArrayList<String>();
        StringTokenizer tokenizer = new StringTokenizer(coordinates, "/");
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

    private Double parseSeconds(String string) {
        Double result = parseDouble(string);
        return !isEmpty(result) ? result : null;
    }

    @SuppressWarnings("unchecked")
    private BaseRoute parseRoute(List<String> coordinates, NavigationPosition before, NavigationPosition after) {
        BaseRoute route = new NavigatingPoiWarnerFormat().createRoute(Waypoints, null, new ArrayList<NavigationPosition>());
        // count backwards as inserting at position 0
        CompactCalendar time = after.getTime();
        int positionInsertionCount = coordinates.size() / 5;
        for (int i = coordinates.size() - 1; i > 0; i -= 5) {
            String instructions = trim(coordinates.get(i));
            Double seconds = parseSeconds(coordinates.get(i - 1));
            // Double meters = parseDouble(coordinates.get(i - 2));
            Double longitude = parseDouble(coordinates.get(i - 3));
            Double latitude = parseDouble(coordinates.get(i - 4));
            if (seconds != null && time != null) {
                Calendar calendar = time.getCalendar();
                calendar.add(SECOND, -seconds.intValue());
                time = fromCalendar(calendar);
            }
            int positionNumber = positionsModel.getRowCount() + (positionInsertionCount - route.getPositionCount()) - 1;
            String comment = instructions != null ? instructions : positionAugmenter.createComment(positionNumber);
            BaseNavigationPosition position = route.createPosition(longitude, latitude, null, null, seconds != null ? time : null, comment);
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
            log.severe("Cannot insert route: " + e.getMessage());
        }
    }

    @SuppressWarnings({"unchecked"})
    private void complementPositions(int row, BaseRoute route) {
        List<NavigationPosition> positions = route.getPositions();
        int index = row;
        for (NavigationPosition position : positions) {
            // do not complement comment since this is limited to 2500 calls/day
            positionAugmenter.complementElevation(index, position.getLongitude(), position.getLatitude());
            positionAugmenter.complementTime(index, position.getTime(), false);
            index++;
        }
    }

    private void insertPosition(int row, Double longitude, Double latitude) {
        positionsModel.add(row, longitude, latitude, null, null, null, positionAugmenter.createComment(positionsModel.getRowCount() + 1));
        positionsSelectionModel.setSelectedPositions(new int[]{row}, true);

        positionAugmenter.complementComment(row, longitude, latitude);
        positionAugmenter.complementElevation(row, longitude, latitude);
        positionAugmenter.complementTime(row, null, true);
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
            row = positions.indexOf(position);
        }
        return row;
    }

    private void movePosition(int row, Double longitude, Double latitude) {
        NavigationPosition reference = positionsModel.getPosition(row);
        Double diffLongitude = reference != null ? longitude - reference.getLongitude() : 0.0;
        Double diffLatitude = reference != null ? latitude - reference.getLatitude() : 0.0;

        int minimum = row;
        for (int index : selectedPositionIndices) {
            if (index < minimum)
                minimum = index;

            NavigationPosition position = positionsModel.getPosition(index);
            if (position == null)
                continue;

            if (index != row) {
                positionsModel.edit(index, LONGITUDE_COLUMN_INDEX, position.getLongitude() + diffLongitude,
                        LATITUDE_COLUMN_INDEX, position.getLatitude() + diffLatitude, false, true);
            } else {
                positionsModel.edit(index, LONGITUDE_COLUMN_INDEX, longitude,
                        LATITUDE_COLUMN_INDEX, latitude, false, true);
            }

            if (preferences.getBoolean(CLEAN_ELEVATION_ON_MOVE_PREFERENCE, false))
                positionsModel.edit(index, ELEVATION_COLUMN_INDEX, null, -1, null, false, false);
            if (preferences.getBoolean(CLEAN_TIME_ON_MOVE_PREFERENCE, false))
                positionsModel.edit(index, TIME_COLUMN_INDEX, null, -1, null, false, false);
            if (preferences.getBoolean(COMPLEMENT_TIME_ON_MOVE_PREFERENCE, false))
                positionAugmenter.complementTime(index, null, true);
        }

        // updating all rows behind the modified is quite expensive, but necessary due to the distance
        // calculation - if that didn't exist the single update of row would be sufficient
        int size;
        synchronized (notificationMutex) {
            size = positions.size() - 1;
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

    private void selectPositions(NavigationPosition northEastCorner, NavigationPosition southWestCorner, boolean replaceSelection) {
        int[] rows = positionsModel.getContainedPositions(northEastCorner, southWestCorner);
        if (rows.length > 0) {
            positionsSelectionModel.setSelectedPositions(rows, replaceSelection);
        }
    }

    private void removePosition(Double longitude, Double latitude, Double threshold) {
        int row = positionsModel.getClosestPosition(longitude, latitude, threshold);
        if (row != -1) {
            positionsModel.remove(new int[]{row});

            executor.execute(new Runnable() {
                public void run() {
                    synchronized (notificationMutex) {
                        haveToRepaintRouteImmediately = true;
                        routeUpdateReason = "remove position";
                        notificationMutex.notifyAll();
                    }
                }
            });
        }
    }

    // listeners

    private final List<MapViewListener> mapViewListeners = new CopyOnWriteArrayList<MapViewListener>();

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
}
