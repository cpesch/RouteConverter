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

import slash.common.helpers.APIKeyRegistry;
import slash.navigation.rest.Get;
import slash.navigation.rest.exception.ServiceUnavailableException;

import java.util.Locale;
import java.util.function.BooleanSupplier;
import java.util.logging.Logger;

import static slash.common.io.Transfer.removeQueryParameter;
import static slash.navigation.googlemaps.GoogleMapsServer.getGoogleMapsServer;

/**
 * Shared REST/API-key plumbing for the three independent Google services
 * (geocoding, elevation, routing) -- each is its own {@link
 * slash.navigation.geocoding.GeocodingService}/{@link slash.navigation.elevation.ElevationService}/
 * {@link slash.navigation.routing.RoutingService} implementation with its own
 * query-limit bookkeeping (a shared counter would attribute one service's
 * quota exhaustion to another), but all three build URLs and check for
 * errors the same way.
 *
 * @author Christian Pesch
 */

class GoogleApiClient {
    private static final Logger log = Logger.getLogger(GoogleApiClient.class.getName());
    private final BooleanSupplier hasApiKey;
    private int overQueryLimitCount, deniedCount;

    GoogleApiClient() {
        this(() -> APIKeyRegistry.getInstance().hasAPIKey("google"));
    }

    GoogleApiClient(BooleanSupplier hasApiKey) {
        this.hasApiKey = hasApiKey;
    }

    // without a key Google denies every request, so treat it like being over the limit
    boolean isOverQueryLimit() {
        return !hasApiKey.getAsBoolean() || overQueryLimitCount > 5 || deniedCount > 5;
    }

    String getGoogleApiUrl(String apiType, String payload) {
        String language = Locale.getDefault().getLanguage();
        String apiKey = APIKeyRegistry.getInstance().getAPIKey("google", apiType);
        return getGoogleMapsServer().getApiUrl() + "/maps/api/" + apiType + "/xml?" + payload +
                "&sensor=false&language=" + language + "&key=" + apiKey;
    }

    Get get(String url) {
        return new Get(url);
    }

    void checkForError(String callerName, String url, String status) throws ServiceUnavailableException {
        // the url ends up in the log and in error reports, the API key must not
        String urlWithoutKey = removeQueryParameter(url, "key");
        if (status.equals("OVER_QUERY_LIMIT")) {
            overQueryLimitCount++;
            log.warning("Google API is over query limit, count: " + overQueryLimitCount + ", url: " + urlWithoutKey);
            throw new ServiceUnavailableException(callerName, urlWithoutKey, status);
        }

        if (status.equals("REQUEST_DENIED")) {
            deniedCount++;
            log.warning("Google API access is denied, count: " + deniedCount + ", url: " + urlWithoutKey);
            throw new ServiceUnavailableException(callerName, urlWithoutKey, status);
        }
    }
}
