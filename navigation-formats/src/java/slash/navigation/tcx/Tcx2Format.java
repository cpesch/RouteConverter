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
import slash.navigation.tcx.binding2.*;

import javax.xml.bind.JAXBException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.logging.Logger;

/**
 * Reads and writes Training Center Database 2 (.tcx) files.
 *
 * @author Christian Pesch
 */

public class Tcx2Format extends TcxFormat {
    private static final Logger log = Logger.getLogger(Tcx2Format.class.getName());

    public String getName() {
        return "Training Center Database 2 (*" + getExtension() + ")";
    }


    private TcxRoute process(TrackT trackT) {
        List<TcxPosition> positions = new ArrayList<TcxPosition>();
        for(TrackpointT trackpointT : trackT.getTrackpoint()) {
            Double longitude = trackpointT.getPosition() != null ? trackpointT.getPosition().getLongitudeDegrees() : null;
            Double latitude = trackpointT.getPosition() != null ? trackpointT.getPosition().getLatitudeDegrees() : null;
            positions.add(new TcxPosition(longitude, latitude, trackpointT.getAltitudeMeters(), parseTime(trackpointT.getTime()), null));
        }
        return new TcxRoute(this, RouteCharacteristics.Track, positions);
    }

    private TcxRoute process(CourseLapT courseLapT) {
        List<TcxPosition> positions = new ArrayList<TcxPosition>();
        positions.add(new TcxPosition(courseLapT.getBeginPosition().getLongitudeDegrees(),
                                      courseLapT.getBeginPosition().getLatitudeDegrees(),
                                      courseLapT.getBeginAltitudeMeters(),
                                      null,
                                      "0 seconds"));
        positions.add(new TcxPosition(courseLapT.getEndPosition().getLongitudeDegrees(),
                                      courseLapT.getEndPosition().getLatitudeDegrees(),
                                      courseLapT.getEndAltitudeMeters(),
                                      null,
                                      courseLapT.getTotalTimeSeconds() + " seconds"));
        return new TcxRoute(this, RouteCharacteristics.Track, positions);
    }

    private List<TcxRoute> process(ActivityLapT activityLapT) {
        List<TcxRoute> result = new ArrayList<TcxRoute>();
        for (TrackT trackT : activityLapT.getTrack()) {
            result.add(process(trackT));
        }
        return result;
    }

    private List<TcxRoute> process(ActivityT activityT) {
        List<TcxRoute> result = new ArrayList<TcxRoute>();
        for (ActivityLapT activityLapT : activityT.getLap()) {
            result.addAll(process(activityLapT));
        }
        return result;
    }

    private TcxRoute processCoursePoints(List<CoursePointT> coursePointTs) {
        List<TcxPosition> positions = new ArrayList<TcxPosition>();
        for(CoursePointT coursePointT : coursePointTs) {
            positions.add(new TcxPosition(coursePointT.getPosition().getLongitudeDegrees(),
                                          coursePointT.getPosition().getLatitudeDegrees(),
                                          coursePointT.getAltitudeMeters(),
                                          parseTime(coursePointT.getTime()),
                                          coursePointT.getName()));
        }
        return new TcxRoute(this, RouteCharacteristics.Route, positions);
    }

    private List<TcxRoute> processCourseLaps(List<CourseLapT> courseLapTs) {
        List<TcxRoute> result = new ArrayList<TcxRoute>();
        for(CourseLapT courseLapT : courseLapTs) {
            result.add(process(courseLapT));
        }
        return result;
    }

    private List<TcxRoute> process(TrainingCenterDatabaseT trainingCenterDatabaseT) {
        List<TcxRoute> result = new ArrayList<TcxRoute>();

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
                    ActivityT activityT = nextSportT.getActivity();
                    result.addAll(process(activityT));
                    ActivityLapT transition = nextSportT.getTransition();
                    result.addAll(process(transition));
                }
            }
        }

        // TrainingCenterDatabase -> CourseList -> Course -> CoursePoint -> Position
        // TrainingCenterDatabase -> CourseList -> Course -> CourseLap -> BeginPosition/EndPosition
        // TrainingCenterDatabase -> CourseList -> Course -> Track -> TrackPoint -> Position
        CourseListT courseListT = trainingCenterDatabaseT.getCourses();
        if (courseListT != null)
            for (CourseT courseT : courseListT.getCourse()) {
                result.add(processCoursePoints(courseT.getCoursePoint()));
                result.addAll(processCourseLaps(courseT.getLap()));
                for(TrackT trackT : courseT.getTrack()) {
                    result.add(process(trackT));
                }
            }
        return result;
    }

    public List<TcxRoute> read(InputStream source, Calendar startDate) throws IOException {
        try {
            TrainingCenterDatabaseT trainingCenterDatabase = TcxUtil.unmarshal2(source);
            List<TcxRoute> result = process(trainingCenterDatabase);
            return result.size() > 0 ? result : null;
        } catch (JAXBException e) {
            log.fine("Error reading " + source + ": " + e.getMessage());
            return null;
        }
    }

    public void write(TcxRoute route, File target, int startIndex, int endIndex, boolean numberPositionNames) throws IOException {
        throw new UnsupportedOperationException();
    }
}