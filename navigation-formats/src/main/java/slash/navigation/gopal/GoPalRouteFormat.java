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

package slash.navigation.gopal;

import slash.navigation.base.BaseRoute;
import slash.navigation.base.XmlNavigationFormat;

import java.util.prefs.Preferences;

/**
 * The base of all GoPal Route formats.
 *
 * @author Christian Pesch
 */

public abstract class GoPalRouteFormat<R extends BaseRoute> extends XmlNavigationFormat<R> {
    protected static final Preferences preferences = Preferences.userNodeForPackage(GoPalRouteFormat.class);

    public String getExtension() {
        return ".xml";
    }

    public int getMaximumPositionCount() {
        return preferences.getInt(getVersion() + "MaximumPositionCount", UNLIMITED_MAXIMUM_POSITION_COUNT);
    }

    protected abstract String getVersion();

    public boolean isSupportsMultipleRoutes() {
        return false;
    }

    public boolean isWritingRouteCharacteristics() {
        return false;
    }
}
