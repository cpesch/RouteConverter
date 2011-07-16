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

    Copyright (C) 2010 Christian Pesch. All Rights Reserved.
*/


package slash.navigation.simple;

import slash.common.io.CompactCalendar;
import slash.navigation.base.*;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

import static slash.navigation.base.RouteCharacteristics.Track;

/**
 * Reads broken Navilink (.sbp) files.
 *
 * @author Malte Neumann
 */

public class BrokenNavilinkFormat extends NavilinkFormat {
    private static final long MINIMUM_TIME_MILLISECONDS = 949427965809l; // 01.01.2000

    public String getName() {
        return "Navilink Garble (*" + getExtension() + ")";
    }

    protected boolean isTrackStart(ByteBuffer buffer) {
        short bitFlags = buffer.get(30);
        short reserved = buffer.get(31);
        return ((bitFlags & 0x01) == 1) && (reserved == 0x14);
    }

    private int readOneByteFromInput(InputStream source, byte[] record) throws IOException {
        System.arraycopy(record, 1, record, 0, record.length - 1);
        return source.read(record, SBP_RECORD_LENGTH - 1, 1);
    }

    private boolean isValidPosition(Wgs84Position position, Wgs84Position previous) {
        boolean valid = (position.getHdop() > -0.0000000001 &&
                position.getHdop() < 10.21);
        valid = valid && (position.getLatitude() >= -90 &&
                position.getLatitude() <= 90);
        valid = valid && (position.getLongitude() >= -180 &&
                position.getLatitude() <= 180);
        valid = valid && (position.getElevation() > -100 &&
                position.getElevation() < 14000);
        valid = valid && (position.getSpeed() >= -0.9999999 &&
                position.getSpeed() < 1200);
        valid = valid && (position.getHeading() > -0.999999 &&
                position.getHeading() < 360.000001);
        valid = valid && (position.getTime().getTimeInMillis() > MINIMUM_TIME_MILLISECONDS &&
                position.getTime().getTimeInMillis() < System.currentTimeMillis());

        if (previous != null) {
            // valid = valid && (position.getTime().getTimeInMillis() > previous.getTime().getTimeInMillis());
            // would ignore too much points, thus I've build an after read check function
            Double speed = position.calculateSpeed(previous);
            if (speed != null)
                valid = valid && (speed < 1200);
        }

        return valid;
    }

    private void deleteLogicalWrongPositionsInList(List<Wgs84Position> positions) {
        int i = 1;
        while (i < positions.size() - 1) {
            Wgs84Position prev = positions.get(i - 1);
            Wgs84Position actual = positions.get(i);
            Wgs84Position next = positions.get(i + 1);

            if (prev.getTime().getTimeInMillis() < actual.getTime().getTimeInMillis() &&
                    actual.getTime().getTimeInMillis() < next.getTime().getTimeInMillis())
                i++;
            else
                positions.remove(i);
        }
    }

    private void deleteLogicalWrongPositions(List<Wgs84Route> routeList) {
        for (Wgs84Route route : routeList) {
            deleteLogicalWrongPositionsInList(route.getPositions());
        }
    }

    public List<Wgs84Route> read(InputStream source, CompactCalendar startDate) throws IOException {
        byte[] record = new byte[SBP_RECORD_LENGTH];
        ByteBuffer sbpRecordByteBuffer = ByteBuffer.wrap(record);
        sbpRecordByteBuffer.order(ByteOrder.LITTLE_ENDIAN);

        List<Wgs84Route> result = new ArrayList<Wgs84Route>();
        Wgs84Route activeRoute = null;

        Wgs84Position position;
        Wgs84Position previousPosition = null;

        int readBytes = 0, pointCount = 0;
        while (source.read(record) == SBP_RECORD_LENGTH) {
            readBytes += SBP_RECORD_LENGTH;
            do {
                sbpRecordByteBuffer.position(0);
                position = decodePosition(sbpRecordByteBuffer);
                if (!isValidPosition(position, previousPosition)) {
                    position = null;
                    int count = readOneByteFromInput(source, record);
                    readBytes += count;
                    if (count != 1) {
                        break;
                    }
                    // the first position must inside the first 40 bytes
                    if (pointCount == 0 && readBytes > 40)
                        break;
                }
            } while (position == null);

            // at least three positions in the first 100 bytes
            if (readBytes > 100 && pointCount < 3) {
                return null;
            }

            if ((activeRoute == null || isTrackStart(sbpRecordByteBuffer)) && position != null) {
                activeRoute = createRoute(Track,
                        TRACK_NAME_DATE_FORMAT.format(position.getTime().getTime()),
                        new ArrayList<BaseNavigationPosition>());
                result.add(activeRoute);
            }

            if (position != null && activeRoute != null)
                activeRoute.getPositions().add(position);
            else
                return result;

            pointCount++;
            previousPosition = position;
        }
        deleteLogicalWrongPositions(result);
        
        // it must be 95% of the file valid
        int minCorrectPositions = (int) ((readBytes / SBP_RECORD_LENGTH) * 0.95);
        if (pointCount < minCorrectPositions)
            return null;
        return result;
    }
}
