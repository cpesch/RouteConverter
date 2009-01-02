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
    along with Foobar; if not, write to the Free Software
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
import java.util.ArrayList;
import java.util.List;
import java.util.Calendar;

/**
 * Reads and writes Training Center Database (.tcx) files.
 *
 * @author Christian Pesch
 */

public class TcxFormat extends XmlNavigationFormat<TcxRoute> {

    public String getExtension() {
        return ".tcx";
    }

    public String getName() {
        return "Training Center Database (*" + getExtension() + ")";
    }

    public int getMaximumPositionCount() {
        return UNLIMITED_MAXIMUM_POSITION_COUNT;
    }

    public boolean isSupportsMultipleRoutes() {
        return true;
    }

    public <P extends BaseNavigationPosition> TcxRoute createRoute(RouteCharacteristics characteristics, String name, List<P> positions) {
        return new TcxRoute(null/*TODO*/, (List<TcxTrackPointPosition>) positions);
    }

    private TcxRoute process(ActivityT activityT, TrackT trackT) {
        List<TcxTrackPointPosition> positions = new ArrayList<TcxTrackPointPosition>();
        for(TrackpointT trackpointT : trackT.getTrackpoint()) {
            positions.add(new TcxTrackPointPosition(trackpointT));
        }
        return new TcxRoute(activityT, positions); 
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

    private TcxRouteList process(TrainingCenterDatabaseT trainingCenterDatabaseT) {
        TcxRouteList result = new TcxRouteList(trainingCenterDatabaseT);
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
                multiSportSessionT.getFirstSport().getActivity(); // TODO
                for (NextSportT nextSportT : multiSportSessionT.getNextSport()) {
                    nextSportT.getActivity(); // TODO
                    nextSportT.getTransition(); // TODO
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

    public List<TcxRoute> read(File source, Calendar startDate) throws IOException {
        try {
            TrainingCenterDatabaseT trainingCenterDatabase = TcxUtil.unmarshal(source);
            List<TcxRoute> result = process(trainingCenterDatabase);
            return result.size() > 0 ? result : null;
        } catch (JAXBException e) {
            return null;
        }
    }


    private TrainingCenterDatabaseT createTcx(TcxRoute route) {
        return null; // TODO
    }

    public void write(TcxRoute route, File target, int startIndex, int endIndex, boolean numberPositionNames) throws IOException {
        try {
            TcxUtil.marshal(createTcx(route), target);
        } catch (JAXBException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
