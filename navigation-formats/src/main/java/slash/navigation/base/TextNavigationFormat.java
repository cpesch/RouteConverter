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

import static slash.common.io.Transfer.ISO_LATIN1_ENCODING;

/**
 * The base of all text based navigation formats.
 *
 * @author Christian Pesch
 */

public abstract class TextNavigationFormat<R extends BaseRoute> extends BaseNavigationFormat<R> {
    protected static final char BYTE_ORDER_MARK = '\ufeff';

    protected boolean isValidStartDate(CompactCalendar startDate) {
        return startDate != null && startDate.hasDateDefined();
    }

    public void read(InputStream source, ParserContext<R> context) throws Exception {
        read(source, ISO_LATIN1_ENCODING, context);
    }

    protected void read(InputStream source, String encoding, ParserContext<R> context) throws IOException {
        try (Reader reader = new InputStreamReader(source, encoding)) {
            read(new BufferedReader(reader), encoding, context);
        }
    }

    // encoding currently only used in GoogleMapsUrlFormat
    public abstract void read(BufferedReader reader, String encoding, ParserContext<R> context) throws IOException;

    protected void write(R route, OutputStream target, String encoding, int startIndex, int endIndex) throws IOException {
        try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(target, encoding))) {
            write(route, writer, startIndex, endIndex);
        }
    }

    public void write(R route, OutputStream target, int startIndex, int endIndex) throws IOException {
        write(route, target, ISO_LATIN1_ENCODING, startIndex, endIndex);
    }

    public abstract void write(R route, PrintWriter writer, int startIndex, int endIndex) throws IOException;

}
