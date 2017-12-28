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

import slash.common.type.CompactCalendar;
import slash.navigation.bcr.BcrPosition;
import slash.navigation.common.Bearing;
import slash.navigation.common.NavigationPosition;
import slash.navigation.csv.ExcelPosition;
import slash.navigation.fpl.GarminFlightPlanPosition;
import slash.navigation.gopal.GoPalPosition;
import slash.navigation.gpx.GpxPosition;
import slash.navigation.itn.TomTomPosition;
import slash.navigation.kml.KmlPosition;
import slash.navigation.nmea.NmeaPosition;
import slash.navigation.nmn.NmnPosition;
import slash.navigation.tour.TourPosition;

import java.util.Calendar;

import static java.lang.Double.isNaN;
import static java.lang.Math.abs;
import static java.lang.Math.asin;
import static java.lang.Math.sin;
import static java.lang.Math.toRadians;
import static java.util.Calendar.DAY_OF_MONTH;
import static java.util.Calendar.MONTH;
import static java.util.Calendar.YEAR;
import static slash.common.io.Transfer.isEmpty;
import static slash.common.type.CompactCalendar.fromCalendar;
import static slash.navigation.common.Bearing.EARTH_RADIUS;
import static slash.navigation.common.Bearing.calculateBearing;

/**
 * The base of all navigation positions.
 *
 * @author Christian Pesch
 */

public abstract class BaseNavigationPosition implements NavigationPosition {

    public boolean hasCoordinates() {
        return getLongitude() != null && getLatitude() != null;
    }

    public boolean hasTime() {
        return getTime() != null;
    }

    public boolean hasSpeed() {
        return getSpeed() != null;
    }

    public void setStartDate(CompactCalendar startDate) {
        if (hasTime() && startDate != null) {
            Calendar calendar = getTime().getCalendar();
            Calendar startDateCalendar = startDate.getCalendar();
            calendar.set(YEAR, startDateCalendar.get(YEAR));
            calendar.set(MONTH, startDateCalendar.get(MONTH));
            calendar.set(DAY_OF_MONTH, startDateCalendar.get(DAY_OF_MONTH));
            setTime(fromCalendar(calendar));
        }
    }

    public Double calculateDistance(NavigationPosition other) {
        return other.hasCoordinates() ? calculateDistance(other.getLongitude(), other.getLatitude()) : null;
    }

    public Double calculateDistance(double longitude, double latitude) {
        if (hasCoordinates()) {
            Bearing bearing = calculateBearing(getLongitude(), getLatitude(), longitude, latitude);
            double distance = bearing.getDistance();
            if (!isNaN(distance))
                return distance;
        }
        return null;
    }

    public Double calculateElevation(NavigationPosition other) {
        if (getElevation() != null && other.getElevation() != null) {
            return other.getElevation() - getElevation();
        }
        return null;
    }

    public Long calculateTime(NavigationPosition other) {
        if (hasTime() && other.hasTime()) {
            return other.getTime().getTimeInMillis() - getTime().getTimeInMillis();
        }
        return null;
    }

    public Double calculateAngle(NavigationPosition other) {
        if (hasCoordinates() && other.hasCoordinates()) {
            Bearing bearing = calculateBearing(getLongitude(), getLatitude(), other.getLongitude(), other.getLatitude());
            return bearing.getAngle();
        }
        return null;
    }

    public Double calculateOrthogonalDistance(NavigationPosition pointA, NavigationPosition pointB) {
        if (hasCoordinates() && pointA.hasCoordinates() && pointB.hasCoordinates()) {
            Bearing bearingAD = calculateBearing(pointA.getLongitude(), pointA.getLatitude(), getLongitude(), getLatitude());
            Double distanceAtoD = bearingAD.getDistance();
            double courseAtoD = toRadians(bearingAD.getAngle());
            double courseAtoB = toRadians(pointA.calculateAngle(pointB));
            return asin(sin(distanceAtoD / EARTH_RADIUS) *
                    sin(courseAtoD - courseAtoB)) * EARTH_RADIUS;
        }
        return null;
    }

    public Double calculateSpeed(NavigationPosition other) {
        if (hasTime() && other.hasTime()) {
            double interval = abs(getTime().getTimeInMillis() - other.getTime().getTimeInMillis()) / 1000.0;
            Double distance = calculateDistance(other);
            if (!isEmpty(distance) && interval > 0.0)
                return distance / interval * 3.6;
        }
        return null;
    }

    @SuppressWarnings("UnusedDeclaration")
    public Wgs84Position asColumbusGpsStandardPosition() {
        return asWgs84Position();
    }

    @SuppressWarnings("UnusedDeclaration")
    public Wgs84Position asColumbusGpsProfessionalPosition() {
        return asWgs84Position();
    }

    @SuppressWarnings("UnusedDeclaration")
    public Wgs84Position asColumbusGpsTypePosition() {
        return asWgs84Position();
    }

    @SuppressWarnings("UnusedDeclaration")
    public Wgs84Position asCoPilotPosition() {
        return asWgs84Position();
    }

    @SuppressWarnings("UnusedDeclaration")
    public ExcelPosition asExcelPosition() {
        return new ExcelPosition(getLongitude(), getLatitude(), getElevation(), getSpeed(), getTime(), getDescription());
    }

    @SuppressWarnings("UnusedDeclaration")
    public GpxPosition asGarbleGpxPosition() {
        return asGpxPosition();
    }

    @SuppressWarnings("UnusedDeclaration")
    public KmlPosition asGarbleKmlPosition() {
        return asKmlPosition();
    }

    @SuppressWarnings("UnusedDeclaration")
    public KmlPosition asGarbleKmlLittleEndianPosition() {
        return asKmlPosition();
    }

    @SuppressWarnings("UnusedDeclaration")
    public KmlPosition asGarbleKmlBetaPosition() {
        return asKmlPosition();
    }

    @SuppressWarnings("UnusedDeclaration")
    public KmlPosition asGarbleKmzPosition() {
        return asKmzPosition();
    }

    @SuppressWarnings("UnusedDeclaration")
    public KmlPosition asGarbleKmzLittleEndianPosition() {
        return asKmzPosition();
    }

    public GarminFlightPlanPosition asGarminFlightPlanPosition() {
        return new GarminFlightPlanPosition(getLongitude(), getLatitude(), getElevation(), getDescription());
    }

    public GkPosition asGkPosition() {
        return new GkPosition(getLongitude(), getLatitude(), getElevation(), getSpeed(), getTime(), getDescription());
    }

    @SuppressWarnings("UnusedDeclaration")
    public Wgs84Position asGlopusPosition() {
        return asWgs84Position();
    }

    @SuppressWarnings("UnusedDeclaration")
    public Wgs84Position asGoogleMapsUrlPosition() {
        return asWgs84Position();
    }

    public GoPalPosition asGoPalRoutePosition() {
        return new GoPalPosition(getLongitude(), getLatitude(), getElevation(), getSpeed(), getTime(), getDescription());
    }

    @SuppressWarnings("UnusedDeclaration")
    public Wgs84Position asGoPalTrackPosition() {
        return asWgs84Position();
    }

    @SuppressWarnings("UnusedDeclaration")
    public Wgs84Position asGoRiderGpsPosition() {
        return asWgs84Position();
    }

    @SuppressWarnings("UnusedDeclaration")
    public Wgs84Position asGpsTunerPosition() {
        return asWgs84Position();
    }

    public GpxPosition asGpxPosition() {
        return new GpxPosition(getLongitude(), getLatitude(), getElevation(), getSpeed(), getTime(), getDescription());
    }

    @SuppressWarnings("UnusedDeclaration")
    public KmlPosition asIgoRoutePosition() {
        return asKmlPosition();
    }

    @SuppressWarnings("UnusedDeclaration")
    public Wgs84Position asHaicomLoggerPosition() {
        return asWgs84Position();
    }

    @SuppressWarnings("UnusedDeclaration")
    public Wgs84Position asPhotoPosition() {
        return asWgs84Position();
    }

    @SuppressWarnings("UnusedDeclaration")
    public Wgs84Position asIbluePosition() {
        return asWgs84Position();
    }

    @SuppressWarnings("UnusedDeclaration")
    public Wgs84Position asKlickTelRoutePosition() {
        return asWgs84Position();
    }

    public KmlPosition asKmlPosition() {
        return new KmlPosition(getLongitude(), getLatitude(), getElevation(), getSpeed(), getTime(), getDescription());
    }

    @SuppressWarnings("UnusedDeclaration")
    public KmlPosition asKmlBetaPosition() {
        return asKmlPosition();
    }

    @SuppressWarnings({"UnusedDeclaration", "WeakerAccess"})
    public KmlPosition asKmzPosition() {
        return asKmlPosition();
    }

    @SuppressWarnings("UnusedDeclaration")
    public KmlPosition asKmzBetaPosition() {
        return asKmzPosition();
    }

    @SuppressWarnings("UnusedDeclaration")
    public Wgs84Position asKompassPosition() {
        return asWgs84Position();
    }

    @SuppressWarnings("UnusedDeclaration")
    public NmeaPosition asMagellanExploristPosition() {
        return asNmeaPosition();
    }

    @SuppressWarnings("UnusedDeclaration")
    public NmeaPosition asMagellanRoutePosition() {
        return asNmeaPosition();
    }

    @SuppressWarnings("UnusedDeclaration")
    public Wgs84Position asMagicMapsIktPosition() {
        return asWgs84Position();
    }

    @SuppressWarnings("UnusedDeclaration")
    public Wgs84Position asMagicMapsGoPosition() {
        return asWgs84Position();
    }

    @SuppressWarnings("UnusedDeclaration")
    public GkPosition asMagicMapsPthPosition() {
        return new GkPosition(getLongitude(), getLatitude(), getElevation(), getSpeed(), getTime(), getDescription());
    }

    @SuppressWarnings("UnusedDeclaration")
    public Wgs84Position asMotoPlanerUrlPosition() {
        return asWgs84Position();
    }

    public BcrPosition asMTPPosition() {
        return new BcrPosition(getLongitude(), getLatitude(), getElevation(), getSpeed(), getTime(), getDescription());
    }

    @SuppressWarnings("UnusedDeclaration")
    public Wgs84Position asNavigatingPoiWarnerPosition() {
        return asWgs84Position();
    }

    @SuppressWarnings("UnusedDeclaration")
    public Wgs84Position asNavigonCruiserPosition() {
        return asWgs84Position();
    }

    public NmeaPosition asNmeaPosition() {
        return new NmeaPosition(getLongitude(), getLatitude(), getElevation(), getSpeed(), getTime(), getDescription());
    }

    @SuppressWarnings("UnusedDeclaration")
    public NmnPosition asNmnFavoritesPosition() {
        return asNmnPosition();
    }

    public NmnPosition asNmnPosition() {
        return new NmnPosition(getLongitude(), getLatitude(), getElevation(), getSpeed(), getTime(), getDescription());
    }

    @SuppressWarnings("UnusedDeclaration")
    public Wgs84Position asNmnUrlPosition() {
        return asWgs84Position();
    }

    @SuppressWarnings("UnusedDeclaration")
    public Wgs84Position asNokiaLandmarkExchangePosition() {
        return asWgs84Position();
    }

    @SuppressWarnings("UnusedDeclaration")
    public Wgs84Position asOpelNaviPosition() {
        return asWgs84Position();
    }

    @SuppressWarnings({"UnusedDeclaration", "WeakerAccess"})
    public Wgs84Position asOvlPosition() {
        return asWgs84Position();
    }

    @SuppressWarnings("UnusedDeclaration")
    public Wgs84Position asQstarzQPosition() {
        return asWgs84Position();
    }

    @SuppressWarnings("UnusedDeclaration")
    public Wgs84Position asRoutePosition() {
        return asWgs84Position();
    }

    @SuppressWarnings("UnusedDeclaration")
    public Wgs84Position asSygicUnicodePosition() {
        return asWgs84Position();
    }

    @SuppressWarnings("UnusedDeclaration")
    public GpxPosition asTcxPosition() {
        return asGpxPosition();
    }

    public TomTomPosition asTomTomRoutePosition() {
        return new TomTomPosition(getLongitude(), getLatitude(), getElevation(), getSpeed(), getTime(), getDescription());
    }

    public TourPosition asTourPosition() {
        return new TourPosition(getLongitude(), getLatitude(), getElevation(), getSpeed(), getTime(), getDescription());
    }

    public Wgs84Position asWgs84Position() {
        return new Wgs84Position(getLongitude(), getLatitude(), getElevation(), getSpeed(), getTime(), getDescription());
    }

    @SuppressWarnings("UnusedDeclaration")
    public Wgs84Position asWintecWbtTkPosition() {
        return asWgs84Position();
    }

    @SuppressWarnings("UnusedDeclaration")
    public Wgs84Position asWintecWbtTesPosition() {
        return asWgs84Position();
    }

    public String toString() {
        return super.toString() + "[longitude=" + getLongitude() + ", latitude=" + getLatitude() + ", description=" + getDescription() + "]";
    }
}
