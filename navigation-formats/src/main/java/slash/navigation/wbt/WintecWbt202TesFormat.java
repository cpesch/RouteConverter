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
import static java.nio.ByteBuffer.allocate;
import static java.nio.ByteOrder.LITTLE_ENDIAN;
import static java.util.Collections.emptyList;
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
    private static final int RECORD_SIZE = 16;
    private static final int MINIMUM_PLAUSIBLE_RECORDS = 3;
    // tolerates a damaged record in a hundred, which is skipped
    private static final double MINIMUM_PLAUSIBLE_SHARE = 0.99;

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
        ByteBuffer plausible = plausibleRecords(buffer);
        if (plausible == null)
            return emptyList();
        return readPositions(plausible, 0, plausible.capacity());
    }

    /**
     * The format has no header, so the first records decide and every other byte sequence of the
     * right length would be read: random bytes and MapSend tracks became dozens of garbage tracks.
     * Real files consist of plausible records only, random data of about one in five.
     *
     * @return the plausible records if they make up nearly all of the file, otherwise null
     */
    static ByteBuffer plausibleRecords(ByteBuffer buffer) {
        buffer.order(LITTLE_ENDIAN);
        int count = buffer.capacity() / RECORD_SIZE;
        ByteBuffer result = allocate(count * RECORD_SIZE);
        int plausibleCount = 0;
        for (int i = 0; i < count; i++) {
            int offset = i * RECORD_SIZE;
            int time = buffer.getInt(offset + 2);
            int latitude = buffer.getInt(offset + 6);
            int longitude = buffer.getInt(offset + 10);
            if (isPlausibleRecord(time, latitude, longitude)) {
                for (int j = 0; j < RECORD_SIZE; j++)
                    result.put(buffer.get(offset + j));
                plausibleCount++;
            }
        }
        if (plausibleCount < MINIMUM_PLAUSIBLE_RECORDS || plausibleCount < count * MINIMUM_PLAUSIBLE_SHARE)
            return null;

        ByteBuffer plausible = allocate(plausibleCount * RECORD_SIZE);
        plausible.put(result.array(), 0, plausibleCount * RECORD_SIZE);
        plausible.position(0);
        return plausible;
    }

    // the calendar of createWaypoint() is lenient and would turn month 15 into a date of the next year
    static boolean isPlausibleRecord(int time, int latitude, int longitude) {
        int month = (time >>> 22) & 0xF;
        int day = (time >>> 17) & 0x1F;
        int hour = (time >>> 12) & 0x1F;
        int minute = (time >>> 6) & 0x3F;
        int second = time & 0x3F;
        if (month < 1 || month > 12 || day < 1 || hour > 23 || minute > 59 || second > 59)
            return false;
        // latitude and longitude in 1/10,000,000 degrees, both zero means no fix
        return abs((long) latitude) <= 900_000_000L && abs((long) longitude) <= 1_800_000_000L &&
                !(latitude == 0 && longitude == 0);
    }
}
