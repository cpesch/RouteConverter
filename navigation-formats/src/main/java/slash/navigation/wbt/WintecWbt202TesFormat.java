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

import static java.lang.Math.abs;
import static java.nio.ByteOrder.LITTLE_ENDIAN;
import static java.util.Calendar.YEAR;

import java.nio.ByteBuffer;
import java.util.List;

import slash.common.type.CompactCalendar;
import slash.navigation.base.BaseNavigationPosition;
import slash.navigation.base.Wgs84Route;

/**
 * Reads and writes Wintec WBT-202 (.tes) files.
 *
 * Format is basically {@link WintecWbt201Tk2Format} without a 16 byte header as described at:
 * http://www.geosetter.de/mantis/view.php?id=373
 *
 * @author Malte Neumann
 */

public class WintecWbt202TesFormat extends WintecWbt201Format {

    public String getExtension() {
        return ".tes";
    }

    public String getName() {
        return "Wintec WBT-202 (*" + getExtension() + ")";
    }

    protected int getHeaderSize() {
        // this means files with less than 3 positions are not recognized
        return 3 * 16; 
    }

    protected boolean checkFormatDescriptor(ByteBuffer buffer) {
        buffer.order(LITTLE_ENDIAN);
        buffer.position(0);

        // read first positions and validate the data
        BaseNavigationPosition previousPosition = null;
        while ((buffer.position() + 16) < buffer.capacity()) { //16: one Record
            /*short trackFlag*/
            buffer.getShort();
            int time = buffer.getInt();
            int latitude = buffer.getInt();
            int longitude = buffer.getInt();
            short altitude = buffer.getShort();
            BaseNavigationPosition position = createWaypoint(time, latitude, longitude, altitude, 1, false);

			if (!isValidData(position, previousPosition)) {
				return false;
            }

            previousPosition = position;
        }
        return true;
    }

	boolean isValidData(BaseNavigationPosition currentPosition, BaseNavigationPosition previousPosition) {
		double lat = currentPosition.getLatitude();
		double lon = currentPosition.getLongitude();
		double elev = currentPosition.getElevation();
		CompactCalendar time = currentPosition.getTime();
		if (lat >= 90 || lat <= -90 || abs(lat) <= 0.00001) {
			return false;
		}
		if (lon >= 180.0 || lon <= -180.0 || abs(lon) <= 0.00001) {
			return false;
		}
		if (elev >= 15000) {
			return false;
		}
		if (time.getCalendar().get(YEAR) <= 1990) {
			return false;
		}

		if (previousPosition == null) {
			return true;
		}

		if (previousPosition.getTime().getTimeInMillis() >= time.getTimeInMillis()) {
			return false;
		}
		Double dist = currentPosition.calculateDistance(previousPosition);
		if (dist == null || dist.equals(Double.valueOf(0d))) {
			return true;
		}
		Double speed = currentPosition.calculateSpeed(previousPosition);
		return speed != null && speed < 1500d;
	}

    protected List<Wgs84Route> internalRead(ByteBuffer buffer) {
        return readPositions(buffer, 0, buffer.capacity());
    }
}
