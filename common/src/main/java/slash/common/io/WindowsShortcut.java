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

import mslinks.ShellLink;
import mslinks.ShellLinkException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import static java.lang.String.format;
import static slash.common.io.Files.getExtension;

/**
 * Represents a Windows shortcut (typically visible to Java only as a '.lnk' file),
 * the counterpart to a macOS {@link MacAlias}. Parsing is delegated to the mslinks
 * library; this class keeps the small facade RouteConverter relies on.
 *
 * @author Christian Pesch
 */
public class WindowsShortcut {
    private static final int MINIMUM_LENGTH = 0x64;
    private static final int MAGIC = 0x0000004C;

    private final ShellLink shellLink;

    /**
     * Provides a quick test to see if this could be a valid link.
     * Instantiating a {@link WindowsShortcut} for an invalid link throws, and
     * exceptions are slow, so code looping over several files should check this first.
     *
     * @param file the potential link
     * @return true if it may be a link, false otherwise
     * @throws IOException if an IOException is thrown while reading from the file
     */
    public static boolean isPotentialValidLink(File file) throws IOException {
        if (!file.isFile() || !getExtension(file).equals(".lnk"))
            return false;

        try (InputStream inputStream = new FileInputStream(file)) {
            byte[] header = new byte[4];
            return inputStream.available() >= MINIMUM_LENGTH
                    && inputStream.read(header) == header.length && bytesToDword(header) == MAGIC;
        }
    }

    public WindowsShortcut(File file) throws IOException {
        try {
            this.shellLink = new ShellLink(file);
        } catch (ShellLinkException e) {
            throw new IOException(format("Could not parse Windows shortcut %s", file), e);
        }
    }

    /**
     * @return the name of the filesystem object pointed to by this shortcut
     */
    public String getRealFilename() {
        return shellLink.resolveTarget();
    }

    /**
     * @return true if the shortcut points to a directory, false otherwise
     */
    public boolean isDirectory() {
        return shellLink.getHeader().getFileAttributesFlags().isDirecory();
    }

    public boolean isFile() {
        return !isDirectory();
    }

    // little-endian DWORD, the classic .lnk header-size marker at offset 0x00
    private static int bytesToDword(byte[] bytes) {
        return (bytes[3] & 0xff) << 24 | (bytes[2] & 0xff) << 16 | (bytes[1] & 0xff) << 8 | (bytes[0] & 0xff);
    }
}
