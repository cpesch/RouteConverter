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

package slash.navigation.base;

import slash.common.type.CompactCalendar;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.util.Calendar;

import static java.util.Calendar.DAY_OF_YEAR;
import static java.util.Calendar.YEAR;

/**
 * The base of all text based navigation formats.
 *
 * @author Christian Pesch
 */

public abstract class TextNavigationFormat<R extends BaseRoute> extends BaseNavigationFormat<R> {
    protected static final char BYTE_ORDER_MARK = '\ufeff';

    protected boolean isValidStartDate(CompactCalendar startDate) {
        if(startDate == null)
            return false;
        Calendar calendar = startDate.getCalendar();
        return !(calendar.get(YEAR) == 1970 && calendar.get(DAY_OF_YEAR) == 1);
    }

    public void read(InputStream source, CompactCalendar startDate, ParserContext<R> context) throws Exception {
        read(source, startDate, DEFAULT_ENCODING, context);
    }

    protected void read(InputStream source, CompactCalendar startDate, String encoding, ParserContext<R> context) throws IOException {
        Reader reader = new InputStreamReader(source, encoding);
        BufferedReader bufferedReader = new BufferedReader(reader);
        try {
            read(bufferedReader, startDate, encoding, context);
        }
        finally {
            reader.close();
        }
    }

    // encoding currently only used in GoogleMapsUrlFormat
    public abstract void read(BufferedReader reader, CompactCalendar startDate, String encoding, ParserContext<R> context) throws IOException;

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
