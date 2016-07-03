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

package slash.navigation.nmea;

import slash.common.type.CompactCalendar;
import slash.navigation.base.BaseNavigationPosition;
import slash.navigation.base.Wgs84Position;
import slash.navigation.common.Orientation;
import slash.navigation.common.ValueAndOrientation;
import slash.navigation.gpx.GpxPosition;
import slash.navigation.itn.TomTomPosition;

import static slash.navigation.common.UnitConversion.*;

/**
 * Represents a position in a NMEA 0183 Sentences (.nmea) file.
 *
 * @author Christian Pesch
 */

public class NmeaPosition extends BaseNavigationPosition {
    private ValueAndOrientation longitude, latitude;
    private Double heading, hdop, vdop, pdop;
    private String description;
    protected Integer satellites;
    private Double elevation;
    private Double speed;
    private CompactCalendar time;

    public NmeaPosition(Double longitude, String eastOrWest, Double latitude, String northOrSouth, Double elevation, Double speed, Double heading, CompactCalendar time, String description) {
        this(null, null, elevation, speed, time, description);
        this.longitude = longitude != null ? new ValueAndOrientation(longitude, Orientation.fromValue(eastOrWest)) : null;
        this.latitude = latitude != null ? new ValueAndOrientation(latitude, Orientation.fromValue(northOrSouth)) : null;
        this.heading = heading;
    }

    public NmeaPosition(Double longitude, Double latitude, Double elevation, Double speed, CompactCalendar time, String description) {
        setElevation(elevation);
        setSpeed(speed);
        setTime(time);
        setLongitude(longitude);
        setLatitude(latitude);
        this.description = description;
    }

    public Double getLongitude() {
        return nmea2degrees(getLongitudeAsValueAndOrientation());
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude2nmea(longitude);
    }

    public Double getLatitude() {
        return nmea2degrees(getLatitudeAsValueAndOrientation());
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude2nmea(latitude);
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Double getElevation() {
        return elevation;
    }

    public void setElevation(Double elevation) {
        this.elevation = elevation;
    }

    public Double getSpeed() {
        return speed;
    }

    public void setSpeed(Double speed) {
        this.speed = speed;
    }

    public CompactCalendar getTime() {
        return time;
    }

    public void setTime(CompactCalendar time) {
        this.time = time;
    }

    public ValueAndOrientation getLongitudeAsValueAndOrientation() {
        return longitude;
    }

    public void setLongitudeAsValueAndOrientation(ValueAndOrientation longitude) {
        this.longitude = longitude;
    }

    public ValueAndOrientation getLatitudeAsValueAndOrientation() {
        return latitude;
    }

    public void setLatitudeAsValueAndOrientation(ValueAndOrientation latitude) {
        this.latitude = latitude;
    }

    public Double getHeading() {
        return heading;
    }

    public void setHeading(Double heading) {
        this.heading = heading;
    }

   public Double getHdop() {
        return hdop;
    }

    public void setHdop(Double hdop) {
        this.hdop = hdop;
    }

    public Double getVdop() {
        return vdop;
    }

    public void setVdop(Double vdop) {
        this.vdop = vdop;
    }

    public Double getPdop() {
        return pdop;
    }

    public void setPdop(Double pdop) {
        this.pdop = pdop;
    }

    public Integer getSatellites() {
        return satellites;
    }

    public void setSatellites(Integer satellites) {
        this.satellites = satellites;
    }


    public GpxPosition asGpxPosition() {
        GpxPosition position = super.asGpxPosition();
        position.setHeading(getHeading());
        position.setHdop(getHdop());
        position.setPdop(getPdop());
        position.setVdop(getVdop());
        position.setSatellites(getSatellites());
        return position;
    }

    public NmeaPosition asNmeaPosition() {
        return this;
    }

    public TomTomPosition asTomTomRoutePosition() {
        TomTomPosition position = super.asTomTomRoutePosition();
        position.setHeading(getHeading());
        return position;
    }

    public Wgs84Position asWgs84Position() {
        Wgs84Position position = super.asWgs84Position();
        position.setHeading(getHeading());
        position.setHdop(getHdop());
        position.setPdop(getPdop());
        position.setVdop(getVdop());
        position.setSatellites(getSatellites());
        return position;
    }


    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        NmeaPosition that = (NmeaPosition) o;

        return !(description != null ? !description.equals(that.description) : that.description != null) &&
                !(getElevation() != null ? !getElevation().equals(that.getElevation()) : that.getElevation() != null) &&
                !(heading != null ? !heading.equals(that.heading) : that.heading != null) &&
                !(latitude != null ? !latitude.equals(that.latitude) : that.latitude != null) &&
                !(longitude != null ? !longitude.equals(that.longitude) : that.longitude != null) &&
                !(hasTime() ? !getTime().equals(that.getTime()) : that.hasTime()) &&
                !(hdop != null ? !hdop.equals(that.hdop) : that.hdop != null) &&
                !(pdop != null ? !pdop.equals(that.pdop) : that.pdop != null) &&
                !(vdop != null ? !vdop.equals(that.vdop) : that.vdop != null) &&
                !(satellites != null ? !satellites.equals(that.satellites) : that.satellites != null);    }

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
