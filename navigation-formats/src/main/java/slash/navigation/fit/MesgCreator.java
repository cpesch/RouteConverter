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

import static com.garmin.fit.File.ACTIVITY;
import static com.garmin.fit.File.COURSE;
import static slash.common.io.Transfer.formatFloat;
import static slash.common.type.CompactCalendar.now;
import static slash.navigation.common.NavigationConversion.degreeToSemiCircle;
import static slash.navigation.common.UnitConversion.kmhToMs;

/**
 * Creates {@link Mesg} messages from {@link Wgs84Position}s.
 *
 * @author Christian Pesch
 */
class MesgCreator {
    private Byte asByte(Double aDouble) {
        return aDouble != null ? aDouble.byteValue() : null;
    }

    private Long asLong(Double aDouble) {
        return aDouble != null ? aDouble.longValue() : null;
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
        mesg.setSpeed(formatFloat(kmhToMs(position.getSpeed())));
        mesg.setAbsolutePressure(asLong(position.getPressure()));
        mesg.setTemperature(asByte(position.getTemperature()));
        mesg.setHeartRate(position.getHeartBeat());
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
        mesg.setEnhancedSpeed(formatFloat(kmhToMs(position.getSpeed())));
        return mesg;
    }

    private CourseMesg createCourseMesg(Wgs84Route route) {
        CourseMesg mesg = new CourseMesg();
        mesg.setName(route.getName());
        return mesg;
    }

    private FileIdMesg createFileIdMesg(RouteCharacteristics characteristics, String productName) {
        FileIdMesg mesg = new FileIdMesg();
        mesg.setProductName(productName);
        mesg.setTimeCreated(new DateTime(now().getTime()));

        switch (characteristics) {
            case Track:
                mesg.setType(ACTIVITY);
                break;
            case Route:
                mesg.setType(COURSE);
                break;
        }
        return mesg;
    }

    private Mesg createPositionMesg(RouteCharacteristics characteristics, Wgs84Position position) {
        switch (characteristics) {
            case Track:
                return createRecordMesg(position);
            case Route:
                return createCoursePointMesg(position);
            case Waypoints:
                return createGpsMetadataMesg(position);
            default:
                throw new IllegalArgumentException("RouteCharacteristics " + characteristics + " is not supported");
        }
    }

    public List<Mesg> createMesgs(Wgs84Route route, String productName, int startIndex, int endIndex) {
        RouteCharacteristics characteristics = route.getCharacteristics();

        List<Mesg> result = new ArrayList<>();
        result.add(createFileIdMesg(characteristics, productName));
        result.add(createCourseMesg(route));

        List<Wgs84Position> positions = route.getPositions();
        for (int i = startIndex; i < endIndex; i++) {
            Wgs84Position position = positions.get(i);
            Mesg mesg = createPositionMesg(characteristics, position);
            result.add(mesg);
        }
        return result;
    }
}
