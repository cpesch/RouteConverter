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
package slash.navigation.pois.mapsforge;

import java.util.List;

/**
 * A node of the category tree of an offline POI source: a category of a {@code .poi} database,
 * or for a {@code .map} file a tag key like {@code amenity} with its values like
 * {@code amenity=bank} as children. Selecting a key selects the category with its whole subtree.
 *
 * @param key      identifies the category within its source, used for selection and persistence
 * @param label    what the user sees
 * @param children the subcategories, empty for a leaf
 * @author Christian Pesch
 */
public record PoiCategory(String key, String label, List<PoiCategory> children) {
}
