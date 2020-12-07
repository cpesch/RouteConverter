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
    Latitude("Breite", "Breitengrad"),
    Longitude("L\u00e4nge", "L\u00e4ngengrad"),
    Elevation("H\u00f6he", "Altitude"),
    Speed("Geschwindigkeit"),
    Heading("Richtung", "Kurs", "Heading", "Course"),
    Pressure("Druck", "Pressure"),
    Temperature("Temperatur", "Temperature"),
    Heartbeat("Heartbeat", "hr", "Herzschlag", "Herzschlagfrequenz"),
    Time("Zeit", "Timestamp", "Zeitstempel", "Date", "Datum"),
    Description("Beschreibung", "Comment", "Kommentar"),
    Unsupported();

    private List<String> alternativeNames;

    ColumnType(String... alternativeNames) {
        this.alternativeNames = asList(alternativeNames);
    }

    public List<String> getAlternativeNames() {
        return alternativeNames;
    }
}
