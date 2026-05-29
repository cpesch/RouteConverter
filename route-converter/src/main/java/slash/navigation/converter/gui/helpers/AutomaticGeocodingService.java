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

import slash.navigation.common.NavigationPosition;
import slash.navigation.geocoding.BaseGeocodingService;
import slash.navigation.geocoding.GeocodingResult;
import slash.navigation.geocoding.GeocodingService;

import javax.naming.ServiceUnavailableException;
import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;

import static java.util.Arrays.sort;

/**
 * A geocoding service that tries to find the best available geocoding service.
 *
 * @author Christian Pesch
 */

public class AutomaticGeocodingService extends BaseGeocodingService {
    private static final Logger log = Logger.getLogger(AutomaticGeocodingService.class.getName());
    private static final String AUTOMATIC_GEOCODING_SERVICE_NAME = "Automatic";

    private final GeocodingServiceFacade geocodingServiceFacade;

    public AutomaticGeocodingService(GeocodingServiceFacade geocodingServiceFacade) {
        this.geocodingServiceFacade = geocodingServiceFacade;
    }

    public String getName() {
        return AUTOMATIC_GEOCODING_SERVICE_NAME;
    }

    public boolean isDownload() {
        return true;
    }

    public boolean isOverQueryLimit() {
        return false;
    }

    public List<GeocodingResult> getPositionsFor(String address) throws IOException, ServiceUnavailableException {
        IOException lastException = null;
        List<GeocodingResult> results = new ArrayList<>();

        for (GeocodingService service : sortByBestEffort(geocodingServiceFacade.getGeocodingServices())) {
            try {
                if(service.isOverQueryLimit())
                    continue;

                List<GeocodingResult> geocodingResults = service.getPositionsFor(address);
                if (geocodingResults != null && !geocodingResults.isEmpty()) {
                    results.addAll(geocodingResults);
                    log.fine("Used " + service.getName() + " to retrieve positions " + geocodingResults + " for " + address);
                }

            } catch (IOException e) {
                lastException = e;
            }
        }

        if(!results.isEmpty())
            return results;

        if(lastException != null)
            throw lastException;
        else
            return null;
    }

    public String getAddressFor(NavigationPosition position) throws IOException, ServiceUnavailableException {
        IOException lastException = null;

        for (GeocodingService service : sortByBestEffort(geocodingServiceFacade.getGeocodingServices())) {
            try {

                String address = service.getAddressFor(position);
                if (address != null) {
                    log.info("Used " + service.getName() + " to retrieve address for " + address);
                    return address;
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

    private GeocodingService[] sortByBestEffort(List<GeocodingService> geocodingServices) {
        List<GeocodingService> toSort = new ArrayList<>(geocodingServices);
        toSort.remove(this);

        GeocodingService[] result = toSort.toArray(new GeocodingService[0]);
        sort(result, new GeocodingServicePriorityComparator());
        return result;
    }

    private static class GeocodingServicePriorityComparator implements Comparator<GeocodingService> {
        private static final Map<String, Integer> PRIORITY = new HashMap<>();
        static {
            PRIORITY.put("Mapsforge POI", 1);
            PRIORITY.put("Mapsforge Map", 2);
            PRIORITY.put("Mapsforge", 2);
            PRIORITY.put("Google Maps", 3);
            PRIORITY.put("Nominatim", 4);
            PRIORITY.put("Photon", 5);
            PRIORITY.put("GeoNames", 6);
        }

        private int getPriority(GeocodingService geocodingService) {
            Integer priority = PRIORITY.get(geocodingService.getName());
            return priority == null ? 10 : priority;
        }

        public int compare(GeocodingService g1, GeocodingService g2) {
            return getPriority(g1) - getPriority(g2);
        }
    }
}
