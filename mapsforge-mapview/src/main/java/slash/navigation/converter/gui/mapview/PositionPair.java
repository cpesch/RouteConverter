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
package slash.navigation.converter.gui.mapview;

import slash.navigation.base.NavigationPosition;

/**
 * A pair of {@link NavigationPosition}s.
 *
 * @author Christian Pesch
 */

public class PositionPair {
    private final NavigationPosition first;
    private final NavigationPosition second;

    public PositionPair(NavigationPosition first, NavigationPosition second) {
        this.first = first;
        this.second = second;
    }

    public NavigationPosition getFirst() {
        return first;
    }

    public NavigationPosition getSecond() {
        return second;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PositionPair positionPair = (PositionPair) o;

        return first.equals(positionPair.first) && second.equals(positionPair.second);
    }

    public int hashCode() {
        int result = first.hashCode();
        result = 31 * result + second.hashCode();
        return result;
    }
}
