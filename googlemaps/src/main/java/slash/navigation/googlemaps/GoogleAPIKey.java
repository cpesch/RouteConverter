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
import static slash.common.io.Transfer.trim;

/**
 * Manages the API Keys for the Google Directions, Elevation, Geocoding, JavaScript API.
 *
 * @author Christian Pesch
 */

public class GoogleAPIKey {
    private static final Preferences preferences = Preferences.userNodeForPackage(GoogleAPIKey.class);
    private static final Logger log = Logger.getLogger(GoogleService.class.getName());
    private static final String GOOGLE_API_KEY_PREFERENCE = "googleMapsApiKey";
    private static final String GOOGLE_API_USAGES = "googleMapsApiUsages";

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
            log.severe("Could not get Google API usages: " + getLocalizedMessage(e));
        }
        log.info("Google API key: " + useAPIKey("usage") + " usage: " + builder.toString());
    }

    public static String getAPIKeyPreference() {
        return preferences.get(GOOGLE_API_KEY_PREFERENCE, "");
    }

    public static void setAPIKeyPreference(String apiKey) {
        preferences.put(GOOGLE_API_KEY_PREFERENCE, apiKey);
    }

    public static String useAPIKey(String apiType) {
        count(preferences, GOOGLE_API_USAGES + "-" + apiType);
        String apiKey = trim(getAPIKeyPreference());
        return apiKey != null ? apiKey : getDefaultAPIKey();
    }

    private static String getDefaultAPIKey() {
        try (InputStream inputStream = GoogleAPIKey.class.getResourceAsStream("google.properties")) {
            Properties properties = new Properties();
            properties.load(inputStream);
            String property = properties.getProperty(GOOGLE_API_KEY_PREFERENCE);
            // for releases
            if(property != null && !property.toLowerCase().contains(GOOGLE_API_KEY_PREFERENCE.toLowerCase()))
                return property;
        }
        catch (IOException e) {
            log.severe("Could not read default Google API Key: " + getLocalizedMessage(e));
        }
        // for tests and development
        return "AIzaSyDC-jYvmmirX_V_gAI7PUPY7Myhyri6U_Q";
    }
}
