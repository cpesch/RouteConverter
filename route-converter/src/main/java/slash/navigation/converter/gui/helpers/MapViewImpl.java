/*
 *
 *     This file is part of RouteConverter.
 *
 *     RouteConverter is free software; you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation; either version 2 of the License, or
 *     (at your option) any later version.
 *
 *     RouteConverter is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with RouteConverter; if not, write to the Free Software
 *     Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 *
 *     Copyright (C) 2007 Christian Pesch. All Rights Reserved.
 * /
 */

package slash.navigation.converter.gui.helpers;

/**
 * Enumeration of supported map views.
 *
 * @author Christian Pesch
 */

public enum MapViewImpl {
    JavaFX("slash.navigation.converter.gui.mapview.JavaFXWebViewMapView", false),
    EclipseSWT("slash.navigation.converter.gui.mapview.EclipseSWTMapView", false),
    Mapsforge("slash.navigation.converter.gui.mapview.MapsforgeMapView", true);

    private String className;
    private boolean download;

    MapViewImpl(String className, boolean download) {
        this.className = className;
        this.download = download;
    }

    public String getClassName() {
        return className;
    }

    public boolean isDownload() {
        return download;
    }
}
