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
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import slash.navigation.datasources.DataSource;
import slash.navigation.datasources.DataSourceManager;
import slash.navigation.datasources.Edition;
import slash.navigation.datasources.helpers.DataSourceService;
import slash.navigation.download.DownloadManager;
import slash.navigation.download.tools.base.BaseDownloadTool;

import javax.xml.bind.JAXBException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import static java.lang.System.exit;
import static slash.common.io.Files.recursiveDelete;
import static slash.navigation.datasources.DataSourceManager.loadAllDataSources;

/**
 * Performs a filesystem mirror from the DataSources catalog.
 *
 * @author Christian Pesch
 */

public class SnapshotCatalog extends BaseDownloadTool {
    private static final Logger log = Logger.getLogger(SnapshotCatalog.class.getName());
    private static final String RESET_ARGUMENT = "reset";

    private DataSourceManager dataSourceManager;
    private boolean reset;

    public void setReset(boolean reset) {
        this.reset = reset;
    }

    private void open() throws IOException {
        dataSourceManager = new DataSourceManager(new DownloadManager(new File(getSnapshotDirectory(), "snapshot-queue.xml")));
        if(reset) {
            dataSourceManager.getDownloadManager().clearQueue();
            recursiveDelete(getSnapshotDirectory());
        }  else
            dataSourceManager.getDownloadManager().loadQueue();
    }

    private void close() {
        dataSourceManager.getDownloadManager().saveQueue();
        dataSourceManager.dispose();
    }

    private List<DataSource> createDataSourceSet(List<Edition> editions) {
        Set<DataSource> result = new HashSet<>();
        for(Edition edition : editions) {
            result.addAll(edition.getDataSources());
        }
        return new ArrayList<>(result);
    }

    public void snapshot() throws IOException, JAXBException {
        open();

        dataSourceManager.downloadRoot(getDataSourcesServer(), getRootDirectory());
        DataSourceService root = loadAllDataSources(getRootDirectory());

        dataSourceManager.downloadEditions(root.getEditions(), getEditionsDirectory());
        DataSourceService editions = loadAllDataSources(getEditionsDirectory());

        dataSourceManager.downloadDataSources(createDataSourceSet(editions.getEditions()), getDataSourcesDirectory());
        DataSourceService dataSources = loadAllDataSources(getDataSourcesDirectory());
        log.info(String.format("Snapshot contains %d editions and %d datasources", editions.getEditions().size(), dataSources.getDataSources().size()));

        close();
    }

    private void run(String[] args) throws Exception {
        CommandLine line = parseCommandLine(args);
        setDataSourcesServer(line.getOptionValue(DATASOURCES_SERVER_ARGUMENT));
        reset = line.hasOption(RESET_ARGUMENT);
        snapshot();
    }

    @SuppressWarnings("AccessStaticViaInstance")
    private CommandLine parseCommandLine(String[] args) throws ParseException {
        CommandLineParser parser = new DefaultParser();
        Options options = new Options();
        options.addOption(Option.builder().argName(DATASOURCES_SERVER_ARGUMENT).numberOfArgs(1).longOpt("server").
                desc("Data sources server").build());
        options.addOption(Option.builder().argName(RESET_ARGUMENT).longOpt("reset").
                desc("Reset local snapshot").build());
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
        exit(0);
    }
}
