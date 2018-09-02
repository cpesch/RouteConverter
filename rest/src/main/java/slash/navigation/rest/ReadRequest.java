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

import org.apache.http.client.methods.HttpRequestBase;

import java.io.IOException;
import java.text.ParseException;
import java.util.Calendar;

import static org.apache.http.HttpHeaders.ACCEPT_RANGES;
import static org.apache.http.HttpHeaders.CONTENT_LENGTH;
import static org.apache.http.HttpHeaders.ETAG;
import static org.apache.http.HttpHeaders.IF_MODIFIED_SINCE;
import static org.apache.http.HttpHeaders.IF_NONE_MATCH;
import static org.apache.http.HttpHeaders.LAST_MODIFIED;
import static slash.common.io.Transfer.parseLong;
import static slash.navigation.rest.RFC2616.formatDate;
import static slash.navigation.rest.RFC2616.parseDate;

/**
 * Wrapper for a HTTP HEAD or GET Request.
 *
 * @author Christian Pesch
 */

abstract class ReadRequest extends HttpRequest {
    public ReadRequest(HttpRequestBase method) {
        super(method);
    }

    public boolean getAcceptByteRanges() throws IOException {
        return "bytes".equals(getHeader(ACCEPT_RANGES));
    }

    public Long getContentLength() throws IOException {
        return parseLong(getHeader(CONTENT_LENGTH));
    }

    public Long getLastModified() throws IOException {
        String lastModified = getHeader(LAST_MODIFIED);
        if(lastModified == null)
            return null;

        try {
            Calendar calendar = parseDate(lastModified);
            return calendar.getTimeInMillis();
        } catch (ParseException e) {
            throw new IOException("Cannot parse last modified: " + lastModified, e);
        }
    }

    public String getETag() throws IOException {
        return getHeader(ETAG);
    }


    public void setIfModifiedSince(long modifiedSince) {
        String ifModifiedSince = formatDate(modifiedSince);
        setHeader(IF_MODIFIED_SINCE, ifModifiedSince);
    }

    public void setIfNoneMatch(String eTag) {
        setHeader(IF_NONE_MATCH, eTag);
    }
}
