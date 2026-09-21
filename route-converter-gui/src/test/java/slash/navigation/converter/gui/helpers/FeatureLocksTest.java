/*
    This file is part of BaseRouteConverter.

    BaseRouteConverter is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    BaseRouteConverter is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with BaseRouteConverter; if not, write to the Free Software
    Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA

    Copyright (C) 2007 Christian Pesch. All Rights Reserved.
*/

package slash.navigation.converter.gui.helpers;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class FeatureLocksTest {
    private static final Set<String> USED_PREFERENCE_NODE_PATHS = new HashSet<>();
    private static final List<String> FEATURES = asList("fpl-g1000", "msfs-pln", "rt-gorider", "rtz-ecdis");

    private Preferences preferences;
    private String preferencesPath;

    @Before
    public void setUp() throws Exception {
        preferences = Preferences.userRoot().node("/RouteConverter-test/" + getClass().getName() + "/" + UUID.randomUUID());
        preferencesPath = preferences.absolutePath();
        assertTrue("preference node path was reused across tests: " + preferencesPath,
                USED_PREFERENCE_NODE_PATHS.add(preferencesPath));
        preferences.flush();
    }

    @After
    public void tearDown() throws Exception {
        try {
            preferences.removeNode();
            preferences.flush();
        } catch (BackingStoreException e) {
            fail("failed to remove and flush preference node " + preferencesPath + ": " + e.getMessage());
        }
    }

    private void lockShown(String featureName, int count) {
        preferences.putInt("lockShown." + featureName, count);
        flushPreferences();
    }

    private void lockClicked(String featureName, int count) {
        preferences.putInt("lockClicked." + featureName, count);
        flushPreferences();
    }

    private void lockLogin(String featureName, int count) {
        preferences.putInt("lockLogin." + featureName, count);
        flushPreferences();
    }

    private void flushPreferences() {
        try {
            preferences.flush();
        } catch (BackingStoreException e) {
            fail("failed to flush preference node " + preferencesPath + ": " + e.getMessage());
        }
    }

    @Test
    public void encodesNothingWhenNoCountersExist() {
        assertEquals("", FeatureLocks.encode(preferences, FEATURES));
    }

    @Test
    public void encodesNothingForEmptyFeatureList() {
        lockShown("fpl-g1000", 3);
        assertEquals("", FeatureLocks.encode(preferences, emptyList()));
    }

    @Test
    public void encodesShownClickedAndLoginCounts() {
        lockShown("fpl-g1000", 3);
        lockClicked("fpl-g1000", 1);
        lockLogin("fpl-g1000", 2);
        assertEquals("fpl-g1000:s3,c1,l2", FeatureLocks.encode(preferences, FEATURES));
    }

    @Test
    public void omitsFeaturesWithoutShownCount() {
        lockShown("fpl-g1000", 1);
        lockClicked("msfs-pln", 2);
        lockLogin("rtz-ecdis", 3);
        assertEquals("fpl-g1000:s1,c0,l0", FeatureLocks.encode(preferences, FEATURES));
    }

    @Test
    public void encodesMultipleFeaturesSeparatedBySemicolon() {
        lockShown("fpl-g1000", 3);
        lockClicked("fpl-g1000", 1);
        lockShown("msfs-pln", 1);
        assertEquals("fpl-g1000:s3,c1,l0;msfs-pln:s1,c0,l0", FeatureLocks.encode(preferences, FEATURES));
    }

    @Test
    public void encodesInListOrderNotInPreferenceStoreOrder() {
        lockShown("rtz-ecdis", 2);
        lockShown("fpl-g1000", 5);
        // a permuted list permutes the output - the preference store order plays no role
        assertEquals("rtz-ecdis:s2,c0,l0;fpl-g1000:s5,c0,l0",
                FeatureLocks.encode(preferences, asList("rtz-ecdis", "fpl-g1000")));
        // registry order, i.e. what ConvertPanel hands over, keeps fpl-g1000 first
        assertEquals("fpl-g1000:s5,c0,l0;rtz-ecdis:s2,c0,l0", FeatureLocks.encode(preferences, FEATURES));
    }

    @Test
    public void encodesOnlyTheFeaturesOfTheGivenList() {
        lockShown("rt-gorider", 7);
        lockShown("msfs-pln", 4);
        assertEquals("msfs-pln:s4,c0,l0", FeatureLocks.encode(preferences, asList("msfs-pln")));
    }
}
