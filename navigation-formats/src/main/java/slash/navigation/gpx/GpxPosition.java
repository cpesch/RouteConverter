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

import slash.navigation.Wgs84Position;
import slash.common.io.CompactCalendar;
import slash.navigation.util.RouteComments;
import slash.common.io.Transfer;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.regex.Matcher;

/**
 * Represents a position in a GPS Exchange Format (.gpx) file.
 *
 * @author Christian Pesch
 */

public class GpxPosition extends Wgs84Position {
    private String reason;
    private Object origin;

    public GpxPosition(Double longitude, Double latitude, Double elevation, Double speed, CompactCalendar time, String comment) {
        super(longitude, latitude, elevation, speed, time, comment);
    }

    public GpxPosition(BigDecimal longitude, BigDecimal latitude, BigDecimal elevation, Double speed,
                       Double heading, CompactCalendar time, String comment, BigDecimal hdop, BigDecimal pdop,
                       BigDecimal vdop, BigInteger satellites, Object origin) {
        this(Transfer.formatDouble(longitude), Transfer.formatDouble(latitude),
                Transfer.formatDouble(elevation), speed, time, comment);
        this.origin = origin;
        // avoid overwriting values determined by setComment() with a null value
        if (heading != null)
            setHeading(heading);
        setHdop(Transfer.formatDouble(hdop));
        setPdop(Transfer.formatDouble(pdop));
        setVdop(Transfer.formatDouble(vdop));
        setSatellites(Transfer.formatInt(satellites));
    }

    public void setComment(String comment) {
        this.comment = comment;
        this.reason = null;
        if (comment == null)
            return;

        RouteComments.parseComment(this, comment);

        // TODO move this logic up
        Matcher matcher = GpxFormat.TRIPMASTER_REASON_PATTERN.matcher(this.comment);
        if (matcher.matches()) {
            this.reason = Transfer.trim(matcher.group(1));
            this.comment = Transfer.trim(matcher.group(3));

            Double heading = RouteComments.parseTripmasterHeading(reason);
            if(heading != null)
                this.heading = heading;
        } /* TODO think about how to solve this with that much errors
          else {
            matcher = GpxFormat.TRIPMASTER_DESCRIPTION_PATTERN.matcher(comment);
            if (matcher.matches()) {
                this.comment = Conversion.trim(matcher.group(1));
                this.reason = Conversion.trim(matcher.group(2));
            }
        } */
    }

    public String getCity() {
        return comment;
    }

    public String getReason() {
        return reason;
    }

    Object getOrigin() {
        return origin;
    }

    <T> T getOrigin(Class<T> resultClass) {
        if (resultClass.isInstance(origin))
            //noinspection unchecked
            return (T) origin;
        else
            return null;
    }


    public GpxPosition asGpxPosition() {
        return this;
    }


    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        GpxPosition that = (GpxPosition) o;

        return !(comment != null ? !comment.equals(that.comment) : that.comment != null) &&
                !(elevation != null ? !elevation.equals(that.elevation) : that.elevation != null) &&
                !(heading != null ? !heading.equals(that.heading) : that.heading != null) &&
                !(latitude != null ? !latitude.equals(that.latitude) : that.latitude != null) &&
                !(longitude != null ? !longitude.equals(that.longitude) : that.longitude != null) &&
                !(time != null ? !time.equals(that.time) : that.time != null) &&
                !(hdop != null ? !hdop.equals(that.hdop) : that.hdop != null) &&
                !(pdop != null ? !pdop.equals(that.pdop) : that.pdop != null) &&
                !(vdop != null ? !vdop.equals(that.vdop) : that.vdop != null) &&
                !(satellites != null ? !satellites.equals(that.satellites) : that.satellites != null);
    }

    public int hashCode() {
        int result;
        result = (longitude != null ? longitude.hashCode() : 0);
        result = 31 * result + (latitude != null ? latitude.hashCode() : 0);
        result = 31 * result + (elevation != null ? elevation.hashCode() : 0);
        result = 31 * result + (heading != null ? heading.hashCode() : 0);
        result = 31 * result + (comment != null ? comment.hashCode() : 0);
        result = 31 * result + (time != null ? time.hashCode() : 0);
        result = 31 * result + (hdop != null ? hdop.hashCode() : 0);
        result = 31 * result + (pdop != null ? pdop.hashCode() : 0);
        result = 31 * result + (vdop != null ? vdop.hashCode() : 0);
        result = 31 * result + (satellites != null ? satellites.hashCode() : 0);
        return result;
    }
}
