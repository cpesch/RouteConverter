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

package slash.navigation.base;

/**
 * Enumeration of the characteristics of a {@link BaseRoute}.
 *
 * Route: a list of points (often with names) connected in a specific order.
 * Usually a collection of waypoints defining the route you want to pass while
 * traveling, created by PC software, or generated inside a GPS device. They
 * can be composed of existing waypoints, or new "routepoints" might be generated.
 *
 * Track: a collection of locations recorded by your GPS device while
 * traveling -- "breadcrumb trails". The order of trackpoints within the track
 * is important. Usually a trackpoint doesn't have a name or description, but usually
 * has a timestamp. This distinguishes a trackpoint from a waypoint.
 *
 * Waypoints: are geopoints that are not necessarily connected to other points,
 * and their order is unimportant. They can be entered before, while or after
 * you actually visit the place and might have tags like name, description and the
 * like. Usually used to mark special locations as your home, a hotel or a geocache.
 *
 * @author Christian Pesch
 */

public enum RouteCharacteristics {
    Route, Track, Waypoints
}
