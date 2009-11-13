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

import slash.common.io.CompactCalendar;
import slash.common.io.Externalization;
import slash.common.io.InputOutput;
import slash.common.io.Platform;
import slash.navigation.BaseNavigationFormat;
import slash.navigation.BaseNavigationPosition;
import slash.navigation.RouteCharacteristics;
import slash.navigation.gpx.Gpx10Format;
import slash.navigation.gpx.Gpx11Format;
import slash.navigation.gpx.GpxPosition;
import slash.navigation.gpx.GpxRoute;

import java.io.*;
import java.util.List;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

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
    private Gpx10Format gpxFormat;

    private Gpx10Format getGpxFormat() {
        if (gpxFormat == null)
            gpxFormat = createGpxFormat();
        return gpxFormat;
    }

    protected Gpx10Format createGpxFormat() {
        return new Gpx10Format(false);
    }

    public static String getBabelPathPreference() {
        return preferences.get(BABEL_PATH_PREFERENCE, "");
    }

    public static void setBabelPathPreference(String babelPathPreference) {
        preferences.put(BABEL_PATH_PREFERENCE, babelPathPreference);
    }

    protected abstract String getBabelFormatName();
    protected abstract boolean isStreamingCapable();

    protected String getBabelOptions() {
        return "-r -w -t";
    }

    public int getMaximumPositionCount() {
        return UNLIMITED_MAXIMUM_POSITION_COUNT;
    }

    @SuppressWarnings({"unchecked"})
    public <P extends BaseNavigationPosition> GpxRoute createRoute(RouteCharacteristics characteristics, String name, List<P> positions) {
        return new GpxRoute(new Gpx11Format(), characteristics, name, null, (List<GpxPosition>) positions);
    }


    private void pumpStream(final InputStream input, final OutputStream output, final String streamName) {
        new Thread(new Runnable() {
            public void run() {
                try {
                    try {
                        byte buffer[] = new byte[2048];
                        int count = 0;
                        while (count >= 0) {
                            count = input.read(buffer);
                            if (count > 0) {
                                output.write(buffer, 0, count);
                                String output = new String(buffer).trim();
                                log.fine("Read " + count + " bytes of " + streamName + " output from gpsbabel process: '" + output + "'");
                            }
                        }
                    } finally {
                        input.close();
                        output.close();
                    }
                }
                catch (IOException e) {
                    log.severe("Could not pump " + streamName + " of gpsbabel process: " + e.getMessage());
                }
            }
        }, "BabelStreamPumper-" + streamName).start();
    }

    private Process execute(String babel, String inputFormatName, String outputFormatName,
                            String commandLineFlags) throws IOException {
        String command = babel + " " + commandLineFlags
                + " -i " + inputFormatName + " -f - -o " + outputFormatName
                + " -F -";
        log.info("Executing '" + command + "'"); 

        command = considerShellScriptForBabel(babel, command);

        try {
            Process process = Runtime.getRuntime().exec(command);
            execute(process, COMMAND_EXECUTION_TIMEOUT);
            return process;
        } catch (IOException e) {
            throw new BabelException("Cannot execute '" + command + "'", babel, e);
        }
    }

    private void execute(final Process process, final int commandExecutionTimeout) {
        new Thread(new Runnable() {
            public void run() {
                try {
                    Thread.sleep(commandExecutionTimeout);
                } catch (InterruptedException e) {
                    log.info("Interrupted while waiting for gpsbabel process to finish");
                }
                try {
                    int exitValue = process.exitValue();
                    log.info("gpsbabel process terminated with exit value " + exitValue);
                } catch (IllegalThreadStateException itse) {
                    log.info("gpsbabel process for format " + getBabelFormatName() + " didn't terminate after " + commandExecutionTimeout + "ms; destroying it");
                    process.destroy();
                }
            }
        }, "BabelExecutor").start();
    }

    private InputStream startBabel(final InputStream source, String sourceFormat,
                                   String targetFormat, String commandLineFlags) throws IOException {
        String babel = findBabel();
        Process process = execute(babel, sourceFormat, targetFormat, commandLineFlags);
        pumpStream(source, process.getOutputStream(), "stdin");
        pumpStream(process.getErrorStream(), System.err, "stderr");
        return process.getInputStream();
    }

    private void readStream(InputStream inputStream, String streamName) throws IOException {
        byte buffer[] = new byte[2048];
        int count = 0;
        while (inputStream.available() > 0 && count < buffer.length) {
            buffer[count++] = (byte) inputStream.read();
        }
        String output = new String(buffer).trim();
        log.fine("Read " + count + " bytes of " + streamName + " output: '" + output + "'");
    }

    private boolean startBabel(File source, String sourceFormat,
                               File target, String targetFormat,
                               String commandLineFlags) throws IOException {
        String babel = findBabel();
        String command = babel + " " + commandLineFlags +
                " -i " + sourceFormat + " -f \"" + source.getAbsolutePath() + "\"" +
                " -o " + targetFormat + " -F \"" + target.getAbsolutePath() + "\"";
        log.info("Executing '" + command + "'");

        command = considerShellScriptForBabel(babel, command);

        int exitValue = execute(babel, command);
        log.info("Executed '" + command + "' with exit value: " + exitValue + " target exists: " + target.exists());
        return exitValue == 0;
    }

    private String findBabel() throws IOException {
        // 1. check if there is a preference and try to find its file
        File babelFile = getBabelPathPreference() != null ? new File(getBabelPathPreference()) : null;
        if (babelFile == null || !babelFile.exists()) {
            babelFile = null;
        }

        // 2. look for "/usr/bin/gpsbabel" in path
        if(babelFile == null) {
            babelFile = new File("/usr/bin/gpsbabel");
            if (!babelFile.exists()) {
                babelFile = null;
            }
        }

        // 3. extract from classpath into temp directrory and execute there
        if(babelFile == null) {
            String path = "bin/" + Platform.getOsName() + "/" + Platform.getOsArchitecture() + "/";
            if (Platform.isWindows()) {
                Externalization.extractFile(path + "libexpat.dll");
                babelFile = Externalization.extractFile(path + "gpsbabel.exe");
            } else if (Platform.isLinux() || Platform.isMac()) {
                babelFile = Externalization.extractFile(path + "gpsbabel");
            }
        }

        // 4. look for unqualified "gpsbabel"
        return babelFile != null ? babelFile.getAbsolutePath() : "gpsbabel";
    }

    private String considerShellScriptForBabel(String babel, String command) throws IOException {
        if (Platform.isLinux() || Platform.isMac()) {
            File shellScript = createShellScript(babel, command);
            command = "/bin/sh " + shellScript.getAbsolutePath();
        }
        return command;
    }

    private File createShellScript(String babelPath, String command) throws IOException {
        File temp = File.createTempFile("gpsbabel", ".sh");
        temp.deleteOnExit();
        BufferedWriter writer = new BufferedWriter(new FileWriter(temp));
        writer.write("#!/bin/sh");
        writer.newLine();
        writer.write("`which chmod` a+x \"" + babelPath + "\"");
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

        log.info("Executed '" + process + "' with exit value: " + exitValue);
        return exitValue;
    }

    public List<GpxRoute> read(InputStream in, CompactCalendar startDate) throws IOException {
        if (isStreamingCapable()) {
            InputStream target = startBabel(in, getBabelFormatName(), BABEL_INTERFACE_FORMAT_NAME, "-r -w -t");
            List<GpxRoute> result = getGpxFormat().read(target, startDate);
            if (result != null && result.size() > 0)
                log.fine("Successfully converted " + getName() + " to " + BABEL_INTERFACE_FORMAT_NAME + " stream");
            return result;
        } else {
            List<GpxRoute> result = null;
            File source = File.createTempFile("babelsource", "." + getBabelFormatName());
            InputOutput inputOutput = new InputOutput(in, new FileOutputStream(source));
            inputOutput.start();
            inputOutput.close();
            File target = File.createTempFile("babeltarget", "." + BABEL_INTERFACE_FORMAT_NAME);
            boolean successful = startBabel(source, getBabelFormatName(), target, BABEL_INTERFACE_FORMAT_NAME, "-r -w -t");
            if (successful) {
                log.fine("Successfully converted " + source + " to " + target);
                result = getGpxFormat().read(new FileInputStream(target), startDate);
            }
            if (source.exists()) {
                if (!source.delete())
                    log.warning("Cannot delete source file " + source);
            }
            if (target.exists()) {
                if (!target.delete())
                    log.warning("Cannot delete target file " + target);
            }
            return result;
        }
    }

    public void write(GpxRoute route, OutputStream target, int startIndex, int endIndex) throws IOException {
        File source = File.createTempFile("babelsource", "." + BABEL_INTERFACE_FORMAT_NAME);
        getGpxFormat().write(route,  new FileOutputStream(source), startIndex, endIndex);
        File targetFile = File.createTempFile("babeltarget", getExtension());
        boolean successful = startBabel(source, BABEL_INTERFACE_FORMAT_NAME, targetFile, getBabelFormatName(), getBabelOptions());
        if (successful) {
            log.fine("Successfully converted " + source + " to " + target);
            new InputOutput(new FileInputStream(targetFile), target).start();
        }
        if (targetFile.exists()) {
            if (!targetFile.delete())
                log.warning("Cannot delete target file " + targetFile);
        }
        if (source.exists()) {
            if (!source.delete())
                log.warning("Cannot delete source file " + source);
        }
    }

    public void write(List<GpxRoute> routes, File target) throws IOException {
        File source = File.createTempFile(target.getName(), "." + BABEL_INTERFACE_FORMAT_NAME);
        getGpxFormat().write(routes, source);
        boolean successful = startBabel(source, BABEL_INTERFACE_FORMAT_NAME, target, getBabelFormatName(), getBabelOptions());
        if (successful)
            log.fine("Successfully converted " + source + " to " + target);
        if (source.exists()) {
            if (!source.delete())
                log.warning("Cannot delete source file " + source);
        }
    }
}
