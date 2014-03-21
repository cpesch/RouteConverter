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

import slash.common.type.CompactCalendar;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

import static java.util.logging.Logger.getLogger;
import static slash.common.io.Files.generateChecksum;
import static slash.common.type.CompactCalendar.fromMillis;

/**
 * Validates the properties of a {@link File}.
 *
 * @author Christian Pesch
 */

public class Validator {
    private static final Logger log = getLogger(Validator.class.getName());

    private final File file;

    public Validator(File file) {
        this.file = file;
    }

    public boolean existsFile() {
        if (!file.exists()) {
            log.warning("File " + file + " does not exist");
            return false;
        }
        return true;
    }

    public boolean validSize(Long expectedSize) {
        if (!existsFile())
            return false;

        if (expectedSize != null && file.length() != expectedSize) {
            log.warning("File " + file + " size is " + file.length() + " but expected " + expectedSize + " bytes");
            return false;
        }
        return true;
    }

    public boolean validChecksum(String expectedChecksum) throws IOException {
        if (!existsFile())
            return false;

        if (expectedChecksum != null) {
            String actualChecksum = generateChecksum(file);
            boolean result = actualChecksum.equals(expectedChecksum);
            if (!result)
                log.warning("File " + file + " checksum is " + actualChecksum + " but expected " + expectedChecksum);
            return result;
        }
        return true;
    }

    public boolean validTimestamp(CompactCalendar expectedTimestamp) {
        if (!existsFile())
            return false;

        if (expectedTimestamp != null) {
            CompactCalendar actualTimestamp = fromMillis(file.lastModified());
            boolean result = actualTimestamp.equals(expectedTimestamp);
            if (!result)
                log.warning("File " + file + " timestamp is " + actualTimestamp + " but expected " + expectedTimestamp);
            return result;
        }
        return true;
    }
}
