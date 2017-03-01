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

import slash.navigation.base.Wgs84Route;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;

import static java.nio.ByteOrder.LITTLE_ENDIAN;
import static slash.common.io.Transfer.ISO_LATIN1_ENCODING;

/**
 * Reads and writes Wintec WBT-201 (.tk2) files.
 *
 * Format is documented at:
 * http://forum.pocketnavigation.de/attachment.php?attachmentid=1082952
 * http://forum.pocketnavigation.de/attachment.php?attachmentid=1082953
 * http://www.steffensiebert.de/soft/python/wintec_201_fileformat.html
 *
 * @author Malte Neumann
 */

public class WintecWbt201Tk2Format extends WintecWbt201Format {
    private static final String FORMAT_DESCRIPTOR = "WintecLogTk2"; // or WintecLogTK2

    public String getExtension() {
        return ".tk2";
    }

    protected int getHeaderSize() {
        return 1024;
    }

    protected boolean checkFormatDescriptor(ByteBuffer buffer) throws IOException {
        buffer.position(0);
        byte[] bytes = new byte[16];
        buffer.get(bytes, 0, 16);
        String formatDescriptor = new String(bytes, 0, 12, ISO_LATIN1_ENCODING).toLowerCase();
        return formatDescriptor.equals(FORMAT_DESCRIPTOR.toLowerCase());
    }

    protected List<Wgs84Route> internalRead(ByteBuffer buffer) {
        /*
           char pHeader[16];//="WintecLogTk2";   //16
           float f32LogVersion;                  //20
           float f32SWVersion;                   //24
           float f32HWVersion;                   //28
           unsigned short u16FlashID;            //30
           char pDeviceName1[20];                //50
           char pDeviceName2[20];                //70
           char pDeviceName3[40];                //110
           char pLocalTimeOfReading[20];         //130
           //------ Tk2 New --------------------------------
           char TrackDescription[300];           //430
           char ZoneIndex;                       //431  // 1 or -1
           unsigned char ZoneHour;               //432
           unsigned char ZoneMinite;             //433
           char pTimeToFirstPoint[27];           //460
           unsigned int TotalPoint;              //464
           unsigned int TotalTime_s;             //468
           unsigned int TotalDistance_m;         //472
           unsigned int NumberOfPushToLog;       //476
           //------------------------------------------------
           char pResever1[548];                  //1024
         */

        buffer.order(LITTLE_ENDIAN);
        buffer.position(140);
        // .tk2 has no TrackInfo Structure, set position to end of file
        // readPositions processes this correctly
        long trackInfoAddress = buffer.capacity();
        return readPositions(buffer, 1024, trackInfoAddress);
    }
}
