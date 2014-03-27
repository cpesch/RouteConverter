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

package slash.navigation.converter.gui.augment;

import slash.common.type.CompactCalendar;

/**
 * Interface for a component that augments a position with elevation, postal address
 * and time information.
 *
 * @author Christian Pesch
 */

public interface PositionAugmenter {
    String createDescription(int index);
    void complementDescription(int row, Double longitude, Double latitude);
    void complementElevation(int row, Double longitude, Double latitude);
    void complementTime(int row, final CompactCalendar time, boolean allowCurrentTime);
    void interrupt();
}
