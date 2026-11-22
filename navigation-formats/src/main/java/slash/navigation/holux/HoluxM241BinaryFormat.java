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

    Copyright (C) 2007 Per Borgentun, e4borgen(at)yahoo.com
    With lot of inspiration from wbt-200.c
       Copyright (C) 2006 Andy Armstrong
    Translated to Java from GPSBabel mtk_logger.cc (master 9c1e336) for RouteConverter, 2026; modified.
*/

package slash.navigation.holux;

import slash.navigation.gpx.GpxPosition;
import slash.navigation.gpx.GpxRoute;

import java.util.List;

import static slash.common.io.Transfer.isEmpty;

/**
 * Reads Holux M-241 Binary (.bin) files.
 * <p>
 * GPSBabel's {@code m241-bin} format: 4 byte float coordinates, a 3 byte float height and no '*' before
 * the checksum. Holux GPSport 245 (and M-1200) dumps are recognized by their marker string and read with
 * speed and height swapped, as GPSBabel does.
 *
 * @author Christian Pesch
 */

public class HoluxM241BinaryFormat extends MtkBinaryFormat {

    public String getName() {
        return "Holux M-241 Binary (*" + getExtension() + ")";
    }

    boolean isHolux() {
        return true;
    }

    protected boolean isValidRoute(GpxRoute route) {
        // is reading GarminPoiDb files
        List<GpxPosition> positions = route.getPositions();
        int count = 0;
        for (GpxPosition position : positions) {
            if ((position.getSatellites() == null || position.getSatellites() < 12) &&
                    (position.getVdop() == null || position.getVdop() < 20.0) &&
                    !isEmpty(position.getLongitude()) &&
                    !isEmpty(position.getLatitude()))
                count++;
        }
        return count == positions.size();
    }
}
