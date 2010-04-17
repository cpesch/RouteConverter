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

package slash.navigation.converter.gui.services;

import slash.navigation.base.NavigationFormat;
import slash.navigation.base.BaseRoute;
import slash.navigation.base.RouteCharacteristics;
import slash.common.io.CompactCalendar;

import java.util.List;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Used in the save combobox to indicate storage in a RouteService.
 *
 * @author Christian Pesch
 */

public class RouteServiceNavigationFormat implements NavigationFormat {
    public String getName() {
        return "Upload to RouteService";
    }

    public String getExtension() {
        throw new UnsupportedOperationException();
    }

    public int getMaximumFileNameLength() {
        throw new UnsupportedOperationException();
    }

    public int getMaximumRouteNameLength() {
        throw new UnsupportedOperationException();
    }

    public int getMaximumPositionCount() {
        throw new UnsupportedOperationException();
    }

    public boolean isSupportsReading() {
        throw new UnsupportedOperationException();
    }

    public boolean isSupportsWriting() {
        throw new UnsupportedOperationException();
    }

    public boolean isSupportsMultipleRoutes() {
        throw new UnsupportedOperationException();
    }

    public BaseRoute createRoute(RouteCharacteristics characteristics, String name, List positions) {
        throw new UnsupportedOperationException();
    }

    public List read(InputStream source) throws IOException {
        throw new UnsupportedOperationException();
    }

    public List read(InputStream source, CompactCalendar startDate) throws IOException {
        throw new UnsupportedOperationException();
    }

    public void write(BaseRoute route, OutputStream target, int startIndex, int endIndex) throws IOException {
        throw new UnsupportedOperationException();
    }
}
