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

import org.junit.Before;
import org.junit.Test;
import slash.common.prefs.InMemoryPreferences;

import java.util.List;
import java.util.prefs.Preferences;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.junit.Assert.assertEquals;

public class FeatureLocksTest {
    private static final List<String> FEATURES = asList("fpl-g1000", "msfs-pln", "rt-gorider", "rtz-ecdis");

    private Preferences preferences;

    @Before
    public void setUp() {
        preferences = new InMemoryPreferences();
    }

    private void lockShown(String featureName, int count) {
        preferences.putInt("lockShown." + featureName, count);
    }

    private void lockClicked(String featureName, int count) {
        preferences.putInt("lockClicked." + featureName, count);
    }

    private void lockLogin(String featureName, int count) {
        preferences.putInt("lockLogin." + featureName, count);
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
