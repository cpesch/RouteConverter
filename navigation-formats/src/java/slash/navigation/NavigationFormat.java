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

package slash.navigation;

import slash.navigation.util.CompactCalendar;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.List;

/**
 * A navigation format.
 *
 * @author Christian Pesch
 */

public interface NavigationFormat<R extends BaseRoute> {
    String getName();
    String getExtension();
    int getMaximumFileNameLength();
    int getMaximumPositionCount();
    boolean isSupportsReading();
    boolean isSupportsWriting();
    boolean isSupportsMultipleRoutes();

    <P extends BaseNavigationPosition> R createRoute(RouteCharacteristics characteristics, String name, List<P> positions);

    List<R> read(InputStream source) throws IOException;
    List<R> read(InputStream source, CompactCalendar startDate) throws IOException;
    void write(R route, File target, int startIndex, int endIndex, boolean numberPositionNames) throws IOException;
}
