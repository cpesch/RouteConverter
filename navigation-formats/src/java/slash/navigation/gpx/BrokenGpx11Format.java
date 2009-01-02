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

import slash.navigation.gpx.binding11.GpxType;

import javax.xml.bind.JAXBException;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Calendar;
import java.util.List;
import java.util.logging.Logger;

/**
 * Reads and writes broken GPS Exchange Format 1.1 (.gpx) files.
 *
 * @author Christian Pesch
 */

public class BrokenGpx11Format extends Gpx11Format {
    private static final Logger log = Logger.getLogger(BrokenGpx11Format.class.getName());

    public String getName() {
        return "GPS Exchange Format " + VERSION + " Garble (*" + getExtension() + ")";
    }

    public boolean isSupportsWriting() {
        return false;
    }

    public List<GpxRoute> read(File source, Calendar startDate) throws IOException {
        FileReader reader = new FileReader(source);
        try {
            GpxType gpxType = GpxUtil.unmarshal11(reader);
            return process(gpxType);
        } catch (JAXBException e) {
            log.fine("Error reading " + source + ": " + e.getMessage());
        }
        finally {
            reader.close();
        }
        return null;
    }
}