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

package slash.navigation.util;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.util.logging.Logger;

/**
 * Provides externalization functionality.
 *
 * @author Christian Pesch
 */

public class Externalization {
    private static final Logger log = Logger.getLogger(Externalization.class.getName());
    private static final File tempDirectory = new File(System.getProperty("java.io.tmpdir") + File.separator + "routeconverter");

    public synchronized static File getTempDirectory() {
        if (!tempDirectory.exists())
            if(!tempDirectory.mkdir())
                log.severe("Could not create temp directory " + tempDirectory);
        return tempDirectory;
    }

    private static File getTempFile(String fileName) {
        int index = fileName.lastIndexOf('/');
        if (index != -1)
            fileName = fileName.substring(index);
        return new File(getTempDirectory(), fileName);
    }

    private static long getLastModified(String fileName) throws IOException {
        long lastModified = Long.MAX_VALUE;
        URL url = Externalization.class.getClassLoader().getResource(fileName);
        if (url != null) {
            URLConnection connection = url.openConnection();
            lastModified = connection.getLastModified();
        }
        return lastModified;
    }

    public static File extractFile(String fileName) throws IOException {
        File target = getTempFile(fileName);
        if (target.exists() && target.lastModified() >= getLastModified(fileName))
            return target;

        InputStream in = Externalization.class.getClassLoader().getResourceAsStream(fileName);
        if (in == null)
            return null;

        log.info("Extracting " + fileName + " to " + target);
        InputOutput inout = new InputOutput(in, new FileOutputStream(target));
        inout.start();
        inout.close();
        return target;
    }

    public static void loadLibrary(String libName) throws IOException {
        String path = "bin/" + Platform.getOsName() + "/" + Platform.getOsArchitecture() + "/" + System.mapLibraryName(libName);
        File lib = extractFile(path);
        if (lib == null)
            throw new FileNotFoundException("Native library " + path + " not in class path");
        System.load(lib.getAbsolutePath());
        log.info("Loaded system library " + lib);
    }
}
