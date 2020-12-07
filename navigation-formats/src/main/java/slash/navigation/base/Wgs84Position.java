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
import slash.navigation.csv.CsvPosition;
import slash.navigation.excel.ExcelPosition;
import slash.navigation.gpx.GpxPosition;
import slash.navigation.itn.TomTomPosition;
import slash.navigation.nmea.NmeaPosition;

import static slash.navigation.base.RouteComments.parseDescription;

/**
 * Represents a WGS84 position in a route.
 *
 * @author Christian Pesch
 */

public class Wgs84Position extends BaseNavigationPosition implements ExtendedSensorNavigationPosition {
    protected Double longitude, latitude, heading, pressure, temperature, hdop, vdop, pdop;
    protected String description;
    protected Integer satellites;
    protected WaypointType waypointType;
    private Double elevation, speed;
    private Short heartBeat;
    private CompactCalendar time;
    private Object origin;

    public Wgs84Position(Double longitude, Double latitude, Double elevation, Double speed, CompactCalendar time, String description) {
        this(longitude, latitude, elevation, speed, time, description, null);
    }

    public Wgs84Position(Double longitude, Double latitude, Double elevation, Double speed, CompactCalendar time, String description, Object origin) {
        setElevation(elevation);
        setSpeed(speed);
        setTime(time);
        this.longitude = longitude;
        this.latitude = latitude;
        setDescription(description);
        this.origin = origin;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
        if (description == null)
            return;

        parseDescription(this, description);
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

    public WaypointType getWaypointType() {
        return waypointType;
    }

    public void setWaypointType(WaypointType waypointType) {
        this.waypointType = waypointType;
    }

    public Double getHeading() {
        return heading;
    }

    public void setHeading(Double heading) {
        this.heading = heading;
    }

    public Double getPressure() {
        return pressure;
    }

    public void setPressure(Double pressure) {
        this.pressure = pressure;
    }

    public Double getTemperature() {
        return temperature;
    }

    public void setTemperature(Double temperature) {
        this.temperature = temperature;
    }

    public Short getHeartBeat() {
        return heartBeat;
    }

    public void setHeartBeat(Short heartBeat) {
        this.heartBeat = heartBeat;
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

    public/*for tests*/ Object getOrigin() {
        return origin;
    }

    public <T> T getOrigin(Class<T> resultClass) {
        if (resultClass.isInstance(origin))
            return resultClass.cast(origin);
        else
            return null;
    }

    public/* for ImageFormat */ void setOrigin(Object origin) {
        this.origin = origin;
    }

    public CsvPosition asCsvPosition() {
        CsvPosition position = super.asCsvPosition();
        ExtendedSensorNavigationPosition.transferExtendedSensorData(this, position);
        return position;
    }

    public ExcelPosition asMicrosoftExcelPosition() {
        ExcelPosition position = super.asMicrosoftExcelPosition();
        ExtendedSensorNavigationPosition.transferExtendedSensorData(this, position);
        return position;
    }

    public GpxPosition asGpxPosition() {
        GpxPosition position = super.asGpxPosition();
        ExtendedSensorNavigationPosition.transferExtendedSensorData(this, position);
        position.setHdop(getHdop());
        position.setPdop(getPdop());
        position.setVdop(getVdop());
        position.setSatellites(getSatellites());
        return position;
    }

    public NmeaPosition asNmeaPosition() {
        NmeaPosition position = super.asNmeaPosition();
        position.setHeading(getHeading());
        position.setHdop(getHdop());
        position.setPdop(getPdop());
        position.setVdop(getVdop());
        position.setSatellites(getSatellites());
        return position;
    }

    public TomTomPosition asTomTomRoutePosition() {
        TomTomPosition position = super.asTomTomRoutePosition();
        position.setHeading(getHeading());
        return position;
    }

    public Wgs84Position asWgs84Position() {
        return this;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Wgs84Position that = (Wgs84Position) o;

        return !(description != null ? !description.equals(that.description) : that.description != null) &&
                !(getElevation() != null ? !getElevation().equals(that.getElevation()) : that.getElevation() != null) &&
                !(heading != null ? !heading.equals(that.heading) : that.heading != null) &&
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
