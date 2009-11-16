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

package slash.navigation.converter.gui.dnd;

/**
 * Provides Drag and Drop helper functions.
 *
 * @author Christian Pesch
 */

public class DnDHelper {
    private static final char SEPARATOR = '\n';

    public static String extractUrl(String string) {
        int index = string.indexOf(SEPARATOR);
        return index == -1 ? string : string.substring(0, index);
    }

    public static String extractDescription(String string) {
        int index = string.indexOf(SEPARATOR);
        return index == -1 ? "" : string.substring(index + 1);
    }
}
