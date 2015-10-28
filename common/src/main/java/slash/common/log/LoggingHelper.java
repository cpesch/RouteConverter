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

package slash.common.log;

import java.io.*;
import java.util.logging.*;

import static java.util.logging.Level.*;

/**
 * Allows to control log output
 *
 * @author Christian Pesch
 */

public class LoggingHelper {
    private static PrintStream stdout = System.out, stderr = System.err;
    private static final int LOG_SIZE = 5* 1024 * 1024;
    private static LoggingHelper instance;

    private LoggingHelper() {
    }

    public static LoggingHelper getInstance() {
        if (instance == null)
            instance = new LoggingHelper();
        return instance;
    }

    public void logToFile() {
        System.out.println("Logging to " + getLogFile().getAbsolutePath());
        logAsDefault();

        Logger logger = Logger.getLogger("");
        try {
            FileHandler handler = new FileHandler("%t/RouteConverter.log", LOG_SIZE, 1, true);
            handler.setLevel(ALL);
            handler.setFilter(FILTER);
            handler.setFormatter(new SimpleFormatter());
            logger.addHandler(handler);
        } catch (IOException e) {
            System.err.println("Cannot configure file logging");
            e.printStackTrace();
        }
        // using ALL brings up a colored JavaFX WebView
        // logger.setLevel(ALL);
        redirectStdOutAndErrToLog();
    }

    public void logToConsole() {
        // to avoid cycles between logging and stdout
        resetStdOutAndErr();
        logAsDefault();

        Handler handler = new ConsoleHandler();
        handler.setLevel(ALL);
        handler.setFilter(FILTER);
        handler.setFormatter(new SimpleFormatter());
        Logger logger = Logger.getLogger("");
        logger.addHandler(handler);
        // using ALL brings up a colored JavaFX WebView
        // logger.setLevel(ALL);
    }

    public void logAsDefault() {
        LogManager.getLogManager().reset();
    }

    public String getLogFileAsString() {
        logAsDefault();

        File logFile = getLogFile();
        String logAsString;
        try {
            logAsString = readFile(logFile);
        } catch (IOException e) {
            logAsString = "Cannot read file " + logFile + ":" + e;
        }

        logToFile();
        return logAsString;
    }

    private static final Filter FILTER = new Filter() {
        public boolean isLoggable(LogRecord record) {
            return record.getLoggerName().startsWith("slash");
        }
    };

    private File getLogFile() {
        return new File(System.getProperty("java.io.tmpdir"), "RouteConverter.log");
    }

    private String readFile(File file) throws IOException {
        StringBuilder buffer = new StringBuilder();

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            while (buffer.length() < LOG_SIZE) {
                String line = reader.readLine();
                if (line == null)
                    break;
                buffer.append(line).append("\n");
            }
        }

        return buffer.toString();
    }

    private void redirectStdOutAndErrToLog() {
        Logger logger = Logger.getLogger("stdout");
        LoggingOutputStream los = new LoggingOutputStream(logger, INFO);
        System.setOut(new PrintStream(los, true));

        logger = Logger.getLogger("stderr");
        los = new LoggingOutputStream(logger, SEVERE);
        System.setErr(new PrintStream(los, true));
    }

    private void resetStdOutAndErr() {
        System.setOut(stdout);
        System.setErr(stderr);
    }
}
