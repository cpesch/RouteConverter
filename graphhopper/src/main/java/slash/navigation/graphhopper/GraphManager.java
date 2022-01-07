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

import slash.common.io.Files;
import slash.navigation.datasources.DataSource;
import slash.navigation.datasources.File;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

import static java.io.File.separator;
import static java.lang.String.format;
import static java.lang.System.currentTimeMillis;
import static java.nio.file.Files.isRegularFile;
import static java.util.stream.Collectors.toList;
import static slash.common.io.Directories.ensureDirectory;
import static slash.common.io.Directories.getApplicationDirectory;
import static slash.common.io.Files.*;
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

    public GraphManager(List<DataSource> dataSources) throws IOException {
        scanLocalGraphs();
        scanRemoteGraphs(dataSources);
    }

    public List<GraphDescriptor> getLocalGraphDescriptors() {
        return new ArrayList<>(localGraphDescriptors);
    }

    public List<GraphDescriptor> getRemoteGraphDescriptors() {
        return new ArrayList<>(remoteGraphDescriptors);
    }

    public String getPath() {
        return preferences.get(DIRECTORY_PREFERENCE, "");
    }

    public void setPath(String path) {
        preferences.put(DIRECTORY_PREFERENCE, path);
    }

    private java.io.File getDirectory() {
        String path = getPath() + separator + "graphhopper";
        return ensureDirectory(getApplicationDirectory(path).getAbsolutePath());
    }

    List<java.io.File> collectPbfFiles() {
        return collectFiles(getDirectory(), DOT_PBF);
    }

    List<java.io.File> collectGraphDirectories() throws IOException {
        return java.nio.file.Files.walk(Paths.get(getDirectory().getPath()))
                .filter(f -> isRegularFile(f) && f.getFileName().startsWith(PROPERTIES))
                .map(f -> f.getParent().toFile())
                .collect(toList());
    }

    private void scanLocalGraphs() throws IOException {
        long start = currentTimeMillis();

        List<java.io.File> files = collectPbfFiles();
        for (java.io.File file : files) {
            checkFile(file);
            localGraphDescriptors.add(new GraphDescriptor(GraphType.PBF, file, null));
        }

        List<java.io.File> directories = collectGraphDirectories();
        for (java.io.File directory : directories) {
            checkDirectory(directory);
            localGraphDescriptors.add(new GraphDescriptor(GraphType.Directory, directory, null));
        }

        /*  Sort
            1. Directories before PBFs
            2. PBFs with graph directories first
            3. then file name by length and alphabet
         */
        localGraphDescriptors.sort(new Comparator<GraphDescriptor>() {
          public int compare(GraphDescriptor g1, GraphDescriptor g2) {
                int order = Integer.compare(g2.getGraphType().order, g1.getGraphType().order);
                if(order != 0)
                    return order;

                if(g1.hasGraphDirectory() && !g2.hasGraphDirectory())
                    return -1;
                if(!g1.hasGraphDirectory() && g2.hasGraphDirectory())
                    return 1;

                return Files.compare(g1.getLocalFile(), g2.getLocalFile());
            }
        });

        long end = currentTimeMillis();
        log.info(format("Collected %d local graph files %s from %s in %d milliseconds",
                files.size(), asDialogString(files, false), getDirectory(), (end - start)));
    }

    private void scanRemoteGraphs(List<DataSource> dataSources) {
        for (DataSource dataSource : dataSources.stream().filter(Objects::nonNull).collect(toList())) {
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
            int order = Integer.compare(g1.getGraphType().order, g2.getGraphType().order);
            if(order != 0)
                return order;

            if (g1.getBoundingBox() == null && g2.getBoundingBox() != null)
                return 1;
            if (g1.getBoundingBox() != null && g2.getBoundingBox() == null)
                return -1;
            if (g1.getBoundingBox() == null && g2.getBoundingBox() == null)
                return g1.getRemoteFile().getUri().compareTo(g2.getRemoteFile().getUri());

            if (g1.getBoundingBox().contains(g2.getBoundingBox()))
                return 1;
            else if (g2.getBoundingBox().contains(g1.getBoundingBox()))
                return -1;
            else
                return (int)(g2.getBoundingBox().getSquareSize() - g1.getBoundingBox().getSquareSize());
        }
    }
}
