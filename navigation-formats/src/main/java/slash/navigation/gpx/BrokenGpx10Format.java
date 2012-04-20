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

package slash.navigation.gpx;

import slash.common.io.CompactCalendar;
import slash.navigation.base.ParserContext;
import slash.navigation.gpx.binding10.Gpx;

import java.io.InputStream;
import java.io.InputStreamReader;

import static slash.navigation.gpx.GpxUtil.unmarshal10;

/**
 * Reads broken GPS Exchange Format 1.0 (.gpx) files.
 *
 * @author Christian Pesch
 */

public class BrokenGpx10Format extends Gpx10Format {
    public String getName() {
        return "GPS Exchange Format " + VERSION + " Garble (*" + getExtension() + ")";
    }

    public boolean isSupportsWriting() {
        return false;
    }

    public void read(InputStream source, CompactCalendar startDate, ParserContext<GpxRoute> context) throws Exception {
        InputStreamReader reader = new InputStreamReader(source);
        try {
            Gpx gpx = unmarshal10(reader);
            process(gpx, context);
        }
        finally {
            reader.close();
        }
    }
}