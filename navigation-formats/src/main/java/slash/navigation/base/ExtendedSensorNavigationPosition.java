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

package slash.navigation.base;

import slash.navigation.common.NavigationPosition;

/**
 * A navigation position that contains data about heading, pressure, temperature.
 *
 * @author Christian Pesch
 */

public interface ExtendedSensorNavigationPosition extends NavigationPosition {
    /**
     * Return the heading of the current course in 0-359 degrees
     *
     * @return the heading of the current course in 0-359 degrees
     */
    Double getHeading();
    void setHeading(Double heading);

    /**
     * Return the pressure in millibars
     *
     * @return the pressure in millibars
     */
    Double getPressure();
    void setPressure(Double pressure);

    /**
     * Return the temperature in degrees celsius
     *
     * @return the temperature in degrees celsius
     */
    Double getTemperature();
    void setTemperature(Double temperature);

    /**
     * Return the heart beat rate in beats per minute
     *
     * @return the heart beat rate in beats per minute
     */
    Short getHeartBeat();
    void setHeartBeat(Short heartBeat);

    static void transferExtendedSensorData(ExtendedSensorNavigationPosition from, ExtendedSensorNavigationPosition to) {
        to.setHeading(from.getHeading());
        to.setPressure(from.getPressure());
        to.setTemperature(from.getTemperature());
        to.setHeartBeat(from.getHeartBeat());
    }
}
