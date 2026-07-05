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

import org.mapsforge.core.graphics.Bitmap;
import org.mapsforge.core.graphics.GraphicFactory;
import org.mapsforge.core.graphics.Paint;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.map.layer.GroupLayer;
import org.mapsforge.map.layer.overlay.Marker;
import org.mapsforge.map.model.DisplayModel;
import slash.navigation.base.BaseRoute;
import slash.navigation.common.NavigationPosition;
import slash.navigation.converter.gui.models.PositionListsModel;
import slash.navigation.gui.models.IntegerModel;
import slash.navigation.mapview.mapsforge.MapsforgeMapView;
import slash.navigation.mapview.mapsforge.lines.Polyline;

import java.util.ArrayList;
import java.util.List;

import static slash.common.helpers.ThreadHelper.invokeInAwtEventQueue;
import static slash.navigation.base.RouteCharacteristics.Route;
import static slash.navigation.base.RouteCharacteristics.Waypoints;

/**
 * Renders every position list of a file that is not the selected one as a read-only,
 * gray, non-interactive overlay for the {@link MapsforgeMapView}. The layers live on a
 * dedicated {@link GroupLayer} the map view keeps below the selected list and its
 * selection markers. Route-type lists are drawn from their stored positions - the
 * routing engine and distance/time computation stay reserved for the selected list.
 *
 * @author Christian Pesch
 */

public class NonSelectedPositionListsRenderer {
    private static final String NON_SELECTED_COLOR = "808080";
    private static final String NON_SELECTED_OPACITY = "0.5";
    private static final int NON_SELECTED_RGBA = 0x80808080;

    private final MapsforgeMapView mapView;
    private final PositionListsModel positionListsModel;
    private final IntegerModel routeLineWidthModel;
    private final IntegerModel trackLineWidthModel;
    private final GraphicFactory graphicFactory;
    private final GroupLayer layer = new GroupLayer();
    private Bitmap waypointIcon;

    public NonSelectedPositionListsRenderer(MapsforgeMapView mapView, PositionListsModel positionListsModel,
                                            IntegerModel routeLineWidthModel, IntegerModel trackLineWidthModel,
                                            GraphicFactory graphicFactory) {
        this.mapView = mapView;
        this.positionListsModel = positionListsModel;
        this.routeLineWidthModel = routeLineWidthModel;
        this.trackLineWidthModel = trackLineWidthModel;
        this.graphicFactory = graphicFactory;
    }

    public GroupLayer getLayer() {
        return layer;
    }

    public void update() {
        invokeInAwtEventQueue(new Runnable() {
            public void run() {
                // GroupLayer.draw()/onDestroy() iterate the plain ArrayList layers under
                // synchronized(this) on the render thread; hold the same monitor while we
                // clear and repopulate here on the EDT to avoid a ConcurrentModificationException
                synchronized (layer) {
                    layer.layers.clear();

                    List<BaseRoute> routes = positionListsModel.getRoutes();
                    BaseRoute selectedRoute = positionListsModel.getSelectedRoute();
                    if (routes != null) {
                        for (BaseRoute route : routes) {
                            if (route == null || route == selectedRoute)
                                continue;
                            addPositionList(route);
                        }
                    }
                }

                layer.requestRedraw();
            }
        });
    }

    @SuppressWarnings("unchecked")
    private void addPositionList(BaseRoute route) {
        List<NavigationPosition> positions = route.getPositions();
        DisplayModel displayModel = mapView.getMapView().getModel().displayModel;

        if (route.getCharacteristics().equals(Waypoints)) {
            Bitmap icon = getWaypointIcon();
            for (NavigationPosition position : positions) {
                if (position == null || !position.hasCoordinates())
                    continue;

                Marker marker = new Marker(mapView.asLatLong(position), icon, 0, 0);
                marker.setDisplayModel(displayModel);
                layer.layers.add(marker);
            }

        } else {
            List<LatLong> latLongs = new ArrayList<>();
            for (NavigationPosition position : positions) {
                if (position != null && position.hasCoordinates())
                    latLongs.add(mapView.asLatLong(position));
            }
            if (latLongs.size() < 2)
                return;

            Paint paint = graphicFactory.createPaint();
            paint.setColor(NON_SELECTED_RGBA);
            paint.setStrokeWidth((route.getCharacteristics().equals(Route) ?
                    routeLineWidthModel : trackLineWidthModel).getInteger());
            Polyline polyline = new Polyline(latLongs, paint, mapView.getTileSize());
            polyline.setDisplayModel(displayModel);
            layer.layers.add(polyline);
        }
    }

    private synchronized Bitmap getWaypointIcon() {
        if (waypointIcon == null)
            waypointIcon = mapView.createWaypointIcon(NON_SELECTED_COLOR, NON_SELECTED_OPACITY);
        return waypointIcon;
    }
}
