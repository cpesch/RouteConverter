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

package slash.navigation.itn;

import slash.navigation.BaseNavigationPosition;
import slash.navigation.util.Conversion;

import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.regex.Matcher;

/**
 * Represents a position in a Tom Tom Rider (.itn) file.
 *
 * @author Christian Pesch
 */

public class ItnPosition extends BaseNavigationPosition {
    static final double INTEGER_FACTOR = 100000.0;

    private Integer longitude, latitude;
    private String city, reason;

    public ItnPosition(Integer longitude, Integer latitude, String comment) {
        super(null, null);
        this.longitude = longitude;
        this.latitude = latitude;
        setComment(comment);
    }

    public ItnPosition(Double longitude, Double latitude, Double elevation, Calendar time, String comment) {
        super(elevation, time);
        setLongitude(longitude);
        setLatitude(latitude);
        setComment(comment);
        // TODO check this out
        // there could be an elevation/time already parsed from comment or one given as a parameter
        if (getElevation() == null || elevation != null)
            setElevation(elevation);
        if (getTime() == null || time != null)
            setTime(time);
    }


    private static Integer asInt(Double aDouble) {
        return aDouble != null ? (int) (aDouble * INTEGER_FACTOR) : null;
    }

    private static Double asDouble(Integer anInteger) {
        return anInteger != null ? anInteger / INTEGER_FACTOR : null;
    }


    public Double getLongitude() {
        return asDouble(getLongitudeAsInt());
    }

    public void setLongitude(Double longitude) {
        this.longitude = asInt(longitude);
    }

    public Double getLatitude() {
        return asDouble(getLatitudeAsInt());
    }

    public void setLatitude(Double latitude) {
        this.latitude = asInt(latitude);
    }

    public String getComment() {
        return city;
    }

    public void setComment(String comment) {
        this.city = comment;
        this.reason = null;
        if (comment == null)
            return;

        Matcher matcher = TomTomRouteFormat.TRIPMASTER_1dot4_PATTERN.matcher(comment);
        if (matcher.matches()) {
            reason = Conversion.trim(matcher.group(1));
            setTime(parseTripmaster1dot4Time(matcher.group(2)));
            elevation = Conversion.parseDouble(matcher.group(3));
            city = Conversion.trim(matcher.group(4));
        }

        matcher = TomTomRouteFormat.TRIPMASTER_SHORT_STARTEND_PATTERN.matcher(comment);
        if (matcher.matches()) {
            String dateStr = Conversion.trim(matcher.group(4));
            city = Conversion.trim(matcher.group(3));
            if (city == null) {
                city = dateStr;
                dateStr = null;
            }
            String timeStr = Conversion.trim(matcher.group(5));
            setTime(parseTripmaster1dot8Date(dateStr + " " + timeStr));
            if (getTime() == null)
                setTime(parseTripmaster1dot4Time(timeStr));
            reason = Conversion.trim(matcher.group(1)) + " : " + (dateStr != null ? dateStr + " - " : "") + timeStr;
            elevation = Conversion.parseDouble(matcher.group(6));
        }

        matcher = TomTomRouteFormat.TRIPMASTER_SHORT_WAYPOINT_PATTERN.matcher(comment);
        if (matcher.matches()) {
            setTime(parseTripmaster1dot4Time(matcher.group(1)));
            reason = "Waypoint";
            elevation = Conversion.parseDouble(matcher.group(2));
            city = null;
        }

        matcher = TomTomRouteFormat.TRIPMASTER_MIDDLE_WAYPOINT_PATTERN.matcher(comment);
        if (matcher.matches()) {
            reason = Conversion.trim(matcher.group(2));
            city = Conversion.trim(matcher.group(3));
            setTime(parseTripmaster1dot4Time(Conversion.trim(matcher.group(1))));
            elevation = Conversion.parseDouble(matcher.group(4));
        }

        matcher = TomTomRouteFormat.TRIPMASTER_LONG_PATTERN.matcher(comment);
        if (matcher.matches()) {
            reason = Conversion.trim(matcher.group(2));
            if (reason != null && reason.endsWith(" :"))
                reason = reason.substring(0, reason.length() - 2);
            setTime(parseTripmaster1dot8Date(matcher.group(4)));
            if (getTime() == null)
                setTime(parseTripmaster1dot4Time(matcher.group(1)));
            city = Conversion.trim(matcher.group(6));
            elevation = Conversion.parseDouble(matcher.group(7));
        }

        matcher = TomTomRouteFormat.LOGPOS_PATTERN.matcher(comment);
        if (matcher.matches()) {
            setTime(parsePilogDate(matcher.group(1)));
            city = Conversion.trim(matcher.group(3));
            reason = Conversion.trim(matcher.group(4));
        }

        matcher = TomTomRouteFormat.PILOG_PATTERN.matcher(comment);
        if (matcher.matches()) {
            setTime(parsePilogDate(matcher.group(1)));
            city = Conversion.trim(matcher.group(3));
            elevation = Conversion.parseDouble(matcher.group(4));
            reason = Conversion.trim(matcher.group(5));
        }
    }


    public Integer getLongitudeAsInt() {
        return longitude;
    }

    public Integer getLatitudeAsInt() {
        return latitude;
    }


    private Calendar parseTripmaster1dot4Time(String string) {
        if (string == null)
            return null;
        try {
            Date date = TomTomRouteFormat.TRIPMASTER_TIME.parse(string);
            Calendar time = Calendar.getInstance();
            time.setTime(date);
            return time;
        } catch (ParseException e) {
            return null;
        }
    }

    private Calendar parseTripmaster1dot8Date(String string) {
        if (string == null)
            return null;
        try {
            Date date = TomTomRouteFormat.TRIPMASTER_DATE.parse(string);
            Calendar time = Calendar.getInstance();
            time.setTime(date);
            return time;
        } catch (ParseException e) {
            return null;
        }
    }

    private Calendar parsePilogDate(String string) {
        if (string == null)
            return null;
        try {
            Date date = TomTomRouteFormat.PILOG_DATE.parse(string);
            Calendar time = Calendar.getInstance();
            time.setTime(date);
            return time;
        } catch (ParseException e) {
            return null;
        }
    }

    public String getCity() {
        return city;
    }

    public String getReason() {
        return reason;
    }

    public ItnPosition asItnPosition() {
        return this;
    }


    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ItnPosition that = (ItnPosition) o;

        return !(city != null ? !city.equals(that.city) : that.city != null) &&
                !(elevation != null ? !elevation.equals(that.elevation) : that.elevation != null) &&
                !(latitude != null ? !latitude.equals(that.latitude) : that.latitude != null) &&
                !(longitude != null ? !longitude.equals(that.longitude) : that.longitude != null) &&
                !(reason != null ? !reason.equals(that.reason) : that.reason != null) &&
                !(time != null ? !time.equals(that.time) : that.time != null);
    }

    public int hashCode() {
        int result;
        result = (longitude != null ? longitude.hashCode() : 0);
        result = 31 * result + (latitude != null ? latitude.hashCode() : 0);
        result = 31 * result + (elevation != null ? elevation.hashCode() : 0);
        result = 31 * result + (city != null ? city.hashCode() : 0);
        result = 31 * result + (reason != null ? reason.hashCode() : 0);
        result = 31 * result + (time != null ? time.hashCode() : 0);
        return result;
    }
}
