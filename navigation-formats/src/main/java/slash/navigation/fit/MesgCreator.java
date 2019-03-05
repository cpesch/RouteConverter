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
import slash.navigation.base.Wgs84Route;

import java.util.ArrayList;
import java.util.List;

import static slash.common.io.Transfer.formatFloat;
import static slash.navigation.common.NavigationConversion.degreeToSemiCircle;

/**
 * Creates {@link Mesg} messages from {@link Wgs84Position}s.
 *
 * @author Christian Pesch
 */
class MesgCreator {
    private Byte asByte(Double aDouble) {
        return aDouble != null ? aDouble.byteValue() : null;
    }

    private Short asShort(Double aDouble) {
        return aDouble != null ? aDouble.shortValue() : null;
    }

    private RecordMesg createRecordMesg(Wgs84Position position) {
        RecordMesg mesg = new RecordMesg();
        mesg.setPositionLong(degreeToSemiCircle(position.getLongitude()));
        mesg.setPositionLat(degreeToSemiCircle(position.getLatitude()));
        CompactCalendar time = position.getTime();
        if (time != null)
            mesg.setTimestamp(new DateTime(time.getTime()));
        mesg.setAltitude(formatFloat(position.getElevation()));
        mesg.setSpeed(formatFloat(position.getSpeed()));
        mesg.setTemperature(asByte(position.getTemperature()));
        mesg.setGpsAccuracy(asShort(position.getPdop()));
        return mesg;
    }

    private CoursePointMesg createCoursePointMesg(Wgs84Position position) {
        CoursePointMesg mesg = new CoursePointMesg();
        mesg.setPositionLong(degreeToSemiCircle(position.getLongitude()));
        mesg.setPositionLat(degreeToSemiCircle(position.getLatitude()));
        CompactCalendar time = position.getTime();
        if (time != null)
            mesg.setTimestamp(new DateTime(time.getTime()));
        mesg.setName(position.getDescription());
        return mesg;
    }

    private GpsMetadataMesg createGpsMetadataMesg(Wgs84Position position) {
        GpsMetadataMesg mesg = new GpsMetadataMesg();
        mesg.setPositionLong(degreeToSemiCircle(position.getLongitude()));
        mesg.setPositionLat(degreeToSemiCircle(position.getLatitude()));
        CompactCalendar time = position.getTime();
        if (time != null)
            mesg.setTimestamp(new DateTime(time.getTime()));
        mesg.setHeading(formatFloat(position.getHeading()));
        mesg.setVelocity(0, formatFloat(position.getSpeed()));
        return mesg;
    }

    public List<Mesg> createMesgs(Wgs84Route route, int startIndex, int endIndex) {
        List<Mesg> result = new ArrayList<>();

        RouteCharacteristics characteristics = route.getCharacteristics();
        List<Wgs84Position> positions = route.getPositions();
        for (int i = startIndex; i < endIndex; i++) {
            Wgs84Position position = positions.get(i);

            switch (characteristics) {
                case Track:
                    RecordMesg recordMesg = createRecordMesg(position);
                    result.add(recordMesg);
                    break;
                case Route:
                    CoursePointMesg coursePointMesg = createCoursePointMesg(position);
                    result.add(coursePointMesg);
                    break;
                case Waypoints:
                    GpsMetadataMesg gpsMetadataMesg = createGpsMetadataMesg(position);
                    result.add(gpsMetadataMesg);
                    break;
                default:
                    throw new IllegalArgumentException("RouteCharacteristics " + characteristics + " is not supported");
            }
        }
        return result;
    }
}
