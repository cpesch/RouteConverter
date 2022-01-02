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
package slash.navigation.converter.gui.helpers;

import slash.navigation.common.LongitudeAndLatitude;
import slash.navigation.common.MapDescriptor;
import slash.navigation.elevation.ElevationService;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;

import static java.util.Arrays.sort;

/**
 * An elevation service that tries to find the best available elevation service.
 *
 * @author Christian Pesch
 */

public class AutomaticElevationService implements ElevationService {
    private static final Logger log = Logger.getLogger(AutomaticElevationService.class.getName());
    private static final String AUTOMATIC_ELEVATION_SERVICE_NAME = "Automatic";
    private static final String JONATHAN_DE_FERRANTI_DEM_3 = "Jonathan de Ferranti DEM 3";

    private final ElevationServiceFacade elevationServiceFacade;

    public AutomaticElevationService(ElevationServiceFacade elevationServiceFacade) {
        this.elevationServiceFacade = elevationServiceFacade;
    }

    public String getName() {
        return AUTOMATIC_ELEVATION_SERVICE_NAME;
    }

    public boolean isDownload() {
        return true;
    }

    public boolean isOverQueryLimit() {
        return false;
    }

    public String getPath() {
        // do not throw UnsupportedOperationException since #isDownload is true to omit (online) suffix in rendering
        return "";
    }

    public void setPath(String path) {
        // do not throw UnsupportedOperationException since #isDownload is true to omit (online) suffix in rendering
    }

    public File getDirectory() {
        for (ElevationService service : sortByBestEffort(elevationServiceFacade.getElevationServices())) {
            if (!service.isDownload())
                continue;

            File directory = service.getDirectory();
            File[] files = directory.listFiles();
            // return first directory with files to help hills shading to show best possible coverage
            if (files != null && files.length > 0)
                return directory;
        }
        return null;
    }

    public Double getElevationFor(double longitude, double latitude) throws IOException {
        IOException lastException = null;

        for (ElevationService service : sortByBestEffort(elevationServiceFacade.getElevationServices())) {
            try {
                if(service.isOverQueryLimit())
                    continue;

                Double elevation = service.getElevationFor(longitude, latitude);
                if (elevation != null) {
                    log.fine("Used " + service.getName() + " to retrieve elevation " + elevation + " for " + longitude + "/" + latitude);
                    return elevation;
                }

            } catch (IOException e) {
                lastException = e;
            }
        }

        if(lastException != null)
            throw lastException;
        else
            return null;
    }

    private ElevationService[] sortByBestEffort(List<ElevationService> elevationServices) {
        List<ElevationService> toSort = new ArrayList<>(elevationServices);
        toSort.remove(this);

        ElevationService[] result = toSort.toArray(new ElevationService[0]);
        sort(result, new ElevationServicePriorityComparator());
        return result;
    }

    public String getPreferredDownloadName() {
        return JONATHAN_DE_FERRANTI_DEM_3;
    }

    public void downloadElevationDataFor(List<LongitudeAndLatitude> longitudeAndLatitudes, boolean waitForDownload) {
        ElevationService service = elevationServiceFacade.findElevationService(getPreferredDownloadName());
        if (service != null)
            service.downloadElevationDataFor(longitudeAndLatitudes, waitForDownload);
    }

    public long calculateRemainingDownloadSize(List<MapDescriptor> mapDescriptors) {
        ElevationService service = elevationServiceFacade.findElevationService(getPreferredDownloadName());
        return service != null ? service.calculateRemainingDownloadSize(mapDescriptors) : 0L;
    }

    public void downloadElevationData(List<MapDescriptor> mapDescriptors) {
        ElevationService service = elevationServiceFacade.findElevationService(getPreferredDownloadName());
        if (service != null)
            service.downloadElevationData(mapDescriptors);
    }

    private static class ElevationServicePriorityComparator implements Comparator<ElevationService> {
        private static final Map<String, Integer> PRIORITY = new HashMap<>();
        static {
            PRIORITY.put("Jonathan de Ferranti DEM 1", 1);
            PRIORITY.put("Sonny LiDAR DGM 1", 2);
            PRIORITY.put("NASA SRTM 1", 3);
            PRIORITY.put(JONATHAN_DE_FERRANTI_DEM_3, 4);
            PRIORITY.put("Sonny LiDAR DGM 3", 5);
            PRIORITY.put("NASA SRTM 3", 6);
            PRIORITY.put("GeoNames", 7);
            PRIORITY.put("Google Maps", 8);
        }

        private int getPriority(ElevationService elevationService) {
            Integer priority = PRIORITY.get(elevationService.getName());
            return priority == null ? 10 : priority;
        }

        public int compare(ElevationService e1, ElevationService e2) {
            return getPriority(e1) - getPriority(e2);
        }
    }
}
