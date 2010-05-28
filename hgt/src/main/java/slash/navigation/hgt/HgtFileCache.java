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

package slash.navigation.hgt;

import slash.common.io.InputOutput;

import java.io.*;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

/**
 * Caches HGT files.
 *
 * @author Christian Pesch
 */

public class HgtFileCache {
    private static Logger log = Logger.getLogger(HgtFileCache.class.getName());
    private static final Preferences preferences = Preferences.userNodeForPackage(HgtFileCache.class);
    private static final String HGT_CACHE_DIRECTORY_PREFERENCE = "hgtCacheDirectory";

    private static File getHgtCacheDirectory() {
        String directoryName = preferences.get(HGT_CACHE_DIRECTORY_PREFERENCE, new File(System.getProperty("user.home"), ".hgt").getAbsolutePath());
        File directory = new File(directoryName);
        if (!directory.exists()) {
            if (!directory.mkdirs())
                throw new IllegalArgumentException("Cannot create hgt cache directory " + directory);
        }
        return directory;
    }

    public File get(String key) {
        File file = put(key);
        return file.exists() ? file : null;
    }

    public InputStream getFileAsStream(String key) throws IOException {
        File file = get(key);
        if (file == null)
            return null;
        return new FileInputStream(file);
    }

    public Object getFileAsObject(String key) throws IOException {
        InputStream inputStream = getFileAsStream(key);
        if (inputStream != null) {
            ObjectInputStream ois = new ObjectInputStream(inputStream);
            try {
                return ois.readObject();
            } catch (ClassNotFoundException e) {
                log.warning("Cannot deserialize object from " + key);
            } finally {
                ois.close();
            }
        }
        return null;
    }

    private File put(String key) {
        return new File(getHgtCacheDirectory(), key);
    }

    public void put(String key, File source) throws IOException {
        File target = put(key);
        InputOutput.copy(new FileInputStream(source), new FileOutputStream(target));
    }

    public void putAsObject(String key, Object value) throws IOException {
        File file = put(key);
        ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(file));
        try {
            out.writeObject(value);
        }
        finally {
            out.close();
        }
    }

    public boolean clear() {
        boolean cleared = true;
        for (File file : getHgtCacheDirectory().listFiles())
            cleared = cleared && file.delete();
        return cleared;
    }
}
