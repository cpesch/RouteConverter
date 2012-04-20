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

import slash.common.io.CompactCalendar;
import slash.navigation.kml.binding21.KmlType;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

import static slash.navigation.kml.KmlUtil.unmarshal21;

/**
 * Reads broken little endian Google Earth 4 (.kml) files.
 *
 * @author Christian Pesch
 */

public class BrokenKml21LittleEndianFormat extends Kml21Format {

    public String getName() {
        return "Google Earth 4 Little Endian Garble (*" + getExtension() + ")";
    }

    public boolean isSupportsWriting() {
        return false;
    }

    List<KmlRoute> internalRead(InputStream source, CompactCalendar startDate) throws IOException, JAXBException {
        InputStreamReader reader = new InputStreamReader(source, UTF16LE_ENCODING);
        try {
            KmlType kmlType = unmarshal21(reader);
            return process(kmlType, startDate);
        }
        finally {
            reader.close();
        }
    }
}