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

import static java.util.Collections.emptyList;

/**
 * The POIs of the selected categories within a bounding box.
 *
 * @param pois      the POIs, nearest to the center of the bounding box first within each group
 * @param truncated true if more POIs matched than the limit allowed to return
 * @author Christian Pesch
 */
public record CategoryPoiResult(List<CategoryPoi> pois, boolean truncated) {
    public static final CategoryPoiResult EMPTY = new CategoryPoiResult(emptyList(), false);
}
