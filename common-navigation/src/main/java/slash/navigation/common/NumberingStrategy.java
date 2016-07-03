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

package slash.navigation.common;

/**
 * Enumeration of the strategies where the Index 1 is for adding numbers to the descriptions of positions.
 *
 * Absolute_Position_Within_Position_List: First position of the position list
 *
 * Relative_Position_In_Current_Selection: First position of the current selection
 *
 * @author Christian Pesch
 */

public enum NumberingStrategy {
    Absolute_Position_Within_Position_List, Relative_Position_In_Current_Selection
}
