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
package slash.navigation.rest;

import org.apache.http.client.methods.HttpGet;

import static org.apache.http.HttpHeaders.RANGE;

/**
 * Wrapper to initiate an HTTP GET Request.
 *
 * @author Christian Pesch
 */

public class Get extends ReadRequest {
    public Get(String url) {
        super(new HttpGet(url));
    }

    public void setRange(long startIndex, Long endIndex) {
        // avoid GZIP'ed range
        disableContentCompression();
        // Apache accepts just bytes=1234-1235 while the spec says bytes 1234-1235/1236
        setHeader(RANGE, "bytes=" + startIndex + "-" + (endIndex != null ? endIndex : ""));
    }
}
