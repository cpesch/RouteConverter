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

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Logger;

import static java.lang.Math.abs;
import static java.lang.String.format;
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
        return new java.io.File(directory, file.getUri()).exists();
    }

    public Downloadable getDownloadableFor(BoundingBox routeBoundingBox) {
        File coveringFile = null;
        Double closestDistanceOfCenters = null;
        File centerFile = null;

        for (File file : dataSource.getFiles()) {
            BoundingBox fileBoundingBox = file.getBoundingBox();
            if (fileBoundingBox == null) {
                log.warning(format("File %s doesn't have a bounding box. Ignoring it.", file));
                continue;
            }

            Double distance = calculateBearing(fileBoundingBox.getCenter().getLongitude(), fileBoundingBox.getCenter().getLatitude(),
                    routeBoundingBox.getCenter().getLongitude(), routeBoundingBox.getCenter().getLatitude()).getDistance();
            if (closestDistanceOfCenters == null ||
                    distance < closestDistanceOfCenters ||
                    abs(distance - closestDistanceOfCenters) < 5.0 && centerFile.getBoundingBox().contains(fileBoundingBox)) {
                centerFile = file;
                closestDistanceOfCenters = distance;
            }

            if (fileBoundingBox.contains(routeBoundingBox)) {
                if (coveringFile == null ||
                        !existsFile(coveringFile) && existsFile(file)) {
                    coveringFile = file;
                }
            }
        }

        log.info(format("File %s (exists %b) covers route, file %s (exists %b) has closest distance of centers", coveringFile, existsFile(coveringFile), centerFile, existsFile(centerFile)));
        // choose the closest distance of centers but prefer covering existing files if closest distance means download
        return existsFile(centerFile) || !existsFile(coveringFile) ? centerFile : coveringFile;
    }

    public Collection<Downloadable> getDownloadableFor(List<BoundingBox> boundingBoxes) {
        Collection<Downloadable> result = new HashSet<>();
        for (BoundingBox boundingBox : boundingBoxes)
            result.add(getDownloadableFor(boundingBox));
        return result;
    }
}
