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
package slash.navigation.converter.gui;

import slash.navigation.converter.gui.helpers.MapViewCallbackImpl;
import slash.navigation.converter.gui.models.GoogleMapsServerModel;
import slash.navigation.gui.Application;
import slash.navigation.mapview.browser.MapViewCallbackGoogle;

/**
 * Implements the callbacks from the MapsforgeMapView to the RouteConverterOpenSource services.
 *
 * @author Christian Pesch
 */

public class MapViewCallbackGoogleImpl extends MapViewCallbackImpl implements MapViewCallbackGoogle {
    public GoogleMapsServerModel getGoogleMapsServerModel() {
        return ((RouteConverterGoogle) Application.getInstance()).getGoogleMapsServerModel();
    }
}
