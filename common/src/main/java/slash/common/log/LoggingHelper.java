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

import static java.util.logging.Level.ALL;

/**
 * Allows to control log output
 *
 * @author Christian Pesch
 */

public class LoggingHelper {
    private static PrintStream stdout = System.out, stderr = System.err;
    private static final int LOG_SIZE = 1024 * 1024;
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

        FileHandler handler = null;
        try {
            handler = new FileHandler("%t/RouteConverter.log", LOG_SIZE, 1, true);
            handler.setLevel(ALL);
            handler.setFilter(FILTER);
            handler.setFormatter(new SimpleFormatter());
        } catch (IOException e) {
            System.err.println("Cannot configure file logging");
            e.printStackTrace();
        }
        Logger logger = Logger.getLogger("");
        logger.addHandler(handler);
        logger.setLevel(ALL);
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
        logger.setLevel(ALL);
    }

    public void logAsDefault() {
        LogManager.getLogManager().reset();
    }


    public String getLogFileAsString() {
        logAsDefault();
        String logAsString = readFile(getLogFile());
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

    private String readFile(File file) {
        StringBuilder buffer = new StringBuilder();

        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(file));
            while (buffer.length() < LOG_SIZE) {
                String line = reader.readLine();
                if (line == null)
                    break;
                buffer.append(line).append("\n");
            }
        } catch (IOException e) {
            System.err.println("Cannot read file " + file.getAbsolutePath());
            e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    // intentionally left empty
                }
            }
        }

        return buffer.toString();
    }


    private void redirectStdOutAndErrToLog() {
        Logger logger = Logger.getLogger("stdout");
        LoggingOutputStream los = new LoggingOutputStream(logger, Level.INFO);
        System.setOut(new PrintStream(los, true));

        logger = Logger.getLogger("stderr");
        los = new LoggingOutputStream(logger, Level.SEVERE);
        System.setErr(new PrintStream(los, true));
    }

    private void resetStdOutAndErr() {
        System.setOut(stdout);
        System.setErr(stderr);
    }
}
