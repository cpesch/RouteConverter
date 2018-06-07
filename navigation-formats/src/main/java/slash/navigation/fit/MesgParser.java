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

package slash.navigation.fit;

import com.garmin.fit.CoursePointMesg;
import com.garmin.fit.CoursePointMesgListener;
import com.garmin.fit.DateTime;
import com.garmin.fit.GpsMetadataMesg;
import com.garmin.fit.GpsMetadataMesgListener;
import com.garmin.fit.Mesg;
import com.garmin.fit.RecordMesg;
import com.garmin.fit.RecordMesgListener;
import com.garmin.fit.SegmentPointMesg;
import com.garmin.fit.SegmentPointMesgListener;
import slash.common.type.CompactCalendar;
import slash.navigation.base.RouteCharacteristics;
import slash.navigation.base.Wgs84Position;

import java.util.ArrayList;
import java.util.List;

import static slash.common.type.CompactCalendar.fromDate;
import static slash.navigation.base.RouteCharacteristics.Route;
import static slash.navigation.base.RouteCharacteristics.Track;
import static slash.navigation.base.RouteCharacteristics.Waypoints;
import static slash.navigation.common.NavigationConversion.semiCircleToDegree;

/**
 * Parses some {@link Mesg} messages and transforms theim to {@link Wgs84Position}s.
 *
 * @author Christian Pesch
 */
class MesgParser implements CoursePointMesgListener, GpsMetadataMesgListener, RecordMesgListener, SegmentPointMesgListener {
    private List<Wgs84Position> positions = new ArrayList<>();
    private int index = 1;
    private RouteCharacteristics characteristics = Waypoints;

    public List<Wgs84Position> getPositions() {
        return positions;
    }

    public RouteCharacteristics getCharacteristics() {
        return characteristics;
    }

    private Double asDouble(Byte aByte) {
        return aByte != null ? aByte.doubleValue() : null;
    }

    private Double asDouble(Float aFloat) {
        return aFloat != null ? aFloat.doubleValue() : null;
    }

    private Double asDouble(Short aShort) {
        return aShort != null ? aShort.doubleValue() : null;
    }

    private CompactCalendar asCalendar(DateTime dateTime) {
        return fromDate(dateTime.getDate());
    }

    private String asDescription(Mesg mesg) {
        return String.format("%s %d", mesg.getName(), index++);
    }

    public void onMesg(CoursePointMesg mesg) {
        positions.add(new Wgs84Position(semiCircleToDegree(mesg.getPositionLong()), semiCircleToDegree(mesg.getPositionLat()),
                null, null, asCalendar(mesg.getTimestamp()), asDescription(mesg)));
        characteristics = Route;
    }

    public void onMesg(GpsMetadataMesg mesg) {
        Float velocity = mesg.getNumVelocity() > 0 ? mesg.getVelocity(0) : null;
        Wgs84Position position = new Wgs84Position(semiCircleToDegree(mesg.getPositionLong()), semiCircleToDegree(mesg.getPositionLat()),
                null, asDouble(velocity), asCalendar(mesg.getTimestamp()), asDescription(mesg));
        position.setHeading(asDouble(mesg.getHeading()));
        positions.add(position);
    }

    public void onMesg(RecordMesg mesg) {
        Wgs84Position position = new Wgs84Position(semiCircleToDegree(mesg.getPositionLong()), semiCircleToDegree(mesg.getPositionLat()),
                asDouble(mesg.getAltitude()), asDouble(mesg.getSpeed()), asCalendar(mesg.getTimestamp()), asDescription(mesg));
        position.setTemperature(asDouble(mesg.getTemperature()));
        position.setPdop(asDouble(mesg.getGpsAccuracy()));
        position.setOrigin(mesg);
        positions.add(position);
        characteristics = Track;
    }

    public void onMesg(SegmentPointMesg mesg) {
        positions.add(new Wgs84Position(semiCircleToDegree(mesg.getPositionLong()), semiCircleToDegree(mesg.getPositionLat()),
                asDouble(mesg.getAltitude()), null, null, asDescription(mesg)));
    }
}
