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

package slash.navigation.kml;

import slash.common.type.CompactCalendar;
import slash.navigation.base.GarbleNavigationFormat;
import slash.navigation.base.ParserContext;
import slash.navigation.kml.binding22.KmlType;

import java.io.InputStream;
import java.io.InputStreamReader;

import static slash.navigation.kml.KmlUtil.unmarshal22;

/**
 * Reads garbled Google Earth 5 (.kml) files.
 *
 * @author Christian Pesch
 */

public class GarbleKml22Format extends Kml22Format implements GarbleNavigationFormat {

    public String getName() {
        return "Google Earth 5 Garble (*" + getExtension() + ")";
    }

    public boolean isSupportsWriting() {
        return false;
    }

    public void read(InputStream source, CompactCalendar startDate, ParserContext<KmlRoute> context) throws Exception {
        try (InputStreamReader reader = new InputStreamReader(source)) {
            KmlType kmlType = unmarshal22(reader);
            process(kmlType, startDate, context);
        }
    }
}