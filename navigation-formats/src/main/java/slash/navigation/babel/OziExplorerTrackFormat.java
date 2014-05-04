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

package slash.navigation.babel;

import slash.navigation.base.RouteCharacteristics;

import static slash.navigation.base.RouteCharacteristics.Track;

/**
 * Writes OziExplorer Track (.plt) files.
 *
 * @author Christian Pesch
 */

public class OziExplorerTrackFormat extends OziExplorerFormat {
    public String getExtension() {
        return ".plt";
    }

    public String getName() {
        return "OziExplorer Track (*" + getExtension() + ")";
    }

    protected String[] getGlobalOptions() {
        return new String[]{"-t"};
    }

    public boolean isSupportsMultipleRoutes() {
        return false;
    }

    protected RouteCharacteristics getRouteCharacteristics() {
        return Track;
    }
}