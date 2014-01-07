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

import slash.navigation.common.LongitudeAndLatitude;
import slash.navigation.elevation.ElevationService;
import slash.navigation.download.Download;
import slash.navigation.download.DownloadManager;
import slash.navigation.download.Extractor;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.*;
import java.util.prefs.Preferences;

import static java.lang.Integer.MAX_VALUE;
import static java.lang.String.format;
import static slash.common.io.Externalization.getTempDirectory;
import static slash.common.io.Files.lastPathFragment;

/**
 * Encapsulates access to HGT files.
 *
 * @author Robert "robekas", Christian Pesch
 */

public class HgtFiles implements ElevationService {
    private static final Preferences preferences = Preferences.userNodeForPackage(HgtFiles.class);
    private static final String DIRECTORY_PREFERENCE = "directory";
    private static final String BASE_URL_PREFERENCE = "baseUrl";

    private final Map<File, RandomAccessFile> randomAccessFileCache = new HashMap<File, RandomAccessFile>();
    private final String name, baseUrl, directory;
    private final Map<String, String> mapping;
    private final DownloadManager downloadManager;

    public HgtFiles(String name, String baseUrl, Map<String, String> mapping, String directory, DownloadManager downloadManager) {
        this.name = name;
        this.baseUrl = baseUrl;
        this.mapping = mapping;
        this.directory = directory;
        this.downloadManager = downloadManager;
    }

    public String getName() {
        return name;
    }

    String getBaseUrl() {
        return preferences.get(BASE_URL_PREFERENCE + getName(), baseUrl);
    }

    File getDirectory() {
        String directoryName = preferences.get(DIRECTORY_PREFERENCE + getName(),
                new File(System.getProperty("user.home"), ".routeconverter/" + directory).getAbsolutePath());
        File directory = new File(directoryName);
        if (!directory.exists()) {
            if (!directory.mkdirs())
                throw new IllegalArgumentException("Cannot create '" + getName() + "' directory '" + directory + "'");
        }
        return directory;
    }

    String createFileKey(double longitude, double latitude) {
        int longitudeAsInteger = (int) longitude;
        int latitudeAsInteger = (int) latitude;
        return format("%s%02d%s%03d", (latitude < 0) ? "S" : "N",
                (latitude < 0) ? ((latitudeAsInteger - 1) * -1) : latitudeAsInteger,
                (longitude < 0) ? "W" : "E",
                (longitude < 0) ? ((longitudeAsInteger - 1) * -1) : longitudeAsInteger);
    }

    private File createFile(String key) {
        return new File(getDirectory(), format("%s%s", key, ".hgt"));
    }

    public Double getElevationFor(double longitude, double latitude) throws IOException {
        File file = createFile(createFileKey(longitude, latitude));
        if (!file.exists())
            return null;

        RandomAccessFile randomAccessFile = randomAccessFileCache.get(file);
        if (randomAccessFile == null) {
            randomAccessFile = new RandomAccessFile(file, "r");
            randomAccessFileCache.put(file, randomAccessFile);
        }
        return new ElevationTile(randomAccessFile).getElevationFor(longitude, latitude);
    }

    public void dispose() {
        for (RandomAccessFile randomAccessFile : randomAccessFileCache.values())
            try {
                randomAccessFile.close();
            } catch (IOException e) {
                throw new IllegalArgumentException("Cannot close random access file" + randomAccessFile);
            }
        randomAccessFileCache.clear();
    }

    public void downloadElevationFor(List<LongitudeAndLatitude> longitudeAndLatitudes) {
        Set<String> keys = new HashSet<String>();
        for (LongitudeAndLatitude longitudeAndLatitude : longitudeAndLatitudes) {
            keys.add(createFileKey(longitudeAndLatitude.longitude, longitudeAndLatitude.latitude));
        }

        Set<String> uris = new HashSet<String>();
        for (String key : keys) {
            File file = createFile(key);
            if (!file.exists()) {
                String uri = mapping.get(key);
                if (uri != null)
                    uris.add(uri);
            }
        }

        Collection<Download> downloads = new HashSet<Download>();
        for (String uri : uris) {
            Download download = download(uri);
            if (download != null)
                downloads.add(download);
        }

        if (!downloads.isEmpty())
            downloadManager.waitForCompletion(downloads);
    }

    private Download download(String uri) {
        String url = format("%s%s", getBaseUrl(), uri);
        File archive = new File(getTempDirectory(), lastPathFragment(uri, MAX_VALUE));
        return downloadManager.queueForDownloadAndProcess(getName() + " elevation data for " + uri, url, archive, new Extractor(getDirectory()));
    }
}
