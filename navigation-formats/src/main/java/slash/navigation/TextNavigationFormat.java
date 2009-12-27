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

import slash.common.io.CompactCalendar;

import java.io.*;
import java.util.Calendar;
import java.util.List;

/**
 * The base of all text based navigation formats.
 *
 * @author Christian Pesch
 */

public abstract class TextNavigationFormat<R extends BaseRoute> extends BaseNavigationFormat<R> {

    protected boolean isValidStartDate(CompactCalendar startDate) {
        if(startDate == null)
            return false;
        Calendar calendar = startDate.getCalendar();
        return !(calendar.get(Calendar.YEAR) == 1970 && calendar.get(Calendar.DAY_OF_YEAR) == 1);
    }

    public List<R> read(InputStream source, CompactCalendar startDate) throws IOException {
        return read(source, startDate, DEFAULT_ENCODING);
    }

    protected List<R> read(InputStream source, CompactCalendar startDate, String encoding) throws IOException {
        Reader reader = new InputStreamReader(source, encoding);
        BufferedReader bufferedReader = new BufferedReader(reader);
        try {
            return read(bufferedReader, startDate, encoding);
        }
        finally {
            reader.close();
        }
    }

    // encoding currently only used in GoogleMapsFormat
    public abstract List<R> read(BufferedReader reader, CompactCalendar startDate, String encoding) throws IOException;

    protected void write(R route, OutputStream target, String encoding, int startIndex, int endIndex) throws IOException {
        PrintWriter writer = new PrintWriter(new OutputStreamWriter(target, encoding));
        try {
            write(route, writer, startIndex, endIndex);
        }
        finally {
            writer.flush();
            writer.close();
        }
    }

    public void write(R route, OutputStream target, int startIndex, int endIndex) throws IOException {
        write(route, target, DEFAULT_ENCODING, startIndex, endIndex);
    }

    public abstract void write(R route, PrintWriter writer, int startIndex, int endIndex) throws IOException;

}
