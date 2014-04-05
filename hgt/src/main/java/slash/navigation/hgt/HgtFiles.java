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

import slash.common.type.CompactCalendar;
import slash.navigation.common.LongitudeAndLatitude;
import slash.navigation.download.Download;
import slash.navigation.download.DownloadManager;
import slash.navigation.download.actions.Validator;
import slash.navigation.download.datasources.File;
import slash.navigation.download.datasources.Fragment;
import slash.navigation.elevation.ElevationService;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.*;
import java.util.prefs.Preferences;

import static java.lang.String.format;
import static slash.common.io.Directories.ensureDirectory;
import static slash.common.io.Directories.getApplicationDirectory;
import static slash.navigation.download.Action.Extract;

/**
 * Encapsulates access to HGT files.
 *
 * @author Robert "robekas", Christian Pesch
 */

public class HgtFiles implements ElevationService {
    private static final Preferences preferences = Preferences.userNodeForPackage(HgtFiles.class);
    private static final String DIRECTORY_PREFERENCE = "directory";
    private static final String BASE_URL_PREFERENCE = "baseUrl";

    private final Map<java.io.File, RandomAccessFile> randomAccessFileCache = new HashMap<java.io.File, RandomAccessFile>();
    private final String name, baseUrl, directory;
    private final Map<String, File> fileMap;
    private final Map<String, Fragment> fragmentMap;
    private final DownloadManager downloadManager;

    public HgtFiles(String name, String baseUrl, String directory,
                    Map<String, File> fileMap, Map<String, Fragment> fragmentMap,
                    DownloadManager downloadManager) {
        this.name = name;
        this.baseUrl = baseUrl;
        this.directory = directory;
        this.fileMap = fileMap;
        this.fragmentMap = fragmentMap;
        this.downloadManager = downloadManager;
    }

    public String getName() {
        return name;
    }

    String getBaseUrl() {
        return preferences.get(BASE_URL_PREFERENCE + getName(), baseUrl);
    }

    public boolean isDownload() {
        return true;
    }

    public String getPath() {
        return preferences.get(DIRECTORY_PREFERENCE + getName(), "");
    }

    public void setPath(String path) {
        preferences.put(DIRECTORY_PREFERENCE + getName(), path);
    }

    private java.io.File getDirectory() {
        String directoryName = getPath();
        java.io.File f = new java.io.File(directoryName);
        if(!f.exists())
            directoryName = getApplicationDirectory(directory).getAbsolutePath();
        return ensureDirectory(directoryName);
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
        return new java.io.File(getPath(), format("%s%s", key, ".hgt"));
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

    private static class FragmentAndTarget {
        public final Fragment fragment;
        public final java.io.File target;

        private FragmentAndTarget(Fragment fragment, java.io.File target) {
            this.fragment = fragment;
            this.target = target;
        }
    }

    public void downloadElevationDataFor(List<LongitudeAndLatitude> longitudeAndLatitudes) {
        Set<String> keys = new HashSet<String>();
        for (LongitudeAndLatitude longitudeAndLatitude : longitudeAndLatitudes) {
            keys.add(createFileKey(longitudeAndLatitude.longitude, longitudeAndLatitude.latitude));
        }

        Set<FragmentAndTarget> fragments = new HashSet<FragmentAndTarget>();
        for (String key : keys) {
            Fragment fragment = fragmentMap.get(key);
            if (fragment != null)
                fragments.add(new FragmentAndTarget(fragment, createFile(key)));
        }

        Collection<Download> downloads = new HashSet<Download>();
        for (FragmentAndTarget fragment : fragments) {
            if (new Validator(fragment.target).existsFile())
                continue;
            downloads.add(download(fragment));
        }

        if (!downloads.isEmpty())
            downloadManager.waitForCompletion(downloads);
    }

    private Download download(FragmentAndTarget fragment) {
        String uri = fragment.fragment.getUri();
        String url = getBaseUrl() + uri;
        File file = fileMap.get(uri);
        Long fileSize = file != null ? file.getSize() : null;
        CompactCalendar fileTimestamp = file != null ? file.getTimestamp() : null;
        String fileChecksum = file != null ? file.getChecksum() : null;
        return downloadManager.queueForDownload(getName() + " elevation data for " + uri, url,
                fileSize, fileChecksum, fileTimestamp, Extract, getDirectory());
    }
}
