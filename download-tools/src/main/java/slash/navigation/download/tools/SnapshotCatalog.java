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
package slash.navigation.download.tools;

import org.apache.commons.cli.*;
import slash.navigation.datasources.DataSource;
import slash.navigation.datasources.DataSourceService;
import slash.navigation.datasources.Edition;
import slash.navigation.download.Download;
import slash.navigation.download.DownloadManager;
import slash.navigation.download.FileAndChecksum;
import slash.navigation.download.tools.base.BaseDownloadTool;

import javax.xml.bind.JAXBException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static slash.navigation.download.Action.Copy;

/**
 * Performs a filesystem mirror from the DataSources catalog.
 *
 * @author Christian Pesch
 */

public class SnapshotCatalog extends BaseDownloadTool {
    private static final Logger log = Logger.getLogger(SnapshotCatalog.class.getName());
    private static final String RESET_ARGUMENT = "reset";
    private static final String EDITIONS_URI = "editions/";
    private static final String FORMAT_XML = "?format=xml";
    private static final String DOT_XML = ".xml";

    private DownloadManager downloadManager = new DownloadManager(new File(getSnapshotDirectory(), "snapshot-queue.xml"));
    private String url;
    private boolean reset = false;

    public void setUrl(String url) {
        this.url = url;
    }

    public void setReset(boolean reset) {
        this.reset = reset;
    }

    void close() {
        downloadManager.saveQueue();
        downloadManager.dispose();
    }

    private void open() throws IOException {
        if(reset) {
            downloadManager.clearQueue();
            deleteAll(getSnapshotDirectory());
        }  else
            downloadManager.loadQueue();
    }

    void deleteAll(File directory) throws IOException {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory())
                    deleteAll(file);
                if (!file.delete())
                    throw new IOException("Could not delete " + file);
            }
        }
    }

    void snapshotRoot(File directory) {
        String editionsUrl = url + EDITIONS_URI + FORMAT_XML;
        File file = new File(directory, "editions" + DOT_XML);
        Download download = downloadManager.queueForDownload("RouteConverter Editions", editionsUrl, Copy, null,
                new FileAndChecksum(file, null), null);
        log.info(format("Downloading %s to %s", editionsUrl, file));
        downloadManager.waitForCompletion(asList(download));
    }

    private void snapshotEditions(DataSourceService dataSourceService, File directory) throws JAXBException, FileNotFoundException {
        List<Download> downloads = new ArrayList<>();
        for (Edition edition : dataSourceService.getEditions()) {
            String editionUrl = edition.getHref() + FORMAT_XML;
            File file = new File(directory, edition.getId() + DOT_XML);
            Download download = downloadManager.queueForDownload("RouteConverter Edition: " + edition.getId(),
                    editionUrl, Copy, null, new FileAndChecksum(file, null), null);
            downloads.add(download);
            log.info(format("Downloading %s to %s", editionUrl, file));
        }
        downloadManager.waitForCompletion(downloads);
    }

    private void snapshotDataSources(DataSourceService dataSourceService, File directory) throws JAXBException, FileNotFoundException {
        Set<DataSource> dataSources = new HashSet<>();
        for(Edition edition : dataSourceService.getEditions())
            dataSources.addAll(edition.getDataSources());

        List<Download> downloads = new ArrayList<>();
        for (DataSource dataSource : dataSources) {
            String datasourceUrl = dataSource.getHref() + FORMAT_XML;
            File file = new File(directory, dataSource.getId() + DOT_XML);
            Download download = downloadManager.queueForDownload("RouteConverter DataSource: " + dataSource.getId(),
                    datasourceUrl, Copy, null, new FileAndChecksum(file, null), null);
            downloads.add(download);
            log.info(format("Downloading %s to %s", datasourceUrl, file));
        }
        downloadManager.waitForCompletion(downloads);
    }

    private void snapshot() throws IOException, JAXBException {
        open();

        snapshotRoot(getRootDirectory());
        DataSourceService editions = loadDataSources(getRootDirectory());
        snapshotEditions(editions, getEditionsDirectory());
        DataSourceService datasources = loadDataSources(getEditionsDirectory());
        snapshotDataSources(datasources, getDataSourcesDirectory());

        close();
    }

    private void run(String[] args) throws Exception {
        CommandLine line = parseCommandLine(args);
        setUrl(line.getOptionValue(URL_ARGUMENT));
        reset = line.hasOption(RESET_ARGUMENT);
        snapshot();
        System.exit(0);
    }

    @SuppressWarnings("AccessStaticViaInstance")
    private CommandLine parseCommandLine(String[] args) throws ParseException {
        CommandLineParser parser = new GnuParser();
        Options options = new Options();
        options.addOption(OptionBuilder.withArgName(URL_ARGUMENT).hasArgs(1).isRequired().withLongOpt("url").
                withDescription("URL to take a snapshot from").create());
        options.addOption(OptionBuilder.withArgName(RESET_ARGUMENT).withLongOpt("reset").
                withDescription("Reset local snapshot").create());
        try {
            return parser.parse(options, args);
        } catch (ParseException e) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp(getClass().getSimpleName(), options);
            throw e;
        }
    }

    public static void main(String[] args) throws Exception {
        new SnapshotCatalog().run(args);
    }
}
