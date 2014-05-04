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

package slash.navigation.base;

import java.util.List;

/**
 * The base of all XML based navigation formats.
 *
 * @author Christian Pesch
 */

public abstract class XmlNavigationFormat<R extends BaseRoute> extends BaseNavigationFormat<R> {
    public static final String HEADER_LINE = "<!-- " + GENERATED_BY + " -->\n";

    public int getMaximumPositionCount() {
        return UNLIMITED_MAXIMUM_POSITION_COUNT;
    }

    protected String asDescription(List<String> strings) {
        StringBuilder buffer = new StringBuilder();
        if (strings != null) {
            for (String string : strings) {
                buffer.append(string).append(",\n");
            }
        }
        if (buffer.indexOf(GENERATED_BY) == -1)
            buffer.append(GENERATED_BY);
        return buffer.toString();
    }
}
