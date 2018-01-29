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

package slash.navigation.converter.gui.models;

/**
 * Contains constants for all columns of the positions table.
 *
 * @author Christian Pesch
 */

public interface PositionColumns {
    int DESCRIPTION_COLUMN_INDEX = 0;
    int DATE_TIME_COLUMN_INDEX = 1;
    int DATE_COLUMN_INDEX = 15;
    int TIME_COLUMN_INDEX = 2;
    int LONGITUDE_COLUMN_INDEX = 3;
    int LATITUDE_COLUMN_INDEX = 4;
    int ELEVATION_COLUMN_INDEX = 5;
    int SPEED_COLUMN_INDEX = 6;
    int TEMPERATURE_COLUMN_INDEX = 17;
    int PRESSURE_COLUMN_INDEX = 16;
    int DISTANCE_COLUMN_INDEX = 7;
    int DISTANCE_DIFFERENCE_COLUMN_INDEX = 18;
    int ELEVATION_ASCEND_COLUMN_INDEX = 8;
    int ELEVATION_DESCEND_COLUMN_INDEX = 9;
    int ELEVATION_DIFFERENCE_COLUMN_INDEX = 10;
    int WAYPOINT_TYPE_COLUMN_INDEX = 12;
    int PHOTO_COLUMN_INDEX = 11;
    int EXIF_COLUMN_INDEX = 13;
    int GPS_COLUMN_INDEX = 14;
}
