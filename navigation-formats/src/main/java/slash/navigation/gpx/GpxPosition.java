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

package slash.navigation.gpx;

import slash.common.type.CompactCalendar;
import slash.navigation.base.WaypointType;
import slash.navigation.base.Wgs84Position;
import slash.navigation.fpl.CountryCode;
import slash.navigation.fpl.GarminFlightPlanPosition;
import slash.navigation.gpx.binding11.WptType;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.regex.Matcher;

import static slash.common.io.Transfer.formatDouble;
import static slash.common.io.Transfer.formatInt;
import static slash.common.io.Transfer.isEmpty;
import static slash.common.io.Transfer.trim;
import static slash.navigation.base.RouteComments.parseDescription;
import static slash.navigation.base.RouteComments.parseTripmasterHeading;
import static slash.navigation.base.WaypointType.UserWaypoint;
import static slash.navigation.gpx.GpxFormat.TRIPMASTER_REASON_PATTERN;
import static slash.navigation.gpx.GpxFormat.parseHeading;
import static slash.navigation.gpx.GpxFormat.parseSpeed;

/**
 * Represents a position in a GPS Exchange Format (.gpx) file.
 *
 * @author Christian Pesch
 */

public class GpxPosition extends Wgs84Position {
    private String reason;
    private GpxPositionExtension positionExtension;

    public GpxPosition(Double longitude, Double latitude, Double elevation, Double speed, CompactCalendar time, String description) {
        this(longitude, latitude, elevation, speed, time, description, null);
    }

    public GpxPosition(Double longitude, Double latitude, Double elevation, Double speed, CompactCalendar time, String description, Object origin) {
        super(longitude, latitude, elevation, speed, time, description, origin);
    }

    // GPX 1.0
    public GpxPosition(BigDecimal longitude, BigDecimal latitude, BigDecimal elevation, Double speed, Double heading,
                       CompactCalendar time, String description, BigDecimal hdop, BigDecimal pdop, BigDecimal vdop,
                       BigInteger satellites, Object origin) {
        this(formatDouble(longitude), formatDouble(latitude), formatDouble(elevation), speed, time, description, origin);
        if (heading != null)
            this.heading = heading;
        setHdop(formatDouble(hdop));
        setPdop(formatDouble(pdop));
        setVdop(formatDouble(vdop));
        setSatellites(formatInt(satellites));
    }

    // GPX 1.1
    public GpxPosition(BigDecimal longitude, BigDecimal latitude, BigDecimal elevation, GpxPositionExtension positionExtension,
                       CompactCalendar time, String description, BigDecimal hdop, BigDecimal pdop, BigDecimal vdop,
                       BigInteger satellites, Object origin) {
        this(formatDouble(longitude), formatDouble(latitude), formatDouble(elevation), null, time, description, origin);
        this.positionExtension = positionExtension;
        setHdop(formatDouble(hdop));
        setPdop(formatDouble(pdop));
        setVdop(formatDouble(vdop));
        setSatellites(formatInt(satellites));
    }

    public void setDescription(String description) {
        this.description = description;
        this.reason = null;
        if (description == null)
            return;

        parseDescription(this, description);

        // TODO move this logic up
        Matcher matcher = TRIPMASTER_REASON_PATTERN.matcher(this.description);
        if (matcher.matches()) {
            this.reason = trim(matcher.group(1));
            this.description = trim(matcher.group(3));

            Double heading = parseTripmasterHeading(reason);
            if (heading != null)
                this.heading = heading;
        } /* TODO think about how to solve this with that much errors
          else {
            matcher = GpxFormat.TRIPMASTER_DESCRIPTION_PATTERN.matcher(description);
            if (matcher.matches()) {
                this.description = trim(matcher.group(1));
                this.reason = trim(matcher.group(2));
            }
        } */

        // TODO is this the correct place?
        if(isEmpty(getHeading()))
            setHeading(parseHeading(description));
        if(isEmpty(getSpeed()))
            setSpeed(parseSpeed(description));
        /*
        result = parseSpeed(wptType.getCmt());
        if (result == null)
            result = parseSpeed(wptType.getName());
        if (result == null)
            result = parseSpeed(wptType.getDesc());
        */
    }

    public String getCity() {
        return description;
    }

    public String getReason() {
        return reason;
    }


    GpxPositionExtension getPositionExtension() {
        return positionExtension;
    }

    void setPositionExtension(GpxPositionExtension positionExtension) {
        this.positionExtension = positionExtension;
    }

    public Double getHeading() {
        return getPositionExtension() != null ? getPositionExtension().getHeading() : super.getHeading();
    }

    public void setHeading(Double heading) {
        if (getPositionExtension() != null)
            getPositionExtension().setHeading(heading);
        else
            super.setHeading(heading);
    }

    public Double getSpeed() {
        return getPositionExtension() != null ? getPositionExtension().getSpeed() : super.getSpeed();
    }

    public void setSpeed(Double speed) {
        if (getPositionExtension() != null)
            getPositionExtension().setSpeed(speed);
        else
            super.setSpeed(speed);
    }

    public Double getTemperature() {
        return getPositionExtension() != null ? getPositionExtension().getTemperature() : super.getTemperature();
    }

    public void setTemperature(Double temperature) {
        if (getPositionExtension() != null)
            getPositionExtension().setTemperature(temperature);
        else
            super.setTemperature(temperature);
    }

    public GarminFlightPlanPosition asGarminFlightPlanPosition() {
        GarminFlightPlanPosition position = new GarminFlightPlanPosition(getLongitude(), getLatitude(), getElevation(), getDescription());
        position.setWaypointType(UserWaypoint);
        WptType wptType = getOrigin(WptType.class);
        if (wptType != null) {
            String type = trim(wptType.getType());
            if (type != null) {
                WaypointType waypointType = WaypointType.fromValue(type);
                position.setWaypointType(waypointType);

                String name = wptType.getName();
                if (name != null && name.length() >= 2)
                    position.setCountryCode(CountryCode.fromValue(name.substring(0, 2)));
            }
            String description = trim(wptType.getCmt());
            if (description != null) {
                position.setDescription(description);
            }
        }
        return position;
    }

    public GpxPosition asGpxPosition() {
        return this;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        GpxPosition that = (GpxPosition) o;

        return !(description != null ? !description.equals(that.description) : that.description != null) &&
                !(getElevation() != null ? !getElevation().equals(that.getElevation()) : that.getElevation() != null) &&
                !(heading != null ? !heading.equals(that.heading) : that.heading != null) &&
                !(temperature != null ? !temperature.equals(that.temperature) : that.temperature != null) &&
                !(latitude != null ? !latitude.equals(that.latitude) : that.latitude != null) &&
                !(longitude != null ? !longitude.equals(that.longitude) : that.longitude != null) &&
                !(hasTime() ? !getTime().equals(that.getTime()) : that.hasTime()) &&
                !(hdop != null ? !hdop.equals(that.hdop) : that.hdop != null) &&
                !(pdop != null ? !pdop.equals(that.pdop) : that.pdop != null) &&
                !(vdop != null ? !vdop.equals(that.vdop) : that.vdop != null) &&
                !(satellites != null ? !satellites.equals(that.satellites) : that.satellites != null);
    }

    public int hashCode() {
        int result;
        result = (longitude != null ? longitude.hashCode() : 0);
        result = 31 * result + (latitude != null ? latitude.hashCode() : 0);
        result = 31 * result + (getElevation() != null ? getElevation().hashCode() : 0);
        result = 31 * result + (heading != null ? heading.hashCode() : 0);
        result = 31 * result + (description != null ? description.hashCode() : 0);
        result = 31 * result + (hasTime() ? getTime().hashCode() : 0);
        result = 31 * result + (hdop != null ? hdop.hashCode() : 0);
        result = 31 * result + (pdop != null ? pdop.hashCode() : 0);
        result = 31 * result + (vdop != null ? vdop.hashCode() : 0);
        result = 31 * result + (satellites != null ? satellites.hashCode() : 0);
        return result;
    }
}
