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

package slash.navigation.bcr;

import java.io.PrintWriter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static slash.common.io.Transfer.trim;
import static slash.navigation.bcr.BcrPosition.STREET_DEFINES_CENTER_NAME;
import static slash.navigation.bcr.BcrPosition.STREET_DEFINES_CENTER_SYMBOL;
import static slash.navigation.bcr.BcrPosition.ZIPCODE_DEFINES_NOTHING;
import static slash.navigation.bcr.BcrSection.STATION_PREFIX;

/**
 * Reads and writes Map &amp; Guide Tourenplaner 2008/2009 (.bcr) files.
 *
 * @author Christian Pesch
 */

public class MTP0809Format extends BcrFormat {
    static final Pattern DESCRIPTION_PATTERN = Pattern.
            compile("(.*?)" + VALUE_SEPARATOR + "(.*)" + VALUE_SEPARATOR +
                    "(.*)" + VALUE_SEPARATOR +
                    "(0)" + VALUE_SEPARATOR + "$");

    public String getName() {
        return "Map&Guide Tourenplaner 2008/2009 (*" + getExtension() + ")";
    }

    protected boolean isValidDescription(BcrSection description) {
        for (int i = 0; i < description.getStationCount(); i++) {
            String stationName = description.getStation(i);
            if (stationName != null) {
                Matcher matcher = DESCRIPTION_PATTERN.matcher(stationName);
                if (!matcher.matches())
                    return false;
            }
        }
        return true;
    }

    protected void writePosition(BcrPosition position, PrintWriter writer, int index) {
        String zipCode = trim(position.getZipCode()) != null ? position.getZipCode() : ZIPCODE_DEFINES_NOTHING;
        String city = position.getCity() != null ? position.getCity() : "";
        String street = trim(position.getStreet()) != null ? position.getStreet() : "";
        if (STREET_DEFINES_CENTER_NAME.equals(street))
            street = STREET_DEFINES_CENTER_SYMBOL;
        String type = trim(position.getType()) != null ? position.getType() : "0";
        String description = zipCode + VALUE_SEPARATOR + city + VALUE_SEPARATOR + street + VALUE_SEPARATOR + type + VALUE_SEPARATOR;
        writer.println(STATION_PREFIX + index + NAME_VALUE_SEPARATOR + description);
    }
}
