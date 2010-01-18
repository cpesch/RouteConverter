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
import org.apache.commons.httpclient.methods.PostMethod;

/**
 * Wrapper to initiate an HTTP POST Request.
 *
 * @author Christian Pesch
 */

public class Post extends MultipartRequest {

    public Post(String url) {
        super(new PostMethod(url));
    }

    public String getHeader(String name) {
        Header header = method.getResponseHeader(name);
        return header != null ? header.getValue() : null;
    }

    public String getLocation() {
        // for the 201 result code
        return getHeader("Location");
    }
}
