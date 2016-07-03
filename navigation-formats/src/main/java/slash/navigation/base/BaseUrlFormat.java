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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;

import static slash.common.io.Transfer.UTF8_ENCODING;

/**
 * The base of all URL based navigation formats.
 *
 * @author Christian Pesch
 */

public abstract class BaseUrlFormat extends SimpleFormat<Wgs84Route> {
    private static final int READ_BUFFER_SIZE = 1024 * 1024;

    public void read(InputStream source, ParserContext<Wgs84Route> context) throws Exception {
        // used to be a UTF-8 then ISO-8859-1 fallback style
        read(source, UTF8_ENCODING, context);
    }

    public void read(BufferedReader reader, String encoding, ParserContext<Wgs84Route> context) throws IOException {
        StringBuilder buffer = new StringBuilder();

        while (buffer.length() < READ_BUFFER_SIZE) {
            String line = reader.readLine();
            if (line == null)
                break;
            buffer.append(line).append("\n");
        }

        String url = findURL(buffer.toString());
        if (url != null)
            processURL(url, encoding, context);
    }

    protected abstract String findURL(String text);
    protected abstract void processURL(String url, String encoding, ParserContext<Wgs84Route> context) throws IOException;

    protected static String replaceLineFeeds(String text, String replaceWith) {
        return text.replaceAll("[\n|\r]", replaceWith).replaceAll("&amp;", "&");
    }
}
