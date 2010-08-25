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

import slash.navigation.bcr.BcrPosition;
import slash.navigation.gopal.GoPalPosition;
import slash.navigation.gpx.GpxPosition;
import slash.navigation.itn.TomTomPosition;
import slash.navigation.kml.KmlPosition;
import slash.navigation.nmea.NmeaPosition;
import slash.navigation.nmn.NmnPosition;
import slash.navigation.tour.TourPosition;
import slash.navigation.util.Bearing;
import slash.common.io.CompactCalendar;

import java.util.Calendar;

/**
 * The base of all navigation positions.
 *
 * @author Christian Pesch
 */

public abstract class BaseNavigationPosition {
    protected Double elevation;
    private Double speed;
    protected CompactCalendar time;

    protected BaseNavigationPosition(Double elevation, Double speed, CompactCalendar time) {
        setElevation(elevation);
        setSpeed(speed);
        setTime(time);
    }

    /**
     * Return the longitude in the WGS84 coordinate system
     *
     * @return the longitude in the WGS84 coordinate system
     */
    public abstract Double getLongitude();

    public abstract void setLongitude(Double longitude);

    /**
     * Return the latitude in the WGS84 coordinate system
     *
     * @return the latitude in the WGS84 coordinate system
     */
    public abstract Double getLatitude();

    public abstract void setLatitude(Double latitude);

    public boolean hasCoordinates() {
        return getLongitude() != null && getLatitude() != null;
    }

    /**
     * Return the elevation in meters above sea level
     *
     * @return the elevation in meters above sea level
     */
    public Double getElevation() {
        return elevation;
    }

    public void setElevation(Double elevation) {
        this.elevation = elevation;
    }

    /**
     * Return the date and time in UTC time zone
     *
     * @return the date and time in UTC time zone
     */
    public CompactCalendar getTime() {
        return time;
    }

    public void setTime(CompactCalendar time) {
        this.time = time;
    }

    /**
     * Return the speed in kilometers per hour
     *
     * @return the speed in kilometers per hour
     */
    public Double getSpeed() {
        return speed;
    }

    public void setSpeed(Double speed) {
        this.speed = speed;
    }

    /**
     * Set the day/month/year-offset for date-less, time-only positions
     *
     * @param startDate the day/month/year-offset
     */
    public void setStartDate(CompactCalendar startDate) {
        if (time != null && startDate != null) {
            Calendar calendar = time.getCalendar();
            Calendar startDateCalendar = startDate.getCalendar();
            calendar.set(Calendar.YEAR, startDateCalendar.get(Calendar.YEAR));
            calendar.set(Calendar.MONTH, startDateCalendar.get(Calendar.MONTH));
            calendar.set(Calendar.DAY_OF_MONTH, startDateCalendar.get(Calendar.DAY_OF_MONTH));
            time = CompactCalendar.fromCalendar(calendar);
        }
    }

    public abstract String getComment();

    public abstract void setComment(String comment);

    /**
     * Calculate the distance in meter between this and the other position.
     *
     * @param other the other position
     * @return the distance in meter between this and the other position
     *         or null if the distance cannot be calculated
     */
    public Double calculateDistance(BaseNavigationPosition other) {
        if (hasCoordinates() && other.hasCoordinates()) {
            Bearing bearing = Bearing.calculateBearing(getLongitude(), getLatitude(), other.getLongitude(), other.getLatitude());
            double distance = bearing.getDistance();
            if (!Double.isNaN(distance))
                return distance;
        }
        return null;
    }

    /**
     * Calculate the elevation in meter between this and the other position.
     *
     * @param other the other position
     * @return the elevation in meter between this and the other position
     *         or null if the elevation cannot be calculated
     */
    public Double calculateElevation(BaseNavigationPosition other) {
        if (getElevation() != null && other.getElevation() != null) {
            return other.getElevation() - getElevation();
        }
        return null;
    }

    /**
     * Calculate the time in milliseconds between this and the other position.
     *
     * @param other the other position
     * @return the time in milliseconds between this and the other position
     *         or null if the time cannot be calculated
     */
    public Long calculateTime(BaseNavigationPosition other) {
        if (getTime() != null && other.getTime() != null) {
            return other.getTime().getTimeInMillis() - getTime().getTimeInMillis();
        }
        return null;
    }

    /**
     * Calculate the angle in degree between this and the other position.
     *
     * @param other the other position
     * @return the angle in degree beween this and the other position
     *         or null if the angle cannot be calculated
     */
    public Double calculateAngle(BaseNavigationPosition other) {
        if (hasCoordinates() && other.hasCoordinates()) {
            Bearing bearing = Bearing.calculateBearing(getLongitude(), getLatitude(), other.getLongitude(), other.getLatitude());
            return bearing.getAngle();
        }
        return null;
    }

    /**
     * Calculate the orthogonal distance of this position to the line from pointA to pointB,
     * supposed you are proceeding on a great circle route from A to B and end up at D, perhaps
     * ending off course.
     * <p/>
     * http://williams.best.vwh.net/avform.htm#XTE
     * http://www.movable-type.co.uk/scripts/latlong.html
     *
     * @param pointA the first point determining the line between pointA and pointB
     * @param pointB the second point determining the line between pointA and pointB
     * @return the orthogonal distance to the line from pointA to pointB in meter
     *         or null if the orthogonal distance cannot be calculated
     */
    public Double calculateOrthogonalDistance(BaseNavigationPosition pointA, BaseNavigationPosition pointB) {
        if (hasCoordinates() && pointA.hasCoordinates() && pointB.hasCoordinates()) {
            Double distanceAtoD = calculateDistance(pointA);
            if (distanceAtoD != null) {
                double courseAtoD = Math.toRadians(pointA.calculateAngle(this));
                double courseAtoB = Math.toRadians(pointA.calculateAngle(pointB));
                return Math.asin(Math.sin(distanceAtoD / Bearing.EARTH_RADIUS) *
                        Math.sin(courseAtoD - courseAtoB)) * Bearing.EARTH_RADIUS;
            }
        }
        return null;
    }

    /**
     * Calculate the average speed between this and the other position.
     *
     * @param other the other position
     * @return the speed in kilometers per hour between this and the other position
     *         or null if time or distance cannot be calculated
     */
    public Double calculateSpeed(BaseNavigationPosition other) {
        if (getTime() != null && other.getTime() != null) {
            double interval = Math.abs(getTime().getTimeInMillis() - other.getTime().getTimeInMillis()) / 1000.0;

            Double distance = calculateDistance(other);
            if (distance != null && interval > 0.0)
                return distance / interval * 3.6;
        }
        return null;
    }


    public Wgs84Position asColumbusVStandardPosition() {
        return asWgs84Position();
    }

    public Wgs84Position asColumbusVProfessionalPosition() {
        return asWgs84Position();
    }

    public Wgs84Position asCoPilotPosition() {
        return asWgs84Position();
    }

    public GpxPosition asCrsPosition() {
        return asGpxPosition();
    }
    
    public GkPosition asGkPosition() {
        return new GkPosition(getLongitude(), getLatitude(), getElevation(), getSpeed(), getTime(), getComment());
    }

    public Wgs84Position asGlopusPosition() {
        return asWgs84Position();
    }

    public Wgs84Position asGoogleMapsPosition() {
        return asWgs84Position();
    }

    public GoPalPosition asGoPalRoutePosition() {
        return new GoPalPosition(getLongitude(), getLatitude(), getElevation(), getSpeed(), getTime(), getComment());
    }

    public Wgs84Position asGoPalTrackPosition() {
        return asWgs84Position();
    }

    public Wgs84Position asGpsTunerPosition() {
        return asWgs84Position();
    }

    public GpxPosition asBrokenGpxPosition() {
        return asGpxPosition();
    }

    public GpxPosition asGpxPosition() {
        return new GpxPosition(getLongitude(), getLatitude(), getElevation(), getSpeed(), getTime(), getComment());
    }

    public Wgs84Position asHaicomLoggerPosition() {
        return asWgs84Position();
    }

    public TomTomPosition asTomTomRoutePosition() {
        return new TomTomPosition(getLongitude(), getLatitude(), getElevation(), getSpeed(), getTime(), getComment());
    }

    public Wgs84Position asKlickTelRoutePosition() {
        return asWgs84Position();
    }

    public KmlPosition asBrokenKmlPosition() {
        return asKmlPosition();
    }

    public KmlPosition asBrokenKmlLittleEndianPosition() {
        return asKmlPosition();
    }

    public KmlPosition asBrokenKmlBetaPosition() {
        return asKmlPosition();
    }

    public KmlPosition asKmlPosition() {
        return new KmlPosition(getLongitude(), getLatitude(), getElevation(), getSpeed(), getTime(), getComment());
    }

    public KmlPosition asKmlBetaPosition() {
        return asKmlPosition();
    }

    public KmlPosition asBrokenKmzPosition() {
        return asKmzPosition();
    }

    public KmlPosition asBrokenKmzLittleEndianPosition() {
        return asKmzPosition();
    }

    public KmlPosition asKmzPosition() {
        return asKmlPosition();
    }

    public KmlPosition asKmzBetaPosition() {
        return asKmzPosition();
    }

    public Wgs84Position asKompassPosition() {
        return asWgs84Position();
    }

    public BcrPosition asMTPPosition() {
        return new BcrPosition(getLongitude(), getLatitude(), getElevation(), getSpeed(), getTime(), getComment());
    }

    public NmeaPosition asMagellanExploristPosition() {
        return asNmeaPosition();
    }

    public NmeaPosition asMagellanRoutePosition() {
        return asNmeaPosition();
    }

    public Wgs84Position asMagicMapsIktPosition() {
        return asWgs84Position();
    }

    public GkPosition asMagicMapsPthPosition() {
        return new GkPosition(getLongitude(), getLatitude(), getElevation(), getSpeed(), getTime(), getComment());
    }

    public Wgs84Position asNavigatingPoiWarnerPosition() {
        return asWgs84Position();
    }

    public NmeaPosition asNmeaPosition() {
        return new NmeaPosition(getLongitude(), getLatitude(), getElevation(), getSpeed(), getTime(), getComment());
    }

    public NmnPosition asNmnFavoritesPosition() {
        return asNmnPosition();
    }

    public NmnPosition asNmnPosition() {
        return new NmnPosition(getLongitude(), getLatitude(), getElevation(), getSpeed(), getTime(), getComment());
    }

    public Wgs84Position asNokiaLandmarkExchangePosition() {
        return asWgs84Position();
    }

    public Wgs84Position asOvlPosition() {
        return asWgs84Position();
    }

    public Wgs84Position asRoutePosition() {
        return asWgs84Position();
    }

    public Wgs84Position asSygicUnicodePosition() {
        return asWgs84Position();
    }

    public GpxPosition asTcxPosition() {
        return asGpxPosition();
    }

    public TourPosition asTourPosition() {
        return new TourPosition(getLongitude(), getLatitude(), getElevation(), getSpeed(), getTime(), getComment());
    }

    public Wgs84Position asWgs84Position() {
        return new Wgs84Position(getLongitude(), getLatitude(), getElevation(), getSpeed(), getTime(), getComment());
    }
}
