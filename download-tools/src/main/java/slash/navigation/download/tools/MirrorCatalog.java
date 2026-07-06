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

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.help.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import slash.navigation.datasources.DataSource;
import slash.navigation.datasources.DataSourceManager;
import slash.navigation.datasources.Source;
import slash.navigation.datasources.helpers.DataSourceService;
import slash.navigation.download.tools.helpers.WgetCommandBuilder;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static java.lang.System.exit;
import static slash.navigation.datasources.DataSourceManager.loadAllDataSources;
import static slash.navigation.download.tools.helpers.GlobToRegex.convert;

/**
 * Mirrors every datasource that carries a {@code <source>} element by invoking wget
 * with arguments synthesized from the source url / level / include / exclude data.
 * Post-mirror cleanup deletes files in the mirror tree that match any
 * {@code <source><exclude>} glob.
 *
 * @author Christian Pesch
 */
public class MirrorCatalog {
    private static final Logger log = Logger.getLogger(MirrorCatalog.class.getName());

    private static final String SNAPSHOT_ARGUMENT = "snapshot";
    private static final String MIRROR_ARGUMENT = "mirror";
    private static final String ID_ARGUMENT = "id";
    private static final String DRY_RUN_ARGUMENT = "dry-run";

    public static void main(String[] args) throws Exception {
        System.setProperty(DataSourceManager.INCLUDE_SOURCE_PROPERTY, "true");

        CommandLine line = parseCommandLine(args);
        Path snapshotDir = Paths.get(line.getOptionValue(SNAPSHOT_ARGUMENT));
        Path mirrorRoot = Paths.get(line.getOptionValue(MIRROR_ARGUMENT));
        boolean dryRun = line.hasOption(DRY_RUN_ARGUMENT);
        Set<String> idFilter = optionValuesToSet(line.getOptionValues(ID_ARGUMENT));

        new MirrorCatalog().run(snapshotDir, mirrorRoot, idFilter, dryRun);
        exit(0);
    }

    public void run(Path snapshotDir, Path mirrorRoot, Set<String> idFilter, boolean dryRun) throws Exception {
        if (!Files.isDirectory(snapshotDir))
            throw new IllegalArgumentException("Snapshot directory does not exist: " + snapshotDir);

        Files.createDirectories(mirrorRoot);
        DataSourceService service = loadAllDataSources(snapshotDir.toFile());
        WgetCommandBuilder builder = new WgetCommandBuilder();

        int mirrored = 0, skipped = 0, failed = 0;
        for (DataSource dataSource : service.getDataSources()) {
            String id = dataSource.getId();
            if (!idFilter.isEmpty() && !idFilter.contains(id))
                continue;
            Source source = dataSource.getSource();
            if (source == null) {
                skipped++;
                log.fine("Skipping " + id + ": no <source>");
                continue;
            }
            List<String> command = builder.buildCommand(dataSource, source, mirrorRoot);
            log.info("Mirror " + id + ": " + builder.formatCommand(command));
            if (dryRun) {
                mirrored++;
                continue;
            }
            int exitCode = runWget(command);
            if (exitCode != 0) {
                failed++;
                log.severe("wget for " + id + " exited with " + exitCode);
                continue;
            }
            int removed = runCleanup(mirrorRoot, source.getExcludes());
            if (removed > 0)
                log.info("Cleanup " + id + ": removed " + removed + " file(s)");
            mirrored++;
        }
        log.info("Mirror done. mirrored=" + mirrored + " skipped=" + skipped + " failed=" + failed);
        if (failed > 0)
            exit(1);
    }

    private int runWget(List<String> command) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        Process process = pb.start();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null)
                System.out.println(line);
        }
        return process.waitFor();
    }

    int runCleanup(Path mirrorRoot, List<String> excludes) throws IOException {
        if (excludes == null || excludes.isEmpty())
            return 0;
        List<Pattern> patterns = new ArrayList<>();
        for (String exclude : excludes)
            patterns.add(Pattern.compile(convert(exclude)));

        int removed = 0;
        try (Stream<Path> paths = Files.walk(mirrorRoot, FileVisitOption.FOLLOW_LINKS)) {
            List<Path> candidates = paths.filter(Files::isRegularFile).toList();
            for (Path candidate : candidates) {
                String relative = mirrorRoot.relativize(candidate).toString();
                for (Pattern pattern : patterns) {
                    if (pattern.matcher(relative).matches()) {
                        Files.deleteIfExists(candidate);
                        log.info("Removed " + relative);
                        removed++;
                        break;
                    }
                }
            }
        }
        return removed;
    }

    private static Set<String> optionValuesToSet(String[] values) {
        return values != null ? new HashSet<>(java.util.Arrays.asList(values)) : Collections.emptySet();
    }

    private static CommandLine parseCommandLine(String[] args) throws ParseException {
        CommandLineParser parser = new DefaultParser();
        Options options = new Options();
        options.addOption(Option.builder().argName(SNAPSHOT_ARGUMENT).numberOfArgs(1).required().longOpt(SNAPSHOT_ARGUMENT)
                .desc("Path to snapshot datasources directory").get());
        options.addOption(Option.builder().argName(MIRROR_ARGUMENT).numberOfArgs(1).required().longOpt(MIRROR_ARGUMENT)
                .desc("Path to local mirror root").get());
        options.addOption(Option.builder().argName(ID_ARGUMENT).hasArgs().longOpt(ID_ARGUMENT)
                .desc("Restrict to a specific datasource id (repeatable)").get());
        options.addOption(Option.builder().longOpt(DRY_RUN_ARGUMENT)
                .desc("Print wget commands without running them").get());
        try {
            return parser.parse(options, args);
        } catch (ParseException e) {
            try {
                HelpFormatter.builder().get().printHelp(MirrorCatalog.class.getSimpleName(), null, options, null, false);
            } catch (IOException ignored) {
                // help output is best-effort
            }
            throw e;
        }
    }
}
