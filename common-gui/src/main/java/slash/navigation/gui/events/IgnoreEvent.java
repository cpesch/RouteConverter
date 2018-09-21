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
package slash.navigation.gui.events;

import javax.swing.event.ListDataEvent;

import static java.lang.Integer.MIN_VALUE;
import static javax.swing.event.ListDataEvent.CONTENTS_CHANGED;

/**
 * Ignore events in FormatAndRoutesModel and CharacteristicsModel.
 *
 * @author Christian Pesch
 */

public class IgnoreEvent {
    public static final int IGNORE = MIN_VALUE;

    public static boolean isIgnoreEvent(ListDataEvent e) {
        return e.getType() == CONTENTS_CHANGED && e.getIndex0() == IGNORE && e.getIndex1() == IGNORE;
    }
}
