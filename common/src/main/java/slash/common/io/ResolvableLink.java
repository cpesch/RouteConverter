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

package slash.common.io;

/**
 * A filesystem indirection that points at another file or directory: a Windows
 * shortcut ({@link WindowsShortcut}) or a macOS alias ({@link MacAlias}).
 * Use {@link Files#resolveLink(java.io.File)} to obtain one.
 *
 * @author Christian Pesch
 */
public interface ResolvableLink {
    /**
     * @return true if the link points to a directory
     */
    boolean isDirectory();

    /**
     * @return true if the link points to a file
     */
    boolean isFile();

    /**
     * @return the path of the file or directory pointed to by this link
     */
    String getRealFilename();
}
