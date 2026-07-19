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
import org.mapsforge.map.layer.download.TileDownloadLayer;
import org.mapsforge.map.layer.hills.*;
import org.mapsforge.map.layer.overlay.Marker;
import org.mapsforge.map.layer.renderer.TileRendererLayer;
import org.mapsforge.map.model.DisplayModel;
import org.mapsforge.map.model.MapViewDimension;
import org.mapsforge.map.model.MapViewPosition;
import org.mapsforge.map.model.common.Observer;
import org.mapsforge.map.rendertheme.XmlRenderThemeMenuCallback;
import org.mapsforge.map.rendertheme.XmlRenderThemeStyleLayer;
import org.mapsforge.map.rendertheme.XmlRenderThemeStyleMenu;
import org.mapsforge.map.scalebar.DefaultMapScaleBar;
import org.mapsforge.map.scalebar.ImperialUnitAdapter;
import org.mapsforge.map.scalebar.MetricUnitAdapter;
import org.mapsforge.map.scalebar.NauticalUnitAdapter;
import org.mapsforge.map.util.MapViewProjection;
import org.mapsforge.map.view.MapView;
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
import slash.navigation.maps.mapsforge.ThemeStyle;
import slash.navigation.mapview.BaseMapView;
import slash.navigation.mapview.MapViewCallback;
import slash.navigation.mapview.mapsforge.helpers.*;
import slash.navigation.mapview.mapsforge.lines.Polyline;
import slash.navigation.mapview.mapsforge.models.ThemeStyleImpl;
import slash.navigation.mapview.mapsforge.overlays.DraggableMarker;
import slash.navigation.mapview.mapsforge.overlays.OverlayManager;
import slash.navigation.mapview.mapsforge.renderer.BorderPainter;
import slash.navigation.mapview.mapsforge.renderer.MagnifierPainter;
import slash.navigation.mapview.mapsforge.renderer.MapViewLayerOperations;
import slash.navigation.mapview.mapsforge.renderer.NonSelectedPositionListsRenderer;
import slash.navigation.mapview.mapsforge.renderer.RouteRenderer;
import slash.navigation.mapview.mapsforge.renderer.TrackRenderer;
import slash.navigation.mapview.mapsforge.tiles.DefaultTileLayerFactory;
import slash.navigation.mapview.mapsforge.tiles.TileLayerFactory;
import slash.navigation.mapview.mapsforge.updater.*;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;
import java.util.List;
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
import static org.mapsforge.map.scalebar.DefaultMapScaleBar.ScaleBarMode.SINGLE;
import static slash.common.helpers.ThreadHelper.invokeInAwtEventQueue;
import static slash.common.type.HexadecimalNumber.encodeInt;
import static slash.navigation.base.RouteCharacteristics.Route;
import static slash.navigation.common.BoundingBox.asBoundingBox;
import static slash.navigation.common.TransformUtil.delta;
import static slash.navigation.common.TransformUtil.isPositionInChina;
import static slash.navigation.converter.gui.models.FixMapMode.Yes;
import static slash.navigation.converter.gui.models.PositionColumns.*;
import static slash.navigation.gui.events.IgnoreEvent.isIgnoreEvent;
import static slash.navigation.gui.helpers.JMenuHelper.createItem;
import static slash.navigation.gui.helpers.JTableHelper.isFirstToLastRow;
import static slash.navigation.maps.mapsforge.MapType.Mapsforge;
import static slash.navigation.maps.mapsforge.helpers.MapUtil.toBoundingBox;
import static slash.navigation.mapview.mapsforge.AwtGraphicMapView.GRAPHIC_FACTORY;
import static slash.navigation.mapview.mapsforge.helpers.ColorHelper.asAlpha;
import static slash.navigation.mapview.mapsforge.helpers.MapViewCalculations.collectBoundingPositions;
import static slash.navigation.mapview.mapsforge.helpers.MapViewCalculations.computeAddRow;
import static slash.navigation.mapview.mapsforge.helpers.MapViewCalculations.thresholdForPixel;
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
    private PositionListsModel positionListsModel;
    private MapPreferencesModel preferencesModel;
    private MapsforgeMapViewCallback mapViewCallback;
    private NonSelectedPositionListsRenderer nonSelectedPositionListsRenderer;

    private final TableModelListener positionsModelListener = new PositionsModelListener();
    private final ListDataListener positionListsModelListener = new PositionListsModelListener();
    private final ListDataListener characteristicsModelListener = new CharacteristicsModelListener();
    private final ChangeListener routingPreferencesListener = e -> {
        if (positionsModel.getRoute().getCharacteristics().equals(Route))
            this.updateDecoupler.replaceRoute();
    };
    private final ChangeListener unitSystemListener = e -> handleUnitSystem();
    private final ChangeListener showCoordinatesListener = e ->
            this.mapViewCoordinateDisplayer.setShowCoordinates(preferencesModel.getShowCoordinatesModel().getBoolean());
    private final ChangeListener repaintPositionListListener = e -> {
        synchronized (MapsforgeMapView.this) {
            this.waypointIcon = null;
        }
        this.updateDecoupler.replaceRoute();
        // line widths apply to the gray set, too
        updateNonSelectedPositionLists();
    };
    private final ChangeListener displayedMapListener = e ->
            handleMapAndThemeUpdate(true, !isVisible(this.mapView.getModel().mapViewPosition.getCenter()));
    private final ChangeListener appliedThemeListener = e -> handleMapAndThemeUpdate(false, false);
    private final ChangeListener appliedThemeStyleListener = e -> handleMapAndThemeUpdate(false, false);
    private final TableModelListener appliedOverlayListener = e -> {
        switch (e.getType()) {
            case INSERT -> this.overlayManager.insert(e.getFirstRow(), e.getLastRow());
            case DELETE -> this.overlayManager.delete(e.getFirstRow(), e.getLastRow());
        }
    };
    private final TableModelListener availableMapsListener = e -> {
        // The online tile-server list loads asynchronously (scanTileServers) after
        // the first render, so at startup a persisted online map isn't resolvable
        // yet and the displayed-map model falls back to the OpenStreetMap default.
        // Once the list arrives, re-apply the persisted selection if it now
        // resolves to a map that isn't the one currently on screen. Mirrors the
        // offline re-scan re-apply in RouteConverter#scanLocalMapsAndThemes.
        if (this.mapsToLayers.isEmpty())
            return; // not rendered yet — the first render applies what resolves
        LocalMap displayed = getMapManager().getDisplayedMapModel().getItem();
        if (displayed != null && !this.mapsToLayers.containsKey(displayed))
            handleMapAndThemeUpdate(false, false);
    };
    private final ChangeListener shadedHillsListener = e -> {
        handleShadedHills();
        handleMapAndThemeUpdate(false, false);
    };

    private MapSelector mapSelector;
    private AwtGraphicMapView mapView;
    private MapViewMoverAndZoomer mapViewMoverAndZoomer;
    private final MapViewCoordinateDisplayer mapViewCoordinateDisplayer = new MapViewCoordinateDisplayer();
    private final BorderPainter borderPainter = new BorderPainter();
    private final MagnifierPainter magnifierPainter = new MagnifierPainter();
    private RouteRenderer routeRenderer;
    private TrackRenderer trackRenderer;
    private TileLayerFactory tileLayerFactory;
    private OverlayManager overlayManager;
    private Layer backgroundLayer;
    private final DelegatingShadeTileSource shadeTileSource = new DelegatingShadeTileSource();
    private final HillsRenderConfig hillsRenderConfig = new HillsRenderConfig(shadeTileSource);
    private SelectionUpdater selectionUpdater;
    private EventMapUpdater routeUpdater, trackUpdater, waypointUpdater;
    private UpdateDecoupler updateDecoupler;

    // initialization

    public void initialize(PositionsModel positionsModel,
                           PositionListsModel positionListsModel,
                           MapPreferencesModel preferencesModel,
                           MapViewCallback mapViewCallback) {
        this.positionsModel = positionsModel;
        this.positionListsModel = positionListsModel;
        this.preferencesModel = preferencesModel;
        this.mapViewCallback = (MapsforgeMapViewCallback) mapViewCallback;

        this.selectionUpdater = new SelectionUpdater(positionsModel, new SelectionOperation() {
            private Bitmap markerIcon;
            {
                try {
                    markerIcon = GRAPHIC_FACTORY.renderSvg(MapsforgeMapView.class.getResourceAsStream("marker.svg"), 1.0f, 32, 54, 100, 1234567892);
                } catch (IOException e) {
                    log.severe("Cannot create marker icon: " + e);
                }
            }

            private Marker createMarker(PositionWithLayer positionWithLayer, LatLong latLong) {
                return new DraggableMarker(MapsforgeMapView.this, positionWithLayer, latLong, markerIcon, 0, -25);
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

        this.updateDecoupler = new UpdateDecoupler(positionsModel, this::getEventMapUpdaterFor);

        positionsModel.addTableModelListener(positionsModelListener);
        positionListsModel.addListDataListener(positionListsModelListener);
        preferencesModel.getRoutingPreferencesModel().addChangeListener(routingPreferencesListener);
        preferencesModel.getCharacteristicsModel().addListDataListener(characteristicsModelListener);
        preferencesModel.getUnitSystemModel().addChangeListener(unitSystemListener);
        preferencesModel.getShowCoordinatesModel().addChangeListener(showCoordinatesListener);
        preferencesModel.getShowShadedHills().addChangeListener(shadedHillsListener);
        preferencesModel.getFixMapModeModel().addChangeListener(repaintPositionListListener);
        preferencesModel.getRouteColorModel().addChangeListener(repaintPositionListListener);
        preferencesModel.getRouteLineWidthModel().addChangeListener(repaintPositionListListener);
        preferencesModel.getTrackColorModel().addChangeListener(repaintPositionListListener);
        preferencesModel.getTrackLineWidthModel().addChangeListener(repaintPositionListListener);
        preferencesModel.getWaypointColorModel().addChangeListener(repaintPositionListListener);

        initializeActions();
        initializeMapView();
        routeRenderer = new RouteRenderer(this, this.mapViewCallback, preferencesModel.getRouteColorModel(),
                preferencesModel.getRouteLineWidthModel(), GRAPHIC_FACTORY);
        trackRenderer = new TrackRenderer(this, preferencesModel.getTrackColorModel(),
                preferencesModel.getTrackLineWidthModel(), GRAPHIC_FACTORY);
        nonSelectedPositionListsRenderer = new NonSelectedPositionListsRenderer(this, positionListsModel,
                preferencesModel.getRouteColorModel(), preferencesModel.getTrackColorModel(),
                preferencesModel.getRouteLineWidthModel(), preferencesModel.getTrackLineWidthModel(), GRAPHIC_FACTORY);
    }

    private static boolean initializedActions = false;

    private synchronized void initializeActions() {
        if (initializedActions)
            return;

        ActionManager actionManager = Application.getInstance().getContext().getActionManager();
        actionManager.register("select-position", new SelectPositionAction());
        actionManager.register("extend-selection", new ExtendSelectionAction());
        actionManager.register("new-position-map", new AddPositionAction());
        actionManager.registerLocal("new-position", MAP, "new-position-map");
        actionManager.register("delete-position-map", new DeletePositionAction());
        actionManager.registerLocal("delete", MAP, "delete-position-map");
        actionManager.register("center-here", new CenterAction());
        actionManager.register("snap-to-road-map", new SnapToRoadAction());
        actionManager.registerLocal("snap-to-road", MAP, "snap-to-road-map");
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

        tileLayerFactory = new DefaultTileLayerFactory(getMapManager(), mapView.getModel().mapViewPosition,
                hillsRenderConfig, menuCallback, GRAPHIC_FACTORY);
        overlayManager = new OverlayManager(tileLayerFactory,
                mapViewCallback.getTileServerMapManager().getAppliedOverlaysModel(), new OverlayManager.Context() {
            public DisplayModel getDisplayModel() {
                return mapView.getModel().displayModel;
            }

            public void redrawLayers() {
                getLayerManager().redrawLayers();
            }

            public void forceOverlayDisplay() {
                mapView.getModel().mapViewPosition.moveCenter(0.0, 0.0);
                mapView.repaint();
            }
        });

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
                    overlayManager.insert(0, mapViewCallback.getTileServerMapManager().getAppliedOverlaysModel().getRowCount() - 1);
                    initialized = true;
                }
            }
        });

        getMapManager().getDisplayedMapModel().addChangeListener(displayedMapListener);
        getMapManager().getAvailableMapsModel().addTableModelListener(availableMapsListener);
        getMapManager().getAppliedThemeModel().addChangeListener(appliedThemeListener);
        getMapManager().getAppliedThemeStyleModel().addChangeListener(appliedThemeStyleListener);
        mapViewCallback.getTileServerMapManager().getAppliedOverlaysModel().addTableModelListener(appliedOverlayListener);
    }

    public void setBackgroundMap(File backgroundMap) {
        long length = backgroundMap.length();
        try {
            // createBackgroundLayer validates the mapsforge header via new MapFile, so a truncated
            // or wrong-content file (e.g. a stale world.map that never re-downloaded) throws here.
            // Without this guard the exception escaped silently on the EDT and the map just stayed
            // blank with nothing in the log.
            backgroundLayer = tileLayerFactory.createBackgroundLayer(backgroundMap);
            handleBackground();
            log.info(format("Loaded background map %s (%d bytes)", backgroundMap, length));
        } catch (Exception e) {
            backgroundLayer = null;
            log.severe(format("Cannot load background map %s (%d bytes): %s", backgroundMap, length, e));
        }
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
            String opacity = Transfer.formatDoubleAsString(Float.valueOf(asAlpha(waypointColorModel)).doubleValue(), 2);
            waypointIcon = createWaypointIcon(color, opacity);
        }
        return waypointIcon;
    }

    public Bitmap createWaypointIcon(String color, String opacity) {
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
        return new AwtBitmap(bufferedImage);
    }

    private void handleUnitSystem() {
        UnitSystem unitSystem = preferencesModel.getUnitSystemModel().getUnitSystem();
        switch (unitSystem) {
            case Metric -> mapView.getMapScaleBar().setDistanceUnitAdapter(MetricUnitAdapter.INSTANCE);
            case Statute -> mapView.getMapScaleBar().setDistanceUnitAdapter(ImperialUnitAdapter.INSTANCE);
            case Nautic -> mapView.getMapScaleBar().setDistanceUnitAdapter(NauticalUnitAdapter.INSTANCE);
        }
    }

    private JPopupMenu createPopupMenu() {
        JPopupMenu menu = new JPopupMenu();
        menu.add(createItem("select-position"));
        menu.add(createItem("new-position"));
        menu.add(createItem("delete"));
        menu.add(createItem("snap-to-road"));
        menu.addSeparator();
        menu.add(createItem("center-here"));
        menu.add(createItem("zoom-in"));
        menu.add(createItem("zoom-out"));
        return menu;
    }

    private static final String THEME_STYLE_ALL = "theme-style-all";
    private final MenuCallback menuCallback = new MenuCallback();

    private class MenuCallback implements XmlRenderThemeMenuCallback {
        public Set<String> getCategories(XmlRenderThemeStyleMenu renderThemeStyleMenu) {
            Map<String, XmlRenderThemeStyleLayer> layers = renderThemeStyleMenu.getLayers();
            String stylePreference = getMapManager().getAppliedThemeStyleModel().getItem() != null ? getMapManager().getAppliedThemeStyleModel().getItem().getUrl() : null;
            String style = stylePreference == null ? renderThemeStyleMenu.getDefaultValue() : stylePreference;
            List<ThemeStyle> themeStyles = layers.values().stream().
                    filter(XmlRenderThemeStyleLayer::isVisible).
                    map(layer -> (ThemeStyle) new ThemeStyleImpl(renderThemeStyleMenu, layer)).
                    toList();
            getMapManager().setThemeStyles(themeStyles, style);

            XmlRenderThemeStyleLayer renderThemeStyleLayer = renderThemeStyleMenu.getLayer(style);
            if (THEME_STYLE_ALL.equals(stylePreference)) {
                return null;

            } else if (renderThemeStyleLayer == null) {
                log.warning("Invalid style \"" + style + "\", showing all styles");
                return null;
            }

            Set<String> categories = renderThemeStyleLayer.getCategories();
            for (XmlRenderThemeStyleLayer overlay : renderThemeStyleLayer.getOverlays()) {
                if (overlay.isEnabled()) {
                    categories.addAll(overlay.getCategories());
                }
            }
            return !categories.isEmpty() ? categories : null;
        }
    }

    private final Map<LocalMap, Layer> mapsToLayers = new HashMap<>();

    private void handleMapAndThemeUpdate(boolean centerAndZoom, boolean alwaysRecenter) {
        Layers layers = getLayerManager().getLayers();

        // add new map with a theme
        LocalMap map = getMapManager().getDisplayedMapModel().getItem();
        Layer layer;
        try {
            layer = tileLayerFactory.createLayerForMap(map);
        } catch (Exception e) {
            mapViewCallback.showMapException(map != null ? map.description() : "<no map>", e);
            return;
        }

        // remove old map
        for (Map.Entry<LocalMap, Layer> entry : mapsToLayers.entrySet()) {
            Layer remove = entry.getValue();
            layers.remove(remove);
            remove.onDestroy();

            if (remove instanceof TileLayer tileLayer)
                tileLayer.getTileCache().destroy();
        }
        mapsToLayers.clear();

        // add map as the first to be behind all additional layers
        layers.add(0, layer);
        mapsToLayers.put(map, layer);

        handleBackground();
        handleOverlays();
        handleNonSelectedPositionLists();

        // then start download layer threads
        if (layer instanceof TileDownloadLayer tileDownloadLayer)
           tileDownloadLayer.start();

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
        layers.remove(overlayManager.getLayer());
        layers.add(overlayManager.getLayer());
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

    private void handleNonSelectedPositionLists() {
        Layer nonSelectedLayer = nonSelectedPositionListsRenderer.getLayer();
        Layers layers = getLayerManager().getLayers();
        layers.remove(nonSelectedLayer);

        // insert directly above the map (and background) layers so that the selected
        // position list and the selection markers are always drawn on top of the gray set
        int index = 0;
        for (Layer layer : mapsToLayers.values())
            index = max(index, layers.indexOf(layer) + 1);
        layers.add(index, nonSelectedLayer);

        // catch position lists that were loaded before the map was initialized
        nonSelectedPositionListsRenderer.update();
    }

    private void updateNonSelectedPositionLists() {
        // may fire from list/preference listeners registered before the renderer is built
        if (nonSelectedPositionListsRenderer != null)
            nonSelectedPositionListsRenderer.update();
    }

    private void handleShadedHills() {
        shadeTileSource.setDelegate(null);

        if (preferencesModel.getShowShadedHills().getBoolean()) {
            ElevationService elevationService = mapViewCallback.getElevationService();
            if (elevationService.isDownload()) {
                File directory = elevationService.getDirectory();
                if (directory != null && directory.exists()) {
                    MemoryCachingHgtReaderTileSource tileSource = new MemoryCachingHgtReaderTileSource(
                            new DemFolderFS(directory), new DiffuseLightShadingAlgorithm(), GRAPHIC_FACTORY, true);
                    shadeTileSource.setDelegate(tileSource);
                    hillsRenderConfig.indexOnThread();
                }
            }
        }
    }

    private EventMapUpdater getEventMapUpdaterFor(RouteCharacteristics characteristics) {
        return switch (characteristics) {
            case Route -> routeUpdater;
            case Track -> trackUpdater;
            case Waypoints -> waypointUpdater;
        };
    }

    public boolean isDownload() {
        return true;
    }

    public String getMapIdentifier() {
        return getMapManager().getDisplayedMapModel().getItem().description();
    }

    public Throwable getInitializationCause() {
        return null;
    }

    public void dispose() {
        getMapManager().getDisplayedMapModel().removeChangeListener(displayedMapListener);
        getMapManager().getAvailableMapsModel().removeTableModelListener(availableMapsListener);
        getMapManager().getAppliedThemeModel().removeChangeListener(appliedThemeListener);
        getMapManager().getAppliedThemeStyleModel().removeChangeListener(appliedThemeStyleListener);
        mapViewCallback.getTileServerMapManager().getAppliedOverlaysModel().removeTableModelListener(appliedOverlayListener);

        positionsModel.removeTableModelListener(positionsModelListener);
        positionListsModel.removeListDataListener(positionListsModelListener);
        preferencesModel.getRoutingPreferencesModel().removeChangeListener(routingPreferencesListener);
        preferencesModel.getCharacteristicsModel().removeListDataListener(characteristicsModelListener);
        preferencesModel.getUnitSystemModel().removeChangeListener(unitSystemListener);
        preferencesModel.getShowCoordinatesModel().removeChangeListener(showCoordinatesListener);
        preferencesModel.getShowShadedHills().removeChangeListener(shadedHillsListener);
        preferencesModel.getFixMapModeModel().removeChangeListener(repaintPositionListListener);
        preferencesModel.getRouteColorModel().removeChangeListener(repaintPositionListListener);
        preferencesModel.getRouteLineWidthModel().removeChangeListener(repaintPositionListListener);
        preferencesModel.getTrackColorModel().removeChangeListener(repaintPositionListListener);
        preferencesModel.getTrackLineWidthModel().removeChangeListener(repaintPositionListListener);
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

        hillsRenderConfig.interruptAndDestroy();
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
        if (!positions.isEmpty()) {
            BoundingBox both = asBoundingBox(positions);
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
            if (layer instanceof TileRendererLayer tileRendererLayer) {
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
        return route != null && !route.getPositions().isEmpty() ? asBoundingBox(route.getPositions()) : null;
    }

    private boolean isGoogleMap(LocalMap map) {
        return map.getCopyrightText().contains("Google");
    }

    private boolean isFixMap(Double longitude, Double latitude) {
        FixMapMode fixMapMode = preferencesModel.getFixMapModeModel().getFixMapMode();
        LocalMap map = getMapManager().getDisplayedMapModel().getItem();
        return fixMapMode.equals(Yes) && isGoogleMap(map) && isPositionInChina(longitude, latitude);
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
                boundingBox.northEast(),
                boundingBox.getSouthEast(),
                boundingBox.southWest(),
                boundingBox.getNorthWest(),
                boundingBox.northEast()
        ));
    }

    org.mapsforge.core.model.BoundingBox asMapsforgeBoundingBox(BoundingBox boundingBox) {
        return new org.mapsforge.core.model.BoundingBox(
                boundingBox.southWest().getLatitude(),
                boundingBox.southWest().getLongitude(),
                boundingBox.northEast().getLatitude(),
                boundingBox.northEast().getLongitude()
        );
    }

    private NavigationPosition asNavigationPosition(LatLong latLong) {
        return new SimpleNavigationPosition(latLong.longitude, latLong.latitude);
    }

    private void centerAndZoom(BoundingBox mapBoundingBox, BoundingBox routeBoundingBox,
                               boolean alwaysZoom, boolean alwaysRecenter) {
        List<NavigationPosition> positions = collectBoundingPositions(mapBoundingBox, routeBoundingBox);

        if (!positions.isEmpty()) {
            BoundingBox both = asBoundingBox(positions);
            if (alwaysZoom)
                zoomToBounds(both);
            setCenter(both.getCenter(), alwaysRecenter);
        }
    }

    private void limitZoomLevel() {
        LocalMap map = mapsToLayers.keySet().iterator().next();
        MapViewPosition mapViewPosition = mapView.getModel().mapViewPosition;

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
                    asMapsforgeBoundingBox(map.getBoundingBox()), getTileSize()) - 3);
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

    public void setCenter(NavigationPosition position) {
        if (position != null)
            setCenter(position, true);
    }

    private void setCenter(NavigationPosition center, boolean alwaysRecenter) {
        setCenter(asLatLong(center), alwaysRecenter);
    }

    private void setCenter(LatLong center, boolean alwaysRecenter) {
        if (alwaysRecenter || mapViewCallback.isRecenterAfterZooming() || !isVisible(center))
            mapView.getModel().mapViewPosition.animateTo(center);
    }

    public BoundingBox getBoundingBox() {
        if (mapView == null || mapView.getWidth() <= 0 || mapView.getHeight() <= 0)
            return null;

        org.mapsforge.core.model.BoundingBox boundingBox = mapView.getBoundingBox();
        return boundingBox != null ? toBoundingBox(boundingBox) : null;
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
        zoomToBounds(asMapsforgeBoundingBox(boundingBox));
    }

    public /*for DraggableMarker*/ MapView getMapView() {
        return mapView;
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
                        asList(position.getLongitude() + diffLongitude, position.getLatitude() + diffLatitude)), false, true);
            } else {

                if(preferencesModel.getCharacteristicsModel().getSelectedCharacteristics().equals(Route)) {
                    NavigationPosition roadPosition = mapViewCallback.getRoutingService().getSnapToRoadPosition(new SimpleNavigationPosition(longitude, latitude));
                    if (roadPosition != null) {
                        longitude = roadPosition.getLongitude();
                        latitude = roadPosition.getLatitude();
                    }
                }

                positionsModel.edit(index, new PositionColumnValues(asList(LONGITUDE_COLUMN_INDEX, LATITUDE_COLUMN_INDEX),
                        asList(longitude, latitude)), false, true);
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
        if (selectionUpdater == null)
            return;
        selectionUpdater.setSelectedPositions(selectedPositions);
    }

    private LatLong getMousePosition() {
        Point point = mapViewMoverAndZoomer.getLastMousePoint();
        return point != null ? new MapViewProjection(mapView).fromPixels(point.getX(), point.getY()) :
                mapView.getModel().mapViewPosition.getCenter();
    }

    private double getThresholdForPixel(LatLong latLong) {
        return thresholdForPixel(latLong.latitude, mapView.getModel().mapViewPosition.getZoomLevel(), getTileSize(), SELECTION_CIRCLE_IN_PIXEL);
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
            NavigationPosition lastSelected = !lastSelectedPositions.isEmpty() ? lastSelectedPositions.get(lastSelectedPositions.size() - 1).getPosition() : null;
            return computeAddRow(lastSelected, positionsModel);
        }

        private void insertPosition(int row, Double longitude, Double latitude) {
            positionsModel.add(row, longitude, latitude, null, null, null, mapViewCallback.createDescription(positionsModel.getRowCount() + 1, null));
            int[] rows = new int[]{row};
            mapViewCallback.complementData(rows, true, true, true, true, false);
            mapViewCallback.setSelectedPositions(rows, true);
        }

        public void run() {
            LatLong latLong = getMousePosition();
            if (latLong != null) {
                if(preferencesModel.getCharacteristicsModel().getSelectedCharacteristics().equals(Route)) {
                    NavigationPosition roadPosition = mapViewCallback.getRoutingService().getSnapToRoadPosition(asNavigationPosition(latLong));
                    if (roadPosition != null) {
                        latLong = asLatLong(roadPosition);
                    }
                }

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

    private class SnapToRoadAction extends FrameAction {
        private void updatePosition(NavigationPosition position, NavigationPosition roadPosition) {
            positionsModel.edit(positionsModel.getIndex(position),
                    new PositionColumnValues(asList(LONGITUDE_COLUMN_INDEX, LATITUDE_COLUMN_INDEX),
                            asList(roadPosition.getLongitude(), roadPosition.getLatitude())), true, true);
        }

        public void run() {
            List<NavigationPosition> selectedPositions = toPositions(selectionUpdater.getPositionWithLayers());
            for(NavigationPosition position : selectedPositions) {
                NavigationPosition roadPosition = mapViewCallback.getRoutingService().getSnapToRoadPosition(position);
                if(roadPosition != null) {
                    updatePosition(position, roadPosition);
                }
            }
            setSelectedPositions(selectedPositions);
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
                NavigationPosition center = !selectedPositions.isEmpty() ? asBoundingBox(selectedPositions).getCenter() : getCenter();
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
            }
        }
    }

    // listeners

    private class PositionListsModelListener implements ListDataListener {
        public void intervalAdded(ListDataEvent e) {
            updateNonSelectedPositionLists();
        }

        public void intervalRemoved(ListDataEvent e) {
            updateNonSelectedPositionLists();
        }

        public void contentsChanged(ListDataEvent e) {
            // selection changes are fired as contentsChanged: the previously selected
            // list joins the gray set, the newly selected one leaves it
            updateNonSelectedPositionLists();
        }
    }

    private class PositionsModelListener implements TableModelListener {
        public void tableChanged(TableModelEvent e) {
            switch (e.getType()) {
                case INSERT, DELETE -> updateDecoupler.handleUpdate(e.getType(), e.getFirstRow(), e.getLastRow());
                case UPDATE -> {
                    if (positionsModel.isContinousRangeOperation())
                        return;
                    if (!(e.getColumn() == DESCRIPTION_COLUMN_INDEX ||
                            e.getColumn() == LONGITUDE_COLUMN_INDEX ||
                            e.getColumn() == LATITUDE_COLUMN_INDEX ||
                            e.getColumn() == ALL_COLUMNS))
                        return;
                    boolean allRowsChanged = isFirstToLastRow(e);
                    if (allRowsChanged)
                        updateDecoupler.replaceRoute();
                    else
                        updateDecoupler.handleUpdate(e.getType(), e.getFirstRow(), e.getLastRow());

                    // center and zoom if a file was just loaded
                    if (allRowsChanged && mapViewCallback.isShowAllPositionsAfterLoading())
                        centerAndZoom(getMapBoundingBox(), getRouteBoundingBox(), true, true);
                }
                default -> throw new IllegalArgumentException("Event type " + e.getType() + " is not supported");
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

}
