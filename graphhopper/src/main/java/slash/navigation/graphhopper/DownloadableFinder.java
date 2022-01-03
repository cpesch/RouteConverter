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

import slash.navigation.common.BoundingBox;
import slash.navigation.common.MapDescriptor;
import slash.navigation.datasources.DataSource;
import slash.navigation.datasources.Downloadable;
import slash.navigation.datasources.File;

import java.util.*;
import java.util.logging.Logger;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static slash.common.io.Files.removeExtension;
import static slash.navigation.common.Bearing.calculateBearing;
import static slash.navigation.graphhopper.PbfUtil.existsGraphDirectory;

/**
 * Finds {@link Downloadable}s for {@link BoundingBox}es from a given {@link DataSource}
 * and prefers {@link Downloadable}s that are already present.
 *
 * @author Christian Pesch
 */

public class DownloadableFinder {
    private static final Logger log = Logger.getLogger(DownloadableFinder.class.getName());
    private final List<DataSource> dataSources;
    private boolean wroteMessageAboutMissingBoundingBox = false;

    public DownloadableFinder(List<DataSource> dataSources) {
        this.dataSources = dataSources;
    }

    private java.io.File toFile(String directory, File file) {
        return new java.io.File(directory, file.getUri());
    }

    private boolean existsFile(String directory, File file) {
        return file != null && toFile(directory, file).exists();
    }

    private boolean existsGraph(String directory, File file) {
        return file != null && existsGraphDirectory(toFile(directory, file));
    }

    private String removeMapDirectoryPrefix(String identifier) {
        // mapIdentifier from MapsforgeMapView have maps subdirectory as a prefix
        if (identifier.startsWith("kurviger/"))
            identifier = identifier.substring("kurviger/".length());
        if (identifier.startsWith("mapsforge/"))
            identifier = identifier.substring("mapsforge/".length());
        return identifier;
    }

    private boolean matchesIdentifier(File file, MapDescriptor mapDescriptor) {
        // try to find europe/germany from europe/germany.map and europe/germany.zip
        return mapDescriptor.getIdentifier() != null &&
                removeExtension(file.getUri()).equals(removeExtension(removeMapDirectoryPrefix(mapDescriptor.getIdentifier())));
    }

    private boolean matchesBoundingBox(File file, MapDescriptor mapDescriptor) {
        BoundingBox fileBoundingBox = file.getBoundingBox();
        if (fileBoundingBox == null) {
            if (!wroteMessageAboutMissingBoundingBox) {
                log.fine(format("File %s doesn't have a bounding box. Ignoring it.", file));
                wroteMessageAboutMissingBoundingBox = true;
            }
            return false;
        }
        return fileBoundingBox.contains(mapDescriptor.getBoundingBox());
    }

    private List<DownloadableDescriptor> getDownloadDescriptorsFor(MapDescriptor mapDescriptor) {
        List<DownloadableDescriptor> descriptors = new ArrayList<>();
        for(DataSource dataSource : dataSources.stream().filter(Objects::nonNull).collect(toList())) {
            for (File file : dataSource.getFiles()) {
                boolean matchesIdentifier = matchesIdentifier(file, mapDescriptor);
                boolean matchesBoundingBox = matchesBoundingBox(file, mapDescriptor);
                if(!matchesBoundingBox && !matchesIdentifier)
                    continue;

                Double distance = file.getBoundingBox() != null && mapDescriptor.getBoundingBox() != null ?
                    calculateBearing(file.getBoundingBox().getCenter().getLongitude(), file.getBoundingBox().getCenter().getLatitude(),
                        mapDescriptor.getBoundingBox().getCenter().getLongitude(), mapDescriptor.getBoundingBox().getCenter().getLatitude()).getDistance()
                        : 0.0;
                boolean existsFile = existsFile(dataSource.getDirectory(), file);
                boolean existsGraphDirectory = existsGraph(dataSource.getDirectory(), file);
                descriptors.add(new DownloadableDescriptor(file, distance, file.getBoundingBox(), existsFile, existsGraphDirectory));
            }
        }
        return descriptors.stream()
                .filter(DownloadableDescriptor::hasValidBoundingBox)
                .sorted()
                .collect(toList());
    }

    private List<Downloadable> asDownloadables(Collection<DownloadableDescriptor> descriptors) {
        List<Downloadable> result = descriptors.stream()
                .filter(DownloadableDescriptor::hasValidBoundingBox)
                .sorted()
                .map(DownloadableDescriptor::getDownloadable)
                .collect(toList());
        // if there is no other choice use the Downloadables with the invalid bounding boxes
        if(result.size() == 0)
            result = descriptors.stream()
                    .sorted()
                    .map(DownloadableDescriptor::getDownloadable)
                    .collect(toList());
        return result;
    }

    List<Downloadable> getDownloadablesFor(MapDescriptor mapDescriptor) {
        List<DownloadableDescriptor> descriptors = getDownloadDescriptorsFor(mapDescriptor);
        List<Downloadable> result = asDownloadables(descriptors);
        log.info(format("Found %d downloadables for map descriptor %s: %s", result.size(), mapDescriptor, result));
        return result;
    }

    public List<Downloadable> getDownloadablesFor(Collection<MapDescriptor> mapDescriptors) {
        Set<DownloadableDescriptor> descriptors = mapDescriptors.stream()
                .flatMap(mapDescriptor -> getDownloadDescriptorsFor(mapDescriptor).stream())
                .collect(toSet());
        List<Downloadable> result = asDownloadables(descriptors);
        log.info(format("Found %d downloadables: %s for %d map descriptors: %s", result.size(), result, mapDescriptors.size(), mapDescriptors));
        return result;
    }

    private static class DownloadableDescriptor implements Comparable<DownloadableDescriptor> {
        private final Downloadable downloadable;
        private final Double distanceToCenter;
        private final BoundingBox fileBoundingBox;
        private final boolean existsFile;
        private final boolean existsGraphDirectory;

        private DownloadableDescriptor(Downloadable downloadable, Double distanceToCenter, BoundingBox fileBoundingBox,
                                       boolean existsFile, boolean existsGraphDirectory) {
            this.downloadable = downloadable;
            this.distanceToCenter = distanceToCenter;
            this.fileBoundingBox = fileBoundingBox;
            this.existsFile = existsFile;
            this.existsGraphDirectory = existsGraphDirectory;
        }

        public Downloadable getDownloadable() {
            return downloadable;
        }

        public boolean hasValidBoundingBox() {
            /* basically it's a good idea to choose the Downloadable with the smallest enclosing bounding box
               unfortunately, files like https://download.geofabrik.de/north-america/us/alaska-latest.osm.pbf
               claim too large bounding boxes */
            return fileBoundingBox == null ||
                    !(fileBoundingBox.getNorthEast().getLongitude() >= 179.9999 ||
                            fileBoundingBox.getSouthWest().getLongitude() <= -179.9999);
        }

        private boolean existsFileOrGraphDirectory() {
            return existsFile || existsGraphDirectory;
        }

        /*  Compare two Downloadables by:
            A < B if A exists and B not
            B < A if B exists and A not
            if (both exist or don't exist)
                A < B if B.contains(A)
                B < A if A.contains(B)
                if (neither contain each other)
                    A compares to B by DistanceToCenter of Route
        */
        public int compareTo(DownloadableDescriptor other) {
            if(existsFileOrGraphDirectory() && !other.existsFileOrGraphDirectory())
                return -1;
            if(!existsFileOrGraphDirectory() && other.existsFileOrGraphDirectory())
                return 1;

            if (fileBoundingBox != null && other.fileBoundingBox != null) {
                if (fileBoundingBox.contains(other.fileBoundingBox))
                    return 2;
                else if (other.fileBoundingBox.contains(fileBoundingBox))
                    return -2;
            }

            return distanceToCenter.compareTo(other.distanceToCenter);
        }

        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            DownloadableDescriptor that = (DownloadableDescriptor) o;

            return getDownloadable().equals(that.getDownloadable());
        }

        public int hashCode() {
            return getDownloadable().hashCode();
        }

        public String toString() {
            return getClass().getSimpleName() + "[downloadable=" + downloadable +
                    ", distanceToCenter=" + distanceToCenter +
                    ", fileBoundingBox=" + fileBoundingBox +
                    ", existsFile=" + existsFile +
                    ", existsGraphDirectory=" + existsGraphDirectory + "]";
        }
    }
}
