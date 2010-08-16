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

import slash.common.io.Transfer;
import slash.common.io.CompactCalendar;
import slash.navigation.base.RouteCharacteristics;
import slash.navigation.base.BaseNavigationPosition;
import slash.navigation.base.Wgs84Position;
import slash.navigation.converter.gui.models.PositionColumns;
import slash.navigation.gui.Application;
import slash.navigation.converter.gui.models.CharacteristicsModel;
import slash.navigation.converter.gui.models.PositionsModel;
import slash.navigation.util.Positions;
import slash.navigation.util.RouteComments;

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
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Interface for a component that displays the positions of a route.
 *
 * @author Christian Pesch
 */

public abstract class BaseMapView implements MapView {
    protected static final Preferences preferences = Preferences.userNodeForPackage(MapView.class);
    protected static final Logger log = Logger.getLogger(MapView.class.getName());

    private static final String MAP_TYPE_PREFERENCE = "mapType";

    private static final int MAXIMUM_POLYLINE_SEGMENT_LENGTH = preferences.getInt("maximumTrackSegmentLength", 35);
    private static final int MAXIMUM_POLYLINE_POSITION_COUNT = preferences.getInt("maximumTrackPositionCount", 1500);
    private static final int MAXIMUM_DIRECTIONS_SEGMENT_LENGTH = preferences.getInt("maximumRouteSegmentLength", 22);
    private static final int MAXIMUM_DIRECTIONS_POSITION_COUNT = preferences.getInt("maximumRoutePositionCount", 500);
    private static final int MAXIMUM_MARKER_SEGMENT_LENGTH = preferences.getInt("maximumWaypointSegmentLength", 5);
    private static final int MAXIMUM_MARKER_POSITION_COUNT = preferences.getInt("maximumWaypointPositionCount", 40);
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

    private PositionsModel positionsModel;
    private List<BaseNavigationPosition> positions;

    private ServerSocket dragListenerServerSocket;
    private Thread mapViewRouteUpdater, mapViewPositionUpdater, mapViewDragListener;

    protected final Object notificationMutex = new Object();
    protected boolean initialized = false;
    private boolean running = true, recenterAfterZooming, pedestrians, avoidHighways,
            haveToInitializeMapOnFirstStart = true, haveToRepaintSelectionImmediately = false,
            haveToRepaintRouteImmediately = false, haveToRecenterMap = false,
            haveToUpdateRoute = false, haveToReplaceRoute = false,
            haveToUpdatePosition = false, ignoreNextZoomCallback = false;
    private final Map<Integer, BitSet> significantPositionCache = new HashMap<Integer, BitSet>(ZOOMLEVEL_SCALE.length);
    private int meters = 0, seconds = 0;

    // initialization

    public void initialize(PositionsModel positionsModel, CharacteristicsModel characteristicsModel,
                           boolean pedestrians, boolean avoidHighways) {
        initializeBrowser();
        setModel(positionsModel, characteristicsModel);
        this.pedestrians = pedestrians;
        this.avoidHighways = avoidHighways;
    }

    protected abstract void initializeBrowser();

    protected void setModel(PositionsModel positionsModel, CharacteristicsModel characteristicsModel) {
        this.positionsModel = positionsModel;

        positionsModel.addTableModelListener(new TableModelListener() {
            public void tableChanged(TableModelEvent e) {
                boolean insertOrDelete = e.getType() == TableModelEvent.INSERT || e.getType() == TableModelEvent.DELETE;
                boolean allRowsChanged = e.getFirstRow() == 0 && e.getLastRow() == Integer.MAX_VALUE;
                // used to be limited to single rows which did work reliably but with usabilty problems
                // if (e.getFirstRow() == e.getLastRow() && insertOrDelete)
                if (!allRowsChanged && insertOrDelete)
                    updateButDontRecenter();
                else {
                    // ignored updates on columns not displayed
                    if(e.getType() == TableModelEvent.UPDATE &&
                            !(e.getColumn() == PositionColumns.DESCRIPTION_COLUMN_INDEX ||
                                    e.getColumn() == PositionColumns.LONGITUDE_COLUMN_INDEX ||
                                    e.getColumn() == PositionColumns.LATITUDE_COLUMN_INDEX))
                        return;
                    update(allRowsChanged || insertOrDelete);
                }
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

    private Throwable initializationCause = null;

    public Throwable getInitializationCause() {
        return initializationCause;
    }

    protected void setInitializationCause(Throwable initializationCause) {
        this.initializationCause = initializationCause;
    }

    public boolean isInitialized() {
        synchronized (notificationMutex) {
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
                        if (haveToRepaintRouteImmediately ||
                            haveToReplaceRoute ||
                            (haveToUpdateRoute && (currentTime - lastTime > 5 * 1000))) {
                            copiedPositions = filterPositionsWithoutCoordinates(positions);
                            recenter = haveToReplaceRoute;
                            haveToUpdateRoute = false;
                            haveToReplaceRoute = false;
                            haveToRepaintRouteImmediately = false;
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

                        long currentTime = System.currentTimeMillis();
                        if (haveToRecenterMap || haveToRepaintSelectionImmediately ||
                            (haveToUpdatePosition && (currentTime - lastTime > 500))) {
                            haveToRecenterMap = false;
                            recenter = !haveToRepaintSelectionImmediately;
                            haveToRepaintSelectionImmediately = false;
                            haveToUpdatePosition = false;
                            copiedSelectedPositions = new int[selectedPositionIndices.length];
                            System.arraycopy(selectedPositionIndices, 0, copiedSelectedPositions, 0, copiedSelectedPositions.length);
                            copiedPositions = filterPositionsWithoutCoordinates(positions);
                        } else
                            continue;
                    }

                    List<BaseNavigationPosition> selected = reducePositions(copiedPositions, copiedSelectedPositions);
                    selectPositions(selected, recenter);
                    log.info("MapView position updated for " + selected.size() + " positions");
                    lastTime = System.currentTimeMillis();
                }
            }
        }, "MapViewPositionUpdater");
        mapViewPositionUpdater.start();
    }

    private ServerSocket createDragListenerServerSocket() {
        try {
            ServerSocket serverSocket = new ServerSocket(0, 0, InetAddress.getByAddress(new byte[]{127, 0, 0, 1}));
            serverSocket.setSoTimeout(1000);
            int port = serverSocket.getLocalPort();
            log.info("MapView listens on port " + port + " for dragging");
            setDragListenerPort(port);
            return serverSocket;
        } catch (IOException e) {
            log.severe("Cannot open drag listener socket: " + e.getMessage());
            return null;
        }
    }

    protected void initializeDragListener() {
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
                        is = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()), 64 * 1024);
                        os = clientSocket.getOutputStream();
                        boolean processingPost = false, processingBody = false;
                        while (true) {
                            try {
                                String line = Transfer.trim(is.readLine());
                                if (line == null) {
                                    if(processingPost && !processingBody) {
                                        processingBody = true;
                                        continue;
                                    } else
                                        break;
                                }
                                if(line.startsWith("POST"))
                                    processingPost = true;
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

    protected void checkLocalhostResolution() {
        try {
            InetAddress localhost = InetAddress.getByName("localhost");
            log.info("localhost is resolved to: " + localhost);
            String localhostName = localhost.getHostAddress();
            log.info("IP of localhost is: " + localhostName);
            if(!localhostName.equals("127.0.0.1"))
                throw new Exception("localhost does not resolve to 127.0.0.1");

            InetAddress ip = InetAddress.getByAddress(new byte[]{127, 0, 0, 1});
            log.info("127.0.0.1 is resolved to: " + ip);
            String ipName = localhost.getHostName();
            log.info("Name of 127.0.0.1 is: " + ipName);
            if(!ipName.equals("localhost"))
                throw new Exception("127.0.0.1 does not resolve to localhost");
        } catch (Exception e) {
            e.printStackTrace();
            final String message = "Probably faulty network setup: " + e.getMessage() + ".\nPlease check your network settings.";
            log.severe(message);
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    JOptionPane.showMessageDialog(getComponent(), message, "Error", JOptionPane.ERROR_MESSAGE);
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

        new Thread(new Runnable() {
            public void run() {
                addMapViewListener(callbackWaiter);
                try {
                    executeScript("checkCallback();");

                    long start = System.currentTimeMillis();
                    while(true) {
                        synchronized (receivedCallback) {
                            if(receivedCallback[0])
                                break;
                        }

                        if (start + 5000 < System.currentTimeMillis())
                            break;

                        try {
                            Thread.sleep(50);
                        } catch (InterruptedException e) {
                            // intentionally left empty
                        }
                    }

                    synchronized (receivedCallback) {
                        if(!receivedCallback[0])
                            SwingUtilities.invokeLater(new Runnable() {
                                public void run() {
                                    String message = "Unable to call RouteConverter from Webbrowser via port " + dragListenerServerSocket.getLocalPort() + ".\nPlease check your firewall settings.";
                                    log.severe(message);
                                    JOptionPane.showMessageDialog(getComponent(), message, "Error", JOptionPane.ERROR_MESSAGE);
                                }
                            });
                    }
                }
                finally {
                    removeMapViewListener(callbackWaiter);
                }
            }
        } , "CallbackChecker").start();
    }

    // disposal

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

        disposeBrowser();

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

    protected abstract void disposeBrowser();

    // getter and setter

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

    protected boolean isVisible() {
        return getComponent().getWidth() > 0;
    }

    private boolean hasPositions() {
        synchronized (notificationMutex) {
            return isInitialized() && positions != null;
        }
    }

    private void setDragListenerPort(int dragListenerPort) {
        synchronized (notificationMutex) {
            executeScript("setDragListenerPort(" + dragListenerPort + ")");
        }
    }

    public void setSelectedPositions(int[] selectedPositions) {
        synchronized (notificationMutex) {
            this.selectedPositionIndices = selectedPositions;
            haveToUpdatePosition = true;
            notificationMutex.notifyAll();
        }
    }

    public void setRecenterAfterZooming(boolean recenterAfterZooming) {
        this.recenterAfterZooming = recenterAfterZooming;
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

    public BaseNavigationPosition getCenter() {
        BaseNavigationPosition northEast = getNorthEastBounds();
        BaseNavigationPosition southWest = getSouthWestBounds();
        return northEast != null && southWest != null ? Positions.center(Arrays.asList(northEast, southWest)) : null;
    }

    protected abstract BaseNavigationPosition getNorthEastBounds();
    protected abstract BaseNavigationPosition getSouthWestBounds();

    protected BaseNavigationPosition getBounds(String script) {
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

    // reduction of positions

    private BitSet calculateSignificantPositionsForZoomLevel(List<BaseNavigationPosition> positions, int zoomLevel) {
        BitSet significant = significantPositionCache.get(zoomLevel);
        if (significant == null) {
            significant = new BitSet(positions.size());

            if (zoomLevel <= MAXIMUM_ZOOMLEVEL_FOR_SIGNIFICANCE_CALCULATION) {
                double threshold = ZOOMLEVEL_SCALE[zoomLevel] / 2500.0;
                long start = System.currentTimeMillis();
                int[] significantPositions = Positions.getSignificantPositions(positions, threshold);
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

    private List<BaseNavigationPosition> filterSignificantPositions(List<BaseNavigationPosition> positions, boolean recenter) {
        int zoomLevel = recenter ? getBoundsZoomLevel(positions) : getCurrentZoomLevel();

        BitSet pointStatus = calculateSignificantPositionsForZoomLevel(positions, zoomLevel);

        List<BaseNavigationPosition> result = new ArrayList<BaseNavigationPosition>();
        for (int i = 0; i < positions.size(); i++)
            if (pointStatus.get(i))
                result.add(positions.get(i));
        return result;
    }

    private List<BaseNavigationPosition> reducePositions(List<BaseNavigationPosition> positions, boolean recenter, int maximumPositionCount) {
        if (positions.size() < 2)
            return positions;

        // determine significant positions for this zoom level
        positions = filterSignificantPositions(positions, recenter);

        // reduce the number of significant positions by a visibility heuristic
        if (positions.size() > maximumPositionCount)
            positions = filterVisiblePositions(positions, 2.5);

        // reduce the number of visible positions by a JS-stability heuristic
        if (positions.size() > maximumPositionCount)
            positions = filterEveryNthPosition(positions, maximumPositionCount);

        return positions;
    }

    private List<BaseNavigationPosition> reducePositions(List<BaseNavigationPosition> positions, int[] indices) {
        // reduced selected positions if they're not selected
        positions = filterSelectedPositions(positions, indices);

        // reduce the number of selected positions by a visibility heuristic
        if (positions.size() > MAXIMUM_SELECTION_COUNT)
            positions = filterVisiblePositions(positions, 1.25);

        // reduce the number of visible positions by a JS-stability heuristic
        if (positions.size() > MAXIMUM_SELECTION_COUNT)
            positions = filterEveryNthPosition(positions, MAXIMUM_SELECTION_COUNT);

        return positions;  
    }

    protected abstract int getBoundsZoomLevel(List<BaseNavigationPosition> positions);
    protected abstract int getCurrentZoomLevel();

    private List<BaseNavigationPosition> filterVisiblePositions(List<BaseNavigationPosition> positions, double factor) {
        BaseNavigationPosition northEast = getNorthEastBounds();
        BaseNavigationPosition southWest = getSouthWestBounds();
        if (northEast == null || southWest == null)
            return positions;

        // heuristic: increase bounds for visible positions to enable dragging the map
        // at the same zoom level, with a factor of 2 you hardly see the cropping even
        // with a small map and a big screen (meaning lots of space to drag the map)
        double width = (northEast.getLongitude() - southWest.getLongitude()) * factor;
        double height = (southWest.getLatitude() - northEast.getLatitude()) * factor;
        northEast.setLongitude(northEast.getLongitude() + width);
        northEast.setLatitude(northEast.getLatitude() - height);
        southWest.setLongitude(southWest.getLongitude() - width);
        southWest.setLatitude(southWest.getLatitude() + height);

        List<BaseNavigationPosition> result = new ArrayList<BaseNavigationPosition>();
        result.add(positions.get(0));

        for (int i = 1; i < positions.size() - 1; i += 1) {
            BaseNavigationPosition position = positions.get(i);
            if (Positions.contains(northEast, southWest, position)) {
                result.add(position);
            }
        }

        result.add(positions.get(positions.size() - 1));

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

    // draw on map

    protected void update(boolean haveToReplaceRoute) {
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
            log.fine(System.currentTimeMillis() + " haveToUpdateRoute: " + haveToUpdateRoute +
                    " haveToReplaceRoute: " + haveToReplaceRoute + " positions: " + positions.size());
            notificationMutex.notifyAll();
        }
    }

    private void updateButDontRecenter() {
        // repaint route immediately, simulates update(true) without recentering
        synchronized (notificationMutex) {
            haveToRepaintRouteImmediately = true;
            significantPositionCache.clear();
            notificationMutex.notifyAll();
        }
    }

    private void removeOverlays() {
        executeScript("removeOverlays();\nremoveDirections();");
    }

    private void addDirectionsToMap(List<BaseNavigationPosition> positions) {
        meters = 0;
        seconds = 0;

        // avoid throwing javascript exceptions if there is nothing to direct
        if(positions.size() < 2) {
            addMarkersToMap(positions);
            return;
        }

        int directionsCount = Transfer.ceiling(positions.size(), MAXIMUM_DIRECTIONS_SEGMENT_LENGTH, false);
        for (int j = 0; j < directionsCount; j++) {
            StringBuffer buffer = new StringBuffer();
            buffer.append("var latlngs = [");
            int maximum = Math.min(positions.size(), (j + 1) * MAXIMUM_DIRECTIONS_SEGMENT_LENGTH + 1);
            for (int i = j * MAXIMUM_DIRECTIONS_SEGMENT_LENGTH; i < maximum; i++) {
                BaseNavigationPosition position = positions.get(i);
                buffer.append("new GLatLng(").append(position.getLatitude()).append(",").append(position.getLongitude()).append(")");
                if (i < maximum - 1)
                    buffer.append(",");
            }
            buffer.append("];\n");
            buffer.append("createDirections().loadFromWaypoints(latlngs, ").
                   append("{ preserveViewport: true, getPolyline: true, avoidHighways: ").
                    append(avoidHighways).append(", travelMode: ").
                    append(pedestrians ? "G_TRAVEL_MODE_WALKING" : "G_TRAVEL_MODE_DRIVING").
                    append(", locale: '").append(Locale.getDefault()).append("'").
                    append(" });");
            executeScript(buffer.toString());
        }
        removeOverlays();
    }

    private void addPolylinesToMap(final List<BaseNavigationPosition> positions) {
        int polylinesCount = Transfer.ceiling(positions.size(), MAXIMUM_POLYLINE_SEGMENT_LENGTH, true);
        for (int j = 0; j < polylinesCount; j++) {
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
            buffer.append("addOverlay(new GPolyline(latlngs,\"#0033FF\",2,1));");
            executeScript(buffer.toString());
        }
        removeOverlays();
   }

    private void addMarkersToMap(List<BaseNavigationPosition> positions) {
        int markersCount = Transfer.ceiling(positions.size(), MAXIMUM_MARKER_SEGMENT_LENGTH, false);
        for (int j = 0; j < markersCount; j++) {
            StringBuffer buffer = new StringBuffer();

            int maximum = Math.min(positions.size(), (j + 1) * MAXIMUM_MARKER_SEGMENT_LENGTH);
            for (int i = j * MAXIMUM_MARKER_SEGMENT_LENGTH; i < maximum; i++) {
                BaseNavigationPosition position = positions.get(i);
                buffer.append("var marker = new GMarker(new GLatLng(").
                        append(position.getLatitude()).append(",").append(position.getLongitude()).
                        append("), { title: \"").append(escape(position.getComment())).append("\", ").
                        append("clickable: false, icon: markerIcon });\n");
                buffer.append("addOverlay(marker);\n");
            }
            executeScript(buffer.toString());
        }
        removeOverlays();
    }

    private void setCenterOfMap(List<BaseNavigationPosition> positions, boolean recenter) {
        StringBuffer buffer = new StringBuffer();
        // set map type only on first start
        if (haveToInitializeMapOnFirstStart) {
            String mapType = preferences.get(MAP_TYPE_PREFERENCE, "Map");
            buffer.append("setMapType(\"").append(mapType).append("\");\n");
        }

        // if there are positions center on first start or if we have to recenter
        if (positions.size() > 0 && (haveToInitializeMapOnFirstStart || recenter)) {
            Wgs84Position northEast = Positions.northEast(positions);
            Wgs84Position southWest = Positions.southWest(positions);
            buffer.append("var zoomLevel = map.getBoundsZoomLevel(new GLatLngBounds(").
                    append("new GLatLng(").append(northEast.getLatitude()).append(",").append(northEast.getLongitude()).append("),").
                    append("new GLatLng(").append(southWest.getLatitude()).append(",").append(southWest.getLongitude()).append(")").
                    append("));\n");
            Wgs84Position center = Positions.center(positions);
            buffer.append("map.setCenter(new GLatLng(").append(center.getLatitude()).append(",").
                    append(center.getLongitude()).append("), zoomLevel);");
            ignoreNextZoomCallback = true;
        }
        executeScript(buffer.toString());
        haveToInitializeMapOnFirstStart = false;
    }

    private int lastSelectedPositionCount = -1;
    private List<BaseNavigationPosition> lastSelectedPositions;
    private int[] selectedPositionIndices = new int[0];

    private void selectPositions(List<BaseNavigationPosition> selectedPositions, boolean recenter) {
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
                    .append(escape(selectedPosition.getComment())).append("\", draggable: true, bouncy: false, dragCrossMove: true });\n");
            buffer.append("addMarker(selected").append(i).append(",").append(i).append(");\n");
        }

        // pan to first position
        if (lastSelectedPositionCount > 0 && recenter) {
            BaseNavigationPosition center = selectedPositions.get(0);
            buffer.append("centerMap(new GLatLng(").append(center.getLatitude()).append(",").
                    append(center.getLongitude()).append("));");
        }
        executeScript(buffer.toString());
    }

    private final Map<Integer, List<BaseNavigationPosition>> insertWaypointsQueue = new HashMap<Integer, List<BaseNavigationPosition>>();

    private void insertWaypoints(final String mode, int[] startPositions) {
        final Map<Integer, List<BaseNavigationPosition>> addToQueue = new HashMap<Integer, List<BaseNavigationPosition>>();
        Random random = new Random();
        synchronized (notificationMutex) {
            for (int i = 0; i < startPositions.length; i++) {
                // skip the very last position without successor
                if (i == positions.size() - 1)
                    continue;
                List<BaseNavigationPosition> successorPredecessor = new ArrayList<BaseNavigationPosition>();
                successorPredecessor.add(positions.get(startPositions[i]));
                successorPredecessor.add(positions.get(startPositions[i] + 1));
                addToQueue.put(random.nextInt(), successorPredecessor);
           }
        }

        synchronized (insertWaypointsQueue) {
            insertWaypointsQueue.putAll(addToQueue);
        }

        new Thread(new Runnable() {
            public void run() {
                for (Integer key : addToQueue.keySet()) {
                    List<BaseNavigationPosition> successorPredecessor = addToQueue.get(key);
                    BaseNavigationPosition from = successorPredecessor.get(0);
                    BaseNavigationPosition to = successorPredecessor.get(1);
                    StringBuffer buffer = new StringBuffer();
                    buffer.append("var latlngs = [");
                    buffer.append("new GLatLng(").append(from.getLatitude()).append(",").append(from.getLongitude()).append("),");
                    buffer.append("new GLatLng(").append(to.getLatitude()).append(",").append(to.getLongitude()).append(")");
                    buffer.append("];\n");
                    buffer.append(mode).append("(").append(key).append(").loadFromWaypoints(latlngs, ").
                            append("{ preserveViewport: true, getPolyline: true, getSteps: true, avoidHighways: ").
                            append(avoidHighways).append(", locale: '").append(Locale.getDefault()).append("'").
                            append(" });");
                    executeScript(buffer.toString());
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        // don't care if this happens
                    }
                }
            }
        }, "WaypointsInserter").start();
    }

    // call Google Maps API functions

    public void insertAllWaypoints(int[] startPositions) {
        insertWaypoints("insertAllWaypoints", startPositions);
    }

    public void insertOnlyTurnpoints(int[] startPositions) {
        insertWaypoints("insertOnlyTurnpoints", startPositions);
    }

    public void print(boolean withRoute) {
        executeScript("printMap(" + withRoute + ");");
    }

    // script execution

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

    protected void logExecuteScript(String script, Object result) {
        String output = System.currentTimeMillis() + " executing script '" + script + (result != null ? "' with result '" + result : "") + "'";
        log.fine(output);
    }

    protected abstract void executeScript(String script);
    protected abstract String executeScriptWithResult(String script);

    // browser callbacks

    private static final Pattern DIRECTIONS_LOAD_PATTERN = Pattern.compile("^GET /load/(\\d*)/(\\d*) .*$");
    private static final Pattern INSERT_POSITION_PATTERN = Pattern.compile("^GET /insert-position/(.*)/(.*) .*$");
    private static final Pattern MOVE_POSITION_PATTERN = Pattern.compile("^GET /move-position/(.*)/(.*)/(.*) .*$");
    private static final Pattern REMOVE_POSITION_PATTERN = Pattern.compile("^GET /remove-position/(.*) .*$");
    private static final Pattern MAP_TYPE_CHANGED_PATTERN = Pattern.compile("^GET /maptypechanged/(.*) .*$");
    private static final Pattern ZOOM_END_PATTERN = Pattern.compile("^GET /zoomend/(.*)/(.*) .*$");
    private static final Pattern MOVE_END_PATTERN = Pattern.compile("^GET /moveend/(.*)/(.*) .*$");
    private static final Pattern CALLBACK_PATTERN = Pattern.compile("^GET /callback/(\\d+) .*$");
    private static final Pattern INSERT_WAYPOINTS_PATTERN = Pattern.compile("^(Insert-All-Waypoints|Insert-Only-Turnpoints): (-?\\d+)/(.*)$");

    private void processDragListenerCallBack(List<String> lines) {
        if (!isAuthenticated(lines))
            return;

        for (String line : lines) {
            Matcher directionsLoadMatcher = DIRECTIONS_LOAD_PATTERN.matcher(line);
            if (directionsLoadMatcher.matches()) {
                meters += Transfer.parseInt(directionsLoadMatcher.group(1));
                seconds += Transfer.parseInt(directionsLoadMatcher.group(2));
                fireCalculatedDistance(meters, seconds);
            }

            Matcher insertPositionMatcher = INSERT_POSITION_PATTERN.matcher(line);
            if (insertPositionMatcher.matches()) {
                Double latitude = Transfer.parseDouble(insertPositionMatcher.group(1));
                Double longitude = Transfer.parseDouble(insertPositionMatcher.group(2));
                insertPosition(longitude, latitude);
            }

            Matcher moePositionMatcher = MOVE_POSITION_PATTERN.matcher(line);
            if (moePositionMatcher.matches()) {
                int index = Transfer.parseInt(moePositionMatcher.group(1));
                Double latitude = Transfer.parseDouble(moePositionMatcher.group(2));
                Double longitude = Transfer.parseDouble(moePositionMatcher.group(3));
                movePosition(index, longitude, latitude);
            }

            Matcher removePositionMatcher = REMOVE_POSITION_PATTERN.matcher(line);
            if (removePositionMatcher.matches()) {
                int index = Transfer.parseInt(removePositionMatcher.group(1));
                removePosition(index);
            }

            Matcher mapTypeChangedMatcher = MAP_TYPE_CHANGED_PATTERN.matcher(line);
            if (mapTypeChangedMatcher.matches()) {
                String mapType = mapTypeChangedMatcher.group(1);
                preferences.put(MAP_TYPE_PREFERENCE, mapType);
            }

            Matcher zoomEndMatcher = ZOOM_END_PATTERN.matcher(line);
            if (zoomEndMatcher.matches()) {
                synchronized (notificationMutex) {
                    // since setCenter() leads to a callback and thus paints the track twice
                    if (ignoreNextZoomCallback)
                        ignoreNextZoomCallback = false;
                    else
                        haveToRepaintRouteImmediately = true;
                    // if enabled, recenter map to selected positions after zooming
                    if (recenterAfterZooming)
                        haveToRecenterMap = true;
                    haveToRepaintSelectionImmediately = true;
                    notificationMutex.notifyAll();
                }
            }

            Matcher moveEndMather = MOVE_END_PATTERN.matcher(line);
            if (moveEndMather.matches()) {
                if (getCurrentZoomLevel() >= MAXIMUM_ZOOMLEVEL_FOR_SIGNIFICANCE_CALCULATION) {
                    synchronized (notificationMutex) {
                        haveToRepaintRouteImmediately = true;
                        haveToRepaintSelectionImmediately = true;
                        notificationMutex.notifyAll();
                    }
                }
            }

            Matcher testMatcher = CALLBACK_PATTERN.matcher(line);
            if (testMatcher.matches()) {
                int port = Transfer.parseInt(testMatcher.group(1));
                fireReceivedCallback(port);
            }

            Matcher insertWaypointsMatcher = INSERT_WAYPOINTS_PATTERN.matcher(line);
            if (insertWaypointsMatcher.matches()) {
                Integer key = Transfer.parseInt(insertWaypointsMatcher.group(2));
                List<Double> coordinates = parseCoordinates(insertWaypointsMatcher.group(3));

                List<BaseNavigationPosition> successorPredecessor;
                synchronized (insertWaypointsQueue) {
                    successorPredecessor = insertWaypointsQueue.remove(key);
                }

                if(coordinates.size() < 4 || successorPredecessor == null)
                   break;

                BaseNavigationPosition before = successorPredecessor.get(0);
                synchronized (notificationMutex) {
                    int index = positions.indexOf(before) + 1;
                    for (int i = coordinates.size() - 1; i > 0; i -= 4) {
                        // Double seconds = coordinates.get(i); Double meters = coordinates.get(i - 1);
                        positionsModel.add(index, coordinates.get(i - 2), coordinates.get(i - 3), null, null, null, null);
                    }
                }

                RouteComments.commentPositions(positions);
            }
        }
    }

    private List<Double> parseCoordinates(String coordinates) {
        List<Double> result = new ArrayList<Double>();
        StringTokenizer tokenizer = new StringTokenizer(coordinates, "/");
        while (tokenizer.hasMoreTokens()) {
            Double longitude = Transfer.parseDouble(tokenizer.nextToken());
            if (tokenizer.hasMoreTokens()) {
                Double latitude = Transfer.parseDouble(tokenizer.nextToken());
                if (tokenizer.hasMoreTokens()) {
                    Double meters = Transfer.parseDouble(tokenizer.nextToken());
                    if (tokenizer.hasMoreTokens()) {
                        Double seconds = Transfer.parseDouble(tokenizer.nextToken());
                        result.add(longitude);
                        result.add(latitude);
                        result.add(meters);
                        result.add(seconds);
                    }
                }
            }
        }
        return result;
    }

    private void insertPosition(Double longitude, Double latitude) {
        BaseNavigationPosition position = lastSelectedPositions.size() > 0 ? lastSelectedPositions.get(lastSelectedPositions.size() - 1) : null;
        // TODO crude logic, or?
        if (position == null && positionsModel.getRowCount() > 0)
            position = positionsModel.getPosition(positionsModel.getRowCount() - 1);
        int row = 0;
        if (position != null)
            row = positionsModel.getIndex(position) + 1;
        positionsModel.add(row, longitude, latitude, null, null, CompactCalendar.fromCalendar(Calendar.getInstance()), Application.getInstance().getContext().getBundle().getString("new-position-comment"));
        fireSelectedPosition(row);
    }

    private void movePosition(int index, Double longitude, Double latitude) {
        BaseNavigationPosition position = lastSelectedPositions.get(index);
        position.setLatitude(latitude);
        position.setLongitude(longitude);

        updateButDontRecenter();

        // notify views about change, leads to update(false)
        int row;
        synchronized (notificationMutex) {
           row = positions.indexOf(position);
        }
        // updating all rows behind the modified is quite expensive, but necessary due to the distance
        // calculation - if that didn't exist the single update of row would be sufficient
        positionsModel.fireTableRowsUpdated(row, positions.size() - 1, PositionColumns.LONGITUDE_COLUMN_INDEX);

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

    private void removePosition(int index) {
        BaseNavigationPosition position = lastSelectedPositions.size() > index ? lastSelectedPositions.get(index) : null;
        if (position != null) {
            int row = positionsModel.getIndex(position);
            positionsModel.remove(new int[]{row});

            synchronized (notificationMutex) {
                haveToRepaintRouteImmediately = true;
                notificationMutex.notifyAll();
            }
        }
    }

    private boolean isAuthenticated(List<String> lines) {
        Map<String, String> map = asMap(lines);
        String host = Transfer.trim(map.get("Host"));
        String id = Transfer.trim(map.get("id"));
        return host != null && host.equals("127.0.0.1:" + dragListenerServerSocket.getLocalPort()) &&
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

    private void fireSelectedPosition(int index) {
        for (MapViewListener listener : mapViewListeners) {
            listener.selectedPosition(index);
        }
    }
}
