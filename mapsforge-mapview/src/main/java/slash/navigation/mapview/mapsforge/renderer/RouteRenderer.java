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
package slash.navigation.mapview.mapsforge.renderer;

import org.mapsforge.core.graphics.GraphicFactory;
import org.mapsforge.core.graphics.Paint;
import org.mapsforge.core.model.LatLong;
import slash.navigation.common.LongitudeAndLatitude;
import slash.navigation.common.NavigationPosition;
import slash.navigation.converter.gui.models.ColorModel;
import slash.navigation.mapview.mapsforge.MapViewCallbackOffline;
import slash.navigation.mapview.mapsforge.MapsforgeMapView;
import slash.navigation.mapview.mapsforge.lines.Line;
import slash.navigation.mapview.mapsforge.lines.Polyline;
import slash.navigation.mapview.mapsforge.models.IntermediateRoute;
import slash.navigation.mapview.mapsforge.updater.PairWithLayer;
import slash.navigation.routing.DownloadFuture;
import slash.navigation.routing.RoutingResult;
import slash.navigation.routing.RoutingService;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

import static java.lang.System.currentTimeMillis;
import static java.lang.Thread.sleep;
import static slash.common.helpers.ThreadHelper.safeJoin;
import static slash.navigation.maps.helpers.MapTransfer.asLatLong;
import static slash.navigation.mapview.MapViewConstants.ROUTE_LINE_WIDTH_PREFERENCE;
import static slash.navigation.mapview.mapsforge.helpers.ColorHelper.asRGBA;

/**
 * Renders a {@link List} of {@link PairWithLayer} for the {@link MapsforgeMapView}.
 *
 * @author Christian Pesch
 */

public class RouteRenderer {
    private static final Logger log = Logger.getLogger(RouteRenderer.class.getName());
    private static final Preferences preferences = Preferences.userNodeForPackage(MapsforgeMapView.class);
    private Paint ROUTE_NOT_VALID_PAINT, ROUTE_DOWNLOADING_PAINT;

    private Thread renderThread = null;
    private final Object notificationMutex = new Object();
    private boolean drawingRoute = false;

    private MapsforgeMapView mapView;
    private MapViewCallbackOffline mapViewCallback;
    private ColorModel routeColorModel;
    private GraphicFactory graphicFactory;

    public RouteRenderer(MapsforgeMapView mapView, MapViewCallbackOffline mapViewCallback, ColorModel routeColorModel,
                         GraphicFactory graphicFactory) {
        this.mapView = mapView;
        this.mapViewCallback = mapViewCallback;
        this.routeColorModel = routeColorModel;
        this.graphicFactory = graphicFactory;
        initialize();
    }

    private void initialize() {
        ROUTE_NOT_VALID_PAINT = graphicFactory.createPaint();
        ROUTE_NOT_VALID_PAINT.setColor(0xFFFF0000);
        ROUTE_NOT_VALID_PAINT.setStrokeWidth(5);
        ROUTE_DOWNLOADING_PAINT = graphicFactory.createPaint();
        ROUTE_DOWNLOADING_PAINT.setColor(0x993379FF);
        ROUTE_DOWNLOADING_PAINT.setStrokeWidth(5);
        ROUTE_DOWNLOADING_PAINT.setDashPathEffect(new float[]{3, 12});
    }

    public void dispose() {
        long start = currentTimeMillis();
        synchronized (notificationMutex) {
            this.drawingRoute = false;
        }

        if (renderThread != null) {
            try {
                safeJoin(renderThread);
            } catch (InterruptedException e) {
                // intentionally left empty
            }
            long end = currentTimeMillis();
            log.info("RouteRenderer stopped after " + (end - start) + " ms");
        }
    }

    public void renderRoute(final List<PairWithLayer> pairWithLayers) {
        dispose();

        renderThread = new Thread(new Runnable() {
            public void run() {
                synchronized (notificationMutex) {
                    RouteRenderer.this.drawingRoute = true;
                }

                try {
                    internalRenderRoute(pairWithLayers);
                } catch (Exception e) {
                    mapViewCallback.showRoutingException(e);
                }
            }
        }, "RouteRenderer");
        renderThread.start();
    }

    private void checkForInterruption() {
        synchronized (notificationMutex) {
            if(!drawingRoute)
                renderThread.interrupt();
        }
    }

    private void internalRenderRoute(List<PairWithLayer> pairWithLayers) {
        drawBeeline(pairWithLayers);
        mapView.fireDistanceAndTime(pairWithLayers);
        checkForInterruption();

        RoutingService service = mapViewCallback.getRoutingService();
        waitForInitialization(service);
        waitForDownload(service, pairWithLayers);
        checkForInterruption();

        drawRoute(pairWithLayers);
        mapView.fireDistanceAndTime(pairWithLayers);
        checkForInterruption();
    }

    private void waitForInitialization(RoutingService service) {
        if (!service.isInitialized()) {
            while (!service.isInitialized()) {
                try {
                    sleep(100);
                } catch (InterruptedException e) {
                    // intentionally left empty
                }
            }
        }
    }

    private LongitudeAndLatitude asLongitudeAndLatitude(NavigationPosition position) {
        return new LongitudeAndLatitude(position.getLongitude(), position.getLatitude());
    }

    private List<LongitudeAndLatitude> asLongitudeAndLatitude(List<PairWithLayer> pairWithLayers) {
        List<LongitudeAndLatitude> result = new ArrayList<>();
        for (PairWithLayer pairWithLayer : pairWithLayers) {
            if(!pairWithLayer.hasCoordinates())
                continue;

            result.add(asLongitudeAndLatitude(pairWithLayer.getFirst()));
            result.add(asLongitudeAndLatitude(pairWithLayer.getSecond()));
        }
        return result;
    }

    private void waitForDownload(RoutingService service, List<PairWithLayer> pairWithLayers) {
        if (service.isDownload()) {
            DownloadFuture future = service.downloadRoutingDataFor(asLongitudeAndLatitude(pairWithLayers));
            if (future.isRequiresDownload()) {
                mapViewCallback.showDownloadNotification();
                future.download();
            }
            if (future.isRequiresProcessing()) {
                mapViewCallback.showProcessNotification();
                future.process();
            }
        }
    }

    private void drawBeeline(List<PairWithLayer> pairsWithLayer) {
        for (PairWithLayer pairWithLayer : pairsWithLayer) {
            if (!pairWithLayer.hasCoordinates())
                continue;

            Line line = new Line(asLatLong(pairWithLayer.getFirst()), asLatLong(pairWithLayer.getSecond()), ROUTE_DOWNLOADING_PAINT, mapView.getTileSize());
            pairWithLayer.setLayer(line);
            mapView.addLayer(line);

            pairWithLayer.setDistance(pairWithLayer.getFirst().calculateDistance(pairWithLayer.getSecond()));
            pairWithLayer.setTime(pairWithLayer.getFirst().calculateTime(pairWithLayer.getSecond()));
        }
    }

    private void drawRoute(List<PairWithLayer> pairWithLayers) {
        Paint paint = graphicFactory.createPaint();
        paint.setColor(asRGBA(routeColorModel));
        paint.setStrokeWidth(preferences.getInt(ROUTE_LINE_WIDTH_PREFERENCE, 5));
        RoutingService routingService = mapViewCallback.getRoutingService();
        for (PairWithLayer pairWithLayer : pairWithLayers) {
            if (!pairWithLayer.hasCoordinates())
                continue;

            IntermediateRoute intermediateRoute = calculateRoute(routingService, pairWithLayer);
            Polyline polyline = new Polyline(intermediateRoute.getLatLongs(), intermediateRoute.isValid() ? paint : ROUTE_NOT_VALID_PAINT, mapView.getTileSize());
            // remove beeline layer then add polyline layer from routing
            mapView.removeLayer(pairWithLayer);
            pairWithLayer.setLayer(polyline);
            mapView.addLayer(polyline);
        }
    }

    private IntermediateRoute calculateRoute(RoutingService routingService, PairWithLayer pairWithLayer) {
        List<LatLong> latLongs = new ArrayList<>();
        latLongs.add(asLatLong(pairWithLayer.getFirst()));
        RoutingResult result = routingService.getRouteBetween(pairWithLayer.getFirst(), pairWithLayer.getSecond(), mapViewCallback.getTravelMode());
        if (result.isValid())
            latLongs.addAll(asLatLong(result.getPositions()));
        pairWithLayer.setDistance(result.getDistance());
        pairWithLayer.setTime(result.getTime());
        latLongs.add(asLatLong(pairWithLayer.getSecond()));
        return new IntermediateRoute(latLongs, result.isValid());
    }
}
