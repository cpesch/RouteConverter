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

import slash.navigation.base.Wgs84Position;
import slash.navigation.base.Wgs84Route;
import slash.common.io.CompactCalendar;
import slash.common.io.Transfer;

import java.io.*;
import java.util.List;

/**
 * Reads and writes CoPilot 7 (.trp) files.
 *
 * @author Christian Pesch
 */

public class CoPilot7Format extends CoPilotFormat {

    public String getName() {
        return "CoPilot 7 (*" + getExtension() + ")";
    }

    public List<Wgs84Route> read(InputStream source, CompactCalendar startDate) throws IOException {
        return read(source, startDate, UTF16_ENCODING);
    }

    protected boolean isDataVersion(String line) {
        return line.startsWith(DATA_VERSION + NAME_VALUE_SEPARATOR);
    }

    public void write(Wgs84Route route, OutputStream target, int startIndex, int endIndex) throws IOException {
        write(route, target, UTF16LE_ENCODING, startIndex, endIndex);
    }

    public void write(Wgs84Route route, PrintWriter writer, int startIndex, int endIndex) {
        // with UTF-16LE no BOM is written, UnicodeLittle would write one by is not supported
        // (see http://java.sun.com/j2se/1.4.2/docs/guide/intl/encoding.doc.html)
        // but the fix from http://mindprod.com/jgloss/encoding.html helped me
        writer.write(BYTE_ORDER_MARK);
        writer.println(DATA_VERSION + NAME_VALUE_SEPARATOR + "7.0.0.x");
        writeHeader(route, writer);
        writePositions(route, writer, startIndex, endIndex);
    }
}