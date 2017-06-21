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
package slash.navigation.columbus;

import slash.navigation.base.NavigationFormat;
import slash.navigation.base.NavigationFormatRegistry;
import slash.navigation.gpx.GpxFormat;
import slash.navigation.kml.Igo8RouteFormat;
import slash.navigation.kml.Kml20Format;
import slash.navigation.kml.Kml21Format;
import slash.navigation.kml.Kml22BetaFormat;
import slash.navigation.kml.Kml22Format;
import slash.navigation.kml.KmzFormat;
import slash.navigation.nmea.NmeaFormat;

/**
 * Managed the navigation formats for the RouteConverter Columbus Edition.
 *
 * @author Christian Pesch
 */
public class ColumbusNavigationFormatRegistry extends NavigationFormatRegistry {
    protected boolean includeReadFormat(NavigationFormat format) {
        return format instanceof ColumbusGpsBinaryFormat ||
                format instanceof ColumbusGpsFormat ||
                format instanceof GpxFormat ||
                format instanceof Kml20Format ||
                format instanceof Kml21Format ||
                format instanceof Kml22BetaFormat ||
                format instanceof Kml22Format && !(format instanceof Igo8RouteFormat) ||
                format instanceof KmzFormat ||
                format instanceof NmeaFormat;
    }
}
