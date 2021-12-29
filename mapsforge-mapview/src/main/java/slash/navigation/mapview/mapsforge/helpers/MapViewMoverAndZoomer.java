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
package slash.navigation.mapview.mapsforge.helpers;

import org.mapsforge.core.model.Dimension;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.map.layer.Layer;
import org.mapsforge.map.layer.LayerManager;
import org.mapsforge.map.layer.Layers;
import org.mapsforge.map.layer.overlay.Marker;
import org.mapsforge.map.model.IMapViewPosition;
import org.mapsforge.map.util.MapViewProjection;
import slash.navigation.mapview.mapsforge.AwtGraphicMapView;
import slash.navigation.mapview.mapsforge.overlays.DraggableMarker;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;

import static java.lang.Thread.sleep;
import static javax.swing.SwingUtilities.isLeftMouseButton;
import static slash.navigation.gui.helpers.UIHelper.*;

/**
 * Move and zoom map upon mouse events of the {@link AwtGraphicMapView}.
 *
 * @author Christian Pesch
 */

public class MapViewMoverAndZoomer extends MouseAdapter {
    private static final int STEPS_TO_MOVE_CENTER = 25;

    private final AwtGraphicMapView mapView;
    private final MapViewProjection projection;
    private final LayerManager layerManager;
    private Point lastMousePressPoint;
    private MarkerAndDelta markerAndDelta;

    public MapViewMoverAndZoomer(AwtGraphicMapView mapView, LayerManager layerManager) {
        this.mapView = mapView;
        this.projection = new MapViewProjection(mapView);
        this.layerManager = layerManager;
        mapView.addMouseListener(this);
        mapView.addMouseMotionListener(this);
        mapView.addMouseWheelListener(this);
    }

    public void mousePressed(MouseEvent e) {
        markerAndDelta = getMarkerFor(e);
        if (markerAndDelta == null)
            lastMousePressPoint = e.getPoint();
    }

    public void mouseDragged(MouseEvent e) {
        if (isLeftMouseButton(e)) {
            if (isMousePressedOnMarker()) {
                startDragCursor(mapView);
                LatLong latLong = projection.fromPixels(e.getX() + markerAndDelta.getDeltaX(), e.getY() + markerAndDelta.getDeltaY());
                Marker marker = markerAndDelta.getMarker();
                marker.setLatLong(latLong);
                marker.requestRedraw();

            } else if (getLastMousePoint() != null) {
                Point point = e.getPoint();
                int moveHorizontal = point.x - lastMousePressPoint.x;
                int moveVertical = point.y - lastMousePressPoint.y;
                mapView.getModel().mapViewPosition.moveCenter(moveHorizontal, moveVertical);
                lastMousePressPoint = point;
            }
        }
    }

    public void mouseReleased(MouseEvent e) {
        if (isMousePressedOnMarker() && isDragCursor(mapView)) {
            LatLong latLong = projection.fromPixels(e.getX() + markerAndDelta.getDeltaX(), e.getY() + markerAndDelta.getDeltaY());
            DraggableMarker marker = markerAndDelta.getMarker();
            marker.onDrop(latLong);
            markerAndDelta = null;
            stopWaitCursor(mapView);
        }
    }

    public void mouseWheelMoved(MouseWheelEvent e) {
        lastMousePressPoint = e.getPoint();
        zoomToMousePosition((byte) -e.getWheelRotation());
    }

    private MarkerAndDelta getMarkerFor(MouseEvent e) {
        LatLong tapLatLong = projection.fromPixels(e.getX(), e.getY());
        org.mapsforge.core.model.Point tapXY = new org.mapsforge.core.model.Point(e.getX(), e.getY());

        Layers layers = layerManager.getLayers();
        for (int i = layers.size() - 1; i >= 0; --i) {
            Layer layer = layers.get(i);
            if (!(layer instanceof Marker))
                continue;

            org.mapsforge.core.model.Point layerXY = projection.toPixels(layer.getPosition());
            if (layer.onTap(tapLatLong, layerXY, tapXY)) {
                return new MarkerAndDelta((DraggableMarker) layer, layerXY.x - tapXY.x, layerXY.y - tapXY.y);
            }
        }
        return null;
    }


    public void centerToMousePosition() {
        Dimension dimension = mapView.getDimension();
        int horizontalDiff = dimension.width / 2 - lastMousePressPoint.x;
        int verticalDiff = dimension.height / 2 - lastMousePressPoint.y;
        animateCenter(horizontalDiff, verticalDiff);
    }

    public void animateCenter(final int horizontalDiff, final int verticalDiff) {
        new Thread(() -> {
            double stepSizeX = horizontalDiff / (double) STEPS_TO_MOVE_CENTER;
            double stepSizeY = verticalDiff / (double) STEPS_TO_MOVE_CENTER;
            for (int i = 0; i < STEPS_TO_MOVE_CENTER; i++) {
                mapView.getModel().mapViewPosition.moveCenter(stepSizeX, stepSizeY);
                try {
                    sleep(10);
                } catch (InterruptedException e) {
                    // intentionally left empty
                }
            }
        }).start();
    }

    public void zoomToMousePosition(byte zoomLevelDiff) {
        if (zoomLevelDiff == 0 || lastMousePressPoint == null)
            return;
        LatLong latLong = projection.fromPixels(lastMousePressPoint.x, lastMousePressPoint.y);
        mapView.getModel().mapViewPosition.setPivot(latLong);
        zoomToPoint(zoomLevelDiff, new org.mapsforge.core.model.Point(lastMousePressPoint.x, lastMousePressPoint.y));
    }

    public void zoomToPosition(byte zoomLevelDiff, LatLong latLong) {
        mapView.getModel().mapViewPosition.setPivot(latLong);
        zoomToPoint(zoomLevelDiff, projection.toPixels(latLong));
    }

    private void zoomToPoint(byte zoomLevelDiff, org.mapsforge.core.model.Point point) {
        IMapViewPosition mapViewPosition = mapView.getModel().mapViewPosition;
        if (mapViewPosition.getZoomLevel() + zoomLevelDiff <= mapViewPosition.getZoomLevelMax() &&
                mapViewPosition.getZoomLevel() + zoomLevelDiff >= mapViewPosition.getZoomLevelMin()) {
            Dimension dimension = mapView.getDimension();
            int horizontalDiff = (int) ((dimension.width / 2 - point.x) * (zoomLevelDiff > 0 ? 0.5 : -1.0));
            int verticalDiff = (int) ((dimension.height / 2 - point.y) * (zoomLevelDiff > 0 ? 0.5 : -1.0));
            mapViewPosition.moveCenterAndZoom(horizontalDiff, verticalDiff, zoomLevelDiff);
        }
    }

    public Point getLastMousePoint() {
        return lastMousePressPoint;
    }

    public boolean isMousePressedOnMarker() {
        return markerAndDelta != null;
    }

    private static class MarkerAndDelta {
        private DraggableMarker marker;
        private double deltaX;
        private double deltaY;

        public MarkerAndDelta(DraggableMarker marker, double deltaX, double deltaY) {
            this.marker = marker;
            this.deltaX = deltaX;
            this.deltaY = deltaY;
        }

        public DraggableMarker getMarker() {
            return marker;
        }

        public double getDeltaX() {
            return deltaX;
        }

        public double getDeltaY() {
            return deltaY;
        }
    }
}
