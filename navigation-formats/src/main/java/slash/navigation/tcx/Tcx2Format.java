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

import slash.navigation.base.RouteCharacteristics;
import slash.navigation.gpx.GpxFormat;
import slash.navigation.gpx.GpxPosition;
import slash.navigation.gpx.GpxRoute;
import slash.navigation.tcx.binding2.*;
import slash.common.io.CompactCalendar;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Reads Training Center Database 2 (.tcx) files.
 *
 * @author Christian Pesch
 */

public class Tcx2Format extends GpxFormat {
    private static final Logger log = Logger.getLogger(Tcx2Format.class.getName());

    public String getName() {
        return "Training Center Database 2 (*" + getExtension() + ")";
    }

    public String getExtension() {
        return ".tcx";
    }

    public boolean isSupportsWriting() {
        return false;
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
        return positions.size() > 0 ? new GpxRoute(this, RouteCharacteristics.Route, courseT.getName(), null, positions) : null;
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


    private GpxRoute process(ActivityLapT activityLapT) {
        List<GpxPosition> positions = new ArrayList<GpxPosition>();
        for (TrackT trackT : activityLapT.getTrack()) {
            positions.addAll(processTrack(trackT));
        }
        return new GpxRoute(this, RouteCharacteristics.Track, activityLapT.getNotes(), null, positions);
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
        for (CourseLapT courseLapT : courseT.getLap()) {
            result.add(processCourseLap(courseT.getName(), courseLapT));
        }
        List<GpxPosition> positions = new ArrayList<GpxPosition>();
        for (TrackT trackT : courseT.getTrack()) {
            positions.addAll(processTrack(trackT));
        }
        result.add(new GpxRoute(this, RouteCharacteristics.Track, courseT.getName(), null, positions));
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

    public void write(GpxRoute route, OutputStream target, int startIndex, int endIndex) throws IOException {
        throw new UnsupportedOperationException();
    }

    public void write(List<GpxRoute> routes, OutputStream target) throws IOException {
        throw new UnsupportedOperationException();
    }
}