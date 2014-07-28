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

package slash.navigation.geonames;

/**
 * A country code, postal code, place name aggregate returned when accessing the GeoNames.org service.
 *
 * @author Christian Pesch
 */

public class PostalCode {
    public String countryCode, postalCode, placeName;

    public PostalCode(String countryCode, String postalCode, String placeName) {
        this.countryCode = countryCode;
        this.postalCode = postalCode;
        this.placeName = placeName;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PostalCode that = (PostalCode) o;

        return !(countryCode != null ? !countryCode.equals(that.countryCode) : that.countryCode != null) &&
                !(placeName != null ? !placeName.equals(that.placeName) : that.placeName != null) &&
                !(postalCode != null ? !postalCode.equals(that.postalCode) : that.postalCode != null);
    }

    public int hashCode() {
        int result = countryCode != null ? countryCode.hashCode() : 0;
        result = 31 * result + (postalCode != null ? postalCode.hashCode() : 0);
        result = 31 * result + (placeName != null ? placeName.hashCode() : 0);
        return result;
    }

    public String toString() {
        return getClass().getSimpleName() + "[countryCode=" + countryCode + ", postalCode=" + postalCode +
                ", placeName=" + placeName + "]";
    }
}
