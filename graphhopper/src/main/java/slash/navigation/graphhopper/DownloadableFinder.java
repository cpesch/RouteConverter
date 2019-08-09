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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Logger;

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
        return file != null && new java.io.File(directory, file.getUri()).exists();
    }

    public List<Downloadable> getDownloadablesFor(BoundingBox routeBoundingBox) {
        List<RoutingTileInfo> resultList = new ArrayList<RoutingTileInfo>();

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


            /*
            // search for file which center has closest distance to the center of the route
            Double distance = calculateBearing(fileBoundingBox.getCenter().getLongitude(), fileBoundingBox.getCenter().getLatitude(),
                    routeBoundingBox.getCenter().getLongitude(), routeBoundingBox.getCenter().getLatitude()).getDistance();
            if (closestDistanceOfCenters == null ||
                    distance < closestDistanceOfCenters ||
                    abs(distance - closestDistanceOfCenters) < 5.0 && centerFile.getBoundingBox().contains(fileBoundingBox)) {
                centerFile = file;
                closestDistanceOfCenters = distance;
                log.info(format("File %s has closest distance %f of centers", centerFile, closestDistanceOfCenters));
                log.info(format("Center File has bounding box: NW %s SE %s",fileBoundingBox.getNorthWest(),fileBoundingBox.getSouthEast())); // TODO remove later
            }

            // search for existing largest file that covers the route
            if (existsFile(file) && (coveringBoundingBox == null || fileBoundingBox.contains(coveringBoundingBox))) {
                coveringFile = file;
                coveringBoundingBox = fileBoundingBox;
                log.info(format("File %s covers route with larger bounding box %s", coveringFile, fileBoundingBox.contains(coveringBoundingBox)));
                log.info(format("File bounding box: NW %s SE %s",fileBoundingBox.getNorthWest(),fileBoundingBox.getSouthEast()));  // TODO remove later
            }
             */
            RoutingTileInfo fileInfo = new RoutingTileInfo();
            fileInfo.downloadable = file;
            fileInfo.centerDistance = distance;
            fileInfo.fileBoundingBox = fileBoundingBox;
            fileInfo.isExisting = existsFile(file);

            resultList.add(fileInfo);

        }

        // log.info(format("File %s (exists %b) covers route, file %s (exists %b) has closest distance %f of centers", coveringFile, existsFile(coveringFile), centerFile, existsFile(centerFile), closestDistanceOfCenters));
        // choose the closest distance of centers but prefer covering existing files if closest distance means download
        // return existsFile(centerFile) || !existsFile(coveringFile) ? centerFile : coveringFile;

        resultList.sort(null);

        List<Downloadable> retVal = new ArrayList<Downloadable>();
        for (RoutingTileInfo info : resultList) {
            retVal.add(info.downloadable);
        }
        // retVal = resultList.stream().map(i->i.downloadable).collect(Collectors.toList());
        log.info(format("Returning %d downloadables: %s",retVal.size(), retVal));
        return retVal;
    }


    private static class RoutingTileInfo implements Comparable<RoutingTileInfo> {

        public Downloadable downloadable;
        public Double centerDistance;
        public BoundingBox fileBoundingBox;
        public boolean isExisting;

        /*  Compare two Routing Tiles by:

            A < B if B.contains(A)
            B < A if A.contains(B)
            if (neither contain each other)
                A compares to B by Center-Distance to Route

         */
        public int compareTo(RoutingTileInfo o) {
            if (fileBoundingBox != null && o.fileBoundingBox != null) {
                if (fileBoundingBox.contains(o.fileBoundingBox)) return 1;
                else if (o.fileBoundingBox.contains(fileBoundingBox)) return -1;
            }
            return centerDistance.compareTo(o.centerDistance);
        }
    }

    /*
    public List<Downloadable> getDownloadablesFor(BoundingBox routeBoundingBox) {
        List<Downloadable> retVal = new ArrayList<Downloadable>();
        List<RoutingTileInfo> resultList = new ArrayList<RoutingTileInfo>();

        for (File file : dataSource.getFiles()) {
            BoundingBox fileBoundingBox = file.getBoundingBox();
            if (fileBoundingBox == null) {
                log.warning(format("File $s doesn't have a bounding box. Ignoring it.", file));
                continue;
            }
            if (!fileBoundingBox.contains(routeBoundingBox))
                continue;

            Double distance = calculateBearing(fileBoundingBox.getCenter().getLongitude(), fileBoundingBox.getCenter().getLatitude(),
                    routeBoundingBox.getCenter().getLongitude(), routeBoundingBox.getCenter().getLatitude()).getDistance();
            RoutingTileInfo fileInfo = new RoutingTileInfo();
            fileInfo.downloadable = file;
            fileInfo.centerDistance = distance;
            fileInfo.fileBoundingBox = fileBoundingBox;
            fileInfo.isExisting = existsFile(file);

            resultList.add(fileInfo);

        }

        resultList.sort(null);
        for (RoutingTileInfo info : resultList) {
            retVal.add(info.downloadable);
        }
        // retVal = resultList.stream().map(i->i.downloadable).collect(Collectors.toList());
        log.info(format("Returning %d elements as downloadables",retVal.size()));
        return retVal;
    }
     */

    public Collection<Downloadable> getDownloadablesFor(List<BoundingBox> boundingBoxes) {
        Collection<Downloadable> result = new HashSet<>();
        for (BoundingBox boundingBox : boundingBoxes)
            result.addAll(getDownloadablesFor(boundingBox));
        return result;
    }
}