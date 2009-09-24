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
package slash.navigation.simple;

import slash.navigation.SimpleRoute;
import slash.navigation.Wgs84Position;
import slash.navigation.util.CompactCalendar;
import slash.navigation.util.Conversion;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.File;
import java.util.List;

/**
 * Reads and writes Sygic POI Unicode (.txt) files.
 * <p/>
 * Standard Header:
 * ; unicode<br/>
 * ; Created from User Poi file sample.upi<br/>
 * ; longitude    latitude    name    phone<br/>
 * Standard Format: 2.324360	48.826760	Rue Antoine Chantin(14ème Arrondissement Paris), Paris	+123456789
 *
 * @author Christian Pesch
 */

public class SygicUnicodeFormat extends SygicFormat {

    public String getName() {
        return "Sygic POI Unicode (*" + getExtension() + ")";
    }

    public List<SimpleRoute> read(InputStream source, CompactCalendar startDate) throws IOException {
        return read(source, startDate, UTF16_ENCODING);
    }

    public void write(SimpleRoute route, File target, int startIndex, int endIndex) throws IOException {
        write(route, target, UTF16LE_ENCODING, startIndex, endIndex);
    }

    private static String formatForSygic(String string) {
        return string != null ? string.replaceAll(TAB, "    ") : "";
    }

    protected void writeHeader(PrintWriter writer) {
        // with UTF-16LE no BOM is written, UnicodeLittle would write one by is not supported
        // (see http://java.sun.com/j2se/1.4.2/docs/guide/intl/encoding.doc.html)
        // but the fix from http://mindprod.com/jgloss/encoding.html helped me
        writer.write('\ufeff');
        writer.println("; unicode");
        writer.println("; " + GENERATED_BY);
        writer.println("; longitude    latitude    name    phone");
        writer.println();
    }

    protected void writePosition(Wgs84Position position, PrintWriter writer, int index, boolean firstPosition) {
        String longitude = Conversion.formatDoubleAsString(position.getLongitude(), 6);
        String latitude = Conversion.formatDoubleAsString(position.getLatitude(), 6);
        String comment = formatForSygic(position.getComment());
        String phone = null;
        int plus = comment.lastIndexOf('+');
        if (plus != -1) {
            phone = comment.substring(plus);
            comment = comment.substring(0, plus - 1);
        }
        writer.println(longitude + TAB + latitude + TAB + comment + (phone != null ? TAB + phone : ""));
    }

    protected void writeFooter(PrintWriter writer, int positionCount) {
        writer.println();
        writer.println("; number of written points " + positionCount);
    }
}