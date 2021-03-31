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

import java.io.File;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

import static java.io.File.separator;
import static java.lang.String.format;
import static slash.common.system.Platform.isWindows;

/**
 * Provides default directories.
 *
 * @author Christian Pesch
 */

public class Directories {
    private static final Logger log = Logger.getLogger(Files.class.getName());
    private static final Preferences preferences = Preferences.userNodeForPackage(Files.class);
    private static final String APPLICATION_DIRECTORY_PREFERENCE = "applicationDirectory";
    private static final String TEMPORARY_DIRECTORY_PREFERENCE = "temporaryDirectory";

    private static String getDefaultApplicationDirectory() {
        String directory = System.getProperty(APPLICATION_DIRECTORY_PREFERENCE);
        return directory != null ? directory :
                System.getProperty("user.home") + separator + ".routeconverter";
    }
    private static String getDefaultTemporaryDirectory() {
        String directory = System.getProperty(TEMPORARY_DIRECTORY_PREFERENCE);
        return directory != null ? directory :
                System.getProperty("java.io.tmpdir") + separator + "routeconverter" + (!isWindows() ? "-" + System.getProperty("user.name") : "");
    }

    private static final String applicationDirectory = preferences.get(APPLICATION_DIRECTORY_PREFERENCE, getDefaultApplicationDirectory());
    private static final String temporaryDirectory = preferences.get(TEMPORARY_DIRECTORY_PREFERENCE, getDefaultTemporaryDirectory());

    public static File ensureDirectory(File directory) {
        if (!directory.exists()) {
            if (!directory.mkdirs()) {
                log.severe("Could not create directory " + directory);
                throw new IllegalArgumentException(format("Cannot create directory %s", directory));
            }
        }
        return directory;
    }

    public static File ensureDirectory(String directory) {
        return ensureDirectory(new File(directory));
    }

    public synchronized static File getApplicationDirectory() {
        return ensureDirectory(applicationDirectory);
    }

    public synchronized static File getApplicationDirectory(String subDirectory) {
        return ensureDirectory(getApplicationDirectory() + separator + subDirectory);
    }

    public synchronized static File getRoutesDirectory() {
        return getApplicationDirectory("routes");
    }

    public synchronized static File getTemporaryDirectory() {
        return ensureDirectory(temporaryDirectory);
    }
}
