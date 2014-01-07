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
import org.mapsforge.core.model.Dimension;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.MapPosition;
import org.mapsforge.map.layer.Layer;
import org.mapsforge.map.layer.LayerManager;
import org.mapsforge.map.layer.Layers;
import org.mapsforge.map.layer.cache.InMemoryTileCache;
import org.mapsforge.map.layer.cache.TileCache;
import org.mapsforge.map.layer.download.TileDownloadLayer;
import org.mapsforge.map.layer.download.tilesource.OpenCycleMap;
import org.mapsforge.map.layer.download.tilesource.OpenStreetMapMapnik;
import org.mapsforge.map.layer.download.tilesource.TileSource;
import org.mapsforge.map.layer.overlay.Marker;
import org.mapsforge.map.layer.renderer.TileRendererLayer;
import org.mapsforge.map.model.MapViewPosition;
import org.mapsforge.map.model.common.Observer;
import org.mapsforge.map.rendertheme.ExternalRenderTheme;
import org.mapsforge.map.rendertheme.XmlRenderTheme;
import slash.navigation.base.BaseNavigationPosition;
import slash.navigation.base.BoundingBox;
import slash.navigation.base.NavigationPosition;
import slash.navigation.base.RouteCharacteristics;
import slash.navigation.brouter.BRouter;
import slash.navigation.common.BasicPosition;
import slash.navigation.converter.gui.augment.PositionAugmenter;
import slash.navigation.converter.gui.mapview.updater.*;
import slash.navigation.converter.gui.models.CharacteristicsModel;
import slash.navigation.converter.gui.models.PositionsModel;
import slash.navigation.converter.gui.models.PositionsSelectionModel;
import slash.navigation.converter.gui.models.UnitSystemModel;
import slash.navigation.routing.RoutingService;

import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import java.awt.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

import static javax.swing.event.TableModelEvent.*;
import static org.mapsforge.core.util.LatLongUtils.zoomForBounds;
import static org.mapsforge.map.rendertheme.InternalRenderTheme.OSMARENDER;
import static slash.navigation.base.Positions.asPosition;
import static slash.navigation.base.RouteCharacteristics.Waypoints;
import static slash.navigation.converter.gui.mapview.AwtGraphicMapView.GRAPHIC_FACTORY;
import static slash.navigation.gui.helpers.JTableHelper.isFirstToLastRow;

/**
 * Implementation for a component that displays the positions of a position list on a map
 * using the rewrite branch of the mapsforge project.
 *
 * @author Christian Pesch
 */

public class MapsforgeMapView implements MapView {
    private static final Preferences preferences = Preferences.userNodeForPackage(MapsforgeMapView.class);
    private static final Logger log = Logger.getLogger(MapsforgeMapView.class.getName());

    private static final String MAPSFORGE_DIRECTORY_PREFERENCE = "mapsforgeDirectory";
    private static final String CENTER_LATITUDE_PREFERENCE = "centerLatitude";
    private static final String CENTER_LONGITUDE_PREFERENCE = "centerLongitude";
    private static final String CENTER_ZOOM_PREFERENCE = "centerZoom";
    static final File OSMARENDERER_INTERNAL = new File("Osmarenderer (internal)");
    static final File OPEN_STREET_MAP_MAPNIK_ONLINE = new File("OpenStreetMapMapnik (online)");
    static final File OPEN_CYCLE_MAP_ONLINE = new File("OpenCycleMap (online)");

    private PositionsModel positionsModel;
    private PositionsSelectionModel positionsSelectionModel;
    private CharacteristicsModel characteristicsModel;
    private UnitSystemModel unitSystemModel;

    private MapSelector mapSelector;
    private AwtGraphicMapView mapView;
    private Layer mapLayer;
    private static Bitmap markerIcon, waypointIcon;

    private boolean recenterAfterZooming, showCoordinates, showWaypointDescription, avoidHighways, avoidTolls;
    private TravelMode travelMode;
    private PositionAugmenter positionAugmenter;
    private RoutingService routingService = new BRouter();
    private SelectionUpdater selectionUpdater;
    private EventMapUpdater eventMapUpdater, routeUpdater, trackUpdater, waypointUpdater;

    // initialization

    public void initialize(PositionsModel positionsModel,
                           PositionsSelectionModel positionsSelectionModel,
                           CharacteristicsModel characteristicsModel,
                           PositionAugmenter positionAugmenter,
                           boolean recenterAfterZooming,
                           boolean showCoordinates, boolean showWaypointDescription,
                           TravelMode travelMode, boolean avoidHighways, boolean avoidTolls,
                           UnitSystemModel unitSystemModel) {
        initializeMapView();
        setModel(positionsModel, positionsSelectionModel, characteristicsModel, unitSystemModel);
        this.positionAugmenter = positionAugmenter;
        this.recenterAfterZooming = recenterAfterZooming;
        this.showCoordinates = showCoordinates;
        this.showWaypointDescription = showWaypointDescription;
        this.travelMode = travelMode;
        this.avoidHighways = avoidHighways;
        this.avoidTolls = avoidTolls;
    }

    private void initializeMapView() {
        mapView = createMapView();

        try {
            markerIcon = GRAPHIC_FACTORY.createResourceBitmap(MapsforgeMapView.class.getResourceAsStream("marker.png"), -1);
            waypointIcon = GRAPHIC_FACTORY.createResourceBitmap(MapsforgeMapView.class.getResourceAsStream("waypoint.png"), -1);
        } catch (IOException e) {
            log.severe("Cannot create marker and waypoint icon: " + e.getMessage());
        }

        mapSelector = new MapSelector(this, getMapsforgeDirectory(), mapView);

        final MapViewPosition mapViewPosition = mapView.getModel().mapViewPosition;
        mapViewPosition.addObserver(new Observer() {
            public void onChange() {
                mapSelector.zoomChanged(mapViewPosition.getZoomLevel());
            }
        });
        mapViewPosition.setZoomLevelMin((byte) 4);
        mapViewPosition.setZoomLevelMax((byte) 22);

        double longitude = preferences.getDouble(CENTER_LONGITUDE_PREFERENCE, -25.0);
        double latitude = preferences.getDouble(CENTER_LATITUDE_PREFERENCE, 35.0);
        byte zoom = (byte) preferences.getInt(CENTER_ZOOM_PREFERENCE, 8);
        mapViewPosition.setMapPosition(new MapPosition(new LatLong(latitude, longitude), zoom));
    }

    private AwtGraphicMapView createMapView() {
        AwtGraphicMapView mapView = new AwtGraphicMapView();
        mapView.getMapScaleBar().setVisible(true);
        mapView.addComponentListener(new MapViewComponentListener(mapView, mapView.getModel().mapViewDimension));

        MapViewMouseEventListener mapViewMouseEventListener = new MapViewMouseEventListener(mapView.getModel().mapViewPosition);
        mapView.addMouseListener(mapViewMouseEventListener);
        mapView.addMouseMotionListener(mapViewMouseEventListener);
        mapView.addMouseWheelListener(mapViewMouseEventListener);
        return mapView;
    }

    private File getMapsforgeDirectory() {
        String directoryName = preferences.get(MAPSFORGE_DIRECTORY_PREFERENCE, new File(System.getProperty("user.home"), ".mapsforge").getAbsolutePath());
        File directory = new File(directoryName);
        if (!directory.exists()) {
            if (!directory.mkdirs()) {
                throw new IllegalArgumentException("Cannot create mapsforge directory " + directory);
            }
        }
        return directory;
    }

    private TileRendererLayer createTileRendererLayer(File mapFile, File themeFile) {
        TileRendererLayer tileRendererLayer = new TileRendererLayer(createTileCache(), mapView.getModel().mapViewPosition, false, GRAPHIC_FACTORY);
        tileRendererLayer.setMapFile(mapFile);
        XmlRenderTheme xmlRenderTheme;
        if (OSMARENDERER_INTERNAL.equals(themeFile))
            xmlRenderTheme = OSMARENDER;
        else
            try {
                xmlRenderTheme = new ExternalRenderTheme(themeFile);
            } catch (FileNotFoundException e) {
                xmlRenderTheme = OSMARENDER;
            }
        tileRendererLayer.setXmlRenderTheme(xmlRenderTheme);
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

    void setMapFile(File mapFile, File themeFile) {
        Layers layers = getLayerManager().getLayers();
        if (mapLayer != null)
            layers.remove(mapLayer);

        if (mapFile == null)
            return;

        if (OPEN_CYCLE_MAP_ONLINE.equals(mapFile))
            this.mapLayer = createTileDownloadLayer(OpenCycleMap.INSTANCE);
        else if (OPEN_STREET_MAP_MAPNIK_ONLINE.equals(mapFile))
            this.mapLayer = createTileDownloadLayer(OpenStreetMapMapnik.INSTANCE);
        else {
            TileRendererLayer tileRendererLayer = createTileRendererLayer(mapFile, themeFile);
            this.mapLayer = tileRendererLayer;
            org.mapsforge.core.model.BoundingBox boundingBox = tileRendererLayer.getMapDatabase().getMapFileInfo().boundingBox;
            setCenter(boundingBox.getCenterPoint());
            zoomToBounds(boundingBox);
        }
        // TODO add fallback if the map file doesn't exist
        layers.add(0, mapLayer);
        getLayerManager().redrawLayers();

        if (mapLayer instanceof TileDownloadLayer)
            ((TileDownloadLayer) mapLayer).start();

        log.info("Using map " + mapFile + " and theme " + themeFile);
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
            private Map<NavigationPosition, Marker> positionsToMarkers = new HashMap<NavigationPosition, Marker>();

            public void add(List<NavigationPosition> positions) {
                for (NavigationPosition position : positions) {
                    Marker marker = new Marker(asLatLong(position), markerIcon, 8, -16);
                    getLayerManager().getLayers().add(marker);
                    positionsToMarkers.put(position, marker);
                }
            }

            public void remove(List<NavigationPosition> positions) {
                for (NavigationPosition position : positions) {
                    Marker marker = positionsToMarkers.get(position);
                    if (marker != null) {
                        getLayerManager().getLayers().remove(marker);
                        positionsToMarkers.remove(position);
                    }
                }
            }
        });

        this.routeUpdater = new TrackUpdater(positionsModel, new TrackOperation() {
            private Map<PositionPair, Polyline> pairsToLines = new HashMap<PositionPair, Polyline>();

            public void add(List<PositionPair> pairs) {
                int tileSize = mapView.getModel().displayModel.getTileSize();
                for (PositionPair pair : pairs) {
                    List<LatLong> latLongs = new ArrayList<LatLong>();
                    latLongs.add(asLatLong(pair.getFirst()));
                    latLongs.addAll(asLatLongs(routingService.getRouteBetween(pair.getFirst(), pair.getSecond())));
                    latLongs.add(asLatLong(pair.getSecond()));
                    Polyline line = new Polyline(latLongs, tileSize);
                    getLayerManager().getLayers().add(line);
                    pairsToLines.put(pair, line);
                }
                getLayerManager().redrawLayers();
            }

            public void remove(List<PositionPair> pairs) {
                for (PositionPair pair : pairs) {
                    Polyline line = pairsToLines.get(pair);
                    if (line != null) {
                        getLayerManager().getLayers().remove(line);
                        pairsToLines.remove(pair);
                    }
                }
                getLayerManager().redrawLayers();    }
        });

        this.trackUpdater = new TrackUpdater(positionsModel, new TrackOperation() {
            private Map<PositionPair, Line> pairsToLines = new HashMap<PositionPair, Line>();

            public void add(List<PositionPair> pairs) {
                int tileSize = mapView.getModel().displayModel.getTileSize();
                for (PositionPair pair : pairs) {
                    Line line = new Line(asLatLong(pair.getFirst()), asLatLong(pair.getSecond()), tileSize);
                    getLayerManager().getLayers().add(line);
                    pairsToLines.put(pair, line);
                }
                getLayerManager().redrawLayers();
            }

            public void remove(List<PositionPair> pairs) {
                for (PositionPair pair : pairs) {
                    Line line = pairsToLines.get(pair);
                    if (line != null) {
                        getLayerManager().getLayers().remove(line);
                        pairsToLines.remove(pair);
                    }
                }
                getLayerManager().redrawLayers();
            }
        });

        this.waypointUpdater = new WaypointUpdater(positionsModel, new WaypointOperation() {
            private Map<NavigationPosition, Marker> positionsToMarkers = new HashMap<NavigationPosition, Marker>();

            public void add(List<NavigationPosition> positions) {
                for (NavigationPosition position : positions) {
                    Marker marker = new Marker(asLatLong(position), waypointIcon, 8, -16);
                    getLayerManager().getLayers().add(marker);
                    positionsToMarkers.put(position, marker);
                }
                getLayerManager().redrawLayers();
            }

            public void remove(List<NavigationPosition> positions) {
                for (NavigationPosition position : positions) {
                    Marker marker = positionsToMarkers.get(position);
                    if (marker != null) {
                        getLayerManager().getLayers().remove(marker);
                        positionsToMarkers.remove(position);
                    }
                }
                getLayerManager().redrawLayers();
            }
        });

        this.eventMapUpdater = waypointUpdater;
        characteristicsModel.addListDataListener(new ListDataListener() {
            private RouteCharacteristics lastCharacteristics = Waypoints;

            public void intervalAdded(ListDataEvent e) {
            }

            public void intervalRemoved(ListDataEvent e) {
            }

            public void contentsChanged(ListDataEvent e) {
                RouteCharacteristics characteristics = MapsforgeMapView.this.characteristicsModel.getSelectedCharacteristics();
                if (characteristics.equals(lastCharacteristics))
                    return;

                eventMapUpdater.handleRemove(0, getPositionsModel().getRowCount() - 1);

                eventMapUpdater = getEventMapUpdaterFor(characteristics);
                eventMapUpdater.handleAdd(0, getPositionsModel().getRowCount() - 1);

                lastCharacteristics = characteristics;
            }
        });

        getPositionsModel().addTableModelListener(new TableModelListener() {
            public void tableChanged(TableModelEvent e) {
                switch (e.getType()) {
                    case INSERT:
                        eventMapUpdater.handleAdd(e.getFirstRow(), e.getLastRow());
                        break;
                    case UPDATE:
                        boolean allRowsChanged = isFirstToLastRow(e);
                        if (allRowsChanged)
                            eventMapUpdater.handleAdd(0, getPositionsModel().getRowCount() - 1);
                        else
                            eventMapUpdater.handleUpdate(e.getFirstRow(), e.getLastRow());

                        if (allRowsChanged) {
                            List<BaseNavigationPosition> positions = getPositionsModel().getRoute().getPositions();
                            if (positions.size() > 0) {
                                BoundingBox boundingBox = new BoundingBox(positions);
                                setCenter(boundingBox.getCenter());
                                zoomToBounds(boundingBox);
                            }
                        }

                        break;
                    case DELETE:
                        eventMapUpdater.handleRemove(e.getFirstRow(), e.getLastRow());
                        break;
                }
            }
        });
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
        return MapsforgeMapView.this.positionsModel;
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
        this.showCoordinates = showCoordinates;
    }

    public void setShowWaypointDescription(boolean showWaypointDescription) {
        this.showWaypointDescription = showWaypointDescription;
    }

    public void setTravelMode(TravelMode travelMode) {
        this.travelMode = travelMode;
    }

    public void setAvoidHighways(boolean avoidHighways) {
        this.avoidHighways = avoidHighways;
    }

    public void setAvoidTolls(boolean avoidTolls) {
        this.avoidTolls = avoidTolls;
    }


    private NavigationPosition asNavigationPosition(LatLong latLong) {
        return asPosition(latLong.longitude, latLong.latitude);
    }

    private LatLong asLatLong(NavigationPosition position) {
        return new LatLong(position.getLatitude(), position.getLongitude());
    }

    private List<LatLong> asLatLong(List<NavigationPosition> positions) {
        List<LatLong> result = new ArrayList<>();
        for (NavigationPosition position : positions) {
            result.add(asLatLong(position));
        }
        return result;
    }

    private org.mapsforge.core.model.BoundingBox asBoundingBox(BoundingBox boundingBox) {
        return new org.mapsforge.core.model.BoundingBox(
                boundingBox.getSouthWest().getLatitude(),
                boundingBox.getSouthWest().getLongitude(),
                boundingBox.getNorthEast().getLatitude(),
                boundingBox.getNorthEast().getLongitude()
        );
    }


    public NavigationPosition getCenter() {
        return asNavigationPosition(mapView.getModel().mapViewPosition.getCenter());
    }

    public void setCenter(LatLong center) {
        mapView.getModel().mapViewPosition.animateTo(center);
    }

    public void setCenter(NavigationPosition center) {
        setCenter(asLatLong(center));
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

    private final java.util.List<MapViewListener> mapViewListeners = new CopyOnWriteArrayList<MapViewListener>();

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
