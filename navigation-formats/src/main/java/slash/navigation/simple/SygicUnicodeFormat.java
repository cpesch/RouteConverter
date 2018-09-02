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

import slash.navigation.base.ParserContext;
import slash.navigation.base.SimpleRoute;
import slash.navigation.base.Wgs84Position;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;

import static slash.common.io.Transfer.UTF16LE_ENCODING;
import static slash.common.io.Transfer.UTF16_ENCODING;
import static slash.common.io.Transfer.formatDoubleAsString;

/**
 * Reads and writes Sygic POI Unicode (.txt) files.
 *
 * Standard Header:
 * ; unicode
 * ; Created from User Poi file sample.upi
 * ; longitude    latitude    name    phone
 * Standard Format: 2.324360	48.826760	Rue Antoine Chantin(14eme Arrondissement Paris), Paris	+123456789
 *
 * @author Christian Pesch
 */

public class SygicUnicodeFormat extends SygicFormat {

    public String getName() {
        return "Sygic POI Unicode (*" + getExtension() + ")";
    }

    public void read(InputStream source, ParserContext<SimpleRoute> context) throws Exception {
        read(source, UTF16_ENCODING, context);
    }

    public void write(SimpleRoute route, OutputStream target, int startIndex, int endIndex) throws IOException {
        write(route, target, UTF16LE_ENCODING, startIndex, endIndex);
    }

    protected void writeHeader(PrintWriter writer, SimpleRoute route) {
        // with UTF-16LE no BOM is written, UnicodeLittle would write one by is not supported
        // (see http://java.sun.com/j2se/1.4.2/docs/guide/intl/encoding.doc.html)
        // but the fix from http://mindprod.com/jgloss/encoding.html helped me
        writer.write(BYTE_ORDER_MARK);
        writer.println("; unicode");
        writer.println("; " + GENERATED_BY);
        writer.println("; longitude    latitude    name    phone");
        writer.println();
    }

    private static String escape(String string) {
        return string != null ? string.replaceAll(TAB, "    ") : "";
    }

    protected void writePosition(Wgs84Position position, PrintWriter writer, int index, boolean firstPosition) {
        String longitude = formatDoubleAsString(position.getLongitude(), 6);
        String latitude = formatDoubleAsString(position.getLatitude(), 6);
        String description = escape(position.getDescription());
        String phone = null;
        int plus = description.lastIndexOf('+');
        if (plus != -1) {
            phone = description.substring(plus);
            description = description.substring(0, plus - 1);
        }
        writer.println(longitude + TAB + latitude + TAB + description + (phone != null ? TAB + phone : ""));
    }

    protected void writeFooter(PrintWriter writer, int positionCount) {
        writer.println();
        writer.println("; number of written points " + positionCount);
    }
}