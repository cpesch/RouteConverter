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

package slash.navigation.rtz;

import slash.common.type.CompactCalendar;
import slash.navigation.base.SimpleRoute;
import slash.navigation.base.Wgs84Position;

import java.util.List;

import static slash.navigation.base.RouteCharacteristics.Route;

/**
 * An RTZ Route Exchange (.rtz) route.
 *
 * @author Christian Pesch
 */

public class RtzRoute extends SimpleRoute<Wgs84Position, RtzFormat> {
    public RtzRoute(String name, List<Wgs84Position> positions) {
        super(new RtzFormat(), Route, name, positions);
    }

    public Wgs84Position createPosition(Double longitude, Double latitude, Double elevation, Double speed, CompactCalendar time, String description) {
        return new Wgs84Position(longitude, latitude, elevation, speed, time, description);
    }
}
