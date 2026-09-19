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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import slash.common.prefs.InMemoryPreferences;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.prefs.Preferences;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Tests for {@link APIKeyRegistry}.
 *
 * @author Christian Pesch
 */

public class APIKeyRegistryTest {
    private static final String API_KEY_PREFERENCE = "ApiKey";
    private static final String API_USAGES = "ApiUsages";

    private final Preferences preferences = new InMemoryPreferences();

    @BeforeEach
    public void setUp() {
        APIKeyRegistry.setPreferences(preferences);
    }

    @AfterEach
    public void tearDown() {
        APIKeyRegistry.setPreferences(Preferences.userNodeForPackage(APIKeyRegistry.class));
    }

    @Test
    public void testGetAPIKeyReturnsPreferenceWhenSet() {
        APIKeyRegistry registry = APIKeyRegistry.getInstance();
        registry.setAPIKeyPreference("mapbox", "stored-api-key");
        assertEquals("stored-api-key", registry.getAPIKey("mapbox", "test"));
    }

    @Test
    public void testGetAPIKeyCountsUsagesInPreferences() {
        APIKeyRegistry registry = APIKeyRegistry.getInstance();
        registry.getAPIKey("mapbox", "test");
        registry.getAPIKey("mapbox", "test");
        assertEquals(2, preferences.getInt("mapbox" + API_USAGES + "-test", 0));
    }

    @Test
    public void testGetAPIKeyFallsBackToPropertiesFileWhenUnset() throws IOException {
        APIKeyRegistry registry = APIKeyRegistry.getInstance();
        assertEquals("", registry.getAPIKeyPreference("geonames"));
        assertEquals(expectedDefaultAPIKey("geonames"), registry.getAPIKey("geonames", "test"));
    }

    @Test
    public void testGetAPIKeyOfUnknownServiceIsNull() {
        assertNull(APIKeyRegistry.getInstance().getAPIKey("notaregisteredservice", "test"));
    }

    @Test
    public void testSetAPIKeyPreferenceWithNullStoresEmptyString() {
        APIKeyRegistry registry = APIKeyRegistry.getInstance();
        registry.setAPIKeyPreference("mapbox", null);
        assertEquals("", registry.getAPIKeyPreference("mapbox"));
    }

    // mirrors getDefaultAPIKey: apikey.properties is Maven-filtered, so a plain
    // build keeps the ${...} placeholder, which counts as unset, while the CI
    // build filters in a real key
    private String expectedDefaultAPIKey(String serviceName) throws IOException {
        try (InputStream inputStream = APIKeyRegistry.class.getResourceAsStream("apikey.properties")) {
            Properties properties = new Properties();
            properties.load(inputStream);
            String property = properties.getProperty(serviceName + API_KEY_PREFERENCE);
            if (property != null && !property.toLowerCase().contains((serviceName + API_KEY_PREFERENCE).toLowerCase()))
                return property;
        }
        return null;
    }
}
