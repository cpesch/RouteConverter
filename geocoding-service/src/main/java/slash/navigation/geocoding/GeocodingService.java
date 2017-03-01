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

package slash.navigation.geocoding;

import slash.navigation.common.NavigationPosition;

import javax.naming.ServiceUnavailableException;
import java.io.IOException;
import java.util.List;

/**
 * Interface for a service that translates addresses into geographic coordinates
 * and a geographic coordinate to an address.
 *
 * @author Christian Pesch
 */

public interface GeocodingService {
    String getName();
    boolean isDownload();
    boolean isOverQueryLimit();

    /**
     * Retrieves a list of {@link NavigationPosition}s for a given address.
     * @param address the address to geocode
     * @return a list of {@link NavigationPosition}s for the given address
     * @throws ServiceUnavailableException if the service is overloaded
     * @throws IOException if the request fails
     */
    List<NavigationPosition> getPositionsFor(String address) throws IOException, ServiceUnavailableException;

    /**
     * Retrieves an address for a given {@link NavigationPosition}.
     * @param position the {@link NavigationPosition} to reverse geocode the address
     * @return an address for the given {@link NavigationPosition}
     * @throws ServiceUnavailableException if the service is overloaded
     * @throws IOException if the request fails
     */
    String getAddressFor(NavigationPosition position) throws IOException, ServiceUnavailableException;
}
