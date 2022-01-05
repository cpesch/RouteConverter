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
import org.mapsforge.map.awt.graphics.AwtBitmap;
import org.mapsforge.map.layer.*;
import org.mapsforge.map.layer.cache.FileSystemTileCache;
import org.mapsforge.map.layer.cache.InMemoryTileCache;
import org.mapsforge.map.layer.cache.TileCache;
import org.mapsforge.map.layer.cache.TwoLevelTileCache;
import org.mapsforge.map.layer.download.TileDownloadLayer;
import org.mapsforge.map.layer.hills.DiffuseLightShadingAlgorithm;
import org.mapsforge.map.layer.hills.HillsRenderConfig;
import org.mapsforge.map.layer.hills.MemoryCachingHgtReaderTileSource;
import org.mapsforge.map.layer.overlay.Marker;
import org.mapsforge.map.layer.renderer.TileRendererLayer;
import org.mapsforge.map.model.DisplayModel;
import org.mapsforge.map.model.IMapViewPosition;
import org.mapsforge.map.model.MapViewDimension;
import org.mapsforge.map.model.common.Observer;
import org.mapsforge.map.reader.MapFile;
import org.mapsforge.map.scalebar.DefaultMapScaleBar;
import org.mapsforge.map.scalebar.ImperialUnitAdapter;
import org.mapsforge.map.scalebar.MetricUnitAdapter;
import org.mapsforge.map.scalebar.NauticalUnitAdapter;
import org.mapsforge.map.util.MapViewProjection;
import slash.common.io.TokenReplacingReader;
import slash.common.io.TokenResolver;
import slash.common.io.Transfer;
import slash.navigation.base.BaseRoute;
import slash.navigation.base.RouteCharacteristics;
import slash.navigation.common.BoundingBox;
import slash.navigation.common.NavigationPosition;
import slash.navigation.common.SimpleNavigationPosition;
import slash.navigation.common.UnitSystem;
import slash.navigation.converter.gui.models.*;
import slash.navigation.elevation.ElevationService;
import slash.navigation.gui.Application;
import slash.navigation.gui.actions.ActionManager;
import slash.navigation.gui.actions.FrameAction;
import slash.navigation.maps.mapsforge.LocalMap;
import slash.navigation.maps.mapsforge.MapsforgeMapManager;
import slash.navigation.maps.mapsforge.impl.MBTilesFileMap;
import slash.navigation.maps.mapsforge.impl.MapsforgeFileMap;
import slash.navigation.maps.mapsforge.impl.TileDownloadMap;
import slash.navigation.maps.mapsforge.mbtiles.TileMBTilesLayer;
import slash.navigation.maps.mapsforge.models.TileServerMapSource;
import slash.navigation.maps.tileserver.TileServer;
import slash.navigation.mapview.BaseMapView;
import slash.navigation.mapview.MapViewCallback;
import slash.navigation.mapview.mapsforge.helpers.*;
import slash.navigation.mapview.mapsforge.lines.Polyline;
import slash.navigation.mapview.mapsforge.overlays.DraggableMarker;
import slash.navigation.mapview.mapsforge.renderer.RouteRenderer;
import slash.navigation.mapview.mapsforge.renderer.TrackRenderer;
import slash.navigation.mapview.mapsforge.updater.*;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.List;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;

import static java.awt.event.InputEvent.CTRL_DOWN_MASK;
import static java.awt.event.KeyEvent.*;
import static java.lang.Integer.MAX_VALUE;
import static java.lang.Math.max;
import static java.lang.String.format;
import static java.lang.System.currentTimeMillis;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static javax.swing.JComponent.WHEN_IN_FOCUSED_WINDOW;
import static javax.swing.KeyStroke.getKeyStroke;
import static javax.swing.SwingUtilities.invokeLater;
import static javax.swing.event.TableModelEvent.*;
import static org.mapsforge.core.graphics.Color.BLUE;
import static org.mapsforge.core.util.LatLongUtils.zoomForBounds;
import static org.mapsforge.core.util.MercatorProjection.calculateGroundResolution;
import static org.mapsforge.core.util.MercatorProjection.getMapSize;
import static org.mapsforge.map.scalebar.DefaultMapScaleBar.ScaleBarMode.SINGLE;
import static slash.common.helpers.ThreadHelper.createSingleThreadExecutor;
import static slash.common.helpers.ThreadHelper.invokeInAwtEventQueue;
import static slash.common.io.Directories.getTemporaryDirectory;
import static slash.common.io.Transfer.encodeUri;
import static slash.common.type.HexadecimalNumber.encodeInt;
import static slash.navigation.base.RouteCharacteristics.Route;
import static slash.navigation.base.RouteCharacteristics.Waypoints;
import static slash.navigation.common.TransformUtil.delta;
import static slash.navigation.common.TransformUtil.isPositionInChina;
import static slash.navigation.converter.gui.models.FixMapMode.Automatic;
import static slash.navigation.converter.gui.models.FixMapMode.Yes;
import static slash.navigation.converter.gui.models.PositionColumns.*;
import static slash.navigation.gui.events.IgnoreEvent.isIgnoreEvent;
import static slash.navigation.gui.helpers.JMenuHelper.createItem;
import static slash.navigation.gui.helpers.JTableHelper.isFirstToLastRow;
import static slash.navigation.maps.mapsforge.MapType.Mapsforge;
import static slash.navigation.maps.mapsforge.helpers.MapUtil.toBoundingBox;
import static slash.navigation.mapview.mapsforge.AwtGraphicMapView.GRAPHIC_FACTORY;
import static slash.navigation.mapview.mapsforge.helpers.ColorHelper.asAlpha;
import static slash.navigation.mapview.mapsforge.helpers.SVGHelper.getResourceBitmap;
import static slash.navigation.mapview.mapsforge.helpers.WithLayerHelper.*;
import static slash.navigation.mapview.mapsforge.models.LocalNames.MAP;

/**
 * Implementation for a component that displays the positions of a position list on a map
 * using the rewrite branch of the mapsforge project.
 *
 * @author Christian Pesch
 */

public class MapsforgeMapView extends BaseMapView {
    private static final Preferences preferences = Preferences.userNodeForPackage(MapsforgeMapView.class);
    private static final Logger log = Logger.getLogger(MapsforgeMapView.class.getName());

    private static final String READ_BUFFER_SIZE_PREFERENCE = "readBufferSize";
    private static final String FIRST_LEVEL_TILE_CACHE_SIZE_PREFERENCE = "firstLevelTileCacheSize";
    private static final String SECOND_LEVEL_TILE_CACHE_SIZE_PREFERENCE = "secondLevelTileCacheSize";
    private static final String DEVICE_SCALE_FACTOR = "mapScaleFactor";
    private static final int SCROLL_DIFF_IN_PIXEL = 100;
    private static final int MINIMUM_VISIBLE_BORDER_IN_PIXEL = 20;
    private static final int SELECTION_CIRCLE_IN_PIXEL = 15;
    private static final byte MINIMUM_ZOOM_LEVEL = 2;
    private static final byte MAXIMUM_ZOOM_LEVEL = 22;

    private PositionsModel positionsModel;
    private MapPreferencesModel preferencesModel;
    private MapViewCallbackOpenSource mapViewCallback;

    private PositionsModelListener positionsModelListener = new PositionsModelListener();
    private RoutingPreferencesListener routingPreferencesListener = new RoutingPreferencesListener();
    private CharacteristicsModelListener characteristicsModelListener = new CharacteristicsModelListener();
    private UnitSystemListener unitSystemListener = new UnitSystemListener();
    private ShowCoordinatesListener showCoordinatesListener = new ShowCoordinatesListener();
    private RepaintPositionListListener repaintPositionListListener = new RepaintPositionListListener();
    private DisplayedMapListener displayedMapListener = new DisplayedMapListener();
    private AppliedThemeListener appliedThemeListener = new AppliedThemeListener();
    private AppliedOverlayListener appliedOverlayListener = new AppliedOverlayListener();
    private ShadedHillsListener shadedHillsListener = new ShadedHillsListener();

    private MapSelector mapSelector;
    private AwtGraphicMapView mapView;
    private MapViewMoverAndZoomer mapViewMoverAndZoomer;
    private MapViewCoordinateDisplayer mapViewCoordinateDisplayer = new MapViewCoordinateDisplayer();
    private BorderPainter borderPainter = new BorderPainter();
    private MagnifierPainter magnifierPainter = new MagnifierPainter();
    private RouteRenderer routeRenderer;
    private TrackRenderer trackRenderer;
    private GroupLayer overlaysLayer = new GroupLayer();
    private TileRendererLayer backgroundLayer;
    private HillsRenderConfig hillsRenderConfig = new HillsRenderConfig(null);
    private SelectionUpdater selectionUpdater;
    private EventMapUpdater routeUpdater, trackUpdater, waypointUpdater;
    private UpdateDecoupler updateDecoupler;

    // initialization

    public void initialize(PositionsModel positionsModel,
                           MapPreferencesModel preferencesModel,
                           MapViewCallback mapViewCallback) {
        this.positionsModel = positionsModel;
        this.preferencesModel = preferencesModel;
        this.mapViewCallback = (MapViewCallbackOpenSource) mapViewCallback;

        this.selectionUpdater = new SelectionUpdater(positionsModel, new SelectionOperation() {
            private Bitmap markerIcon;
            {
                try {
                    markerIcon = GRAPHIC_FACTORY.renderSvg(MapsforgeMapView.class.getResourceAsStream("marker.svg"), 1.0f, 64, 54, 100, 1234567890);
                } catch (IOException e) {
                    log.severe("Cannot create marker icon: " + e);
                }
            }

            private Marker createMarker(PositionWithLayer positionWithLayer, LatLong latLong) {
                return new DraggableMarker(MapsforgeMapView.this, positionWithLayer, latLong, markerIcon, 13, -23);
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
                addObjectsWithLayer(withLayers);
                if (center != null)
                    setCenter(center, false);
            }

            public void remove(List<PositionWithLayer> positionWithLayers) {
                removeObjectWithLayers(positionWithLayers);
            }
        });

        this.routeUpdater = new TrackUpdater(positionsModel, new TrackOperation() {
            public void add(List<PairWithLayer> pairWithLayers) {
                routeRenderer.renderRoute(getMapIdentifier(), pairWithLayers,
                        () -> mapViewCallback.getDistanceAndTimeAggregator().addDistancesAndTimes(toDistanceAndTimes(pairWithLayers)));
            }

            public void update(List<PairWithLayer> pairWithLayers) {
                removeLayers(toLayers(pairWithLayers));
                routeRenderer.renderRoute(getMapIdentifier(), pairWithLayers,
                        () -> mapViewCallback.getDistanceAndTimeAggregator().updateDistancesAndTimes(toDistanceAndTimes(pairWithLayers)));
            }

            public void remove(List<PairWithLayer> pairWithLayers) {
                removeLayers(toLayers(pairWithLayers));
                mapViewCallback.getDistanceAndTimeAggregator().removeDistancesAndTimes(toDistanceAndTimes(pairWithLayers));
            }
        });

        this.trackUpdater = new TrackUpdater(positionsModel, new TrackOperation() {
            public void add(List<PairWithLayer> pairWithLayers) {
                trackRenderer.renderTrack(pairWithLayers, () -> mapViewCallback.getDistanceAndTimeAggregator().addDistancesAndTimes(toDistanceAndTimes(pairWithLayers)));
            }

            public void update(List<PairWithLayer> pairWithLayers) {
                removeLayers(toLayers(pairWithLayers));
                trackRenderer.renderTrack(pairWithLayers, () -> mapViewCallback.getDistanceAndTimeAggregator().updateDistancesAndTimes(toDistanceAndTimes(pairWithLayers)));
            }

            public void remove(List<PairWithLayer> pairWithLayers) {
                removeLayers(toLayers(pairWithLayers));
                mapViewCallback.getDistanceAndTimeAggregator().removeDistancesAndTimes(toDistanceAndTimes(pairWithLayers));
            }
        });

        this.waypointUpdater = new WaypointUpdater(positionsModel, new WaypointOperation() {
            private Marker createMarker(PositionWithLayer positionWithLayer) {
               return new Marker(asLatLong(positionWithLayer.getPosition()), createWaypointIcon(), 0, 0);
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
                addObjectsWithLayer(withLayers);
            }

            public void update(List<PositionWithLayer> positionWithLayers) {
                List<Layer> remove = toLayers(positionWithLayers);
                removeLayers(remove);
                add(positionWithLayers);
            }

            public void remove(List<PositionWithLayer> positionWithLayers) {
                removeObjectWithLayers(positionWithLayers);
            }
        });

        this.updateDecoupler = new UpdateDecoupler();

        positionsModel.addTableModelListener(positionsModelListener);
        preferencesModel.getRoutingPreferencesModel().addChangeListener(routingPreferencesListener);
        preferencesModel.getCharacteristicsModel().addListDataListener(characteristicsModelListener);
        preferencesModel.getUnitSystemModel().addChangeListener(unitSystemListener);
        preferencesModel.getShowCoordinatesModel().addChangeListener(showCoordinatesListener);
        preferencesModel.getShowShadedHills().addChangeListener(shadedHillsListener);
        preferencesModel.getFixMapModeModel().addChangeListener(repaintPositionListListener);
        preferencesModel.getRouteColorModel().addChangeListener(repaintPositionListListener);
        preferencesModel.getTrackColorModel().addChangeListener(repaintPositionListListener);
        preferencesModel.getWaypointColorModel().addChangeListener(repaintPositionListListener);

        initializeActions();
        initializeMapView();
        routeRenderer = new RouteRenderer(this, this.mapViewCallback, preferencesModel.getRouteColorModel(), GRAPHIC_FACTORY);
        trackRenderer = new TrackRenderer(this, preferencesModel.getTrackColorModel(), GRAPHIC_FACTORY);
    }

    private static boolean initializedActions = false;

    private synchronized void initializeActions() {
        if (initializedActions)
            return;

        ActionManager actionManager = Application.getInstance().getContext().getActionManager();
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
                actionManager.run("zoom-in");
            }
        }, getKeyStroke(VK_ADD, CTRL_DOWN_MASK), WHEN_IN_FOCUSED_WINDOW);
        mapSelector.getMapViewPanel().registerKeyboardAction(new FrameAction() {
            public void run() {
                actionManager.run("zoom-out");
            }
        }, getKeyStroke(VK_MINUS, CTRL_DOWN_MASK), WHEN_IN_FOCUSED_WINDOW);
        mapSelector.getMapViewPanel().registerKeyboardAction(new FrameAction() {
            public void run() {
                actionManager.run("zoom-out");
            }
        }, getKeyStroke(VK_SUBTRACT, CTRL_DOWN_MASK), WHEN_IN_FOCUSED_WINDOW);
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

        final IMapViewPosition mapViewPosition = mapView.getModel().mapViewPosition;
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
                    handleOverlayInsert(0, mapViewCallback.getTileServerMapManager().getAppliedOverlaysModel().getRowCount() - 1);
                    initialized = true;
                }
            }
        });

        getMapManager().getDisplayedMapModel().addChangeListener(displayedMapListener);
        getMapManager().getAppliedThemeModel().addChangeListener(appliedThemeListener);
        mapViewCallback.getTileServerMapManager().getAppliedOverlaysModel().addTableModelListener(appliedOverlayListener);
    }

    public void setBackgroundMap(File backgroundMap) {
        backgroundLayer = createTileRendererLayer(new MapFile(backgroundMap), backgroundMap.getName());
        handleBackground();
    }

    public void updateMapAndThemesAfterDirectoryScanning() {
        if (mapView != null)
            handleMapAndThemeUpdate(false, false);
    }

    protected float getDeviceScaleFactor() {
        return preferences.getInt(DEVICE_SCALE_FACTOR, Toolkit.getDefaultToolkit().getScreenResolution()) / 96.0f;
    }

    private AwtGraphicMapView createMapView() {
        // Multithreaded map rendering
        Parameters.NUMBER_OF_THREADS = Runtime.getRuntime().availableProcessors() + 1;
        // Maximum read buffer size
        Parameters.MAXIMUM_BUFFER_SIZE = preferences.getInt(READ_BUFFER_SIZE_PREFERENCE, 16000000);
        // No square frame buffer since the device orientation hardly changes
        Parameters.SQUARE_FRAME_BUFFER = false;
        // Use improved HA3 frame buffer
        Parameters.FRAME_BUFFER_HA3 = true;

        float deviceScaleFactor = getDeviceScaleFactor();
        DisplayModel.setDeviceScaleFactor(deviceScaleFactor);
        log.info(format("Map is scaled with factor %f, screen resolution is %d dpi", deviceScaleFactor, Toolkit.getDefaultToolkit().getScreenResolution()));

        AwtGraphicMapView mapView = new AwtGraphicMapView();
        new MapViewResizer(mapView, mapView.getModel().mapViewDimension);
        mapView.getMapScaleBar().setVisible(true);
        ((DefaultMapScaleBar) mapView.getMapScaleBar()).setScaleBarMode(SINGLE);
        return mapView;
    }

    private Bitmap waypointIcon;

    private synchronized Bitmap createWaypointIcon() {
        if(waypointIcon == null) {
            ColorModel waypointColorModel = preferencesModel.getWaypointColorModel();
            String color = encodeInt(waypointColorModel.getColor().getRed(), 2) +
                    encodeInt(waypointColorModel.getColor().getGreen(), 2) +
                    encodeInt(waypointColorModel.getColor().getBlue(), 2);
            String opacity = Transfer.formatDoubleAsString(new Float(asAlpha(waypointColorModel)).doubleValue(), 2);

            InputStream inputStream = MapsforgeMapView.class.getResourceAsStream("waypoint.svg");
            assert inputStream != null;
            Reader reader = new TokenReplacingReader(new InputStreamReader(inputStream), new TokenResolver() {
                public String resolveToken(String tokenName) {
                    if (tokenName.equals("color"))
                        return color;
                    if (tokenName.equals("opacity"))
                        return opacity;
                    return tokenName;
                }
            });
            BufferedImage bufferedImage = getResourceBitmap(reader, "waypoint-" + color + "-" + opacity, getDeviceScaleFactor(), 100f, 16, 16, 100);
            waypointIcon = new AwtBitmap(bufferedImage);
        }
        return waypointIcon;
    }

    private void handleUnitSystem() {
        UnitSystem unitSystem = preferencesModel.getUnitSystemModel().getUnitSystem();
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

    private TileRendererLayer createTileRendererLayer(MapFile mapFile, String cacheId) {
        TileRendererLayer tileRendererLayer = new TileRendererLayer(createTileCache(cacheId), mapFile,
                mapView.getModel().mapViewPosition, true, true, true,
                GRAPHIC_FACTORY, hillsRenderConfig);
        tileRendererLayer.setXmlRenderTheme(getMapManager().getAppliedThemeModel().getItem().getXmlRenderTheme());
        return tileRendererLayer;
    }

    private TileCache createTileCache(String cacheId) {
        TileCache firstLevelTileCache = new InMemoryTileCache(preferences.getInt(FIRST_LEVEL_TILE_CACHE_SIZE_PREFERENCE, 256));
        File cacheDirectory = new File(getTemporaryDirectory(), encodeUri(cacheId));
        TileCache secondLevelTileCache = new FileSystemTileCache(preferences.getInt(SECOND_LEVEL_TILE_CACHE_SIZE_PREFERENCE, 2048), cacheDirectory, GRAPHIC_FACTORY);
        return new TwoLevelTileCache(firstLevelTileCache, secondLevelTileCache);
    }

    private Layer createLayerForMap(LocalMap map) {
        switch (map.getType()) {
            case Mapsforge:
                return createTileRendererLayer(((MapsforgeFileMap)map).getMapFile(), map.getUrl());
            case MBTiles:
                return new TileMBTilesLayer(createTileCache(map.getUrl()), mapView.getModel().mapViewPosition, true, ((MBTilesFileMap)map).getMBTilesFile(), GRAPHIC_FACTORY);
            case Download:
                return new TileDownloadLayer(createTileCache(map.getUrl()), mapView.getModel().mapViewPosition, ((TileDownloadMap)map).getTileSource(), GRAPHIC_FACTORY);
            default:
                throw new IllegalArgumentException("Unknown MapType " + map.getType());
        }
    }

    private final Map<LocalMap, Layer> mapsToLayers = new HashMap<>();

    private void handleMapAndThemeUpdate(boolean centerAndZoom, boolean alwaysRecenter) {
        Layers layers = getLayerManager().getLayers();

        // add new map with a theme
        LocalMap map = getMapManager().getDisplayedMapModel().getItem();
        Layer layer;
        try {
            layer = createLayerForMap(map);
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
                //noinspection rawtypes
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

    private void handleOverlayInsert(int firstRow, int lastRow) {
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
        // force immediate display of the overlay
        mapView.getModel().mapViewPosition.moveCenter(0.0, 0.0);
    }

    private void handleOverlayDelete(int firstRow, int lastRow) {
        for (int i = lastRow; i >= firstRow; i--) {
            if (i >= overlaysLayer.layers.size())
                continue;

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
        if (map.getType().equals(Mapsforge))
            layers.add(0, backgroundLayer);
    }

    private void handleShadedHills() {
        hillsRenderConfig.setTileSource(null);

        if (preferencesModel.getShowShadedHills().getBoolean()) {
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

    public String getMapIdentifier() {
        return getMapManager().getDisplayedMapModel().getItem().getDescription();
    }

    public Throwable getInitializationCause() {
        return null;
    }

    public void dispose() {
        getMapManager().getDisplayedMapModel().removeChangeListener(displayedMapListener);
        getMapManager().getAppliedThemeModel().removeChangeListener(appliedThemeListener);
        mapViewCallback.getTileServerMapManager().getAppliedOverlaysModel().removeTableModelListener(appliedOverlayListener);

        positionsModel.removeTableModelListener(positionsModelListener);
        preferencesModel.getRoutingPreferencesModel().removeChangeListener(routingPreferencesListener);
        preferencesModel.getCharacteristicsModel().removeListDataListener(characteristicsModelListener);
        preferencesModel.getUnitSystemModel().removeChangeListener(unitSystemListener);
        preferencesModel.getShowCoordinatesModel().removeChangeListener(showCoordinatesListener);
        preferencesModel.getShowShadedHills().removeChangeListener(shadedHillsListener);
        preferencesModel.getFixMapModeModel().removeChangeListener(repaintPositionListListener);
        preferencesModel.getRouteColorModel().removeChangeListener(repaintPositionListListener);
        preferencesModel.getTrackColorModel().removeChangeListener(repaintPositionListListener);
        preferencesModel.getWaypointColorModel().removeChangeListener(repaintPositionListListener);

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

    public void showMapBorder(BoundingBox mapBoundingBox) {
        borderPainter.showMapBorder(mapBoundingBox);
    }

    public void showPositionMagnifier(List<NavigationPosition> positions) {
        magnifierPainter.showPositionMagnifier(positions);
    }

    public void addLayer(Layer layer) {
        addLayers(singletonList(layer));
    }

    public void addLayers(final List<Layer> layers) {
        invokeInAwtEventQueue(new Runnable() {
            public void run() {
                getLayerManager().pause();
                if (!getLayerManager().getLayers().addAll(layers, true))
                    log.warning("Cannot add layers " + layers);
                getLayerManager().proceed();
            }
        });
    }

    public void addObjectsWithLayer(final List<? extends ObjectWithLayer> withLayers) {
        invokeInAwtEventQueue(new Runnable() {
            public void run() {
                List<Layer> layers = new ArrayList<>();

                for (ObjectWithLayer withLayer : withLayers) {
                    Layer layer = withLayer.getLayer();
                    if (layer != null) {
                        layers.add(layer);
                    } else
                        log.warning("Could not find layer to add for " + withLayer);
                }

                addLayers(layers);
            }
        });
    }

    public void removeLayer(Layer layer) {
        removeLayers(singletonList(layer));
    }

    private void removeLayers(final List<Layer> layers) {
        invokeInAwtEventQueue(new Runnable() {
            public void run() {
                getLayerManager().pause();
                if (!getLayerManager().getLayers().removeAll(layers, true))
                    log.warning("Cannot remove layers " + layers);
                getLayerManager().proceed();
            }
        });
    }

    private void removeObjectWithLayers(final List<? extends ObjectWithLayer> withLayers) {
        invokeInAwtEventQueue(new Runnable() {
            public void run() {
                List<Layer> layers = new ArrayList<>();

                for (ObjectWithLayer withLayer : withLayers) {
                    Layer layer = withLayer.getLayer();
                    if (layer != null) {
                        layers.add(layer);
                    } else
                        log.warning("Could not find layer to remove for " + withLayer);
                    withLayer.setLayer(null);
                }

                removeLayers(layers);
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

    private boolean isGoogleMap(LocalMap map) {
        return map.getCopyrightText().contains("Google");
    }

    private boolean isFixMap(Double longitude, Double latitude) {
        FixMapMode fixMapMode = preferencesModel.getFixMapModeModel().getFixMapMode();
        LocalMap map = getMapManager().getDisplayedMapModel().getItem();
        return fixMapMode.equals(Yes) || fixMapMode.equals(Automatic) && isGoogleMap(map) && isPositionInChina(longitude, latitude);
    }

    public LatLong asLatLong(NavigationPosition position) {
        if (position == null)
            return null;

        double longitude = position.getLongitude() != null ? position.getLongitude() : 0.0;
        double latitude = position.getLatitude() != null ? position.getLatitude() : 0.0;
        if (isFixMap(longitude, latitude)) {
            double[] delta = delta(latitude, longitude);
            longitude += delta[1];
            latitude += delta[0];
        }
        return new LatLong(latitude, longitude);
    }

    public List<LatLong> asLatLong(List<NavigationPosition> positions) {
        List<LatLong> result = new ArrayList<>();
        for (NavigationPosition position : positions) {
            LatLong latLong = asLatLong(position);
            if (latLong != null)
                result.add(latLong);
        }
        return result;
    }

    private List<LatLong> asLatLong(BoundingBox boundingBox) {
        return asLatLong(asList(
                boundingBox.getNorthEast(),
                boundingBox.getSouthEast(),
                boundingBox.getSouthWest(),
                boundingBox.getNorthWest(),
                boundingBox.getNorthEast()
        ));
    }

    org.mapsforge.core.model.BoundingBox asBoundingBox(BoundingBox boundingBox) {
        return new org.mapsforge.core.model.BoundingBox(
                boundingBox.getSouthWest().getLatitude(),
                boundingBox.getSouthWest().getLongitude(),
                boundingBox.getNorthEast().getLatitude(),
                boundingBox.getNorthEast().getLongitude()
        );
    }

    private NavigationPosition asNavigationPosition(LatLong latLong) {
        return new SimpleNavigationPosition(latLong.longitude, latLong.latitude);
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
        IMapViewPosition mapViewPosition = mapView.getModel().mapViewPosition;

        // first set maximum as the implementation checks that minimum > maximum
        Integer max = map.getZoomLevelMax();
        byte zoomLevelMax = max != null ? max.byteValue() : MAXIMUM_ZOOM_LEVEL;
        // limit maximum to prevent zooming in to grey areas
        mapViewPosition.setZoomLevelMax(zoomLevelMax);

        // don't zoom above maximum
        if(mapViewPosition.getZoomLevel() > zoomLevelMax)
            mapViewPosition.setZoomLevel(zoomLevelMax);

        Integer min = map.getZoomLevelMin();
        byte zoomLevelMin = min != null ? min.byteValue() : MINIMUM_ZOOM_LEVEL;
        // limit minimum zoom to prevent zooming out too much and losing the map
        MapViewDimension mapViewDimension = mapView.getModel().mapViewDimension;
        if (map.getType().equals(Mapsforge) && mapViewDimension.getDimension() != null)
            zoomLevelMin = (byte) max(0, zoomForBounds(mapViewDimension.getDimension(),
                    asBoundingBox(map.getBoundingBox()), getTileSize()) - 3);
        mapViewPosition.setZoomLevelMin(zoomLevelMin);

        // don't zoom below minimum
        if(mapViewPosition.getZoomLevel() < zoomLevelMin)
            mapViewPosition.setZoomLevel(zoomLevelMin);
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
        if (alwaysRecenter || mapViewCallback.isRecenterAfterZooming() || !isVisible(center))
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

    public void movePosition(PositionWithLayer positionWithLayer, Double longitude, Double latitude) {
        final int row = positionsModel.getIndex(positionWithLayer.getPosition());
        if(row == -1) {
            log.warning("Marker without position " + this);
            return;
        }

        NavigationPosition reference = positionsModel.getPosition(row);
        Double diffLongitude = reference != null ? longitude - reference.getLongitude() : 0.0;
        Double diffLatitude = reference != null ? latitude - reference.getLatitude() : 0.0;

        boolean moveCompleteSelection = preferences.getBoolean(MOVE_COMPLETE_SELECTION_PREFERENCE, true);
        boolean cleanElevation = preferences.getBoolean(CLEAN_ELEVATION_ON_MOVE_PREFERENCE, false);
        boolean complementElevation = preferences.getBoolean(COMPLEMENT_ELEVATION_ON_MOVE_PREFERENCE, true);
        boolean cleanTime = preferences.getBoolean(CLEAN_TIME_ON_MOVE_PREFERENCE, false);
        boolean complementTime = preferences.getBoolean(COMPLEMENT_TIME_ON_MOVE_PREFERENCE, true);

        int firstIndex = MAX_VALUE;
        int lastIndex = 0;
        for (int index : selectionUpdater.getIndices()) {
            if (index < firstIndex)
                firstIndex = index;
            if (index > lastIndex)
                lastIndex = index;

            NavigationPosition position = positionsModel.getPosition(index);
            if (position == null)
                continue;

            if (index != row) {
                if (!moveCompleteSelection)
                    continue;

                positionsModel.edit(index, new PositionColumnValues(asList(LONGITUDE_COLUMN_INDEX, LATITUDE_COLUMN_INDEX),
                        Arrays.asList(position.getLongitude() + diffLongitude, position.getLatitude() + diffLatitude)), false, true);
            } else {
                positionsModel.edit(index, new PositionColumnValues(asList(LONGITUDE_COLUMN_INDEX, LATITUDE_COLUMN_INDEX),
                        Arrays.asList(longitude, latitude)), false, true);
            }

            if (cleanTime)
                positionsModel.edit(index, new PositionColumnValues(DATE_TIME_COLUMN_INDEX, null), false, false);
            if (cleanElevation)
                positionsModel.edit(index, new PositionColumnValues(ELEVATION_COLUMN_INDEX, null), false, false);

            if (preferences.getBoolean(COMPLEMENT_DATA_PREFERENCE, false) && (complementTime || complementElevation))
                mapViewCallback.complementData(new int[]{index}, false, complementTime, complementElevation, true, false);
        }

        // one big large update event at the end
        positionsModel.fireTableRowsUpdated(firstIndex, lastIndex, ALL_COLUMNS);

        invokeLater(() -> setSelectedPositions(selectionUpdater.getIndices(), true));
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
            mapViewCallback.setSelectedPositions(new int[]{row}, replaceSelection);
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
            mapViewCallback.setSelectedPositions(rows, true);
            mapViewCallback.complementData(rows, true, true, true, true, false);
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
        private final byte zoomLevelDiff;

        private ZoomAction(int zoomLevelDiff) {
            this.zoomLevelDiff = (byte) zoomLevelDiff;
        }

        public void run() {
            if (mapViewCallback.isRecenterAfterZooming()) {
                List<NavigationPosition> selectedPositions = toPositions(selectionUpdater.getPositionWithLayers());
                NavigationPosition center = selectedPositions.size() > 0 ? new BoundingBox(selectedPositions).getCenter() : getCenter();
                mapViewMoverAndZoomer.zoomToPosition(zoomLevelDiff, asLatLong(center));
            } else
                mapViewMoverAndZoomer.zoomToMousePosition(zoomLevelDiff);
        }
    }

    private class BorderPainter {
        private Polyline mapBorder, routeBorder;

        private Polyline drawBorder(BoundingBox boundingBox) {
            Paint paint = GRAPHIC_FACTORY.createPaint();
            paint.setColor(BLUE);
            paint.setStrokeWidth(3);
            paint.setDashPathEffect(new float[]{3, 12});
            Polyline polyline = new Polyline(asLatLong(boundingBox), paint, getTileSize());
            addLayer(polyline);
            return polyline;
        }

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

                boolean zoomToMap = routeBoundingBox == null || mapBoundingBox.contains(routeBoundingBox);
                centerAndZoom(mapBoundingBox, zoomToMap ? mapBoundingBox : routeBoundingBox, true, true);
            }
        }
    }

    private class MagnifierPainter {
        private Bitmap magnifierIcon;
        {
            try {
                magnifierIcon = GRAPHIC_FACTORY.renderSvg(MapsforgeMapView.class.getResourceAsStream("magnifier.svg"), 1.0f, 57, 56, 100, 1234567891);
            } catch (IOException e) {
                log.severe("Cannot create magnifier icon: " + e);
            }
        }
        private final List<Layer> markers = new ArrayList<>();

        public void showPositionMagnifier(List<NavigationPosition> positions) {
            if(!markers.isEmpty()) {
                removeLayers(markers);
                markers.clear();
            }

            if(positions != null && !positions.isEmpty()) {
                List<Layer> icons = positions.stream()
                        .map(position -> new Marker(asLatLong(position), magnifierIcon, -10, 13))
                        .collect(Collectors.toList());
                addLayers(icons);
                markers.addAll(icons);

                setCenter(new BoundingBox(positions).getCenter(), true);
            }
        }
    }

    private class UpdateDecoupler {
        private final ExecutorService executor = createSingleThreadExecutor("UpdateDecoupler");
        private EventMapUpdater eventMapUpdater = getEventMapUpdaterFor(Waypoints);

        public void replaceRoute() {
            executor.execute(() -> {
                // remove all from previous event map updater
                eventMapUpdater.handleRemove(0, MAX_VALUE);

                // select current event map updater and let him add all
                eventMapUpdater = getEventMapUpdaterFor(positionsModel.getRoute().getCharacteristics());
                eventMapUpdater.handleAdd(0, positionsModel.getRowCount() - 1);
            });
        }

        public void handleUpdate(final int eventType, final int firstRow, final int lastRow) {
            executor.execute(() -> {
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
            });
        }

        public void dispose() {
            executor.shutdownNow();
        }
    }

    // listeners

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
                    if (allRowsChanged && mapViewCallback.isShowAllPositionsAfterLoading())
                        centerAndZoom(getMapBoundingBox(), getRouteBoundingBox(), true, true);
                    break;
                default:
                    throw new IllegalArgumentException("Event type " + e.getType() + " is not supported");
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
            if (isIgnoreEvent(e))
                return;
            // move behind OverlayPositionsModel calling DistanceAndTimeAggregator#clearDistancesAndTimes
            // which would clear the already calculated track data
            invokeLater(() -> updateDecoupler.replaceRoute());
        }
    }

    private class ShowCoordinatesListener implements ChangeListener {
        public void stateChanged(ChangeEvent e) {
            mapViewCoordinateDisplayer.setShowCoordinates(preferencesModel.getShowCoordinatesModel().getBoolean());
        }
    }

    private class RepaintPositionListListener implements ChangeListener {
        public void stateChanged(ChangeEvent e) {
            synchronized (MapsforgeMapView.this) {
                waypointIcon = null;
            }
            updateDecoupler.replaceRoute();
        }
    }

    private class RoutingPreferencesListener implements ChangeListener {
        public void stateChanged(ChangeEvent e) {
            if (positionsModel.getRoute().getCharacteristics().equals(Route))
                updateDecoupler.replaceRoute();
        }
    }

    private class UnitSystemListener implements ChangeListener {
        public void stateChanged(ChangeEvent e) {
            handleUnitSystem();
        }
    }

    private class DisplayedMapListener implements ChangeListener {
        private LocalMap lastMap;

        public void stateChanged(ChangeEvent e) {
            handleMapAndThemeUpdate(true, !isVisible(mapView.getModel().mapViewPosition.getCenter()));

            // if the map changes from/to Google in Automatic mode, do a recalculation
            LocalMap currentMap = getMapManager().getDisplayedMapModel().getItem();
            if(preferencesModel.getFixMapModeModel().getFixMapMode().equals(Automatic)) {
                if(lastMap == null || isGoogleMap(lastMap) != isGoogleMap(currentMap))
                    updateDecoupler.replaceRoute();
            }
            lastMap = currentMap;
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
                    handleOverlayInsert(e.getFirstRow(), e.getLastRow());
                    break;
                case DELETE:
                    handleOverlayDelete(e.getFirstRow(), e.getLastRow());
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
