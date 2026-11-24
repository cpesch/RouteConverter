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
package slash.navigation.csv;

import java.util.List;

import static java.util.Arrays.asList;

/**
 * Enumeration of supported column types.
 *
 * @author Christian Pesch
 */

enum ColumnType {
    Latitude(true, "Breite", "Breitengrad"),
    Longitude(true, "L\u00e4nge", "L\u00e4ngengrad"),
    // the hemisphere of an unsigned latitude or longitude, as in INDEX,LATITUDE,N/S,LONGITUDE,E/W
    LatitudeHemisphere(true, "N/S"),
    LongitudeHemisphere(true, "E/W"),
    Elevation(true, "H\u00f6he", "Altitude"),
    Speed(false, "Geschwindigkeit"),
    Heading(false, "Richtung", "Kurs", "Heading", "Course", "Direction"),
    Pressure(false, "Druck", "Pressure"),
    Temperature(false, "Temperatur", "Temperature"),
    Heartbeat(false, "Heartbeat", "hr", "Herzschlag", "Herzschlagfrequenz"),
    Time(true, "Zeit", "Timestamp", "Zeitstempel", "Date", "Datum", "Positionszeit", "Position time", "Positiontime"),
    Description(false, "Beschreibung", "Comment", "Kommentar"),
    Coordinates(true, "Koordinaten"),
    Unsupported(false);

    private final boolean caseInsensitive;
    private final List<String> alternativeNames;

    ColumnType(boolean caseInsensitive, String... alternativeNames) {
        this.caseInsensitive = caseInsensitive;
        this.alternativeNames = asList(alternativeNames);
    }

    public List<String> getAlternativeNames() {
        return alternativeNames;
    }

    /**
     * The names of coordinate, elevation and time columns match case-insensitively: Google
     * Takeout writes {@code timestamp,latitude,longitude,altitude}. The others match exactly,
     * so that a lower-case {@code description} column alone does not claim a foreign CSV.
     */
    boolean matches(String columnName) {
        if (columnName == null)
            return false;
        if (caseInsensitive ? name().equalsIgnoreCase(columnName) : name().equals(columnName))
            return true;
        for (String alternativeName : getAlternativeNames())
            if (caseInsensitive ? alternativeName.equalsIgnoreCase(columnName) : alternativeName.equals(columnName))
                return true;
        return false;
    }

    static ColumnType classify(String value) {
        if (value != null) {
            for (ColumnType columnType : values()) {
                if (columnType.matches(value))
                    return columnType;
            }
        }
        return Unsupported;
    }
}
