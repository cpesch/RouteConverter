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

package slash.navigation.babel;

import slash.navigation.BaseNavigationFormat;
import slash.navigation.BaseNavigationPosition;
import slash.navigation.RouteCharacteristics;
import slash.navigation.gpx.Gpx10Format;
import slash.navigation.gpx.GpxRoute;
import slash.navigation.gpx.GpxPosition;
import slash.navigation.gpx.Gpx11Format;
import slash.navigation.util.Externalization;
import slash.navigation.util.Platform;

import java.io.*;
import java.util.List;
import java.util.Calendar;
import java.util.prefs.Preferences;
import java.util.logging.Logger;

/**
 * The base of all GPSBabel based formats.
 *
 * @author Christian Pesch
 */

public abstract class BabelFormat extends BaseNavigationFormat<GpxRoute> {
    private static final Logger log = Logger.getLogger(BabelFormat.class.getName());
    private static final Preferences preferences = Preferences.userNodeForPackage(BabelFormat.class);
    private static final String BABEL_PATH_PREFERENCE = "babelPath";
    private static final String BABEL_INTERFACE_FORMAT_NAME = "gpx";
    private Gpx10Format gpxFormat;

    private Gpx10Format getGpxFormat() {
        if (gpxFormat == null)
            gpxFormat = createGpxFormat();
        return gpxFormat;
    }

    protected Gpx10Format createGpxFormat() {
        return new Gpx10Format();
    }

    public static String getBabelPathPreference() {
        return preferences.get(BABEL_PATH_PREFERENCE, "");
    }

    public static void setBabelPathPreference(String babelPathPreference) {
        preferences.put(BABEL_PATH_PREFERENCE, babelPathPreference);
    }

    protected abstract String getBabelFormatName();

    protected String getBabelOptions() {
        return "-r -w -t";
    }

    public int getMaximumPositionCount() {
        return UNLIMITED_MAXIMUM_POSITION_COUNT;
    }

    public <P extends BaseNavigationPosition> GpxRoute createRoute(RouteCharacteristics characteristics, String name, List<P> positions) {
        return new GpxRoute(new Gpx11Format(), characteristics, name, null, (List<GpxPosition>) positions);
    }

    private void readStream(InputStream inputStream, String streamName) throws IOException {
        byte buffer[] = new byte[2048];
        int count = 0;
        while (inputStream.available() > 0 && count < buffer.length) {
            buffer[count++] = (byte) inputStream.read();
        }
        String output = new String(buffer).trim();
        log.info("Read " + count + " bytes of " + streamName + " output: '" + output + "'");
    }

    private boolean startBabel(File source, String sourceFormat,
                               File target, String targetFormat,
                               String commandLineFlags) throws IOException {
        File babel = getBabelPathPreference() != null ? new File(getBabelPathPreference()) : null;
        boolean babelExists = false;
        try {
            babelExists = babel.exists();
        } catch (SecurityException se) {
            // that's ok, we assume that the file does not exist because we can't access it
        }
        if (babel == null || !babelExists) {
            if (Platform.isWindows()) {
                Externalization.extractFile(getClass(), "libexpat.dll");
                babel = Externalization.extractFile(getClass(), "gpsbabel.exe");
            }
            if (Platform.isLinux()) {
                babel = Externalization.extractFile(getClass(), "gpsbabel-linux-glibc2.3");
            }
            if (Platform.isMac()) {
                babel = Externalization.extractFile(getClass(), "gpsbabel-mac");
            }
        }

        if (babel == null || !babel.exists())
            return false;

        String command = babel.getAbsolutePath() + " -D9 " + commandLineFlags +
                " -i " + sourceFormat + " -f \"" + source.getAbsolutePath() + "\"" +
                " -o " + targetFormat + " -F \"" + target.getAbsolutePath() + "\"";

        log.fine("Executing '" + command + "'");

        if (Platform.isLinux() || Platform.isMac()) {
            File temp = executeViaShell(babel.getAbsolutePath(), command);
            command = "/bin/sh " + temp.getAbsolutePath();
        }

        int exitCode = execute(babel.getAbsolutePath(), command);
        log.info("Executed '" + command + "' with exit code: " + exitCode + " target exists: " + target.exists());
        return exitCode == 0;
    }

    private File executeViaShell(String babel, String command) throws IOException {
        File temp = File.createTempFile("gpsbabel", ".sh");
        temp.deleteOnExit();
        BufferedWriter writer = new BufferedWriter(new FileWriter(temp));
        writer.write("#!/bin/sh");
        writer.newLine();
        writer.write("`which chmod` a+x \"" + babel + "\"");
        writer.newLine();
        writer.write(command);
        writer.newLine();
        writer.flush();
        writer.close();
        return temp;
    }

    private static final int COMMAND_EXECUTION_TIMEOUT = 5000;
    private static final int COMMAND_EXECUTION_RECHECK_INTERVAL = 250;

    private int execute(String babelPath, String command) throws IOException {
        Process process;
        try {
            process = Runtime.getRuntime().exec(command);
        } catch (IOException e) {
            throw new BabelException("Cannot execute '" + command + "'", babelPath, e);
        }

        InputStream inputStream = new BufferedInputStream(process.getInputStream());
        InputStream errorStream = new BufferedInputStream(process.getErrorStream());

        boolean hasExitValue = false;
        int currentTimeout = COMMAND_EXECUTION_TIMEOUT;
        int exitValue = -1;

        while (!hasExitValue) {
            try {
                while (inputStream.available() > 0 || errorStream.available() > 0) {
                    readStream(inputStream, "input");
                    readStream(errorStream, "error");
                }
            }
            catch (IOException e) {
                log.severe("Couldn't read response: " + e.getMessage());
            }

            try {
                exitValue = process.exitValue();
                hasExitValue = true;
            }
            catch (IllegalThreadStateException e) {
                try {
                    Thread.sleep(COMMAND_EXECUTION_RECHECK_INTERVAL);
                    currentTimeout = currentTimeout - COMMAND_EXECUTION_RECHECK_INTERVAL;
                    if (currentTimeout < 0 && currentTimeout >= -COMMAND_EXECUTION_RECHECK_INTERVAL) {
                        log.severe("Command doesn't terminate. Shutting down command...");
                        process.destroy();
                    } else if (currentTimeout < 0) {
                        log.severe("Command still doesn't terminate");
                        Thread.sleep(COMMAND_EXECUTION_RECHECK_INTERVAL);
                    }
                } catch (InterruptedException e1) {
                    // doesn't matter
                }
            }
        }

        if (!hasExitValue) {
            log.severe("Command doesn't return exit value. Shutting down command...");
            process.destroy();
        }

        try {
            readStream(inputStream, "input");
            readStream(errorStream, "error");
        }
        catch (IOException e) {
            log.severe("Couldn't read final response: " + e.getMessage());
        }

        return exitValue;
    }

    public List<GpxRoute> read(File source, Calendar startDate) throws IOException {
        List<GpxRoute> result = null;
        File target = File.createTempFile(source.getName(), "." + BABEL_INTERFACE_FORMAT_NAME);
        boolean successful = startBabel(source, getBabelFormatName(), target, BABEL_INTERFACE_FORMAT_NAME, "-r -w -t");
        if (successful) {
            log.fine("Successfully converted " + source + " to " + target);
            result = getGpxFormat().read(target, startDate);
        }
        if (target.exists())
            target.delete();
        return result;
    }

    public void write(GpxRoute route, File target, int startIndex, int endIndex, boolean numberPositionNames) throws IOException {
        File source = File.createTempFile(target.getName(), "." + BABEL_INTERFACE_FORMAT_NAME);
        getGpxFormat().write(route, source, startIndex, endIndex, numberPositionNames);
        boolean successful = startBabel(source, BABEL_INTERFACE_FORMAT_NAME, target, getBabelFormatName(), getBabelOptions());
        if (successful)
            log.fine("Successfully converted " + source + " to " + target);
        if (source.exists())
            source.delete();
    }

    public void write(List<GpxRoute> routes, File target) throws IOException {
        File source = File.createTempFile(target.getName(), "." + BABEL_INTERFACE_FORMAT_NAME);
        getGpxFormat().write(routes, source);
        boolean successful = startBabel(source, BABEL_INTERFACE_FORMAT_NAME, target, getBabelFormatName(), getBabelOptions());
        if (successful)
            log.fine("Successfully converted " + source + " to " + target);
        if (source.exists())
            source.delete();
    }
}
