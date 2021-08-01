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

package slash.navigation.converter.gui.comparators;

import slash.navigation.common.NavigationPosition;

import java.util.Comparator;

/**
 * Compares {@link NavigationPosition}s by their description.
 *
 * @author Christian Pesch
 */

public class DescriptionComparator implements Comparator<NavigationPosition> {
    private static final Comparator<String> NULL_SAFE_STRING_COMPARATOR = Comparator
            .nullsFirst(String::compareToIgnoreCase);

    private static final Comparator<NavigationPosition> DESCRIPTION_COMPARATOR = Comparator
            .comparing(NavigationPosition::getDescription, NULL_SAFE_STRING_COMPARATOR)
            .thenComparing(NavigationPosition::getDescription, NULL_SAFE_STRING_COMPARATOR);

    public int compare(NavigationPosition p1, NavigationPosition p2) {
        return DESCRIPTION_COMPARATOR.compare(p1, p2);
    }
}
