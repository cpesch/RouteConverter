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
import slash.common.io.NotClosingUnderlyingInputStream;
import slash.navigation.rest.Get;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Downloads HGT files.
 *
 * @author Christian Pesch
 */

public class HgtFileDownloader {
    private static Logger log = Logger.getLogger(HgtFileDownloader.class.getName());
    private static final Preferences preferences = Preferences.userNodeForPackage(HgtFiles.class);
    private static final String HGT_FILES_URL_PREFERENCE = "hgtFilesUrl";
    private static final String HGT_FILES_URL_FAILURES = "hgtFilesUrl.failures";
    private static final List<String> CONTINENTS = Arrays.
            asList("Eurasia", "North_America", "Australia", "South_America", "Africa", "Islands");

    private HgtFileCache fileCache;
    private Set<String> downloadFailures = new HashSet<String>();

    public HgtFileDownloader(HgtFileCache fileCache) {
        this.fileCache = fileCache;
        initialize();
    }

    private static String getHgtFilesUrl() {
        return preferences.get(HGT_FILES_URL_PREFERENCE, "http://dds.cr.usgs.gov/srtm/version2_1/SRTM3/");
    }

    @SuppressWarnings({"unchecked"})
    private synchronized void initialize() {
        try {
            downloadFailures = (Set<String>) fileCache.getFileAsObject(HGT_FILES_URL_FAILURES);
        } catch (IOException e) {
            log.warning("Could not deserialize failures: " + e.getMessage());
        }
        if (downloadFailures == null)
            downloadFailures = new HashSet<String>();
    }

    private synchronized boolean hasDownloadAlreadyFailed(String key) {
        return downloadFailures.contains(key);
    }

    private synchronized void addFailedDownload(String key) {
        downloadFailures.add(key);
        try {
            fileCache.putAsObject(HGT_FILES_URL_FAILURES, downloadFailures);
        } catch (IOException e) {
            log.warning("Could not serialize failures: " + e.getMessage());
        }
    }

    private InputStream downloadFromUrl(String url) throws IOException {
        Get get = new Get(url);
        InputStream inputStream = get.executeAsStream(true);
        if (get.isSuccessful())
            return inputStream;
        else
            throw new IOException("Cannot access " + url);
    }

    private File extractFileFrom(InputStream inputStream, String key) throws IOException {
        File file = null;

        ZipInputStream zipInputStream = null;
        try {
            zipInputStream = new ZipInputStream(inputStream);
            ZipEntry entry = zipInputStream.getNextEntry();
            while (entry != null) {
                if (!entry.isDirectory()) {
                    File extracted = File.createTempFile("routeconverter", ".hgt");
                    InputOutput.copy(new NotClosingUnderlyingInputStream(zipInputStream), new FileOutputStream(extracted));
                    zipInputStream.closeEntry();

                    if (entry.getName().equals(key))
                        file = extracted;
                }
                entry = zipInputStream.getNextEntry();
            }
        }
        finally {
            if (zipInputStream != null)
                zipInputStream.close();
        }
        return file;
    }

    public File download(String key) {
        File file = null;

        for (String continent : CONTINENTS) {
            String url = String.format("%s%s/%s.zip", getHgtFilesUrl(), continent, key);
            if (!hasDownloadAlreadyFailed(url)) {
                try {
                    InputStream inputStream = downloadFromUrl(url);
                    if (inputStream != null) {
                        file = extractFileFrom(inputStream, key);
                        if (file != null)
                            return file;
                    }
                } catch (IOException e) {
                    log.warning("Cannot download from '" + url + "': " + e.getMessage());
                    addFailedDownload(url);
                }
            }
        }

        return file;
    }
}