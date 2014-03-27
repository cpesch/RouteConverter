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

import org.apache.http.client.methods.HttpHead;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import static java.util.Locale.US;
import static org.apache.http.HttpHeaders.ACCEPT_RANGES;
import static org.apache.http.HttpHeaders.CONTENT_LENGTH;
import static org.apache.http.HttpHeaders.IF_MODIFIED_SINCE;
import static org.apache.http.HttpHeaders.LAST_MODIFIED;
import static slash.common.io.Transfer.parseLong;

/**
 * Wrapper to initiate an HTTP HEAD Request.
 *
 * @author Christian Pesch
 */

public class Head extends HttpRequest {
    private static final String RFC822_DATE = "EEE, dd MMM yyyy HH:mm:ss Z";

    public Head(String url) {
        super(new HttpHead(url));
        // avoid Content-Length: for GZIP'ed file
        disableContentCompression();
    }

    private DateFormat createDateFormat() {
        return new SimpleDateFormat(RFC822_DATE, US);
    }

    public void setIfModifiedSince(long modifiedSince) {
        String ifModifiedSince = createDateFormat().format(new Date(modifiedSince));
        setHeader(IF_MODIFIED_SINCE, ifModifiedSince);
    }

    public boolean getAcceptByteRanges() throws IOException {
        return "bytes".equals(getHeader(ACCEPT_RANGES));
    }

    public Long getContentLength() throws IOException {
        return parseLong(getHeader(CONTENT_LENGTH));
    }

    public Long getLastModified() throws IOException {
        String lastModified = getHeader(LAST_MODIFIED);
        try {
            Date date = createDateFormat().parse(lastModified);
            return date.getTime();
        } catch (ParseException e) {
            throw new IOException("Cannot parse last modified: " + lastModified, e);
        }
    }
}
