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

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.methods.GetMethod;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Wrapper to initiate an HTTP GET Request.
 *
 * @author Christian Pesch
 */

public class Get extends HttpRequest {
    private static final Pattern CONTENT_DISPOSITION_PATTERN = Pattern.compile(".*filename=\"(.+)\"");

    public Get(String url, Credentials credentials) {
        super(new GetMethod(url), credentials);
    }

    public Get(String url) {
        super(new GetMethod(url));
    }

    public String getContentDisposition() {
        Header header = method.getResponseHeader("Content-Disposition");
        return header != null ? header.getValue() : null;
    }

    @SuppressWarnings("UnusedDeclaration")
    public String getAttachmentFileName() {
        String contentDisposition = getContentDisposition();
        if (contentDisposition != null) {
            Matcher matcher = CONTENT_DISPOSITION_PATTERN.matcher(contentDisposition);
            if (matcher.matches())
                return matcher.group(1);
            else
                return contentDisposition;
        }
        return null;
    }
}
