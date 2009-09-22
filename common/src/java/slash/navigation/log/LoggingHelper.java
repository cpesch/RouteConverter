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

package slash.navigation.log;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.logging.*;

/**
 * Allows to control log output
 *
 * @author Christian Pesch
 */

public class LoggingHelper {
    private static PrintStream stdout = System.out, stderr = System.err;

    public static void logToFile() {
        File logFile = new File(System.getProperty("java.io.tmpdir"), "RouteConverter.log");
        System.out.println("Logging to " + logFile.getAbsolutePath());
        readDebugConfig();
        redirectStdOutAndErrToLog();
    }


    public static void logToStdOut() {
        // to avoid cycles between logging and stdout
        resetStdOutAndErr();
        logAsDefault();

        Handler consoleHandler = new ConsoleHandler();
        consoleHandler.setLevel(Level.ALL);
        consoleHandler.setFilter(new Filter() {
            public boolean isLoggable(LogRecord record) {
                return record.getLoggerName().startsWith("slash");
            }
        });
        consoleHandler.setFormatter(new SimpleFormatter());
        Logger logger = Logger.getLogger("");
        logger.addHandler(consoleHandler);
        logger.setLevel(Level.ALL);
    }

    private static void readDebugConfig() {
        try {
            LogManager.getLogManager().readConfiguration(LoggingHelper.class.getResourceAsStream("logging.properties"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void logAsDefault() {
        try {
            LogManager.getLogManager().readConfiguration();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void redirectStdOutAndErrToLog() {
        Logger logger = Logger.getLogger("stdout");
        LoggingOutputStream los = new LoggingOutputStream(logger, Level.INFO);
        System.setOut(new PrintStream(los, true));

        logger = Logger.getLogger("stderr");
        los = new LoggingOutputStream(logger, Level.SEVERE);
        System.setErr(new PrintStream(los, true));
    }

    private static void resetStdOutAndErr() {
        System.setOut(stdout);
        System.setErr(stderr);
    }
}
