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
package slash.navigation.mapview.mapsforge;

import org.mapsforge.core.graphics.Bitmap;
import org.mapsforge.core.graphics.Paint;
import org.mapsforge.core.model.Dimension;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.MapPosition;
import org.mapsforge.core.util.Parameters;
import org.mapsforge.map.layer.GroupLayer;
import org.mapsforge.map.layer.Layer;
import org.mapsforge.map.layer.LayerManager;
import org.mapsforge.map.layer.Layers;
import org.mapsforge.map.layer.TileLayer;
import org.mapsforge.map.layer.cache.FileSystemTileCache;
import org.mapsforge.map.layer.cache.InMemoryTileCache;
import org.mapsforge.map.layer.cache.TileCache;
import org.mapsforge.map.layer.cache.TwoLevelTileCache;
import org.mapsforge.map.layer.download.TileDownloadLayer;
import org.mapsforge.map.layer.download.tilesource.TileSource;
import org.mapsforge.map.layer.hills.DiffuseLightShadingAlgorithm;
import org.mapsforge.map.layer.hills.HillsRenderConfig;
import org.mapsforge.map.layer.hills.MemoryCachingHgtReaderTileSource;
import org.mapsforge.map.layer.overlay.Marker;
import org.mapsforge.map.layer.renderer.TileRendererLayer;
import org.mapsforge.map.model.MapViewDimension;
import org.mapsforge.map.model.MapViewPosition;
import org.mapsforge.map.model.common.Observer;
import org.mapsforge.map.reader.MapFile;
import org.mapsforge.map.scalebar.DefaultMapScaleBar;
import org.mapsforge.map.scalebar.ImperialUnitAdapter;
import org.mapsforge.map.scalebar.MetricUnitAdapter;
import org.mapsforge.map.scalebar.NauticalUnitAdapter;
import org.mapsforge.map.util.MapViewProjection;
import slash.navigation.base.BaseRoute;
import slash.navigation.base.RouteCharacteristics;
import slash.navigation.common.BoundingBox;
import slash.navigation.common.DistanceAndTime;
import slash.navigation.common.NavigationPosition;
import slash.navigation.common.UnitSystem;
import slash.navigation.converter.gui.models.CharacteristicsModel;
import slash.navigation.converter.gui.models.ColorModel;
import slash.navigation.converter.gui.models.PositionsModel;
import slash.navigation.converter.gui.models.PositionsSelectionModel;
import slash.navigation.converter.gui.models.UnitSystemModel;
import slash.navigation.elevation.ElevationService;
import slash.navigation.gui.Application;
import slash.navigation.gui.actions.ActionManager;
import slash.navigation.gui.actions.FrameAction;
import slash.navigation.gui.models.BooleanModel;
import slash.navigation.maps.mapsforge.LocalMap;
import slash.navigation.maps.mapsforge.MapsforgeMapManager;
import slash.navigation.maps.mapsforge.models.TileServerMapSource;
import slash.navigation.maps.tileserver.TileServer;
import slash.navigation.mapview.MapView;
import slash.navigation.mapview.MapViewCallback;
import slash.navigation.mapview.MapViewListener;
import slash.navigation.mapview.mapsforge.helpers.MapViewCoordinateDisplayer;
import slash.navigation.mapview.mapsforge.helpers.MapViewMoverAndZoomer;
import slash.navigation.mapview.mapsforge.helpers.MapViewPopupMenu;
import slash.navigation.mapview.mapsforge.helpers.MapViewResizer;
import slash.navigation.mapview.mapsforge.lines.Line;
import slash.navigation.mapview.mapsforge.lines.Polyline;
import slash.navigation.mapview.mapsforge.overlays.DraggableMarker;
import slash.navigation.mapview.mapsforge.renderer.RouteRenderer;
import slash.navigation.mapview.mapsforge.updater.EventMapUpdater;
import slash.navigation.mapview.mapsforge.updater.ObjectWithLayer;
import slash.navigation.mapview.mapsforge.updater.PairWithLayer;
import slash.navigation.mapview.mapsforge.updater.PositionWithLayer;
import slash.navigation.mapview.mapsforge.updater.SelectionOperation;
import slash.navigation.mapview.mapsforge.updater.SelectionUpdater;
import slash.navigation.mapview.mapsforge.updater.TrackOperation;
import slash.navigation.mapview.mapsforge.updater.TrackUpdater;
import slash.navigation.mapview.mapsforge.updater.WaypointOperation;
import slash.navigation.mapview.mapsforge.updater.WaypointUpdater;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

import static java.awt.event.InputEvent.CTRL_DOWN_MASK;
import static java.awt.event.KeyEvent.VK_DOWN;
import static java.awt.event.KeyEvent.VK_LEFT;
import static java.awt.event.KeyEvent.VK_MINUS;
import static java.awt.event.KeyEvent.VK_PLUS;
import static java.awt.event.KeyEvent.VK_RIGHT;
import static java.awt.event.KeyEvent.VK_UP;
import static java.lang.Integer.MAX_VALUE;
import static java.lang.Math.max;
import static java.lang.System.currentTimeMillis;
import static javax.swing.JComponent.WHEN_IN_FOCUSED_WINDOW;
import static javax.swing.KeyStroke.getKeyStroke;
import static javax.swing.event.TableModelEvent.ALL_COLUMNS;
import static javax.swing.event.TableModelEvent.DELETE;
import static javax.swing.event.TableModelEvent.INSERT;
import static javax.swing.event.TableModelEvent.UPDATE;
import static org.mapsforge.core.graphics.Color.BLUE;
import static org.mapsforge.core.util.LatLongUtils.zoomForBounds;
import static org.mapsforge.core.util.MercatorProjection.calculateGroundResolution;
import static org.mapsforge.core.util.MercatorProjection.getMapSize;
import static org.mapsforge.map.scalebar.DefaultMapScaleBar.ScaleBarMode.SINGLE;
import static slash.common.helpers.ThreadHelper.createSingleThreadExecutor;
import static slash.common.helpers.ThreadHelper.invokeInAwtEventQueue;
import static slash.common.io.Directories.getTemporaryDirectory;
import static slash.common.io.Transfer.encodeUri;
import static slash.common.io.Transfer.isEmpty;
import static slash.navigation.base.RouteCharacteristics.Route;
import static slash.navigation.base.RouteCharacteristics.Waypoints;
import static slash.navigation.converter.gui.models.PositionColumns.DESCRIPTION_COLUMN_INDEX;
import static slash.navigation.converter.gui.models.PositionColumns.LATITUDE_COLUMN_INDEX;
import static slash.navigation.converter.gui.models.PositionColumns.LONGITUDE_COLUMN_INDEX;
import static slash.navigation.gui.events.IgnoreEvent.isIgnoreEvent;
import static slash.navigation.gui.helpers.JMenuHelper.createItem;
import static slash.navigation.gui.helpers.JTableHelper.isFirstToLastRow;
import static slash.navigation.maps.mapsforge.helpers.MapTransfer.asBoundingBox;
import static slash.navigation.maps.mapsforge.helpers.MapTransfer.asLatLong;
import static slash.navigation.maps.mapsforge.helpers.MapTransfer.asNavigationPosition;
import static slash.navigation.maps.mapsforge.helpers.MapTransfer.toBoundingBox;
import static slash.navigation.mapview.MapViewConstants.TRACK_LINE_WIDTH_PREFERENCE;
import static slash.navigation.mapview.mapsforge.AwtGraphicMapView.GRAPHIC_FACTORY;
import static slash.navigation.mapview.mapsforge.helpers.ColorHelper.asRGBA;
import static slash.navigation.mapview.mapsforge.models.LocalNames.MAP;

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
    private static final String READ_BUFFER_SIZE_PREFERENCE = "readBufferSize";
    private static final String FIRST_LEVEL_TILE_CACHE_SIZE_PREFERENCE = "firstLevelTileCacheSize";
    private static final String SECOND_LEVEL_TILE_CACHE_SIZE_PREFERENCE = "secondLevelTileCacheSize";
    private static final String MAP_SCALE_FACTOR = "mapScaleFactor";
    private static final int SCROLL_DIFF_IN_PIXEL = 100;
    private static final int MINIMUM_VISIBLE_BORDER_IN_PIXEL = 20;
    private static final int SELECTION_CIRCLE_IN_PIXEL = 15;
    private static final byte MINIMUM_ZOOM_LEVEL = 2;
    private static final byte MAXIMUM_ZOOM_LEVEL = 22;

    private PositionsModel positionsModel;
    private PositionsSelectionModel positionsSelectionModel;
    private CharacteristicsModel characteristicsModel;
    private BooleanModel showAllPositionsAfterLoading;
    private BooleanModel recenterAfterZooming;
    private BooleanModel showCoordinates;
    private ColorModel routeColorModel;
    private ColorModel trackColorModel;
    private UnitSystemModel unitSystemModel;
    private MapViewCallbackOpenSource mapViewCallback;

    private PositionsModelListener positionsModelListener = new PositionsModelListener();
    private CharacteristicsModelListener characteristicsModelListener = new CharacteristicsModelListener();
    private RoutingServiceListener routingServiceListener = new RoutingServiceListener();
    private ShowCoordinatesListener showCoordinatesListener = new ShowCoordinatesListener();
    private ColorModelListener colorModelListener = new ColorModelListener();
    private UnitSystemListener unitSystemListener = new UnitSystemListener();
    private DisplayedMapListener displayedMapListener = new DisplayedMapListener();
    private AppliedThemeListener appliedThemeListener = new AppliedThemeListener();
    private AppliedOverlayListener appliedOverlayListener = new AppliedOverlayListener();
    private ShadedHillsListener shadedHillsListener = new ShadedHillsListener();

    private MapSelector mapSelector;
    private AwtGraphicMapView mapView;
    private MapViewMoverAndZoomer mapViewMoverAndZoomer;
    private MapViewCoordinateDisplayer mapViewCoordinateDisplayer = new MapViewCoordinateDisplayer();
    private static Bitmap markerIcon, waypointIcon;
    private GroupLayer overlaysLayer = new GroupLayer();
    private TileRendererLayer backgroundLayer;
    private HillsRenderConfig hillsRenderConfig = new HillsRenderConfig(null);
    private SelectionUpdater selectionUpdater;
    private EventMapUpdater routeUpdater, trackUpdater, waypointUpdater;
    private RouteRenderer routeRenderer;
    private UpdateDecoupler updateDecoupler;

    // initialization

    public void initialize(final PositionsModel positionsModel,
                           PositionsSelectionModel positionsSelectionModel,
                           CharacteristicsModel characteristicsModel,
                           MapViewCallback mapViewCallback,
                           BooleanModel showAllPositionsAfterLoading,
                           BooleanModel recenterAfterZooming,
                           BooleanModel showCoordinates,
                           BooleanModel showWaypointDescription,       /* ignored */
                           ColorModel aRouteColorModel,
                           final ColorModel aTrackColorModel,
                           UnitSystemModel unitSystemModel             /* ignored */) {
        this.mapViewCallback = (MapViewCallbackOpenSource) mapViewCallback;
        this.positionsModel = positionsModel;
        this.positionsSelectionModel = positionsSelectionModel;
        this.characteristicsModel = characteristicsModel;
        this.showAllPositionsAfterLoading = showAllPositionsAfterLoading;
        this.recenterAfterZooming = recenterAfterZooming;
        this.showCoordinates = showCoordinates;
        this.routeColorModel = aRouteColorModel;
        this.trackColorModel = aTrackColorModel;
        this.unitSystemModel = unitSystemModel;

        this.selectionUpdater = new SelectionUpdater(positionsModel, new SelectionOperation() {
            private Marker createMarker(PositionWithLayer positionWithLayer, LatLong latLong) {
                return new DraggableMarker(positionsModel, positionWithLayer, latLong, markerIcon, 0, -27);
            }

            public void add(List<PositionWithLayer> positionWithLayers) {
                LatLong center = null;
                List<PositionWithLayer> withLayers = new ArrayList<>();
                for (final PositionWithLayer positionWithLayer : positionWithLayers) {
                    if (!positionWithLayer.hasCoordinates())
                        continue;

                    LatLong latLong = asLatLong(positionWithLayer.getPosition());
                    Marker marker = createMarker(positionWithLayer, latLong);
                    positionWithLayer.setLayer(marker);
                    withLayers.add(positionWithLayer);
                    center = latLong;
                }
                addLayers(withLayers);
                if (center != null)
                    setCenter(center, false);
            }

            public void remove(List<PositionWithLayer> positionWithLayers) {
                removeLayers(positionWithLayers, true);
            }
        });

        this.routeUpdater = new TrackUpdater(positionsModel, new TrackOperation() {
            private List<PairWithLayer> pairs = new ArrayList<>();

            public void add(List<PairWithLayer> pairWithLayers) {
                internalAdd(pairWithLayers);
            }

            public void update(final List<PairWithLayer> pairWithLayers) {
                internalRemove(pairWithLayers);
                internalAdd(pairWithLayers);
                updateSelectionAfterUpdate(pairWithLayers);
            }

            public void remove(List<PairWithLayer> pairWithLayers) {
                internalRemove(pairWithLayers);
                fireDistanceAndTime();
                updateSelectionAfterRemove(pairWithLayers);
            }

            private void internalAdd(List<PairWithLayer> pairWithLayers) {
                pairs.addAll(pairWithLayers);
                routeRenderer.renderRoute(pairWithLayers, new Runnable() {
                    public void run() {
                        fireDistanceAndTime();
                    }
                });
            }

            private void internalRemove(List<PairWithLayer> pairWithLayers) {
                // speed optimization for large numbers of pairWithLayers
                if (pairs.size() == pairWithLayers.size())
                    pairs.clear();
                else
                    pairs.removeAll(pairWithLayers);
                for (PairWithLayer pairWithLayer : pairWithLayers)
                    pairWithLayer.setDistanceAndTime(null);
                removeLayers(pairWithLayers, true);
            }

            private void fireDistanceAndTime() {
                Map<Integer, DistanceAndTime> result = new HashMap<>(pairs.size());
                double aggregatedDistance = 0.0;
                long aggregatedTime = 0L;
                for (int i = 0; i < pairs.size(); i++) {
                    PairWithLayer pairWithLayer = pairs.get(i);
                    DistanceAndTime distanceAndTime = pairWithLayer.getDistanceAndTime();
                    if (distanceAndTime != null) {
                        Double distance = distanceAndTime.getDistance();
                        if (!isEmpty(distance))
                            aggregatedDistance += distance;
                        Long time = distanceAndTime.getTime();
                        if (!isEmpty(time))
                            aggregatedTime += time;
                    }
                    result.put(i + 1, new DistanceAndTime(aggregatedDistance, aggregatedTime));
                }
                fireCalculatedDistances(result);
            }
        });

        this.trackUpdater = new TrackUpdater(positionsModel, new TrackOperation() {
            public void add(List<PairWithLayer> pairWithLayers) {
                internalAdd(pairWithLayers);
            }

            public void update(final List<PairWithLayer> pairWithLayers) {
                internalRemove(pairWithLayers);
                internalAdd(pairWithLayers);
                updateSelectionAfterUpdate(pairWithLayers);
            }

            public void remove(List<PairWithLayer> pairWithLayers) {
                internalRemove(pairWithLayers);
                updateSelectionAfterRemove(pairWithLayers);
            }

            private void internalAdd(List<PairWithLayer> pairWithLayers) {
                Paint paint = GRAPHIC_FACTORY.createPaint();
                paint.setColor(asRGBA(trackColorModel));
                paint.setStrokeWidth(preferences.getInt(TRACK_LINE_WIDTH_PREFERENCE, 2));
                int tileSize = getTileSize();

                List<PairWithLayer> withLayers = new ArrayList<>();
                for (PairWithLayer pair : pairWithLayers) {
                    if (!pair.hasCoordinates())
                        continue;

                    Line line = new Line(asLatLong(pair.getFirst()), asLatLong(pair.getSecond()), paint, tileSize);
                    pair.setLayer(line);
                    withLayers.add(pair);
                }
                addLayers(withLayers);
            }

            private void internalRemove(List<PairWithLayer> pairWithLayers) {
                removeLayers(pairWithLayers, true);
            }
        });

        this.waypointUpdater = new WaypointUpdater(positionsModel, new WaypointOperation() {
            private Marker createMarker(PositionWithLayer positionWithLayer) {
                return new Marker(asLatLong(positionWithLayer.getPosition()), waypointIcon, 1, 0);
            }

            public void add(List<PositionWithLayer> positionWithLayers) {
                List<PositionWithLayer> withLayers = new ArrayList<>();
                for (PositionWithLayer positionWithLayer : positionWithLayers) {
                    if (!positionWithLayer.hasCoordinates())
                        return;

                    Marker marker = createMarker(positionWithLayer);
                    positionWithLayer.setLayer(marker);
                    withLayers.add(positionWithLayer);
                }
                addLayers(withLayers);
            }

            public void update(final List<PositionWithLayer> positionWithLayers) {
                removeLayers(positionWithLayers, false);

                List<NavigationPosition> updated = new ArrayList<>();
                List<PositionWithLayer> withLayers = new ArrayList<>();
                for (PositionWithLayer positionWithLayer : positionWithLayers) {
                    if (!positionWithLayer.hasCoordinates())
                        return;

                    Marker marker = createMarker(positionWithLayer);
                    positionWithLayer.setLayer(marker);
                    withLayers.add(positionWithLayer);
                    updated.add(positionWithLayer.getPosition());
                }
                addLayers(withLayers);

                selectionUpdater.updatedPositions(new ArrayList<>(updated));
            }

            public void remove(List<PositionWithLayer> positionWithLayers) {
                List<NavigationPosition> removed = new ArrayList<>();
                for (PositionWithLayer positionWithLayer : positionWithLayers)
                    removed.add(positionWithLayer.getPosition());
                removeLayers(positionWithLayers, true);
                selectionUpdater.removedPositions(removed);
            }
        });

        this.updateDecoupler = new UpdateDecoupler();

        positionsModel.addTableModelListener(positionsModelListener);
        characteristicsModel.addListDataListener(characteristicsModelListener);
        mapViewCallback.addRoutingServiceChangeListener(routingServiceListener);
        showCoordinates.addChangeListener(showCoordinatesListener);
        routeColorModel.addChangeListener(colorModelListener);
        trackColorModel.addChangeListener(colorModelListener);
        unitSystemModel.addChangeListener(unitSystemListener);

        this.mapViewCallback.getShowShadedHills().addChangeListener(shadedHillsListener);

        initializeActions();
        initializeMapView();
        routeRenderer = new RouteRenderer(this, this.mapViewCallback, routeColorModel, GRAPHIC_FACTORY);
    }

    private boolean initializedActions = false;

    private void initializeActions() {
        ActionManager actionManager = Application.getInstance().getContext().getActionManager();
        if (initializedActions)
            return;

        actionManager.register("select-position", new SelectPositionAction());
        actionManager.register("extend-selection", new ExtendSelectionAction());
        actionManager.register("add-position", new AddPositionAction());
        actionManager.register("delete-position-from-map", new DeletePositionAction());
        actionManager.registerLocal("delete", MAP, "delete-position-from-map");
        actionManager.register("center-here", new CenterAction());
        actionManager.register("zoom-in", new ZoomAction(+1));
        actionManager.register("zoom-out", new ZoomAction(-1));

        initializedActions = true;
    }

    private MapsforgeMapManager getMapManager() {
        return mapViewCallback.getMapsforgeMapManager();
    }

    private LayerManager getLayerManager() {
        return mapView.getLayerManager();
    }

    private void initializeMapView() {
        mapView = createMapView();
        handleUnitSystem();

        try {
            markerIcon = GRAPHIC_FACTORY.createResourceBitmap(MapsforgeMapView.class.getResourceAsStream("marker.png"), -1);
            waypointIcon = GRAPHIC_FACTORY.createResourceBitmap(MapsforgeMapView.class.getResourceAsStream("waypoint.png"), -1);
        } catch (IOException e) {
            log.severe("Cannot create marker and waypoint icon: " + e);
        }

        mapSelector = new MapSelector(getMapManager(), mapView);
        mapViewMoverAndZoomer = new MapViewMoverAndZoomer(mapView, getLayerManager());
        mapViewCoordinateDisplayer.initialize(mapView, mapViewCallback);
        new MapViewPopupMenu(mapView, createPopupMenu());

        final ActionManager actionManager = Application.getInstance().getContext().getActionManager();
        mapSelector.getMapViewPanel().registerKeyboardAction(new FrameAction() {
            public void run() {
                actionManager.run("zoom-in");
            }
        }, getKeyStroke(VK_PLUS, CTRL_DOWN_MASK), WHEN_IN_FOCUSED_WINDOW);
        mapSelector.getMapViewPanel().registerKeyboardAction(new FrameAction() {
            public void run() {
                actionManager.run("zoom-out");
            }
        }, getKeyStroke(VK_MINUS, CTRL_DOWN_MASK), WHEN_IN_FOCUSED_WINDOW);
        mapSelector.getMapViewPanel().registerKeyboardAction(new FrameAction() {
            public void run() {
                mapViewMoverAndZoomer.animateCenter(SCROLL_DIFF_IN_PIXEL, 0);
            }
        }, getKeyStroke(VK_LEFT, CTRL_DOWN_MASK), WHEN_IN_FOCUSED_WINDOW);
        mapSelector.getMapViewPanel().registerKeyboardAction(new FrameAction() {
            public void run() {
                mapViewMoverAndZoomer.animateCenter(-SCROLL_DIFF_IN_PIXEL, 0);
            }
        }, getKeyStroke(VK_RIGHT, CTRL_DOWN_MASK), WHEN_IN_FOCUSED_WINDOW);
        mapSelector.getMapViewPanel().registerKeyboardAction(new FrameAction() {
            public void run() {
                mapViewMoverAndZoomer.animateCenter(0, SCROLL_DIFF_IN_PIXEL);
            }
        }, getKeyStroke(VK_UP, CTRL_DOWN_MASK), WHEN_IN_FOCUSED_WINDOW);
        mapSelector.getMapViewPanel().registerKeyboardAction(new FrameAction() {
            public void run() {
                mapViewMoverAndZoomer.animateCenter(0, -SCROLL_DIFF_IN_PIXEL);
            }
        }, getKeyStroke(VK_DOWN, CTRL_DOWN_MASK), WHEN_IN_FOCUSED_WINDOW);

        final MapViewPosition mapViewPosition = mapView.getModel().mapViewPosition;
        mapViewPosition.setZoomLevelMin(MINIMUM_ZOOM_LEVEL);
        mapViewPosition.setZoomLevelMax(MAXIMUM_ZOOM_LEVEL);

        double longitude = preferences.getDouble(CENTER_LONGITUDE_PREFERENCE, -25.0);
        double latitude = preferences.getDouble(CENTER_LATITUDE_PREFERENCE, 35.0);
        byte zoom = (byte) preferences.getInt(CENTER_ZOOM_PREFERENCE, MINIMUM_ZOOM_LEVEL);
        mapViewPosition.setMapPosition(new MapPosition(new LatLong(latitude, longitude), zoom));

        mapView.getModel().mapViewDimension.addObserver(new Observer() {
            private boolean initialized;

            public void onChange() {
                if (!initialized) {
                    handleShadedHills();
                    handleMapAndThemeUpdate(true, true);
                    initialized = true;
                }
            }
        });

        getMapManager().getDisplayedMapModel().addChangeListener(displayedMapListener);
        getMapManager().getAppliedThemeModel().addChangeListener(appliedThemeListener);
        mapViewCallback.getTileServerMapManager().getAppliedOverlaysModel().addTableModelListener(appliedOverlayListener);
    }

    public void setBackgroundMap(File backgroundMap) {
        backgroundLayer = createTileRendererLayer(backgroundMap, backgroundMap.getName());
        handleBackground();
    }

    public void updateMapAndThemesAfterDirectoryScanning() {
        if (mapView != null)
            handleMapAndThemeUpdate(false, false);
    }

    protected float getMapScaleFactor() {
        return preferences.getInt(MAP_SCALE_FACTOR, 100) / 100.0f;
    }

    private AwtGraphicMapView createMapView() {
        // Multithreaded map rendering
        Parameters.NUMBER_OF_THREADS = Runtime.getRuntime().availableProcessors();
        // Maximum read buffer size
        Parameters.MAXIMUM_BUFFER_SIZE = preferences.getInt(READ_BUFFER_SIZE_PREFERENCE, 2500000);
        // No square frame buffer since the device orientation hardly changes
        Parameters.SQUARE_FRAME_BUFFER = false;

        AwtGraphicMapView mapView = new AwtGraphicMapView();
        new MapViewResizer(mapView, mapView.getModel().mapViewDimension);
        mapView.getMapScaleBar().setVisible(true);
        ((DefaultMapScaleBar) mapView.getMapScaleBar()).setScaleBarMode(SINGLE);
        mapView.getModel().displayModel.setUserScaleFactor(getMapScaleFactor());
        return mapView;
    }

    private void handleUnitSystem() {
        UnitSystem unitSystem = unitSystemModel.getUnitSystem();
        switch (unitSystem) {
            case Metric:
                mapView.getMapScaleBar().setDistanceUnitAdapter(MetricUnitAdapter.INSTANCE);
                break;
            case Statute:
                mapView.getMapScaleBar().setDistanceUnitAdapter(ImperialUnitAdapter.INSTANCE);
                break;
            case Nautic:
                mapView.getMapScaleBar().setDistanceUnitAdapter(NauticalUnitAdapter.INSTANCE);
                break;
            default:
                throw new IllegalArgumentException("Unknown UnitSystem " + unitSystem);
        }
    }

    private JPopupMenu createPopupMenu() {
        JPopupMenu menu = new JPopupMenu();
        menu.add(createItem("select-position"));
        menu.add(createItem("add-position"));    // TODO should be "new-position"
        menu.add(createItem("delete-position-from-map"));
        menu.addSeparator();
        menu.add(createItem("center-here"));
        menu.add(createItem("zoom-in"));
        menu.add(createItem("zoom-out"));
        return menu;
    }

    private TileRendererLayer createTileRendererLayer(File mapFile, String cacheId) {
        TileRendererLayer tileRendererLayer = new TileRendererLayer(createTileCache(cacheId), new MapFile(mapFile),
                mapView.getModel().mapViewPosition, true, true, true,
                GRAPHIC_FACTORY, hillsRenderConfig);
        tileRendererLayer.setXmlRenderTheme(getMapManager().getAppliedThemeModel().getItem().getXmlRenderTheme());
        return tileRendererLayer;
    }

    private TileDownloadLayer createTileDownloadLayer(TileSource tileSource, String cacheId) {
        return new TileDownloadLayer(createTileCache(cacheId), mapView.getModel().mapViewPosition, tileSource, GRAPHIC_FACTORY);
    }

    private TileCache createTileCache(String cacheId) {
        TileCache firstLevelTileCache = new InMemoryTileCache(preferences.getInt(FIRST_LEVEL_TILE_CACHE_SIZE_PREFERENCE, 256));
        File cacheDirectory = new File(getTemporaryDirectory(), encodeUri(cacheId));
        TileCache secondLevelTileCache = new FileSystemTileCache(preferences.getInt(SECOND_LEVEL_TILE_CACHE_SIZE_PREFERENCE, 2048), cacheDirectory, GRAPHIC_FACTORY);
        return new TwoLevelTileCache(firstLevelTileCache, secondLevelTileCache);
    }

    private void updateSelectionAfterUpdate(List<PairWithLayer> pairWithLayers) {
        Set<NavigationPosition> updated = new HashSet<>();
        for (PairWithLayer pair : pairWithLayers) {
            updated.add(pair.getFirst());
            updated.add(pair.getSecond());
        }
        selectionUpdater.updatedPositions(new ArrayList<>(updated));
    }

    private void updateSelectionAfterRemove(List<PairWithLayer> pairWithLayers) {
        Set<NavigationPosition> removed = new HashSet<>();
        for (PairWithLayer pair : pairWithLayers) {
            removed.add(pair.getFirst());
            removed.add(pair.getSecond());
        }
        selectionUpdater.removedPositions(new ArrayList<>(removed));
    }

    private java.util.Map<LocalMap, Layer> mapsToLayers = new HashMap<>();

    private void handleMapAndThemeUpdate(boolean centerAndZoom, boolean alwaysRecenter) {
        Layers layers = getLayerManager().getLayers();

        // add new map with a theme
        LocalMap map = getMapManager().getDisplayedMapModel().getItem();
        Layer layer;
        try {
            layer = map.isVector() ? createTileRendererLayer(map.getFile(), map.getUrl()) : createTileDownloadLayer(map.getTileSource(), map.getUrl());
        } catch (Exception e) {
            mapViewCallback.showMapException(map != null ? map.getDescription() : "<no map>", e);
            return;
        }

        // remove old map
        for (Map.Entry<LocalMap, Layer> entry : mapsToLayers.entrySet()) {
            Layer remove = entry.getValue();
            layers.remove(remove);
            remove.onDestroy();

            if (remove instanceof TileLayer)
                ((TileLayer) remove).getTileCache().destroy();
        }
        mapsToLayers.clear();

        // add map as the first to be behind all additional layers
        layers.add(0, layer);
        mapsToLayers.put(map, layer);

        handleBackground();
        handleOverlays();

        // then start download layer threads
        if (layer instanceof TileDownloadLayer)
           ((TileDownloadLayer) layer).start();

        // center and zoom: if map is initialized, doesn't contain route or there is no route
        BoundingBox mapBoundingBox = getMapBoundingBox();
        BoundingBox routeBoundingBox = getRouteBoundingBox();
        if (centerAndZoom &&
                ((mapBoundingBox != null && routeBoundingBox != null && !mapBoundingBox.contains(routeBoundingBox)) ||
                        routeBoundingBox == null)) {
            boolean alwaysZoom = mapBoundingBox == null || !mapBoundingBox.contains(getCenter());
            centerAndZoom(mapBoundingBox, routeBoundingBox, alwaysZoom, alwaysRecenter);
        }
        limitZoomLevel();
        log.info("Using map " + mapsToLayers.keySet() + " and theme " + getMapManager().getAppliedThemeModel().getItem() + " with zoom " + getZoom());
    }

    private void handleOverlays() {
        Layers layers = getLayerManager().getLayers();
        layers.remove(overlaysLayer);
        layers.add(overlaysLayer);
    }

    private void handleOverlayInsertion(int firstRow, int lastRow) {
        for (int i = firstRow; i < lastRow + 1; i++) {
            TileServer tileServer = mapViewCallback.getTileServerMapManager().getAppliedOverlaysModel().getItem(i);
            TileServerMapSource mapSource = new TileServerMapSource(tileServer);
            mapSource.setAlpha(true);
            TileDownloadLayer overlay = new TileDownloadLayer(createTileCache(tileServer.getId()), mapView.getModel().mapViewPosition, mapSource, GRAPHIC_FACTORY);
            overlaysLayer.layers.add(overlay);
            overlay.setDisplayModel(mapView.getModel().displayModel);
            overlay.start();
            getLayerManager().redrawLayers();
        }
    }

    private void handleOverlayDeletion(int firstRow, int lastRow) {
        for (int i = lastRow; i >= firstRow; i--) {
            Layer layer = overlaysLayer.layers.get(i);
            TileDownloadLayer overlay = (TileDownloadLayer) layer;
            overlaysLayer.layers.remove(overlay);
            overlaysLayer.requestRedraw();
            overlay.onDestroy();
        }
    }

    private void handleBackground() {
        if (backgroundLayer == null)
            return;

        Layers layers = getLayerManager().getLayers();
        layers.remove(backgroundLayer);

        LocalMap map = getMapManager().getDisplayedMapModel().getItem();
        if (map.isVector())
            layers.add(0, backgroundLayer);
    }

    private void handleShadedHills() {
        hillsRenderConfig.setTileSource(null);

        if (mapViewCallback.getShowShadedHills().getBoolean()) {
            ElevationService elevationService = mapViewCallback.getElevationService();
            if (elevationService.isDownload()) {
                File directory = elevationService.getDirectory();
                if (directory != null && directory.exists()) {
                    MemoryCachingHgtReaderTileSource tileSource = new MemoryCachingHgtReaderTileSource(directory, new DiffuseLightShadingAlgorithm(), GRAPHIC_FACTORY);
                    tileSource.setEnableInterpolationOverlap(true);
                    hillsRenderConfig.setTileSource(tileSource);
                    hillsRenderConfig.indexOnThread();
                }
            }
        }
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

    public boolean isInitialized() {
        return true;
    }

    public boolean isDownload() {
        return true;
    }

    public Throwable getInitializationCause() {
        return null;
    }

    public void dispose() {
        getMapManager().getDisplayedMapModel().removeChangeListener(displayedMapListener);
        getMapManager().getAppliedThemeModel().removeChangeListener(appliedThemeListener);
        mapViewCallback.getTileServerMapManager().getAppliedOverlaysModel().removeTableModelListener(appliedOverlayListener);

        positionsModel.removeTableModelListener(positionsModelListener);
        characteristicsModel.removeListDataListener(characteristicsModelListener);
        mapViewCallback.removeRoutingServiceChangeListener(routingServiceListener);
        routeColorModel.removeChangeListener(colorModelListener);
        trackColorModel.removeChangeListener(colorModelListener);
        unitSystemModel.removeChangeListener(unitSystemListener);
        mapViewCallback.getShowShadedHills().removeChangeListener(shadedHillsListener);

        long start = currentTimeMillis();
        if (routeRenderer != null)
            routeRenderer.dispose();

        updateDecoupler.dispose();

        long end = currentTimeMillis();
        log.info("RouteRenderer stopped after " + (end - start) + " ms");

        NavigationPosition center = getCenter();
        preferences.putDouble(CENTER_LONGITUDE_PREFERENCE, center.getLongitude());
        preferences.putDouble(CENTER_LATITUDE_PREFERENCE, center.getLatitude());
        int zoom = getZoom();
        preferences.putInt(CENTER_ZOOM_PREFERENCE, zoom);

        mapView.destroyAll();
    }

    public Component getComponent() {
        return mapSelector.getComponent();
    }

    public void resize() {
        // intentionally left empty
    }

    @SuppressWarnings("unchecked")
    public void showAllPositions() {
        List<NavigationPosition> positions = positionsModel.getRoute().getPositions();
        if (positions.size() > 0) {
            BoundingBox both = new BoundingBox(positions);
            zoomToBounds(both);
            setCenter(both.getCenter(), true);
        }
    }

    private Polyline mapBorder, routeBorder;

    public void showMapBorder(BoundingBox mapBoundingBox) {
        if (mapBorder != null) {
            removeLayer(mapBorder);
            mapBorder = null;
        }
        if (routeBorder != null) {
            removeLayer(routeBorder);
            routeBorder = null;
        }

        if (mapBoundingBox != null) {
            mapBorder = drawBorder(mapBoundingBox);

            BoundingBox routeBoundingBox = getRouteBoundingBox();
            if (routeBoundingBox != null)
                routeBorder = drawBorder(routeBoundingBox);

            centerAndZoom(mapBoundingBox, routeBoundingBox, true, true);
        }
    }

    private Polyline drawBorder(BoundingBox boundingBox) {
        Paint paint = GRAPHIC_FACTORY.createPaint();
        paint.setColor(BLUE);
        paint.setStrokeWidth(3);
        paint.setDashPathEffect(new float[]{3, 12});
        Polyline polyline = new Polyline(asLatLong(boundingBox), paint, getTileSize());
        addLayer(polyline);
        return polyline;
    }

    public void addLayer(final Layer layer) {
        invokeInAwtEventQueue(new Runnable() {
            public void run() {
                getLayerManager().getLayers().add(layer);
                if (!getLayerManager().getLayers().contains(layer))
                    log.warning("Cannot add layer " + layer);
            }
        });
    }

    public void addLayers(final List<? extends ObjectWithLayer> withLayers) {
        invokeInAwtEventQueue(new Runnable() {
            public void run() {
                for (int i = 0, c = withLayers.size(); i < c; i++) {
                    final ObjectWithLayer withLayer = withLayers.get(i);
                    final Layer layer = withLayer.getLayer();
                    if (layer != null) {
                        // redraw only for last added layer
                        boolean redraw = i == c - 1;
                        getLayerManager().getLayers().add(layer, redraw);
                        if (!getLayerManager().getLayers().contains(layer))
                            log.warning("Cannot add layer " + layer);
                    } else
                        log.warning("Could not find layer to add for " + withLayer);
                }
            }
        });
    }

    public void removeLayer(final Layer layer) {
        invokeInAwtEventQueue(new Runnable() {
            public void run() {
                if (!getLayerManager().getLayers().remove(layer))
                    log.warning("Cannot remove layer " + layer);
            }
        });
    }

    private void removeLayers(final List<? extends ObjectWithLayer> withLayers, final boolean clearLayer) {
        invokeInAwtEventQueue(new Runnable() {
            public void run() {
                for (int i = 0, c = withLayers.size(); i < c; i++) {
                    ObjectWithLayer withLayer = withLayers.get(i);
                    Layer layer = withLayer.getLayer();
                    if (layer != null) {
                        // redraw only for last removed layer
                        boolean redraw = i == c - 1;
                        if (!getLayerManager().getLayers().remove(layer, redraw))
                            log.warning("Cannot remove layer " + layer);
                    } else
                        log.warning("Could not find layer to remove for " + withLayer);

                    if (clearLayer)
                        withLayer.setLayer(null);
                }
            }
        });
    }

    private BoundingBox getMapBoundingBox() {
        Collection<Layer> values = mapsToLayers.values();
        if (!values.isEmpty()) {
            Layer layer = values.iterator().next();
            if (layer instanceof TileRendererLayer) {
                TileRendererLayer tileRendererLayer = (TileRendererLayer) layer;
                return toBoundingBox(tileRendererLayer.getMapDataStore().boundingBox());
            }
        }
        return null;
    }

    public int getTileSize() {
        return mapView.getModel().displayModel.getTileSize();
    }

    @SuppressWarnings("unchecked")
    private BoundingBox getRouteBoundingBox() {
        BaseRoute route = positionsModel.getRoute();
        return route != null && route.getPositions().size() > 0 ? new BoundingBox(route.getPositions()) : null;
    }

    private void centerAndZoom(BoundingBox mapBoundingBox, BoundingBox routeBoundingBox,
                               boolean alwaysZoom, boolean alwaysRecenter) {
        List<NavigationPosition> positions = new ArrayList<>();

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
            if (alwaysZoom)
                zoomToBounds(both);
            setCenter(both.getCenter(), alwaysRecenter);
        }
    }

    private void limitZoomLevel() {
        LocalMap map = mapsToLayers.keySet().iterator().next();

        byte zoomLevelMin = map.isVector() ? MINIMUM_ZOOM_LEVEL : map.getTileSource().getZoomLevelMin();
        // limit minimum zoom to prevent zooming out too much and losing the map
        MapViewDimension mapViewDimension = mapView.getModel().mapViewDimension;
        if (map.isVector() && mapViewDimension.getDimension() != null)
            zoomLevelMin = (byte) max(0, zoomForBounds(mapViewDimension.getDimension(),
                    asBoundingBox(map.getBoundingBox()), getTileSize()) - 3);

        MapViewPosition mapViewPosition = mapView.getModel().mapViewPosition;
        mapViewPosition.setZoomLevelMin(zoomLevelMin);

        byte zoomLevelMax = map.isVector() ? MAXIMUM_ZOOM_LEVEL : map.getTileSource().getZoomLevelMax();
        // limit maximum to prevent zooming in to grey area
        mapViewPosition.setZoomLevelMax(zoomLevelMax);
    }

    private boolean isVisible(LatLong latLong) {
        MapViewProjection projection = new MapViewProjection(mapView);
        LatLong upperLeft = projection.fromPixels(MINIMUM_VISIBLE_BORDER_IN_PIXEL, MINIMUM_VISIBLE_BORDER_IN_PIXEL);
        Dimension dimension = mapView.getDimension();
        LatLong lowerRight = projection.fromPixels(dimension.width - MINIMUM_VISIBLE_BORDER_IN_PIXEL, dimension.height - MINIMUM_VISIBLE_BORDER_IN_PIXEL);
        return upperLeft != null && lowerRight != null && new org.mapsforge.core.model.BoundingBox(lowerRight.latitude, upperLeft.longitude, upperLeft.latitude, lowerRight.longitude).contains(latLong);
    }

    public NavigationPosition getCenter() {
        return asNavigationPosition(mapView.getModel().mapViewPosition.getCenter());
    }

    private void setCenter(LatLong center, boolean alwaysRecenter) {
        if (alwaysRecenter || recenterAfterZooming.getBoolean() || !isVisible(center))
            mapView.getModel().mapViewPosition.animateTo(center);
    }

    private void setCenter(NavigationPosition center, boolean alwaysRecenter) {
        setCenter(asLatLong(center), alwaysRecenter);
    }

    private int getZoom() {
        return mapView.getModel().mapViewPosition.getZoomLevel();
    }

    private void setZoom(int zoom) {
        mapView.setZoomLevel((byte) zoom);
    }

    private void zoomToBounds(org.mapsforge.core.model.BoundingBox boundingBox) {
        Dimension dimension = mapView.getModel().mapViewDimension.getDimension();
        if (dimension == null)
            return;
        byte zoom = zoomForBounds(dimension, boundingBox, getTileSize());
        setZoom(zoom);
    }

    private void zoomToBounds(BoundingBox boundingBox) {
        zoomToBounds(asBoundingBox(boundingBox));
    }

    public boolean isSupportsPrinting() {
        return false;
    }

    public void print(String title) {
        throw new UnsupportedOperationException("Printing not supported");
    }

    public String getMapsPath() {
        return getMapManager().getMapsPath();
    }

    public void setMapsPath(String path) throws IOException {
        getMapManager().setMapsPath(path);
        getMapManager().scanMaps();
    }

    public String getThemesPath() {
        return getMapManager().getThemePath();
    }

    public void setThemesPath(String path) throws IOException {
        getMapManager().setThemePath(path);
        getMapManager().scanThemes();
    }

    public void setSelectedPositions(int[] selectedPositions, boolean replaceSelection) {
        if (selectionUpdater == null)
            return;
        selectionUpdater.setSelectedPositions(selectedPositions, replaceSelection);
    }

    public void setSelectedPositions(List<NavigationPosition> selectedPositions) {
        throw new UnsupportedOperationException("photo panel not available in " + MapsforgeMapView.class.getSimpleName());
    }

    private LatLong getMousePosition() {
        Point point = mapViewMoverAndZoomer.getLastMousePoint();
        return point != null ? new MapViewProjection(mapView).fromPixels(point.getX(), point.getY()) :
                mapView.getModel().mapViewPosition.getCenter();
    }

    private double getThresholdForPixel(LatLong latLong) {
        long mapSize = getMapSize(mapView.getModel().mapViewPosition.getZoomLevel(), getTileSize());
        double metersPerPixel = calculateGroundResolution(latLong.latitude, mapSize);
        return metersPerPixel * SELECTION_CIRCLE_IN_PIXEL;
    }

    private void selectPosition(LatLong latLong, Double threshold, boolean replaceSelection) {
        int row = positionsModel.getClosestPosition(latLong.longitude, latLong.latitude, threshold);
        if (row != -1 && !mapViewMoverAndZoomer.isMousePressedOnMarker()) {
            log.info("Selecting position at " + latLong + ", row is " + row);
            positionsSelectionModel.setSelectedPositions(new int[]{row}, replaceSelection);
        }
    }

    private class SelectPositionAction extends FrameAction {
        public void run() {
            LatLong latLong = getMousePosition();
            if (latLong != null) {
                Double threshold = getThresholdForPixel(latLong);
                selectPosition(latLong, threshold, true);
            }
        }
    }

    private class ExtendSelectionAction extends FrameAction {
        public void run() {
            LatLong latLong = getMousePosition();
            if (latLong != null) {
                Double threshold = getThresholdForPixel(latLong);
                selectPosition(latLong, threshold, false);
            }
        }
    }

    private class AddPositionAction extends FrameAction {
        private int getAddRow() {
            List<PositionWithLayer> lastSelectedPositions = selectionUpdater.getPositionWithLayers();
            NavigationPosition position = lastSelectedPositions.size() > 0 ? lastSelectedPositions.get(lastSelectedPositions.size() - 1).getPosition() : null;
            // quite crude logic to be as robust as possible on failures
            if (position == null && positionsModel.getRowCount() > 0)
                position = positionsModel.getPosition(positionsModel.getRowCount() - 1);
            return position != null ? positionsModel.getIndex(position) + 1 : 0;
        }

        private void insertPosition(int row, Double longitude, Double latitude) {
            positionsModel.add(row, longitude, latitude, null, null, null, mapViewCallback.createDescription(positionsModel.getRowCount() + 1, null));
            int[] rows = new int[]{row};
            positionsSelectionModel.setSelectedPositions(rows, true);
            // TODO this results in drawing errors
            // mapViewCallback.complementData(rows, true, true, true, true, false);
        }

        public void run() {
            LatLong latLong = getMousePosition();
            if (latLong != null) {
                int row = getAddRow();
                log.info("Adding position at " + latLong + " to row " + row);
                insertPosition(row, latLong.longitude, latLong.latitude);
            }
        }
    }

    private class DeletePositionAction extends FrameAction {
        private void removePosition(LatLong latLong, Double threshold) {
            int row = positionsModel.getClosestPosition(latLong.longitude, latLong.latitude, threshold);
            log.info("Deleting position at " + latLong + " from row " + row);
            if (row != -1) {
                positionsModel.remove(new int[]{row});
            }
        }

        public void run() {
            LatLong latLong = getMousePosition();
            if (latLong != null) {
                Double threshold = getThresholdForPixel(latLong);
                removePosition(latLong, threshold);
            }
        }
    }

    private class CenterAction extends FrameAction {
        public void run() {
            mapViewMoverAndZoomer.centerToMousePosition();
        }
    }

    private class ZoomAction extends FrameAction {
        private byte zoomLevelDiff;

        private ZoomAction(int zoomLevelDiff) {
            this.zoomLevelDiff = (byte) zoomLevelDiff;
        }

        public void run() {
            mapViewMoverAndZoomer.zoomToMousePosition(zoomLevelDiff);
        }
    }

    private class UpdateDecoupler {
        private final ExecutorService executor = createSingleThreadExecutor("UpdateDecoupler");
        private EventMapUpdater eventMapUpdater = getEventMapUpdaterFor(Waypoints);

        public void replaceRoute() {
            executor.execute(new Runnable() {
                public void run() {
                    // remove all from previous event map updater
                    eventMapUpdater.handleRemove(0, MAX_VALUE);

                    // select current event map updater and let him add all
                    eventMapUpdater = getEventMapUpdaterFor(positionsModel.getRoute().getCharacteristics());
                    eventMapUpdater.handleAdd(0, MapsforgeMapView.this.positionsModel.getRowCount() - 1);
                }
            });
        }

        public void handleUpdate(final int eventType, final int firstRow, final int lastRow) {
            executor.execute(new Runnable() {
                public void run() {
                   switch (eventType) {
                        case INSERT:
                            eventMapUpdater.handleAdd(firstRow, lastRow);
                            break;
                        case UPDATE:
                            eventMapUpdater.handleUpdate(firstRow, lastRow);
                            break;
                        case DELETE:
                            eventMapUpdater.handleRemove(firstRow, lastRow);
                            break;
                        default:
                            throw new IllegalArgumentException("Event type " + eventType + " is not supported");
                    }
                }
            });
        }

        public void dispose() {
            executor.shutdownNow();
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

    private void fireCalculatedDistances(Map<Integer, DistanceAndTime> indexToDistanceAndTime) {
        for (MapViewListener listener : mapViewListeners) {
            listener.calculatedDistances(indexToDistanceAndTime);
        }
    }

    private class PositionsModelListener implements TableModelListener {
        public void tableChanged(TableModelEvent e) {
            switch (e.getType()) {
                case INSERT:
                case DELETE:
                    updateDecoupler.handleUpdate(e.getType(), e.getFirstRow(), e.getLastRow());
                    break;
                case UPDATE:
                    if (positionsModel.isContinousRange())
                        return;
                    if (!(e.getColumn() == DESCRIPTION_COLUMN_INDEX ||
                            e.getColumn() == LONGITUDE_COLUMN_INDEX ||
                            e.getColumn() == LATITUDE_COLUMN_INDEX ||
                            e.getColumn() == ALL_COLUMNS))
                        return;

                    boolean allRowsChanged = isFirstToLastRow(e);
                    if(allRowsChanged)
                        updateDecoupler.replaceRoute();
                    else
                        updateDecoupler.handleUpdate(e.getType(), e.getFirstRow(), e.getLastRow());

                    // center and zoom if a file was just loaded
                    if (allRowsChanged && showAllPositionsAfterLoading.getBoolean())
                        centerAndZoom(getMapBoundingBox(), getRouteBoundingBox(), true, true);
                    break;
                default:
                    throw new IllegalArgumentException("Event type " + e.getType() + " is not supported");
            }
        }
    }

    private class RoutingServiceListener implements ChangeListener {
        public void stateChanged(ChangeEvent e) {
            if (positionsModel.getRoute().getCharacteristics().equals(Route))
                updateDecoupler.replaceRoute();
        }
    }

    private class CharacteristicsModelListener implements ListDataListener {
        public void intervalAdded(ListDataEvent e) {
        }

        public void intervalRemoved(ListDataEvent e) {
        }

        public void contentsChanged(ListDataEvent e) {
            // ignore events following setRoute()
            if (isIgnoreEvent(e))
                return;
            updateDecoupler.replaceRoute();
        }
    }

    private class ShowCoordinatesListener implements ChangeListener {
        public void stateChanged(ChangeEvent e) {
            mapViewCoordinateDisplayer.setShowCoordinates(showCoordinates.getBoolean());
        }
    }

    private class ColorModelListener implements ChangeListener {
        public void stateChanged(ChangeEvent e) {
            updateDecoupler.replaceRoute();
        }
    }

    private class UnitSystemListener implements ChangeListener {
        public void stateChanged(ChangeEvent e) {
            handleUnitSystem();
        }
    }

    private class DisplayedMapListener implements ChangeListener {
        public void stateChanged(ChangeEvent e) {
            handleMapAndThemeUpdate(true, !isVisible(mapView.getModel().mapViewPosition.getCenter()));
        }
    }

    private class AppliedThemeListener implements ChangeListener {
        public void stateChanged(ChangeEvent e) {
            handleMapAndThemeUpdate(false, false);
        }
    }

    private class AppliedOverlayListener implements TableModelListener {
        public void tableChanged(TableModelEvent e) {
            switch (e.getType()) {
                case INSERT:
                    handleOverlayInsertion(e.getFirstRow(), e.getLastRow());
                    break;
                case DELETE:
                    handleOverlayDeletion(e.getFirstRow(), e.getLastRow());
                    break;
                case UPDATE:
                    break;
                default:
                    throw new IllegalArgumentException("Event type " + e.getType() + " is not supported");
            }
        }
    }

    private class ShadedHillsListener implements ChangeListener {
        public void stateChanged(ChangeEvent e) {
            handleShadedHills();
            handleMapAndThemeUpdate(false, false);
        }
    }

}
