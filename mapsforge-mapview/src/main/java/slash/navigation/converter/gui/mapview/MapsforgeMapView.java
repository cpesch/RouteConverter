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

import org.mapsforge.core.graphics.Bitmap;
import org.mapsforge.core.graphics.Paint;
import org.mapsforge.core.model.Dimension;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.MapPosition;
import org.mapsforge.map.layer.Layer;
import org.mapsforge.map.layer.LayerManager;
import org.mapsforge.map.layer.Layers;
import org.mapsforge.map.layer.cache.InMemoryTileCache;
import org.mapsforge.map.layer.cache.TileCache;
import org.mapsforge.map.layer.download.TileDownloadLayer;
import org.mapsforge.map.layer.download.tilesource.TileSource;
import org.mapsforge.map.layer.overlay.Marker;
import org.mapsforge.map.layer.renderer.TileRendererLayer;
import org.mapsforge.map.model.MapViewPosition;
import org.mapsforge.map.model.common.Observer;
import org.mapsforge.map.util.MapViewProjection;
import slash.navigation.base.BaseRoute;
import slash.navigation.base.RouteCharacteristics;
import slash.navigation.common.BoundingBox;
import slash.navigation.common.LongitudeAndLatitude;
import slash.navigation.common.NavigationPosition;
import slash.navigation.converter.gui.mapview.helpers.MapViewComponentListener;
import slash.navigation.converter.gui.mapview.helpers.MapViewMouseEventListener;
import slash.navigation.converter.gui.mapview.lines.Line;
import slash.navigation.converter.gui.mapview.lines.Polyline;
import slash.navigation.converter.gui.mapview.updater.*;
import slash.navigation.converter.gui.models.CharacteristicsModel;
import slash.navigation.converter.gui.models.PositionsModel;
import slash.navigation.converter.gui.models.PositionsSelectionModel;
import slash.navigation.converter.gui.models.UnitSystemModel;
import slash.navigation.gui.Application;
import slash.navigation.gui.actions.ActionManager;
import slash.navigation.gui.actions.FrameAction;
import slash.navigation.maps.LocalMap;
import slash.navigation.maps.Theme;
import slash.navigation.routing.DownloadFuture;
import slash.navigation.routing.RoutingResult;
import slash.navigation.routing.RoutingService;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

import static java.lang.Integer.MAX_VALUE;
import static java.lang.Math.abs;
import static java.text.MessageFormat.format;
import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static javax.swing.JOptionPane.ERROR_MESSAGE;
import static javax.swing.JOptionPane.showMessageDialog;
import static javax.swing.event.TableModelEvent.*;
import static org.mapsforge.core.graphics.Color.BLUE;
import static org.mapsforge.core.util.LatLongUtils.zoomForBounds;
import static org.mapsforge.core.util.MercatorProjection.calculateGroundResolution;
import static slash.navigation.base.RouteCharacteristics.Waypoints;
import static slash.navigation.converter.gui.mapview.AwtGraphicMapView.GRAPHIC_FACTORY;
import static slash.navigation.converter.gui.models.PositionColumns.*;
import static slash.navigation.gui.helpers.JMenuHelper.createItem;
import static slash.navigation.gui.helpers.JTableHelper.isFirstToLastRow;
import static slash.navigation.maps.helpers.MapTransfer.*;

/**
 * Implementation for a component that displays the positions of a position list on a map
 * using the rewrite branch of the mapsforge project.
 *
 * @author Christian Pesch
 */

public class MapsforgeMapView implements MapView {
    private static final Preferences preferences = Preferences.userNodeForPackage(MapsforgeMapView.class);
    private static final Logger log = Logger.getLogger(MapsforgeMapView.class.getName());

    private static final String CENTER_LATITUDE_PREFERENCE = "centerLatitude";
    private static final String CENTER_LONGITUDE_PREFERENCE = "centerLongitude";
    private static final String CENTER_ZOOM_PREFERENCE = "centerZoom";

    private PositionsModel positionsModel;
    private PositionsSelectionModel positionsSelectionModel;
    private CharacteristicsModel characteristicsModel;
    private MapViewCallback mapViewCallback;
    private UnitSystemModel unitSystemModel;

    private MapSelector mapSelector;
    private AwtGraphicMapView mapView;
    private MapViewMouseEventListener mapViewMouseEventListener;
    private static Bitmap markerIcon, waypointIcon;
    private static Paint TRACK_PAINT, ROUTE_PAINT, ROUTE_DOWNLOADING_PAINT;
    private SelectionUpdater selectionUpdater;
    private EventMapUpdater eventMapUpdater, routeUpdater, trackUpdater, waypointUpdater;
    private ExecutorService executor = newSingleThreadExecutor();

    private boolean recenterAfterZooming;

    // initialization

    public void initialize(PositionsModel positionsModel,
                           PositionsSelectionModel positionsSelectionModel,
                           CharacteristicsModel characteristicsModel,
                           MapViewCallback mapViewCallback,
                           boolean recenterAfterZooming,
                           boolean showCoordinates, boolean showWaypointDescription,
                           TravelMode travelMode, boolean avoidHighways, boolean avoidTolls,
                           UnitSystemModel unitSystemModel) {
        this.mapViewCallback = mapViewCallback;
        setModel(positionsModel, positionsSelectionModel, characteristicsModel, unitSystemModel);
        initializeActions();
        initializeMapView();
        this.recenterAfterZooming = recenterAfterZooming;
    }

    private void initializeActions() {
        ActionManager actionManager = Application.getInstance().getContext().getActionManager();
        actionManager.register("select-position", new SelectPositionAction());
        actionManager.register("extend-selection", new ExtendSelectionAction());
        actionManager.register("add-position", new AddPositionAction());
        actionManager.register("delete-position", new DeletePositionAction());
        actionManager.register("center-here", new CenterAction());
        actionManager.register("zoom-in", new ZoomAction(+1));
        actionManager.register("zoom-out", new ZoomAction(-1));
    }

    private void initializeMapView() {
        mapView = createMapView();

        mapViewMouseEventListener = new MapViewMouseEventListener(mapView, createPopupMenu());
        mapView.addMouseListener(mapViewMouseEventListener);
        mapView.addMouseMotionListener(mapViewMouseEventListener);
        mapView.addMouseWheelListener(mapViewMouseEventListener);

        try {
            markerIcon = GRAPHIC_FACTORY.createResourceBitmap(MapsforgeMapView.class.getResourceAsStream("marker.png"), -1);
            waypointIcon = GRAPHIC_FACTORY.createResourceBitmap(MapsforgeMapView.class.getResourceAsStream("waypoint.png"), -1);
        } catch (IOException e) {
            log.severe("Cannot create marker and waypoint icon: " + e);
        }
        TRACK_PAINT = GRAPHIC_FACTORY.createPaint();
        TRACK_PAINT.setColor(BLUE);
        TRACK_PAINT.setStrokeWidth(3);
        ROUTE_PAINT = GRAPHIC_FACTORY.createPaint();
        ROUTE_PAINT.setColor(0x993379FF);
        ROUTE_PAINT.setStrokeWidth(5);
        ROUTE_DOWNLOADING_PAINT = GRAPHIC_FACTORY.createPaint();
        ROUTE_DOWNLOADING_PAINT.setColor(0x993379FF);
        ROUTE_DOWNLOADING_PAINT.setStrokeWidth(5);
        ROUTE_DOWNLOADING_PAINT.setDashPathEffect(new float[]{3, 12});

        mapSelector = new MapSelector(mapViewCallback.getMapManager(), mapView);

        final MapViewPosition mapViewPosition = mapView.getModel().mapViewPosition;
        mapViewPosition.addObserver(new Observer() {
            public void onChange() {
                mapSelector.zoomChanged(mapViewPosition.getZoomLevel());
            }
        });

        double longitude = preferences.getDouble(CENTER_LONGITUDE_PREFERENCE, -25.0);
        double latitude = preferences.getDouble(CENTER_LATITUDE_PREFERENCE, 35.0);
        byte zoom = (byte) preferences.getInt(CENTER_ZOOM_PREFERENCE, 2);
        mapViewPosition.setMapPosition(new MapPosition(new LatLong(latitude, longitude), zoom));

        mapView.getModel().mapViewDimension.addObserver(new Observer() {
            private boolean initialized = false;

            public void onChange() {
                if (!initialized) {
                    handleMapAndThemeUpdate(true, true);
                    initialized = true;
                }
            }
        });

        mapViewCallback.getMapManager().getDisplayedMapModel().addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                handleMapAndThemeUpdate(true, !isVisible(mapView.getModel().mapViewPosition.getCenter(), 20));
            }
        });
        mapViewCallback.getMapManager().getAppliedThemeModel().addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                handleMapAndThemeUpdate(false, false);
            }
        });
    }

    private AwtGraphicMapView createMapView() {
        AwtGraphicMapView mapView = new AwtGraphicMapView();
        mapView.getMapScaleBar().setVisible(true);
        mapView.addComponentListener(new MapViewComponentListener(mapView, mapView.getModel().mapViewDimension));
        return mapView;
    }

    private JPopupMenu createPopupMenu() {
        JPopupMenu menu = new JPopupMenu();
        menu.add(createItem("select-position"));
        menu.add(createItem("add-position"));    // TODO should be "new-position"
        menu.add(createItem("delete-position")); // TODO should be "delete"
        menu.addSeparator();
        menu.add(createItem("center-here"));
        menu.add(createItem("zoom-in"));
        menu.add(createItem("zoom-out"));
        return menu;
    }

    private TileRendererLayer createTileRendererLayer(LocalMap map, Theme theme) {
        TileRendererLayer tileRendererLayer = new TileRendererLayer(createTileCache(), mapView.getModel().mapViewPosition, false, GRAPHIC_FACTORY);
        tileRendererLayer.setMapFile(map.getFile());
        tileRendererLayer.setXmlRenderTheme(theme.getXmlRenderTheme());
        return tileRendererLayer;
    }

    private TileDownloadLayer createTileDownloadLayer(TileSource tileSource) {
        return new TileDownloadLayer(createTileCache(), mapView.getModel().mapViewPosition, tileSource, GRAPHIC_FACTORY);
    }

    private TileCache createTileCache() {
        TileCache firstLevelTileCache = new InMemoryTileCache(64);
        // TODO think about replacing with file system cache that survives restarts
        // File cacheDirectory = new File(System.getProperty("java.io.tmpdir"), "mapsforge");
        // TileCache secondLevelTileCache = new FileSystemTileCache(1024, cacheDirectory, GRAPHIC_FACTORY);
        // return new TwoLevelTileCache(firstLevelTileCache, secondLevelTileCache);
        return firstLevelTileCache;
    }

    protected void setModel(PositionsModel positionsModel,
                            PositionsSelectionModel positionsSelectionModel,
                            CharacteristicsModel characteristicsModel,
                            UnitSystemModel unitSystemModel) {
        this.positionsModel = positionsModel;
        this.positionsSelectionModel = positionsSelectionModel;
        this.characteristicsModel = characteristicsModel;
        this.unitSystemModel = unitSystemModel;

        this.selectionUpdater = new SelectionUpdater(positionsModel, new SelectionOperation() {
            public void add(List<PositionWithLayer> positionWithLayers) {
                LatLong center = null;
                for (PositionWithLayer positionWithLayer : positionWithLayers) {
                    LatLong position = asLatLong(positionWithLayer.getPosition());
                    Marker marker = new Marker(position, markerIcon, 8, -16);
                    positionWithLayer.setLayer(marker);
                    getLayerManager().getLayers().add(marker);
                    center = position;
                }
                if (center != null)
                    setCenter(center, false);
            }

            public void remove(List<PositionWithLayer> positionWithLayers) {
                for (PositionWithLayer positionWithLayer : positionWithLayers) {
                    Layer layer = positionWithLayer.getLayer();
                    if (layer != null)
                        getLayerManager().getLayers().remove(layer);
                    else
                        log.warning("Could not find layer for selection position " + positionWithLayer);
                    positionWithLayer.setLayer(null);
                }
            }
        });

        this.routeUpdater = new TrackUpdater(positionsModel, new TrackOperation() {
            private java.util.Map<PairWithLayer, Double> pairsToDistances = new HashMap<PairWithLayer, Double>();
            private java.util.Map<PairWithLayer, Long> pairsToTimes = new HashMap<PairWithLayer, Long>();

            public void add(final List<PairWithLayer> pairWithLayers) {
                internalAdd(pairWithLayers);
            }

            public void update(List<PairWithLayer> pairWithLayers) {
                internalRemove(pairWithLayers);
                internalAdd(pairWithLayers);
            }

            public void remove(List<PairWithLayer> pairWithLayers) {
                internalRemove(pairWithLayers);
                updateSelectionAfterRemove(pairWithLayers);
            }

            private void internalAdd(final List<PairWithLayer> pairWithLayers) {
                executor.execute(new Runnable() {
                    public void run() {
                        final DownloadFuture future = mapViewCallback.getRoutingService().downloadRoutingDataFor(asLongitudeAndLatitude(pairWithLayers));
                        if (future.isRequiresDownload() || future.isRequiresProcessing()) {
                            drawBeeline(pairWithLayers);
                            fireDistanceAndTime();

                            if (future.isRequiresDownload())
                                future.download();
                            if (future.isRequiresProcessing())
                                future.process();

                            removeLines(pairWithLayers, true);
                            drawRoute(pairWithLayers);
                            fireDistanceAndTime();
                        } else {
                            drawRoute(pairWithLayers);
                            fireDistanceAndTime();
                        }
                    }
                });
            }

            private void drawBeeline(List<PairWithLayer> pairsWithLayer) {
                int tileSize = mapView.getModel().displayModel.getTileSize();
                for (PairWithLayer pairWithLayer : pairsWithLayer) {
                    Line line = new Line(asLatLong(pairWithLayer.getFirst()), asLatLong(pairWithLayer.getSecond()), ROUTE_DOWNLOADING_PAINT, tileSize);
                    pairWithLayer.setLayer(line);
                    getLayerManager().getLayers().add(line);

                    Double distance = pairWithLayer.getFirst().calculateDistance(pairWithLayer.getSecond());
                    pairsToDistances.put(pairWithLayer, distance);
                    Long time = pairWithLayer.getFirst().calculateTime(pairWithLayer.getSecond());
                    pairsToTimes.put(pairWithLayer, time);
                }
            }

            private void removeLines(List<PairWithLayer> pairWithLayers, boolean removeDistancesAndTimes) {
                for (PairWithLayer pairWithLayer : pairWithLayers) {
                    Layer layer = pairWithLayer.getLayer();
                    if (layer != null)
                        getLayerManager().getLayers().remove(layer);
                    else
                        log.warning("Could not find layer for route pair " + pairWithLayer);
                    pairWithLayer.setLayer(null);

                    if(removeDistancesAndTimes) {
                        pairsToDistances.remove(pairWithLayer);
                        pairsToTimes.remove(pairWithLayer);
                    }
                }
            }

            private void drawRoute(List<PairWithLayer> pairWithLayers) {
                int tileSize = mapView.getModel().displayModel.getTileSize();
                RoutingService routingService = mapViewCallback.getRoutingService();
                for (PairWithLayer pairWithLayer : pairWithLayers) {
                    List<LatLong> latLongs = calculateRoute(routingService, pairWithLayer);
                    if (latLongs != null) {
                        Polyline polyline = new Polyline(latLongs, ROUTE_PAINT, tileSize);
                        pairWithLayer.setLayer(polyline);
                        getLayerManager().getLayers().add(polyline);
                    }
                }
            }

            private List<LatLong> calculateRoute(RoutingService routingService, PairWithLayer pairWithLayer) {
                List<LatLong> latLongs = new ArrayList<LatLong>();
                latLongs.add(asLatLong(pairWithLayer.getFirst()));
                RoutingResult intermediate = routingService.getRouteBetween(pairWithLayer.getFirst(), pairWithLayer.getSecond());
                if (intermediate != null) {
                    latLongs.addAll(asLatLong(intermediate.getPositions()));
                    pairsToDistances.put(pairWithLayer, intermediate.getDistance());
                    pairsToTimes.put(pairWithLayer, intermediate.getTime());
                }
                latLongs.add(asLatLong(pairWithLayer.getSecond()));
                return latLongs;
            }

            private void internalRemove(List<PairWithLayer> pairWithLayers) {
                removeLines(pairWithLayers, true);
                fireDistanceAndTime();
            }

            private void fireDistanceAndTime() {
                double totalDistance = 0.0;
                for (Double distance : pairsToDistances.values()) {
                    if (distance != null)
                        totalDistance += distance;
                }
                long totalTime = 0;
                for (Long time : pairsToTimes.values()) {
                    if (time != null)
                        totalTime += time;
                }
                fireCalculatedDistance((int) totalDistance, (int) (totalTime > 0 ? totalTime / 1000 : 0));
            }
        });

        this.trackUpdater = new TrackUpdater(positionsModel, new TrackOperation() {
            public void add(List<PairWithLayer> pairWithLayers) {
                internalAdd(pairWithLayers);
            }

            public void update(List<PairWithLayer> pairWithLayers) {
                internalRemove(pairWithLayers);
                internalAdd(pairWithLayers);
            }

            public void remove(List<PairWithLayer> pairWithLayers) {
                internalRemove(pairWithLayers);
                updateSelectionAfterRemove(pairWithLayers);
            }

            private void internalAdd(List<PairWithLayer> pairWithLayers) {
                int tileSize = mapView.getModel().displayModel.getTileSize();
                for (PairWithLayer pair : pairWithLayers) {
                    Line line = new Line(asLatLong(pair.getFirst()), asLatLong(pair.getSecond()), TRACK_PAINT, tileSize);
                    pair.setLayer(line);
                    getLayerManager().getLayers().add(line);
                }
            }

            private void internalRemove(List<PairWithLayer> pairWithLayers) {
                for (PairWithLayer pairWithLayer : pairWithLayers) {
                    Layer layer = pairWithLayer.getLayer();
                    if (layer != null)
                        getLayerManager().getLayers().remove(layer);
                    else
                        log.warning("Could not find layer for track pair " + pairWithLayer);
                    pairWithLayer.setLayer(null);
                }
            }
        });

        this.waypointUpdater = new WaypointUpdater(positionsModel, new WaypointOperation() {
            public void add(List<PositionWithLayer> positionWithLayers) {
                for (PositionWithLayer positionWithLayer : positionWithLayers) {
                    internalAdd(positionWithLayer);
                }
            }

            public void update(List<PositionWithLayer> positionWithLayers) {
                for (PositionWithLayer positionWithLayer : positionWithLayers) {
                    internalRemove(positionWithLayer);
                    internalAdd(positionWithLayer);
                }
            }

            public void remove(List<PositionWithLayer> positionWithLayers) {
                List<NavigationPosition> removed = new ArrayList<NavigationPosition>();
                for (PositionWithLayer positionWithLayer : positionWithLayers) {
                    internalRemove(positionWithLayer);
                    removed.add(positionWithLayer.getPosition());
                }
                selectionUpdater.removedPositions(removed);
            }

            private void internalAdd(PositionWithLayer positionWithLayer) {
                Marker marker = new Marker(asLatLong(positionWithLayer.getPosition()), waypointIcon, 1, 0);
                positionWithLayer.setLayer(marker);
                getLayerManager().getLayers().add(marker);
            }

            private void internalRemove(PositionWithLayer positionWithLayer) {
                Layer layer = positionWithLayer.getLayer();
                if (layer != null)
                    getLayerManager().getLayers().remove(layer);
                else
                    log.warning("Could not find layer for position " + positionWithLayer);
                positionWithLayer.setLayer(null);
            }
        });

        this.eventMapUpdater = getEventMapUpdaterFor(Waypoints);

        characteristicsModel.addListDataListener(new ListDataListener() {
            public void intervalAdded(ListDataEvent e) {
            }

            public void intervalRemoved(ListDataEvent e) {
            }

            public void contentsChanged(ListDataEvent e) {
                updateRouteButDontRecenter();
            }
        });

        getPositionsModel().addTableModelListener(new TableModelListener() {
            public void tableChanged(TableModelEvent e) {
                switch (e.getType()) {
                    case INSERT:
                        eventMapUpdater.handleAdd(e.getFirstRow(), e.getLastRow());
                        break;
                    case UPDATE:
                        if (getPositionsModel().isContinousRange())
                            return;
                        if (!(e.getColumn() == DESCRIPTION_COLUMN_INDEX ||
                                e.getColumn() == LONGITUDE_COLUMN_INDEX ||
                                e.getColumn() == LATITUDE_COLUMN_INDEX ||
                                e.getColumn() == ALL_COLUMNS))
                            return;

                        boolean allRowsChanged = isFirstToLastRow(e);
                        if (!allRowsChanged)
                            eventMapUpdater.handleUpdate(e.getFirstRow(), e.getLastRow());
                        if (allRowsChanged)
                            centerAndZoom(getMapBoundingBox(), getRouteBoundingBox(), true);

                        break;
                    case DELETE:
                        eventMapUpdater.handleRemove(e.getFirstRow(), e.getLastRow());
                        break;
                }
            }
        });
    }

    private void updateSelectionAfterRemove(List<PairWithLayer> pairWithLayers) {
        Set<NavigationPosition> removed = new HashSet<NavigationPosition>();
        for (PairWithLayer pair : pairWithLayers) {
            removed.add(pair.getFirst());
            removed.add(pair.getSecond());
        }
        selectionUpdater.removedPositions(new ArrayList<NavigationPosition>(removed));
    }

    private java.util.Map<LocalMap, Layer> mapsToLayers = new HashMap<LocalMap, Layer>();

    private void handleMapAndThemeUpdate(boolean centerAndZoom, boolean alwaysRecenter) {
        Layers layers = getLayerManager().getLayers();

        // add new map with a theme
        LocalMap map = mapViewCallback.getMapManager().getDisplayedMapModel().getItem();
        Theme theme = mapViewCallback.getMapManager().getAppliedThemeModel().getItem();
        Layer layer;
        try {
            layer = map.isVector() ? createTileRendererLayer(map, theme) : createTileDownloadLayer(map.getTileSource());
        } catch (Exception e) {
            showMessageDialog(getComponent(), format(ResourceBundle.getBundle("slash/navigation/converter/gui/mapview/MapsforgeMapView").
                    getString("cannot-load-map"), map.getDescription(), e, "Error", ERROR_MESSAGE);
            return;
        }

        // remove old map
        for (LocalMap localMap : mapsToLayers.keySet())
            layers.remove(mapsToLayers.get(localMap));
        mapsToLayers.clear();

        // add map as the first to be behind all additional layers
        layers.add(0, layer);
        mapsToLayers.put(map, layer);

        // then start download layer threads
        if (layer instanceof TileDownloadLayer)
            ((TileDownloadLayer) layer).start();

        // center and zoom: if map is initialized, doesn't contain route or there is no route
        BoundingBox mapBoundingBox = getMapBoundingBox();
        BoundingBox routeBoundingBox = getRouteBoundingBox();
        if (centerAndZoom &&
                ((mapBoundingBox != null && routeBoundingBox != null && !mapBoundingBox.contains(routeBoundingBox)) ||
                        routeBoundingBox == null)) {
            centerAndZoom(mapBoundingBox, routeBoundingBox, alwaysRecenter);
        }
        limitZoomLevel();
        log.info("Using map " + mapsToLayers.keySet() + " and theme " + theme);
    }

    private BaseRoute lastRoute = null;
    private RouteCharacteristics lastCharacteristics = Waypoints; // corresponds to default eventMapUpdater

    private void updateRouteButDontRecenter() {
        // avoid duplicate work
        RouteCharacteristics characteristics = MapsforgeMapView.this.characteristicsModel.getSelectedCharacteristics();
        BaseRoute route = getPositionsModel().getRoute();
        if (lastCharacteristics.equals(characteristics) && lastRoute != null && lastRoute.equals(getPositionsModel().getRoute()))
            return;
        lastCharacteristics = characteristics;
        lastRoute = route;

        // throw away running routing executions      // TODO use signals later
        executor.shutdownNow();
        executor = newSingleThreadExecutor();

        // remove all from previous event map updater
        eventMapUpdater.handleRemove(0, MAX_VALUE);

        // select current event map updater and let him add all
        eventMapUpdater = getEventMapUpdaterFor(characteristics);
        eventMapUpdater.handleAdd(0, getPositionsModel().getRowCount() - 1);
    }

    private LayerManager getLayerManager() {
        return mapView.getLayerManager();
    }

    private EventMapUpdater getEventMapUpdaterFor(RouteCharacteristics characteristics) {
        switch (characteristics) {
            case Route:
                return routeUpdater;
            case Track:
                return trackUpdater;
            case Waypoints:
                return waypointUpdater;
            default:
                throw new IllegalArgumentException("RouteCharacteristics " + characteristics + " is not supported");
        }
    }

    private PositionsModel getPositionsModel() {
        return positionsModel;
    }

    public boolean isSupportedPlatform() {
        return true;
    }

    public boolean isInitialized() {
        return true;
    }

    public Throwable getInitializationCause() {
        return null;
    }

    public void dispose() {
        NavigationPosition center = getCenter();
        preferences.putDouble(CENTER_LONGITUDE_PREFERENCE, center.getLongitude());
        preferences.putDouble(CENTER_LATITUDE_PREFERENCE, center.getLatitude());
        int zoom = getZoom();
        preferences.putInt(CENTER_ZOOM_PREFERENCE, zoom);

        executor.shutdownNow();
        mapView.getModel().mapViewPosition.destroy();
        mapView.destroy();
    }

    public Component getComponent() {
        return mapSelector.getComponent();
    }

    public void resize() {
        // intentionally left empty
    }

    public void setRecenterAfterZooming(boolean recenterAfterZooming) {
        this.recenterAfterZooming = recenterAfterZooming;
    }

    public void setShowCoordinates(boolean showCoordinates) {
        // TODO implement me
    }

    public void setShowWaypointDescription(boolean showWaypointDescription) {
        // TODO implement me
    }

    public void setTravelMode(TravelMode travelMode) {
        // TODO implement me
    }

    public void setAvoidHighways(boolean avoidHighways) {
        // TODO implement me
    }

    public void setAvoidTolls(boolean avoidTolls) {
        // TODO implement me
    }

    private Polyline mapBorder, routeBorder;

    public void showMapBorder(BoundingBox mapBoundingBox) {
        if (mapBorder != null) {
            getLayerManager().getLayers().remove(mapBorder);
            mapBorder = null;
        }
        if (routeBorder != null) {
            getLayerManager().getLayers().remove(routeBorder);
            routeBorder = null;
        }

        if (mapBoundingBox != null)
            mapBorder = drawBorder(mapBoundingBox);

        BoundingBox routeBoundingBox = getRouteBoundingBox();
        if (routeBoundingBox != null)
            routeBorder = drawBorder(routeBoundingBox);

        centerAndZoom(mapBoundingBox, routeBoundingBox, false);
    }

    private Polyline drawBorder(BoundingBox boundingBox) {
        Paint paint = GRAPHIC_FACTORY.createPaint();
        paint.setColor(BLUE);
        paint.setStrokeWidth(3);
        paint.setDashPathEffect(new float[]{3, 12});
        Polyline polyline = new Polyline(asLatLong(boundingBox), paint, mapView.getModel().displayModel.getTileSize());
        getLayerManager().getLayers().add(polyline);
        return polyline;
    }

    private BoundingBox getMapBoundingBox() {
        Collection<Layer> values = mapsToLayers.values();
        if (!values.isEmpty()) {
            Layer layer = values.iterator().next();
            if (layer instanceof TileRendererLayer) {
                TileRendererLayer tileRendererLayer = (TileRendererLayer) layer;
                return toBoundingBox(tileRendererLayer.getMapDatabase().getMapFileInfo().boundingBox);
            }
        }
        return null;
    }

    private BoundingBox getRouteBoundingBox() {
        BaseRoute route = getPositionsModel().getRoute();
        return route != null && route.getPositions().size() > 0 ? new BoundingBox(route.getPositions()) : null;
    }

    private void centerAndZoom(BoundingBox mapBoundingBox, BoundingBox routeBoundingBox, boolean alwaysRecenter) {
        List<NavigationPosition> positions = new ArrayList<NavigationPosition>();

        // if there is a route and we center and zoom, then use the route bounding box
        if (routeBoundingBox != null) {
            positions.add(routeBoundingBox.getNorthEast());
            positions.add(routeBoundingBox.getSouthWest());
        }

        // if the map is limited
        if (mapBoundingBox != null) {

            // if there is a route
            if (routeBoundingBox != null) {
                positions.add(routeBoundingBox.getNorthEast());
                positions.add(routeBoundingBox.getSouthWest());
                // if the map is limited and doesn't cover the route
                if (!mapBoundingBox.contains(routeBoundingBox)) {
                    positions.add(mapBoundingBox.getNorthEast());
                    positions.add(mapBoundingBox.getSouthWest());
                }

                // if there just a map
            } else {
                positions.add(mapBoundingBox.getNorthEast());
                positions.add(mapBoundingBox.getSouthWest());
            }
        }

        if (positions.size() > 0) {
            BoundingBox both = new BoundingBox(positions);
            zoomToBounds(both);
            setCenter(both.getCenter(), alwaysRecenter);
        }
    }

    private void limitZoomLevel() {
        // limit minimum zoom to prevent zooming out too much and losing the map
        byte zoomLevelMin = 2;
        LocalMap map = mapsToLayers.keySet().iterator().next();
        if (map.isVector() && mapView.getModel().mapViewDimension.getDimension() != null)
            zoomLevelMin = (byte) (zoomForBounds(mapView.getModel().mapViewDimension.getDimension(),
                    asBoundingBox(map.getBoundingBox()), mapView.getModel().displayModel.getTileSize()) - 3);
        mapView.getModel().mapViewPosition.setZoomLevelMin(zoomLevelMin);

        // limit maximum to prevent zooming in to grey area
        byte zoomLevelMax = (byte) (map.isVector() ? 22 : 18);
        mapView.getModel().mapViewPosition.setZoomLevelMax(zoomLevelMax);
    }

    private LongitudeAndLatitude asLongitudeAndLatitude(NavigationPosition position) {
        return new LongitudeAndLatitude(position.getLongitude(), position.getLatitude());
    }

    private List<LongitudeAndLatitude> asLongitudeAndLatitude(List<PairWithLayer> pairs) {
        List<LongitudeAndLatitude> result = new ArrayList<>();
        for (PairWithLayer pair : pairs) {
            result.add(asLongitudeAndLatitude(pair.getFirst()));
            result.add(asLongitudeAndLatitude(pair.getSecond()));
        }
        return result;
    }

    private boolean isVisible(LatLong latLong, int border) {
        MapViewProjection projection = new MapViewProjection(mapView);
        LatLong upperLeft = projection.fromPixels(border, border);
        Dimension dimension = mapView.getDimension();
        LatLong lowerRight = projection.fromPixels(dimension.width - border, dimension.height - border);
        return upperLeft != null && lowerRight != null && new org.mapsforge.core.model.BoundingBox(lowerRight.latitude, upperLeft.longitude, upperLeft.latitude, lowerRight.longitude).contains(latLong);
    }

    public NavigationPosition getCenter() {
        return asNavigationPosition(mapView.getModel().mapViewPosition.getCenter());
    }

    private void setCenter(LatLong center, boolean alwaysRecenter) {
        if (alwaysRecenter || recenterAfterZooming || !isVisible(center, 20))
            mapView.getModel().mapViewPosition.animateTo(center);
    }

    private void setCenter(NavigationPosition center, boolean alwaysRecenter) {
        setCenter(asLatLong(center), alwaysRecenter);
    }

    private int getZoom() {
        return mapView.getModel().mapViewPosition.getZoomLevel();
    }

    private void setZoom(int zoom) {
        mapView.getModel().mapViewPosition.setZoomLevel((byte) zoom);
    }

    private void zoomToBounds(org.mapsforge.core.model.BoundingBox boundingBox) {
        Dimension dimension = mapView.getModel().mapViewDimension.getDimension();
        if (dimension == null)
            return;
        byte zoom = zoomForBounds(dimension, boundingBox, mapView.getModel().displayModel.getTileSize());
        // zoom out a bit if the bounding box is pretty large since the user selected a wrong map in the MapsDialog
        if (abs(boundingBox.minLatitude - boundingBox.maxLatitude) > 10.0 || abs(boundingBox.maxLongitude - boundingBox.minLongitude) > 10.0)
            zoom -= 1;
        setZoom(zoom);
    }

    private void zoomToBounds(BoundingBox boundingBox) {
        zoomToBounds(asBoundingBox(boundingBox));
    }


    public void print(String title, boolean withDirections) {
        // TODO implement me
    }

    public void insertAllWaypoints(int[] startPositions) {
        // TODO implement me
    }

    public void insertOnlyTurnpoints(int[] startPositions) {
        // TODO implement me
    }

    public void setSelectedPositions(int[] selectedPositions, boolean replaceSelection) {
        if (selectionUpdater == null)
            return;
        selectionUpdater.setSelectedPositions(selectedPositions, replaceSelection);
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

    private LatLong getMousePosition() {
        Point point = mapViewMouseEventListener.getMousePosition();
        return point != null ? new MapViewProjection(mapView).fromPixels(point.getX(), point.getY()) :
                mapView.getModel().mapViewPosition.getCenter();
    }

    private double getThresholdForPixel(LatLong latLong, int pixel) {
        double metersPerPixel = calculateGroundResolution(latLong.latitude,
                mapView.getModel().mapViewPosition.getZoomLevel(), mapView.getModel().displayModel.getTileSize());
        return metersPerPixel * pixel;
    }

    private void selectPosition(Double longitude, Double latitude, Double threshold, boolean replaceSelection) { // TODO same as in BaseMapView
        int row = positionsModel.getClosestPosition(longitude, latitude, threshold);
        if (row != -1)
            positionsSelectionModel.setSelectedPositions(new int[]{row}, replaceSelection);
    }

    private class SelectPositionAction extends FrameAction {
        public void run() {
            LatLong latLong = getMousePosition();
            if (latLong != null) {
                Double threshold = getThresholdForPixel(latLong, 15);
                selectPosition(latLong.longitude, latLong.latitude, threshold, true);
            }
        }
    }

    private class ExtendSelectionAction extends FrameAction {
        public void run() {
            LatLong latLong = getMousePosition();
            if (latLong != null) {
                Double threshold = getThresholdForPixel(latLong, 15);
                selectPosition(latLong.longitude, latLong.latitude, threshold, false);
            }
        }
    }

    private class AddPositionAction extends FrameAction {
        private int getAddRow() { // TODO same as in BaseMapView
            List<PositionWithLayer> lastSelectedPositions = selectionUpdater.getPositionWithLayers();
            NavigationPosition position = lastSelectedPositions.size() > 0 ? lastSelectedPositions.get(lastSelectedPositions.size() - 1).getPosition() : null;
            // quite crude logic to be as robust as possible on failures
            if (position == null && positionsModel.getRowCount() > 0)
                position = positionsModel.getPosition(positionsModel.getRowCount() - 1);
            return position != null ? positionsModel.getIndex(position) + 1 : 0;
        }

        private void insertPosition(int row, Double longitude, Double latitude) {
            positionsModel.add(row, longitude, latitude, null, null, null, mapViewCallback.createDescription(positionsModel.getRowCount() + 1, null));
            positionsSelectionModel.setSelectedPositions(new int[]{row}, true);

            mapViewCallback.complementDescription(row, longitude, latitude);         // TODO same as BaseMapView
            mapViewCallback.complementElevation(row, longitude, latitude);
            mapViewCallback.complementTime(row, null, true);
        }

        public void run() {
            LatLong latLong = getMousePosition();
            if (latLong != null) {
                int row = getAddRow();
                insertPosition(row, latLong.longitude, latLong.latitude);
            }
        }
    }

    private class DeletePositionAction extends FrameAction {
        private void removePosition(Double longitude, Double latitude, Double threshold) {
            int row = positionsModel.getClosestPosition(longitude, latitude, threshold);
            if (row != -1) {
                positionsModel.remove(new int[]{row});
            }
        }

        public void run() {
            LatLong latLong = getMousePosition();
            if (latLong != null) {
                Double threshold = getThresholdForPixel(latLong, 15);
                removePosition(latLong.longitude, latLong.latitude, threshold);
            }
        }
    }

    private class CenterAction extends FrameAction {
        public void run() {
            mapViewMouseEventListener.centerToMousePosition();
        }
    }

    private class ZoomAction extends FrameAction {
        private byte zoomLevelDiff;

        private ZoomAction(int zoomLevelDiff) {
            this.zoomLevelDiff = (byte) zoomLevelDiff;
        }

        public void run() {
            mapViewMouseEventListener.zoomToMousePosition(zoomLevelDiff);
        }
    }
}
