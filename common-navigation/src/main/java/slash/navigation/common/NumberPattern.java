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
 * Enumeration of the ways to add numbers to the descriptions of positions.
 *
 * Description_Only: Hamburg
 *
 * Number_Only: 1234
 *
 * Number_Directly_Followed_By_Description: 1234Hamburg
 *
 * Number_Space_Then_Description: 1234 Hamburg
 *
 * @author Christian Pesch
 */

public enum NumberPattern {
    Description_Only, Number_Only, Number_Directly_Followed_By_Description, Number_Space_Then_Description
}
