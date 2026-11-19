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

    Copyright (C) 2026 Christian Pesch. All Rights Reserved.
*/

package slash.navigation.base;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static slash.navigation.base.FormatClassNameAliases.migrateCounters;
import static slash.navigation.base.FormatClassNameAliases.resolve;

/**
 * Tests for {@link FormatClassNameAliases}.
 *
 * @author Christian Pesch
 */

public class FormatClassNameAliasesTest {
    private static final String OLD_NAME = "slash.navigation.babel.GeoCachingFormat";
    private static final String LOADABLE_OLD_NAME = "test.old.Format";
    private static final String LOADABLE_NEW_NAME = "java.lang.Object";
    private static final Map<String, String> LOADABLE_TABLE = Map.of(LOADABLE_OLD_NAME, LOADABLE_NEW_NAME);

    private Preferences preferences;

    @BeforeEach
    public void setUp() {
        preferences = Preferences.userRoot().node("slash/navigation/test-aliases-" + UUID.randomUUID());
    }

    @AfterEach
    public void tearDown() throws BackingStoreException {
        preferences.removeNode();
    }

    @Test
    public void resolveReturnsUnknownNameUnchanged() {
        assertEquals("some.Unknown", resolve("some.Unknown"));
    }

    @Test
    public void resolveReturnsNullUnchanged() {
        assertNull(resolve(null));
    }

    @Test
    public void resolveReturnsEmptyUnchanged() {
        assertEquals("", resolve(""));
    }

    @Test
    public void resolveReturnsRealTableEntryUnchangedSinceNoTargetExistsYet() {
        assertEquals(OLD_NAME, resolve(OLD_NAME));
    }

    @Test
    public void resolveMapsToLoadableTarget() {
        assertEquals(LOADABLE_NEW_NAME, resolve(LOADABLE_OLD_NAME, LOADABLE_TABLE));
    }

    @Test
    public void migrateCountersAddsOldValueIntoNewAndRemovesOldKey() {
        String prefix = "readCount";
        preferences.putInt(prefix + LOADABLE_OLD_NAME, 3);
        preferences.putInt(prefix + LOADABLE_NEW_NAME, 2);

        migrateCounters(preferences, LOADABLE_TABLE, prefix);

        assertEquals(5, preferences.getInt(prefix + LOADABLE_NEW_NAME, 0));
        assertNull(preferences.get(prefix + LOADABLE_OLD_NAME, null));
    }

    @Test
    public void migrateCountersIsIdempotent() {
        String prefix = "readCount";
        preferences.putInt(prefix + LOADABLE_OLD_NAME, 3);
        preferences.putInt(prefix + LOADABLE_NEW_NAME, 2);

        migrateCounters(preferences, LOADABLE_TABLE, prefix);
        migrateCounters(preferences, LOADABLE_TABLE, prefix);

        assertEquals(5, preferences.getInt(prefix + LOADABLE_NEW_NAME, 0));
        assertNull(preferences.get(prefix + LOADABLE_OLD_NAME, null));
    }
}
