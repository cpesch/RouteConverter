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
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import static java.util.Locale.US;
import static org.apache.http.HttpHeaders.*;
import static slash.common.io.Transfer.parseLong;

/**
 * Wrapper for a HTTP HEAD or GET Request.
 *
 * @author Christian Pesch
 */

abstract class ReadRequest extends HttpRequest {
    private static final String RFC1123_DATE = "EEE, dd MMM yyyy HH:mm:ss z";
    private static final TimeZone GMT = TimeZone.getTimeZone("GMT");

    public ReadRequest(HttpRequestBase method) {
        super(method);
    }

    public ReadRequest(HttpRequestBase method, Credentials credentials) {
        super(method, credentials);
    }

    private DateFormat createDateFormat() {
        SimpleDateFormat format = new SimpleDateFormat(RFC1123_DATE, US);
        // django.utils.http.RFC1123_DATE expects GMT
        format.setTimeZone(GMT);
        return format;
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
            Date date = createDateFormat().parse(lastModified);
            return date.getTime();
        } catch (ParseException e) {
            throw new IOException("Cannot parse last modified: " + lastModified, e);
        }
    }

    public String getETag() throws IOException {
        return getHeader(ETAG);
    }


    public void setIfModifiedSince(long modifiedSince) {
        String ifModifiedSince = createDateFormat().format(new Date(modifiedSince));
        setHeader(IF_MODIFIED_SINCE, ifModifiedSince);
    }

    public void setIfNoneMatch(String eTag) {
        setHeader(IF_NONE_MATCH, eTag);
    }
}
