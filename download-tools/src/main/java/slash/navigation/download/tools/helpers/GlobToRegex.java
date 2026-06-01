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
package slash.navigation.download.tools.helpers;

/**
 * Converts a wget-style filename glob into an equivalent regular expression.
 * Examples: {@code *.zip}, {@code *-latest.osm.pbf}, or a star-slash basename pattern.
 * Used by ScanWebsite to apply source include/exclude patterns through the existing
 * regex-based {@link AnchorFilter}.
 *
 * @author Christian Pesch
 */
public class GlobToRegex {

    public static String convert(String glob) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < glob.length(); i++) {
            char c = glob.charAt(i);
            switch (c) {
                case '*':
                    result.append(".*");
                    break;
                case '?':
                    result.append('.');
                    break;
                case '.':
                case '\\':
                case '(':
                case ')':
                case '[':
                case ']':
                case '{':
                case '}':
                case '+':
                case '^':
                case '$':
                case '|':
                    result.append('\\').append(c);
                    break;
                default:
                    result.append(c);
            }
        }
        return result.toString();
    }
}
