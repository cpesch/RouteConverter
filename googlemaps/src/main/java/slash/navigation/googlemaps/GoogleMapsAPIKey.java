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
package slash.navigation.googlemaps;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Logger;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import static java.lang.String.format;
import static slash.common.helpers.ExceptionHelper.getLocalizedMessage;
import static slash.common.helpers.PreferencesHelper.count;

/**
 * Manages the API Keys for the Google Maps API.
 *
 * @author Christian Pesch
 */

public class GoogleMapsAPIKey {
    private static final Preferences preferences = Preferences.userNodeForPackage(GoogleMapsAPIKey.class);
    private static final Logger log = Logger.getLogger(GoogleMapsService.class.getName());
    private static final String GOOGLE_MAPS_API_KEY_PREFERENCE = "googleMapsApiKey";
    private static final String GOOGLE_MAPS_API_USAGES = "googleMapsApiUsages";

    public static void logUsage() {
        StringBuilder builder = new StringBuilder();
        try {
            for (String key : preferences.keys()) {
                int count = preferences.getInt(key, 0);
                if(count > 0) {
                    int index = key.indexOf('-');
                    String action = index != -1 ? key.substring(index + 1) : key;
                    builder.append(format("%n%s, count: %d", action, count));
                }
            }
        } catch (BackingStoreException e) {
            log.severe("Could not get preferences keys: " + getLocalizedMessage(e));
        }
        log.info("Google Maps API usage:" + builder.toString());
    }

    private static final String REMOVED_GOOGLE_MAPS_API_KEY_PREFERENCE = "removedGoogleMapsApiKey";

    public static String getAPIKey(String action) {
        count(preferences, GOOGLE_MAPS_API_USAGES + "-" + action);
        if(!preferences.getBoolean(REMOVED_GOOGLE_MAPS_API_KEY_PREFERENCE, false)) {
            preferences.remove(GOOGLE_MAPS_API_KEY_PREFERENCE);
            preferences.putBoolean(REMOVED_GOOGLE_MAPS_API_KEY_PREFERENCE, true);
        }
        return preferences.get(GOOGLE_MAPS_API_KEY_PREFERENCE, readAPIKey());
    }

    private static String readAPIKey() {
        try (InputStream inputStream = GoogleMapsAPIKey.class.getResourceAsStream("googlemaps.properties")) {
            Properties properties = new Properties();
            properties.load(inputStream);
            String property = properties.getProperty(GOOGLE_MAPS_API_KEY_PREFERENCE);
            if(property != null && !property.contains(GOOGLE_MAPS_API_KEY_PREFERENCE))
                return property;
        }
        catch (IOException e) {
            log.severe("Could not read GoogleMaps API Key: " + getLocalizedMessage(e));
        }
        return "AIzaSyBa8PNFRv02fg1Dv_G64SfoRxfytBFKxJw";
    }
}
