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

package slash.navigation;

import slash.navigation.bcr.BcrPosition;
import slash.navigation.gopal.GoPalPosition;
import slash.navigation.gpx.GpxPosition;
import slash.navigation.itn.ItnPosition;
import slash.navigation.kml.KmlPosition;
import slash.navigation.nmea.NmeaPosition;
import slash.navigation.nmn.NmnPosition;
import slash.navigation.tour.TourPosition;
import slash.navigation.util.Bearing;

import java.util.Calendar;

/**
 * The base of all navigation positions.
 *
 * @author Christian Pesch
 */

public abstract class BaseNavigationPosition {

    /**
     * Return the longitude in the WGS84 coordinate system
     * @return the longitude in the WGS84 coordinate system
     */
    public abstract Double getLongitude();
    public abstract void setLongitude(Double longitude);

    /**
     * Return the latitude in the WGS84 coordinate system
     * @return the latitude in the WGS84 coordinate system
     */
    public abstract Double getLatitude();
    public abstract void setLatitude(Double latitude);

    public boolean hasCoordinates() {
        return getLongitude() != null && getLatitude() != null;
    }

    /**
     * Return the elevation in meters above sea level
     * @return the elevation in meters above sea level
     */
    public abstract Double getElevation();
    public abstract void setElevation(Double elevation);

    /**
     * Return the date and time in UTC time zone
     * @return the date and time in UTC time zone
     */
    public abstract Calendar getTime();

    public abstract String getComment();
    public abstract void setComment(String comment);

    /**
     * @return the distance in m this and the other position
     */
    public double getDistance(BaseNavigationPosition other) {
        Bearing bearing = Bearing.calculateBearing(getLongitude(), getLatitude(), other.getLongitude(), other.getLatitude());
        return bearing.getDistance();
    }

    /**
     * @return the azimuth in degree this and the other position
     */
    public double getAngle(BaseNavigationPosition other) {
        Bearing bearing = Bearing.calculateBearing(getLongitude(), getLatitude(), other.getLongitude(), other.getLatitude());
        return bearing.getAngle();
    }
    
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
     * @return the orthogonal distance to the line from pointA to pointB in meter. Positive numbers
     * mean right of course, negative numbers mean left of course
     */
    public double getOrthogonalDistance(BaseNavigationPosition pointA, BaseNavigationPosition pointB){
        double distanceAtoD = getDistance(pointA);
        double courseAtoD = Math.toRadians(pointA.getAngle(this));
        double courseAtoB = Math.toRadians(pointA.getAngle(pointB));
        return Math.asin(Math.sin(distanceAtoD / Bearing.EARTH_RADIUS) *
                Math.sin(courseAtoD - courseAtoB)) * Bearing.EARTH_RADIUS;
    }

    /**
     * @return the speed in km/h between this and the other position
     */
    public double getSpeed(KmlPosition other) {
        if (getTime() != null && other.getTime() != null) {
            long interval = Math.abs(getTime().getTimeInMillis() - other.getTime().getTimeInMillis()) / 1000;
            return getDistance(other) / interval * 3.6;
        } else
            return 0.0;
    }


    public Wgs84Position asCoPilotPosition() {
        return asWgs84Position();
    }

    public GkPosition asGkPosition() {
        return new GkPosition(getLongitude(), getLatitude(), getElevation(), getTime(), getComment());
    }

    public Wgs84Position asGlopusPosition() {
        return asWgs84Position();
    }

    public Wgs84Position asGoogleMapsPosition() {
        return asWgs84Position();
    }

    public GoPalPosition asGoPalRoutePosition() {
        return new GoPalPosition(getLongitude(), getLatitude(), getComment());
    }

    public Wgs84Position asGoPalTrackPosition() {
        return asWgs84Position();
    }

    public Wgs84Position asGpsTunerPosition() {
        return asWgs84Position();
    }

    public GpxPosition asGpxPosition() {
        return new GpxPosition(getLongitude(), getLatitude(), getElevation(), getTime(), getComment());
    }

    public Wgs84Position asHaicomLoggerPosition() {
        return asWgs84Position();
    }

    public ItnPosition asItnPosition() {
        return new ItnPosition(getLongitude(), getLatitude(), getElevation(), getTime(), getComment());
    }

    public KmlPosition asKmlPosition() {
        return new KmlPosition(getLongitude(), getLatitude(), getElevation(), getTime(), getComment());
    }

    public KmlPosition asKmzPosition() {
        return asKmlPosition();
    }

    public BcrPosition asMTPPosition() {
        return new BcrPosition(getLongitude(), getLatitude(), getElevation(), getComment());
    }

    public NmeaPosition asMagellanExploristPosition() {
        return asNmeaPosition();
    }

    public Wgs84Position asMagicMapsIktPosition() {
        return asWgs84Position();
    }

    public GkPosition asMagicMapsPthPosition() {
        return new GkPosition(getLongitude(), getLatitude(), getElevation(), getTime(), getComment());
    }

    public Wgs84Position asNavigatingPoiWarnerPosition() {
        return asWgs84Position();
    }

    public NmeaPosition asNmeaPosition() {
        return new NmeaPosition(getLongitude(), getLatitude(), getElevation(), getTime(), getComment());
    }

    public NmnPosition asNmnFavoritesPosition() {
        return asNmnPosition();
    }

    public NmnPosition asNmnPosition() {
        return new NmnPosition(getLongitude(), getLatitude(), getElevation(), getTime(), getComment());
    }

    public Wgs84Position asOvlPosition() {
        return asWgs84Position();
    }

    public Wgs84Position asRoutePosition() {
        return asWgs84Position();
    }

    public TourPosition asTourPosition() {
        return new TourPosition(getLongitude(), getLatitude(), getComment());
    }

    public Wgs84Position asWgs84Position() {
        return new Wgs84Position(getLongitude(), getLatitude(), getElevation(), getTime(), getComment());
    }
}
