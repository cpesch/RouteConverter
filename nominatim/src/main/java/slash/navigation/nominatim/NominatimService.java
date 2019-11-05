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

package slash.navigation.nominatim;

import slash.navigation.common.NavigationPosition;
import slash.navigation.common.SimpleNavigationPosition;
import slash.navigation.geocoding.GeocodingService;
import slash.navigation.nominatim.reverse.AddresspartsType;
import slash.navigation.nominatim.reverse.ReversegeocodeType;
import slash.navigation.nominatim.search.PlaceType;
import slash.navigation.nominatim.search.SearchresultsType;
import slash.navigation.rest.Get;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

import static slash.common.io.Transfer.encodeUri;
import static slash.common.io.Transfer.trim;

/**
 * Encapsulates REST access to the OSM Nominatim service.
 *
 * @author Christian Pesch
 */

public class NominatimService implements GeocodingService {
    private static final Preferences preferences = Preferences.userNodeForPackage(NominatimService.class);
    private static final String NOMINATIM_URL_PREFERENCE = "nominatiumUrl";

    public String getName() {
        return "Nominatim";
    }

    public boolean isDownload() {
        return false;
    }

    public boolean isOverQueryLimit() {
        return false;
    }

    private String getNominatimUrl() {
        return preferences.get(NOMINATIM_URL_PREFERENCE, "https://nominatim.openstreetmap.org/");
    }

    private String execute(String uri) throws IOException {
        String url = getNominatimUrl() + uri;
        Get get = new Get(url);
        String result = get.executeAsString();
        if (get.isSuccessful())
            return result;
        return null;
    }

    private SearchresultsType getSearchFor(String uri) throws IOException {
        String result = execute(uri);
        if (result != null) {
            try {
                return NominatimUtil.unmarshalSearch(result);
            } catch (JAXBException e) {
                throw new IOException("Cannot unmarshall " + result + ": " + e, e);
            }
        }
        return null;
    }

    private List<NavigationPosition> extractPositions(List<PlaceType> placeTypes) {
        List<NavigationPosition> result = new ArrayList<>(placeTypes.size());
        for (PlaceType placeType : placeTypes) {
            result.add(new SimpleNavigationPosition(placeType.getLon().doubleValue(), placeType.getLat().doubleValue(),
                    null, placeType.getDisplayName() + " (" + placeType.getType() + ")"));
        }
        return result;
    }

    public List<NavigationPosition> getPositionsFor(String address) throws IOException {
        SearchresultsType result = getSearchFor("search.php/?q=" + encodeUri(address) + "&limit=10&format=xml");
        if (result == null)
            return null;
        return extractPositions(result.getPlace());
    }

    private ReversegeocodeType getReverseFor(String uri) throws IOException {
        String result = execute(uri);
        if (result != null) {
            try {
                return NominatimUtil.unmarshalReverse(result);
            } catch (JAXBException e) {
                throw new IOException("Cannot unmarshall " + result + ": " + e, e);
            }
        }
        return null;
    }

    public String getAddressFor(NavigationPosition position) throws IOException {
        ReversegeocodeType type = getReverseFor("reverse?lat=" + position.getLatitude() +
                "&lon=" + position.getLongitude() + "&zoom=18&addressdetails=1&format=xml");
        if (type == null)
            return null;
        AddresspartsType parts = type.getAddressparts();
        if (parts == null)
            return null;

        String result = (parts.getRoad() != null ? parts.getRoad() :
                parts.getSuburb() != null ? parts.getSuburb() : "") +
                (parts.getHouseNumber() != null ? " " + parts.getHouseNumber() : "");
        if (result.length() > 0)
            result += ", ";
        result += (parts.getPostcode() != null ? parts.getPostcode() : "") + " " +
                        (parts.getCity() != null ? parts.getCity() + ", " :
                                parts.getTown() != null ? parts.getTown() + ", " :
                                        parts.getVillage() != null ? parts.getVillage() + ", " :
                                                parts.getCounty() != null ? parts.getCounty() + ", " : "") +
                        (parts.getState() != null ? parts.getState() + ", " : "") +
                        (parts.getCountry() != null ? parts.getCountry() : "");
        return trim(result);
    }
}
