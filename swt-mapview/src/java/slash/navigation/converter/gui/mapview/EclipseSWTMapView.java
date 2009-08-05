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

import chrriis.dj.nativeswing.swtimpl.components.*;
import slash.navigation.BaseNavigationPosition;
import slash.navigation.RouteCharacteristics;
import slash.navigation.Wgs84Position;
import slash.navigation.converter.gui.models.CharacteristicsModel;
import slash.navigation.converter.gui.models.PositionsModel;
import slash.navigation.util.*;

import javax.swing.*;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import java.awt.*;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.*;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.lang.reflect.InvocationTargetException;

/**
 * Displays the positions of a route.
 *
 * @author Christian Pesch
 */

public class EclipseSWTMapView implements MapView {
    private static final Preferences preferences = Preferences.userNodeForPackage(EclipseSWTMapView.class);
    private static final Logger log = Logger.getLogger(EclipseSWTMapView.class.getName());
    private static final String DEBUG_PREFERENCE = "debug";
    private static final String MAP_TYPE_PREFERENCE = "mapType";
    private static final String SCALE_CONTROL_PREFERENCE = "scaleControl";
    private static final int MAXIMUM_POLYLINE_SEGMENT_LENGTH = preferences.getInt("maximumTrackSegmentLength", 35);
    private static final int MAXIMUM_POLYLINE_POSITION_COUNT = preferences.getInt("maximumTrackPositionCount", 1500);
    private static final int MAXIMUM_DIRECTIONS_SEGMENT_LENGTH = preferences.getInt("maximumRouteSegmentLength", 22);
    private static final int MAXIMUM_DIRECTIONS_POSITION_COUNT = preferences.getInt("maximumRoutePositionCount", 500);
    private static final int MAXIMUM_MARKER_SEGMENT_LENGTH = preferences.getInt("maximumWaypointSegmentLength", 5);
    private static final int MAXIMUM_MARKER_POSITION_COUNT = preferences.getInt("maximumWaypointPositionCount", 50);
    private static final int MAXIMUM_SELECTION_COUNT = preferences.getInt("maximumSelectionCount", 10);
    private static final int[] ZOOMLEVEL_SCALE = {
            400000000,
            200000000,
            100000000,
            50000000,
            25000000,
            12500000,
            6400000,
            3200000,
            1600000,
            800000,
            400000,
            200000,
            100000,
            50000,
            25000,
            12500,
            6400,
            3200
    };
    private static final int MAXIMUM_ZOOMLEVEL_FOR_SIGNIFICANCE_CALCULATION = 16;

    private final List<MapViewListener> mapViewListeners = new CopyOnWriteArrayList<MapViewListener>();
    private JWebBrowser webBrowser;
    private ServerSocket dragListenerServerSocket;
    private PositionsModel positionsModel;
    private Thread mapViewRouteUpdater, mapViewPositionUpdater, mapViewDragListener;
    private final Object notificationMutex = new Object();
    private boolean initialized = false, running = true, pedestrians, avoidHighways,
            haveToInitializeMapOnFirstStart = true,
            haveToRepaintImmediately = false,
            haveToUpdateRoute = false, haveToReplaceRoute = false,
            haveToUpdatePosition = false;
    private final boolean debug = preferences.getBoolean(DEBUG_PREFERENCE, true);
    private final Map<Integer, BitSet> significantPositionCache = new HashMap<Integer, BitSet>(ZOOMLEVEL_SCALE.length);

    public boolean isSupportedPlatform() {
        return Platform.isLinux() || Platform.isMac() || Platform.isWindows();
    }

    public void initialize(PositionsModel positionsModel, CharacteristicsModel characteristicsModel,
                           boolean pedestrians, boolean avoidHighways) {
        initialize();
        setModel(positionsModel, characteristicsModel);
        this.pedestrians = pedestrians;
        this.avoidHighways = avoidHighways;
    }

    private void setModel(PositionsModel positionsModel, CharacteristicsModel characteristicsModel) {
        this.positionsModel = positionsModel;

        positionsModel.addTableModelListener(new TableModelListener() {
            public void tableChanged(TableModelEvent e) {
                boolean insertOrDelete = e.getType() == TableModelEvent.INSERT || e.getType() == TableModelEvent.DELETE;
                boolean allRowsChanged = e.getFirstRow() == 0 && e.getLastRow() == Integer.MAX_VALUE;
                // TODO if we had all the data if (!allRowsChanged && insertOrDelete)
                if (e.getFirstRow() == e.getLastRow() && insertOrDelete)
                    updateButDontRecenter();
                else
                    update(allRowsChanged || insertOrDelete);
            }
        });
        characteristicsModel.addListDataListener(new ListDataListener() {
            public void intervalAdded(ListDataEvent e) {
            }
            public void intervalRemoved(ListDataEvent e) {
            }
            public void contentsChanged(ListDataEvent e) {
                update(true);
            }
        });
    }

    public Component getComponent() {
        return webBrowser;
    }

    private JWebBrowser createWebBrowser() {
        try {
            JWebBrowser browser = new JWebBrowser();
            browser.setBarsVisible(false);
            return browser;
        } catch (Throwable t) {
            log.severe("Cannot create WebBrowser: " + t.getMessage());
            initializationCause = t;
            return null;
        }
    }

    private boolean loadWebPage(JWebBrowser webBrowser) {
        try {
            File html = Externalization.extractFile("slash/navigation/converter/gui/mapview/routeconverter.html");
            if (html == null)
                throw new IllegalArgumentException("Cannot extract routeconverter.html");
            webBrowser.navigate(html.toURI().toURL().toExternalForm());
            if (debug)
            System.out.println(System.currentTimeMillis() + " loadWebPage thread " + Thread.currentThread());
        } catch (Throwable t) {
            log.severe("Cannot create WebBrowser: " + t.getMessage());
            initializationCause = t;
            return false;
        }
        return true;
    }

    private Throwable initializationCause = null;

    public Throwable getInitializationCause() {
        return initializationCause;
    }

    private void initialize() {
        webBrowser = createWebBrowser();
        if (webBrowser == null)
            return;

        webBrowser.addWebBrowserListener(new WebBrowserListener() {
            public void windowWillOpen(WebBrowserWindowWillOpenEvent e) {
                if (debug)
                System.out.println(System.currentTimeMillis() + " windowWillOpen " + e.isConsumed() + " thread " + Thread.currentThread());
            }

            public void windowOpening(WebBrowserWindowOpeningEvent e) {
                if (debug)
                System.out.println(System.currentTimeMillis() + " windowOpening " + e.getLocation() + "/" + e.getSize() + " thread " + Thread.currentThread());
            }

            public void windowClosing(WebBrowserEvent e) {
                if (debug)
                System.out.println(System.currentTimeMillis() + " windowClosing " + e + " thread " + Thread.currentThread());
            }

            public void locationChanging(WebBrowserNavigationEvent e) {
                if (debug)
                System.out.println(System.currentTimeMillis() + " locationChanging " + e.getNewResourceLocation() + " thread " + Thread.currentThread());
            }

            public void locationChanged(WebBrowserNavigationEvent e) {
                if (debug)
                System.out.println(System.currentTimeMillis() + " locationChanged " + e.getNewResourceLocation() + " thread " + Thread.currentThread());
            }

            public void locationChangeCanceled(WebBrowserNavigationEvent e) {
                if (debug)
                System.out.println(System.currentTimeMillis() + " locationChangeCanceled " + e.getNewResourceLocation() + " thread " + Thread.currentThread());
            }

            private int startCount = 0;

            public void loadingProgressChanged(WebBrowserEvent e) {
                if (debug)
                System.out.println(System.currentTimeMillis() + " loadingProgressChanged " + e.getWebBrowser().getLoadingProgress() + " thread " + Thread.currentThread());

                if(e.getWebBrowser().getLoadingProgress() == 100 && startCount == 0) {
                    // get out of the listener callback
                    new Thread(new Runnable() {
                        public void run() {
                            if(Platform.isLinux()) {
                                System.out.println(System.currentTimeMillis() + " started sleeping for 2s on Linux");
                                try {
                                    Thread.sleep(2000);
                                } catch (InterruptedException e1) {
                                    // intentionally left empty
                                }
                                System.out.println(System.currentTimeMillis() + " stopped sleeping for 2s on Linux");
                            }
                            tryToInitialize(startCount++);
                        }
                    }, "MapViewInitializer").start();
                }
            }

            public void titleChanged(WebBrowserEvent e) {
                if (debug)
                System.out.println(System.currentTimeMillis() + " titleChanged " + e.getWebBrowser().getPageTitle() + " thread " + Thread.currentThread());
            }

            public void statusChanged(WebBrowserEvent e) {
                if (debug)
                System.out.println(System.currentTimeMillis() + " statusChanged " + e.getWebBrowser().getStatusText() + " thread " + Thread.currentThread());
            }

            public void commandReceived(WebBrowserEvent e, String command, String[] args) {
                if (debug)
                System.out.println(System.currentTimeMillis() + " commandReceived " + command + " thread " + Thread.currentThread());
            }
        });

        if (!loadWebPage(webBrowser)) {
            dispose();
            return;
        }

        initializeBrowserInteraction();
    }

    private void tryToInitialize(int counter) {
        boolean existsCompatibleBrowser = getComponent() != null && isCompatible();
        synchronized (this) {
            initialized = existsCompatibleBrowser;
        }
        System.out.println(System.currentTimeMillis() + " initialized map: "+ initialized); // TODO remove me later

        if (isInitialized()) {
            if (debug)
            System.out.println(System.currentTimeMillis() + " compatible, further initializing map");
            initializeDragListener();
            initializeAfterLoading();
            checkCallback();
        } else {
            if(counter++ < 2) {
                System.out.println(System.currentTimeMillis() + " WAITING "+ counter*2000 + " seconds"); // TODO remove me later
                try {
                    Thread.sleep(counter*2000);
                } catch (InterruptedException e) {
                    // intentionally left empty
                }

                System.out.println(System.currentTimeMillis() + " LOADING page again");
                loadWebPage(webBrowser);
            }
        }
    }

    private void initializeBrowserInteraction() {
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

        mapViewRouteUpdater = new Thread(new Runnable() {
            public void run() {
                long lastTime = 0;
                boolean recenter;
                while (true) {
                    List<BaseNavigationPosition> copiedPositions;
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
                        long currentTime = System.currentTimeMillis();
                        if (haveToRepaintImmediately ||
                                haveToReplaceRoute ||
                                (haveToUpdateRoute && (currentTime - lastTime > 5 * 1000))) {
                            copiedPositions = filterPositionsWithoutCoordinates(positions);
                            recenter = haveToReplaceRoute;
                            haveToUpdateRoute = false;
                            haveToReplaceRoute = false;
                            haveToRepaintImmediately = false;
                        } else
                            continue;
                    }

                    copiedPositions = reducePositions(copiedPositions, recenter, getMaximumPositionCount());
                    setCenterOfMap(copiedPositions, recenter);
                    switch (positionsModel.getRoute().getCharacteristics()) {
                        case Route:
                            addDirectionsToMap(copiedPositions);
                            break;
                        case Waypoints:
                            addMarkersToMap(copiedPositions);
                            break;
                        default:
                            addPolylinesToMap(copiedPositions);
                    }
                    log.info("MapView updated for " + copiedPositions.size() + " positions of type " +
                            positionsModel.getRoute().getCharacteristics() + ", recentering: " + recenter);
                    lastTime = System.currentTimeMillis();
                }
            }
        }, "MapViewRouteUpdater");
        mapViewRouteUpdater.start();

        mapViewPositionUpdater = new Thread(new Runnable() {
            public void run() {
                long lastTime = 0;
                while (true) {
                    int[] copiedSelectedPositions;
                    List<BaseNavigationPosition> copiedPositions;
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

                        long currentTime = System.currentTimeMillis();
                        if (haveToUpdatePosition && (currentTime - lastTime > 500)) {
                            haveToUpdatePosition = false;
                            copiedSelectedPositions = new int[selectedPositionIndices.length];
                            System.arraycopy(selectedPositionIndices, 0, copiedSelectedPositions, 0, copiedSelectedPositions.length);
                            copiedPositions = filterPositionsWithoutCoordinates(positions);
                        } else
                            continue;
                    }

                    List<BaseNavigationPosition> selected = filterSelectedPositions(copiedPositions, copiedSelectedPositions);
                    selectPositions(selected);
                    log.info("MapView position updated for " + selected.size() + " positions");
                    lastTime = System.currentTimeMillis();
                }
            }
        }, "MapViewPositionUpdater");
        mapViewPositionUpdater.start();
    }

    private int getMaximumPositionCount() {
        switch (positionsModel.getRoute().getCharacteristics()) {
            case Route:
                return MAXIMUM_DIRECTIONS_POSITION_COUNT;
            case Waypoints:
                return MAXIMUM_MARKER_POSITION_COUNT;
            default:
                return MAXIMUM_POLYLINE_POSITION_COUNT;
        }
    }

    private ServerSocket createDragListenerServerSocket() {
        try {
            ServerSocket serverSocket = new ServerSocket(0);
            serverSocket.setSoTimeout(1000);
            int port = serverSocket.getLocalPort();
            log.info("MapView listens on port " + port + " for dragging");
            executeScript("setDragListenerPort(" + port + ");");
            return serverSocket;
        } catch (IOException e) {
            log.severe("Cannot open drag listener socket: " + e.getMessage());
            return null;
        }
    }

    private void initializeDragListener() {
        dragListenerServerSocket = createDragListenerServerSocket();
        if (dragListenerServerSocket == null)
            return;

        mapViewDragListener = new Thread(new Runnable() {
            public void run() {
                while (true) {
                    synchronized (notificationMutex) {
                        if (!running) {
                            return;
                        }
                    }

                    Socket clientSocket = null;
                    BufferedReader is = null;
                    OutputStream os = null;
                    try {
                        List<String> lines = new ArrayList<String>();
                        clientSocket = dragListenerServerSocket.accept();
                        is = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                        os = clientSocket.getOutputStream();
                        while (true) {
                            try {
                                String line = is.readLine();
                                if (Conversion.trim(line) == null)
                                    break;
                                lines.add(line);
                            } catch (IOException e) {
                                log.severe("Cannot read line from drag listener port:" + e.getMessage());
                                break;
                            }
                        }
                        processDragListenerCallBack(lines);
                    } catch (SocketTimeoutException e) {
                        // intentionally left empty
                    } catch (IOException e) {
                        synchronized (notificationMutex) {
                            if (running) {
                                log.severe("Cannot listen at drag listener socket: " + e.getMessage());
                            }
                        }
                        break;
                    }
                    finally {
                        try {
                            if (is != null)
                                is.close();
                            if (os != null)
                                os.close();
                            if (clientSocket != null)
                                clientSocket.close();
                        } catch (IOException e) {
                            log.severe("Cannot close drag listener socket: " + e.getMessage());
                        }
                    }
                }
            }
        }, "MapViewDragListener");
        mapViewDragListener.start();
    }

    private void initializeAfterLoading() {
        resize();
        update(true);
        if(preferences.getBoolean(SCALE_CONTROL_PREFERENCE, false))
            executeScript("map.addControl(new GScaleControl());");
    }

    private void checkCallback() {
        // TODO check localhost resolution
        executeScript("checkCallback();");
    }

    private BitSet calculateSignificantPositionsForZoomLevel(List<BaseNavigationPosition> positions, int zoomLevel) {
        BitSet significant = significantPositionCache.get(zoomLevel);
        if (significant == null) {
            significant = new BitSet(positions.size());

            if (zoomLevel <= MAXIMUM_ZOOMLEVEL_FOR_SIGNIFICANCE_CALCULATION) {
                double threshold = ZOOMLEVEL_SCALE[zoomLevel] / 2500.0;
                long start = System.currentTimeMillis();
                int[] significantPositions = Calculation.getSignificantPositions(positions, threshold);
                long end = System.currentTimeMillis();
                log.info("zoomLevel " + zoomLevel + " < " + MAXIMUM_ZOOMLEVEL_FOR_SIGNIFICANCE_CALCULATION + " threshold " + threshold + " significant positions " + significantPositions.length + " calculated in " + (end - start) + " milliseconds");
                for (int significantPosition : significantPositions)
                    significant.set(significantPosition);
            } else {
                // on all zoom level about MAXIMUM_ZOOMLEVEL_FOR_SIGNIFICANCE_CALCULATION
                // use all positions since the calculation is too expensive
                log.info("zoomLevel " + zoomLevel + " use all " + positions.size() + "positions");
                significant.set(0, positions.size(), true);
            }
            significantPositionCache.put(zoomLevel, significant);
        }
        return significant;
    }

    private List<BaseNavigationPosition> reducePositions(List<BaseNavigationPosition> positions, boolean recenter, int maximumPositionCount) {
        if (positions.size() < 2)
            return positions;

        // determine significant positions for this zoom level
        positions = filterSignificantPositions(positions, recenter);

        // reduce the number of significant positions by a visibility heuristic
        if (positions.size() > maximumPositionCount)
            positions = filterVisiblePositions(positions);

        // reduce the number of visible positions by a JS-stability heuristic
        if (positions.size() > maximumPositionCount)
            positions = filterEveryNthPosition(positions, maximumPositionCount);

        return positions;
    }

    private List<BaseNavigationPosition> filterSignificantPositions(List<BaseNavigationPosition> positions, boolean recenter) {
        int zoomLevel = recenter ? getBoundsZoomLevel(positions) : getCurrentZoomLevel();

        BitSet pointStatus = calculateSignificantPositionsForZoomLevel(positions, zoomLevel);

        List<BaseNavigationPosition> result = new ArrayList<BaseNavigationPosition>();
        for (int i = 0; i < positions.size(); i++)
            if (pointStatus.get(i))
                result.add(positions.get(i));
        return result;
    }

    private List<BaseNavigationPosition> filterVisiblePositions(List<BaseNavigationPosition> positions) {
        BaseNavigationPosition northEast = getNorthEastBounds();
        BaseNavigationPosition southWest = getSouthWestBounds();
        if (northEast == null || southWest == null)
            return positions;

        // heuristic: increase bounds for visible positions to enable dragging the map
        // at the same zoom level, with a factor of 2 you hardly see the cropping even
        // with a small map and a big screen (meaning lots of space to drag the map)
        double width = (northEast.getLongitude() - southWest.getLongitude()) * 2.0;
        double height = (southWest.getLatitude() - northEast.getLatitude()) * 2.0;
        northEast.setLongitude(northEast.getLongitude() + width);
        northEast.setLatitude(northEast.getLatitude() - height);
        southWest.setLongitude(southWest.getLongitude() - width);
        southWest.setLatitude(southWest.getLatitude() + height);

        List<BaseNavigationPosition> result = new ArrayList<BaseNavigationPosition>();
        for (int i = 1; i < positions.size(); i++) {
            BaseNavigationPosition position = positions.get(i);
            if (Calculation.containsPosition(northEast, southWest, position)) {
                result.add(position);
            }
        }

        log.info("filtered visible positions to reduce " + positions.size() + " positions to " + result.size());
        return result;
    }

    private List<BaseNavigationPosition> filterEveryNthPosition(List<BaseNavigationPosition> positions, int maximumPositionCount) {
        List<BaseNavigationPosition> result = new ArrayList<BaseNavigationPosition>();
        result.add(positions.get(0));

        double increment = positions.size() / (double) maximumPositionCount;
        for (double i = 1; i < positions.size() - 1; i += increment) {
            result.add(positions.get((int) i));
        }

        result.add(positions.get(positions.size() - 1));

        log.info("filtered every " + increment + "th position to reduce " + positions.size() + " positions to " + result.size() + " (maximum was " + maximumPositionCount + ")");
        return result;
    }

    private List<BaseNavigationPosition> filterSelectedPositions(List<BaseNavigationPosition> positions, int[] selectedIndices) {
        List<BaseNavigationPosition> result = new ArrayList<BaseNavigationPosition>();
        for (int selectedIndex : selectedIndices) {
            if (selectedIndex >= positions.size())
                continue;
            result.add(positions.get(selectedIndex));
        }
        return result;
    }

    private List<BaseNavigationPosition> filterPositionsWithoutCoordinates(List<BaseNavigationPosition> positions) {
        List<BaseNavigationPosition> result = new ArrayList<BaseNavigationPosition>();
        // copy to avoid ConcurrentModificationException
        positions = new ArrayList<BaseNavigationPosition>(positions);
        for (BaseNavigationPosition position : positions) {
            if (position.hasCoordinates())
                result.add(position);
        }
        return result;
    }

    public void dispose() {
        long start = System.currentTimeMillis();
        synchronized (notificationMutex) {
            running = false;
            notificationMutex.notifyAll();
        }

        if (mapViewPositionUpdater != null) {
            try {
                mapViewPositionUpdater.join();
            } catch (InterruptedException e) {
                // intentionally left empty
            }
            long end = System.currentTimeMillis();
            log.info("MapViewPositionUpdater stopped after " + (end - start) + " ms");
        }

        if (mapViewRouteUpdater != null) {
            try {
                mapViewRouteUpdater.join();
            } catch (InterruptedException e) {
                // intentionally left empty
            }
            long end = System.currentTimeMillis();
            log.info("MapViewRouteUpdater stopped after " + (end - start) + " ms");
        }

        if (dragListenerServerSocket != null) {
            try {
                dragListenerServerSocket.close();
            } catch (IOException e) {
                log.severe("Cannot close drag listener socket:" + e.getMessage());
            }
            long end = System.currentTimeMillis();
            log.info("MapViewDragListenerSocket stopped after " + (end - start) + " ms");
        }

        if (mapViewDragListener != null) {
            try {
                mapViewDragListener.join();
            } catch (InterruptedException e) {
                // intentionally left empty
            }
            long end = System.currentTimeMillis();
            log.info("MapViewDragListener stopped after " + (end - start) + " ms");
        }
    }

    private boolean isCompatible() {
        String result = executeScriptWithResult("return window.isCompatible && isCompatible();");
        return Boolean.parseBoolean(result);
    }

    public boolean isInitialized() {
        synchronized (this) {
            return initialized;
        }
    }

    private boolean isVisible() {
        return getComponent().getWidth() > 0;
    }

    private boolean hasPositions() {
        synchronized (notificationMutex) {
            return isInitialized() && positions != null;
        }
    }

    private boolean hasBeenResizedToInvisible = false;

    public void resize() {
        new Thread(new Runnable() {
            public void run() {
                if (!isInitialized() || !getComponent().isShowing())
                    return;

                synchronized (notificationMutex) {
                    // if map is not visible remember to update and resize it again
                    // once the map becomes visible again
                    if (!isVisible()) {
                        hasBeenResizedToInvisible = true;
                    } else if (hasBeenResizedToInvisible) {
                        hasBeenResizedToInvisible = false;
                        update(true);
                    }
                    resizeMap();
                }
            }
        }, "BrowserResizer").start();
    }

    private int lastWidth = -1, lastHeight = -1;

    private void resizeMap() {
        synchronized (notificationMutex) {
            int width = Math.max(getComponent().getWidth() - 17, 0);
            int height = Math.max(getComponent().getHeight() - 2, 0);
            if (width != lastWidth || height != lastHeight) {
                executeScript("resize(" + width + "," + height + ");");
            }
            lastWidth = width;
            lastHeight = height;
        }
    }

    private List<BaseNavigationPosition> positions;

    private int getBoundsZoomLevel(List<BaseNavigationPosition> positions) {
        if ((positions == null) || (positions.size() < 1))
            return 0;

        Wgs84Position northEast = Calculation.getNorthEast(positions);
        Wgs84Position southWest = Calculation.getSouthWest(positions);

        StringBuffer buffer = new StringBuffer();
        buffer.append("return map.getBoundsZoomLevel(new GLatLngBounds(").
                append("new GLatLng(").append(northEast.getLatitude()).append(",").
                append(northEast.getLongitude()).append("),").
                append("new GLatLng(").append(southWest.getLatitude()).append(",").
                append(southWest.getLongitude()).append(")").append("));");

        String zoomLevel = executeScriptWithResult(buffer.toString());
        return zoomLevel != null ? Conversion.parseInt(zoomLevel) : 0;
    }

    private int getCurrentZoomLevel() {
        String zoomLevel = executeScriptWithResult("return map.getZoom();");
        return Conversion.parseInt(zoomLevel);
    }

    private BaseNavigationPosition getBounds(String script) {
        String result = executeScriptWithResult(script);
        if (result == null)
            return null;

        StringTokenizer tokenizer = new StringTokenizer(result, ",");
        if (tokenizer.countTokens() != 2)
            return null;

        String latitude = tokenizer.nextToken();
        String longitude = tokenizer.nextToken();
        return new Wgs84Position(Double.parseDouble(longitude), Double.parseDouble(latitude), null, null, null, null);
    }

    private BaseNavigationPosition getNorthEastBounds() {
        return getBounds("return getNorthEastBounds();");
    }

    private BaseNavigationPosition getSouthWestBounds() {
        return getBounds("return getSouthWestBounds();");
    }

    public BaseNavigationPosition getCenter() {
        BaseNavigationPosition northEast = getNorthEastBounds();
        BaseNavigationPosition southWest = getSouthWestBounds();
        return northEast != null && southWest != null ? Calculation.center(Arrays.asList(northEast, southWest)) : null;
    }

    private void update(boolean haveToReplaceRoute) {
        if (!isInitialized() || !getComponent().isShowing())
            return;

        synchronized (notificationMutex) {
            this.positions = positionsModel.getRoute() != null ? positionsModel.getRoute().getPositions() : null;
            this.haveToUpdateRoute = true;
            if (haveToReplaceRoute) {
                this.haveToReplaceRoute = true;
                this.haveToUpdatePosition = true;
                significantPositionCache.clear();
            }
            log.fine(System.currentTimeMillis() + " update haveToReplaceRoute: " + haveToReplaceRoute + " positions: " + positions);
            notificationMutex.notifyAll();
        }
    }

    private int lastDirectionsCount = -1, lastPolylinesCount = -1, lastMarkersCount = -1, meters = 0, seconds = 0;

    private void removeOverlays() {
        if (lastDirectionsCount > 0) {
            for (int j = 0; j < Conversion.ceiling(lastDirectionsCount, MAXIMUM_DIRECTIONS_SEGMENT_LENGTH, false); j++) {
                StringBuffer buffer = new StringBuffer();
                int maximum = Math.min(lastDirectionsCount, (j + 1) * MAXIMUM_DIRECTIONS_SEGMENT_LENGTH);
                for (int i = j * MAXIMUM_DIRECTIONS_SEGMENT_LENGTH; i < maximum; i++)
                    buffer.append("map.removeOverlay(directions").append(i).append(".getPolyline());");
                executeScript(buffer.toString());
            }
            lastDirectionsCount = -1;
        }

        if (lastPolylinesCount > 0) {
            for (int j = 0; j < Conversion.ceiling(lastPolylinesCount, MAXIMUM_POLYLINE_SEGMENT_LENGTH, false); j++) {
                StringBuffer buffer = new StringBuffer();
                int maximum = Math.min(lastPolylinesCount, (j + 1) * MAXIMUM_POLYLINE_SEGMENT_LENGTH);
                for (int i = j * MAXIMUM_POLYLINE_SEGMENT_LENGTH; i < maximum; i++)
                    buffer.append("map.removeOverlay(line").append(i).append(");");
                executeScript(buffer.toString());
            }
            lastPolylinesCount = -1;
        }

        if (lastMarkersCount > 0) {
            for (int j = 0; j < Conversion.ceiling(lastMarkersCount, MAXIMUM_MARKER_SEGMENT_LENGTH, false); j++) {
                StringBuffer buffer = new StringBuffer();
                int maximum = Math.min(lastMarkersCount, (j + 1) * MAXIMUM_MARKER_SEGMENT_LENGTH);
                for (int i = j * MAXIMUM_MARKER_SEGMENT_LENGTH; i < maximum; i++)
                    buffer.append("map.removeOverlay(marker").append(i).append(");");
                executeScript(buffer.toString());
            }
            lastMarkersCount = -1;
        }
    }

    private void addDirectionsToMap(List<BaseNavigationPosition> positions) {
        removeOverlays();

        meters = 0;
        seconds = 0;

        // avoid throwing javascript exceptions if there is nothing to direct
        if(positions.size() < 2) {
            addMarkersToMap(positions);
            return;
        }

        lastDirectionsCount = Conversion.ceiling(positions.size(), MAXIMUM_DIRECTIONS_SEGMENT_LENGTH, false);
        for (int j = 0; j < lastDirectionsCount; j++) {
            StringBuffer buffer = new StringBuffer();
            buffer.append("var directions").append(j).append(" = createDirections();");
            buffer.append("var latlngs = [");
            int maximum = Math.min(positions.size(), (j + 1) * MAXIMUM_DIRECTIONS_SEGMENT_LENGTH + 1);
            for (int i = j * MAXIMUM_DIRECTIONS_SEGMENT_LENGTH; i < maximum; i++) {
                BaseNavigationPosition position = positions.get(i);
                buffer.append("new GLatLng(").append(position.getLatitude()).append(",").append(position.getLongitude()).append(")");
                if (i < maximum - 1)
                    buffer.append(",");
            }
            buffer.append("];\n");
            buffer.append("directions").append(j).append(".loadFromWaypoints(latlngs, ").
                   append("{ preserveViewport: true, getPolyline: true, avoidHighways: ").
                    append(avoidHighways).append(", travelMode: ").
                    append(pedestrians ? "G_TRAVEL_MODE_WALKING" : "G_TRAVEL_MODE_DRIVING").
                    append(", locale: '").append(Locale.getDefault()).append("'").
                    append(" });");
            executeScript(buffer.toString());
        }
    }

    private void addPolylinesToMap(final List<BaseNavigationPosition> positions) {
        removeOverlays();

        lastPolylinesCount = Conversion.ceiling(positions.size(), MAXIMUM_POLYLINE_SEGMENT_LENGTH, true);
        for (int j = 0; j < lastPolylinesCount; j++) {
            StringBuffer buffer = new StringBuffer();
            buffer.append("var latlngs = [");
            int maximum = Math.min(positions.size(), (j + 1) * MAXIMUM_POLYLINE_SEGMENT_LENGTH + 1);
            for (int i = j * MAXIMUM_POLYLINE_SEGMENT_LENGTH; i < maximum; i++) {
                BaseNavigationPosition position = positions.get(i);
                buffer.append("new GLatLng(").append(position.getLatitude()).append(",").append(position.getLongitude()).append(")");
                if (i < maximum - 1)
                    buffer.append(",");
            }
            buffer.append("];\n");
            buffer.append("var line").append(j).append(" = new GPolyline(latlngs,\"#0033FF\",2,1);\n");
            buffer.append("map.addOverlay(line").append(j).append(");");
            executeScript(buffer.toString());
        }

        new Thread(new Runnable() {
            public void run() {
                calculatedDistance(0, 0);

                int meters = 0;
                long delta = 0;
                Calendar minimumTime = null, maximumTime = null;
                BaseNavigationPosition previous = null;
                for (int i = 0; i < positions.size(); i++) {
                    BaseNavigationPosition next = positions.get(i);
                    if (previous != null) {
                        Double distance = previous.calculateDistance(next);
                        if (distance != null)
                            meters += distance;
                        Long time = previous.calculateTime(next);
                        if (time != null)
                            delta += time;
                    }

                    CompactCalendar time = next.getTime();
                    if (time != null) {
                        Calendar calendar = time.getCalendar();
                        if (minimumTime == null || calendar.before(minimumTime))
                            minimumTime = calendar;
                        if (maximumTime == null || calendar.after(maximumTime))
                            maximumTime = calendar;
                    }

                    if (i % 100 == 0)
                        calculatedDistance(meters, delta > 0 ? (int) (delta / 1000) : 0);

                    previous = next;
                }

                int summedUp = delta > 0 ? (int) delta / 1000 : 0;
                int maxMinusMin = minimumTime != null ? (int) ((maximumTime.getTimeInMillis() - minimumTime.getTimeInMillis()) / 1000) : 0;
                calculatedDistance(meters, Math.max(maxMinusMin, summedUp));
            }
        }, "PolylineDistanceCalculator").start();
    }

    private void addMarkersToMap(List<BaseNavigationPosition> positions) {
        removeOverlays();

        lastMarkersCount = positions.size();
        for (int j = 0; j < Conversion.ceiling(positions.size(), MAXIMUM_MARKER_SEGMENT_LENGTH, false); j++) {
            StringBuffer buffer = new StringBuffer();

            int maximum = Math.min(positions.size(), (j + 1) * MAXIMUM_MARKER_SEGMENT_LENGTH);
            for (int i = j * MAXIMUM_MARKER_SEGMENT_LENGTH; i < maximum; i++) {
                BaseNavigationPosition position = positions.get(i);
                buffer.append("var marker").append(i).append(" = new GMarker(new GLatLng(").
                        append(position.getLatitude()).append(",").append(position.getLongitude()).
                        append("), { title: \"").append(escape(position.getComment())).append("\", ").
                        append("clickable: false, icon: markerIcon });\n");
                buffer.append("map.addOverlay(marker").append(i).append(");\n");
            }
            executeScript(buffer.toString());
        }
    }

    private void setCenterOfMap(List<BaseNavigationPosition> positions, boolean recenter) {
        StringBuffer buffer = new StringBuffer();
        // set map type only on first start
        if (haveToInitializeMapOnFirstStart) {
            String mapType = preferences.get(MAP_TYPE_PREFERENCE, "Map");
            buffer.append("setMapType(\"").append(mapType).append("\");");
        }

        // if there are positions center on first start or if we have to recenter
        if (positions.size() > 0 && (haveToInitializeMapOnFirstStart || recenter)) {
            Wgs84Position northEast = Calculation.getNorthEast(positions);
            Wgs84Position southWest = Calculation.getSouthWest(positions);
            buffer.append("var zoomLevel = map.getBoundsZoomLevel(new GLatLngBounds(").
                    append("new GLatLng(").append(northEast.getLatitude()).append(",").append(northEast.getLongitude()).append("),").
                    append("new GLatLng(").append(southWest.getLatitude()).append(",").append(southWest.getLongitude()).append(")").
                    append("));\n");
            Wgs84Position center = Calculation.center(positions);
            buffer.append("map.setCenter(new GLatLng(").append(center.getLatitude()).append(",").
                    append(center.getLongitude()).append("), zoomLevel);");
        }
        executeScript(buffer.toString());
        haveToInitializeMapOnFirstStart = false;
    }

    private int lastSelectedPositionCount = -1;
    private List<BaseNavigationPosition> lastSelectedPositions;
    private int[] selectedPositionIndices = new int[0];

    private void selectPositions(List<BaseNavigationPosition> selectedPositions) {
        if (selectedPositions.size() > MAXIMUM_SELECTION_COUNT)
            selectedPositions = selectedPositions.subList(0, MAXIMUM_SELECTION_COUNT);

        // delete old
        if (lastSelectedPositionCount >= 0) {
            StringBuffer buffer = new StringBuffer();
            for (int i = 0; i < lastSelectedPositionCount; i++) {
                buffer.append("map.removeOverlay(selected").append(i).append(");\n");
            }
            executeScript(buffer.toString());
        }

        // build up new
        StringBuffer buffer = new StringBuffer();
        lastSelectedPositionCount = selectedPositions.size();
        lastSelectedPositions = selectedPositions;
        for (int i = 0; i < lastSelectedPositionCount; i++) {
            BaseNavigationPosition selectedPosition = selectedPositions.get(i);
            buffer.append("var selected").append(i).append(" = ");
            buffer.append("new GMarker(new GLatLng(").append(selectedPosition.getLatitude()).append(",")
                    .append(selectedPosition.getLongitude()).append("), { title: \"")
                    .append(escape(selectedPosition.getComment())).append("\", draggable: true });\n");
            buffer.append("addMarker(selected").append(i).append(",").append(i).append(");\n");
        }

        // pan to first position
        if (lastSelectedPositionCount > 0) {
            BaseNavigationPosition center = selectedPositions.get(0);
            buffer.append("centerMap(new GLatLng(").append(center.getLatitude()).append(",").
                    append(center.getLongitude()).append("));");
        }
        executeScript(buffer.toString());
    }

    private void logExecuteScript(String script, Object result) {
        String output = System.currentTimeMillis() + " executing script '" + script + (result != null ? "' with result '" + result : "") + "'";
        if(debug) {
            System.out.println(output);
            log.info(output);
        } else
            log.fine(output);
    }

    private void executeScript(final String script) {
        if(script.length() == 0)
            return;

        if (!SwingUtilities.isEventDispatchThread()) {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        webBrowser.executeJavascript(script);
                        logExecuteScript(script, null);
                    }
                });
        } else {
            webBrowser.executeJavascript(script);
            logExecuteScript(script, null);
        }
    }

    private String executeScriptWithResult(final String script) {
        if(script.length() == 0)
            return null;

        final Object[] result = new Object[1];
        if (!SwingUtilities.isEventDispatchThread()) {
            try {
                SwingUtilities.invokeAndWait(new Runnable() {
                    public void run() {
                        result[0] = webBrowser.executeJavascriptWithResult(script);
                    }
                });
            } catch (InterruptedException e) {
                log.severe("Cannot execute script with result: " + e.getMessage());
            } catch (InvocationTargetException e) {
                log.severe("Cannot execute script with result: " + e.getMessage());
            }
        } else {
            result[0] = webBrowser.executeJavascriptWithResult(script);
        }

        logExecuteScript(script, result[0]);
        return result[0] != null ? result[0].toString() : null;
    }

    private String escape(String string) {
        if(string == null)
            return "";
        StringBuffer buffer = new StringBuffer(string);
        for (int i = 0; i < buffer.length(); i++) {
            char c = buffer.charAt(i);
            if (!(Character.isLetterOrDigit(c) || Character.isWhitespace(c) || c == '\'' || c == ',')) {
                buffer.deleteCharAt(i);
                i--;
            }
        }
        return buffer.toString();
    }

    public void setSelectedPositions(int[] selectedPositions) {
        synchronized (notificationMutex) {
            this.selectedPositionIndices = selectedPositions;
            haveToUpdatePosition = true;
            notificationMutex.notifyAll();
        }
    }

    public void setPedestrians(boolean pedestrians) {
        this.pedestrians = pedestrians;
        if(positionsModel.getRoute().getCharacteristics() == RouteCharacteristics.Route)
            update(false);
    }

    public void setAvoidHighways(boolean avoidHighways) {
        this.avoidHighways = avoidHighways;
        if(positionsModel.getRoute().getCharacteristics() == RouteCharacteristics.Route)
            update(false);
    }

    public void print() {
        executeScript("window.print();");
    }

    private static final Pattern DRAG_END_PATTERN = Pattern.compile("^GET /dragend/(.*)/(.*)/(.*) .*$");
    private static final Pattern DIRECTIONS_LOAD_PATTERN = Pattern.compile("^GET /load/(\\d*)/(\\d*) .*$");
    private static final Pattern MAP_TYPE_CHANGED_PATTERN = Pattern.compile("^GET /maptypechanged/(.*) .*$");
    private static final Pattern ZOOM_END_PATTERN = Pattern.compile("^GET /zoomend/(.*)/(.*) .*$");
    private static final Pattern MOVE_END_PATTERN = Pattern.compile("^GET /moveend/(.*)/(.*) .*$");
    private static final Pattern CALLBACK_PATTERN = Pattern.compile("^GET /callback/(\\d+) .*$");

    private void processDragListenerCallBack(List<String> lines) {
        if (!isAuthenticated(lines))
            return;

        for (String line : lines) {
            Matcher dragEndMatcher = DRAG_END_PATTERN.matcher(line);
            if (dragEndMatcher.matches()) {
                int index = Conversion.parseInt(dragEndMatcher.group(1));
                Double latitude = Conversion.parseDouble(dragEndMatcher.group(2));
                Double longitude = Conversion.parseDouble(dragEndMatcher.group(3));
                movedPosition(index, longitude, latitude);
            }

            Matcher directionsLoadMatcher = DIRECTIONS_LOAD_PATTERN.matcher(line);
            if (directionsLoadMatcher.matches()) {
                meters += Conversion.parseInt(directionsLoadMatcher.group(1));
                seconds += Conversion.parseInt(directionsLoadMatcher.group(2));
                calculatedDistance(meters, seconds);
            }

            Matcher mapTypeChangedMatcher = MAP_TYPE_CHANGED_PATTERN.matcher(line);
            if (mapTypeChangedMatcher.matches()) {
                String mapType = mapTypeChangedMatcher.group(1);
                preferences.put(MAP_TYPE_PREFERENCE, mapType);
            }

            Matcher zoomEndMatcher = ZOOM_END_PATTERN.matcher(line);
            if (zoomEndMatcher.matches()) {
                synchronized (notificationMutex) {
                    haveToRepaintImmediately = true;
                    notificationMutex.notifyAll();
                }
            }

            Matcher moveEndMather = MOVE_END_PATTERN.matcher(line);
            if (moveEndMather.matches()) {
                if (getCurrentZoomLevel() >= MAXIMUM_ZOOMLEVEL_FOR_SIGNIFICANCE_CALCULATION) {
                    synchronized (notificationMutex) {
                        haveToRepaintImmediately = true;
                        notificationMutex.notifyAll();
                    }
                }
            }

            Matcher testMatcher = CALLBACK_PATTERN.matcher(line);
            if (testMatcher.matches()) {
                int port = Conversion.parseInt(testMatcher.group(1));
                receivedCallback(port);
            }
        }
    }

    private void movedPosition(int index, Double longitude, Double latitude) {
        BaseNavigationPosition modify = lastSelectedPositions.get(index);
        modify.setLatitude(latitude);
        modify.setLongitude(longitude);

        updateButDontRecenter();

        // notify views about change, leads to update(false)
        int row;
        synchronized (notificationMutex) {
           row = positions.indexOf(modify);
        }
        positionsModel.fireTableRowsUpdated(row, row);

        // give time for repainting of the route
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            // intentionally left empty
        }

        // repaint position marker
        synchronized (notificationMutex) {
            haveToUpdatePosition = true;
            notificationMutex.notifyAll();
        }
    }

    private void updateButDontRecenter() {
        // repaint route immediately, simulates update(true) without recentering
        synchronized (notificationMutex) {
            haveToRepaintImmediately = true;
            significantPositionCache.clear();
            notificationMutex.notifyAll();
        }
    }

    private boolean isAuthenticated(List<String> lines) {
        Map<String, String> map = asMap(lines);
        String host = Conversion.trim(map.get("Host"));
        String id = Conversion.trim(map.get("id"));
        return host != null && host.equals("localhost:" + dragListenerServerSocket.getLocalPort()) &&
                id != null && id.equals("Jx3dQUv4");
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


    public void addMapViewListener(MapViewListener listener) {
        mapViewListeners.add(listener);
    }

    public void removeMapViewListener(MapViewListener listener) {
        mapViewListeners.remove(listener);
    }

    private void calculatedDistance(int meters, int seconds) {
        for (MapViewListener listener : mapViewListeners) {
            listener.calculatedDistance(meters, seconds);
        }
    }

    private void receivedCallback(int port) {
        for (MapViewListener listener : mapViewListeners) {
            listener.receivedCallback(port);
        }
    }
}
