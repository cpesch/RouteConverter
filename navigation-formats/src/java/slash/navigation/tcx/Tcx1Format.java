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
import slash.navigation.MultipleRoutesFormat;
import slash.navigation.tcx.binding1.*;

import javax.xml.bind.JAXBException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.List;
import java.util.ArrayList;
import java.util.Collection;
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


    private List<TcxRoute> process(CourseFolderT courseFolderT) {
        List<TcxRoute> result = new ArrayList<TcxRoute>();
        for(CourseFolderT t : courseFolderT.getFolder())
            result.addAll(process(t));

        for(CourseT course : courseFolderT.getCourse()) {
            // TODO process Course
            course.getName();
        }

        return result;
    }

    private List<TcxRoute> process(TrainingCenterDatabaseT trainingCenterDatabaseT) {
        List<TcxRoute> result = new ArrayList<TcxRoute>();

        // TrainingCenterDatabase -> Courses -> CourseFolder -> Course -> CoursePoint -> Position
        // TrainingCenterDatabase -> Courses -> CourseFolder -> CourseFolder -> Course -> CoursePoint -> Position
        // TrainingCenterDatabase -> Courses -> CourseFolder -> Course -> Lap -> BeginPosition
        // TrainingCenterDatabase -> Courses -> CourseFolder -> Course -> Lap -> EndPosition
        // TrainingCenterDatabase -> Courses -> CourseFolder -> Course -> Track -> TrackPoint -> Position
        result.addAll(process(trainingCenterDatabaseT.getCourses().getCourseFolder()));
        trainingCenterDatabaseT.getCourses().getCourseFolder().getCourse().get(0).getCoursePoint().get(0).getPosition();
        trainingCenterDatabaseT.getCourses().getCourseFolder().getFolder().get(0).getCourse().get(0).getCoursePoint().get(0).getPosition();
        trainingCenterDatabaseT.getCourses().getCourseFolder().getCourse().get(0).getLap().get(0).getBeginPosition();
        trainingCenterDatabaseT.getCourses().getCourseFolder().getCourse().get(0).getLap().get(0).getEndPosition();
        trainingCenterDatabaseT.getCourses().getCourseFolder().getCourse().get(0).getTrack().get(0).getTrackpoint().get(0).getPosition();

        // TrainingCenterDatabase -> History -> Biking -> Run -> ActivityLap -> Track -> TrackPoint -> Position
        // TrainingCenterDatabase -> History -> Biking -> HistoryFolder -> Run -> ActivityLap -> Track -> TrackPoint -> Position
        // TrainingCenterDatabase -> History -> Other -> Run -> ActivityLap -> Track -> TrackPoint -> Position
        // TrainingCenterDatabase -> History -> Other -> HistoryFolder -> Run -> ActivityLap -> Track -> TrackPoint -> Position
        // TrainingCenterDatabase -> History -> Running -> Run -> ActivityLap -> Track -> TrackPoint -> Position
        // TrainingCenterDatabase -> History -> Running -> HistoryFolder -> Run -> ActivityLap -> Track -> TrackPoint -> Position
        trainingCenterDatabaseT.getHistory().getBiking().getRun().get(0).getLap().get(0).getTrack().get(0).getTrackpoint().get(0).getPosition();
        trainingCenterDatabaseT.getHistory().getBiking().getFolder().get(0).getRun().get(0).getLap().get(0).getTrack().get(0).getTrackpoint().get(0).getPosition();
        trainingCenterDatabaseT.getHistory().getOther().getRun().get(0).getLap().get(0).getTrack().get(0).getTrackpoint().get(0).getPosition();
        trainingCenterDatabaseT.getHistory().getOther().getFolder().get(0).getRun().get(0).getLap().get(0).getTrack().get(0).getTrackpoint().get(0).getPosition();
        trainingCenterDatabaseT.getHistory().getRunning().getRun().get(0).getLap().get(0).getTrack().get(0).getTrackpoint().get(0).getPosition();
        trainingCenterDatabaseT.getHistory().getRunning().getFolder().get(0).getRun().get(0).getLap().get(0).getTrack().get(0).getTrackpoint().get(0).getPosition();

        // TrainingCenterDatabase -> History -> MultiSport -> MultiSportSession -> FirstSport -> Run -> ActivityLap -> Track -> TrackPoint -> Position
        // TrainingCenterDatabase -> History -> MultiSport -> MultiSportSession -> NextSport -> Run -> ActivityLap -> Track -> TrackPoint -> Position
        // TrainingCenterDatabase -> History -> MultiSport -> MultiSportSession -> NextSport -> Transition -> Track -> TrackPoint -> Position
        trainingCenterDatabaseT.getHistory().getMultiSport().getMultiSportSession().get(0).getFirstSport().getRun().getLap().get(0).getTrack().get(0).getTrackpoint().get(0).getPosition();
        trainingCenterDatabaseT.getHistory().getMultiSport().getMultiSportSession().get(0).getNextSport().get(0).getRun().getLap().get(0).getTrack().get(0).getTrackpoint().get(0).getPosition();
        trainingCenterDatabaseT.getHistory().getMultiSport().getMultiSportSession().get(0).getNextSport().get(0).getTransition().getTrack().get(0).getTrackpoint().get(0).getPosition();

        return result;
    }

    public List<TcxRoute> read(InputStream source, Calendar startDate) throws IOException {
        try {
            TrainingCenterDatabaseT trainingCenterDatabase = TcxUtil.unmarshal1(source);
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