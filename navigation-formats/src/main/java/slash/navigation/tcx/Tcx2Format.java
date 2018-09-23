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

import slash.common.io.Transfer;
import slash.navigation.base.ParserContext;
import slash.navigation.base.Wgs84Position;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static slash.common.io.Transfer.isEmpty;
import static slash.common.io.Transfer.parseXMLTime;
import static slash.navigation.base.RouteCharacteristics.Route;
import static slash.navigation.base.RouteCharacteristics.Track;
import static slash.navigation.base.RouteCharacteristics.Waypoints;
import static slash.navigation.tcx.TcxUtil.marshal2;
import static slash.navigation.tcx.TcxUtil.unmarshal2;

/**
 * Reads Training Center Database 2 (.tcx) files.
 *
 * @author Christian Pesch
 */

public class Tcx2Format extends TcxFormat {

    public String getName() {
        return "Training Center Database 2 (*" + getExtension() + ")";
    }


    private Double convertLongitude(PositionT positionT) {
        return positionT != null ? positionT.getLongitudeDegrees() : null;
    }

    private Double convertLatitude(PositionT positionT) {
        return positionT != null ? positionT.getLatitudeDegrees() : null;
    }

    private List<Wgs84Position> processTrack(TrackT trackT) {
        List<Wgs84Position> result = new ArrayList<>();
        for (TrackpointT trackpointT : trackT.getTrackpoint()) {
            result.add(new Wgs84Position(convertLongitude(trackpointT.getPosition()),
                    convertLatitude(trackpointT.getPosition()),
                    trackpointT.getAltitudeMeters(),
                    null,
                    parseXMLTime(trackpointT.getTime()),
                    null,
                    trackpointT));
        }
        return result;
    }

    private TcxRoute processCoursePoints(CourseT courseT) {
        List<Wgs84Position> positions = new ArrayList<>();
        for (CoursePointT coursePointT : courseT.getCoursePoint()) {
            positions.add(new Wgs84Position(convertLongitude(coursePointT.getPosition()),
                    convertLatitude(coursePointT.getPosition()),
                    coursePointT.getAltitudeMeters(),
                    null,
                    parseXMLTime(coursePointT.getTime()),
                    coursePointT.getName()));
        }
        return positions.size() > 0 ? new TcxRoute(this, Route, courseT.getName(), positions) : null;
    }

    private TcxRoute processCourseLap(String name, CourseLapT courseLapT) {
        List<Wgs84Position> positions = new ArrayList<>();
        positions.add(new Wgs84Position(convertLongitude(courseLapT.getBeginPosition()),
                convertLatitude(courseLapT.getBeginPosition()),
                courseLapT.getBeginAltitudeMeters(),
                null,
                null,
                "0 seconds"));
        positions.add(new Wgs84Position(convertLongitude(courseLapT.getEndPosition()),
                convertLatitude(courseLapT.getEndPosition()),
                courseLapT.getEndAltitudeMeters(),
                null,
                null,
                courseLapT.getTotalTimeSeconds() + " seconds"));
        return new TcxRoute(this, Waypoints, name, positions);
    }


    private TcxRoute process(ActivityLapT activityLapT) {
        List<Wgs84Position> positions = new ArrayList<>();
        for (TrackT trackT : activityLapT.getTrack()) {
            positions.addAll(processTrack(trackT));
        }
        return new TcxRoute(this, Track, activityLapT.getNotes(), positions);
    }

    private List<TcxRoute> process(ActivityT activityT) {
        List<TcxRoute> result = new ArrayList<>();
        for (ActivityLapT activityLapT : activityT.getLap()) {
            result.add(process(activityLapT));
        }
        return result;
    }

    private List<TcxRoute> process(CourseT courseT) {
        List<TcxRoute> result = new ArrayList<>();
        TcxRoute coursePoints = processCoursePoints(courseT);
        if (coursePoints != null)
            result.add(coursePoints);

        for (CourseLapT courseLapT : courseT.getLap()) {
            result.add(processCourseLap(courseT.getName(), courseLapT));
        }

        List<Wgs84Position> positions = new ArrayList<>();
        for (TrackT trackT : courseT.getTrack()) {
            positions.addAll(processTrack(trackT));
        }
        result.add(new TcxRoute(this, Track, courseT.getName(), positions));
        return result;
    }

    private List<TcxRoute> process(TrainingCenterDatabaseT trainingCenterDatabaseT) {
        List<TcxRoute> result = new ArrayList<>();

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

    public void read(InputStream source, ParserContext<TcxRoute> context) throws Exception {
        TrainingCenterDatabaseT trainingCenterDatabase = unmarshal2(source);
        context.appendRoutes(process(trainingCenterDatabase));
    }


    private HeartRateInBeatsPerMinuteT getHeartBeatRateT(Wgs84Position position) {
        TrackpointT trackpointT = position.getOrigin(TrackpointT.class);
        if (trackpointT != null) {
            HeartRateInBeatsPerMinuteT heartRateBpm = trackpointT.getHeartRateBpm();
            if (heartRateBpm != null)
                return heartRateBpm;
        }

        Short heartBeatRate = getHeartBeatRate(position);
        if (heartBeatRate != null) {
            HeartRateInBeatsPerMinuteT result = new ObjectFactory().createHeartRateInBeatsPerMinuteT();
            result.setValue(heartBeatRate);
            return result;
        }
        return null;
    }

    private PositionT createPosition(Wgs84Position position) {
        PositionT positionT = new ObjectFactory().createPositionT();
        if (position.getLongitude() != null)
            positionT.setLongitudeDegrees(position.getLongitude());
        if (position.getLatitude() != null)
            positionT.setLatitudeDegrees(position.getLatitude());
        return positionT;
    }

    private CourseLapT createCourseLap(TcxRoute route, int startIndex, int endIndex) {
        CourseLapT courseLapT = new ObjectFactory().createCourseLapT();
        Wgs84Position first = route.getPositionCount() >= startIndex ? route.getPosition(startIndex) : null;
        Wgs84Position last = route.getPositionCount() >= endIndex ? route.getPosition(endIndex - 1) : null;
        if (last == null)
            last = first;

        if (first != null)
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

    private TrackT createTrack(TcxRoute route, int startIndex, int endIndex) {
        ObjectFactory objectFactory = new ObjectFactory();
        TrackT trackT = objectFactory.createTrackT();
        List<TrackpointT> trackpoints = trackT.getTrackpoint();

        List<Wgs84Position> positions = route.getPositions();
        Wgs84Position previous = null;
        double distance = 0.0;
        for (int i = startIndex; i < endIndex; i++) {
            Wgs84Position position = positions.get(i);
            TrackpointT trackpointT = objectFactory.createTrackpointT();
            trackpointT.setAltitudeMeters(position.getElevation());
            trackpointT.setHeartRateBpm(getHeartBeatRateT(position));
            trackpointT.setPosition(createPosition(position));
            trackpointT.setTime(Transfer.formatXMLTime(position.getTime()));

            if (previous != null) {
                Double previousDistance = previous.calculateDistance(position);
                if (!isEmpty(previousDistance))
                    distance += previousDistance;
            }
            previous = position;
            trackpointT.setDistanceMeters(distance);

            trackpoints.add(trackpointT);
        }
        return trackT;
    }

    private CourseT createCourse(TcxRoute route, String routeName, int startIndex, int endIndex) {
        CourseT courseT = new ObjectFactory().createCourseT();
        courseT.setName(routeName);
        courseT.getLap().add(createCourseLap(route, startIndex, endIndex));
        courseT.getTrack().add(createTrack(route, startIndex, endIndex));
        return courseT;
    }

    private TrainingCenterDatabaseT createTrainingCenterDatabase(TcxRoute route, int startIndex, int endIndex) {
        ObjectFactory objectFactory = new ObjectFactory();
        TrainingCenterDatabaseT trainingCenterDatabaseT = objectFactory.createTrainingCenterDatabaseT();
        CourseListT courseListT = objectFactory.createCourseListT();
        trainingCenterDatabaseT.setCourses(courseListT);
        List<CourseT> courses = courseListT.getCourse();
        courses.add(createCourse(route, asRouteName(route.getName()), startIndex, endIndex));
        return trainingCenterDatabaseT;
    }

    private TrainingCenterDatabaseT createTrainingCenterDatabase(List<TcxRoute> routes) {
        ObjectFactory objectFactory = new ObjectFactory();
        TrainingCenterDatabaseT trainingCenterDatabaseT = objectFactory.createTrainingCenterDatabaseT();
        CourseListT courseListT = objectFactory.createCourseListT();
        trainingCenterDatabaseT.setCourses(courseListT);
        List<CourseT> courses = courseListT.getCourse();

        Set<String> routeNames = new HashSet<>(routes.size());
        for (TcxRoute route : routes) {
            String routeName = createUniqueRouteName(route.getName(), routeNames);
            routeNames.add(routeName);
            courses.add(createCourse(route, routeName, 0, route.getPositionCount()));
        }
        return trainingCenterDatabaseT;
    }

    public void write(TcxRoute route, OutputStream target, int startIndex, int endIndex) throws IOException {
        try {
            marshal2(createTrainingCenterDatabase(route, startIndex, endIndex), target);
        } catch (JAXBException e) {
            throw new IOException("Cannot marshall " + route + ": " + e, e);
        }
    }

    public void write(List<TcxRoute> routes, OutputStream target) throws IOException {
        try {
            marshal2(createTrainingCenterDatabase(routes), target);
        } catch (JAXBException e) {
            throw new IOException("Cannot marshall " + routes + ": " + e, e);
        }
    }
}