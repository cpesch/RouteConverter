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

package slash.navigation.googlemaps;

import jakarta.xml.bind.JAXBException;
import slash.navigation.common.BoundingBox;
import slash.navigation.common.LongitudeAndLatitude;
import slash.navigation.common.MapDescriptor;
import slash.navigation.elevation.ElevationService;
import slash.navigation.googlemaps.elevation.ElevationResponse;
import slash.navigation.rest.Get;
import slash.navigation.rest.exception.ServiceUnavailableException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import static slash.navigation.googlemaps.GoogleUtil.unmarshalElevation;

/**
 * Encapsulates REST access to the Google Elevation API Service.
 *
 * @author Christian Pesch
 */

public class GoogleElevationService implements ElevationService {
    private static final Logger log = Logger.getLogger(GoogleElevationService.class.getName());
    private final GoogleApiClient apiClient = new GoogleApiClient();

    public String getName() {
        return "Google";
    }

    public boolean isOverQueryLimit() {
        return apiClient.isOverQueryLimit();
    }

    private String getElevationUrl(String payload) {
        return apiClient.getGoogleApiUrl("elevation", payload);
    }

    private void checkForError(String url, String status) throws ServiceUnavailableException {
        apiClient.checkForError(getClass().getSimpleName(), url, status);
    }

    public Double getElevationFor(double longitude, double latitude) throws IOException {
        String url = getElevationUrl("locations=" + latitude + "," + longitude); // could be up to 512 locations
        Get get = apiClient.get(url);
        log.info("Getting elevation for " + longitude + "," + latitude);
        String result = get.executeAsString();
        if (get.isSuccessful())
            try {
                ElevationResponse elevationResponse = unmarshalElevation(result);
                if (elevationResponse != null) {
                    String status = elevationResponse.getStatus();
                    checkForError(url, status);
                    List<Double> elevations = extractElevations(elevationResponse.getResult());
                    return !elevations.isEmpty() ? elevations.getFirst() : null;
                }
            } catch (JAXBException e) {
                throw new IOException("Cannot unmarshall " + result + ": " + e, e);
            }
        return null;
    }

    private List<Double> extractElevations(List<ElevationResponse.Result> responses) {
        List<Double> results = new ArrayList<>(responses.size());
        for (ElevationResponse.Result response : responses) {
            results.add(response.getElevation().doubleValue());
        }
        return results;
    }

    public boolean isDownload() {
        return false;
    }

    public String getPath() {
        throw new UnsupportedOperationException();
    }

    public void setPath(String path) {
        throw new UnsupportedOperationException();
    }

    public File getDirectory() {
        throw new UnsupportedOperationException();
    }

    public void downloadElevationDataFor(List<LongitudeAndLatitude> longitudeAndLatitudes, boolean waitForDownload) {
        throw new UnsupportedOperationException();
    }

    public long calculateRemainingDownloadSize(List<MapDescriptor> mapDescriptors) {
        throw new UnsupportedOperationException();
    }

    public void downloadElevationData(List<MapDescriptor> mapDescriptors) {
        throw new UnsupportedOperationException();
    }

    public Map<BoundingBox, Boolean> getCoverageTiles(BoundingBox area) {
        return Collections.emptyMap();
    }
}
