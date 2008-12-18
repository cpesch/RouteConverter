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
    along with Foobar; if not, write to the Free Software
    Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA

    Copyright (C) 2007 Christian Pesch. All Rights Reserved.
*/

package slash.navigation.util;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;

/**
 * Provides externalization functionality.
 *
 * @author Christian Pesch
 */

public class Externalization {
    private static File tempDirectory;

    public synchronized static File getTempDirectory() throws IOException {
        if (tempDirectory == null) {
            File tempFile = File.createTempFile("routeconverter", ".tmp");
            tempDirectory = tempFile.getParentFile();
            tempFile.delete();
        }
        return tempDirectory;
    }

    private static File getTempFile(String fileName) throws IOException {
        return new File(getTempDirectory(), fileName);
    }

    private static long getLastModified(Class<?> clazz, String fileName) throws IOException {
        long lastModified = Long.MAX_VALUE;
        URL url = clazz.getResource(fileName);
        if(url != null) {
            URLConnection connection = url.openConnection();
            lastModified = connection.getLastModified();
        }
        return lastModified;
    }

    public static File extractFile(Class<?> clazz, String fileName) throws IOException {
        File target = getTempFile(fileName);
        if (target.exists() &&  target.lastModified() >= getLastModified(clazz, fileName))
            return target;

        InputStream in = clazz.getResourceAsStream(fileName);
        if (in == null)
            return null;

        InputOutput inout = new InputOutput(in, new FileOutputStream(target));
        inout.start();
        inout.close();
        return target;
    }

    public static void loadLibrary(Class<?> clazz, String libName) throws IOException {
        String platformLibName = System.mapLibraryName(libName);
        File lib = extractFile(clazz, platformLibName);
        if (lib == null)
            throw new FileNotFoundException("Native library " + platformLibName + " not in class path");
        System.load(lib.getAbsolutePath());
    }
}
