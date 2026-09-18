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
import org.mapsforge.map.layer.GroupLayer;
import org.mapsforge.map.layer.Layer;
import org.mapsforge.map.layer.LayerManager;
import org.mapsforge.map.layer.Layers;
import org.mapsforge.map.model.MapViewPosition;
import org.mapsforge.map.util.MapViewProjection;
import slash.navigation.mapview.mapsforge.AwtGraphicMapView;
import slash.navigation.mapview.mapsforge.overlays.DraggableMarker;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.util.List;

import static java.awt.event.InputEvent.CTRL_DOWN_MASK;
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
    private GrabbedMarker grabbedMarker;
    private boolean dragging;

    public MapViewMoverAndZoomer(AwtGraphicMapView mapView, LayerManager layerManager) {
        this(mapView, layerManager, new MapViewProjection(mapView));
    }

    MapViewMoverAndZoomer(AwtGraphicMapView mapView, LayerManager layerManager, MapViewProjection projection) {
        this.mapView = mapView;
        this.projection = projection;
        this.layerManager = layerManager;
        mapView.addMouseListener(this);
        mapView.addMouseMotionListener(this);
        mapView.addMouseWheelListener(this);
    }

    public void mouseClicked(MouseEvent e) {
        // enables Select and New from context menu
        lastMousePressPoint = e.getPoint();
    }

    public void mousePressed(MouseEvent e) {
        grabbedMarker = getMarkerFor(e);
        dragging = false;
        if (grabbedMarker == null)
            lastMousePressPoint = e.getPoint();
    }

    public void mouseDragged(MouseEvent e) {
        if (isLeftMouseButton(e)) {
            if (isMousePressedOnMarker()) {
                dragging = true;
                grabbedMarker.marker().setLatLong(draggedTo(e));
                requestRedraw();

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
        if (isMousePressedOnMarker() && dragging)
            grabbedMarker.marker().onDrop(draggedTo(e));
        // clear the pressed-on-marker state on every release: a plain click on a marker (no drag)
        // must not leave isMousePressedOnMarker() stuck true, which would suppress selecting that
        // position and let a subsequent "new position" fall back to the map center (off the route)
        grabbedMarker = null;
        dragging = false;
    }

    /**
     * The position a drag puts the marker at: the pin keeps the offset it was grabbed with, so its
     * tip -- the anchor, ~25px below the middle of the icon -- decides where the position lands,
     * not the mouse pointer. That is how brouter-web, graphhopper and kurviger behave and what
     * RouteConverter did up to 3.6, so grabbing the pin by its head stays predictable.
     */
    private LatLong draggedTo(MouseEvent e) {
        return projection.fromPixels(e.getX() + grabbedMarker.offsetX(), e.getY() + grabbedMarker.offsetY());
    }

    /**
     * Repaints the map after a marker moved. {@link Layer#requestRedraw()} cannot do it: it is a
     * no-op until a {@link org.mapsforge.map.layer.Redrawer} is assigned, and only {@link Layers#add}
     * assigns one. Selection markers are children of the selectionLayer {@link GroupLayer} instead
     * (#357), so they never get a redrawer and the dragged pin stayed frozen at its old position
     * until the drop. Ask the {@link LayerManager}, which is the redrawer the group layer uses.
     */
    private void requestRedraw() {
        layerManager.redrawLayers();
    }

    public void mouseWheelMoved(MouseWheelEvent e) {
        lastMousePressPoint = e.getPoint();
        zoomToMousePosition((byte) -e.getWheelRotation());
    }

    /**
     * Recursively searches for a DraggableMarker at the given tap point.
     * Handles both top-level markers and markers nested in GroupLayers (e.g., selectionLayer).
     * Returns the topmost marker under the tap (last in list wins, matching original scan order).
     *
     * @param layers       the list of layers to search (top-level or nested)
     * @param projection   the map projection for coordinate conversion
     * @param tapLatLong   the tap point in geographic coordinates
     * @param tapXY        the tap point in pixel coordinates
     * @return the DraggableMarker at the tap point, or null if none found
     */
    static DraggableMarker findDraggableMarkerAt(List<Layer> layers, MapViewProjection projection,
                                                 LatLong tapLatLong, org.mapsforge.core.model.Point tapXY) {
        for (int i = layers.size() - 1; i >= 0; --i) {
            Layer layer = layers.get(i);
            if (layer instanceof GroupLayer groupLayer) {
                // Recurse into GroupLayer children to find markers inside
                DraggableMarker found = findDraggableMarkerAt(groupLayer.layers, projection, tapLatLong, tapXY);
                if (found != null)
                    return found;
                continue;
            }
            if (!(layer instanceof DraggableMarker draggableMarker))
                continue;

            org.mapsforge.core.model.Point layerXY = projection.toPixels(layer.getPosition());
            if (draggableMarker.onTap(tapLatLong, layerXY, tapXY))
                return draggableMarker;
        }
        return null;
    }

    private GrabbedMarker getMarkerFor(MouseEvent e) {
        if((e.getModifiersEx() & CTRL_DOWN_MASK) != CTRL_DOWN_MASK) {
            LatLong tapLatLong = projection.fromPixels(e.getX(), e.getY());
            org.mapsforge.core.model.Point tapXY = new org.mapsforge.core.model.Point(e.getX(), e.getY());

            DraggableMarker marker = findDraggableMarkerAt(layerManager.getLayers().getLayers(), projection, tapLatLong, tapXY);
            if (marker != null) {
                org.mapsforge.core.model.Point layerXY = projection.toPixels(marker.getPosition());
                return new GrabbedMarker(marker, layerXY.x - tapXY.x, layerXY.y - tapXY.y);
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
        MapViewPosition mapViewPosition = mapView.getModel().mapViewPosition;
        if (mapViewPosition.getZoomLevel() + zoomLevelDiff <= mapViewPosition.getZoomLevelMax() &&
                mapViewPosition.getZoomLevel() + zoomLevelDiff >= mapViewPosition.getZoomLevelMin()) {
            Dimension dimension = mapView.getDimension();
            int horizontalDiff = (int) ((dimension.width / 2.0 - point.x) * (zoomLevelDiff > 0 ? 0.5 : -1.0));
            int verticalDiff = (int) ((dimension.height / 2.0 - point.y) * (zoomLevelDiff > 0 ? 0.5 : -1.0));
            mapViewPosition.moveCenterAndZoom(horizontalDiff, verticalDiff, zoomLevelDiff);
        }
    }

    public Point getLastMousePoint() {
        return lastMousePressPoint;
    }

    public boolean isMousePressedOnMarker() {
        return grabbedMarker != null;
    }

    /**
     * A marker under the mouse together with the offset from the grab point to its anchor, so the
     * pin can be dragged without jumping under the pointer.
     */
    private record GrabbedMarker(DraggableMarker marker, double offsetX, double offsetY) {
    }
}
