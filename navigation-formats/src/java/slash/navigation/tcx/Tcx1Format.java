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
import slash.navigation.tcx.binding1.*;

import javax.xml.bind.JAXBException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
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

   

    private List<TcxRoute> process(TrainingCenterDatabaseT trainingCenterDatabaseT) {
        return new ArrayList<TcxRoute>(); // TODO
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


    private TrainingCenterDatabaseT createTcx(TcxRoute route) {
        return null; // TODO
    }

    public void write(TcxRoute route, File target, int startIndex, int endIndex, boolean numberPositionNames) throws IOException {
        try {
            TcxUtil.marshal1(createTcx(route), target);
        } catch (JAXBException e) {
            throw new IllegalArgumentException(e);
        }
    }
}