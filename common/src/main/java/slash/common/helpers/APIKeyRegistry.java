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
package slash.common.helpers;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Logger;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import static java.lang.String.format;
import static slash.common.helpers.ExceptionHelper.getLocalizedMessage;
import static slash.common.helpers.PreferencesHelper.count;
import static slash.common.io.Transfer.trim;

/**
 * Manages API Keys via the preferences.
 *
 * @author Christian Pesch
 */

public class APIKeyRegistry {
    private static final Preferences preferences = Preferences.userNodeForPackage(APIKeyRegistry.class);
    private static final Logger log = Logger.getLogger(APIKeyRegistry.class.getName());
    private static final String API_KEY_PREFERENCE = "ApiKey";
    private static final String API_USAGES = "ApiUsages";

    private static APIKeyRegistry instance = new APIKeyRegistry();

    private APIKeyRegistry() {
    }

    public static synchronized APIKeyRegistry getInstance() {
        if (instance == null)
            instance = new APIKeyRegistry();
        return instance;
    }

    private Set<String> determineServiceNames() throws BackingStoreException {
        Set<String> result = new HashSet<>();
        for (String key : preferences.keys()) {
            if(key.endsWith(API_KEY_PREFERENCE))
                result.add(key.substring(0, key.length() - API_KEY_PREFERENCE.length()));
        }
        return result;
    }

    public void logUsage() {
        try {
            for (String serviceName : determineServiceNames()) {

                StringBuilder builder = new StringBuilder();
                for (String key : preferences.keys()) {
                    if(!key.startsWith(serviceName))
                        continue;

                    int count = preferences.getInt(key, 0);
                    if (count > 0) {
                        int index = key.indexOf('-');
                        String action = index != -1 ? key.substring(index + 1) : key;
                        builder.append(format("%n%s, count: %d", action, count));
                    }
                }

                String apiKey = getAPIKey(serviceName, "usage");
                if (apiKey != null)
                    log.info(serviceName + " API key: " + apiKey + " usage:" + builder.toString());
            }

        } catch (BackingStoreException e) {
            log.severe("Could not log API usages: " + getLocalizedMessage(e));
        }
    }

    public String getAPIKeyPreference(String serviceName) {
        return preferences.get(serviceName + API_KEY_PREFERENCE, "");
    }

    public void setAPIKeyPreference(String serviceName, String apiKey) {
        preferences.put(serviceName + API_KEY_PREFERENCE, apiKey != null ? apiKey : "");
    }

    public String getAPIKey(String serviceName, String apiType) {
        count(preferences, serviceName + API_USAGES + "-" + apiType);
        String apiKey = trim(getAPIKeyPreference(serviceName));
        return apiKey != null ? apiKey : getDefaultAPIKey(serviceName);
    }

    private String getDefaultAPIKey(String serviceName) {
        try (InputStream inputStream = APIKeyRegistry.class.getResourceAsStream("apikey.properties")) {
            Properties properties = new Properties();
            properties.load(inputStream);
            String property = properties.getProperty(serviceName + API_KEY_PREFERENCE);
            if(property != null && !property.toLowerCase().contains((serviceName + API_KEY_PREFERENCE).toLowerCase()))
                return property;
        }
        catch (IOException e) {
            log.severe("Could not read default Google API Key: " + getLocalizedMessage(e));
        }
        return null;
    }
}
