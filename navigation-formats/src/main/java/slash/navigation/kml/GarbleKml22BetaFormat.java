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

import slash.navigation.base.GarbleNavigationFormat;
import slash.navigation.base.ParserContext;
import slash.navigation.kml.binding22beta.KmlType;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import static slash.navigation.kml.KmlUtil.unmarshal22Beta;

/**
 * Reads garble Google Earth 4.2 (.kml) files.
 *
 * @author Christian Pesch
 */

public class GarbleKml22BetaFormat extends Kml22BetaFormat implements GarbleNavigationFormat {

    public String getName() {
        return "Google Earth 4.2 Garble (*" + getExtension() + ")";
    }

    public boolean isSupportsWriting() {
        return false;
    }

    public void read(InputStream source, ParserContext<KmlRoute> context) throws IOException {
        try (InputStreamReader reader = new InputStreamReader(source)) {
            KmlType kmlType = unmarshal22Beta(reader);
            process(kmlType, context);
        }
    }
}