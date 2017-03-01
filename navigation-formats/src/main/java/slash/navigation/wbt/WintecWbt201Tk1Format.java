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
 * Reads and writes Wintec WBT-201 (.tk1) files.
 *
 * Format is documented at:
 * http://forum.pocketnavigation.de/attachment.php?attachmentid=1082952
 * http://forum.pocketnavigation.de/attachment.php?attachmentid=1082953
 * http://www.steffensiebert.de/soft/python/wintec_201_fileformat.html
 *
 * @author Malte Neumann
 */

public class WintecWbt201Tk1Format extends WintecWbt201Format {
    private static final String FORMAT_DESCRIPTOR = "WintecLogFormat";

    public String getExtension() {
        return ".tk1";
    }

    protected int getHeaderSize() {
        return 1024;
    }

    protected boolean checkFormatDescriptor(ByteBuffer buffer) throws IOException {
        buffer.position(0);
        byte[] bytes = new byte[16];
        buffer.get(bytes, 0, 16);
        String formatDescriptor = new String(bytes, 0, 15, ISO_LATIN1_ENCODING);
        return formatDescriptor.equals(FORMAT_DESCRIPTOR);
    }

    protected List<Wgs84Route> internalRead(ByteBuffer buffer) {
        /* 
           char pHeader[16];//="WintecLogFormat"; //16
           float f32LogVersion;                   //20
           float f32SWVersion;                    //24
           float f32HWVersion;                    //28
           unsigned short u16FlashID;             //30
           unsigned short u16DataLength;          //32
           unsigned long u32HowManyData;          //36
           unsigned char u8HowManyCheckSumAtLast; //37
           unsigned char u8Reserver;              //38
           unsigned short u16Reserver;            //40
           char pDeviceName1[20];                 //60
           char pDeviceName2[20];                 //80
           char pDeviceName3[40];                 //120
           char pLocalTimeOfReading[20];          //140
           unsigned int u32StartTrackInfoStructAddressForSeek; //144
           unsigned int u32TrackNumber;           //148
           char pResever1[876];                   //1024
         */

        buffer.order(LITTLE_ENDIAN);
        buffer.position(140);
        int trackInfoAddress = buffer.getInt();
        return readPositions(buffer, 1024, trackInfoAddress);
    }
}
