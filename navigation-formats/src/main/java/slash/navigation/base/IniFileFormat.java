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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Represents INI file section based text route formats.
 *
 * @author Christian Pesch
 */

public abstract class IniFileFormat<R extends BaseRoute> extends TextNavigationFormat<R> {
    protected static final char SECTION_PREFIX = '[';
    protected static final char SECTION_POSTFIX = ']';

    protected static final char NAME_VALUE_SEPARATOR = '=';
    protected static final Pattern NAME_VALUE_PATTERN = Pattern.compile("(.+?)\\s*" + NAME_VALUE_SEPARATOR + "\\s*(.+)");

    protected boolean isNameValue(String line) {
        Matcher matcher = NAME_VALUE_PATTERN.matcher(line);
        return matcher.matches();
    }

    protected String parseName(String line) {
        Matcher matcher = NAME_VALUE_PATTERN.matcher(line);
        if (!matcher.matches())
            throw new IllegalArgumentException("'" + line + "' does not match");
        return matcher.group(1);
    }

    protected String parseValue(String line) {
        Matcher matcher = NAME_VALUE_PATTERN.matcher(line);
        if (!matcher.matches())
            throw new IllegalArgumentException("'" + line + "' does not match");
        return matcher.group(2);
    }

}
