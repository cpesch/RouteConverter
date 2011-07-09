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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * Reads broken Navilink (.sbp) files.
 *
 * @author Malte Neumann
 */

public class BrokenNavilinkFormat extends NavilinkFormat {
    private static final int SBP_RECORD_LENGTH_SHORT = 31;

    public String getName() {
        return "Navilink Garble (*" + getExtension() + ")";
    }

    public List<Wgs84Route> read(InputStream source, CompactCalendar startDate) throws IOException {
        List<Wgs84Route> resultRouteList = null;

        byte[] header = new byte[HEADER_SIZE];
        if (source.read(header) == HEADER_SIZE) {
            ByteBuffer sbpRecordByteBuffer = ByteBuffer.allocate(SBP_RECORD_LENGTH);
            sbpRecordByteBuffer.order(ByteOrder.LITTLE_ENDIAN);

            resultRouteList = new ArrayList<Wgs84Route>();
            Wgs84Route activeRoute = null;

            byte[] record = new byte[SBP_RECORD_LENGTH];
            Wgs84Position position;
            int expectedByteLength = SBP_RECORD_LENGTH;
            while (source.read(record, 0, expectedByteLength) == expectedByteLength) {
                if (expectedByteLength == SBP_RECORD_LENGTH_SHORT) {
                    // insert 1 byte at position 1
                    for (int i = record.length - 1; i > 1; i--)
                        record[i] = record[i - 1];
                    record[1] = 3;

                    expectedByteLength = SBP_RECORD_LENGTH;
                }

                sbpRecordByteBuffer.position(0);
                sbpRecordByteBuffer.put(record);

                position = decodePosition(sbpRecordByteBuffer);
                if ((short) record[30] > 0x0F) {
                    expectedByteLength = SBP_RECORD_LENGTH_SHORT;
                }

                if ((activeRoute == null) || (isTrackStart(sbpRecordByteBuffer))) {
                    activeRoute = createRoute(RouteCharacteristics.Track,
                            TRACK_NAME_DATE_FORMAT.format(position.getTime().getTime()),
                            new ArrayList<BaseNavigationPosition>());
                    resultRouteList.add(activeRoute);
                }

                activeRoute.getPositions().add(position);
            }
        }
        return resultRouteList;
    }
}
