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

package slash.navigation.tcx;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import slash.common.io.CompactCalendar;
import slash.common.io.Transfer;
import slash.navigation.gpx.GpxFormat;
import slash.navigation.gpx.GpxPosition;
import slash.navigation.gpx.GpxRoute;
import slash.navigation.gpx.binding11.WptType;
import slash.navigation.tcx.binding1.*;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

/**
 * Writes Training Center 1 Course (.crs) files.
 *
 * @author Christian Pesch
 */

public class Crs1Format extends GpxFormat {

    public String getName() {
        return "Training Center 1 Course (*" + getExtension() + ")";
    }

    public String getExtension() {
        return ".crs";
    }

    public boolean isSupportsReading() {
        return false;
    }

    public List<GpxRoute> read(InputStream source, CompactCalendar startDate) throws IOException {
        throw new UnsupportedOperationException();
    }

    private Double getHeartBeatRate(WptType wptType) {
        Double heartBeatRate = null;
        if (wptType.getExtensions() != null) {
            for (Object any : wptType.getExtensions().getAny()) {
                if (any instanceof Element) {
                    Element extension = (Element) any;
                    if ("TrackPointExtension".equals(extension.getLocalName())) {
                        for (int i = 0; i < extension.getChildNodes().getLength(); i++) {
                            Node hr = extension.getChildNodes().item(i);
                            if ("hr".equals(hr.getLocalName()))
                                heartBeatRate = Transfer.parseDouble(hr.getTextContent());
                        }
                    }
                }
            }
        }
        return heartBeatRate;
    }

    private Short getHeartBeatRate(GpxPosition position) {
        // conversion is done currently only from Gpx11Format to Crs1Format
        if (position != null) {
            WptType wpt = position.getOrigin(WptType.class);
            if (wpt != null) {
                Double heartBeatRate = getHeartBeatRate(wpt);
                if (heartBeatRate != null)
                    return heartBeatRate.shortValue();
            }
        }
        return null;
    }

    private PositionT createPosition(GpxPosition position) {
        PositionT positionT = new ObjectFactory().createPositionT();
        if (position.getLongitude() != null)
            positionT.setLongitudeDegrees(position.getLongitude());
        if (position.getLatitude() != null)
            positionT.setLatitudeDegrees(position.getLatitude());
        return positionT;
    }

    private CourseLapT createCourseLap(GpxRoute route, int startIndex, int endIndex) {
        CourseLapT courseLapT = new ObjectFactory().createCourseLapT();
        GpxPosition first = route.getPositionCount() >= startIndex ? route.getPosition(startIndex) : null;
        GpxPosition last = route.getPositionCount() >= endIndex ? route.getPosition(endIndex - 1) : null;
        if (last == null)
            last = first;

        double distanceMeters = 0.0;
        long totalTimeMilliSeconds = 0;
        List<GpxPosition> positions = route.getPositions();
        GpxPosition previous = null;
        for (int i = startIndex; i < endIndex; i++) {
            GpxPosition position = positions.get(i);
            if (previous != null) {
                Double distance = previous.calculateDistance(position);
                if (distance != null)
                    distanceMeters += distance;
                Long time = previous.calculateTime(position);
                if (time != null)
                    totalTimeMilliSeconds += time;
            }
            previous = position;
        }
        courseLapT.setAverageHeartRateBpm(getHeartBeatRate(first));
        courseLapT.setDistanceMeters(distanceMeters);
        courseLapT.setIntensity(IntensityT.fromValue("Active"));
        courseLapT.setTotalTimeSeconds(totalTimeMilliSeconds / 1000);

        if (first != null) {
            courseLapT.setBeginPosition(createPosition(first));
            if (first.getElevation() != null)
                courseLapT.setBeginAltitudeMeters(first.getElevation());
        }
        if (last != null) {
            courseLapT.setEndPosition(createPosition(last));
            if (last.getElevation() != null)
                courseLapT.setEndAltitudeMeters(last.getElevation());
        }
        return courseLapT;
    }

    private TrackT createTrack(GpxRoute route, int startIndex, int endIndex) {
        ObjectFactory objectFactory = new ObjectFactory();
        TrackT trackT = objectFactory.createTrackT();
        List<TrackpointT> trackpoints = trackT.getTrackpoint();

        List<GpxPosition> positions = route.getPositions();
        GpxPosition first = null;
        CompactCalendar lastTime = CompactCalendar.getInstance();
        for (int i = startIndex; i < endIndex; i++) {
            GpxPosition position = positions.get(i);
            TrackpointT trackpointT = objectFactory.createTrackpointT();
            trackpointT.setAltitudeMeters(position.getElevation());
            trackpointT.setHeartRateBpm(getHeartBeatRate(position));
            trackpointT.setPosition(createPosition(position));

            CompactCalendar time = position.getTime();
            if(time != null)
                lastTime = time;
            else
                // ensure that the time is always set
                time = lastTime;
            trackpointT.setTime(formatTime(time));

            if (first != null)
                trackpointT.setDistanceMeters(first.calculateDistance(position));
            else {
                trackpointT.setDistanceMeters(0.0);
                first = position;
            }
            trackpoints.add(trackpointT);
        }
        return trackT;
    }

    private CourseT createCourse(GpxRoute route, String routeName, int startIndex, int endIndex) {
        CourseT courseT = new ObjectFactory().createCourseT();
        // ensure the course name does not exceed 15 characters
        courseT.setName(routeName.substring(0, Math.min(routeName.length(), 15)));
        courseT.getLap().add(createCourseLap(route, startIndex, endIndex));
        courseT.getTrack().add(createTrack(route, startIndex, endIndex));
        return courseT;
    }

    private CourseFolderT createCourseFolder(CoursesT coursesT) {
        CourseFolderT courseFolderT = new ObjectFactory().createCourseFolderT();
        courseFolderT.setName("RouteConverter");
        courseFolderT.setNotes(GENERATED_BY);
        coursesT.setCourseFolder(courseFolderT);
        return courseFolderT;
    }

    private TrainingCenterDatabaseT createTrainingCenterDatabase(GpxRoute route, int startIndex, int endIndex) {
        ObjectFactory objectFactory = new ObjectFactory();
        TrainingCenterDatabaseT trainingCenterDatabaseT = objectFactory.createTrainingCenterDatabaseT();
        CoursesT coursesT = objectFactory.createCoursesT();
        trainingCenterDatabaseT.setCourses(coursesT);
        CourseFolderT courseFolderT = createCourseFolder(coursesT);
        List<CourseT> courses = courseFolderT.getCourse();
        courses.add(createCourse(route, route.getName(), startIndex, endIndex));
        return trainingCenterDatabaseT;
    }

    private TrainingCenterDatabaseT createTrainingCenterDatabase(List<GpxRoute> routes) {
        ObjectFactory objectFactory = new ObjectFactory();
        TrainingCenterDatabaseT trainingCenterDatabaseT = objectFactory.createTrainingCenterDatabaseT();
        CoursesT coursesT = objectFactory.createCoursesT();
        trainingCenterDatabaseT.setCourses(coursesT);
        CourseFolderT courseFolderT = createCourseFolder(coursesT);
        List<CourseT> courses = courseFolderT.getCourse();
        for (int i = 0; i < routes.size(); i++) {
            GpxRoute route = routes.get(i);
            // ensure that route names are unique
            courses.add(createCourse(route, (i+1) + ": " + route.getName(), 0, route.getPositionCount()));
        }
        return trainingCenterDatabaseT;
    }

    public void write(GpxRoute route, OutputStream target, int startIndex, int endIndex) throws IOException {
        try {
            TcxUtil.marshal1(createTrainingCenterDatabase(route, startIndex, endIndex), target);
        } catch (JAXBException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public void write(List<GpxRoute> routes, OutputStream target) throws IOException {
        try {
            TcxUtil.marshal1(createTrainingCenterDatabase(routes), target);
        } catch (JAXBException e) {
            throw new IllegalArgumentException(e);
        }
    }
}