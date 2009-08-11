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

package slash.navigation.util;

import java.io.IOException;
import java.io.File;
import java.util.logging.LogManager;

/**
 * Allows to control debug output
 *
 * @author Christian Pesch
 */

public class DebugOutput {
    public static void activate() {
        // System.setProperty("java.util.logging.config.class", DebugOutput.class.getName());
        File logFile = new File(System.getProperty("java.io.tmpdir"), "RouteConverter.log");
        System.out.println("Logging to " + logFile.getAbsolutePath());
        readDebugConfig();
    }

    public static void deactivate() {
        // System.clearProperty("java.util.logging.config.class");
        readDefaultConfig();
    }

    private static void readDebugConfig() {
        try {
            LogManager.getLogManager().readConfiguration(DebugOutput.class.getResourceAsStream("debugoutput.properties"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void readDefaultConfig() {
        try {
            LogManager.getLogManager().readConfiguration();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public DebugOutput() throws SecurityException {
        readDebugConfig();
    }
}
