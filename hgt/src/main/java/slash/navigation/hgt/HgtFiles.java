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

package slash.navigation.hgt;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Encapsulates access to HGT files.
 *
 * @author Robert "robekas", Christian Pesch
 */

public class HgtFiles {
    private Map<Integer, ElevationTile> tileCache = new HashMap<Integer, ElevationTile>();

    public Integer getElevationFor(double longitude, double latitude) throws IOException {
        int longitudeAsInteger = (int) longitude;   // values from -180 to +180: 0 - 360
        int latitudeAsInteger = (int) latitude;     // values from  -90 to  +90: 0 - 180
        Integer key = latitudeAsInteger * 100000 + longitudeAsInteger;

        ElevationTile tile = tileCache.get(key);
        if (tile == null) {
            tile = new ElevationTile(longitude, latitude);
            tileCache.put(key, tile);
        }
        return tile.getElevationFor(longitude, latitude);
    }
}
