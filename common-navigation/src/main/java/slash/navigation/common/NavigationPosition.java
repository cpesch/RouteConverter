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
package slash.navigation.common;

import slash.common.type.CompactCalendar;

/**
 * A navigation position.
 *
 * @author Christian Pesch
 */

public interface NavigationPosition {
    /**
     * Return the longitude in the WGS84 coordinate system
     *
     * @return the longitude in the WGS84 coordinate system
     */
    Double getLongitude();
    void setLongitude(Double longitude);

    /**
     * Return the latitude in the WGS84 coordinate system
     *
     * @return the latitude in the WGS84 coordinate system
     */
    Double getLatitude();
    void setLatitude(Double latitude);

    boolean hasCoordinates();

    /**
     * Return the elevation in meters above sea level
     *
     * @return the elevation in meters above sea level
     */
    Double getElevation();
    void setElevation(Double elevation);

    /**
     * Return the date and time in UTC time zone
     *
     * @return the date and time in UTC time zone
     */
    CompactCalendar getTime();
    void setTime(CompactCalendar time);

    boolean hasTime();

    /**
     * Return the speed in kilometers per hour
     *
     * @return the speed in kilometers per hour
     */
    Double getSpeed();
    void setSpeed(Double speed);

    String getDescription();
    void setDescription(String description);

    /**
     * Set the day/month/year-offset for date-less, time-only positions
     *
     * @param startDate the day/month/year-offset
     */
    void setStartDate(CompactCalendar startDate);

    /**
     * Calculate the distance in meters between this and the other position.
     *
     * @param other the other position
     * @return the distance in meters between this and the other position
     *         or null if the distance cannot be calculated
     */
    Double calculateDistance(NavigationPosition other);
    Double calculateDistance(double longitude, double latitude);

    /**
     * Calculate the orthogonal distance of this position to the line from pointA to pointB,
     * supposed you are proceeding on a great circle route from A to B and end up at D, perhaps
     * ending off course.
     *
     * http://williams.best.vwh.net/avform.htm#XTE
     * http://www.movable-type.co.uk/scripts/latlong.html
     *
     * @param pointA the first point determining the line between pointA and pointB
     * @param pointB the second point determining the line between pointA and pointB
     * @return the orthogonal distance to the line from pointA to pointB in meter
     *         or null if the orthogonal distance cannot be calculated
     */
    Double calculateOrthogonalDistance(NavigationPosition pointA, NavigationPosition pointB);

    /**
     * Calculate the angle in degree between this and the other position.
     *
     * @param other the other position
     * @return the angle in degree between this and the other position
     *         or null if the angle cannot be calculated
     */
    Double calculateAngle(NavigationPosition other);

    /**
     * Calculate the elevation in meter between this and the other position.
     *
     * @param other the other position
     * @return the elevation in meter between this and the other position
     *         or null if the elevation cannot be calculated
     */
    Double calculateElevation(NavigationPosition other);

    /**
     * Calculate the time in milliseconds between this and the other position.
     *
     * @param other the other position
     * @return the time in milliseconds between this and the other position
     *         or null if the time cannot be calculated
     */
    Long calculateTime(NavigationPosition other);

    /**
     * Calculate the average speed in kilometers per hour between this and the other position.
     *
     * @param other the other position
     * @return the speed in kilometers per hour between this and the other position
     *         or null if time or distance cannot be calculated
     */
    Double calculateSpeed(NavigationPosition other);
}