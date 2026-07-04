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
package slash.navigation.converter.gui.models;

import slash.navigation.common.DistanceAndTime;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * An in-memory, session-scoped cache of {@link DistanceAndTime}s keyed by route URL.
 * Survives category navigation, cleared on restart.
 *
 * @author Christian Pesch
 */

public class RouteDistanceAndTimeCache implements RouteMetadataSource {
    private final Map<String, DistanceAndTime> urlToDistanceAndTime = new ConcurrentHashMap<>();

    public void put(String url, DistanceAndTime distanceAndTime) {
        urlToDistanceAndTime.put(url, distanceAndTime);
    }

    public DistanceAndTime getDistanceAndTime(String url) {
        return url != null ? urlToDistanceAndTime.get(url) : null;
    }
}
