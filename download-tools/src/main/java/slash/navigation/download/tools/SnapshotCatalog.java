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

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import slash.navigation.datasources.DataSource;
import slash.navigation.datasources.DataSourceService;
import slash.navigation.download.Download;
import slash.navigation.download.DownloadManager;
import slash.navigation.download.FileAndChecksum;
import slash.navigation.download.tools.base.BaseDownloadTool;

import javax.xml.bind.JAXBException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import static java.lang.String.format;
import static slash.common.io.Directories.getTemporaryDirectory;
import static slash.navigation.download.Action.Copy;

/**
 * Performs a filesystem mirror from the DataSources catalog.
 *
 * @author Christian Pesch
 */

public class SnapshotCatalog extends BaseDownloadTool {
    private static final Logger log = Logger.getLogger(SnapshotCatalog.class.getName());
    private static final String[] EDITIONS = {"Online", "Offline"};

    private String url;
    private DownloadManager downloadManager = new DownloadManager(new File(getTemporaryDirectory(), "snapshot-queue.xml"));

    private void deleteAll(File directory) throws IOException {
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

    private void snapshot() throws IOException, JAXBException {
        downloadManager.clearQueue();
        deleteAll(getDataSourcesTarget());

        snapshotEditions(getEditionTarget());
        DataSourceService dataSourceService = loadDataSources(getEditionTarget());
        snapshotDataSources(dataSourceService, getDataSourcesTarget());
    }

    private void snapshotEditions(File target) {
        List<Download> downloads = new ArrayList<>();
        // TODO remove hardcoded EDITIONS by scanning http://www.routeconverter.com/datasources/edition/
        for (String edition : EDITIONS) {
            String editionUri = edition.toLowerCase() + ".xml";
            String editionUrl = url + "edition/" + editionUri;
            Download download = downloadManager.queueForDownload("RouteConverter " + edition + " Edition: Catalog of Data Sources",
                    editionUrl, Copy, null, new FileAndChecksum(new File(target, editionUri), null), null);
            downloads.add(download);
            log.info(format("Downloading '%s'", editionUrl));
        }
        downloadManager.waitForCompletion(downloads);
    }

    private void snapshotDataSources(DataSourceService dataSourceService, File directory) throws JAXBException, FileNotFoundException {
        List<Download> downloads = new ArrayList<>();
        for (DataSource dataSource : dataSourceService.getDataSources()) {
            for (slash.navigation.datasources.File file : dataSource.getFiles()) {
                String url = dataSource.getBaseUrl() + file.getUri();
                Download download = downloadManager.queueForDownload(dataSource.getName() + ": Data Source " + file.getUri(),
                        url, Copy, null, new FileAndChecksum(new File(directory, file.getUri().toLowerCase()),
                                file.getLatestChecksum()), null);
                downloads.add(download);
                log.info(format("Downloading '%s'", url));
            }
        }
        downloadManager.waitForCompletion(downloads);
    }

    private void run(String[] args) throws Exception {
        CommandLine line = parseCommandLine(args);
        url = line.getOptionValue(URL_ARGUMENT);
        snapshot();
        System.exit(0);
    }

    private CommandLine parseCommandLine(String[] args) throws ParseException {
        CommandLineParser parser = new GnuParser();
        Options options = new Options();
        //noinspection AccessStaticViaInstance
        options.addOption(OptionBuilder.withArgName(URL_ARGUMENT).hasArgs(1).isRequired().withLongOpt("url").
                withDescription("URL to take a snapshot from").create());
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
