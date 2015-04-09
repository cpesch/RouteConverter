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
package slash.navigation.download;

import java.io.File;

/**
 * A file, it's expected and actual checksum
 *
 * @author Christian Pesch
 */

public class FileAndChecksum {
    private final File file;
    private Checksum expectedChecksum;
    private Checksum actualChecksum;

    public FileAndChecksum(File file, Checksum expectedChecksum) {
        this.file = file;
        this.expectedChecksum = expectedChecksum;
    }

    public File getFile() {
        return file;
    }

    public Checksum getExpectedChecksum() {
        return expectedChecksum;
    }

    public void setExpectedChecksum(Checksum expectedChecksum) {
        this.expectedChecksum = expectedChecksum;
    }

    public Checksum getActualChecksum() {
        return actualChecksum;
    }

    public void setActualChecksum(Checksum actualChecksum) {
        this.actualChecksum = actualChecksum;
    }

    public String toString() {
        return getClass().getSimpleName() + "[file=" + getFile() + ", expectedChecksum=" + getExpectedChecksum() +
                ", actualChecksum=" + getActualChecksum() + "]";
    }
}
