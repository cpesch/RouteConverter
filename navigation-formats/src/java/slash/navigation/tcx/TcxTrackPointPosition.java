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

import slash.navigation.tcx.binding2.TrackpointT;

import java.util.Calendar;

/**
 * Represents a track position in a Training Center Database (.tcx) file.
 *
 * @author Christian Pesch
 */

class TcxTrackPointPosition extends TcxPosition {
    private TrackpointT trackPoint;

    TcxTrackPointPosition(TrackpointT trackPoint) {
        this.trackPoint = trackPoint;
    }

    public Double getLongitude() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void setLongitude(Double longitude) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public Double getLatitude() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void setLatitude(Double latitude) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public Double getElevation() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void setElevation(Double elevation) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public Calendar getTime() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public String getComment() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void setComment(String comment) {
        //To change body of implemented methods use File | Settings | File Templates.
    }


}
