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

package slash.navigation.bcr;

import java.io.PrintWriter;

import static slash.navigation.bcr.BcrSection.STATION_PREFIX;

/**
 * Reads and writes Map &amp; Guide Tourenplaner 2006/2007 (.bcr) files.
 *
 * @author Christian Pesch
 */

public class MTP0607Format extends BcrFormat {
    public String getName() {
        return "Map&Guide Tourenplaner 2006/2007 (*" + getExtension() + ")";
    }

    protected boolean isValidDescription(BcrSection description) {
        return true;
    }

    protected void writePosition(BcrPosition position, PrintWriter writer, int index) {
        String description = (position.getZipCode() != null ? position.getZipCode() + " " : "") +
                (position.getCity() != null ? position.getCity() : "");
        writer.println(STATION_PREFIX + index + NAME_VALUE_SEPARATOR + description);
    }
}
