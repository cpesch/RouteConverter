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

import slash.navigation.XmlNavigationFormat;
import slash.navigation.BaseNavigationPosition;
import slash.navigation.RouteCharacteristics;

import java.util.List;

/**
 * The base of all Training Center Database formats.
 *
 * @author Christian Pesch
 */

public abstract class TcxFormat extends XmlNavigationFormat<TcxRoute> {

    public String getExtension() {
        return ".tcx";
    }

    public int getMaximumPositionCount() {
        return UNLIMITED_MAXIMUM_POSITION_COUNT;
    }

    public boolean isSupportsWriting() {
        return false;
    }

    public boolean isSupportsMultipleRoutes() {
        return true;
    }

    public <P extends BaseNavigationPosition> TcxRoute createRoute(RouteCharacteristics characteristics, String name, List<P> positions) {
        return new TcxRoute(this, (List<TcxPosition>) positions);
    }
}
