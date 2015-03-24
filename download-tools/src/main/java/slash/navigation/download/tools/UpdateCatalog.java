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
import slash.navigation.download.tools.base.BaseDownloadTool;

import javax.xml.bind.JAXBException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.logging.Logger;

/**
 * Updates the resources from the DataSources catalog from websites
 *
 * @author Christian Pesch
 */

public class UpdateCatalog extends BaseDownloadTool {
    private static final Logger log = Logger.getLogger(ScanWebsite.class.getName());

    private String id, datasourcesServer, datasourcesUserName, datasourcesPassword;

    private void update() throws IOException, JAXBException {
        DataSourceService dataSourceService = new DataSourceService();
        dataSourceService.load(new FileInputStream(new File(getDataSourcesDirectory(), id + ".xml")));
        DataSource source = dataSourceService.getDataSourceById(id);
        if(source == null)
            throw new IllegalArgumentException("Unknown data source: " + id);
    }

    private void run(String[] args) throws Exception {
        CommandLine line = parseCommandLine(args);
        id = line.getOptionValue(ID_ARGUMENT);
        datasourcesServer = line.getOptionValue(DATASOURCES_SERVER_ARGUMENT);
        datasourcesUserName = line.getOptionValue(DATASOURCES_USERNAME_ARGUMENT);
        datasourcesPassword = line.getOptionValue(DATASOURCES_PASSWORD_ARGUMENT);
        update();
        System.exit(0);
    }

    @SuppressWarnings("AccessStaticViaInstance")
    private CommandLine parseCommandLine(String[] args) throws ParseException {
        CommandLineParser parser = new GnuParser();
        Options options = new Options();
        options.addOption(OptionBuilder.withArgName(ID_ARGUMENT).hasArgs().isRequired().withLongOpt("id").
                withDescription("ID of the data source").create());
        options.addOption(OptionBuilder.withArgName(DATASOURCES_SERVER_ARGUMENT).hasArgs(1).withLongOpt("server").
                withDescription("Data sources server").create());
        options.addOption(OptionBuilder.withArgName(DATASOURCES_USERNAME_ARGUMENT).hasArgs(1).withLongOpt("username").
                withDescription("Data sources server user name").create());
        options.addOption(OptionBuilder.withArgName(DATASOURCES_PASSWORD_ARGUMENT).hasArgs(1).withLongOpt("password").
                withDescription("Data sources server password").create());
        try {
            return parser.parse(options, args);
        } catch (ParseException e) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp(getClass().getSimpleName(), options);
            throw e;
        }
    }

    public static void main(String[] args) throws Exception {
        new UpdateCatalog().run(args);
    }
}
