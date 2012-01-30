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

import slash.navigation.base.Wgs84Route;

import java.io.PrintWriter;

/**
 * Reads and writes CoPilot 6 (.trp) files.
 *
 * @author Christian Pesch
 */

public class CoPilot6Format extends CoPilotFormat {

    public String getName() {
        return "CoPilot 6 (*" + getExtension() + ")";
    }

    protected boolean isDataVersion(String line) {
        return line.startsWith(DATA_VERSION + NAME_VALUE_SEPARATOR + "6");
    }

    public void write(Wgs84Route route, PrintWriter writer, int startIndex, int endIndex) {
        writer.println(DATA_VERSION + NAME_VALUE_SEPARATOR + "6.0.0.27");
        writeHeader(route, writer);
        writePositions(route, writer, startIndex, endIndex);
    }
}
