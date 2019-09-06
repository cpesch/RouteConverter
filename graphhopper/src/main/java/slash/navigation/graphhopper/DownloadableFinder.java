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
import slash.navigation.datasources.DataSource;
import slash.navigation.datasources.Downloadable;
import slash.navigation.datasources.File;

import java.util.*;
import java.util.logging.Logger;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static slash.navigation.common.Bearing.calculateBearing;

/**
 * Finds {@link Downloadable}s for {@link BoundingBox}es from a given {@link DataSource}
 * and prefers {@link Downloadable}s that are already present.
 *
 * @author Christian Pesch
 */

public class DownloadableFinder {
    private static final Logger log = Logger.getLogger(DownloadableFinder.class.getName());
    private DataSource dataSource;
    private java.io.File directory;

    public DownloadableFinder(DataSource dataSource, java.io.File directory) {
        this.dataSource = dataSource;
        this.directory = directory;
    }

    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    private boolean existsFile(File file) {
        return file != null && new java.io.File(directory, file.getUri()).exists();
    }

    public List<Downloadable> getDownloadablesFor(BoundingBox routeBoundingBox) {
        List<DownloadableDescriptor> descriptors = new ArrayList<>();
        for (File file : dataSource.getFiles()) {
            BoundingBox fileBoundingBox = file.getBoundingBox();
            if (fileBoundingBox == null) {
                log.warning(format("File %s doesn't have a bounding box. Ignoring it.", file));
                continue;
            }
            if (!fileBoundingBox.contains(routeBoundingBox))
                continue;

            Double distance = calculateBearing(fileBoundingBox.getCenter().getLongitude(), fileBoundingBox.getCenter().getLatitude(),
                    routeBoundingBox.getCenter().getLongitude(), routeBoundingBox.getCenter().getLatitude()).getDistance();
            boolean existsFile = existsFile(file);
            descriptors.add(new DownloadableDescriptor(file, distance, fileBoundingBox, existsFile));
        }

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

        log.info(format("Found %d downloadables for %s: %s", result.size(), routeBoundingBox, result));
        return result;
    }

    public Collection<Downloadable> getDownloadablesFor(Collection<BoundingBox> boundingBoxes) {
        Set<Downloadable> result = new HashSet<>();
        for (BoundingBox boundingBox : boundingBoxes)
            result.addAll(getDownloadablesFor(boundingBox));
        return result;
    }

    private static class DownloadableDescriptor implements Comparable<DownloadableDescriptor> {
        private final Downloadable downloadable;
        private final Double distanceToCenter;
        private final BoundingBox fileBoundingBox;
        private final boolean existsFile;

        private DownloadableDescriptor(Downloadable downloadable, Double distanceToCenter, BoundingBox fileBoundingBox, boolean existsFile) {
            this.downloadable = downloadable;
            this.distanceToCenter = distanceToCenter;
            this.fileBoundingBox = fileBoundingBox;
            this.existsFile = existsFile;
        }

        public Downloadable getDownloadable() {
            return downloadable;
        }

        public boolean hasValidBoundingBox() {
            /* basically it's a good idea to choose the Downloadable with the smallest enclosing bounding box
               unfortunately, files like https://download.geofabrik.de/north-america/us/alaska-latest.osm.pbf
               claim too large bounding boxes */
            return !(fileBoundingBox.getNorthEast().getLongitude() >= 179.9999 ||
                    fileBoundingBox.getSouthWest().getLongitude() <= -179.9999);
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
            if(existsFile && !other.existsFile)
                return -1;
            if(!existsFile && other.existsFile)
                return 1;

            if (fileBoundingBox != null && other.fileBoundingBox != null) {
                if (fileBoundingBox.contains(other.fileBoundingBox))
                    return 2;
                else if (other.fileBoundingBox.contains(fileBoundingBox))
                    return -2;
            }

            return distanceToCenter.compareTo(other.distanceToCenter);
        }

        public String toString() {
            return getClass().getSimpleName() + "[downloadable=" + downloadable +
                    ", distanceToCenter=" + distanceToCenter +
                    ", fileBoundingBox=" + fileBoundingBox +
                    ", existsFile=" + existsFile + "]";
        }
    }
}