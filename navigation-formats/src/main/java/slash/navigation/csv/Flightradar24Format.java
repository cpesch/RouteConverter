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

import slash.common.type.CompactCalendar;
import slash.common.type.ISO8601;

import java.util.LinkedHashMap;
import java.util.Map;

import static java.lang.Math.round;
import static slash.common.io.Transfer.*;
import static slash.navigation.common.UnitConversion.*;
import static slash.navigation.csv.ColumnType.*;
import static slash.navigation.csv.CsvPosition.parseCalendar;

/**
 * Reads comma separated CSV (.csv) files from <a href="https://www.flightradar24.com/">Flightradar24</a>.
 *
 * @author Christian Pesch
 */

public class Flightradar24Format extends CsvFormat {
    private static final String TIMESTAMP_COLUMN = "Timestamp";
    private static final String UTC_COLUMN = "UTC";
    private static final String CALLSIGN_COLUMN = "Callsign";
    private static final String POSITION_COLUMN = "Position";
    private static final String ALTITUDE_COLUMN = "Altitude";
    private static final String DIRECTION_COLUMN = "Direction";

    public String getName() {
        return "Flightradar 24 (" + getExtension() + ")";
    }

    protected char getColumnSeparator() {
        return ',';
    }

    protected LinkedHashMap<String, String> transformRead(LinkedHashMap<String, String> rowAsMap) {
        LinkedHashMap<String, String> result = new LinkedHashMap<>(rowAsMap);

        result.put(Time.name(), trim(result.remove(UTC_COLUMN)));
        result.remove(TIMESTAMP_COLUMN);
        result.put(Description.name(), trim(result.remove(CALLSIGN_COLUMN)));

        String position = result.remove(POSITION_COLUMN);
        if (position != null && !position.isEmpty()) {
            String[] parts = position.split(",");
            if (parts.length == 2) {
                result.put(Latitude.name(), trim(parts[0]));
                result.put(Longitude.name(), trim(parts[1]));
            }
        }

        Double altitude = parseDouble(result.remove(ALTITUDE_COLUMN));
        if (altitude != null)
            result.put(Elevation.name(), formatDoubleAsString(feetToMeters(altitude), 2));

        Double speed = parseDouble(result.remove(Speed.name()));
        if (speed != null)
            result.put(Speed.name(), formatDoubleAsString(nauticMilesToKiloMeter(speed), 2));

        result.put(Heading.name(), trim(result.remove(DIRECTION_COLUMN)));

        return result;
    }

    protected Map<String, String> transformWrite(Map<String, String> rowAsMap) {
        LinkedHashMap<String, String> result = new LinkedHashMap<>();

        CompactCalendar time = parseCalendar(rowAsMap.get(Time.name()));
        if (time != null) {
            result.put(TIMESTAMP_COLUMN, formatLongAsString(time.getTimeInMillis() / 1000));
            result.put(UTC_COLUMN, ISO8601.formatDate(time));
        }

        String description = rowAsMap.get(Description.name());
        if (description != null)
            result.put(CALLSIGN_COLUMN, description);

        String latitude = rowAsMap.get(Latitude.name());
        String longitude = rowAsMap.get(Longitude.name());
        if (latitude != null && longitude != null)
            result.put(POSITION_COLUMN, latitude + "," + longitude);

        Double elevation = parseDouble(rowAsMap.get(Elevation.name()));
        if (elevation != null)
            result.put(ALTITUDE_COLUMN, formatLongAsString(round(meterToFeets(elevation))));

        Double speed = parseDouble(rowAsMap.get(Speed.name()));
        if (speed != null)
            result.put(Speed.name(), formatLongAsString(round(kiloMeterToNauticMiles(speed))));

        result.put(DIRECTION_COLUMN, rowAsMap.get(Heading.name()));

        return result;
    }
}
