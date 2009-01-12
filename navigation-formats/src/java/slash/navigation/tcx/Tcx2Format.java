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

import slash.navigation.BaseNavigationPosition;
import slash.navigation.RouteCharacteristics;
import slash.navigation.XmlNavigationFormat;
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


    private TcxRoute process(ActivityT activityT, TrackT trackT) {
        List<TcxPosition> positions = new ArrayList<TcxPosition>();
        for(TrackpointT trackpointT : trackT.getTrackpoint()) {
            positions.add(null); // TODO new TcxPosition(trackpointT));
        }
        return null; // TODO new TcxRoute(activityT, positions);
    }

    private List<TcxRoute> process(ActivityT activityT, ActivityLapT activityLapT) {
        List<TcxRoute> result = new ArrayList<TcxRoute>();
        for (TrackT trackT : activityLapT.getTrack()) {
            result.add(process(activityT, trackT));
        }
        return result;
    }

    private List<TcxRoute> process(ActivityListT activityListT) {
        List<TcxRoute> result = new ArrayList<TcxRoute>();
        for (ActivityT activityT : activityListT.getActivity()) {
            for(ActivityLapT activityLapT : activityT.getLap()) {
                result.addAll(process(activityT, activityLapT));
            }
        }
        activityListT.getMultiSportSession(); // TODO
        return result;
    }

    private List<TcxRoute> process(TrainingCenterDatabaseT trainingCenterDatabaseT) {
        List<TcxRoute> result = new ArrayList<TcxRoute>();
        ActivityListT activityListT = trainingCenterDatabaseT.getActivities();
        if (activityListT != null) {
            // TrainingCenterDatabase -> ActivityList -> Activity -> ActivityLap -> Track -> TrackPoint -> Position
            for (ActivityT activityT : activityListT.getActivity()) {
                for (ActivityLapT activityLapT : activityT.getLap()) {
                    result.addAll(process(activityT, activityLapT));
                }
            }
            // TrainingCenterDatabase -> ActivityList -> MultiSportSession -> FirstSport -> Activity -> ActivityLap -> Track -> TrackPoint -> Position
            // TrainingCenterDatabase -> ActivityList -> MultiSportSession -> NextSport -> Activity -> ActivityLap -> Track -> TrackPoint -> Position
            // TrainingCenterDatabase -> ActivityList -> MultiSportSession -> NextSport -> ActivityLap -> Track -> TrackPoint -> Position
            for (MultiSportSessionT multiSportSessionT : activityListT.getMultiSportSession()) {
                ActivityT activity1 = multiSportSessionT.getFirstSport().getActivity();
                for (ActivityLapT activityLapT : activity1.getLap()) {
                    result.addAll(process(activity1, activityLapT));
                }
                for (NextSportT nextSportT : multiSportSessionT.getNextSport()) {
                    ActivityT activityT = nextSportT.getActivity();
                    for (ActivityLapT activityLapT : activityT.getLap()) {
                        result.addAll(process(activityT, activityLapT));
                    }
                    ActivityLapT transition = nextSportT.getTransition();
                    result.addAll(process(activityT /*TODO*/, transition));
                }
            }
        }

        // TrainingCenterDatabase -> CourseList -> Course -> CoursePoint -> Position
        // TrainingCenterDatabase -> CourseList -> Course -> CourseLap -> Position
        // TrainingCenterDatabase -> CourseList -> Course -> Track -> TrackPoint -> Position
        CourseListT courseListT = trainingCenterDatabaseT.getCourses();
        if (courseListT != null)
            for (CourseT courseT : courseListT.getCourse()) {
                courseT.getCoursePoint(); // TODO
                courseT.getLap(); // TODO
                courseT.getTrack(); // TODO
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


    private TrainingCenterDatabaseT createTcx(TcxRoute route) {
        return null; // TODO
    }

    public void write(TcxRoute route, File target, int startIndex, int endIndex, boolean numberPositionNames) throws IOException {
        try {
            TcxUtil.marshal2(createTcx(route), target);
        } catch (JAXBException e) {
            throw new IllegalArgumentException(e);
        }
    }
}