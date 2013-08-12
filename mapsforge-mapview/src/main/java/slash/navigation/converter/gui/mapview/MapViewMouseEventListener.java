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

import org.mapsforge.map.model.MapViewPosition;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;

import static javax.swing.SwingUtilities.isLeftMouseButton;

/**
 * Listen to mouse events of the {@link MapsforgeMapView}'s {@link MapViewPosition}
 *
 * @author Christian Pesch, inspired by org.mapsforge.map.swing.view
 */

public class MapViewMouseEventListener extends MouseAdapter {
    private final MapViewPosition mapViewPosition;
    private Point lastDragPoint;

    public MapViewMouseEventListener(MapViewPosition mapViewPosition) {
        this.mapViewPosition = mapViewPosition;
    }

    public void mouseDragged(MouseEvent mouseEvent) {
        if (isLeftMouseButton(mouseEvent)) {
            Point point = mouseEvent.getPoint();
            if (lastDragPoint != null) {
                int moveHorizontal = point.x - lastDragPoint.x;
                int moveVertical = point.y - lastDragPoint.y;
                mapViewPosition.moveCenter(moveHorizontal, moveVertical);
            }
            lastDragPoint = point;
        }
    }

    public void mousePressed(MouseEvent mouseEvent) {
        if (isLeftMouseButton(mouseEvent)) {
            lastDragPoint = mouseEvent.getPoint();
        }
    }

    public void mouseReleased(MouseEvent mouseEvent) {
        lastDragPoint = null;
    }

    public void mouseWheelMoved(MouseWheelEvent mouseWheelEvent) {
        byte zoomLevelDiff = (byte) -mouseWheelEvent.getWheelRotation();
        mapViewPosition.zoom(zoomLevelDiff);
    }
}
