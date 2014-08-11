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

package slash.navigation.earthtools;

import slash.navigation.common.LongitudeAndLatitude;
import slash.navigation.elevation.ElevationService;
import slash.navigation.earthtools.binding.Height;
import slash.navigation.rest.Get;

import java.io.IOException;
import java.util.List;
import java.util.prefs.Preferences;

import static slash.common.io.Transfer.parseInt;
import static slash.navigation.earthtools.EarthToolsUtil.unmarshal;

/**
 * Encapsulates REST access to the earthtools.org service.
 *
 * @author Christian Pesch
 */

public class EarthToolsService implements ElevationService {
    private static final Preferences preferences = Preferences.userNodeForPackage(EarthToolsService.class);
    private static final String GEONAMES_URL_PREFERENCE = "earthtoolsUrl";

    private static String getEarthToolsUrlPreference() {
        return preferences.get(GEONAMES_URL_PREFERENCE, "http://www.earthtools.org/");
    }

    public String getName() {
        return "EarthTools";
    }

    public Double getElevationFor(double longitude, double latitude) throws IOException {
        Get get = new Get(getEarthToolsUrlPreference() + "height/" + latitude + "/" + longitude);
        String result = get.executeAsString();
        if (get.isSuccessful())
            try {
                Height height = unmarshal(result);
                Integer elevation = parseInt(height.getMeters());
                if (elevation != null && !elevation.equals(-9999))
                    return elevation.doubleValue();
            } catch (Exception e) {
                throw new IOException("Cannot unmarshall " + result + ": " + e, e);
            }
        return null;
    }

    public boolean isDownload() {
        return false;
    }

    public String getPath() {
        throw new UnsupportedOperationException();
    }

    public void setPath(String path) {
        throw new UnsupportedOperationException();
    }

    public void downloadElevationDataFor(List<LongitudeAndLatitude> longitudeAndLatitudes) {
        throw new UnsupportedOperationException();
    }
}