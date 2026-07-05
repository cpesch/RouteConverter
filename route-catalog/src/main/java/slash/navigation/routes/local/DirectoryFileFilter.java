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

package slash.navigation.routes.local;

import slash.common.io.MacAlias;
import slash.common.io.WindowsShortcut;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;

/**
 * A file filter that accepts only directories not starting with a dot plus the
 * files that may resolve to directories: Windows shortcuts and macOS aliases.
 *
 * @author Christian Pesch
 */
public class DirectoryFileFilter implements FileFilter {
    public boolean accept(File file) {
        return file.isDirectory() && !file.getName().startsWith(".") ||
                file.isFile() && (isLink(file) || isAlias(file));
    }

    private static boolean isLink(File file) {
        try {
            return WindowsShortcut.isPotentialValidLink(file);
        } catch (IOException e) {
            return false;
        }
    }

    private static boolean isAlias(File file) {
        try {
            return MacAlias.isPotentialValidAlias(file);
        } catch (IOException e) {
            return false;
        }
    }
}
