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

import slash.common.type.CompactCalendar;
import slash.navigation.base.BaseNavigationPosition;
import slash.navigation.base.ParserContext;
import slash.navigation.base.RouteCharacteristics;
import slash.navigation.base.SimpleFormat;
import slash.navigation.base.Wgs84Position;
import slash.navigation.base.Wgs84Route;
import slash.navigation.common.NavigationPosition;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import static java.lang.Long.parseLong;
import static java.util.Calendar.DAY_OF_MONTH;
import static java.util.Calendar.HOUR_OF_DAY;
import static java.util.Calendar.MINUTE;
import static java.util.Calendar.MONTH;
import static java.util.Calendar.SECOND;
import static java.util.Calendar.YEAR;
import static slash.common.type.CompactCalendar.createDateFormat;
import static slash.common.type.CompactCalendar.fromCalendar;
import static slash.navigation.base.RouteCharacteristics.Track;

/**
 * Reads Navilink (.sbp) files.
 *
 * Format is documented at:
 * http://notes.splitbrain.org/navilink
 * http://www.SteffenSiebert.de/soft/python/locosys_tools.html
 * gpsbabel source navilink.c, sbp.c
 *
 * Devices are:
 * Locosys BGT-31 http://www.locosystech.com/product.php?zln=en&amp;id=30
 *
 * @author Malte Neumann
 */

public class NavilinkFormat extends SimpleFormat<Wgs84Route> {
    protected static final int HEADER_SIZE = 64;
    protected static final int SBP_RECORD_LENGTH = 32;
    protected static final String TRACK_NAME_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";

    public String getExtension() {
        return ".sbp";
    }

    public String getName() {
        return "Navilink (*" + getExtension() + ")";
    }

    public int getMaximumPositionCount() {
        return UNLIMITED_MAXIMUM_POSITION_COUNT;
    }

    public boolean isSupportsReading() {
        return true;
    }

    public boolean isSupportsMultipleRoutes() {
        return true;
    }

    public boolean isSupportsWriting() {
        return false;
    }

    @SuppressWarnings("unchecked")
    public <P extends NavigationPosition> Wgs84Route createRoute(RouteCharacteristics characteristics, String name, List<P> positions) {
        Wgs84Route newRoute = new Wgs84Route(this, characteristics, (List<Wgs84Position>) positions);
        newRoute.setName(name);
        return newRoute;
    }

    public void read(BufferedReader reader, String encoding, ParserContext<Wgs84Route> context) {
        // this format parses the InputStream directly but wants to derive from SimpleFormat to use Wgs84Route
        throw new UnsupportedOperationException();
    }

    private boolean checkHeader(byte[] header) {
        /* gpbsbabel sbp.c:
         * A complete SBP file contains 64 bytes header,
         *
         * Here is the definition of the SBP header 
         * BYTE 0 ~1 : true SBP header length 
         * BYTE 2~63:  MID_FILE_ID(0xa0 0xa2 2 byte len 0xfd) with following payload :
         *             User Name, Serial Number, Log Rate, Firmware Version
         *               >field separator:","
         *               >User Name : MAX CHAR(13)
         *               >Serial Number : MAX CHAR(8)
         *               >Log Rate : MAX CHAR 3, 0..255 in seconds
         *               >Firmware Version  :  MAX CHAR (13)
         *               // will stuff 0xff for remaining bytes
         */

        return ((header[2] == (byte) 0xA0) && (header[3] == (byte) 0xA2) && (header[6] == (byte) 0xFD));
    }

    private static final long MONTH_MASK = parseLong("11111111110000000000000000000000", 2);
    private static final long DAY_MASK = parseLong("00000000001111100000000000000000", 2);
    private static final long HOUR_MASK = parseLong("00000000000000011111000000000000", 2);
    private static final long MINUTE_MASK = parseLong("00000000000000000000111111000000", 2);
    private static final long SECOND_MASK = parseLong("00000000000000000000000000111111", 2);

    protected CompactCalendar decodeDateTime(long dateTime) {
        /*
          Packed_Date_Time_UTC:
          bit 31..22: year*12+month (10 bits) : real year= year+2000
          bit 17..21: day (5bits)
          bit 12..16: hour (5bits)
          bit  6..11: min  (6bits)
          bit  0..5 : sec  (6bits)
      
          0        1        2        3
          01234567 01234567 01234567 01234567 
          ........ ........ ........ ........
          SSSSSSMM MMMMHHHH Hdddddmm mmmmmmmm
      
         */
        int second = (int) ((dateTime & SECOND_MASK));
        int minute = (int) ((dateTime & MINUTE_MASK) >> 6);
        int hour = (int) ((dateTime & HOUR_MASK) >> 12);
        int day = (int) ((dateTime & DAY_MASK) >> 17);
        int months = (int) ((dateTime & MONTH_MASK) >> 22);
        int month = months % 12;
        int year = 2000 + months / 12;

        Calendar calendar = Calendar.getInstance(CompactCalendar.UTC);
        calendar.set(YEAR, year);
        calendar.set(MONTH, month - 1); // Java month starts with 0
        calendar.set(DAY_OF_MONTH, day);
        calendar.set(HOUR_OF_DAY, hour);
        calendar.set(MINUTE, minute);
        calendar.set(SECOND, second);
        return fromCalendar(calendar);
    }

    protected boolean isTrackStart(ByteBuffer buffer) {
        short bitFlags = buffer.get(30);
        return (bitFlags & 0x01) == 1;
    }

    protected Wgs84Position decodePosition(ByteBuffer buffer) {
        buffer.position(0);

        /*
        typedef __packed struct
        {
          UINT8 HDOP;        // HDOP [0..51] with resolution 0.2 
          UINT8 SVIDCnt;        // Number of SVs in solution [0 to 12] 
          UINT16 UtcSec;        // UTC Second [0 to 59] in seconds with resolution 0.001 
          UINT32 date_time_UTC_packed; // refer to protocol doc
          UINT32 SVIDList;    // SVs in solution:  Bit 0=1: SV1, Bit 1=1: SV2, ... , Bit 31=1: SV32 
          INT32 Lat;            // Latitude [-90 to 90] in degrees with resolution 0.0000001 
          INT32 Lon;            // Longitude [-180 to 180] in degrees with resolution 0.0000001 
          INT32 AltCM;            // Altitude from Mean Sea Level in centi meters 
          UINT16 Sog;            // Speed Over Ground in m/sec with resolution 0.01 
          UINT16 Cog;            // Course Over Ground [0 to 360] in degrees with resolution 0.01 
          INT16 ClmbRte;        // Climb rate in m/sec with resolution 0.01 
          UINT8 bitFlags;     // bitFlags, default 0x00,    bit 0=1 indicate the first point after power on 
          UINT8 reserved;     //Malte: It seems that is also Flag: If bitFlags bit 0 = 1 and reserved = 0x14 = First point of new Track. This is used in gpsbabel for a new track.  
        } T_SBP;   
        */

        byte hdop = buffer.get();
        byte satellites = buffer.get();
        buffer.getShort(); //Second resolution 0.001  --> ignore
        CompactCalendar dateTime = decodeDateTime(buffer.getInt());
        buffer.getInt(); //SVs in solution --> ignore
        int latitude = buffer.getInt();
        int longitude = buffer.getInt();
        int altitudeCm = buffer.getInt();
        short speedMeterPerSecond = buffer.getShort();
        int heading = buffer.getShort() & 0xFFFF;
        Wgs84Position position = new Wgs84Position(longitude / 10000000.0,
                latitude / 10000000.0,
                altitudeCm / 100.0,
                speedMeterPerSecond * 0.01 * 3.6,
                dateTime,
                null);
        position.setHdop(hdop * 0.2);
        position.setSatellites((int) satellites);
        position.setHeading(heading * 0.01);
        return position;
    }

    public void read(InputStream source, ParserContext<Wgs84Route> context) throws Exception {
        byte[] header = new byte[HEADER_SIZE];
        if ((source.read(header) == HEADER_SIZE) && checkHeader(header)) {
            ByteBuffer sbpRecordByteBuffer = ByteBuffer.allocate(SBP_RECORD_LENGTH);
            sbpRecordByteBuffer.order(ByteOrder.LITTLE_ENDIAN);

            Wgs84Route activeRoute = null;
            byte[] record = new byte[SBP_RECORD_LENGTH];
            while (source.read(record) == SBP_RECORD_LENGTH) {
                sbpRecordByteBuffer.position(0);
                sbpRecordByteBuffer.put(record);

                Wgs84Position position = decodePosition(sbpRecordByteBuffer);
                if ((activeRoute == null) || (isTrackStart(sbpRecordByteBuffer))) {
                    activeRoute = createRoute(Track,
                            createDateFormat(TRACK_NAME_DATE_FORMAT).format(position.getTime().getTime()),
                            new ArrayList<BaseNavigationPosition>());
                    context.appendRoute(activeRoute);
                }

                activeRoute.getPositions().add(position);
            }
        }
    }

    public void write(Wgs84Route route, PrintWriter writer, int startIndex, int endIndex) {
        throw new UnsupportedOperationException();
    }
}
