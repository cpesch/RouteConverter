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
package slash.navigation.converter.gui.mapview.helpers;

import org.mapsforge.core.model.Dimension;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.map.model.MapViewPosition;
import org.mapsforge.map.util.MapViewProjection;
import slash.navigation.converter.gui.mapview.AwtGraphicMapView;
import slash.navigation.converter.gui.mapview.MapsforgeMapView;
import slash.navigation.gui.Application;
import slash.navigation.gui.actions.ActionManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;

import static java.lang.Thread.sleep;
import static javax.swing.SwingUtilities.isLeftMouseButton;
import static javax.swing.SwingUtilities.isRightMouseButton;

/**
 * Listen to mouse events of the {@link MapsforgeMapView}'s {@link MapViewPosition}
 *
 * @author Christian Pesch, inspired by org.mapsforge.map.swing.view
 */

public class MapViewMouseEventListener extends MouseAdapter {
    private final AwtGraphicMapView mapView;
    private final JPopupMenu popupMenu;
    private Point lastMousePressPoint;

    public MapViewMouseEventListener(AwtGraphicMapView mapView, JPopupMenu popupMenu) {
        this.mapView = mapView;
        this.popupMenu = popupMenu;
    }

    public void mouseDragged(MouseEvent e) {
        if (isLeftMouseButton(e)) {
            Point point = e.getPoint();
            if (lastMousePressPoint != null) {
                int moveHorizontal = point.x - lastMousePressPoint.x;
                int moveVertical = point.y - lastMousePressPoint.y;
                mapView.getModel().mapViewPosition.moveCenter(moveHorizontal, moveVertical);
            }
            lastMousePressPoint = point;
        }
    }

    public void mousePressed(MouseEvent e) {
        lastMousePressPoint = e.getPoint();

        if (isLeftMouseButton(e)) {
            boolean shiftKey = e.isShiftDown();
            boolean altKey = e.isAltDown();
            boolean ctrlKey = e.isControlDown();
            ActionManager actionManager = Application.getInstance().getContext().getActionManager();
            if (!shiftKey && !altKey && !ctrlKey)
                actionManager.run("select-position");
            else if (shiftKey && !altKey && !ctrlKey)
                actionManager.run("extend-selection");
            else if (!shiftKey && !altKey && ctrlKey)
                actionManager.run("add-position");
            else if (!shiftKey && altKey && ctrlKey)
                actionManager.run("delete-position");

        } else if (isRightMouseButton(e)) {
            popupMenu.show(mapView, e.getX(), e.getY());
        }
    }

    public void mouseReleased(MouseEvent e) {
        lastMousePressPoint = null;
    }

    public void mouseWheelMoved(MouseWheelEvent e) {
        zoomToMousePosition((byte) -e.getWheelRotation(), e.getX(), e.getY());
    }

    private static final int TOTAL_STEPS = 25;

    public void centerToMousePosition() {
        new Thread(new Runnable() {
            public void run() {
                Dimension dimension = mapView.getDimension();
                int horizontalDiff = dimension.width / 2 - lastMousePressPoint.x;
                int verticalDiff = dimension.height / 2 - lastMousePressPoint.y;

                double stepSizeX = horizontalDiff / TOTAL_STEPS;
                double stepSizeY = verticalDiff / TOTAL_STEPS;
                for (int i = 0; i < TOTAL_STEPS; i++) {
                    mapView.getModel().mapViewPosition.moveCenter(stepSizeX, stepSizeY);
                    try {
                        sleep(10);
                    } catch (InterruptedException e) {
                        // intentionally left empty
                    }
                }
            }
        }).start();
    }

    public void zoomToMousePosition(byte zoomLevelDiff) {
        zoomToMousePosition(zoomLevelDiff, lastMousePressPoint.x, lastMousePressPoint.y);
    }

    public void zoomToMousePosition(byte zoomLevelDiff, int mouseX, int mouseY) {
        LatLong mouse = new MapViewProjection(mapView).fromPixels(mouseX, mouseY);
        mapView.getModel().mapViewPosition.setPivot(mouse);

        Dimension dimension = mapView.getDimension();
        int horizontalDiff = (int) ((dimension.width / 2 - mouseX) * (zoomLevelDiff > 0 ? 0.5 : -1.0));
        int verticalDiff = (int) ((dimension.height / 2 - mouseY) * (zoomLevelDiff > 0 ? 0.5 : -1.0));
        mapView.getModel().mapViewPosition.moveCenterAndZoom(horizontalDiff, verticalDiff, zoomLevelDiff);
    }

    public Point getMousePosition() {
        return lastMousePressPoint != null ? lastMousePressPoint : null;
    }
}
