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

package slash.navigation.copilot;

import slash.common.io.CompactCalendar;
import slash.common.io.Transfer;
import slash.navigation.base.Wgs84Position;
import slash.navigation.base.Wgs84Route;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.List;

/**
 * Reads and writes CoPilot 8 (.trp) files.
 *
 * @author Christian Pesch
 */

public class CoPilot8Format extends CoPilotFormat {
    protected static final String SHOW = "Show";

    public String getName() {
        return "CoPilot 8 (*" + getExtension() + ")";
    }

    public List<Wgs84Route> read(InputStream source, CompactCalendar startDate) throws IOException {
        return read(source, startDate, UTF16_ENCODING);
    }

    protected boolean isDataVersion(String line) {
        return line.startsWith(DATA_VERSION + ":");
    }

    public void write(Wgs84Route route, OutputStream target, int startIndex, int endIndex) throws IOException {
        write(route, target, UTF16LE_ENCODING, startIndex, endIndex);
    }

    public void write(Wgs84Route route, PrintWriter writer, int startIndex, int endIndex) {
        // with UTF-16LE no BOM is written, UnicodeLittle would write one by is not supported
        // (see http://java.sun.com/j2se/1.4.2/docs/guide/intl/encoding.doc.html)
        // but the fix from http://mindprod.com/jgloss/encoding.html helped me
        writer.write('\ufeff');
        writer.println(DATA_VERSION + ":1.13.5.2");
        writer.println(START_TRIP + NAME_VALUE_SEPARATOR + route.getName());
        writer.println(CREATOR + NAME_VALUE_SEPARATOR + GENERATED_BY);
        writer.println("TollClosed=0");
        writer.println(END_TRIP);
        writer.println();

        List<Wgs84Position> positions = route.getPositions();
        for (int i = startIndex; i < endIndex; i++) {
            Wgs84Position position = positions.get(i);
            writer.println(START_STOP + NAME_VALUE_SEPARATOR + "Stop " + i);
            String longitude = Transfer.formatIntAsString(position.getLongitude() != null ? (int) (position.getLongitude() * INTEGER_FACTOR) : null);
            writer.println(LONGITUDE + NAME_VALUE_SEPARATOR + longitude);
            String latitude = Transfer.formatIntAsString(position.getLatitude() != null ? (int) (position.getLatitude() * INTEGER_FACTOR) : null);
            writer.println(LATITUDE + NAME_VALUE_SEPARATOR + latitude);

            // TODO write decomposed comment
            // Name=
            // Address=11 Veilchenstrasse
            // City=Gladbeck
            // State=DE
            // County=Recklinghausen
            // Zip=47853

            String comment = position.getComment();
            int index = comment.indexOf(',');
            String city = index != -1 ? comment.substring(0, index) : comment;
            city = Transfer.trim(city);
            String address = index != -1 ? comment.substring(index + 1) : comment;
            address = Transfer.trim(address);
            boolean first = i == startIndex;
            boolean last = i == endIndex - 1;

            // only store address if there was a comma in the comment
            writer.println(ADDRESS + NAME_VALUE_SEPARATOR + (index != -1 ? address : ""));
            // otherwise store comment als city
            writer.println(CITY + NAME_VALUE_SEPARATOR + city);
            if (first || last)
                writer.println(SHOW + NAME_VALUE_SEPARATOR + "1"); // Target/Stop target
            else
                writer.println(SHOW + NAME_VALUE_SEPARATOR + "0"); // Waypoint
            writer.println(END_STOP);
            writer.println();

            writer.println(START_STOP_OPT + NAME_VALUE_SEPARATOR + "Stop " + i);
            writer.println("Loaded=1");
            writer.println(END_STOP_OPT);
            writer.println();
        }
    }
}