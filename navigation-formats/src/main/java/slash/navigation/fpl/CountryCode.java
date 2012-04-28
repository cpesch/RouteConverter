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

package slash.navigation.fpl;

/**
 * Enumeration of supported country codes.
 *
 * @author Christian Pesch
 */

public enum CountryCode {
    Austria("LO"),
    Croatia("LD"),
    Czechia("LK"),
    France("LF"),
    Germany("ED"),
    Hungary("LG"),
    Italy("LI"),
    Poland("EP"),
    Portugal("LP"),
    Slovakia("LZ"),
    Slovenia("LJ"),
    Spain("LE"),
    Swiss("LS"),
    UnitedStates1("K1"),
    UnitedStates2("K2"),
    UnitedStates3("K3"),
    UnitedStates4("K4"),
    UnitedStates5("K5"),
    UnitedStates6("K6"),
    UnitedStates7("K7"),
    Undefined("??");

    private String value;

    CountryCode(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    public static CountryCode fromValue(String value) {
        for (CountryCode countryCode : CountryCode.values()) {
            if (countryCode.value().equalsIgnoreCase(value))
                return countryCode;
        }
        return null;
    }
}
