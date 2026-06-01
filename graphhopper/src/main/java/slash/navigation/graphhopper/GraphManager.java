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
package slash.navigation.graphhopper;

import slash.navigation.datasources.DataSource;
import slash.navigation.datasources.File;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import java.util.stream.Stream;

import static java.lang.String.format;
import static java.lang.System.currentTimeMillis;
import static java.nio.file.Files.isRegularFile;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static slash.common.io.Directories.ensureDirectory;
import static slash.common.io.Directories.getApplicationDirectory;
import static slash.common.io.Files.*;
import static slash.common.io.Transfer.isEmpty;
import static slash.navigation.graphhopper.PbfUtil.DOT_PBF;
import static slash.navigation.graphhopper.PbfUtil.PROPERTIES;

/**
 * Collects GraphHopper graphs, as directories and PBFs locally and as ZIPs and PBFs remotely.
 *
 * @author Christian Pesch
 */

public class GraphManager {
    private static final Logger log = Logger.getLogger(GraphManager.class.getName());
    private static final Preferences preferences = Preferences.userNodeForPackage(GraphManager.class);
    private static final String DIRECTORY_PREFERENCE = "directory";

    private final List<GraphDescriptor> localGraphDescriptors = new ArrayList<>();
    private final List<GraphDescriptor> remoteGraphDescriptors = new ArrayList<>();

    public GraphManager(DataSource kurviger, DataSource mapsforge, DataSource graphHopper) throws IOException {
        scanLocalGraphs(kurviger, mapsforge, graphHopper);
        scanRemoteGraphs(asList(kurviger, mapsforge, graphHopper));
    }

    List<GraphDescriptor> getLocalGraphDescriptors() {
        return new ArrayList<>(localGraphDescriptors);
    }

    List<GraphDescriptor> getRemoteGraphDescriptors() {
        return new ArrayList<>(remoteGraphDescriptors);
    }

    public String getPath() {
        return preferences.get(DIRECTORY_PREFERENCE, "");
    }

    public void setPath(String path) {
        preferences.put(DIRECTORY_PREFERENCE, path);
    }

    java.io.File getDirectory(DataSource dataSource) {
        String path = getPath();
        if (isEmpty(path) || !new java.io.File(path).exists())
            path = getApplicationDirectory(dataSource.getDirectory()).getAbsolutePath();
        return ensureDirectory(path);
    }

    List<java.io.File> collectPbfFiles(DataSource dataSource) {
        return collectFiles(getDirectory(dataSource), DOT_PBF);
    }

    List<java.io.File> collectGraphDirectories(DataSource dataSource) throws IOException {
        try (Stream<Path> files = java.nio.file.Files.walk(Paths.get(getDirectory(dataSource).getPath()))) {
            return files
                    .filter(f -> isRegularFile(f) && f.getFileName().startsWith(PROPERTIES))
                    .map(f -> f.getParent().toFile())
                    .collect(toList());
        }
    }

    private void scanLocalGraphs(DataSource kurviger, DataSource mapsforge, DataSource graphHopper) throws IOException {
        long start = currentTimeMillis();

        List<java.io.File> files = emptyList();
        if(graphHopper != null) {
            files = collectPbfFiles(graphHopper);
            for (java.io.File file : files) {
                checkFile(file);
                localGraphDescriptors.add(new GraphDescriptor(GraphType.PBF, file, null));
            }
        }

        List<java.io.File> directories = new ArrayList<>();
        if(kurviger != null)
            directories.addAll(collectGraphDirectories(kurviger));
        if(mapsforge != null)
            directories.addAll(collectGraphDirectories(mapsforge));
        for (java.io.File directory : directories) {
            checkDirectory(directory);
            localGraphDescriptors.add(new GraphDescriptor(GraphType.Directory, directory, null));
        }

        /*  Sort
            1. Directories before PBFs
            2. PBFs with graph directories first
            3. then file name by length and alphabet
         */
        localGraphDescriptors.sort((g1, g2) -> {
              int order = Integer.compare(g2.getGraphType().order, g1.getGraphType().order);
              if(order != 0)
                  return order;

              if(g1.hasGraphDirectory() && !g2.hasGraphDirectory())
                  return -1;
              if(!g1.hasGraphDirectory() && g2.hasGraphDirectory())
                  return 1;

              return compare(g1.getLocalFile(), g2.getLocalFile());
          });

        long end = currentTimeMillis();
        log.info(format("Collected %d local graph files %s in %d milliseconds",
                files.size(), asDialogString(files, false), (end - start)));
    }

    private void scanRemoteGraphs(List<DataSource> dataSources) {
        for (DataSource dataSource : dataSources.stream().filter(Objects::nonNull).toList()) {
            for (File file : dataSource.getFiles()) {
                if (getExtension(file.getUri()).equals(DOT_PBF))
                    remoteGraphDescriptors.add(new GraphDescriptor(GraphType.PBF, null, file));
                else
                    remoteGraphDescriptors.add(new GraphDescriptor(GraphType.ZIP, null, file));
            }
        }
        remoteGraphDescriptors.sort(new GraphDescriptorComparator());

        log.fine(format("Collected %d remote graphs", remoteGraphDescriptors.size()));
    }

    enum GraphType {
        ZIP(1), PBF(2), Directory(-1);

        public final int order;

        GraphType(int order) {
            this.order = order;
        }
    }

    static class GraphDescriptorComparator implements Comparator<GraphDescriptor> {
        /*  Sort
            1. ZIP before PBFs
            2. bounding boxes contained by others before containing
            3. when bounding box missing: by uri alphabetically
        */
        public int compare(GraphDescriptor g1, GraphDescriptor g2) {
            if (g1 == g2 || g1.equals(g2))
                return 0;
            return internalCompare(g1, g2);
        }

        private int internalCompare(GraphDescriptor g1, GraphDescriptor g2) {
            int order = Integer.compare(g1.getGraphType().order, g2.getGraphType().order);
            if(order != 0)
                return order;

            if (g1.getBoundingBox() == null && g2.getBoundingBox() != null)
                return 1;
            if (g1.getBoundingBox() != null && g2.getBoundingBox() == null)
                return -1;
            if (g1.getBoundingBox() == null && g2.getBoundingBox() == null)
                return compareByIdentifier(g1, g2);

            boolean g1ContainsG2 = g1.getBoundingBox().contains(g2.getBoundingBox());
            boolean g2ContainsG1 = g2.getBoundingBox().contains(g1.getBoundingBox());
            if (g1ContainsG2 != g2ContainsG1)
                return g1ContainsG2 ? 1 : -1;

            int squareSize = Double.compare(g1.getBoundingBox().getSquareSize(), g2.getBoundingBox().getSquareSize());
            if(squareSize != 0)
                return squareSize;

            return compareByIdentifier(g1, g2);
        }

        private int compareByIdentifier(GraphDescriptor g1, GraphDescriptor g2) {
            String identifier1 = getIdentifier(g1);
            String identifier2 = getIdentifier(g2);
            int result = identifier1.compareToIgnoreCase(identifier2);
            return result != 0 ? result : identifier1.compareTo(identifier2);
        }

        private String getIdentifier(GraphDescriptor graphDescriptor) {
            if (graphDescriptor.getRemoteFile() != null)
                return graphDescriptor.getRemoteFile().getUri();
            if (graphDescriptor.getLocalFile() != null)
                return graphDescriptor.getLocalFile().getAbsolutePath();
            return "";
        }
    }
}
