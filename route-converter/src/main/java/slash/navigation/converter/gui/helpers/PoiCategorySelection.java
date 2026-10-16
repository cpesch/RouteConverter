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
package slash.navigation.converter.gui.helpers;

import slash.navigation.pois.mapsforge.PoiCategory;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * The categories selected for the POI overlay. A selected category stands for its whole subtree,
 * so only the topmost selected keys are kept: checking {@code Amenities} stores one key instead of
 * the hundreds of its subcategories, which keeps the selection within the size of a preference.
 * A category is addressed by its path from the top-level category down to it.
 *
 * @author Christian Pesch
 */

public class PoiCategorySelection {
    private final Set<String> keys;

    public PoiCategorySelection(Set<String> keys) {
        this.keys = new LinkedHashSet<>(keys);
    }

    public Set<String> getKeys() {
        return Set.copyOf(keys);
    }

    public boolean isSelected(List<PoiCategory> path) {
        for (PoiCategory category : path) {
            if (keys.contains(category.key()))
                return true;
        }
        return false;
    }

    public boolean isPartiallySelected(List<PoiCategory> path) {
        return !isSelected(path) && containsSelectedDescendant(path.get(path.size() - 1));
    }

    private boolean containsSelectedDescendant(PoiCategory category) {
        for (PoiCategory child : category.children()) {
            if (keys.contains(child.key()) || containsSelectedDescendant(child))
                return true;
        }
        return false;
    }

    public void toggle(List<PoiCategory> path) {
        if (isSelected(path))
            deselect(path);
        else
            select(path);
    }

    private void select(List<PoiCategory> path) {
        PoiCategory category = path.get(path.size() - 1);
        removeDescendants(category);
        keys.add(category.key());
    }

    private void deselect(List<PoiCategory> path) {
        PoiCategory category = path.get(path.size() - 1);
        keys.remove(category.key());
        removeDescendants(category);

        // a selected ancestor stands for this category, too: replace it by the siblings
        // along the path, so the rest of its subtree stays selected
        int selectedAncestor = -1;
        for (int i = path.size() - 2; i >= 0; i--) {
            if (keys.remove(path.get(i).key()))
                selectedAncestor = i;
        }
        if (selectedAncestor == -1)
            return;

        for (int i = selectedAncestor; i < path.size() - 1; i++) {
            String next = path.get(i + 1).key();
            for (PoiCategory child : path.get(i).children()) {
                if (!child.key().equals(next))
                    keys.add(child.key());
            }
        }
    }

    private void removeDescendants(PoiCategory category) {
        for (PoiCategory child : category.children()) {
            keys.remove(child.key());
            removeDescendants(child);
        }
    }
}
