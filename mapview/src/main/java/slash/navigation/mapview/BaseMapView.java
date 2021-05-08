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

package slash.navigation.mapview;

/**
 * The base of all {@link MapView} implementations.
 *
 * @author Christian Pesch
 */

public abstract class BaseMapView implements MapView {
    protected static final String CENTER_LATITUDE_PREFERENCE = "centerLatitude";
    protected static final String CENTER_LONGITUDE_PREFERENCE = "centerLongitude";
    protected static final String CENTER_ZOOM_PREFERENCE = "centerZoom";

    protected static final String COMPLEMENT_DATA_PREFERENCE = "complementData";
    protected static final String CLEAN_ELEVATION_ON_MOVE_PREFERENCE = "cleanElevationOnMove";
    protected static final String COMPLEMENT_ELEVATION_ON_MOVE_PREFERENCE = "complementElevationOnMove";
    protected static final String CLEAN_TIME_ON_MOVE_PREFERENCE = "cleanTimeOnMove";
    protected static final String COMPLEMENT_TIME_ON_MOVE_PREFERENCE = "complementTimeOnMove";
    protected static final String MOVE_COMPLETE_SELECTION_PREFERENCE = "moveCompleteSelection";
}
