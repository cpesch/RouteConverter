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
import slash.navigation.tcx.binding1.ActivityLapT;
import slash.navigation.tcx.binding1.CourseFolderT;
import slash.navigation.tcx.binding1.CourseLapT;
import slash.navigation.tcx.binding1.CoursePointT;
import slash.navigation.tcx.binding1.CourseT;
import slash.navigation.tcx.binding1.CoursesT;
import slash.navigation.tcx.binding1.HistoryFolderT;
import slash.navigation.tcx.binding1.HistoryT;
import slash.navigation.tcx.binding1.IntensityT;
import slash.navigation.tcx.binding1.MultiSportFolderT;
import slash.navigation.tcx.binding1.MultiSportSessionT;
import slash.navigation.tcx.binding1.NextSportT;
import slash.navigation.tcx.binding1.ObjectFactory;
import slash.navigation.tcx.binding1.PositionT;
import slash.navigation.tcx.binding1.RunT;
import slash.navigation.tcx.binding1.TrackT;
import slash.navigation.tcx.binding1.TrackpointT;
import slash.navigation.tcx.binding1.TrainingCenterDatabaseT;

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
import static slash.navigation.tcx.TcxUtil.marshal1;
import static slash.navigation.tcx.TcxUtil.unmarshal1;

/**
 * Reads Training Center Database 1 (.tcx) files.
 *
 * @author Christian Pesch
 */

public class Tcx1Format extends TcxFormat {

    public String getName() {
        return "Training Center Database 1 (*" + getExtension() + ")";
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

    private TcxRoute processCoursePoints(String name, CourseT courseT) {
        List<Wgs84Position> positions = new ArrayList<>();
        for (CoursePointT coursePointT : courseT.getCoursePoint()) {
            positions.add(new Wgs84Position(convertLongitude(coursePointT.getPosition()),
                    convertLatitude(coursePointT.getPosition()),
                    coursePointT.getAltitudeMeters(),
                    null,
                    parseXMLTime(coursePointT.getTime()),
                    coursePointT.getName()));
        }
        return positions.size() > 0 ? new TcxRoute(this, Route, name, positions) : null;
    }

    private TcxRoute processCourseLap(String name, CourseLapT courseLapT) {
        List<Wgs84Position> positions = new ArrayList<>();
        positions.add(new Wgs84Position(convertLongitude(courseLapT.getBeginPosition()),
                convertLatitude(courseLapT.getBeginPosition()),
                courseLapT.getBeginAltitudeMeters(),
                null,
                null,
                "0 seconds",
                courseLapT));
        positions.add(new Wgs84Position(convertLongitude(courseLapT.getEndPosition()),
                convertLatitude(courseLapT.getEndPosition()),
                courseLapT.getEndAltitudeMeters(),
                null,
                null,
                courseLapT.getTotalTimeSeconds() + " seconds",
                courseLapT));
        return new TcxRoute(this, Waypoints, name, positions);
    }


    private TcxRoute processTracks(String name, List<TrackT> trackListT) {
        List<Wgs84Position> positions = new ArrayList<>();
        for (TrackT trackT : trackListT) {
            positions.addAll(processTrack(trackT));
        }
        return new TcxRoute(this, Track, name, positions);
    }

    private List<TcxRoute> processRun(String name, RunT runT) {
        List<TcxRoute> result = new ArrayList<>();
        for (ActivityLapT activityLapT : runT.getLap())
            result.add(processTracks(name, activityLapT.getTrack()));
        return result;
    }

    private List<TcxRoute> processCourseLap(String name, CourseT courseT) {
        List<TcxRoute> result = new ArrayList<>();
        for (CourseLapT courseLapT : courseT.getLap()) {
            result.add(processCourseLap(name, courseLapT));
        }
        return result;
    }

    private List<TcxRoute> process(String name, CourseFolderT courseFolderT) {
        String prefix = name != null ? name + "/" : "";

        List<TcxRoute> result = new ArrayList<>();
        for (CourseFolderT folderT : courseFolderT.getFolder())
            result.addAll(process(prefix + folderT.getName(), folderT));

        for (CourseT courseT : courseFolderT.getCourse()) {
            String positionListName = prefix + courseT.getName();
            TcxRoute coursePoints = processCoursePoints(positionListName, courseT);
            if (coursePoints != null)
                result.add(coursePoints);
            result.addAll(processCourseLap(positionListName, courseT));
            result.add(processTracks(positionListName, courseT.getTrack()));
        }
        return result;
    }

    private List<TcxRoute> process(CourseFolderT courseFolderT) {
        return process(courseFolderT.getName(), courseFolderT);
    }

    private List<TcxRoute> process(String name, MultiSportFolderT multiSportFolderT) {
        List<TcxRoute> result = new ArrayList<>();
        for (MultiSportFolderT folderT : multiSportFolderT.getFolder())
            result.addAll(process(name + "/" + folderT.getName(), folderT));

        for (MultiSportSessionT multiSportSessionT : multiSportFolderT.getMultiSportSession()) {
            String positionListName = name + "/" + multiSportFolderT.getName();
            result.addAll(processRun(positionListName, multiSportSessionT.getFirstSport().getRun()));
            for (NextSportT nextSportT : multiSportSessionT.getNextSport()) {
                result.addAll(processRun(positionListName, nextSportT.getRun()));
                ActivityLapT activityLapT = nextSportT.getTransition();
                if (activityLapT != null)
                    result.add(processTracks(positionListName, activityLapT.getTrack()));
            }
        }
        return result;
    }

    private List<TcxRoute> process(MultiSportFolderT multiSportFolderT) {
        return process(multiSportFolderT.getName(), multiSportFolderT);
    }

    private List<TcxRoute> process(String name, HistoryFolderT historyFolderT) {
        List<TcxRoute> result = new ArrayList<>();
        for (HistoryFolderT folderT : historyFolderT.getFolder())
            result.addAll(process(name + "/" + folderT.getName(), folderT));

        for (RunT runT : historyFolderT.getRun())
            result.addAll(processRun(name, runT));
        return result;
    }

    private List<TcxRoute> process(HistoryFolderT historyFolderT) {
        return process(historyFolderT.getName(), historyFolderT);
    }

    private List<TcxRoute> process(TrainingCenterDatabaseT trainingCenterDatabaseT) {
        List<TcxRoute> result = new ArrayList<>();

        // TrainingCenterDatabase -> Courses -> CourseFolder -> Course -> CoursePoint -> Position
        // TrainingCenterDatabase -> Courses -> CourseFolder -> CourseFolder* -> Course -> CoursePoint -> Position
        // TrainingCenterDatabase -> Courses -> CourseFolder -> Course -> Lap -> BeginPosition/EndPosition
        // TrainingCenterDatabase -> Courses -> CourseFolder -> Course -> Track -> TrackPoint -> Position
        CoursesT coursesT = trainingCenterDatabaseT.getCourses();
        if (coursesT != null)
            result.addAll(process(coursesT.getCourseFolder()));

        HistoryT historyT = trainingCenterDatabaseT.getHistory();
        if (historyT != null) {
            // TrainingCenterDatabase -> History -> Biking -> Run -> ActivityLap -> Track -> TrackPoint -> Position
            // TrainingCenterDatabase -> History -> Biking -> HistoryFolder* -> Run -> ActivityLap -> Track -> TrackPoint -> Position
            // TrainingCenterDatabase -> History -> Other -> Run -> ActivityLap -> Track -> TrackPoint -> Position
            // TrainingCenterDatabase -> History -> Other -> HistoryFolder* -> Run -> ActivityLap -> Track -> TrackPoint -> Position
            // TrainingCenterDatabase -> History -> Running -> Run -> ActivityLap -> Track -> TrackPoint -> Position
            // TrainingCenterDatabase -> History -> Running -> HistoryFolder* -> Run -> ActivityLap -> Track -> TrackPoint -> Position
            result.addAll(process(historyT.getBiking()));
            result.addAll(process(historyT.getOther()));
            result.addAll(process(historyT.getRunning()));

            // TrainingCenterDatabase -> History -> MultiSport -> MultiSportSession -> FirstSport -> Run -> ActivityLap -> Track -> TrackPoint -> Position
            // TrainingCenterDatabase -> History -> MultiSport -> MultiSportFolder* -> MultiSportSession -> FirstSport -> Run -> ActivityLap -> Track -> TrackPoint -> Position
            // TrainingCenterDatabase -> History -> MultiSport -> MultiSportSession -> NextSport -> Run -> ActivityLap -> Track -> TrackPoint -> Position
            // TrainingCenterDatabase -> History -> MultiSport -> MultiSportFolder* -> MultiSportSession -> NextSport -> Run -> ActivityLap -> Track -> TrackPoint -> Position
            // TrainingCenterDatabase -> History -> MultiSport -> MultiSportSession -> NextSport -> Transition -> Track -> TrackPoint -> Position
            // TrainingCenterDatabase -> History -> MultiSport -> MultiSportFolder* -> MultiSportSession -> NextSport -> Transition -> Track -> TrackPoint -> Position
            result.addAll(process(historyT.getMultiSport()));
        }
        return result;
    }

    public void read(InputStream source, ParserContext<TcxRoute> context) throws Exception {
        TrainingCenterDatabaseT trainingCenterDatabase = unmarshal1(source);
        context.appendRoutes(process(trainingCenterDatabase));
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

        courseLapT.setAverageHeartRateBpm(getHeartBeatRate(first));
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
            trackpointT.setHeartRateBpm(getHeartBeatRate(position));
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

    private CourseFolderT createCourseFolder(CoursesT coursesT) {
        CourseFolderT courseFolderT = new ObjectFactory().createCourseFolderT();
        courseFolderT.setName("RouteConverter");
        coursesT.setCourseFolder(courseFolderT);
        return courseFolderT;
    }

    private TrainingCenterDatabaseT createTrainingCenterDatabase(TcxRoute route, int startIndex, int endIndex) {
        ObjectFactory objectFactory = new ObjectFactory();
        TrainingCenterDatabaseT trainingCenterDatabaseT = objectFactory.createTrainingCenterDatabaseT();
        CoursesT coursesT = objectFactory.createCoursesT();
        trainingCenterDatabaseT.setCourses(coursesT);
        CourseFolderT courseFolderT = createCourseFolder(coursesT);
        List<CourseT> courses = courseFolderT.getCourse();
        courses.add(createCourse(route, asRouteName(route.getName()), startIndex, endIndex));
        return trainingCenterDatabaseT;
    }

    private TrainingCenterDatabaseT createTrainingCenterDatabase(List<TcxRoute> routes) {
        ObjectFactory objectFactory = new ObjectFactory();
        TrainingCenterDatabaseT trainingCenterDatabaseT = objectFactory.createTrainingCenterDatabaseT();
        CoursesT coursesT = objectFactory.createCoursesT();
        trainingCenterDatabaseT.setCourses(coursesT);
        CourseFolderT courseFolderT = createCourseFolder(coursesT);
        List<CourseT> courses = courseFolderT.getCourse();

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
            marshal1(createTrainingCenterDatabase(route, startIndex, endIndex), target);
        } catch (JAXBException e) {
            throw new IOException("Cannot marshall " + route + ": " + e, e);
        }
    }

    public void write(List<TcxRoute> routes, OutputStream target) throws IOException {
        try {
            marshal1(createTrainingCenterDatabase(routes), target);
        } catch (JAXBException e) {
            throw new IOException("Cannot marshall " + routes + ": " + e, e);
        }
    }
}