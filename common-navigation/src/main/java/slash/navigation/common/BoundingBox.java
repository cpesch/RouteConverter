/*
 *
 *     This file is part of RouteConverter.
 *
 *     RouteConverter is free software; you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation; either version 2 of the License, or
 *     (at your option) any later version.
 *
 *     RouteConverter is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with RouteConverter; if not, write to the Free Software
 *     Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 *
 *     Copyright (C) 2007 Christian Pesch. All Rights Reserved.
 * /
 */
package slash.navigation.common;

import slash.common.type.CompactCalendar;

import java.util.List;

import static slash.common.type.CompactCalendar.fromMillis;

/**
 * An area defined by the north east and south west {@link NavigationPosition}s.
 *
 * @author Christian Pesch
 */
public class BoundingBox {
    private final NavigationPosition northEast, southWest;

    public BoundingBox(NavigationPosition northEast, NavigationPosition southWest) {
        this.northEast = northEast;
        this.southWest = southWest;
    }

    public BoundingBox(Double longitudeNorthEast, Double latitudeNorthEast,
                       Double longitudeSouthWest, Double latitudeSouthWest) {
        this(new SimpleNavigationPosition(longitudeNorthEast, latitudeNorthEast),
                new SimpleNavigationPosition(longitudeSouthWest, latitudeSouthWest));
    }

    public BoundingBox(List<? extends NavigationPosition> positions) {
        double maximumLongitude = -180.0, maximumLatitude = -90.0,
                minimumLongitude = 180.0, minimumLatitude = 90.0;
        CompactCalendar maximumTime = null, minimumTime = null;

        for (NavigationPosition position : positions) {
            Double longitude = position.getLongitude();
            if (longitude == null)
                continue;
            if (longitude > maximumLongitude)
                maximumLongitude = longitude;
            if (longitude < minimumLongitude)
                minimumLongitude = longitude;
            Double latitude = position.getLatitude();
            if (latitude == null)
                continue;
            if (latitude > maximumLatitude)
                maximumLatitude = latitude;
            if (latitude < minimumLatitude)
                minimumLatitude = latitude;
            CompactCalendar time = position.getTime();
            if (time == null)
                continue;
            if (maximumTime == null || time.after(maximumTime))
                maximumTime = time;
            if (minimumTime == null || time.before(minimumTime))
                minimumTime = time;
        }
        this.northEast = new SimpleNavigationPosition(maximumLongitude, maximumLatitude, maximumTime);
        this.southWest = new SimpleNavigationPosition(minimumLongitude, minimumLatitude, minimumTime);
    }

    public NavigationPosition getNorthEast() {
        return northEast;
    }

    public NavigationPosition getSouthWest() {
        return southWest;
    }

    public NavigationPosition getSouthEast() {
        return new SimpleNavigationPosition(northEast.getLongitude(), southWest.getLatitude());
    }

    public NavigationPosition getNorthWest() {
        return new SimpleNavigationPosition(southWest.getLongitude(), northEast.getLatitude());
    }

    public double getSquareSize() {
        return (getSouthWest().getLongitude() - getNorthEast().getLongitude()) *
                (getSouthWest().getLatitude() - getNorthEast().getLatitude());
    }

    public boolean contains(NavigationPosition position) {
        boolean result = position.getLongitude() >= southWest.getLongitude();
        result = result && (position.getLongitude() <= northEast.getLongitude());
        result = result && (position.getLatitude() >= southWest.getLatitude());
        result = result && (position.getLatitude() <= northEast.getLatitude());
        return result;
    }

    public boolean contains(BoundingBox boundingBox) {
        return contains(boundingBox.getNorthEast()) && contains(boundingBox.getSouthEast()) &&
                contains(boundingBox.getSouthWest()) && contains(boundingBox.getNorthWest());
    }

    public NavigationPosition getCenter() {
        double longitude = southWest.getLongitude() + northEast.getLongitude();
        if(longitude != 0.0)
            longitude /=2;
        double latitude = southWest.getLatitude() + northEast.getLatitude();
        if (latitude != 0.0)
            latitude /=2;
        CompactCalendar time = null;
        if (northEast.hasTime() && southWest.hasTime()) {
            long millis = northEast.getTime().getTimeInMillis() +
                    (southWest.getTime().getTimeInMillis() - northEast.getTime().getTimeInMillis()) / 2;
            time = fromMillis(millis);
        }
        return new SimpleNavigationPosition(longitude, latitude, time);
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BoundingBox that = (BoundingBox) o;

        return !(northEast != null ? !northEast.equals(that.northEast) : that.northEast != null) &&
                !(southWest != null ? !southWest.equals(that.southWest) : that.southWest != null);
    }

    public int hashCode() {
        int result = northEast != null ? northEast.hashCode() : 0;
        result = 31 * result + (southWest != null ? southWest.hashCode() : 0);
        return result;
    }

    public String toString() {
        return getClass().getSimpleName() + "[northEast=" + northEast + ", southWest=" + southWest + "]";
    }
}
