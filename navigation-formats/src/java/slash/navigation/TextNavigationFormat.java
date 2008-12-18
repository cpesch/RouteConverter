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
    along with Foobar; if not, write to the Free Software
    Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA

    Copyright (C) 2007 Christian Pesch. All Rights Reserved.
*/

package slash.navigation;

import java.io.*;
import java.util.Calendar;
import java.util.List;

/**
 * The base of all text based navigation formats.
 *
 * @author Christian Pesch
 */

public abstract class TextNavigationFormat<R extends BaseRoute> extends BaseNavigationFormat<R> {
    protected Calendar startDate; // used by GopalTrackFormat, ItnFormat, KienzleGpsFormat and NmeaFormat

    protected boolean isStartDate(Calendar calendar) {
        return calendar != null && calendar.get(Calendar.YEAR) != 1970 && calendar.get(Calendar.DAY_OF_YEAR) != 1;
    }

    public List<R> read(File source) throws IOException {
        startDate = Calendar.getInstance();
        startDate.setTimeInMillis(source.lastModified());
        return read(source, DEFAULT_ENCODING);
    }

    protected List<R> read(File source, String encoding) throws IOException {
        Reader reader = new InputStreamReader(new FileInputStream(source), encoding);
        BufferedReader bufferedReader = new BufferedReader(reader);
        try {
            return read(bufferedReader, encoding);
        }
        finally {
            reader.close();
        }
    }

    public abstract List<R> read(BufferedReader reader, String encoding) throws IOException;

    protected void write(R route, File target, String encoding, int startIndex, int endIndex, boolean numberPositionNames) throws IOException {
        PrintWriter writer = new PrintWriter(target, encoding);
        try {
            write(route, writer, startIndex, endIndex, numberPositionNames);
        }
        finally {
            writer.flush();
            writer.close();
        }
    }

    public void write(R route, File target, int startIndex, int endIndex, boolean numberPositionNames) throws IOException {
        write(route, target, DEFAULT_ENCODING, startIndex, endIndex, numberPositionNames);
    }

    public abstract void write(R route, PrintWriter writer, int startIndex, int endIndex, boolean numberPositionNames);

}
