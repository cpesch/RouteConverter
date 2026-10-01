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

    Copyright (C) 2026 Christian Pesch. All Rights Reserved.
*/

package slash.navigation.converter.cmdline;

import slash.navigation.base.BaseNavigationFormat;
import slash.navigation.base.BaseRoute;
import slash.navigation.base.CmdLineNavigationFormatRegistry;
import slash.navigation.base.MultipleRoutesFormat;
import slash.navigation.base.NavigationFormat;
import slash.navigation.base.NavigationFormatParser;
import slash.navigation.base.NavigationFormatRegistry;
import slash.navigation.base.ParserResult;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import static slash.navigation.base.NavigationFormatParser.getNumberOfFilesToWriteFor;
import static slash.navigation.base.SponsorGatedFormats.isSponsorGated;

/**
 * {@code convert --to <format> [--from <format>[,<format>...]] <source file> <target file>}: the
 * non-interactive conversion used by the online converter on the server.
 * <p>
 * Unlike the legacy {@code <source> <target format> <target>} mode it
 * <ul>
 * <li>restricts reading to the formats named with {@code --from} (no probing of all 100+ readers,
 * some of which start external programs), and
 * <li>writes exactly the given target path and never splits into several files,
 * <li>refuses the sponsor-gated write formats, which cannot ask for a donation here,
 * <li>reports through exit codes and one line of JSON on {@code out} instead of {@code System.exit}
 * in the middle of the work.
 * </ul>
 * Formats are named by the simple class name, e.g. {@code Gpx11Format}.
 *
 * @author Christian Pesch
 */

public class ConvertCommand {
    private static final Logger log = Logger.getLogger(ConvertCommand.class.getName());

    public static final int OK = 0;
    public static final int USAGE = 5;
    public static final int SOURCE_MISSING = 10;
    public static final int TARGET_EXISTS = 11;
    public static final int UNKNOWN_TARGET_FORMAT = 15;
    public static final int UNKNOWN_SOURCE_FORMAT = 16;
    public static final int TARGET_FORMAT_NOT_ALLOWED = 17;
    public static final int SOURCE_NOT_READABLE = 20;
    public static final int NOTHING_TO_CONVERT = 21;
    public static final int NEEDS_MULTIPLE_FILES = 26;
    public static final int WRITE_FAILED = 25;

    static final String USAGE_TEXT = "Usage: java -jar RouteConverterCmdLine.jar convert --to <format> " +
            "[--from <format>[,<format>...]] <source file> <target file>";

    private final NavigationFormatRegistry registry = new CmdLineNavigationFormatRegistry();

    private NavigationFormat<BaseRoute<?, ?>> find(List<NavigationFormat<BaseRoute<?, ?>>> formats, String name) {
        for (NavigationFormat<BaseRoute<?, ?>> format : formats)
            if (name.equals(format.getClass().getSimpleName()))
                return format;
        return null;
    }

    public int run(String[] args, PrintStream out) {
        String to = null, from = null;
        List<String> files = new ArrayList<>();
        for (int i = 1; i < args.length; i++) {
            switch (args[i]) {
                case "--to" -> to = i + 1 < args.length ? args[++i] : null;
                case "--from" -> from = i + 1 < args.length ? args[++i] : null;
                default -> files.add(args[i]);
            }
        }
        if (to == null || files.size() != 2 || (from == null && List.of(args).contains("--from"))) {
            log.severe(USAGE_TEXT);
            return USAGE;
        }

        File source = new File(files.get(0)).getAbsoluteFile();
        File target = new File(files.get(1)).getAbsoluteFile();
        if (!source.isFile()) {
            log.severe("Source does not exist; stopping.");
            return SOURCE_MISSING;
        }
        if (target.exists()) {
            log.severe("Target already exists; stopping.");
            return TARGET_EXISTS;
        }

        NavigationFormat<BaseRoute<?, ?>> targetFormat = find(registry.getWriteFormats(), to);
        if (targetFormat == null) {
            log.severe("Target format '" + to + "' does not exist or cannot be written; stopping.");
            return UNKNOWN_TARGET_FORMAT;
        }
        if (isSponsorGated(targetFormat)) {
            log.severe("Target format '" + to + "' is a sponsor feature and not available here; stopping.");
            return TARGET_FORMAT_NOT_ALLOWED;
        }

        List<NavigationFormat<BaseRoute<?, ?>>> sourceFormats = new ArrayList<>();
        if (from != null) {
            for (String name : from.split(",")) {
                NavigationFormat<BaseRoute<?, ?>> format = find(registry.getReadFormats(), name.trim());
                if (format == null) {
                    log.severe("Source format '" + name + "' does not exist or cannot be read; stopping.");
                    return UNKNOWN_SOURCE_FORMAT;
                }
                sourceFormats.add(format);
            }
        }

        NavigationFormatParser parser = new NavigationFormatParser(registry);
        ParserResult result;
        try {
            result = from != null ? parser.read(source, sourceFormats) : parser.read(source);
        } catch (IOException | RuntimeException e) {
            log.severe("Cannot read source: " + e);
            return SOURCE_NOT_READABLE;
        }
        if (result == null || !result.isSuccessful()) {
            log.severe("Cannot read source; stopping.");
            return SOURCE_NOT_READABLE;
        }

        List<BaseRoute<?, ?>> routes = result.getAllRoutes();
        int positions = 0;
        for (BaseRoute<?, ?> route : routes)
            positions += route.getPositionCount();
        if (positions == 0) {
            log.severe("Source contains no positions; stopping.");
            return NOTHING_TO_CONVERT;
        }

        int written = 1;
        try {
            if (targetFormat.isSupportsMultipleRoutes()) {
                new NavigationFormatParser(registry).write(routes, (MultipleRoutesFormat<?>) targetFormat, target);
                written = routes.size();
            } else {
                BaseRoute<?, ?> route = result.getTheRoute();
                if (getNumberOfFilesToWriteFor(route, targetFormat, false) != 1) {
                    log.severe("Target format cannot hold that many positions in one file; stopping.");
                    return NEEDS_MULTIPLE_FILES;
                }
                new NavigationFormatParser(registry).write(route, targetFormat, false, false, null, target);
            }
        } catch (IOException | RuntimeException e) {
            log.severe("Cannot write target: " + e);
            try {
                java.nio.file.Files.deleteIfExists(target.toPath());
            } catch (IOException ignored) {
                // best effort: the caller removes its work directory anyway
            }
            return WRITE_FAILED;
        }

        // routesWritten < routesRead: the target format holds only one route/track, the others were left out
        out.println("{\"routesRead\":" + routes.size() + ",\"routesWritten\":" + written + ",\"positions\":" + positions + "}");
        return OK;
    }
}
