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

package slash.navigation.converter.gui.panels;

import org.junit.Test;

import java.util.HashSet;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Set;

import static java.util.Arrays.asList;
import static java.util.Locale.ENGLISH;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static slash.navigation.converter.gui.panels.ConvertPanel.SPONSOR_FEATURES;

/**
 * Pins the sponsor feature table - the single source driving the write gate in
 * {@link ConvertPanel#checkWriteFormat} and the lock counters sent with the update-check.
 */
public class ConvertPanelSponsorFeatureTest {
    private static final List<String> SPONSOR_FEATURE_IDS =
            asList("fpl-g1000", "msfs-pln", "rt-gorider", "rtz-ecdis");

    @Test
    public void sponsorFeaturesAreInRegistryOrder() {
        // list equality against distinct expected ids also pins uniqueness
        assertEquals(SPONSOR_FEATURE_IDS,
                SPONSOR_FEATURES.stream().map(ConvertPanel.SponsorFeature::name).toList());
    }

    @Test
    public void everySponsorFeatureIdHasABundleDescription() {
        ResourceBundle bundle = ResourceBundle.getBundle(
                "slash/navigation/converter/gui/RouteConverter", ENGLISH);
        for (ConvertPanel.SponsorFeature feature : SPONSOR_FEATURES) {
            // the same key the dialog looks up - a drifted id shows up here as a missing key
            String description = bundle.getString("feature-locked-desc-" + feature.name());
            assertFalse(description.isEmpty());
        }
    }

    @Test
    public void everySponsorFeatureGatesADistinctFormatClass() {
        Set<Class<?>> formatClasses = new HashSet<>();
        for (ConvertPanel.SponsorFeature feature : SPONSOR_FEATURES)
            if (!formatClasses.add(feature.formatClass()))
                throw new AssertionError("duplicate format class: " + feature.formatClass());
        assertEquals(SPONSOR_FEATURES.size(), formatClasses.size());
    }
}
