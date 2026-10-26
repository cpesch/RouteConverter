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
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static slash.common.helpers.APIKeyRegistry.isPlainAPIKey;

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
    public void testHasAPIKeyIsFalseForUnknownService() {
        assertFalse(APIKeyRegistry.getInstance().hasAPIKey("nosuchservice"));
    }

    @Test
    public void testHasAPIKeyIsTrueAfterSettingPreference() {
        APIKeyRegistry registry = APIKeyRegistry.getInstance();
        registry.setAPIKeyPreference("nosuchservice", "k");
        assertTrue(registry.hasAPIKey("nosuchservice"));
    }

    @Test
    public void testHasAPIKeyIsFalseForBlankPreference() {
        APIKeyRegistry registry = APIKeyRegistry.getInstance();
        registry.setAPIKeyPreference("nosuchservice", "  ");
        assertFalse(registry.hasAPIKey("nosuchservice"));
    }

    @Test
    public void testHasAPIKeyDoesNotCountAUsage() throws BackingStoreException {
        APIKeyRegistry registry = APIKeyRegistry.getInstance();
        registry.setAPIKeyPreference("nosuchservice", "k");
        registry.hasAPIKey("nosuchservice");
        for (String key : preferences.keys())
            assertFalse(key.startsWith("nosuchservice" + API_USAGES), key);
    }

    @Test
    public void testSetAPIKeyPreferenceWithNullStoresEmptyString() {
        APIKeyRegistry registry = APIKeyRegistry.getInstance();
        registry.setAPIKeyPreference("mapbox", null);
        assertEquals("", registry.getAPIKeyPreference("mapbox"));
    }

    @Test
    public void testLogUsageNamesSourceOfKeyInsteadOfKey() {
        APIKeyRegistry registry = APIKeyRegistry.getInstance();
        registry.setAPIKeyPreference("mapbox", "secret-api-key");
        registry.getAPIKey("mapbox", "tiles");

        List<String> messages = logMessages(registry::logUsage);

        assertEquals(1, messages.size());
        assertTrue(messages.get(0).startsWith("mapbox API key: own usage:"), messages.get(0));
        assertFalse(messages.get(0).contains("secret-api-key"), messages.get(0));
    }

    @Test
    public void testIsPlainAPIKeyRejectsKeysThatLookLikeAUrlOrTemplate() {
        assertFalse(isPlainAPIKey("https://attacker.example/?apikey=1"));
        assertFalse(isPlainAPIKey("key/with/slashes"));
        assertFalse(isPlainAPIKey("key:with:colons"));
        assertFalse(isPlainAPIKey("key with space"));
        assertFalse(isPlainAPIKey("{apikey}"));
    }

    @Test
    public void testIsPlainAPIKeyAcceptsPlainTokens() {
        assertTrue(isPlainAPIKey(null));
        assertTrue(isPlainAPIKey("abcDEF123-_."));
    }

    @Test
    public void testGetAPIKeyWarnsOnceForNonPlainKeyAndStillReturnsIt() {
        APIKeyRegistry registry = APIKeyRegistry.getInstance();
        String nonPlainAPIKey = "https://tile.example/{z}/{x}/{y}.png?apikey=secret";
        // the registry is a singleton that remembers which services it warned about
        registry.setAPIKeyPreference("nonplainservice", nonPlainAPIKey);

        List<String> messages = logMessages(() -> {
            assertEquals(nonPlainAPIKey, registry.getAPIKey("nonplainservice", "map"));
            assertEquals(nonPlainAPIKey, registry.getAPIKey("nonplainservice", "map"));
        });

        assertEquals(1, messages.size());
        assertTrue(messages.get(0).startsWith("nonplainservice API key is not a plain token"), messages.get(0));
        assertFalse(messages.get(0).contains("secret"), messages.get(0));
    }

    @Test
    public void testGetAPIKeyDoesNotWarnForPlainKey() {
        APIKeyRegistry registry = APIKeyRegistry.getInstance();
        registry.setAPIKeyPreference("plainservice", "abcDEF123-_.");

        List<String> messages = logMessages(() -> registry.getAPIKey("plainservice", "map"));

        assertTrue(messages.isEmpty(), messages.toString());
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

    private static List<String> logMessages(Runnable runnable) {
        List<String> messages = new ArrayList<>();
        Logger logger = Logger.getLogger(APIKeyRegistry.class.getName());
        Handler handler = new Handler() {
            public void publish(LogRecord record) {
                messages.add(record.getMessage());
            }

            public void flush() {
            }

            public void close() {
            }
        };
        logger.addHandler(handler);
        try {
            runnable.run();
        } finally {
            logger.removeHandler(handler);
        }
        return messages;
    }
}
