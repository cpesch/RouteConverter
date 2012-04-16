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

import slash.common.io.CompactCalendar;
import slash.navigation.gpx.GpxPosition;
import slash.navigation.gpx.GpxRoute;
import slash.navigation.tcx.binding2.ActivityLapT;
import slash.navigation.tcx.binding2.ActivityListT;
import slash.navigation.tcx.binding2.ActivityT;
import slash.navigation.tcx.binding2.CourseLapT;
import slash.navigation.tcx.binding2.CourseListT;
import slash.navigation.tcx.binding2.CoursePointT;
import slash.navigation.tcx.binding2.CourseT;
import slash.navigation.tcx.binding2.HeartRateInBeatsPerMinuteT;
import slash.navigation.tcx.binding2.IntensityT;
import slash.navigation.tcx.binding2.MultiSportSessionT;
import slash.navigation.tcx.binding2.NextSportT;
import slash.navigation.tcx.binding2.ObjectFactory;
import slash.navigation.tcx.binding2.PositionT;
import slash.navigation.tcx.binding2.TrackT;
import slash.navigation.tcx.binding2.TrackpointT;
import slash.navigation.tcx.binding2.TrainingCenterDatabaseT;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import static java.lang.Math.min;
import static slash.navigation.base.RouteCharacteristics.Route;
import static slash.navigation.base.RouteCharacteristics.Track;
import static slash.navigation.base.RouteCharacteristics.Waypoints;

/**
 * Reads Training Center Database 2 (.tcx) files.
 *
 * @author Christian Pesch
 */

public class Tcx2Format extends TcxFormat {
    private static final Logger log = Logger.getLogger(Tcx2Format.class.getName());

    public String getName() {
        return "Training Center Database 2 (*" + getExtension() + ")";
    }


    private Double convertLongitude(PositionT positionT) {
        return positionT != null ? positionT.getLongitudeDegrees() : null;
    }

    private Double convertLatitude(PositionT positionT) {
        return positionT != null ? positionT.getLatitudeDegrees() : null;
    }

    private List<GpxPosition> processTrack(TrackT trackT) {
        List<GpxPosition> result = new ArrayList<GpxPosition>();
        for (TrackpointT trackpointT : trackT.getTrackpoint()) {
            result.add(new GpxPosition(convertLongitude(trackpointT.getPosition()),
                    convertLatitude(trackpointT.getPosition()),
                    trackpointT.getAltitudeMeters(),
                    null,
                    parseTime(trackpointT.getTime()),
                    null));
        }
        return result;
    }

    private GpxRoute processCoursePoints(CourseT courseT) {
        List<GpxPosition> positions = new ArrayList<GpxPosition>();
        for (CoursePointT coursePointT : courseT.getCoursePoint()) {
            positions.add(new GpxPosition(convertLongitude(coursePointT.getPosition()),
                    convertLatitude(coursePointT.getPosition()),
                    coursePointT.getAltitudeMeters(),
                    null,
                    parseTime(coursePointT.getTime()),
                    coursePointT.getName()));
        }
        return positions.size() > 0 ? new GpxRoute(this, Route, courseT.getName(), null, positions) : null;
    }

    private GpxRoute processCourseLap(String name, CourseLapT courseLapT) {
        List<GpxPosition> positions = new ArrayList<GpxPosition>();
        positions.add(new GpxPosition(convertLongitude(courseLapT.getBeginPosition()),
                convertLatitude(courseLapT.getBeginPosition()),
                courseLapT.getBeginAltitudeMeters(),
                null,
                null,
                "0 seconds"));
        positions.add(new GpxPosition(convertLongitude(courseLapT.getEndPosition()),
                convertLatitude(courseLapT.getEndPosition()),
                courseLapT.getEndAltitudeMeters(),
                null,
                null,
                courseLapT.getTotalTimeSeconds() + " seconds"));
        return new GpxRoute(this, Waypoints, name, null, positions);
    }


    private GpxRoute process(ActivityLapT activityLapT) {
        List<GpxPosition> positions = new ArrayList<GpxPosition>();
        for (TrackT trackT : activityLapT.getTrack()) {
            positions.addAll(processTrack(trackT));
        }
        return new GpxRoute(this, Track, activityLapT.getNotes(), null, positions);
    }

    private List<GpxRoute> process(ActivityT activityT) {
        List<GpxRoute> result = new ArrayList<GpxRoute>();
        for (ActivityLapT activityLapT : activityT.getLap()) {
            result.add(process(activityLapT));
        }
        return result;
    }

    private List<GpxRoute> process(CourseT courseT) {
        List<GpxRoute> result = new ArrayList<GpxRoute>();
        GpxRoute coursePoints = processCoursePoints(courseT);
        if (coursePoints != null)
            result.add(coursePoints);

        boolean writtenByRouteConverter = courseT.getNotes() != null &&
                GENERATED_BY.equals(courseT.getNotes());
        if (!writtenByRouteConverter) {
            for (CourseLapT courseLapT : courseT.getLap()) {
                result.add(processCourseLap(courseT.getName(), courseLapT));
            }
        }
        List<GpxPosition> positions = new ArrayList<GpxPosition>();
        for (TrackT trackT : courseT.getTrack()) {
            positions.addAll(processTrack(trackT));
        }
        result.add(new GpxRoute(this, Track, courseT.getName(), null, positions));
        return result;
    }

    private List<GpxRoute> process(TrainingCenterDatabaseT trainingCenterDatabaseT) {
        List<GpxRoute> result = new ArrayList<GpxRoute>();

        ActivityListT activityListT = trainingCenterDatabaseT.getActivities();
        if (activityListT != null) {
            // TrainingCenterDatabase -> ActivityList -> Activity -> ActivityLap -> Track -> TrackPoint -> Position
            for (ActivityT activityT : activityListT.getActivity()) {
                result.addAll(process(activityT));
            }

            // TrainingCenterDatabase -> ActivityList -> MultiSportSession -> FirstSport -> Activity -> ActivityLap -> Track -> TrackPoint -> Position
            // TrainingCenterDatabase -> ActivityList -> MultiSportSession -> NextSport -> Activity -> ActivityLap -> Track -> TrackPoint -> Position
            // TrainingCenterDatabase -> ActivityList -> MultiSportSession -> NextSport -> ActivityLap -> Track -> TrackPoint -> Position
            for (MultiSportSessionT multiSportSessionT : activityListT.getMultiSportSession()) {
                result.addAll(process(multiSportSessionT.getFirstSport().getActivity()));
                for (NextSportT nextSportT : multiSportSessionT.getNextSport()) {
                    result.addAll(process(nextSportT.getActivity()));
                    result.add(process(nextSportT.getTransition()));
                }
            }
        }

        // TrainingCenterDatabase -> CourseList -> Course -> CoursePoint -> Position
        // TrainingCenterDatabase -> CourseList -> Course -> CourseLap -> BeginPosition/EndPosition
        // TrainingCenterDatabase -> CourseList -> Course -> Track -> TrackPoint -> Position
        CourseListT courseListT = trainingCenterDatabaseT.getCourses();
        if (courseListT != null) {
            for (CourseT courseT : courseListT.getCourse()) {
                result.addAll(process(courseT));
            }
        }
        return result;
    }

    public List<GpxRoute> read(InputStream source, CompactCalendar startDate) throws IOException {
        try {
            TrainingCenterDatabaseT trainingCenterDatabase = TcxUtil.unmarshal2(source);
            List<GpxRoute> result = process(trainingCenterDatabase);
            return result.size() > 0 ? result : null;
        } catch (JAXBException e) {
            log.fine("Error reading " + source + ": " + e.getMessage());
            return null;
        }
    }


    private HeartRateInBeatsPerMinuteT getHeartBeatRateT(GpxPosition position) {
        Short heartBeatRate = getHeartBeatRate(position);
        if (heartBeatRate != null) {
            HeartRateInBeatsPerMinuteT result = new ObjectFactory().createHeartRateInBeatsPerMinuteT();
            result.setValue(heartBeatRate);
            return result;
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

        courseLapT.setAverageHeartRateBpm(getHeartBeatRateT(first));
        courseLapT.setDistanceMeters(route.getDistance());
        courseLapT.setIntensity(IntensityT.fromValue("Active"));
        courseLapT.setTotalTimeSeconds(route.getTime() / 1000.0);

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
        GpxPosition previous = null;
        double distance = 0.0;
        for (int i = startIndex; i < endIndex; i++) {
            GpxPosition position = positions.get(i);
            TrackpointT trackpointT = objectFactory.createTrackpointT();
            trackpointT.setAltitudeMeters(position.getElevation());
            trackpointT.setHeartRateBpm(getHeartBeatRateT(position));
            trackpointT.setPosition(createPosition(position));
            trackpointT.setTime(formatTime(position.getTime()));

            if (previous != null) {
                distance += previous.calculateDistance(position);
            }
            previous = position;
            trackpointT.setDistanceMeters(distance);

            trackpoints.add(trackpointT);
        }
        return trackT;
    }

    private CourseT createCourse(GpxRoute route, String routeName, int startIndex, int endIndex) {
        CourseT courseT = new ObjectFactory().createCourseT();
        // ensure the course name does not exceed 15 characters
        courseT.setName(routeName.substring(0, min(routeName.length(), 15)));
        courseT.setNotes(GENERATED_BY);
        courseT.getLap().add(createCourseLap(route, startIndex, endIndex));
        courseT.getTrack().add(createTrack(route, startIndex, endIndex));
        return courseT;
    }

    private TrainingCenterDatabaseT createTrainingCenterDatabase(GpxRoute route, int startIndex, int endIndex) {
        ObjectFactory objectFactory = new ObjectFactory();
        TrainingCenterDatabaseT trainingCenterDatabaseT = objectFactory.createTrainingCenterDatabaseT();
        CourseListT courseListT = objectFactory.createCourseListT();
        trainingCenterDatabaseT.setCourses(courseListT);
        List<CourseT> courses = courseListT.getCourse();
        courses.add(createCourse(route, asRouteName(route.getName()), startIndex, endIndex));
        return trainingCenterDatabaseT;
    }

    private TrainingCenterDatabaseT createTrainingCenterDatabase(List<GpxRoute> routes) {
        ObjectFactory objectFactory = new ObjectFactory();
        TrainingCenterDatabaseT trainingCenterDatabaseT = objectFactory.createTrainingCenterDatabaseT();
        CourseListT courseListT = objectFactory.createCourseListT();
        trainingCenterDatabaseT.setCourses(courseListT);
        List<CourseT> courses = courseListT.getCourse();
        for (int i = 0; i < routes.size(); i++) {
            GpxRoute route = routes.get(i);
            // ensure that route names are unique
            courses.add(createCourse(route, (i + 1) + ": " + asRouteName(route.getName()), 0, route.getPositionCount()));
        }
        return trainingCenterDatabaseT;
    }

    public void write(GpxRoute route, OutputStream target, int startIndex, int endIndex) throws IOException {
        try {
            TcxUtil.marshal2(createTrainingCenterDatabase(route, startIndex, endIndex), target);
        } catch (JAXBException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public void write(List<GpxRoute> routes, OutputStream target) throws IOException {
        try {
            TcxUtil.marshal2(createTrainingCenterDatabase(routes), target);
        } catch (JAXBException e) {
            throw new IllegalArgumentException(e);
        }
    }
}