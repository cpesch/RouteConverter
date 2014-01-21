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
import slash.navigation.download.Download;
import slash.navigation.download.DownloadManager;
import slash.navigation.download.datasources.File;
import slash.navigation.download.datasources.Fragment;
import slash.navigation.elevation.ElevationService;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.*;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

import static java.lang.String.format;
import static java.util.logging.Logger.getLogger;
import static slash.navigation.download.Action.Extract;

/**
 * Encapsulates access to HGT files.
 *
 * @author Robert "robekas", Christian Pesch
 */

public class HgtFiles implements ElevationService {
    private static final Logger log = getLogger(HgtFiles.class.getName());
    private static final Preferences preferences = Preferences.userNodeForPackage(HgtFiles.class);
    private static final String DIRECTORY_PREFERENCE = "directory";
    private static final String BASE_URL_PREFERENCE = "baseUrl";

    private final Map<java.io.File, RandomAccessFile> randomAccessFileCache = new HashMap<java.io.File, RandomAccessFile>();
    private final String name, baseUrl, directory;
    private final Map<String, Fragment> archiveMap;
    private final Map<String, File> fileMap;
    private final DownloadManager downloadManager;

    public HgtFiles(String name, String baseUrl, String directory,
                    Map<String, Fragment> archiveMap, Map<String, File> fileMap,
                    DownloadManager downloadManager) {
        this.name = name;
        this.baseUrl = baseUrl;
        this.directory = directory;
        this.archiveMap = archiveMap;
        this.fileMap = fileMap;
        this.downloadManager = downloadManager;
    }

    public String getName() {
        return name;
    }

    String getBaseUrl() {
        return preferences.get(BASE_URL_PREFERENCE + getName(), baseUrl);
    }

    private java.io.File getDirectory() {
        String directoryName = preferences.get(DIRECTORY_PREFERENCE + getName(),
                new java.io.File(System.getProperty("user.home"), ".routeconverter/" + directory).getAbsolutePath());
        java.io.File directory = new java.io.File(directoryName);
        if (!directory.exists()) {
            if (!directory.mkdirs())
                throw new IllegalArgumentException(format("Cannot create '%s' directory '%s'", getName(), directory));
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

    private java.io.File createFile(String key) {
        return new java.io.File(getDirectory(), format("%s%s", key, ".hgt"));
    }

    public Double getElevationFor(double longitude, double latitude) throws IOException {
        java.io.File file = createFile(createFileKey(longitude, latitude));
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

    public void downloadElevationDataFor(List<LongitudeAndLatitude> longitudeAndLatitudes) {
        Set<String> keys = new HashSet<String>();
        for (LongitudeAndLatitude longitudeAndLatitude : longitudeAndLatitudes) {
            keys.add(createFileKey(longitudeAndLatitude.longitude, longitudeAndLatitude.latitude));
        }

        Set<Fragment> fragments = new HashSet<Fragment>();
        for (String key : keys) {
            Fragment fragment = archiveMap.get(key);
            if (fragment == null)
                continue;

            java.io.File file = createFile(key);
            if (!validate(file, fragment.getSize()))
                fragments.add(fragment);
        }

        Collection<Download> downloads = new HashSet<Download>();
        for (Fragment fragment : fragments)
            downloads.add(download(fragment));

        if (!downloads.isEmpty())
            downloadManager.waitForCompletion(downloads);
    }

    private boolean validate(java.io.File file, Long fileSize) {
        // do not validate checksum here since it's too expensive
        if (!file.exists()) {
            log.info("File " + file + " does not exist");
            return false;
        }
        if (fileSize != null && file.length() != fileSize) {
            log.info("File " + file + " size is " + file.length() + " but expected " + fileSize + " bytes");
            return false;
        }
        return true;
    }

    private Download download(Fragment fragment) {
        String uri = fragment.getUri();
        File file = fileMap.get(uri);
        Long fileSize = file != null ? file.getSize() : null;
        String fileChecksum = file != null ? file.getChecksum() : null;
        return downloadManager.queueForDownload(getName() + " elevation data for " + uri, getBaseUrl() + uri,
                fileSize, fileChecksum, Extract, getDirectory());
    }
}
