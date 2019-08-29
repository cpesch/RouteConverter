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

import slash.navigation.base.BaseNavigationFormat;
import slash.navigation.base.ParserContext;
import slash.navigation.base.ParserContextImpl;
import slash.navigation.base.RouteCharacteristics;
import slash.navigation.common.NavigationPosition;
import slash.navigation.gpx.Gpx10Format;
import slash.navigation.gpx.GpxPosition;
import slash.navigation.gpx.GpxRoute;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

import static java.io.File.createTempFile;
import static java.lang.Thread.sleep;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static slash.common.io.Directories.getApplicationDirectory;
import static slash.common.io.Directories.getTemporaryDirectory;
import static slash.common.io.InputOutput.DEFAULT_BUFFER_SIZE;
import static slash.common.io.InputOutput.copyAndClose;
import static slash.common.system.Platform.*;
import static slash.navigation.base.RouteCharacteristics.*;

/**
 * The base of all GPSBabel based formats.
 *
 * @author Christian Pesch, Axel Uhl
 */

public abstract class BabelFormat extends BaseNavigationFormat<GpxRoute> {
    private static final Logger log = Logger.getLogger(BabelFormat.class.getName());
    private static final Preferences preferences = Preferences.userNodeForPackage(BabelFormat.class);
    private static final String BABEL_PATH_PREFERENCE = "babelPath";
    private static final String BABEL_INTERFACE_FORMAT_NAME = "gpx";
    private static final String[] ROUTE_WAYPOINTS_TRACKS = new String[]{"-r", "-w", "-t"};
    private static final String USR_BIN_GPSBABEL = "/usr/bin/gpsbabel";
    private Gpx10Format gpxFormat;

    private Gpx10Format getGpxFormat() {
        if (gpxFormat == null)
            gpxFormat = createGpxFormat();
        return gpxFormat;
    }

    protected Gpx10Format createGpxFormat() {
        return new Gpx10Format(false, true);
    }

    public static String getBabelPathPreference() {
        return preferences.get(BABEL_PATH_PREFERENCE, "");
    }

    public static void setBabelPathPreference(String babelPathPreference) {
        preferences.put(BABEL_PATH_PREFERENCE, babelPathPreference);
    }

    private int getReadCommandExecutionTimeoutPreference() {
        return preferences.getInt("readCommandExecutionTimeout", 10000);
    }

    private int getWriteCommandExecutionTimeOutPreference() {
        return preferences.getInt("writeCommandExecutionTimeout", 30000);
    }

    protected abstract String getFormatName();

    public boolean isWritingRouteCharacteristics() {
        return false;
    }

    protected abstract boolean isStreamingCapable();

    protected String[] getGlobalOptions() {
        return ROUTE_WAYPOINTS_TRACKS;
    }

    @SuppressWarnings({"UnusedDeclaration"})
    protected String getFormatOptions(GpxRoute route) {
        return "";
    }

    protected List<RouteCharacteristics> getBabelCharacteristics() {
        return asList(Route, Track, Waypoints);
    }

    public int getMaximumPositionCount() {
        return UNLIMITED_MAXIMUM_POSITION_COUNT;
    }

    @SuppressWarnings({"unchecked"})
    public <P extends NavigationPosition> GpxRoute createRoute(RouteCharacteristics characteristics, String name, List<P> positions) {
        return new GpxRoute(new Gpx10Format(), characteristics, name, null, (List<GpxPosition>) positions);
    }

    // stream

    private Process execute(String babel, String sourceFormat, String targetFormat, String[] globalFlags) throws IOException {
        List<String> args = new ArrayList<>();
        args.add(babel);
        args.addAll(asList(globalFlags));
        args.addAll(asList("-i", sourceFormat, "-f", "-",
                "-o", targetFormat, "-F", "-"));
        log.info("Executing '" + args + "'");

        args = considerShellScriptForBabel(babel, args);

        try {
            return Runtime.getRuntime().exec(args.toArray(new String[0]));
        } catch (IOException e) {
            throw new BabelException("Cannot execute '" + args + "'", babel, e);
        }
    }

    private Thread observeProcess(final Process process, final int commandExecutionTimeout) {
        return new Thread(new Runnable() {
            public void run() {
                try {
                    sleep(commandExecutionTimeout);
                } catch (InterruptedException e) {
                    // intentionally left empty
                }
                try {
                    int exitValue = process.exitValue();
                    log.fine("gpsbabel process terminated with exit value " + exitValue);
                } catch (IllegalThreadStateException itse) {
                    log.warning("gpsbabel process for format " + getFormatName() + " didn't terminate after " + commandExecutionTimeout + "ms; destroying it");
                    process.destroy();
                }
            }
        }, "BabelObserver");
    }

    private void pumpStream(final InputStream input, final OutputStream output, final String streamName, final boolean closeOutput) {
        new Thread(new Runnable() {
            public void run() {
                try {
                    try {
                        byte buffer[] = new byte[DEFAULT_BUFFER_SIZE];
                        int count = 0;
                        while (count >= 0) {
                            count = input.read(buffer);
                            if (count > 0) {
                                output.write(buffer, 0, count);
                                String output = new String(buffer).trim();
                                log.fine("Read " + count + " bytes of " + streamName + " from gpsbabel process: '" + output + "'");
                            }
                        }
                    } finally {
                        input.close();
                        if (closeOutput)
                            output.close();
                    }
                } catch (IOException e) {
                    log.fine("Could not pump " + streamName + " of gpsbabel process: " + e);
                }
            }
        }, "BabelStreamPumper-" + streamName).start();
    }

    private Process startBabel(InputStream source, String sourceFormat, String[] commandLineFlags) throws IOException {
        String babel = findBabel();
        Process process = execute(babel, sourceFormat, BABEL_INTERFACE_FORMAT_NAME, commandLineFlags);
        pumpStream(source, process.getOutputStream(), "input", true);
        pumpStream(process.getErrorStream(), System.err, "error", false);
        return process;
    }

    private void readStream(InputStream source, ParserContext<GpxRoute> context) throws IOException {
        Process process = startBabel(source, getFormatName(), getGlobalOptions());
        Thread observer = observeProcess(process, getReadCommandExecutionTimeoutPreference());
        observer.start();
        InputStream target = process.getInputStream();
        getGpxFormat().read(target, context);
        observer.interrupt();
        target.close();
    }

    // temp file

    private String escapeFilePathWithSpaces(String filePath) {
        if (isLinux() || isMac()) {
            filePath = "\"" + filePath + "\"";
        }
        return filePath;
    }

    private boolean startBabel(File source, String sourceFormat,
                               File target, String targetFormat,
                               String[] globalFlags, String formatFlags,
                               int timeout) throws IOException {
        String babel = findBabel();
        List<String> args = new ArrayList<>();
        args.add(babel);
        args.addAll(asList(globalFlags));
        args.addAll(asList("-i", sourceFormat,
                "-f", escapeFilePathWithSpaces(source.getAbsolutePath()),
                "-o", escapeFilePathWithSpaces(targetFormat + formatFlags),
                "-F", escapeFilePathWithSpaces(target.getAbsolutePath())));
        log.info("Executing '" + args + "'");

        args = considerShellScriptForBabel(babel, args);

        int exitValue = execute(babel, args, timeout);
        log.fine("Executed '" + args + "' with exit value: " + exitValue + " target exists: " + target.exists());
        return exitValue == 0;
    }

    private static final int COMMAND_EXECUTION_RECHECK_INTERVAL = 250;

    private int execute(String babelPath, List<String> args, int timeout) throws IOException {
        Process process;
        try {
            process = Runtime.getRuntime().exec(args.toArray(new String[0]));
        } catch (IOException e) {
            throw new BabelException("Cannot execute '" + args + "'", babelPath, e);
        }

        InputStream inputStream = new BufferedInputStream(process.getInputStream());
        InputStream errorStream = new BufferedInputStream(process.getErrorStream());

        boolean hasExitValue = false;
        int exitValue = -1;

        while (!hasExitValue) {
            try {
                while (inputStream.available() > 0 || errorStream.available() > 0) {
                    readStream(inputStream, "input");
                    readStream(errorStream, "error");
                }
            } catch (IOException e) {
                log.severe("Couldn't read response: " + e);
            }

            try {
                exitValue = process.exitValue();
                hasExitValue = true;
            } catch (IllegalThreadStateException e) {
                try {
                    sleep(COMMAND_EXECUTION_RECHECK_INTERVAL);
                    timeout = timeout - COMMAND_EXECUTION_RECHECK_INTERVAL;
                    if (timeout < 0 && timeout >= -COMMAND_EXECUTION_RECHECK_INTERVAL) {
                        log.severe("Command doesn't terminate. Shutting down args...");
                        process.destroy();
                    } else if (timeout < 0) {
                        log.severe("Command still doesn't terminate");
                        sleep(COMMAND_EXECUTION_RECHECK_INTERVAL);
                    }
                } catch (InterruptedException e1) {
                    // doesn't matter
                }
            }
        }

        try {
            readStream(inputStream, "input");
            readStream(errorStream, "error");
        } catch (IOException e) {
            log.severe("Couldn't read final response: " + e);
        }

        log.info("Executed '" + process.toString() + "' with exit value: " + exitValue);
        return exitValue;
    }

    private void readStream(InputStream inputStream, String streamName) throws IOException {
        byte buffer[] = new byte[DEFAULT_BUFFER_SIZE];
        int count = 0;
        while (inputStream.available() > 0 && count < buffer.length) {
            buffer[count++] = (byte) inputStream.read();
        }
        String output = new String(buffer).trim();
        log.fine("Read " + count + " bytes of " + streamName + " output: '" + output + "'");
    }

    private void readFile(InputStream source, ParserContext<GpxRoute> context) throws IOException {
        File sourceFile = null, targetFile = null;
        try {
            sourceFile = createTempFile("babel-read-source", "." + getFormatName(), getTemporaryDirectory());
            copyAndClose(source, new FileOutputStream(sourceFile));
            targetFile = createTempFile("babel-read-target", "." + BABEL_INTERFACE_FORMAT_NAME, getTemporaryDirectory());
            boolean successful = startBabel(sourceFile, getFormatName(), targetFile, BABEL_INTERFACE_FORMAT_NAME, getGlobalOptions(), "", getReadCommandExecutionTimeoutPreference());
            if (successful) {
                try (InputStream target = new IllegalCharacterFilterInputStream(new FileInputStream(targetFile))) {
                    getGpxFormat().read(target, context);
                    log.fine("Successfully converted " + sourceFile + " to " + targetFile);
                }
            }
        } finally {
            delete(sourceFile);
            delete(targetFile);
        }
    }

    // both

    private File checkIfBabelExists(String path) {
        File file = new File(path);
        return file.exists() ? file : null;
    }

    private String findBabel() throws IOException {
        // 1. check if there is a preference and try to find its file
        File babelFile = getBabelPathPreference() != null ? new File(getBabelPathPreference()) : null;
        if (babelFile == null || !babelFile.exists()) {
            babelFile = null;
        }

        // 2. look for "c:\Program Files\GPSBabel\gpsbabel.exe"
        if (babelFile == null && isWindows()) {
            babelFile = checkIfBabelExists(System.getenv("ProgramFiles") + "\\GPSBabel\\gpsbabel.exe");
        }

        // 3. look for "c:\Program Files (x86)\GPSBabel\gpsbabel.exe"
        if (babelFile == null && isWindows()) {
            babelFile = checkIfBabelExists(System.getenv("ProgramFiles(x86)") + "\\GPSBabel\\gpsbabel.exe");
        }

        // 4. look for "/Applications/GPSBabelFE.app/Contents/MacOS/gpsbabel"
        if (babelFile == null && isMac()) {
            babelFile = checkIfBabelExists("/Applications/GPSBabelFE.app/Contents/MacOS/gpsbabel");
        }

        // 5. look for "/usr/bin/gpsbabel" in path
        if (babelFile == null && !isWindows()) {
            babelFile = checkIfBabelExists(USR_BIN_GPSBABEL);
        }

        // 6. look for ApplicationDirectory\\thirdparty\\gpsbabel.exe
        if (babelFile == null && isWindows()) {
            babelFile = checkIfBabelExists(getApplicationDirectory("thirdparty/gpsbabel") + "\\gpsbabel.exe");
        }

        // 7. look for ApplicationDirectory/thirdparty/gpsbabel
        if (babelFile == null && !isWindows()) {
            babelFile = checkIfBabelExists(getApplicationDirectory("thirdparty/gpsbabel") + "/gpsbabel");
        }

        // 8. look for unqualified "gpsbabel"
        return babelFile != null ? babelFile.getCanonicalPath() : "gpsbabel";
    }

    private List<String> considerShellScriptForBabel(String babel, List<String> args) throws IOException {
        if (isLinux() || isMac()) {
            File shellScript = createShellScript(babel, args);
            args = asList("/bin/sh", shellScript.getAbsolutePath());
        }
        return args;
    }

    private File createShellScript(String babelPath, List<String> args) throws IOException {
        File temp = createTempFile("gpsbabel", ".sh", getTemporaryDirectory());
        temp.deleteOnExit();
        try(BufferedWriter writer = new BufferedWriter(new FileWriter(temp))) {
            writer.write("#!/bin/sh");
            writer.newLine();
            // help Mac gpsbabel find its QtCore library
            if (isMac()) {
                writer.write("cd `dirname \"" + babelPath + "\"`");
                writer.newLine();
            }
            for (String arg : args) {
                writer.write(arg);
                writer.write(" ");
            }
            writer.newLine();
        }
        return temp;
    }

    // filter/sanitizing after reading

    private List<GpxRoute> filterValidRoutes(List<GpxRoute> routes) {
        if (routes == null)
            return null;

        List<GpxRoute> result = new ArrayList<>();
        for (GpxRoute aRoute : routes) {
            GpxRoute route = sanitizeRouteAfterReading(aRoute);
            if (isValidRoute(route))
                result.add(route);
        }
        return result.size() > 0 ? result : null;
    }

    protected boolean isValidRoute(GpxRoute route) {
        return true;
    }

    protected GpxRoute sanitizeRouteAfterReading(GpxRoute route) {
        return route;
    }

    protected void delete(File file) {
        if (file != null && file.exists()) {
            if (!file.delete())
                log.warning("Cannot delete babel file " + file);
        }
    }


    public void read(InputStream source, ParserContext<GpxRoute> context) throws IOException {
        ParserContext<GpxRoute> gpxContext = new ParserContextImpl<>();
        if (isStreamingCapable()) {
            readStream(source, gpxContext);
        } else {
            readFile(source, gpxContext);
        }
        List<GpxRoute> result = filterValidRoutes(gpxContext.getRoutes());
        if (result != null && result.size() > 0) {
            context.appendRoutes(result);
            log.fine("Successfully converted " + getName() + " to " + BABEL_INTERFACE_FORMAT_NAME);
        }
    }

    protected List<GpxRoute> modifyBeforeWriting(List<GpxRoute> routes) {
        return routes;
    }

    public void write(GpxRoute route, OutputStream target, int startIndex, int endIndex) throws IOException {
        File sourceFile = null, targetFile = null;
        try {
            sourceFile = createTempFile("babel-write-source", "." + BABEL_INTERFACE_FORMAT_NAME, getTemporaryDirectory());
            GpxRoute write = modifyBeforeWriting(singletonList(route)).get(0);
            getGpxFormat().write(write, new FileOutputStream(sourceFile), startIndex, endIndex, getBabelCharacteristics());
            targetFile = createTempFile("babel-write-target", getExtension(), getTemporaryDirectory());

            boolean successful = startBabel(sourceFile, BABEL_INTERFACE_FORMAT_NAME, targetFile, getFormatName(), getGlobalOptions(), getFormatOptions(route), getWriteCommandExecutionTimeOutPreference());
            if (!successful)
                throw new IOException("Could not convert " + sourceFile + " to " + targetFile);

            copyAndClose(new FileInputStream(targetFile), target);
            log.info("Successfully converted " + sourceFile + " to " + targetFile);
        } finally {
            delete(sourceFile);
            delete(targetFile);
        }
    }

    public void write(List<GpxRoute> routes, OutputStream target) throws IOException {
        File sourceFile = null, targetFile = null;
        try {
            sourceFile = createTempFile("babel-write-all-source", "." + BABEL_INTERFACE_FORMAT_NAME, getTemporaryDirectory());
            getGpxFormat().write(modifyBeforeWriting(routes), new FileOutputStream(sourceFile));
            targetFile = createTempFile("babel-write-all-target", getExtension(), getTemporaryDirectory());

            boolean successful = startBabel(sourceFile, BABEL_INTERFACE_FORMAT_NAME, targetFile, getFormatName(), getGlobalOptions(), getFormatOptions(routes.get(0)), getWriteCommandExecutionTimeOutPreference());
            if (!successful)
                throw new IOException("Could not convert " + sourceFile + " to " + targetFile);

            copyAndClose(new FileInputStream(targetFile), target);
            log.info("Successfully converted " + sourceFile + " to " + targetFile);
        } finally {
            delete(sourceFile);
            delete(targetFile);
        }
    }
}
