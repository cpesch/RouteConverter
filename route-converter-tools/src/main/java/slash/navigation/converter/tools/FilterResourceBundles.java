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
package slash.navigation.converter.tools;

import org.apache.commons.cli.*;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.MissingResourceException;

import static java.lang.System.exit;

/**
 * Filters resource bundles.
 *
 * @author Christian Pesch
 */

public class FilterResourceBundles {
    private static final String DEFAULT_ARGUMENT = "default";
    private static final String FILTER_ARGUMENT = "filter";

    private String defaultBundle, filterBundle;

    private void run(String[] args) throws Exception {
        CommandLine line = parseCommandLine(args);
        defaultBundle = line.getOptionValue(DEFAULT_ARGUMENT);
        filterBundle = line.getOptionValue(FILTER_ARGUMENT);
        filter();
    }

    private void filter() throws IOException {
        OrderedResourceBundle default0 = new OrderedResourceBundle(new FileInputStream(defaultBundle));
        OrderedResourceBundle filter = new OrderedResourceBundle(new FileInputStream(filterBundle));
        for(String key : default0.getOrderedKeys()) {
            String defaultValue = default0.getString(key);
            try {
                String filterValue = filter.getString(key);
                if (defaultValue.equals(filterValue)) {
                    if (!filter.handleRemoteObject(key).equals(filterValue))
                        throw new IOException("Failed to remove key " + key);
                }
            }
            catch (MissingResourceException e) {
                // intentionally left empty
            }
        }
        filter.store(new FileOutputStream(filterBundle));
    }

    @SuppressWarnings("AccessStaticViaInstance")
    private CommandLine parseCommandLine(String[] args) throws ParseException {
        CommandLineParser parser = new DefaultParser();
        Options options = new Options();
        options.addOption(Option.builder().argName(DEFAULT_ARGUMENT).hasArgs().numberOfArgs(1).required().
                longOpt("default").desc("Default resource bundle").build());
        options.addOption(Option.builder().argName(FILTER_ARGUMENT).hasArgs().numberOfArgs(1).required().
                longOpt("filter").desc("Resource bundle to filter").build());
        try {
            return parser.parse(options, args);
        } catch (ParseException e) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp(getClass().getSimpleName(), options);
            throw e;
        }
    }

    public static void main(String[] args) throws Exception {
        new FilterResourceBundles().run(args);
        exit(0);
    }
}
