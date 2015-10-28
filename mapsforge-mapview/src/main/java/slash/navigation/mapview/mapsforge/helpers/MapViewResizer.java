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

import org.mapsforge.map.model.MapViewDimension;
import slash.navigation.mapview.mapsforge.AwtGraphicMapView;

import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

/**
 * Resize map upon component resize events of the {@link AwtGraphicMapView}..
 *
 * @author Christian Pesch
 */

public class MapViewResizer extends ComponentAdapter {
    private final AwtGraphicMapView mapView;
    private final MapViewDimension mapViewDimension;

    public MapViewResizer(AwtGraphicMapView mapView, MapViewDimension mapViewDimension) {
        this.mapView = mapView;
        this.mapViewDimension = mapViewDimension;
        mapView.addComponentListener(this);
    }

    public void componentResized(ComponentEvent componentEvent) {
        Dimension container = mapView.getSize();
        org.mapsforge.core.model.Dimension mapView = mapViewDimension.getDimension();
        if (mapView == null || container.height != mapView.height || container.width != mapView.width)
            mapViewDimension.setDimension(new org.mapsforge.core.model.Dimension(container.width, container.height));
    }
}
