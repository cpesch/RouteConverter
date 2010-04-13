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

package slash.navigation.wbt;

import slash.navigation.BaseNavigationPosition;
import slash.navigation.Wgs84Route;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;

/**
 * Reads and writes Wintec WBT-202 (.tes) files.
 * <p/>
 * Format is basically {@link WintecWbt201Tk2Format) without a 16 byte header as described at:
 * http://www.geosetter.de/mantis/view.php?id=373
 *
 * @author Malte Neumann
 */

public class WintecWbt202TesFormat extends WintecWbt201Format {

    public String getName() {
        return "Wintec WBT-202 (*" + getExtension() + ")";
    }

    public String getExtension() {
        return ".tes";
    }

    protected boolean checkFormatDescriptor(ByteBuffer buffer) throws IOException {
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.position(0);

        /*short trackFlag*/ buffer.getShort();

        // read first position and validate the data
        int time = buffer.getInt();
        int latitude = buffer.getInt();
        int longitude = buffer.getInt();
        short altitude = buffer.getShort();
        BaseNavigationPosition firstPosition = createWaypoint(time, latitude, longitude, altitude, 1, false);
        return firstPosition.getLatitude() < 90 && firstPosition.getLatitude() > -90 &&
                firstPosition.getLongitude() < 180 && firstPosition.getLongitude() > -180 &&
                firstPosition.getElevation() < 20000;
    }

    protected List<Wgs84Route> read(ByteBuffer buffer) throws IOException {
        return readPositions(buffer, 0, buffer.capacity());
    }
}
