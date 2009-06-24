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

import slash.navigation.RouteCharacteristics;
import slash.navigation.gpx.GpxPosition;
import slash.navigation.gpx.GpxRoute;
import slash.navigation.tcx.binding1.*;
import slash.navigation.util.CompactCalendar;

import javax.xml.bind.JAXBException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Reads and writes Training Center Database 1 (.tcx) files.
 *
 * @author Christian Pesch
 */

public class Tcx1Format extends TcxFormat {
    private static final Logger log = Logger.getLogger(Tcx1Format.class.getName());

    public String getName() {
        return "Training Center Database 1 (*" + getExtension() + ")";
    }


    private Double convertLongitude(PositionT positionT) {
        return positionT != null ? positionT.getLongitudeDegrees() : null;
    }

    private Double convertLatitude(PositionT positionT) {
        return positionT != null ? positionT.getLatitudeDegrees() : null;
    }

    private GpxRoute processTrack(String name, TrackT trackT) {
        List<GpxPosition> positions = new ArrayList<GpxPosition>();
        for (TrackpointT trackpointT : trackT.getTrackpoint()) {
            positions.add(new GpxPosition(convertLongitude(trackpointT.getPosition()),
                    convertLatitude(trackpointT.getPosition()),
                    trackpointT.getAltitudeMeters(),
                    null,
                    parseTime(trackpointT.getTime()),
                    null));
        }
        return new GpxRoute(this, RouteCharacteristics.Track, name, null, positions);
    }

    private GpxRoute processCoursePoints(String name, CourseT courseT) {
        List<GpxPosition> positions = new ArrayList<GpxPosition>();
        for (CoursePointT coursePointT : courseT.getCoursePoint()) {
            positions.add(new GpxPosition(convertLongitude(coursePointT.getPosition()),
                    convertLatitude(coursePointT.getPosition()),
                    coursePointT.getAltitudeMeters(),
                    null,
                    parseTime(coursePointT.getTime()),
                    coursePointT.getName()));
        }
        return positions.size() > 0 ? new GpxRoute(this, RouteCharacteristics.Route, name, null, positions) : null;
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
        return new GpxRoute(this, RouteCharacteristics.Waypoints, name, null, positions);
    }


    private List<GpxRoute> processTracks(String name, List<TrackT> trackListT) {
        List<GpxRoute> result = new ArrayList<GpxRoute>();
        for (TrackT trackT : trackListT) {
            result.add(processTrack(name, trackT));
        }
        return result;
    }

    private List<GpxRoute> processRun(String name, RunT runT) {
        List<GpxRoute> result = new ArrayList<GpxRoute>();
        for (ActivityLapT activityLapT : runT.getLap())
            result.addAll(processTracks(name, activityLapT.getTrack()));
        return result;
    }

    private List<GpxRoute> processCourseLap(String name, CourseT courseT) {
        List<GpxRoute> result = new ArrayList<GpxRoute>();
        for (CourseLapT courseLapT : courseT.getLap()) {
            result.add(processCourseLap(name, courseLapT));
        }
        return result;
    }

    private List<GpxRoute> process(String name, CourseFolderT courseFolderT) {
        String prefix = name != null && !"RouteConverter".equals(name) ? name + "/" : "";

        List<GpxRoute> result = new ArrayList<GpxRoute>();
        for (CourseFolderT folderT : courseFolderT.getFolder())
            result.addAll(process(prefix + folderT.getName(), folderT));

        for (CourseT courseT : courseFolderT.getCourse()) {
            String positionListName = prefix + courseT.getName();
            GpxRoute coursePoints = processCoursePoints(positionListName, courseT);
            if (coursePoints != null)
                result.add(coursePoints);
            // TODO ignored for better RW roundtrip result
            // result.addAll(processCourseLap(positionListName, courseT));
            result.addAll(processTracks(positionListName, courseT.getTrack()));
        }
        return result;
    }

    private List<GpxRoute> process(CourseFolderT courseFolderT) {
        return process(courseFolderT.getName(), courseFolderT);
    }

    private List<GpxRoute> process(String name, MultiSportFolderT multiSportFolderT) {
        List<GpxRoute> result = new ArrayList<GpxRoute>();
        for (MultiSportFolderT folderT : multiSportFolderT.getFolder())
            result.addAll(process(name + "/" + folderT.getName(), folderT));

        for (MultiSportSessionT multiSportSessionT : multiSportFolderT.getMultiSportSession()) {
            String positionListName = name + "/" + multiSportFolderT.getName();
            result.addAll(processRun(positionListName, multiSportSessionT.getFirstSport().getRun()));
            for (NextSportT nextSportT : multiSportSessionT.getNextSport()) {
                result.addAll(processRun(positionListName, nextSportT.getRun()));
                ActivityLapT activityLapT = nextSportT.getTransition();
                if (activityLapT != null)
                    result.addAll(processTracks(positionListName, activityLapT.getTrack()));
            }
        }
        return result;
    }

    private List<GpxRoute> process(MultiSportFolderT multiSportFolderT) {
        return process(multiSportFolderT.getName(), multiSportFolderT);
    }

    private List<GpxRoute> process(String name, HistoryFolderT historyFolderT) {
        List<GpxRoute> result = new ArrayList<GpxRoute>();
        for (HistoryFolderT folderT : historyFolderT.getFolder())
            result.addAll(process(name + "/" + folderT.getName(), folderT));

        for (RunT runT : historyFolderT.getRun())
            result.addAll(processRun(name, runT));
        return result;
    }

    private List<GpxRoute> process(HistoryFolderT historyFolderT) {
        return process(historyFolderT.getName(), historyFolderT);
    }

    private List<GpxRoute> process(TrainingCenterDatabaseT trainingCenterDatabaseT) {
        List<GpxRoute> result = new ArrayList<GpxRoute>();

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

    public List<GpxRoute> read(InputStream source, CompactCalendar startDate) throws IOException {
        try {
            TrainingCenterDatabaseT trainingCenterDatabase = TcxUtil.unmarshal1(source);
            List<GpxRoute> result = process(trainingCenterDatabase);
            return result.size() > 0 ? result : null;
        } catch (JAXBException e) {
            log.fine("Error reading " + source + ": " + e.getMessage());
            return null;
        }
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

        courseLapT.setDistanceMeters(distanceMeters);
        courseLapT.setTotalTimeSeconds(totalTimeMilliSeconds / 1000);
        courseLapT.setIntensity(IntensityT.fromValue("Active"));

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
        for (int i = startIndex; i < endIndex; i++) {
            GpxPosition position = positions.get(i);
            TrackpointT trackpointT = objectFactory.createTrackpointT();
            trackpointT.setAltitudeMeters(position.getElevation());
            trackpointT.setPosition(createPosition(position));
            trackpointT.setTime(formatTime(position.getTime()));
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

    private CourseT createCourse(GpxRoute route, int startIndex, int endIndex) {
        CourseT courseT = new ObjectFactory().createCourseT();
        courseT.setName(route.getName());
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
        courses.add(createCourse(route, startIndex, endIndex));
        return trainingCenterDatabaseT;
    }

    private TrainingCenterDatabaseT createTrainingCenterDatabase(List<GpxRoute> routes) {
        ObjectFactory objectFactory = new ObjectFactory();
        TrainingCenterDatabaseT trainingCenterDatabaseT = objectFactory.createTrainingCenterDatabaseT();
        CoursesT coursesT = objectFactory.createCoursesT();
        trainingCenterDatabaseT.setCourses(coursesT);
        CourseFolderT courseFolderT = createCourseFolder(coursesT);
        List<CourseT> courses = courseFolderT.getCourse();
        for (GpxRoute route : routes)
            courses.add(createCourse(route, 0, route.getPositionCount()));
        return trainingCenterDatabaseT;
    }

    public void write(GpxRoute route, File target, int startIndex, int endIndex) throws IOException {
        try {
            TcxUtil.marshal1(createTrainingCenterDatabase(route, startIndex, endIndex), target);
        } catch (JAXBException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public void write(List<GpxRoute> routes, File target) throws IOException {
        try {
            TcxUtil.marshal1(createTrainingCenterDatabase(routes), target);
        } catch (JAXBException e) {
            throw new IllegalArgumentException(e);
        }
    }
}