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
package slash.navigation.gui.helpers;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.MissingResourceException;

import static java.util.Arrays.asList;
import static java.util.Collections.list;
import static org.junit.Assert.*;

/**
 * Tests for {@link CombinedResourceBundle}.
 *
 * @author Christian Pesch
 */
public class CombinedResourceBundleTest {
    private static final String BUNDLE_1 = "slash.navigation.gui.helpers.testbundle1";
    private static final String BUNDLE_2 = "slash.navigation.gui.helpers.testbundle2";

    private static CombinedResourceBundle load(String... bundleNames) {
        CombinedResourceBundle bundle = new CombinedResourceBundle(asList(bundleNames));
        bundle.load();
        return bundle;
    }

    @Test
    public void mergesKeysFromAllBundles() {
        CombinedResourceBundle bundle = load(BUNDLE_1, BUNDLE_2);

        assertEquals("a1", bundle.getString("key.a"));
        assertEquals("b2", bundle.getString("key.b"));
    }

    @Test
    public void laterBundleOverridesEarlierOnSharedKey() {
        assertEquals("shared2", load(BUNDLE_1, BUNDLE_2).getString("key.shared"));
        assertEquals("shared1", load(BUNDLE_2, BUNDLE_1).getString("key.shared"));
    }

    @Test
    public void getKeysExposesEveryMergedKey() {
        CombinedResourceBundle bundle = load(BUNDLE_1, BUNDLE_2);

        List<String> keys = new ArrayList<>(list(bundle.getKeys()));
        Collections.sort(keys);

        assertEquals(asList("key.a", "key.b", "key.shared"), keys);
    }

    @Test
    public void unknownKeyThrowsMissingResource() {
        CombinedResourceBundle bundle = load(BUNDLE_1);

        assertThrows(MissingResourceException.class, () -> bundle.getString("key.does.not.exist"));
    }

    @Test
    public void emptyBundleListYieldsNoKeys() {
        CombinedResourceBundle bundle = load();

        assertFalse(bundle.getKeys().hasMoreElements());
    }
}
