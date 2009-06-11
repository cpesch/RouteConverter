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

package slash.navigation.itn;

import slash.navigation.util.CompactCalendar;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * Reads and writes Tom Tom 8 Route (.itn) files.
 *
 * @author Christian Pesch
 */

public class TomTom8RouteFormat extends TomTomRouteFormat {

    public String getName() {
        return "Tom Tom 8 Route (*" + getExtension() + ")";
    }

    public List<TomTomRoute> read(InputStream source, CompactCalendar startDate) throws IOException {
        return read(source, startDate, UTF8_ENCODING);
    }

    protected boolean isIso885915ButReadWithUtf8(String string) {
        if (string != null) {
            for (int i = 0; i < string.length(); i++) {
                char c = string.charAt(i);
                if (c == '\ufffd')
                    return true;
            }
        }
        return false;
    }

    public void write(TomTomRoute route, File target, int startIndex, int endIndex) throws IOException {
        write(route, target, UTF8_ENCODING, startIndex, endIndex);
    }
}