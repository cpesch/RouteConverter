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

import java.util.*;

import static java.util.Collections.enumeration;
import static java.util.Collections.list;

/**
 * Combines several {@link ResourceBundle}s into one.
 *
 * @author Christian Pesch
 */
public class CombinedResourceBundle extends ResourceBundle {
    private final Map<String, String> resources = new HashMap<>();
    private final List<String> bundleNames;

    public CombinedResourceBundle(List<String> bundleNames) {
        this.bundleNames = bundleNames;
    }

    public void load() {
        bundleNames.forEach(bundleName -> {
            // Load English first as a fallback, then let the active-locale bundle override
            // it. There is no no-suffix base bundle (English lives in *_en.properties), so a
            // key present only in the English bundle would otherwise have no fallback and
            // throw MissingResourceException for every other locale - e.g. the list-none /
            // list-and / list-more keys used by DialogStrings on a German UI.
            merge(getBundle(bundleName, Locale.ENGLISH));
            merge(getBundle(bundleName));
        });
    }

    private void merge(ResourceBundle bundle) {
        list(bundle.getKeys()).forEach(key -> resources.put(key, bundle.getString(key)));
    }

    public Object handleGetObject(String key) {
        return resources.get(key);
    }

    public Enumeration<String> getKeys() {
        return enumeration(resources.keySet());
    }
}
