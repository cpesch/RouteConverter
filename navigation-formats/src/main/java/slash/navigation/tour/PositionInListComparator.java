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

package slash.navigation.tour;

import java.util.Comparator;

import static slash.common.io.Transfer.parseInteger;
import static slash.navigation.tour.TourFormat.POSITION_IN_LIST;

/**
 * Compares {@link TourPosition} by the PositionInList attribute.
 *
 * @author Christian Pesch
 */

class PositionInListComparator implements Comparator<TourPosition> {
    private int extractPositionInList(TourPosition position) {
        Integer integer = parseInteger(position.get(POSITION_IN_LIST));
        return integer != null ? integer : position.hashCode();
    }

    public int compare(TourPosition p1, TourPosition p2) {
        return extractPositionInList(p1) - extractPositionInList(p2);
    }
}
