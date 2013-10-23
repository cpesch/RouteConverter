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
import org.mapsforge.map.model.common.Observer;
import org.mapsforge.map.rendertheme.ExternalRenderTheme;
import org.mapsforge.map.rendertheme.XmlRenderTheme;
import slash.navigation.base.BoundingBox;
import slash.navigation.base.NavigationPosition;
import slash.navigation.converter.gui.augment.PositionAugmenter;
import slash.navigation.converter.gui.models.CharacteristicsModel;
import slash.navigation.converter.gui.models.PositionsModel;
import slash.navigation.converter.gui.models.PositionsSelectionModel;
import slash.navigation.converter.gui.models.UnitSystemModel;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import java.awt.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

import static java.lang.Math.abs;
import static java.lang.Math.floor;
import static java.lang.Math.log;
import static java.lang.Math.min;
import static javax.swing.event.TableModelEvent.DELETE;
import static javax.swing.event.TableModelEvent.INSERT;
import static javax.swing.event.TableModelEvent.UPDATE;
import static org.mapsforge.core.model.Tile.TILE_SIZE;
import static org.mapsforge.core.util.MercatorProjection.latitudeToPixelY;
import static org.mapsforge.core.util.MercatorProjection.longitudeToPixelX;
import static org.mapsforge.map.rendertheme.InternalRenderTheme.OSMARENDER;
import static slash.navigation.base.Positions.asPosition;
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
    private static Bitmap markerIcon;

    private boolean recenterAfterZooming, showCoordinates, showWaypointDescription, avoidHighways, avoidTolls;
    private TravelMode travelMode;
    private PositionAugmenter positionAugmenter;
    private SelectionUpdater selectionUpdater;
    private TrackUpdater trackUpdater;

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
            markerIcon = GRAPHIC_FACTORY.createBitmap(MapsforgeMapView.class.getResourceAsStream("marker.png"));
        } catch (IOException e) {
            log.severe("Cannot create marker icon: " + e.getMessage());
        }

        mapSelector = new MapSelector(this, getMapsforgeDirectory(), mapView);

        mapView.getModel().mapViewPosition.addObserver(new Observer() {
            public void onChange() {
                mapSelector.zoomChanged(mapView.getModel().mapViewPosition.getZoomLevel());
            }
        });

        double longitude = preferences.getDouble(CENTER_LONGITUDE_PREFERENCE, -25.0);
        double latitude = preferences.getDouble(CENTER_LATITUDE_PREFERENCE, 35.0);
        byte zoom = (byte) preferences.getInt(CENTER_ZOOM_PREFERENCE, 8);
        mapView.getModel().mapViewPosition.setMapPosition(new MapPosition(new LatLong(latitude, longitude), zoom));
    }

    private AwtGraphicMapView createMapView() {
        AwtGraphicMapView mapView = new AwtGraphicMapView();
        // TODO avoids NullPointerExceptions in MapScaleBar#draw()
        mapView.getModel().mapViewDimension.setDimension(new Dimension(0, 0));
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
        TileRendererLayer tileRendererLayer = new TileRendererLayer(createTileCache(), mapView.getModel().mapViewPosition, GRAPHIC_FACTORY);
        tileRendererLayer.setMapFile(mapFile);
        XmlRenderTheme xmlRenderTheme;
        if(OSMARENDERER_INTERNAL.equals(themeFile))
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
        TileDownloadLayer tileDownloadLayer = new TileDownloadLayer(createTileCache(), mapView.getModel().mapViewPosition, tileSource, GRAPHIC_FACTORY);
        tileDownloadLayer.start();
        return tileDownloadLayer;
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
        LayerManager layerManager = mapView.getLayerManager();
        Layers layers = layerManager.getLayers();
        if(mapLayer != null)
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
        layerManager.redrawLayers();

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
                for(NavigationPosition position : positions) {
                    Marker marker = new Marker(asLatLong(position), markerIcon, 8, -16);
                    mapView.getLayerManager().getLayers().add(marker);
                    positionsToMarkers.put(position, marker);
                }
                if (positions.size() > 0)
                    setCenter(positions.get(positions.size() - 1));
            }

            public void remove(List<NavigationPosition> positions) {
                for (NavigationPosition position : positions) {
                    Marker marker = positionsToMarkers.get(position);
                    if (marker != null) {
                        mapView.getLayerManager().getLayers().remove(marker);
                        positionsToMarkers.remove(position);
                    }
                }
            }
        });

        this.trackUpdater = new TrackUpdater(positionsModel, new TrackOperation() {
            private Map<PositionPair, Line> pairsToLines = new HashMap<PositionPair, Line>();

            public void add(List<PositionPair> pairs) {
                for (PositionPair pair : pairs) {
                    Line line = new Line(asLatLong(pair.getFirst()), asLatLong(pair.getSecond()));
                    mapView.getLayerManager().getLayers().add(line);
                    pairsToLines.put(pair, line);
                }
            }

            public void remove(List<PositionPair> pairs) {
                for (PositionPair pair : pairs) {
                    Line line = pairsToLines.get(pair);
                    if (line != null) {
                        mapView.getLayerManager().getLayers().remove(line);
                        pairsToLines.remove(pair);
                    }
                }
            }
        });

        this.positionsModel.addTableModelListener(new TableModelListener() {
            public void tableChanged(TableModelEvent e) {
                switch (e.getType()) {
                    case INSERT:
                        trackUpdater.handleAdd(e.getFirstRow(), e.getLastRow());
                        break;
                    case UPDATE:
                        trackUpdater.handleUpdate(e.getFirstRow(), e.getLastRow());

                        boolean allRowsChanged = isFirstToLastRow(e);
                        if (allRowsChanged) {
                            List positions = MapsforgeMapView.this.positionsModel.getRoute().getPositions();
                            if (positions.size() > 0) {
                                BoundingBox boundingBox = new BoundingBox(positions);
                                setCenter(boundingBox.getCenter());
                                zoomToBounds(boundingBox);
                            }
                        }

                        break;
                    case DELETE:
                        trackUpdater.handleRemove(e.getFirstRow(), e.getLastRow());
                        break;
                }
            }
        });
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
        mapView.getModel().mapViewPosition.setCenter(center);
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
        Dimension mapViewDimension = mapView.getModel().mapViewDimension.getDimension();
        if(mapViewDimension == null)
            return;
        double dxMax = longitudeToPixelX(boundingBox.maxLongitude, (byte) 0) / TILE_SIZE;
        double dxMin = longitudeToPixelX(boundingBox.minLongitude, (byte) 0) / TILE_SIZE;
        double zoomX = floor(-log(3.8) * log(abs(dxMax-dxMin)) + mapViewDimension.width / TILE_SIZE);
        double dyMax = latitudeToPixelY(boundingBox.maxLatitude, (byte) 0) / TILE_SIZE;
        double dyMin = latitudeToPixelY(boundingBox.minLatitude, (byte) 0) / TILE_SIZE;
        double zoomY = floor(-log(3.8) * log(abs(dyMax-dyMin)) + mapViewDimension.height / TILE_SIZE);
        int newZoom = new Double(min(zoomX, zoomY)).intValue();
        setZoom(newZoom);
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
        if(selectionUpdater == null)
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
