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

import com.garmin.fit.*;
import slash.common.type.CompactCalendar;
import slash.navigation.base.RouteCharacteristics;
import slash.navigation.base.Wgs84Position;

import java.util.ArrayList;
import java.util.List;

import static com.garmin.fit.File.ACTIVITY;
import static com.garmin.fit.File.COURSE;
import static slash.common.io.Transfer.trim;
import static slash.common.type.CompactCalendar.fromDate;
import static slash.navigation.base.RouteCharacteristics.*;
import static slash.navigation.common.NavigationConversion.semiCircleToDegree;
import static slash.navigation.common.UnitConversion.msToKmh;

/**
 * Parses some {@link Mesg} messages and transforms them to {@link Wgs84Position}s.
 *
 * @author Christian Pesch
 */
class MesgParser implements CourseMesgListener, CoursePointMesgListener, FileIdMesgListener, GpsMetadataMesgListener, RecordMesgListener, SegmentPointMesgListener {
    private final List<Wgs84Position> positions = new ArrayList<>();
    private int index = 1;
    private String name = null;
    private RouteCharacteristics characteristics = Waypoints;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public RouteCharacteristics getCharacteristics() {
        return characteristics;
    }

    public List<Wgs84Position> getPositions() {
        return positions;
    }

    private Double asDouble(Byte aByte) {
        return aByte != null ? aByte.doubleValue() : null;
    }

    private Double asDouble(Float aFloat) {
        return aFloat != null ? aFloat.doubleValue() : null;
    }

    private Double asDouble(Long aLong) {
        return aLong != null ? aLong.doubleValue() : null;
    }

    private Double asDouble(Short aShort) {
        return aShort != null ? aShort.doubleValue() : null;
    }

    private CompactCalendar asCalendar(DateTime dateTime) {
        return dateTime != null ? fromDate(dateTime.getDate()) : null;
    }

    private String asDescription(Mesg mesg) {
        return String.format("%s %d", mesg.getName(), index++);
    }

    public void onMesg(CourseMesg mesg) {
        String name = trim(mesg.getName());
        if(name != null)
            setName(name);
    }

    public void onMesg(CoursePointMesg mesg) {
        Wgs84Position position = new Wgs84Position(semiCircleToDegree(mesg.getPositionLong()), semiCircleToDegree(mesg.getPositionLat()),
                null, null, asCalendar(mesg.getTimestamp()), asDescription(mesg));
        position.setOrigin(mesg);
        positions.add(position);

        characteristics = Route;
    }

    public void onMesg(FileIdMesg mesg) {
        if(mesg.getType().equals(COURSE))
            characteristics = Route;
        if(mesg.getType().equals(ACTIVITY))
            characteristics = Track;
    }

    public void onMesg(GpsMetadataMesg mesg) {
        CompactCalendar time = asCalendar(mesg.getUtcTimestamp() != null ? mesg.getUtcTimestamp() : mesg.getTimestamp());

        Wgs84Position position = new Wgs84Position(semiCircleToDegree(mesg.getPositionLong()), semiCircleToDegree(mesg.getPositionLat()),
                asDouble(mesg.getEnhancedAltitude()), msToKmh(asDouble(mesg.getEnhancedSpeed())), time, asDescription(mesg));
        position.setHeading(asDouble(mesg.getHeading()));
        position.setOrigin(mesg);
        positions.add(position);
    }

    public void onMesg(RecordMesg mesg) {
        Float elevation = mesg.getEnhancedAltitude() != null ? mesg.getEnhancedAltitude() : mesg.getAltitude();
        Float speed = mesg.getEnhancedSpeed() != null ? mesg.getEnhancedSpeed() : mesg.getSpeed();

        Wgs84Position position = new Wgs84Position(semiCircleToDegree(mesg.getPositionLong()), semiCircleToDegree(mesg.getPositionLat()),
                asDouble(elevation), msToKmh(asDouble(speed)), asCalendar(mesg.getTimestamp()), asDescription(mesg));
        position.setPressure(asDouble(mesg.getAbsolutePressure()));
        position.setTemperature(asDouble(mesg.getTemperature()));
        position.setHeartBeat(mesg.getHeartRate());
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
