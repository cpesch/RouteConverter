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
package slash.navigation.download.tools.helpers;

import slash.navigation.rest.Get;

import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Logger;

import static java.lang.String.format;

/**
 * Reads a byte range of a URL with an HTTP {@code Range} request.
 *
 * @author Christian Pesch
 */
public class HttpRangeReader implements RangeReader {
    private static final Logger log = Logger.getLogger(HttpRangeReader.class.getName());
    private static final int SC_PARTIAL_CONTENT = 206;

    private final String url;

    public HttpRangeReader(String url) {
        this.url = url;
    }

    public byte[] read(long offset, int length) throws IOException {
        long end = offset + length - 1;
        Get request = new Get(url);
        request.setCacheControlNoCache();
        request.setRange(offset, end);
        byte[] result = request.execute(response -> {
            // a server without range support answers 200 with the whole resource: do not download it
            if (response.getCode() != SC_PARTIAL_CONTENT)
                throw new IOException(format("GET %d-%d for %s returned status code %d instead of %d",
                        offset, end, url, response.getCode(), SC_PARTIAL_CONTENT));
            try (InputStream inputStream = response.getEntity().getContent()) {
                return inputStream.readNBytes(length);
            }
        });
        if (result == null)
            throw new IOException(format("GET %d-%d for %s failed", offset, end, url));
        log.info(format("GET %d-%d for %s returned %d bytes", offset, end, url, result.length));
        return result;
    }
}
