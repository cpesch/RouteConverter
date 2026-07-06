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

package slash.navigation.gui.models;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.prefs.AbstractPreferences;

/**
 * A minimal, fully in-memory {@link java.util.prefs.Preferences} node for tests.
 * Isolated per instance, so it does not touch the shared OS-level backing store
 * that loses writes under concurrent multi-fork test execution.
 *
 * @author Christian Pesch
 */
class InMemoryPreferences extends AbstractPreferences {
    private final Map<String, String> values = new ConcurrentHashMap<>();
    private final Map<String, InMemoryPreferences> children = new ConcurrentHashMap<>();

    InMemoryPreferences() {
        super(null, "");
    }

    private InMemoryPreferences(InMemoryPreferences parent, String name) {
        super(parent, name);
    }

    protected void putSpi(String key, String value) {
        values.put(key, value);
    }

    protected String getSpi(String key) {
        return values.get(key);
    }

    protected void removeSpi(String key) {
        values.remove(key);
    }

    protected void removeNodeSpi() {
        values.clear();
        children.clear();
    }

    protected String[] keysSpi() {
        return values.keySet().toArray(new String[0]);
    }

    protected String[] childrenNamesSpi() {
        return children.keySet().toArray(new String[0]);
    }

    protected AbstractPreferences childSpi(String name) {
        return children.computeIfAbsent(name, n -> new InMemoryPreferences(this, n));
    }

    protected void syncSpi() {
    }

    protected void flushSpi() {
    }
}
