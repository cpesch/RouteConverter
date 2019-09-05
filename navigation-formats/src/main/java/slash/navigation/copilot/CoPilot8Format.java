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

import slash.navigation.base.ParserContext;
import slash.navigation.base.Wgs84Route;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;

import static slash.common.io.Transfer.UTF16LE_ENCODING;
import static slash.common.io.Transfer.UTF16_ENCODING;

/**
 * Reads and writes CoPilot 8 (.trp) files.
 *
 * @author Christian Pesch
 */

public class CoPilot8Format extends CoPilotFormat {

    public String getName() {
        return "CoPilot 8 (*" + getExtension() + ")";
    }

    public void read(InputStream source, ParserContext<Wgs84Route> context) throws IOException {
        read(source, UTF16_ENCODING, context);
    }

    protected boolean isDataVersion(String line) {
        return line.startsWith(DATA_VERSION + ":1");
    }

    public void write(Wgs84Route route, OutputStream target, int startIndex, int endIndex) throws IOException {
        write(route, target, UTF16LE_ENCODING, startIndex, endIndex);
    }

    public void write(Wgs84Route route, PrintWriter writer, int startIndex, int endIndex) {
        // with UTF-16LE no BOM is written, UnicodeLittle would write one by is not supported
        // (see http://java.sun.com/j2se/1.4.2/docs/guide/intl/encoding.doc.html)
        // but the fix from http://mindprod.com/jgloss/encoding.html helped me
        writer.write(BYTE_ORDER_MARK);
        writer.println(DATA_VERSION + ":1.13.5.2");
        writeHeader(route, writer);
        writePositions(route, writer, startIndex, endIndex);
    }
}