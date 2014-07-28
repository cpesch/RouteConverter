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

package slash.navigation.download.actions;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.logging.Logger;

import static java.lang.String.format;
import static java.util.logging.Logger.getLogger;
import static slash.common.io.Files.generateChecksum;
import static slash.common.type.CompactCalendar.fromMillis;

/**
 * Validates the {@link Checksum} of a {@link File}.
 *
 * @author Christian Pesch
 */

public class Validator {
    private static final Logger log = getLogger(Validator.class.getName());
    private final File file;
    private final Checksum checksum;

    public Validator(File file) throws IOException {
        this.file = file;
        this.checksum = validate();
    }

    private Checksum validate() throws IOException {
        if (!file.exists())
            throw new FileNotFoundException(format("File %s not found", file));
        return new Checksum(generateChecksum(file), file.length(), fromMillis(file.lastModified()));
    }

    public boolean existsFile() {
        return file.exists();
    }

    public boolean validChecksum(Checksum expectedChecksum) throws IOException {
        if (!existsFile())
            return false;

        if (expectedChecksum != null) {
            boolean result = expectedChecksum.equals(checksum);
            if (!result)
                log.warning("File " + file + " has " + checksum + " but expected " + expectedChecksum);
            return result;
        }
        return true;
    }
}
