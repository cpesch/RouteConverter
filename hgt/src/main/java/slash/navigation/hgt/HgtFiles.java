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

import slash.navigation.common.BoundingBox;
import slash.navigation.common.LongitudeAndLatitude;
import slash.navigation.datasources.DataSource;
import slash.navigation.datasources.Downloadable;
import slash.navigation.datasources.Fragment;
import slash.navigation.download.Action;
import slash.navigation.download.Download;
import slash.navigation.download.DownloadManager;
import slash.navigation.download.FileAndChecksum;
import slash.navigation.elevation.ElevationService;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.*;
import java.util.prefs.Preferences;

import static java.lang.String.format;
import static slash.common.io.Directories.ensureDirectory;
import static slash.common.io.Directories.getApplicationDirectory;
import static slash.common.io.Files.removeExtension;

/**
 * Encapsulates access to HGT files.
 *
 * @author Robert "robekas", Christian Pesch
 */

public class HgtFiles implements ElevationService {
    private static final Preferences preferences = Preferences.userNodeForPackage(HgtFiles.class);
    private static final String DIRECTORY_PREFERENCE = "directory";
    private static final String BASE_URL_PREFERENCE = "baseUrl";
    private static final String DOT_HGT = ".hgt";

    private final Map<java.io.File, RandomAccessFile> randomAccessFileCache = new HashMap<>();
    private final DataSource dataSource;
    private final DownloadManager downloadManager;

    public HgtFiles(DataSource dataSource, DownloadManager downloadManager) {
        this.dataSource = dataSource;
        this.downloadManager = downloadManager;
    }

    public String getName() {
        return dataSource.getName();
    }

    String getBaseUrl() {
        return preferences.get(BASE_URL_PREFERENCE + getName(), dataSource.getBaseUrl());
    }

    public boolean isDownload() {
        return true;
    }

    public boolean isOverQueryLimit() {
        return false;
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
        if (!f.exists())
            directoryName = getApplicationDirectory(dataSource.getDirectory()).getAbsolutePath();
        return ensureDirectory(directoryName);
    }

    String createFileKey(double longitude, double latitude) {
        int longitudeAsInteger = (int) longitude;
        int latitudeAsInteger = (int) latitude;
        return format("%s%02d%s%03d" + DOT_HGT, (latitude < 0) ? "S" : "N",
                (latitude < 0) ? ((latitudeAsInteger - 1) * -1) : latitudeAsInteger,
                (longitude < 0) ? "W" : "E",
                (longitude < 0) ? ((longitudeAsInteger - 1) * -1) : longitudeAsInteger);
    }

    private java.io.File createFile(String key) {
        return new java.io.File(getDirectory(), key);
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

    public void downloadElevationDataFor(List<LongitudeAndLatitude> longitudeAndLatitudes, boolean waitForDownload) {
        Set<String> keys = new HashSet<>();
        for (LongitudeAndLatitude longitudeAndLatitude : longitudeAndLatitudes) {
            keys.add(createFileKey(longitudeAndLatitude.longitude, longitudeAndLatitude.latitude));
        }

        Collection<Downloadable> downloadables = new HashSet<>();
        for (String key : keys) {
            Fragment<Downloadable> fragment = dataSource.getFragment(key);
            // fallback as long as .hgt is not part of the keys
            if (fragment == null)
                fragment = dataSource.getFragment(removeExtension(key));
            if (fragment != null && !createFile(fragment.getKey()).exists())
                downloadables.add(fragment.getDownloadable());
        }

        Collection<Download> downloads = new HashSet<>();
        for (Downloadable downloadable : downloadables) {
            downloads.add(download(downloadable));
        }

        if (!downloads.isEmpty() && waitForDownload)
            downloadManager.waitForCompletion(downloads);
    }

    private Download download(Downloadable downloadable) {
        List<FileAndChecksum> fragments = new ArrayList<>();
        for (Fragment otherFragments : downloadable.getFragments()) {
            String key = otherFragments.getKey();
            // ignore fragment keys without extension which are reported by old RouteConverter releases
            if (key.endsWith(DOT_HGT))
                fragments.add(new FileAndChecksum(createFile(key), otherFragments.getLatestChecksum()));
        }

        String uri = downloadable.getUri();
        String url = getBaseUrl() + uri;
        return downloadManager.queueForDownload(getName() + " Elevation Tile: " + uri, url, Action.valueOf(dataSource.getAction()),
                new FileAndChecksum(getDirectory(), downloadable.getLatestChecksum()), fragments);
    }

    private Collection<Fragment<Downloadable>> getDownloadablesFor(BoundingBox boundingBox) {
        Collection<Fragment<Downloadable>> result = new HashSet<>();

        double longitude = boundingBox.getSouthWest().getLongitude();
        while (longitude < boundingBox.getNorthEast().getLongitude()) {

            double latitude = boundingBox.getSouthWest().getLatitude();
            while (latitude < boundingBox.getNorthEast().getLatitude()) {
                String key = createFileKey(longitude, latitude);
                Fragment<Downloadable> fragment = dataSource.getFragment(key);
                if (fragment != null)
                    result.add(fragment);
                latitude += 1.0;
            }

            longitude += 1.0;
        }
        return result;
    }

    private Collection<Fragment<Downloadable>> getDownloadablesFor(List<BoundingBox> boundingBoxes) {
        Collection<Fragment<Downloadable>> result = new HashSet<>();
        for (BoundingBox boundingBox : boundingBoxes)
            result.addAll(getDownloadablesFor(boundingBox));
        return result;
    }

    private Collection<Downloadable> asDownloadableSet(Collection<Fragment<Downloadable>> fragments) {
        Collection<Downloadable> result = new ArrayList<>();
        for (Fragment<Downloadable> fragment : fragments)
            result.add(fragment.getDownloadable());
        return result;
    }

    public long calculateRemainingDownloadSize(List<BoundingBox> boundingBoxes) {
        Collection<Fragment<Downloadable>> fragments = getDownloadablesFor(boundingBoxes);

        Collection<Downloadable> downloadables = new HashSet<>();
        for (Fragment<Downloadable> fragment : fragments) {
            java.io.File file = createFile(fragment.getKey());
            if (!file.exists())
                downloadables.add(fragment.getDownloadable());
        }

        long notExists = 0L;
        for (Downloadable downloadable : downloadables) {
            Long contentLength = downloadable.getLatestChecksum().getContentLength();
            if (contentLength == null)
                continue;

            notExists += contentLength;
        }
        return notExists;
    }

    public void downloadElevationData(List<BoundingBox> boundingBoxes) {
        Collection<Fragment<Downloadable>> fragments = getDownloadablesFor(boundingBoxes);
        for (Downloadable downloadable : asDownloadableSet(fragments)) {
            download(downloadable);
        }
    }
}
